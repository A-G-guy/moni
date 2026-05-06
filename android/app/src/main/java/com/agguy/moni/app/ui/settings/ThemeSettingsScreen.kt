@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.ThemeSettings
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.ThemeMode

/**
 * 外观设置二级页面。
 *
 * 整合主题模式、动态颜色两个设置项，页面内直接展示选择器，选择即时生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    themeSettings: ThemeSettings,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("外观") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = MoniIcons.ArrowBack),
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // === 主题模式 ===
            SectionTitle("主题模式")
            ThemeModeSelector(
                currentMode = themeSettings.themeMode,
                onModeSelected = onUpdateThemeMode
            )

            HorizontalDivider()

            // === 动态颜色 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "动态颜色",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "使用系统壁纸颜色（Android 12+）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = themeSettings.dynamicColor,
                    onCheckedChange = onUpdateDynamicColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemeMode.LIGHT to "浅色",
        ThemeMode.DARK to "深色",
        ThemeMode.SYSTEM to "跟随系统"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = currentMode == mode
            val containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val border = if (selected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onModeSelected(mode) },
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                contentColor = contentColor,
                border = border
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
