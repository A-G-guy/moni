package com.agguy.moni.app.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.agguy.moni.app.theme.presets.AirySakuraColors
import com.agguy.moni.app.theme.presets.AnimeSkyColors
import com.agguy.moni.app.theme.presets.CrispMintColors
import com.agguy.moni.app.theme.presets.NeonLavenderColors
import com.agguy.moni.app.theme.presets.OatmealGoldColors
import com.agguy.moni.app.theme.presets.SunsetCoralColors

/**
 * 预设配色方案枚举。
 *
 * 8 套配色：官方 Expressive 默认 + 系统动态颜色 + 6 套自定义配色。
 */
enum class PresetColorScheme {
    DEFAULT,
    DYNAMIC,
    AIRY_SAKURA,
    ANIME_SKY,
    CRISP_MINT,
    NEON_LAVENDER,
    OATMEAL_GOLD,
    SUNSET_CORAL
}

/** 中文显示名称。 */
val PresetColorScheme.displayName: String
    get() = when (this) {
        PresetColorScheme.DEFAULT -> "默认"
        PresetColorScheme.DYNAMIC -> "动态颜色"
        PresetColorScheme.AIRY_SAKURA -> "晴空樱粉"
        PresetColorScheme.ANIME_SKY -> "动漫天蓝"
        PresetColorScheme.CRISP_MINT -> "清新薄荷"
        PresetColorScheme.NEON_LAVENDER -> "霓虹薰衣草"
        PresetColorScheme.OATMEAL_GOLD -> "燕麦金"
        PresetColorScheme.SUNSET_CORAL -> "落日珊瑚"
    }

/**
 * 种子色，用于 UI 色块展示。
 *
 * 对于自定义配色，这是 Theme Builder 输入色（对应 `primaryContainerLight`）。
 * DEFAULT 使用 M3 默认紫，DYNAMIC 使用彩虹紫表示多彩动态。
 */
val PresetColorScheme.seedColor: Color
    get() = when (this) {
        PresetColorScheme.DEFAULT -> Color(0xFF6750A4)
        PresetColorScheme.DYNAMIC -> Color(0xFF9C27B0)
        PresetColorScheme.AIRY_SAKURA -> AirySakuraColors.primaryContainerLight
        PresetColorScheme.ANIME_SKY -> AnimeSkyColors.primaryContainerLight
        PresetColorScheme.CRISP_MINT -> CrispMintColors.primaryContainerLight
        PresetColorScheme.NEON_LAVENDER -> NeonLavenderColors.primaryContainerLight
        PresetColorScheme.OATMEAL_GOLD -> OatmealGoldColors.primaryContainerLight
        PresetColorScheme.SUNSET_CORAL -> SunsetCoralColors.primaryContainerLight
    }

/** M3 生成的主色，用于选中态边框等高亮场景。 */
val PresetColorScheme.primaryColor: Color
    get() = when (this) {
        PresetColorScheme.DEFAULT -> Color(0xFF6750A4)
        PresetColorScheme.DYNAMIC -> Color(0xFF7B1FA2)
        PresetColorScheme.AIRY_SAKURA -> AirySakuraColors.primaryLight
        PresetColorScheme.ANIME_SKY -> AnimeSkyColors.primaryLight
        PresetColorScheme.CRISP_MINT -> CrispMintColors.primaryLight
        PresetColorScheme.NEON_LAVENDER -> NeonLavenderColors.primaryLight
        PresetColorScheme.OATMEAL_GOLD -> OatmealGoldColors.primaryLight
        PresetColorScheme.SUNSET_CORAL -> SunsetCoralColors.primaryLight
    }

/** 是否需要系统壁纸动态色（Android 12+）。 */
fun PresetColorScheme.isDynamic(): Boolean = this == PresetColorScheme.DYNAMIC

/** 非 Composable 浅色 ColorScheme（供测试与非 UI 场景）。 */
fun PresetColorScheme.toLightColorScheme(): ColorScheme = when (this) {
    PresetColorScheme.DEFAULT -> expressiveLightColorScheme()
    PresetColorScheme.DYNAMIC -> expressiveLightColorScheme()
    PresetColorScheme.AIRY_SAKURA -> airySakuraLight()
    PresetColorScheme.ANIME_SKY -> animeSkyLight()
    PresetColorScheme.CRISP_MINT -> crispMintLight()
    PresetColorScheme.NEON_LAVENDER -> neonLavenderLight()
    PresetColorScheme.OATMEAL_GOLD -> oatmealGoldLight()
    PresetColorScheme.SUNSET_CORAL -> sunsetCoralLight()
}

