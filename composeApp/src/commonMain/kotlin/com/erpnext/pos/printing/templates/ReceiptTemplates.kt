package com.erpnext.pos.printing.templates

import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import com.erpnext.pos.domain.printing.model.ReceiptTotalsLabels
import com.erpnext.pos.localization.AppLanguage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ReceiptTemplateMetadata(
    val companyName: String? = null,
    val cashierName: String? = null,
    val customerName: String? = null,
    val posProfileId: String? = null,
    val logoUrl: String? = null,
)

private const val POS_RECEIPT_LABEL = "POS:"

@OptIn(ExperimentalTime::class)
fun buildPendingInvoicePaymentReceipt(
    invoiceId: String,
    amount: Double,
    currencyCode: String,
    modeOfPayment: String,
    referenceNo: String?,
    notes: String?,
    pendingAfterPayment: Double? = null,
    language: AppLanguage = AppLanguage.Spanish,
    metadata: ReceiptTemplateMetadata = ReceiptTemplateMetadata(),
): ReceiptDocument {
  val labels = receiptText(language)
  val rightAmount = formatReceiptAmount(amount, currencyCode)
  val pendingAfterPaymentText =
      pendingAfterPayment?.let { formatReceiptAmount(it.coerceAtLeast(0.0), currencyCode) }
  val companyLabel = metadata.companyName?.trim().orEmpty()
  val customerLabel = metadata.customerName?.trim().orEmpty()
  val cashierLabel = metadata.cashierName?.trim().orEmpty()
  val posProfileLabel = metadata.posProfileId?.trim().orEmpty()
  val body =
      buildList {
        add(ReceiptLine(labels.invoice, invoiceId))
        customerLabel
            .takeIf { it.isNotBlank() }
            ?.let { add(ReceiptLine(labels.customer, it.take(32))) }
        cashierLabel.takeIf { it.isNotBlank() }?.let { add(ReceiptLine(labels.cashier, it.take(28))) }
        posProfileLabel
            .takeIf { it.isNotBlank() }
            ?.let { add(ReceiptLine(labels.posProfile, it.take(28))) }
        add(ReceiptLine(labels.modeOfPay, modeOfPayment.ifBlank { labels.notAvailable }))
        add(ReceiptLine(labels.amount, rightAmount))
        add(ReceiptLine(labels.date, nowDateLabel()))
        add(ReceiptLine(labels.day, nowWeekdayLabel(language)))
        referenceNo
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { add(ReceiptLine(labels.reference, it.take(30))) }
        notes?.trim()?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine(labels.note, it.take(40))) }
      }

  return ReceiptDocument(
      documentId = "pending-payment-${Clock.System.now().toEpochMilliseconds()}",
      header =
          ReceiptSection(
              lines =
                  buildList {
                    add(companyLabel.ifBlank { "ERPNext POS" })
                    add(labels.pendingInvoicePaymentTitle)
                    add(invoiceId.take(36))
                  },
              alignment = PrintAlignment.CENTER,
          ),
      bodyLines = body,
      totals = ReceiptTotals(total = rightAmount),
      totalsLabels =
          ReceiptTotalsLabels(
              subtotal = labels.subtotal,
              tax = labels.tax,
              total = labels.total,
          ),
      footer =
          ReceiptSection(
              lines =
                  buildList {
                    add(labels.paymentRegisteredLocally)
                    pendingAfterPaymentText?.let { add("${labels.pendingAfterPayment}: $it") }
                  },
              alignment = PrintAlignment.CENTER,
          ),
  )
}

@OptIn(ExperimentalTime::class)
fun buildBillingSaleReceipt(
    invoiceLabel: String,
    customerLabel: String,
    currencyCode: String,
    itemLines: List<Pair<String, Double>>,
    subtotal: Double,
    taxes: Double,
    total: Double,
    paidAmount: Double,
    balanceDue: Double,
    language: AppLanguage = AppLanguage.Spanish,
    metadata: ReceiptTemplateMetadata = ReceiptTemplateMetadata(),
): ReceiptDocument {
  val labels = receiptText(language)
  val companyLabel = metadata.companyName?.trim().orEmpty()
  val cashierLabel = metadata.cashierName?.trim().orEmpty()
  val posProfileLabel = metadata.posProfileId?.trim().orEmpty()
  val trimmedCustomerLabel = customerLabel.trim().ifBlank { labels.notAvailable }
  val normalizedItemLines = itemLines.filter { it.first.isNotBlank() }
  val body =
      buildList {
        add(ReceiptLine(labels.invoice, invoiceLabel))
        add(ReceiptLine(labels.customer, trimmedCustomerLabel.take(28)))
        add(ReceiptLine(labels.date, nowDateLabel()))
        add(ReceiptLine(labels.day, nowWeekdayLabel(language)))
        cashierLabel.takeIf { it.isNotBlank() }?.let { add(ReceiptLine(labels.cashier, it.take(28))) }
        posProfileLabel
            .takeIf { it.isNotBlank() }
            ?.let { add(ReceiptLine(POS_RECEIPT_LABEL, it.take(28))) }
        if (normalizedItemLines.isNotEmpty()) {
          add(ReceiptLine(""))
        }
        normalizedItemLines.forEach { (label, amount) ->
          add(ReceiptLine(label.take(28), formatReceiptAmount(amount, currencyCode)))
        }
        if (normalizedItemLines.isNotEmpty()) {
          add(ReceiptLine(""))
        }
      }

  return ReceiptDocument(
      documentId = "billing-sale-${Clock.System.now().toEpochMilliseconds()}",
      header =
          ReceiptSection(
              lines =
                  buildList {
                    add(companyLabel.ifBlank { "ERPNext POS" })
                    add(labels.salesReceiptTitle)
                    add(invoiceLabel.take(36))
                    add("")
                  },
              alignment = PrintAlignment.CENTER,
          ),
      bodyLines = body,
      totals =
          ReceiptTotals(
              subTotal = formatReceiptAmount(subtotal, currencyCode),
              tax = formatReceiptAmount(taxes, currencyCode),
              total = formatReceiptAmount(total, currencyCode),
          ),
      totalsLabels =
          ReceiptTotalsLabels(
              subtotal = labels.subtotal,
              tax = labels.tax,
              total = labels.total,
          ),
      footer =
          ReceiptSection(
              lines =
                  listOf(
                      "${labels.paid}: ${formatReceiptAmount(paidAmount, currencyCode)}",
                      "${labels.pendingAfterPayment}: ${formatReceiptAmount(balanceDue, currencyCode)}",
                  ),
              alignment = PrintAlignment.CENTER,
          ),
  )
}

