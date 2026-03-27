package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.resolveInvoiceDisplayAmounts
import com.erpnext.pos.views.customer.CustomerInvoiceHistoryState
import com.erpnext.pos.views.customer.isWithinDays

internal data class CustomerHistoryOverviewData(
    val subtitle: String,
    val filteredInvoices: List<SalesInvoiceBO>,
    val pendingCount: Int,
    val paidCount: Int,
)

internal fun buildCustomerHistoryOverview(
    historyState: CustomerInvoiceHistoryState,
    snapshotInvoices: List<SalesInvoiceBO>,
    selectedRangeDays: Int,
): CustomerHistoryOverviewData {
  val filteredInvoices = snapshotInvoices.filter { isWithinDays(it.postingDate, selectedRangeDays) }
  val subtitle =
      when (historyState) {
        is CustomerInvoiceHistoryState.Success ->
            "${filteredInvoices.size} facturas en $selectedRangeDays días"
        CustomerInvoiceHistoryState.Loading -> "Cargando historial..."
        is CustomerInvoiceHistoryState.Error -> "Historial no disponible"
        else -> "Historial de facturas"
      }
  val pendingCount =
      filteredInvoices.count {
        val status = it.status?.trim()?.lowercase()
        status == "unpaid" || status == "overdue" || status == "partly paid"
      }
  val paidCount = filteredInvoices.count { it.status?.trim()?.lowercase() == "paid" }

  return CustomerHistoryOverviewData(
      subtitle = subtitle,
      filteredInvoices = filteredInvoices,
      pendingCount = pendingCount,
      paidCount = paidCount,
  )
}

@Composable
internal fun CustomerHistoryOverviewHeader(
    overview: CustomerHistoryOverviewData,
    selectedRangeDays: Int,
    historyMessage: String?,
    onRangeSelected: (Int) -> Unit,
) {
  Text(
      text = overview.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    HistoryRangeChip(
        label = "7 días",
        selected = selectedRangeDays == 7,
        onClick = { onRangeSelected(7) },
    )
    HistoryRangeChip(
        label = "30 días",
        selected = selectedRangeDays == 30,
        onClick = { onRangeSelected(30) },
    )
    HistoryRangeChip(
        label = "90 días",
        selected = selectedRangeDays == 90,
        onClick = { onRangeSelected(90) },
    )
  }
  if (overview.filteredInvoices.isNotEmpty()) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      HistoryStatChip(
          label = "Total",
          value = overview.filteredInvoices.size.toString(),
          modifier = Modifier.weight(1f),
      )
      HistoryStatChip(
          label = "Pendientes",
          value = overview.pendingCount.toString(),
          modifier = Modifier.weight(1f),
      )
      HistoryStatChip(
          label = "Pagadas",
          value = overview.paidCount.toString(),
          modifier = Modifier.weight(1f),
      )
    }
  }
  if (!historyMessage.isNullOrBlank()) {
    Text(
        text = historyMessage,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun HistoryStatChip(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
  ) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          text = value,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun HistoryRangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
  FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
internal fun InvoiceHistorySummary(
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
) {
  val companyCurrency = normalizeCurrency(posBaseCurrency)
  val invoiceCurrency = normalizeCurrency(invoices.firstOrNull()?.currency)
  val totalBase =
      invoices.sumOf { resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingCompany }
  val totalPos =
      invoices.sumOf { resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingInvoice }

  if (totalBase > 0.0 || totalPos > 0.0) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                )
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (invoiceCurrency.isNotBlank()) {
          Text(invoiceCurrency, style = MaterialTheme.typography.labelSmall)
          Text(
              formatCurrency(invoiceCurrency, totalPos),
              style = MaterialTheme.typography.labelMedium,
          )
        } else {
          Text(companyCurrency, style = MaterialTheme.typography.labelSmall)
          Text(
              formatCurrency(companyCurrency, totalBase),
              style = MaterialTheme.typography.labelMedium,
          )
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(companyCurrency, style = MaterialTheme.typography.labelSmall)
        Text(
            formatCurrency(companyCurrency, totalBase),
            style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}
