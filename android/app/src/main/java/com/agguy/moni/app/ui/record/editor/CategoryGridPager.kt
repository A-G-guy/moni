@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.CategoryIcon
import com.agguy.moni.app.theme.GroupedCategoryIcons
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.core.CoreCategory

/** 每页网格列数 */
private const val GRID_COLUMNS = 4

/** 每页网格行数 */
private const val GRID_ROWS = 3

/** 每页最多显示的分类数 */
private const val ITEMS_PER_PAGE = GRID_COLUMNS * GRID_ROWS

/**
 * 分类选择翻页网格。
 *
 * 一级分类：4列×3行翻页网格，超出一页左右滑动，下方分页指示器。
 * 二级分类：点击含子项的一级分类后原地展开，第一个格子为"返回上级"。
 */
@Composable
fun CategoryGridPager(
    categories: List<CoreCategory>,
    selectedCategoryId: Long,
    selectedParentCategoryId: Long,
    isInSubCategoryView: Boolean,
    currentGridPage: Int,
    onCategorySelected: (Long) -> Unit,
    onEnterSubCategory: (Long) -> Unit,
    onExitSubCategory: () -> Unit,
    onGridPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val parentCategories = remember(categories) {
        categories
            .filter { it.parentId == null && it.archivedAt == null }
            .sortedBy { it.sortOrder }
    }

    val childrenByParent = remember(categories) {
        categories
            .filter { it.parentId != null && it.archivedAt == null }
            .groupBy { it.parentId!! }
            .mapValues { entry -> entry.value.sortedBy { it.sortOrder } }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 网格区域（固定高度）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AnimatedContent(
                targetState = isInSubCategoryView,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "category_grid_switch"
            ) { inSubView ->
                if (inSubView) {
                    val children = childrenByParent[selectedParentCategoryId] ?: emptyList()
                    SubCategoryGrid(
                        children = children,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = {
                            onCategorySelected(it)
                            onExitSubCategory()
                        },
                        onBack = onExitSubCategory
                    )
                } else {
                    PrimaryCategoryPager(
                        parentCategories = parentCategories,
                        childrenByParent = childrenByParent,
                        selectedCategoryId = selectedCategoryId,
                        onCategorySelected = onCategorySelected,
                        onEnterSubCategory = onEnterSubCategory,
                        currentPage = currentGridPage,
                        onPageChanged = onGridPageChanged
                    )
                }
            }
        }

        // 分页指示器（仅一级视图显示）
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibilityWithFade(visible = !isInSubCategoryView) {
            val pageCount = (parentCategories.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            if (pageCount > 1) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = currentGridPage
                )
            }
        }
    }
}

@Composable
private fun PrimaryCategoryPager(
    parentCategories: List<CoreCategory>,
    childrenByParent: Map<Long, List<CoreCategory>>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    onEnterSubCategory: (Long) -> Unit,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val pageCount = (parentCategories.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        pageCount = { pageCount }
    )

    // 同步外部 page 状态
    if (pagerState.currentPage != currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 8.dp
    ) { page ->
        val startIndex = page * ITEMS_PER_PAGE
        val endIndex = (startIndex + ITEMS_PER_PAGE).coerceAtMost(parentCategories.size)
        val pageCategories = parentCategories.subList(startIndex, endIndex)

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(pageCategories, key = { it.id }) { category ->
                val hasChildren = childrenByParent.containsKey(category.id)
                CategoryGridItem(
                    category = category,
                    isSelected = category.id == selectedCategoryId,
                    hasChildren = hasChildren,
                    onClick = {
                        if (hasChildren) {
                            onEnterSubCategory(category.id)
                        } else {
                            onCategorySelected(category.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SubCategoryGrid(
    children: List<CoreCategory>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            // 第一个格子：返回上级
            item(key = "back") {
                BackToParentItem(onClick = onBack)
            }

            // 子分类
            items(children, key = { it.id }) { category ->
                CategoryGridItem(
                    category = category,
                    isSelected = category.id == selectedCategoryId,
                    hasChildren = false,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryGridItem(
    category: CoreCategory,
    isSelected: Boolean,
    hasChildren: Boolean,
    onClick: () -> Unit
) {
    val iconRes = remember(category.iconName) {
        iconNameToRes(category.iconName)
    }
    val filledIconRes = remember(category.iconName) {
        iconNameToFilledRes(category.iconName)
    }
    val displayIcon = if (isSelected && filledIconRes != null) filledIconRes else iconRes

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
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
            .aspectRatio(0.85f),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (hasChildren) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(6.dp)
                        )
                    }
                ) {
                    CategoryItemContent(
                        iconRes = displayIcon,
                        name = category.name,
                        contentColor = contentColor
                    )
                }
            } else {
                CategoryItemContent(
                    iconRes = displayIcon,
                    name = category.name,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun CategoryItemContent(
    iconRes: Int,
    name: String,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MoniIcon(
            icon = iconRes,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = contentColor
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BackToParentItem(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MoniIcon(
                icon = MoniIcons.ArrowBack,
                contentDescription = "返回上级",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "返回",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun AnimatedVisibilityWithFade(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        content()
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