/** 非 Composable 深色 ColorScheme（供测试与非 UI 场景）。 */
fun PresetColorScheme.toDarkColorScheme(): ColorScheme = when (this) {
    PresetColorScheme.DEFAULT -> darkColorScheme()
    PresetColorScheme.DYNAMIC -> darkColorScheme()
    PresetColorScheme.AIRY_SAKURA -> airySakuraDark()
    PresetColorScheme.ANIME_SKY -> animeSkyDark()
    PresetColorScheme.CRISP_MINT -> crispMintDark()
    PresetColorScheme.NEON_LAVENDER -> neonLavenderDark()
    PresetColorScheme.OATMEAL_GOLD -> oatmealGoldDark()
    PresetColorScheme.SUNSET_CORAL -> sunsetCoralDark()
}

/**
 * 解析配色方案为 ColorScheme。
 *
 * DYNAMIC 分支需要 [LocalContext] 以获取系统壁纸动态色（Android 12+），
 * 其余分支委托给非 Composable 版本。
 *
 * @param darkTheme 是否深色模式
 */
@Composable
fun PresetColorScheme.toColorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    PresetColorScheme.DYNAMIC -> {
        val context = LocalContext.current
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
    }
    else -> if (darkTheme) toDarkColorScheme() else toLightColorScheme()
}

// === Airy Sakura ===

private fun airySakuraLight(): ColorScheme = lightColorScheme(
    primary = AirySakuraColors.primaryLight,
    onPrimary = AirySakuraColors.onPrimaryLight,
    primaryContainer = AirySakuraColors.primaryContainerLight,
    onPrimaryContainer = AirySakuraColors.onPrimaryContainerLight,
    secondary = AirySakuraColors.secondaryLight,
    onSecondary = AirySakuraColors.onSecondaryLight,
    secondaryContainer = AirySakuraColors.secondaryContainerLight,
    onSecondaryContainer = AirySakuraColors.onSecondaryContainerLight,
    tertiary = AirySakuraColors.tertiaryLight,
    onTertiary = AirySakuraColors.onTertiaryLight,
    tertiaryContainer = AirySakuraColors.tertiaryContainerLight,
    onTertiaryContainer = AirySakuraColors.onTertiaryContainerLight,
    error = AirySakuraColors.errorLight,
    onError = AirySakuraColors.onErrorLight,
    errorContainer = AirySakuraColors.errorContainerLight,
    onErrorContainer = AirySakuraColors.onErrorContainerLight,
    background = AirySakuraColors.backgroundLight,
    onBackground = AirySakuraColors.onBackgroundLight,
    surface = AirySakuraColors.surfaceLight,
    onSurface = AirySakuraColors.onSurfaceLight,
    surfaceVariant = AirySakuraColors.surfaceVariantLight,
    onSurfaceVariant = AirySakuraColors.onSurfaceVariantLight,
    outline = AirySakuraColors.outlineLight,
    outlineVariant = AirySakuraColors.outlineVariantLight,
    scrim = AirySakuraColors.scrimLight,
    inverseSurface = AirySakuraColors.inverseSurfaceLight,
    inverseOnSurface = AirySakuraColors.inverseOnSurfaceLight,
    inversePrimary = AirySakuraColors.inversePrimaryLight,
    surfaceDim = AirySakuraColors.surfaceDimLight,
    surfaceBright = AirySakuraColors.surfaceBrightLight,
    surfaceContainerLowest = AirySakuraColors.surfaceContainerLowestLight,
    surfaceContainerLow = AirySakuraColors.surfaceContainerLowLight,
    surfaceContainer = AirySakuraColors.surfaceContainerLight,
    surfaceContainerHigh = AirySakuraColors.surfaceContainerHighLight,
    surfaceContainerHighest = AirySakuraColors.surfaceContainerHighestLight,
)

