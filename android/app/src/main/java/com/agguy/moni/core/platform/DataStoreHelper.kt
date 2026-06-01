package com.agguy.moni.core.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moni_settings")

/**
 * 设置数据存储帮助类。
 *
 * 封装 DataStore Preferences 读写操作，用于持久化应用设置。
 */
object DataStoreHelper {
    private val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val PRESET_COLOR_SCHEME_KEY = stringPreferencesKey("preset_color_scheme")

    // 自动备份配置键
    private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")
    private val AUTO_BACKUP_FREQUENCY_KEY = stringPreferencesKey("auto_backup_frequency")
    private val AUTO_BACKUP_MAX_COUNT_KEY = intPreferencesKey("auto_backup_max_count")
    private val AUTO_BACKUP_COPY_TO_EXTERNAL_KEY = booleanPreferencesKey("auto_backup_copy_to_external")
    private val AUTO_BACKUP_EXTERNAL_URI_KEY = stringPreferencesKey("auto_backup_external_uri")
    private val AUTO_BACKUP_LAST_TIME_KEY = stringPreferencesKey("auto_backup_last_time")

    // 账单条目内容显示设置键
    private val RECORD_SHOW_ICON_KEY = booleanPreferencesKey("record_show_icon")
    private val RECORD_SHOW_FULL_CATEGORY_KEY = booleanPreferencesKey("record_show_full_category")
    private val RECORD_NOTE_PRIORITY_KEY = booleanPreferencesKey("record_note_priority")

    // 语言设置键
    private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")

    // 小键盘数字键布局设置键
    private val NUMPAD_SWAP_TOP_BOTTOM_ROWS_KEY = booleanPreferencesKey("numpad_swap_top_bottom_rows")

