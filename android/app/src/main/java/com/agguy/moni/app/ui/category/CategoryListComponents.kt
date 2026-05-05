package com.agguy.moni.app.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory

/**
 * 根据分类类型字符串获取对应的主题色。
 *
 * @param categoryType 分类类型序列化名称（"expense" 或 "income"）
 * @return 支出为红色，收入为绿色
 */
@Composable
fun categoryColorForType(categoryType: String): androidx.compose.ui.graphics.Color =
    if (categoryType == "expense") {
        MaterialTheme.colorScheme.expenseRed
    } else {
        MaterialTheme.colorScheme.incomeGreen
    }

/**
 * 将分类列表按层级展平：一级分类在前，其子分类紧跟其后。
 */
fun flattenCategoriesWithHierarchy(categories: List<CoreCategory>): List<Pair<CoreCategory, Boolean>> {
    val active = categories.filter { it.archivedAt == null }
    val (parents, children) = active.partition { it.parentId == null }
    return buildList {
        for (parent in parents.sortedBy { it.sortOrder }) {
            add(parent to false)
            children
                .filter { it.parentId == parent.id }
                .sortedBy { it.sortOrder }
                .forEach { add(it to true) }
        }
    }
}

@Composable
fun CategoryListContent(
    categories: List<CoreCategory>,
    onArchiveRequest: (CoreCategory) -> Unit,
    onEditRequest: (CoreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val flatList: List<Pair<CoreCategory, Boolean>> = remember(categories) { flattenCategoriesWithHierarchy(categories) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = flatList,
            key = { (cat, _) -> cat.id }
        ) { (category, isSub) ->
            CategoryListItem(
                category = category,
                isSubCategory = isSub,
                onArchiveClick = { onArchiveRequest(category) },
                onEditClick = { onEditRequest(category) }
            )
        }
    }
}

@Composable
fun CategoryListItem(
    category: CoreCategory,
    isSubCategory: Boolean = false,
    onArchiveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = categoryColorForType(category.categoryType)

    MoniCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isSubCategory) 40.dp else 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标（圆角方框背景）
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(if (isSubCategory) 32.dp else 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = if (isSubCategory) 0.1f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                MoniIcon(
                    icon = iconNameToRes(category.iconName),
                    contentDescription = null,
                    modifier = Modifier.size(if (isSubCategory) 18.dp else 22.dp),
                    tint = categoryColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSubCategory) "› ${category.name}" else category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSubCategory) FontWeight.Normal else FontWeight.Medium
                )
                if (!category.description.isNullOrBlank()) {
                    Text(
                        text = category.description,
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
        Spacer(modifier = Modifier.height(8.dp))
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
fun ArchiveConfirmDialog(category: CoreCategory, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
fun UnarchiveConfirmDialog(category: CoreCategory, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
