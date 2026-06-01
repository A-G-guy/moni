package com.agguy.moni.app.ui.aibookkeeping

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

private const val MAX_IMAGE_EDGE = 1600
private const val JPEG_QUALITY = 84

/** 将用户选择的图片压缩为 Rust Core 可校验和适配的 base64 payload。 */
object AiImagePayloadReader {
    fun read(context: Context, uri: Uri): AiSelectedImage {
        val originalBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取图片")
        val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            ?: error("无法解析图片")
        val scaledBitmap = bitmap.scaleToMaxEdge(MAX_IMAGE_EDGE)
        val compressedBytes = ByteArrayOutputStream().use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        return AiSelectedImage(
            uri = uri,
            mimeType = "image/jpeg",
            base64Data = Base64.encodeToString(compressedBytes, Base64.NO_WRAP),
            originalSizeBytes = originalBytes.size.toLong(),
        )
    }

    private fun Bitmap.scaleToMaxEdge(maxEdge: Int): Bitmap {
        val currentMaxEdge = maxOf(width, height)
        if (currentMaxEdge <= maxEdge) return this
        val scale = maxEdge.toFloat() / currentMaxEdge.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
