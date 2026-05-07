package com.agguy.moni.app.ui.record

import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RecordListItem 显示逻辑单元测试。
 */
class RecordListItemDisplayTest {

    private val parentCategory = CoreCategory(
        id = 1,
        name = "餐饮",
        description = null,
        categoryType = "expense",
        iconName = "restaurant",
        sortOrder = 0,
        parentId = null
    )

    private val childCategory = CoreCategory(
        id = 2,
        name = "午餐",
        description = null,
        categoryType = "expense",
        iconName = "lunch_dining",
        sortOrder = 0,
        parentId = 1
    )

    private val standaloneCategory = CoreCategory(
        id = 3,
        name = "交通",
        description = null,
        categoryType = "expense",
        iconName = "directions_car",
        sortOrder = 0,
        parentId = null
    )

    private val recordWithChild = CoreRecord(
        id = 1,
        amountCents = 2500,
        recordType = "expense",
        categoryId = 2,
        parentCategoryId = 1,
        categoryName = "午餐",
        note = "公司食堂",
        createdAt = 1715140800
    )

    private val recordStandalone = CoreRecord(
        id = 2,
        amountCents = 5000,
        recordType = "expense",
        categoryId = 3,
        parentCategoryId = null,
        categoryName = "交通",
        note = "",
        createdAt = 1715140800
    )

    // region resolveCategoryDisplay

    @Test
    fun `resolveCategoryDisplay returns plain name when showFullCategory is false`() {
        val categories = listOf(parentCategory, childCategory)
        val result = resolveCategoryDisplay(recordWithChild, categories, showFullCategory = false)
        assertEquals("午餐", result)
    }

    @Test
    fun `resolveCategoryDisplay returns full path when record has parent category`() {
        val categories = listOf(parentCategory, childCategory)
        val result = resolveCategoryDisplay(recordWithChild, categories, showFullCategory = true)
        assertEquals("餐饮 › 午餐", result)
    }

    @Test
    fun `resolveCategoryDisplay returns plain name when category has no parent`() {
        val categories = listOf(standaloneCategory)
        val result = resolveCategoryDisplay(recordStandalone, categories, showFullCategory = true)
        assertEquals("交通", result)
    }

    @Test
    fun `resolveCategoryDisplay returns plain name when category not found`() {
        val categories = emptyList<CoreCategory>()
        val result = resolveCategoryDisplay(recordWithChild, categories, showFullCategory = true)
        assertEquals("午餐", result)
    }

    @Test
    fun `resolveCategoryDisplay returns plain name when parent not found`() {
        val categories = listOf(childCategory) // parentCategory missing
        val result = resolveCategoryDisplay(recordWithChild, categories, showFullCategory = true)
        assertEquals("午餐", result)
    }

    // endregion

    // region primary / secondary text selection

    @Test
    fun `default mode uses category as primary and note as secondary`() {
        val categoryDisplay = resolveCategoryDisplay(recordWithChild, listOf(parentCategory, childCategory), false)
        val primaryText: String
        val secondaryText: String?
        val notePriority = false
        val note = recordWithChild.note

        if (notePriority && note.isNotBlank()) {
            primaryText = note
            secondaryText = categoryDisplay
        } else {
            primaryText = categoryDisplay
            secondaryText = note.takeIf { it.isNotBlank() }
        }

        assertEquals("午餐", primaryText)
        assertEquals("公司食堂", secondaryText)
    }

    @Test
    fun `notePriority mode uses note as primary and category as secondary`() {
        val categoryDisplay = resolveCategoryDisplay(recordWithChild, listOf(parentCategory, childCategory), false)
        val primaryText: String
        val secondaryText: String?
        val notePriority = true
        val note = recordWithChild.note

        if (notePriority && note.isNotBlank()) {
            primaryText = note
            secondaryText = categoryDisplay
        } else {
            primaryText = categoryDisplay
            secondaryText = note.takeIf { it.isNotBlank() }
        }

        assertEquals("公司食堂", primaryText)
        assertEquals("午餐", secondaryText)
    }

    @Test
    fun `notePriority with blank note falls back to category only`() {
        val categoryDisplay = resolveCategoryDisplay(recordStandalone, listOf(standaloneCategory), false)
        val primaryText: String
        val secondaryText: String?
        val notePriority = true
        val note = recordStandalone.note

        if (notePriority && note.isNotBlank()) {
            primaryText = note
            secondaryText = categoryDisplay
        } else {
            primaryText = categoryDisplay
            secondaryText = note.takeIf { it.isNotBlank() }
        }

        assertEquals("交通", primaryText)
        assertEquals(null, secondaryText)
    }

    // endregion
}
