package com.agguy.moni.core.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agguy.moni.app.theme.DefaultSeedColor
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
import kotlinx.serialization.json.longOrNull

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "moni_settings")

/**
 * 设置数据存储帮助类。
 *
 * 封装 DataStore Preferences 读写操作，用于持久化应用设置。
 */
object DataStoreHelper {
    private val CURRENCY_SYMBOL_KEY = stringPreferencesKey("currency_symbol")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val SEED_COLOR_KEY = longPreferencesKey("seed_color")

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
     * 获取动态颜色开关流。
     */
    fun dynamicColorFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: false
    }

    /**
     * 保存动态颜色开关。
     */
    suspend fun saveDynamicColor(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    /**
     * 获取种子色流。
     */
    fun seedColorFlow(context: Context): Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SEED_COLOR_KEY] ?: DefaultSeedColor.value.toLong()
    }

    /**
     * 保存种子色。
     */
    suspend fun saveSeedColor(context: Context, colorValue: Long) {
        context.dataStore.edit { preferences ->
            preferences[SEED_COLOR_KEY] = colorValue
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
                "dynamic_color" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[DYNAMIC_COLOR_KEY] ?: false
                ),
                "seed_color" to kotlinx.serialization.json.JsonPrimitive(
                    preferences[SEED_COLOR_KEY] ?: DefaultSeedColor.value.toLong()
                ),
            )
        ).toString()
    }

    /**
     * 从 JSON 字符串恢复设置项。
     *
     * 若 JSON 中不含支持的 schema 版本号，则跳过恢复（避免旧格式或损坏数据）。
     */
    suspend fun restoreFromJson(context: Context, json: String) {
        val element = kotlinx.serialization.json.Json.parseToJsonElement(json)
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
            obj["dynamic_color"]?.jsonPrimitive?.booleanOrNull?.let {
                preferences[DYNAMIC_COLOR_KEY] = it
            }
            obj["seed_color"]?.jsonPrimitive?.longOrNull?.let {
                preferences[SEED_COLOR_KEY] = it
            }
        }
    }
}
