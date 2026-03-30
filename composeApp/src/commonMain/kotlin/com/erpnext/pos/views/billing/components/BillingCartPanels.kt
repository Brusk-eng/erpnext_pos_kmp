package com.erpnext.pos.views.billing.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingState
import com.erpnext.pos.views.billing.CustomerSelector
import com.erpnext.pos.views.billing.LabCartHeader
import com.erpnext.pos.views.billing.LabCartItem
import com.erpnext.pos.views.billing.LabCategoryTabs
import com.erpnext.pos.views.billing.LabProductCard
import com.erpnext.pos.views.billing.LabSearchBar
import com.erpnext.pos.views.billing.PaymentTotalsRow
import kotlinx.coroutines.launch

@Composable
internal fun BillingProductBrowserPanel(
    products: LazyPagingItems<ItemBO>,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    accent: Color,
    onProductAdded: (ItemBO) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme
  val scope = rememberCoroutineScope()
  val gridState = rememberLazyGridState()
  val visibleProducts =
      remember(products.itemSnapshotList.items) {
        products.itemSnapshotList.items.filter { it.actualQty >= 1.0 }
      }
  val showBackToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
  val minGridItem = if (compact) 150.dp else 220.dp
  val gridHeightModifier =
      if (compact) Modifier.fillMaxWidth().height(360.dp) else Modifier.fillMaxSize()

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    LabSearchBar(
        value = searchQuery,
        onChange = onSearchQueryChange,
        onClear = onClearSearch,
    )
    LabCategoryTabs(
        categories = categories,
        selectedCategory = selectedCategory,
        onSelect = onSelectCategory,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = if (selectedCategory == "Todos") strings.customer.allLabel else selectedCategory,
          style = MaterialTheme.typography.titleSmall,
          color = colors.onSurfaceVariant,
      )
      Text(
          text = "(${visibleProducts.size})",
          style = MaterialTheme.typography.labelMedium,
          color = colors.onSurfaceVariant.copy(alpha = 0.75f),
      )
    }
    Box(modifier = Modifier.fillMaxWidth()) {
      LazyVerticalGrid(
          state = gridState,
          columns = GridCells.Adaptive(minSize = minGridItem),
          verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
          horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
          modifier = gridHeightModifier,
      ) {
        items(
            count = visibleProducts.size,
            key = { index -> visibleProducts[index].itemCode },
        ) { index ->
          val item = visibleProducts[index]
          LabProductCard(
              item = item,
              baseCurrency = baseCurrency,
              exchangeRateByCurrency = exchangeRateByCurrency,
              accent = accent,
              onClick = { onProductAdded(item) },
          )
        }
        if (products.loadState.append is LoadState.Loading) {
          item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
      }
      if (!compact && showBackToTop) {
        Button(
            onClick = { scope.launch { gridState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
          Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Back to top")
        }
      }
    }
  }
}

@Composable
internal fun BillingCartPanel(
    state: BillingState.Success,
    invoiceCurrency: String,
    secondaryCurrency: String?,
    toSecondary: (Double) -> Double?,
    action: BillingAction,
    accent: Color,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme

  Surface(
      color = colors.surface,
      shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
      tonalElevation = 2.dp,
      shadowElevation = if (compact) 8.dp else 12.dp,
      modifier =
          if (compact) modifier
          else modifier.widthIn(min = 320.dp, max = 420.dp).fillMaxHeight(),
  ) {
    Column(
        modifier =
            if (compact) Modifier.fillMaxWidth().padding(14.dp)
            else Modifier.fillMaxHeight().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 0.dp),
    ) {
      LabCartHeader(itemCount = state.cartItems.sumOf { it.quantity }.toInt(), accent = accent)
      if (!compact) {
        Spacer(Modifier.height(12.dp))
      }
      CustomerSelector(
          customers = state.customers,
          query = state.customerSearchQuery,
          onQueryChange = action.onCustomerSearchQueryChange,
          onCustomerSelected = action.onCustomerSelected,
      )
      if (!compact) {
        Spacer(Modifier.height(12.dp))
      }
      Text(
          text = strings.billing.cartSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = colors.onSurface,
      )
      if (state.cartItems.isEmpty()) {
        Box(
            modifier =
                if (compact) Modifier.fillMaxWidth().height(90.dp)
                else Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
          Text(
              text = strings.billing.cartEmptyTitle,
              style = MaterialTheme.typography.bodySmall,
              color = colors.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
            modifier =
                if (compact) Modifier.fillMaxWidth().heightIn(max = 220.dp)
                else Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(state.cartItems, key = { it.itemCode }) { item ->
            LabCartItem(
                item = item,
                baseCurrency = invoiceCurrency,
                exchangeRateByCurrency = state.exchangeRateByCurrency,
                onUpdateQuantity = { qty -> action.onQuantityChanged(item.itemCode, qty) },
                onRemove = { action.onRemoveItem(item.itemCode) },
            )
          }
        }
      }

      if (!compact) {
        Spacer(Modifier.height(12.dp))
      }
      BillingCartSummaryCard(
          state = state,
          invoiceCurrency = invoiceCurrency,
          secondaryCurrency = secondaryCurrency,
          toSecondary = toSecondary,
      )
      if (!compact) {
        Spacer(Modifier.height(12.dp))
      }
      Button(
          onClick = onCheckout,
          enabled = state.selectedCustomer != null && state.cartItems.isNotEmpty(),
          modifier = Modifier.fillMaxWidth(),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = colors.primary,
                  contentColor = colors.onPrimary,
              ),
      ) {
        Text(strings.billing.checkoutButton)
      }
      if (!compact) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = strings.billing.continueToPaymentsHint,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
internal fun BillingCartSummaryCard(
    state: BillingState.Success,
    invoiceCurrency: String,
    secondaryCurrency: String?,
    toSecondary: (Double) -> Double?,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val colors = MaterialTheme.colorScheme

  Card(
      modifier = modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
  ) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
          text = strings.billing.cartSummaryTitle,
          style = MaterialTheme.typography.labelLarge,
          color = colors.onSurface,
      )
      Text(
          text = "${strings.billing.customerSectionTitle}: ${state.selectedCustomer?.customerName ?: "--"}",
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
      )
      Text(
          text = "${strings.billing.cartItemsLabel}: ${state.cartItems.size}",
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
      )
      HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
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
      PaymentTotalsRow(
          "Total",
          invoiceCurrency,
          state.total,
          secondaryCurrencyCode = secondaryCurrency,
          secondaryAmount = toSecondary(state.total),
      )
    }
  }
}
