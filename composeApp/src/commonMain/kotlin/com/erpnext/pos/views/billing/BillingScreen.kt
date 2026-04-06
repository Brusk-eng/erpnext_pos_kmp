@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.erpnext.pos.views.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.navigation.GlobalTopBarState
import com.erpnext.pos.navigation.LocalTopBarController
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.utils.loading.LoadingUiState
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.resolveRateBetweenFromBaseRates
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.billing.components.BillingCartPanel
import com.erpnext.pos.views.billing.components.BillingCheckoutContent
import com.erpnext.pos.views.billing.components.BillingCheckoutStep
import com.erpnext.pos.views.billing.components.BillingProductBrowserPanel
import com.erpnext.pos.views.billing.components.BillingScreenScaffold
import com.erpnext.pos.views.billing.components.BillingSuccessDialog
import com.erpnext.pos.views.billing.components.PaymentAmountEntry
import com.erpnext.pos.views.billing.components.PaymentAmountSummary
import com.erpnext.pos.views.billing.components.PaymentModeSelector
import com.erpnext.pos.views.billing.components.PaymentReferenceField
import com.erpnext.pos.views.billing.components.RegisteredPaymentsCard
import com.erpnext.pos.views.billing.components.buildPaymentLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val SUCCESS_POPUP_HIDE_DELAY_MS = 5_000L
private const val PHONE_SMALLEST_WIDTH_DP = 600f

data class CartItem(
    val itemCode: String,
    val name: String,
    val currency: String?,
    val quantity: Double,
    val price: Double,
    val availableQty: Double? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    state: BillingState,
    productsPagingFlow: Flow<PagingData<ItemBO>>,
    action: BillingAction,
    snackbar: SnackbarController,
) {
  val strings = LocalAppStrings.current
  val uiSnackbar = snackbar.snackbar.collectAsState().value
  val loadingState by LoadingIndicator.state.collectAsState(initial = LoadingUiState())
  val globalBusy = loadingState.isLoading
  var step by rememberSaveable { mutableStateOf(BillingCheckoutStep.Cart) }
  val successState =
      when (state) {
        is BillingState.Success -> state
        is BillingState.Error -> state.previous
        else -> null
      }
  val successMessage = successState?.successMessage
  val successDialogMessage = successState?.successDialogMessage
  val successDialogInvoice = successState?.successDialogInvoice
  val successDialogId = successState?.successDialogId ?: 0L
  var popupMessage by rememberSaveable { mutableStateOf<String?>(null) }
  var popupInvoice by rememberSaveable { mutableStateOf<String?>(null) }
  val topBarController = LocalTopBarController.current
  val inactivityTimeoutMs = 5 * 60 * 1000L
  var lastInteraction by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
  val hasActiveSale =
      state is BillingState.Success &&
          (state.selectedCustomer != null ||
              state.cartItems.isNotEmpty() ||
              state.paymentLines.isNotEmpty())

  // Si salimos de Success, regresamos al primer paso.
  LaunchedEffect(state, successState) {
    if (successState == null) {
      popupMessage = null
      popupInvoice = null
      action.onClearSuccessMessage()
      step = BillingCheckoutStep.Cart
    } else if (
        successState.selectedCustomer == null &&
            successState.cartItems.isEmpty() &&
            step != BillingCheckoutStep.Cart
    ) {
      step = BillingCheckoutStep.Cart
    }
  }

  LaunchedEffect(successDialogId, successDialogMessage, successMessage) {
    if (successDialogId == 0L) return@LaunchedEffect
    val message =
        (successDialogMessage ?: successMessage)?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
    popupMessage = message
    popupInvoice = successDialogInvoice
    delay(SUCCESS_POPUP_HIDE_DELAY_MS)
    if (!popupMessage.isNullOrBlank()) {
      popupMessage = null
      popupInvoice = null
      action.onClearSuccessMessage()
      if (step == BillingCheckoutStep.Checkout) {
        step = BillingCheckoutStep.Cart
      }
    }
  }

  LaunchedEffect(lastInteraction, hasActiveSale) {
    if (!hasActiveSale) return@LaunchedEffect
    val now = Clock.System.now().toEpochMilliseconds()
    val remaining = inactivityTimeoutMs - (now - lastInteraction)
    if (remaining <= 0) {
      step = BillingCheckoutStep.Cart
      action.onResetSale()
      snackbar.show(strings.billing.inactivityResetMessage, SnackbarType.Info, SnackbarPosition.Top)
      return@LaunchedEffect
    }
    delay(remaining)
    val elapsed = Clock.System.now().toEpochMilliseconds() - lastInteraction
    if (elapsed >= inactivityTimeoutMs && hasActiveSale) {
      step = BillingCheckoutStep.Cart
      action.onResetSale()
      snackbar.show(strings.billing.inactivityResetMessage, SnackbarType.Info, SnackbarPosition.Top)
    }
  }

  DisposableEffect(Unit) { onDispose { topBarController.reset() } }

  LaunchedEffect(step, state) {
    topBarController.set(
        GlobalTopBarState(
            subtitle =
                if (state is BillingState.Success && state.selectedCustomer != null) {
                  state.selectedCustomer.customerName
                } else {
                  null
                },
            showBack = step != BillingCheckoutStep.Cart,
            onBack = {
              if (step == BillingCheckoutStep.Checkout) {
                step = BillingCheckoutStep.Cart
              } else if (step == BillingCheckoutStep.Cart) {
                action.onBack()
              }
            },
        )
    )
  }

  Box(
      Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            awaitPointerEvent()
            lastInteraction = Clock.System.now().toEpochMilliseconds()
          }
        }
      }
  ) {
    BillingScreenScaffold(
        state = state,
        step = step,
        productsPagingFlow = productsPagingFlow,
        action = action,
        snackbar = snackbar,
        globalBusy = globalBusy,
        onCheckoutRequested = {
          val success = state as? BillingState.Success
          if (success?.selectedCustomer == null) {
            snackbar.show(
                strings.billing.selectCustomerForDocuments,
                SnackbarType.Error,
                SnackbarPosition.Top,
            )
          } else if (success.cartItems.isEmpty()) {
            snackbar.show(
                strings.billing.emptyCartSnackbar,
                SnackbarType.Error,
                SnackbarPosition.Top,
            )
          } else {
            step = BillingCheckoutStep.Checkout
          }
        }
    )

    if (!popupMessage.isNullOrBlank()) {
      BillingSuccessDialog(
          message = popupMessage ?: strings.billing.finalizeSale,
          invoiceReference = popupInvoice,
          onDismiss = {
            popupMessage = null
            popupInvoice = null
            action.onClearSuccessMessage()
            if (step == BillingCheckoutStep.Checkout) {
              step = BillingCheckoutStep.Cart
            }
          },
      )
    }
  }

  SnackbarHost(
      snackbar = uiSnackbar,
      onDismiss = snackbar::dismiss,
      modifier = Modifier.fillMaxSize(),
  )
}

