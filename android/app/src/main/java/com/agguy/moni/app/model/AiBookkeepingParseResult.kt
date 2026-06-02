package com.agguy.moni.app.model

import com.agguy.moni.core.RecordType
import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** AI 记账解析结果。 */
data class AiBookkeepingParseResult(
    val isBookkeeping: Boolean,
    val replyText: String,
    val cardData: DraftCardData?,
    val confidence: Double,
    val clarificationQuestion: String?,
)

@Serializable
internal data class AiBookkeepingParseResultDto(
    @SerialName("is_bookkeeping") val isBookkeeping: Boolean,
    @SerialName("reply_text") val replyText: String,
    @SerialName("card_data") val cardData: AiDraftCardDataDto? = null,
    val confidence: Double = 0.0,
    @SerialName("clarification_question") val clarificationQuestion: String? = null,
) {
    fun toModel(): AiBookkeepingParseResult = AiBookkeepingParseResult(
        isBookkeeping = isBookkeeping,
        replyText = replyText,
        cardData = cardData?.toModel(),
        confidence = confidence,
        clarificationQuestion = clarificationQuestion,
    )
}

@Serializable
internal data class AiDraftCardDataDto(
    @SerialName("amount_cents") val amountCents: Long = 0L,
    @SerialName("record_type") val recordType: String = "expense",
    @SerialName("category_id") val categoryId: Long = -1L,
    val timestamp: Long = 0L,
    val note: String = "",
) {
    fun toModel(): DraftCardData = DraftCardData(
        amountCents = amountCents,
        recordType = parseRecordType(recordType),
        categoryId = categoryId,
        timestamp = timestamp,
        note = note,
    )

    private fun parseRecordType(raw: String): RecordType =
        when (raw.lowercase(Locale.ROOT)) {
            "income" -> RecordType.INCOME
            else -> RecordType.EXPENSE
        }
}
