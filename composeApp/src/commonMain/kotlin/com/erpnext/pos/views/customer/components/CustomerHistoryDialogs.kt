@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.customer.CustomerPaymentState
import com.erpnext.pos.views.customer.ReturnAccountingSummary
import com.erpnext.pos.views.customer.ReturnDestination
import com.erpnext.pos.views.customer.ReturnSelectionItemUi
import com.erpnext.pos.views.customer.requiresReference

internal data class ReturnDialogConfirmation(val applyRefund: Boolean)

@Composable
internal fun PartialReturnDialog(
    invoiceId: String,
    invoiceLocal: SalesInvoiceWithItemsAndPayments?,
    returnLoading: Boolean,
    returnError: String?,
    historyBusy: Boolean,
    refundOptions: List<String>,
    returnPolicy: ReturnPolicySettings,
    paymentState: CustomerPaymentState,
    refundMode: String?,
    onRefundModeChange: (String?) -> Unit,
    refundReference: String,
    onRefundReferenceChange: (String) -> Unit,
    returnReason: String,
    onReturnReasonChange: (String) -> Unit,
    qtyByItemCode: Map<String, Double>,
    onQtyByItemCodeChange: (Map<String, Double>) -> Unit,
    returnDestination: ReturnDestination,
    onReturnDestinationChange: (ReturnDestination) -> Unit,
    isPhysicalReturn: Boolean,
    onIsPhysicalReturnChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (ReturnDialogConfirmation) -> Unit,
) {
  val selectableItems =
      remember(invoiceLocal) {
        invoiceLocal
            ?.items
            ?.groupBy { it.itemCode }
            ?.map { (itemCode, rows) ->
              val soldQty = rows.sumOf { kotlin.math.abs(it.qty) }
              val totalAmount = rows.sumOf { kotlin.math.abs(it.amount) }
              val unitAmount =
                  if (soldQty > 0.0) totalAmount / soldQty
                  else kotlin.math.abs(rows.firstOrNull()?.rate ?: 0.0)
              ReturnSelectionItemUi(
                  itemCode = itemCode,
                  itemName =
                      rows.firstNotNullOfOrNull { row -> row.itemName?.takeIf { it.isNotBlank() } }
                          ?: itemCode,
                  soldQty = soldQty,
                  unitAmount = unitAmount,
              )
            }
            ?.sortedBy { it.itemName.lowercase() } ?: emptyList()
      }
  val invoiceCurrency = normalizeCurrency(invoiceLocal?.invoice?.currency)
  val returnTotal =
      remember(selectableItems, qtyByItemCode) {
        selectableItems.sumOf { item ->
          val qty = (qtyByItemCode[item.itemCode] ?: 0.0).coerceAtLeast(0.0)
          qty.coerceAtMost(item.soldQty) * item.unitAmount
        }
      }
  val projectedOutstanding =
      invoiceLocal?.invoice?.outstandingAmount?.let { (it - returnTotal).coerceAtLeast(0.0) }
  val refundAllowed = returnPolicy.allowRefunds
  val effectiveDestination = if (refundAllowed) returnDestination else ReturnDestination.CREDIT
  val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
  val selectedMode =
      paymentState.paymentModes.firstOrNull { it.modeOfPayment.equals(refundMode, true) }
  val needsReference = refundEnabled && requiresReference(selectedMode)
  val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
  val missingReference = needsReference && refundReference.isBlank()
  val missingReason = returnPolicy.requireReason && returnReason.isBlank()
  val hasRefundOptions = refundOptions.isNotEmpty()
  val canConfirmRefund =
      !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
  val canConfirm = canConfirmRefund && !missingReason
  val canConfirmReturn = qtyByItemCode.values.any { it > 0.0 }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Retorno parcial") },
      text = {
        PartialReturnDialogContent(
            invoiceId = invoiceId,
            returnLoading = returnLoading,
            returnError = returnError,
            historyBusy = historyBusy,
            refundOptions = refundOptions,
            refundEnabled = refundEnabled,
            returnPolicy = returnPolicy,
            refundMode = refundMode,
            onRefundModeChange = onRefundModeChange,
            refundReference = refundReference,
            onRefundReferenceChange = onRefundReferenceChange,
            returnReason = returnReason,
            onReturnReasonChange = onReturnReasonChange,
            selectableItems = selectableItems,
            qtyByItemCode = qtyByItemCode,
            onQtyByItemCodeChange = onQtyByItemCodeChange,
            returnDestination = returnDestination,
            onReturnDestinationChange = onReturnDestinationChange,
            refundAllowed = refundAllowed,
            needsReference = needsReference,
            invoiceCurrency = invoiceCurrency,
            returnTotal = returnTotal,
            projectedOutstanding = projectedOutstanding,
            isPhysicalReturn = isPhysicalReturn,
            onIsPhysicalReturnChange = onIsPhysicalReturnChange,
            canConfirmReturn = canConfirmReturn,
            missingRefundMode = missingRefundMode,
            missingReference = missingReference,
            missingReason = missingReason,
            effectiveDestination = effectiveDestination,
        )
      },
      confirmButton = {
        TextButton(
            onClick = { onConfirm(ReturnDialogConfirmation(refundEnabled)) },
            enabled = !historyBusy && canConfirmReturn && canConfirm,
        ) {
          Text("Confirmar")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text("Cancelar") }
      },
  )
}

