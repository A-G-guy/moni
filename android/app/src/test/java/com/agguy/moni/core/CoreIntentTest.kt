package com.agguy.moni.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreIntentTest {

    @Test
    fun `record type serial names`() {
        assertEquals("income", RecordType.INCOME.serialName)
        assertEquals("expense", RecordType.EXPENSE.serialName)
    }

    @Test
    fun `record type serializes correctly`() {
        val intent = CoreIntent.RecordCreate(
            amountCents = 100,
            recordType = RecordType.INCOME,
            categoryId = 1
        )
        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject
        assertEquals("income", obj["record_type"]?.jsonPrimitive?.content)
    }

    @Test
    fun `export format serializes correctly`() {
        val csvIntent = CoreIntent.SettingsExportData(format = ExportFormat.CSV)
        val csvJson = BridgeJsonEncode.encodeToString(csvIntent)
        val csvObj = BridgeJson.parseToJsonElement(csvJson).jsonObject
        assertEquals("csv", csvObj["format"]?.jsonPrimitive?.content)

        val jsonIntent = CoreIntent.SettingsExportData(format = ExportFormat.JSON)
        val jsonJson = BridgeJsonEncode.encodeToString(jsonIntent)
        val jsonObj = BridgeJson.parseToJsonElement(jsonJson).jsonObject
        assertEquals("json", jsonObj["format"]?.jsonPrimitive?.content)
    }

    @Test
    fun `all intent types have correct discriminator`() {
        val cases = listOf(
            CoreIntent.RecordCreate(amountCents = 100, recordType = RecordType.EXPENSE, categoryId = 1) to "record_create",
            CoreIntent.RecordUpdate(id = 1) to "record_update",
            CoreIntent.RecordDelete(id = 1) to "record_delete",
            CoreIntent.RecordList() to "record_list",
            CoreIntent.RecordGet(id = 1) to "record_get",
            CoreIntent.CategoryCreate(name = "", categoryType = RecordType.EXPENSE, iconName = "") to "category_create",
            CoreIntent.CategoryUpdate(id = 1) to "category_update",
            CoreIntent.CategoryArchive(id = 1) to "category_archive",
            CoreIntent.CategoryUnarchive(id = 1) to "category_unarchive",
            CoreIntent.CategoryList to "category_list",
            CoreIntent.StatsMonthlySummary() to "stats_monthly_summary",
            CoreIntent.StatsCategoryBreakdown(yearMonth = "2026-05") to "stats_category_breakdown",
            CoreIntent.SettingsUpdateCurrency(symbol = "$") to "settings_update_currency",
            CoreIntent.SettingsExportData(format = ExportFormat.CSV) to "settings_export_data",
            CoreIntent.NavigateTo(screen = "records") to "navigate_to",
            CoreIntent.DismissError to "dismiss_error",
        )

        for ((intent, expectedType) in cases) {
            val json = BridgeJsonEncode.encodeToString(intent)
            val obj = BridgeJson.parseToJsonElement(json).jsonObject
            assertEquals(expectedType, obj["type"]?.jsonPrimitive?.content)
        }
    }
}
