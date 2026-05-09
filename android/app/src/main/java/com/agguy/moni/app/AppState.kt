package com.agguy.moni.app

import com.agguy.moni.core.CoreAppState
import com.agguy.moni.core.CoreBudget
import com.agguy.moni.core.CoreBudgetCheckResult
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreCategoryBreakdown
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.CoreOverviewMetrics
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.CoreRecordGroup

data class AppState(
    val records: List<CoreRecord> = emptyList(),
    val recordGroups: List<CoreRecordGroup> = emptyList(),
    val categories: List<CoreCategory> = emptyList(),
    val monthlySummaries: List<CoreMonthlySummary> = emptyList(),
    val currentMonthBreakdown: List<CoreCategoryBreakdown> = emptyList(),
    val budgets: List<CoreBudget> = emptyList(),
    val budgetCheckResult: CoreBudgetCheckResult? = null,
    val overviewMetrics: CoreOverviewMetrics? = null,
    val errorMessage: String? = null,
    val errorKey: String? = null,
    val errorArgs: List<String> = emptyList(),
    val currencySymbol: String = "¥",
    val isSearchMode: Boolean = false,
    val searchKeyword: String = "",
    val searchResultCount: Int = 0,
)

/**
 * 搜索筛选参数。
 */
data class SearchParams(
    val recordType: String? = null,
    val categoryIds: List<Long> = emptyList(),
    val amountMin: Long? = null,
    val amountMax: Long? = null,
    val dateStart: Long? = null,
    val dateEnd: Long? = null,
    val sortBy: String = "created_at",
    val sortOrder: String = "desc",
)

fun CoreAppState.toAppState(): AppState = AppState(
    records = records,
    recordGroups = recordGroups,
    categories = categories,
    monthlySummaries = monthlySummaries,
    currentMonthBreakdown = currentMonthBreakdown,
    budgets = budgets,
    budgetCheckResult = budgetCheckResult,
    overviewMetrics = overviewMetrics,
    searchResultCount = searchResultCount,
    errorMessage = ui.errorMessage,
    errorKey = ui.errorKey,
    errorArgs = ui.errorArgs,
    currencySymbol = settings.currencySymbol
)
