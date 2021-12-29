/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensionsImpl
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.computeKotlinPaths
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.IrMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.jvm.compiler.forAllFiles
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.CompilationCanceledException
import java.io.File

class IncrementalFirJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    outputFiles: Collection<File>,
    modulesApiHistory: ModulesApiHistory,
    kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    classpathChanges: ClasspathChanges
) : IncrementalJvmCompilerRunner(
    workingDir,
    reporter,
    false,
    buildHistoryFile,
    outputFiles,
    modulesApiHistory,
    kotlinSourceFilesExtensions,
    classpathChanges
) {

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JVMCompilerArguments,
        caches: IncrementalJvmCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
//        val isIncremental = true // TODO
        val collector = GroupingMessageCollector(messageCollector, args.allWarningsAsErrors)
        // from K2JVMCompiler (~)
        val moduleName = args.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        val targetId = TargetId(moduleName, "java-production") // TODO: get rid of magic constant

        val dirtySources = linkedSetOf<File>().apply { addAll(sourcesToCompile) }

        // TODO: probably shoudl be passed along with sourcesToCompile
        // TODO: file path normalization
        val commonSources = args.commonSources?.mapTo(mutableSetOf(), ::File).orEmpty()

        val exitCode = ExitCode.OK
        val allCompiledSources = LinkedHashSet<File>()
        val rootDisposable = Disposer.newDisposable()

        try {
            // - configuration
            val configuration = CompilerConfiguration().apply {

                put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, collector)
                put(IrMessageLogger.IR_MESSAGE_LOGGER, IrMessageCollector(collector))

                setupCommonArguments(args) { JvmMetadataVersion(*it) }

                if (IncrementalCompilation.isEnabledForJvm()) {
                    putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, services[LookupTracker::class.java])

                    putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, services[ExpectActualTracker::class.java])

                    putIfNotNull(CommonConfigurationKeys.INLINE_CONST_TRACKER, services[InlineConstTracker::class.java])

                    putIfNotNull(
                        JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS,
                        services[IncrementalCompilationComponents::class.java]
                    )

                    putIfNotNull(ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER, services[JavaClassesTracker::class.java])
                }

                setupJvmSpecificArguments(args)
            }

            val paths = computeKotlinPaths(collector, args)
            if (collector.hasErrors()) return ExitCode.COMPILATION_ERROR to emptyList()

            // -- plugins
            val pluginClasspaths: Iterable<String> = args.pluginClasspaths?.asIterable() ?: emptyList()
            val pluginOptions = args.pluginOptions?.toMutableList() ?: ArrayList()
            // TODO: add scripting support when ready in FIR
            val pluginLoadResult = PluginCliParser.loadPluginsSafe(pluginClasspaths, pluginOptions, configuration)
            if (pluginLoadResult != ExitCode.OK) return pluginLoadResult to emptyList()
            // -- /plugins

            with(configuration) {
                configureJavaModulesContentRoots(args)
                configureStandardLibs(paths, args)
                configureAdvancedJvmOptions(args)
                configureKlibPaths(args)

                val destination = File(args.destination ?: ".")
                if (destination.path.endsWith(".jar")) {
                    put(JVMConfigurationKeys.OUTPUT_JAR, destination)
                } else {
                    put(JVMConfigurationKeys.OUTPUT_DIRECTORY, destination)
                }
                addAll(JVMConfigurationKeys.MODULES, listOf(ModuleBuilder(targetId.name, destination.path, targetId.type)))

                configureBaseRoots(args)
                configureSourceRootsFromSources(allSources, commonSources, args.javaPackagePrefix)
            }
            // - /configuration

            setIdeaIoUseFallback()

            // -AbstractProjectEnvironment-
            val projectEnvironment =
                createProjectEnvironment(configuration, rootDisposable, EnvironmentConfigFiles.JVM_CONFIG_FILES, messageCollector)

            // -sources
            val allPlatformSourceFiles = linkedSetOf<File>() // TODO: get from caller
            val allCommonSourceFiles = linkedSetOf<File>()

            configuration.kotlinSourceRoots.forAllFiles(configuration, projectEnvironment.project) { virtualFile, isCommon ->
                val file = File(virtualFile.canonicalPath ?: virtualFile.path)
                if (!file.isFile) error("TODO: better error: file not found $virtualFile")
                if (isCommon) allCommonSourceFiles.add(file)
                else allPlatformSourceFiles.add(file)
            }

            val diagnosticsReporter = DiagnosticReporterFactory.createReporter()
            val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)

            // !! main class - maybe from cache?
            var mainClassFqName: FqName? = null

            var incrementalExcludesScope: AbstractProjectFileSearchScope? = null

            fun firIncrementalCycle(): ModuleCompilerAnalyzedOutput? {
                while (true) {

                    val compilerInput = ModuleCompilerInput(
                        targetId,
                        CommonPlatforms.defaultCommonPlatform, dirtySources.filter { it in allCommonSourceFiles },
                        JvmPlatforms.unspecifiedJvmPlatform, dirtySources.filter { it in allPlatformSourceFiles },
                        configuration
                    )

                    val analysisResults =
                        compileModuleToAnalyzedFir(
                            compilerInput,
                            compilerEnvironment,
                            emptyList(),
                            incrementalExcludesScope,
                            diagnosticsReporter
                        )

                    // TODO: consider what to do if many compilations find a main class
                    if (mainClassFqName == null && configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
                        mainClassFqName = findMainClass(analysisResults.fir)
                    }

                    allCompiledSources.addAll(dirtySources)

                    if (diagnosticsReporter.hasErrors) {
                        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                            diagnosticsReporter,
                            collector,
                            configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
                        )
                        return null
                    }

                    val newDirtySources =
                        collectNewDirtySources(analysisResults, targetId, configuration, caches, allCompiledSources, reporter)

                    if (!isIncremental || newDirtySources.isEmpty()) return analysisResults

                    caches.platformCache.markDirty(newDirtySources)
                    val newDirtyFilesOutputsScope =
                        projectEnvironment.getSearchScopeByIoFiles(caches.inputsCache.getOutputForSourceFiles(newDirtySources))
                    incrementalExcludesScope = incrementalExcludesScope.let {
                        when {
                            newDirtyFilesOutputsScope.isEmpty -> it
                            it == null || it.isEmpty -> newDirtyFilesOutputsScope
                            else -> it + newDirtyFilesOutputsScope
                        }
                    }
                    caches.inputsCache.removeOutputForSourceFiles(newDirtySources)
                    dirtySources.addAll(newDirtySources)
                    projectEnvironment.localFileSystem.refresh(false)
                }
            }

            val cycleResult = firIncrementalCycle() ?: return ExitCode.COMPILATION_ERROR to allCompiledSources

            val extensions = JvmGeneratorExtensionsImpl(configuration)
            val irGenerationExtensions =
                (projectEnvironment as? VfsBasedProjectEnvironment)?.project?.let { IrGenerationExtension.getInstances(it) }.orEmpty()
            val mangler = JvmDescriptorMangler(null)
            val signaturer = JvmIdSignatureDescriptor(JvmDescriptorMangler(null))
            val allCommonFirFiles = cycleResult.session.moduleData.dependsOnDependencies
                .map { it.session }
                .filter { it.kind == FirSession.Kind.Source }
                .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

            val (irModuleFragment, symbolTable, components) = Fir2IrConverter.createModuleFragment(
                cycleResult.session, cycleResult.scopeSession, cycleResult.fir + allCommonFirFiles,
                cycleResult.session.languageVersionSettings, mangler, signaturer,
                extensions, FirJvmKotlinMangler(cycleResult.session), IrFactoryImpl,
                FirJvmVisibilityConverter,
                Fir2IrJvmSpecialAnnotationSymbolProvider(),
                irGenerationExtensions
            )

            val irInput = ModuleCompilerIrBackendInput(
                targetId,
                configuration,
                extensions,
                irModuleFragment,
                symbolTable,
                components,
                cycleResult.session
            )

            val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment)

            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                diagnosticsReporter,
                collector,
                configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            )

            writeOutputs(
                projectEnvironment,
                configuration,
                listOf(codegenOutput.generationState),
                mainClassFqName
            )
        } catch (e: CompilationCanceledException) {
            collector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", null)
            return ExitCode.OK to allCompiledSources
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is CompilationCanceledException) {
                collector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", null)
                return ExitCode.OK to allCompiledSources
            } else {
                throw e
            }
        } finally {
            collector.flush()
            Disposer.dispose(rootDisposable)
        }
        return exitCode to allCompiledSources
    }
}


