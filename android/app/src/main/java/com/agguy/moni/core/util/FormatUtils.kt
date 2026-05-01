package com.agguy.moni.core.util

/**
 * 将金额（单位：分）格式化为显示字符串。
 *
 * @param cents 金额，单位为分
 * @return 格式化后的字符串，如 "123" 或 "123.45"
 */
fun formatAmount(cents: Long): String {
    val yuan = cents / 100
    val fen = kotlin.math.abs(cents % 100)
    if (fen == 0L) return "$yuan"
    val sign = if (cents < 0) "-" else ""
    return "$sign${kotlin.math.abs(yuan)}.${fen.toString().padStart(2, '0')}"
}
