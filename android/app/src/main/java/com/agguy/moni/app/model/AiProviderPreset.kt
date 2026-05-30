package com.agguy.moni.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** AI Provider API 格式。 */
enum class AiApiFormat(val wireName: String, val displayName: String) {
    OpenAiChatCompletions("open_ai_chat_completions", "OpenAI Compatible"),
    GeminiGenerateContent("gemini_generate_content", "Gemini")
}

/** 模型思考程度。 */
enum class AiThinkingLevel(val wireName: String, val displayName: String) {
    Off("off", "关闭"),
    Low("low", "低"),
    Medium("medium", "中"),
    High("high", "高")
}

/** AI Provider 预设列表项。 */
data class AiProviderPreset(
    val id: Long,
    val name: String,
    val apiFormat: AiApiFormat,
    val baseUrl: String,
    val maskedApiKey: String,
    val hasApiKey: Boolean,
    val model: String,
    val thinkingLevel: AiThinkingLevel,
    val supportsVision: Boolean,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** AI Provider 预设保存请求。 */
data class AiProviderPresetSaveRequest(
    val id: Long? = null,
    val name: String,
    val apiFormat: AiApiFormat,
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val thinkingLevel: AiThinkingLevel = AiThinkingLevel.Off,
    val supportsVision: Boolean = false,
    val isDefault: Boolean = false,
)

@Serializable
internal data class AiProviderPresetDto(
    val id: Long,
    val name: String,
    @SerialName("api_format") val apiFormat: String,
    @SerialName("base_url") val baseUrl: String,
    @SerialName("masked_api_key") val maskedApiKey: String = "",
    @SerialName("has_api_key") val hasApiKey: Boolean = false,
    val model: String,
    @SerialName("thinking_level") val thinkingLevel: String = "off",
    @SerialName("supports_vision") val supportsVision: Boolean = false,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
) {
    fun toModel(): AiProviderPreset = AiProviderPreset(
        id = id,
        name = name,
        apiFormat = parseApiFormat(apiFormat),
        baseUrl = baseUrl,
        maskedApiKey = maskedApiKey,
        hasApiKey = hasApiKey,
        model = model,
        thinkingLevel = parseThinkingLevel(thinkingLevel),
        supportsVision = supportsVision,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Serializable
internal data class AiProviderPresetSaveRequestDto(
    val id: Long? = null,
    val name: String,
    @SerialName("api_format") val apiFormat: String,
    @SerialName("base_url") val baseUrl: String,
    @SerialName("api_key") val apiKey: String? = null,
    val model: String,
    @SerialName("thinking_level") val thinkingLevel: String = "off",
    @SerialName("supports_vision") val supportsVision: Boolean = false,
    @SerialName("is_default") val isDefault: Boolean = false,
)

internal fun AiProviderPresetSaveRequest.toDto(): AiProviderPresetSaveRequestDto =
    AiProviderPresetSaveRequestDto(
        id = id,
        name = name,
        apiFormat = apiFormat.wireName,
        baseUrl = baseUrl,
        apiKey = apiKey?.takeIf { it.isNotBlank() },
        model = model,
        thinkingLevel = thinkingLevel.wireName,
        supportsVision = supportsVision,
        isDefault = isDefault,
    )

internal fun parseApiFormat(raw: String): AiApiFormat =
    AiApiFormat.entries.firstOrNull { it.wireName == raw } ?: AiApiFormat.OpenAiChatCompletions

internal fun parseThinkingLevel(raw: String): AiThinkingLevel =
    AiThinkingLevel.entries.firstOrNull { it.wireName == raw } ?: AiThinkingLevel.Off