fun CompilerConfiguration.configureBaseRoots(args: K2JVMCompilerArguments) {

    var isJava9Module = false
    args.javaSourceRoots?.forEach {
        val file = File(it)
        val packagePrefix = args.javaPackagePrefix
        addJavaSourceRoot(file, packagePrefix)
        if (!isJava9Module && packagePrefix == null && (file.name == PsiJavaModule.MODULE_INFO_FILE ||
                    (file.isDirectory && file.listFiles()?.any { it.name == PsiJavaModule.MODULE_INFO_FILE } == true))
        ) {
            isJava9Module = true
        }
    }

    args.classpath?.split(File.pathSeparator)?.forEach { classpathRoot ->
        add(
            CLIConfigurationKeys.CONTENT_ROOTS,
            if (isJava9Module) JvmModulePathRoot(File(classpathRoot)) else JvmClasspathRoot(File(classpathRoot))
        )
    }

    // TODO: modularJdkRoot (now seems only processed from the build file
}

fun CompilerConfiguration.configureSourceRootsFromSources(
    allSources: Collection<File>, commonSources: Set<File>, javaPackagePrefix: String?
) {
    for (sourceFile in allSources) {
        if (sourceFile.name.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION)) {
            addJavaSourceRoot(sourceFile, javaPackagePrefix)
        } else {
            addKotlinSourceRoot(sourceFile.path, isCommon = sourceFile in commonSources)

            if (sourceFile.isDirectory) {
                addJavaSourceRoot(sourceFile, javaPackagePrefix)
            }
        }
    }
}