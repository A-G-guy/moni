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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.SymbolIcon
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 记账详情屏（新增/编辑）。
 *
 * 布局：
 * - 顶部：收入/支出胶囊切换
 * - 中部：分类垂直滚动网格（固定高度）
 * - 弹性空白：空间过多时在此处填充
 * - 底部：综合输入面板（固定高度，紧贴底部）
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

    // 缓存最近一次有效的预算检查结果，避免 BudgetCheck 往返延迟导致的闪烁。
    // 仅在切换分类或切到收入时清空；金额变化期间保持旧值显示直到新结果到达。
    var displayedCheckResult by remember { mutableStateOf<com.agguy.moni.core.CoreBudgetCheckResult?>(null) }

    LaunchedEffect(state.selectedCategoryId, state.recordType) {
        if (state.recordType != RecordType.EXPENSE || state.selectedCategoryId == -1L) {
            displayedCheckResult = null
        }
    }

    LaunchedEffect(appState.budgetCheckResult) {
        appState.budgetCheckResult?.let { result ->
            if (result.categoryId == state.selectedCategoryId) {
                displayedCheckResult = result
            }
        }
    }

    // 监听金额/分类变化，触发预算实时检查。
    // 首次延迟 50ms（几乎立即），后续延迟 150ms（避免快速输入时过度触发）。
    LaunchedEffect(state.confirmedAmountCents, state.selectedCategoryId, state.recordType) {
        if (state.recordType == RecordType.EXPENSE &&
            state.confirmedAmountCents > 0 &&
            state.selectedCategoryId != -1L
        ) {
            val delayMs = if (displayedCheckResult == null) 50L else 150L
            kotlinx.coroutines.delay(delayMs)
            val yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            onDispatch(
                CoreIntent.BudgetCheck(
                    categoryId = state.selectedCategoryId,
                    yearMonth = yearMonth,
                    amountCents = state.confirmedAmountCents
                )
            )
        }
    }

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
                        SymbolIcon(name = "arrow_back", contentDescription = "返回", size = 24.dp)
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                // 乐观删除：立即导航，后台异步执行删除；若失败用户可在列表页看到错误
                                existingRecord.let {
                                    onDispatch(CoreIntent.RecordDelete(it.id))
                                }
                                onNavigateBack()
                            }
                        ) {
                            SymbolIcon(
                                name = "delete",
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.expenseRed,
                                size = 24.dp
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .navigationBarsPadding(),
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

            // 中部：分类网格（占满剩余空间，内部垂直滚动，预算条在滚动区内）
            CategoryGridPager(
                categories = filteredCategories,
                selectedCategoryId = state.selectedCategoryId,
                currentGridPage = state.currentGridPage,
                budgetCheckResult = displayedCheckResult,
                currencySymbol = appState.currencySymbol,
                onCategorySelected = { state.selectCategory(it) },
                onGridPageChanged = { state.currentGridPage = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 4.dp)
            )

            // 底部：综合控制面板（固定高度区域，包含金额/信息行/键盘）
            RecordEditorPanel(
                state = state,
                currencySymbol = appState.currencySymbol,
                budgetCheckResult = displayedCheckResult,
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
                modifier = Modifier.fillMaxWidth()
            )
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
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        overflowIndicator = {}
    ) {
        toggleableItem(
            checked = selectedType == RecordType.EXPENSE,
            label = "支出",
            onCheckedChange = { if (it) onTypeSelected(RecordType.EXPENSE) },
            icon = {
                if (selectedType == RecordType.EXPENSE) {
                    SymbolIcon(name = "check", contentDescription = null, size = 18.dp)
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
                    SymbolIcon(name = "check", contentDescription = null, size = 18.dp)
                }
            },
            weight = 1f
        )
    }
}
