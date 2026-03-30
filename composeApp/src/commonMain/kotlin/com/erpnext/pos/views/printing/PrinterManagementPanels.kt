@file:OptIn(ExperimentalLayoutApi::class)

package com.erpnext.pos.views.printing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterLanguage
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.localization.AppLanguage

@Composable
internal fun DiscoveredPrintersPanel(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onRefreshDiscovery: () -> Unit,
    onUseDiscoveredDevice: (DiscoveredPrinterDevice) -> Unit,
) {
  SectionCard(
      title = tr(language, "Dispositivos Bluetooth detectados", "Detected Bluetooth devices"),
      subtitle =
          tr(
              language,
              "Lista de Android vinculada. Nombre Bluetooth y MAC son reales; marca, modelo, papel, chars y code page son sugeridos.",
              "This is the paired Android list. Bluetooth name and MAC are real. Brand, model, paper, chars, and code page are suggested for faster setup.",
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          tr(language, "Dispositivos vinculados", "Paired devices"),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
      AssistChip(
          onClick = onRefreshDiscovery,
          label = { Text(tr(language, "Actualizar", "Refresh")) },
          leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Refresh, contentDescription = null) },
      )
    }
    Spacer(Modifier.height(12.dp))
    if (state.discoveredDevices.isEmpty()) {
      EmptyState(
          icon = Icons.Outlined.Bluetooth,
          title =
              tr(
                  language,
                  "No se encontraron impresoras Bluetooth vinculadas",
                  "No paired Bluetooth printers found",
              ),
          body =
              tr(
                  language,
                  "Vincula la impresora desde ajustes de Android, mantén Bluetooth encendido y toca Actualizar.",
                  "Pair the printer from Android settings, keep Bluetooth on, and tap Refresh.",
              ),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.discoveredDevices.forEach { device ->
          DiscoveredPrinterCard(
              device = device,
              language = language,
              onUseDiscoveredDevice = { onUseDiscoveredDevice(device) },
          )
        }
      }
    }
  }
}

