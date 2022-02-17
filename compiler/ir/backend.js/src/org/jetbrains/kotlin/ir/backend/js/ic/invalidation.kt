/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File


private fun invalidateCacheForModule(
    library: KotlinLibrary,
    incrementalCache: IncrementalCache,
    externalHashes: Map<IdSignature, ULong>,
): Pair<Set<String>, Map<String, ULong>> {
    val fileFingerPrints = mutableMapOf<String, ULong>()
    val dirtyFiles = mutableSetOf<String>()

    if (incrementalCache.klibUpdated) {
        for ((index, file) in incrementalCache.srcFilesInOrderFromKLib.withIndex()) {
            // 1. get cached fingerprints
            val fileOldFingerprint = incrementalCache.srcFingerprints[file] ?: 0

            // 2. calculate new fingerprints
            val fileNewFingerprint = library.fingerprint(index)

            if (fileOldFingerprint != fileNewFingerprint) {
                fileFingerPrints[file] = fileNewFingerprint
                incrementalCache.invalidateForSrcFile(file)

                // 3. form initial dirty set
                dirtyFiles.add(file)
            }
        }
    }

    // 4. extend dirty set with inline functions
    do {
        if (dirtyFiles.size == incrementalCache.srcFilesInOrderFromKLib.size) break

        val oldSize = dirtyFiles.size
        for (file in incrementalCache.srcFilesInOrderFromKLib) {

            if (file in dirtyFiles) continue

            // check for clean file
            val usedInlineFunctions = incrementalCache.usedFunctions[file] ?: emptyMap()

            for ((sig, oldHash) in usedInlineFunctions) {
                val actualHash = externalHashes[sig] ?: incrementalCache.implementedFunctions.firstNotNullOfOrNull { it[sig] }
                // null means inline function is from dirty file, could be a bit more optimal
                if (actualHash == null || oldHash != actualHash) {
                    fileFingerPrints[file] = incrementalCache.srcFingerprints[file] ?: error("Cannot find fingerprint for $file")
                    incrementalCache.invalidateForSrcFile(file)
                    dirtyFiles.add(file)
                    break
                }
            }
        }
    } while (oldSize != dirtyFiles.size)

    // 5. invalidate file caches
    for (deleted in incrementalCache.deletedSrcFiles) {
        incrementalCache.invalidateForSrcFile(deleted)
    }

    return dirtyFiles to fileFingerPrints
}

private fun buildCacheForModule(
    configuration: CompilerConfiguration,
    irModule: IrModuleFragment,
    deserializer: JsIrLinker,
    dependencies: Collection<IrModuleFragment>,
    dirtyFiles: Collection<String>,
    cleanInlineHashes: Map<IdSignature, ULong>,
    incrementalCache: IncrementalCache,
    fileFingerPrints: Map<String, ULong>,
    mainArguments: List<String>?,
    cacheExecutor: CacheExecutor
) {
    val dirtyIrFiles = irModule.files.filter { it.fileEntry.name in dirtyFiles }

    val flatHashes = InlineFunctionFlatHashBuilder().apply {
        dirtyIrFiles.forEach { it.acceptVoid(this) }
    }.getFlatHashes()

    val hashProvider = object : InlineFunctionHashProvider {
        override fun hashForExternalFunction(declaration: IrSimpleFunction): ULong? {
            return declaration.symbol.signature?.let { cleanInlineHashes[it] }
        }
    }

    val hashBuilder = InlineFunctionHashBuilder(hashProvider, flatHashes)

    val hashes = hashBuilder.buildHashes(dirtyIrFiles)

    val splitPerFiles =
        hashes.entries.filter { !it.key.isFakeOverride && (it.key.symbol.signature?.visibleCrossFile ?: false) }.groupBy({ it.key.file }) {
            val signature = it.key.symbol.signature ?: error("Unexpected private inline fun ${it.key.render()}")
            signature to it.value
        }

    val inlineGraph = hashBuilder.buildInlineGraph(hashes)

    dirtyIrFiles.forEach { irFile ->
        val fileName = irFile.fileEntry.name
        incrementalCache.updateHashes(
            srcPath = fileName,
            fingerprint = fileFingerPrints[fileName] ?: error("No fingerprint found for file $fileName"),
            usedFunctions = inlineGraph[irFile],
            implementedFunctions = splitPerFiles[irFile]?.toMap()
        )
    }

    // TODO: actual way of building a cache could change in future
    cacheExecutor.execute(
        irModule,
        dependencies,
        deserializer,
        configuration,
        dirtyFiles,
        incrementalCache,
        emptySet(),
        mainArguments
    )
}

