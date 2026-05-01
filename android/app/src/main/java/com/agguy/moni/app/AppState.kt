package com.agguy.moni.app

import com.agguy.moni.core.CoreAppState
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreCategoryBreakdown
import com.agguy.moni.core.CoreMonthlySummary
import com.agguy.moni.core.CoreRecord

data class AppState(
    val records: List<CoreRecord> = emptyList(),
    val categories: List<CoreCategory> = emptyList(),
    val monthlySummaries: List<CoreMonthlySummary> = emptyList(),
    val currentMonthBreakdown: List<CoreCategoryBreakdown> = emptyList(),
    val errorMessage: String? = null,
    val currencySymbol: String = "¥"
)

fun CoreAppState.toAppState(): AppState = AppState(
    records = records,
    categories = categories,
    monthlySummaries = monthlySummaries,
    currentMonthBreakdown = currentMonthBreakdown,
    errorMessage = ui.errorMessage,
    currencySymbol = settings.currencySymbol
)
