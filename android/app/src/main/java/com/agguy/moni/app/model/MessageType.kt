package com.agguy.moni.app.model

/**
 * 聊天消息类型枚举。
 */
enum class MessageType {
    /** 用户输入的文本消息 */
    USER_TEXT,

    /** AI 生成的记账卡片 */
    AI_CARD,

    /** AI 的纯文本回复（非记账内容） */
    AI_TEXT,

    /** 系统消息 */
    SYSTEM
}
