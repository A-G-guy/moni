package com.agguy.moni.app.model

/**
 * 记账卡片生命周期状态枚举。
 */
enum class CardStatus {
    /** 待确认态（可编辑） */
    DRAFT,

    /** 已保存态（只读） */
    SAVED,

    /** 已失效态（不可交互） */
    EXPIRED
}
