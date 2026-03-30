package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun CustomerFilters(
    searchQuery: String,
    selectedState: String,
    isWideLayout: Boolean,
    totalCount: Int,
    pendingCount: Int,
    states: List<String> = listOf("Pendientes", "Sin Pendientes"),
    onQueryChange: (String) -> Unit,
    onStateChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
  var localSearchQuery by rememberSaveable { mutableStateOf(searchQuery) }

  LaunchedEffect(searchQuery) {
    if (searchQuery != localSearchQuery) {
      localSearchQuery = searchQuery
    }
  }

  LaunchedEffect(localSearchQuery) {
    delay(250)
    if (localSearchQuery != searchQuery) {
      onQueryChange(localSearchQuery)
    }
  }

  androidx.compose.foundation.layout.Column(modifier = modifier) {
    if (isWideLayout) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
          CustomerFilterChipRow(
              selectedState = selectedState,
              totalCount = totalCount,
              pendingCount = pendingCount,
              states = states,
              onStateChange = onStateChange,
          )
        }

        SearchTextField(
            searchQuery = localSearchQuery,
            onSearchQueryChange = { localSearchQuery = it },
            placeholderText = "Buscar cliente por nombre o teléfono...",
            modifier = Modifier.weight(1.2f),
        )
      }
    } else {
      LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
      ) {
        item {
          CustomerStateFilterChip(
              label = "Todos",
              value = "$totalCount",
              selected = selectedState == "Todos",
              onClick = { onStateChange("Todos") },
              accentBrush = customerFilterBrush(MaterialTheme.colorScheme.primary),
          )
        }
        items(states) { state ->
          val accent =
              if (state == "Pendientes") MaterialTheme.colorScheme.tertiary
              else MaterialTheme.colorScheme.secondary
          val value =
              if (state == "Pendientes") "$pendingCount"
              else "${(totalCount - pendingCount).coerceAtLeast(0)}"
          CustomerStateFilterChip(
              label = state,
              value = value,
              selected = selectedState == state,
              onClick = { onStateChange(state) },
              accentBrush = customerFilterBrush(accent),
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))

      SearchTextField(
          searchQuery = localSearchQuery,
          onSearchQueryChange = { localSearchQuery = it },
          placeholderText = "Buscar cliente por nombre o teléfono...",
      )
    }
  }
}

@Composable
private fun CustomerFilterChipRow(
    selectedState: String,
    totalCount: Int,
    pendingCount: Int,
    states: List<String>,
    onStateChange: (String?) -> Unit,
) {
  CustomerStateFilterChip(
      label = "Todos",
      value = "$totalCount",
      selected = selectedState == "Todos",
      onClick = { onStateChange("Todos") },
      accentBrush = customerFilterBrush(MaterialTheme.colorScheme.primary),
  )
  states.forEach { state ->
    val accent =
        if (state == "Pendientes") MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.secondary
    val value =
        if (state == "Pendientes") "$pendingCount"
        else "${(totalCount - pendingCount).coerceAtLeast(0)}"
    CustomerStateFilterChip(
        label = state,
        value = value,
        selected = selectedState == state,
        onClick = { onStateChange(state) },
        accentBrush = customerFilterBrush(accent),
    )
  }
}

@Composable
private fun CustomerStateFilterChip(
    label: String,
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentBrush: Brush,
) {
  AssistChip(
      onClick = onClick,
      label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(text = label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
          Surface(
              shape = androidx.compose.foundation.shape.CircleShape,
              color =
                  if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
            )
          }
        }
      },
      colors =
          AssistChipDefaults.assistChipColors(
              containerColor =
                  if (selected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surface,
              labelColor =
                  if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurface,
          ),
      border =
          androidx.compose.foundation.BorderStroke(
              1.dp,
              if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
              else MaterialTheme.colorScheme.outlineVariant,
          ),
      modifier = Modifier.background(accentBrush, shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)),
  )
}

private fun customerFilterBrush(accent: androidx.compose.ui.graphics.Color): Brush =
    Brush.horizontalGradient(
        listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.02f))
    )

@Composable
internal fun CustomerListPane(
    customers: androidx.paging.compose.LazyPagingItems<CustomerBO>,
    posCurrency: String,
    companyCurrency: String,
    cashboxManager: CashBoxManager,
    listState: LazyListState,
    showBackToTop: Boolean,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onSelect: (CustomerBO) -> Unit,
    onQuickAction: (CustomerBO, CustomerQuickActionType) -> Unit,
) {
  val scope = rememberCoroutineScope()
  Box(modifier = Modifier.fillMaxSize()) {
    CustomerListContent(
        customers = customers,
        posCurrency = posCurrency,
        companyCurrency = companyCurrency,
        cashboxManager = cashboxManager,
        isWideLayout = isWideLayout,
        isDesktop = isDesktop,
        listState = listState,
        onOpenQuickActions = onOpenQuickActions,
        onSelect = onSelect,
        onQuickAction = onQuickAction,
    )
    if (showBackToTop) {
      FilledTonalButton(
          onClick = { scope.launch { listState.animateScrollToItem(0) } },
          modifier = Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 10.dp),
      ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Back to top",
        )
      }
    }
  }
}

@Composable
internal fun CustomerListContent(
    customers: androidx.paging.compose.LazyPagingItems<CustomerBO>,
    posCurrency: String,
    companyCurrency: String,
    cashboxManager: CashBoxManager,
    listState: LazyListState,
    isWideLayout: Boolean,
    isDesktop: Boolean,
    onOpenQuickActions: (CustomerBO) -> Unit,
    onSelect: (CustomerBO) -> Unit,
    onQuickAction: (CustomerBO, CustomerQuickActionType) -> Unit,
) {
  val spacing = if (isWideLayout) 16.dp else 12.dp
  val companyCurr = normalizeCurrency(companyCurrency)
  val posCurr = normalizeCurrency(posCurrency)
  val contextExchangeRate = remember(cashboxManager) { cashboxManager.getContext()?.exchangeRate }

  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    items(
        count = customers.itemCount,
        key = { index -> customers[index]?.name ?: "customer_list_$index" },
    ) { index ->
      val customer = customers[index] ?: return@items
      CustomerItem(
          customer = customer,
          posCurrency = posCurr,
          companyCurrency = companyCurr,
          contextExchangeRate = contextExchangeRate,
          isDesktop = isDesktop,
          onSelect = onSelect,
          onOpenQuickActions = { onOpenQuickActions(customer) },
          onQuickAction = { actionType -> onQuickAction(customer, actionType) },
      )
    }
    if (customers.loadState.append is LoadState.Loading) {
      item(key = "customers_append_loading") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
    }
    val appendError = customers.loadState.append as? LoadState.Error
    if (appendError != null) {
      item(key = "customers_append_error") {
        OutlinedButton(onClick = { customers.retry() }, modifier = Modifier.fillMaxWidth()) {
          Text("Reintentar carga")
        }
      }
    }
  }
}
