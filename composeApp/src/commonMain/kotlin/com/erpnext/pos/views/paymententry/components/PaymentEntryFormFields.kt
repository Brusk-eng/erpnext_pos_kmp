package com.erpnext.pos.views.paymententry.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModeSelectorField(
    label: String,
    value: String,
    options: List<String>,
    shape: RoundedCornerShape,
    colors: TextFieldColors,
    onSelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier =
            Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        label = { Text(label) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        shape = shape,
        colors = colors,
        placeholder = { Text("Seleccionar...") },
        readOnly = true,
        singleLine = true,
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      if (options.isEmpty()) {
        DropdownMenuItem(
            text = { Text("Sin opciones disponibles") },
            onClick = { expanded = false },
            enabled = false,
        )
      } else {
        options.forEach { option ->
          DropdownMenuItem(
              text = { Text(option) },
              onClick = {
                onSelected(option)
                expanded = false
              },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReferenceDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    isError: Boolean,
    errorText: String?,
    shape: RoundedCornerShape,
    colors: TextFieldColors,
) {
  var openPicker by remember { mutableStateOf(false) }
  val dateFieldInteraction = remember { MutableInteractionSource() }
  val initialSelectedDateMillis =
      remember(value) {
        runCatching {
              val date = LocalDate.parse(value)
              date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            }
            .getOrNull()
      }
  val pickerState =
      androidx.compose.material3.rememberDatePickerState(
          initialSelectedDateMillis = initialSelectedDateMillis
      )

  Box(modifier = modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        isError = isError,
        supportingText = { errorText?.let { Text(it) } },
        shape = shape,
        colors = colors,
        singleLine = true,
        readOnly = true,
        trailingIcon = {
          Icon(imageVector = Icons.Filled.DateRange, contentDescription = "Seleccionar fecha")
        },
    )
    Box(
        modifier =
            Modifier.matchParentSize().clickable(
                interactionSource = dateFieldInteraction,
                indication = null,
            ) {
              openPicker = true
            }
    )
  }

  if (openPicker) {
    DatePickerDialog(
        onDismissRequest = { openPicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                  val isoDate =
                      Instant.fromEpochMilliseconds(millis)
                          .toLocalDateTime(TimeZone.UTC)
                          .date
                          .toString()
                  onDateSelected(isoDate)
                }
                openPicker = false
              }
          ) {
            Text("Aceptar")
          }
        },
        dismissButton = { TextButton(onClick = { openPicker = false }) { Text("Cancelar") } },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
