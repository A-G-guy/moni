package com.agguy.moni.app.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Material Symbols 图标目录回归测试。
 */
class SymbolIconCatalogTest {

    @Test
    fun `all grouped icons should have codepoint`() {
        val missing = SymbolGroups.ALL_ICONS
            .map { it.name }
            .filterNot(SymbolCodepoints::contains)

        assertTrue("分组图标缺少 codepoint: $missing", missing.isEmpty())
    }

    @Test
    fun `grouped icon metadata should match codepoint table`() {
        val mismatched = SymbolGroups.ALL_ICONS.filter { icon ->
            SymbolCodepoints.get(icon.name) != icon.codepoint
        }

        assertTrue("分组图标 codepoint 与渲染表不一致: $mismatched", mismatched.isEmpty())
    }

    @Test
    fun `grouped icon names should be unique`() {
        val duplicateNames = SymbolGroups.ALL_GROUPS
            .flatMap { group -> group.icons.map { it.name } }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }

        assertTrue("分类图标选择器存在重复图标名: $duplicateNames", duplicateNames.isEmpty())
    }

    @Test
    fun `display names should be unique inside each group`() {
        val duplicateDisplayNames = SymbolGroups.ALL_GROUPS.mapNotNull { group ->
            val duplicates = group.icons
                .groupingBy { it.displayName }
                .eachCount()
                .filterValues { it > 1 }
            if (duplicates.isEmpty()) null else group.label to duplicates
        }

        assertTrue("同一分组内存在重复显示名: $duplicateDisplayNames", duplicateDisplayNames.isEmpty())
    }

    @Test
    fun `runtime referenced icons should not fallback`() {
        val fallback = SymbolCodepoints.get("more_horiz")
        val runtimeIcons = listOf(
            "add_photo_alternate",
            "auto_awesome",
            "cloud_download",
            "cloud_upload",
            "event_repeat",
            "folder_copy",
            "folder_open",
            "image",
            "search_off",
            "takeout_dining",
        )

        runtimeIcons.forEach { iconName ->
            assertTrue("运行时图标缺少 codepoint: $iconName", SymbolCodepoints.contains(iconName))
            assertTrue("运行时图标不应 fallback: $iconName", SymbolCodepoints.get(iconName) != fallback)
        }
    }

    @Test
    fun `unknown icon should use fallback`() {
        assertEquals(SymbolCodepoints.get("more_horiz"), SymbolCodepoints.get("unknown_icon_for_test"))
    }

    @Test
    fun `category keyword recommendation should return valid icons`() {
        val recommended = listOf(
            SymbolGroups.iconForCategory("AI 自动记账"),
            SymbolGroups.iconForCategory("外卖"),
            SymbolGroups.iconForCategory("工资"),
            SymbolGroups.iconForCategory("投资收益"),
            SymbolGroups.iconForCategory("午餐"),
        )

        val invalid = recommended.filterNot(SymbolCodepoints::contains)
        assertTrue("关键词推荐返回了无效图标: $invalid", invalid.isEmpty())
    }
}
