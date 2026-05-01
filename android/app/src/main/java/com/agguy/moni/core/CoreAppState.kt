package com.agguy.moni.core

import kotlinx.serialization.Serializable

@Serializable
data class CoreAppState(
    val records: List<CoreRecord> = emptyList(),
    val categories: List<CoreCategory> = emptyList(),
    val monthlySummaries: List<CoreMonthlySummary> = emptyList(),
    val currentMonthBreakdown: List<CoreCategoryBreakdown> = emptyList(),
    val settings: CoreSettings = CoreSettings(),
    val ui: CoreUiState = CoreUiState(),
    val greetingMessage: String = ""
)

@Serializable
data class CoreRecord(
    val id: Long,
    val amountCents: Long,
    val recordType: String,
    val categoryId: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CoreCategory(
    val id: Long,
    val name: String,
    val categoryType: String,
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int,
    val isPreset: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CoreMonthlySummary(
    val yearMonth: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long
)

@Serializable
data class CoreCategoryBreakdown(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val amountCents: Long,
    val percentage: Double
)

@Serializable
data class CoreSettings(
    val currencySymbol: String = "¥"
)

@Serializable
data class CoreUiState(
    val activeTab: String = "records",
    val selectedRecordId: Long? = null,
    val errorMessage: String? = null
)
