@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.agguy.moni.app.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.agguy.moni.R
import com.agguy.moni.core.util.formatAmount

/**
 * 金额输入组件。
 *
 * 升级到 Material 3 Expressive：[animateContentSize] 接入 motionScheme.defaultSpatialSpec()，
 * 大额预览用 displayMediumEmphasized 凸显 hero moment。
 *
 * @param value 金额，单位为分
 * @param onValueChange 金额变化回调，单位为分
 * @param currencySymbol 货币符号，默认为 ¥
 * @param modifier 修饰符
 */
@Composable
fun AmountInput(
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "¥"
) {
    var textValue by remember(value) {
        mutableStateOf(if (value > 0) formatAmount(value) else "")
    }

    val sizeAnimSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    Column(
        modifier = modifier.animateContentSize(animationSpec = sizeAnimSpec),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                val cleaned = newText.filter { it.isDigit() || it == '.' }
                val parts = cleaned.split(".")
                val integerPart = parts[0].filter { it.isDigit() }
                val decimalPart = if (parts.size > 1) {
                    parts[1].filter { it.isDigit() }.take(2)
                } else {
                    ""
                }

                val cents = if (integerPart.isEmpty() && decimalPart.isEmpty()) {
                    0L
                } else {
                    val integer = integerPart.toLongOrNull() ?: 0L
                    val decimal = if (decimalPart.isEmpty()) {
                        0L
                    } else {
                        when (decimalPart.length) {
                            1 -> decimalPart.toLong() * 10
                            else -> decimalPart.toLong()
                        }
                    }
                    integer * 100 + decimal
                }

                textValue = formatDisplay(integerPart, decimalPart, currencySymbol)
                onValueChange(cents)
            },
            label = { Text(stringResource(R.string.record_amount)) },
            placeholder = { Text("${currencySymbol}0.00") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        )

        Text(
            text = if (value > 0) "${currencySymbol}${formatAmount(value)}" else "${currencySymbol}0.00",
            style = MaterialTheme.typography.displayMediumEmphasized,
            color = if (value > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

private fun formatDisplay(integer: String, decimal: String, symbol: String): String = if (decimal.isEmpty()) {
    if (integer.isEmpty()) "" else "$symbol$integer"
} else {
    "$symbol$integer.$decimal"
}
