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
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreRecordGroup
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
                        "账单",
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
                        MoniIcon(
                            icon = MoniIcons.Budget,
                            contentDescription = "预算管理"
                        )
                    }
                    IconButton(onClick = onNavigateToCategoryList) {
                        MoniIcon(
                            icon = MoniIcons.FilterList,
                            contentDescription = "分类管理"
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
                MoniIcon(
                    icon = MoniIcons.AddFilled,
                    contentDescription = "记一笔"
                )
            }
        }
    ) { innerPadding ->
        if (appState.recordGroups.isEmpty()) {
            val hasAnyRecords = appState.monthlySummaries.isNotEmpty()
            EmptyRecordList(
                isMonthEmpty = hasAnyRecords,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            RecordListContent(
                recordGroups = appState.recordGroups,
                currencySymbol = appState.currencySymbol,
                onRecordClick = { onNavigateToRecordDetail(it) },
                modifier = Modifier.padding(innerPadding)
            )
        }
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
    recordGroups: List<CoreRecordGroup>,
    currencySymbol: String,
    onRecordClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        recordGroups.forEach { group ->
            item(key = "header_${group.date}") {
                DayHeader(
                    date = group.date,
                    incomeCents = group.incomeCents,
                    expenseCents = group.expenseCents,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(group.records, key = { it.id }) { record ->
                RecordListItem(
                    record = record,
                    currencySymbol = currencySymbol,
                    onClick = { onRecordClick(record.id) }
                )
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
                        text = "收 ${currencySymbol}${formatAmount(incomeCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.incomeGreen
                    )
                }
                if (expenseCents > 0) {
                    Text(
                        text = "支 ${currencySymbol}${formatAmount(expenseCents)}",
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
                text = if (isMonthEmpty) "该月暂无记账记录" else "暂无记账记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮记一笔",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDisplayDate(dateStr: String): String = try {
    val date = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    }
} catch (_: Exception) {
    dateStr
}

private fun formatYearMonthShort(yearMonth: String): String = try {
    val parts = yearMonth.split('-')
    if (parts.size == 2) {
        "${parts[0]}年${parts[1].toInt()}月"
    } else {
        yearMonth
    }
} catch (_: Exception) {
    yearMonth
}
