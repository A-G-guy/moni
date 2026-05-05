@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.components.MonthPickerSheet
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.util.formatAmount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Expressive [HorizontalCenteredHeroCarousel] 月度概览翻页器。
 *
 * 月份按时间正序排列（最旧→最新），初始 anchor 是当前月（不存在则退回最新一项）。
 * 用户左右拖动后 [androidx.compose.material3.carousel.CarouselState.currentItem] 会更新,
 * 通过 [snapshotFlow] + [distinctUntilChanged] 节流后回调 [onMonthChanged]，
 * 用于驱动下方饼图重载。
 *
 * **聚焦/侧卡双 layout**：
 * - 聚焦 ([HeroSummaryCard])：完整月份标题 + 结余 + 收入支出双列；点击整张卡（含水波纹）唤起 [MonthPickerSheet]。
 * - 侧卡 ([MiniSummaryCard])：仅"X月" + 结余金额，避免被 mask 截断的"残缺数字"喧宾夺主；
 *   点击通过 [androidx.compose.material3.carousel.CarouselState.animateScrollToItem] 滚动到该月。
 *
 * 通过比较 itemContent 内的 `index` 与 [androidx.compose.material3.carousel.CarouselState.currentItem]
 * 决定渲染 Hero 还是 Mini。currentItem 是 IntState，会在 settle 后切换，
 * 触发对应 item 在 composition 阶段切换 layout，无需读取 carouselItemDrawInfo。
 *
 * **大力滑动**：
 * [CarouselDefaults.multiBrowseFlingBehavior] 替代默认 single advance，允许"滑一下飞过多张"。
 *
 * **复位**：
 * 当 carouselState.currentItem 与当前月索引偏离 > [MonthsAwayThreshold] 时，
 * 右上角浮出小型 FAB"今"，点击 [androidx.compose.material3.carousel.CarouselState.animateScrollToItem]
 * 回到当前月。
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
internal fun MonthSummaryCarousel(
    summaries: List<CoreMonthlySummary>,
    currencySymbol: String,
    currentYearMonth: String,
    selectedYearMonth: String,
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

    // 监听外部 selectedYearMonth 变化，自动滚动到对应月份
    LaunchedEffect(selectedYearMonth, summaries) {
        val targetIndex = summaries.indexOfFirst { it.yearMonth == selectedYearMonth }
        if (targetIndex >= 0 && carouselState.currentItem != targetIndex) {
            coroutineScope.launch {
                carouselState.animateScrollToItem(targetIndex)
            }
        }
    }

    // 监听 carousel 内部滑动，仅当新月份与外部不同时才回调，避免循环触发
    LaunchedEffect(carouselState, summaries) {
        snapshotFlow { carouselState.currentItem }
            .distinctUntilChanged()
            .collect { index ->
                summaries.getOrNull(index)?.let { summary ->
                    if (summary.yearMonth != selectedYearMonth) {
                        onMonthChanged(summary.yearMonth)
                    }
                }
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
            // contentPadding 设为 0：见函数 KDoc 中"stuck state 设计"说明
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
            // currentItem 是 IntState，settle 后切换到新索引，触发对应 item 在 composition 阶段切换 layout。
            val isFocused = index == carouselState.currentItem
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.large)
            ) {
                if (isFocused) {
                    HeroSummaryCard(
                        summary = summary,
                        currencySymbol = currencySymbol,
                        onClick = { sheetVisible = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MiniSummaryCard(
                        summary = summary,
                        currencySymbol = currencySymbol,
                        onClick = {
                            coroutineScope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
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
            currentYearMonth = selectedYearMonth,
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

private val SmallItemWidth = 56.dp
private val CarouselItemSpacing = 12.dp
private val CarouselContentPadding = 16.dp
private const val MonthsAwayThreshold = 2

/**
 * Hero 月度概览卡片：carousel 居中项的视觉主角。
 *
 * 整张卡片可点击，水波纹由 [MoniCard] 通过 `Modifier.clickable` 默认 ripple 提供，
 * 点击后唤起 [MonthPickerSheet] 直达任意月份。
 */
@Composable
private fun HeroSummaryCard(
    summary: CoreMonthlySummary,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoniCard(
        modifier = modifier,
        variant = MoniCardVariant.Elevated,
        onClick = onClick
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

/**
 * Mini 月度概览卡片：carousel 两侧的预览。
 *
 * 容器仅 [SmallItemWidth] 宽，主卡 layout 会被 mask 截断成"残缺数字"，
 * 因此 layout 主动精简：顶部"X月"，底部结余金额（[TextOverflow.Ellipsis] 兜底极端长度）。
 *
 * 整张卡片可点击（含水波纹），点击后通过
 * [androidx.compose.material3.carousel.CarouselState.animateScrollToItem]
 * 平滑滚动到对应月份，使其转为 [HeroSummaryCard]。
 */
@Composable
private fun MiniSummaryCard(
    summary: CoreMonthlySummary,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoniCard(
        modifier = modifier,
        variant = MoniCardVariant.Tonal,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatMonthOnly(summary.yearMonth),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${currencySymbol}${formatAmount(summary.balanceCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (summary.balanceCents >= 0) {
                    MaterialTheme.colorScheme.incomeGreen
                } else {
                    MaterialTheme.colorScheme.expenseRed
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

private fun formatYearMonth(yearMonth: String): String {
    // yearMonth 形如 "2026-05" → "2026 年 5 月"
    val parts = yearMonth.split('-')
    return if (parts.size == 2) {
        "${parts[0]} 年 ${parts[1].toIntOrNull() ?: parts[1]} 月"
    } else {
        yearMonth
    }
}

private fun formatMonthOnly(yearMonth: String): String {
    // yearMonth 形如 "2026-05" → "5月"，去掉年份让 56dp 侧卡能塞下
    val parts = yearMonth.split('-')
    return if (parts.size == 2) {
        "${parts[1].toIntOrNull() ?: parts[1]}月"
    } else {
        yearMonth
    }
}
