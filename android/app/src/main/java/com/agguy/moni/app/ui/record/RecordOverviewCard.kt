@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
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
 * - 第一行：本月总支出（红色）+ 月结余（正绿负红）
 * - 第二行（条件渲染）：总预算进度条，含理想进度标记
 * - 第三行：今日支出 + 日均支出 + 日均剩余（主题色）
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
        calculateOverviewMetrics(selectedYearMonth, recordGroups, monthlySummaries, budgets, LocalDate.now())
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
            // 第一行：本月总支出（红色）+ 月结余（正绿负红）
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

            // 第三行：今日支出 + 日均支出 + 日均剩余（白色）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OverviewStatItem(
                    label = "今日支出",
                    amountCents = metrics.todayExpense,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                OverviewStatItem(
                    label = "日均支出",
                    amountCents = metrics.dailyAvg,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    alignCenter = true
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
 * @param alignEnd 是否右对齐
 * @param alignCenter 是否居中对齐（优先级低于 alignEnd）
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
    alignCenter: Boolean = false,
    isLarge: Boolean = false
) {
    val amountText = if (amountCents != null) {
        "$currencySymbol${formatAmount(amountCents)}"
    } else {
        "-"
    }

    val horizontalAlignment = when {
        alignEnd -> Alignment.End
        alignCenter -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
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
 * 预算进度区域：文本标签 + 带理想进度标记的原生进度条。
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

        IdealProgressBar(
            actualPercentage = actualPercentage,
            idealPercentage = idealPercentage
        )
    }
}

/**
 * 使用 Material 3 LinearProgressIndicator 作为基础，叠加理想进度标记。
 *
 * 底层使用官方 LinearProgressIndicator 渲染实际进度；
 * 通过 Canvas 在指示器轨道上方绘制理想进度竖线标记。
 */
@Composable
private fun IdealProgressBar(
    actualPercentage: Double,
    idealPercentage: Double,
    modifier: Modifier = Modifier
) {
    val clampedActual = actualPercentage.coerceIn(0.0, 1.0).toFloat()
    val clampedIdeal = idealPercentage.coerceIn(0.0, 1.0).toFloat()

    val progressColor = when {
        actualPercentage > 1.0 -> MaterialTheme.colorScheme.expenseRed
        actualPercentage >= 0.8 -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        // 原生进度条：实际支出进度
        LinearProgressIndicator(
            progress = { clampedActual },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )

        // 理想进度标记：竖线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = size.width
            val trackHeight = size.height
            val markerX = trackWidth * clampedIdeal
            val markerWidth = 2.dp.toPx().coerceAtLeast(1f)

            // 竖线
            drawRect(
                color = Color.White,
                topLeft = Offset(
                    x = (markerX - markerWidth / 2f).coerceIn(0f, trackWidth - markerWidth),
                    y = 0f
                ),
                size = Size(width = markerWidth, height = trackHeight)
            )
        }
    }
}

/**
 * 概览指标数据。
 */
internal data class OverviewMetrics(
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
internal fun calculateOverviewMetrics(
    selectedYearMonth: String,
    recordGroups: List<CoreRecordGroup>,
    monthlySummaries: List<CoreMonthlySummary>,
    budgets: List<CoreBudget>,
    today: LocalDate
): OverviewMetrics {
    val yearMonth = parseYearMonth(selectedYearMonth)
        ?: return OverviewMetrics()

    val totalDays = yearMonth.lengthOfMonth()
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

    // 日均剩余：有总预算时为"剩余总预算 / 剩余天数"，否则为"月结余 / 剩余天数"
    val dailyRemaining = if (remainingDays > 0) {
        if (totalBudget != null) {
            (totalBudget.amountCents - monthExpense) / remainingDays
        } else {
            monthBalance / remainingDays
        }
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
internal fun parseYearMonth(yearMonthStr: String): YearMonth? {
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
internal fun calculateDayCounts(
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