@Composable
internal fun BillingLabContent(
    state: BillingState.Success,
    productsPagingFlow: Flow<PagingData<ItemBO>>,
    action: BillingAction,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  val accent = colors.primary
  val background = colors.background
  val leftPanelBg = colors.surfaceVariant
  val invoiceCurrency = state.currency?.trim()?.uppercase().orEmpty().ifBlank { "USD" }
  val baseCurrency = state.baseCurrency?.trim()?.uppercase().orEmpty().ifBlank { invoiceCurrency }
  val secondaryCurrency =
      resolveSecondaryCurrency(
          invoiceCurrency = invoiceCurrency,
          baseCurrency = baseCurrency,
          exchangeRateByCurrency = state.exchangeRateByCurrency,
      )
  fun toSecondary(amount: Double): Double? {
    return convertToSecondary(
        amount = amount,
        secondaryCurrency = secondaryCurrency,
        exchangeRateByCurrency = state.exchangeRateByCurrency,
    )
  }

  val products = productsPagingFlow.collectAsLazyPagingItems()
  val categoriesFromSnapshot =
      remember(products.itemSnapshotList.items) {
        products.itemSnapshotList.items
            .mapNotNull { it.itemGroup.takeIf { g -> g.isNotBlank() } }
            .distinct()
            .sorted()
      }
  val categories =
      remember(state.productCategories, categoriesFromSnapshot) {
        state.productCategories.ifEmpty { categoriesFromSnapshot }
      }
  var selectedCategory by rememberSaveable { mutableStateOf(state.selectedProductCategory) }

  LaunchedEffect(state.selectedProductCategory) {
    if (selectedCategory != state.selectedProductCategory) {
      selectedCategory = state.selectedProductCategory
    }
  }
  val windowSizeClass = rememberWindowSizeClass()
  val windowInfo = LocalWindowInfo.current
  val density = LocalDensity.current
  val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
  val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp().value }
  val smallestWidthDp = minOf(windowWidthDp, windowHeightDp)
  val isCompactPhone =
      getPlatformName() != "Desktop" &&
          smallestWidthDp < PHONE_SMALLEST_WIDTH_DP &&
          (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
              windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact)
  if (isCompactPhone) {
    BillingLabContentCompact(
        state = state,
        products = products,
        categories = categories,
        selectedCategory = selectedCategory,
        onSelectCategory = {
          selectedCategory = it
          action.onProductCategorySelected(it)
        },
        invoiceCurrency = invoiceCurrency,
        secondaryCurrency = secondaryCurrency,
        toSecondary = { amount -> toSecondary(amount) },
        action = action,
        onCheckout = onCheckout,
        modifier = Modifier.fillMaxSize(),
    )
    return
  }

  Column(
      modifier = modifier.background(background).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!state.allowPartialPayment) {
      CreditSalesDisabledBanner(strings.billing.creditSalesNotAllowedBanner)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(
          modifier =
              Modifier.weight(1f)
                  .fillMaxHeight()
                  .background(leftPanelBg, RoundedCornerShape(20.dp))
                  .padding(16.dp)
      ) {
        BillingProductBrowserPanel(
            products = products,
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = {
              selectedCategory = it
              action.onProductCategorySelected(it)
            },
            searchQuery = state.productSearchQuery,
            onSearchQueryChange = action.onProductSearchQueryChange,
            onClearSearch = { action.onProductSearchQueryChange("") },
            baseCurrency = invoiceCurrency,
            exchangeRateByCurrency = state.exchangeRateByCurrency,
            accent = accent,
            onProductAdded = action.onProductAdded,
            modifier = Modifier.fillMaxSize(),
        )
      }

      BillingCartPanel(
          state = state,
          invoiceCurrency = invoiceCurrency,
          secondaryCurrency = secondaryCurrency,
          toSecondary = { amount -> toSecondary(amount) },
          action = action,
          accent = accent,
          onCheckout = onCheckout,
          compact = false,
      )
    }
  }
}

@Composable
internal fun BillingLabContentCompact(
    state: BillingState.Success,
    products: androidx.paging.compose.LazyPagingItems<ItemBO>,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    invoiceCurrency: String,
    secondaryCurrency: String?,
    toSecondary: (Double) -> Double?,
    action: BillingAction,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  val accent = colors.primary
  val scrollState = rememberScrollState()

  Column(
      modifier = modifier.background(colors.background).verticalScroll(scrollState).padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!state.allowPartialPayment) {
      CreditSalesDisabledBanner(strings.billing.creditSalesNotAllowedBanner)
    }

    BillingCartPanel(
        state = state,
        invoiceCurrency = invoiceCurrency,
        secondaryCurrency = secondaryCurrency,
        toSecondary = { amount -> toSecondary(amount) },
        action = action,
        accent = accent,
        onCheckout = onCheckout,
        compact = true,
    )

    Surface(
        color = colors.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
    ) {
      Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        BillingProductBrowserPanel(
            products = products,
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory,
            searchQuery = state.productSearchQuery,
            onSearchQueryChange = action.onProductSearchQueryChange,
            onClearSearch = { action.onProductSearchQueryChange("") },
            baseCurrency = invoiceCurrency,
            exchangeRateByCurrency = state.exchangeRateByCurrency,
            accent = colors.primary,
            onProductAdded = action.onProductAdded,
            compact = true,
        )
      }
    }
  }
}

