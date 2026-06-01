package com.agguy.moni.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NumPadSettings 数据类单元测试。
 */
class NumPadSettingsTest {

    @Test
    fun `default settings has swapTopAndBottomRows false`() {
        val defaults = NumPadSettings()
        assertFalse(defaults.swapTopAndBottomRows)
    }

    @Test
    fun `copy preserves values`() {
        val settings = NumPadSettings(
            swapTopAndBottomRows = true
        )
        assertTrue(settings.swapTopAndBottomRows)
    }

    @Test
    fun `copy with single change preserves others`() {
        val original = NumPadSettings()
        val updated = original.copy(swapTopAndBottomRows = true)
        assertTrue(updated.swapTopAndBottomRows)
    }
}
