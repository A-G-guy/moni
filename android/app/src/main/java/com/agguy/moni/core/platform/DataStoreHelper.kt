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
import kotlinx.coroutines.flow.map

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
    fun currencySymbolFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CURRENCY_SYMBOL_KEY] ?: "¥"
        }
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
    fun themeModeFlow(context: Context): Flow<ThemeMode> {
        return context.dataStore.data.map { preferences ->
            when (preferences[THEME_MODE_KEY]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
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
    fun dynamicColorFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: false
        }
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
    fun seedColorFlow(context: Context): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[SEED_COLOR_KEY] ?: DefaultSeedColor.value.toLong()
        }
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
}
