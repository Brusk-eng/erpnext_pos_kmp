@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.views.home.HomeMetrics
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class HomeMetricInput(
    val days: Int = 7,
    val nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val openingEntryId: String? = null,
)

class LoadHomeMetricsUseCase(private val salesInvoiceDao: SalesInvoiceDao) :
    UseCase<HomeMetricInput, HomeMetrics>() {

  override suspend fun useCaseFunction(input: HomeMetricInput): HomeMetrics {
    val tz = TimeZone.currentSystemDefault()
    val today = Instant.fromEpochMilliseconds(input.nowMillis).toLocalDateTime(tz).date
    val todayString = today.toString()
    val dayOpeningEntryId = input.openingEntryId?.trim()?.takeIf { it.isNotBlank() }

    // Con turno activo, los KPIs y ventanas de tendencia se calculan en el mismo alcance
    // para evitar mezclar ventas globales con ventas del turno.
    val trendOpeningEntryId = dayOpeningEntryId
    val shiftSummaryToday =
        trendOpeningEntryId?.let { opening ->
          salesInvoiceDao.getShiftTodaySummary(todayString, opening)
        }
    val totalSalesToday =
        shiftSummaryToday?.totalSalesToday
            ?: (salesInvoiceDao.getTotalSalesForDate(todayString, trendOpeningEntryId) ?: 0.0)
    val invoicesToday =
        shiftSummaryToday?.invoicesToday
            ?: salesInvoiceDao.getSalesCountForDate(todayString, trendOpeningEntryId)
    val customersToday =
        shiftSummaryToday?.customersToday
            ?: salesInvoiceDao.getDistinctCustomersForDate(todayString, trendOpeningEntryId)
    val outstanding = salesInvoiceDao.getTotalOutstanding(null) ?: 0.0
    val avgTicket = if (invoicesToday > 0) totalSalesToday / invoicesToday else 0.0

    return HomeMetrics(
        totalSalesToday = totalSalesToday,
        invoicesToday = invoicesToday,
        avgTicket = avgTicket,
        customersToday = customersToday,
        outstandingTotal = outstanding
    )
  }
}
// 277