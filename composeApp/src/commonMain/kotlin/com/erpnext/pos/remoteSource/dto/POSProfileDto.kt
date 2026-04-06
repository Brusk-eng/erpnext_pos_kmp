package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class POSProfileSimpleDto(
    @SerialName("name") val profileName: String,
    val company: String,
    val currency: String,
)

@Serializable
data class POSProfileDto(
    @SerialName("name") val profileName: String,
    @SerialName("warehouse") val warehouse: String,
    @SerialName("route") val route: String? = null,
    val company: String,
    val currency: String,
    @SerialName("income_account") val incomeAccount: String? = null,
    @SerialName("expense_account") val expenseAccount: String? = null,
    @SerialName("payments") val payments: List<PaymentModesDto>,
    val branch: String? = null,
    @SerialName("apply_discount_on") val applyDiscountOn: String,
    @SerialName("cost_center") val costCenter: String? = null,
    @SerialName("selling_price_list") val sellingPriceList: String,
    @SerialName("allow_partial_payment")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowPartialPayment: Boolean = false,
)

@Serializable
data class PaymentModesDto(
    @Serializable(with = IntAsBooleanSerializer::class) val default: Boolean,
    @SerialName("mode_of_payment") val modeOfPayment: String,
    @SerialName("allow_in_returns")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowInReturns: Boolean,
    val account: String? = null,
    val currency: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("mode_of_payment_type") val legacyModeOfPaymentType: String? = null,
) {
  val resolvedType: String?
    get() = type ?: legacyModeOfPaymentType
}
