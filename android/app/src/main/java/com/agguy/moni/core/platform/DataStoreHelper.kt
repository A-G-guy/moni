package com.agguy.moni.core.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agguy.moni.app.theme.PresetColorScheme
import com.agguy.moni.app.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
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
            preferences.clear()
            obj["currency_symbol"]?.jsonPrimitive?.content?.let {
                preferences[CURRENCY_SYMBOL_KEY] = it
            }
            obj["theme_mode"]?.jsonPrimitive?.content?.let {
                preferences[THEME_MODE_KEY] = it
            }
            obj["preset_color_scheme"]?.jsonPrimitive?.content?.let {
                preferences[PRESET_COLOR_SCHEME_KEY] = it
            }
        }
    }
}