private fun loadModules(
    languageVersionSettings: LanguageVersionSettings,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>
): Map<ModuleDescriptor, KotlinLibrary> {
    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    var runtimeModule: ModuleDescriptorImpl? = null

    // TODO: deduplicate this code using part from klib.kt
    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val lookupTracker = LookupTracker.DO_NOTHING
        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            languageVersionSettings,
            LockBasedStorageManager.NO_LOCKS,
            runtimeModule?.builtIns,
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
        )
        if (isBuiltIns) runtimeModule = md

        val dependencies = dependencyGraph[current]!!.map { getModuleDescriptor(it) }
        md.setDependencies(listOf(md) + dependencies)
        md
    }

    return dependencyGraph.keys.associateBy { klib -> getModuleDescriptor(klib) }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun createLinker(
    configuration: CompilerConfiguration,
    loadedModules: Map<ModuleDescriptor, KotlinLibrary>,
    irFactory: IrFactory
): JsIrLinker {
    val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None
    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)
    val moduleDescriptor = loadedModules.keys.last()
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
    return JsIrLinker(null, logger, irBuiltIns, symbolTable, null)
}

private fun KotlinLibrary.moduleCanonicalName() = libraryFile.canonicalPath

private fun loadLibraries(configuration: CompilerConfiguration, dependencies: Collection<String>): Map<String, KotlinLibrary> {
    val allResolvedDependencies = jsResolveLibraries(
        dependencies,
        configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
        configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    )

    return allResolvedDependencies.getFullList().associateBy { it.moduleCanonicalName() }
}

fun interface CacheExecutor {
    fun execute(
        currentModule: IrModuleFragment,
        dependencies: Collection<IrModuleFragment>,
        deserializer: JsIrLinker,
        configuration: CompilerConfiguration,
        dirtyFiles: Collection<String>?, // if null consider the whole module dirty
        artifactCache: ArtifactCache,
        exportedDeclarations: Set<FqName>,
        mainArguments: List<String>?,
    )
}

private fun checkLibrariesHash(
    currentLib: KotlinLibrary,
    dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    incrementalCache: IncrementalCache,
    klibIncrementalCaches: Map<KotlinLibrary, IncrementalCache>,
): Boolean {
    val flatHash = File(currentLib.moduleCanonicalName()).fileHashForIC()
    val dependencies = dependencyGraph[currentLib] ?: error("Cannot find dependencies for ${currentLib.libraryName}")

    val transHash = HashCombiner(flatHash).also {
        for (dep in dependencies) {
            val depCache = klibIncrementalCaches[dep] ?: error("Cannot cache info for ${dep.libraryName}")
            it.update(depCache.klibTransitiveHash)
        }
    }.getResult()
    return incrementalCache.checkAndUpdateCacheFastInfo(flatHash, transHash)
}

enum class CacheUpdateStatus(val upToDate: Boolean) {
    DIRTY(upToDate = false),
    NO_DIRTY_FILES(upToDate = true),
    FAST_PATH(upToDate = true);

    var removed: Collection<String> = emptyList()
        private set
    var updated: Collection<String> = emptyList()
        private set
    var updatedAll: Boolean = false
        private set

    fun srcFiles(
        srcRemoved: Collection<String>,
        srcUpdated: Collection<String> = emptyList(),
        updatedAllSrc: Boolean = false
    ): CacheUpdateStatus {
        removed = srcRemoved
        updated = srcUpdated
        updatedAll = updatedAllSrc
        return this
    }
}

