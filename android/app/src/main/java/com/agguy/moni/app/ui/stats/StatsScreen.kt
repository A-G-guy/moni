@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.util.formatAmount
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 统计仪表盘页面。
 *
 * Material 3 Expressive 改造点：
 * - 标题用 [androidx.compose.material3.Typography.headlineSmall]，强化 hero 字号；
 * - 顶部月度概览改为 [HorizontalCenteredHeroCarousel]：用户左右滑动浏览近 N 个月的概览，
 *   居中项的高亮 + mask 的"翻页感"是 Expressive 的招牌 hero 元素；
 * - 选中月份会自动驱动下方饼图重载（通过 [androidx.compose.material3.carousel.CarouselState.currentItem] +
 *   [snapshotFlow]），实现"翻页同步"语义；
 * - 首次加载（数据为空）时显示 [ContainedLoadingIndicator]，是 Expressive 推荐的
 *   "morphing shape" 加载占位，比 CircularProgressIndicator 更动感。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        onDispatch(CoreIntent.StatsMonthlySummary(months = 6))
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

/**
 * Expressive [HorizontalCenteredHeroCarousel] 月度概览翻页器。
 *
 * 月份按时间正序排列（最旧→最新），初始 anchor 是最新月（即数组最后一项）。
 * 用户左右拖动后 [androidx.compose.material3.carousel.CarouselState.currentItem] 会更新，
 * 通过 [snapshotFlow] + [distinctUntilChanged] 节流后回调 [onMonthChanged]，
 * 用于驱动下方饼图重载。
 */
@Composable
private fun MonthSummaryCarousel(
    summaries: List<CoreMonthlySummary>,
    currencySymbol: String,
    onMonthChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // initialItem 必须 < itemCount；数据为空时本 composable 不会被调用，故 lastIndex >= 0
    val initialIndex = remember(summaries.size) {
        (summaries.size - 1).coerceAtLeast(0)
    }
    val carouselState = rememberCarouselState(initialItem = initialIndex) { summaries.size }

    LaunchedEffect(carouselState, summaries) {
        snapshotFlow { carouselState.currentItem }
            .distinctUntilChanged()
            .collect { index ->
                summaries.getOrNull(index)?.let { onMonthChanged(it.yearMonth) }
            }
    }

    HorizontalCenteredHeroCarousel(
        state = carouselState,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) { index ->
        val summary = summaries[index]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.large)
        ) {
            HeroSummaryCard(
                summary = summary,
                currencySymbol = currencySymbol,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Hero 月度概览卡片：carousel 居中项的视觉主角。
 */
@Composable
private fun HeroSummaryCard(
    summary: CoreMonthlySummary,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    MoniCard(
        modifier = modifier,
        variant = MoniCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatYearMonth(summary.yearMonth),
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "结余",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${currencySymbol}${formatAmount(summary.balanceCents)}",
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = if (summary.balanceCents >= 0) {
                        MaterialTheme.colorScheme.incomeGreen
                    } else {
                        MaterialTheme.colorScheme.expenseRed
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AmountStat(
                    label = "收入",
                    amountCents = summary.incomeCents,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.incomeGreen,
                    modifier = Modifier.weight(1f)
                )
                AmountStat(
                    label = "支出",
                    amountCents = summary.expenseCents,
                    currencySymbol = currencySymbol,
                    color = MaterialTheme.colorScheme.expenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AmountStat(
    label: String,
    amountCents: Long,
    currencySymbol: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${currencySymbol}${formatAmount(amountCents)}",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = color
        )
    }
}

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

private fun formatYearMonth(yearMonth: String): String {
    // yearMonth 形如 "2026-05" → "2026 年 5 月"
    val parts = yearMonth.split('-')
    return if (parts.size == 2) {
        "${parts[0]} 年 ${parts[1].toIntOrNull() ?: parts[1]} 月"
    } else {
        yearMonth
    }
}
