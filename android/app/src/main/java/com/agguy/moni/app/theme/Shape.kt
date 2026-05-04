package com.agguy.moni.app.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Moni 应用的形状配置。
 *
 * 已升级到 Material 3 Expressive 2025 规范的圆角档位：
 * - large: 16dp → 20dp
 * - extraLarge: 28dp → 32dp
 * - 新增 Expressive 三档"增量"圆角：[Shapes.largeIncreased]、[Shapes.extraLargeIncreased]、
 *   [Shapes.extraExtraLarge]，用于 dialog、carousel item、hero 元素。
 */
val MoniShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
    extraExtraLarge = RoundedCornerShape(48.dp)
)
