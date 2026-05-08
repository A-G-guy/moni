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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent

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
    isReorderMode: Boolean = false,
    onDispatch: (CoreIntent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val flatList: List<Pair<CoreCategory, Boolean>> = remember(categories) { flattenCategoriesWithHierarchy(categories) }
    val parentIds = remember(flatList) { flatList.filter { !it.second }.map { it.first.id } }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = flatList,
            key = { (cat, _) -> cat.id }
        ) { (category, isSub) ->
            val canMoveUp = remember(category.id, flatList, isReorderMode) {
                if (!isReorderMode) false
                else if (isSub) {
                    val siblingIds = flatList.filter { it.second && it.first.parentId == category.parentId }.map { it.first.id }
                    siblingIds.indexOf(category.id) > 0
                } else {
                    parentIds.indexOf(category.id) > 0
                }
            }
            val canMoveDown = remember(category.id, flatList, isReorderMode) {
                if (!isReorderMode) false
                else if (isSub) {
                    val siblingIds = flatList.filter { it.second && it.first.parentId == category.parentId }.map { it.first.id }
                    val idx = siblingIds.indexOf(category.id)
                    idx >= 0 && idx < siblingIds.size - 1
                } else {
                    val idx = parentIds.indexOf(category.id)
                    idx >= 0 && idx < parentIds.size - 1
                }
            }
            CategoryListItem(
                category = category,
                isSubCategory = isSub,
                isReorderMode = isReorderMode,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onArchiveClick = { onArchiveRequest(category) },
                onEditClick = { onEditRequest(category) },
                onMoveUp = {
                    val siblings = if (category.parentId == null) {
                        categories.filter { it.parentId == null && it.archivedAt == null }.sortedBy { it.sortOrder }
                    } else {
                        categories.filter { it.parentId == category.parentId && it.archivedAt == null }.sortedBy { it.sortOrder }
                    }
                    val currentIndex = siblings.indexOfFirst { it.id == category.id }
                    if (currentIndex > 0) {
                        val newOrder = siblings.toMutableList()
                        java.util.Collections.swap(newOrder, currentIndex, currentIndex - 1)
                        onDispatch(CoreIntent.CategoryReorder(newOrder.map { it.id }))
                    }
                },
                onMoveDown = {
                    val siblings = if (category.parentId == null) {
                        categories.filter { it.parentId == null && it.archivedAt == null }.sortedBy { it.sortOrder }
                    } else {
                        categories.filter { it.parentId == category.parentId && it.archivedAt == null }.sortedBy { it.sortOrder }
                    }
                    val currentIndex = siblings.indexOfFirst { it.id == category.id }
                    if (currentIndex >= 0 && currentIndex < siblings.size - 1) {
                        val newOrder = siblings.toMutableList()
                        java.util.Collections.swap(newOrder, currentIndex, currentIndex + 1)
                        onDispatch(CoreIntent.CategoryReorder(newOrder.map { it.id }))
                    }
                }
            )
        }
    }
}

@Composable
fun CategoryListItem(
    category: CoreCategory,
    isSubCategory: Boolean = false,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onArchiveClick: () -> Unit,
    onEditClick: () -> Unit,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
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
                    end = if (isReorderMode) 8.dp else 16.dp,
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
                SymbolIcon(
                    name = category.iconName,
                    contentDescription = null,
                    size = if (isSubCategory) 18.dp else 22.dp,
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

            if (isReorderMode) {
                // 排序模式：显示上移/下移按钮
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        SymbolIcon(
                            name = "expand_less",
                            contentDescription = stringResource(R.string.search),
                            size = 20.dp,
                            tint = if (canMoveUp) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        SymbolIcon(
                            name = "expand_more",
                            contentDescription = stringResource(R.string.search),
                            size = 20.dp,
                            tint = if (canMoveDown) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            } else {
                // 普通模式：显示编辑/归档按钮
                IconButton(onClick = onEditClick) {
                    SymbolIcon(
                        name = "edit",
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 24.dp
                    )
                }

                IconButton(onClick = onArchiveClick) {
                    SymbolIcon(
                        name = "archive",
                        contentDescription = stringResource(R.string.category_archive),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 24.dp
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
            text = stringResource(R.string.category_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.category_empty_hint),
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
            title = { Text(stringResource(R.string.archive_title)) },
            text = {
                Text(
                    stringResource(R.string.archive_message, category.name)
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.archive_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
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
            title = { Text(stringResource(R.string.unarchive_title)) },
            text = {
                Text(
                    stringResource(R.string.unarchive_message, category.name)
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.unarchive_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
