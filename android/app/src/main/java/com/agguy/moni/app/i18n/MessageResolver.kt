package com.agguy.moni.app.i18n

import android.content.Context
import com.agguy.moni.R

/**
 * Snackbar 消息 key 解析器。
 *
 * 将 Rust 后端返回的 `message_key` 映射为本地化字符串。
 */
object MessageResolver {

    fun resolve(context: Context, key: String): String {
        val resId = when (key) {
            "category_created" -> R.string.message_category_created
            "category_updated" -> R.string.message_category_updated
            "category_archived" -> R.string.message_category_archived
            "category_unarchived" -> R.string.message_category_unarchived
            "category_deleted" -> R.string.message_category_deleted
            "sort_saved" -> R.string.message_sort_saved
            "record_saved" -> R.string.message_record_saved
            "record_updated" -> R.string.message_record_updated
            "record_deleted" -> R.string.message_record_deleted
            "budget_saved" -> R.string.message_budget_saved
            "category_budget_saved" -> R.string.message_category_budget_saved
            "total_budget_saved" -> R.string.message_total_budget_saved
            "budget_deleted" -> R.string.message_budget_deleted
            "presets_reset" -> R.string.message_presets_reset
            "mock_data_generated" -> R.string.message_mock_data_generated
            "data_cleared" -> R.string.message_data_cleared
            else -> 0
        }
        return if (resId != 0) context.getString(resId) else key
    }

    /**
     * 解析带参数的 message_key。
     */
    fun resolve(context: Context, key: String, args: Map<String, String>): String {
        val base = resolve(context, key)
        return if (key == "mock_data_generated" && args.containsKey("count")) {
            context.getString(R.string.message_mock_data_generated, args["count"])
        } else {
            base
        }
    }
}
