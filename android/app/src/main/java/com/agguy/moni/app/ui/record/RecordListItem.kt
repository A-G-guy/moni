package com.agguy.moni.app.ui.record

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconForCategory
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.util.formatAmount
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.util.Log
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordListItem(
    record: CoreRecord,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val isExpense = record.recordType == "expense"
    val amountColor = if (isExpense) MaterialTheme.colorScheme.expenseRed else MaterialTheme.colorScheme.incomeGreen
    val sign = if (isExpense) "-" else "+"
    val amountText = "${sign}${currencySymbol}${formatAmount(record.amountCents)}"

    MoniCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标指示器（浅色圆角方框 + 分类图标）
            CategoryIndicator(
                colorHex = record.categoryColor,
                iconName = record.categoryName
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 分类名称和备注
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (record.note.isNotBlank()) {
                    Text(
                        text = record.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 金额和时间
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(record.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CategoryIndicator(colorHex: String, iconName: String) {
    val color = try {
        Color(colorHex.toColorInt())
    } catch (e: Exception) {
        Log.w("Moni", "颜色解析失败: $colorHex, ${e.message}")
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        MoniIcon(
            icon = iconForCategory(iconName),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = color
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        Log.w("Moni", "时间格式化失败: timestamp=$timestamp, ${e.message}")
        ""
    }
}
