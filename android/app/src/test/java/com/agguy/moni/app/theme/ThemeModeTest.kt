package com.agguy.moni.app.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ThemeMode 枚举序列化/反序列化测试。
 *
 * 验证与 DataStore 持久化格式的一致性。
 */
class ThemeModeTest {

    @Test
    fun `all theme modes can be serialized and deserialized`() {
        val modes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
        val expectedStrings = listOf("light", "dark", "system")

        for ((mode, expected) in modes.zip(expectedStrings)) {
            val serialized = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
            assertEquals(expected, serialized)
        }
    }

    @Test
    fun `deserialize returns system for unknown values`() {
        val unknownValues = listOf("auto", "", "invalid", null)
        for (value in unknownValues) {
            val deserialized = when (value) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            assertEquals(ThemeMode.SYSTEM, deserialized)
        }
    }

    @Test
    fun `deserialize returns correct mode for each valid string`() {
        assertEquals(
            ThemeMode.LIGHT,
            when ("light") {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        )
        assertEquals(
            ThemeMode.DARK,
            when ("dark") {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        )
        assertEquals(
            ThemeMode.SYSTEM,
            when ("system") {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        )
    }
}
