package com.agguy.moni.core

import com.agguy.moni.core.platform.LogCollector
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CoreEffectRunner {
    var onShowSnackbar: ((String) -> Unit)? = null
    var onNavigate: ((String) -> Unit)? = null
    var onExportFile: ((String, String) -> Unit)? = null

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
            "export_file" -> {
                val json = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson).jsonObject
                } catch (e: Exception) {
                    LogCollector.w("Moni", "导出参数解析失败: ${e.message}")
                    null
                }
                val format = json?.get("format")?.jsonPrimitive?.content ?: "csv"
                val content = json?.get("content")?.jsonPrimitive?.content ?: ""
                LogCollector.i("CoreEffectRunner", "导出文件: format=$format, contentLength=${content.length}")
                onExportFile?.invoke(format, content)
            }
            else -> LogCollector.w("MoniEffect", "未知的 effect 类型: ${effect.kind}")
        }
    }
}
