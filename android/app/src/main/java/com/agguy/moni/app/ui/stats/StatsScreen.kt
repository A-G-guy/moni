@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.util.formatAmount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

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
 *
 * 月份范围加载到 36（前后约一年的数据），并由 carousel + 月份选择 Sheet 双轨切换：
 * - Carousel：渐进浏览相邻月份；
 * - [MonthPickerSheet]：点击主卡片标题旁的箭头唤起，用网格直达任意月份；
 * - "回到本月" FAB：当焦点偏离本月超过 [MonthsAwayThreshold] 时，AnimatedVisibility 浮现。
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

/**
 * Expressive [HorizontalCenteredHeroCarousel] 月度概览翻页器。
 *
 * 月份按时间正序排列（最旧→最新），初始 anchor 是当前月（不存在则退回最新一项）。
 * 用户左右拖动后 [androidx.compose.material3.carousel.CarouselState.currentItem] 会更新，
 * 通过 [snapshotFlow] + [distinctUntilChanged] 节流后回调 [onMonthChanged]，
 * 用于驱动下方饼图重载。
 *
 * **视觉降噪**：
 * 通过 [CarouselItemScope.carouselItemDrawInfo] 在 [graphicsLayer] 阶段读取 `size / maxSize`，
 * 主卡居中态保持 100% 不透明并完整展示标题 + 收支详情；侧卡只显示标题，详情 alpha 衰减到接近 0，
 * 整张卡片再叠加一层主体 alpha 渐变，从而避免被 mask 截断的"残缺数字"喧宾夺主。
 *
 * **大力滑动**：
 * [CarouselDefaults.multiBrowseFlingBehavior] 替代默认 single advance，允许"滑一下飞过多张"。
 *
 * **快捷跳转 + 复位**：
 * - 主卡片标题旁悬停一个 ▾ 提示，点击唤起 [MonthPickerSheet]，可直达任意年月；
 * - 当 carouselState.currentItem 与当前月索引偏离 > [MonthsAwayThreshold] 时，
 *   右上角浮出小型 FAB"今"，点击 [androidx.compose.material3.carousel.CarouselState.animateScrollToItem]
 *   回到当前月。
 *
 * **边缘态（stuck state）设计**：
 * 滑到首/末月时，Material 3 内部会通过 keyline 移位生成
 * `[anchor, large, small, small, anchor]` 或 `[anchor, small, small, large, anchor]`，大卡片自然贴到容器边缘，
 * 另一侧露出两张完整小卡。由于移位仅搬动 keyline 位置而不改 size，
 * 边缘态的 small 与居中态左右两侧的 small **尺寸完全一致**。
 *
 * 注意 carousel 自身的 [PaddingValues] contentPadding 在 stuck state 下会让 large 距 carousel 边 = padding 值
 * （Material 3 内部 createShiftedKeylineListForContentPadding 行为），导致大卡未贴屏幕边；
 * 因此这里把 contentPadding 设为 0，把水平间距改用外层 Modifier.padding 实现，
 * stuck state 时 large 真正贴 carousel 容器边，整体到屏幕边的距离 = 外层 padding，左右严格对称。
 *
 * 为避免 Material 3 在默认 40-56dp 区间内根据 viewport 自适应、导致
 * 不同设备宽度上 small 尺寸有微小差异，这里显式锁定 minSmallItemWidth = maxSmallItemWidth = [SmallItemWidth]，
 * 跨设备视觉一致。
 */
