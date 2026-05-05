package com.agguy.moni.app.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory

/**
 * 已归档分类的可折叠区块。
 *
 * - 默认收起，点击标题行展开；
 * - 展示已归档分类列表，每项带「恢复」操作；
 * - 所有分类均可恢复（预设分类概念已弱化）。
 */
@Composable
fun ArchivedSection(
    categories: List<CoreCategory>,
    onUnarchiveRequest: (CoreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        ArchivedSectionHeader(
            count = categories.size,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (categories.isEmpty()) {
                EmptyArchivedHint()
            } else {
                ArchivedCategoryList(
                    categories = categories,
                    onUnarchiveRequest = onUnarchiveRequest
                )
            }
        }
    }
}

@Composable
private fun ArchivedSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "已归档",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 数量徽标
        Box(
            modifier = Modifier
                .height(20.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        MoniIcon(
            icon = if (expanded) MoniIcons.ExpandLess else MoniIcons.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArchivedCategoryList(
    categories: List<CoreCategory>,
    onUnarchiveRequest: (CoreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { "archived_${it.id}" }) { category ->
            ArchivedCategoryItem(
                category = category,
                onUnarchiveClick = { onUnarchiveRequest(category) }
            )
        }
    }
}

@Composable
private fun ArchivedCategoryItem(category: CoreCategory, onUnarchiveClick: () -> Unit, modifier: Modifier = Modifier) {
    val categoryColor = if (category.categoryType == "expense") {
        MaterialTheme.colorScheme.expenseRed
    } else {
        MaterialTheme.colorScheme.incomeGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标（带颜色背景）
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    drawCircle(categoryColor.copy(alpha = 0.15f), radius = size.minDimension / 2)
                }
                MoniIcon(
                    icon = iconNameToRes(category.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = categoryColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!category.description.isNullOrBlank()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onUnarchiveClick) {
                MoniIcon(
                    icon = MoniIcons.Unarchive,
                    contentDescription = "恢复",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyArchivedHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "暂无已归档分类",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
