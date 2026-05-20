package com.agguy.moni.app.repository

import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.ChatMessage

/**
 * AI 记账聊天消息数据仓库接口。
 */
interface ChatRepository {
    /** 插入新消息，返回自增 ID。 */
    suspend fun insert(message: ChatMessage): Long

    /** 按会话 ID 分页查询消息列表。 */
    suspend fun getBySession(sessionId: String, limit: Int = 50, offset: Int = 0): List<ChatMessage>

    /** 更新消息卡片状态。 */
    suspend fun updateStatus(id: Long, status: CardStatus)

    /** 删除单条消息。 */
    suspend fun delete(id: Long)

    /** 清空指定会话的所有消息。 */
    suspend fun clearSession(sessionId: String)
}
