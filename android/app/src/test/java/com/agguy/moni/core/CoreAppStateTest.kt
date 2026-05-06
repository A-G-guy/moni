package com.agguy.moni.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CoreAppState 及其聚合下的全部 @Serializable 数据类的单元测试。
 *
 * 覆盖范围：
 * - 8 个数据类的 kotlinx.serialization round-trip 一致性。
 * - 默认值、可空字段、空集合的边界处理。
 * - data class 自动生成的 equals / copy 行为。
 * - JSON 解码端 ignoreUnknownKeys 容错。
 */
class CoreAppStateTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `CoreAppState 默认构造的全部字段为空集合或默认子对象`() {
        val state = CoreAppState()

        assertTrue(state.records.isEmpty())
        assertTrue(state.recordGroups.isEmpty())
        assertTrue(state.categories.isEmpty())
        assertTrue(state.monthlySummaries.isEmpty())
        assertTrue(state.currentMonthBreakdown.isEmpty())
        assertEquals(CoreSettings(), state.settings)
        assertEquals(CoreUiState(), state.ui)
    }

    @Test
    fun `CoreAppState 序列化 round-trip 保持完整数据`() {
        val original = CoreAppState(
            records = listOf(sampleRecord(id = 7L)),
            recordGroups = listOf(sampleRecordGroup()),
            categories = listOf(sampleCategory(id = 11L)),
            monthlySummaries = listOf(CoreMonthlySummary("2026-05", 5_000, 3_000, 2_000)),
            currentMonthBreakdown = listOf(CoreCategoryBreakdown(11L, "餐饮", 1_500, 0.5)),
            settings = CoreSettings(currencySymbol = "$"),
            ui = CoreUiState(activeTab = "stats", selectedRecordId = 7L, errorMessage = "boom")
        )

        val encoded = json.encodeToString(CoreAppState.serializer(), original)
        val decoded = json.decodeFromString(CoreAppState.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals(1, decoded.records.size)
        assertEquals(7L, decoded.records.first().id)
        assertEquals("$", decoded.settings.currencySymbol)
        assertEquals("stats", decoded.ui.activeTab)
    }

    @Test
    fun `CoreAppState 解码忽略未知字段`() {
        val rawJson = """
            {
              "records": [],
              "categories": [],
              "monthlySummaries": [],
              "currentMonthBreakdown": [],
              "settings": { "currencySymbol": "¥" },
              "ui": { "activeTab": "records" },
              "extraField": 42
            }
        """.trimIndent()

        val state = json.decodeFromString(CoreAppState.serializer(), rawJson)

        assertTrue(state.categories.isEmpty())
        assertEquals("¥", state.settings.currencySymbol)
        assertEquals("records", state.ui.activeTab)
    }

    @Test
    fun `CoreRecord round-trip 保持全部字段`() {
        val original = CoreRecord(
            id = 1L,
            amountCents = 12_345L,
            recordType = "expense",
            categoryId = 9L,
            categoryName = "餐饮",
            note = "工作餐",
            createdAt = 1_700_000_000L
        )

        val encoded = json.encodeToString(CoreRecord.serializer(), original)
        val decoded = json.decodeFromString(CoreRecord.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals(12_345L, decoded.amountCents)
        assertEquals("expense", decoded.recordType)
        assertEquals("工作餐", decoded.note)
    }

    @Test
    fun `CoreRecordGroup 默认 records 为空列表且支持非空赋值`() {
        val emptyGroup = CoreRecordGroup(date = "2026-05-06", incomeCents = 0L, expenseCents = 0L)
        assertTrue(emptyGroup.records.isEmpty())

        val nonEmpty = CoreRecordGroup(
            date = "2026-05-06",
            incomeCents = 100L,
            expenseCents = 200L,
            records = listOf(sampleRecord(id = 3L))
        )
        val decoded = json.decodeFromString(
            CoreRecordGroup.serializer(),
            json.encodeToString(CoreRecordGroup.serializer(), nonEmpty)
        )

        assertEquals(1, decoded.records.size)
        assertEquals(3L, decoded.records.first().id)
        assertEquals("2026-05-06", decoded.date)
    }

    @Test
    fun `CoreCategory 可空字段默认 null 且 round-trip 保留 null`() {
        val parent = CoreCategory(
            id = 1L,
            name = "餐饮",
            categoryType = "expense",
            iconName = "restaurant",
            sortOrder = 0
        )

        assertNull(parent.description)
        assertNull(parent.parentId)
        assertNull(parent.archivedAt)
        assertEquals(0L, parent.createdAt)
        assertEquals(0L, parent.updatedAt)

        val decoded = json.decodeFromString(
            CoreCategory.serializer(),
            json.encodeToString(CoreCategory.serializer(), parent)
        )
        assertEquals(parent, decoded)
        assertNull(decoded.description)
        assertNull(decoded.parentId)
    }

    @Test
    fun `CoreCategory 子分类与归档信息序列化保留`() {
        val sub = CoreCategory(
            id = 5L,
            name = "午餐",
            description = "工作日午餐",
            categoryType = "expense",
            iconName = "lunch",
            sortOrder = 2,
            parentId = 1L,
            archivedAt = 1_750_000_000L,
            createdAt = 1_700_000_000L,
            updatedAt = 1_700_500_000L
        )

        val decoded = json.decodeFromString(
            CoreCategory.serializer(),
            json.encodeToString(CoreCategory.serializer(), sub)
        )

        assertEquals(sub, decoded)
        assertEquals(1L, decoded.parentId)
        assertEquals(1_750_000_000L, decoded.archivedAt)
        assertEquals("工作日午餐", decoded.description)
    }

    @Test
    fun `CoreMonthlySummary round-trip 与 equals`() {
        val a = CoreMonthlySummary("2026-05", 100L, 50L, 50L)
        val b = CoreMonthlySummary("2026-05", 100L, 50L, 50L)
        val c = CoreMonthlySummary("2026-04", 100L, 50L, 50L)

        assertEquals(a, b)
        assertNotEquals(a, c)

        val decoded = json.decodeFromString(
            CoreMonthlySummary.serializer(),
            json.encodeToString(CoreMonthlySummary.serializer(), a)
        )
        assertEquals(a, decoded)
    }

    @Test
    fun `CoreCategoryBreakdown 保留 Double 精度`() {
        val original = CoreCategoryBreakdown(
            categoryId = 3L,
            categoryName = "交通",
            amountCents = 1234L,
            percentage = 0.1234
        )

        val decoded = json.decodeFromString(
            CoreCategoryBreakdown.serializer(),
            json.encodeToString(CoreCategoryBreakdown.serializer(), original)
        )

        assertEquals(original, decoded)
        assertEquals(0.1234, decoded.percentage, 1e-9)
        assertEquals("交通", decoded.categoryName)
    }

    @Test
    fun `CoreSettings 默认货币符号为人民币符号`() {
        val defaults = CoreSettings()
        assertEquals("¥", defaults.currencySymbol)

        val custom = defaults.copy(currencySymbol = "€")
        assertEquals("€", custom.currencySymbol)
        assertNotEquals(defaults, custom)
    }

    @Test
    fun `CoreUiState 默认值与 copy 行为`() {
        val defaults = CoreUiState()
        assertEquals("records", defaults.activeTab)
        assertNull(defaults.selectedRecordId)
        assertNull(defaults.errorMessage)

        val updated = defaults.copy(selectedRecordId = 99L, errorMessage = "失败")
        assertEquals(99L, updated.selectedRecordId)
        assertEquals("失败", updated.errorMessage)
        assertEquals("records", updated.activeTab)
        // copy 不修改原对象
        assertNull(defaults.selectedRecordId)
    }

    @Test
    fun `CoreUiState round-trip 处理 null 字段`() {
        val state = CoreUiState(activeTab = "stats", selectedRecordId = null, errorMessage = null)

        val encoded = json.encodeToString(CoreUiState.serializer(), state)
        val decoded = json.decodeFromString(CoreUiState.serializer(), encoded)

        assertEquals(state, decoded)
        assertNull(decoded.selectedRecordId)
        assertNull(decoded.errorMessage)
    }

    @Test
    fun `CoreAppState 默认构造序列化为空对象`() {
        val state = CoreAppState()
        val encoded = json.encodeToString(CoreAppState.serializer(), state)

        // kotlinx.serialization 默认不编码默认值，故空状态序列化为 {}
        assertEquals("{}", encoded)
    }

    @Test
    fun `CoreAppState copy 仅替换指定字段`() {
        val base = CoreAppState(settings = CoreSettings(currencySymbol = "$"))
        val copied = base.copy(settings = CoreSettings(currencySymbol = "¥"))

        assertEquals("$", base.settings.currencySymbol)
        assertEquals("¥", copied.settings.currencySymbol)
        // 其它字段保持引用相等
        assertEquals(base.records, copied.records)
        assertEquals(base.ui, copied.ui)
    }

    private fun sampleRecord(id: Long): CoreRecord = CoreRecord(
        id = id,
        amountCents = 100L,
        recordType = "expense",
        categoryId = 1L,
        categoryName = "餐饮",
        note = "",
        createdAt = 0L
    )

    private fun sampleRecordGroup(): CoreRecordGroup = CoreRecordGroup(
        date = "2026-05-06",
        incomeCents = 0L,
        expenseCents = 100L,
        records = listOf(sampleRecord(id = 2L))
    )

    private fun sampleCategory(id: Long): CoreCategory = CoreCategory(
        id = id,
        name = "餐饮",
        categoryType = "expense",
        iconName = "restaurant",
        sortOrder = 0
    )
}
