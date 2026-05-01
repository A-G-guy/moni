package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agguy.moni.app.theme.ExpenseRed
import com.agguy.moni.app.theme.IncomeGreen
import com.agguy.moni.core.CoreMonthlySummary

/**
 * 月度收支柱状图。
 *
 * 展示近 N 个月的收入和支出对比。
 *
 * @param summaries 月度汇总数据列表
 * @param currencySymbol 货币符号
 * @param modifier 修饰符
 */
@Composable
fun MonthlyBarChart(
    summaries: List<CoreMonthlySummary>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) {
        EmptyChartPlaceholder("暂无月度数据")
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Column(modifier = modifier) {
        Text(
            text = "月度趋势",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val paddingStart = 32f
            val paddingEnd = 8f
            val paddingTop = 24f
            val paddingBottom = 28f
            val chartWidth = size.width - paddingStart - paddingEnd
            val chartHeight = size.height - paddingTop - paddingBottom

            val maxAmount = summaries.maxOfOrNull {
                maxOf(it.incomeCents, it.expenseCents)
            }?.coerceAtLeast(1) ?: 1L

            val barGroupWidth = chartWidth / summaries.size
            val barWidth = barGroupWidth * 0.35f
            val gap = barGroupWidth * 0.15f

            // 绘制零线
            val zeroY = paddingTop + chartHeight
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(paddingStart, zeroY),
                end = Offset(size.width - paddingEnd, zeroY),
                strokeWidth = 1f
            )

            // 绘制柱状图
            summaries.forEachIndexed { index, summary ->
                val groupX = paddingStart + index * barGroupWidth
                val centerX = groupX + barGroupWidth / 2

                // 收入柱
                val incomeHeight = (summary.incomeCents.toFloat() / maxAmount) * chartHeight
                val incomeX = centerX - barWidth - gap / 2
                drawRect(
                    color = IncomeGreen,
                    topLeft = Offset(incomeX, zeroY - incomeHeight),
                    size = Size(barWidth, incomeHeight)
                )

                // 支出柱
                val expenseHeight = (summary.expenseCents.toFloat() / maxAmount) * chartHeight
                val expenseX = centerX + gap / 2
                drawRect(
                    color = ExpenseRed,
                    topLeft = Offset(expenseX, zeroY - expenseHeight),
                    size = Size(barWidth, expenseHeight)
                )

                // 月份标签
                val monthLabel = summary.yearMonth.substring(5) // "2026-04" -> "04"
                val labelResult = textMeasurer.measure(monthLabel, labelStyle)
                drawText(
                    textMeasurer = textMeasurer,
                    text = monthLabel,
                    style = labelStyle,
                    topLeft = Offset(
                        centerX - labelResult.size.width / 2,
                        zeroY + 6f
                    )
                )

                // 收入数值标签（仅当柱子够高时显示）
                if (incomeHeight > 20f && summary.incomeCents > 0) {
                    val incomeText = formatShortAmount(summary.incomeCents, currencySymbol)
                    val valueResult = textMeasurer.measure(incomeText, valueStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = incomeText,
                        style = valueStyle,
                        topLeft = Offset(
                            incomeX + barWidth / 2 - valueResult.size.width / 2,
                            zeroY - incomeHeight - valueResult.size.height - 2f
                        )
                    )
                }

                // 支出数值标签
                if (expenseHeight > 20f && summary.expenseCents > 0) {
                    val expenseText = formatShortAmount(summary.expenseCents, currencySymbol)
                    val valueResult = textMeasurer.measure(expenseText, valueStyle)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = expenseText,
                        style = valueStyle,
                        topLeft = Offset(
                            expenseX + barWidth / 2 - valueResult.size.width / 2,
                            zeroY - expenseHeight - valueResult.size.height - 2f
                        )
                    )
                }
            }

            // 绘制 Y 轴最大值标签
            val maxLabel = formatShortAmount(maxAmount, currencySymbol)
            val maxResult = textMeasurer.measure(maxLabel, valueStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = maxLabel,
                style = valueStyle,
                topLeft = Offset(paddingStart - maxResult.size.width - 4f, paddingTop - maxResult.size.height / 2)
            )
        }

        // 图例
        ChartLegend(
            items = listOf(
                LegendItem("收入", IncomeGreen),
                LegendItem("支出", ExpenseRed)
            )
        )
    }
}

/**
 * 饼图旁或底部的图例。
 */
@Composable
private fun ChartLegend(
    items: List<LegendItem>,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        items.forEach { item ->
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawRect(color = item.color, size = this.size)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class LegendItem(val label: String, val color: Color)

private fun formatShortAmount(cents: Long, symbol: String): String {
    val yuan = cents / 100
    return when {
        yuan >= 10000 -> "${symbol}${yuan / 10000}w"
        yuan >= 1000 -> "${symbol}${yuan / 1000}k"
        else -> "${symbol}${yuan}"
    }
}