private fun airySakuraDark(): ColorScheme = darkColorScheme(
    primary = AirySakuraColors.primaryDark,
    onPrimary = AirySakuraColors.onPrimaryDark,
    primaryContainer = AirySakuraColors.primaryContainerDark,
    onPrimaryContainer = AirySakuraColors.onPrimaryContainerDark,
    secondary = AirySakuraColors.secondaryDark,
    onSecondary = AirySakuraColors.onSecondaryDark,
    secondaryContainer = AirySakuraColors.secondaryContainerDark,
    onSecondaryContainer = AirySakuraColors.onSecondaryContainerDark,
    tertiary = AirySakuraColors.tertiaryDark,
    onTertiary = AirySakuraColors.onTertiaryDark,
    tertiaryContainer = AirySakuraColors.tertiaryContainerDark,
    onTertiaryContainer = AirySakuraColors.onTertiaryContainerDark,
    error = AirySakuraColors.errorDark,
    onError = AirySakuraColors.onErrorDark,
    errorContainer = AirySakuraColors.errorContainerDark,
    onErrorContainer = AirySakuraColors.onErrorContainerDark,
    background = AirySakuraColors.backgroundDark,
    onBackground = AirySakuraColors.onBackgroundDark,
    surface = AirySakuraColors.surfaceDark,
    onSurface = AirySakuraColors.onSurfaceDark,
    surfaceVariant = AirySakuraColors.surfaceVariantDark,
    onSurfaceVariant = AirySakuraColors.onSurfaceVariantDark,
    outline = AirySakuraColors.outlineDark,
    outlineVariant = AirySakuraColors.outlineVariantDark,
    scrim = AirySakuraColors.scrimDark,
    inverseSurface = AirySakuraColors.inverseSurfaceDark,
    inverseOnSurface = AirySakuraColors.inverseOnSurfaceDark,
    inversePrimary = AirySakuraColors.inversePrimaryDark,
    surfaceDim = AirySakuraColors.surfaceDimDark,
    surfaceBright = AirySakuraColors.surfaceBrightDark,
    surfaceContainerLowest = AirySakuraColors.surfaceContainerLowestDark,
    surfaceContainerLow = AirySakuraColors.surfaceContainerLowDark,
    surfaceContainer = AirySakuraColors.surfaceContainerDark,
    surfaceContainerHigh = AirySakuraColors.surfaceContainerHighDark,
    surfaceContainerHighest = AirySakuraColors.surfaceContainerHighestDark,
)

// === Anime Sky ===

private fun animeSkyLight(): ColorScheme = lightColorScheme(
    primary = AnimeSkyColors.primaryLight,
    onPrimary = AnimeSkyColors.onPrimaryLight,
    primaryContainer = AnimeSkyColors.primaryContainerLight,
    onPrimaryContainer = AnimeSkyColors.onPrimaryContainerLight,
    secondary = AnimeSkyColors.secondaryLight,
    onSecondary = AnimeSkyColors.onSecondaryLight,
    secondaryContainer = AnimeSkyColors.secondaryContainerLight,
    onSecondaryContainer = AnimeSkyColors.onSecondaryContainerLight,
    tertiary = AnimeSkyColors.tertiaryLight,
    onTertiary = AnimeSkyColors.onTertiaryLight,
    tertiaryContainer = AnimeSkyColors.tertiaryContainerLight,
    onTertiaryContainer = AnimeSkyColors.onTertiaryContainerLight,
    error = AnimeSkyColors.errorLight,
    onError = AnimeSkyColors.onErrorLight,
    errorContainer = AnimeSkyColors.errorContainerLight,
    onErrorContainer = AnimeSkyColors.onErrorContainerLight,
    background = AnimeSkyColors.backgroundLight,
    onBackground = AnimeSkyColors.onBackgroundLight,
    surface = AnimeSkyColors.surfaceLight,
    onSurface = AnimeSkyColors.onSurfaceLight,
    surfaceVariant = AnimeSkyColors.surfaceVariantLight,
    onSurfaceVariant = AnimeSkyColors.onSurfaceVariantLight,
    outline = AnimeSkyColors.outlineLight,
    outlineVariant = AnimeSkyColors.outlineVariantLight,
    scrim = AnimeSkyColors.scrimLight,
    inverseSurface = AnimeSkyColors.inverseSurfaceLight,
    inverseOnSurface = AnimeSkyColors.inverseOnSurfaceLight,
    inversePrimary = AnimeSkyColors.inversePrimaryLight,
    surfaceDim = AnimeSkyColors.surfaceDimLight,
    surfaceBright = AnimeSkyColors.surfaceBrightLight,
    surfaceContainerLowest = AnimeSkyColors.surfaceContainerLowestLight,
    surfaceContainerLow = AnimeSkyColors.surfaceContainerLowLight,
    surfaceContainer = AnimeSkyColors.surfaceContainerLight,
    surfaceContainerHigh = AnimeSkyColors.surfaceContainerHighLight,
    surfaceContainerHighest = AnimeSkyColors.surfaceContainerHighestLight,
)