@Composable
internal fun BillingLabCheckoutStep(
    state: BillingState.Success,
    action: BillingAction,
    modifier: Modifier = Modifier,
) {
  val invoiceCurrency = state.currency?.trim()?.uppercase().orEmpty().ifBlank { "USD" }
  val baseCurrency = state.baseCurrency?.trim()?.uppercase().orEmpty().ifBlank { invoiceCurrency }
  val secondaryCurrency =
      resolveSecondaryCurrency(
          invoiceCurrency = invoiceCurrency,
          baseCurrency = baseCurrency,
          exchangeRateByCurrency = state.exchangeRateByCurrency,
      )
  fun toSecondary(amount: Double): Double? {
    return convertToSecondary(
        amount = amount,
        secondaryCurrency = secondaryCurrency,
        exchangeRateByCurrency = state.exchangeRateByCurrency,
    )
  }
  BillingCheckoutContent(
      state = state,
      action = action,
      invoiceCurrency = invoiceCurrency,
      secondaryCurrency = secondaryCurrency,
      toSecondary = { amount -> toSecondary(amount) },
      modifier = modifier,
  )
}

@Composable
internal fun LabSearchBar(value: String, onChange: (String) -> Unit, onClear: () -> Unit) {
  val strings = LocalAppStrings.current
  OutlinedTextField(
      value = value,
      onValueChange = onChange,
      modifier = Modifier.fillMaxWidth(),
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (value.isNotBlank()) {
          IconButton(onClick = onClear) { Icon(Icons.Default.Clear, contentDescription = null) }
        }
      },
      placeholder = { Text(strings.billing.productSearchPlaceholder) },
      singleLine = true,
      colors =
          TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              unfocusedContainerColor =
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              cursorColor = MaterialTheme.colorScheme.primary,
          ),
      shape = RoundedCornerShape(18.dp),
  )
}

@Composable
internal fun LabCategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    item {
      LabCategoryChip(
          label = strings.customer.allLabel,
          selected = selectedCategory == "Todos",
          onClick = { onSelect("Todos") },
      )
    }
    items(categories, key = { it }) { category ->
      LabCategoryChip(
          label = category,
          selected = selectedCategory == category,
          onClick = { onSelect(category) },
      )
    }
  }
}