@Composable
private fun MonthSummaryCarousel(
    summaries: List<CoreMonthlySummary>,
    currencySymbol: String,
    currentYearMonth: String,
    onMonthChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // initialItem 必须 < itemCount；数据为空时本 composable 不会被调用，故 lastIndex >= 0
    val currentMonthIndex = remember(summaries, currentYearMonth) {
        summaries.indexOfFirst { it.yearMonth == currentYearMonth }
            .takeIf { it >= 0 }
            ?: (summaries.size - 1).coerceAtLeast(0)
    }
    val carouselState = rememberCarouselState(initialItem = currentMonthIndex) { summaries.size }
    val coroutineScope = rememberCoroutineScope()
    var sheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(carouselState, summaries) {
        snapshotFlow { carouselState.currentItem }
            .distinctUntilChanged()
            .collect { index ->
                summaries.getOrNull(index)?.let { onMonthChanged(it.yearMonth) }
            }
    }

    val showJumpToToday by remember(currentMonthIndex) {
        derivedStateOf {
            currentMonthIndex >= 0 &&
                (carouselState.currentItem - currentMonthIndex).absoluteValue > MonthsAwayThreshold
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalCenteredHeroCarousel(
            state = carouselState,
            itemSpacing = CarouselItemSpacing,
            flingBehavior = CarouselDefaults.multiBrowseFlingBehavior(state = carouselState),
            // contentPadding 设为 0：Material 3 在 stuck state 下，
            // 会用 createShiftedKeylineListForContentPadding 把 large 中心向 padding 内侧偏移，
            // 让 large 距离 carousel 容器边缘 = afterContentPadding，视觉上"没贴到边"。
            // 改为把水平 padding 加到 carousel 外层 Modifier，让 carousel 容器自身就是缩小后的 viewport，
            // stuck state 时 large 自然贴 carousel 边、整体距屏幕边就是外层 padding，左右一致。
            contentPadding = PaddingValues(0.dp),
            // 显式锁定 small 尺寸：Material 3 在 stuck state 下通过 move 重排 keyline 但不改 size，
            // min == max 进一步避免 Material 在默认区间内做 viewport 适配，使中间态与边缘态严格相等。
            minSmallItemWidth = SmallItemWidth,
            maxSmallItemWidth = SmallItemWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CarouselContentPadding)
                .height(180.dp)
        ) { index ->
            val summary = summaries[index]
            // 在 graphicsLayer / 自定义 alpha modifier 中读取 carouselItemDrawInfo，
            // 这些回调发生在 draw 阶段，状态变化只触发重绘而不会引起组合树重组。
            val itemScope: CarouselItemScope = this
            val focusRatioProvider = { focusRatioOf(itemScope) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.large)
            ) {
                HeroSummaryCard(
                    summary = summary,
                    currencySymbol = currencySymbol,
                    detailAlphaProvider = { detailAlphaFromRatio(focusRatioProvider()) },
                    cardAlphaProvider = { cardAlphaFromRatio(focusRatioProvider()) },
                    onTitleClick = { sheetVisible = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showJumpToToday,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = CarouselContentPadding + 8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        carouselState.animateScrollToItem(currentMonthIndex)
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = "今",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }

    if (sheetVisible) {
        MonthPickerSheet(
            availableYearMonths = remember(summaries) { summaries.map { it.yearMonth }.toSet() },
            currentYearMonth = summaries.getOrNull(carouselState.currentItem)?.yearMonth
                ?: currentYearMonth,
            todayYearMonth = currentYearMonth,
            onYearMonthSelected = { yearMonth ->
                val targetIndex = summaries.indexOfFirst { it.yearMonth == yearMonth }
                if (targetIndex >= 0) {
                    coroutineScope.launch {
                        carouselState.animateScrollToItem(targetIndex)
                    }
                }
                sheetVisible = false
            },
            onDismiss = { sheetVisible = false }
        )
    }
}

/** 把当前 item 的主轴 size / maxSize 折算成 `[0,1]` 区间的聚焦比例。 */
private fun focusRatioOf(scope: CarouselItemScope): Float {
    val info = scope.carouselItemDrawInfo
    val maxSize = info.maxSize.coerceAtLeast(1f)
    return (info.size / maxSize).coerceIn(0f, 1f)
}

/** 整张卡片的透明度：从 0.45 渐变到 1，避免侧卡完全消失看不出"还有内容"。 */
private fun cardAlphaFromRatio(ratio: Float): Float =
    0.45f + 0.55f * ratio

/** 卡片次要内容（结余/收入/支出）的透明度：聚焦阈值 0.85 后才显现，避免侧卡显示残缺数字。 */
private fun detailAlphaFromRatio(ratio: Float): Float =
    ((ratio - DetailRevealThreshold) / (1f - DetailRevealThreshold)).coerceIn(0f, 1f)

private val SmallItemWidth = 56.dp
private val CarouselItemSpacing = 12.dp
private val CarouselContentPadding = 16.dp
private const val MonthsToLoad = 36
private const val MonthsAwayThreshold = 2
private const val DetailRevealThreshold = 0.85f

/**
 * Hero 月度概览卡片：carousel 居中项的视觉主角。
 *
 * @param detailAlphaProvider draw 阶段读取，在侧卡时让结余/收入/支出几乎完全淡出。
 * @param cardAlphaProvider draw 阶段读取，控制整张卡片相对透明度。
 * @param onTitleClick 点击主卡片标题（含 ▾ 暗示）时唤起月份选择面板。
 */
@Composable
private fun HeroSummaryCard(
    summary: CoreMonthlySummary,
    currencySymbol: String,
    detailAlphaProvider: () -> Float,
    cardAlphaProvider: () -> Float,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoniCard(
        modifier = modifier.graphicsLayer { alpha = cardAlphaProvider() },
        variant = MoniCardVariant.Elevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTitleClick)
            ) {
                Text(
                    text = formatYearMonth(summary.yearMonth),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // ▾ 用 unicode 字符渲染，避免引入额外 icon 资源；侧卡时跟随 detailAlpha 一起隐藏。
                // 用 graphicsLayer 而非 Modifier.alpha：alpha 形参在 composition 阶段读取 state 会触发每帧重组；
                // graphicsLayer 块在 draw 阶段执行，读 carouselItemDrawInfo 仅触发重绘。
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { alpha = detailAlphaProvider() }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.graphicsLayer { alpha = detailAlphaProvider() }
            ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = detailAlphaProvider() },
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
