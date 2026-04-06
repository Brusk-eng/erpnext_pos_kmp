package com.erpnext.pos.views.paymententry

import com.erpnext.pos.utils.view.DateTimeProvider
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class PaymentEntryFieldErrors(
    val referenceNoError: String? = null,
    val referenceDateError: String? = null,
    val userMessage: String? = null,
)

internal fun generateAutomaticReference(
    entryType: PaymentEntryType,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
): String {
  val prefix =
      when (entryType) {
        PaymentEntryType.InternalTransfer -> "TRF"
        PaymentEntryType.Pay -> "CMP"
        PaymentEntryType.Receive -> "REF"
      }
  val compactDate = DateTimeProvider.todayDate().replace("-", "")
  val sequence = (nowMillis % 1_000_000L).toString().padStart(6, '0')
  return "$prefix-$compactDate-$sequence"
}

internal fun shouldReplaceWithAutoReference(
    currentReference: String,
    previousAutoReference: String,
): Boolean = currentReference.isBlank() || currentReference == previousAutoReference

internal fun roundMoney(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

internal fun buildNarration(state: PaymentEntryState): String {
  val segments = buildList {
    state.concept.trim().takeIf { it.isNotBlank() }?.let { add("Concepto: $it") }
    state.party.trim().takeIf { it.isNotBlank() }?.let { add("Tercero: $it") }
    state.referenceNo.trim().takeIf { it.isNotBlank() }?.let { add("Ref: $it") }
    state.notes.trim().takeIf { it.isNotBlank() }?.let { add("Nota: $it") }
  }
  return segments.joinToString(" | ")
}

internal fun reallocateSupplierInvoices(
    invoices: List<SupplierPendingInvoiceUi>,
    amountText: String,
): List<SupplierPendingInvoiceUi> {
  var remaining = amountText.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  return invoices.map { row ->
    if (!row.selected || row.conversionError) {
      row.copy(allocatedAmountPaymentCurrency = 0.0, allocatedAmountInvoiceCurrency = 0.0)
    } else {
      val rate = row.paymentToInvoiceRate ?: 1.0
      val maxPaymentAmount =
          row.outstandingAmountPaymentCurrency
              ?: if (rate > 0.0) row.outstandingAmountInvoiceCurrency / rate else 0.0
      val allocatedPayment = minOf(remaining, maxPaymentAmount.coerceAtLeast(0.0))
      val allocatedInvoice =
          (allocatedPayment * rate).coerceAtMost(
              row.outstandingAmountInvoiceCurrency.coerceAtLeast(0.0)
          )
      remaining = (remaining - allocatedPayment).coerceAtLeast(0.0)
      row.copy(
          allocatedAmountPaymentCurrency = roundMoney(allocatedPayment),
          allocatedAmountInvoiceCurrency = roundMoney(allocatedInvoice),
      )
    }
  }
}

internal fun resolveOutstandingInPaymentCurrency(row: SupplierPendingInvoiceUi): Double {
  val paymentOutstanding = row.outstandingAmountPaymentCurrency
  if (paymentOutstanding != null) return paymentOutstanding.coerceAtLeast(0.0)

  val rate = row.paymentToInvoiceRate
  if (rate != null && rate > 0.0) {
    return (row.outstandingAmountInvoiceCurrency / rate).coerceAtLeast(0.0)
  }
  return row.outstandingAmountInvoiceCurrency.coerceAtLeast(0.0)
}

internal fun isSupplierInvoiceClosed(status: String?): Boolean {
  val normalized = status?.trim()?.lowercase().orEmpty()
  if (normalized.isBlank()) return false
  return normalized == "paid" || normalized == "cancelled" || normalized == "canceled"
}

internal fun resolveFieldErrors(json: Json, rawMessage: String?): PaymentEntryFieldErrors {
  val text = rawMessage?.trim().orEmpty()
  if (text.isBlank()) return PaymentEntryFieldErrors()

  val extractedMessage =
      runCatching {
            val root = json.parseToJsonElement(text).jsonObject
            root["message"]
                ?.jsonObject
                ?.get("error")
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
          }
          .getOrNull()
          ?.trim()
          .orEmpty()

  val effective = extractedMessage.ifBlank { text }
  val normalized = effective.lowercase()
  val mentionsReferenceNo =
      normalized.contains("nro de referencia") || normalized.contains("numero de referencia")
  val mentionsReferenceDate = normalized.contains("fecha de referencia")

  if (!mentionsReferenceNo && !mentionsReferenceDate) {
    return PaymentEntryFieldErrors(userMessage = extractedMessage.ifBlank { null })
  }

  return PaymentEntryFieldErrors(
      referenceNoError =
          if (mentionsReferenceNo) "Número de referencia requerido para transacción bancaria."
          else null,
      referenceDateError =
          if (mentionsReferenceDate) "Fecha de referencia requerida para transacción bancaria."
          else null,
      userMessage = extractedMessage.ifBlank { effective },
  )
}