@Composable
private fun LabCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  val background = if (selected) colors.primary else colors.surfaceVariant
  val textColor = if (selected) colors.onPrimary else colors.onSurfaceVariant
  Surface(
      color = background,
      shape = RoundedCornerShape(999.dp),
      shadowElevation = if (selected) 3.dp else 0.dp,
      modifier = Modifier.clickable(onClick = onClick),
  ) {
    Text(
        text = label,
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}

@Composable
internal fun LabProductCard(
    item: ItemBO,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    accent: Color,
    onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val isDesktop = remember { getPlatformName() == "Desktop" }
  val imageUrl = item.image?.trim().orEmpty()

  val itemCurrency = item.currency?.trim()?.uppercase().orEmpty()
  val base = baseCurrency.trim().uppercase()
  val baseRates = exchangeRateByCurrency.toMutableMap().apply { this[base] = 1.0 }
  val rate =
      if (itemCurrency.isBlank() || itemCurrency == base) 1.0
      else
          resolveRateBetweenFromBaseRates(
              fromCurrency = itemCurrency,
              toCurrency = base,
              baseCurrency = base,
              baseRates = baseRates,
          ) ?: 1.0
  val displayPrice = bd(item.price * rate).moneyScale(0).toDouble(0)

  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()
  val showAction = !isDesktop || hovered

  Surface(
      color = colors.surface,
      shape = RoundedCornerShape(18.dp),
      tonalElevation = 0.dp,
      shadowElevation = 2.dp,
      border = BorderStroke(1.dp, colors.outlineVariant),
      modifier = Modifier.hoverable(interactionSource).clickable(onClick = onClick),
  ) {
    Box {
      Column {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
          if (imageUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model =
                    ImageRequest.Builder(LocalPlatformContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
          } else {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
              Text(
                  text = item.name.take(2).uppercase(),
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.onSurfaceVariant,
              )
            }
          }

          Surface(
              color = colors.primary,
              shape = RoundedCornerShape(999.dp),
              shadowElevation = 2.dp,
              modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
          ) {
            Text(
                text = "Disp. ${item.actualQty.formatQty()}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${baseCurrency.toCurrencySymbol()} $displayPrice",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (showAction) {
        Surface(
            color = accent,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 4.dp,
            modifier =
                Modifier.align(Alignment.BottomEnd).padding(12.dp).clickable(onClick = onClick),
        ) {
          Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Agregar",
              tint = colors.onPrimary,
              modifier = Modifier.padding(6.dp),
          )
        }
      }
    }
  }
}

@Composable
internal fun LabCartHeader(itemCount: Int, accent: Color) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(color = accent, shape = RoundedCornerShape(12.dp)) {
        Icon(
            imageVector = Icons.Default.Money,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(8.dp),
        )
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
            text = strings.billing.currentOrderTitle,
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
        )
        Text(
            text = "$itemCount ${strings.billing.cartItemsLabel.lowercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
internal fun LabCartItem(
    item: CartItem,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    onUpdateQuantity: (Double) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val isDesktop = remember { getPlatformName() == "Desktop" }

  val interactionSource = remember { MutableInteractionSource() }
  val hovered by interactionSource.collectIsHoveredAsState()

  val showActions = !isDesktop || hovered
  val actionAlpha by
      animateFloatAsState(
          targetValue = if (showActions) 1f else 0f,
          animationSpec = tween(durationMillis = 150),
          label = "cartActionsAlpha",
      )
  val itemCurrency = item.currency?.trim()?.uppercase().orEmpty()
  val base = baseCurrency.trim().uppercase()
  val baseRates = exchangeRateByCurrency.toMutableMap().apply { this[base] = 1.0 }
  val rate =
      if (itemCurrency.isBlank() || itemCurrency == base) 1.0
      else
          resolveRateBetweenFromBaseRates(
              fromCurrency = itemCurrency,
              toCurrency = base,
              baseCurrency = base,
              baseRates = baseRates,
          ) ?: 1.0
  val unitDisplay = bd(item.price * rate).toDouble(0)
  val lineDisplay = unitDisplay * item.quantity
  var appeared by remember { mutableStateOf(false) }
  LaunchedEffect(item.itemCode) { appeared = true }
  var isRemoving by remember { mutableStateOf(false) }
  LaunchedEffect(isRemoving) {
    if (isRemoving) {
      delay(180)
      onRemove()
    }
  }

  val appearAlpha by
      animateFloatAsState(
          targetValue = if (appeared && !isRemoving) 1f else 0f,
          animationSpec = tween(durationMillis = 170),
          label = "cartAppearAlpha",
      )
  val appearScale by
      animateFloatAsState(
          targetValue = if (appeared && !isRemoving) 1f else 0.92f,
          animationSpec = tween(durationMillis = 170),
          label = "cartAppearScale",
      )

  AnimatedVisibility(
      visible = !isRemoving,
      exit = fadeOut(tween(160)) + shrinkVertically(tween(160)),
  ) {
    Surface(
        color = colors.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier =
            modifier
                .hoverable(interactionSource)
                .graphicsLayer(alpha = appearAlpha, scaleX = appearScale, scaleY = appearScale)
                .animateContentSize(),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = item.name,
              style = MaterialTheme.typography.bodyMedium,
              color = colors.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
          Text(
              text = formatAmount(baseCurrency.toCurrencySymbol(), unitDisplay),
              style = MaterialTheme.typography.labelSmall,
              color = colors.onSurfaceVariant,
          )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
              onClick = { onUpdateQuantity((item.quantity - 1).coerceAtLeast(1.0)) },
              modifier = Modifier.graphicsLayer(alpha = actionAlpha),
              enabled = showActions,
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Menos")
          }
          Text(
              text = item.quantity.formatQty(),
              style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
              color = colors.onSurface,
          )
          IconButton(
              onClick = { onUpdateQuantity(item.quantity + 1) },
              modifier = Modifier.graphicsLayer(alpha = actionAlpha),
              enabled = showActions,
          ) {
            Icon(Icons.Default.Add, contentDescription = "Más")
          }
          Text(
              text = formatAmount(baseCurrency.toCurrencySymbol(), lineDisplay),
              style = MaterialTheme.typography.labelLarge,
              color = colors.onSurface,
          )
          IconButton(
              onClick = { isRemoving = true },
              modifier = Modifier.graphicsLayer(alpha = actionAlpha),
              enabled = showActions,
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerSelector(
    customers: List<CustomerBO>,
    query: String,
    onQueryChange: (String) -> Unit,
    onCustomerSelected: (CustomerBO) -> Unit,
) {
  val strings = LocalAppStrings.current
  var expanded by remember { mutableStateOf(false) }
  var displayLimit by remember { mutableStateOf(50) }
  var anchorWidthPx by remember { mutableStateOf(0) }
  val density = LocalDensity.current
  val hasCustomers = customers.isNotEmpty()

  ExposedDropdownMenuBox(
      expanded = expanded && hasCustomers,
      onExpandedChange = {
        if (hasCustomers) {
          expanded = !expanded
        }
      },
  ) {
    AppTextField(
        value = query,
        onValueChange = {
          onQueryChange(it)
          expanded = true
        },
        modifier =
            Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .onGloballyPositioned { anchorWidthPx = it.size.width }
                .onFocusChanged { focusState -> expanded = focusState.isFocused },
        label = strings.billing.searchCustomerLabel,
        placeholder = strings.billing.searchCustomerPlaceholder,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )
    DropdownMenu(
        expanded = expanded && hasCustomers,
        onDismissRequest = { expanded = false },
        properties =
            PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = false,
            ),
    ) {
      val menuWidth =
          remember(anchorWidthPx) {
            if (anchorWidthPx > 0) with(density) { anchorWidthPx.toDp() } else 360.dp
          }
      Column(
          modifier =
              Modifier.width(menuWidth.coerceIn(280.dp, 520.dp))
                  .heightIn(max = 320.dp)
                  .verticalScroll(rememberScrollState())
      ) {
        customers.take(displayLimit).forEach { customer ->
          DropdownMenuItem(
              text = { Text("${customer.name} - ${customer.customerName}") },
              onClick = {
                onCustomerSelected(customer)
                expanded = false
              },
          )
        }
        if (customers.size > displayLimit) {
          DropdownMenuItem(
              text = { Text(strings.common.showMore) },
              onClick = { displayLimit += 50 },
          )
        }
      }
    }
  }
}

@Composable
internal fun PaymentTotalsRow(
    label: String,
    currencyCode: String,
    amount: Double,
    secondaryCurrencyCode: String? = null,
    secondaryAmount: Double? = null,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(horizontalAlignment = Alignment.End) {
      Text(
          text = formatAmount(currencyCode.toCurrencySymbol(), amount),
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
      )
      if (secondaryCurrencyCode != null && secondaryAmount != null) {
        Text(
            text = formatAmount(secondaryCurrencyCode.toCurrencySymbol(), secondaryAmount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
internal fun SectionHeader(title: String, accent: Color) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
        modifier =
            Modifier.size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
    )
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

private fun resolveSecondaryCurrency(
    invoiceCurrency: String,
    baseCurrency: String?,
    exchangeRateByCurrency: Map<String, Double>,
): String? {
  val invoice = invoiceCurrency.trim().uppercase()
  val preferred =
      when (invoice) {
        "USD" -> "NIO"
        "NIO" -> "USD"
        else -> baseCurrency?.trim()?.uppercase()
      }
  if (preferred.isNullOrBlank() || preferred == invoice) return null
  return preferred.takeIf { exchangeRateByCurrency[it]?.let { rate -> rate > 0.0 } == true }
}

private fun convertToSecondary(
    amount: Double,
    secondaryCurrency: String?,
    exchangeRateByCurrency: Map<String, Double>,
): Double? {
  if (secondaryCurrency.isNullOrBlank()) return null
  val rate = exchangeRateByCurrency[secondaryCurrency]?.takeIf { it > 0.0 } ?: return null
  return amount / rate
}

internal fun inferCurrencyFromModeName(modeOfPayment: String, fallback: String): String {
  val upper = modeOfPayment.trim().uppercase()
  return when {
    upper.contains("USD") || upper.contains("DOLAR") -> "USD"
    upper.contains("NIO") || upper.contains("CORDO") -> "NIO"
    else -> fallback
  }
}

@Composable
internal fun PaymentSection(
    state: BillingState,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    paymentLines: List<PaymentLine>,
    paymentModes: List<POSPaymentModeOption>,
    paidAmountBase: Double,
    totalAmount: Double,
    isCreditSale: Boolean,
    onAddPaymentLine: (PaymentLine) -> Unit,
    onRemovePaymentLine: (Int) -> Unit,
    onPaymentCurrencySelected: (String) -> Unit,
    paymentListMaxHeight: Dp = 240.dp,
) {
  val strings = LocalAppStrings.current
  val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
  var selectedMode by remember(modeOptions) { mutableStateOf("") }
  val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
  val requiresReference = remember(selectedModeOption) { requiresReference(selectedModeOption) }
  val inferredCurrency =
      remember(selectedMode, baseCurrency) { inferCurrencyFromModeName(selectedMode, baseCurrency) }
  val modeCurrency =
      remember(state, selectedMode) {
        (state as? BillingState.Success)?.paymentModeCurrencyByMode?.get(selectedMode)
      }
  var selectedCurrency by
      remember(selectedMode, baseCurrency) { mutableStateOf(modeCurrency ?: inferredCurrency) }
  var amountInput by remember { mutableStateOf("") }
  var amountValue by remember { mutableStateOf(0.0) }
  var rateInput by remember { mutableStateOf("1.0") }
  var referenceInput by remember { mutableStateOf("") }

  LaunchedEffect(selectedMode, modeCurrency, inferredCurrency) {
    selectedCurrency = modeCurrency?.trim()?.uppercase() ?: inferredCurrency
  }

  LaunchedEffect(selectedCurrency, exchangeRateByCurrency) {
    onPaymentCurrencySelected(selectedCurrency)
    if (selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
      rateInput = "1.0"
    } else {
      exchangeRateByCurrency[selectedCurrency.uppercase()]?.let { rate ->
        rateInput = rate.toString()
      }
    }
  }

  LaunchedEffect(selectedMode) { referenceInput = "" }
  Column(modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp)) {
    val pendingAmount = (totalAmount - paidAmountBase).coerceAtLeast(0.0)

    AnimatedVisibility(
        visible = isCreditSale,
        enter = fadeIn(animationSpec = tween(260)) + expandVertically(animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180)),
    ) {
      Column {
        Text(
            strings.billing.partialPaymentHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
      }
    }

    PaymentModeSelector(
        selectedMode = selectedMode,
        modeOptions = modeOptions,
        onModeSelected = { selectedMode = it },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(12.dp))

    val rateValue = rateInput.toDoubleOrNull() ?: 0.0
    val canAdd =
        amountValue > 0.0 &&
            rateValue > 0.0 &&
            selectedMode.isNotBlank() &&
            (!requiresReference || referenceInput.isNotBlank())

    PaymentAmountEntry(
        selectedCurrency = selectedCurrency,
        baseCurrency = baseCurrency,
        amountInput = amountInput,
        amountValue = amountValue,
        onAmountInputChange = { amountInput = it },
        onAmountValueChange = { amountValue = it },
        canAdd = canAdd,
        onClearAmount = {
          amountInput = ""
          amountValue = 0.0
        },
        onAddPayment = {
          onAddPaymentLine(
              buildPaymentLine(
                  selectedMode = selectedMode,
                  amountValue = amountValue,
                  selectedCurrency = selectedCurrency,
                  baseCurrency = baseCurrency,
                  rateValue = rateValue,
                  referenceInput = referenceInput,
              )
          )
          amountInput = ""
          amountValue = 0.0
          referenceInput = ""
          if (selectedCurrency == baseCurrency) {
            rateInput = "1.0"
          }
        },
        rateInput = rateInput,
        modifier = Modifier.fillMaxWidth(),
    )

    if (requiresReference) {
      PaymentReferenceField(
          selectedMode = selectedMode,
          referenceInput = referenceInput,
          onReferenceInputChange = { referenceInput = it },
          modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(12.dp))
    }

    PaymentAmountSummary(
        baseCurrency = baseCurrency,
        paidAmountBase = paidAmountBase,
        pendingAmount = pendingAmount,
    )

    Spacer(Modifier.height(10.dp))

    RegisteredPaymentsCard(
        paymentLines = paymentLines,
        baseCurrency = baseCurrency,
        paymentListMaxHeight = paymentListMaxHeight,
        onRemovePaymentLine = onRemovePaymentLine,
    )

    Spacer(Modifier.height(8.dp))
  }
}

enum class DiscountInputType {
  Code,
  Percent,
  Amount,
}

@Composable
internal fun DiscountShippingInputs(state: BillingState.Success, action: BillingAction) {
  val strings = LocalAppStrings.current
  val baseCurrency = state.currency ?: "USD"
  val initialType =
      remember(
          state.manualDiscountAmount,
          state.discountCode,
          state.manualDiscountPercent,
      ) {
        when {
          state.manualDiscountAmount > 0.0 -> DiscountInputType.Amount
          state.discountCode.isNotBlank() -> DiscountInputType.Code
          state.manualDiscountPercent > 0.0 -> DiscountInputType.Percent
          else -> DiscountInputType.Percent
        }
      }

  var discountType by rememberSaveable(initialType) { mutableStateOf(initialType) }

  fun selectDiscountType(type: DiscountInputType) {
    discountType = type
    when (type) {
      DiscountInputType.Amount -> {
        action.onDiscountCodeChanged("")
        action.onManualDiscountPercentChanged("")
      }

      DiscountInputType.Code -> {
        action.onManualDiscountAmountChanged("")
        action.onManualDiscountPercentChanged("")
      }

      DiscountInputType.Percent -> {
        action.onDiscountCodeChanged("")
        action.onManualDiscountAmountChanged("")
      }
    }
  }

  Column(
      modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(strings.billing.discountSectionTitle, style = MaterialTheme.typography.bodyMedium)
    val chipElevation =
        FilterChipDefaults.filterChipElevation(
            elevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp,
            disabledElevation = 0.dp,
        )
    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            containerColor = MaterialTheme.colorScheme.surface,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurface,
        )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
          selected = discountType == DiscountInputType.Amount,
          onClick = { selectDiscountType(DiscountInputType.Amount) },
          elevation = chipElevation,
          colors = chipColors,
          label = { Text(strings.billing.discountTypeAmount) },
      )
      FilterChip(
          selected = discountType == DiscountInputType.Code,
          onClick = { selectDiscountType(DiscountInputType.Code) },
          elevation = chipElevation,
          colors = chipColors,
          label = { Text(strings.billing.discountTypeCode) },
      )
      FilterChip(
          selected = discountType == DiscountInputType.Percent,
          onClick = { selectDiscountType(DiscountInputType.Percent) },
          elevation = chipElevation,
          colors = chipColors,
          label = { Text(strings.billing.discountTypePercent) },
      )
    }
    val paymentModes = state.paymentModes
    val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
    var selectedMode by remember(modeOptions) { mutableStateOf("") }
    val selectedCurrency =
        remember(state, selectedMode) {
          state.paymentModeCurrencyByMode[selectedMode] ?: baseCurrency
        }
    var amountInput by remember { mutableStateOf(state.manualDiscountAmount.toString()) }
    var amountValue by remember { mutableStateOf(state.manualDiscountAmount) }
    var rateInput by remember { mutableStateOf("1.0") }
    when (discountType) {
      DiscountInputType.Amount -> {
        MoneyTextField(
            currencyCode = selectedCurrency,
            rawValue = if (state.manualDiscountAmount > 0.0) amountInput else "",
            onRawValueChange = {
              amountInput = it
              action.onManualDiscountAmountChanged(amountInput)
            },
            label = "${strings.billing.discountAmountLabel} (${baseCurrency.toCurrencySymbol()})",
            enabled = true,
            onAmountChanged = { amountValue = it },
            supportingText = {
              if (!selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
                val rate = rateInput.toDoubleOrNull() ?: 0.0
                val base = amountValue * rate
                Text(
                    "${strings.billing.baseLabel}: ${formatAmount(baseCurrency.toCurrencySymbol(), base)}"
                )
              }
            },
            trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
      }

      DiscountInputType.Code -> {
        AppTextField(
            value = state.discountCode,
            onValueChange = action.onDiscountCodeChanged,
            label = strings.billing.discountCodeLabel,
            placeholder = strings.billing.discountCodePlaceholder,
            trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
      }

      DiscountInputType.Percent -> {
        AppTextField(
            value =
                if (state.manualDiscountPercent > 0.0) state.manualDiscountPercent.toString()
                else "",
            onValueChange = action.onManualDiscountPercentChanged,
            label = strings.billing.discountPercentLabel,
            trailingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
            keyboardOptions =
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
    if (state.deliveryCharges.isNotEmpty()) {
      Text(strings.billing.shippingSectionTitle, style = MaterialTheme.typography.bodyMedium)
      val deliveryChargeLabel =
          state.selectedDeliveryCharge?.label ?: strings.billing.deliveryChargePlaceholder
      var deliveryExpanded by remember { mutableStateOf(false) }
      ExposedDropdownMenuBox(
          expanded = deliveryExpanded,
          onExpandedChange = { deliveryExpanded = !deliveryExpanded },
      ) {
        AppTextField(
            value = deliveryChargeLabel,
            onValueChange = {},
            label = strings.billing.deliveryChargeLabel,
            modifier =
                Modifier.fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true,
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryExpanded)
            },
        )
        ExposedDropdownMenu(
            expanded = deliveryExpanded,
            onDismissRequest = { deliveryExpanded = false },
        ) {
          DropdownMenuItem(
              text = { Text(strings.billing.noShippingOption) },
              onClick = {
                action.onDeliveryChargeSelected(null)
                deliveryExpanded = false
              },
          )
          state.deliveryCharges.forEach { charge ->
            val chargeLabel =
                "${
                            charge.label
                        } (${formatAmount(baseCurrency.toCurrencySymbol(), charge.defaultRate)})"
            DropdownMenuItem(
                text = { Text(chargeLabel) },
                onClick = {
                  action.onDeliveryChargeSelected(charge)
                  deliveryExpanded = false
                },
            )
          }
        }
      }
    }
    Text(
        strings.billing.discountHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
  }
}

@Composable
internal fun CreditTermsSection(
    isCreditSale: Boolean,
    allowPartialPayment: Boolean,
    paymentTerms: List<PaymentTermBO>,
    selectedPaymentTerm: PaymentTermBO?,
    creditSaleTooltipMessage: String?,
    onCreditSaleChanged: (Boolean) -> Unit,
    onPaymentTermSelected: (PaymentTermBO?) -> Unit,
) {
  val strings = LocalAppStrings.current
  val hasCreditWarning = !creditSaleTooltipMessage.isNullOrBlank()
  Column(
      modifier =
          Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp)
              .animateContentSize(animationSpec = tween(260)),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val canEnableCredit = paymentTerms.isNotEmpty()
    if (allowPartialPayment) {
      Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          border =
              BorderStroke(
                  width = if (hasCreditWarning) 1.5.dp else 1.dp,
                  color =
                      if (hasCreditWarning) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.outlineVariant,
              ),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(strings.billing.creditSaleLabel, style = MaterialTheme.typography.bodyMedium)
          Switch(
              checked = isCreditSale,
              onCheckedChange = onCreditSaleChanged,
              enabled = canEnableCredit,
              colors =
                  SwitchDefaults.colors(
                      checkedThumbColor = MaterialTheme.colorScheme.primary,
                      checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                      checkedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                      uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      uncheckedTrackColor =
                          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                      uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                      disabledCheckedThumbColor =
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                      disabledCheckedTrackColor =
                          MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                      disabledUncheckedThumbColor =
                          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                      disabledUncheckedTrackColor =
                          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                  ),
          )
        }
      }
    }

    AnimatedVisibility(
        visible = allowPartialPayment && isCreditSale,
        enter = fadeIn(animationSpec = tween(260)) + expandVertically(animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180)),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.billing.paymentTermsLabel, style = MaterialTheme.typography.bodyMedium)
        var templateExpanded by remember { mutableStateOf(false) }
        val templateLabel = selectedPaymentTerm?.name ?: strings.billing.paymentTermPlaceholder
        ExposedDropdownMenuBox(
            expanded = templateExpanded,
            onExpandedChange = { templateExpanded = !templateExpanded },
        ) {
          AppTextField(
              value = templateLabel,
              onValueChange = {},
              label = strings.billing.paymentTermLabel,
              placeholder = strings.billing.paymentTermPlaceholder,
              modifier =
                  Modifier // .fillMaxWidth()
                      .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
              readOnly = true,
              trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded)
              },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          )
          ExposedDropdownMenu(
              expanded = templateExpanded,
              onDismissRequest = { templateExpanded = false },
          ) {
            paymentTerms.forEach { term ->
              DropdownMenuItem(
                  text = { Text(term.name) },
                  onClick = {
                    onPaymentTermSelected(term)
                    templateExpanded = false
                  },
              )
            }
          }
        }
        selectedPaymentTerm?.let { term ->
          val creditDays = term.creditDays ?: 0
          val creditMonths = term.creditMonths ?: 0
          val termsLabel =
              buildString {
                    if (creditMonths > 0) {
                      append("$creditMonths mes(es)")
                    }
                    if (creditDays > 0) {
                      if (isNotEmpty()) append(" + ")
                      append("$creditDays dia(s)")
                    }
                  }
                  .ifBlank { strings.billing.sameDayLabel }
          Text(
              text = "${strings.billing.termsLabel}: $termsLabel",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          term.description
              ?.takeIf { it.isNotBlank() }
              ?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
        }
      }
    }

    AnimatedVisibility(
        visible = allowPartialPayment && !isCreditSale && !canEnableCredit,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180)),
    ) {
      Text(
          text = strings.billing.noPaymentTermsAvailable,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun CreditSalesDisabledBanner(message: String) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
  ) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
  }
}

private fun Double.formatQty(): String {
  return if (this % 1.0 == 0.0) {
    this.toInt().toString()
  } else {
    this.toString()
  }
}

internal fun requiresReference(option: POSPaymentModeOption?): Boolean {
  val type = option?.type?.trim().orEmpty()
  return type.equals("Bank", ignoreCase = true) ||
      type.equals("Card", ignoreCase = true) ||
      option?.modeOfPayment?.contains("bank", ignoreCase = true) == true ||
      option?.modeOfPayment?.contains("card", ignoreCase = true) == true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
      label = { Text(label) },
      placeholder = placeholder?.let { { Text(it) } },
      singleLine = singleLine,
      enabled = enabled,
      readOnly = readOnly,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      shape = RoundedCornerShape(14.dp),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor =
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
          ),
  )
}

