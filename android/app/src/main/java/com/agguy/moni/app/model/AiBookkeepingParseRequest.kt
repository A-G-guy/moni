package com.agguy.moni.app.model

import kotlinx.serialization.Serializable

/** AI 记账单轮解析请求。 */
data class AiBookkeepingParseRequest(
    val text: String,
    val images: List<AiBookkeepingImageInput> = emptyList(),
    val sentAt: Long? = null,
)

/** 发送给 Rust Core 的图片输入。 */
data class AiBookkeepingImageInput(
    val mimeType: String,
    val base64Data: String,
    val originalSizeBytes: Long? = null,
)

@Serializable
internal data class AiBookkeepingParseRequestDto(
    val text: String,
    val images: List<AiBookkeepingImageInputDto> = emptyList(),
    val sentAt: Long? = null,
)

@Serializable
internal data class AiBookkeepingImageInputDto(
    val mimeType: String,
    val base64Data: String,
    val originalSizeBytes: Long? = null,
)

internal fun AiBookkeepingParseRequest.toDto(): AiBookkeepingParseRequestDto =
    AiBookkeepingParseRequestDto(
        text = text,
        images = images.map { it.toDto() },
        sentAt = sentAt,
    )

private fun AiBookkeepingImageInput.toDto(): AiBookkeepingImageInputDto =
    AiBookkeepingImageInputDto(
        mimeType = mimeType,
        base64Data = base64Data,
        originalSizeBytes = originalSizeBytes,
    )
