package com.agguy.moni.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.agguy.moni.app.components.MoniBottomBar
import com.agguy.moni.app.navigation.MoniNavHost
import com.agguy.moni.app.theme.MoniTheme

@Composable
fun MoniApp() {
    MoniTheme {
        val viewModel: AppViewModel = viewModel()
        val appState by viewModel.uiState.collectAsState()
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            bottomBar = {
                MoniBottomBar(
                    activeTab = "records",
                    onTabSelected = { /* TODO: 迭代二实现导航切换 */ }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            MoniNavHost(
                navController = navController,
                appState = appState,
                onDispatch = viewModel::dispatch,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