data class MoneyUiSpec(
    val code: String,
    val decimals: Int = 2,
    val groupSep: Char = ',',
    val decimalSep: Char = '.',
)

private fun moneyUiSpec(currencyCode: String, fallbackDecimals: Int = 2): MoneyUiSpec {
  return when (val code = currencyCode.trim().uppercase()) {
    "NIO" -> MoneyUiSpec(code = code, decimals = 2, groupSep = ',', decimalSep = '.')
    "USD" -> MoneyUiSpec(code = code, decimals = 2, groupSep = ',', decimalSep = '.')
    "EUR" -> MoneyUiSpec(code = code, decimals = 2, groupSep = '.', decimalSep = ',')
    else -> MoneyUiSpec(code = code, decimals = fallbackDecimals, groupSep = ',', decimalSep = '.')
  }
}

private fun sanitizeMoneyInput(input: String, maxDecimals: Int): String {
  val s = input.trim().replace(" ", "")
  val filtered = s.filter { it.isDigit() || it == '.' || it == ',' }
  if (filtered.isBlank()) return ""

  val lastDot = filtered.lastIndexOf('.')
  val lastComma = filtered.lastIndexOf(',')
  val decIdx = maxOf(lastDot, lastComma)

  fun normalizeInt(digits: String): String = digits.trimStart('0').ifBlank { "0" }

  return if (decIdx >= 0) {
    val intDigits = filtered.take(decIdx).filter { it.isDigit() }
    val decDigits = filtered.substring(decIdx + 1).filter { it.isDigit() }.take(maxDecimals)
    normalizeInt(intDigits) + "." + decDigits
  } else {
    val intDigits = filtered.filter { it.isDigit() }
    normalizeInt(intDigits)
  }
}

