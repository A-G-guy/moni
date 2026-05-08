package com.agguy.moni.app.theme

import com.agguy.moni.app.icons.SymbolGroups

/**
 * 按分类名称关键词匹配推荐图标名。
 * 返回图标名称字符串（供数据库存储）。
 */
fun iconForCategory(name: String): String = SymbolGroups.iconForCategory(name)

// ============================================================
// 图标分组数据结构（兼容层，委托到 SymbolGroups）
// ============================================================

/**
 * 单个图标的元数据。
 *
 * @property name 图标名称（存入数据库的标识）
 * @property displayName 中文显示名
 * @property codepoint Unicode codepoint
 */
data class CategoryIcon(val name: String, val displayName: String, val codepoint: Int)

/**
 * 图标分组。
 *
 * @property label 分组显示名称
 * @property icons 该分组下的图标列表
 */
data class IconGroup(val label: String, val icons: List<CategoryIcon>)

/**
 * 所有可供用户选择的分类图标，按主题分组。
 */
val GroupedCategoryIcons: List<IconGroup> = SymbolGroups.ALL_GROUPS.map { group ->
    IconGroup(
        label = group.label,
        icons = group.icons.map { CategoryIcon(it.name, it.displayName, it.codepoint) }
    )
}

/**
 * 扁平化的所有可供用户选择的分类图标列表。
 *
 * Pair 的 first 为 iconName（存入数据库），second 为中文显示名。
 */
val AvailableCategoryIcons: List<Pair<String, String>> =
    GroupedCategoryIcons.flatMap { group ->
        group.icons.map { it.name to it.displayName }
    }
