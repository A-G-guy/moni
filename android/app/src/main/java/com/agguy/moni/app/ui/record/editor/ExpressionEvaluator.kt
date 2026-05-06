package com.agguy.moni.app.ui.record.editor

/**
 * 金额表达式计算器。
 *
 * 仅支持简单加减运算（从左到右顺序计算），
 * 每个操作数最多两位小数，结果四舍五入后转为分（cents）。
 */
object ExpressionEvaluator {

    private val operatorRegex = "(?<!^)(?=[+-])".toRegex()

    /**
     * 解析表达式并返回计算结果（分）。
     *
     * @param expression 原始表达式，如 "15.5+20"、"100-25.5+10"
     * @return 计算结果（分），表达式无效时返回 null
     */
    fun evaluateToCents(expression: String): Long? {
        if (expression.isBlank()) return null
        if (!hasPendingOperation(expression)) {
            return parseOperandToCents(expression)
        }

        // 以运算符结尾（如 "15+"）视为无效
        val trimmed = expression.trim()
        if (trimmed.last() in setOf('+', '-')) return null

        val tokens = expression.split(operatorRegex)
        if (tokens.isEmpty()) return null

        var resultCents = parseOperandToCents(tokens[0]) ?: return null

        for (i in 1 until tokens.size) {
            val rawToken = tokens[i]
            if (rawToken.isEmpty()) return null

            // split 后 token 包含前导运算符，如 "+20"、"-25.5"
            val operator = rawToken[0]
            val operandStr = rawToken.substring(1)
            if (operandStr.isEmpty()) return null

            val operandCents = parseOperandToCents(operandStr) ?: return null

            resultCents = when (operator) {
                '+' -> resultCents + operandCents
                '-' -> resultCents - operandCents
                else -> return null
            }
        }

        return if (resultCents >= 0) resultCents else null
    }

    /**
     * 判断表达式是否包含未计算的运算符。
     *
     * 注意：以运算符结尾的表达式（如 "15+"）也视为包含未计算运算符，
     * 但 evaluateToCents 会返回 null。
     */
    fun hasPendingOperation(expression: String): Boolean {
        if (expression.isBlank()) return false
        val trimmed = expression.trim()
        // 以运算符结尾视为待计算
        if (trimmed.last() in setOf('+', '-')) return true
        return trimmed.contains('+') || trimmed.contains('-')
    }

    /**
     * 格式化表达式用于显示，在运算符两侧添加空格。
     */
    fun formatForDisplay(expression: String): String {
        return expression.replace("+", " + ").replace("-", " - ")
    }

    /**
     * 解析单个操作数为分。
     *
     * 有效格式：整数（"15"）、一位小数（"15.5"）、两位小数（"15.50"）。
     * 不允许：空字符串、多个小数点、超过两位小数、负数（表达式中负号由运算符处理）。
     */
    private fun parseOperandToCents(operand: String): Long? {
        val trimmed = operand.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("-")) return null // 负数由运算符处理

        val parts = trimmed.split(".")
        if (parts.size > 2) return null // 多个小数点

        val integerPart = parts[0]
        if (integerPart.isEmpty()) return null
        if (!integerPart.all { it.isDigit() }) return null

        val integer = integerPart.toLongOrNull() ?: return null

        val decimalPart = if (parts.size > 1) parts[1] else ""
        if (decimalPart.isNotEmpty() && !decimalPart.all { it.isDigit() }) return null
        if (decimalPart.length > 2) return null

        val decimal = when (decimalPart.length) {
            0 -> 0L
            1 -> decimalPart.toLong() * 10
            2 -> decimalPart.toLong()
            else -> return null
        }

        return integer * 100 + decimal
    }
}
