@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/**
 * 主题模式枚举。
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Moni 主题入口。
 *
 * 配色策略：
 * - 默认：官方 Expressive 配色（expressiveLightColorScheme / darkColorScheme）
 * - 动态色（Android 12+）：系统壁纸提取的 [dynamicLightColorScheme] / [dynamicDarkColorScheme]
 * - 预设配色：6 套 Material Theme Builder 导出的 Expressive 配色
 *
 * @param themeMode 主题模式：浅色、深色或跟随系统
 * @param presetColorScheme 预设配色方案（含 DEFAULT / DYNAMIC / 6 套自定义）
 * @param content 主题内容
 */
@Composable
fun MoniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    presetColorScheme: PresetColorScheme = PresetColorScheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = presetColorScheme.toColorScheme(darkTheme)

    val motionScheme = MotionScheme.expressive()

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MoniShapes,
        motionScheme = motionScheme,
        content = content
    )
}
