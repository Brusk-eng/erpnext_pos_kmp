@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.paymententry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.utils.loading.LoadingUiState
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.paymententry.components.ModeSelectorField
import com.erpnext.pos.views.paymententry.components.PayEntrySection
import com.erpnext.pos.views.paymententry.components.PaymentEntryDetailSection
import com.erpnext.pos.views.paymententry.components.ReferenceDatePickerField
import com.erpnext.pos.views.paymententry.components.PaymentEntrySubmitSection
import com.erpnext.pos.views.paymententry.components.ReceiveInvoiceSection
import com.erpnext.pos.views.paymententry.components.SupplierPendingInvoicesSection
import com.erpnext.pos.views.paymententry.components.TransferFlowSection
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(state: PaymentEntryState, action: PaymentEntryAction) {
  val snackbar = koinInject<SnackbarController>()
  val loadingState by LoadingIndicator.state.collectAsState(initial = LoadingUiState())
  val globalBusy = loadingState.isLoading
  val colorScheme = MaterialTheme.colorScheme
  val fieldShape = RoundedCornerShape(12.dp)
  val fieldColors = paymentEntryFieldColors()

  state.errorMessage?.let { snackbar.show(it, SnackbarType.Error, SnackbarPosition.Top) }

  state.successMessage?.let { snackbar.show(it, SnackbarType.Success, SnackbarPosition.Top) }

  Scaffold(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.20f)) { padding ->
    Column(
        modifier =
            Modifier.padding(padding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (state.offlineModeEnabled || !state.isOnline) {
        val offlineMessage =
            when {
              state.offlineModeEnabled && !state.isOnline ->
                  "Modo offline activo y sin Internet. Este módulo solo funciona en línea."
              state.offlineModeEnabled ->
                  "Modo offline activo. Desactívalo en Configuraciones para continuar."
              else -> "No hay conexión a Internet. Este módulo solo funciona en línea."
            }
        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
          Text(
              text = offlineMessage,
              color = colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(10.dp),
          )
        }
      }

      if (state.entryType == PaymentEntryType.Receive) {
        ReceiveInvoiceSection(
            invoiceId = state.invoiceId,
            onInvoiceIdChanged = action.onInvoiceIdChanged,
            fieldShape = fieldShape,
            fieldColors = fieldColors,
        )
      }

      when (state.entryType) {
        PaymentEntryType.InternalTransfer -> {
          TransferFlowSection(
              sourceValue = state.sourceAccount,
              destinationValue = state.targetAccount,
              options = state.accountOptions,
              onSourceSelected = action.onSourceAccountChanged,
              onDestinationSelected = action.onTargetAccountChanged,
              fieldShape = fieldShape,
              fieldColors = fieldColors,
          )
        }

        PaymentEntryType.Pay,
        PaymentEntryType.Receive -> {
          if (state.entryType == PaymentEntryType.Pay) {
            PayEntrySection(
                state = state,
                action = action,
                fieldShape = fieldShape,
                fieldColors = fieldColors,
            )
          } else {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text("Cuenta y cobro", style = MaterialTheme.typography.titleSmall)
                ModeSelectorField(
                    label = "Modo de pago",
                    value = state.modeOfPayment,
                    options = state.availableModes,
                    shape = fieldShape,
                    colors = fieldColors,
                    onSelected = action.onModeOfPaymentChanged,
                )
              }
            }
          }
        }
      }

      if (state.entryType != PaymentEntryType.Pay) {
        PaymentEntryDetailSection(
            state = state,
            action = action,
            fieldShape = fieldShape,
            fieldColors = fieldColors,
            colorScheme = colorScheme,
        )
      }

      PaymentEntrySubmitSection(
          state = state,
          globalBusy = globalBusy,
          colorScheme = colorScheme,
          onSubmit = action.onSubmit,
      )
    }
  }
}

@Composable
private fun paymentEntryFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    )

@Preview(showBackground = true)
@Composable
private fun PaymentEntryScreenPreview() {
  PaymentEntryScreen(
      state =
          PaymentEntryState(
              entryType = PaymentEntryType.Pay,
              modeOfPayment = "Efectivo",
              amount = "1250.00",
          ),
      action = PaymentEntryAction(),
  )
}
