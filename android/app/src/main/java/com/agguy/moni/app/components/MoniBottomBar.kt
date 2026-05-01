package com.agguy.moni.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MoniBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "账单") },
            label = { Text("账单") },
            selected = activeTab == "records",
            onClick = { onTabSelected("records") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.BarChart, contentDescription = "统计") },
            label = { Text("统计") },
            selected = activeTab == "stats",
            onClick = { onTabSelected("stats") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") },
            label = { Text("设置") },
            selected = activeTab == "settings",
            onClick = { onTabSelected("settings") }
        )
    }
}
