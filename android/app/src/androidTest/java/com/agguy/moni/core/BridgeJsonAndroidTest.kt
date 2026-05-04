package com.agguy.moni.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * BridgeJson Android 仪器测试。
 *
 * 覆盖新字段（description、archivedAt）的序列化与反序列化，
 * 确保 Kotlin 与 Rust 之间的 JSON 契约正确。
 */
class BridgeJsonAndroidTest {

    @Test
    fun 反序列化应正确解析description字段() {
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
                "recordGroups": [],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records", "selectedRecordId": null, "errorMessage": null }
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)

        assertEquals(1, state.categories.size)
        val category = state.categories[0]
        assertEquals("餐饮", category.name)
        assertEquals("日常餐饮消费", category.description)
    }

    @Test
    fun 反序列化应正确解析archivedAt字段() {
        val json = """
            {
                "records": [],
                "categories": [
                    {
                        "id": 2,
                        "name": "旧项目",
                        "description": null,
                        "categoryType": "expense",
                        "iconName": "category",
                        "sortOrder": 3,
                        "isPreset": false,
                        "archivedAt": 1715000000000,
                        "createdAt": 1700000000000,
                        "updatedAt": 1715000000000
                    }
                ],
                "recordGroups": [],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records", "selectedRecordId": null, "errorMessage": null }
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)

        assertEquals(1, state.categories.size)
        val category = state.categories[0]
        assertEquals("旧项目", category.name)
        assertEquals(1715000000000L, category.archivedAt)
    }

    @Test
    fun 反序列化应正确处理null的description和archivedAt() {
        val json = """
            {
                "records": [],
                "categories": [
                    {
                        "id": 1,
                        "name": "通用",
                        "description": null,
                        "categoryType": "expense",
                        "iconName": "category",
                        "sortOrder": 0,
                        "isPreset": true,
                        "archivedAt": null,
                        "createdAt": 0,
                        "updatedAt": 0
                    }
                ],
                "recordGroups": [],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records", "selectedRecordId": null, "errorMessage": null }
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)

        assertEquals(1, state.categories.size)
        val category = state.categories[0]
        assertNull(category.description)
        assertNull(category.archivedAt)
    }

    @Test
    fun CategoryCreate序列化应包含description字段() {
        val intent: CoreIntent = CoreIntent.CategoryCreate(
            name = "测试分类",
            description = "测试描述",
            categoryType = RecordType.EXPENSE,
            iconName = "restaurant"
        )

        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_create", obj["type"]?.jsonPrimitive?.content)
        assertEquals("测试分类", obj["name"]?.jsonPrimitive?.content)
        assertEquals("测试描述", obj["description"]?.jsonPrimitive?.content)
        assertEquals("expense", obj["category_type"]?.jsonPrimitive?.content)
        assertEquals("restaurant", obj["icon_name"]?.jsonPrimitive?.content)
    }

    @Test
    fun CategoryCreate序列化中null的description应被包含() {
        val intent: CoreIntent = CoreIntent.CategoryCreate(
            name = "无描述分类",
            description = null,
            categoryType = RecordType.INCOME,
            iconName = "salary"
        )

        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_create", obj["type"]?.jsonPrimitive?.content)
        assertEquals("无描述分类", obj["name"]?.jsonPrimitive?.content)
        // encodeDefaults = true 确保 null 字段也被编码
        assertNotNull(obj["description"])
        assertEquals("null", obj["description"]?.jsonPrimitive?.content)
    }

    @Test
    fun CategoryUpdate序列化应包含description字段() {
        val intent: CoreIntent = CoreIntent.CategoryUpdate(
            id = 1,
            name = "新名称",
            description = "新描述",
            iconName = "new_icon"
        )

        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_update", obj["type"]?.jsonPrimitive?.content)
        assertEquals(1L, obj["id"]?.jsonPrimitive?.content?.toLong())
        assertEquals("新名称", obj["name"]?.jsonPrimitive?.content)
        assertEquals("新描述", obj["description"]?.jsonPrimitive?.content)
        assertEquals("new_icon", obj["icon_name"]?.jsonPrimitive?.content)
    }

    @Test
    fun CategoryArchive序列化应正确() {
        val intent: CoreIntent = CoreIntent.CategoryArchive(id = 42)

        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_archive", obj["type"]?.jsonPrimitive?.content)
        assertEquals(42L, obj["id"]?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun CategoryUnarchive序列化应正确() {
        val intent: CoreIntent = CoreIntent.CategoryUnarchive(id = 42)

        val json = BridgeJsonEncode.encodeToString(intent)
        val obj = BridgeJson.parseToJsonElement(json).jsonObject

        assertEquals("category_unarchive", obj["type"]?.jsonPrimitive?.content)
        assertEquals(42L, obj["id"]?.jsonPrimitive?.content?.toLong())
    }

    @Test
    fun 反序列化应忽略未知字段() {
        val json = """
            {
                "records": [],
                "categories": [
                    {
                        "id": 1,
                        "name": "测试",
                        "description": "描述",
                        "categoryType": "expense",
                        "iconName": "restaurant",
                        "sortOrder": 1,
                        "isPreset": true,
                        "archivedAt": null,
                        "createdAt": 0,
                        "updatedAt": 0,
                        "futureField": "should be ignored",
                        "anotherUnknown": 123
                    }
                ],
                "recordGroups": [],
                "monthlySummaries": [],
                "currentMonthBreakdown": [],
                "settings": { "currencySymbol": "¥" },
                "ui": { "activeTab": "records", "selectedRecordId": null, "errorMessage": null }
            }
        """.trimIndent()

        val state = BridgeJson.decodeFromString(CoreAppState.serializer(), json)

        assertEquals(1, state.categories.size)
        assertEquals("测试", state.categories[0].name)
        assertEquals("描述", state.categories[0].description)
    }
}
