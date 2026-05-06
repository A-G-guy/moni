@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.agguy.moni.app.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.ui.record.editor.CategoryGridPager
import com.agguy.moni.app.ui.record.editor.DatePickerBottomSheet
import com.agguy.moni.app.ui.record.editor.RecordEditorPanel
import com.agguy.moni.app.ui.record.editor.RecordEditorState
import com.agguy.moni.app.ui.record.editor.TimePickerBottomSheet
import com.agguy.moni.app.ui.record.editor.rememberRecordEditorState
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName
import java.time.Instant
import java.time.ZoneId

/**
 * 记账详情屏（新增/编辑）。
 *
 * 全新一屏闭环设计：
 * - 顶部：收入/支出胶囊切换
 * - 中部：分类翻页网格
 * - 底部：综合输入面板
 *
 * 除备注外全程不依赖系统输入法，所有操作在一屏内完成。
 */
@Composable
fun RecordDetailScreen(
    appState: AppState,
    recordId: Long?,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existingRecord = remember(recordId, appState.records) {
        appState.records.find { it.id == recordId }
    }
    val isEditMode = existingRecord != null

    val state = rememberRecordEditorState(existingRecord, appState.categories)
    var showDateSheet by remember { mutableStateOf(false) }
    var showTimeSheet by remember { mutableStateOf(false) }

    // 根据当前类型过滤未归档分类
    val filteredCategories = remember(appState.categories, state.recordType) {
        appState.categories.filter {
            it.categoryType == state.recordType.serialName && it.archivedAt == null
        }
    }

    // 保存条件
    val isSaveEnabled = state.confirmedAmountCents > 0 && state.selectedCategoryId != -1L

    // 备注编辑模式下拦截返回键
    BackHandler(enabled = state.isNoteEditing) {
        state.endNoteEdit()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑记录" else "记一笔") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        MoniIcon(MoniIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                existingRecord.let {
                                    onDispatch(CoreIntent.RecordDelete(it.id))
                                    onNavigateBack()
                                }
                            }
                        ) {
                            MoniIcon(
                                MoniIcons.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.expenseRed
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 顶部：类型切换
            RecordTypeToggle(
                selectedType = state.recordType,
                onTypeSelected = { state.updateType(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            // 中部：分类网格
            CategoryGridPager(
                categories = filteredCategories,
                selectedCategoryId = state.selectedCategoryId,
                currentGridPage = state.currentGridPage,
                onCategorySelected = { state.selectCategory(it) },
                onGridPageChanged = { state.currentGridPage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            // 底部：综合控制面板
            RecordEditorPanel(
                state = state,
                currencySymbol = appState.currencySymbol,
                onDateClick = { showDateSheet = true },
                onTimeClick = { showTimeSheet = true },
                onNoteClick = { state.startNoteEdit() },
                onNoteDone = { state.endNoteEdit() },
                onDigitClick = { state.appendDigit(it) },
                onOperatorClick = { state.appendOperator(it) },
                onBackspace = { state.backspace() },
                onCalculate = { state.calculate() },
                onSave = {
                    if (isSaveEnabled) {
                        if (isEditMode) {
                            onDispatch(
                                CoreIntent.RecordUpdate(
                                    id = existingRecord.id,
                                    amountCents = state.confirmedAmountCents,
                                    recordType = state.recordType,
                                    categoryId = state.selectedCategoryId,
                                    note = state.note
                                )
                            )
                        } else {
                            onDispatch(
                                CoreIntent.RecordCreate(
                                    amountCents = state.confirmedAmountCents,
                                    recordType = state.recordType,
                                    categoryId = state.selectedCategoryId,
                                    note = state.note,
                                    timestamp = state.timestamp
                                )
                            )
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // 底部缓冲：吸收分类区缩小后释放的多余空间，保持键盘高度不变
            Spacer(modifier = Modifier.height(56.dp))
        }
    }

    // 日期选择 BottomSheet
    if (showDateSheet) {
        DatePickerBottomSheet(
            selectedTimestamp = state.timestamp,
            onDateSelected = { dateSeconds ->
                state.updateDate(dateSeconds)
            },
            onDismiss = { showDateSheet = false }
        )
    }

    // 时间选择 BottomSheet
    if (showTimeSheet) {
        TimePickerBottomSheet(
            selectedTimestamp = state.timestamp,
            onTimeSelected = { hour, minute ->
                state.updateTime(hour, minute)
            },
            onDismiss = { showTimeSheet = false }
        )
    }
}

/**
 * 收入/支出胶囊切换器。
 */
@Composable
private fun RecordTypeToggle(
    selectedType: RecordType,
    onTypeSelected: (RecordType) -> Unit,
    modifier: Modifier = Modifier
) {
    ButtonGroup(
        modifier = modifier.padding(horizontal = 8.dp),
        overflowIndicator = {}
    ) {
        toggleableItem(
            checked = selectedType == RecordType.EXPENSE,
            label = "支出",
            onCheckedChange = { if (it) onTypeSelected(RecordType.EXPENSE) },
            icon = {
                if (selectedType == RecordType.EXPENSE) {
                    MoniIcon(MoniIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
            weight = 1f
        )
        toggleableItem(
            checked = selectedType == RecordType.INCOME,
            label = "收入",
            onCheckedChange = { if (it) onTypeSelected(RecordType.INCOME) },
            icon = {
                if (selectedType == RecordType.INCOME) {
                    MoniIcon(MoniIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            },
            weight = 1f
        )
    }
}