@Composable
internal fun FullReturnDialog(
    invoiceId: String,
    invoice: SalesInvoiceBO?,
    invoiceLocal: SalesInvoiceWithItemsAndPayments?,
    fullReturnLoading: Boolean,
    fullReturnError: String?,
    historyBusy: Boolean,
    refundOptions: List<String>,
    returnPolicy: ReturnPolicySettings,
    paymentState: CustomerPaymentState,
    refundMode: String?,
    onRefundModeChange: (String?) -> Unit,
    refundReference: String,
    onRefundReferenceChange: (String) -> Unit,
    returnReason: String,
    onReturnReasonChange: (String) -> Unit,
    returnDestination: ReturnDestination,
    onReturnDestinationChange: (ReturnDestination) -> Unit,
    isPhysicalReturn: Boolean,
    onIsPhysicalReturnChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (ReturnDialogConfirmation) -> Unit,
) {
  val invoiceCurrency = normalizeCurrency(invoiceLocal?.invoice?.currency ?: invoice?.currency)
  val returnTotal = (invoiceLocal?.invoice?.grandTotal ?: invoice?.total ?: 0.0).coerceAtLeast(0.0)
  val outstandingAmount = invoiceLocal?.invoice?.outstandingAmount ?: invoice?.outstandingAmount
  val projectedOutstanding = outstandingAmount?.let { (it - returnTotal).coerceAtLeast(0.0) }
  val refundAllowed = returnPolicy.allowRefunds
  val effectiveDestination = if (refundAllowed) returnDestination else ReturnDestination.CREDIT
  val refundEnabled = refundAllowed && effectiveDestination == ReturnDestination.RETURN
  val selectedMode =
      paymentState.paymentModes.firstOrNull { it.modeOfPayment.equals(refundMode, true) }
  val needsReference = refundEnabled && requiresReference(selectedMode)
  val missingRefundMode = refundEnabled && refundMode.isNullOrBlank()
  val missingReference = needsReference && refundReference.isBlank()
  val missingReason = returnPolicy.requireReason && returnReason.isBlank()
  val hasRefundOptions = refundOptions.isNotEmpty()
  val canConfirmRefund =
      !refundEnabled || (hasRefundOptions && !missingRefundMode && !missingReference)
  val canConfirm = canConfirmRefund && !missingReason

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Retorno total") },
      text = {
        FullReturnDialogContent(
            invoiceId = invoiceId,
            fullReturnLoading = fullReturnLoading,
            fullReturnError = fullReturnError,
            historyBusy = historyBusy,
            refundOptions = refundOptions,
            refundEnabled = refundEnabled,
            returnPolicy = returnPolicy,
            refundMode = refundMode,
            onRefundModeChange = onRefundModeChange,
            refundReference = refundReference,
            onRefundReferenceChange = onRefundReferenceChange,
            returnReason = returnReason,
            onReturnReasonChange = onReturnReasonChange,
            returnDestination = returnDestination,
            onReturnDestinationChange = onReturnDestinationChange,
            refundAllowed = refundAllowed,
            needsReference = needsReference,
            invoiceCurrency = invoiceCurrency,
            returnTotal = returnTotal,
            projectedOutstanding = projectedOutstanding,
            isPhysicalReturn = isPhysicalReturn,
            onIsPhysicalReturnChange = onIsPhysicalReturnChange,
            missingRefundMode = missingRefundMode,
            missingReference = missingReference,
            missingReason = missingReason,
            effectiveDestination = effectiveDestination,
        )
      },
      confirmButton = {
        Button(enabled = !historyBusy && canConfirm, onClick = { onConfirm(ReturnDialogConfirmation(refundEnabled)) }) {
          Text("Confirmar retorno")
        }
      },
      dismissButton = {
        OutlinedButton(enabled = !historyBusy, onClick = onDismiss) { Text("Cerrar") }
      },
  )
}

