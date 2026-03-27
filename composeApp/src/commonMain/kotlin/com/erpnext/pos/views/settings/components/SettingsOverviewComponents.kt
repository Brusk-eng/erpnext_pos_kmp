package com.erpnext.pos.views.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.settings.POSSettingBO

@Composable
internal fun SettingsOverviewRow(
    settings: POSSettingBO,
    syncSettings: SyncSettings,
    syncState: SyncState,
    onSyncNow: () -> Unit,
    onCancelSync: () -> Unit,
    compact: Boolean,
    isOnline: Boolean,
) {
  BoxWithConstraints {
    val isWide = maxWidth > 780.dp
    if (isWide) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsInfoCard(settings = settings, modifier = Modifier.weight(0.42f), compact = compact)
        SettingsHeroCard(
            syncSettings = syncSettings,
            syncState = syncState,
            onSyncNow = onSyncNow,
            onCancelSync = onCancelSync,
            modifier = Modifier.weight(0.65f),
            compact = compact,
            isOnline = isOnline,
            offlineMode = settings.offlineMode,
        )
      }
    } else {
      Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SettingsInfoCard(settings = settings, compact = compact)
        SettingsHeroCard(
            syncSettings = syncSettings,
            syncState = syncState,
            onSyncNow = onSyncNow,
            onCancelSync = onCancelSync,
            compact = compact,
            isOnline = isOnline,
            offlineMode = settings.offlineMode,
        )
      }
    }
  }
}

@Composable
private fun SettingsInfoCard(
    settings: POSSettingBO,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  val tokens = settingsTokens()
  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = tokens.cardContainer),
  ) {
    Column(modifier = Modifier.padding(if (compact) 14.dp else 18.dp)) {
      Text(
          text = "Contexto POS",
          style =
              MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 0.1.sp,
              ),
          color = tokens.titleColor,
      )
      Spacer(modifier = Modifier.height(if (compact) 8.dp else 12.dp))
      SummaryGrid(
          items =
              listOf(
                  SummaryField(strings.settings.companyLabel, settings.company),
                  SummaryField(strings.settings.posProfileLabel, settings.posProfile),
                  SummaryField("Apertura (POE)", settings.openingEntryId),
                  SummaryField(strings.settings.warehouseLabel, settings.warehouse),
                  SummaryField(strings.settings.priceListLabel, settings.priceList),
              )
      )
    }
  }
}

@Composable
private fun SettingsHeroCard(
    syncSettings: SyncSettings,
    syncState: SyncState,
    onSyncNow: () -> Unit,
    onCancelSync: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean,
    isOnline: Boolean,
    offlineMode: Boolean,
) {
  val tokens = settingsTokens()
  val gradient = Brush.verticalGradient(colors = listOf(tokens.heroPrimary, tokens.heroSecondary))
  val strings = LocalAppStrings.current
  val statusLabel =
      when (syncState) {
        SyncState.IDLE -> strings.settings.syncStatusIdle
        SyncState.SUCCESS -> strings.settings.syncStatusSuccess
        is SyncState.ERROR -> strings.settings.syncStatusError
        is SyncState.SYNCING -> strings.settings.syncStatusSyncing
      }
  val statusStyle =
      when (syncState) {
        SyncState.IDLE ->
            StatusStyle(container = tokens.heroText.copy(alpha = 0.12f), content = tokens.heroText)
        SyncState.SUCCESS ->
            StatusStyle(container = tokens.heroText.copy(alpha = 0.12f), content = tokens.heroText)
        is SyncState.ERROR ->
            StatusStyle(
                container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                content = MaterialTheme.colorScheme.onErrorContainer,
            )
        is SyncState.SYNCING ->
            StatusStyle(container = tokens.heroText.copy(alpha = 0.18f), content = tokens.heroText)
      }

  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(26.dp),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .background(gradient)
                .graphicsLayer { clip = true }
                .padding(if (compact) 14.dp else 18.dp)
    ) {
      Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
              text = strings.settings.syncTitle,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = tokens.heroText,
          )
          StatusPill(
              text = statusLabel,
              containerColor = statusStyle.container,
              contentColor = statusStyle.content,
          )
        }
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusPill(
              text = if (isOnline) "Conectado" else "Sin conexión",
              containerColor =
                  if (isOnline) {
                    tokens.heroText.copy(alpha = 0.16f)
                  } else {
                    MaterialTheme.colorScheme.errorContainer
                  },
              contentColor =
                  if (isOnline) {
                    tokens.heroText
                  } else {
                    MaterialTheme.colorScheme.onErrorContainer
                  },
          )
          StatusPill(
              text = if (offlineMode) "Modo offline activo" else "Modo offline: off",
              containerColor = tokens.heroText.copy(alpha = 0.12f),
              contentColor = tokens.heroText,
          )
        }
        Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))
        Text(
            text = syncSettings.lastSyncAt?.toErpDateTime() ?: strings.settings.lastSyncNever,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.heroText,
        )
        Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))
        Button(
            onClick = onSyncNow,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = tokens.heroButton,
                    contentColor = tokens.heroButtonText,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.settings.syncNowButton)
        }
        if (syncState is SyncState.SYNCING) {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
              onClick = onCancelSync,
              modifier = Modifier.fillMaxWidth(),
              border = BorderStroke(1.dp, tokens.heroText.copy(alpha = 0.35f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.heroButtonText),
          ) {
            Text(strings.settings.syncCancelButton)
          }
        }
      }
    }
  }
}

