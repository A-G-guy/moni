@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.util.formatAmount

/**
 * 预算管理主屏。
 *
 * - 顶部总预算卡片
 * - 支出分类预算列表（一级分类可展开/收起，二级分类缩进）
 * - 默认展开有预算设置的分类
 */
@Composable
fun BudgetListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editorVisible by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<CoreBudget?>(null) }
    var editingCategory by remember { mutableStateOf<CoreCategory?>(null) }
    var editingParentBudget by remember { mutableStateOf<CoreBudget?>(null) }

    // 仅支出分类
    val expenseCategories = remember(appState.categories) {
        appState.categories.filter { it.categoryType == "expense" && it.archivedAt == null }
    }

    // 一级分类
    val parentCategories = remember(expenseCategories) {
        expenseCategories.filter { it.parentId == null }
    }

    // 预算查找辅助
    fun findBudget(categoryId: Long?): CoreBudget? {
        return appState.budgets.find { it.categoryId == categoryId }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        MoniIcon(MoniIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 总预算卡片
            item {
                TotalBudgetCard(
                    totalBudget = findBudget(null),
                    onClick = {
                        editingBudget = findBudget(null)
                        editingCategory = null
                        editingParentBudget = null
                        editorVisible = true
                    }
                )
            }

            // 分类预算列表
            items(parentCategories, key = { it.id }) { parent ->
                val children = expenseCategories.filter { it.parentId == parent.id }
                val parentBudget = findBudget(parent.id)

                ParentBudgetItem(
                    category = parent,
                    budget = parentBudget,
                    children = children,
                    childrenBudgets = children.map { findBudget(it.id) },
                    onParentClick = {
                        editingBudget = parentBudget
                        editingCategory = parent
                        editingParentBudget = null
                        editorVisible = true
                    },
                    onChildClick = { child ->
                        editingBudget = findBudget(child.id)
                        editingCategory = child
                        editingParentBudget = parentBudget
                        editorVisible = true
                    }
                )
            }
        }
    }

    if (editorVisible) {
        val categoryName = editingCategory?.name ?: "总预算"
        BudgetEditorSheet(
            budget = editingBudget,
            categoryName = categoryName,
            parentBudget = editingParentBudget,
            onDispatch = onDispatch,
            onDismiss = { editorVisible = false }
        )
    }
}

/**
 * 总预算卡片。
 */
@Composable
private fun TotalBudgetCard(
    totalBudget: CoreBudget?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总预算",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (totalBudget != null) {
                    BudgetStatusDot(status = totalBudget.status)
                }
            }

            if (totalBudget != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "¥${formatAmount(totalBudget.spentCents)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/ ¥${formatAmount(totalBudget.amountCents)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BudgetProgressBar(percentage = totalBudget.percentage)

                Text(
                    text = "剩余 ¥${formatAmount(totalBudget.remainingCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "设置总预算，掌控全局消费",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 一级分类预算项（可展开/收起）。
 */
@Composable
private fun ParentBudgetItem(
    category: CoreCategory,
    budget: CoreBudget?,
    children: List<CoreCategory>,
    childrenBudgets: List<CoreBudget?>,
    onParentClick: () -> Unit,
    onChildClick: (CoreCategory) -> Unit
) {
    // 默认展开条件：自身有预算 或 任一子分类有预算
    val hasAnyBudget = budget != null || childrenBudgets.any { it != null }
    var expanded by remember(category.id) { mutableStateOf(hasAnyBudget) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // 一级分类头部（点击展开/收起 或 编辑）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 展开/收起图标
                    MoniIcon(
                        icon = if (expanded) MoniIcons.ExpandLess else MoniIcons.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 分类图标 + 名称
                    MoniIcon(
                        icon = MoniIcons.Budget,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 右侧：预算信息或设置按钮
                if (budget != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "¥${formatAmount(budget.spentCents)} / ¥${formatAmount(budget.amountCents)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        BudgetStatusLabel(status = budget.status)
                    }
                } else {
                    TextButton(onClick = onParentClick) {
                        Text("设置预算")
                    }
                }
            }

            // 进度条（一级分类）
            if (budget != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BudgetProgressBar(percentage = budget.percentage)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 二级分类列表（展开时显示）
            if (expanded && children.isNotEmpty()) {
                Column {
                    children.forEachIndexed { index, child ->
                        val childBudget = childrenBudgets.getOrNull(index)
                        ChildBudgetItem(
                            category = child,
                            budget = childBudget,
                            parentBudget = budget,
                            onClick = { onChildClick(child) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 二级分类预算项。
 */
@Composable
private fun ChildBudgetItem(
    category: CoreCategory,
    budget: CoreBudget?,
    parentBudget: CoreBudget?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 48.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MoniIcon(
                icon = MoniIcons.Budget,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (budget != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${formatAmount(budget.spentCents)} / ¥${formatAmount(budget.amountCents)}",
                    style = MaterialTheme.typography.bodySmall
                )
                BudgetStatusLabel(status = budget.status)
            }
        } else {
            TextButton(onClick = onClick) {
                Text("设置")
            }
        }
    }

    // 软冲突提示
    if (budget != null && parentBudget != null && budget.amountCents > parentBudget.amountCents) {
        BudgetSoftConflictWarning(
            childAmount = budget.amountCents,
            parentAmount = parentBudget.amountCents,
            modifier = Modifier.padding(start = 48.dp, end = 16.dp)
        )
    }
}