private fun animeSkyDark(): ColorScheme = darkColorScheme(
    primary = AnimeSkyColors.primaryDark,
    onPrimary = AnimeSkyColors.onPrimaryDark,
    primaryContainer = AnimeSkyColors.primaryContainerDark,
    onPrimaryContainer = AnimeSkyColors.onPrimaryContainerDark,
    secondary = AnimeSkyColors.secondaryDark,
    onSecondary = AnimeSkyColors.onSecondaryDark,
    secondaryContainer = AnimeSkyColors.secondaryContainerDark,
    onSecondaryContainer = AnimeSkyColors.onSecondaryContainerDark,
    tertiary = AnimeSkyColors.tertiaryDark,
    onTertiary = AnimeSkyColors.onTertiaryDark,
    tertiaryContainer = AnimeSkyColors.tertiaryContainerDark,
    onTertiaryContainer = AnimeSkyColors.onTertiaryContainerDark,
    error = AnimeSkyColors.errorDark,
    onError = AnimeSkyColors.onErrorDark,
    errorContainer = AnimeSkyColors.errorContainerDark,
    onErrorContainer = AnimeSkyColors.onErrorContainerDark,
    background = AnimeSkyColors.backgroundDark,
    onBackground = AnimeSkyColors.onBackgroundDark,
    surface = AnimeSkyColors.surfaceDark,
    onSurface = AnimeSkyColors.onSurfaceDark,
    surfaceVariant = AnimeSkyColors.surfaceVariantDark,
    onSurfaceVariant = AnimeSkyColors.onSurfaceVariantDark,
    outline = AnimeSkyColors.outlineDark,
    outlineVariant = AnimeSkyColors.outlineVariantDark,
    scrim = AnimeSkyColors.scrimDark,
    inverseSurface = AnimeSkyColors.inverseSurfaceDark,
    inverseOnSurface = AnimeSkyColors.inverseOnSurfaceDark,
    inversePrimary = AnimeSkyColors.inversePrimaryDark,
    surfaceDim = AnimeSkyColors.surfaceDimDark,
    surfaceBright = AnimeSkyColors.surfaceBrightDark,
    surfaceContainerLowest = AnimeSkyColors.surfaceContainerLowestDark,
    surfaceContainerLow = AnimeSkyColors.surfaceContainerLowDark,
    surfaceContainer = AnimeSkyColors.surfaceContainerDark,
    surfaceContainerHigh = AnimeSkyColors.surfaceContainerHighDark,
    surfaceContainerHighest = AnimeSkyColors.surfaceContainerHighestDark,
)

// === Crisp Mint ===

private fun crispMintLight(): ColorScheme = lightColorScheme(
    primary = CrispMintColors.primaryLight,
    onPrimary = CrispMintColors.onPrimaryLight,
    primaryContainer = CrispMintColors.primaryContainerLight,
    onPrimaryContainer = CrispMintColors.onPrimaryContainerLight,
    secondary = CrispMintColors.secondaryLight,
    onSecondary = CrispMintColors.onSecondaryLight,
    secondaryContainer = CrispMintColors.secondaryContainerLight,
    onSecondaryContainer = CrispMintColors.onSecondaryContainerLight,
    tertiary = CrispMintColors.tertiaryLight,
    onTertiary = CrispMintColors.onTertiaryLight,
    tertiaryContainer = CrispMintColors.tertiaryContainerLight,
    onTertiaryContainer = CrispMintColors.onTertiaryContainerLight,
    error = CrispMintColors.errorLight,
    onError = CrispMintColors.onErrorLight,
    errorContainer = CrispMintColors.errorContainerLight,
    onErrorContainer = CrispMintColors.onErrorContainerLight,
    background = CrispMintColors.backgroundLight,
    onBackground = CrispMintColors.onBackgroundLight,
    surface = CrispMintColors.surfaceLight,
    onSurface = CrispMintColors.onSurfaceLight,
    surfaceVariant = CrispMintColors.surfaceVariantLight,
    onSurfaceVariant = CrispMintColors.onSurfaceVariantLight,
    outline = CrispMintColors.outlineLight,
    outlineVariant = CrispMintColors.outlineVariantLight,
    scrim = CrispMintColors.scrimLight,
    inverseSurface = CrispMintColors.inverseSurfaceLight,
    inverseOnSurface = CrispMintColors.inverseOnSurfaceLight,
    inversePrimary = CrispMintColors.inversePrimaryLight,
    surfaceDim = CrispMintColors.surfaceDimLight,
    surfaceBright = CrispMintColors.surfaceBrightLight,
    surfaceContainerLowest = CrispMintColors.surfaceContainerLowestLight,
    surfaceContainerLow = CrispMintColors.surfaceContainerLowLight,
    surfaceContainer = CrispMintColors.surfaceContainerLight,
    surfaceContainerHigh = CrispMintColors.surfaceContainerHighLight,
    surfaceContainerHighest = CrispMintColors.surfaceContainerHighestLight,
)

