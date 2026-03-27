package com.erpnext.pos.views.billing.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingState
import com.erpnext.pos.views.billing.CreditSalesDisabledBanner
import com.erpnext.pos.views.billing.CreditTermsSection
import com.erpnext.pos.views.billing.DiscountShippingInputs
import com.erpnext.pos.views.billing.PaymentSection
import com.erpnext.pos.views.billing.PaymentTotalsRow
import com.erpnext.pos.views.billing.SectionHeader

@Composable
internal fun BillingCheckoutContent(
    state: BillingState.Success,
    action: BillingAction,
    invoiceCurrency: String,
    secondaryCurrency: String?,
    toSecondary: (Double) -> Double?,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  val panelBg = colors.surfaceVariant.copy(alpha = 0.48f)
  val panelBorder = colors.outlineVariant.copy(alpha = 0.42f)

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
          text = strings.billing.checkoutDataTitle,
          style = MaterialTheme.typography.titleLarge,
          color = colors.onSurface,
      )
      Text(
          text = strings.billing.checkoutReviewSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
      )
    }
    if (!state.allowPartialPayment) {
      Spacer(Modifier.height(10.dp))
      CreditSalesDisabledBanner(strings.billing.creditSalesNotAllowedBanner)
    }
    Spacer(Modifier.height(12.dp))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(bottom = 72.dp)) {
      val isWide = maxWidth >= 980.dp
      if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
              modifier = Modifier.weight(0.38f),
              verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            BillingTotalCard(
                state = state,
                invoiceCurrency = invoiceCurrency,
                secondaryCurrency = secondaryCurrency,
                toSecondary = toSecondary,
                panelBg = panelBg,
                panelBorder = panelBorder,
                modifier = Modifier.fillMaxWidth(),
            )
            BillingCreditCard(
                state = state,
                action = action,
                panelBg = panelBg,
                panelBorder = panelBorder,
                modifier = Modifier.fillMaxWidth(),
            )
            BillingDiscountCard(
                state = state,
                action = action,
                panelBg = panelBg,
                panelBorder = panelBorder,
                modifier = Modifier.fillMaxWidth(),
            )
          }
          BillingPaymentsCard(
              state = state,
              action = action,
              invoiceCurrency = invoiceCurrency,
              panelBg = panelBg,
              panelBorder = panelBorder,
              listMaxHeight = 360.dp,
              modifier = Modifier.weight(0.62f),
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          BillingTotalCard(
              state = state,
              invoiceCurrency = invoiceCurrency,
              secondaryCurrency = secondaryCurrency,
              toSecondary = toSecondary,
              panelBg = panelBg,
              panelBorder = panelBorder,
              modifier = Modifier.fillMaxWidth(),
          )
          BillingCreditCard(
              state = state,
              action = action,
              panelBg = panelBg,
              panelBorder = panelBorder,
              modifier = Modifier.fillMaxWidth(),
          )
          BillingDiscountCard(
              state = state,
              action = action,
              panelBg = panelBg,
              panelBorder = panelBorder,
              modifier = Modifier.fillMaxWidth(),
          )
          BillingPaymentsCard(
              state = state,
              action = action,
              invoiceCurrency = invoiceCurrency,
              panelBg = panelBg,
              panelBorder = panelBorder,
              listMaxHeight = 180.dp,
              modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun BillingTotalCard(
    state: BillingState.Success,
    invoiceCurrency: String,
    secondaryCurrency: String?,
    toSecondary: (Double) -> Double?,
    panelBg: Color,
    panelBorder: Color,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Card(
      modifier = modifier,
      shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = panelBg),
      border = BorderStroke(1.dp, panelBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SectionHeader(title = LocalAppStrings.current.billing.totalLabel, accent = colors.primary)
      Text(
          text = formatAmount(invoiceCurrency.toCurrencySymbol(), state.total),
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = colors.onSurface,
      )
      HorizontalDivider(color = colors.outlineVariant, thickness = 1.2.dp)
      PaymentTotalsRow(
          "Pagado",
          invoiceCurrency,
          state.paidAmountBase,
          secondaryCurrencyCode = secondaryCurrency,
          secondaryAmount = toSecondary(state.paidAmountBase),
      )
      PaymentTotalsRow(
          "Pendiente",
          invoiceCurrency,
          state.balanceDueBase,
          secondaryCurrencyCode = secondaryCurrency,
          secondaryAmount = toSecondary(state.balanceDueBase),
      )
      PaymentTotalsRow(
          "Cambio",
          invoiceCurrency,
          state.changeDueBase,
          secondaryCurrencyCode = secondaryCurrency,
          secondaryAmount = toSecondary(state.changeDueBase),
      )
      HorizontalDivider(color = colors.outlineVariant, thickness = 1.2.dp)
      PaymentTotalsRow(
          "Subtotal",
          invoiceCurrency,
          state.subtotal,
          secondaryCurrencyCode = secondaryCurrency,
          secondaryAmount = toSecondary(state.subtotal),
      )
      if (state.taxes > 0.0) {
        PaymentTotalsRow(
            "Impuestos",
            invoiceCurrency,
            state.taxes,
            secondaryCurrencyCode = secondaryCurrency,
            secondaryAmount = toSecondary(state.taxes),
        )
      }
      if (state.discount > 0.0) {
        PaymentTotalsRow(
            "Descuento",
            invoiceCurrency,
            -state.discount,
            secondaryCurrencyCode = secondaryCurrency,
            secondaryAmount = toSecondary(-state.discount),
        )
      }
      if (state.shippingAmount > 0.0) {
        PaymentTotalsRow(
            "Envío",
            invoiceCurrency,
            state.shippingAmount,
            secondaryCurrencyCode = secondaryCurrency,
            secondaryAmount = toSecondary(state.shippingAmount),
        )
      }
    }
  }
}

@Composable
private fun BillingCreditCard(
    state: BillingState.Success,
    action: BillingAction,
    panelBg: Color,
    panelBorder: Color,
    modifier: Modifier = Modifier,
) {
  if (!state.allowPartialPayment || state.paymentTerms.isEmpty()) return
  val colors = MaterialTheme.colorScheme

  Card(
      modifier = modifier,
      colors = CardDefaults.cardColors(containerColor = panelBg),
      border = BorderStroke(1.dp, panelBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
        modifier = Modifier.padding(12.dp).animateContentSize(animationSpec = tween(260)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionHeader(title = LocalAppStrings.current.billing.creditSaleLabel, accent = colors.tertiary)
      CreditTermsSection(
          isCreditSale = state.isCreditSale,
          allowPartialPayment = state.allowPartialPayment,
          paymentTerms = state.paymentTerms,
          selectedPaymentTerm = state.selectedPaymentTerm,
          creditSaleTooltipMessage = state.creditSaleTooltipMessage,
          onCreditSaleChanged = action.onCreditSaleChanged,
          onPaymentTermSelected = action.onPaymentTermSelected,
      )
    }
  }
}

@Composable
private fun BillingDiscountCard(
    state: BillingState.Success,
    action: BillingAction,
    panelBg: Color,
    panelBorder: Color,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Card(
      modifier = modifier,
      colors = CardDefaults.cardColors(containerColor = panelBg),
      border = BorderStroke(1.dp, panelBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionHeader(
          title = LocalAppStrings.current.billing.totalsDiscountsShippingTitle,
          accent = colors.secondary,
      )
      DiscountShippingInputs(state, action)
    }
  }
}

@Composable
private fun BillingPaymentsCard(
    state: BillingState.Success,
    action: BillingAction,
    invoiceCurrency: String,
    panelBg: Color,
    panelBorder: Color,
    listMaxHeight: Dp,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Card(
      modifier = modifier,
      colors = CardDefaults.cardColors(containerColor = panelBg),
      border = BorderStroke(1.dp, panelBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SectionHeader(title = LocalAppStrings.current.billing.paymentsTitle, accent = colors.primary)
      PaymentSection(
          state = state,
          baseCurrency = invoiceCurrency,
          exchangeRateByCurrency = state.exchangeRateByCurrency,
          paymentLines = state.paymentLines,
          paymentModes = state.paymentModes,
          paidAmountBase = state.paidAmountBase,
          totalAmount = state.total,
          isCreditSale = state.isCreditSale,
          onAddPaymentLine = action.onAddPaymentLine,
          onRemovePaymentLine = action.onRemovePaymentLine,
          onPaymentCurrencySelected = action.onPaymentCurrencySelected,
          paymentListMaxHeight = listMaxHeight,
      )
    }
  }
}
