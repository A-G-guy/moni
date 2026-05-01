package com.agguy.moni.core

import android.util.Log
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CoreEffectRunner {
    var onShowSnackbar: ((String) -> Unit)? = null
    var onNavigate: ((String) -> Unit)? = null
    var onExportFile: ((String, String) -> Unit)? = null

    fun runEffect(effect: CoreEffect) {
        when (effect.kind) {
            "log" -> Log.d("MoniEffect", effect.payloadJson)
            "show_snackbar" -> {
                val message = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject["message"]?.jsonPrimitive?.content ?: effect.payloadJson
                } catch (e: Exception) {
                    Log.w("Moni", "Effect 消息解析失败，使用原始 payload: ${e.message}")
                    effect.payloadJson
                }
                onShowSnackbar?.invoke(message)
            }
            "navigate" -> {
                val screen = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson)
                        .jsonObject["screen"]?.jsonPrimitive?.content ?: ""
                } catch (e: Exception) {
                    Log.w("Moni", "导航目标解析失败: ${e.message}")
                    ""
                }
                onNavigate?.invoke(screen)
            }
            "export_file" -> {
                val json = try {
                    kotlinx.serialization.json.Json.parseToJsonElement(effect.payloadJson).jsonObject
                } catch (e: Exception) {
                    Log.w("Moni", "导出参数解析失败: ${e.message}")
                    null
                }
                val format = json?.get("format")?.jsonPrimitive?.content ?: "csv"
                val content = json?.get("content")?.jsonPrimitive?.content ?: ""
                onExportFile?.invoke(format, content)
            }
            else -> Log.w("MoniEffect", "未知的 effect 类型: ${effect.kind}")
        }
    }
}
