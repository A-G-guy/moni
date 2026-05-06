package com.agguy.moni.app.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * PresetColorScheme 单元测试。
 *
 * 验证每套预设配色的 ColorScheme 组装正确且主色符合预期。
 */
class PresetColorSchemeTest {

    @Test
    fun `all preset schemes have non null light color scheme`() {
        PresetColorScheme.entries.forEach { scheme ->
            val colorScheme = scheme.toLightColorScheme()
            assertNotNull("$scheme lightColorScheme should not be null", colorScheme)
        }
    }

    @Test
    fun `all preset schemes have non null dark color scheme`() {
        PresetColorScheme.entries.forEach { scheme ->
            val colorScheme = scheme.toDarkColorScheme()
            assertNotNull("$scheme darkColorScheme should not be null", colorScheme)
        }
    }

    @Test
    fun `airy sakura light primary matches expected`() {
        val scheme = PresetColorScheme.AIRY_SAKURA.toLightColorScheme()
        assertEquals(Color(0xFF9B3E64), scheme.primary)
    }

    @Test
    fun `anime sky light primary matches expected`() {
        val scheme = PresetColorScheme.ANIME_SKY.toLightColorScheme()
        assertEquals(Color(0xFF0061A6), scheme.primary)
    }

    @Test
    fun `crisp mint light primary matches expected`() {
        val scheme = PresetColorScheme.CRISP_MINT.toLightColorScheme()
        assertEquals(Color(0xFF006C4E), scheme.primary)
    }

    @Test
    fun `neon lavender light primary matches expected`() {
        val scheme = PresetColorScheme.NEON_LAVENDER.toLightColorScheme()
        assertEquals(Color(0xFF7D2ECB), scheme.primary)
    }

    @Test
    fun `oatmeal gold light primary matches expected`() {
        val scheme = PresetColorScheme.OATMEAL_GOLD.toLightColorScheme()
        assertEquals(Color(0xFF7D562D), scheme.primary)
    }

    @Test
    fun `sunset coral light primary matches expected`() {
        val scheme = PresetColorScheme.SUNSET_CORAL.toLightColorScheme()
        assertEquals(Color(0xFFA43D0F), scheme.primary)
    }

    @Test
    fun `display names are in chinese`() {
        assertEquals("晴空樱粉", PresetColorScheme.AIRY_SAKURA.displayName)
        assertEquals("动漫天蓝", PresetColorScheme.ANIME_SKY.displayName)
        assertEquals("清新薄荷", PresetColorScheme.CRISP_MINT.displayName)
        assertEquals("霓虹薰衣草", PresetColorScheme.NEON_LAVENDER.displayName)
        assertEquals("燕麦金", PresetColorScheme.OATMEAL_GOLD.displayName)
        assertEquals("落日珊瑚", PresetColorScheme.SUNSET_CORAL.displayName)
    }

    @Test
    fun `seed colors match expected values`() {
        assertEquals(Color(0xFFFF8FB8), PresetColorScheme.AIRY_SAKURA.seedColor)
        assertEquals(Color(0xFF269BFF), PresetColorScheme.ANIME_SKY.seedColor)
        assertEquals(Color(0xFF17D19B), PresetColorScheme.CRISP_MINT.seedColor)
        assertEquals(Color(0xFF974DE6), PresetColorScheme.NEON_LAVENDER.seedColor)
        assertEquals(Color(0xFFD4A373), PresetColorScheme.OATMEAL_GOLD.seedColor)
        assertEquals(Color(0xFFFA7C4B), PresetColorScheme.SUNSET_CORAL.seedColor)
    }
}
