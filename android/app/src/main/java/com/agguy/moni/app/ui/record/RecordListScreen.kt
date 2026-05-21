@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.RecordItemDisplaySettings
import com.agguy.moni.app.SearchParams
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
 *
 * 返回导航层级（由内到外）：
 * 1. 覆盖层：DropdownMenu / ModalBottomSheet — Material 3 组件内置处理
 * 2. 状态模式：搜索模式 — BackHandler 退出搜索
 * 3. 页面级：根页面 — 系统默认退出应用
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
    onNavigateToAiBookkeeping: () -> Unit,
    onEnterSearchMode: () -> Unit,
    onExitSearchMode: () -> Unit,
    onUpdateSearchKeyword: (String) -> Unit,
    onUpdateSearchParams: (SearchParams) -> Unit,
    onResetSearchParams: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetVisible by remember { mutableStateOf(false) }
    var filterSheetVisible by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val monthLabel = remember(selectedYearMonth) {
        formatYearMonthShort(selectedYearMonth)
    }

    // 搜索模式下拦截系统返回键，优先退出搜索而非退出应用
    BackHandler(enabled = appState.isSearchMode) {
        focusManager.clearFocus()
        keyboardController?.hide()
        onExitSearchMode()
    }

    // 进入搜索模式时自动聚焦搜索框
    if (appState.isSearchMode) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = appState.isSearchMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(150))
                        },
                        label = "top_bar_title"
                    ) { isSearchMode ->
                        if (isSearchMode) {
                            TextField(
                                value = appState.searchKeyword,
                                onValueChange = { onUpdateSearchKeyword(it) },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                ),
                                shape = MaterialTheme.shapes.large,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                            )
                        } else {
                            Text(
                                stringResource(R.string.record_list_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (appState.isSearchMode) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onExitSearchMode()
                        }) {
                            SymbolIcon(
                                name = "arrow_back",
                                contentDescription = stringResource(R.string.back),
                                size = 24.dp
                            )
                        }
                    }
                },
                actions = {
                    AnimatedContent(
                        targetState = appState.isSearchMode,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(150))
                        },
                        label = "top_bar_actions"
                    ) { isSearchMode ->
                        if (isSearchMode) {
                            IconButton(onClick = { filterSheetVisible = true }) {
                                SymbolIcon(
                                    name = "filter_list",
                                    contentDescription = stringResource(R.string.filter_title),
                                    size = 24.dp
                                )
                            }
                        } else {
                            Row {
                                TextButton(onClick = { sheetVisible = true }) {
                                    Text(
                                        text = monthLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = onEnterSearchMode) {
                                    SymbolIcon(
                                        name = "search",
                                        contentDescription = stringResource(R.string.search),
                                        size = 24.dp
                                    )
                                }
                                Box {
                                    IconButton(onClick = { moreMenuExpanded = true }) {
                                        SymbolIcon(
                                            name = "more_vert",
                                            contentDescription = stringResource(R.string.more),
                                            size = 24.dp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = moreMenuExpanded,
                                        onDismissRequest = { moreMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.more_menu_budget)) },
                                            leadingIcon = {
                                                SymbolIcon(
                                                    name = "savings",
                                                    contentDescription = null,
                                                    size = 20.dp
                                                )
                                            },
                                            onClick = {
                                                moreMenuExpanded = false
                                                onNavigateToBudgetList()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.more_menu_category)) },
                                            leadingIcon = {
                                                SymbolIcon(
                                                    name = "category",
                                                    contentDescription = null,
                                                    size = 20.dp
                                                )
                                            },
                                            onClick = {
                                                moreMenuExpanded = false
                                                onNavigateToCategoryList()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (!appState.isSearchMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = onNavigateToAiBookkeeping,
                        shape = MaterialTheme.shapes.large
                    ) {
                        SymbolIcon(
                            name = "smart_toy",
                            filled = true,
                            contentDescription = "AI 记账",
                            size = 24.dp
                        )
                    }
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

    if (filterSheetVisible) {
        FilterSheet(
            categories = appState.categories,
            currentParams = SearchParams(),
            onApply = { params ->
                onUpdateSearchParams(params)
                filterSheetVisible = false
            },
            onReset = {
                onResetSearchParams()
                filterSheetVisible = false
            },
            onDismiss = { filterSheetVisible = false }
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
        if (appState.isSearchMode) {
            // 搜索模式：显示搜索结果统计卡片
            item(key = "search_overview") {
                SearchResultOverviewCard(
                    recordCount = appState.searchResultCount,
                    overviewMetrics = appState.overviewMetrics,
                    currencySymbol = appState.currencySymbol,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        } else {
            item(key = "overview") {
                RecordOverviewCard(
                    selectedYearMonth = selectedYearMonth,
                    overviewMetrics = appState.overviewMetrics,
                    currencySymbol = appState.currencySymbol,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (appState.recordGroups.isEmpty()) {
            item(key = "empty") {
                if (appState.isSearchMode) {
                    EmptySearchResult(
                        modifier = Modifier.fillParentMaxSize()
                    )
                } else {
                    val hasAnyRecords = appState.monthlySummaries.isNotEmpty()
                    EmptyRecordList(
                        isMonthEmpty = hasAnyRecords,
                        yearMonth = selectedYearMonth,
                        errorMessage = appState.errorMessage,
                        modifier = Modifier.fillParentMaxSize()
                    )
                }
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

@Composable
private fun formatDisplayDate(dateStr: String): String {
    val parsed = try {
        LocalDate.parse(dateStr) to LocalDate.now()
    } catch (_: Exception) {
        return dateStr
    }
    val (date, today) = parsed
    return when {
        date == today -> stringResource(R.string.date_today)
        date == today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_month_day_format)))
    }
}

@Composable
private fun EmptySearchResult(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SymbolIcon(
                name = "search_off",
                contentDescription = null,
                size = 48.dp,
                modifier = Modifier.alpha(0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.search_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
