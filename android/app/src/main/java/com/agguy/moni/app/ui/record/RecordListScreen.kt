package com.agguy.moni.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agguy.moni.app.AppState
import com.agguy.moni.app.theme.ExpenseRed
import com.agguy.moni.app.theme.IncomeGreen
import com.agguy.moni.core.CoreIntent
import com.agguy.moni.core.CoreRecord
import com.agguy.moni.core.util.formatAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordListScreen(
    appState: AppState,
    onDispatch: (CoreIntent) -> Unit,
    onNavigateToRecordDetail: (Long?) -> Unit,
    onNavigateToCategoryList: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recordToDelete by remember { mutableLongStateOf(-1L) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("账单") },
                actions = {
                    TextButton(onClick = onNavigateToCategoryList) {
                        Text("分类")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToRecordDetail(null) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "记一笔")
            }
        }
    ) { innerPadding ->
        if (appState.records.isEmpty()) {
            EmptyRecordList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            RecordListContent(
                records = appState.records,
                categories = appState.categories,
                currencySymbol = appState.currencySymbol,
                onRecordClick = { onNavigateToRecordDetail(it) },
                onDeleteRequest = { recordToDelete = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (recordToDelete != -1L) {
        AlertDialog(
            onDismissRequest = { recordToDelete = -1L },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDispatch(CoreIntent.RecordDelete(recordToDelete))
                        recordToDelete = -1L
                    }
                ) {
                    Text("删除", color = ExpenseRed)
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
    records: List<CoreRecord>,
    categories: List<com.agguy.moni.core.CoreCategory>,
    currencySymbol: String,
    onRecordClick: (Long) -> Unit,
    onDeleteRequest: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedRecords = remember(records) { groupRecordsByDate(records) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedRecords.forEach { (date, dayRecords) ->
            item(key = "header_$date") {
                DayHeader(
                    date = date,
                    records = dayRecords,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(dayRecords, key = { it.id }) { record ->
                val category = categories.find { it.id == record.categoryId }
                RecordListItem(
                    record = record,
                    categoryName = category?.name ?: "未知分类",
                    categoryColor = category?.colorHex ?: "#808080",
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
    records: List<CoreRecord>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val income = records.filter { it.recordType == "income" }.sumOf { it.amountCents }
    val expense = records.filter { it.recordType == "expense" }.sumOf { it.amountCents }

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
                if (income > 0) {
                    Text(
                        text = "收 ${currencySymbol}${formatAmount(income)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen
                    )
                }
                if (expense > 0) {
                    Text(
                        text = "支 ${currencySymbol}${formatAmount(expense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun EmptyRecordList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

private fun groupRecordsByDate(records: List<CoreRecord>): Map<String, List<CoreRecord>> {
    return records.groupBy { record ->
        try {
            LocalDate.ofInstant(Instant.ofEpochSecond(record.createdAt), ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            ""
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