fun actualizeCaches(
    includes: String,
    compilerConfiguration: CompilerConfiguration,
    dependencies: Collection<String>,
    icCachePaths: Collection<String>,
    irFactory: () -> IrFactory,
    mainArguments: List<String>?,
    executor: CacheExecutor,
    callback: (CacheUpdateStatus, String) -> Unit
): List<KLibArtifact> {
    val (libraries, dependencyGraph, configHash) = CacheConfiguration(dependencies, compilerConfiguration)
    val cacheMap = libraries.values.zip(icCachePaths).toMap()

    val klibIncrementalCaches = mutableMapOf<KotlinLibrary, IncrementalCache>()

    val visitedLibraries = mutableSetOf<KotlinLibrary>()
    fun visitDependency(library: KotlinLibrary) {
        if (library in visitedLibraries) return
        visitedLibraries.add(library)

        val libraryDeps = dependencyGraph[library] ?: error("Unknown library ${library.libraryName}")
        libraryDeps.forEach { visitDependency(it) }

        val cachePath = cacheMap[library] ?: error("Unknown cache for library ${library.libraryName}")


        val incrementalCache = IncrementalCache(library, cachePath)
        klibIncrementalCaches[library] = incrementalCache

        incrementalCache.invalidateCacheForNewConfig(configHash)
        val updateStatus = when {
            checkLibrariesHash(library, dependencyGraph, incrementalCache, klibIncrementalCaches) -> CacheUpdateStatus.FAST_PATH
            else -> actualizeCacheForModule(
                library = library,
                configuration = compilerConfiguration,
                dependencyGraph = getDependencySubGraphFor(library, dependencyGraph),
                klibIncrementalCaches = klibIncrementalCaches,
                irFactory = irFactory(),
                mainArguments = mainArguments,
                cacheExecutor = executor,
            )
        }
        callback(updateStatus, library.libraryFile.path)
    }

    val canonicalIncludes = File(includes).canonicalPath
    val mainLibrary = libraries[canonicalIncludes] ?: error("Main library not found in libraries: $canonicalIncludes")
    visitDependency(mainLibrary)
    return klibIncrementalCaches.map { it.value.fetchArtifacts() }
}

private fun getDependencySubGraphFor(
    targetLib: KotlinLibrary,
    dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>
): Map<KotlinLibrary, List<KotlinLibrary>> {
    val subGraph = mutableMapOf<KotlinLibrary, List<KotlinLibrary>>()

    fun addDependsFor(library: KotlinLibrary) {
        if (library in subGraph) {
            return
        }
        val dependencies = dependencyGraph[library] ?: error("Cannot find dependencies for ${library.libraryName}")
        subGraph[library] = dependencies
        for (dependency in dependencies) {
            addDependsFor(dependency)
        }
    }
    addDependsFor(targetLib)
    return subGraph
}

class CacheConfiguration(
    dependencies: Collection<String>,
    val compilerConfiguration: CompilerConfiguration
) {
    val libraries: Map<String, KotlinLibrary> = loadLibraries(compilerConfiguration, dependencies)

    private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>
        get() {
            val nameToKotlinLibrary: Map<String, KotlinLibrary> = libraries.values.associateBy { it.moduleName }

            return libraries.values.associateWith {
                it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                    nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
                }
            }
        }

    private val configHash
        get() = compilerConfiguration.configHashForIC()

    operator fun component1() = libraries
    operator fun component2() = dependencyGraph
    operator fun component3() = configHash
}

