package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 统计仪表盘页面。
 *
 * 展示本月收支结余概览、月度趋势柱状图和支出分类饼图。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    LaunchedEffect(Unit) {
        onDispatch(CoreIntent.StatsMonthlySummary(months = 6))
        onDispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = currentYearMonth))
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("统计") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 本月概览卡片
            MonthSummaryCards(
                summaries = appState.monthlySummaries,
                currentYearMonth = currentYearMonth,
                currencySymbol = appState.currencySymbol
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 月度柱状图
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MonthlyBarChart(
                        summaries = appState.monthlySummaries,
                        currencySymbol = appState.currencySymbol
                    )
                }
            }

            // 分类饼图
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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

@Composable
private fun MonthSummaryCards(
    summaries: List<CoreMonthlySummary>,
    currentYearMonth: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val currentSummary = summaries.find { it.yearMonth == currentYearMonth }
        ?: CoreMonthlySummary(
            yearMonth = currentYearMonth,
            incomeCents = 0,
            expenseCents = 0,
            balanceCents = 0
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            label = "本月收入",
            amount = currentSummary.incomeCents,
            currencySymbol = currencySymbol,
            color = MaterialTheme.colorScheme.incomeGreen,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "本月支出",
            amount = currentSummary.expenseCents,
            currencySymbol = currencySymbol,
            color = MaterialTheme.colorScheme.expenseRed,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "结余",
            amount = currentSummary.balanceCents,
            currencySymbol = currencySymbol,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Long,
    currencySymbol: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${currencySymbol}${formatAmount(amount)}",
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

