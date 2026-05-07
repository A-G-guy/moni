package com.agguy.moni.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.components.MoniCard
import com.agguy.moni.app.components.MoniCardVariant
import com.agguy.moni.app.ui.budget.BudgetProgressBar
import com.agguy.moni.app.ui.budget.BudgetStatusLabel
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.util.formatAmount

/**
 * 统计页预算健康概览卡片。
 *
 * 回答三个问题：这个月整体是否安全？哪里最紧张？有多少项需要关注？
 * 预算存在才渲染；无预算时 StatsScreen 不显示此卡片。
 *
 * @param budgets 当前月份的预算列表（已含实时计算字段）
 * @param currencySymbol 货币符号，如 "¥"
 * @param yearMonth 当前选中年月，如 "2026-05"
 * @param onNavigateToBudget 点击「查看全部预算」的回调
 */
@Composable
fun StatsBudgetCard(
    budgets: List<CoreBudget>,
    currencySymbol: String,
    yearMonth: String,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (totalBudgets, categoryBudgets) = budgets.partition { it.categoryId == null }
    val totalBudget = totalBudgets.firstOrNull()

    val riskCount = categoryBudgets.count { it.status == "critical" || it.status == "overrun" }
    val sortedRisks = categoryBudgets
        .sortedByDescending { it.percentage }
        .take(3)

    val monthLabel = formatYearMonth(yearMonth)

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
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${monthLabel}预算",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "查看全部预算 →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 主状态区
            if (totalBudget != null) {
                TotalBudgetHeader(totalBudget, currencySymbol)
            } else {
                CategoryBudgetHeader(riskCount, categoryBudgets)
            }

            // 风险列表
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
            text = "剩余 $currencySymbol${formatAmount(totalBudget.remainingCents)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = when (totalBudget.status) {
                "overrun" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = "已用 $percentageText · 预算 $currencySymbol${formatAmount(totalBudget.amountCents)}",
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
        riskCount > 0 -> "${riskCount} 项需要留意"
        categoryBudgets.isNotEmpty() -> "预算状态良好"
        else -> ""
    }

    val statusColor = when {
        riskCount > 0 -> MaterialTheme.colorScheme.error
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
    val name = budget.categoryName ?: "未知分类"
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
                "overrun" -> "超支 $currencySymbol${formatAmount(kotlin.math.abs(budget.remainingCents))}"
                else -> "已用 $percentageText"
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

/**
 * 将 "yyyy-MM" 格式化为中文月份标题，如 "2026-05" → "5月"。
 */
private fun formatYearMonth(yearMonth: String): String {
    return try {
        val month = yearMonth.substringAfter("-").toInt()
        "${month}月"
    } catch (_: Exception) {
        "本月"
    }
}
