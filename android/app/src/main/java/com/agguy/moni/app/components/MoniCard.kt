package com.agguy.moni.app.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Moni 通用卡片容器。
 *
 * 基于 Material 3 tonal elevation 理念：零阴影、surfaceContainerLow 背景、
 * large 圆角（16dp），用于列表项、设置项等场景。
 *
 * @param modifier 修饰符
 * @param onClick 点击回调（可选）
 * @param content 卡片内容
 */
@Composable
fun MoniCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        content()
    }
}
