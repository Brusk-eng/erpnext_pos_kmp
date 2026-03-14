package com.erpnext.pos.views.home

data class HomeMetrics(
    val totalSalesToday: Double = 0.0,
    val invoicesToday: Int = 0,
    val avgTicket: Double = 0.0,
    val customersToday: Int = 0,
    val outstandingTotal: Double = 0.0,
    val inventoryAlerts: List<InventoryAlert> = emptyList(),
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

data class InventoryAlert(
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val status: InventoryAlertStatus,
    val reorderLevel: Double?,
    val reorderQty: Double?,
)

enum class InventoryAlertStatus {
  CRITICAL,
  LOW,
}
// 76