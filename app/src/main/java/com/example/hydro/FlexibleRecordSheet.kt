package com.example.hydro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FlexibleRecordSheet(
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountMl by rememberSaveable { mutableFloatStateOf(DEFAULT_FLEXIBLE_RECORD_ML.toFloat()) }
    val amount = amountMl.roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "灵活记录",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "$amount ml",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = amountMl,
            onValueChange = { amountMl = it },
            valueRange = MIN_FLEXIBLE_RECORD_ML.toFloat()..MAX_FLEXIBLE_RECORD_ML.toFloat(),
            steps = FLEXIBLE_RECORD_STEPS,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$MIN_FLEXIBLE_RECORD_ML ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$MAX_FLEXIBLE_RECORD_ML ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = { onConfirm(amount) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("确定")
        }
    }
}

private const val MIN_FLEXIBLE_RECORD_ML = 50
private const val MAX_FLEXIBLE_RECORD_ML = 1000
private const val DEFAULT_FLEXIBLE_RECORD_ML = 350
private const val FLEXIBLE_RECORD_STEPS =
    (MAX_FLEXIBLE_RECORD_ML - MIN_FLEXIBLE_RECORD_ML) / 50 - 1
