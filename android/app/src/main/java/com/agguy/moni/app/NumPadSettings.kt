package com.agguy.moni.app

/**
 * 小键盘数字键布局设置。
 *
 * 控制记账页自定义小键盘的数字行排列方式。
 */
data class NumPadSettings(
    /** 是否交换顶部（789）与第三行（123）数字位置。关闭（默认）保留 789 在顶部、123 在第三行。 */
    val swapTopAndBottomRows: Boolean = false
)
