package com.erpnext.pos.views.payment

import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.CurrencySpec
import com.erpnext.pos.utils.resolvePaymentToReceivableRate
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import kotlin.math.pow
import kotlin.math.round

internal suspend fun resolvePaidFromAccount(
    invoice: SalesInvoiceDto,
    invoiceNameForLocal: String,
    context: POSContext,
    customer: CustomerBO,
    receivableCurrency: String?,
    invoiceLocalSource: InvoiceLocalSource,
): String? {
  invoice.debitTo?.takeIf { it.isNotBlank() }?.let { return it }

  invoice.name?.takeIf { it.isNotBlank() }?.let { remoteName ->
    invoiceLocalSource
        .getInvoiceByName(remoteName)
        ?.invoice
        ?.debitTo
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }
  }

  invoiceLocalSource
      .getInvoiceByName(invoiceNameForLocal)
      ?.invoice
      ?.debitTo
      ?.takeIf { it.isNotBlank() }
      ?.let { return it }

  return invoiceLocalSource.findRecentDebitTo(
      company = context.company,
      customer = customer.name,
      partyAccountCurrency = receivableCurrency,
  )
}

internal suspend fun capPaymentLineToOutstanding(
    line: PaymentLine,
    remainingOutstandingRc: Double?,
    receivableCurrency: String?,
    paidToCurrency: String?,
    invoiceCurrency: String?,
    rateInvToRc: Double?,
    cacheForReceivable: Map<String, Double>,
    context: POSContext,
    currencySpecs: Map<String, CurrencySpec>,
    resolveRateBetweenCurrencies: suspend (fromCurrency: String, toCurrency: String, context: POSContext) -> Double?,
): PaymentLine {
  val outstanding = remainingOutstandingRc?.takeIf { it > 0.0 } ?: return line
  val rc = normalizeCurrency(receivableCurrency)
  val pay = normalizeCurrency(paidToCurrency)
  if (rc.isBlank() || pay.isBlank()) return line

  val paySpec = currencySpecs[pay] ?: CurrencySpec(code = pay, minorUnits = 2, cashScale = 2)
  val maxEntered =
      if (pay.equals(rc, ignoreCase = true)) {
        outstanding
      } else {
        val rate =
            resolvePaymentToReceivableRate(
                paymentCurrency = pay,
                invoiceCurrency = normalizeCurrency(invoiceCurrency),
                receivableCurrency = rc,
                paymentToInvoiceRate = line.exchangeRate,
                invoiceToReceivableRate = rateInvToRc,
            )
                ?: cacheForReceivable[pay]
                ?: resolveRateBetweenCurrencies(pay, rc, context)
        if (rate == null || rate <= 0.0) return line
        outstanding / rate
      }

  val scale = paySpec.cashScale.coerceAtMost(paySpec.minorUnits)
  val capped = roundToScale(maxEntered, scale)
  if (line.enteredAmount <= capped + 0.000001) return line

  return line.copy(
      enteredAmount = capped,
      baseAmount = roundToCurrency(capped * line.exchangeRate),
  )
}

internal fun roundToScale(value: Double, scale: Int): Double {
  if (!value.isFinite()) return value
  if (scale <= 0) return round(value)
  val factor = 10.0.pow(scale.toDouble())
  return round(value * factor) / factor
}

internal suspend fun resolveRateBetweenCurrencies(
    fromCurrency: String,
    toCurrency: String,
    context: POSContext,
    exchangeRateRepository: ExchangeRateRepository,
): Double? {
  val from = normalizeCurrency(fromCurrency)
  val to = normalizeCurrency(toCurrency)
  if (from.equals(to, ignoreCase = true)) return 1.0

  exchangeRateRepository.getLocalRate(from, to)?.takeIf { it > 0.0 }?.let { return it }
  exchangeRateRepository.getLocalRate(to, from)?.takeIf { it > 0.0 }?.let { return 1 / it }

  val ctxCurrency = normalizeCurrency(context.currency)
  val ctxRate = context.exchangeRate
  if (ctxRate > 0.0) {
    if (from.equals(ctxCurrency, true) && to.equals("USD", true)) return 1 / ctxRate
    if (from.equals("USD", true) && to.equals(ctxCurrency, true)) return ctxRate
  }
  return null
}
