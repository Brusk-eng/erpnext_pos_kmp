package com.erpnext.pos.views.billing

import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentScheduleDto
import com.erpnext.pos.remoteSource.sdk.toUserMessage
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundForCurrency
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.salesflow.SalesFlowSource

internal fun buildInvoiceItems(
    current: BillingState.Success,
    context: POSContext,
    invoiceCurrency: String,
): MutableList<SalesInvoiceItemDto> {
  val source = current.salesFlowContext
  val sourceId = source?.sourceId
  val salesOrderId = if (source?.sourceType == SalesFlowSource.SalesOrder) sourceId else null
  val deliveryNoteId = if (source?.sourceType == SalesFlowSource.DeliveryNote) sourceId else null

  return current.cartItems
      .map { cart ->
        val rate = roundForCurrency(cart.price, invoiceCurrency)
        val amount = roundForCurrency(cart.quantity * rate, invoiceCurrency)
        SalesInvoiceItemDto(
            itemCode = cart.itemCode,
            itemName = cart.name,
            qty = cart.quantity,
            rate = rate,
            amount = amount,
            discountPercentage = null,
            warehouse = context.warehouse,
            incomeAccount = context.incomeAccount,
            salesOrder = salesOrderId,
            deliveryNote = deliveryNoteId,
        )
      }
      .toMutableList()
}

internal fun buildInvoiceRemarks(
    current: BillingState.Success,
    paymentLines: List<PaymentLine>,
    shippingAmount: Double,
    baseCurrency: String,
): String? {
  return buildList {
        current.salesFlowContext?.let { context ->
          val label = context.sourceLabel()
          if (label != null && context.sourceType != SalesFlowSource.Customer) {
            val sourceText =
                context.sourceId?.let { "Source: $label (ID: $it)" } ?: "Origen: $label"
            add(sourceText)
          }
        }
        addAll(
            paymentLines.mapNotNull { line ->
              if (line.currency.equals(baseCurrency, ignoreCase = true)) null
              else
                  "Moneda de pago (${line.modeOfPayment}): ${line.currency}, tipo de cambio: ${line.exchangeRate}"
            }
        )
        addAll(
            paymentLines.mapNotNull { line ->
              line.referenceNumber
                  ?.takeIf { it.isNotBlank() }
                  ?.let { "Referencia (${line.modeOfPayment}): $it" }
            }
        )
        if (current.discountCode.isNotBlank()) add("Código de descuento: ${current.discountCode}")
        if (shippingAmount > 0.0) add("Envío: $shippingAmount")
      }
      .joinToString(separator = "; ")
      .takeIf { it.isNotBlank() }
}

internal fun resolveItemPriceForInvoiceCurrency(
    item: ItemBO,
    invoiceCurrency: String?,
    rateToInvoice: Double?,
    posCurrency: String?,
    exchangeRate: Double?,
): Double {
  val itemCurrency =
      item.currency?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
          ?: normalizeCurrency(posCurrency)
  val invoice = normalizeCurrency(invoiceCurrency)
  if (itemCurrency.isBlank() || invoice.isBlank()) {
    return roundForCurrency(item.price, invoiceCurrency)
  }
  if (itemCurrency.equals(invoice, ignoreCase = true)) return item.price

  rateToInvoice
      ?.takeIf { it > 0.0 }
      ?.let { directRate ->
        return roundForCurrency(item.price * directRate, invoiceCurrency)
      }

  val rate = exchangeRate?.takeIf { it > 0.0 } ?: return roundForCurrency(item.price, invoiceCurrency)
  val pos = normalizeCurrency(posCurrency)
  if (itemCurrency.equals("USD", true) && invoice.equals(pos, true)) {
    val converted = if (rate > 1.0) item.price * rate else item.price / rate
    return roundForCurrency(converted, invoiceCurrency)
  }
  if (itemCurrency.equals(pos, true) && invoice.equals("USD", true)) {
    val converted = if (rate > 1.0) item.price / rate else item.price * rate
    return roundForCurrency(converted, invoiceCurrency)
  }
  return roundForCurrency(item.price, invoiceCurrency)
}

internal fun resolveDueDate(
    isCreditSale: Boolean,
    postingDate: String,
    term: PaymentTermBO?,
): String {
  if (!isCreditSale) return postingDate
  val resolvedTerm = term ?: error("El término de pago es obligatorio para ventas a crédito.")
  val withMonths = com.erpnext.pos.utils.view.DateTimeProvider.addMonths(postingDate, resolvedTerm.creditMonths ?: 0)
  return com.erpnext.pos.utils.view.DateTimeProvider.addDays(withMonths, resolvedTerm.creditDays ?: 0)
}

