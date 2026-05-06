@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
 * 配色策略完全基于官方 Material 3 Expressive API：
 * - 动态色（Android 12+）：使用系统壁纸提取的 [dynamicLightColorScheme] / [dynamicDarkColorScheme]
 * - 默认：使用官方 Expressive 默认配色 [expressiveLightColorScheme] / [expressiveDarkColorScheme]
 *
 * @param themeMode 主题模式：浅色、深色或跟随系统
 * @param dynamicColor 是否启用 Android 12+ 动态颜色
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
        else -> if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
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
