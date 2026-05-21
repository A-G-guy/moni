package com.agguy.moni.app.repository

import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.core.BridgeJson
import com.agguy.moni.core.RecordType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import uniffi.moni_core.MoniCore

/**
 * AI 记账聊天消息数据仓库实现。
 *
 * 通过 UniFFI 直接调用 Rust 内核的数据库操作。
 */
class ChatRepositoryImpl(
    private val core: MoniCore,
) : ChatRepository {

    override suspend fun insert(message: ChatMessage): Long {
        val cardDataJson = message.cardData?.let { encodeDraftCardData(it) }
        val cardStatus = message.cardStatus?.name
        return core.chatInsert(
            sessionId = message.sessionId,
            messageType = message.messageType.name,
            content = message.content,
            cardDataJson = cardDataJson,
            cardStatus = cardStatus,
        )
    }

    override suspend fun getBySession(sessionId: String, limit: Int, offset: Int): List<ChatMessage> {
        val json = core.chatGetBySession(sessionId, limit.toLong(), offset.toLong())
        val rows: List<ChatMessageDto> = BridgeJson.decodeFromString(ListSerializer(ChatMessageDto.serializer()), json)
        return rows.map { it.toModel() }
    }

    override suspend fun updateStatus(id: Long, status: CardStatus) {
        core.chatUpdateStatus(id, status.name)
    }

    override suspend fun delete(id: Long) {
        core.chatDelete(id)
    }

    override suspend fun clearSession(sessionId: String) {
        core.chatClearSession(sessionId)
    }

    /**
     * Rust 返回的聊天消息 JSON 结构映射。
     */
    @Serializable
    private data class ChatMessageDto(
        val id: Long,
        @SerialName("session_id") val sessionId: String,
        @SerialName("message_type") val messageType: String,
        val content: String,
        @SerialName("card_data_json") val cardDataJson: String? = null,
        @SerialName("card_status") val cardStatus: String? = null,
        @SerialName("created_at") val createdAt: Long,
    ) {
        fun toModel(): ChatMessage = ChatMessage(
            id = id,
            sessionId = sessionId,
            messageType = parseMessageType(messageType),
            content = content,
            cardData = cardDataJson?.let { decodeDraftCardData(it) },
            cardStatus = cardStatus?.let { parseCardStatus(it) },
            createdAt = createdAt,
        )

        private fun parseMessageType(raw: String): MessageType =
            try {
                MessageType.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                MessageType.SYSTEM
            }

        private fun parseCardStatus(raw: String): CardStatus =
            try {
                CardStatus.valueOf(raw)
            } catch (_: IllegalArgumentException) {
                CardStatus.EXPIRED
            }
    }
}

/**
 * 将 [DraftCardData] 手动序列化为 JSON 字符串。
 * 由于 [DraftCardData] 未添加 @Serializable，使用手动构造避免引入模型层改动。
 */
private fun encodeDraftCardData(data: DraftCardData): String {
    val noteEscaped = data.note.replace("\\", "\\\\").replace("\"", "\\\"")
    return """{"amount_cents":${data.amountCents},"record_type":"${data.recordType.name.lowercase()}","category_id":${data.categoryId},"account_id":${data.accountId},"timestamp":${data.timestamp},"note":"$noteEscaped"}"""
}

/**
 * 将 JSON 字符串手动反序列化为 [DraftCardData]。
 */
private fun decodeDraftCardData(json: String): DraftCardData {
    val obj = BridgeJson.parseToJsonElement(json).jsonObject
    return DraftCardData(
        amountCents = obj["amount_cents"]?.jsonPrimitive?.longOrNull ?: 0L,
        recordType = when (obj["record_type"]?.jsonPrimitive?.content) {
            "income" -> RecordType.INCOME
            else -> RecordType.EXPENSE
        },
        categoryId = obj["category_id"]?.jsonPrimitive?.longOrNull ?: -1L,
        accountId = obj["account_id"]?.jsonPrimitive?.longOrNull ?: -1L,
        timestamp = obj["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L,
        note = obj["note"]?.jsonPrimitive?.content ?: "",
    )
}
