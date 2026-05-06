package com.agguy.moni.app

import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * ThemeSettings 数据类单元测试。
 */
class ThemeSettingsTest {

    @Test
    fun `default theme settings uses system mode and no dynamic color`() {
        val defaults = ThemeSettings()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertFalse(defaults.dynamicColor)
        assertEquals(PresetColorScheme.AIRY_SAKURA, defaults.presetColorScheme)
    }

    @Test
    fun `theme settings copy preserves values`() {
        val settings = ThemeSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = true,
            presetColorScheme = PresetColorScheme.SUNSET_CORAL
        )
        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(true, settings.dynamicColor)
        assertEquals(PresetColorScheme.SUNSET_CORAL, settings.presetColorScheme)
    }

    @Test
    fun `theme settings copy with changes`() {
        val original = ThemeSettings(
            themeMode = ThemeMode.LIGHT,
            dynamicColor = false,
            presetColorScheme = PresetColorScheme.CRISP_MINT
        )
        val updated = original.copy(themeMode = ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertFalse(updated.dynamicColor)
        assertEquals(PresetColorScheme.CRISP_MINT, updated.presetColorScheme)
    }
}
