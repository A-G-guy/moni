package com.agguy.moni.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.agguy.moni.R
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.agguy.moni.app.i18n.AppLocaleManager
import com.agguy.moni.app.i18n.ErrorMessageResolver
import com.agguy.moni.app.navigation.Screen
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import com.agguy.moni.app.ui.record.editor.ExpressionEvaluator
import com.agguy.moni.core.CoreEffectRunner
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RustCoreController
import com.agguy.moni.core.platform.AppRestarter
import com.agguy.moni.core.platform.AutoBackupScheduler
import com.agguy.moni.core.platform.DataStoreHelper
import com.agguy.moni.core.platform.LogCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 主题设置状态。
 */
data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val presetColorScheme: PresetColorScheme = PresetColorScheme.DEFAULT
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

    private val _recordItemDisplaySettings = MutableStateFlow(RecordItemDisplaySettings())
    val recordItemDisplaySettings: StateFlow<RecordItemDisplaySettings> = _recordItemDisplaySettings.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow("")
    val selectedYearMonth: StateFlow<String> = _selectedYearMonth.asStateFlow()

    private val _language = MutableStateFlow(AppLocaleManager.AppLanguage.SYSTEM)
    val language: StateFlow<AppLocaleManager.AppLanguage> = _language.asStateFlow()

    private val _searchParams = MutableStateFlow(SearchParams())

    private var _navController: NavHostController? = null
    private var budgetCheckJob: kotlinx.coroutines.Job? = null
    private var searchDebounceJob: kotlinx.coroutines.Job? = null

    /** 供 UI 层绑定导航控制器。 */
    fun bindNavController(navController: NavHostController) {
        _navController = navController
    }

    /** 供 UI 层绑定 Snackbar 回调。 */
    fun bindSnackbarCallback(callback: (String) -> Unit) {
        effectRunner.onShowSnackbar = callback
    }

    init {
        // 初始化表达式计算器（委托给 Rust 内核）
        ExpressionEvaluator.initialize(rustCore)

        effectRunner.onPersistSetting = { key, value ->
            viewModelScope.launch {
                when (key) {
                    "currency_symbol" -> DataStoreHelper.saveCurrencySymbol(getApplication(), value)
                    else -> LogCollector.w("AppViewModel", "未知的持久化设置 key: $key")
                }
            }
        }

        effectRunner.onNavigate = { screen ->
            LogCollector.d("MoniNavigate", "Navigate to: $screen")
            _navController?.let { nav ->
                when (screen) {
                    "record_list" -> nav.navigate(Screen.RecordList) { popUpTo(Screen.RecordList) { inclusive = false } }
                    "stats" -> nav.navigate(Screen.Stats) { popUpTo(Screen.RecordList) { inclusive = false } }
                    "settings" -> nav.navigate(Screen.Settings) { popUpTo(Screen.RecordList) { inclusive = false } }
                    "budget_list" -> nav.navigate(Screen.BudgetList)
                    "category_list" -> nav.navigate(Screen.CategoryList)
                    "developer_options" -> nav.navigate(Screen.DeveloperOptions)
                    "archived_categories" -> nav.navigate(Screen.ArchivedCategories)
                    "data_management" -> nav.navigate(Screen.DataManagement)
                    "theme_settings" -> nav.navigate(Screen.ThemeSettings)
                    "dev_log" -> nav.navigate(Screen.DevLog)
                    else -> LogCollector.w("MoniNavigate", "未知的导航目标: $screen")
                }
            }
        }

        viewModelScope.launch {
            val currentYearMonth = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"))
            _selectedYearMonth.value = currentYearMonth

            try {
                LogCollector.i("AppViewModel", "开始初始化数据库")
                val dbPath = application.filesDir.absolutePath + "/moni.db"
                val mutation = rustCore.initializeWithDb(dbPath)
                applyMutation(mutation)
                LogCollector.i("AppViewModel", "数据库初始化完成: $dbPath")

                // 同步顺序调用，避免 monthly_summaries 与 RefreshMonthData 竞态
                applyMutation(rustCore.dispatch(CoreIntent.CategoryList))
                applyMutation(rustCore.dispatch(CoreIntent.StatsMonthlySummary(months = 36)))
                applyMutation(rustCore.dispatch(CoreIntent.RefreshMonthData(yearMonth = currentYearMonth)))
                syncCurrencySymbolFromDataStore()
                syncThemeSettingsFromDataStore()
                syncRecordItemDisplaySettingsFromDataStore()
                syncLanguageFromDataStore()
            } catch (e: Exception) {
                val app = getApplication<Application>()
                val errDetail = "${e.javaClass.simpleName}: ${e.message?.take(100)}"
                val err = app.getString(R.string.error_db_init_failed, errDetail)
                LogCollector.e("AppViewModel", err, e)
                try {
                    val mutation = rustCore.initialize()
                    applyMutation(mutation)
                    // 内存模式下也要加载基础数据，确保页面正常显示
                    dispatch(CoreIntent.CategoryList)
                    dispatch(CoreIntent.RecordListByMonth(yearMonth = currentYearMonth))
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "$err (${app.getString(R.string.error_fallback_memory_mode)})"
                    )
                } catch (inner: Exception) {
                    val innerErr = app.getString(
                        R.string.error_memory_init_failed,
                        inner.message?.take(100) ?: ""
                    )
                    LogCollector.e("AppViewModel", innerErr, inner)
                    _uiState.value = _uiState.value.copy(errorMessage = "$err; $innerErr")
                }
            }

            // 自动备份配置同步独立保护，避免 WorkManager 异常影响正常数据加载
            try {
                syncAutoBackupSettings()
            } catch (e: Exception) {
                LogCollector.e("AppViewModel", "自动备份配置同步失败", e)
            }
        }
    }

    fun selectYearMonth(yearMonth: String) {
        if (_selectedYearMonth.value == yearMonth) return
        _selectedYearMonth.value = yearMonth
        dispatch(CoreIntent.RefreshMonthData(yearMonth = yearMonth))
    }

    // region 搜索功能

    fun enterSearchMode() {
        _uiState.value = _uiState.value.copy(isSearchMode = true)
    }

    fun exitSearchMode() {
        searchDebounceJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isSearchMode = false,
            searchKeyword = "",
            searchResultCount = 0
        )
        _searchParams.value = SearchParams()
        val yearMonth = _selectedYearMonth.value
        if (yearMonth.isNotEmpty()) {
            dispatch(CoreIntent.RefreshMonthData(yearMonth = yearMonth))
        }
    }

    fun updateSearchKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(searchKeyword = keyword)
        if (_uiState.value.isSearchMode) {
            scheduleSearch()
        }
    }

    fun updateSearchParams(params: SearchParams) {
        _searchParams.value = params
        if (_uiState.value.isSearchMode) {
            scheduleSearch()
        }
    }

    fun resetSearchParams() {
        _searchParams.value = SearchParams()
        if (_uiState.value.isSearchMode) {
            scheduleSearch()
        }
    }

    private fun scheduleSearch() {
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (_uiState.value.isSearchMode) {
                performSearch()
            }
        }
    }

    private fun performSearch() {
        val keyword = _uiState.value.searchKeyword
        val params = _searchParams.value
        dispatch(
            CoreIntent.RecordSearch(
                keyword = keyword.takeIf { it.isNotBlank() },
                recordType = params.recordType,
                categoryIds = params.categoryIds,
                amountMin = params.amountMin,
                amountMax = params.amountMax,
                dateStart = params.dateStart,
                dateEnd = params.dateEnd,
                sortBy = params.sortBy,
                sortOrder = params.sortOrder
            )
        )
    }

    // endregion

    fun dispatch(intent: CoreIntent) {
        viewModelScope.launch {
            LogCollector.i("AppViewModel", "Dispatch intent: ${intent::class.simpleName}")
            try {
                val mutation = rustCore.dispatch(intent)
                applyMutation(mutation)

                // 记录变更成功后自动刷新当前月份数据及月度汇总。
                if ((intent is CoreIntent.RecordCreate ||
                        intent is CoreIntent.RecordDelete ||
                        intent is CoreIntent.RecordUpdate) &&
                    !hasMutationError(mutation)
                ) {
                    refreshAfterRecordMutation()
                }
            } catch (e: Exception) {
                LogCollector.e("AppViewModel", "Dispatch 失败: ${intent::class.simpleName}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = getApplication<Application>().getString(
                        R.string.error_operation_failed,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    suspend fun createRecordFromAi(intent: CoreIntent.RecordCreate): Boolean {
        LogCollector.i("AppViewModel", "AI 记账创建记录")
        return try {
            val mutation = rustCore.dispatch(intent)
            applyMutation(mutation)
            if (hasMutationError(mutation)) {
                false
            } else {
                refreshAfterRecordMutation()
                true
            }
        } catch (e: Exception) {
            LogCollector.e("AppViewModel", "AI 记账创建记录失败", e)
            _uiState.value = _uiState.value.copy(
                errorMessage = getApplication<Application>().getString(
                    R.string.error_operation_failed,
                    e.message ?: ""
                )
            )
            false
        }
    }

    private suspend fun refreshAfterRecordMutation() {
        if (_uiState.value.isSearchMode) {
            // 搜索模式下刷新搜索结果。
            runCatching {
                rustCore.dispatch(CoreIntent.StatsMonthlySummary(months = 36))
            }.onSuccess(::applyMutation).onFailure { e ->
                LogCollector.e("AppViewModel", "刷新月度统计失败", e)
            }
            runCatching {
                val keyword = _uiState.value.searchKeyword
                val params = _searchParams.value
                rustCore.dispatch(
                    CoreIntent.RecordSearch(
                        keyword = keyword.takeIf { it.isNotBlank() },
                        recordType = params.recordType,
                        categoryIds = params.categoryIds,
                        amountMin = params.amountMin,
                        amountMax = params.amountMax,
                        dateStart = params.dateStart,
                        dateEnd = params.dateEnd,
                        sortBy = params.sortBy,
                        sortOrder = params.sortOrder
                    )
                )
            }.onSuccess(::applyMutation).onFailure { e ->
                LogCollector.e("AppViewModel", "刷新搜索结果失败", e)
            }
            return
        }

        val yearMonth = _selectedYearMonth.value
        if (yearMonth.isEmpty()) return

        // 先刷新月度汇总，确保 RefreshMonthData 中的 StatsOverviewMetrics
        // 在 Rust 端计算时使用的是最新的 monthly_summaries。
        runCatching {
            rustCore.dispatch(CoreIntent.StatsMonthlySummary(months = 36))
        }.onSuccess(::applyMutation).onFailure { e ->
            LogCollector.e("AppViewModel", "刷新月度统计失败", e)
        }
        runCatching {
            rustCore.dispatch(CoreIntent.RefreshMonthData(yearMonth = yearMonth))
        }.onSuccess(::applyMutation).onFailure { e ->
            LogCollector.e("AppViewModel", "刷新月份数据失败", e)
        }
    }

    private fun hasMutationError(mutation: com.agguy.moni.core.CoreMutation): Boolean =
        (mutation.state.ui.errorMessage != null || mutation.state.ui.errorKey != null) && mutation.effects.isEmpty()

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
        val presetColorScheme = DataStoreHelper.presetColorSchemeFlow(getApplication()).first()
        _themeSettings.value = ThemeSettings(themeMode, presetColorScheme)
    }

    private suspend fun syncAutoBackupSettings() {
        val app = getApplication<Application>()
        val enabled = DataStoreHelper.autoBackupEnabledFlow(app).first()
        val frequency = DataStoreHelper.autoBackupFrequencyFlow(app).first()

        // 注册/更新 WorkManager 周期任务
        AutoBackupScheduler.schedule(app, enabled, frequency)

        // "每次启动"频率：直接触发一次备份
        if (enabled && frequency == "every_launch") {
            AutoBackupScheduler.triggerOnce(app)
        }
    }

    private suspend fun syncLanguageFromDataStore() {
        val code = DataStoreHelper.languageFlow(getApplication()).first()
        _language.value = AppLocaleManager.AppLanguage.fromCode(code)
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            DataStoreHelper.saveThemeMode(getApplication(), mode)
            _themeSettings.value = _themeSettings.value.copy(themeMode = mode)
        }
    }

    private suspend fun syncRecordItemDisplaySettingsFromDataStore() {
        val showIcon = DataStoreHelper.recordShowIconFlow(getApplication()).first()
        val showFullCategory = DataStoreHelper.recordShowFullCategoryFlow(getApplication()).first()
        val notePriority = DataStoreHelper.recordNotePriorityFlow(getApplication()).first()
        _recordItemDisplaySettings.value = RecordItemDisplaySettings(showIcon, showFullCategory, notePriority)
    }

    fun updateRecordShowIcon(show: Boolean) {
        viewModelScope.launch {
            DataStoreHelper.saveRecordShowIcon(getApplication(), show)
            _recordItemDisplaySettings.value = _recordItemDisplaySettings.value.copy(showIcon = show)
        }
    }

    fun updateRecordShowFullCategory(show: Boolean) {
        viewModelScope.launch {
            DataStoreHelper.saveRecordShowFullCategory(getApplication(), show)
            _recordItemDisplaySettings.value = _recordItemDisplaySettings.value.copy(showFullCategory = show)
        }
    }

    fun updateRecordNotePriority(priority: Boolean) {
        viewModelScope.launch {
            DataStoreHelper.saveRecordNotePriority(getApplication(), priority)
            _recordItemDisplaySettings.value = _recordItemDisplaySettings.value.copy(notePriority = priority)
        }
    }

    fun updatePresetColorScheme(scheme: PresetColorScheme) {
        viewModelScope.launch {
            DataStoreHelper.savePresetColorScheme(getApplication(), scheme)
            _themeSettings.value = _themeSettings.value.copy(presetColorScheme = scheme)
        }
    }

    /**
     * 切换应用语言。
     *
     * 保存语言设置到 DataStore，联动更新货币符号（仅当用户未手动修改过时），
     * 然后重启应用使新语言生效。
     */
    fun updateLanguage(language: AppLocaleManager.AppLanguage) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val currentSymbol = DataStoreHelper.currencySymbolFlow(app).first()

            DataStoreHelper.saveLanguage(app, language.code)
            _language.value = language

            // 货币联动：仅当当前货币是另一语言的默认值时才自动切换
            if (language != AppLocaleManager.AppLanguage.SYSTEM) {
                val shouldSwitch = when {
                    language == AppLocaleManager.AppLanguage.ENGLISH && currentSymbol == "¥" -> true
                    language == AppLocaleManager.AppLanguage.CHINESE && currentSymbol == "$" -> true
                    else -> false
                }
                if (shouldSwitch) {
                    dispatch(CoreIntent.SettingsUpdateCurrency(symbol = language.defaultCurrency))
                }
            }

            LogCollector.i("AppViewModel", "语言切换为 ${language.code}，即将重启应用")
            AppRestarter.restartApp(app)
        }
    }

    fun navigateToRecordDetail(recordId: Long? = null) {
        _navController?.navigate(Screen.RecordDetail(recordId))
    }

    fun navigateToCategoryList() {
        _navController?.navigate(Screen.CategoryList)
    }

    fun navigateToBudgetList() {
        _navController?.navigate(Screen.BudgetList)
    }

    fun navigateToDevLog() {
        _navController?.navigate(Screen.DevLog)
    }

    /**
     * 触发预算检查（含去抖动）。
     * 首次延迟 50ms，后续延迟 150ms，避免快速输入时过度触发 FFI。
     */
    fun checkBudget(categoryId: Long, amountCents: Long) {
        budgetCheckJob?.cancel()
        budgetCheckJob = viewModelScope.launch {
            val delayMs = if (_uiState.value.budgetCheckResult == null) 50L else 150L
            kotlinx.coroutines.delay(delayMs)
            val yearMonth = _selectedYearMonth.value
            if (yearMonth.isNotEmpty() && amountCents > 0) {
                dispatch(
                    CoreIntent.BudgetCheck(
                        categoryId = categoryId,
                        yearMonth = yearMonth,
                        amountCents = amountCents
                    )
                )
            }
        }
    }

    /** 取消正在进行的预算检查并清空结果。 */
    fun clearBudgetCheck() {
        budgetCheckJob?.cancel()
        _uiState.value = _uiState.value.copy(budgetCheckResult = null)
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

    fun navigateToAiSettings() {
        _navController?.navigate(Screen.AiSettings)
    }

    fun navigateToAiBookkeeping() {
        _navController?.navigate(Screen.AiBookkeeping)
    }

    fun navigateBack() {
        _navController?.popBackStack()
    }

    private fun applyMutation(mutation: com.agguy.moni.core.CoreMutation) {
        val current = _uiState.value
        val appState = mutation.state.toAppState()
        // 如果 Rust 返回了 error_key，使用本地化错误消息
        val localizedError = appState.errorKey?.let { key ->
            ErrorMessageResolver.resolve(getApplication(), key, appState.errorArgs)
        } ?: appState.errorMessage
        // 保留 Kotlin 侧纯 UI 状态（搜索模式、关键词等），其余使用 Rust 返回的新状态
        _uiState.value = appState.copy(
            errorMessage = localizedError,
            isSearchMode = current.isSearchMode,
            searchKeyword = current.searchKeyword
        )
        mutation.effects.forEach { effectRunner.runEffect(it) }
    }
}
