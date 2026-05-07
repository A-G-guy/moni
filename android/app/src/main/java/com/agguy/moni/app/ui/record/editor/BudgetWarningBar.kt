package com.agguy.moni.app.ui.record.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.CoreBudgetCheckResult
import com.agguy.moni.core.util.formatAmount

/**
 * 记账页预算预警轻提示条。
 *
 * 显示在分类网格和金额输入之间，根据预算检查结果动态展示。
 */
@Composable
fun BudgetWarningBar(
    checkResult: CoreBudgetCheckResult?,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (checkResult == null || checkResult.effectiveAvailable == null) return

    val effective = checkResult.effectiveAvailable
    val isOverrun = effective < 0
    val isCritical = !isOverrun && checkResult.postSaveStatus == "critical"

    val backgroundColor = when {
        isOverrun -> MaterialTheme.colorScheme.errorContainer
        isCritical -> Color(0xFFFFF3E0) // 浅橙色
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = when {
        isOverrun -> MaterialTheme.colorScheme.onErrorContainer
        isCritical -> Color(0xFFE65100) // 深橙色
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val message = when {
        isOverrun -> {
            val overrunAmount = formatAmount(-effective)
            val bottleneck = when (checkResult.bottleneckBudget) {
                "total" -> "总预算"
                "parent" -> "父级预算"
                "self" -> "该分类预算"
                else -> "预算"
            }
            "受${bottleneck}限制，本月已超支 ¥$overrunAmount"
        }
        isCritical -> {
            val remaining = formatAmount(effective)
            val bottleneck = when (checkResult.bottleneckBudget) {
                "total" -> "总预算"
                "parent" -> "父级预算"
                "self" -> "该分类预算"
                else -> "预算"
            }
            "受${bottleneck}限制，本月建议最多再花 ¥$remaining"
        }
        else -> {
            val remaining = formatAmount(effective)
            "本月建议最多再花 ¥$remaining"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = if (isOverrun || isCritical) FontWeight.Medium else FontWeight.Normal
        )
    }
}
