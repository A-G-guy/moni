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

    private val _selectedImages = MutableStateFlow<List<AiSelectedImage>>(emptyList())
    val selectedImages: StateFlow<List<AiSelectedImage>> = _selectedImages.asStateFlow()

    private val _supportsVision = MutableStateFlow(false)
    val supportsVision: StateFlow<Boolean> = _supportsVision.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val sessionId = "default_session"

    init {
        loadMessages()
        loadDefaultPresetCapability()
    }

    fun loadMessages() {
        viewModelScope.launch {
            _messages.value = chatRepository.getBySession(sessionId)
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
            val sentAt = System.currentTimeMillis() / 1000
            val userContent = buildUserMessageContent(text, images.size)
            val userMessage = ChatMessage(
                sessionId = sessionId,
                messageType = MessageType.USER_TEXT,
                content = userContent,
                createdAt = sentAt
            )
            chatRepository.insert(userMessage)
            _inputText.value = ""
            _selectedImages.value = emptyList()
            refreshMessages()

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
                    val aiCardMessage = ChatMessage(
                        sessionId = sessionId,
                        messageType = MessageType.AI_CARD,
                        content = "",
                        cardData = cardData,
                        cardStatus = CardStatus.DRAFT,
                        createdAt = System.currentTimeMillis() / 1000
                    )
                    chatRepository.insert(aiCardMessage)

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
            } finally {
                refreshMessages()
                _isLoading.value = false
            }
        }
    }

    fun updateCardData(messageId: Long, cardData: DraftCardData) {
        viewModelScope.launch {
            chatRepository.updateCardData(messageId, cardData)
            refreshMessages()
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
                        createdAt = System.currentTimeMillis() / 1000
                    )
                )
                refreshMessages()
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
                    createdAt = System.currentTimeMillis() / 1000
                )
            )
            refreshMessages()
        }
    }

    private suspend fun refreshMessages() {
        _messages.value = chatRepository.getBySession(sessionId)
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
        return "$friendlyMessage\n\n详情：${buildSanitizedErrorDetail(error)}"
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
}
