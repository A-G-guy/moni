@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * - 添加/归档对话框接入 motion token；
 * - FAB 圆角与新的 corner token 体系（large=20）保持一致；
 * - 移除 `material-icons-extended` 依赖，统一使用项目内 [MoniIcons] (Material Symbols Rounded vectors)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToArchive by remember { mutableStateOf<CoreCategory?>(null) }

    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
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
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
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
                    onArchiveRequest = { categoryToArchive = it }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showAddDialog,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        if (showAddDialog) {
            AddCategoryDialog(
                categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
                onConfirm = { name, iconName ->
                    onDispatch(
                        CoreIntent.CategoryCreate(
                            name = name,
                            categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
                            iconName = iconName
                        )
                    )
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }

    AnimatedVisibility(
        visible = categoryToArchive != null,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        categoryToArchive?.let { category ->
            AlertDialog(
                onDismissRequest = { categoryToArchive = null },
                shape = MaterialTheme.shapes.extraLarge,
                title = { Text("确认归档") },
                text = { Text("确定要归档「${category.name}」吗？\n归档后该分类将不再出现在新建记录的选择中，但历史记录依然保留。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDispatch(CoreIntent.CategoryArchive(category.id))
                            categoryToArchive = null
                        }
                    ) {
                        Text("归档")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToArchive = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
