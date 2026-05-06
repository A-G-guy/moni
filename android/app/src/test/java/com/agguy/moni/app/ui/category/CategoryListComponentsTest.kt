package com.agguy.moni.app.ui.category

import com.agguy.moni.core.CoreCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CategoryListComponents 中纯函数 flattenCategoriesWithHierarchy 的单元测试。
 *
 * 覆盖范围：
 * - 仅父分类列表 → 全 false。
 * - 父子混合 → 子分类紧跟父分类，标 true。
 * - 已归档分类被过滤。
 * - 按 sortOrder 排序。
 * - 空输入 → 空输出。
 * - 多个父，每个有不同数量子。
 */
class CategoryListComponentsTest {

    @Test
    fun `空列表返回空结果`() {
        val result = flattenCategoriesWithHierarchy(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `仅父分类列表全部标记为 false`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            parentCategory(id = 2L, sortOrder = 1)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(2, result.size)
        assertFalse(result[0].second)
        assertFalse(result[1].second)
        assertEquals(1L, result[0].first.id)
        assertEquals(2L, result[1].first.id)
    }

    @Test
    fun `父子混合时子分类紧跟父分类并标记为 true`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            childCategory(id = 2L, parentId = 1L, sortOrder = 0),
            childCategory(id = 3L, parentId = 1L, sortOrder = 1)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(3, result.size)
        assertEquals(1L, result[0].first.id)
        assertFalse(result[0].second)

        assertEquals(2L, result[1].first.id)
        assertTrue(result[1].second)

        assertEquals(3L, result[2].first.id)
        assertTrue(result[2].second)
    }

    @Test
    fun `已归档分类被过滤`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            parentCategory(id = 2L, sortOrder = 1, archivedAt = 1_700_000_000L),
            childCategory(id = 3L, parentId = 1L, sortOrder = 0)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(2, result.size)
        assertEquals(1L, result[0].first.id)
        assertEquals(3L, result[1].first.id)
        assertTrue(result[1].second)
    }

    @Test
    fun `父分类按 sortOrder 升序排列`() {
        val categories = listOf(
            parentCategory(id = 3L, sortOrder = 2),
            parentCategory(id = 1L, sortOrder = 0),
            parentCategory(id = 2L, sortOrder = 1)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(3, result.size)
        assertEquals(1L, result[0].first.id)
        assertEquals(2L, result[1].first.id)
        assertEquals(3L, result[2].first.id)
    }

    @Test
    fun `子分类按 sortOrder 升序排列`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            childCategory(id = 4L, parentId = 1L, sortOrder = 3),
            childCategory(id = 2L, parentId = 1L, sortOrder = 1),
            childCategory(id = 3L, parentId = 1L, sortOrder = 2)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(4, result.size)
        assertEquals(1L, result[0].first.id)
        assertTrue(result[1].second)
        assertEquals(2L, result[1].first.id)
        assertEquals(3L, result[2].first.id)
        assertEquals(4L, result[3].first.id)
    }

    @Test
    fun `多个父分类各自拥有不同数量子分类`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            childCategory(id = 4L, parentId = 1L, sortOrder = 0),
            parentCategory(id = 2L, sortOrder = 1),
            childCategory(id = 5L, parentId = 2L, sortOrder = 0),
            childCategory(id = 6L, parentId = 2L, sortOrder = 1),
            parentCategory(id = 3L, sortOrder = 2)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(6, result.size)
        // parent 1 + 1 child
        assertEquals(1L, result[0].first.id)
        assertFalse(result[0].second)
        assertEquals(4L, result[1].first.id)
        assertTrue(result[1].second)
        // parent 2 + 2 children
        assertEquals(2L, result[2].first.id)
        assertFalse(result[2].second)
        assertEquals(5L, result[3].first.id)
        assertTrue(result[3].second)
        assertEquals(6L, result[4].first.id)
        assertTrue(result[4].second)
        // parent 3 + 0 children
        assertEquals(3L, result[5].first.id)
        assertFalse(result[5].second)
    }

    @Test
    fun `孤儿子分类 parentId 不存在于列表中时不被输出`() {
        val categories = listOf(
            parentCategory(id = 1L, sortOrder = 0),
            childCategory(id = 2L, parentId = 99L, sortOrder = 0)
        )

        val result = flattenCategoriesWithHierarchy(categories)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].first.id)
        assertFalse(result[0].second)
    }

    private fun parentCategory(
        id: Long,
        sortOrder: Int,
        archivedAt: Long? = null
    ): CoreCategory = CoreCategory(
        id = id,
        name = "分类$id",
        categoryType = "expense",
        iconName = "icon",
        sortOrder = sortOrder,
        archivedAt = archivedAt
    )

    private fun childCategory(
        id: Long,
        parentId: Long,
        sortOrder: Int
    ): CoreCategory = CoreCategory(
        id = id,
        name = "子分类$id",
        categoryType = "expense",
        iconName = "icon",
        sortOrder = sortOrder,
        parentId = parentId
    )
}
