package com.agguy.moni.app

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agguy.moni.app.components.MoniBottomBar
import com.agguy.moni.app.navigation.MoniNavHost
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.MoniTheme
import com.agguy.moni.di.AppModule
import kotlinx.coroutines.launch

@Composable
fun MoniApp() {
    MoniTheme {
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val viewModel: AppViewModel = viewModel(
            factory = AppViewModelFactory(
                application = application,
                rustCore = AppModule.provideRustCoreController(),
                effectRunner = AppModule.provideCoreEffectRunner(context),
            )
        )
        val appState by viewModel.uiState.collectAsState()
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val snackbarScope = rememberCoroutineScope()

        // 将 NavController 绑定到 ViewModel
        LaunchedEffect(navController) {
            viewModel.bindNavController(navController)
        }

        // 将 SnackbarHostState 绑定到 EffectRunner
        LaunchedEffect(snackbarHostState) {
            viewModel.bindSnackbarCallback { message ->
                snackbarScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
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
