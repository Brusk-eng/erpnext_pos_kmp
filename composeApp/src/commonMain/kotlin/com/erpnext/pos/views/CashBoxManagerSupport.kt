package com.erpnext.pos.views

import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.localSource.preferences.ShiftMovementRecord
import com.erpnext.pos.localSource.preferences.ShiftMovementType
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.localSource.entities.UserEntity

internal fun UserEntity.resolveServerUserId(): String {
  return name
      .trim()
      .ifBlank { username?.trim().orEmpty() }
      .ifBlank { email.trim() }
}

internal fun UserEntity.ownedIdentifiers(): Set<String> {
  return listOf(resolveServerUserId(), name, username, email)
      .mapNotNull { candidate -> candidate?.trim()?.lowercase()?.takeIf { it.isNotBlank() } }
      .toSet()
}

internal fun String?.normalizeOpeningEntryId(): String? {
  return this?.trim()?.takeIf { it.isNotBlank() }
}

internal fun buildLocalOpeningEntryId(profileName: String, userId: String, now: Long): String {
  val normalizedProfile = profileName.trim().uppercase().take(12)
  val normalizedUser = userId.substringBefore('@').uppercase().take(8)
  return "LOCAL-OPEN-$normalizedProfile-$normalizedUser-$now"
}

internal fun buildLocalClosingEntryId(profileName: String, userId: String, now: Long): String {
  val normalizedProfile = profileName.trim().uppercase().take(12)
  val normalizedUser = userId.substringBefore('@').uppercase().take(8)
  return "LOCAL-CLOSE-$normalizedProfile-$normalizedUser-$now"
}

internal fun roundMoney(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

internal fun buildModeCurrencyMap(
    methods: List<ResolvedPaymentMethod>,
    fallbackCurrency: String,
): Map<String, String> {
  return methods.associate { method ->
    val currency = normalizeCurrency(method.currency ?: fallbackCurrency)
    method.mopName to currency
  }
}

internal fun resolveModeCurrency(modeOfPayment: String, context: POSContext): String {
  val fromContext =
      context.paymentModes
          .firstOrNull { it.modeOfPayment.equals(modeOfPayment, ignoreCase = true) }
          ?.currency
          ?.trim()
          ?.takeIf { it.isNotBlank() }
  return normalizeCurrency(fromContext ?: context.currency)
}

internal fun signedShiftMovementAmount(movement: ShiftMovementRecord): Double {
  return when (movement.movementType) {
    ShiftMovementType.INTERNAL_TRANSFER_IN,
    ShiftMovementType.CASH_IN -> movement.amount
    ShiftMovementType.INTERNAL_TRANSFER_OUT,
    ShiftMovementType.CASH_OUT,
    ShiftMovementType.EXPENSE,
    ShiftMovementType.REFUND -> -movement.amount
    ShiftMovementType.COLLECTION -> 0.0
  }
}
