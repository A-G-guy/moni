package com.agguy.moni.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecordType {
    @SerialName("income") INCOME,
    @SerialName("expense") EXPENSE
}

@Serializable
enum class ExportFormat {
    @SerialName("csv") CSV,
    @SerialName("json") JSON
}

@Serializable
sealed class CoreIntent {
    // 记录相关
    @Serializable
    @SerialName("record_create")
    data class RecordCreate(
        val amountCents: Long,
        val recordType: RecordType,
        val categoryId: Long,
        val note: String = "",
        val timestamp: Long? = null
    ) : CoreIntent()

    @Serializable
    @SerialName("record_update")
    data class RecordUpdate(
        val id: Long,
        val amountCents: Long? = null,
        val recordType: RecordType? = null,
        val categoryId: Long? = null,
        val note: String? = null
    ) : CoreIntent()

    @Serializable
    @SerialName("record_delete")
    data class RecordDelete(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("record_list")
    data class RecordList(val page: Int = 0, val pageSize: Int = 50) : CoreIntent()

    @Serializable
    @SerialName("record_get")
    data class RecordGet(val id: Long) : CoreIntent()

    // 分类相关
    @Serializable
    @SerialName("category_create")
    data class CategoryCreate(
        val name: String,
        val categoryType: RecordType,
        val iconName: String,
        val colorHex: String
    ) : CoreIntent()

    @Serializable
    @SerialName("category_delete")
    data class CategoryDelete(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("category_list")
    data object CategoryList : CoreIntent()

    // 统计相关
    @Serializable
    @SerialName("stats_monthly_summary")
    data class StatsMonthlySummary(val months: Int = 6) : CoreIntent()

    @Serializable
    @SerialName("stats_category_breakdown")
    data class StatsCategoryBreakdown(val yearMonth: String) : CoreIntent()

    // 设置相关
    @Serializable
    @SerialName("settings_update_currency")
    data class SettingsUpdateCurrency(val symbol: String) : CoreIntent()

    @Serializable
    @SerialName("settings_export_data")
    data class SettingsExportData(val format: ExportFormat) : CoreIntent()

    // UI 相关
    @Serializable
    @SerialName("navigate_to")
    data class NavigateTo(val screen: String) : CoreIntent()

    @Serializable
    @SerialName("dismiss_error")
    data object DismissError : CoreIntent()
}

/** 获取 RecordType 的序列化名称，用于与来自 Rust 的 String 字段比较。 */
val RecordType.serialName: String
    get() = when (this) {
        RecordType.INCOME -> "income"
        RecordType.EXPENSE -> "expense"
    }
