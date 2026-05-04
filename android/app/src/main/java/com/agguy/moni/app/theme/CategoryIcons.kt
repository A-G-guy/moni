package com.agguy.moni.app.theme

import com.agguy.moni.app.icons.MoniIcons

/**
 * 分类名称到图标资源的映射。
 *
 * 支持中英文名称匹配，未命中时回退到通用分类图标。
 */
fun iconForCategory(name: String): Int = when (name.lowercase()) {
    "restaurant", "餐饮", "食物", "food", "吃饭", "美食" -> MoniIcons.Restaurant
    "transport", "交通", "出行", "打车", "公交", "地铁" -> MoniIcons.DirectionsCar
    "shopping", "购物", "消费", "买买买", "超市" -> MoniIcons.ShoppingBag
    "salary", "工资", "收入", "薪资", "奖金" -> MoniIcons.Payments
    "housing", "住房", "房租", "房贷", "物业" -> MoniIcons.Home
    "entertainment", "娱乐", "游戏", "电影", "休闲" -> MoniIcons.SportsEsports
    "medical", "医疗", "医院", "药品", "健康" -> MoniIcons.LocalHospital
    "education", "教育", "学习", "培训", "学费" -> MoniIcons.School
    "help" -> MoniIcons.Help
    else -> MoniIcons.Category
}

/**
 * 可供用户选择的分类图标列表。
 *
 * Pair 的 first 为 iconName（存入数据库），second 为图标资源 ID。
 */
val AvailableCategoryIcons: List<Pair<String, Int>> = listOf(
    "restaurant" to MoniIcons.Restaurant,
    "transport" to MoniIcons.DirectionsCar,
    "shopping" to MoniIcons.ShoppingBag,
    "salary" to MoniIcons.Payments,
    "housing" to MoniIcons.Home,
    "entertainment" to MoniIcons.SportsEsports,
    "medical" to MoniIcons.LocalHospital,
    "education" to MoniIcons.School,
    "category" to MoniIcons.Category,
)
