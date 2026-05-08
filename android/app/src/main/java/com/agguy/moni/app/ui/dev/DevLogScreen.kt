@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.BuildConfig
import com.agguy.moni.R
import com.agguy.moni.app.icons.SymbolIcon
import com.agguy.moni.app.theme.expenseRed
import com.agguy.moni.core.platform.LogCollector
import com.agguy.moni.core.platform.LogLevel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日志查看页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLogScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logs = remember { LogCollector.getLogs() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snapshot = remember(context) { buildSnapshot(context) }
    val timeFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.dev_log_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        SymbolIcon(
                            name = "arrow_back",
                            contentDescription = stringResource(R.string.back),
                            size = 24.dp
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        val content = buildExportContent(snapshot, context)
                        copyToClipboard(context, content)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.dev_log_copy))
                }
                TextButton(
                    onClick = {
                        val content = buildExportContent(snapshot, context)
                        shareAsText(context, content)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.dev_log_share))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = snapshot,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (logs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.dev_log_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(logs) { entry ->
                    val time = timeFormatter.format(java.time.Instant.ofEpochMilli(entry.timestamp))
                    val levelColor = when (entry.level) {
                        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                        LogLevel.INFO -> MaterialTheme.colorScheme.primary
                        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
                        LogLevel.ERROR -> MaterialTheme.colorScheme.expenseRed
                    }
                    Text(
                        text = "[$time] ${entry.level.name.first()}/${entry.tag}: ${entry.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = levelColor
                    )
                }
            }
        }
    }
}

private fun buildExportContent(snapshot: String, context: Context): String {
    val kotlinLogs = LogCollector.formatLogs()
    val systemLogs = LogCollector.collectProcessLogcat()
    return buildString {
        appendLine(snapshot)
        appendLine()
        appendLine(context.getString(R.string.dev_log_kotlin))
        appendLine(kotlinLogs)
        appendLine()
        appendLine(context.getString(R.string.dev_log_system))
        appendLine(systemLogs)
    }
}

private fun buildSnapshot(context: Context): String {
    val sb = StringBuilder()
    sb.appendLine(context.getString(R.string.dev_log_snapshot_title))
    sb.appendLine(context.getString(R.string.dev_log_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
    sb.appendLine(context.getString(R.string.dev_log_device, Build.MODEL))
    sb.appendLine(context.getString(R.string.dev_log_android, Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
    sb.appendLine(context.getString(R.string.dev_log_package, context.packageName))
    sb.appendLine(context.getString(R.string.dev_log_snapshot_end))
    return sb.toString()
}

private fun copyToClipboard(context: Context, content: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.dev_log_clipboard_label), content))
}

private fun shareAsText(context: Context, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.dev_share_logs_subject))
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.dev_share_logs_title)))
}
