package com.agguy.moni.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * 应用语言管理器。
 *
 * 封装语言设置的读写、Locale 应用与恢复逻辑。
 * 支持三种语言选项：跟随系统、中文、英文。
 */
object AppLocaleManager {

    enum class AppLanguage(val code: String, val defaultCurrency: String) {
        SYSTEM("system", ""),
        CHINESE("zh", "¥"),
        ENGLISH("en", "$");

        companion object {
            fun fromCode(code: String): AppLanguage =
                entries.find { it.code == code } ?: SYSTEM
        }
    }

    /**
     * 获取当前保存的语言设置。
     */
    fun getSavedLanguage(context: Context): AppLanguage {
        val code = com.agguy.moni.core.platform.DataStoreHelper.languageFlow(context)
            .let { runBlockingOrDefault(it, AppLanguage.SYSTEM.code) }
        return AppLanguage.fromCode(code)
    }

    /**
     * 应用已保存的 Locale 到 Context，返回配置后的新 Context。
     *
     * 在 [android.app.Activity.attachBaseContext] 中调用，确保整个应用生命周期内
     * 资源加载使用正确的 Locale。
     */
    fun applyLocale(context: Context): Context {
        val savedLanguage = runBlockingOrDefault(
            com.agguy.moni.core.platform.DataStoreHelper.languageFlow(context),
            AppLanguage.SYSTEM.code
        )
        val language = AppLanguage.fromCode(savedLanguage)

        if (language == AppLanguage.SYSTEM) {
            return context
        }

        val locale = Locale(language.code)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))

        return context.createConfigurationContext(config)
    }

    /**
     * 获取当前实际生效的 Locale 显示名称（用于设置项 subtitle）。
     */
    fun getCurrentDisplayName(context: Context): String {
        val saved = runBlockingOrDefault(
            com.agguy.moni.core.platform.DataStoreHelper.languageFlow(context),
            AppLanguage.SYSTEM.code
        )
        return when (AppLanguage.fromCode(saved)) {
            AppLanguage.SYSTEM -> context.getString(com.agguy.moni.R.string.settings_language_system)
            AppLanguage.CHINESE -> context.getString(com.agguy.moni.R.string.settings_language_chinese)
            AppLanguage.ENGLISH -> context.getString(com.agguy.moni.R.string.settings_language_english)
        }
    }

    /**
     * 获取系统首选语言是否为中文。
     *
     * 用于判断首次启动时的默认货币符号。
     */
    fun isSystemLanguageChinese(context: Context): Boolean {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        val primary = locales.get(0)
        return primary?.language == "zh"
    }

    private fun <T> runBlockingOrDefault(flow: Flow<T>, default: T): T {
        return try {
            runBlocking { flow.first() }
        } catch (_: Exception) {
            default
        }
    }
}
