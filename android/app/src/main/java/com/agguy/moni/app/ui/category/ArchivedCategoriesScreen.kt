@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.iconNameToRes
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName

/**
 * 已归档分类二级页面。
 *
 * - 顶部 Tab 切换支出/收入已归档分类；
 * - 列表展示已归档分类，支持恢复操作；
 * - 预设分类仅展示，不提供恢复按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedCategoriesScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var categoryToUnarchive by remember { mutableStateOf<CoreCategory?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        onDispatch(CoreIntent.CategoryList)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "已归档分类",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        MoniIcon(MoniIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("支出") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("收入") }
                )
            }

            val categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME
            val archivedCategories = appState.categories.filter {
                it.categoryType == categoryType.serialName && it.archivedAt != null
            }

            if (archivedCategories.isEmpty()) {
                EmptyArchivedCategoriesList()
            } else {
                ArchivedCategoriesListContent(
                    archivedCategories = archivedCategories,
                    onUnarchiveRequest = { categoryToUnarchive = it }
                )
            }
        }
    }

    // 恢复确认对话框
    categoryToUnarchive?.let { category ->
        UnarchiveConfirmDialog(
            category = category,
            onConfirm = {
                onDispatch(CoreIntent.CategoryUnarchive(category.id))
                categoryToUnarchive = null
            },
            onDismiss = { categoryToUnarchive = null }
        )
    }
}

@Composable
private fun ArchivedCategoriesListContent(
    archivedCategories: List<CoreCategory>,
    onUnarchiveRequest: (CoreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val flatList = remember(archivedCategories) {
        val parentIds = archivedCategories.map { it.id }.toSet()
        val (parents, children) = archivedCategories.partition { it.parentId == null || !parentIds.contains(it.parentId) }
        buildList {
            for (parent in parents.sortedBy { it.sortOrder }) {
                add(parent to false)
                archivedCategories
                    .filter { it.parentId == parent.id }
                    .sortedBy { it.sortOrder }
                    .forEach { add(it to true) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(flatList, key = { "archived_${it.first.id}" }) { (category, isSub) ->
            ArchivedCategoryItem(
                category = category,
                isSubCategory = isSub,
                onUnarchiveClick = { onUnarchiveRequest(category) }
            )
        }
    }
}

@Composable
private fun ArchivedCategoryItem(
    category: CoreCategory,
    isSubCategory: Boolean = false,
    onUnarchiveClick: () -> Unit,
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
            // 分类图标
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
private fun EmptyArchivedCategoriesList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "暂无已归档分类",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
