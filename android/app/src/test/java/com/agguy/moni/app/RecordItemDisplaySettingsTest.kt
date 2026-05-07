package com.agguy.moni.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RecordItemDisplaySettings 数据类单元测试。
 */
class RecordItemDisplaySettingsTest {

    @Test
    fun `default settings has showIcon true and others false`() {
        val defaults = RecordItemDisplaySettings()
        assertTrue(defaults.showIcon)
        assertFalse(defaults.showFullCategory)
        assertFalse(defaults.notePriority)
    }

    @Test
    fun `copy preserves values`() {
        val settings = RecordItemDisplaySettings(
            showIcon = false,
            showFullCategory = true,
            notePriority = true
        )
        assertFalse(settings.showIcon)
        assertTrue(settings.showFullCategory)
        assertTrue(settings.notePriority)
    }

    @Test
    fun `copy with single change preserves others`() {
        val original = RecordItemDisplaySettings()
        val updated = original.copy(showFullCategory = true)
        assertTrue(updated.showIcon)
        assertTrue(updated.showFullCategory)
        assertFalse(updated.notePriority)
    }
}
