@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName

/**
 * 分类管理屏。
 *
 * Material 3 Expressive 改造点：
 * - [TopAppBar] 替代 [androidx.compose.material3.TopAppBar]，强化 hero moment；
 * - 分类编辑/新增统一使用 [CategoryEditorSheet]（ModalBottomSheet），支持编辑模式；
 * - FAB 圆角与新的 corner token 体系（large=20）保持一致；
 * - 移除 `material-icons-extended` 依赖，统一使用项目内 [MoniIcons] (Material Symbols Rounded vectors)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToArchivedCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var categoryToEdit by remember { mutableStateOf<CoreCategory?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var categoryToArchive by remember { mutableStateOf<CoreCategory?>(null) }

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
                        text = "分类管理",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        MoniIcon(MoniIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToArchivedCategories) {
                        MoniIcon(MoniIcons.Archive, contentDescription = "已归档分类")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                MoniIcon(MoniIcons.AddFilled, contentDescription = "添加分类")
            }
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
            val filteredCategories = appState.categories.filter {
                it.categoryType == categoryType.serialName && it.archivedAt == null
            }

            if (filteredCategories.isEmpty()) {
                EmptyCategoryList()
            } else {
                CategoryListContent(
                    categories = filteredCategories,
                    onArchiveRequest = { categoryToArchive = it },
                    onEditRequest = { categoryToEdit = it }
                )
            }
        }
    }

    // 新增/编辑分类 BottomSheet
    if (showAddSheet || categoryToEdit != null) {
        CategoryEditorSheet(
            category = categoryToEdit,
            defaultType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
            onDispatch = onDispatch,
            onDismiss = {
                showAddSheet = false
                categoryToEdit = null
            }
        )
    }

    // 归档确认对话框
    categoryToArchive?.let { category ->
        ArchiveConfirmDialog(
            category = category,
            onConfirm = {
                onDispatch(CoreIntent.CategoryArchive(category.id))
                categoryToArchive = null
            },
            onDismiss = { categoryToArchive = null }
        )
    }
}
