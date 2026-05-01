package com.agguy.moni.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object RecordList : Screen()

    @Serializable
    data class RecordDetail(val recordId: Long? = null) : Screen()

    @Serializable
    data object CategoryList : Screen()

    @Serializable
    data object Stats : Screen()

    @Serializable
    data object Settings : Screen()
}