private fun crispMintDark(): ColorScheme = darkColorScheme(
    primary = CrispMintColors.primaryDark,
    onPrimary = CrispMintColors.onPrimaryDark,
    primaryContainer = CrispMintColors.primaryContainerDark,
    onPrimaryContainer = CrispMintColors.onPrimaryContainerDark,
    secondary = CrispMintColors.secondaryDark,
    onSecondary = CrispMintColors.onSecondaryDark,
    secondaryContainer = CrispMintColors.secondaryContainerDark,
    onSecondaryContainer = CrispMintColors.onSecondaryContainerDark,
    tertiary = CrispMintColors.tertiaryDark,
    onTertiary = CrispMintColors.onTertiaryDark,
    tertiaryContainer = CrispMintColors.tertiaryContainerDark,
    onTertiaryContainer = CrispMintColors.onTertiaryContainerDark,
    error = CrispMintColors.errorDark,
    onError = CrispMintColors.onErrorDark,
    errorContainer = CrispMintColors.errorContainerDark,
    onErrorContainer = CrispMintColors.onErrorContainerDark,
    background = CrispMintColors.backgroundDark,
    onBackground = CrispMintColors.onBackgroundDark,
    surface = CrispMintColors.surfaceDark,
    onSurface = CrispMintColors.onSurfaceDark,
    surfaceVariant = CrispMintColors.surfaceVariantDark,
    onSurfaceVariant = CrispMintColors.onSurfaceVariantDark,
    outline = CrispMintColors.outlineDark,
    outlineVariant = CrispMintColors.outlineVariantDark,
    scrim = CrispMintColors.scrimDark,
    inverseSurface = CrispMintColors.inverseSurfaceDark,
    inverseOnSurface = CrispMintColors.inverseOnSurfaceDark,
    inversePrimary = CrispMintColors.inversePrimaryDark,
    surfaceDim = CrispMintColors.surfaceDimDark,
    surfaceBright = CrispMintColors.surfaceBrightDark,
    surfaceContainerLowest = CrispMintColors.surfaceContainerLowestDark,
    surfaceContainerLow = CrispMintColors.surfaceContainerLowDark,
    surfaceContainer = CrispMintColors.surfaceContainerDark,
    surfaceContainerHigh = CrispMintColors.surfaceContainerHighDark,
    surfaceContainerHighest = CrispMintColors.surfaceContainerHighestDark,
)

// === Neon Lavender ===

private fun neonLavenderLight(): ColorScheme = lightColorScheme(
    primary = NeonLavenderColors.primaryLight,
    onPrimary = NeonLavenderColors.onPrimaryLight,
    primaryContainer = NeonLavenderColors.primaryContainerLight,
    onPrimaryContainer = NeonLavenderColors.onPrimaryContainerLight,
    secondary = NeonLavenderColors.secondaryLight,
    onSecondary = NeonLavenderColors.onSecondaryLight,
    secondaryContainer = NeonLavenderColors.secondaryContainerLight,
    onSecondaryContainer = NeonLavenderColors.onSecondaryContainerLight,
    tertiary = NeonLavenderColors.tertiaryLight,
    onTertiary = NeonLavenderColors.onTertiaryLight,
    tertiaryContainer = NeonLavenderColors.tertiaryContainerLight,
    onTertiaryContainer = NeonLavenderColors.onTertiaryContainerLight,
    error = NeonLavenderColors.errorLight,
    onError = NeonLavenderColors.onErrorLight,
    errorContainer = NeonLavenderColors.errorContainerLight,
    onErrorContainer = NeonLavenderColors.onErrorContainerLight,
    background = NeonLavenderColors.backgroundLight,
    onBackground = NeonLavenderColors.onBackgroundLight,
    surface = NeonLavenderColors.surfaceLight,
    onSurface = NeonLavenderColors.onSurfaceLight,
    surfaceVariant = NeonLavenderColors.surfaceVariantLight,
    onSurfaceVariant = NeonLavenderColors.onSurfaceVariantLight,
    outline = NeonLavenderColors.outlineLight,
    outlineVariant = NeonLavenderColors.outlineVariantLight,
    scrim = NeonLavenderColors.scrimLight,
    inverseSurface = NeonLavenderColors.inverseSurfaceLight,
    inverseOnSurface = NeonLavenderColors.inverseOnSurfaceLight,
    inversePrimary = NeonLavenderColors.inversePrimaryLight,
    surfaceDim = NeonLavenderColors.surfaceDimLight,
    surfaceBright = NeonLavenderColors.surfaceBrightLight,
    surfaceContainerLowest = NeonLavenderColors.surfaceContainerLowestLight,
    surfaceContainerLow = NeonLavenderColors.surfaceContainerLowLight,
    surfaceContainer = NeonLavenderColors.surfaceContainerLight,
    surfaceContainerHigh = NeonLavenderColors.surfaceContainerHighLight,
    surfaceContainerHighest = NeonLavenderColors.surfaceContainerHighestLight,
)

