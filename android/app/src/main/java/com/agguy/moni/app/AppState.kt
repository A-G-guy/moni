package com.agguy.moni.app

import com.agguy.moni.core.CoreAppState

data class AppState(
    val greetingMessage: String = "",
    val errorMessage: String? = null
)

fun CoreAppState.toAppState(): AppState = AppState(
    greetingMessage = greetingMessage,
    errorMessage = ui.errorMessage
)
