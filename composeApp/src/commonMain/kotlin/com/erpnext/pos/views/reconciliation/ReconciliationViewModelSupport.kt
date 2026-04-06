package com.erpnext.pos.views.reconciliation

import com.erpnext.pos.localSource.preferences.ShiftMovementRecord
import com.erpnext.pos.localSource.preferences.ShiftMovementType
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.POSContext

internal fun isCashModeName(mode: String?): Boolean {
  if (mode.isNullOrBlank()) return false
  val normalized = mode.trim()
  return normalized.contains("cash", ignoreCase = true) ||
      normalized.contains("efectivo", ignoreCase = true)
}

internal fun sanitizeModeName(mode: String?): String? = mode?.trim()?.takeIf { it.isNotBlank() }

internal fun normalizeModeKey(mode: String?): String? = sanitizeModeName(mode)?.uppercase()

internal fun normalizeCurrencyOrNull(value: String?): String? {
  val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
  return normalizeCurrency(normalized)
}

internal fun isCashMode(mode: String?, cashModeKeys: Set<String>): Boolean {
  if (isCashModeName(mode)) return true
  val key = normalizeModeKey(mode) ?: return false
  return cashModeKeys.contains(key)
}

internal fun resolveModeCurrency(
    mode: String,
    modeCurrency: Map<String, String>,
    posCurrency: String,
): String {
  val fromMode = normalizeModeKey(mode)?.let { modeCurrency[it] }?.let(::normalizeCurrencyOrNull)
  return fromMode ?: normalizeCurrency(posCurrency)
}

internal fun mapOpeningByCurrency(
    openingByMode: Map<String, Double>,
    modeCurrency: Map<String, String>,
    posCurrency: String,
): Map<String, Double> {
  val acc = mutableMapOf<String, Double>()
  openingByMode.forEach { (mode, amount) ->
    val currency = resolveModeCurrency(mode, modeCurrency, posCurrency)
    acc[currency] = (acc[currency] ?: 0.0) + amount
  }
  return acc.mapValues { roundToCurrency(it.value) }
}

internal fun subtractCurrencyMaps(
    totalByCurrency: Map<String, Double>,
    subtractByCurrency: Map<String, Double>,
): Map<String, Double> {
  val result = totalByCurrency.toMutableMap()
  subtractByCurrency.forEach { (code, amount) ->
    result[code] = roundToCurrency((result[code] ?: 0.0) - amount)
  }
  return result.mapValues { roundToCurrency(it.value) }
}

internal fun buildExpectedByMode(
    openingByMode: Map<String, Double>,
    paymentsByMode: Map<String, Double>,
    availableModes: List<String>,
): Map<String, Double> {
  val expected = openingByMode.toMutableMap()
  availableModes.forEach { mode ->
    if (!expected.containsKey(mode)) {
      expected[mode] = 0.0
    }
  }
  paymentsByMode.forEach { (mode, amount) ->
    expected[mode] = roundToCurrency((expected[mode] ?: 0.0) + amount)
  }
  return expected.mapValues { roundToCurrency(it.value) }
}

internal fun aggregateMovementAdjustmentsByMode(
    movements: List<ShiftMovementRecord>,
): Map<String, Double> {
  if (movements.isEmpty()) return emptyMap()
  val adjustments = mutableMapOf<String, Double>()
  movements.forEach { movement ->
    val mode = movement.modeOfPayment.trim()
    if (mode.isBlank()) return@forEach
    val signedAmount =
        when (movement.movementType) {
          ShiftMovementType.INTERNAL_TRANSFER_IN,
          ShiftMovementType.CASH_IN -> movement.amount
          ShiftMovementType.INTERNAL_TRANSFER_OUT,
          ShiftMovementType.CASH_OUT,
          ShiftMovementType.EXPENSE,
          ShiftMovementType.REFUND -> -movement.amount
          ShiftMovementType.COLLECTION -> 0.0
        }
    adjustments[mode] = roundToCurrency((adjustments[mode] ?: 0.0) + signedAmount)
  }
  return adjustments
}

internal fun resolveCashModes(
    context: POSContext,
    openingByMode: Map<String, Double>,
    configuredCashModes: List<String>,
): Set<String> {
  val fromRepository = configuredCashModes.mapNotNull(::sanitizeModeName).toSet()
  if (fromRepository.isNotEmpty()) return fromRepository

  val configuredByType =
      context.paymentModes
          .filter { mode -> mode.type?.equals("Cash", ignoreCase = true) == true }
          .flatMap { mode -> listOf(mode.modeOfPayment, mode.name) }
          .mapNotNull(::sanitizeModeName)
          .toSet()
  if (configuredByType.isNotEmpty()) return configuredByType

  val direct =
      context.paymentModes
          .flatMap { mode -> listOf(mode.modeOfPayment, mode.name) }
          .mapNotNull(::sanitizeModeName)
          .filter(::isCashModeName)
          .toSet()
  if (direct.isNotEmpty()) return direct

  val fallback = openingByMode.keys.mapNotNull(::sanitizeModeName).filter(::isCashModeName).toSet()
  return fallback.ifEmpty { openingByMode.keys.mapNotNull(::sanitizeModeName).toSet() }
}

internal data class CreditTotals(
    val partialTotal: Double,
    val pendingTotal: Double,
    val partialByCurrency: Map<String, Double>,
    val pendingByCurrency: Map<String, Double>,
)
