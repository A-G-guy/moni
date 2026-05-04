package com.agguy.moni.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === 预设 Seed 色块 ===

/**
 * 可供用户选择的 8 个预设主题种子色。
 * 使用 Material Kolor `rememberDynamicColorScheme(seedColor)` 生成完整色板。
 */
val SeedSwatches = listOf(
    Color(0xFF0F5C5E), // 深青
    Color(0xFF4A3F8C), // 沉稳紫
    Color(0xFF1F3A6E), // 墨蓝
    Color(0xFF2E6A4D), // 森绿
    Color(0xFFB36A2E), // 暖橙
    Color(0xFFB14F77), // 玫瑰粉
    Color(0xFF6E4A2E), // 棕褐
    Color(0xFF2A7AA1), // 水青
)

/** 默认种子色：深青 */
val DefaultSeedColor = SeedSwatches[0]

// === 亮度辅助 ===

/** 计算颜色亮度。 */
fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}

/** 判断当前色板是否为浅色模式。 */
fun ColorScheme.isLight(): Boolean = this.background.luminance() > 0.5f

// === 语义色扩展（收入/支出） ===

/** 收入绿色：浅色下祖母绿，暗色下柔和青绿。 */
val ColorScheme.incomeGreen: Color
    @Composable
    get() = if (isLight()) Color(0xFF3A8A6E) else Color(0xFF7AC9A8)

/** 支出红色：浅色下深砖红，暗色下柔和粉红。 */
val ColorScheme.expenseRed: Color
    @Composable
    get() = if (isLight()) Color(0xFFB33A3A) else Color(0xFFE89999)

// === 向后兼容的硬编码常量（已弃用，请使用 ColorScheme 扩展属性） ===

@Deprecated("使用 MaterialTheme.colorScheme.incomeGreen 替代", ReplaceWith("MaterialTheme.colorScheme.incomeGreen"))
val IncomeGreen = Color(0xFF3A8A6E)

@Deprecated("使用 MaterialTheme.colorScheme.expenseRed 替代", ReplaceWith("MaterialTheme.colorScheme.expenseRed"))
val ExpenseRed = Color(0xFFB33A3A)
