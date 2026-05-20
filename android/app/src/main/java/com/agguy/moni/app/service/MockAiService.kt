package com.agguy.moni.app.service

import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.core.RecordType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * 模拟 AI 解析服务。
 *
 * 通过简单的关键词匹配与正则表达式模拟 AI 对用户自然语言输入的理解，
 * 从中提取记账所需的结构化信息。不接入真实 LLM API。
 */
object MockAiService {

    /**
     * AI 解析结果。
     *
     * @property isBookkeeping 输入是否为记账内容
     * @property replyText AI 回复文本
     * @property cardData 解析出的卡片数据（仅 [isBookkeeping] = true 时存在）
     */
    data class ParseResult(
        val isBookkeeping: Boolean,
        val replyText: String,
        val cardData: DraftCardData? = null
    )

    private val amountRegex = Regex("(\\d+(?:\\.\\d{1,2})?)(?:\\s*[元块])?")

    private val categoryRules: List<Pair<List<String>, CategoryDef>> = listOf(
        listOf("餐", "吃", "饭", "麦当劳", "肯德基", "奶茶") to CategoryDef(1, "餐饮"),
        listOf("车", "地铁", "公交", "打车", "油费") to CategoryDef(2, "交通"),
        listOf("超市", "买", "购物", "淘宝", "京东") to CategoryDef(3, "购物"),
        listOf("电影", "娱乐", "游戏", "KTV") to CategoryDef(4, "娱乐"),
        listOf("水电", "房租", "物业") to CategoryDef(5, "居住"),
        listOf("医院", "药", "看病") to CategoryDef(6, "医疗"),
    )

    private val politeReplies = listOf(
        "谢谢夸奖，今天有产生什么开销需要我帮你记下来吗？",
        "好的呢，有需要记账的开销随时告诉我哦～",
        "嗯嗯，今天花钱了吗？我来帮你记账～",
        "好的，有什么开销需要我帮你记录吗？",
        "收到～需要我帮你记一笔账吗？"
    )

    /**
     * 解析用户输入的文本，提取记账信息。
     *
     * @param text 用户输入的自然语言文本
     * @return [ParseResult] 解析结果
     */
    fun parse(text: String): ParseResult {
        val amountMatch = amountRegex.find(text)
        val amountStr = amountMatch?.groupValues?.get(1) ?: return nonBookkeepingReply()
        val amount = amountStr.toDoubleOrNull() ?: return nonBookkeepingReply()

        val amountCents = (amount * 100).roundToLong()
        val category = findCategory(text)
        val timestamp = parseTime(text)

        val categoryDisplay = if (category.id > 0) category.name else "未分类"
        val replyText = "好的，已记录一笔${categoryDisplay}支出${amount}元。"

        val cardData = DraftCardData(
            amountCents = amountCents,
            recordType = RecordType.EXPENSE,
            categoryId = category.id,
            accountId = -1L,
            timestamp = timestamp,
            note = text
        )

        return ParseResult(
            isBookkeeping = true,
            replyText = replyText,
            cardData = cardData
        )
    }

    private fun nonBookkeepingReply(): ParseResult {
        return ParseResult(
            isBookkeeping = false,
            replyText = politeReplies.random()
        )
    }

    private fun findCategory(text: String): CategoryDef {
        for ((keywords, categoryDef) in categoryRules) {
            if (keywords.any { text.contains(it) }) {
                return categoryDef
            }
        }
        return CategoryDef(-1L, "未分类")
    }

    internal fun parseTime(text: String): Long {
        val now = LocalDateTime.now()

        val dateOffset = when {
            text.contains("前天") -> -2L
            text.contains("昨天") -> -1L
            else -> 0L
        }

        val timeOfDay = when {
            text.contains("早上") -> LocalTime.of(8, 0)
            text.contains("中午") -> LocalTime.of(12, 0)
            text.contains("晚上") -> LocalTime.of(20, 0)
            else -> null
        }

        val dateRegex = Regex("(\\d{1,2})月(\\d{1,2})日")
        val dateMatch = dateRegex.find(text)

        return if (dateMatch != null) {
            val month = dateMatch.groupValues[1].toInt()
            val day = dateMatch.groupValues[2].toInt()
            val resolvedTime = timeOfDay ?: LocalTime.from(now)
            val dateTime = LocalDateTime.of(
                now.year, month, day, resolvedTime.hour, resolvedTime.minute, 0
            )
            dateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        } else {
            val baseDate = now.toLocalDate().plusDays(dateOffset)
            val resolvedTime = timeOfDay ?: LocalTime.from(now)
            val dateTime = LocalDateTime.of(baseDate, resolvedTime)
            dateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
        }
    }

    private data class CategoryDef(
        val id: Long,
        val name: String
    )
}
