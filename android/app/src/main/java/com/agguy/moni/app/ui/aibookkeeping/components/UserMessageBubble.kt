package com.agguy.moni.app.ui.aibookkeeping.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * 用户消息气泡组件。
 *
 * 气泡位于右侧，使用主题色 [MaterialTheme.colorScheme.primaryContainer] 作为背景，
 * 显示纯文本消息。圆角采用非对称设计（左上角更方，其他角圆润），模拟聊天气泡的"尾巴"效果。
 *
 * @param content 消息文本内容
 * @param modifier 可选的 modifier
 */
@Composable
fun UserMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 60.dp, end = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.75f),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Preview(name = "UserMessageBubble - Light", showBackground = true)
@Composable
private fun UserMessageBubblePreviewLight() {
    MoniTheme {
        UserMessageBubble(
            content = "今天中午吃了一碗牛肉面，花费25元。"
        )
    }
}

@Preview(name = "UserMessageBubble - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UserMessageBubblePreviewDark() {
    MoniTheme {
        UserMessageBubble(
            content = "今天中午吃了一碗牛肉面，花费25元。"
        )
    }
}
