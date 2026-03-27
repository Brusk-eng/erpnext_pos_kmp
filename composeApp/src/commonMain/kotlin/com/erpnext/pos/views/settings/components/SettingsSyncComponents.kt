package com.erpnext.pos.views.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.SyncLogEntry
import com.erpnext.pos.domain.models.SyncLogStatus
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.settings.ExpandableSection
import com.erpnext.pos.views.settings.SettingItem
import com.erpnext.pos.views.settings.SettingSection
import com.erpnext.pos.views.settings.SettingToggle

@Composable
internal fun SyncSection(
    syncSettings: SyncSettings,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit,
    onTtlHoursClick: () -> Unit,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  val tokens = settingsTokens()
  SettingSection(
      title = strings.settings.syncTitle,
      icon = Icons.Outlined.Sync,
      compact = compact,
  ) {
    SyncTogglesColumn(
        autoSync = syncSettings.autoSync,
        syncOnStartup = syncSettings.syncOnStartup,
        wifiOnly = syncSettings.wifiOnly,
        useTtl = syncSettings.useTtl,
        onAutoSyncChanged = onAutoSyncChanged,
        onSyncOnStartupChanged = onSyncOnStartupChanged,
        onWifiOnlyChanged = onWifiOnlyChanged,
        onUseTtlChanged = onUseTtlChanged,
        ttlHours = syncSettings.ttlHours,
        onTtlHoursClick = onTtlHoursClick,
        compact = compact,
    )
    Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
    Text(
        text = strings.settings.syncBackgroundHint,
        style = MaterialTheme.typography.bodySmall,
        color = tokens.subtleText,
    )
  }
}

@Composable
internal fun SyncLogSection(entries: List<SyncLogEntry>, compact: Boolean) {
  val strings = LocalAppStrings.current
  val tokens = settingsTokens()
  ExpandableSection(
      title = strings.settings.syncLogTitle,
      icon = Icons.Outlined.History,
      compact = compact,
      initiallyExpanded = false,
  ) {
    if (entries.isEmpty()) {
      Text(
          text = strings.settings.syncLogEmpty,
          style = MaterialTheme.typography.bodySmall,
          color = tokens.subtleText,
      )
      return@ExpandableSection
    }

    entries.take(6).forEach { entry ->
      val statusLabel =
          when (entry.status) {
            SyncLogStatus.SUCCESS -> strings.settings.syncStatusSuccess
            SyncLogStatus.PARTIAL -> strings.settings.syncLogStatusPartial
            SyncLogStatus.ERROR -> strings.settings.syncStatusError
            SyncLogStatus.CANCELED -> strings.settings.syncLogStatusCanceled
          }
      Column(modifier = Modifier.padding(bottom = 10.dp)) {
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelLarge,
            color =
                when (entry.status) {
                  SyncLogStatus.SUCCESS -> tokens.accent
                  SyncLogStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                  SyncLogStatus.ERROR -> MaterialTheme.colorScheme.error
                  SyncLogStatus.CANCELED -> tokens.mutedText
                },
        )
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.subtleText,
        )
        Text(
            text = entry.startedAt.toErpDateTime(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.subtleText,
        )
        if (entry.failedSteps.isNotEmpty()) {
          Text(
              text = entry.failedSteps.joinToString(" · "),
              style = MaterialTheme.typography.labelSmall,
              color = tokens.subtleText,
          )
        }
      }
    }
  }
}

@Composable
private fun SyncTogglesColumn(
    autoSync: Boolean,
    syncOnStartup: Boolean,
    wifiOnly: Boolean,
    useTtl: Boolean,
    ttlHours: Int,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onUseTtlChanged: (Boolean) -> Unit,
    onTtlHoursClick: () -> Unit,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SettingToggle(
        label = strings.settings.autoSyncLabel,
        checked = autoSync,
        onCheckedChange = onAutoSyncChanged,
        compact = compact,
    )
    SettingToggle(
        label = strings.settings.syncOnStartupLabel,
        checked = syncOnStartup,
        onCheckedChange = onSyncOnStartupChanged,
        compact = compact,
    )
    SettingToggle(
        label = strings.settings.wifiOnlyLabel,
        checked = wifiOnly,
        onCheckedChange = onWifiOnlyChanged,
        compact = compact,
    )
    SettingToggle(
        label = strings.settings.useTtlLabel,
        checked = useTtl,
        onCheckedChange = onUseTtlChanged,
        supportingText = strings.settings.useTtlHelp,
        compact = compact,
        showDivider = true,
    )
    SettingItem(
        label = strings.settings.ttlHoursLabel,
        value = "$ttlHours h",
        onClick = onTtlHoursClick,
        enabled = useTtl,
        compact = compact,
        showDivider = false,
    )
  }
}
