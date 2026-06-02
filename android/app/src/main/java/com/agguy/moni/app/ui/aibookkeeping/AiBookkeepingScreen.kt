@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.aibookkeeping

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.NumPadSettings
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.app.ui.aibookkeeping.components.AiMessageContainer
import com.agguy.moni.app.ui.aibookkeeping.components.SmartDraftCard
import com.agguy.moni.app.ui.aibookkeeping.components.UserMessageBubble
import com.agguy.moni.core.CoreCategory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TIME_SEPARATOR_INTERVAL_SECONDS = 10 * 60L

/** AI 记账页面。 */
@Composable
fun AiBookkeepingScreen(
    viewModel: AiBookkeepingViewModel,
    categories: List<CoreCategory>,
    onNavigateBack: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    numPadSettings: NumPadSettings = NumPadSettings(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val supportsVision by viewModel.supportsVision.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingOlder by viewModel.isLoadingOlder.collectAsState()
    val hasOlderMessages by viewModel.hasOlderMessages.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val images = uris.mapNotNull { uri ->
            runCatching { AiImagePayloadReader.read(context, uri) }.getOrNull()
        }
        viewModel.addImages(images)
    }

    val timelineItems = buildTimelineItems(messages)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("记账助手", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(name = "arrow_back", contentDescription = "返回", size = 24.dp)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearChat, enabled = messages.isNotEmpty()) {
                        SymbolIcon(name = "delete_sweep", contentDescription = "清空聊天", size = 24.dp)
                    }
                    IconButton(onClick = onNavigateToAiSettings) {
                        SymbolIcon(name = "settings", contentDescription = "AI 设置", size = 24.dp)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                selectedImages = selectedImages,
                supportsVision = supportsVision,
                isLoading = isLoading,
                onInputChange = viewModel::onInputChange,
                onAddImage = { imagePicker.launch("image/*") },
                onRemoveImage = viewModel::removeImage,
                onSend = viewModel::sendMessage
            )
        }
    ) { innerPadding ->
        when {
            messages.isEmpty() && isLoading -> LoadingCenter(modifier = Modifier.padding(innerPadding))
            messages.isEmpty() -> EmptyState(modifier = Modifier.padding(innerPadding))
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasOlderMessages) {
                        item(key = "load_more") {
                            LoadOlderRow(
                                isLoading = isLoadingOlder,
                                onClick = viewModel::loadOlderMessages,
                            )
                        }
                    }
                    items(
                        items = timelineItems,
                        key = { item -> item.key }
                    ) { item ->
                        when (item) {
                            is ChatTimelineItem.TimeSeparator -> TimeSeparator(timestamp = item.timestamp)
                            is ChatTimelineItem.Message -> ChatMessageItem(
                                message = item.message,
                                categories = categories,
                                onCardDataChange = viewModel::updateCardData,
                                onSaveCard = viewModel::saveCard,
                                onCancelCard = viewModel::cancelCard,
                                onDeleteMessage = viewModel::deleteMessage,
                                onRetryMessage = viewModel::retryMessage,
                                numPadSettings = numPadSettings
                            )
                        }
                    }
                    if (isLoading) {
                        item(key = "loading") { LoadingIndicator() }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "开始记账吧，比如：中午吃麦当劳花了35，或发送发票图片",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun LoadingCenter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LoadOlderRow(isLoading: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick, enabled = !isLoading) {
            Text(if (isLoading) "加载中..." else "加载更早消息")
        }
    }
}

