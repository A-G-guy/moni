package com.agguy.moni.app

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agguy.moni.app.components.MoniBottomBar
import com.agguy.moni.app.navigation.MoniNavHost
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.MoniTheme
import com.agguy.moni.app.ui.backup.BackupViewModel
import com.agguy.moni.app.ui.settings.ExportDataDialog
import com.agguy.moni.app.ui.settings.ImportConfirmDialog
import com.agguy.moni.di.AppModule
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoniApp() {
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
    val themeSettings by viewModel.themeSettings.collectAsState()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val dbPath = remember { application.filesDir.absolutePath + "/moni.db" }

    val backupViewModel: BackupViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BackupViewModel(
                    application,
                    AppModule.provideRustCoreController()
                ) as T
            }
        }
    )

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val backupOperationState by backupViewModel.operationState.collectAsState()
    val inspectResult by backupViewModel.inspectResult.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { backupViewModel.exportToSaf(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importUri = it
            showImportDialog = true
            backupViewModel.clearInspectResult()
            backupViewModel.inspectBackupFromUri(it)
        }
    }

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

    MoniTheme(
        themeMode = themeSettings.themeMode,
        seedColor = Color(themeSettings.seedColor.toULong()),
        dynamicColor = themeSettings.dynamicColor
    ) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute?.contains("RecordList") == true ||
                    currentRoute?.contains("Stats") == true ||
                    currentRoute?.contains("Settings") == true

                if (showBottomBar) {
                    val activeTab = when {
                        currentRoute.contains("Stats") -> "stats"
                        currentRoute.contains("Settings") -> "settings"
                        else -> "records"
                    }
                    MoniBottomBar(
                        activeTab = activeTab,
                        onTabSelected = { tab ->
                            // 当前已在该 tab 时不触发空动画
                            if (tab == activeTab) return@MoniBottomBar
                            val destination = when (tab) {
                                "stats" -> Screen.Stats
                                "settings" -> Screen.Settings
                                else -> Screen.RecordList
                            }
                            navController.navigate(destination) {
                                popUpTo(Screen.RecordList) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        visible = showBottomBar
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            MoniNavHost(
                navController = navController,
                appState = appState,
                themeSettings = themeSettings,
                selectedYearMonth = selectedYearMonth,
                onDispatch = viewModel::dispatch,
                onSelectYearMonth = viewModel::selectYearMonth,
                onNavigateToRecordDetail = viewModel::navigateToRecordDetail,
                onNavigateToCategoryList = viewModel::navigateToCategoryList,
                onNavigateToArchivedCategories = viewModel::navigateToArchivedCategories,
                onNavigateBack = viewModel::navigateBack,
                onUpdateThemeMode = viewModel::updateThemeMode,
                onUpdateDynamicColor = viewModel::updateDynamicColor,
                onUpdateSeedColor = viewModel::updateSeedColor,
                onNavigateToDeveloperOptions = viewModel::navigateToDeveloperOptions,
                onNavigateToDevLog = viewModel::navigateToDevLog,
                onClearAllData = viewModel::clearAllData,
                onNavigateToBackupManager = viewModel::navigateToBackupManager,
                onShowExportDialog = { showExportDialog = true },
                onShowImportDialog = { importLauncher.launch(arrayOf("application/zip")) },
                backupViewModel = backupViewModel,
                dbPath = dbPath,
                modifier = Modifier.padding(innerPadding)
            )
        }

        // 导出数据对话框
        if (showExportDialog) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            ExportDataDialog(
                operationState = backupOperationState,
                onExportToInternal = {
                    backupViewModel.exportToInternal()
                },
                onExportToSaf = {
                    exportLauncher.launch("Moni_Backup_$timestamp.zip")
                },
                onDismiss = {
                    showExportDialog = false
                    backupViewModel.resetState()
                }
            )
        }

        // 导入确认对话框
        if (showImportDialog) {
            ImportConfirmDialog(
                inspectResult = inspectResult,
                operationState = backupOperationState,
                onConfirm = {
                    importUri?.let { uri ->
                        backupViewModel.importFromSaf(uri, dbPath)
                    }
                },
                onDismiss = {
                    showImportDialog = false
                    importUri = null
                    backupViewModel.clearInspectResult()
                    backupViewModel.resetState()
                }
            )
        }
    }
}
