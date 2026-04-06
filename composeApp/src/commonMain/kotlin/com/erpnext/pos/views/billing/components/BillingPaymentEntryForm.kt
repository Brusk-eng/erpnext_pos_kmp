package com.erpnext.pos.views.billing.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.billing.PaymentLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentModeSelector(
    selectedMode: String,
    modeOptions: List<String>,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var modeExpanded by remember { mutableStateOf(false) }

  Text(strings.billing.paymentModeLabel, style = MaterialTheme.typography.bodyMedium)
  ExposedDropdownMenuBox(
      expanded = modeExpanded,
      onExpandedChange = { modeExpanded = !modeExpanded },
  ) {
    AppTextField(
        value = selectedMode,
        onValueChange = {},
        modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        label = strings.billing.paymentModeLabel,
        placeholder = strings.billing.paymentModePlaceholder,
        readOnly = true,
        leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
    )
    DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
      modeOptions.forEach { mode ->
        DropdownMenuItem(
            text = { Text(mode) },
            onClick = {
              onModeSelected(mode)
              modeExpanded = false
            },
        )
      }
    }
  }
}

@Composable
internal fun PaymentAmountEntry(
    selectedCurrency: String,
    baseCurrency: String,
    amountInput: String,
    amountValue: Double,
    onAmountInputChange: (String) -> Unit,
    onAmountValueChange: (Double) -> Unit,
    canAdd: Boolean,
    onClearAmount: () -> Unit,
    onAddPayment: () -> Unit,
    rateInput: String,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  MoneyTextField(
      currencyCode = selectedCurrency,
      rawValue = amountInput,
      onRawValueChange = onAmountInputChange,
      label = strings.billing.amountLabel,
      enabled = true,
      onAmountChanged = onAmountValueChange,
      supportingText = {
        if (!selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
          val rate = rateInput.toDoubleOrNull() ?: 0.0
          val base = amountValue * rate
          Text("${strings.billing.baseLabel}: ${formatAmount(baseCurrency.toCurrencySymbol(), base)}")
        }
      },
      trailingIcon = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onClearAmount, enabled = amountInput.isNotBlank()) {
            Icon(Icons.Default.Delete, contentDescription = strings.common.clear)
          }
          IconButton(onClick = onAddPayment, enabled = canAdd) {
            Icon(Icons.Default.Add, contentDescription = strings.billing.addPayment)
          }
        }
      },
      modifier = modifier,
  )
}

@Composable
internal fun PaymentReferenceField(
    selectedMode: String,
    referenceInput: String,
    onReferenceInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  AppTextField(
      value = referenceInput,
      onValueChange = onReferenceInputChange,
      label = strings.billing.referenceNumberLabel,
      placeholder = "#11231",
      leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
      supportingText = {
        if (referenceInput.isBlank()) {
          Text("${strings.billing.referenceRequiredHint} $selectedMode.")
        }
      },
      isError = referenceInput.isBlank(),
      modifier = modifier,
  )
}

internal fun buildPaymentLine(
    selectedMode: String,
    amountValue: Double,
    selectedCurrency: String,
    baseCurrency: String,
    rateValue: Double,
    referenceInput: String,
): PaymentLine {
  val effectiveRate = if (selectedCurrency.equals(baseCurrency, ignoreCase = true)) 1.0 else rateValue
  return PaymentLine(
      modeOfPayment = selectedMode,
      enteredAmount = amountValue,
      currency = selectedCurrency,
      exchangeRate = effectiveRate,
      baseAmount = amountValue * effectiveRate,
      referenceNumber = referenceInput.takeIf { it.isNotBlank() },
  )
}
