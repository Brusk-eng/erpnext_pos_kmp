package com.erpnext.pos.views.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.AppStrings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.datetimeNow

@Composable
internal fun HomeOpenedContent(
    userName: String,
    isCompactHeightPhone: Boolean,
    metrics: HomeMetrics,
    actions: HomeAction,
    syncState: SyncState,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          if (isCompactHeightPhone) {
            modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          } else {
            modifier.fillMaxWidth()
          },
      horizontalAlignment = Alignment.Start,
  ) {
    Text(
        "Bienvenido $userName",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = datetimeNow(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(if (isCompactHeightPhone) 12.dp else 24.dp))

    BISection(
        metrics = metrics,
        actions = actions,
        modifier =
            if (isCompactHeightPhone) {
              Modifier.fillMaxWidth()
            } else {
              Modifier.weight(1f).fillMaxWidth()
            },
    )

    Spacer(Modifier.height(if (isCompactHeightPhone) 12.dp else 24.dp))

    if (syncState is SyncState.SYNCING) {
      SyncStatusCard(syncState = syncState, strings = strings, onCancel = { actions.cancelSync() })
    }
  }
}

@Composable
internal fun HomeClosedContent(modifier: Modifier = Modifier) {
  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Text(
        text = "¡Es hora de empezar a vender!",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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

@Composable
internal fun OpenCashboxButton(
    isCashboxOpen: Boolean,
    isCompactWidthPhone: Boolean,
    isSyncing: Boolean,
    onClick: () -> Unit,
) {
  if (isCashboxOpen) return
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth().padding(bottom = if (isCompactWidthPhone) 88.dp else 16.dp),
      enabled = !isSyncing,
      colors =
          ButtonDefaults.buttonColors(
              containerColor =
                  if (!isCashboxOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
          ),
  ) {
    Text(
        text = if (isCashboxOpen) "Cerrar Caja" else "Abrir Caja",
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
  }
}

@Composable
private fun SyncStatusCard(syncState: SyncState.SYNCING, strings: AppStrings, onCancel: () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "sync_icon_transition")
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
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector = Icons.Filled.Sync,
          contentDescription = "Sincronizando",
          modifier = Modifier.size(40.dp).rotate(angle),
          tint = MaterialTheme.colorScheme.primary,
      )

      Spacer(Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "Sincronizando datos...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = syncState.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        if (syncState.currentStep != null && syncState.totalSteps != null && syncState.totalSteps > 0) {
          Spacer(Modifier.height(6.dp))
          Text(
              text = "${strings.settings.syncStepLabel} ${syncState.currentStep} ${strings.settings.syncStepOfLabel} ${syncState.totalSteps}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Button(
          onClick = onCancel,
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
              ),
      ) {
        Text(strings.settings.syncCancelButton)
      }
    }
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
  }
}

@Composable
internal fun FullScreenErrorMessage(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(Icons.Filled.CloudOff, "Error", Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
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
internal fun FullScreenLoadingIndicator(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center),
        trackColor = Color.Blue,
        color = Color.Cyan,
        strokeWidth = 2.dp,
    )
  }
}
