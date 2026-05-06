package com.agguy.moni.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController
import com.agguy.moni.core.platform.AppRestarter
import com.agguy.moni.core.platform.DataStoreHelper
import com.agguy.moni.core.platform.LogCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 主题设置状态。
 */
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val presetColorScheme: PresetColorScheme = PresetColorScheme.AIRY_SAKURA
)

class AppViewModel(
    application: Application,
    private val rustCore: RustCoreController,
    private val effectRunner: CoreEffectRunner,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow("")
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private var _navController: NavHostController? = null

    /** 供 UI 层绑定导航控制器。 */
    fun bindNavController(navController: NavHostController) {
        _navController = navController
    }

    /** 供 UI 层绑定 Snackbar 回调。 */
    fun bindSnackbarCallback(callback: (String) -> Unit) {
        effectRunner.onShowSnackbar = callback
    }

    init {
        effectRunner.onNavigate = { screen ->
            LogCollector.d("MoniNavigate", "Navigate to: $screen")
        }

        viewModelScope.launch {
            try {
                LogCollector.i("AppViewModel", "开始初始化数据库")
                val dbPath = application.filesDir.absolutePath + "/moni.db"
                val mutation = rustCore.initializeWithDb(dbPath)
                applyMutation(mutation)
                LogCollector.i("AppViewModel", "数据库初始化完成: $dbPath")

                val currentYearMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"))
                _selectedYearMonth.value = currentYearMonth

                dispatch(CoreIntent.CategoryList)
                dispatch(CoreIntent.StatsMonthlySummary(months = 36))
                dispatch(CoreIntent.RecordListByMonth(yearMonth = currentYearMonth))
                dispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = currentYearMonth))
                syncCurrencySymbolFromDataStore()
                syncThemeSettingsFromDataStore()
            } catch (e: Exception) {
                LogCollector.e("AppViewModel", "数据库初始化失败，回退到内存模式", e)
                val mutation = rustCore.initialize()
                applyMutation(mutation)
            }
        }
    }

    fun selectYearMonth(yearMonth: String) {
        if (_selectedYearMonth.value == yearMonth) return
        _selectedYearMonth.value = yearMonth
        dispatch(CoreIntent.RecordListByMonth(yearMonth = yearMonth))
        dispatch(CoreIntent.StatsCategoryBreakdown(yearMonth = yearMonth))
    }

    fun dispatch(intent: CoreIntent) {
        viewModelScope.launch {
            LogCollector.i("AppViewModel", "Dispatch intent: ${intent::class.simpleName}")
            if (intent is CoreIntent.SettingsUpdateCurrency) {
                DataStoreHelper.saveCurrencySymbol(
                    getApplication(),
                    intent.symbol
                )
            }
            val mutation = rustCore.dispatch(intent)
            applyMutation(mutation)

            // 记录变更后自动刷新当前月份数据及月度汇总
            if (intent is CoreIntent.RecordCreate ||
                intent is CoreIntent.RecordDelete ||
                intent is CoreIntent.RecordUpdate
            ) {
                val yearMonth = _selectedYearMonth.value
                if (yearMonth.isNotEmpty()) {
                    rustCore.dispatch(CoreIntent.RecordListByMonth(yearMonth = yearMonth))
                        .let(::applyMutation)
                    rustCore.dispatch(CoreIntent.StatsMonthlySummary(months = 36))
                        .let(::applyMutation)
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            LogCollector.w("AppViewModel", "执行清空所有数据")
            try {
                val mutation = rustCore.dispatch(CoreIntent.DevClearAllData)
                applyMutation(mutation)
                DataStoreHelper.clearAll(getApplication())
                LogCollector.i("AppViewModel", "数据已清空，即将重启应用")
                AppRestarter.restartApp(getApplication())
            } catch (e: Exception) {
                LogCollector.e("AppViewModel", "清空数据失败", e)
            }
        }
    }

    fun navigateToDeveloperOptions() {
        _navController?.navigate(Screen.DeveloperOptions)
    }

    private suspend fun syncCurrencySymbolFromDataStore() {
        val savedSymbol = DataStoreHelper
            .currencySymbolFlow(getApplication())
            .first()
        if (savedSymbol != _uiState.value.currencySymbol) {
            dispatch(CoreIntent.SettingsUpdateCurrency(symbol = savedSymbol))
        }
    }

    private suspend fun syncThemeSettingsFromDataStore() {
        val themeMode = DataStoreHelper.themeModeFlow(getApplication()).first()
        val dynamicColor = DataStoreHelper.dynamicColorFlow(getApplication()).first()
        val presetColorScheme = DataStoreHelper.presetColorSchemeFlow(getApplication()).first()
        _themeSettings.value = ThemeSettings(themeMode, dynamicColor, presetColorScheme)
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            DataStoreHelper.saveThemeMode(getApplication(), mode)
            _themeSettings.value = _themeSettings.value.copy(themeMode = mode)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            DataStoreHelper.saveDynamicColor(getApplication(), enabled)
            _themeSettings.value = _themeSettings.value.copy(dynamicColor = enabled)
        }
    }

    fun updatePresetColorScheme(scheme: PresetColorScheme) {
        viewModelScope.launch {
            DataStoreHelper.savePresetColorScheme(getApplication(), scheme)
            _themeSettings.value = _themeSettings.value.copy(presetColorScheme = scheme)
        }
    }

    fun navigateToRecordDetail(recordId: Long? = null) {
        _navController?.navigate(Screen.RecordDetail(recordId))
    }

    fun navigateToCategoryList() {
        _navController?.navigate(Screen.CategoryList)
    }

    fun navigateToDevLog() {
        _navController?.navigate(Screen.DevLog)
    }

    fun navigateToArchivedCategories() {
        _navController?.navigate(Screen.ArchivedCategories)
    }

    fun navigateToDataManagement() {
        _navController?.navigate(Screen.DataManagement)
    }

    fun navigateToThemeSettings() {
        _navController?.navigate(Screen.ThemeSettings)
    }

    fun navigateBack() {
        _navController?.popBackStack()
    }

    private fun applyMutation(mutation: com.agguy.moni.core.CoreMutation) {
        _uiState.value = mutation.state.toAppState()
        mutation.effects.forEach { effectRunner.runEffect(it) }
    }
}
