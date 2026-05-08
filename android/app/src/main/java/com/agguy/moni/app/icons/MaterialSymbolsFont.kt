package com.agguy.moni.app.icons

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.agguy.moni.R

/**
 * Material Symbols 静态字体族定义。
 *
 * 通过双静态字体方案（outlined + filled）替代可变字体的 FontVariation API，
 * 避免引入 ExperimentalTextApi，同时保持 API 28 兼容性。
 */
val MaterialSymbolsOutlined = FontFamily(
    Font(R.font.material_symbols_outlined)
)

val MaterialSymbolsFilled = FontFamily(
    Font(R.font.material_symbols_filled)
)
