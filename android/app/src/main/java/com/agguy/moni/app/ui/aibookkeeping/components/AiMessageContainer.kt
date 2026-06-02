package com.agguy.moni.app.ui.aibookkeeping.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.theme.MoniTheme

/**
 * AI 消息容器组件。
 *
 * 容器位于左侧，使用中性表面色作为背景，模拟 AI 聊天气泡效果。
 * 支持纯文本或自定义内容插槽。
 *
 * @param modifier 可选的 modifier
 * @param content 自定义内容
 */
@Composable
fun AiMessageContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 60.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            content()
        }
    }
}

/**
 * AI 消息容器组件（纯文本快捷版本）。
 *
 * @param text 消息文本内容
 * @param modifier 可选的 modifier
 */
@Composable
fun AiMessageContainer(
    text: String,
    modifier: Modifier = Modifier
) {
    AiMessageContainer(modifier = modifier) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Preview(name = "AiMessageContainer - Light", showBackground = true)
@Composable
private fun AiMessageContainerPreviewLight() {
    MoniTheme {
        AiMessageContainer(
            text = "已整理成待确认的记账卡片，请确认后保存。"
        )
    }
}

@Preview(
    name = "AiMessageContainer - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AiMessageContainerPreviewDark() {
    MoniTheme {
        AiMessageContainer(
            text = "已整理成待确认的记账卡片，请确认后保存。"
        )
    }
}

@Preview(
    name = "AiMessageContainer - Long Text",
    showBackground = true
)
@Composable
private fun AiMessageContainerPreviewLongText() {
    MoniTheme {
        AiMessageContainer(
            text = "这是一条较长的 AI 消息，用于测试容器在多行文本情况下的显示效果。消息气泡会根据内容自动扩展高度，但最大宽度被限制在屏幕宽度的75%以内，确保布局不会过于宽大。"
        )
    }
}
