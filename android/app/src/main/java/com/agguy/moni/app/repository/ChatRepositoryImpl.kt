package com.agguy.moni.app.repository

import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.core.BridgeJson
import com.agguy.moni.core.BridgeJsonEncode
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
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
        val cardStatus = message.cardStatus?.name?.lowercase(Locale.ROOT)
        return core.chatInsert(
            sessionId = message.sessionId,
            messageType = message.messageType.name.lowercase(Locale.ROOT),
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
        core.chatUpdateStatus(id, status.name.lowercase(Locale.ROOT))
    }

    override suspend fun updateCardData(id: Long, cardData: DraftCardData) {
        core.chatUpdateCardData(id, encodeDraftCardData(cardData))
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
                MessageType.valueOf(raw.uppercase(Locale.ROOT))
            } catch (_: IllegalArgumentException) {
                MessageType.SYSTEM
            }

        private fun parseCardStatus(raw: String): CardStatus =
            try {
                CardStatus.valueOf(raw.uppercase(Locale.ROOT))
            } catch (_: IllegalArgumentException) {
                CardStatus.EXPIRED
            }
    }
}

/**
 * 将 [DraftCardData] 序列化为 JSON 字符串。
 */
private fun encodeDraftCardData(data: DraftCardData): String =
    BridgeJsonEncode.encodeToString(DraftCardData.serializer(), data)

/**
 * 将 JSON 字符串反序列化为 [DraftCardData]。
 */
private fun decodeDraftCardData(json: String): DraftCardData =
    BridgeJson.decodeFromString(DraftCardData.serializer(), json)
