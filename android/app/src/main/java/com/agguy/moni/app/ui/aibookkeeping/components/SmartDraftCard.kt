package com.agguy.moni.app.ui.aibookkeeping.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agguy.moni.app.components.AutoResizeText
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.model.CardStatus
import com.agguy.moni.app.model.DraftCardData
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.app.ui.record.editor.CategoryGridPager
import com.agguy.moni.app.ui.record.editor.CustomNumPad
import com.agguy.moni.app.ui.record.editor.DatePickerBottomSheet
import com.agguy.moni.app.ui.record.editor.RecordEditorState
import com.agguy.moni.app.ui.record.editor.TimePickerBottomSheet
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.RecordType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class EditorSheet {
    NONE, AMOUNT, CATEGORY, ACCOUNT
}

@Composable
fun SmartDraftCard(
    cardData: DraftCardData,
    cardStatus: CardStatus = CardStatus.DRAFT,
    onCardDataChange: (DraftCardData) -> Unit = {},
    categories: List<CoreCategory> = emptyList(),
    accounts: List<String> = listOf("微信支付", "支付宝", "现金", "银行卡"),
    onSaveClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentSheet by remember { mutableStateOf(EditorSheet.NONE) }
    var isNoteEditing by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(cardData.note) }
    var tempCategoryId by remember { mutableLongStateOf(cardData.categoryId) }
    var categoryGridPage by remember { mutableIntStateOf(0) }
    var tempAccountIndex by remember { mutableIntStateOf(
        if (cardData.accountId >= 0 && cardData.accountId < accounts.size)
            cardData.accountId.toInt()
        else 0
    ) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempTimestamp by remember { mutableLongStateOf(cardData.timestamp) }

    val amountEditorState = rememberSaveable(saver = RecordEditorState.Saver) {
        RecordEditorState(null)
    }

    LaunchedEffect(currentSheet == EditorSheet.AMOUNT, cardData.amountCents, cardData.categoryId) {
        if (currentSheet == EditorSheet.AMOUNT) {
            val tempRecord = CoreRecord(
                id = 0,
                amountCents = cardData.amountCents,
                recordType = cardData.recordType.name,
                categoryId = cardData.categoryId,
                parentCategoryId = null,
                categoryName = categories.find { it.id == cardData.categoryId }?.name ?: "",
                note = cardData.note,
                createdAt = cardData.timestamp
            )
            amountEditorState.loadFromRecord(tempRecord, categories)
        }
    }

    LaunchedEffect(cardData.note) {
        noteText = cardData.note
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    val breathingAnimatable = remember { androidx.compose.animation.core.Animatable(0.45f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            breathingAnimatable.animateTo(
                targetValue = 0.6f,
                animationSpec = tween(1000, easing = LinearEasing)
            )
            breathingAnimatable.animateTo(
                targetValue = 0.3f,
                animationSpec = tween(1000, easing = LinearEasing)
            )
        }
    }
    val breathingAlpha by breathingAnimatable.asState()

    val targetBaseAlpha = when (cardStatus) {
        CardStatus.DRAFT -> 0.45f
        CardStatus.SAVED -> 0.3f
        else -> 0f
    }
    val animatedBaseColor by animateColorAsState(
        targetValue = primaryColor.copy(alpha = targetBaseAlpha),
        animationSpec = tween(300),
        label = "borderBaseColor"
    )

    val borderColor = if (cardStatus == CardStatus.DRAFT) {
        primaryColor.copy(alpha = breathingAlpha)
    } else {
        animatedBaseColor
    }

    val stampAlpha by animateFloatAsState(
        targetValue = if (cardStatus == CardStatus.SAVED) 1f else 0f,
        animationSpec = tween(300),
        label = "stampAlpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeaderRow(
                    cardData = cardData,
                    categories = categories,
                    onCategoryClick = { currentSheet = EditorSheet.CATEGORY }
                )

                InfoColumn(
                    cardData = cardData,
                    categories = categories,
                    accounts = accounts,
                    isNoteEditing = isNoteEditing,
                    noteText = noteText,
                    onNoteTextChange = { noteText = it },
                    onNoteEditStart = { isNoteEditing = true },
                    onNoteEditConfirm = {
                        onCardDataChange(cardData.copy(note = noteText))
                        isNoteEditing = false
                    },
                    onAmountClick = { currentSheet = EditorSheet.AMOUNT },
                    onAccountClick = { currentSheet = EditorSheet.ACCOUNT },
                    onTimeClick = {
                        tempTimestamp = cardData.timestamp
                        showDatePicker = true
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = cardStatus == CardStatus.DRAFT,
                    enter = fadeIn(animationSpec = tween(300)) +
                        expandVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) +
                        shrinkVertically(animationSpec = tween(300))
                ) {
                    ActionRow(
                        onCancelClick = onCancelClick,
                        onSaveClick = onSaveClick
                    )
                }
            }

            if (stampAlpha > 0f) {
                SavedStamp(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .alpha(stampAlpha)
                )
            }
        }
    }

    if (currentSheet == EditorSheet.AMOUNT) {
        AmountEditorSheet(
            editorState = amountEditorState,
            onConfirm = {
                onCardDataChange(cardData.copy(amountCents = amountEditorState.confirmedAmountCents))
                currentSheet = EditorSheet.NONE
            },
            onDismiss = { currentSheet = EditorSheet.NONE }
        )
    }

    if (currentSheet == EditorSheet.CATEGORY) {
        CategoryEditorSheet(
            categories = categories,
            selectedCategoryId = tempCategoryId,
            currentGridPage = categoryGridPage,
            onCategorySelected = { tempCategoryId = it },
            onGridPageChanged = { categoryGridPage = it },
            onConfirm = {
                onCardDataChange(cardData.copy(categoryId = tempCategoryId))
                currentSheet = EditorSheet.NONE
            },
            onDismiss = {
                tempCategoryId = cardData.categoryId
                currentSheet = EditorSheet.NONE
            }
        )
    }

    if (currentSheet == EditorSheet.ACCOUNT) {
        AccountEditorSheet(
            accounts = accounts,
            selectedIndex = tempAccountIndex,
            onAccountSelected = { tempAccountIndex = it },
            onConfirm = {
                onCardDataChange(cardData.copy(accountId = tempAccountIndex.toLong()))
                currentSheet = EditorSheet.NONE
            },
            onDismiss = {
                tempAccountIndex = if (cardData.accountId >= 0 && cardData.accountId < accounts.size)
                    cardData.accountId.toInt() else 0
                currentSheet = EditorSheet.NONE
            }
        )
    }

    if (showDatePicker) {
        DatePickerBottomSheet(
            selectedTimestamp = tempTimestamp,
            onDateSelected = { dateSeconds ->
                tempTimestamp = dateSeconds
                showDatePicker = false
                showTimePicker = true
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TimePickerBottomSheet(
            selectedTimestamp = tempTimestamp,
            onTimeSelected = { hour, minute ->
                val selectedDate = Instant.ofEpochSecond(tempTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val newDateTime = selectedDate.atTime(hour, minute)
                val newTimestamp = newDateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
                onCardDataChange(cardData.copy(timestamp = newTimestamp))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun HeaderRow(
    cardData: DraftCardData,
    categories: List<CoreCategory>,
    onCategoryClick: () -> Unit
) {
    val isExpense = cardData.recordType == RecordType.EXPENSE
    val typeLabel = if (isExpense) "支出" else "收入"
    val typeColor = if (isExpense) {
        MaterialTheme.colorScheme.expenseRed
    } else {
        MaterialTheme.colorScheme.incomeGreen
    }

    val selectedCategory = categories.find { it.id == cardData.categoryId }
    val categoryName = selectedCategory?.name ?: "未分类"
    val categoryIcon = selectedCategory?.iconName ?: "help_outline"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { onCategoryClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SymbolIcon(
                name = categoryIcon,
                contentDescription = "分类图标",
                size = 20.dp
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = typeColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = typeLabel,
                color = typeColor,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoColumn(
    cardData: DraftCardData,
    categories: List<CoreCategory>,
    accounts: List<String>,
    isNoteEditing: Boolean,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    onNoteEditStart: () -> Unit,
    onNoteEditConfirm: () -> Unit,
    onAmountClick: () -> Unit,
    onAccountClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val amountText = "%.2f".format(cardData.amountCents / 100.0)
    val amountColor = if (cardData.recordType == RecordType.EXPENSE) {
        MaterialTheme.colorScheme.expenseRed
    } else {
        MaterialTheme.colorScheme.incomeGreen
    }

    val accountName = if (cardData.accountId >= 0 && cardData.accountId < accounts.size) {
        accounts[cardData.accountId.toInt()]
    } else {
        accounts.firstOrNull() ?: "未指定"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAmountClick() },
            contentAlignment = Alignment.Center
        ) {
            AutoResizeText(
                text = amountText,
                maxFontSize = 48.sp,
                minFontSize = 24.sp,
                style = MaterialTheme.typography.displayMedium,
                color = amountColor
            )
        }

        InfoRowItem(
            iconName = "account_balance_wallet",
            iconDescription = "账户图标",
            text = accountName,
            onClick = onAccountClick
        )

        InfoRowItem(
            iconName = "schedule",
            iconDescription = "时间图标",
            text = formatTimestamp(cardData.timestamp),
            onClick = onTimeClick
        )

        if (isNoteEditing) {
            TextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("添加备注...") },
                trailingIcon = {
                    IconButton(onClick = onNoteEditConfirm) {
                        SymbolIcon(
                            name = "check",
                            contentDescription = "确认备注",
                            size = 20.dp
                        )
                    }
                }
            )
        } else if (cardData.note.isNotEmpty()) {
            InfoRowItem(
                iconName = "edit_note",
                iconDescription = "备注图标",
                text = cardData.note,
                onClick = onNoteEditStart
            )
        }
    }
}

@Composable
private fun InfoRowItem(
    iconName: String,
    iconDescription: String,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SymbolIcon(
            name = iconName,
            contentDescription = iconDescription,
            size = 16.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionRow(
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onCancelClick
        ) {
            Text(
                text = "取消",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onSaveClick
        ) {
            Text(text = "确认保存")
        }
    }
}

@Composable
private fun SavedStamp(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = primaryColor.copy(alpha = 0.08f),
            modifier = Modifier.size(56.dp)
        ) {}

        Text(
            text = "已入账",
            style = MaterialTheme.typography.labelLarge,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.rotate(-15f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountEditorSheet(
    editorState: RecordEditorState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = editorState.displayText,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomNumPad(
                recordType = editorState.recordType,
                amountExpression = editorState.amountExpression,
                amountCents = editorState.confirmedAmountCents,
                hasSelectedCategory = true,
                onDigitClick = { editorState.appendDigit(it) },
                onOperatorClick = { editorState.appendOperator(it) },
                onBackspace = { editorState.backspace() },
                onCalculate = { editorState.calculate() },
                onSave = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("取消")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorSheet(
    categories: List<CoreCategory>,
    selectedCategoryId: Long,
    currentGridPage: Int,
    onCategorySelected: (Long) -> Unit,
    onGridPageChanged: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            CategoryGridPager(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                currentGridPage = currentGridPage,
                onCategorySelected = onCategorySelected,
                onGridPageChanged = onGridPageChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(onClick = onConfirm) {
                    Text("确认")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditorSheet(
    accounts: List<String>,
    selectedIndex: Int,
    onAccountSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择账户",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                accounts.forEachIndexed { index, account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(index) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onAccountSelected(index) }
                        )
                        Text(
                            text = account,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(onClick = onConfirm) {
                    Text("确认")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "今天 12:00"

    val zone = ZoneId.systemDefault()
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zone)
    val localDate = dateTime.toLocalDate()
    val today = LocalDate.now()

    val datePrefix = when {
        localDate == today -> "今天"
        localDate == today.minusDays(1) -> "昨天"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MM-dd"))
    }
    val timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return "$datePrefix $timeStr"
}

@Preview(showBackground = true)
@Composable
private fun SmartDraftCardDraftPreview() {
    MaterialTheme {
        SmartDraftCard(
            cardData = DraftCardData(
                amountCents = 3500L,
                recordType = RecordType.EXPENSE,
                categoryId = 1L,
                accountId = 0L,
                timestamp = System.currentTimeMillis() / 1000,
                note = "麦当劳"
            ),
            cardStatus = CardStatus.DRAFT
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SmartDraftCardSavedPreview() {
    MaterialTheme {
        SmartDraftCard(
            cardData = DraftCardData(
                amountCents = 500000L,
                recordType = RecordType.INCOME,
                categoryId = 2L,
                accountId = 1L,
                timestamp = System.currentTimeMillis() / 1000 - 86400,
                note = ""
            ),
            cardStatus = CardStatus.SAVED
        )
    }
}
