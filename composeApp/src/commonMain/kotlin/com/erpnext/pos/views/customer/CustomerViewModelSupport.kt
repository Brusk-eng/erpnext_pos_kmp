package com.erpnext.pos.views.customer

import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toCurrencySymbol

internal data class ReturnPreview(
    val currency: String,
    val returnTotal: Double,
    val projectedOutstanding: Double?,
)

internal fun buildPaymentModeCurrencyMap(
    definitions: List<ModeOfPaymentEntity>
): Map<String, String> = buildMap {
  definitions.forEach { definition ->
    val currency = definition.currency?.trim()?.uppercase().orEmpty()
    if (currency.isNotBlank()) {
      put(definition.modeOfPayment, currency)
      put(definition.name, currency)
    }
  }
}

internal fun buildPaymentReferenceCacheKey(
    invoiceId: String,
    modeOfPayment: String,
    enteredCurrency: String,
    enteredAmount: Double,
): String =
    listOf(
            invoiceId.trim(),
            modeOfPayment.trim(),
            enteredCurrency.trim().uppercase(),
            roundToCurrency(enteredAmount).toString(),
        )
        .joinToString("|")

internal fun buildPartialReturnPreview(
    invoice: SalesInvoiceWithItemsAndPayments?,
    requested: Map<String, Double>,
): ReturnPreview? {
  invoice ?: return null
  val requestedRemaining = requested.mapValues { it.value.coerceAtLeast(0.0) }.toMutableMap()
  var total = 0.0
  invoice.items.forEach { item ->
    val desired = (requestedRemaining[item.itemCode] ?: 0.0).coerceAtLeast(0.0)
    if (desired <= 0.0) return@forEach
    val soldQty = kotlin.math.abs(item.qty)
    val qtyToReturn = desired.coerceAtMost(soldQty)
    if (qtyToReturn <= 0.0) return@forEach
    requestedRemaining[item.itemCode] = (desired - qtyToReturn).coerceAtLeast(0.0)
    val perUnit = if (item.qty != 0.0) item.amount / item.qty else item.rate
    total += kotlin.math.abs(perUnit) * qtyToReturn
  }
  val outstanding = invoice.invoice.outstandingAmount.coerceAtLeast(0.0)
  return ReturnPreview(
      currency = normalizeCurrency(invoice.invoice.currency),
      returnTotal = roundToCurrency(total),
      projectedOutstanding = roundToCurrency((outstanding - total).coerceAtLeast(0.0)),
  )
}

internal fun buildFullReturnPreview(invoice: SalesInvoiceEntity?): ReturnPreview? {
  invoice ?: return null
  val total = invoice.grandTotal.coerceAtLeast(0.0)
  val outstanding = invoice.outstandingAmount.coerceAtLeast(0.0)
  return ReturnPreview(
      currency = normalizeCurrency(invoice.currency),
      returnTotal = roundToCurrency(total),
      projectedOutstanding = roundToCurrency((outstanding - total).coerceAtLeast(0.0)),
  )
}

internal fun buildReturnPostMessage(
    creditNoteName: String?,
    preview: ReturnPreview?,
    applyRefund: Boolean,
    isPartial: Boolean,
): String {
  val base =
      when {
        !creditNoteName.isNullOrBlank() && isPartial ->
            "Retorno parcial registrado como $creditNoteName."
        !creditNoteName.isNullOrBlank() -> "Retorno registrado como $creditNoteName."
        isPartial -> "Retorno parcial registrado."
        else -> "Retorno registrado."
      }
  val destination = if (applyRefund) "reembolso" else "crédito a favor"
  val projection =
      preview
          ?.let {
            " Monto devuelto estimado: ${formatMoney(it.currency, it.returnTotal)}. " +
                "Saldo estimado tras retorno: ${
                    formatMoney(it.currency, it.projectedOutstanding ?: 0.0)
                }."
          }
          .orEmpty()
  val notice =
      " Nota: en ERPNext el saldo visible puede mantenerse temporalmente hasta la conciliación o cierre de caja."
  return "$base Se aplicó como $destination.$projection$notice"
}

internal fun formatMoney(currency: String, amount: Double): String {
  val symbol = currency.toCurrencySymbol().ifBlank { currency }
  return "$symbol ${formatDoubleToString(amount, 2)}"
}