private fun neonLavenderDark(): ColorScheme = darkColorScheme(
    primary = NeonLavenderColors.primaryDark,
    onPrimary = NeonLavenderColors.onPrimaryDark,
    primaryContainer = NeonLavenderColors.primaryContainerDark,
    onPrimaryContainer = NeonLavenderColors.onPrimaryContainerDark,
    secondary = NeonLavenderColors.secondaryDark,
    onSecondary = NeonLavenderColors.onSecondaryDark,
    secondaryContainer = NeonLavenderColors.secondaryContainerDark,
    onSecondaryContainer = NeonLavenderColors.onSecondaryContainerDark,
    tertiary = NeonLavenderColors.tertiaryDark,
    onTertiary = NeonLavenderColors.onTertiaryDark,
    tertiaryContainer = NeonLavenderColors.tertiaryContainerDark,
    onTertiaryContainer = NeonLavenderColors.onTertiaryContainerDark,
    error = NeonLavenderColors.errorDark,
    onError = NeonLavenderColors.onErrorDark,
    errorContainer = NeonLavenderColors.errorContainerDark,
    onErrorContainer = NeonLavenderColors.onErrorContainerDark,
    background = NeonLavenderColors.backgroundDark,
    onBackground = NeonLavenderColors.onBackgroundDark,
    surface = NeonLavenderColors.surfaceDark,
    onSurface = NeonLavenderColors.onSurfaceDark,
    surfaceVariant = NeonLavenderColors.surfaceVariantDark,
    onSurfaceVariant = NeonLavenderColors.onSurfaceVariantDark,
    outline = NeonLavenderColors.outlineDark,
    outlineVariant = NeonLavenderColors.outlineVariantDark,
    scrim = NeonLavenderColors.scrimDark,
    inverseSurface = NeonLavenderColors.inverseSurfaceDark,
    inverseOnSurface = NeonLavenderColors.inverseOnSurfaceDark,
    inversePrimary = NeonLavenderColors.inversePrimaryDark,
    surfaceDim = NeonLavenderColors.surfaceDimDark,
    surfaceBright = NeonLavenderColors.surfaceBrightDark,
    surfaceContainerLowest = NeonLavenderColors.surfaceContainerLowestDark,
    surfaceContainerLow = NeonLavenderColors.surfaceContainerLowDark,
    surfaceContainer = NeonLavenderColors.surfaceContainerDark,
    surfaceContainerHigh = NeonLavenderColors.surfaceContainerHighDark,
    surfaceContainerHighest = NeonLavenderColors.surfaceContainerHighestDark,
)

// === Oatmeal Gold ===

