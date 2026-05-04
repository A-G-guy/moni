@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.icons.MoniIcon
import com.agguy.moni.app.icons.MoniIcons
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.app.theme.incomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreRecordGroup
import com.agguy.moni.core.util.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 账单列表主屏。
 *
 * Material 3 Expressive 改造点：
 * - 标题用 [androidx.compose.material3.Typography.displaySmallEmphasized] 强化 hero moment；
 * - FAB + AppBar action 收敛到底部 [HorizontalFloatingToolbar]，分类入口与「记一笔」并列；
 * - 删除确认对话框的 scale 动画接入 [androidx.compose.material3.MotionScheme]，统一动效曲线。
 */
@Composable
fun RecordListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recordToDelete by remember { mutableLongStateOf(-1L) }
    val dialogSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "账单",
                        style = MaterialTheme.typography.displaySmallEmphasized
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = true
            ) {
                IconButton(onClick = onNavigateToCategoryList) {
                    MoniIcon(
                        icon = MoniIcons.FilterList,
                        contentDescription = "分类管理"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { onNavigateToRecordDetail(null) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.size(56.dp)
                ) {
                    MoniIcon(
                        icon = MoniIcons.AddFilled,
                        contentDescription = "记一笔"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (appState.recordGroups.isEmpty()) {
            EmptyRecordList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            RecordListContent(
                recordGroups = appState.recordGroups,
                currencySymbol = appState.currencySymbol,
                onRecordClick = { onNavigateToRecordDetail(it) },
                onDeleteRequest = { recordToDelete = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    AnimatedVisibility(
        visible = recordToDelete != -1L,
        enter = scaleIn(animationSpec = dialogSpec),
        exit = scaleOut(animationSpec = dialogSpec)
    ) {
        AlertDialog(
            onDismissRequest = { recordToDelete = -1L },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDispatch(CoreIntent.RecordDelete(recordToDelete))
                        recordToDelete = -1L
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.expenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = -1L }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RecordListContent(
    recordGroups: List<CoreRecordGroup>,
    currencySymbol: String,
    onRecordClick: (Long) -> Unit,
    onDeleteRequest: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        recordGroups.forEach { group ->
            item(key = "header_${group.date}") {
                DayHeader(
                    date = group.date,
                    incomeCents = group.incomeCents,
                    expenseCents = group.expenseCents,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(group.records, key = { it.id }) { record ->
                RecordListItem(
                    record = record,
                    currencySymbol = currencySymbol,
                    onClick = { onRecordClick(record.id) },
                    onLongClick = { onDeleteRequest(record.id) }
                )
            }
        }
    }
}

@Composable
private fun DayHeader(
    date: String,
    incomeCents: Long,
    expenseCents: Long,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDisplayDate(date),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (incomeCents > 0) {
                    Text(
                        text = "收 ${currencySymbol}${formatAmount(incomeCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.incomeGreen
                    )
                }
                if (expenseCents > 0) {
                    Text(
                        text = "支 ${currencySymbol}${formatAmount(expenseCents)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.expenseRed
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun EmptyRecordList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "暂无记账记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右下角按钮记一笔",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        when (date) {
            today -> "今天"
            today.minusDays(1) -> "昨天"
            else -> date.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
        }
    } catch (_: Exception) {
        dateStr
    }
}
