package com.agguy.moni.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecordType {
    @SerialName("income")
    INCOME,

    @SerialName("expense")
    EXPENSE
}

@Serializable
enum class ExportFormat {
    @SerialName("csv")
    CSV,

    @SerialName("json")
    JSON
}

@Serializable
enum class BudgetScope {
    @SerialName("this_month")
    THIS_MONTH,

    @SerialName("this_and_future")
    THIS_AND_FUTURE,

    @SerialName("future_only")
    FUTURE_ONLY
}

@Serializable
enum class MockPreset {
    @SerialName("normal")
    NORMAL,

    @SerialName("stress")
    STRESS
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
        val note: String? = null,
        val timestamp: Long? = null
    ) : CoreIntent()

    @Serializable
    @SerialName("record_delete")
    data class RecordDelete(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("record_list")
    data class RecordList(val page: Int = 0, val pageSize: Int = 50) : CoreIntent()

    @Serializable
    @SerialName("record_list_by_month")
    data class RecordListByMonth(val yearMonth: String) : CoreIntent()

    @Serializable
    @SerialName("record_get")
    data class RecordGet(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("record_search")
    data class RecordSearch(
        val keyword: String? = null,
        val recordType: String? = null,
        val categoryIds: List<Long> = emptyList(),
        val amountMin: Long? = null,
        val amountMax: Long? = null,
        val dateStart: Long? = null,
        val dateEnd: Long? = null,
        val sortBy: String = "created_at",
        val sortOrder: String = "desc",
        val page: Int = 0,
        val pageSize: Int = 50
    ) : CoreIntent()

    // 分类相关
    @Serializable
    @SerialName("category_create")
    data class CategoryCreate(
        val name: String,
        val description: String? = null,
        val categoryType: RecordType,
        val iconName: String,
        val parentId: Long? = null
    ) : CoreIntent()

    @Serializable
    @SerialName("category_update")
    data class CategoryUpdate(
        val id: Long,
        val name: String? = null,
        val description: String? = null,
        val iconName: String? = null,
        val parentId: Long? = null,
        val clearParentId: Boolean = false
    ) : CoreIntent()

    @Serializable
    @SerialName("category_archive")
    data class CategoryArchive(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("category_unarchive")
    data class CategoryUnarchive(val id: Long) : CoreIntent()

    @Serializable
    @SerialName("category_list")
    data object CategoryList : CoreIntent()

    @Serializable
    @SerialName("category_reorder")
    data class CategoryReorder(val orderedIds: List<Long>) : CoreIntent()

    // 统计相关
    @Serializable
    @SerialName("stats_monthly_summary")
    data class StatsMonthlySummary(val months: Int = 6) : CoreIntent()

    @Serializable
    @SerialName("stats_category_breakdown")
    data class StatsCategoryBreakdown(
        val yearMonth: String,
        val aggregateByParent: Boolean = false
    ) : CoreIntent()

    @Serializable
    @SerialName("stats_overview_metrics")
    data class StatsOverviewMetrics(
        val yearMonth: String,
        val today: String
    ) : CoreIntent()

    // 设置相关
    @Serializable
    @SerialName("settings_update_currency")
    data class SettingsUpdateCurrency(val symbol: String) : CoreIntent()

    // 聚合 Intent：刷新当月全部数据（减少 FFI 往返）
    @Serializable
    @SerialName("refresh_month_data")
    data class RefreshMonthData(val yearMonth: String) : CoreIntent()

    @Serializable
    @SerialName("settings_export_data")
    data class SettingsExportData(val format: ExportFormat) : CoreIntent()

    // 预算相关
    @Serializable
    @SerialName("budget_upsert")
    data class BudgetUpsert(
        val categoryId: Long? = null,
        val amountCents: Long,
        val yearMonth: String,
        val scope: BudgetScope
    ) : CoreIntent()

    @Serializable
    @SerialName("budget_delete")
    data class BudgetDelete(
        val id: Long,
        val yearMonth: String,
        val scope: BudgetScope
    ) : CoreIntent()

    @Serializable
    @SerialName("budget_list")
    data class BudgetList(val yearMonth: String? = null) : CoreIntent()

    @Serializable
    @SerialName("budget_check")
    data class BudgetCheck(
        val categoryId: Long,
        val yearMonth: String,
        val amountCents: Long
    ) : CoreIntent()

    // 开发者选项
    @Serializable
    @SerialName("dev_clear_all_data")
    data object DevClearAllData : CoreIntent()

    @Serializable
    @SerialName("dev_generate_mock_data")
    data class DevGenerateMockData(val count: Int, val preset: MockPreset) : CoreIntent()

    @Serializable
    @SerialName("dev_seed_presets")
    data object DevSeedPresets : CoreIntent()

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