internal data class SummaryField(val label: String, val value: String)

@Composable
private fun SummaryGrid(items: List<SummaryField>) {
  val rows = items.chunked(2)
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    rows.forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryItem(row[0], modifier = Modifier.weight(1f))
        if (row.size > 1) {
          SummaryItem(row[1], modifier = Modifier.weight(1f))
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun SummaryItem(item: SummaryField, modifier: Modifier = Modifier) {
  val tokens = settingsTokens()
  Column(modifier = modifier) {
    Text(text = item.label, style = MaterialTheme.typography.labelSmall, color = tokens.subtleText)
    Text(
        text = item.value,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = tokens.titleColor,
    )
  }
}

private data class StatusStyle(val container: Color, val content: Color)

@Composable
internal fun StatusPill(text: String, containerColor: Color, contentColor: Color) {
  Box(
      modifier =
          Modifier.background(containerColor, RoundedCornerShape(999.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
  ) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = contentColor)
  }
}

internal data class SettingsTokens(
    val cardContainer: Color,
    val cardBorder: Color,
    val divider: Color,
    val titleColor: Color,
    val valueColor: Color,
    val subtleText: Color,
    val mutedText: Color,
    val accent: Color,
    val iconTint: Color,
    val heroPrimary: Color,
    val heroSecondary: Color,
    val heroBorder: Color,
    val heroText: Color,
    val heroButton: Color,
    val heroButtonText: Color,
)

@Composable
internal fun settingsTokens(colorScheme: ColorScheme = MaterialTheme.colorScheme): SettingsTokens =
    SettingsTokens(
        cardContainer = colorScheme.surface,
        cardBorder = colorScheme.outlineVariant.copy(alpha = 0.55f),
        divider = colorScheme.outlineVariant.copy(alpha = 0.35f),
        titleColor = colorScheme.onSurface,
        valueColor = colorScheme.onSurfaceVariant,
        subtleText = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
        mutedText = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        accent = colorScheme.primary,
        iconTint = colorScheme.onSurfaceVariant,
        heroPrimary = colorScheme.primaryContainer.copy(alpha = 0.96f),
        heroSecondary = colorScheme.secondaryContainer.copy(alpha = 0.96f),
        heroBorder = colorScheme.outlineVariant.copy(alpha = 0.4f),
        heroText = colorScheme.onPrimaryContainer,
        heroButton = colorScheme.onPrimaryContainer,
        heroButtonText = colorScheme.primary,
    )
