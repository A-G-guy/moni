@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(appState: AppState, onDispatch: (CoreIntent) -> Unit, modifier: Modifier = Modifier) {
    val currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        // 拉满前后一年共 ~24 个月的范围；多余配额留给历史数据，月份不足时数据库自然只返回有数据的月份。
        onDispatch(CoreIntent.StatsMonthlySummary(months = MonthsToLoad))
        onDispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = currentYearMonth))
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "统计",
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
                    currentYearMonth = currentYearMonth,
                    onMonthChanged = { yearMonth ->
                        onDispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = yearMonth))
                    }
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
