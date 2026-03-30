package com.erpnext.pos.views.settings.components

import AppColorTheme
import AppThemeMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.settings.ExpandableSection
import com.erpnext.pos.views.settings.POSSettingAction
import com.erpnext.pos.views.settings.POSSettingBO
import com.erpnext.pos.views.settings.SettingItem
import com.erpnext.pos.views.settings.SettingSection
import com.erpnext.pos.views.settings.SettingToggle

@Composable
internal fun MissingContextBanner() {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer
          ),
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
  ) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "Configuración limitada",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
            text =
                "No se encontró un perfil POS activo. Abre una caja o selecciona un perfil para ver datos completos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }
  }
}

@Composable
internal fun OperationSection(
    settings: POSSettingBO,
    action: POSSettingAction,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  SettingSection(
      title = strings.settings.operationTitle,
      icon = Icons.Outlined.Tune,
      compact = compact,
  ) {
    SettingToggle(
        label = strings.settings.taxesIncludedLabel,
        checked = settings.taxesIncluded,
        onCheckedChange = action.onTaxesIncludedChanged,
        compact = compact,
    )
    SettingToggle(
        label = strings.settings.offlineModeLabel,
        checked = settings.offlineMode,
        onCheckedChange = action.onOfflineModeChanged,
        supportingText = strings.settings.offlineModeHelp,
        compact = compact,
    )
    SettingToggle(
        label = "Permitir venta con stock negativo",
        checked = settings.allowNegativeStock,
        onCheckedChange = {},
        enabled = false,
        supportingText = "Controlado por ERPNext (Desk).",
        compact = compact,
        showDivider = false,
    )
  }
}

@Composable
internal fun ReturnPolicySection(
    returnPolicy: ReturnPolicySettings,
    compact: Boolean,
    onOpenDestination: () -> Unit,
    onOpenMaxDays: () -> Unit,
    onReturnPolicyChanged: (ReturnPolicySettings) -> Unit,
) {
  ExpandableSection(
      title = "Política de devoluciones",
      description = "Se aplica localmente y alinea los retornos con ERPNext v16.",
      icon = Icons.AutoMirrored.Outlined.Undo,
      compact = compact,
      initiallyExpanded = false,
  ) {
    SettingItem(
        label = "Destino por defecto",
        value =
            if (returnPolicy.defaultDestination == ReturnDestinationPolicy.REFUND) {
              "Reembolso"
            } else {
              "Crédito a favor"
            },
        onClick = onOpenDestination,
        compact = compact,
    )
    SettingItem(
        label = "Límite de días para retornar",
        value =
            if (returnPolicy.maxDaysAfterInvoice <= 0) {
              "Sin límite"
            } else {
              "${returnPolicy.maxDaysAfterInvoice} días"
            },
        onClick = onOpenMaxDays,
        compact = compact,
    )
    SettingToggle(
        label = "Permitir reembolsos",
        checked = returnPolicy.allowRefunds,
        onCheckedChange = { enabled ->
          val destination =
              if (enabled) {
                returnPolicy.defaultDestination
              } else {
                ReturnDestinationPolicy.CREDIT
              }
          onReturnPolicyChanged(
              returnPolicy.copy(
                  allowRefunds = enabled,
                  defaultDestination = destination,
              )
          )
        },
        compact = compact,
    )
    SettingToggle(
        label = "Reembolso solo con factura pagada",
        checked = returnPolicy.requirePaidInvoiceForRefund,
        onCheckedChange = { enabled ->
          onReturnPolicyChanged(returnPolicy.copy(requirePaidInvoiceForRefund = enabled))
        },
        enabled = returnPolicy.allowRefunds,
        compact = compact,
    )
    SettingToggle(
        label = "Permitir retornos parciales",
        checked = returnPolicy.allowPartialReturns,
        onCheckedChange = { enabled ->
          onReturnPolicyChanged(returnPolicy.copy(allowPartialReturns = enabled))
        },
        compact = compact,
    )
    SettingToggle(
        label = "Permitir retornos totales",
        checked = returnPolicy.allowFullReturns,
        onCheckedChange = { enabled ->
          onReturnPolicyChanged(returnPolicy.copy(allowFullReturns = enabled))
        },
        compact = compact,
    )
    SettingToggle(
        label = "Requerir motivo",
        checked = returnPolicy.requireReason,
        onCheckedChange = { enabled ->
          onReturnPolicyChanged(returnPolicy.copy(requireReason = enabled))
        },
        compact = compact,
        showDivider = false,
    )
  }
}

@Composable
internal fun HardwareSection(
    settings: POSSettingBO,
    onPrinterEnabledChanged: (Boolean) -> Unit,
    onCashDrawerEnabledChanged: (Boolean) -> Unit,
    onOpenPrinters: () -> Unit,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  SettingSection(
      title = strings.settings.hardwareTitle,
      icon = Icons.Outlined.Print,
      compact = compact,
  ) {
    SettingToggle(
        label = strings.settings.printerEnabledLabel,
        checked = settings.printerEnabled,
        onCheckedChange = onPrinterEnabledChanged,
        compact = compact,
    )
    SettingToggle(
        label = strings.settings.cashDrawerEnabledLabel,
        checked = settings.cashDrawerEnabled,
        onCheckedChange = onCashDrawerEnabledChanged,
        compact = compact,
        showDivider = true,
    )
    Button(onClick = onOpenPrinters) { Text("Manage printers") }
  }
}

@Composable
internal fun AppearanceSection(
    language: AppLanguage,
    theme: AppColorTheme,
    themeMode: AppThemeMode,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppColorTheme) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  SettingSection(
      title = strings.settings.languageTitle,
      description = strings.settings.languageInstantHint,
      icon = Icons.Outlined.Language,
      compact = compact,
  ) {
    LanguageSelector(
        currentLanguage = language,
        onLanguageSelected = onLanguageSelected,
        compact = compact,
    )
    ThemeChipSelector(currentTheme = theme, onThemeSelected = onThemeSelected)
    ThemeModeChipSelector(currentMode = themeMode, onModeSelected = onThemeModeSelected)
  }
}
