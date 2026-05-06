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

    @Serializable
    data object DeveloperOptions : Screen()

    @Serializable
    data object ArchivedCategories : Screen()

    @Serializable
    data object DevLog : Screen()

    @Serializable
    data object DataManagement : Screen()

    @Serializable
    data object ThemeSettings : Screen()
}
