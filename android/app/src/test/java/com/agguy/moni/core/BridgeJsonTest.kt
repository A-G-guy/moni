package com.agguy.moni.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeJsonTest {

    @Test
    fun `intent encode uses snake_case and type discriminator`() {
        val intent: CoreIntent = CoreIntent.RecordCreate(
            amountCents = 1234,
            recordType = RecordType.EXPENSE,
            categoryId = 1,
            note = "测试"
        )
        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("record_create", obj["type"]?.jsonPrimitive?.content)
        assertEquals(1234, obj["amount_cents"]?.jsonPrimitive?.content?.toInt())
        assertEquals("expense", obj["record_type"]?.jsonPrimitive?.content)
        assertEquals(1L, obj["category_id"]?.jsonPrimitive?.content?.toLong())
        assertEquals("测试", obj["note"]?.jsonPrimitive?.content)
    }

    @Test
    fun `intent encode includes default values`() {
        val intent: CoreIntent = CoreIntent.RecordList()
        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("record_list", obj["type"]?.jsonPrimitive?.content)
        assertEquals(0, obj["page"]?.jsonPrimitive?.content?.toInt())
        assertEquals(50, obj["page_size"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `state decode handles camelCase fields`() {
        val json = """
            {
                "records": [],
                "categories": [
                    {
                        "id": 1,
                        "name": "餐饮",
                        "description": "日常餐饮消费",
                        "categoryType": "expense",
                        "iconName": "restaurant",
                        "sortOrder": 1,
                        "isPreset": true,
                        "archivedAt": null,
                        "createdAt": 0,
                        "updatedAt": 0
                    }
                ],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records", "selectedRecordId": null, "errorMessage": null }
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)
        assertEquals(1, state.categories.size)
        assertEquals("餐饮", state.categories[0].name)
        assertEquals("日常餐饮消费", state.categories[0].description)
        assertEquals("expense", state.categories[0].categoryType)
        assertEquals("restaurant", state.categories[0].iconName)
        assertEquals("¥", state.settings.currencySymbol)
    }

    @Test
    fun `state decode ignores unknown keys`() {
        val json = """
            {
                "records": [],
                "categories": [],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records" },
                "futureField": "should be ignored"
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)
        assertTrue(state.records.isEmpty())
    }

    @Test
    fun `category create intent encodes without color`() {
        val intent: CoreIntent = CoreIntent.CategoryCreate(
            name = "测试",
            description = "描述",
            categoryType = RecordType.INCOME,
            iconName = "star"
        )
        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_create", obj["type"]?.jsonPrimitive?.content)
        assertEquals("测试", obj["name"]?.jsonPrimitive?.content)
        assertEquals("描述", obj["description"]?.jsonPrimitive?.content)
        assertEquals("income", obj["category_type"]?.jsonPrimitive?.content)
        assertEquals("star", obj["icon_name"]?.jsonPrimitive?.content)
    }
}
