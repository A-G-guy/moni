package com.agguy.moni.app.ui.record.editor

import kotlin.math.round

/**
 * 金额表达式计算器。
 *
 * 支持加减乘除四则运算，遵循标准运算符优先级（先乘除后加减）。
 * 每个操作数最多两位小数，中间计算使用 Double 保持精度，
 * 最终结果四舍五入后转为分（cents）。
 */
object ExpressionEvaluator {

    /** 运算符及其优先级（数值越大优先级越高） */
    private val precedence = mapOf("+" to 1, "-" to 1, "×" to 2, "÷" to 2)

    /** 所有支持的运算符集合 */
    private val operators = setOf("+", "-", "×", "÷")

    /**
     * 解析表达式并返回计算结果（分）。
     *
     * @param expression 原始表达式，如 "15.5+20"、"100-25×2"
     * @return 计算结果（分），表达式无效时返回 null
     */
    fun evaluateToCents(expression: String): Long? {
        if (expression.isBlank()) return null
        val trimmed = expression.trim()
        if (trimmed.last() in setOf('+', '-', '×', '÷')) return null

        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) return null
        if (tokens.size == 1) {
            return parseOperandToCents(tokens[0])
        }

        val postfix = toPostfix(tokens)
        val result = evaluatePostfix(postfix) ?: return null

        return if (result >= 0) round(result * 100).toLong() else null
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
        if (trimmed.last() in setOf('+', '-', '×', '÷')) return true
        return trimmed.any { it in setOf('+', '-', '×', '÷') }
    }

    /**
     * 格式化表达式用于显示，在运算符两侧添加空格。
     */
    fun formatForDisplay(expression: String): String {
        return expression
            .replace("+", " + ")
            .replace("-", " - ")
            .replace("×", " × ")
            .replace("÷", " ÷ ")
    }

    /**
     * 将表达式字符串拆分为操作数和运算符 token 列表。
     */
    private fun tokenize(expression: String): List<String> {
        val tokens = mutableListOf<String>()
        val currentNumber = StringBuilder()

        for (char in expression) {
            when (char) {
                '+', '-', '×', '÷' -> {
                    if (currentNumber.isNotEmpty()) {
                        tokens.add(currentNumber.toString())
                        currentNumber.clear()
                    }
                    tokens.add(char.toString())
                }

                else -> currentNumber.append(char)
            }
        }
        if (currentNumber.isNotEmpty()) {
            tokens.add(currentNumber.toString())
        }
        return tokens
    }

    /**
     * 中缀表达式转后缀表达式（Shunting Yard 算法）。
     */
    private fun toPostfix(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val opStack = mutableListOf<String>()

        for (token in tokens) {
            if (token in operators) {
                while (opStack.isNotEmpty() &&
                    opStack.last() in operators &&
                    precedence[opStack.last()]!! >= precedence[token]!!
                ) {
                    output.add(opStack.removeAt(opStack.size - 1))
                }
                opStack.add(token)
            } else {
                output.add(token)
            }
        }
        while (opStack.isNotEmpty()) {
            output.add(opStack.removeAt(opStack.size - 1))
        }
        return output
    }

    /**
     * 后缀表达式求值（中间结果以元为单位，使用 Double 保持精度）。
     */
    private fun evaluatePostfix(postfix: List<String>): Double? {
        val stack = mutableListOf<Double>()

        for (token in postfix) {
            if (token in operators) {
                if (stack.size < 2) return null
                val b = stack.removeAt(stack.size - 1)
                val a = stack.removeAt(stack.size - 1)
                val result = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "×" -> a * b
                    "÷" -> if (b == 0.0) return null else a / b
                    else -> return null
                }
                stack.add(result)
            } else {
                val cents = parseOperandToCents(token) ?: return null
                stack.add(cents / 100.0)
            }
        }
        return if (stack.size == 1) stack[0] else null
    }

    /**
     * 解析单个操作数为分。
     *
     * 有效格式：整数（"15"）、一位小数（"15.5"）、两位小数（"15.50"）。
     * 不允许：空字符串、多个小数点、超过两位小数、负数（表达式中负号由运算符处理）、
     * 小数点后无数字（"15."）。
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
        // 有小数点但小数部分为空（如 "15."）视为不完整
        if (parts.size > 1 && decimalPart.isEmpty()) return null
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
