package com.agguy.moni.app.ui.record

import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.CoreRecordGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * RecordOverviewCard 中纯计算逻辑的单元测试。
 *
 * 覆盖范围：
 * - calculateOverviewMetrics：当前月、过去月、未来月、空月份、有/无总预算。
 * - parseYearMonth：合法格式、非法格式、边界值。
 * - calculateDayCounts：当前月、过去月、未来月的天数计算。
 */
class RecordOverviewCalculatorTest {

    // 固定测试日期：2024-03-15（3月有31天）
    private val fixedToday = LocalDate.of(2024, 3, 15)

    // 测试数据构建辅助
    private fun monthlySummary(
        yearMonth: String,
        income: Long,
        expense: Long
    ) = CoreMonthlySummary(
        yearMonth = yearMonth,
        incomeCents = income,
        expenseCents = expense,
        balanceCents = income - expense
    )

    private fun recordGroup(
        date: String,
        expense: Long = 0,
        income: Long = 0
    ) = CoreRecordGroup(
        date = date,
        incomeCents = income,
        expenseCents = expense,
        records = emptyList()
    )

    private fun totalBudget(
        amount: Long,
        spent: Long = 0
    ) = CoreBudget(
        id = 1,
        categoryId = null,
        categoryName = "总预算",
        amountCents = amount,
        periodType = "monthly",
        createdAt = 0,
        updatedAt = 0,
        spentCents = spent,
        remainingCents = amount - spent,
        percentage = if (amount > 0) spent.toDouble() / amount else 0.0,
        status = "safe",
        isSnapshot = false
    )

    // ==================== calculateOverviewMetrics ====================

