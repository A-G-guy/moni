package com.agguy.moni.app

import com.agguy.moni.core.CoreAppState
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.CoreSettings
import com.agguy.moni.core.CoreUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {

    @Test
    fun `toAppState maps all fields correctly`() {
        val coreState = CoreAppState(
            records = listOf(
                CoreRecord(
                    id = 1,
                    amountCents = 1234,
                    recordType = "expense",
                    categoryId = 1,
                    categoryName = "餐饮",
                    note = "测试",
                    createdAt = 1000
                )
            ),
            categories = listOf(
                CoreCategory(
                    id = 1,
                    name = "餐饮",
                    description = "日常餐饮",
                    categoryType = "expense",
                    iconName = "restaurant",
                    sortOrder = 1,
                    isPreset = true,
                    createdAt = 0,
                    updatedAt = 0
                )
            ),
            settings = CoreSettings(currencySymbol = "$"),
            ui = CoreUiState(errorMessage = "some error")
        )

        val appState = coreState.toAppState()

        assertEquals(1, appState.records.size)
        assertEquals(1234, appState.records[0].amountCents)
        assertEquals(1, appState.categories.size)
        assertEquals("餐饮", appState.categories[0].name)
        assertEquals("日常餐饮", appState.categories[0].description)
        assertEquals("$", appState.currencySymbol)
        assertEquals("some error", appState.errorMessage)
    }

    @Test
    fun `toAppState uses defaults for empty state`() {
        val coreState = CoreAppState()
        val appState = coreState.toAppState()

        assertTrue(appState.records.isEmpty())
        assertTrue(appState.categories.isEmpty())
        assertTrue(appState.monthlySummaries.isEmpty())
        assertTrue(appState.currentMonthBreakdown.isEmpty())
        assertNull(appState.errorMessage)
        assertEquals("¥", appState.currencySymbol)
    }
}
