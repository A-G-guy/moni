package com.agguy.moni.core.util

/**
 * 将金额（单位：分）格式化为显示字符串。
 *
 * @param cents 金额，单位为分
 * @return 格式化后的字符串，如 "123" 或 "123.45"
 */
fun formatAmount(cents: Long): String {
    // Long.MIN_VALUE 的绝对值无法用 Long 表示，使用 BigDecimal 处理
    if (cents == Long.MIN_VALUE) {
        return java.math.BigDecimal.valueOf(cents)
            .movePointLeft(2)
            .toPlainString()
    }
    val absCents = kotlin.math.abs(cents)
    val yuan = absCents / 100
    val fen = absCents % 100
    if (fen == 0L) return "${if (cents < 0) "-$yuan" else "$yuan"}"
    val sign = if (cents < 0) "-" else ""
    return "$sign$yuan.${fen.toString().padStart(2, '0')}"
}
