@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreOverviewMetrics
import com.agguy.moni.core.util.formatAmount

/**
 * 搜索结果概览卡片，展示搜索结果的统计信息。
 */
@Composable
fun SearchResultOverviewCard(
    recordCount: Int,
    overviewMetrics: CoreOverviewMetrics?,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.search_result_count, recordCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (overviewMetrics != null && (overviewMetrics.monthIncome > 0 || overviewMetrics.monthExpense > 0)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (overviewMetrics.monthIncome > 0) {
                    Column {
                        Text(
                            text = stringResource(R.string.record_income),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currencySymbol}${formatAmount(overviewMetrics.monthIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.incomeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (overviewMetrics.monthExpense > 0) {
                    Column {
                        Text(
                            text = stringResource(R.string.record_expense),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currencySymbol}${formatAmount(overviewMetrics.monthExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.expenseRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (overviewMetrics.monthBalance != 0L) {
                    Column {
                        Text(
                            text = stringResource(R.string.record_balance),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currencySymbol}${formatAmount(overviewMetrics.monthBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (overviewMetrics.monthBalance >= 0) {
                                MaterialTheme.colorScheme.incomeGreen
                            } else {
                                MaterialTheme.colorScheme.expenseRed
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
