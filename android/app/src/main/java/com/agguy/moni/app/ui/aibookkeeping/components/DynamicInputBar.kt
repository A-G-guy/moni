package com.agguy.moni.app.ui.aibookkeeping.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.MoniTheme

/**
 * 动态输入栏组件。
 *
 * 位于 AI 记账聊天界面底部，提供文本输入、附件添加、语音输入和发送功能。
 * 输入框支持多行（最多 4 行），输入内容时发送按钮自动变为可用状态。
 *
 * @param inputText 当前输入框文本
 * @param onInputChange 输入内容变化回调
 * @param onSendClick 发送按钮点击回调
 * @param modifier 可选的 modifier
 */
@Composable
fun DynamicInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 左侧：图片入口暂未开放，先使用贴合语义的图片添加图标提示能力边界。
        IconButton(
            onClick = {
                Toast.makeText(context, "即将上线", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.alpha(0.38f)
        ) {
            SymbolIcon(
                name = "add_photo_alternate",
                contentDescription = "添加图片",
                size = 24.dp
            )
        }

        // 中间：文本输入框，圆角、无边框、surfaceContainerHighest 底色
        TextField(
            value = inputText,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = "说点什么，如：请朋友喝奶茶32元",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = MaterialTheme.shapes.large,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // 右侧：语音输入按钮（当前不可用）
        IconButton(
            onClick = { /* 预留 */ },
            enabled = false
        ) {
            SymbolIcon(
                name = "adaptive_audio_mic",
                contentDescription = "语音输入",
                size = 24.dp
            )
        }

        // 最右侧：发送按钮（仅在有输入内容时可用）
        IconButton(
            onClick = onSendClick,
            enabled = inputText.isNotBlank()
        ) {
            SymbolIcon(
                name = "send",
                contentDescription = "发送",
                size = 24.dp
            )
        }
    }
}

@Preview(name = "DynamicInputBar - Light", showBackground = true)
@Composable
private fun DynamicInputBarPreviewLight() {
    MoniTheme {
        DynamicInputBar(
            inputText = "",
            onInputChange = {},
            onSendClick = {}
        )
    }
}

@Preview(
    name = "DynamicInputBar - Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DynamicInputBarPreviewDark() {
    MoniTheme {
        DynamicInputBar(
            inputText = "今天请朋友喝奶茶",
            onInputChange = {},
            onSendClick = {}
        )
    }
}
