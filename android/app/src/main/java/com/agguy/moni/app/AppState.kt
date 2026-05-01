package com.agguy.moni.app

import com.agguy.moni.core.CoreAppState
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord

data class AppState(
    val records: List<CoreRecord> = emptyList(),
    val categories: List<CoreCategory> = emptyList(),
    val greetingMessage: String = "",
    val errorMessage: String? = null,
    val currencySymbol: String = "¥"
)

fun CoreAppState.toAppState(): AppState = AppState(
    records = records,
    categories = categories,
    greetingMessage = greetingMessage,
    errorMessage = ui.errorMessage,
    currencySymbol = settings.currencySymbol
)
