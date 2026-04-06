package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.resolveInvoiceDisplayAmounts
import com.erpnext.pos.views.customer.InvoicePdfActionOption

@Composable
internal fun InvoiceHistoryRow(
    invoice: SalesInvoiceBO,
    isBusy: Boolean,
    posBaseCurrency: String,
    returnPolicy: ReturnPolicySettings,
    onCancel: (String) -> Unit,
    onReturnTotal: (String) -> Unit,
    onPartialReturn: (String) -> Unit = {},
    onDownloadPdf: (String, InvoicePdfActionOption) -> Unit = { _, _ -> },
) {
  val display =
      resolveInvoiceDisplayAmounts(
          invoice = invoice,
          companyCurrency = normalizeCurrency(posBaseCurrency),
      )
  val baseTotal = bd(display.totalCompany).toDouble(0)
  val baseOutstanding = bd(display.outstandingCompany).toDouble(0)
  val posTotal = bd(display.totalInvoice).toDouble(0)
  val posOutstanding = bd(display.outstandingInvoice).toDouble(0)
  val statusLabel = invoice.status ?: "Sin estado"
  val statusKey = invoice.status?.trim()?.lowercase()
  val localizedStatus = normalizedStatus(statusKey)
  val hasPayments = invoice.paidAmount > 0.0 || invoice.payments.any { it.amount > 0.0 }
  val unpaidStatuses =
      setOf("draft", "unpaid", "overdue", "overdue and discounted", "unpaid and discounted")
  val paidStatuses = setOf("paid", "partly paid", "partly paid and discounted")
  val isDraftOrUnpaid = statusKey in unpaidStatuses
  val isPaidOrPartly = statusKey in paidStatuses
  val allowCancel = isDraftOrUnpaid && !hasPayments
  val allowReturn = isPaidOrPartly || hasPayments
  val allowFullReturn = allowReturn && returnPolicy.allowFullReturns
  val allowPartialReturn = allowReturn && returnPolicy.allowPartialReturns
  val (statusBg, statusText) =
      when (statusLabel.lowercase()) {
        "paid" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "partly paid" ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        "overdue" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        "unpaid",
        "draft" ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "cancelled" ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
      }

  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(invoice.invoiceId, fontWeight = FontWeight.SemiBold)
          Text(
              text = invoice.postingDate,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          InvoiceHistoryBadge(text = if (invoice.isPos == true) "POS" else "Crédito")
          Surface(color = statusBg, shape = RoundedCornerShape(10.dp)) {
            Text(
                text = localizedStatus,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = statusText,
            )
          }
        }
      }
      Spacer(Modifier.size(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        InvoiceAmountColumn(
            label = "Total",
            primaryValue = formatCurrency(display.invoiceCurrency, posTotal),
            secondaryValue = formatCurrency(display.companyCurrency, baseTotal),
        )
        InvoiceAmountColumn(
            label = "Pendiente",
            primaryValue = formatCurrency(display.invoiceCurrency, posOutstanding),
            secondaryValue = formatCurrency(display.companyCurrency, baseOutstanding),
            primaryColor =
                if (invoice.outstandingAmount > 0.0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            horizontalAlignment = Alignment.End,
        )
      }
      InvoicePdfMenu(invoiceId = invoice.invoiceId, onDownloadPdf = onDownloadPdf)
      Spacer(Modifier.size(10.dp))
      InvoiceActionButtons(
          invoiceId = invoice.invoiceId,
          isBusy = isBusy,
          allowCancel = allowCancel,
          allowFullReturn = allowFullReturn,
          allowPartialReturn = allowPartialReturn,
          onCancel = onCancel,
          onReturnTotal = onReturnTotal,
          onPartialReturn = onPartialReturn,
      )
    }
  }
}

@Composable
private fun InvoiceHistoryBadge(text: String) {
  Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(8.dp),
  ) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun InvoiceAmountColumn(
    label: String,
    primaryValue: String,
    secondaryValue: String,
    primaryColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
  Column(horizontalAlignment = horizontalAlignment) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(primaryValue, style = MaterialTheme.typography.titleSmall, color = primaryColor)
    Text(
        secondaryValue,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun InvoicePdfMenu(
    invoiceId: String,
    onDownloadPdf: (String, InvoicePdfActionOption) -> Unit,
) {
  var expanded by remember(invoiceId) { mutableStateOf(false) }
  val currentOnDownloadPdf by rememberUpdatedState(onDownloadPdf)

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Box {
      TextButton(onClick = { expanded = true }, enabled = invoiceId.isNotBlank()) {
        Text("Descargar PDF")
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Abrir ahora") },
            onClick = {
              expanded = false
              currentOnDownloadPdf(invoiceId, InvoicePdfActionOption.OPEN_NOW)
            },
        )
        DropdownMenuItem(
            text = { Text("Guardar en...") },
            onClick = {
              expanded = false
              currentOnDownloadPdf(invoiceId, InvoicePdfActionOption.SAVE_AS)
            },
        )
        DropdownMenuItem(
            text = { Text("Compartir") },
            onClick = {
              expanded = false
              currentOnDownloadPdf(invoiceId, InvoicePdfActionOption.SHARE)
            },
        )
      }
    }
  }
}

@Composable
private fun InvoiceActionButtons(
    invoiceId: String,
    isBusy: Boolean,
    allowCancel: Boolean,
    allowFullReturn: Boolean,
    allowPartialReturn: Boolean,
    onCancel: (String) -> Unit,
    onReturnTotal: (String) -> Unit,
    onPartialReturn: (String) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (allowCancel) {
      FilledTonalButton(
          onClick = { onCancel(invoiceId) },
          enabled = !isBusy,
          modifier = Modifier.weight(1f),
      ) {
        if (isBusy) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
          Text("Cancelar")
        }
      }
    }
    if (allowFullReturn) {
      OutlinedButton(
          onClick = { onReturnTotal(invoiceId) },
          enabled = !isBusy,
          modifier = Modifier.weight(1f),
      ) {
        Text("Retorno total")
      }
    }
    if (allowPartialReturn) {
      OutlinedButton(
          onClick = { onPartialReturn(invoiceId) },
          enabled = !isBusy,
          modifier = Modifier.weight(1f),
      ) {
        Text("Retorno parcial")
      }
    }
  }
}

@Composable
private fun normalizedStatus(status: String?): String {
  val strings = LocalAppStrings.current.invoice
  return when (status) {
    "draft" -> strings.draft
    "unpaid" -> strings.unpaid
    "paid" -> strings.paid
    "partly paid" -> strings.partlyPaid
    "canceled" -> strings.canceled
    "credit note" -> strings.creditNote
    "return" -> strings.returned
    else -> strings.draft
  }
}
