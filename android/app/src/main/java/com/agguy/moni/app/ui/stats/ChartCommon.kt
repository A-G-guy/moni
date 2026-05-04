package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun EmptyChartPlaceholder(message: String, modifier: Modifier = Modifier) {
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

@Composable
fun ChartLegend(items: List<LegendItem>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(item.color)
                )
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

data class LegendItem(val label: String, val color: Color)

fun formatShortAmount(cents: Long, symbol: String): String {
    val yuan = cents / 100
    return when {
        yuan >= 10000 -> "${symbol}${yuan / 10000}w"
        yuan >= 1000 -> "${symbol}${yuan / 1000}k"
        else -> "${symbol}$yuan"
    }
}

/** 饼图色板：按索引循环取色，供分类占比图使用。 */
val PieColors: List<Color> = listOf(
    Color(0xFFB33A3A),
    Color(0xFFD4684A),
    Color(0xFFE89A5A),
    Color(0xFFF5C26A),
    Color(0xFF7BA85A),
    Color(0xFF4A8C7A),
    Color(0xFF3A6A9A),
    Color(0xFF5A4A8A),
    Color(0xFF8A4A6A),
    Color(0xFF9A6A4A),
    Color(0xFF6A8A5A),
    Color(0xFF4A7A8A),
)

fun pieColorAt(index: Int): Color = PieColors[index % PieColors.size]
