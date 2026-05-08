@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.app.theme.ThemeMode

@Composable
fun CurrencyPickerDialog(currentSymbol: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        "¥" to stringResource(R.string.currency_cny),
        "$" to stringResource(R.string.currency_usd),
        "€" to stringResource(R.string.currency_eur),
        "£" to stringResource(R.string.currency_gbp)
    )
    var selected by remember { mutableStateOf(currentSymbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.currency_picker_title)) },
        text = {
            Column {
                options.forEach { (symbol, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = symbol }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selected == symbol,
                            onClick = { selected = symbol }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.currency_format, symbol, name))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeModePickerDialog(currentMode: ThemeMode, onConfirm: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark),
        ThemeMode.SYSTEM to stringResource(R.string.theme_system)
    )
    var selected by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.theme_mode)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = mode }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selected == mode,
                            onClick = { selected = mode }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
