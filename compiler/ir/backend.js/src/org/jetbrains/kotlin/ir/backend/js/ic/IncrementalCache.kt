/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import java.io.File


class IncrementalCache(private val library: KotlinLibrary, cachePath: String) : ArtifactCache() {
    companion object {
        private const val cacheFullInfoFile = "cache.full.info"
        private const val cacheFastInfoFile = "cache.fast.info"
        private const val binaryAstSuffix = ".binary.ast"
    }

    class CacheFastInfo(
        var moduleName: String? = null,
        var flatHash: ULong = 0UL,
        var transHash: ULong = 0UL,
        var configHash: ULong = 0UL,
        var initialFlatHash: ULong = 0UL
    )

    private enum class CacheState { NON_LOADED, FETCHED_FOR_DEPENDENCY, FETCHED_FULL }

    private var state = CacheState.NON_LOADED

    private val cacheDir = File(cachePath)
    private val signatureToIdMapping = mutableMapOf<String, Map<IdSignature, Int>>()

    private val fingerprints = mutableMapOf<String, ULong>()
    private val usedInlineFunctions = mutableMapOf<String, Map<IdSignature, ULong>>()
    private val implementedInlineFunctions = mutableMapOf<String, Map<IdSignature, ULong>>()

    private var cacheFastInfo = CacheFastInfo().apply {
        File(cacheDir, cacheFastInfoFile).useCodedInputIfExists {
            moduleName = readString()
            flatHash = readFixed64().toULong()
            transHash = readFixed64().toULong()
            configHash = readFixed64().toULong()
        }
        initialFlatHash = flatHash
    }

    val srcFingerprints: Map<String, ULong> get() = fingerprints
    val usedFunctions: Map<String, Map<IdSignature, ULong>> get() = usedInlineFunctions
    val implementedFunctions: Collection<Map<IdSignature, ULong>> get() = implementedInlineFunctions.values

    val klibUpdated: Boolean get() = cacheFastInfo.run { initialFlatHash == 0UL || initialFlatHash != flatHash }

    val klibTransitiveHash: ULong get() = cacheFastInfo.transHash

    var srcFilesInOrderFromKLib: List<String> = emptyList()
        private set

    var deletedSrcFiles: Set<String> = emptySet()
        private set

    fun updateSignatureToIdMapping(srcPath: String, mapping: Map<IdSignature, Int>) {
        signatureToIdMapping[srcPath] = mapping
    }

    fun updateHashes(
        srcPath: String, fingerprint: ULong, usedFunctions: Map<IdSignature, ULong>?, implementedFunctions: Map<IdSignature, ULong>?
    ) {
        fingerprints[srcPath] = fingerprint
        usedFunctions?.let { usedInlineFunctions[srcPath] = it }
        implementedFunctions?.let { implementedInlineFunctions[srcPath] = it }
    }

    fun invalidateCacheForNewConfig(configHash: ULong) {
        if (cacheFastInfo.configHash != configHash) {
            invalidate()
            cacheFastInfo.configHash = configHash
        }
    }

    fun checkAndUpdateCacheFastInfo(flatHash: ULong, transHash: ULong): Boolean {
        if (cacheFastInfo.transHash != transHash) {
            cacheFastInfo.flatHash = flatHash
            cacheFastInfo.transHash = transHash
            return false
        }
        return true
    }

    private fun commitCacheFastInfo(klibModuleName: String? = null): Unit = cacheFastInfo.run {
        moduleName = klibModuleName ?: moduleName
        val name = moduleName ?: error("Internal error: uninitialized fast cache info for ${library.libraryName}")
        File(cacheDir, cacheFastInfoFile).useCodedOutput {
            writeStringNoTag(name)
            writeFixed64NoTag(flatHash.toLong())
            writeFixed64NoTag(transHash.toLong())
            writeFixed64NoTag(configHash.toLong())
        }
    }

