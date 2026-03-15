package com.erpnext.pos.views.home

data class HomeMetrics(
    val totalSalesToday: Double = 0.0,
    val invoicesToday: Int = 0,
    val avgTicket: Double = 0.0,
    val customersToday: Int = 0,
    val outstandingTotal: Double = 0.0,
    val currencyMetrics: List<CurrencyHomeMetric> = emptyList(),
)

data class CurrencyHomeMetric(
    val currency: String,
    val totalSalesToday: Double,
    val invoicesToday: Int,
    val avgTicket: Double,
    val customersToday: Int,
    val outstandingTotal: Double,
)
// 76