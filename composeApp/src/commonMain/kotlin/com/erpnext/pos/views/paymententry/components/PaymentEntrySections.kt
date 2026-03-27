package com.erpnext.pos.views.paymententry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.paymententry.PaymentEntryAction
import com.erpnext.pos.views.paymententry.PaymentEntryState
import com.erpnext.pos.views.paymententry.PaymentEntryType

@Composable
internal fun ReceiveInvoiceSection(
    invoiceId: String,
    onInvoiceIdChanged: (String) -> Unit,
    fieldShape: androidx.compose.foundation.shape.RoundedCornerShape,
    fieldColors: TextFieldColors,
) {
  ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text("Documento", style = MaterialTheme.typography.labelLarge)
      OutlinedTextField(
          value = invoiceId,
          onValueChange = onInvoiceIdChanged,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Factura") },
          shape = fieldShape,
          colors = fieldColors,
          readOnly = true,
          singleLine = true,
      )
    }
  }
}

@Composable
internal fun PaymentEntryDetailSection(
    state: PaymentEntryState,
    action: PaymentEntryAction,
    fieldShape: androidx.compose.foundation.shape.RoundedCornerShape,
    fieldColors: TextFieldColors,
    colorScheme: androidx.compose.material3.ColorScheme,
) {
  ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      val wide = maxWidth > 720.dp
      val medium = maxWidth > 460.dp

      val detailTitle =
          when (state.entryType) {
            PaymentEntryType.Pay -> "Detalle del gasto"
            PaymentEntryType.InternalTransfer -> "Detalle de transferencia"
            PaymentEntryType.Receive -> "Detalle del cobro"
          }
      val referenceNoLabelCompact =
          when (state.entryType) {
            PaymentEntryType.InternalTransfer -> "Ref. transferencia"
            PaymentEntryType.Pay -> "Ref. comprobante"
            PaymentEntryType.Receive -> "Ref. cobro"
          }
      val referenceNoLabelFull =
          when (state.entryType) {
            PaymentEntryType.InternalTransfer -> "Referencia de transferencia"
            PaymentEntryType.Pay -> "Referencia del comprobante"
            PaymentEntryType.Receive -> "Número de referencia"
          }
      val referenceNoPlaceholder =
          when (state.entryType) {
            PaymentEntryType.InternalTransfer -> "TRX-001"
            PaymentEntryType.Pay -> "FACT/CHK-001"
            PaymentEntryType.Receive -> "REF-001"
          }
      val amountLabel =
          when (state.entryType) {
            PaymentEntryType.Pay -> "Monto del gasto"
            PaymentEntryType.InternalTransfer -> "Monto a transferir"
            PaymentEntryType.Receive -> "Monto a registrar"
          }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            detailTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text =
                when (state.entryType) {
                  PaymentEntryType.Pay ->
                      "Completa el monto y, si aplica, los datos del comprobante bancario."
                  PaymentEntryType.InternalTransfer ->
                      "Usa referencia y fecha cuando la cuenta origen o destino sea bancaria."
                  PaymentEntryType.Receive ->
                      "Registra el importe cobrado y la referencia si el medio de pago la requiere."
                },
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
        )

        if (wide) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = androidx.compose.ui.Alignment.Top,
          ) {
            MoneyTextField(
                currencyCode = state.currencyCode,
                rawValue = state.amount,
                onRawValueChange = action.onAmountChanged,
                modifier = Modifier.weight(1.15f),
                label = amountLabel,
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                  value = state.referenceNo,
                  onValueChange = action.onReferenceNoChanged,
                  modifier = Modifier.fillMaxWidth(),
                  label = { Text(referenceNoLabelCompact) },
                  placeholder = { Text(referenceNoPlaceholder) },
                  isError = state.referenceNoError != null,
                  supportingText = { state.referenceNoError?.let { Text(it) } },
                  shape = fieldShape,
                  colors = fieldColors,
                  singleLine = true,
              )
              ReferenceDatePickerField(
                  value = state.referenceDate,
                  onDateSelected = action.onReferenceDateChanged,
                  modifier = Modifier.fillMaxWidth(),
                  label = "Fecha referencia",
                  isError = state.referenceDateError != null,
                  errorText = state.referenceDateError,
                  shape = fieldShape,
                  colors = fieldColors,
              )
            }
          }
        } else {
          MoneyTextField(
              currencyCode = state.currencyCode,
              rawValue = state.amount,
              onRawValueChange = action.onAmountChanged,
              label = amountLabel,
              imeAction = androidx.compose.ui.text.input.ImeAction.Next,
          )

          if (medium) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Top,
            ) {
              OutlinedTextField(
                  value = state.referenceNo,
                  onValueChange = action.onReferenceNoChanged,
                  modifier = Modifier.weight(1f),
                  label = { Text(referenceNoLabelCompact) },
                  placeholder = { Text(referenceNoPlaceholder) },
                  isError = state.referenceNoError != null,
                  supportingText = { state.referenceNoError?.let { Text(it) } },
                  shape = fieldShape,
                  colors = fieldColors,
                  singleLine = true,
              )
              ReferenceDatePickerField(
                  value = state.referenceDate,
                  onDateSelected = action.onReferenceDateChanged,
                  modifier = Modifier.weight(1f),
                  label = "Fecha referencia",
                  isError = state.referenceDateError != null,
                  errorText = state.referenceDateError,
                  shape = fieldShape,
                  colors = fieldColors,
              )
            }
          } else {
            OutlinedTextField(
                value = state.referenceNo,
                onValueChange = action.onReferenceNoChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(referenceNoLabelFull) },
                placeholder = { Text(referenceNoPlaceholder) },
                isError = state.referenceNoError != null,
                supportingText = { state.referenceNoError?.let { Text(it) } },
                shape = fieldShape,
                colors = fieldColors,
                singleLine = true,
            )
            ReferenceDatePickerField(
                value = state.referenceDate,
                onDateSelected = action.onReferenceDateChanged,
                modifier = Modifier.fillMaxWidth(),
                label = "Fecha de referencia",
                isError = state.referenceDateError != null,
                errorText = state.referenceDateError,
                shape = fieldShape,
                colors = fieldColors,
            )
          }
        }

        OutlinedTextField(
            value = state.notes,
            onValueChange = action.onNotesChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notas / observaciones") },
            shape = fieldShape,
            colors = fieldColors,
            minLines = 2,
            maxLines = 3,
        )
      }
    }
  }
}

