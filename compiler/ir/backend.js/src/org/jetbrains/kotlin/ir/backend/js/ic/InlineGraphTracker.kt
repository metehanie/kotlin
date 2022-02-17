/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid


class InlineFunctionFlatHashBuilder : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.isInline) {
            flatHashes[declaration] = declaration.irElementHashForIC()
        }
        // go deeper since local inline special declarations (like a reference adaptor) may appear
        declaration.acceptChildren(this, null)
    }

    private val flatHashes = mutableMapOf<IrSimpleFunction, ULong>()

    fun getFlatHashes() = flatHashes
}

interface InlineFunctionHashProvider {
    fun hashForExternalFunction(declaration: IrSimpleFunction): ULong?
}

class InlineFunctionHashBuilder(
    private val hashProvider: InlineFunctionHashProvider,
    private val flatHashes: Map<IrSimpleFunction, ULong>
) {
    private val inlineFunctionCallGraph: MutableMap<IrSimpleFunction, Set<IrSimpleFunction>> = mutableMapOf()

    private inner class GraphBuilder : IrElementVisitor<Unit, MutableSet<IrSimpleFunction>> {

        override fun visitElement(element: IrElement, data: MutableSet<IrSimpleFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: MutableSet<IrSimpleFunction>) {
            val newGraph = mutableSetOf<IrSimpleFunction>()
            inlineFunctionCallGraph[declaration] = newGraph
            declaration.acceptChildren(this, newGraph)
        }


        override fun visitCall(expression: IrCall, data: MutableSet<IrSimpleFunction>) {
            val callee = expression.symbol.owner

            if (callee.isInline) {
                data.add(callee)
            }

            expression.acceptChildren(this, data)
        }
    }

    private inner class InlineFunctionHashProcessor {
        private val computedHashes = mutableMapOf<IrSimpleFunction, ULong>()
        private val processingFunctions = mutableSetOf<IrSimpleFunction>()

        private fun processInlineFunction(f: IrSimpleFunction): ULong = computedHashes.getOrPut(f) {
            if (!processingFunctions.add(f)) {
                error("Inline circle through function ${f.render()} detected")
            }
            val callees = inlineFunctionCallGraph[f] ?: error("Internal error: Inline function is missed in inline graph ${f.render()}")
            val flatHash = flatHashes[f] ?: error("Internal error: No flat hash for ${f.render()}")
            val functionInlineHash = HashCombiner(flatHash).also {
                for (callee in callees) {
                    it.update(processCallee(callee))
                }
            }.getResult()
            processingFunctions.remove(f)
            functionInlineHash
        }

        private fun processCallee(callee: IrSimpleFunction): ULong {
            if (callee in flatHashes) {
                return processInlineFunction(callee)
            }
            return hashProvider.hashForExternalFunction(callee) ?: error("Internal error: No hash found for ${callee.render()}")
        }

        fun process(): Map<IrSimpleFunction, ULong> {
            for ((f, callees) in inlineFunctionCallGraph.entries) {
                if (f.isInline) {
                    processInlineFunction(f)
                } else {
                    callees.forEach(::processCallee)
                }
            }
            return computedHashes
        }
    }

    fun buildHashes(dirtyFiles: Collection<IrFile>): Map<IrSimpleFunction, ULong> {
        dirtyFiles.forEach { it.acceptChildren(GraphBuilder(), mutableSetOf()) }
        return InlineFunctionHashProcessor().process()
    }

    fun buildInlineGraph(computedHashed: Map<IrSimpleFunction, ULong>): Map<IrFile, Map<IdSignature, ULong>> {
        val perFileInlineGraph = inlineFunctionCallGraph.entries.groupBy({ it.key.file }) {
            it.value
        }

        return perFileInlineGraph.entries.associate {
            val usedInlineFunctions = mutableMapOf<IdSignature, ULong>()
            it.value.forEach { edges ->
                edges.forEach { callee ->
                    if (!callee.isFakeOverride) {
                        val signature = callee.symbol.signature // ?: error("Expecting signature for ${callee.render()}")
                        if (signature?.visibleCrossFile == true) {
                            val calleeHash = computedHashed[callee]
                                ?: hashProvider.hashForExternalFunction(callee)
                                ?: error("Internal error: No has found for ${callee.render()}")
                            usedInlineFunctions[signature] = calleeHash
                        }
                    }
                }
            }
            it.key to usedInlineFunctions
        }
    }
}