@Composable
private fun TimeSeparator(timestamp: Long) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        ) {
            Text(
                text = formatSeparatorTime(timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "思考中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    categories: List<CoreCategory>,
    onCardDataChange: (Long, com.agguy.moni.app.model.DraftCardData) -> Unit,
    onSaveCard: (Long) -> Unit,
    onCancelCard: (Long) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onRetryMessage: (Long) -> Unit,
    numPadSettings: NumPadSettings = NumPadSettings(),
    modifier: Modifier = Modifier
) {
    val isError = message.isAiErrorMessage()
    Box(modifier = modifier.fillMaxWidth()) {
        when (message.messageType) {
            MessageType.USER_TEXT -> {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    UserMessageBubble(content = message.content)
                    DeleteMessageButton(onClick = { onDeleteMessage(message.id) })
                }
            }

            MessageType.AI_CARD -> {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    AiMessageContainer {
                        val cardData = message.cardData
                        if (cardData != null) {
                            SmartDraftCard(
                                cardData = cardData,
                                cardStatus = message.cardStatus
                                    ?: com.agguy.moni.app.model.CardStatus.DRAFT,
                                onCardDataChange = { updated -> onCardDataChange(message.id, updated) },
                                categories = categories,
                                onSaveClick = { onSaveCard(message.id) },
                                onCancelClick = { onCancelCard(message.id) },
                                numPadSettings = numPadSettings
                            )
                        } else {
                            Text(
                                text = "暂无卡片数据",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    DeleteMessageButton(onClick = { onDeleteMessage(message.id) })
                }
            }

            MessageType.AI_TEXT -> {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    if (isError) {
                        AiErrorMessageContainer(
                            text = message.displayContent(),
                            onRetry = { onRetryMessage(message.id) },
                            onDelete = { onDeleteMessage(message.id) },
                        )
                    } else {
                        AiMessageContainer(text = message.displayContent())
                        DeleteMessageButton(onClick = { onDeleteMessage(message.id) })
                    }
                }
            }

            MessageType.SYSTEM -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteMessageButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 8.dp)) {
        Text("删除", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AiErrorMessageContainer(text: String, onRetry: () -> Unit, onDelete: () -> Unit) {
    AiMessageContainer {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "处理失败",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = text, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onRetry) { Text("重试") }
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    selectedImages: List<AiSelectedImage>,
    supportsVision: Boolean,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: (AiSelectedImage) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedImages.forEachIndexed { index, image ->
                        AssistChip(
                            onClick = { onRemoveImage(image) },
                            label = { Text("图片 ${index + 1}") },
                            leadingIcon = {
                                SymbolIcon(name = "image", contentDescription = null, size = 18.dp)
                            },
                            trailingIcon = {
                                SymbolIcon(name = "close", contentDescription = "移除图片", size = 18.dp)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (supportsVision) {
                    IconButton(onClick = onAddImage, enabled = !isLoading) {
                        SymbolIcon(
                            name = "add_photo_alternate",
                            contentDescription = "添加图片",
                            size = 24.dp
                        )
                    }
                }

                TextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("描述你的记账内容...") },
                    enabled = !isLoading,
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp)
                )
                OutlinedButton(
                    onClick = onSend,
                    enabled = (inputText.isNotBlank() || selectedImages.isNotEmpty()) && !isLoading,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(if (isLoading) "..." else "发送")
                }
            }
        }
    }
}

private sealed class ChatTimelineItem {
    abstract val key: String

    data class TimeSeparator(val timestamp: Long) : ChatTimelineItem() {
        override val key: String = "time_$timestamp"
    }

    data class Message(val message: ChatMessage) : ChatTimelineItem() {
        override val key: String = "message_${message.id}"
    }
}

private fun buildTimelineItems(messages: List<ChatMessage>): List<ChatTimelineItem> {
    val items = mutableListOf<ChatTimelineItem>()
    var previousTimestamp: Long? = null
    messages.forEach { message ->
        val previous = previousTimestamp
        val shouldShowTime = previous == null ||
            message.createdAt - previous >= TIME_SEPARATOR_INTERVAL_SECONDS ||
            !isSameLocalDay(previous, message.createdAt)
        if (shouldShowTime) {
            items += ChatTimelineItem.TimeSeparator(message.createdAt)
        }
        items += ChatTimelineItem.Message(message)
        previousTimestamp = message.createdAt
    }
    return items
}

private fun isSameLocalDay(first: Long, second: Long): Boolean {
    val zone = ZoneId.systemDefault()
    val firstDate = Instant.ofEpochSecond(first).atZone(zone).toLocalDate()
    val secondDate = Instant.ofEpochSecond(second).atZone(zone).toLocalDate()
    return firstDate == secondDate
}

private fun formatSeparatorTime(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zone)
    val date = dateTime.toLocalDate()
    val today = LocalDate.now(zone)
    val dateText = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
    return "$dateText ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}
