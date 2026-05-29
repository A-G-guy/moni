package com.agguy.moni.app.model

import com.agguy.moni.core.RecordType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AI 生成的待确认记账卡片数据。
 *
 * 由 AI 从用户自然语言中提取的结构化记账信息，
 * 待用户确认后可转换为正式记录。
 *
 * @property amountCents 金额（单位：分）
 * @property recordType 收支类型
 * @property categoryId 分类 ID，-1 表示未分类
 * @property accountId 账户 ID，-1 表示未指定
 * @property timestamp 时间戳（秒），0 表示未指定
 * @property note 备注
 */
@Serializable
data class DraftCardData(
    @SerialName("amount_cents") val amountCents: Long = 0L,
    @SerialName("record_type") val recordType: RecordType = RecordType.EXPENSE,
    @SerialName("category_id") val categoryId: Long = -1L,
    @SerialName("account_id") val accountId: Long = -1L,
    val timestamp: Long = 0L,
    val note: String = ""
)
