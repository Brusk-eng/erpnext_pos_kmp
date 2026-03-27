package com.erpnext.pos.views.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.localization.LocalAppStrings

@Composable
internal fun TtlHoursDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
  val strings = LocalAppStrings.current
  var input by remember { mutableStateOf(initialValue.coerceIn(1, 168).toString()) }
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(strings.settings.ttlHoursLabel) },
      text = {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it.filter(Char::isDigit) },
            label = { Text(strings.settings.ttlHoursInputLabel) },
            supportingText = { Text(strings.settings.ttlHoursRangeHint) },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
      },
      confirmButton = {
        Button(
            onClick = {
              val value = input.toIntOrNull()?.coerceIn(1, 168) ?: 6
              onConfirm(value)
            }
        ) {
          Text(strings.settings.ttlHoursInputLabel)
        }
      },
  )
}

@Composable
internal fun ReturnPolicyDaysDialog(
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
  var input by remember { mutableStateOf(initialValue.toString()) }
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Límite de días") },
      text = {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Días (0 = sin límite)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
      },
      confirmButton = {
        Button(
            onClick = {
              val value = input.toIntOrNull()?.coerceAtLeast(0) ?: 0
              onConfirm(value)
            }
        ) {
          Text("Guardar")
        }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}

@Composable
internal fun ReturnPolicyDestinationDialog(
    current: ReturnDestinationPolicy,
    allowRefunds: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ReturnDestinationPolicy) -> Unit,
) {
  var selected by remember { mutableStateOf(current) }
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Destino por defecto") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Define si el retorno genera reembolso inmediato o crédito a favor.")
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selected == ReturnDestinationPolicy.CREDIT,
                onClick = { selected = ReturnDestinationPolicy.CREDIT },
                label = { Text("Crédito") },
            )
            FilterChip(
                selected = selected == ReturnDestinationPolicy.REFUND,
                onClick = { selected = ReturnDestinationPolicy.REFUND },
                label = { Text("Reembolso") },
                enabled = allowRefunds,
            )
          }
          if (!allowRefunds) {
            Text(
                "Los reembolsos están deshabilitados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      },
      confirmButton = { Button(onClick = { onConfirm(selected) }) { Text("Guardar") } },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
  )
}
