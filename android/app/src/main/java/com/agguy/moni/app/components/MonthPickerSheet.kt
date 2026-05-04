@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * 月份快捷跳转面板。
 *
 * 顶部为年份 [FilterChip] 横向滚动条，下方 3 行 × 4 列的月份网格。
 * 仅允许选择 [availableYearMonths] 中存在数据的月份；其他月份变灰禁用，
 * 避免选中后跳转到 carousel 不存在的位置（当前 [com.agguy.moni.core.CoreIntent.StatsMonthlySummary]
 * 不支持按中心月份动态加载，因此把"未记账"月份的快速跳转入口直接关闭最直白）。
 *
 * 当前月用主色高亮；今日所在月再叠加一个细描边以方便定位。
 *
 * @param availableYearMonths 数据源中实际存在的 `yyyy-MM` 集合
 * @param currentYearMonth 当前 carousel 焦点月，用作"高亮选中态"
 * @param todayYearMonth 当下系统时间所在月，用于追加"今天"标记
 * @param onYearMonthSelected 选中后的回调，调用方负责 dismiss
 * @param onDismiss 用户下拉/点击背景导致关闭时回调
 */
@Composable
fun MonthPickerSheet(
    availableYearMonths: Set<String>,
    currentYearMonth: String,
    todayYearMonth: String,
    onYearMonthSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialYear = remember(currentYearMonth) {
        parseYear(currentYearMonth) ?: LocalDate.now().year
    }
    var selectedYear by remember { mutableIntStateOf(initialYear) }

    val years = remember(availableYearMonths, todayYearMonth) {
        buildSet {
            availableYearMonths.mapNotNullTo(this) { parseYear(it) }
            parseYear(todayYearMonth)?.let { add(it) }
        }.sortedDescending()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "跳转到月份",
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(years) { year ->
                    FilterChip(
                        selected = year == selectedYear,
                        onClick = { selectedYear = year },
                        label = { Text("$year 年") }
                    )
                }
            }

            MonthGrid(
                year = selectedYear,
                availableYearMonths = availableYearMonths,
                currentYearMonth = currentYearMonth,
                todayYearMonth = todayYearMonth,
                onMonthSelected = onYearMonthSelected
            )
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    availableYearMonths: Set<String>,
    currentYearMonth: String,
    todayYearMonth: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (rowStart in 0 until 12 step MonthsPerRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (monthIndex in rowStart until rowStart + MonthsPerRow) {
                    val month = monthIndex + 1
                    val yearMonth = formatYearMonth(year, month)
                    val available = yearMonth in availableYearMonths
                    val isFocus = yearMonth == currentYearMonth
                    val isToday = yearMonth == todayYearMonth
                    MonthCell(
                        label = "${month}月",
                        available = available,
                        isFocus = isFocus,
                        isToday = isToday,
                        onClick = { if (available) onMonthSelected(yearMonth) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    available: Boolean,
    isFocus: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val variant = when {
        isFocus -> MoniCardVariant.Filled
        isToday -> MoniCardVariant.Outlined
        else -> MoniCardVariant.Tonal
    }
    MoniCard(
        modifier = modifier,
        variant = variant,
        onClick = if (available) onClick else null
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            val textColor = when {
                isFocus -> MaterialTheme.colorScheme.primary
                !available -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = label,
                style = if (isFocus) MaterialTheme.typography.titleMediumEmphasized
                        else MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}

private const val MonthsPerRow = 4

private fun parseYear(yearMonth: String): Int? =
    yearMonth.split('-').firstOrNull()?.toIntOrNull()

private fun formatYearMonth(year: Int, month: Int): String =
    "%04d-%02d".format(year, month)
