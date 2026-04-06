package com.erpnext.pos.views.reconciliation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localization.ReconciliationStrings
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.components.DenominationUi
import com.erpnext.pos.views.components.buildDenominationsForCurrency
import kotlin.math.abs

private const val CURRENCY_MINOR_UNITS_FACTOR = 100.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    state: ReconciliationState,
    mode: ReconciliationMode,
    closeState: CloseCashboxState,
    actions: ReconciliationAction,
) {
  val appStrings = LocalAppStrings.current
  val strings = appStrings.reconciliation
  val summary = (state as? ReconciliationState.Success)?.summary
  val formatter = remember { DecimalFormatter() }
  // Conteo por moneda (usa monedas de métodos de efectivo si están configuradas)
  val countCurrencies =
      remember(summary?.currency, summary?.cashCurrencies) {
        val fromProfile = summary?.cashCurrencies?.map { it.uppercase() }?.distinct().orEmpty()
        if (fromProfile.isNotEmpty()) {
          fromProfile.sorted()
        } else {
          buildList {
                summary?.currency?.uppercase()?.let { add(it) }
                add("USD")
                add("NIO")
              }
              .distinct()
        }
      }
  var selectedCountCurrency by
      remember(summary?.currency, countCurrencies) {
        mutableStateOf(countCurrencies.firstOrNull() ?: summary?.currency?.uppercase() ?: "USD")
      }
  val countState =
      remember(summary?.openingEntryId) { mutableStateMapOf<String, List<DenominationUi>>() }

  fun ensureDenomsFor(currency: String) {
    if (!countState.containsKey(currency)) {
      val symbol =
          when {
            currency.equals(summary?.currency, ignoreCase = true) -> summary?.currencySymbol
            else -> currency.toCurrencySymbol().ifBlank { null }
          }
      countState[currency] = buildDenominationsForCurrency(currency, symbol, formatter)
    }
  }
  countCurrencies.forEach { ensureDenomsFor(it) }
  var denominations by
      remember(selectedCountCurrency, summary?.openingEntryId) {
        mutableStateOf(countState[selectedCountCurrency] ?: emptyList())
      }
  val cashTotal = if (summary == null) 0.0 else denominations.sumOf { it.value * it.count }

  val expectedCashByMode: Map<String, Double> =
      summary?.let {
        val cashKeys = it.cashModes.ifEmpty { it.expectedByMode.keys }
        it.expectedByMode.filterKeys { key -> cashKeys.contains(key) }
      } ?: emptyMap()
  val expectedCashByCurrency: Map<String, Double> =
      summary?.let { s -> buildExpectedCashByCurrency(summary = s) } ?: emptyMap()
  // Totales contados por moneda
  val cashTotalsByCurrency =
      countState.mapValues { entry -> entry.value.sumOf { it.value * it.count } }
  // Mapear conteos a modos de pago según moneda, sin duplicar el total en múltiples modos.
  val countedByMode = run {
    val cashModes = summary?.cashModes?.ifEmpty { expectedCashByMode.keys } ?: emptySet()
    val fallbackCurrency = summary?.currency?.uppercase() ?: countCurrencies.firstOrNull() ?: "USD"
    val cashModeCurrency = summary?.cashModeCurrency.orEmpty()
    val modesByCurrency =
        cashModes.groupBy { mode -> cashModeCurrency[mode]?.uppercase() ?: fallbackCurrency }
    val primaryModes =
        modesByCurrency.mapValues { (_, modes) ->
          modes.firstOrNull { isPrimaryCashModeName(it) } ?: modes.first()
        }
    primaryModes
        .mapNotNull { (currency, mode) ->
          val counted = cashTotalsByCurrency[currency] ?: 0.0
          mode to counted
        }
        .toMap()
  }
  val fallbackCurrency = summary?.currency?.uppercase() ?: countCurrencies.firstOrNull() ?: "USD"
  val countedByCurrency = cashTotalsByCurrency.ifEmpty { mapOf(fallbackCurrency to 0.0) }
  val differenceByCurrency = buildDifferenceByCurrency(expectedCashByCurrency, countedByCurrency)

  Scaffold(
      topBar = {
        ReconciliationHeader(
            state = state,
            strings = strings,
        )
      },
      bottomBar = {
        if (summary != null && mode == ReconciliationMode.Close) {
          ReconciliationActionsBar(
              differenceByCurrency = differenceByCurrency,
              isClosing = closeState.isClosing,
              onClose = { actions.onConfirmClose(countedByMode) },
              onSaveDraft = actions.onSaveDraft,
              strings = strings,
          )
        }
      },
      containerColor = MaterialTheme.colorScheme.background,
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      when (state) {
        ReconciliationState.Loading -> {
          Text(strings.loadingLabel)
        }

        ReconciliationState.Empty -> {
          EmptyReconciliationState(strings)
        }

        is ReconciliationState.Error -> {
          Text(state.message)
        }

        is ReconciliationState.Success -> {
          ReconciliationContent(
              modifier =
                  Modifier.fillMaxWidth()
                      .widthIn(max = 1240.dp)
                      .align(Alignment.TopCenter)
                      .padding(horizontal = 20.dp, vertical = 24.dp),
              summary = state.summary,
              expectedCashByCurrency = expectedCashByCurrency,
              countCurrencies = countCurrencies,
              selectedCountCurrency = selectedCountCurrency,
              onCurrencyChange = { currency ->
                selectedCountCurrency = currency
                denominations = countState[currency] ?: emptyList()
              },
              // openingTotalsByCurrency = openingTotalsByCurrency,
              denominations = denominations,
              onDenominationChange = { value, count ->
                val updated =
                    denominations.map { denom ->
                      if (denom.value == value) denom.copy(count = count) else denom
                    }
                denominations = updated
                countState[selectedCountCurrency] = updated
              },
              cashTotal = cashTotal,
              countedByCurrency = countedByCurrency,
              strings = strings,
          )
        }
      }
      if (!closeState.errorMessage.isNullOrBlank()) {
        Surface(
            modifier =
                Modifier.align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
          Text(
              text = closeState.errorMessage,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
      if (closeState.isSyncing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            icon = {
              Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
              )
            },
            title = { Text("Sincronizando...") },
            text = {
              Column {
                Text(
                    closeState.syncMessage ?: "Esperando que la caja se alinee con el servidor.",
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                )
              }
            },
        )
      }
    }
  }
}

