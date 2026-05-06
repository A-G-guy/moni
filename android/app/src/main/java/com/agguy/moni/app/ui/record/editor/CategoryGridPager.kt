@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.GroupedCategoryIcons
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.core.CoreCategory

/** 每页网格列数 */
private const val GRID_COLUMNS = 5

/** 每页网格行数 */
private const val GRID_ROWS = 3

/** 每页最多显示的分类数 */
private const val ITEMS_PER_PAGE = GRID_COLUMNS * GRID_ROWS

/** 分类网格项类型 */
private sealed class CategoryGridItem {
    data class Parent(val category: CoreCategory) : CategoryGridItem()
    data class Child(val category: CoreCategory, val parentId: Long) : CategoryGridItem()
}

/**
 * 分类选择翻页网格。
 *
 * 一级分类与二级分类平铺并列显示，仅在视觉上区分。
 * 有子分类的一级分类本身也可被选中。
 */
@Composable
fun CategoryGridPager(
    categories: List<CoreCategory>,
    selectedCategoryId: Long,
    currentGridPage: Int,
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

    val pageCount = (flatItems.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 网格区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            if (flatItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无分类，请先添加分类",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val pagerState = rememberPagerState(
                    initialPage = currentGridPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
                    pageCount = { pageCount }
                )

                androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != currentGridPage) {
                        onGridPageChanged(pagerState.currentPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    pageSpacing = 4.dp
                ) { page ->
                    val startIndex = page * ITEMS_PER_PAGE
                    val endIndex = (startIndex + ITEMS_PER_PAGE).coerceAtMost(flatItems.size)
                    val pageItems = flatItems.subList(startIndex, endIndex)

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        userScrollEnabled = false
                    ) {
                        items(
                            items = pageItems,
                            key = { item ->
                                when (item) {
                                    is CategoryGridItem.Parent -> "p-${item.category.id}"
                                    is CategoryGridItem.Child -> "c-${item.category.id}"
                                }
                            }
                        ) { item ->
                            when (item) {
                                is CategoryGridItem.Parent -> {
                                    PrimaryCategoryItem(
                                        category = item.category,
                                        isSelected = item.category.id == selectedCategoryId,
                                        onClick = { onCategorySelected(item.category.id) }
                                    )
                                }

                                is CategoryGridItem.Child -> {
                                    SubCategoryItem(
                                        category = item.category,
                                        isSelected = item.category.id == selectedCategoryId,
                                        onClick = { onCategorySelected(item.category.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 分页指示器
        Spacer(modifier = Modifier.height(8.dp))
        if (pageCount > 1) {
            PageIndicator(
                pageCount = pageCount,
                currentPage = currentGridPage
            )
        }
    }
}

@Composable
private fun PrimaryCategoryItem(
    category: CoreCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconRes = remember(category.iconName) {
        iconNameToRes(category.iconName)
    }
    val filledIconRes = remember(category.iconName) {
        iconNameToFilledRes(category.iconName)
    }
    val displayIcon = if (isSelected && filledIconRes != null) filledIconRes else iconRes

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
        modifier = Modifier
            .fillMaxWidth()
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
            MoniIcon(
                icon = displayIcon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
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
    onClick: () -> Unit
) {
    val iconRes = remember(category.iconName) {
        iconNameToRes(category.iconName)
    }
    val filledIconRes = remember(category.iconName) {
        iconNameToFilledRes(category.iconName)
    }
    val displayIcon = if (isSelected && filledIconRes != null) filledIconRes else iconRes

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
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
        modifier = Modifier
            .fillMaxWidth()
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
            MoniIcon(
                icon = displayIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val size by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 6.dp,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                ),
                label = "indicator_size"
            )
            val color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(MaterialTheme.shapes.small)
            ) {
                Surface(
                    color = color,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        }
    }
}

/**
 * 根据图标名称查找选中态（filled）图标资源 ID。
 */
private fun iconNameToFilledRes(iconName: String): Int? {
    return GroupedCategoryIcons
        .asSequence()
        .flatMap { it.icons.asSequence() }
        .firstOrNull { it.name == iconName }
        ?.iconResFilled
}
