package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.ui.budget.BudgetProgressBar
import com.agguy.moni.app.ui.budget.BudgetStatusLabel
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.util.formatAmount

/**
 * 统计页预算健康概览卡片。
 *
 * 预算存在才渲染；无预算时 StatsScreen 不显示此卡片。
 */
@Composable
fun StatsBudgetCard(
    budgets: List<CoreBudget>,
    currencySymbol: String,
    yearMonth: String,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (totalBudget, categoryBudgets, riskCount, sortedRisks) = remember(budgets) {
        val (totals, categories) = budgets.partition { it.categoryId == null }
        val risks = categories
            .sortedByDescending { it.percentage }
            .take(3)
        val riskCnt = categories.count { it.status == "critical" || it.status == "overrun" }
        Quadruple(totals.firstOrNull(), categories, riskCnt, risks)
    }

    val monthLabel = remember(yearMonth) { formatMonthOnly(yearMonth) }

    MoniCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        variant = MoniCardVariant.Tonal,
        onClick = onNavigateToBudget
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.budget_month_format, monthLabel),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.action_view_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (totalBudget != null) {
                TotalBudgetHeader(totalBudget, currencySymbol)
            } else {
                CategoryBudgetHeader(riskCount, categoryBudgets)
            }

            if (sortedRisks.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedRisks.forEach { budget ->
                        BudgetRiskItem(budget, currencySymbol)
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun TotalBudgetHeader(
    totalBudget: CoreBudget,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val percentageText = "${(totalBudget.percentage * 100).toInt()}%"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.budget_remaining_format, "$currencySymbol${formatAmount(totalBudget.remainingCents)}"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = when (totalBudget.status) {
                "overrun" -> MaterialTheme.colorScheme.expenseRed
                "critical" -> Color(0xFFFFA726)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = stringResource(R.string.budget_used_percent_format, "$percentageText") + " · " + stringResource(R.string.budget_total_format, "$currencySymbol${formatAmount(totalBudget.amountCents)}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BudgetProgressBar(
            percentage = totalBudget.percentage,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CategoryBudgetHeader(
    riskCount: Int,
    categoryBudgets: List<CoreBudget>,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        riskCount > 0 -> stringResource(R.string.status_items_attention, riskCount)
        else -> stringResource(R.string.status_good)
    }

    val statusColor = when {
        riskCount > 0 -> MaterialTheme.colorScheme.expenseRed
        else -> MaterialTheme.colorScheme.primary
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = statusColor,
        modifier = modifier
    )
}

@Composable
private fun BudgetRiskItem(
    budget: CoreBudget,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val name = budget.categoryName ?: stringResource(R.string.status_unknown_category)
    val percentageText = "${(budget.percentage * 100).toInt()}%"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium
                )
                BudgetStatusLabel(status = budget.status)
            }

            val rightText = when (budget.status) {
                "overrun" -> stringResource(R.string.budget_spent_format, "$currencySymbol${formatAmount(kotlin.math.abs(budget.remainingCents))}")
                else -> stringResource(R.string.budget_used_percent_format, percentageText)
            }
            Text(
                text = rightText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BudgetProgressBar(
            percentage = budget.percentage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}