@Composable
private fun PartialReturnDialogContent(
    invoiceId: String,
    returnLoading: Boolean,
    returnError: String?,
    historyBusy: Boolean,
    refundOptions: List<String>,
    refundEnabled: Boolean,
    returnPolicy: ReturnPolicySettings,
    refundMode: String?,
    onRefundModeChange: (String?) -> Unit,
    refundReference: String,
    onRefundReferenceChange: (String) -> Unit,
    returnReason: String,
    onReturnReasonChange: (String) -> Unit,
    selectableItems: List<ReturnSelectionItemUi>,
    qtyByItemCode: Map<String, Double>,
    onQtyByItemCodeChange: (Map<String, Double>) -> Unit,
    returnDestination: ReturnDestination,
    onReturnDestinationChange: (ReturnDestination) -> Unit,
    refundAllowed: Boolean,
    needsReference: Boolean,
    invoiceCurrency: String,
    returnTotal: Double,
    projectedOutstanding: Double?,
    isPhysicalReturn: Boolean,
    onIsPhysicalReturnChange: (Boolean) -> Unit,
    canConfirmReturn: Boolean,
    missingRefundMode: Boolean,
    missingReference: Boolean,
    missingReason: Boolean,
    effectiveDestination: ReturnDestination,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ReturnDialogHeader(
        invoiceId = invoiceId,
        isPhysicalReturn = isPhysicalReturn,
        onIsPhysicalReturnChange = onIsPhysicalReturnChange,
        historyBusy = historyBusy,
    )
    ReturnDialogLoadingState(returnLoading = returnLoading, error = returnError)
    RefundDestinationSection(
        refundAllowed = refundAllowed,
        returnDestination = returnDestination,
        onReturnDestinationChange = onReturnDestinationChange,
    )
    RefundConfigurationSection(
        refundEnabled = refundEnabled,
        refundOptions = refundOptions,
        refundMode = refundMode,
        onRefundModeChange = onRefundModeChange,
        refundReference = refundReference,
        onRefundReferenceChange = onRefundReferenceChange,
        needsReference = needsReference,
        historyBusy = historyBusy,
    )
    ReturnAccountingSummary(
        returnTotal = returnTotal,
        currency = invoiceCurrency,
        refundEnabled = refundEnabled,
        creditEnabled = effectiveDestination == ReturnDestination.CREDIT,
        projectedOutstanding = projectedOutstanding,
        affectInventory = isPhysicalReturn,
    )
    if (returnPolicy.requireReason) {
      OutlinedTextField(
          value = returnReason,
          onValueChange = onReturnReasonChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Motivo (requerido)") },
          singleLine = false,
          minLines = 2,
          enabled = !historyBusy,
      )
    }
    Text(
        "Selecciona cantidades a devolver:",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    PartialReturnItemsList(
        selectableItems = selectableItems,
        qtyByItemCode = qtyByItemCode,
        onQtyByItemCodeChange = onQtyByItemCodeChange,
        invoiceCurrency = invoiceCurrency,
        historyBusy = historyBusy,
        returnLoading = returnLoading,
        returnError = returnError,
    )
    ReturnValidationMessages(
        canConfirmReturn = canConfirmReturn,
        refundEnabled = refundEnabled,
        missingRefundMode = missingRefundMode,
        missingReference = missingReference,
        missingReason = missingReason,
    )
  }
}