private fun oatmealGoldLight(): ColorScheme = lightColorScheme(
    primary = OatmealGoldColors.primaryLight,
    onPrimary = OatmealGoldColors.onPrimaryLight,
    primaryContainer = OatmealGoldColors.primaryContainerLight,
    onPrimaryContainer = OatmealGoldColors.onPrimaryContainerLight,
    secondary = OatmealGoldColors.secondaryLight,
    onSecondary = OatmealGoldColors.onSecondaryLight,
    secondaryContainer = OatmealGoldColors.secondaryContainerLight,
    onSecondaryContainer = OatmealGoldColors.onSecondaryContainerLight,
    tertiary = OatmealGoldColors.tertiaryLight,
    onTertiary = OatmealGoldColors.onTertiaryLight,
    tertiaryContainer = OatmealGoldColors.tertiaryContainerLight,
    onTertiaryContainer = OatmealGoldColors.onTertiaryContainerLight,
    error = OatmealGoldColors.errorLight,
    onError = OatmealGoldColors.onErrorLight,
    errorContainer = OatmealGoldColors.errorContainerLight,
    onErrorContainer = OatmealGoldColors.onErrorContainerLight,
    background = OatmealGoldColors.backgroundLight,
    onBackground = OatmealGoldColors.onBackgroundLight,
    surface = OatmealGoldColors.surfaceLight,
    onSurface = OatmealGoldColors.onSurfaceLight,
    surfaceVariant = OatmealGoldColors.surfaceVariantLight,
    onSurfaceVariant = OatmealGoldColors.onSurfaceVariantLight,
    outline = OatmealGoldColors.outlineLight,
    outlineVariant = OatmealGoldColors.outlineVariantLight,
    scrim = OatmealGoldColors.scrimLight,
    inverseSurface = OatmealGoldColors.inverseSurfaceLight,
    inverseOnSurface = OatmealGoldColors.inverseOnSurfaceLight,
    inversePrimary = OatmealGoldColors.inversePrimaryLight,
    surfaceDim = OatmealGoldColors.surfaceDimLight,
    surfaceBright = OatmealGoldColors.surfaceBrightLight,
    surfaceContainerLowest = OatmealGoldColors.surfaceContainerLowestLight,
    surfaceContainerLow = OatmealGoldColors.surfaceContainerLowLight,
    surfaceContainer = OatmealGoldColors.surfaceContainerLight,
    surfaceContainerHigh = OatmealGoldColors.surfaceContainerHighLight,
    surfaceContainerHighest = OatmealGoldColors.surfaceContainerHighestLight,
)

private fun oatmealGoldDark(): ColorScheme = darkColorScheme(
    primary = OatmealGoldColors.primaryDark,
    onPrimary = OatmealGoldColors.onPrimaryDark,
    primaryContainer = OatmealGoldColors.primaryContainerDark,
    onPrimaryContainer = OatmealGoldColors.onPrimaryContainerDark,
    secondary = OatmealGoldColors.secondaryDark,
    onSecondary = OatmealGoldColors.onSecondaryDark,
    secondaryContainer = OatmealGoldColors.secondaryContainerDark,
    onSecondaryContainer = OatmealGoldColors.onSecondaryContainerDark,
    tertiary = OatmealGoldColors.tertiaryDark,
    onTertiary = OatmealGoldColors.onTertiaryDark,
    tertiaryContainer = OatmealGoldColors.tertiaryContainerDark,
    onTertiaryContainer = OatmealGoldColors.onTertiaryContainerDark,
    error = OatmealGoldColors.errorDark,
    onError = OatmealGoldColors.onErrorDark,
    errorContainer = OatmealGoldColors.errorContainerDark,
    onErrorContainer = OatmealGoldColors.onErrorContainerDark,
    background = OatmealGoldColors.backgroundDark,
    onBackground = OatmealGoldColors.onBackgroundDark,
    surface = OatmealGoldColors.surfaceDark,
    onSurface = OatmealGoldColors.onSurfaceDark,
    surfaceVariant = OatmealGoldColors.surfaceVariantDark,
    onSurfaceVariant = OatmealGoldColors.onSurfaceVariantDark,
    outline = OatmealGoldColors.outlineDark,
    outlineVariant = OatmealGoldColors.outlineVariantDark,
    scrim = OatmealGoldColors.scrimDark,
    inverseSurface = OatmealGoldColors.inverseSurfaceDark,
    inverseOnSurface = OatmealGoldColors.inverseOnSurfaceDark,
    inversePrimary = OatmealGoldColors.inversePrimaryDark,
    surfaceDim = OatmealGoldColors.surfaceDimDark,
    surfaceBright = OatmealGoldColors.surfaceBrightDark,
    surfaceContainerLowest = OatmealGoldColors.surfaceContainerLowestDark,
    surfaceContainerLow = OatmealGoldColors.surfaceContainerLowDark,
    surfaceContainer = OatmealGoldColors.surfaceContainerDark,
    surfaceContainerHigh = OatmealGoldColors.surfaceContainerHighDark,
    surfaceContainerHighest = OatmealGoldColors.surfaceContainerHighestDark,
)

// === Sunset Coral ===