private fun buildExpectedCashByCurrency(summary: ReconciliationSummaryUi): Map<String, Double> {
  if (summary.expectedByMode.isEmpty()) {
    return mapOf(summary.currency.uppercase() to summary.expectedTotal)
  }
  val cashModeKeys = summary.cashModes.map { it.uppercase() }.toSet()
  val modeCurrencyByKey = summary.cashModeCurrency.mapKeys { (mode, _) -> mode.uppercase() }
  val fallbackCurrency = summary.currency.uppercase()
  val totals = mutableMapOf<String, Double>()
  summary.expectedByMode.forEach { (mode, amount) ->
    val modeKey = mode.uppercase()
    if (cashModeKeys.isNotEmpty() && !cashModeKeys.contains(modeKey)) return@forEach
    val currency = modeCurrencyByKey[modeKey]?.uppercase() ?: fallbackCurrency
    totals[currency] = (totals[currency] ?: 0.0) + amount
  }
  if (totals.isEmpty()) {
    totals[fallbackCurrency] = summary.expectedTotal
  }
  return totals.mapValues { (_, amount) ->
    kotlin.math.round(amount * CURRENCY_MINOR_UNITS_FACTOR) / CURRENCY_MINOR_UNITS_FACTOR
  }
}

private fun isPrimaryCashModeName(mode: String): Boolean {
  val normalized = mode.trim().lowercase()
  return normalized.contains("efectivo") || normalized.contains("cash")
}

internal const val EPS = 0.01

internal enum class BalanceState {
  DEFICIT,
  NEUTRAL,
  OK_OR_SURPLUS,
}

private fun stateForCurrency(counted: Double, expected: Double): BalanceState {
  val delta = counted - expected
  return when {
    delta < -EPS -> BalanceState.DEFICIT
    abs(delta) <= EPS && (abs(counted) <= EPS || abs(expected) <= EPS) -> BalanceState.NEUTRAL
    else -> BalanceState.OK_OR_SURPLUS
  }
}

internal fun overallState(
    currencies: List<String>,
    countedByCurrency: Map<String, Double>,
    expectedByCurrency: Map<String, Double>,
): BalanceState {
  var hasGreen = false

  for (code in currencies) {
    val counted = countedByCurrency[code] ?: 0.0
    val expected = expectedByCurrency[code] ?: 0.0

    when (stateForCurrency(counted, expected)) {
      BalanceState.DEFICIT -> return BalanceState.DEFICIT
      BalanceState.OK_OR_SURPLUS -> hasGreen = true
      BalanceState.NEUTRAL -> Unit
    }
  }
  return if (hasGreen) BalanceState.OK_OR_SURPLUS else BalanceState.NEUTRAL
}


fun computeCreditPartial(summary: ReconciliationSummaryUi): Double {
  // Usa el total calculado por facturas del turno.
  return summary.creditPartialTotal
}

fun computeCreditAmounts(summary: ReconciliationSummaryUi): Pair<Double, Double> {
  // Separa pagos parciales de pendientes usando el resumen ya calculado.
  val creditPartial = computeCreditPartial(summary)
  val creditPending = summary.creditPendingTotal
  return creditPartial to creditPending
}

fun formatCurrency(
    value: Double,
    currencyCode: String,
    currencySymbol: String?,
    formatter: DecimalFormatter,
): String {
  val prefix = currencySymbol?.takeIf { it.isNotBlank() } ?: currencyCode
  val formatted = formatter.format(value, 2, includeSeparator = true)
  return "$prefix $formatted"
}

fun Double.formatCurrencyWithCode(code: String): String {
  val formatter = DecimalFormatter()
  val symbol = code.toCurrencySymbol().ifBlank { code }
  return formatCurrency(this, code, symbol, formatter)
}

fun buildCurrencySummaryLine(
    values: Map<String, Double>,
    formatAmountFor: (Double, String) -> String,
): String {
  if (values.isEmpty()) return ""
  return values.entries.joinToString(" / ") { (code, amount) -> formatAmountFor(amount, code) }
}

fun buildDifferenceByCurrency(
    expectedByCurrency: Map<String, Double>,
    countedByCurrency: Map<String, Double>,
): Map<String, Double> {
  val codes = (expectedByCurrency.keys + countedByCurrency.keys).map { it.uppercase() }.distinct()
  return codes.associateWith { code ->
    val expected = expectedByCurrency[code] ?: 0.0
    val counted = countedByCurrency[code] ?: 0.0
    counted - expected
  }
}
