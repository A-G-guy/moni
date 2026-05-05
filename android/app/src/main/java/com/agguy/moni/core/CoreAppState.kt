package com.agguy.moni.core

import kotlinx.serialization.Serializable

@Serializable
data class CoreAppState(
    val records: List<CoreRecord> = emptyList(),
    val recordGroups: List<CoreRecordGroup> = emptyList(),
    val categories: List<CoreCategory> = emptyList(),
    val monthlySummaries: List<CoreMonthlySummary> = emptyList(),
    val currentMonthBreakdown: List<CoreCategoryBreakdown> = emptyList(),
    val settings: CoreSettings = CoreSettings(),
    val ui: CoreUiState = CoreUiState()
)

@Serializable
data class CoreRecord(
    val id: Long,
    val amountCents: Long,
    val recordType: String,
    val categoryId: Long,
    val categoryName: String,
    val note: String,
    val createdAt: Long,
)

@Serializable
data class CoreRecordGroup(
    val date: String,
    val incomeCents: Long,
    val expenseCents: Long,
    val records: List<CoreRecord> = emptyList(),
)

@Serializable
data class CoreCategory(
    val id: Long,
    val name: String,
    val description: String? = null,
    val categoryType: String,
    val iconName: String,
    val sortOrder: Int,
    val archivedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
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
    val amountCents: Long,
    val percentage: Double
)

@Serializable
data class CoreSettings(val currencySymbol: String = "¥")

@Serializable
data class CoreUiState(
    val activeTab: String = "records",
    val selectedRecordId: Long? = null,
    val errorMessage: String? = null
)