private fun formatReceiptAmount(amount: Double, currencyCode: String): String {
  val scaled = kotlin.math.round(amount * 100.0).toLong()
  val sign = if (scaled < 0) "-" else ""
  val absScaled = kotlin.math.abs(scaled)
  val whole = absScaled / 100
  val decimal = (absScaled % 100).toString().padStart(2, '0')
  return "${currencyCode.uppercase()} $sign$whole.$decimal"
}

private fun nowDateLabel(): String {
  val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  return ldt.date.toString()
}

private fun nowWeekdayLabel(language: AppLanguage): String {
  val ldt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  return weekdayLabel(ldt.dayOfWeek, language)
}

private fun weekdayLabel(dayOfWeek: DayOfWeek, language: AppLanguage): String =
    when (language) {
      AppLanguage.English ->
          when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Monday"
            DayOfWeek.TUESDAY -> "Tuesday"
            DayOfWeek.WEDNESDAY -> "Wednesday"
            DayOfWeek.THURSDAY -> "Thursday"
            DayOfWeek.FRIDAY -> "Friday"
            DayOfWeek.SATURDAY -> "Saturday"
            DayOfWeek.SUNDAY -> "Sunday"
          }
      AppLanguage.Spanish ->
          when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Lunes"
            DayOfWeek.TUESDAY -> "Martes"
            DayOfWeek.WEDNESDAY -> "Miercoles"
            DayOfWeek.THURSDAY -> "Jueves"
            DayOfWeek.FRIDAY -> "Viernes"
            DayOfWeek.SATURDAY -> "Sabado"
            DayOfWeek.SUNDAY -> "Domingo"
          }
    }

private data class ReceiptText(
    val invoice: String,
    val customer: String,
    val cashier: String,
    val posProfile: String,
    val modeOfPay: String,
    val amount: String,
    val date: String,
    val day: String,
    val reference: String,
    val note: String,
    val pendingInvoicePaymentTitle: String,
    val salesReceiptTitle: String,
    val paymentRegisteredLocally: String,
    val pendingAfterPayment: String,
    val paid: String,
    val subtotal: String,
    val tax: String,
    val total: String,
    val notAvailable: String,
)

private fun receiptText(language: AppLanguage): ReceiptText =
    if (language == AppLanguage.English) {
      ReceiptText(
          invoice = "Invoice",
          customer = "Customer",
          cashier = "Cashier",
          posProfile = "POS Profile",
          modeOfPay = "Mode Of Pay",
          amount = "Amount",
          date = "Date",
          day = "Day",
          reference = "Reference",
          note = "Note",
          pendingInvoicePaymentTitle = "Pending Invoice Payment",
          salesReceiptTitle = "Sales Receipt",
          paymentRegisteredLocally = "Payment registered locally",
          pendingAfterPayment = "Pending after payment",
          paid = "Paid",
          subtotal = "Subtotal",
          tax = "Tax",
          total = "TOTAL",
          notAvailable = "N/A",
      )
    } else {
      ReceiptText(
          invoice = "Factura",
          customer = "Cliente",
          cashier = "Cajero",
          posProfile = "POS Profile",
          modeOfPay = "Modo de Pago",
          amount = "Monto",
          date = "Fecha",
          day = "Dia",
          reference = "Referencia",
          note = "Nota",
          pendingInvoicePaymentTitle = "Pago de Factura Pendiente",
          salesReceiptTitle = "Recibo de Venta",
          paymentRegisteredLocally = "Pago registrado localmente",
          pendingAfterPayment = "Pendiente despues del pago",
          paid = "Pagado",
          subtotal = "Subtotal",
          tax = "Impuesto",
          total = "TOTAL",
          notAvailable = "N/D",
      )
    }
