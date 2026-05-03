@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 主题模式枚举。
 */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * Moni 暗色色板。
 *
 * 基于 Material 3 暗色色彩体系手动构建，保持品牌绿色调一致性。
 */
private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF3F4A43),
    scrim = Color(0xFF000000),
    inverseSurface = OnBackgroundDark,
    inverseOnSurface = BackgroundDark,
    inversePrimary = PrimaryLight,
    surfaceTint = PrimaryDark,
    surfaceBright = Color(0xFF353B36),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF262C27),
    surfaceContainerHighest = Color(0xFF313732),
    surfaceContainerLow = Color(0xFF171D19),
    surfaceContainerLowest = Color(0xFF0A100C),
    surfaceDim = BackgroundDark
)

/**
 * 构建浅色色板。
 *
 * 优先使用 expressiveLightColorScheme() 获取高饱和度的 expressive 色板，
 * 再覆盖 primary 系列保持品牌深绿色。
 */
private fun buildLightColorScheme(): ColorScheme {
    return expressiveLightColorScheme().copy(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight
    )
}

/**
 * Moni 主题入口。
 *
 * @param themeMode 主题模式：浅色、深色或跟随系统
 * @param dynamicColor 是否启用 Android 12+ 动态颜色（默认关闭）
 * @param content 主题内容
 */
@Composable
fun MoniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> buildLightColorScheme()
    }

    val motionScheme = MotionScheme.expressive()

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MoniShapes,
        motionScheme = motionScheme,
        content = content
    )
}
