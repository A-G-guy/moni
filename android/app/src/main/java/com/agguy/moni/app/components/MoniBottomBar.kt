package com.agguy.moni.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons

@Composable
fun MoniBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
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