@Composable
private fun FullReturnDialogContent(
    invoiceId: String,
    fullReturnLoading: Boolean,
    fullReturnError: String?,
    historyBusy: Boolean,
    refundOptions: List<String>,
    refundEnabled: Boolean,
    returnPolicy: ReturnPolicySettings,
    refundMode: String?,
    onRefundModeChange: (String?) -> Unit,
    refundReference: String,
    onRefundReferenceChange: (String) -> Unit,
    returnReason: String,
    onReturnReasonChange: (String) -> Unit,
    returnDestination: ReturnDestination,
    onReturnDestinationChange: (ReturnDestination) -> Unit,
    refundAllowed: Boolean,
    needsReference: Boolean,
    invoiceCurrency: String,
    returnTotal: Double,
    projectedOutstanding: Double?,
    isPhysicalReturn: Boolean,
    onIsPhysicalReturnChange: (Boolean) -> Unit,
    missingRefundMode: Boolean,
    missingReference: Boolean,
    missingReason: Boolean,
    effectiveDestination: ReturnDestination,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    ReturnDialogHeader(
        invoiceId = invoiceId,
        isPhysicalReturn = isPhysicalReturn,
        onIsPhysicalReturnChange = onIsPhysicalReturnChange,
        historyBusy = historyBusy,
    )
    ReturnDialogLoadingState(returnLoading = fullReturnLoading, error = fullReturnError)
    RefundDestinationSection(
        refundAllowed = refundAllowed,
        returnDestination = returnDestination,
        onReturnDestinationChange = onReturnDestinationChange,
    )
    RefundConfigurationSection(
        refundEnabled = refundEnabled,
        refundOptions = refundOptions,
        refundMode = refundMode,
        onRefundModeChange = onRefundModeChange,
        refundReference = refundReference,
        onRefundReferenceChange = onRefundReferenceChange,
        needsReference = needsReference,
        historyBusy = historyBusy,
    )
    ReturnAccountingSummary(
        returnTotal = returnTotal,
        currency = invoiceCurrency,
        refundEnabled = refundEnabled,
        creditEnabled = effectiveDestination == ReturnDestination.CREDIT,
        projectedOutstanding = projectedOutstanding,
        affectInventory = isPhysicalReturn,
    )
    if (returnPolicy.requireReason) {
      OutlinedTextField(
          value = returnReason,
          onValueChange = onReturnReasonChange,
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Motivo (requerido)") },
          singleLine = false,
          minLines = 2,
          enabled = !historyBusy,
      )
    }
    ReturnValidationMessages(
        canConfirmReturn = true,
        refundEnabled = refundEnabled,
        missingRefundMode = missingRefundMode,
        missingReference = missingReference,
        missingReason = missingReason,
    )
  }
}

@Composable
private fun ReturnDialogHeader(
    invoiceId: String,
    isPhysicalReturn: Boolean,
    onIsPhysicalReturnChange: (Boolean) -> Unit,
    historyBusy: Boolean,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("Factura: $invoiceId", modifier = Modifier.weight(1f))
    FilterChip(
        selected = isPhysicalReturn,
        onClick = { onIsPhysicalReturnChange(!isPhysicalReturn) },
        enabled = !historyBusy,
        label = { Text("Retorno físico") },
    )
  }
}

@Composable
private fun ReturnDialogLoadingState(returnLoading: Boolean, error: String?) {
  if (returnLoading) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text("Cargando detalle local...", style = MaterialTheme.typography.bodySmall)
  }
  if (!error.isNullOrBlank()) {
    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
  }
}