@Composable
internal fun PaymentEntrySubmitSection(
    state: PaymentEntryState,
    globalBusy: Boolean,
    colorScheme: androidx.compose.material3.ColorScheme,
    onSubmit: () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val compactAction = maxWidth < 520.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (compactAction) Arrangement.Center else Arrangement.End,
    ) {
      Button(
          onClick = onSubmit,
          modifier = if (compactAction) Modifier.fillMaxWidth() else Modifier,
          enabled = !state.isSubmitting && !globalBusy && state.isOnline && !state.offlineModeEnabled,
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = colorScheme.primary,
                  contentColor = colorScheme.onPrimary,
              ),
      ) {
        if (state.isSubmitting) {
          CircularProgressIndicator(
              modifier = Modifier.padding(end = 8.dp).size(16.dp),
              strokeWidth = 2.dp,
          )
        }
        Text(
            when (state.entryType) {
              PaymentEntryType.Pay -> "Registrar gasto"
              PaymentEntryType.InternalTransfer -> "Transferir"
              PaymentEntryType.Receive -> "Registrar cobro"
            }
        )
      }
    }
  }

  Text(
      text =
          if (state.entryType == PaymentEntryType.InternalTransfer) {
            "No altera el total del turno; reclasifica saldo entre cuentas contables."
          } else if (state.entryType == PaymentEntryType.Receive) {
            "Entrada permitida únicamente para cobro de factura del cliente."
          } else {
            "El gasto disminuye caja/banco y se registra contra la cuenta de gasto."
          },
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
  )
}
