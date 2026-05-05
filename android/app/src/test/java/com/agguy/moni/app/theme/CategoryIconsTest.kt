package com.agguy.moni.app.theme

import com.agguy.moni.app.icons.MoniIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CategoryIcons` 单元测试，覆盖 `iconNameToRes` 与 `iconForCategory` 的命中/兜底分支，
 * 并断言 `GroupedCategoryIcons` 与 `AvailableCategoryIcons` 的基础结构不变量。
 */
class CategoryIconsTest {

    @Test
    fun `iconNameToRes returns the same id as the matching grouped entry`() {
        val expected = GroupedCategoryIcons.first().icons.first()
        val actual = iconNameToRes(expected.name)
        assertEquals(
            "命中名称应返回该图标的 iconRes",
            expected.iconRes,
            actual,
        )
    }

    @Test
    fun `iconNameToRes falls back to default when name is unknown`() {
        val fallback = iconNameToRes("definitely_unknown_icon_name_$$$$")
        assertEquals(MoniIcons.Category, fallback)
    }

    @Test
    fun `iconForCategory hits expected MoniIcons for primary keywords`() {
        // 验证主语义路径都能落到正确的 icon ID（不一定 != 0，因 unit test 下的资源 ID）
        assertEquals(MoniIcons.Restaurant, iconForCategory("餐饮"))
        assertEquals(MoniIcons.Restaurant, iconForCategory("FOOD"))
        assertEquals(MoniIcons.DirectionsCar, iconForCategory("transport"))
        assertEquals(MoniIcons.ShoppingBag, iconForCategory("购物"))
        assertEquals(MoniIcons.Payments, iconForCategory("salary"))
        assertEquals(MoniIcons.Home, iconForCategory("住房"))
        assertEquals(MoniIcons.SportsEsports, iconForCategory("ENTERTAINMENT"))
        assertEquals(MoniIcons.LocalHospital, iconForCategory("medical"))
        assertEquals(MoniIcons.School, iconForCategory("EduCation"))
        assertEquals(MoniIcons.Help, iconForCategory("HELP"))
    }

    @Test
    fun `iconForCategory falls back to Category for unknown keywords`() {
        assertEquals(MoniIcons.Category, iconForCategory("毫无意义的分类名"))
        assertEquals(MoniIcons.Category, iconForCategory(""))
    }

    @Test
    fun `iconForCategory is case insensitive`() {
        assertEquals(iconForCategory("food"), iconForCategory("FOOD"))
        assertEquals(iconForCategory("transport"), iconForCategory("Transport"))
    }

    @Test
    fun `GroupedCategoryIcons is non empty and every group has at least one icon`() {
        assertTrue("GroupedCategoryIcons 应非空", GroupedCategoryIcons.isNotEmpty())
        GroupedCategoryIcons.forEachIndexed { idx, group ->
            assertNotNull("分组 $idx 应有 label", group.label)
            assertFalse("分组 ${group.label} 不应为空", group.icons.isEmpty())
        }
    }

    @Test
    fun `AvailableCategoryIcons matches sum of group icons`() {
        val expectedCount = GroupedCategoryIcons.sumOf { it.icons.size }
        assertEquals(
            "扁平列表大小应等于各分组图标数之和",
            expectedCount,
            AvailableCategoryIcons.size,
        )
    }

    @Test
    fun `AvailableCategoryIcons preserves name order across groups`() {
        val flatNames = AvailableCategoryIcons.map { it.first }
        val expected = GroupedCategoryIcons.flatMap { group -> group.icons.map { it.name } }
        assertEquals(expected, flatNames)
    }

    @Test
    fun `every grouped icon name is unique`() {
        val allNames = GroupedCategoryIcons.flatMap { it.icons }.map { it.name }
        assertEquals(
            "图标 name 不应重复",
            allNames.size,
            allNames.toSet().size,
        )
    }

    @Test
    fun `iconForCategory and iconNameToRes can disagree but both never throw`() {
        // 防御性：调用任意 nonsense 字符串都不应抛异常
        listOf("", " ", "🚀", "中文")
            .forEach { input ->
                iconForCategory(input)
                iconNameToRes(input)
            }
    }

    @Test
    fun `IconGroup data class equals and copy work`() {
        val group = IconGroup("test", listOf(CategoryIcon("a", 1, 2)))
        val copy = group.copy(label = "renamed")
        assertEquals("renamed", copy.label)
        assertEquals(group.icons, copy.icons)
        assertNotEquals(group, copy)
    }
}
