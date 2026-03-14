package com.erpnext.pos.utils

data class CurrencyDenominations(val bills: List<Double>, val coins: List<Double>)

object DenominationCatalog {
    private val catalog: Map<String, CurrencyDenominations> =
        mapOf(
            "USD" to
                    CurrencyDenominations(
                        bills = listOf(100.0, 50.0, 20.0, 10.0, 5.0, 2.0, 1.0),
                        coins = emptyList(),
                    ),
            "NIO" to
                    CurrencyDenominations(
                        bills = listOf(1000.0, 500.0, 200.0, 100.0, 50.0, 20.0, 10.0),
                        coins = listOf(5.0, 1.0, 0.5, 0.25),
                    ),
        )

    fun forCurrency(code: String): CurrencyDenominations =
        catalog[normalizeCurrency(code)] ?: catalog["USD"]!!
}

fun normalizeCurrency(code: String): String =
    when (code.uppercase()) {
        "C$",
        "CORDOBA",
        "CORDOBAS",
        "NIO" -> "NIO"

        "$",
        "USD",
        "DOLAR",
        "DOLARES" -> "USD"

        else -> code.uppercase()
    }
