package com.agguy.moni.core.platform

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream

/**
 * 数据导出帮助类。
 *
 * 通过 MediaStore API 将导出内容保存到系统 Download 目录。
 */
object ExportHelper {

    /**
     * 将内容保存到 Download 目录。
     *
     * @param context 上下文
     * @param format 文件格式（csv / json），决定扩展名和 MIME 类型
     * @param content 文件内容
     */
    fun saveToDownloads(context: Context, format: String, content: String) {
        val fileName = "moni_export_${System.currentTimeMillis()}.${format.lowercase()}"
        val mimeType = when (format.lowercase()) {
            "json" -> "application/json"
            else -> "text/csv"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: run {
                    Toast.makeText(context, "导出失败：无法创建文件", Toast.LENGTH_SHORT).show()
                    return
                }

            try {
                resolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                Toast.makeText(context, "已保存到 Download/$fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Android 9 及以下使用传统方式
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                Toast.makeText(context, "已保存到 Download/$fileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
