package com.agguy.moni.core.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
}