    @Test
    fun `当前月份指标计算正确`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-03",
            recordGroups = listOf(recordGroup("2024-03-15", expense = 2000)),
            monthlySummaries = listOf(monthlySummary("2024-03", income = 20_000, expense = 15_000)),
            budgets = listOf(totalBudget(amount = 30_000, spent = 15_000)),
            today = fixedToday
        )

        assertEquals(15_000L, metrics.monthExpense)
        assertEquals(5_000L, metrics.monthBalance)
        assertEquals(2_000L, metrics.todayExpense)
        assertEquals(1_000L, metrics.dailyAvg) // 15000 / 15
        assertEquals(312L, metrics.dailyRemaining) // 5000 / 16 = 312
        assertEquals(15, metrics.elapsedDays)
        assertEquals(16, metrics.remainingDays)
        assertEquals(31, metrics.totalDays)
        assertEquals(30_000L, metrics.totalBudget?.amountCents)
    }

    @Test
    fun `过去月份今日支出为null且日均剩余为null`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-02",
            recordGroups = emptyList(),
            monthlySummaries = listOf(monthlySummary("2024-02", income = 10_000, expense = 8_000)),
            budgets = emptyList(),
            today = fixedToday
        )

        assertEquals(8_000L, metrics.monthExpense)
        assertEquals(2_000L, metrics.monthBalance)
        assertNull(metrics.todayExpense)
        assertEquals(275L, metrics.dailyAvg) // 8000 / 29（2024闰年2月有29天）
        assertNull(metrics.dailyRemaining) // 剩余天数为0
        assertEquals(29, metrics.elapsedDays)
        assertEquals(0, metrics.remainingDays)
        assertNull(metrics.totalBudget)
    }

    @Test
    fun `未来月份日均支出为null`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-04",
            recordGroups = emptyList(),
            monthlySummaries = listOf(monthlySummary("2024-04", income = 5_000, expense = 3_000)),
            budgets = emptyList(),
            today = fixedToday
        )

        assertEquals(3_000L, metrics.monthExpense)
        assertEquals(2_000L, metrics.monthBalance)
        assertNull(metrics.todayExpense)
        assertNull(metrics.dailyAvg) // 未来月不显示
        assertEquals(66L, metrics.dailyRemaining) // 2000 / 30 = 66
        assertEquals(0, metrics.elapsedDays)
        assertEquals(30, metrics.remainingDays)
    }

    @Test
    fun `空月份所有指标为0或null`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-03",
            recordGroups = emptyList(),
            monthlySummaries = emptyList(),
            budgets = emptyList(),
            today = fixedToday
        )

        assertEquals(0L, metrics.monthExpense)
        assertEquals(0L, metrics.monthBalance)
        assertEquals(0L, metrics.todayExpense) // 当前月但无记录，今日支出为0
        assertEquals(0L, metrics.dailyAvg) // 0 / 15 = 0
        assertEquals(0L, metrics.dailyRemaining) // 0 / 16 = 0
        assertEquals(15, metrics.elapsedDays)
        assertEquals(16, metrics.remainingDays)
        assertNull(metrics.totalBudget)
    }

    @Test
    fun `有总预算时totalBudget不为null`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-03",
            recordGroups = emptyList(),
            monthlySummaries = listOf(monthlySummary("2024-03", income = 0, expense = 0)),
            budgets = listOf(totalBudget(amount = 10_000)),
            today = fixedToday
        )

        assertEquals(10_000L, metrics.totalBudget?.amountCents)
        assertEquals(0L, metrics.totalBudget?.spentCents)
    }

    @Test
    fun `无总预算时totalBudget为null`() {
        val categoryBudget = CoreBudget(
            id = 2,
            categoryId = 1,
            categoryName = "餐饮",
            amountCents = 5_000,
            periodType = "monthly",
            createdAt = 0,
            updatedAt = 0,
            spentCents = 0,
            remainingCents = 5_000,
            percentage = 0.0,
            status = "safe",
            isSnapshot = false
        )

        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-03",
            recordGroups = emptyList(),
            monthlySummaries = emptyList(),
            budgets = listOf(categoryBudget),
            today = fixedToday
        )

        assertNull(metrics.totalBudget) // 只有分类预算，无总预算
    }

    @Test
    fun `月结余为负时日均剩余为负数`() {
        val metrics = calculateOverviewMetrics(
            selectedYearMonth = "2024-03",
            recordGroups = emptyList(),
            monthlySummaries = listOf(monthlySummary("2024-03", income = 5_000, expense = 10_000)),
            budgets = emptyList(),
            today = fixedToday
        )

        assertEquals(-5_000L, metrics.monthBalance)
        assertEquals(-312L, metrics.dailyRemaining) // -5000 / 16
    }

    // ==================== parseYearMonth ====================

    @Test
    fun `parseYearMonth 合法格式解析成功`() {
        assertEquals(YearMonth.of(2024, 3), parseYearMonth("2024-03"))
        assertEquals(YearMonth.of(2024, 12), parseYearMonth("2024-12"))
        assertEquals(YearMonth.of(2024, 1), parseYearMonth("2024-01"))
    }

    @Test
    fun `parseYearMonth 月份无前导零也能解析`() {
        assertEquals(YearMonth.of(2024, 3), parseYearMonth("2024-3"))
    }

    @Test
    fun `parseYearMonth 非法格式返回null`() {
        assertNull(parseYearMonth("invalid"))
        assertNull(parseYearMonth(""))
        assertNull(parseYearMonth("2024"))
        assertNull(parseYearMonth("2024-03-01"))
        assertNull(parseYearMonth("2024--03"))
    }

    @Test
    fun `parseYearMonth 无效月份返回null`() {
        assertNull(parseYearMonth("2024-13"))
        assertNull(parseYearMonth("2024-00"))
        assertNull(parseYearMonth("2024--1"))
    }

    // ==================== calculateDayCounts ====================

    @Test
    fun `calculateDayCounts 当前月计算正确`() {
        val yearMonth = YearMonth.of(2024, 3)
        val current = YearMonth.of(2024, 3)
        val today = LocalDate.of(2024, 3, 15)
        val totalDays = 31

        val (elapsed, remaining) = calculateDayCounts(yearMonth, current, today, totalDays)
        assertEquals(15, elapsed)
        assertEquals(16, remaining)
    }

    @Test
    fun `calculateDayCounts 当前月月初边界`() {
        val yearMonth = YearMonth.of(2024, 3)
        val current = YearMonth.of(2024, 3)
        val today = LocalDate.of(2024, 3, 1)
        val totalDays = 31

        val (elapsed, remaining) = calculateDayCounts(yearMonth, current, today, totalDays)
        assertEquals(1, elapsed) // 保底为1
        assertEquals(30, remaining)
    }

    @Test
    fun `calculateDayCounts 当前月月末边界`() {
        val yearMonth = YearMonth.of(2024, 3)
        val current = YearMonth.of(2024, 3)
        val today = LocalDate.of(2024, 3, 31)
        val totalDays = 31

        val (elapsed, remaining) = calculateDayCounts(yearMonth, current, today, totalDays)
        assertEquals(31, elapsed)
        assertEquals(0, remaining)
    }

    @Test
    fun `calculateDayCounts 过去月全部已过`() {
        val yearMonth = YearMonth.of(2024, 2)
        val current = YearMonth.of(2024, 3)
        val today = LocalDate.of(2024, 3, 15)
        val totalDays = 29 // 闰年

        val (elapsed, remaining) = calculateDayCounts(yearMonth, current, today, totalDays)
        assertEquals(29, elapsed)
        assertEquals(0, remaining)
    }

    @Test
    fun `calculateDayCounts 未来月全部未过`() {
        val yearMonth = YearMonth.of(2024, 4)
        val current = YearMonth.of(2024, 3)
        val today = LocalDate.of(2024, 3, 15)
        val totalDays = 30

        val (elapsed, remaining) = calculateDayCounts(yearMonth, current, today, totalDays)
        assertEquals(0, elapsed)
        assertEquals(30, remaining)
    }
}
