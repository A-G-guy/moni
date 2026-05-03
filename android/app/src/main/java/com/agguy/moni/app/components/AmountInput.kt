package com.agguy.moni.app.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agguy.moni.core.util.formatAmount

/**
 * 金额输入组件。
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
    currencySymbol: String = "¥",
    modifier: Modifier = Modifier
) {
    var textValue by remember(value) {
        mutableStateOf(if (value > 0) formatAmount(value) else "")
    }

    Column(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                val cleaned = newText.filter { it.isDigit() || it == '.' }
                val parts = cleaned.split(".")
                val integerPart = parts[0].filter { it.isDigit() }
                val decimalPart = if (parts.size > 1) {
                    parts[1].filter { it.isDigit() }.take(2)
                } else ""

                val cents = if (integerPart.isEmpty() && decimalPart.isEmpty()) {
                    0L
                } else {
                    val integer = integerPart.toLongOrNull() ?: 0L
                    val decimal = if (decimalPart.isEmpty()) 0L else {
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
            label = { Text("金额") },
            placeholder = { Text("${currencySymbol}0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
        )

        if (value > 0) {
            Text(
                text = "${currencySymbol}${formatAmount(value)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDisplay(integer: String, decimal: String, symbol: String): String {
    return if (decimal.isEmpty()) {
        if (integer.isEmpty()) "" else "$symbol$integer"
    } else {
        "$symbol$integer.$decimal"
    }
}

