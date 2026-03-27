@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Money
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.resolveInvoiceDisplayAmounts
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.customer.CustomerInvoicesState
import com.erpnext.pos.views.customer.CustomerPaymentState
import com.erpnext.pos.views.customer.InvoicePdfActionOption

@Composable
internal fun OutstandingInvoicesList(
    invoicesState: CustomerInvoicesState,
    outstandingInvoicesPagingItems: LazyPagingItems<SalesInvoiceBO>,
    companyCurrency: String,
    invoiceListMaxHeight: Dp,
    selectedInvoiceId: String?,
    onInvoiceSelected: (SalesInvoiceBO, OutstandingInvoiceSelection) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
) {
  val strings = LocalAppStrings.current

  when (invoicesState) {
    CustomerInvoicesState.Idle -> {
      Text(
          text = strings.customer.selectCustomerToViewInvoices,
          style = MaterialTheme.typography.bodyMedium,
      )
    }

    CustomerInvoicesState.Loading -> {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
      }
    }

    is CustomerInvoicesState.Error -> {
      Text(
          text = invoicesState.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
      )
    }

    is CustomerInvoicesState.Success -> {
      val refreshState = outstandingInvoicesPagingItems.loadState.refresh
      val appendState = outstandingInvoicesPagingItems.loadState.append
      val isLoading = refreshState is LoadState.Loading
      val hasError = refreshState is LoadState.Error || appendState is LoadState.Error
      if (hasError) {
        Text(
            text =
                (refreshState as? LoadState.Error)?.error?.message
                    ?: (appendState as? LoadState.Error)?.error?.message
                    ?: "No se pudieron cargar las facturas pendientes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
      } else if (isLoading && outstandingInvoicesPagingItems.itemCount == 0) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          CircularProgressIndicator()
        }
      } else if (outstandingInvoicesPagingItems.itemCount == 0) {
        Text(text = strings.customer.emptyOsInvoices, style = MaterialTheme.typography.bodyMedium)
      } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = invoiceListMaxHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(
              count = outstandingInvoicesPagingItems.itemCount,
              key = { index -> outstandingInvoicesPagingItems[index]?.invoiceId ?: "outstanding_$index" },
          ) { index ->
            val invoice = outstandingInvoicesPagingItems[index] ?: return@items
            OutstandingInvoiceCard(
                invoice = invoice,
                companyCurrency = companyCurrency,
                isSelected = invoice.invoiceId == selectedInvoiceId,
                onSelected = onInvoiceSelected,
                onDownloadInvoicePdf = onDownloadInvoicePdf,
            )
          }
          if (outstandingInvoicesPagingItems.loadState.append is LoadState.Loading) {
            item {
              Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                  horizontalArrangement = Arrangement.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OutstandingInvoiceCard(
    invoice: SalesInvoiceBO,
    companyCurrency: String,
    isSelected: Boolean,
    onSelected: (SalesInvoiceBO, OutstandingInvoiceSelection) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
) {
  val display = resolveInvoiceDisplayAmounts(invoice = invoice, companyCurrency = companyCurrency)
  val selection =
      OutstandingInvoiceSelection(
          companyCurrency = display.companyCurrency,
          invoiceCurrency = display.invoiceCurrency,
          outstandingCompany = display.outstandingCompany,
          outstandingInvoice = display.outstandingInvoice,
      )
  val baseOutstanding = bd(display.outstandingCompany).toDouble(0)
  val baseLabel = formatCurrency(display.companyCurrency, baseOutstanding)
  val posLabel = formatCurrency(display.invoiceCurrency, bd(display.outstandingInvoice).toDouble(0))

  Card(
      modifier = Modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                  } else {
                    MaterialTheme.colorScheme.surface
                  }
          ),
      border =
          if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onSelected(invoice, selection) }
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
              text = invoice.invoiceId,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text = "Publicado: ${invoice.postingDate}",
              style = MaterialTheme.typography.bodySmall,
          )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          when (invoice.syncStatus) {
            "Pending" -> {
              AssistChip(onClick = {}, label = { Text("Sincronizacion pendiente") })
            }

            "Failed" -> {
              AssistChip(
                  onClick = {},
                  label = { Text("Sincronizacion falló") },
                  colors =
                      AssistChipDefaults.assistChipColors(
                          containerColor = MaterialTheme.colorScheme.errorContainer,
                          labelColor = MaterialTheme.colorScheme.onErrorContainer,
                      ),
              )
            }
          }
          RadioButton(selected = isSelected, onClick = { onSelected(invoice, selection) })
        }
      }

      Text(
          text = "Pendiente: $posLabel",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
      )
      Text(
          text = "Moneda base: $baseLabel",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      OutstandingInvoicePdfMenu(
          invoiceId = invoice.invoiceId,
          onDownloadInvoicePdf = onDownloadInvoicePdf,
      )
    }
  }
}

