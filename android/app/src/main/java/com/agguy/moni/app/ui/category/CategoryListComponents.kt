package com.agguy.moni.app.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory

@Composable
fun CategoryListContent(
    categories: List<CoreCategory>,
    onArchiveRequest: (CoreCategory) -> Unit,
    onEditRequest: (CoreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryListItem(
                category = category,
                onArchiveClick = { onArchiveRequest(category) },
                onEditClick = { onEditRequest(category) }
            )
        }
    }
}

@Composable
fun CategoryListItem(
    category: CoreCategory,
    onArchiveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = if (category.categoryType == "expense") {
        MaterialTheme.colorScheme.expenseRed
    } else {
        MaterialTheme.colorScheme.incomeGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .height(24.dp)
            ) {
                drawCircle(categoryColor, radius = 12.dp.toPx())
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (category.isPreset) {
                    Text(
                        text = "预设分类",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                MoniIcon(
                    icon = MoniIcons.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!category.isPreset) {
                IconButton(onClick = onArchiveClick) {
                    MoniIcon(
                        icon = MoniIcons.Archive,
                        contentDescription = "归档",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCategoryList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无分类",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角添加",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 归档确认对话框。
 */
@Composable
fun ArchiveConfirmDialog(
    category: CoreCategory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    AnimatedVisibility(
        visible = true,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("确认归档") },
            text = {
                Text(
                    "确定要归档「${category.name}」吗？\n" +
                    "归档后该分类将不再出现在新建记录的选择中，但历史记录依然保留。"
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("归档")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 恢复确认对话框。
 */
@Composable
fun UnarchiveConfirmDialog(
    category: CoreCategory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    AnimatedVisibility(
        visible = true,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("确认恢复") },
            text = {
                Text(
                    "确定要恢复「${category.name}」吗？\n" +
                    "恢复后该分类将重新出现在新建记录的选择中。"
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
}
