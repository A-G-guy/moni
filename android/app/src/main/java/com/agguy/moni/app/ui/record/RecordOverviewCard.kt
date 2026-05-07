@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.CoreRecordGroup
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.YearMonth

/**
 * 账单页顶部月度概览卡片。
 *
 * 三行布局：
 * - 第一行：本月总支出 + 月结余
 * - 第二行（条件渲染）：总预算进度条（含理想进度标记）
 * - 第三行：今日支出 + 日均支出 + 日均剩余
 */
@Composable
fun RecordOverviewCard(
    selectedYearMonth: String,
    recordGroups: List<CoreRecordGroup>,
    monthlySummaries: List<CoreMonthlySummary>,
    budgets: List<CoreBudget>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val metrics = remember(selectedYearMonth, recordGroups, monthlySummaries, budgets) {
        calculateOverviewMetrics(selectedYearMonth, recordGroups, monthlySummaries, budgets)
    }

    MoniCard(
        modifier = modifier.fillMaxWidth(),
        variant = MoniCardVariant.Tonal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 第一行：本月总支出 + 月结余
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                OverviewStatItem(
                    label = "本月总支出",
                    amountCents = metrics.monthExpense,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.expenseRed,
                    isLarge = true
                )
                OverviewStatItem(
                    label = "月结余",
                    amountCents = metrics.monthBalance,
                    currencySymbol = currencySymbol,
                    color = if (metrics.monthBalance >= 0) {
                        MaterialTheme.colorScheme.incomeGreen
                    } else {
                        MaterialTheme.colorScheme.expenseRed
                    },
                    isLarge = true,
                    alignEnd = true
                )
            }

            // 第二行：预算进度（仅在设置了总预算时显示）
            AnimatedVisibility(visible = metrics.totalBudget != null) {
                metrics.totalBudget?.let { budget ->
                    BudgetProgressSection(
                        budget = budget,
                        monthExpense = metrics.monthExpense,
                        elapsedDays = metrics.elapsedDays,
                        totalDays = metrics.totalDays,
                        currencySymbol = currencySymbol
                    )
                }
            }

            // 第三行：今日支出 + 日均支出 + 日均剩余
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                OverviewStatItem(
                    label = "今日支出",
                    amountCents = metrics.todayExpense,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OverviewStatItem(
                    label = "日均支出",
                    amountCents = metrics.dailyAvg,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
                OverviewStatItem(
                    label = "日均剩余",
                    amountCents = metrics.dailyRemaining,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }
        }
    }
}

/**
 * 单个指标项：标签 + 金额。
 *
 * @param amountCents null 时显示 "-"
 * @param alignEnd 是否右对齐（用于多列布局中的右侧列）
 * @param isLarge 是否使用大字号（第一行）
 */
@Composable
private fun OverviewStatItem(
    label: String,
    amountCents: Long?,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    isLarge: Boolean = false
) {
    val amountText = if (amountCents != null) {
        "$currencySymbol${formatAmount(amountCents)}"
    } else {
        "-"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(
            text = label,
            style = if (isLarge) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelSmall
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amountText,
            style = if (isLarge) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * 预算进度区域：文本标签 + 带理想进度标记的进度条。
 */
@Composable
private fun BudgetProgressSection(
    budget: CoreBudget,
    monthExpense: Long,
    elapsedDays: Int,
    totalDays: Int,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val actualPercentage = if (budget.amountCents > 0) {
        monthExpense.toDouble() / budget.amountCents.toDouble()
    } else {
        0.0
    }
    val idealPercentage = if (totalDays > 0) {
        elapsedDays.toDouble() / totalDays.toDouble()
    } else {
        0.0
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val remainingText = if (budget.remainingCents < 0) {
                "超支 $currencySymbol${formatAmount(kotlin.math.abs(budget.remainingCents))}"
            } else {
                "剩余 $currencySymbol${formatAmount(budget.remainingCents)}"
            }
            val remainingColor = when (budget.status) {
                "overrun" -> MaterialTheme.colorScheme.expenseRed
                "critical" -> Color(0xFFFFA726)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = remainingText,
                style = MaterialTheme.typography.bodySmall,
                color = remainingColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "共 $currencySymbol${formatAmount(budget.amountCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OverviewProgressBar(
            actualPercentage = actualPercentage,
            idealPercentage = idealPercentage
        )
    }
}

/**
 * 带理想进度标记的进度条。
 *
 * - 底层灰色轨道
 * - 填充层：实际支出进度（颜色按状态变化）
 * - 游标：竖线标记理想进度位置
 */
@Composable
private fun OverviewProgressBar(
    actualPercentage: Double,
    idealPercentage: Double,
    modifier: Modifier = Modifier
) {
    val clampedActual = actualPercentage.coerceIn(0.0, 1.0)
    val clampedIdeal = idealPercentage.coerceIn(0.0, 1.0)

    val progressColor = when {
        actualPercentage > 1.0 -> MaterialTheme.colorScheme.expenseRed
        actualPercentage >= 0.8 -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val width = maxWidth

        // 轨道
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )

        // 实际进度填充
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(width * clampedActual.toFloat())
                .clip(RoundedCornerShape(4.dp))
                .background(progressColor)
        )

        // 理想进度标记（竖线）
        val markerOffset = calculateMarkerOffset(width, clampedIdeal)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .offset(x = markerOffset)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    RoundedCornerShape(1.dp)
                )
        )
    }
}

