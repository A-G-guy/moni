@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

/**
 * 主题模式枚举。
 */
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/**
 * Moni 主题入口。
 *
 * @param themeMode 主题模式：浅色、深色或跟随系统
 * @param seedColor 种子色，用于生成 Material 3 色板（默认深青）
 * @param dynamicColor 是否启用 Android 12+ 动态颜色（默认关闭，优先级高于 seedColor）
 * @param content 主题内容
 */
@Composable
fun MoniTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color = DefaultSeedColor,
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
        else -> rememberDynamicColorScheme(seedColor = seedColor, isDark = darkTheme)
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
