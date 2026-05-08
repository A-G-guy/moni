package com.agguy.moni.core

import android.content.Context
import com.agguy.moni.app.i18n.MessageResolver
import com.agguy.moni.core.platform.LogCollector
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CoreEffectRunner(context: Context) {
    private val appContext = context.applicationContext

    var onShowSnackbar: ((String) -> Unit)? = null
    var onNavigate: ((String) -> Unit)? = null
    var onPersistSetting: ((String, String) -> Unit)? = null

    fun runEffect(effect: CoreEffect) {
        when (effect.kind) {
            "log" -> LogCollector.d("MoniEffect", effect.payloadJson)

            "persist_setting" -> {
                val payload = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject
                } catch (e: Exception) {
                    LogCollector.w("Moni", "persist_setting payload 解析失败: ${e.message}")
                    null
                }
                val key = payload?.get("key")?.jsonPrimitive?.content
                val value = payload?.get("value")?.jsonPrimitive?.content
                if (key != null && value != null) {
                    LogCollector.i("CoreEffectRunner", "持久化设置: $key = $value")
                    onPersistSetting?.invoke(key, value)
                } else {
                    LogCollector.w("Moni", "persist_setting 缺少 key 或 value")
                }
            }

            "show_snackbar" -> {
                val payload = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject
                } catch (e: Exception) {
                    LogCollector.w("Moni", "Effect 消息解析失败，使用原始 payload: ${e.message}")
                    null
                }

                val messageKey = payload?.get("message_key")?.jsonPrimitive?.content
                val message = if (messageKey != null) {
                    val args = mutableMapOf<String, String>()
                    payload["count"]?.jsonPrimitive?.content?.let { args["count"] = it }
                    MessageResolver.resolve(appContext, messageKey, args)
                } else {
                    payload?.get("message")?.jsonPrimitive?.content ?: effect.payloadJson
                }
                LogCollector.i("CoreEffectRunner", "显示 Snackbar: $message")
                onShowSnackbar?.invoke(message)
            }

            "navigate" -> {
                val screen = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject["screen"]?.jsonPrimitive?.content ?: ""
                } catch (e: Exception) {
                    LogCollector.w("Moni", "导航目标解析失败: ${e.message}")
                    ""
                }
                LogCollector.i("CoreEffectRunner", "导航到: $screen")
                onNavigate?.invoke(screen)
            }

            else -> LogCollector.w("MoniEffect", "未知的 effect 类型: ${effect.kind}")
        }
    }
}