    private fun CodedInputStream.readFunctionHashes(
        deserializer: IdSignatureDeserializer, signatureToId: MutableMap<IdSignature, Int>? = null
    ): Map<IdSignature, ULong>? {
        val functions = readInt32()
        if (functions == 0) {
            return null
        }
        val result = mutableMapOf<IdSignature, ULong>()
        for (funIndex in 0 until functions) {
            val sigId = readInt32()
            val hash = readFixed64().toULong()
            try {
                val signature = deserializer.deserializeIdSignature(sigId)
                result[signature] = hash
                signatureToId?.let { it[signature] = sigId }
            } catch (ex: IndexOutOfBoundsException) {
                // Signature has been removed
            }
        }
        return result
    }

    fun fetchFullCacheData() {
        when (state) {
            CacheState.FETCHED_FULL -> return
            CacheState.FETCHED_FOR_DEPENDENCY ->
                error("Internal error: cache for ${library.libraryName} has been already fetched for dependency")
            CacheState.NON_LOADED -> {
                state = CacheState.FETCHED_FULL
                val signatureReaders = library.filesAndSigReaders()
                srcFilesInOrderFromKLib = signatureReaders.map { it.first }

                File(cacheDir, cacheFullInfoFile).useCodedInputIfExists {
                    val deleted = mutableSetOf<String>()

                    val signatureReadersMap = signatureReaders.toMap()
                    val srcFiles = readInt32()
                    for (srcIndex in 0 until srcFiles) {
                        val srcPath = readString()
                        val fingerprint = readFixed64().toULong()

                        val deserializer = signatureReadersMap[srcPath]
                        if (deserializer != null) {
                            fingerprints[srcPath] = fingerprint
                            val signatureToId = mutableMapOf<IdSignature, Int>()
                            readFunctionHashes(deserializer, signatureToId)?.let { implementedInlineFunctions[srcPath] = it }
                            readFunctionHashes(deserializer, signatureToId)?.let { usedInlineFunctions[srcPath] = it }
                            if (signatureToId.isNotEmpty()) {
                                signatureToIdMapping[srcPath] = signatureToId
                            }
                        } else {
                            deleted.add(srcPath)
                            skipFunctionHashes()
                            skipFunctionHashes()
                        }
                    }

                    deletedSrcFiles = deleted
                }
            }
        }
    }

    private fun CodedInputStream.skipFunctionHashes() {
        val functions = readInt32()
        for (funIndex in 0 until functions) {
            readInt32() // skip sigid
            readFixed64() // skip hash
        }
    }

    fun fetchCacheDataForDependency() = File(cacheDir, cacheFullInfoFile).useCodedInputIfExists {
        if (state == CacheState.NON_LOADED) {
            state = CacheState.FETCHED_FOR_DEPENDENCY
            val signatureReadersMap = library.filesAndSigReaders().toMap()

            val srcFiles = readInt32()
            for (srcIndex in 0 until srcFiles) {
                val srcPath = readString()
                val fingerprint = readFixed64().toULong()

                val deserializer = signatureReadersMap[srcPath]
                if (deserializer != null) {
                    fingerprints[srcPath] = fingerprint
                    readFunctionHashes(deserializer)?.let { implementedInlineFunctions[srcPath] = it }
                }
                skipFunctionHashes()
            }
        }
    }

    private fun getBinaryAstPath(srcFile: String): File {
        val binaryAstFileName = File(srcFile).name + srcFile.stringHashForIC().toString(16) + binaryAstSuffix
        return File(cacheDir, binaryAstFileName)
    }

    private fun CodedOutputStream.writeFunctionHashes(sigToIndexMap: Map<IdSignature, Int>, hashes: Map<IdSignature, ULong>) {
        writeInt32NoTag(hashes.size)
        for ((sig, functionHash) in hashes) {
            val sigId = sigToIndexMap[sig] ?: error("No index found for sig $sig")
            writeInt32NoTag(sigId)
            writeFixed64NoTag(functionHash.toLong())
        }
    }

