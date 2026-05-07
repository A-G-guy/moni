@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.theme.expenseRed

/**
 * 预算进度条，Material 3 官方组件 + 动画。
 *
 * - 安全（< 80%）：主色调
 * - 临界（80% - 100%）：橙色
 * - 超支（> 100%）：红色
 */
@Composable
fun BudgetProgressBar(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0.0, 1.0)
    val color = when {
        percentage > 1.0 -> MaterialTheme.colorScheme.expenseRed
        percentage >= 0.8 -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    val animatedProgress by animateFloatAsState(
        targetValue = clamped.toFloat(),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "budget_progress"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp
    )
}

/**
 * 预算状态小圆点。
 */
@Composable
fun BudgetStatusDot(
    status: String,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        "overrun" -> MaterialTheme.colorScheme.expenseRed
        "critical" -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * 预算状态标签文本。
 */
@Composable
fun BudgetStatusLabel(
    status: String,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        "overrun" -> "超支" to MaterialTheme.colorScheme.expenseRed
        "critical" -> "接近上限" to Color(0xFFFFA726)
        else -> "安全" to MaterialTheme.colorScheme.primary
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

/**
 * 软冲突警告文本。
 */
@Composable
fun BudgetSoftConflictWarning(
    childAmount: Long,
    parentAmount: Long,
    modifier: Modifier = Modifier
) {
    if (childAmount > parentAmount) {
        Text(
            text = "子分类预算已超过父级预算，实际消费时会受父级限制",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFA726),
            modifier = modifier.padding(top = 4.dp)
        )
    }
}
