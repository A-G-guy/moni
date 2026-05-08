package com.agguy.moni.app.ui.record

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconForCategory
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.util.formatAmount
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 解析账单条目应显示的分类名称。
 *
 * @param showFullCategory 为 true 时，若记录属于二级分类，则拼接为 "父分类 › 子分类" 格式。
 */
fun resolveCategoryDisplay(
    record: CoreRecord,
    categories: List<CoreCategory>,
    showFullCategory: Boolean
): String {
    if (!showFullCategory) return record.categoryName

    val category = categories.find { it.id == record.categoryId } ?: return record.categoryName
    if (category.parentId == null) return record.categoryName

    val parent = categories.find { it.id == category.parentId }
    return if (parent != null) "${parent.name} › ${category.name}" else record.categoryName
}

@Composable
fun RecordListItem(
    record: CoreRecord,
    currencySymbol: String,
    categories: List<CoreCategory>,
    showIcon: Boolean,
    showFullCategory: Boolean,
    notePriority: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = record.recordType == "expense"
    val amountColor = if (isExpense) MaterialTheme.colorScheme.expenseRed else MaterialTheme.colorScheme.incomeGreen
    val sign = if (isExpense) "-" else "+"
    val amountText = "${sign}${currencySymbol}${formatAmount(record.amountCents)}"

    val categoryDisplay = resolveCategoryDisplay(record, categories, showFullCategory)

    val primaryText: String
    val secondaryText: String?

    if (notePriority && record.note.isNotBlank()) {
        primaryText = record.note
        secondaryText = categoryDisplay
    } else {
        primaryText = categoryDisplay
        secondaryText = record.note.takeIf { it.isNotBlank() }
    }

    MoniCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                CategoryIndicator(
                    isExpense = isExpense,
                    iconName = record.categoryName
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                secondaryText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 金额和时间
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleLarge,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(record.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CategoryIndicator(isExpense: Boolean, iconName: String) {
    val color = if (isExpense) MaterialTheme.colorScheme.expenseRed else MaterialTheme.colorScheme.incomeGreen

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        SymbolIcon(
            name = iconForCategory(iconName),
            contentDescription = null,
            size = 22.dp,
            tint = color
        )
    }
}

private fun formatTime(timestamp: Long): String = try {
    LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) {
    Log.w("Moni", "时间格式化失败: timestamp=$timestamp, ${e.message}")
    ""
}
