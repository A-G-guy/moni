package com.agguy.moni.app.service

import com.agguy.moni.core.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MockAiServiceTest {

    @Test
    fun `中午吃麦当劳花了35 yields food category and 35 yuan at noon`() {
        val result = MockAiService.parse("中午吃麦当劳花了35")
        assertTrue("应为记账内容", result.isBookkeeping)
        assertNotNull("应生成卡片数据", result.cardData)

        val card = result.cardData!!
        assertEquals("金额应为 35 元（3500 分）", 3500L, card.amountCents)
        assertEquals("分类应为餐饮（id=1）", 1L, card.categoryId)
        assertEquals("收支类型应为支出", RecordType.EXPENSE, card.recordType)

        val dateTime = epochToLocalDateTime(card.timestamp)
        assertEquals("日期应为今天", LocalDateTime.now().toLocalDate(), dateTime.toLocalDate())
        assertEquals("时间应为中午 12:00", 12, dateTime.hour)
        assertEquals(0, dateTime.minute)
    }

    @Test
    fun `昨天打车45 yields transport category and 45 yuan`() {
        val result = MockAiService.parse("昨天打车45")
        assertTrue("应为记账内容", result.isBookkeeping)
        assertNotNull("应生成卡片数据", result.cardData)

        val card = result.cardData!!
        assertEquals("金额应为 45 元（4500 分）", 4500L, card.amountCents)
        assertEquals("分类应为交通（id=2）", 2L, card.categoryId)

        val dateTime = epochToLocalDateTime(card.timestamp)
        val now = LocalDateTime.now()
        assertEquals("日期应为昨天", now.toLocalDate().minusDays(1), dateTime.toLocalDate())
    }

    @Test
    fun `今天天气真好 is not bookkeeping`() {
        val result = MockAiService.parse("今天天气真好")
        assertFalse("非记账内容", result.isBookkeeping)
        assertNotNull("应包含回复文本", result.replyText)
        assertTrue("回复不应为空", result.replyText.isNotBlank())
    }

    @Test
    fun `50 yields unclassified expense of 50 yuan`() {
        val result = MockAiService.parse("50")
        assertTrue("应为记账内容", result.isBookkeeping)
        assertNotNull("应生成卡片数据", result.cardData)

        val card = result.cardData!!
        assertEquals("金额应为 50 元（5000 分）", 5000L, card.amountCents)
        assertEquals("分类应为未分类（id=-1）", -1L, card.categoryId)
        assertEquals("收支类型应为支出", RecordType.EXPENSE, card.recordType)
    }

    @Test
    fun `请朋友喝奶茶32元 yields food category and 32 yuan`() {
        val result = MockAiService.parse("请朋友喝奶茶32元")
        assertTrue("应为记账内容", result.isBookkeeping)
        assertNotNull("应生成卡片数据", result.cardData)

        val card = result.cardData!!
        assertEquals("金额应为 32 元（3200 分）", 3200L, card.amountCents)
        assertEquals("分类应为餐饮（id=1）", 1L, card.categoryId)
    }

    private fun epochToLocalDateTime(epochSecond: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneId.systemDefault())
    }
}