internal fun buildPaymentSchedule(
    isCreditSale: Boolean,
    term: PaymentTermBO?,
    dueDate: String,
): List<SalesInvoicePaymentScheduleDto> {
  if (!isCreditSale) return emptyList()
  val resolvedTerm = term ?: error("El término de pago es obligatorio para ventas a crédito.")
  val portion = resolvedTerm.invoicePortion ?: 0.0
  if (portion <= 0.0) return emptyList()
  return listOf(
      SalesInvoicePaymentScheduleDto(
          paymentTerm = resolvedTerm.name,
          invoicePortion = portion,
          dueDate = dueDate,
          modeOfPayment = resolvedTerm.modeOfPayment,
      )
  )
}

internal fun convertSourceDocument(
    source: com.erpnext.pos.domain.models.SourceDocumentOption,
    baseCurrency: String,
    rate: Double,
): com.erpnext.pos.domain.models.SourceDocumentOption {
  if (rate == 1.0) return source
  val convertedTotals =
      source.totals?.let { totals ->
        totals.copy(
            netTotal = totals.netTotal?.let { it * rate },
            grandTotal = totals.grandTotal?.let { it * rate },
            taxTotal = totals.taxTotal?.let { it * rate },
            currency = baseCurrency,
        )
      }
  val convertedItems =
      source.items.map { item -> item.copy(rate = item.rate * rate, amount = item.amount * rate) }
  return source.copy(items = convertedItems, totals = convertedTotals)
}

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

internal fun resetFromSource(current: BillingState.Success): BillingState.Success {
  return current.copy(
      cartItems = emptyList(),
      subtotal = 0.0,
      taxes = 0.0,
      discount = 0.0,
      discountCode = "",
      manualDiscountAmount = 0.0,
      manualDiscountPercent = 0.0,
      shippingAmount = 0.0,
      selectedDeliveryCharge = null,
      total = 0.0,
      isCreditSale = false,
      selectedPaymentTerm = null,
      paymentLines = emptyList(),
      paidAmountBase = 0.0,
      balanceDueBase = 0.0,
      changeDueBase = 0.0,
      creditSaleTooltipMessage = null,
      paymentErrorMessage = null,
      cartErrorMessage = null,
      sourceDocument = null,
      isSourceDocumentApplied = false,
  )
}

internal fun formatQty(value: Double): String {
  return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

internal fun buildQtyErrorMessage(
    itemName: String,
    maxQty: Double,
    tr: (spanish: String, english: String) -> String,
): String {
  return tr(
      "Solo hay ${formatQty(maxQty)} disponibles para $itemName.",
      "Only ${formatQty(maxQty)} units are available for $itemName.",
  )
}

internal fun shouldSuggestRateSync(error: Throwable): Boolean {
  val message = error.message ?: return false
  return message.contains("tasa de cambio", ignoreCase = true) ||
      message.contains("exchange rate", ignoreCase = true)
}

internal fun buildFinalizeErrorMessage(
    current: BillingState.Success?,
    error: Throwable,
    shouldSuggestRateSync: Boolean,
    roundForCurrency: (Double, String?) -> Double,
): String {
  val baseMessage = error.toUserMessage("No se pudo crear la factura.")
  val sourceInfo =
      current?.salesFlowContext?.sourceLabel()?.let { label ->
        current.salesFlowContext.sourceId?.let { id -> "$label ($id)" } ?: label
      } ?: "N/A"
  val customerInfo = current?.selectedCustomer?.name ?: "N/A"
  val totalInfo = current?.total?.let { roundForCurrency(it, current.currency) } ?: 0.0
  val paidInfo = current?.paidAmountBase?.let { roundForCurrency(it, current.currency) } ?: 0.0
  val linesInfo = current?.paymentLines?.size ?: 0
  val creditInfo = current?.isCreditSale ?: false
  val errorType = error::class.simpleName ?: "Error"
  return buildString {
    append(baseMessage)
    append(" | Tipo: ").append(errorType)
    append(" | Cliente: ").append(customerInfo)
    append(" | Origen: ").append(sourceInfo)
    append(" | Total: ").append(totalInfo)
    append(" | Pagado: ").append(paidInfo)
    append(" | Pagos: ").append(linesInfo)
    append(" | Crédito: ").append(creditInfo)
    if (shouldSuggestRateSync) {
      append(" | Sugerencia: sincroniza tasas de cambio e intenta de nuevo")
    }
  }
}
