@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.core.CoreIntent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 统计仪表盘页面。
 *
 * Material 3 Expressive 改造点：
 * - 标题用 [androidx.compose.material3.Typography.headlineSmall]，强化 hero 字号；
 * - 顶部月度概览改为 [MonthSummaryCarousel]：用户左右滑动浏览近 N 个月的概览，
 *   居中项的高亮 + mask 的"翻页感"是 Expressive 的招牌 hero 元素；
 * - 选中月份会自动驱动下方饼图重载（实现细节见 [MonthSummaryCarousel] 的 KDoc）；
 * - 首次加载（数据为空）时显示 [ContainedLoadingIndicator]，是 Expressive 推荐的
 *   "morphing shape" 加载占位，比 CircularProgressIndicator 更动感。
 *
 * 月份联动：本页通过 [selectedYearMonth] 与账单页共享当前选中月份；
 * [MonthSummaryCarousel] 既响应外部月份变化自动滚动，也向外部通知用户手动滑动产生的月份切换。
 *
 * 返回导航层级（由内到外）：
 * 1. 页面级：根页面 — 系统默认退出应用
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    appState: AppState,
    selectedYearMonth: String,
    onDispatch: (CoreIntent) -> Unit,
    onSelectYearMonth: (String) -> Unit,
    onNavigateToBudget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    var aggregateByParent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (appState.monthlySummaries.isEmpty()) {
            onDispatch(CoreIntent.StatsMonthlySummary(months = MonthsToLoad))
        }
    }

    LaunchedEffect(selectedYearMonth, aggregateByParent) {
        onDispatch(
            CoreIntent.StatsCategoryBreakdown(
                yearMonth = selectedYearMonth,
                aggregateByParent = aggregateByParent
            )
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (appState.monthlySummaries.isEmpty()) {
                LoadingPlaceholder()
            } else {
                MonthSummaryCarousel(
                    summaries = appState.monthlySummaries,
                    currencySymbol = appState.currencySymbol,
                    currentYearMonth = todayYearMonth,
                    selectedYearMonth = selectedYearMonth,
                    onMonthChanged = { yearMonth ->
                        onDispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = yearMonth))
                        onSelectYearMonth(yearMonth)
                    }
                )
            }

            if (appState.budgets.isNotEmpty()) {
                StatsBudgetCard(
                    budgets = appState.budgets,
                    currencySymbol = appState.currencySymbol,
                    yearMonth = selectedYearMonth,
                    onNavigateToBudget = onNavigateToBudget
                )
            }

            MoniCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MonthlyBarChart(
                        summaries = appState.monthlySummaries,
                        currencySymbol = appState.currencySymbol
                    )
                }
            }

            MoniCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stats_expense_breakdown),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !aggregateByParent,
                                onClick = { aggregateByParent = false },
                                label = { Text(stringResource(R.string.stats_secondary_category)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = aggregateByParent,
                                onClick = { aggregateByParent = true },
                                label = { Text(stringResource(R.string.stats_primary_category)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    CategoryPieChart(
                        breakdowns = appState.currentMonthBreakdown,
                        currencySymbol = appState.currencySymbol
                    )
                }
            }
        }
    }
}

private const val MonthsToLoad = 36

/**
 * 数据加载占位：用 Expressive [ContainedLoadingIndicator] 替代旧的 CircularProgressIndicator。
 */
@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
    }
}
