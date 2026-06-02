package com.agguy.moni.app.ui.aibookkeeping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agguy.moni.app.model.AiBookkeepingParseRequest
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

internal const val AI_ERROR_PREFIX = "__MONI_AI_ERROR__\n"

internal fun ChatMessage.isAiErrorMessage(): Boolean =
    messageType == MessageType.AI_TEXT && content.startsWith(AI_ERROR_PREFIX)

internal fun ChatMessage.displayContent(): String = content.removePrefix(AI_ERROR_PREFIX)

/**
 * AI 记账页面 ViewModel。
 *
 * @param chatRepository 聊天消息数据仓库
 * @param onCreateRecord 创建真实记账记录的统一入口，返回 true 表示创建成功
 */
class AiBookkeepingViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository,
    private val chatRetentionDays: Int,
    private val onCreateRecord: suspend (CoreIntent.RecordCreate) -> Boolean,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<AiSelectedImage>>(emptyList())
    val selectedImages: StateFlow<List<AiSelectedImage>> = _selectedImages.asStateFlow()

    private val _supportsVision = MutableStateFlow(false)
    val supportsVision: StateFlow<Boolean> = _supportsVision.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingOlder = MutableStateFlow(false)
    val isLoadingOlder: StateFlow<Boolean> = _isLoadingOlder.asStateFlow()

    private val _hasOlderMessages = MutableStateFlow(false)
    val hasOlderMessages: StateFlow<Boolean> = _hasOlderMessages.asStateFlow()

    private val sessionId = "default_session"

    init {
        loadInitialMessages()
        loadDefaultPresetCapability()
    }

    fun loadMessages() {
        loadInitialMessages()
    }

    fun loadOlderMessages() {
        if (_isLoadingOlder.value || !_hasOlderMessages.value) return
        viewModelScope.launch {
            _isLoadingOlder.value = true
            try {
                val older = chatRepository.getBySession(
                    sessionId = sessionId,
                    limit = PAGE_SIZE,
                    offset = _messages.value.size,
                )
                _messages.value = (older + _messages.value).distinctBy { it.id }
                _hasOlderMessages.value = older.size == PAGE_SIZE
            } finally {
                _isLoadingOlder.value = false
            }
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun addImages(images: List<AiSelectedImage>) {
        if (!_supportsVision.value || images.isEmpty()) return
        _selectedImages.value = (_selectedImages.value + images).distinctBy { it.uri }
    }

    fun removeImage(image: AiSelectedImage) {
        _selectedImages.value = _selectedImages.value.filterNot { it.uri == image.uri }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val images = _selectedImages.value
        if ((text.isBlank() && images.isEmpty()) || _isLoading.value) return
        if (images.isNotEmpty() && !_supportsVision.value) {
            insertAiText("当前默认 AI 预设未开启识图能力，请在 AI 设置中手动开启后再发送图片。")
            return
        }

        viewModelScope.launch {
            _inputText.value = ""
            _selectedImages.value = emptyList()
            sendRequest(text = text, images = images)
        }
    }

    fun retryMessage(messageId: Long) {
        val messages = _messages.value
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0 || _isLoading.value) return
        val userMessage = messages
            .take(targetIndex)
            .lastOrNull { it.messageType == MessageType.USER_TEXT }
            ?: return
        val retryText = userMessage.content
            .lineSequence()
            .filterNot { it.startsWith("已附加 ") }
            .joinToString("\n")
            .trim()
        if (retryText.isBlank()) {
            insertAiError("图片消息无法自动重试，请重新选择图片后发送。")
            return
        }
        viewModelScope.launch {
            sendRequest(text = retryText, images = emptyList())
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            runCatching { chatRepository.delete(messageId) }
            refreshMessagesPreservingWindow()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearSession(sessionId)
            _messages.value = emptyList()
            _hasOlderMessages.value = false
        }
    }

    fun updateCardData(messageId: Long, cardData: DraftCardData) {
        viewModelScope.launch {
            chatRepository.updateCardData(messageId, cardData)
            refreshMessagesPreservingWindow()
        }
    }

    /**
     * 保存卡片：创建真实记账记录，并将消息状态更新为 SAVED。
     */
    fun saveCard(messageId: Long) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            val cardData = message?.cardData ?: return@launch
            if (cardData.categoryId <= 0) {
                chatRepository.insert(
                    ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = "请先为这张卡片选择分类，再确认入账。",
                        createdAt = nowSeconds()
                    )
                )
                refreshMessagesPreservingWindow()
                return@launch
            }

            val created = onCreateRecord(
                CoreIntent.RecordCreate(
                    amountCents = cardData.amountCents,
                    recordType = cardData.recordType,
                    categoryId = cardData.categoryId,
                    note = cardData.note,
                    timestamp = if (cardData.timestamp > 0) cardData.timestamp else null
                )
            )

            if (created) {
                chatRepository.updateStatus(messageId, CardStatus.SAVED)
                refreshMessagesPreservingWindow()
            } else {
                chatRepository.insert(
                    ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = AI_ERROR_PREFIX + "入账失败，请检查分类是否可用后重试。",
                        createdAt = nowSeconds()
                    )
                )
                refreshMessagesPreservingWindow()
            }
        }
    }

    /**
     * 取消卡片：从仓库删除该消息。
     */
    fun cancelCard(messageId: Long) {
        deleteMessage(messageId)
    }

    private fun loadInitialMessages() {
        viewModelScope.launch {
            cleanupExpiredMessages()
            val latest = chatRepository.getBySession(sessionId, PAGE_SIZE, 0)
            _messages.value = latest
            _hasOlderMessages.value = latest.size == PAGE_SIZE
        }
    }

    private suspend fun cleanupExpiredMessages() {
        if (chatRetentionDays <= 0) return
        val before = nowSeconds() - chatRetentionDays.toLong() * 86_400L
        chatRepository.deleteOlderThan(sessionId, before)
    }

    private suspend fun sendRequest(text: String, images: List<AiSelectedImage>) {
        val sentAt = nowSeconds()
        val userContent = buildUserMessageContent(text, images.size)
        chatRepository.insert(
            ChatMessage(
                sessionId = sessionId,
                messageType = MessageType.USER_TEXT,
                content = userContent,
                createdAt = sentAt
            )
        )
        refreshMessagesPreservingWindow()

        _isLoading.value = true
        delay(300.milliseconds)

        try {
            val parseResult = aiRepository.parseBookkeeping(
                AiBookkeepingParseRequest(
                    text = text,
                    images = images.map { it.toInput() },
                    sentAt = sentAt,
                )
            )

            if (parseResult.isBookkeeping) {
                val cardData = parseResult.cardData
                    ?: DraftCardData(
                        amountCents = 0L,
                        recordType = com.agguy.moni.core.RecordType.EXPENSE,
                        categoryId = -1L,
                        note = text
                    )
                chatRepository.insert(
                    ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_CARD,
                        content = "",
                        cardData = cardData,
                        cardStatus = CardStatus.DRAFT,
                        createdAt = nowSeconds()
                    )
                )
            } else {
                chatRepository.insert(
                    ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_TEXT,
                        content = parseResult.replyText,
                        createdAt = nowSeconds()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AiBookkeepingVM", "AI 处理失败", e)
            chatRepository.insert(
                ChatMessage(
                    sessionId = sessionId,
                    messageType = MessageType.AI_TEXT,
                    content = buildAiErrorMessage(e),
                    createdAt = nowSeconds()
                )
            )
        } finally {
            refreshMessagesPreservingWindow()
            _isLoading.value = false
        }
    }

    private fun loadDefaultPresetCapability() {
        viewModelScope.launch {
            runCatching { aiRepository.getDefaultPreset() }
                .onSuccess { preset -> _supportsVision.value = preset?.supportsVision == true }
                .onFailure { error ->
                    android.util.Log.w("AiBookkeepingVM", "读取默认 AI 预设失败", error)
                    _supportsVision.value = false
                }
        }
    }

    private fun insertAiText(content: String) {
        viewModelScope.launch {
            chatRepository.insert(
                ChatMessage(
                    sessionId = sessionId,
                    messageType = MessageType.AI_TEXT,
                    content = content,
                    createdAt = nowSeconds()
                )
            )
            refreshMessagesPreservingWindow()
        }
    }

    private fun insertAiError(content: String) {
        viewModelScope.launch {
            chatRepository.insert(
                ChatMessage(
                    sessionId = sessionId,
                    messageType = MessageType.AI_TEXT,
                    content = AI_ERROR_PREFIX + content,
                    createdAt = nowSeconds()
                )
            )
            refreshMessagesPreservingWindow()
        }
    }

    private suspend fun refreshMessagesPreservingWindow() {
        val limit = maxOf(PAGE_SIZE, _messages.value.size + 2)
        val rows = chatRepository.getBySession(sessionId, limit, 0)
        _messages.value = rows
        _hasOlderMessages.value = rows.size == limit
    }

    private fun buildUserMessageContent(text: String, imageCount: Int): String {
        val parts = buildList {
            if (text.isNotBlank()) add(text)
            if (imageCount > 0) add("已附加 $imageCount 张图片")
        }
        return parts.joinToString(separator = "\n")
    }

    private fun buildAiErrorMessage(error: Exception): String {
        val message = error.message.orEmpty()
        val friendlyMessage = when {
            message.contains("未配置默认 AI provider") -> "请先在 设置 > AI 设置 中配置默认 AI 预设。"
            message.contains("API key 为空") || message.contains("API Key") -> "请检查默认 AI 预设的 API Key。"
            message.contains("不支持图片识别") -> "当前默认 AI 预设不支持识图，请在 AI 设置中手动开启或移除图片。"
            message.contains("图片输入无效") -> "图片不符合要求，请减少数量或重新选择图片。"
            message.contains("认证失败") -> "AI Provider 认证失败，请检查 API Key。"
            message.contains("限流") -> "AI Provider 请求被限流，请稍后重试。"
            message.contains("响应 JSON") || message.contains("记账结构") -> "AI 返回内容不符合结构化 JSON 要求，请重试或更换模型。"
            else -> "AI 处理出错，请查看下面的详细信息。"
        }
        return "$AI_ERROR_PREFIX$friendlyMessage\n\n详情：${buildSanitizedErrorDetail(error)}"
    }

    private fun buildSanitizedErrorDetail(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }
            .map { throwable ->
                val type = throwable::class.java.simpleName
                val message = throwable.message.orEmpty().ifBlank { "无错误消息" }
                "$type: $message"
            }
            .joinToString(separator = "\ncaused by: ")
        return redactSensitiveText(chain).take(1200)
    }

    private fun redactSensitiveText(text: String): String = text
        .replace(Regex("(?i)(authorization\\s*[:=]\\s*bearer\\s+)[^\\s,}]+"), "$1[REDACTED]")
        .replace(Regex("(?i)(x-goog-api-key\\s*[:=]\\s*)[^\\s,}]+"), "$1[REDACTED]")
        .replace(Regex("sk-[A-Za-z0-9_-]{12,}"), "sk-[REDACTED]")

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        const val PAGE_SIZE = 30
    }
}
