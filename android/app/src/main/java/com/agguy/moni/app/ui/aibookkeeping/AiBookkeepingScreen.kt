@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.NumPadSettings
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.model.ChatMessage
import com.agguy.moni.app.model.MessageType
import com.agguy.moni.app.ui.aibookkeeping.components.AiMessageContainer
import com.agguy.moni.app.ui.aibookkeeping.components.SmartDraftCard
import com.agguy.moni.app.ui.aibookkeeping.components.UserMessageBubble
import com.agguy.moni.core.CoreCategory

/**
 * AI 记账页面。
 *
 * @param viewModel AI 记账 ViewModel
 * @param categories 当前可用分类，用于草稿卡片编辑
 * @param onNavigateBack 返回上一页回调
 * @param onNavigateToRecordList 跳转到账单列表回调
 */
@Composable
fun AiBookkeepingScreen(
    viewModel: AiBookkeepingViewModel,
    categories: List<CoreCategory>,
    onNavigateBack: () -> Unit,
    onNavigateToRecordList: () -> Unit,
    numPadSettings: NumPadSettings = NumPadSettings(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()
    val supportsVision by viewModel.supportsVision.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val images = uris.mapNotNull { uri ->
            runCatching { AiImagePayloadReader.read(context, uri) }.getOrNull()
        }
        viewModel.addImages(images)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "记账助手",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = "arrow_back",
                            contentDescription = "返回",
                            size = 24.dp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRecordList) {
                        SymbolIcon(
                            name = "receipt",
                            contentDescription = "账单历史",
                            size = 24.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            messages.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            messages.isEmpty() -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageItem(
                            message = message,
                            categories = categories,
                            onCardDataChange = viewModel::updateCardData,
                            onSaveCard = viewModel::saveCard,
                            onCancelCard = viewModel::cancelCard,
                            numPadSettings = numPadSettings
                        )
                    }

                    if (isLoading) {
                        item {
                            LoadingIndicator()
                        }
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
    numPadSettings: NumPadSettings = NumPadSettings(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        when (message.messageType) {
            MessageType.USER_TEXT -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    UserMessageBubble(content = message.content)
                }
            }

            MessageType.AI_CARD -> {
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
            }

            MessageType.AI_TEXT -> {
                AiMessageContainer(text = message.content)
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
                                SymbolIcon(
                                    name = "image",
                                    contentDescription = null,
                                    size = 18.dp
                                )
                            },
                            trailingIcon = {
                                SymbolIcon(
                                    name = "close",
                                    contentDescription = "移除图片",
                                    size = 18.dp
                                )
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
                    IconButton(
                        onClick = onAddImage,
                        enabled = !isLoading
                    ) {
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
