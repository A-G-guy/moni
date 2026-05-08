package com.agguy.moni.app.icons

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/**
 * Material Symbols 字体图标 Composable。
 *
 * 通过 [name] 查找对应的 Unicode codepoint，以 [Text] 组件渲染字形。
 * 使用双静态字体方案（outlined / filled）切换状态，无需成对资源文件。
 *
 * @param name 图标名称（如 "restaurant"），对应 Material Symbols 官方命名。
 *             若找不到映射，自动 fallback 到 "more_horiz"。
 * @param contentDescription 无障碍描述；为 null 时使用 [name] 作为 fallback。
 * @param modifier 修饰符，通常通过 [Modifier.size] 控制图标容器尺寸。
 * @param tint 图标着色，默认跟随 [LocalContentColor]。
 * @param filled 是否使用 filled 变体，默认 outlined。
 * @param size 图标字号（dp 数值直接映射为 sp，Material Symbols 默认 1:1 对应）。
 */
@Composable
fun SymbolIcon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
    size: Dp = Dp.Unspecified,
) {
    val codepoint = SymbolCodepoints.get(name)
    val glyph = String(Character.toChars(codepoint))
    val fontFamily = if (filled) MaterialSymbolsFilled else MaterialSymbolsOutlined
    val semanticLabel = contentDescription ?: name

    val fontSize = if (size != Dp.Unspecified) {
        with(LocalDensity.current) { size.toSp() }
    } else {
        24.sp
    }

    Text(
        text = glyph,
        fontFamily = fontFamily,
        fontSize = fontSize,
        color = tint,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clearAndSetSemantics {
                this.contentDescription = semanticLabel
            }
    )
}
