package com.agguy.moni.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.agguy.moni.app.components.MoniBottomBar
import com.agguy.moni.app.navigation.MoniNavHost
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.MoniTheme

@Composable
fun MoniApp() {
    MoniTheme {
        val viewModel: AppViewModel = viewModel()
        val appState by viewModel.uiState.collectAsState()
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarScope = rememberCoroutineScope()

        // 将 NavController 绑定到 ViewModel
        LaunchedEffect(navController) {
            viewModel.navController = navController
        }

        // 将 SnackbarHostState 绑定到 EffectRunner
        LaunchedEffect(snackbarHostState) {
            viewModel.effectRunner.onShowSnackbar = { message ->
                snackbarScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }

        Scaffold(
            bottomBar = {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                val showBottomBar = currentRoute?.contains("RecordList") == true
                        || currentRoute?.contains("Stats") == true
                        || currentRoute?.contains("Settings") == true

                if (showBottomBar) {
                    val activeTab = when {
                        currentRoute.contains("Stats") -> "stats"
                        currentRoute.contains("Settings") -> "settings"
                        else -> "records"
                    }
                    MoniBottomBar(
                        activeTab = activeTab,
                        onTabSelected = { tab ->
                            val destination = when (tab) {
                                "stats" -> Screen.Stats
                                "settings" -> Screen.Settings
                                else -> Screen.RecordList
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.RecordList) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            MoniNavHost(
                navController = navController,
                appState = appState,
                onDispatch = viewModel::dispatch,
                onNavigateToRecordDetail = viewModel::navigateToRecordDetail,
                onNavigateToCategoryList = viewModel::navigateToCategoryList,
                onNavigateBack = viewModel::navigateBack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