private fun parseMoneyToDouble(raw: String): Double =
    raw.trim().let { if (it.endsWith(".")) it.dropLast(1) else it }.toDoubleOrNull() ?: 0.0

private fun normalizeRawMoneyInput(input: String, maxDecimals: Int): String {
  val s = input.trim().replace(" ", "")
  if (s.isBlank()) return ""

  // Permitimos dígitos y separadores . ,
  val filtered = s.filter { it.isDigit() || it == '.' || it == ',' }
  if (filtered.isBlank()) return ""

  // Usamos el ÚLTIMO separador como decimal, el resto se considera miles
  val lastDot = filtered.lastIndexOf('.')
  val lastComma = filtered.lastIndexOf(',')
  val decIdx = maxOf(lastDot, lastComma)

  fun cleanIntDigits(d: String): String {
    val digits = d.filter { it.isDigit() }
    // Evita "00012" -> "12"
    val trimmed = digits.trimStart('0')
    return trimmed.ifBlank { "0" }
  }

  return if (decIdx >= 0) {
    val intDigits = cleanIntDigits(filtered.take(decIdx))
    val decDigits = filtered.substring(decIdx + 1).filter { it.isDigit() }.take(maxDecimals)
    "$intDigits.$decDigits" // raw siempre con '.'
  } else {
    cleanIntDigits(filtered)
  }
}

