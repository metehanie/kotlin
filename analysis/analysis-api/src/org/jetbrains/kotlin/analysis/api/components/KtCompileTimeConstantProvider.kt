/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.psi.KtExpression

public enum class KtConstantEvaluationMode {
    /**
     * In this mode, expressions and properties that are free from runtime behaviors/changes will be evaluated,
     *   such as `const val` properties or binary expressions whose operands are constants.
     */
    CONSTANT_EXPRESSION_EVALUATION,

    /**
     * In this mode, more expressions and properties that could be composites of other constants will be evaluated,
     *   such as `val` properties with constant initializers or binary expressions whose operands could be constants.
     */
    CONSTANT_LIKE_EXPRESSION_EVALUATION;
}

public abstract class KtCompileTimeConstantProvider : KtAnalysisSessionComponent() {
    public abstract fun evaluate(
        expression: KtExpression,
        mode: KtConstantEvaluationMode,
    ): KtConstantValue?
}

public interface KtCompileTimeConstantProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtExpression.evaluate(mode: KtConstantEvaluationMode): KtConstantValue? =
        analysisSession.compileTimeConstantProvider.evaluate(this, mode)
}
