@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.navigation.LocalTopBarController
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.datetimeNow
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeState,
    actions: HomeAction,
) {
  var showOpeningView by remember { mutableStateOf(false) }
  var currentProfiles by remember { mutableStateOf(emptyList<POSProfileSimpleBO>()) }
  var currentUser by remember { mutableStateOf<UserBO?>(null) }
  val snackbar: SnackbarController = koinInject()
  val syncState by actions.syncState.collectAsState()
  val strings = LocalAppStrings.current
  val topBarController = LocalTopBarController.current
  val homeMetrics by actions.homeMetrics.collectAsState()
  val openingState by actions.openingState.collectAsState()
  val isCashboxOpen by actions.isCashboxOpen().collectAsState()
  val windowSizeClass = rememberWindowSizeClass()
  val isCompactWidthPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
  val isCompactHeightPhone =
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact &&
          (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
              windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium)

  LaunchedEffect(uiState) {
    if (uiState is HomeState.POSProfiles) {
      currentProfiles = uiState.posProfiles
      currentUser = uiState.user
    }
  }

  LaunchedEffect(isCashboxOpen) {
    if (isCashboxOpen && showOpeningView) {
      showOpeningView = false
    }
  }

  LaunchedEffect(showOpeningView) { topBarController.update(isVisible = !showOpeningView) }
  DisposableEffect(Unit) { onDispose { topBarController.reset() } }

  if (showOpeningView) {
    CashboxOpeningScreen(
        uiState = uiState,
        profiles = currentProfiles,
        user = currentUser,
        openingState = openingState,
        onLoadOpeningProfile = actions.onLoadOpeningProfile,
        onOpenCashbox = actions.onOpenCashbox,
        onSelectProfile = { actions.onPosSelected(it) },
        onDismiss = {
          showOpeningView = false
          actions.initialState()
        },
        snackbar,
    )
  } else {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
      Column(
          modifier =
              Modifier.padding(paddingValues)
                  .fillMaxSize()
                  .background(MaterialTheme.colorScheme.background)
                  .padding(start = 12.dp, end = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        when (uiState) {
          is HomeState.Loading -> FullScreenLoadingIndicator()
          is HomeState.Error ->
              FullScreenErrorMessage(uiState.message, { actions.loadInitialData() })

          is HomeState.POSProfiles -> {
            // Saludo y banners
            if (isCashboxOpen) {
              Column(
                  modifier =
                      if (isCompactHeightPhone) {
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                      } else {
                        Modifier.fillMaxWidth()
                      },
                  horizontalAlignment = Alignment.Start,
              ) {
                Text(
                    "Bienvenido ${uiState.user.firstName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = datetimeNow(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
              }

              Spacer(Modifier.height(if (isCompactHeightPhone) 12.dp else 24.dp))

              BISection(
                  metrics = homeMetrics,
                  actions = actions,
                  modifier =
                      if (isCompactHeightPhone) {
                        Modifier.fillMaxWidth()
                      } else {
                        Modifier.weight(1f).fillMaxWidth()
                      },
              )

              Spacer(Modifier.height(if (isCompactHeightPhone) 12.dp else 24.dp))

              Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                when (syncState) {
                  is SyncState.SYNCING -> {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "sync_icon_transition")
                    val angle by
                        infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                                ),
                            label = "sync_icon_rotation",
                        )

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                    ) {
                      Row(
                          modifier = Modifier.fillMaxWidth().padding(16.dp),
                          verticalAlignment = Alignment.CenterVertically,
                      ) {
                        // Icono animado
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sincronizando",
                            modifier = Modifier.size(40.dp).rotate(angle), // Aplicamos la rotación
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.width(16.dp))

                        // Columna para los textos
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                              text = "Sincronizando datos...",
                              style = MaterialTheme.typography.titleMedium,
                              fontWeight = FontWeight.Bold,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                          Spacer(Modifier.height(4.dp))
                          Text(
                              // Mostramos el mensaje específico de la
                              // sincronización
                              text = (syncState as SyncState.SYNCING).message,
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                          )
                          val step = (syncState as SyncState.SYNCING).currentStep
                          val total = (syncState as SyncState.SYNCING).totalSteps
                          if (step != null && total != null && total > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text =
                                    "${strings.settings.syncStepLabel} $step ${strings.settings.syncStepOfLabel} $total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                          }
                        }
                        Button(
                            onClick = { actions.cancelSync() },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                        ) {
                          Text(strings.settings.syncCancelButton)
                        }
                      }
                    }
                  }

                  else -> {
                    // No se muestra nada si no se está sincronizando
                  }
                }
              }
            } else {
              Column(
                  modifier =
                      if (isCompactHeightPhone) {
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                      } else {
                        Modifier.weight(1f).fillMaxWidth()
                      },
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center,
              ) {
                Text(
                    text = "¡Es hora de empezar a vender!",
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Abrir caja",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp),
                )
              }
            }

            // Botón abrir caja
            if (!isCashboxOpen) {
              Button(
                  onClick = {
                    if (isCashboxOpen) {
                      actions.onCloseCashbox()
                    } else {
                      if (currentProfiles.isNotEmpty()) {
                        showOpeningView = true
                      }
                    }
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(bottom = if (isCompactWidthPhone) 88.dp else 16.dp),
                  enabled = syncState !is SyncState.SYNCING,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor =
                              if (!isCashboxOpen) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.error,
                      ),
              ) {
                Text(
                    text = if (isCashboxOpen) "Cerrar Caja" else "Abrir Caja",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
              }
            }
          }
          else -> {}  // FIXME: Better way?
        }
      }
    }
  }
}

