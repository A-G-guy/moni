package com.agguy.moni.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CoreEffect 与 CoreUpdate 的单元测试。
 *
 * 覆盖范围：
 * - 序列化/反序列化 round-trip 一致性。
 * - effects 列表为空、含多个 effect 的边界。
 * - data class equals / copy 行为。
 */
class CoreEffectTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `CoreEffect round-trip 保持 kind 与 payloadJson`() {
        val original = CoreEffect(kind = "navigate", payloadJson = "{\"screen\":\"settings\"}")

        val encoded = json.encodeToString(CoreEffect.serializer(), original)
        val decoded = json.decodeFromString(CoreEffect.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals("navigate", decoded.kind)
        assertEquals("{\"screen\":\"settings\"}", decoded.payloadJson)
    }

    @Test
    fun `CoreEffect payloadJson 可为空字符串`() {
        val original = CoreEffect(kind = "toast", payloadJson = "")

        val decoded = json.decodeFromString(
            CoreEffect.serializer(),
            json.encodeToString(CoreEffect.serializer(), original)
        )

        assertEquals("toast", decoded.kind)
        assertEquals("", decoded.payloadJson)
    }

    @Test
    fun `CoreEffect equals 与 copy 行为`() {
        val a = CoreEffect(kind = "a", payloadJson = "1")
        val b = CoreEffect(kind = "a", payloadJson = "1")
        val c = CoreEffect(kind = "a", payloadJson = "2")

        assertEquals(a, b)
        assertNotEquals(a, c)

        val copied = a.copy(payloadJson = "3")
        assertEquals("3", copied.payloadJson)
        assertEquals("a", copied.kind)
        assertNotEquals(a, copied)
    }

    @Test
    fun `CoreUpdate effects 为空列表时 round-trip 正确`() {
        val original = CoreUpdate(stateJson = "{}", effects = emptyList())

        val encoded = json.encodeToString(CoreUpdate.serializer(), original)
        val decoded = json.decodeFromString(CoreUpdate.serializer(), encoded)

        assertEquals(original, decoded)
        assertTrue(decoded.effects.isEmpty())
        assertEquals("{}", decoded.stateJson)
    }

    @Test
    fun `CoreUpdate effects 含多个 effect 时顺序保持`() {
        val original = CoreUpdate(
            stateJson = "{\"tab\":\"home\"}",
            effects = listOf(
                CoreEffect(kind = "navigate", payloadJson = "{\"to\":\"list\"}"),
                CoreEffect(kind = "toast", payloadJson = "\"保存成功\""),
                CoreEffect(kind = "vibrate", payloadJson = "{}")
            )
        )

        val decoded = json.decodeFromString(
            CoreUpdate.serializer(),
            json.encodeToString(CoreUpdate.serializer(), original)
        )

        assertEquals(3, decoded.effects.size)
        assertEquals("navigate", decoded.effects[0].kind)
        assertEquals("toast", decoded.effects[1].kind)
        assertEquals("vibrate", decoded.effects[2].kind)
        assertEquals("\"保存成功\"", decoded.effects[1].payloadJson)
    }

    @Test
    fun `CoreUpdate copy 仅替换指定字段`() {
        val base = CoreUpdate(stateJson = "{}", effects = listOf(CoreEffect("a", "1")))
        val copied = base.copy(stateJson = "{\"x\":1}")

        assertEquals("{}", base.stateJson)
        assertEquals("{\"x\":1}", copied.stateJson)
        assertEquals(1, copied.effects.size)
        assertEquals("a", copied.effects[0].kind)
    }
}
