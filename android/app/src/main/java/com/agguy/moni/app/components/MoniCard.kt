@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive 中卡片的四种形态。Filled 与 Tonal 的区别在于背景层级，
 * Outlined 用于"轻量"分组，Elevated 是 expressive 推荐 hero 卡片。
 */
enum class MoniCardVariant {
    Filled,
    Tonal,
    Outlined,
    Elevated
}

/**
 * Moni 通用卡片容器。
 *
 * Material 3 Expressive 把 Card 拆成 Filled / Tonal / Outlined / Elevated 四种形态，
 * 通过 [variant] 选择。默认 [MoniCardVariant.Tonal]：surfaceContainerLow 背景、零阴影、
 * large 圆角（20dp），与 carousel maskClip 保持一致。
 *
 * @param modifier 修饰符
 * @param variant 卡片形态，默认 Tonal
 * @param onClick 点击回调（可选）
 * @param content 卡片内容
 */
@Composable
fun MoniCard(
    modifier: Modifier = Modifier,
    variant: MoniCardVariant = MoniCardVariant.Tonal,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clip(MaterialTheme.shapes.large).clickable(onClick = onClick)
    } else {
        modifier
    }

    when (variant) {
        MoniCardVariant.Filled -> Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) { content() }

        MoniCardVariant.Tonal -> Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) { content() }

        MoniCardVariant.Outlined -> OutlinedCard(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large
        ) { content() }

        MoniCardVariant.Elevated -> Card(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) { content() }
    }
}