private class MoneyVisualTransformation(
    private val spec: MoneyUiSpec,
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val raw = text.text
    val normalized = normalizeRawMoneyInput(raw, spec.decimals)

    // Mantén el raw original (lo que el usuario edita) tal cual; el display se basa en normalized.
    // Para el mapping, usamos el mismo "raw" (porque offsets se miden sobre text.text).
    // OJO: lo ideal es que tu TextField.value SIEMPRE sea normalized.
    // (abajo te lo dejo así, para que el mapping sea exacto)
    val value = normalized

    val dotIndex = value.indexOf('.')
    val hasDot = dotIndex >= 0
    val intPart = if (hasDot) value.take(dotIndex) else value
    val decRaw = if (hasDot) value.substring(dotIndex + 1) else ""

    // --- build grouped int + mapping int offsets ---
    val n = intPart.length
    val mapIntOffsets = IntArray(n + 1)
    val groupedInt = StringBuilder()

    var rawIntOffset = 0
    for (ch in intPart) {
      mapIntOffsets[rawIntOffset] = groupedInt.length
      groupedInt.append(ch)
      rawIntOffset++

      val remaining = n - rawIntOffset
      if (remaining > 0 && remaining % 3 == 0) {
        groupedInt.append(spec.groupSep)
      }
    }
    mapIntOffsets[n] = groupedInt.length

    val decShown = decRaw.take(spec.decimals).padEnd(spec.decimals, '0')
    val transformed = buildString {
      append(groupedInt)
      append(spec.decimalSep)
      append(decShown)
    }

    val intTransLen = groupedInt.length
    val decStart = intTransLen + 1

    val offsetMapping =
        object : OffsetMapping {
          override fun originalToTransformed(offset: Int): Int {
            val o = offset.coerceIn(0, value.length)

            // offsets dentro del entero
            val intLen = intPart.length
            if (!hasDot) {
              return if (o <= intLen) mapIntOffsets[o] else intTransLen
            }

            // has dot
            return when {
              o <= intLen -> mapIntOffsets[o]
              o == intLen + 1 -> decStart // cursor “después del punto”
              else -> {
                val decOffset = (o - (intLen + 1)).coerceAtMost(spec.decimals)
                decStart + decOffset
              }
            }
          }

          override fun transformedToOriginal(offset: Int): Int {
            val t = offset.coerceIn(0, transformed.length)

            // dentro del entero (incluye comas)
            if (t <= intTransLen) {
              // busca el mayor rawIntOffset cuyo mapped <= t
              var i = 0
              while (i < mapIntOffsets.size && mapIntOffsets[i] <= t) i++
              return (i - 1).coerceAtLeast(0)
            }

            // en la parte decimal (siempre existe visualmente)
            if (!hasDot) {
              // si no hay punto en raw, no permitimos que el cursor se meta en los decimales
              // “falsos”
              return intPart.length
            }

            // hay punto en raw: permitimos editar decimales
            if (t == intTransLen) return intPart.length
            if (t == decStart) return intPart.length + 1

            val decOffset = (t - decStart).coerceIn(0, spec.decimals)
            val rawDecLen = decRaw.length.coerceAtMost(spec.decimals)
            val clamped = decOffset.coerceAtMost(rawDecLen)
            return (intPart.length + 1 + clamped)
          }
        }

    return TransformedText(AnnotatedString(transformed), offsetMapping)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTextField(
    currencyCode: String,
    rawValue: String,
    onRawValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Monto",
    enabled: Boolean = true,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onAmountChanged: (Double) -> Unit = {},
) {
  val spec = remember(currencyCode) { moneyUiSpec(currencyCode) }
  val symbol =
      remember(currencyCode) {
        val s = currencyCode.toCurrencySymbol()
        s.ifBlank { currencyCode }
      }
  val transformation = remember(spec) { MoneyVisualTransformation(spec) }

  var tfv by
      remember(spec.decimals) {
        val sanitized = sanitizeMoneyInput(rawValue, spec.decimals)
        mutableStateOf(TextFieldValue(sanitized, selection = TextRange(sanitized.length)))
      }

  LaunchedEffect(rawValue, spec.decimals) {
    val sanitized = sanitizeMoneyInput(rawValue, spec.decimals)
    if (sanitized != tfv.text) {
      val nextCursor = tfv.selection.start.coerceIn(0, sanitized.length)
      tfv = TextFieldValue(text = sanitized, selection = TextRange(nextCursor))
    }
  }

  LaunchedEffect(tfv.text) { onAmountChanged(parseMoneyToDouble(tfv.text)) }

  TextField(
      value = tfv,
      onValueChange = { typed ->
        val sanitized = sanitizeMoneyInput(typed.text, spec.decimals)
        val typedCursor = typed.selection.end.coerceIn(0, typed.text.length)
        val sanitizedPrefix = sanitizeMoneyInput(typed.text.take(typedCursor), spec.decimals)
        val nextCursor = sanitizedPrefix.length.coerceIn(0, sanitized.length)
        tfv = typed.copy(text = sanitized, selection = TextRange(nextCursor))
        onRawValueChange(sanitized)
      },
      modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
      label = { Text(label) },
      prefix = {
        Text(
            text = symbol.ifBlank { symbol },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
      },
      trailingIcon = trailingIcon,
      singleLine = true,
      enabled = enabled,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
      visualTransformation = transformation,
      shape = RoundedCornerShape(14.dp),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              unfocusedContainerColor =
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
          ),
  )
}
