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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreOverviewMetrics
import com.agguy.moni.core.util.formatAmount

/**
 * 账单页顶部月度概览卡片。
 *
 * 三行布局：
 * - 第一行：今日支出（expenseRed）+ 日均剩余（incomeGreen，大字号）
 * - 第二行（条件渲染）：总预算进度条，含理想进度标记
 * - 第三行：本月总支出 + 日均支出 + 月结余（主题色）
 */
@Composable
fun RecordOverviewCard(
    selectedYearMonth: String,
    overviewMetrics: CoreOverviewMetrics?,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val metrics = overviewMetrics ?: CoreOverviewMetrics()

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
            // 第一行：今日支出（expenseRed）+ 日均剩余（incomeGreen，大字号）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                OverviewStatItem(
                    label = stringResource(R.string.overview_today_expense),
                    amountCents = metrics.todayExpense,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.expenseRed,
                    isLarge = true
                )
                OverviewStatItem(
                    label = stringResource(R.string.overview_daily_remaining),
                    amountCents = metrics.dailyRemaining,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.incomeGreen,
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

            // 第三行：本月总支出（主题色）+ 日均支出 + 月结余（主题色）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                OverviewStatItem(
                    label = stringResource(R.string.overview_month_total),
                    amountCents = metrics.monthExpense,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                OverviewStatItem(
                    label = stringResource(R.string.overview_daily_avg),
                    amountCents = metrics.dailyAvg,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    alignCenter = true
                )
                OverviewStatItem(
                    label = stringResource(R.string.overview_month_balance),
                    amountCents = metrics.monthBalance,
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
                stringResource(R.string.budget_spent_format, "$currencySymbol${formatAmount(kotlin.math.abs(budget.remainingCents))}")
            } else {
                stringResource(R.string.budget_remaining_format, "$currencySymbol${formatAmount(budget.remainingCents)}")
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
                text = stringResource(R.string.budget_total_format, "$currencySymbol${formatAmount(budget.amountCents)}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IdealProgressBar(
            actualPercentage = actualPercentage,
            idealPercentage = idealPercentage,
            progressStatus = budget.progressStatus
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
    progressStatus: String?,
    modifier: Modifier = Modifier
) {
    // 进度条显示已花费比例：未花时空，花得越多越满
    val clampedActual = actualPercentage.coerceIn(0.0, 1.0).toFloat()
    val clampedIdeal = idealPercentage.coerceIn(0.0, 1.0).toFloat()

    // 颜色由 Rust 内核根据实际支出与理想时间进度的对比计算
    val progressColor = when (progressStatus) {
        "overrun" -> MaterialTheme.colorScheme.expenseRed
        "warning" -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        // 原生进度条：已花费比例
        LinearProgressIndicator(
            progress = { clampedActual },
            modifier = Modifier.fillMaxSize(),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp
        )

        // 理想进度标记：竖线（颜色用主题色系）
        val markerColor = MaterialTheme.colorScheme.onPrimaryContainer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackWidth = size.width
            val trackHeight = size.height
            val markerX = trackWidth * clampedIdeal
            val markerWidth = 2.dp.toPx().coerceAtLeast(1f)

            drawRect(
                color = markerColor,
                topLeft = Offset(
                    x = (markerX - markerWidth / 2f).coerceIn(0f, trackWidth - markerWidth),
                    y = 0f
                ),
                size = Size(width = markerWidth, height = trackHeight)
            )
        }
    }
}
