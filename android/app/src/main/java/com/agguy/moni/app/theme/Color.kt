package com.agguy.moni.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// === 品牌基础色（供 Theme.kt 构建色板使用） ===

val PrimaryLight = Color(0xFF006C4C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF88F8C8)
val OnPrimaryContainerLight = Color(0xFF002114)
val SecondaryLight = Color(0xFF4D6357)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCFE9D9)
val OnSecondaryContainerLight = Color(0xFF092016)
val TertiaryLight = Color(0xFF3D6473)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC1E8FB)
val OnTertiaryContainerLight = Color(0xFF001F29)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFFBFDF8)
val OnBackgroundLight = Color(0xFF191C1A)
val SurfaceLight = Color(0xFFFBFDF8)
val OnSurfaceLight = Color(0xFF191C1A)
val SurfaceVariantLight = Color(0xFFDCE5DD)
val OnSurfaceVariantLight = Color(0xFF404943)
val OutlineLight = Color(0xFF707973)

// === 暗色主题基础色 ===

val PrimaryDark = Color(0xFF6DDD81)
val OnPrimaryDark = Color(0xFF003914)
val PrimaryContainerDark = Color(0xFF008738)
val OnPrimaryContainerDark = Color(0xFF6DDD81)
val SecondaryDark = Color(0xFFA5D2A6)
val OnSecondaryDark = Color(0xFF0F3819)
val SecondaryContainerDark = Color(0xFF1F4F29)
val OnSecondaryContainerDark = Color(0xFFA5D2A6)
val TertiaryDark = Color(0xFF7BCDD0)
val OnTertiaryDark = Color(0xFF003739)
val TertiaryContainerDark = Color(0xFF004F52)
val OnTertiaryContainerDark = Color(0xFF7BCDD0)
val ErrorDark = Color(0xFFFFB4A9)
val OnErrorDark = Color(0xFF680003)
val ErrorContainerDark = Color(0xFF930006)
val OnErrorContainerDark = Color(0xFFFFB4A9)
val BackgroundDark = Color(0xFF0F1511)
val OnBackgroundDark = Color(0xFFDFE4DE)
val SurfaceDark = Color(0xFF0F1511)
val OnSurfaceDark = Color(0xFFDFE4DE)
val SurfaceVariantDark = Color(0xFF1F2822)
val OnSurfaceVariantDark = Color(0xFFBFC9C1)
val OutlineDark = Color(0xFF89938D)

// === ColorScheme 语义化扩展属性（浅色/暗色自适应） ===

/**
 * 计算颜色的亮度（Material Design 公式）。
 */
private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}

/**
 * 判断当前色板是否为浅色模式。
 */
fun ColorScheme.isLight(): Boolean = this.background.luminance() > 0.5f

/**
 * 收入绿色：浅色下明亮绿，暗色下柔和青绿。
 */
val ColorScheme.incomeGreen: Color
    @Composable
    get() = if (isLight()) Color(0xFF00B894) else Color(0xFF7BCDD0)

/**
 * 支出红色：浅色下明亮红，暗色下柔和红。
 */
val ColorScheme.expenseRed: Color
    @Composable
    get() = if (isLight()) Color(0xFFFF6B6B) else Color(0xFFFFB4A9)

// === 向后兼容的硬编码常量（已弃用，请使用 ColorScheme 扩展属性） ===

@Deprecated("使用 MaterialTheme.colorScheme.incomeGreen 替代", ReplaceWith("MaterialTheme.colorScheme.incomeGreen"))
val IncomeGreen = Color(0xFF00B894)

@Deprecated("使用 MaterialTheme.colorScheme.expenseRed 替代", ReplaceWith("MaterialTheme.colorScheme.expenseRed"))
val ExpenseRed = Color(0xFFFF6B6B)
