package com.agguy.moni.app.ui.budget

import java.time.LocalDate
import java.time.YearMonth

/**
 * 预算日均统计数据。
 *
 * @property dailyBudgetCents 日均预算（分）
 * @property dailyRemainingCents 剩余日均（分）
 * @property remainingCents 剩余预算（分）
 * @property elapsedDays 已过天数
 * @property remainingDays 剩余天数
 * @property totalDays 当月总天数
 */
data class BudgetDailyStats(
    val dailyBudgetCents: Long,
    val dailyRemainingCents: Long,
    val remainingCents: Long,
    val elapsedDays: Int,
    val remainingDays: Int,
    val totalDays: Int
)

/**
 * 根据预算金额、剩余金额和指定年月计算日均数据。
 *
 * - 过去月份：剩余天数 = 0，剩余日均 = 0
 * - 未来月份：已过天数 = 0，剩余日均按剩余天数计算
 * - 当前月份：按实际日期计算已过/剩余天数
 * - 当月最后一天（剩余天数 = 0）：剩余日均 = 0
 *
 * @param amountCents 预算金额（分）
 * @param remainingCents 剩余金额（分）
 * @param yearMonth 格式 "YYYY-MM"
 * @return 计算结果，月份无效返回 null
 */
fun calculateBudgetDailyStats(
    amountCents: Long,
    remainingCents: Long,
    yearMonth: String
): BudgetDailyStats? {
    val ym = parseYearMonth(yearMonth) ?: return null
    val today = LocalDate.now()
    val totalDays = ym.lengthOfMonth()

    val todayYearMonth = YearMonth.from(today)
    val (elapsedDays, remainingDays) = when {
        ym.isBefore(todayYearMonth) -> totalDays to 0
        ym.isAfter(todayYearMonth) -> 0 to totalDays
        else -> today.dayOfMonth to (totalDays - today.dayOfMonth).coerceAtLeast(0)
    }

    val dailyBudget = if (totalDays > 0) amountCents / totalDays else 0
    val dailyRemaining = if (remainingDays > 0) remainingCents / remainingDays else 0

    return BudgetDailyStats(
        dailyBudgetCents = dailyBudget,
        dailyRemainingCents = dailyRemaining,
        remainingCents = remainingCents,
        elapsedDays = elapsedDays,
        remainingDays = remainingDays,
        totalDays = totalDays
    )
}

private fun parseYearMonth(yearMonth: String): YearMonth? {
    return try {
        val parts = yearMonth.split("-")
        if (parts.size == 2) {
            YearMonth.of(parts[0].toInt(), parts[1].toInt())
        } else null
    } catch (_: Exception) {
        null
    }
}