/**
 * 计算标记偏移量，确保不超出轨道边界。
 */
private fun calculateMarkerOffset(totalWidth: Dp, idealFraction: Double): Dp {
    val markerWidth = 2.dp
    val rawOffset = totalWidth * idealFraction.toFloat() - markerWidth / 2
    return rawOffset.coerceIn(0.dp, (totalWidth - markerWidth).coerceAtLeast(0.dp))
}

/**
 * 概览指标数据。
 */
private data class OverviewMetrics(
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    val monthBalance: Long = 0,
    val todayExpense: Long? = null,
    val dailyAvg: Long? = null,
    val dailyRemaining: Long? = null,
    val totalBudget: CoreBudget? = null,
    val elapsedDays: Int = 1,
    val totalDays: Int = 30,
    val remainingDays: Int = 0
)

/**
 * 计算概览所需的全部指标。
 */
private fun calculateOverviewMetrics(
    selectedYearMonth: String,
    recordGroups: List<CoreRecordGroup>,
    monthlySummaries: List<CoreMonthlySummary>,
    budgets: List<CoreBudget>
): OverviewMetrics {
    val yearMonth = parseYearMonth(selectedYearMonth)
        ?: return OverviewMetrics()

    val totalDays = yearMonth.lengthOfMonth()
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.from(today)

    val (elapsedDays, remainingDays) = calculateDayCounts(yearMonth, currentYearMonth, today, totalDays)

    // 月度汇总
    val summary = monthlySummaries.find { it.yearMonth == selectedYearMonth }
    val monthExpense = summary?.expenseCents ?: 0
    val monthIncome = summary?.incomeCents ?: 0
    val monthBalance = summary?.balanceCents ?: 0

    // 今日支出（仅当前月有效）
    val todayExpense = if (yearMonth == currentYearMonth) {
        recordGroups.find { it.date == today.toString() }?.expenseCents ?: 0
    } else {
        null
    }

    // 日均支出（未来月不显示）
    val dailyAvg = if (yearMonth.isAfter(currentYearMonth)) {
        null
    } else {
        monthExpense / elapsedDays.coerceAtLeast(1)
    }

    // 总预算
    val totalBudget = budgets.find { it.categoryId == null }

    // 日均剩余（有预算且还有剩余天数时计算）
    val dailyRemaining = if (totalBudget != null && remainingDays > 0) {
        (totalBudget.amountCents - monthExpense) / remainingDays
    } else {
        null
    }

    return OverviewMetrics(
        monthExpense = monthExpense,
        monthIncome = monthIncome,
        monthBalance = monthBalance,
        todayExpense = todayExpense,
        dailyAvg = dailyAvg,
        dailyRemaining = dailyRemaining,
        totalBudget = totalBudget,
        elapsedDays = elapsedDays,
        totalDays = totalDays,
        remainingDays = remainingDays
    )
}

/**
 * 解析 "YYYY-MM" 字符串为 YearMonth。
 */
private fun parseYearMonth(yearMonthStr: String): YearMonth? {
    val parts = yearMonthStr.split("-")
    if (parts.size != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    return try {
        YearMonth.of(year, month)
    } catch (_: Exception) {
        null
    }
}

/**
 * 计算已过天数、剩余天数。
 */
private fun calculateDayCounts(
    yearMonth: YearMonth,
    currentYearMonth: YearMonth,
    today: LocalDate,
    totalDays: Int
): Pair<Int, Int> = when {
    yearMonth == currentYearMonth -> {
        val elapsed = today.dayOfMonth.coerceAtLeast(1)
        val remaining = (totalDays - elapsed).coerceAtLeast(0)
        elapsed to remaining
    }
    yearMonth.isBefore(currentYearMonth) -> {
        totalDays to 0
    }
    else -> {
        0 to totalDays
    }
}
