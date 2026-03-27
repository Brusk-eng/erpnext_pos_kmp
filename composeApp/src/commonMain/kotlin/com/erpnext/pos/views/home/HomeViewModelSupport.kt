package com.erpnext.pos.views.home

import com.erpnext.pos.domain.usecases.HomeLiveShiftMetrics
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.sync.GateResult
import com.erpnext.pos.utils.normalizeCurrency

internal fun GateResult.requireReady() {
  when (this) {
    is GateResult.Failed -> error(reason)
    is GateResult.Pending -> error(reason)
    GateResult.Ready -> Unit
  }
}

internal fun mergeLiveShiftMetrics(
    current: HomeMetrics,
    live: HomeLiveShiftMetrics,
): HomeMetrics {
  val liveByCurrency = live.byCurrency.associateBy { normalizeCurrency(it.currency) }
  val existingByCurrency = current.currencyMetrics.associateBy { normalizeCurrency(it.currency) }
  val allCurrencyKeys = (existingByCurrency.keys + liveByCurrency.keys).toList().sorted()

  val mergedCurrencies =
      allCurrencyKeys.map { key ->
        val existing = existingByCurrency[key]
        val liveCurrency = liveByCurrency[key]
        if (existing != null) {
          existing.copy(
              totalSalesToday = liveCurrency?.totalSalesToday ?: 0.0,
              invoicesToday = liveCurrency?.invoicesToday ?: 0,
              avgTicket = liveCurrency?.avgTicket ?: 0.0,
              customersToday = liveCurrency?.customersToday ?: 0,
          )
        } else {
          CurrencyHomeMetric(
              currency = liveCurrency?.currency ?: key,
              totalSalesToday = liveCurrency?.totalSalesToday ?: 0.0,
              invoicesToday = liveCurrency?.invoicesToday ?: 0,
              avgTicket = liveCurrency?.avgTicket ?: 0.0,
              customersToday = liveCurrency?.customersToday ?: 0,
              outstandingTotal = 0.0,
          )
        }
      }

  return current.copy(
      totalSalesToday = live.totalSalesToday,
      invoicesToday = live.invoicesToday,
      avgTicket = live.avgTicket,
      customersToday = live.customersToday,
      currencyMetrics = mergedCurrencies,
  )
}

internal fun buildOpeningProfileState(
    profileId: String,
    company: String,
    baseCurrency: String,
    methods: List<ResolvedPaymentMethod>,
    cashMethodsByCurrency: Map<String, List<ResolvedPaymentMethod>>,
): CashboxOpeningProfileState =
    CashboxOpeningProfileState(
        profileId = profileId,
        company = company,
        baseCurrency = baseCurrency,
        methods = methods,
        cashMethodsByCurrency = cashMethodsByCurrency,
        isLoading = false,
        error = null,
    )

internal fun String?.normalizedOpeningEntryId(): String? = this?.trim()?.takeIf { it.isNotBlank() }