private fun sunsetCoralLight(): ColorScheme = lightColorScheme(
    primary = SunsetCoralColors.primaryLight,
    onPrimary = SunsetCoralColors.onPrimaryLight,
    primaryContainer = SunsetCoralColors.primaryContainerLight,
    onPrimaryContainer = SunsetCoralColors.onPrimaryContainerLight,
    secondary = SunsetCoralColors.secondaryLight,
    onSecondary = SunsetCoralColors.onSecondaryLight,
    secondaryContainer = SunsetCoralColors.secondaryContainerLight,
    onSecondaryContainer = SunsetCoralColors.onSecondaryContainerLight,
    tertiary = SunsetCoralColors.tertiaryLight,
    onTertiary = SunsetCoralColors.onTertiaryLight,
    tertiaryContainer = SunsetCoralColors.tertiaryContainerLight,
    onTertiaryContainer = SunsetCoralColors.onTertiaryContainerLight,
    error = SunsetCoralColors.errorLight,
    onError = SunsetCoralColors.onErrorLight,
    errorContainer = SunsetCoralColors.errorContainerLight,
    onErrorContainer = SunsetCoralColors.onErrorContainerLight,
    background = SunsetCoralColors.backgroundLight,
    onBackground = SunsetCoralColors.onBackgroundLight,
    surface = SunsetCoralColors.surfaceLight,
    onSurface = SunsetCoralColors.onSurfaceLight,
    surfaceVariant = SunsetCoralColors.surfaceVariantLight,
    onSurfaceVariant = SunsetCoralColors.onSurfaceVariantLight,
    outline = SunsetCoralColors.outlineLight,
    outlineVariant = SunsetCoralColors.outlineVariantLight,
    scrim = SunsetCoralColors.scrimLight,
    inverseSurface = SunsetCoralColors.inverseSurfaceLight,
    inverseOnSurface = SunsetCoralColors.inverseOnSurfaceLight,
    inversePrimary = SunsetCoralColors.inversePrimaryLight,
    surfaceDim = SunsetCoralColors.surfaceDimLight,
    surfaceBright = SunsetCoralColors.surfaceBrightLight,
    surfaceContainerLowest = SunsetCoralColors.surfaceContainerLowestLight,
    surfaceContainerLow = SunsetCoralColors.surfaceContainerLowLight,
    surfaceContainer = SunsetCoralColors.surfaceContainerLight,
    surfaceContainerHigh = SunsetCoralColors.surfaceContainerHighLight,
    surfaceContainerHighest = SunsetCoralColors.surfaceContainerHighestLight,
)

private fun sunsetCoralDark(): ColorScheme = darkColorScheme(
    primary = SunsetCoralColors.primaryDark,
    onPrimary = SunsetCoralColors.onPrimaryDark,
    primaryContainer = SunsetCoralColors.primaryContainerDark,
    onPrimaryContainer = SunsetCoralColors.onPrimaryContainerDark,
    secondary = SunsetCoralColors.secondaryDark,
    onSecondary = SunsetCoralColors.onSecondaryDark,
    secondaryContainer = SunsetCoralColors.secondaryContainerDark,
    onSecondaryContainer = SunsetCoralColors.onSecondaryContainerDark,
    tertiary = SunsetCoralColors.tertiaryDark,
    onTertiary = SunsetCoralColors.onTertiaryDark,
    tertiaryContainer = SunsetCoralColors.tertiaryContainerDark,
    onTertiaryContainer = SunsetCoralColors.onTertiaryContainerDark,
    error = SunsetCoralColors.errorDark,
    onError = SunsetCoralColors.onErrorDark,
    errorContainer = SunsetCoralColors.errorContainerDark,
    onErrorContainer = SunsetCoralColors.onErrorContainerDark,
    background = SunsetCoralColors.backgroundDark,
    onBackground = SunsetCoralColors.onBackgroundDark,
    surface = SunsetCoralColors.surfaceDark,
    onSurface = SunsetCoralColors.onSurfaceDark,
    surfaceVariant = SunsetCoralColors.surfaceVariantDark,
    onSurfaceVariant = SunsetCoralColors.onSurfaceVariantDark,
    outline = SunsetCoralColors.outlineDark,
    outlineVariant = SunsetCoralColors.outlineVariantDark,
    scrim = SunsetCoralColors.scrimDark,
    inverseSurface = SunsetCoralColors.inverseSurfaceDark,
    inverseOnSurface = SunsetCoralColors.inverseOnSurfaceDark,
    inversePrimary = SunsetCoralColors.inversePrimaryDark,
    surfaceDim = SunsetCoralColors.surfaceDimDark,
    surfaceBright = SunsetCoralColors.surfaceBrightDark,
    surfaceContainerLowest = SunsetCoralColors.surfaceContainerLowestDark,
    surfaceContainerLow = SunsetCoralColors.surfaceContainerLowDark,
    surfaceContainer = SunsetCoralColors.surfaceContainerDark,
    surfaceContainerHigh = SunsetCoralColors.surfaceContainerHighDark,
    surfaceContainerHighest = SunsetCoralColors.surfaceContainerHighestDark,
)
