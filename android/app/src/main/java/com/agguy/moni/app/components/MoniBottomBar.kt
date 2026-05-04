@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons

/**
 * 底部导航栏。
 *
 * 升级到 Material 3 Expressive：移除手写 containerColor，让 NavigationBar 自动应用
 * 默认 surfaceContainer 配色；slide 进出动画接入 motionScheme.defaultEffectsSpec()。
 */
@Composable
fun MoniBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<IntOffset>()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = effectsSpec, initialOffsetY = { it }),
        exit = slideOutVertically(animationSpec = effectsSpec, targetOffsetY = { it })
    ) {
        NavigationBar(modifier = modifier) {
            NavigationBarItem(
                icon = {
                    MoniIcon(
                        icon = if (activeTab == "records") MoniIcons.ReceiptFilled else MoniIcons.Receipt,
                        contentDescription = "账单"
                    )
                },
                label = { Text("账单") },
                selected = activeTab == "records",
                onClick = { onTabSelected("records") }
            )
            NavigationBarItem(
                icon = {
                    MoniIcon(
                        icon = if (activeTab == "stats") MoniIcons.BarChartFilled else MoniIcons.BarChart,
                        contentDescription = "统计"
                    )
                },
                label = { Text("统计") },
                selected = activeTab == "stats",
                onClick = { onTabSelected("stats") }
            )
            NavigationBarItem(
                icon = {
                    MoniIcon(
                        icon = if (activeTab == "settings") MoniIcons.SettingsFilled else MoniIcons.Settings,
                        contentDescription = "设置"
                    )
                },
                label = { Text("设置") },
                selected = activeTab == "settings",
                onClick = { onTabSelected("settings") }
            )
        }
    }
}
