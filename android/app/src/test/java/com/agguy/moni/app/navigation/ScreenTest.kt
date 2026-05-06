package com.agguy.moni.app.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `Screen` sealed class 单元测试。
 *
 * 覆盖：
 * - 各 data object 的相等性（`===` 单例语义）；
 * - 各 data class 的相等/拷贝/默认值；
 * - kotlinx.serialization 的多态序列化与反序列化；
 * - 在统一 Json 配置下的 round-trip 一致性。
 */
class ScreenTest {

    private val json = Json {
        // 与项目中使用 sealed 多态序列化的常见配置一致
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    @Test
    fun `RecordList is a singleton data object`() {
        val a: Screen = Screen.RecordList
        val b: Screen = Screen.RecordList
        // data object 应是单例
        assertTrue("RecordList 应为单例", a === b)
    }

    @Test
    fun `RecordDetail equality and copy work as data class`() {
        val a = Screen.RecordDetail(recordId = 1L)
        val b = Screen.RecordDetail(recordId = 1L)
        val c = Screen.RecordDetail(recordId = 2L)
        assertEquals("相同 id 的 RecordDetail 应相等", a, b)
        assertNotEquals("不同 id 的 RecordDetail 不应相等", a, c)
        val copied = a.copy(recordId = 9L)
        assertEquals(9L, copied.recordId)
    }

    @Test
    fun `RecordDetail recordId defaults to null`() {
        val d = Screen.RecordDetail()
        assertNull("默认 recordId 应为 null", d.recordId)
    }

    @Test
    fun `RecordList serializes round trip`() {
        val original: Screen = Screen.RecordList
        val text = json.encodeToString(Screen.serializer(), original)
        val decoded = json.decodeFromString(Screen.serializer(), text)
        assertEquals(original, decoded)
    }

    @Test
    fun `RecordDetail serializes round trip with id`() {
        val original: Screen = Screen.RecordDetail(recordId = 42L)
        val text = json.encodeToString(Screen.serializer(), original)
        val decoded = json.decodeFromString(Screen.serializer(), text)
        assertEquals(original, decoded)
        assertTrue("序列化文本应包含 recordId 字段", text.contains("recordId"))
    }

    @Test
    fun `RecordDetail serializes round trip with null id`() {
        val original: Screen = Screen.RecordDetail()
        val text = json.encodeToString(Screen.serializer(), original)
        val decoded = json.decodeFromString(Screen.serializer(), text) as Screen.RecordDetail
        assertNull(decoded.recordId)
    }

    @Test
    fun `all singleton screens round trip`() {
        val singletons: List<Screen> = listOf(
            Screen.RecordList,
            Screen.CategoryList,
            Screen.Stats,
            Screen.Settings,
            Screen.DeveloperOptions,
            Screen.ArchivedCategories,
            Screen.DevLog,
            Screen.DataManagement,
        )
        singletons.forEach { original ->
            val text = json.encodeToString(Screen.serializer(), original)
            val decoded = json.decodeFromString(Screen.serializer(), text)
            assertEquals("$original 序列化往返应保持相等", original, decoded)
        }
    }

    @Test
    fun `serialized JSON contains discriminator`() {
        val text = json.encodeToString(Screen.serializer(), Screen.Settings)
        assertTrue("应包含 type 鉴别字段", text.contains("\"type\""))
    }

    @Test
    fun `different singletons are not equal`() {
        val a: Screen = Screen.RecordList
        val b: Screen = Screen.Settings
        val c: Screen = Screen.Stats
        val d: Screen = Screen.DataManagement
        assertNotEquals(a, b)
        assertNotEquals(c, d)
    }

    @Test
    fun `Screen serializer is registered for sealed hierarchy`() {
        // 防御性：确保 serializer() 不返回 null（编译期保证，但显式断言便于回归）
        assertNotNull(Screen.serializer())
    }
}