@Composable
private fun RefundDestinationSection(
    refundAllowed: Boolean,
    returnDestination: ReturnDestination,
    onReturnDestinationChange: (ReturnDestination) -> Unit,
) {
  Text(
      text = "Destino del monto devuelto",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
  )
  if (refundAllowed) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      ReturnDestination.entries.forEach { destination ->
        FilterChip(
            selected = returnDestination == destination,
            onClick = { onReturnDestinationChange(destination) },
            label = { Text(destination.label) },
        )
      }
    }
  } else {
    Text(
        "Reembolsos deshabilitados; se aplicará crédito a favor.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
  Spacer(Modifier.height(6.dp))
}

@Composable
private fun RefundConfigurationSection(
    refundEnabled: Boolean,
    refundOptions: List<String>,
    refundMode: String?,
    onRefundModeChange: (String?) -> Unit,
    refundReference: String,
    onRefundReferenceChange: (String) -> Unit,
    needsReference: Boolean,
    historyBusy: Boolean,
) {
  var refundModeExpanded by remember { mutableStateOf(false) }

  if (refundEnabled && refundOptions.isNotEmpty()) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reembolso", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        ExposedDropdownMenuBox(
            expanded = refundModeExpanded,
            onExpandedChange = { refundModeExpanded = !refundModeExpanded },
        ) {
          OutlinedTextField(
              value = refundMode ?: "",
              onValueChange = {},
              modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              label = { Text("Modo de reembolso (opcional)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = refundModeExpanded) },
              leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
              readOnly = true,
              singleLine = true,
              enabled = !historyBusy,
              supportingText = { Text("Vacío = solo nota de crédito.") },
          )
          ExposedDropdownMenu(
              expanded = refundModeExpanded,
              onDismissRequest = { refundModeExpanded = false },
          ) {
            refundOptions.forEach { option ->
              DropdownMenuItem(
                  text = { Text(option) },
                  onClick = {
                    onRefundModeChange(option)
                    refundModeExpanded = false
                  },
              )
            }
          }
        }
      }
    }
  }

  if (refundEnabled && refundOptions.isEmpty()) {
    Text(
        "No hay modos de pago disponibles para reembolsos.",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
  }

  if (refundEnabled && needsReference) {
    OutlinedTextField(
        value = refundReference,
        onValueChange = onRefundReferenceChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Referencia (requerida)") },
        singleLine = true,
        enabled = !historyBusy,
    )
  }

  if (refundEnabled) {
    Text(
        "El retorno genera una nota de crédito aplicable contra la factura original.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PartialReturnItemsList(
    selectableItems: List<ReturnSelectionItemUi>,
    qtyByItemCode: Map<String, Double>,
    onQtyByItemCodeChange: (Map<String, Double>) -> Unit,
    invoiceCurrency: String,
    historyBusy: Boolean,
    returnLoading: Boolean,
    returnError: String?,
) {
  if (selectableItems.isNotEmpty()) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
      items(selectableItems, key = { it.itemCode }) { item ->
        val soldQty = item.soldQty
        val current = qtyByItemCode[item.itemCode] ?: 0.0
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Checkbox(
                      checked = current > 0.0,
                      onCheckedChange = { checked ->
                        val next = if (checked) soldQty else 0.0
                        onQtyByItemCodeChange(qtyByItemCode.toMutableMap().apply { put(item.itemCode, next) })
                      },
                      enabled = !historyBusy,
                  )
                  Text(
                      text = item.itemName,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                  )
                }
                Text("Código: ${item.itemCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Vendidos: ${formatDoubleToString(soldQty, 2)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Precio unitario: ${formatCurrency(invoiceCurrency, item.unitAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = {
                      val next = (current - 1.0).coerceAtLeast(0.0)
                      onQtyByItemCodeChange(qtyByItemCode.toMutableMap().apply { put(item.itemCode, next) })
                    },
                    enabled = !historyBusy && current > 0.0,
                ) {
                  Icon(Icons.Default.Remove, null)
                }
                Text(text = formatDoubleToString(current, 2), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                IconButton(
                    onClick = {
                      val next = (current + 1.0).coerceAtMost(soldQty)
                      onQtyByItemCodeChange(qtyByItemCode.toMutableMap().apply { put(item.itemCode, next) })
                    },
                    enabled = !historyBusy && current < soldQty,
                ) {
                  Icon(Icons.Default.Add, null)
                }
              }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Subtotal devolución", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(formatCurrency(invoiceCurrency, item.unitAmount * current.coerceAtLeast(0.0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  } else if (!returnLoading && returnError.isNullOrBlank()) {
    Text(
        "No hay artículos disponibles para devolución.",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
  }
}

@Composable
private fun ReturnValidationMessages(
    canConfirmReturn: Boolean,
    refundEnabled: Boolean,
    missingRefundMode: Boolean,
    missingReference: Boolean,
    missingReason: Boolean,
) {
  if (!canConfirmReturn) {
    Text("Selecciona al menos un item.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
  }
  if (refundEnabled && missingRefundMode) {
    Text("Selecciona un modo de reembolso.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
  }
  if (refundEnabled && missingReference) {
    Text("La referencia es requerida para este modo de reembolso.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
  }
  if (missingReason) {
    Text("Debes indicar el motivo del retorno.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
  }
}
