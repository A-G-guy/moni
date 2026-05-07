package com.agguy.moni.app

/**
 * 账单条目内容显示设置。
 *
 * 控制账单列表中每个条目（RecordListItem）的显示方式。
 */
data class RecordItemDisplaySettings(
    val showIcon: Boolean = true,
    val showFullCategory: Boolean = false,
    val notePriority: Boolean = false
)
