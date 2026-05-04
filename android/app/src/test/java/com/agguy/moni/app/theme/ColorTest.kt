package com.agguy.moni.app.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Color.kt 单元测试。
 *
 * 验证亮度计算、浅色/暗色判断以及语义化颜色扩展。
 */
class ColorTest {

    @Test
    fun `luminance of pure white is 1`() {
        val white = Color(0xFFFFFFFF)
        // 白色 RGB 均为 1.0，亮度 = 0.2126 + 0.7152 + 0.0722 = 1.0
        val expected = 1.0f
        val actual = white.luminance()
        assertEquals(expected, actual, 0.001f)
    }

    @Test
    fun `luminance of pure black is 0`() {
        val black = Color(0xFF000000)
        val actual = black.luminance()
        assertEquals(0.0f, actual, 0.001f)
    }

    @Test
    fun `luminance of mid gray is around 0_5`() {
        val gray = Color(0xFF808080)
        val actual = gray.luminance()
        // 0x80 = 128/255 ≈ 0.502, 直接按 sRGB 值加权
        // 亮度 ≈ 0.502 * (0.2126 + 0.7152 + 0.0722) ≈ 0.502
        assertTrue("mid gray luminance should be around 0.5", actual in 0.48f..0.52f)
    }

    @Test
    fun `isLight returns true for light background`() {
        // 仅校验 isLight() 与 background 的关系，故用工厂函数填充其余槽位（含 Expressive 'fixed' 容器）
        val lightScheme = lightColorScheme(background = Color(0xFFFBFDF8))
        assertTrue(lightScheme.isLight())
    }

    @Test
    fun `isLight returns false for dark background`() {
        val darkScheme = lightColorScheme(background = Color(0xFF0F1511))
        assertFalse(darkScheme.isLight())
    }

    @Test
    fun `light background has luminance above threshold`() {
        val lightBg = Color(0xFFFBFDF8)
        assertTrue("light bg luminance > 0.5", lightBg.luminance() > 0.5f)
    }

    @Test
    fun `dark background has luminance below threshold`() {
        val darkBg = Color(0xFF0F1511)
        assertTrue("dark bg luminance < 0.5", darkBg.luminance() < 0.5f)
    }
}
