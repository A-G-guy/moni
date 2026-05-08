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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.agguy.moni.R
import com.agguy.moni.app.icons.SymbolIcon

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
    modifier: Modifier = Modifier,
    visible: Boolean = true
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
                    SymbolIcon(
                        name = "receipt",
                        filled = activeTab == "records",
                        contentDescription = stringResource(R.string.nav_records)
                    )
                },
                label = { Text(stringResource(R.string.nav_records)) },
                selected = activeTab == "records",
                onClick = { onTabSelected("records") }
            )
            NavigationBarItem(
                icon = {
                    SymbolIcon(
                        name = "bar_chart",
                        filled = activeTab == "stats",
                        contentDescription = stringResource(R.string.nav_stats)
                    )
                },
                label = { Text(stringResource(R.string.nav_stats)) },
                selected = activeTab == "stats",
                onClick = { onTabSelected("stats") }
            )
            NavigationBarItem(
                icon = {
                    SymbolIcon(
                        name = "settings",
                        filled = activeTab == "settings",
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                },
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = activeTab == "settings",
                onClick = { onTabSelected("settings") }
            )
        }
    }
}
