@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.components.AmountInput
import com.agguy.moni.app.components.DatePickerField
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.CoreCategory
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.RecordType
import com.agguy.moni.core.serialName
import java.time.LocalDate
import java.time.ZoneId

/**
 * 记账详情屏（新增/编辑）。
 *
 * Material 3 Expressive 改造点：
 * - 收入/支出切换：[androidx.compose.material3.SegmentedButton] 替换为 [ButtonGroup] + [ToggleButton]，
 *   按下时邻居 squish 形变，是 Expressive 招牌交互；
 * - 保存按钮升级到 Large size（56dp）：用 [ButtonDefaults.LargeContentPadding] + [ButtonDefaults.LargeContainerHeight]；
 * - 备注 [OutlinedTextField] 加 medium 圆角，与新的 corner token 体系协同；
 * - 删除确认 [AlertDialog] 改用 [androidx.compose.material3.Shapes.extraLarge] (32dp) 圆角并接入 motion token。
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    var amountCents by remember { mutableLongStateOf(existingRecord?.amountCents ?: 0L) }
    var recordType by remember {
        mutableStateOf(
            if (existingRecord?.recordType == "income") RecordType.INCOME else RecordType.EXPENSE
        )
    }
    var selectedCategoryId by remember { mutableLongStateOf(existingRecord?.categoryId ?: -1L) }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }
    var timestamp by remember {
        mutableLongStateOf(
            existingRecord?.createdAt ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isSaveEnabled = amountCents > 0 && selectedCategoryId != -1L

    val contentSpec = MaterialTheme.motionScheme.fastSpatialSpec<IntSize>()
    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(existingRecord) {
        existingRecord?.let {
            amountCents = it.amountCents
            recordType = if (it.recordType == "income") RecordType.INCOME else RecordType.EXPENSE
            selectedCategoryId = it.categoryId
            note = it.note
            timestamp = it.createdAt
        }
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
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            MoniIcon(
                                MoniIcons.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.expenseRed
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (isSaveEnabled) {
                        if (isEditMode) {
                            onDispatch(
                                CoreIntent.RecordUpdate(
                                    id = existingRecord.id,
                                    amountCents = amountCents,
                                    recordType = recordType,
                                    categoryId = selectedCategoryId,
                                    note = note
                                )
                            )
                        } else {
                            onDispatch(
                                CoreIntent.RecordCreate(
                                    amountCents = amountCents,
                                    recordType = recordType,
                                    categoryId = selectedCategoryId,
                                    note = note,
                                    timestamp = timestamp
                                )
                            )
                        }
                        onNavigateBack()
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "保存",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp)
                .animateContentSize(animationSpec = contentSpec),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RecordTypeSelector(
                selectedType = recordType,
                onTypeSelected = {
                    recordType = it
                    selectedCategoryId = -1L
                }
            )

            AmountInput(
                value = amountCents,
                onValueChange = { amountCents = it },
                currencySymbol = appState.currencySymbol
            )

            DatePickerField(
                timestamp = timestamp,
                onTimestampChange = { timestamp = it }
            )

            CategorySelector(
                categories = appState.categories.filter { it.categoryType == recordType.serialName },
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { selectedCategoryId = it }
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("可选，最多50字") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    AnimatedVisibility(
        visible = showDeleteConfirm,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        existingRecord?.let { onDispatch(CoreIntent.RecordDelete(it.id)) }
                        showDeleteConfirm = false
                        onNavigateBack()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 收入/支出切换器。
 *
 * 升级为 Material 3 Expressive [ButtonGroup] + 声明式 [androidx.compose.material3.ButtonGroupScope.toggleableItem]：
 * 选中时邻居自动 squish 形变（M3 Expressive 招牌"挤压"交互），并由 ButtonGroup 自动管理 overflow，
 * 与按钮族 motion 风格统一。
 */
@Composable
private fun RecordTypeSelector(
    selectedType: RecordType,
    onTypeSelected: (RecordType) -> Unit,
    modifier: Modifier = Modifier
) {
    ButtonGroup(
        overflowIndicator = { /* 仅 2 个固定项，永不溢出 */ },
        modifier = modifier.fillMaxWidth()
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

@Composable
private fun CategorySelector(
    categories: List<CoreCategory>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "分类",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (categories.isEmpty()) {
            Text(
                text = "暂无分类，请先添加分类",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(category: CoreCategory, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(category.name) },
        leadingIcon = if (isSelected) {
            {
                MoniIcon(
                    MoniIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}