@Composable
private fun OutstandingInvoicePdfMenu(
    invoiceId: String,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
) {
  var expanded by remember(invoiceId) { mutableStateOf(false) }
  val currentDownload by rememberUpdatedState(onDownloadInvoicePdf)

  Box {
    TextButton(onClick = { expanded = true }, enabled = invoiceId.isNotBlank()) {
      Text("Descargar PDF")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
          text = { Text("Abrir ahora") },
          onClick = {
            expanded = false
            currentDownload(invoiceId, InvoicePdfActionOption.OPEN_NOW)
          },
      )
      DropdownMenuItem(
          text = { Text("Guardar en...") },
          onClick = {
            expanded = false
            currentDownload(invoiceId, InvoicePdfActionOption.SAVE_AS)
          },
      )
      DropdownMenuItem(
          text = { Text("Compartir") },
          onClick = {
            expanded = false
            currentDownload(invoiceId, InvoicePdfActionOption.SHARE)
          },
      )
    }
  }
}

internal data class OutstandingInvoiceSelection(
    val companyCurrency: String,
    val invoiceCurrency: String,
    val outstandingCompany: Double,
    val outstandingInvoice: Double,
)

@Composable
internal fun OutstandingPaymentForm(
    paymentState: CustomerPaymentState,
    selectedInvoiceId: String?,
    selectedMode: String,
    paymentModes: List<POSPaymentModeOption>,
    selectedCurrency: String,
    requiresReference: Boolean,
    referenceInput: String,
    onReferenceInputChange: (String) -> Unit,
    amountRaw: String,
    onAmountRawChange: (String) -> Unit,
    onAmountChanged: (Double) -> Unit,
    conversionError: Boolean,
    companyCurrency: String,
    amountInCompanyCurrency: Double?,
    changeDue: Double,
    isSubmitEnabled: Boolean,
    onModeSelected: (POSPaymentModeOption) -> Unit,
    onSubmit: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var modeExpanded by remember { mutableStateOf(false) }

  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = strings.customer.registerPaymentTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
      )

      ExposedDropdownMenuBox(
          expanded = modeExpanded,
          onExpandedChange = { modeExpanded = !modeExpanded },
      ) {
        AppTextField(
            value = selectedMode,
            onValueChange = {},
            label = strings.customer.selectPaymentMode,
            placeholder = strings.customer.selectPaymentMode,
            readOnly = true,
            modifier =
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
        )
        ExposedDropdownMenu(
            expanded = modeExpanded,
            onDismissRequest = { modeExpanded = false },
        ) {
          paymentModes.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.name) },
                onClick = {
                  onModeSelected(mode)
                  modeExpanded = false
                },
            )
          }
        }
      }

      if (requiresReference) {
        AppTextField(
            value = referenceInput,
            onValueChange = onReferenceInputChange,
            label = "Número de referencia",
            placeholder = "#11231",
            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
            supportingText = {
              if (referenceInput.isBlank()) {
                Text("Requerido para pagos con $selectedMode.")
              }
            },
            isError = referenceInput.isBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
      }

      MoneyTextField(
          currencyCode = selectedCurrency,
          rawValue = amountRaw,
          onRawValueChange = onAmountRawChange,
          label = strings.customer.amountLabel,
          onAmountChanged = onAmountChanged,
          supportingText = {
            if (conversionError) {
              Text(
                  text = "Tasa de cambio no encontrada de $selectedCurrency a $companyCurrency.",
                  color = MaterialTheme.colorScheme.error,
              )
            } else if (!selectedCurrency.equals(companyCurrency, ignoreCase = true)) {
              Text(
                  "Valor en ${companyCurrency.toCurrencySymbol()}: ${
                      amountInCompanyCurrency?.let { formatCurrency(companyCurrency, it) } ?: "—"
                  }"
              )
            }
          },
      )

      if (changeDue > 0.0) {
        Text(
            text = "Cambio: ${formatCurrency(selectedCurrency, changeDue)}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    Button(
        onClick = onSubmit,
        enabled = isSubmitEnabled && selectedInvoiceId?.isNotBlank() == true,
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
          if (paymentState.isSubmitting) strings.customer.processing
          else strings.customer.registerPaymentButton
      )
    }
  }
}