@Composable
internal fun SavedPrintersPanel(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onCreateNew: () -> Unit,
    onSelectProfile: (String) -> Unit,
) {
  SectionCard(
      title = tr(language, "Perfiles de impresora guardados", "Saved printer profiles"),
      subtitle =
          tr(
              language,
              "Estos perfiles ya están almacenados localmente en el POS.",
              "These are the profiles already stored locally in the POS.",
          ),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          if (language == AppLanguage.Spanish) {
            "${state.profiles.size} perfil${if (state.profiles.size == 1) "" else "es"} guardado${if (state.profiles.size == 1) "" else "s"}"
          } else {
            "${state.profiles.size} saved profile${if (state.profiles.size == 1) "" else "s"}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      AssistChip(
          onClick = onCreateNew,
          label = { Text(tr(language, "Nuevo", "New")) },
          leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Add, contentDescription = null) },
      )
    }
    Spacer(Modifier.height(12.dp))
    if (state.profiles.isEmpty()) {
      EmptyState(
          icon = Icons.Outlined.Print,
          title = tr(language, "Aún no hay perfiles guardados", "No saved profiles yet"),
          body =
              tr(
                  language,
                  "Toca un dispositivo Bluetooth arriba o crea un perfil de red manual.",
                  "Tap a Bluetooth device above or create a manual network profile.",
              ),
      )
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        state.profiles.forEach { profile ->
          PrinterProfileCard(
              profile = profile,
              language = language,
              selected = profile.id == state.selectedProfileId,
              onClick = { onSelectProfile(profile.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun DiscoveredPrinterCard(
    device: DiscoveredPrinterDevice,
    language: AppLanguage,
    onUseDiscoveredDevice: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
              "MAC ${device.address}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onUseDiscoveredDevice) { Text(tr(language, "Usar este dispositivo", "Use this device")) }
      }

      Text(
          tr(language, "Categorías detectadas", "Detected categories"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(
            tr(language, "Conexión", "Connection"),
            humanTransport(device.transportType, language),
            MaterialTheme.colorScheme.primary,
        )
        device.familyHint?.let {
          CategoryBadge(tr(language, "Tipo", "Type"), humanFamily(it, language), MaterialTheme.colorScheme.secondary)
        }
        device.languageHint?.let {
          CategoryBadge(
              tr(language, "Lenguaje", "Language"),
              humanLanguage(it),
              MaterialTheme.colorScheme.tertiary,
          )
        }
      }

      Text(
          tr(language, "Capacidades sugeridas", "Suggested capabilities"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        device.brandHint?.let { CategoryBadge(tr(language, "Marca", "Brand"), it, MaterialTheme.colorScheme.primary) }
        device.modelHint?.let { CategoryBadge(tr(language, "Modelo", "Model"), it, MaterialTheme.colorScheme.secondary) }
        device.paperWidthMmHint?.let {
          CategoryBadge(
              tr(language, "Papel", "Paper"),
              "${it} mm",
              MaterialTheme.colorScheme.tertiary,
          )
        }
        device.charactersPerLineHint?.let {
          CategoryBadge(
              tr(language, "Chars", "Chars"),
              "${it}/${tr(language, "línea", "line")}",
              MaterialTheme.colorScheme.primary,
          )
        }
        device.codePageHint?.let { CategoryBadge(tr(language, "Code page", "Code page"), it, MaterialTheme.colorScheme.secondary) }
      }

      val metadataLine = remember(device) { listOfNotNull(device.confidenceLabel).joinToString(" • ") }
      if (metadataLine.isNotBlank()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          androidx.compose.material3.Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(metadataLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun PrinterProfileCard(
    profile: PrinterProfile,
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
      shape = RoundedCornerShape(22.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor =
                  if (selected) MaterialTheme.colorScheme.surfaceContainerHighest
                  else MaterialTheme.colorScheme.surfaceContainerLow
          ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Text(
              listOfNotNull(profile.brandHint, profile.modelHint)
                  .joinToString(" ")
                  .ifBlank { tr(language, "Perfil genérico", "Generic profile") },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (profile.isDefault) {
          CategoryBadge(
              tr(language, "Predeterminada", "Default"),
              tr(language, "Sí", "Yes"),
              MaterialTheme.colorScheme.primary,
          )
        }
      }

      Text(
          tr(language, "Categorías de impresora", "Printer categories"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(tr(language, "Tipo", "Type"), humanFamily(profile.family, language), MaterialTheme.colorScheme.secondary)
        CategoryBadge(
            tr(language, "Lenguaje", "Language"),
            humanLanguage(profile.language),
            MaterialTheme.colorScheme.tertiary,
        )
        profile.supportedTransports.forEach { transport ->
          CategoryBadge(tr(language, "Ruta", "Route"), humanTransport(transport, language), MaterialTheme.colorScheme.primary)
        }
      }

      Text(
          tr(language, "Capacidades", "Capabilities"),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryBadge(tr(language, "Papel", "Paper"), "${profile.paperWidthMm} mm", MaterialTheme.colorScheme.secondary)
        CategoryBadge(
            tr(language, "Chars", "Chars"),
            "${profile.charactersPerLine}/${tr(language, "línea", "line")}",
            MaterialTheme.colorScheme.tertiary,
        )
        if (!profile.isEnabled) {
          CategoryBadge(
              tr(language, "Estado", "Status"),
              tr(language, "Deshabilitada", "Disabled"),
              MaterialTheme.colorScheme.error,
          )
        }
      }

      val routeSummary =
          buildString {
            if (!profile.bluetoothMacAddress.isNullOrBlank()) {
              append("Bluetooth ${profile.bluetoothName ?: profile.bluetoothMacAddress}")
            }
            if (!profile.host.isNullOrBlank()) {
              if (isNotBlank()) append(" • ")
              append("${tr(language, "Red", "Network")} ${profile.host}:${profile.port ?: 9100}")
            }
          }
      if (routeSummary.isNotBlank()) {
        Text(routeSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
internal fun SectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
  ElevatedCard(
      modifier = modifier,
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      content()
    }
  }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), CircleShape)
                .padding(12.dp)
    ) {
      androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
    Text(title, fontWeight = FontWeight.SemiBold)
    Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
internal fun HumanBadge(text: String, accent: Color) {
  Box(
      modifier =
          Modifier.background(accent.copy(alpha = 0.12f), CircleShape)
              .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = accent)
  }
}

@Composable
internal fun CategoryBadge(label: String, value: String, accent: Color) {
  HumanBadge(text = "$label: $value", accent = accent)
}

internal fun humanTransport(type: TransportType, language: AppLanguage): String =
    when (type) {
      TransportType.TCP_RAW -> tr(language, "Red (TCP)", "Network (TCP)")
      TransportType.BT_SPP -> tr(language, "Bluetooth Android", "Bluetooth Android")
      TransportType.BT_DESKTOP -> tr(language, "Bluetooth Desktop", "Bluetooth Desktop")
    }

internal fun humanFamily(family: PrinterFamily, language: AppLanguage): String =
    when (family) {
      PrinterFamily.RECEIPT -> tr(language, "Ticket", "Receipt")
      PrinterFamily.LABEL -> tr(language, "Etiqueta", "Label")
    }

internal fun humanLanguage(printerLanguage: PrinterLanguage): String =
    when (printerLanguage) {
      PrinterLanguage.ESC_POS -> "ESC/POS"
      PrinterLanguage.ZPL -> "ZPL"
    }

internal fun tr(language: AppLanguage, spanish: String, english: String): String =
    if (language == AppLanguage.Spanish) {
      spanish
    } else {
      english
    }