    private fun commitCacheFullInfo() {
        if (state != CacheState.FETCHED_FULL) {
            error("Internal error: cache for ${library.libraryName} has not been fetched fully")
        }

        File(cacheDir, cacheFullInfoFile).useCodedOutput {
            writeInt32NoTag(fingerprints.size)
            for ((srcPath, fingerprint) in fingerprints) {
                writeStringNoTag(srcPath)
                writeFixed64NoTag(fingerprint.toLong())

                val sigToIndexMap = signatureToIdMapping[srcPath] ?: emptyMap()
                writeFunctionHashes(sigToIndexMap, implementedInlineFunctions[srcPath] ?: emptyMap())
                writeFunctionHashes(sigToIndexMap, usedInlineFunctions[srcPath] ?: emptyMap())
            }
        }
    }

    private fun clearCacheAfterCommit() {
        state = CacheState.FETCHED_FOR_DEPENDENCY
        signatureToIdMapping.clear()
        usedInlineFunctions.clear()
        srcFilesInOrderFromKLib = emptyList()
        deletedSrcFiles = emptySet()
    }

    fun commitCacheForRemovedSrcFiles() {
        commitCacheFastInfo()
        if (deletedSrcFiles.isNotEmpty()) {
            commitCacheFullInfo()
        }
        clearCacheAfterCommit()
    }

    fun commitCacheForRebuiltSrcFiles(klibModuleName: String) {
        commitCacheFastInfo(klibModuleName)
        commitCacheFullInfo()
        for ((srcPath, ast) in binaryAsts) {
            getBinaryAstPath(srcPath).apply { recreate() }.writeBytes(ast)
        }
        clearCacheAfterCommit()
    }

    override fun fetchArtifacts() = KLibArtifact(
        moduleName = cacheFastInfo.moduleName ?: error("Internal error: missing module name"),
        fileArtifacts = fingerprints.keys.map {
            SrcFileArtifact(it, getBinaryAstPath(it).absolutePath, binaryAsts[it])
        })

    fun invalidate() {
        cacheDir.deleteRecursively()
        signatureToIdMapping.clear()
        implementedInlineFunctions.clear()
        usedInlineFunctions.clear()
        fingerprints.clear()
        binaryAsts.clear()
        cacheFastInfo = CacheFastInfo()
        srcFilesInOrderFromKLib = emptyList()
        deletedSrcFiles = emptySet()
    }

    fun invalidateForSrcFile(srcPath: String) {
        getBinaryAstPath(srcPath).delete()
        signatureToIdMapping.remove(srcPath)
        implementedInlineFunctions.remove(srcPath)
        usedInlineFunctions.remove(srcPath)
        fingerprints.remove(srcPath)
        binaryAsts.remove(srcPath)
    }

    private fun KotlinLibrary.filesAndSigReaders(): List<Pair<String, IdSignatureDeserializer>> {
        val fileSize = fileCount()
        val result = ArrayList<Pair<String, IdSignatureDeserializer>>(fileSize)
        val extReg = ExtensionRegistryLite.newInstance()

        for (i in 0 until fileSize) {
            val fileStream = file(i).codedInputStream
            val fileProto = IrFile.parseFrom(fileStream, extReg)
            val sigReader = IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
                private fun err(): Nothing = error("Not supported")
                override fun irDeclaration(index: Int): ByteArray = err()
                override fun type(index: Int): ByteArray = err()
                override fun signature(index: Int): ByteArray = signature(index, i)
                override fun string(index: Int): ByteArray = string(index, i)
                override fun body(index: Int): ByteArray = err()
                override fun debugInfo(index: Int): ByteArray? = null
            }), null)

            result.add(fileProto.fileEntry.name to sigReader)
        }

        return result
    }
}
