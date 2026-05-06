package com.agguy.moni.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// === 亮度辅助 ===

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
