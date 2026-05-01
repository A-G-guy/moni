package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.CoreCategoryBreakdown
import com.agguy.moni.core.util.formatAmount
import android.util.Log

/**
 * 分类支出饼图。
 *
 * 展示本月各分类支出的占比分布。
 *
 * @param breakdowns 分类支出占比数据
 * @param currencySymbol 货币符号
 * @param modifier 修饰符
 */
@Composable
fun CategoryPieChart(
    breakdowns: List<CoreCategoryBreakdown>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (breakdowns.isEmpty()) {
        EmptyChartPlaceholder("暂无支出数据")
        return
    }

    Column(modifier = modifier) {
        Text(
            text = "支出构成",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 饼图
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChartCanvas(breakdowns = breakdowns)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 图例列表
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                breakdowns.take(6).forEach { item ->
                    PieLegendItem(
                        color = parseColor(item.colorHex),
                        label = item.categoryName,
                        percentage = item.percentage,
                        amountText = "${currencySymbol}${formatAmount(item.amountCents)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun PieChartCanvas(
    breakdowns: List<CoreCategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(140.dp)) {
        val total = breakdowns.sumOf { it.percentage }
        if (total <= 0) return@Canvas

        val canvasSize = size.minDimension
        val radius = canvasSize / 2 - 4f
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f // 从顶部开始

        breakdowns.forEach { item ->
            val sweepAngle = (item.percentage / total * 360f).toFloat()
            val color = parseColor(item.colorHex)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // 中心白色圆形（甜甜圈效果）
        drawCircle(
            color = Color.White,
            radius = radius * 0.5f,
            center = center
        )
    }
}

@Composable
private fun PieLegendItem(
    color: Color,
    label: String,
    percentage: Double,
    amountText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyChartPlaceholder(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Log.w("Moni", "饼图颜色解析失败: $colorHex, ${e.message}")
        Color.Gray
    }
}

