package com.erpnext.pos.views.paymententry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.views.paymententry.SupplierPendingInvoiceUi

@Composable
internal fun SupplierPendingInvoicesSection(
    modifier: Modifier = Modifier,
    visible: Boolean,
    invoices: List<SupplierPendingInvoiceUi>,
    paymentCurrency: String,
    enteredAmountText: String,
    isLoading: Boolean,
    errorMessage: String?,
    onToggleInvoice: (String) -> Unit,
) {
  if (!visible) return

  val scheme = MaterialTheme.colorScheme
  val summary = rememberSupplierPendingInvoicesSummary(invoices, enteredAmountText)

  ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
          text = "Facturas pendientes del proveedor",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      Text(
          text =
              "Selecciona las facturas a las que se aplicará este pago. La asignación se calcula automáticamente según el monto en la moneda de la cuenta de pago.",
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
      )
      Text(
          text = "Pendientes: ${invoices.size}  |  Seleccionadas: ${summary.selectedCount}",
          style = MaterialTheme.typography.bodySmall,
          color = scheme.onSurfaceVariant,
      )
      if (summary.hasSelected) {
        Text(
            text =
                "Adeudado seleccionado (${paymentCurrency.ifBlank { "Moneda pago" }}): " +
                    formatInvoiceAmount(paymentCurrency, summary.selectedOutstandingInPaymentCurrency),
            style = MaterialTheme.typography.bodySmall,
        )
        if (summary.changeInFavor > 0.0) {
          Text(
              text = "Vuelto a favor: ${formatInvoiceAmount(paymentCurrency, summary.changeInFavor)}",
              style = MaterialTheme.typography.bodySmall,
              color = scheme.primary,
              fontWeight = FontWeight.SemiBold,
          )
        }
      }

      if (isLoading) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Text("Cargando facturas pendientes...", style = MaterialTheme.typography.bodySmall)
        }
      }

      errorMessage?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall, color = scheme.error)
      }

      if (!isLoading && errorMessage == null && invoices.isEmpty()) {
        Text(
            text = "No hay facturas pendientes para este proveedor.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
      }

      if (invoices.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(items = invoices, key = { it.invoiceName }) { invoice ->
            SupplierPendingInvoiceRow(
                invoice = invoice,
                paymentCurrency = paymentCurrency,
                onToggleInvoice = onToggleInvoice,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SupplierPendingInvoiceRow(
    invoice: SupplierPendingInvoiceUi,
    paymentCurrency: String,
    onToggleInvoice: (String) -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val outstandingInPaymentCurrency = resolveOutstandingInPaymentCurrency(invoice)

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(scheme.surface)
              .clickable { onToggleInvoice(invoice.invoiceName) }
              .padding(8.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Checkbox(
        checked = invoice.selected,
        onCheckedChange = { onToggleInvoice(invoice.invoiceName) },
    )
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(invoice.invoiceName, fontWeight = FontWeight.SemiBold)
      if (invoice.status.isNotBlank()) {
        Text(
            text = "Estado: ${invoice.status}",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
      }
      val dates =
          listOf(
                  invoice.postingDate.takeIf { it.isNotBlank() }?.let { "Emisión: $it" },
                  invoice.dueDate.takeIf { it.isNotBlank() }?.let { "Vence: $it" },
              )
              .filterNotNull()
              .joinToString("  •  ")
      if (dates.isNotBlank()) {
        Text(
            dates,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
      }
      Text(
          text =
              "Adeudado (factura): ${formatInvoiceAmount(invoice.invoiceCurrency, invoice.outstandingAmountInvoiceCurrency)}",
          style = MaterialTheme.typography.bodySmall,
      )
      if (outstandingInPaymentCurrency != null) {
        Text(
            text =
                "Adeudado (${paymentCurrency.ifBlank { "Moneda pago" }}): " +
                    formatInvoiceAmount(paymentCurrency, outstandingInPaymentCurrency),
            style = MaterialTheme.typography.bodySmall,
        )
      }
      if (invoice.conversionError) {
        Text(
            text =
                "No se encontró tipo de cambio ${invoice.paymentCurrency} -> ${invoice.invoiceCurrency}",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.error,
        )
      }
      if (invoice.selected) {
        Text(
            text =
                "Aplicado (${paymentCurrency.ifBlank { "Moneda pago" }}): " +
                    formatInvoiceAmount(paymentCurrency, invoice.allocatedAmountPaymentCurrency),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (invoice.allocatedAmountPaymentCurrency > 0.0) scheme.primary else scheme.error,
        )
        Text(
            text =
                "Aplicado (factura): " +
                    formatInvoiceAmount(
                        invoice.invoiceCurrency,
                        invoice.allocatedAmountInvoiceCurrency,
                    ),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (invoice.allocatedAmountInvoiceCurrency > 0.0) scheme.primary else scheme.error,
        )
      }
    }
  }
}

private data class SupplierPendingInvoicesSummary(
    val selectedCount: Int,
    val selectedOutstandingInPaymentCurrency: Double,
    val changeInFavor: Double,
    val hasSelected: Boolean,
)

@Composable
private fun rememberSupplierPendingInvoicesSummary(
    invoices: List<SupplierPendingInvoiceUi>,
    enteredAmountText: String,
): SupplierPendingInvoicesSummary {
  val selectedInvoices = invoices.filter { it.selected && !it.conversionError }
  val selectedOutstandingInPaymentCurrency =
      selectedInvoices.sumOf { resolveOutstandingInPaymentCurrency(it) ?: 0.0 }
  val enteredAmount = enteredAmountText.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val hasSelected = selectedInvoices.isNotEmpty()
  val changeInFavor =
      if (hasSelected && enteredAmount > selectedOutstandingInPaymentCurrency) {
        enteredAmount - selectedOutstandingInPaymentCurrency
      } else {
        0.0
      }

  return SupplierPendingInvoicesSummary(
      selectedCount = selectedInvoices.size,
      selectedOutstandingInPaymentCurrency = selectedOutstandingInPaymentCurrency,
      changeInFavor = changeInFavor,
      hasSelected = hasSelected,
  )
}

private fun formatInvoiceAmount(currency: String, amount: Double): String {
  val code = currency.ifBlank { "" }
  val rounded = kotlin.math.round(amount * 100.0) / 100.0
  return if (code.isBlank()) rounded.toString() else "$code $rounded"
}

private fun resolveOutstandingInPaymentCurrency(invoice: SupplierPendingInvoiceUi): Double? {
  invoice.outstandingAmountPaymentCurrency?.let { return it }
  val rate = invoice.paymentToInvoiceRate
  if (rate != null && rate > 0.0) {
    return invoice.outstandingAmountInvoiceCurrency / rate
  }
  return null
}
