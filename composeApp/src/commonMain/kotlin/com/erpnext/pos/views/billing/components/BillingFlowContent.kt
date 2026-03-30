package com.erpnext.pos.views.billing.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.paging.PagingData
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingLabCheckoutStep
import com.erpnext.pos.views.billing.BillingLabContent
import com.erpnext.pos.views.billing.BillingState
import kotlinx.coroutines.flow.Flow

internal enum class BillingCheckoutStep {
  Cart,
  Checkout,
}

@Composable
internal fun BillingScreenScaffold(
    state: BillingState,
    step: BillingCheckoutStep,
    productsPagingFlow: Flow<PagingData<ItemBO>>,
    action: BillingAction,
    snackbar: SnackbarController,
    globalBusy: Boolean,
    onCheckoutRequested: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme

  Scaffold(
      containerColor = colors.background,
      bottomBar = {
        val successState = state as? BillingState.Success
        if (successState != null && step == BillingCheckoutStep.Checkout) {
          BillingFinalizeBar(
              state = successState,
              enabled = !globalBusy,
              onFinalizeSale = action.onFinalizeSale,
          )
        }
      },
  ) { paddingValues ->
    when (state) {
      BillingState.Loading -> {
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
              trackColor = colors.onSecondary,
              color = colors.onPrimary,
              strokeWidth = 2.dp,
          )
        }
      }

      is BillingState.Error -> {
        LaunchedEffect(state.message) {
          snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
        }
        val previous = state.previous
        if (previous != null) {
          BillingStepSwitcher(
              step = step,
              state = previous,
              productsPagingFlow = productsPagingFlow,
              action = action,
              onCheckoutRequested = onCheckoutRequested,
              modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
          )
        } else {
          EmptyBillingState(
              showSyncRates = state.showSyncRates,
              onSyncExchangeRates = action.onSyncExchangeRates,
              modifier = Modifier.fillMaxSize().padding(paddingValues),
          )
        }
      }

      BillingState.Empty -> {
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
          snackbar.show(strings.billing.noDataAvailable, SnackbarType.Info, SnackbarPosition.Top)
        }
      }

      is BillingState.Success -> {
        BillingStepSwitcher(
            step = step,
            state = state,
            productsPagingFlow = productsPagingFlow,
            action = action,
            onCheckoutRequested = onCheckoutRequested,
            modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()),
        )
      }
    }
  }
}

@Composable
private fun BillingStepSwitcher(
    step: BillingCheckoutStep,
    state: BillingState.Success,
    productsPagingFlow: Flow<PagingData<com.erpnext.pos.domain.models.ItemBO>>,
    action: BillingAction,
    onCheckoutRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
  AnimatedContent(
      targetState = step,
      transitionSpec = {
        fadeIn(tween(180)) +
            slideInVertically(animationSpec = tween(180), initialOffsetY = { it / 6 }) togetherWith
            fadeOut(tween(160)) +
                slideOutVertically(animationSpec = tween(160), targetOffsetY = { -it / 8 })
      },
      label = "billing_step_transition",
  ) { targetStep ->
    when (targetStep) {
      BillingCheckoutStep.Cart ->
          BillingLabContent(
              state = state,
              productsPagingFlow = productsPagingFlow,
              action = action,
              onCheckout = onCheckoutRequested,
              modifier = modifier,
          )

      BillingCheckoutStep.Checkout ->
          BillingLabCheckoutStep(
              state = state,
              action = action,
              modifier = modifier,
          )
    }
  }
}

@Composable
private fun EmptyBillingState(
    showSyncRates: Boolean,
    onSyncExchangeRates: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
    ) {
      Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            text = strings.billing.noDataAvailable,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        if (showSyncRates) {
          Button(onClick = onSyncExchangeRates) { Text(strings.billing.syncRatesButton) }
        }
      }
    }
  }
}

@Composable
private fun BillingFinalizeBar(
    state: BillingState.Success,
    enabled: Boolean,
    onFinalizeSale: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  val canFinalize =
      state.selectedCustomer != null &&
          state.cartItems.isNotEmpty() &&
          (state.isCreditSale || state.paidAmountBase + 0.01 >= state.total) &&
          (!state.isCreditSale || state.selectedPaymentTerm != null)

  Box(
      contentAlignment = Alignment.CenterEnd,
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  Brush.verticalGradient(
                      colors = listOf(colors.background.copy(alpha = 0.0f), colors.background)
                  )
              ),
  ) {
    Button(
        onClick = onFinalizeSale,
        enabled = canFinalize && enabled,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(strings.billing.finalizeSale, fontWeight = FontWeight.Bold)
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = Icons.Default.ShoppingCartCheckout,
            contentDescription = strings.billing.finalizeSale,
            tint = Color.White,
        )
      }
    }
  }
}

@Composable
internal fun BillingSuccessDialog(
    message: String,
    invoiceReference: String?,
    onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(dismissOnClickOutside = true, dismissOnBackPress = true),
  ) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        tonalElevation = 8.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.widthIn(min = 420.dp),
    ) {
      Column(
          modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = message,
            color = colors.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        invoiceReference?.let { invoice ->
          Text(
              text = "${strings.billing.referenceLabel}: $invoice",
              color = colors.onSurface.copy(alpha = 0.75f),
              fontSize = 14.sp,
          )
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = onDismiss) {
          Text(strings.billing.closeButton)
        }
      }
    }
  }
}
