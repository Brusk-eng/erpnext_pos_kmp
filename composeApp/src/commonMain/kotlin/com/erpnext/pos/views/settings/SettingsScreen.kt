@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.settings.components.AppearanceSection
import com.erpnext.pos.views.settings.components.HardwareSection
import com.erpnext.pos.views.settings.components.LanguageSelector
import com.erpnext.pos.views.settings.components.MissingContextBanner
import com.erpnext.pos.views.settings.components.OperationSection
import com.erpnext.pos.views.settings.components.ReturnPolicyDaysDialog
import com.erpnext.pos.views.settings.components.ReturnPolicyDestinationDialog
import com.erpnext.pos.views.settings.components.ReturnPolicySection
import com.erpnext.pos.views.settings.components.SettingsOverviewRow
import com.erpnext.pos.views.settings.components.SyncLogSection
import com.erpnext.pos.views.settings.components.SyncSection
import com.erpnext.pos.views.settings.components.ThemeChipSelector
import com.erpnext.pos.views.settings.components.ThemeModeChipSelector
import com.erpnext.pos.views.settings.components.TtlHoursDialog
import com.erpnext.pos.views.settings.components.settingsTokens
import org.koin.compose.koinInject

@Composable
fun PosSettingsScreen(state: POSSettingState, action: POSSettingAction) {
  val snackbar = koinInject<SnackbarController>()
  val networkMonitor = koinInject<NetworkMonitor>()
  val strings = LocalAppStrings.current
  val scrollState = rememberScrollState()
  val isOnline by networkMonitor.isConnected.collectAsState(false)

  BoxWithConstraints(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val isCompact = maxWidth < 640.dp
    val contentPadding = if (isCompact) 12.dp else 16.dp
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(contentPadding)) {
      when (state) {
        is POSSettingState.Success -> {
          var showReturnDaysDialog by remember { mutableStateOf(false) }
          var showReturnDestinationDialog by remember { mutableStateOf(false) }
          var showTtlHoursDialog by remember { mutableStateOf(false) }

          if (showReturnDaysDialog) {
            ReturnPolicyDaysDialog(
                initialValue = state.returnPolicy.maxDaysAfterInvoice,
                onDismiss = { showReturnDaysDialog = false },
                onConfirm = { value ->
                  showReturnDaysDialog = false
                  action.onReturnPolicyChanged(state.returnPolicy.copy(maxDaysAfterInvoice = value))
                },
            )
          }
          if (showReturnDestinationDialog) {
            ReturnPolicyDestinationDialog(
                current = state.returnPolicy.defaultDestination,
                allowRefunds = state.returnPolicy.allowRefunds,
                onDismiss = { showReturnDestinationDialog = false },
                onConfirm = { value ->
                  showReturnDestinationDialog = false
                  action.onReturnPolicyChanged(state.returnPolicy.copy(defaultDestination = value))
                },
            )
          }
          if (showTtlHoursDialog) {
            TtlHoursDialog(
                initialValue = state.syncSettings.ttlHours,
                onDismiss = { showTtlHoursDialog = false },
                onConfirm = { value ->
                  showTtlHoursDialog = false
                  action.onTtlHoursChanged(value)
                },
            )
          }

          SettingsOverviewRow(
              settings = state.settings,
              syncSettings = state.syncSettings,
              syncState = state.syncState,
              onSyncNow = action.onSyncNow,
              onCancelSync = action.onCancelSync,
              compact = isCompact,
              isOnline = isOnline,
          )
          Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 18.dp))

          if (!state.hasContext) {
            MissingContextBanner()
          }

          SyncSection(
              syncSettings = state.syncSettings,
              onAutoSyncChanged = action.onAutoSyncChanged,
              onSyncOnStartupChanged = action.onSyncOnStartupChanged,
              onWifiOnlyChanged = action.onWifiOnlyChanged,
              onUseTtlChanged = action.onUseTtlChanged,
              onTtlHoursClick = { showTtlHoursDialog = true },
              compact = isCompact,
          )

          OperationSection(settings = state.settings, action = action, compact = isCompact)

          ReturnPolicySection(
              returnPolicy = state.returnPolicy,
              compact = isCompact,
              onOpenDestination = { showReturnDestinationDialog = true },
              onOpenMaxDays = { showReturnDaysDialog = true },
              onReturnPolicyChanged = action.onReturnPolicyChanged,
          )

          HardwareSection(
              settings = state.settings,
              onPrinterEnabledChanged = action.onPrinterEnabledChanged,
              onCashDrawerEnabledChanged = action.onCashDrawerEnabledChanged,
              onOpenPrinters = action.onOpenPrinters,
              compact = isCompact,
          )

          AppearanceSection(
              language = state.language,
              theme = state.theme,
              themeMode = state.themeMode,
              onLanguageSelected = action.onLanguageSelected,
              onThemeSelected = action.onThemeSelected,
              onThemeModeSelected = action.onThemeModeSelected,
              compact = isCompact,
          )

          SyncLogSection(entries = state.syncLog, compact = isCompact)
        }

        is POSSettingState.Loading -> {
          Spacer(modifier = Modifier.height(120.dp))
          CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        is POSSettingState.Error -> {
          snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
        }
      }
    }
  }
}