    /**
     * 获取货币符号流。
     */
    fun currencySymbolFlow(context: Context): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY_SYMBOL_KEY] ?: "¥"
    }

    /**
     * 保存货币符号。
     */
    suspend fun saveCurrencySymbol(context: Context, symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY_SYMBOL_KEY] = symbol
        }
    }

    /**
     * 获取主题模式流。
     */
    fun themeModeFlow(context: Context): Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_MODE_KEY]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    /**
     * 保存主题模式。
     */
    suspend fun saveThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    /**
     * 获取预设配色方案流。
     */
    fun presetColorSchemeFlow(context: Context): Flow<PresetColorScheme> = context.dataStore.data.map { preferences ->
        when (preferences[PRESET_COLOR_SCHEME_KEY]) {
            "dynamic" -> PresetColorScheme.DYNAMIC
            "airy_sakura" -> PresetColorScheme.AIRY_SAKURA
            "anime_sky" -> PresetColorScheme.ANIME_SKY
            "crisp_mint" -> PresetColorScheme.CRISP_MINT
            "neon_lavender" -> PresetColorScheme.NEON_LAVENDER
            "oatmeal_gold" -> PresetColorScheme.OATMEAL_GOLD
            "sunset_coral" -> PresetColorScheme.SUNSET_CORAL
            else -> PresetColorScheme.DEFAULT
        }
    }

    /**
     * 保存预设配色方案。
     */
    suspend fun savePresetColorScheme(context: Context, scheme: PresetColorScheme) {
        context.dataStore.edit { preferences ->
            preferences[PRESET_COLOR_SCHEME_KEY] = when (scheme) {
                PresetColorScheme.DEFAULT -> "default"
                PresetColorScheme.DYNAMIC -> "dynamic"
                PresetColorScheme.AIRY_SAKURA -> "airy_sakura"
                PresetColorScheme.ANIME_SKY -> "anime_sky"
                PresetColorScheme.CRISP_MINT -> "crisp_mint"
                PresetColorScheme.NEON_LAVENDER -> "neon_lavender"
                PresetColorScheme.OATMEAL_GOLD -> "oatmeal_gold"
                PresetColorScheme.SUNSET_CORAL -> "sunset_coral"
            }
        }
    }

    // region 自动备份配置读写

    /**
     * 获取自动备份启用状态流。
     */
    fun autoBackupEnabledFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_ENABLED_KEY] ?: false
    }

    /**
     * 保存自动备份启用状态。
     */
    suspend fun saveAutoBackupEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED_KEY] = enabled
        }
    }

    /**
     * 获取自动备份频率流。
     * 返回值："every_launch" | "daily" | "weekly" | "monthly"
     */
    fun autoBackupFrequencyFlow(context: Context): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_FREQUENCY_KEY] ?: "daily"
    }

    /**
     * 保存自动备份频率。
     */
    suspend fun saveAutoBackupFrequency(context: Context, frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_FREQUENCY_KEY] = frequency
        }
    }

    /**
     * 获取自动备份最大保留数量流（默认 7，范围 3~30）。
     */
    fun autoBackupMaxCountFlow(context: Context): Flow<Int> = context.dataStore.data.map { preferences ->
        val value = preferences[AUTO_BACKUP_MAX_COUNT_KEY] ?: 7
        value.coerceIn(3, 30)
    }

    /**
     * 保存自动备份最大保留数量。
     */
    suspend fun saveAutoBackupMaxCount(context: Context, maxCount: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_MAX_COUNT_KEY] = maxCount.coerceIn(3, 30)
        }
    }

    /**
     * 获取是否自动复制到外部目录流。
     */
    fun autoBackupCopyToExternalFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_COPY_TO_EXTERNAL_KEY] ?: false
    }

    /**
     * 保存是否自动复制到外部目录。
     */
    suspend fun saveAutoBackupCopyToExternal(context: Context, copyToExternal: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_COPY_TO_EXTERNAL_KEY] = copyToExternal
        }
    }

    /**
     * 获取外部备份目录 SAF URI 流。
     */
    fun autoBackupExternalUriFlow(context: Context): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_EXTERNAL_URI_KEY]
    }

    /**
     * 保存外部备份目录 SAF URI。
     */
    suspend fun saveAutoBackupExternalUri(context: Context, uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[AUTO_BACKUP_EXTERNAL_URI_KEY] = uri
            } else {
                preferences.remove(AUTO_BACKUP_EXTERNAL_URI_KEY)
            }
        }
    }

    /**
     * 获取上次自动备份时间 ISO 8601 流。
     */
    fun autoBackupLastTimeFlow(context: Context): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTO_BACKUP_LAST_TIME_KEY]
    }

    /**
     * 保存上次自动备份时间。
     */
    suspend fun saveAutoBackupLastTime(context: Context, lastTime: String?) {
        context.dataStore.edit { preferences ->
            if (lastTime != null) {
                preferences[AUTO_BACKUP_LAST_TIME_KEY] = lastTime
            } else {
                preferences.remove(AUTO_BACKUP_LAST_TIME_KEY)
            }
        }
    }

    // endregion

    // region 账单条目内容显示设置

    /**
     * 获取账单条目显示图标流（默认 true）。
     */
    fun recordShowIconFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RECORD_SHOW_ICON_KEY] ?: true
    }

    /**
     * 保存账单条目显示图标设置。
     */
    suspend fun saveRecordShowIcon(context: Context, show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECORD_SHOW_ICON_KEY] = show
        }
    }

    /**
     * 获取账单条目显示完整分类流（默认 false）。
     */
    fun recordShowFullCategoryFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RECORD_SHOW_FULL_CATEGORY_KEY] ?: false
    }

    /**
     * 保存账单条目显示完整分类设置。
     */
    suspend fun saveRecordShowFullCategory(context: Context, show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECORD_SHOW_FULL_CATEGORY_KEY] = show
        }
    }

    /**
     * 获取账单条目备注优先流（默认 false）。
     */
    fun recordNotePriorityFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RECORD_NOTE_PRIORITY_KEY] ?: false
    }

    /**
     * 保存账单条目备注优先设置。
     */
    suspend fun saveRecordNotePriority(context: Context, priority: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[RECORD_NOTE_PRIORITY_KEY] = priority
        }
    }

    // endregion

    // region 语言设置读写

    /**
     * 获取应用语言设置流。
     * 返回值："system" | "zh" | "en"
     */
    fun languageFlow(context: Context): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE_KEY] ?: "system"
    }

    /**
     * 保存应用语言设置。
     */
    suspend fun saveLanguage(context: Context, code: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE_KEY] = code
        }
    }

    // endregion

    // region 小键盘数字键布局设置

    /**
     * 获取小键盘数字键交换顶部与第三行状态流（默认 false）。
     */
    fun numpadSwapTopBottomRowsFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NUMPAD_SWAP_TOP_BOTTOM_ROWS_KEY] ?: false
    }

    /**
     * 保存小键盘数字键交换顶部与第三行设置。
     */
    suspend fun saveNumpadSwapTopBottomRows(context: Context, swap: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NUMPAD_SWAP_TOP_BOTTOM_ROWS_KEY] = swap
        }
    }

    // endregion

    /**
     * 清空所有设置数据。
     */
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 导出所有设置项为 JSON 字符串。
     */
    suspend fun snapshotJson(context: Context): String {
        val preferences = context.dataStore.data.first()
        return kotlinx.serialization.json.JsonObject(
            mapOf(
                "schema" to kotlinx.serialization.json.JsonPrimitive(1),
                "currency_symbol" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[CURRENCY_SYMBOL_KEY] ?: "¥"
                ),
                "theme_mode" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[THEME_MODE_KEY] ?: "system"
                ),
                "preset_color_scheme" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[PRESET_COLOR_SCHEME_KEY] ?: "default"
                ),
                "auto_backup_enabled" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_ENABLED_KEY] ?: false
                ),
                "auto_backup_frequency" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_FREQUENCY_KEY] ?: "daily"
                ),
                "auto_backup_max_count" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_MAX_COUNT_KEY] ?: 7
                ),
                "auto_backup_copy_to_external" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_COPY_TO_EXTERNAL_KEY] ?: false
                ),
                "auto_backup_external_uri" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_EXTERNAL_URI_KEY]
                ),
                "auto_backup_last_time" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[AUTO_BACKUP_LAST_TIME_KEY]
                ),
                "record_show_icon" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[RECORD_SHOW_ICON_KEY] ?: true
                ),
                "record_show_full_category" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[RECORD_SHOW_FULL_CATEGORY_KEY] ?: false
                ),
                "record_note_priority" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[RECORD_NOTE_PRIORITY_KEY] ?: false
                ),
                "app_language" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[APP_LANGUAGE_KEY] ?: "system"
                ),
                "numpad_swap_top_bottom_rows" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[NUMPAD_SWAP_TOP_BOTTOM_ROWS_KEY] ?: false
                ),
            )
        ).toString()
    }

    /**
     * 从 JSON 字符串恢复设置项。
     *
     * 若 JSON 中不含支持的 schema 版本号，则跳过恢复（避免旧格式或损坏数据）。
     * 旧数据中的 dynamic_color / seed_color 字段会被静默忽略。
     */
    suspend fun restoreFromJson(context: Context, json: String) {
        val element = Json.parseToJsonElement(json)
        val obj = element.jsonObject
        val schemaVersion = obj["schema"]?.jsonPrimitive?.intOrNull ?: 0
        if (schemaVersion != 1) {
            // schema 不匹配时保留默认设置，不抛异常（保证应用可用性）
            return
        }
        context.dataStore.edit { preferences ->
            // 不调用 clear()，只覆盖 JSON 中存在的键，保证原子性
            obj["currency_symbol"]?.jsonPrimitive?.content?.let {
                preferences[CURRENCY_SYMBOL_KEY] = it
            }
            obj["theme_mode"]?.jsonPrimitive?.content?.let {
                preferences[THEME_MODE_KEY] = it
            }
            obj["preset_color_scheme"]?.jsonPrimitive?.content?.let {
                preferences[PRESET_COLOR_SCHEME_KEY] = it
            }

            // 自动备份配置（旧备份可能不含这些字段，缺失时保留当前值）
            obj["auto_backup_enabled"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[AUTO_BACKUP_ENABLED_KEY] = it
            }
            obj["auto_backup_frequency"]?.jsonPrimitive?.content?.let {
                preferences[AUTO_BACKUP_FREQUENCY_KEY] = it
            }
            obj["auto_backup_max_count"]?.jsonPrimitive?.intOrNull?.let {
                preferences[AUTO_BACKUP_MAX_COUNT_KEY] = it.coerceIn(3, 30)
            }
            obj["auto_backup_copy_to_external"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[AUTO_BACKUP_COPY_TO_EXTERNAL_KEY] = it
            }

            // 处理可能为 JSON null 的可选字符串字段
            when (val el = obj["auto_backup_external_uri"]) {
                is JsonNull -> preferences.remove(AUTO_BACKUP_EXTERNAL_URI_KEY)
                is JsonPrimitive -> preferences[AUTO_BACKUP_EXTERNAL_URI_KEY] = el.content
                else -> {}
            }
            when (val el = obj["auto_backup_last_time"]) {
                is JsonNull -> preferences.remove(AUTO_BACKUP_LAST_TIME_KEY)
                is JsonPrimitive -> preferences[AUTO_BACKUP_LAST_TIME_KEY] = el.content
                else -> {}
            }

            // 账单条目内容显示设置（旧备份可能不含这些字段，缺失时保留当前值）
            obj["record_show_icon"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[RECORD_SHOW_ICON_KEY] = it
            }
            obj["record_show_full_category"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[RECORD_SHOW_FULL_CATEGORY_KEY] = it
            }
            obj["record_note_priority"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[RECORD_NOTE_PRIORITY_KEY] = it
            }
            obj["app_language"]?.jsonPrimitive?.content?.let {
                preferences[APP_LANGUAGE_KEY] = it
            }

            // 小键盘数字键布局设置（旧备份可能不含此字段，缺失时保留当前值）
            obj["numpad_swap_top_bottom_rows"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[NUMPAD_SWAP_TOP_BOTTOM_ROWS_KEY] = it
            }
        }
    }
}
