package com.agguy.moni.core

import com.agguy.moni.core.platform.LogCollector
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CoreEffectRunner {
    var onShowSnackbar: ((String) -> Unit)? = null
    var onNavigate: ((String) -> Unit)? = null

    fun runEffect(effect: CoreEffect) {
        when (effect.kind) {
            "log" -> LogCollector.d("MoniEffect", effect.payloadJson)

            "show_snackbar" -> {
                val message = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject["message"]?.jsonPrimitive?.content ?: effect.payloadJson
                } catch (e: Exception) {
                    LogCollector.w("Moni", "Effect 消息解析失败，使用原始 payload: ${e.message}")
                    effect.payloadJson
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