@Composable
internal fun SettingSection(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
  val tokens = settingsTokens()
  ElevatedCard(
      modifier = modifier.fillMaxWidth().padding(bottom = 14.dp),
      shape = RoundedCornerShape(22.dp),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = tokens.cardContainer),
  ) {
    Column(modifier = Modifier.padding(if (compact) 14.dp else 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
          Icon(imageVector = icon, contentDescription = null, tint = tokens.iconTint)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.titleColor,
        )
      }
      if (description != null) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.subtleText,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
      } else {
        Spacer(modifier = Modifier.height(10.dp))
      }
      content()
    }
  }
}

@Composable
internal fun ExpandableSection(
    title: String,
    description: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    compact: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
  val tokens = settingsTokens()
  var expanded by remember { mutableStateOf(initiallyExpanded) }
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).animateContentSize(),
      shape = RoundedCornerShape(22.dp),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = tokens.cardContainer),
  ) {
    Column(modifier = Modifier.padding(if (compact) 14.dp else 16.dp)) {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
      ) {
        if (icon != null) {
          Icon(imageVector = icon, contentDescription = null, tint = tokens.iconTint)
          Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = tokens.titleColor,
          )
          if (description != null && expanded) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.subtleText,
                modifier = Modifier.padding(top = 4.dp),
            )
          }
        }
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = tokens.iconTint,
        )
      }
      AnimatedVisibility(
          visible = expanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut(),
      ) {
        Column(modifier = Modifier.padding(top = if (compact) 10.dp else 12.dp)) { content() }
      }
    }
  }
}

@Composable
internal fun SettingItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    compact: Boolean = false,
    showDivider: Boolean = true,
) {
  val tokens = settingsTokens()
  Column {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = if (compact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            color = tokens.titleColor,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color =
                if (enabled) {
                  tokens.valueColor
                } else {
                  tokens.mutedText
                },
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint =
              if (enabled) {
                tokens.accent
              } else {
                tokens.mutedText
              },
      )
    }
    if (showDivider) {
      HorizontalDivider(color = tokens.divider)
    }
  }
}

@Composable
internal fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
    compact: Boolean = false,
    showDivider: Boolean = true,
) {
  val tokens = settingsTokens()
  Column {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(vertical = if (compact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            color = tokens.titleColor,
        )
        if (supportingText != null) {
          Text(
              text = supportingText,
              style = MaterialTheme.typography.bodySmall,
              color = tokens.subtleText,
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Switch(
          checked = checked,
          onCheckedChange = onCheckedChange,
          enabled = enabled,
          colors =
              SwitchDefaults.colors(
                  checkedThumbColor = tokens.accent,
                  uncheckedThumbColor = tokens.titleColor,
              ),
      )
    }
    if (showDivider) {
      HorizontalDivider(color = tokens.divider)
    }
  }
}

// 1384
