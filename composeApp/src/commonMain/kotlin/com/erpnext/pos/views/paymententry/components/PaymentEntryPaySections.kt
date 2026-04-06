package com.erpnext.pos.views.paymententry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.paymententry.PaymentEntryAction
import com.erpnext.pos.views.paymententry.PaymentEntryState

@Composable
internal fun PayEntrySection(
    state: PaymentEntryState,
    action: PaymentEntryAction,
    fieldShape: RoundedCornerShape,
    fieldColors: TextFieldColors,
) {
  ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      val showInvoicesOnRight = maxWidth > 940.dp

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Gastos (Master/Details)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        val detailSection: @Composable (Modifier) -> Unit = { sectionModifier ->
          Column(
              modifier = sectionModifier,
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (state.partyOptions.isNotEmpty()) {
              ModeSelectorField(
                  label = "Proveedor / Tercero",
                  value = state.party,
                  options = state.partyOptions,
                  shape = fieldShape,
                  colors = fieldColors,
                  onSelected = action.onPartyChanged,
              )
            } else {
              OutlinedTextField(
                  value = state.party,
                  onValueChange = action.onPartyChanged,
                  modifier = Modifier.fillMaxWidth(),
                  label = { Text("Proveedor / Tercero") },
                  shape = fieldShape,
                  colors = fieldColors,
                  singleLine = true,
              )
            }

            if (state.accountOptions.isNotEmpty()) {
              ModeSelectorField(
                  label = "Cuenta de pago",
                  value = state.sourceAccount,
                  options = state.accountOptions,
                  shape = fieldShape,
                  colors = fieldColors,
                  onSelected = action.onSourceAccountChanged,
              )
            }

            OutlinedTextField(
                value = state.concept,
                onValueChange = action.onConceptChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Concepto del gasto (opcional)") },
                placeholder = { Text("Describe motivo y naturaleza del gasto") },
                shape = fieldShape,
                colors = fieldColors,
                minLines = 3,
                maxLines = 5,
            )

            ExpenseDetailFieldsSection(
                state = state,
                action = action,
                fieldShape = fieldShape,
                fieldColors = fieldColors,
            )
          }
        }

        if (showInvoicesOnRight) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
          ) {
            detailSection(Modifier.weight(0.95f))
            SupplierPendingInvoicesSection(
                modifier = Modifier.weight(1.05f),
                visible = true,
                invoices = state.supplierPendingInvoices,
                paymentCurrency = state.currencyCode,
                enteredAmountText = state.amount,
                isLoading = state.supplierInvoicesLoading,
                errorMessage = state.supplierInvoicesError,
                onToggleInvoice = action.onSupplierInvoiceToggled,
            )
          }
        } else {
          detailSection(Modifier.fillMaxWidth())
          SupplierPendingInvoicesSection(
              modifier = Modifier.fillMaxWidth(),
              visible = true,
              invoices = state.supplierPendingInvoices,
              paymentCurrency = state.currencyCode,
              enteredAmountText = state.amount,
              isLoading = state.supplierInvoicesLoading,
              errorMessage = state.supplierInvoicesError,
              onToggleInvoice = action.onSupplierInvoiceToggled,
          )
        }
      }
    }
  }
}

@Composable
private fun ExpenseDetailFieldsSection(
    state: PaymentEntryState,
    action: PaymentEntryAction,
    fieldShape: RoundedCornerShape,
    fieldColors: TextFieldColors,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val wide = maxWidth > 620.dp
    val medium = maxWidth > 460.dp

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Detalle del gasto",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text = "Completa el monto y, si aplica, los datos del comprobante bancario.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
          MoneyTextField(
              currencyCode = state.currencyCode,
              rawValue = state.amount,
              onRawValueChange = action.onAmountChanged,
              modifier = Modifier.weight(1.15f),
              label = "Monto del gasto",
              imeAction = androidx.compose.ui.text.input.ImeAction.Next,
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.referenceNo,
                onValueChange = action.onReferenceNoChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ref. comprobante") },
                placeholder = { Text("FACT/CHK-001") },
                isError = state.referenceNoError != null,
                supportingText = {
                  Text(
                      state.referenceNoError ?: "Se genera automáticamente si lo dejas vacío."
                  )
                },
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
            label = "Monto del gasto",
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        )

        if (medium) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top,
          ) {
            OutlinedTextField(
                value = state.referenceNo,
                onValueChange = action.onReferenceNoChanged,
                modifier = Modifier.weight(1f),
                label = { Text("Ref. comprobante") },
                placeholder = { Text("FACT/CHK-001") },
                isError = state.referenceNoError != null,
                supportingText = {
                  Text(
                      state.referenceNoError ?: "Se genera automáticamente si lo dejas vacío."
                  )
                },
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
              label = { Text("Referencia del comprobante") },
              placeholder = { Text("FACT/CHK-001") },
              isError = state.referenceNoError != null,
              supportingText = {
                Text(state.referenceNoError ?: "Se genera automáticamente si lo dejas vacío.")
              },
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
    }
  }
}
