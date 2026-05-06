@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agguy.moni.core.RecordType

/**
 * 4×4 自定义多功能数字键盘。
 *
 * 数字与运算符完美融合，彻底替代系统数字键盘。
 */
@Composable
fun CustomNumPad(
    recordType: RecordType,
    amountExpression: String,
    amountCents: Long,
    hasSelectedCategory: Boolean,
    onDigitClick: (String) -> Unit,
    onOperatorClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onCalculate: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPendingOp = ExpressionEvaluator.hasPendingOperation(amountExpression)
    val canSave = amountCents > 0 && hasSelectedCategory && !hasPendingOp
    val canCalculate = hasPendingOp && amountExpression.isNotEmpty()
            && amountExpression.last() !in setOf('+', '-', '.')

    val actionButtonText = when {
        canSave -> "保存"
        canCalculate -> "="
        else -> "完成"
    }

    val actionButtonEnabled = canSave || canCalculate

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 第一行: 7 8 9 +
        KeyRow {
            NumKey("7") { onDigitClick("7") }
            NumKey("8") { onDigitClick("8") }
            NumKey("9") { onDigitClick("9") }
            OpKey("+") { onOperatorClick("+") }
        }

        // 第二行: 4 5 6 -
        KeyRow {
            NumKey("4") { onDigitClick("4") }
            NumKey("5") { onDigitClick("5") }
            NumKey("6") { onDigitClick("6") }
            OpKey("-") { onOperatorClick("-") }
        }

        // 第三行 + 第四行合并右侧保存按钮（weight=2f 使内部两行与上方各行等高）
        Row(
            modifier = Modifier.fillMaxWidth().weight(2f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 左侧 3×2 数字区
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NumKey("1") { onDigitClick("1") }
                    NumKey("2") { onDigitClick("2") }
                    NumKey("3") { onDigitClick("3") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NumKey(".") { onDigitClick(".") }
                    NumKey("0") { onDigitClick("0") }
                    BackspaceKey(onBackspace)
                }
            }

            // 右侧合并按钮（占两行高度）
            ActionKey(
                text = actionButtonText,
                enabled = actionButtonEnabled,
                onClick = {
                    when {
                        canSave -> onSave()
                        canCalculate -> onCalculate()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        content()
    }
}

@Composable
private fun RowScope.NumKey(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RowScope.OpKey(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RowScope.BackspaceKey(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "⌫",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionKey(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceAlpha = if (enabled) 1f else 0.6f

    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .fillMaxHeight()
            .alpha(surfaceAlpha),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = if (enabled) 4.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                color = contentColor
            )
        }
    }
}
