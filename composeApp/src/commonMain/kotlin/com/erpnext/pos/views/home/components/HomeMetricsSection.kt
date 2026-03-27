package com.erpnext.pos.views.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.toCurrencySymbol

@Composable
internal fun BISection(metrics: HomeMetrics, actions: HomeAction, modifier: Modifier = Modifier) {
  val currencyMetrics =
      if (metrics.currencyMetrics.isNotEmpty()) {
        metrics.currencyMetrics
      } else {
        listOf(
            CurrencyHomeMetric(
                currency = "NIO",
                totalSalesToday = metrics.totalSalesToday,
                invoicesToday = metrics.invoicesToday,
                avgTicket = metrics.avgTicket,
                customersToday = metrics.customersToday,
                outstandingTotal = metrics.outstandingTotal,
            )
        )
      }

  if (currencyMetrics.isEmpty()) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
          text = "Aún no hay métricas disponibles.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  val preferredCurrency = currencyMetrics.first().currency
  var selectedCurrency by remember(currencyMetrics, preferredCurrency) { mutableStateOf(preferredCurrency) }
  val selectedMetric = currencyMetrics.firstOrNull { it.currency == selectedCurrency } ?: currencyMetrics.first()
  val symbol = selectedMetric.currency.toCurrencySymbol().ifBlank { selectedMetric.currency }

  LazyColumn(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(bottom = 16.dp),
  ) {
    item {
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 840.dp
        if (isWide) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionsGrid(modifier = Modifier.weight(1f), actions = actions)
          }
        } else {
          Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionsGrid(modifier = Modifier.fillMaxWidth(), actions = actions)
          }
        }
      }
    }
    item { KpiRow(metric = selectedMetric, symbol = symbol) }
  }
}

@Composable
private fun QuickActionsGrid(modifier: Modifier = Modifier, actions: HomeAction) {
  val quickActions =
      listOf(
          ActionItem(
              "Sincronizar ahora",
              Icons.Filled.Sync,
              MaterialTheme.colorScheme.primary,
              MaterialTheme.colorScheme.onPrimary,
              action = { actions.sync() },
          ),
          ActionItem(
              "Reconciliación",
              Icons.Filled.Shield,
              MaterialTheme.colorScheme.tertiary,
              MaterialTheme.colorScheme.onTertiary,
              action = { actions.onOpenReconciliation() },
          ),
          ActionItem(
              "Ajustes POS",
              Icons.Filled.Settings,
              MaterialTheme.colorScheme.secondary,
              MaterialTheme.colorScheme.onSecondary,
              action = { actions.onOpenSettings() },
          ),
          ActionItem(
              "Cerrar caja",
              Icons.AutoMirrored.Filled.Logout,
              MaterialTheme.colorScheme.error,
              MaterialTheme.colorScheme.onError,
              action = { actions.onCloseCashbox() },
          ),
          ActionItem(
              "Cerrar sesión",
              Icons.Filled.Close,
              MaterialTheme.colorScheme.surfaceVariant,
              MaterialTheme.colorScheme.onSurfaceVariant,
              action = { actions.onLogout() },
          ),
          ActionItem(
              "Resumen diario",
              Icons.Filled.LocalOffer,
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.onSurface,
              action = { actions.onOpenReconciliation() },
          ),
      )

  Card(
      modifier = modifier,
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
          text = "Acciones rápidas",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(12.dp))
      quickActions.chunked(3).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          row.forEach { action -> QuickActionButton(action, Modifier.weight(1f)) }
          if (row.size < 3) Spacer(Modifier.weight((3 - row.size).toFloat()))
        }
        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

private data class ActionItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val contentColor: Color,
    val action: () -> Unit,
)

@Composable
private fun QuickActionButton(action: ActionItem, modifier: Modifier = Modifier) {
  Button(
      onClick = action.action,
      modifier = modifier.height(56.dp),
      colors = ButtonDefaults.buttonColors(containerColor = action.color, contentColor = action.contentColor),
  ) {
    Icon(imageVector = action.icon, contentDescription = null, tint = action.contentColor, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = action.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun KpiRow(metric: CurrencyHomeMetric, symbol: String) {
  val cards =
      listOf(
          KpiCell("Tickets", metric.invoicesToday.toString()),
          KpiCell("Ticket prom.", "$symbol ${formatAmount(metric.avgTicket)}"),
          KpiCell("Clientes", metric.customersToday.toString()),
          KpiCell("Pendiente", "$symbol ${formatAmount(metric.outstandingTotal)}"),
      )
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val isWide = maxWidth >= 840.dp
    if (isWide) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.forEach { cell -> KpiTile(title = cell.title, value = cell.value, modifier = Modifier.weight(1f)) }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { row ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { cell -> KpiTile(title = cell.title, value = cell.value, modifier = Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
          }
        }
      }
    }
  }
}

private data class KpiCell(val title: String, val value: String)

@Composable
private fun KpiTile(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
      Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
  }
}

private fun formatAmount(value: Double): String = formatDoubleToString(value, 2)
