@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.core.CoreBudgetCheckResult
import com.agguy.moni.core.CoreCategory

/** 网格列数 */
private const val GRID_COLUMNS = 5

/** 分类网格项类型 */
private sealed class CategoryGridItem {
    data class Parent(val category: CoreCategory) : CategoryGridItem()
    data class Child(val category: CoreCategory, val parentId: Long) : CategoryGridItem()
}

/**
 * 分类选择垂直滚动网格。
 *
 * 一级分类与二级分类平铺并列显示，仅在视觉上区分。
 * 有子分类的一级分类本身也可被选中。
 * 高度自适应内容，空间过多时下方自然留白。
 */
@Composable
fun CategoryGridPager(
    categories: List<CoreCategory>,
    selectedCategoryId: Long,
    currentGridPage: Int,
    budgetCheckResult: CoreBudgetCheckResult? = null,
    currencySymbol: String = "",
    onCategorySelected: (Long) -> Unit,
    onGridPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val flatItems = remember(categories) {
        buildList {
            val parents = categories
                .filter { it.parentId == null && it.archivedAt == null }
                .sortedBy { it.sortOrder }
            val childrenByParent = categories
                .filter { it.parentId != null && it.archivedAt == null }
                .groupBy { it.parentId!! }
                .mapValues { entry -> entry.value.sortedBy { it.sortOrder } }

            parents.forEach { parent ->
                add(CategoryGridItem.Parent(parent))
                childrenByParent[parent.id]?.forEach { child ->
                    add(CategoryGridItem.Child(child, parent.id))
                }
            }
        }
    }

    if (flatItems.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.category_no_categories_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 预算预警条（作为滚动内容顶部，避免挤压底部键盘）
            if (budgetCheckResult != null && budgetCheckResult.effectiveAvailable != null) {
                BudgetWarningBar(
                    checkResult = budgetCheckResult,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            val rows = flatItems.chunked(GRID_COLUMNS)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { item ->
                        when (item) {
                            is CategoryGridItem.Parent -> {
                                PrimaryCategoryItem(
                                    category = item.category,
                                    isSelected = item.category.id == selectedCategoryId,
                                    onClick = { onCategorySelected(item.category.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            is CategoryGridItem.Child -> {
                                SubCategoryItem(
                                    category = item.category,
                                    isSelected = item.category.id == selectedCategoryId,
                                    onClick = { onCategorySelected(item.category.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    // 补齐空位保持行宽一致
                    repeat(GRID_COLUMNS - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryCategoryItem(
    category: CoreCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val borderWidth = if (isSelected) 2.dp else 1.5.dp

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            SymbolIcon(
                name = category.iconName,
                filled = isSelected,
                contentDescription = null,
                size = 28.dp,
                tint = contentColor
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubCategoryItem(
    category: CoreCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val borderWidth = if (isSelected) 2.dp else 1.dp

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
        ) {
            SymbolIcon(
                name = category.iconName,
                filled = isSelected,
                contentDescription = null,
                size = 28.dp,
                tint = contentColor
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

