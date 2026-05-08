package com.agguy.moni.app.ui.record.editor

import com.agguy.moni.core.RustCoreController

/**
 * 金额表达式计算器 —— 薄封装层，实际计算委托给 Rust 内核。
 *
 * 需在初始化后调用 [initialize] 注入 [RustCoreController] 实例。
 */
object ExpressionEvaluator {

    private lateinit var rustCore: RustCoreController

    /** 注入 Rust 核心控制器。 */
    fun initialize(rustCore: RustCoreController) {
        this.rustCore = rustCore
    }

    /**
     * 解析表达式并返回计算结果（分）。
     *
     * @param expression 原始表达式，如 "15.5+20"、"100-25×2"
     * @return 计算结果（分），表达式无效时返回 null
     */
    fun evaluateToCents(expression: String): Long? {
        return rustCore.evaluateExpression(expression)
    }

    /**
     * 判断表达式是否包含未计算的运算符。
     *
     * 注意：以运算符结尾的表达式（如 "15+"）也视为包含未计算运算符，
     * 但 evaluateToCents 会返回 null。
     */
    fun hasPendingOperation(expression: String): Boolean {
        return rustCore.hasPendingOperation(expression)
    }

    /**
     * 格式化表达式用于显示，在运算符两侧添加空格。
     */
    fun formatForDisplay(expression: String): String {
        return rustCore.formatExpressionForDisplay(expression)
    }
}
