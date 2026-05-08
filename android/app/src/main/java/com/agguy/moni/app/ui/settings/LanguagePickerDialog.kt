package com.agguy.moni.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
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
import com.agguy.moni.app.i18n.AppLocaleManager

@Composable
fun LanguagePickerDialog(
    currentLanguage: AppLocaleManager.AppLanguage,
    onConfirm: (AppLocaleManager.AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        AppLocaleManager.AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
        AppLocaleManager.AppLanguage.CHINESE to stringResource(R.string.settings_language_chinese),
        AppLocaleManager.AppLanguage.ENGLISH to stringResource(R.string.settings_language_english)
    )
    var selected by remember { mutableStateOf(currentLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                options.forEach { (language, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = language }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selected == language,
                            onClick = { selected = language }
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