@Composable
private fun BISection(metrics: HomeMetrics, actions: HomeAction, modifier: Modifier = Modifier) {
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
                    outstandingTotal = metrics.outstandingTotal
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
  var selectedCurrency by
      remember(currencyMetrics, preferredCurrency) { mutableStateOf(preferredCurrency) }
  val selectedMetric =
      currencyMetrics.firstOrNull { it.currency == selectedCurrency } ?: currencyMetrics.first()
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
                    QuickActionsGrid(modifier = Modifier.weight(1f), actions)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickActionsGrid(modifier = Modifier.fillMaxWidth(), actions)
                }
            }
        }
    }

    item { KpiRow(metric = selectedMetric, symbol = symbol) }

  }
}

@Composable
private fun QuickActionsGrid(modifier: Modifier = Modifier, actions: HomeAction) {
  val actions =
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
      actions.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
      colors =
          ButtonDefaults.buttonColors(
              containerColor = action.color,
              contentColor = action.contentColor,
          ),
  ) {
    Icon(
        imageVector = action.icon,
        contentDescription = null,
        tint = action.contentColor,
        modifier = Modifier.size(18.dp),
    )
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
        cards.forEach { cell ->
          KpiTile(title = cell.title, value = cell.value, modifier = Modifier.weight(1f))
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { row ->
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            row.forEach { cell ->
              KpiTile(title = cell.title, value = cell.value, modifier = Modifier.weight(1f))
            }
            if (row.size == 1) {
              Spacer(Modifier.weight(1f))
            }
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
      Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(4.dp))
      Text(
          text = value,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

private fun formatAmount(value: Double): String = formatDoubleToString(value, 2)

@Composable
private fun FullScreenErrorMessage(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
          Icons.Filled.CloudOff,
          "Error",
          Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(16.dp))
      Text(
          errorMessage,
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(16.dp))
      Button(onClick = onRetry) { Text("Reintentar") }
    }
  }
}

@Composable
private fun FullScreenLoadingIndicator(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center),
        trackColor = Color.Blue,
        color = Color.Cyan,
        strokeWidth = 2.dp,
    )
  }
}
// 1190
