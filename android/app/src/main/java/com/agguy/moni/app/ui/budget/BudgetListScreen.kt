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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.BudgetScope
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    selectedYearMonth: String,
    onDispatch: (CoreIntent) -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editorVisible by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<CoreBudget?>(null) }
    var editingCategory by remember { mutableStateOf<CoreCategory?>(null) }
    var editingParentBudget by remember { mutableStateOf<CoreBudget?>(null) }
    var monthPickerVisible by remember { mutableStateOf(false) }
    var deleteConfirmBudget by remember { mutableStateOf<CoreBudget?>(null) }

    val monthLabel = remember(selectedYearMonth) {
        val parts = selectedYearMonth.split("-")
        if (parts.size == 2) {
            String.format("%s-%02d", parts[0], parts[1].toInt())
        } else {
            ""
        }
    }

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
                title = { Text(stringResource(R.string.budget_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(name = "arrow_back", contentDescription = stringResource(R.string.back), size = 24.dp)
                    }
                },
                actions = {
                    TextButton(onClick = { monthPickerVisible = true }) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                    },
                    onDelete = { findBudget(null)?.let { deleteConfirmBudget = it } }
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
                    onParentDelete = { parentBudget?.let { deleteConfirmBudget = it } },
                    onChildClick = { child ->
                        editingBudget = findBudget(child.id)
                        editingCategory = child
                        editingParentBudget = parentBudget
                        editorVisible = true
                    },
                    onChildDelete = { child ->
                        findBudget(child.id)?.let { deleteConfirmBudget = it }
                    }
                )
            }
        }
    }

    if (editorVisible) {
        val categoryName = editingCategory?.name ?: stringResource(R.string.budget_total)
        BudgetEditorSheet(
            budget = editingBudget,
            categoryId = editingCategory?.id,
            categoryName = categoryName,
            parentBudget = editingParentBudget,
            yearMonth = selectedYearMonth,
            onDispatch = onDispatch,
            onDismiss = { editorVisible = false }
        )
    }

    if (monthPickerVisible) {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        MonthPickerSheet(
            availableYearMonths = emptySet(),
            currentYearMonth = selectedYearMonth,
            todayYearMonth = today,
            onYearMonthSelected = { selected ->
                monthPickerVisible = false
                onSelectYearMonth(selected)
            },
            onDismiss = { monthPickerVisible = false }
        )
    }

    // 列表直接删除确认
    if (deleteConfirmBudget != null) {
        val budget = deleteConfirmBudget!!
        AlertDialog(
            onDismissRequest = { deleteConfirmBudget = null },
            title = { Text(stringResource(R.string.budget_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.budget_delete_message))
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = selectedYearMonth,
                                    scope = BudgetScope.THIS_MONTH
                                )
                            )
                            deleteConfirmBudget = null
                        }
                    ) {
                        Text(stringResource(R.string.budget_stop_this_month))
                    }
                    TextButton(
                        onClick = {
                            onDispatch(
                                CoreIntent.BudgetDelete(
                                    id = budget.id,
                                    yearMonth = selectedYearMonth,
                                    scope = BudgetScope.FUTURE_ONLY
                                )
                            )
                            deleteConfirmBudget = null
                        }
                    ) {
                        Text(stringResource(R.string.budget_stop_next_month))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { deleteConfirmBudget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 总预算卡片。
 */
@Composable
private fun TotalBudgetCard(
    totalBudget: CoreBudget?,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                    text = stringResource(R.string.budget_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalBudget != null) {
                        BudgetStatusDot(status = totalBudget.status)
                        IconButton(onClick = onDelete) {
                            SymbolIcon(
                                name = "delete",
                                contentDescription = stringResource(R.string.budget_delete_total),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 24.dp
                            )
                        }
                    }
                }
            }

            if (totalBudget != null) {
                // 大字显示预算金额，避免与已用金额混淆
                Text(
                    text = "¥${formatAmount(totalBudget.amountCents)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                BudgetProgressBar(percentage = totalBudget.percentage)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.budget_spent_format, "¥${formatAmount(totalBudget.spentCents)}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.budget_remaining_format, "¥${formatAmount(totalBudget.remainingCents)}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (totalBudget.status) {
                            "overrun" -> MaterialTheme.colorScheme.expenseRed
                            "critical" -> Color(0xFFFFA726)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.budget_set_total_hint),
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
    onParentDelete: () -> Unit,
    onChildClick: (CoreCategory) -> Unit,
    onChildDelete: (CoreCategory) -> Unit
) {
    // 所有分类默认展开
    val hasChildren = children.isNotEmpty()
    var expanded by remember(category.id) { mutableStateOf(true) }

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
                    // 展开/收起图标（仅当有子分类时显示）
                    if (hasChildren) {
                        SymbolIcon(
                            name = if (expanded) "expand_less" else "expand_more",
                            contentDescription = if (expanded) stringResource(R.string.close) else stringResource(R.string.search),
                            size = 20.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // 无子分类时占同等宽度保持对齐
                        Spacer(modifier = Modifier.size(20.dp))
                    }

                    // 分类图标 + 名称
                    SymbolIcon(
                        name = category.iconName,
                        contentDescription = null,
                        size = 24.dp,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "¥${formatAmount(budget.amountCents)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.budget_spent_format, "¥${formatAmount(budget.spentCents)}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BudgetStatusLabel(status = budget.status)
                        }
                        IconButton(onClick = onParentDelete) {
                            SymbolIcon(
                                name = "delete",
                                contentDescription = stringResource(R.string.budget_delete_total),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                size = 24.dp
                            )
                        }
                    }
                } else {
                    TextButton(onClick = onParentClick) {
                        Text(stringResource(R.string.budget_set))
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
                            onClick = { onChildClick(child) },
                            onDelete = { onChildDelete(child) }
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
    onClick: () -> Unit,
    onDelete: () -> Unit
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
            SymbolIcon(
                name = category.iconName,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (budget != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥${formatAmount(budget.amountCents)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.budget_spent_format, "¥${formatAmount(budget.spentCents)}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BudgetStatusLabel(status = budget.status)
                }
                IconButton(onClick = onDelete) {
                    SymbolIcon(
                        name = "delete",
                        contentDescription = stringResource(R.string.budget_delete_total),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        size = 24.dp
                    )
                }
            }
        } else {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.action_set))
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
