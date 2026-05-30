package com.agguy.moni.app.ui.aibookkeeping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.app.repository.AiRepository
import com.agguy.moni.app.repository.ChatRepository
import com.agguy.moni.core.CoreIntent
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AI 记账页面 ViewModel。
 *
 * @param chatRepository 聊天消息数据仓库
 * @param onCreateRecord 创建真实记账记录的统一入口，返回 true 表示创建成功
 */
class AiBookkeepingViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository,
    private val onCreateRecord: suspend (CoreIntent.RecordCreate) -> Boolean,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sessionId = "default_session"

    init {
        loadMessages()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _messages.value = chatRepository.getBySession(sessionId)
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _isLoading.value) return

        viewModelScope.launch {
            val userMessage = ChatMessage(
                sessionId = sessionId,
                messageType = MessageType.USER_TEXT,
                content = text,
                createdAt = System.currentTimeMillis() / 1000
            )
            chatRepository.insert(userMessage)
            _inputText.value = ""
            refreshMessages()

            _isLoading.value = true
            delay(500.milliseconds)

            try {
                val parseResult = aiRepository.parseBookkeeping(text)

                if (parseResult.isBookkeeping) {
                    val cardData = parseResult.cardData
                        ?: DraftCardData(
                            amountCents = 0L,
                            recordType = com.agguy.moni.core.RecordType.EXPENSE,
                            categoryId = 1L,
                            note = text
                        )
                    val aiCardMessage = ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_CARD,
                        content = "",
                        cardData = cardData,
                        cardStatus = CardStatus.DRAFT,
                        createdAt = System.currentTimeMillis() / 1000
                    )
                    chatRepository.insert(aiCardMessage)

                    val aiTextMessage = ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = parseResult.replyText,
                        createdAt = System.currentTimeMillis() / 1000
                    )
                    chatRepository.insert(aiTextMessage)
                } else {
                    val aiTextMessage = ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = parseResult.replyText,
                        createdAt = System.currentTimeMillis() / 1000
                    )
                    chatRepository.insert(aiTextMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("AiBookkeepingVM", "AI 处理失败", e)
                val errorMessage = ChatMessage(
                    sessionId = sessionId,
                    messageType = MessageType.AI_TEXT,
                    content = buildAiErrorMessage(e),
                    createdAt = System.currentTimeMillis() / 1000
                )
                chatRepository.insert(errorMessage)
            }

            refreshMessages()
            _isLoading.value = false
        }
    }

    /**
     * 保存卡片：创建真实记账记录，并将消息状态更新为 SAVED。
     */
    fun saveCard(messageId: Long) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            val cardData = message?.cardData ?: return@launch

            val created = onCreateRecord(
                CoreIntent.RecordCreate(
                    amountCents = cardData.amountCents,
                    recordType = cardData.recordType,
                    categoryId = if (cardData.categoryId > 0) cardData.categoryId else 1L,
                    note = cardData.note,
                    timestamp = if (cardData.timestamp > 0) cardData.timestamp else null
                )
            )

            if (created) {
                chatRepository.updateStatus(messageId, CardStatus.SAVED)
                refreshMessages()
            } else {
                chatRepository.insert(
                    ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = "入账失败，请检查分类是否可用后重试。",
                        createdAt = System.currentTimeMillis() / 1000
                    )
                )
                refreshMessages()
            }
        }
    }

    /**
     * 取消卡片：从仓库删除该消息。
     */
    fun cancelCard(messageId: Long) {
        viewModelScope.launch {
            chatRepository.delete(messageId)
            refreshMessages()
        }
    }

    private suspend fun refreshMessages() {
        _messages.value = chatRepository.getBySession(sessionId)
    }

    private fun buildAiErrorMessage(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("未配置默认 AI provider") -> "请先在 设置 > AI 设置 中配置默认 AI 预设。"
            message.contains("认证失败") -> "AI Provider 认证失败，请检查 API Key。"
            message.contains("限流") -> "AI Provider 请求被限流，请稍后重试。"
            else -> "AI 处理出错，请稍后重试。"
        }
    }
}
