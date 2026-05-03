package com.agguy.moni.app.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.agguy.moni.app.AppState
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CoreCategory?>(null) }

    LaunchedEffect(Unit) {
        onDispatch(CoreIntent.CategoryList)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "添加分类")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
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
                it.categoryType == categoryType.serialName
            }

            if (filteredCategories.isEmpty()) {
                EmptyCategoryList()
            } else {
                CategoryListContent(
                    categories = filteredCategories,
                    onDeleteRequest = { categoryToDelete = it }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = showAddDialog,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
    ) {
        if (showAddDialog) {
            AddCategoryDialog(
                categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
                onConfirm = { name, iconName, colorHex ->
                    onDispatch(
                        CoreIntent.CategoryCreate(
                            name = name,
                            categoryType = if (selectedTab == 0) RecordType.EXPENSE else RecordType.INCOME,
                            iconName = iconName,
                            colorHex = colorHex
                        )
                    )
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }

    AnimatedVisibility(
        visible = categoryToDelete != null,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = scaleOut(animationSpec = spring())
    ) {
        categoryToDelete?.let { category ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除「${category.name}」吗？\n如果该分类已被使用，将无法删除。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDispatch(CoreIntent.CategoryDelete(category.id))
                            categoryToDelete = null
                        }
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
