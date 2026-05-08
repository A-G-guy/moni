@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.budget

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 预算列表项数据模型。
 */
private sealed class BudgetListItem {
    data class Total(val budget: CoreBudget?) : BudgetListItem()
    data class Category(
        val category: CoreCategory,
        val budget: CoreBudget?,
        val isChild: Boolean,
        val parentBudget: CoreBudget? = null
    ) : BudgetListItem()
}

/**
 * 将支出分类按层级展平：一级分类在前，子分类紧跟其后。
 * 总预算作为独立项排在最前面。
 */
private fun flattenBudgetItems(
    categories: List<CoreCategory>,
    budgets: List<CoreBudget>
): List<BudgetListItem> {
    val expense = categories.filter { it.categoryType == "expense" && it.archivedAt == null }
    val parents = expense.filter { it.parentId == null }
    return buildList {
        add(BudgetListItem.Total(budgets.find { it.categoryId == null }))
        for (parent in parents) {
            val parentBudget = budgets.find { it.categoryId == parent.id }
            add(BudgetListItem.Category(parent, parentBudget, isChild = false))
            expense
                .filter { it.parentId == parent.id }
                .forEach { child ->
                    add(BudgetListItem.Category(
                        category = child,
                        budget = budgets.find { it.categoryId == child.id },
                        isChild = true,
                        parentBudget = parentBudget
                    ))
                }
        }
    }
}

/**
 * 预算管理主屏。
 *
 * - 总预算卡片 + 分类预算列表，统一使用 MoniCard（Tonal）样式
 * - 层级展平（参考分类管理页），整行点击弹出编辑弹窗
 * - 已设置预算的卡片显示进度条 + 日均/剩余日均/剩余统计
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

    val monthLabel = remember(selectedYearMonth) {
        val parts = selectedYearMonth.split("-")
        if (parts.size == 2) {
            String.format("%s-%02d", parts[0], parts[1].toInt())
        } else {
            ""
        }
    }

    val budgetItems = remember(appState.categories, appState.budgets) {
        flattenBudgetItems(appState.categories, appState.budgets)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = budgetItems,
                key = { item ->
                    when (item) {
                        is BudgetListItem.Total -> -1L
                        is BudgetListItem.Category -> item.category.id
                    }
                }
            ) { item ->
                when (item) {
                    is BudgetListItem.Total -> {
                        TotalBudgetCard(
                            totalBudget = item.budget,
                            yearMonth = selectedYearMonth,
                            currencySymbol = appState.currencySymbol,
                            onClick = {
                                editingBudget = item.budget
                                editingCategory = null
                                editingParentBudget = null
                                editorVisible = true
                            }
                        )
                    }
                    is BudgetListItem.Category -> {
                        BudgetCategoryCard(
                            category = item.category,
                            budget = item.budget,
                            isChild = item.isChild,
                            parentBudget = item.parentBudget,
                            yearMonth = selectedYearMonth,
                            currencySymbol = appState.currencySymbol,
                            onClick = {
                                editingBudget = item.budget
                                editingCategory = item.category
                                editingParentBudget = item.parentBudget
                                editorVisible = true
                            }
                        )
                    }
                }
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
            currencySymbol = appState.currencySymbol,
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
}

/**
 * 总预算卡片。
 */
@Composable
private fun TotalBudgetCard(
    totalBudget: CoreBudget?,
    yearMonth: String,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dailyStats = remember(totalBudget, yearMonth) {
        totalBudget?.let {
            calculateBudgetDailyStats(it.amountCents, it.remainingCents, yearMonth)
        }
    }

    MoniCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.budget_total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (totalBudget != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currencySymbol${formatAmount(totalBudget.amountCents)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        BudgetStatusLabel(status = totalBudget.status)
                    }
                } else {
                    Text(
                        text = stringResource(R.string.budget_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (totalBudget != null && dailyStats != null) {
                BudgetProgressBar(percentage = totalBudget.percentage)
                BudgetDailyStatsRow(
                    dailyStats = dailyStats,
                    currencySymbol = currencySymbol,
                    status = totalBudget.status
                )
            } else {
                Text(
                    text = stringResource(R.string.budget_tap_to_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 分类预算卡片。
 */
@Composable
private fun BudgetCategoryCard(
    category: CoreCategory,
    budget: CoreBudget?,
    isChild: Boolean,
    parentBudget: CoreBudget?,
    yearMonth: String,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val dailyStats = remember(budget, yearMonth) {
        budget?.let {
            calculateBudgetDailyStats(it.amountCents, it.remainingCents, yearMonth)
        }
    }
    val categoryColor = MaterialTheme.colorScheme.expenseRed

    MoniCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (isChild) 40.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(if (isChild) 32.dp else 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = if (isChild) 0.1f else 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    SymbolIcon(
                        name = category.iconName,
                        contentDescription = null,
                        size = if (isChild) 18.dp else 22.dp,
                        tint = categoryColor
                    )
                }

                // 分类名称
                Text(
                    text = if (isChild) "› ${category.name}" else category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isChild) FontWeight.Normal else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                // 右侧金额或状态
                if (budget != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currencySymbol${formatAmount(budget.amountCents)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        BudgetStatusLabel(status = budget.status)
                    }
                } else {
                    Text(
                        text = stringResource(R.string.budget_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (budget != null && dailyStats != null) {
                BudgetProgressBar(percentage = budget.percentage)
                BudgetDailyStatsRow(
                    dailyStats = dailyStats,
                    currencySymbol = currencySymbol,
                    status = budget.status
                )
            }

            // 软冲突提示
            if (isChild && parentBudget != null && budget != null &&
                budget.amountCents > parentBudget.amountCents
            ) {
                BudgetSoftConflictWarning(
                    childAmount = budget.amountCents,
                    parentAmount = parentBudget.amountCents
                )
            }
        }
    }
}

/**
 * 预算日均统计信息行。
 */
@Composable
private fun BudgetDailyStatsRow(
    dailyStats: BudgetDailyStats,
    currencySymbol: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DailyStatItem(
            label = stringResource(R.string.budget_daily_average),
            amountCents = dailyStats.dailyBudgetCents,
            currencySymbol = currencySymbol
        )
        DailyStatItem(
            label = stringResource(R.string.budget_daily_remaining),
            amountCents = dailyStats.dailyRemainingCents,
            currencySymbol = currencySymbol,
            isNegativeHighlighted = true,
            status = status
        )
        DailyStatItem(
            label = stringResource(R.string.budget_remaining),
            amountCents = dailyStats.remainingCents,
            currencySymbol = currencySymbol,
            isNegativeHighlighted = true,
            status = status
        )
    }
}

/**
 * 单日统计数据项。
 */
@Composable
private fun DailyStatItem(
    label: String,
    amountCents: Long,
    currencySymbol: String,
    isNegativeHighlighted: Boolean = false,
    status: String = ""
) {
    val isNegative = amountCents < 0
    val amountColor = if (isNegativeHighlighted && isNegative) {
        when (status) {
            "overrun" -> MaterialTheme.colorScheme.expenseRed
            "critical" -> Color(0xFFFFA726)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$currencySymbol${formatAmount(amountCents)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
