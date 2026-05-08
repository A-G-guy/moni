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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.SymbolIcon
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
    var isReorderMode by remember { mutableStateOf(false) }

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
                        text = stringResource(R.string.category_list_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(name = "arrow_back", contentDescription = stringResource(R.string.back), size = 24.dp)
                    }
                },
                actions = {
                    if (!isReorderMode) {
                        IconButton(onClick = onNavigateToArchivedCategories) {
                            SymbolIcon(name = "archive", contentDescription = stringResource(R.string.archived_title), size = 24.dp)
                        }
                    }
                    TextButton(onClick = { isReorderMode = !isReorderMode }) {
                        Text(if (isReorderMode) stringResource(R.string.done) else stringResource(R.string.sort))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!isReorderMode) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = MaterialTheme.shapes.large
                ) {
                    SymbolIcon(name = "add", filled = true, contentDescription = stringResource(R.string.category_new), size = 24.dp)
                }
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
                    text = { Text(stringResource(R.string.category_expense)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.category_income)) }
                )
            }

            val categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME
            val typeCategories = appState.categories.filter {
                it.categoryType == categoryType.serialName && it.archivedAt == null
            }

            if (typeCategories.isEmpty()) {
                EmptyCategoryList()
            } else {
                CategoryListContent(
                    categories = typeCategories,
                    onArchiveRequest = { categoryToArchive = it },
                    onEditRequest = { categoryToEdit = it },
                    isReorderMode = isReorderMode,
                    onDispatch = onDispatch
                )
            }
        }
    }

    // 新增/编辑分类 BottomSheet
    if (showAddSheet || categoryToEdit != null) {
        CategoryEditorSheet(
            category = categoryToEdit,
            defaultType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
            categories = appState.categories,
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
