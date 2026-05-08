package com.agguy.moni.app.i18n

import android.content.Context
import com.agguy.moni.R

/**
 * 错误消息 key 解析器。
 *
 * 将 Rust 后端返回的 `error_key` 映射为本地化字符串，支持参数格式化。
 */
object ErrorMessageResolver {

    fun resolve(context: Context, key: String, args: List<String>): String {
        val resId = when (key) {
            "error_internal" -> R.string.error_internal
            "error_database" -> R.string.error_database
            "error_invalid_input" -> R.string.error_invalid_input
            "error_category_name_empty" -> R.string.error_category_name_empty
            "error_icon_name_empty" -> R.string.error_icon_name_empty
            "error_amount_must_be_positive" -> R.string.error_amount_must_be_positive
            "error_budget_amount_must_be_positive" -> R.string.error_budget_amount_must_be_positive
            "error_budget_only_expense" -> R.string.error_budget_only_expense
            "error_category_archived_for_record" -> R.string.error_category_archived_for_record
            "error_record_type_mismatch" -> R.string.error_record_type_mismatch
            "error_cannot_set_self_as_parent" -> R.string.error_cannot_set_self_as_parent
            "error_has_children_cannot_be_sub" -> R.string.error_has_children_cannot_be_sub
            "error_reorder_list_empty" -> R.string.error_reorder_list_empty
            "error_archived_cannot_sort" -> R.string.error_archived_cannot_sort
            "error_different_level_cannot_sort" -> R.string.error_different_level_cannot_sort
            "error_parent_category_archived" -> R.string.error_parent_category_archived
            "error_parent_child_type_mismatch" -> R.string.error_parent_child_type_mismatch
            "error_only_single_level" -> R.string.error_only_single_level
            "error_category_in_use" -> R.string.error_category_in_use
            "error_data_already_exists" -> R.string.error_data_already_exists
            "error_database_busy" -> R.string.error_database_busy
            "error_database_locked" -> R.string.error_database_locked
            "error_record_not_found" -> R.string.error_record_not_found
            "error_category_not_found" -> R.string.error_category_not_found
            "error_category_already_archived" -> R.string.error_category_already_archived
            "error_category_not_archived" -> R.string.error_category_not_archived
            "error_backup_zip" -> R.string.error_backup_zip
            "error_backup_manifest_invalid" -> R.string.error_backup_manifest_invalid
            "error_backup_corrupted" -> R.string.error_backup_corrupted
            "error_backup_too_new" -> R.string.error_backup_too_new
            "error_backup_restore_failed" -> R.string.error_backup_restore_failed
            "error_backup_io" -> R.string.error_backup_io
            "error_note_too_long" -> R.string.error_note_too_long
            else -> 0
        }

        return if (resId != 0) {
            formatWithArgs(context, resId, args)
        } else {
            key
        }
    }

    private fun formatWithArgs(context: Context, resId: Int, args: List<String>): String {
        return when (args.size) {
            0 -> context.getString(resId)
            1 -> context.getString(resId, args[0])
            2 -> context.getString(resId, args[0], args[1])
            else -> context.getString(resId, args[0], args[1], args[2])
        }
    }
}