private fun actualizeCacheForModule(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    klibIncrementalCaches: Map<KotlinLibrary, IncrementalCache>,
    irFactory: IrFactory,
    mainArguments: List<String>?,
    cacheExecutor: CacheExecutor,
): CacheUpdateStatus {
    // 1. Invalidate
    val dependencies = dependencyGraph[library]!!

    val incrementalCache = klibIncrementalCaches[library] ?: error("No cache provider for $library")

    val sigHashes = mutableMapOf<IdSignature, ULong>()
    dependencies.forEach { lib ->
        klibIncrementalCaches[lib]?.let { libCache ->
            libCache.fetchCacheDataForDependency()
            libCache.implementedFunctions.forEach { sigHashes.putAll(it) }
        }
    }

    incrementalCache.fetchFullCacheData()
    val (dirtySet, fileFingerPrints) = invalidateCacheForModule(library, incrementalCache, sigHashes)
    val removed = incrementalCache.deletedSrcFiles

    if (dirtySet.isEmpty()) {
        // up-to-date
        incrementalCache.commitCacheForRemovedSrcFiles()
        return CacheUpdateStatus.NO_DIRTY_FILES.srcFiles(removed)
    }

    // 2. Build
    val (jsIrLinker, currentIrModule, irModules) = processJsIrLinker(library, configuration, dependencyGraph, dirtySet, irFactory)

    val currentModuleDeserializer = jsIrLinker.moduleDeserializer(currentIrModule.descriptor)

    incrementalCache.implementedFunctions.forEach { sigHashes.putAll(it) }

    for (dirtySrcFile in dirtySet) {
        val signatureMapping = currentModuleDeserializer.signatureDeserializerForFile(dirtySrcFile).signatureToIndexMapping()
        incrementalCache.updateSignatureToIdMapping(dirtySrcFile, signatureMapping)
    }

    buildCacheForModule(
        configuration,
        currentIrModule,
        jsIrLinker,
        irModules,
        dirtySet,
        sigHashes,
        incrementalCache,
        fileFingerPrints,
        mainArguments,
        cacheExecutor
    )

    val updatedAll = dirtySet.size == incrementalCache.srcFilesInOrderFromKLib.size
    incrementalCache.commitCacheForRebuiltSrcFiles(currentIrModule.name.asString())
    // invalidated and re-built
    return CacheUpdateStatus.DIRTY.srcFiles(removed, dirtySet, updatedAll)
}

private fun processJsIrLinker(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    dirtyFiles: Collection<String>?,
    irFactory: IrFactory,
): Triple<JsIrLinker, IrModuleFragment, Collection<IrModuleFragment>> {
    val loadedModules = loadModules(configuration.languageVersionSettings, dependencyGraph)

    val jsIrLinker = createLinker(configuration, loadedModules, irFactory)

    val irModules = ArrayList<Pair<IrModuleFragment, KotlinLibrary>>(loadedModules.size)

    // TODO: modules deserialized here have to be reused for cache building further
    for ((descriptor, loadedLibrary) in loadedModules) {
        if (library == loadedLibrary) {
            if (dirtyFiles != null) {
                irModules.add(jsIrLinker.deserializeDirtyFiles(descriptor, loadedLibrary, dirtyFiles) to loadedLibrary)
            } else {
                irModules.add(jsIrLinker.deserializeFullModule(descriptor, loadedLibrary) to loadedLibrary)
            }
        } else {
            irModules.add(jsIrLinker.deserializeHeadersWithInlineBodies(descriptor, loadedLibrary) to loadedLibrary)
        }
    }

    jsIrLinker.init(null, emptyList())

    ExternalDependenciesGenerator(jsIrLinker.symbolTable, listOf(jsIrLinker)).generateUnboundSymbolsAsDependencies()

    jsIrLinker.postProcess()

    val currentIrModule = irModules.find { it.second == library }?.first!!

    return Triple(jsIrLinker, currentIrModule, irModules.map { it.first })
}

// Used for tests only
fun rebuildCacheForDirtyFiles(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    dirtyFiles: Collection<String>?,
    artifactCache: ArtifactCache,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?,
): String {
    val (jsIrLinker, currentIrModule, irModules) = processJsIrLinker(library, configuration, dependencyGraph, dirtyFiles, irFactory)

    buildCacheForModuleFiles(
        currentIrModule,
        irModules,
        jsIrLinker,
        configuration,
        dirtyFiles,
        artifactCache,
        exportedDeclarations,
        mainArguments
    )
    return currentIrModule.name.asString()
}

@Suppress("UNUSED_PARAMETER")
fun buildCacheForModuleFiles(
    currentModule: IrModuleFragment,
    dependencies: Collection<IrModuleFragment>,
    deserializer: JsIrLinker,
    configuration: CompilerConfiguration,
    dirtyFiles: Collection<String>?, // if null consider the whole module dirty
    artifactCache: ArtifactCache,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?,
) {
    compileWithIC(
        currentModule,
        configuration = configuration,
        deserializer = deserializer,
        dependencies = dependencies,
        mainArguments = mainArguments,
        exportedDeclarations = exportedDeclarations,
        filesToLower = dirtyFiles?.toSet(),
        artifactCache = artifactCache,
    )
}
