package com.agguy.moni.app.ui.stats

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.CoreCategoryBreakdown
import com.agguy.moni.core.util.formatAmount

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

    val totalExpense = breakdowns.sumOf { it.amountCents }
    val totalText = "${currencySymbol}${formatAmount(totalExpense)}"

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
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChartCanvas(breakdowns = breakdowns)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "本月支出",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 图例列表
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
    val context = LocalContext.current
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(breakdowns) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        if (scale == 0f) {
            animationProgress.snapTo(1f)
        } else {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Canvas(modifier = modifier.size(140.dp)) {
        val total = breakdowns.sumOf { it.percentage }
        if (total <= 0) return@Canvas

        val canvasSize = size.minDimension
        val center = Offset(size.width / 2, size.height / 2)
        // 预留边距防止 stroke 超出 Canvas 边界
        val maxRadius = canvasSize / 2 - 8f
        val strokeWidth = maxRadius * 0.30f
        val radius = maxRadius - strokeWidth / 2
        val gapAngle = 2f
        var startAngle = -90f

        breakdowns.forEach { item ->
            val rawSweep = (item.percentage / total * 360f).toFloat() * animationProgress.value
            val sweepAngle = (rawSweep - gapAngle).coerceAtLeast(0f)
            val color = parseColor(item.colorHex)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            startAngle += rawSweep
        }
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
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${percentage.toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(64.dp)
        )
    }
}
