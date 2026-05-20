package com.agguy.moni.app.model

/**
 * 聊天消息数据模型。
 *
 * 表示 AI 记账对话中的单条消息，支持文本消息与记账卡片两种形态。
 *
 * @property id 消息 ID，-1 表示未保存的新消息
 * @property sessionId 会话标识
 * @property messageType 消息类型
 * @property content 文本内容
 * @property cardData 卡片数据，仅 [MessageType.AI_CARD] 类型时使用
 * @property cardStatus 卡片状态，仅 [MessageType.AI_CARD] 类型时使用
 * @property createdAt 创建时间戳（秒）
 */
data class ChatMessage(
    val id: Long = -1L,
    val sessionId: String = "",
    val messageType: MessageType = MessageType.AI_CARD,
    val content: String = "",
    val cardData: DraftCardData? = null,
    val cardStatus: CardStatus? = null,
    val createdAt: Long = 0L
) {
    /**
     * 快速创建消息，自动将 [id] 设置为 -1（未保存）。
     */
    constructor(
        sessionId: String,
        messageType: MessageType,
        content: String,
        cardData: DraftCardData? = null,
        cardStatus: CardStatus? = null,
        createdAt: Long = 0L
    ) : this(
        id = -1L,
        sessionId = sessionId,
        messageType = messageType,
        content = content,
        cardData = cardData,
        cardStatus = cardStatus,
        createdAt = createdAt
    )
}
