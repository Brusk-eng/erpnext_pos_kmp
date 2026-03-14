package com.erpnext.pos.utils

expect fun formatAmount(symbol: String, amount: Double): String

fun formatCurrency(code: String, amount: Double): String {
    val symbol = code.toCurrencySymbol().ifBlank { code }
    val normalized = normalizeCurrency(code).uppercase()
    val display = roundForCurrency(amount, normalized)

    val decimals =
        when (normalized) {
            "USD",
            "NIO" -> 2
            else -> 2
        }
    val formatted = formatAmount(symbol, display)
    return if (decimals == 2) formatted else formatAmount(symbol, roundToCurrency(display, decimals))
}
