package com.erpnext.pos.views.billing.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.PaymentLine

@Composable
internal fun PaymentAmountSummary(
    baseCurrency: String,
    paidAmountBase: Double,
    pendingAmount: Double,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    PaymentAmountSummaryCard(
        title = strings.billing.paidBaseLabel,
        amountText = formatAmount(baseCurrency.toCurrencySymbol(), paidAmountBase),
        modifier = Modifier.weight(1f),
    )
    PaymentAmountSummaryCard(
        title = strings.billing.balanceDueLabel,
        amountText = formatAmount(baseCurrency.toCurrencySymbol(), pendingAmount),
        modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun PaymentAmountSummaryCard(
    title: String,
    amountText: String,
    modifier: Modifier = Modifier,
) {
  Card(
      modifier = modifier,
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          text = amountText,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
internal fun RegisteredPaymentsCard(
    paymentLines: List<PaymentLine>,
    baseCurrency: String,
    paymentListMaxHeight: Dp,
    onRemovePaymentLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = strings.billing.paymentsRegisteredTitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(6.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .heightIn(min = 84.dp, max = paymentListMaxHeight)
                  .verticalScroll(rememberScrollState())
                  .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (paymentLines.isEmpty()) {
          Text(
              strings.billing.paymentsEmpty,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          paymentLines.forEachIndexed { index, line ->
            RegisteredPaymentLine(
                line = line,
                baseCurrency = baseCurrency,
                onRemove = { onRemovePaymentLine(index) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RegisteredPaymentLine(
    line: PaymentLine,
    baseCurrency: String,
    onRemove: () -> Unit,
) {
  AnimatedVisibility(
      visible = true,
      enter = fadeIn(animationSpec = tween(360)) + expandVertically(),
      exit = fadeOut(animationSpec = tween(320)) + shrinkVertically(),
  ) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(text = line.modeOfPayment, fontWeight = FontWeight.SemiBold)
          Text(
              text = formatAmount(line.currency.toCurrencySymbol(), line.enteredAmount),
              style = MaterialTheme.typography.titleSmall,
          )
          Text(
              text =
                  "Base: ${formatAmount(baseCurrency.toCurrencySymbol(), line.baseAmount)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onRemove) {
          Icon(
              Icons.Default.Delete,
              contentDescription = "Eliminar línea de pago",
              tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
