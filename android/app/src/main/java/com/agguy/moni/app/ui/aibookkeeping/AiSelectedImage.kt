package com.agguy.moni.app.ui.aibookkeeping

import android.net.Uri
import com.agguy.moni.app.model.AiBookkeepingImageInput

/** AI 记账待发送图片。 */
data class AiSelectedImage(
    val uri: Uri,
    val mimeType: String,
    val base64Data: String,
    val originalSizeBytes: Long,
) {
    fun toInput(): AiBookkeepingImageInput = AiBookkeepingImageInput(
        mimeType = mimeType,
        base64Data = base64Data,
        originalSizeBytes = originalSizeBytes,
    )
}
