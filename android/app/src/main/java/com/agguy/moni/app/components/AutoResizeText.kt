package com.agguy.moni.app.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 根据外层容器宽度动态收缩字号的单行文本。
 *
 * 适用于"容器尺寸固定但内容长度不固定"的场景（如饼图中央金额、徽标数字）。
 * 实现思路：
 * 1. 起始用 [maxFontSize] 渲染；
 * 2. 通过 onTextLayout 监听溢出，溢出则按 [shrinkStep] 逐步缩小字号；
 * 3. 直到不再溢出或达到 [minFontSize] 为止；
 * 4. 收敛之前用 drawWithContent 屏蔽实际绘制，避免可见抖动；
 * 5. text 或 maxFontSize 变化时重置整个收敛过程。
 *
 * 极端情况：若文字到达 [minFontSize] 仍溢出，按 [overflow] 处理。
 *
 * @param text 显示文本
 * @param maxFontSize 起始（最大）字号
 * @param minFontSize 收敛下限字号
 * @param style 基础文本样式（其 fontSize 会被覆盖）
 * @param color 文本颜色
 * @param textAlign 文本对齐
 * @param shrinkStep 每次溢出后字号缩减步长，默认 1sp
 * @param overflow 收敛到最小字号仍溢出时的回退策略，默认 [TextOverflow.Ellipsis]
 * @param modifier 修饰符
 */
@Composable
fun AutoResizeText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    shrinkStep: TextUnit = 1.sp,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    var fontSize by remember(text, maxFontSize, minFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize, minFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = style.copy(fontSize = fontSize),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = overflow,
        onTextLayout = { result ->
            if (!readyToDraw && result.didOverflowWidth) {
                val next = fontSize.value - shrinkStep.value
                if (next <= minFontSize.value) {
                    fontSize = minFontSize
                    readyToDraw = true
                } else {
                    fontSize = next.sp
                }
            } else if (!readyToDraw) {
                readyToDraw = true
            }
        }
    )
}
