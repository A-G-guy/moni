package com.agguy.moni.app.ui.aibookkeeping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.app.repository.ChatRepository
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.RustCoreController
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
 * @param rustCore Rust 核心控制器，用于创建真实记账记录
 */
class AiBookkeepingViewModel(
    private val chatRepository: ChatRepository,
    private val rustCore: RustCoreController,
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

    /**
     * 发送用户消息并触发模拟 AI 响应流程。
     * 当前使用模拟数据，未来接入真实 AI 解析服务。
     */
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

            val aiCardMessage = ChatMessage(
                sessionId = sessionId,
                messageType = MessageType.AI_CARD,
                content = "",
                cardData = DraftCardData(
                    amountCents = 2500L,
                    recordType = RecordType.EXPENSE,
                    note = "模拟记账"
                ),
                cardStatus = CardStatus.DRAFT,
                createdAt = System.currentTimeMillis() / 1000
            )
            chatRepository.insert(aiCardMessage)

            val aiTextMessage = ChatMessage(
                sessionId = sessionId,
                messageType = MessageType.AI_TEXT,
                content = "已为你识别到一笔支出，金额 25.00 元。请确认或修改。",
                createdAt = System.currentTimeMillis() / 1000
            )
            chatRepository.insert(aiTextMessage)

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

            rustCore.dispatch(
                CoreIntent.RecordCreate(
                    amountCents = cardData.amountCents,
                    recordType = cardData.recordType,
                    categoryId = if (cardData.categoryId > 0) cardData.categoryId else 1L,
                    note = cardData.note,
                    timestamp = if (cardData.timestamp > 0) cardData.timestamp else null
                )
            )

            chatRepository.updateStatus(messageId, CardStatus.SAVED)
            refreshMessages()
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
}
