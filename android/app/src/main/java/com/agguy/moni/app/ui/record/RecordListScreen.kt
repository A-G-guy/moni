@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.RecordItemDisplaySettings
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 账单列表主屏。
 *
 * 月份过滤与 TopAppBar 月份选择器：
 * - TopAppBar 右侧展示当前月份按钮（点击唤起 [MonthPickerSheet]）和分类管理图标；
 * - FAB 改为独立圆形，仅保留"记一笔"；
 * - 默认显示系统当前月的账单数据，空状态区分"全局无记录"与"该月无记录"。
 */
@Composable
fun RecordListScreen(
    appState: AppState,
    selectedYearMonth: String,
    recordItemDisplaySettings: RecordItemDisplaySettings,
    onDispatch: (CoreIntent) -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    onNavigateToBudgetList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val monthLabel = remember(selectedYearMonth) {
        formatYearMonthShort(selectedYearMonth)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.record_list_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    TextButton(onClick = { sheetVisible = true }) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToBudgetList) {
                        SymbolIcon(
                            name = "savings",
                            contentDescription = stringResource(R.string.budget_list_title),
                            size = 24.dp
                        )
                    }
                    IconButton(onClick = onNavigateToCategoryList) {
                        SymbolIcon(
                            name = "category",
                            contentDescription = stringResource(R.string.category_list_title),
                            size = 24.dp
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToRecordDetail(null) },
                shape = MaterialTheme.shapes.large
            ) {
                SymbolIcon(
                    name = "add",
                    filled = true,
                    contentDescription = stringResource(R.string.editor_title_new),
                    size = 24.dp
                )
            }
        }
    ) { innerPadding ->
        RecordListContent(
            appState = appState,
            selectedYearMonth = selectedYearMonth,
            recordItemDisplaySettings = recordItemDisplaySettings,
            onRecordClick = { onNavigateToRecordDetail(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (sheetVisible) {
        MonthPickerSheet(
            availableYearMonths = remember(appState.monthlySummaries) {
                appState.monthlySummaries.map { it.yearMonth }.toSet()
            },
            currentYearMonth = selectedYearMonth,
            todayYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
            onYearMonthSelected = { yearMonth ->
                onSelectYearMonth(yearMonth)
                sheetVisible = false
            },
            onDismiss = { sheetVisible = false }
        )
    }
}

@Composable
private fun RecordListContent(
    appState: AppState,
    selectedYearMonth: String,
    recordItemDisplaySettings: RecordItemDisplaySettings,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "overview") {
            RecordOverviewCard(
                selectedYearMonth = selectedYearMonth,
                overviewMetrics = appState.overviewMetrics,
                currencySymbol = appState.currencySymbol,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (appState.recordGroups.isEmpty()) {
            item(key = "empty") {
                val hasAnyRecords = appState.monthlySummaries.isNotEmpty()
                EmptyRecordList(
                    isMonthEmpty = hasAnyRecords,
                    yearMonth = selectedYearMonth,
                    errorMessage = appState.errorMessage,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else {
            appState.recordGroups.forEach { group ->
                item(key = "header_${group.date}") {
                    DayHeader(
                        date = group.date,
                        incomeCents = group.incomeCents,
                        expenseCents = group.expenseCents,
                        currencySymbol = appState.currencySymbol,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(group.records, key = { it.id }) { record ->
                    RecordListItem(
                        record = record,
                        currencySymbol = appState.currencySymbol,
                        categories = appState.categories,
                        showIcon = recordItemDisplaySettings.showIcon,
                        showFullCategory = recordItemDisplaySettings.showFullCategory,
                        notePriority = recordItemDisplaySettings.notePriority,
                        onClick = { onRecordClick(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: String,
    incomeCents: Long,
    expenseCents: Long,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDisplayDate(date),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (incomeCents > 0) {
                    Text(
                        text = stringResource(R.string.record_income_abbr, "", "${currencySymbol}${formatAmount(incomeCents)}"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.incomeGreen
                    )
                }
                if (expenseCents > 0) {
                    Text(
                        text = stringResource(R.string.record_expense_abbr, "", "${currencySymbol}${formatAmount(expenseCents)}"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.expenseRed
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun EmptyRecordList(
    isMonthEmpty: Boolean,
    yearMonth: String,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isMonthEmpty) stringResource(R.string.record_list_empty_month) else stringResource(R.string.record_list_empty_global),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.record_list_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.record_list_month_label, yearMonth),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatDisplayDate(dateStr: String): String = try {
    val date = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, EEEE"))
    }
} catch (_: Exception) {
    dateStr
}

private fun formatYearMonthShort(yearMonth: String): String = try {
    val parts = yearMonth.split('-')
    if (parts.size == 2) {
        String.format("%s-%02d", parts[0], parts[1].toInt())
    } else {
        yearMonth
    }
} catch (_: Exception) {
    yearMonth
}
