@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.erpnext.pos.views.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.DenominationCount
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.components.DenominationCounter
import com.erpnext.pos.views.components.DenominationCounterLabels
import com.erpnext.pos.views.components.DenominationUi
import com.erpnext.pos.views.home.OpeningStep
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun OpeningFormSection(
    user: UserBO?,
    profiles: List<POSProfileSimpleBO>,
    selectedProfile: POSProfileSimpleBO?,
    onSelectProfile: (POSProfileSimpleBO) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    totalsByCurrency: Map<String, Double>,
    isLoading: Boolean,
    canOpen: Boolean,
    cashModesMissingCurrency: List<String>,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    showActions: Boolean = true,
) {
  SectionCard(title = "Detalles de Apertura") {
    val nowInstant by
        produceState(initialValue = Clock.System.now()) {
          while (true) {
            value = Clock.System.now()
            delay(1000)
          }
        }
    val now = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val outline = MaterialTheme.colorScheme.outlineVariant
    var notes by remember { mutableStateOf("") }
    val cashierName =
        listOfNotNull(
                user?.firstName?.takeIf { it.isNotBlank() },
                user?.lastName?.takeIf { !it.isNullOrBlank() },
            )
            .joinToString(" ")
            .ifBlank { user?.name ?: "" }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      if (isLoading) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          androidx.compose.material3.LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth(),
              color = MaterialTheme.colorScheme.primary,
          )
          Text(
              text = "Cargando información del POS...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, outline.copy(alpha = 0.6f)),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
                text = "Fecha",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text =
                    "${dayName(now.dayOfWeek)} ${now.dayOfMonth} ${monthName(now.monthNumber)} ${now.year}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Hora",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selectedProfile?.name ?: "Seleccionar POS",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            label = { Text("Perfil de POS") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
          profiles.forEach { profile ->
            androidx.compose.material3.DropdownMenuItem(
                text = {
                  Column {
                    Text(profile.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        profile.company,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                },
                onClick = {
                  onSelectProfile(profile)
                  onExpandedChange(false)
                },
            )
          }
        }
      }

      OutlinedTextField(
          value = cashierName,
          onValueChange = {},
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Cajero / Usuario") },
          leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          },
          readOnly = true,
      )

      OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Notas / Observaciones") },
          placeholder = { Text("Detalles adicionales para la apertura") },
          singleLine = false,
          maxLines = 3,
      )

      Divider(color = MaterialTheme.colorScheme.outlineVariant)

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Resumen de Apertura",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        totalsByCurrency.forEach { (cur, total) ->
          SummaryRow(
              icon = Icons.Outlined.Wallet,
              label = "Efectivo $cur",
              value = formatCurrency(cur, total),
          )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Total Apertura",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          )
          Column(horizontalAlignment = Alignment.End) {
            totalsByCurrency.forEach { (cur, total) ->
              Text(
                  text = formatCurrency(cur, total),
                  style =
                      MaterialTheme.typography.titleLarge.copy(
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary,
                      ),
              )
            }
          }
        }
      }

      if (cashModesMissingCurrency.isNotEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
        ) {
          Text(
              text =
                  "ASSUMPTION: these cash modes have no currency configured and use the base currency: ${cashModesMissingCurrency.joinToString(", ")}",
              modifier = Modifier.padding(12.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      if (showActions) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
              onClick = onCancel,
              modifier = Modifier.weight(1f),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  ),
          ) {
            Text("Cancelar")
          }
          Button(
              onClick = onOpen,
              enabled = canOpen,
              modifier = Modifier.weight(1f),
              colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          ) {
            Text("Abrir Caja", color = MaterialTheme.colorScheme.onPrimary)
          }
        }
      }

      Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
          shape = RoundedCornerShape(10.dp),
      ) {
        Text(
            text =
                "Apertura solo en efectivo. Si tienes otros métodos, regístralos luego como pagos.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

@Composable
internal fun CompactOpeningStepSelector(
    currentStep: OpeningStep,
    totalsByCurrency: Map<String, Double>,
    onStepSelected: (OpeningStep) -> Unit,
) {
  val totalsSummary =
      totalsByCurrency.entries.joinToString(" · ") { (currency, total) ->
        formatCurrency(currency, total)
      }
  SectionCard(title = "Flujo de apertura") {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = currentStep == OpeningStep.Details,
            onClick = { onStepSelected(OpeningStep.Details) },
            label = { Text("1. Detalles") },
        )
        FilterChip(
            selected = currentStep == OpeningStep.Count,
            onClick = { onStepSelected(OpeningStep.Count) },
            label = { Text("2. Conteo") },
        )
      }
      Text(
          text =
              if (totalsSummary.isBlank()) {
                "Completa los datos y luego registra el conteo de efectivo."
              } else {
                "Resumen actual: $totalsSummary"
              },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
internal fun CompactOpeningActionBar(
    modifier: Modifier = Modifier,
    step: OpeningStep,
    canOpen: Boolean,
    totalsByCurrency: Map<String, Double>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
) {
  Surface(
      modifier = modifier.fillMaxWidth().navigationBarsPadding(),
      tonalElevation = 10.dp,
      shadowElevation = 14.dp,
      color = MaterialTheme.colorScheme.surface,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      if (totalsByCurrency.isNotEmpty()) {
        Text(
            text =
                totalsByCurrency.entries.joinToString(" · ") { (currency, total) ->
                  formatCurrency(currency, total)
                },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
          Spacer(Modifier.width(6.dp))
          Text(if (step == OpeningStep.Details) "Cancelar" else "Volver")
        }
        if (step == OpeningStep.Details) {
          Button(onClick = onNext, modifier = Modifier.weight(1f)) {
            Text("Ir a conteo")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
          }
        } else {
          Button(onClick = onOpen, enabled = canOpen, modifier = Modifier.weight(1f)) {
            Text("Abrir Caja")
          }
        }
      }
    }
  }
}

@Composable
internal fun OpeningCashContent(
    countCurrencies: List<String>,
    selectedCurrency: String,
    denominations: List<DenominationUi>,
    onCurrencyChange: (String) -> Unit,
    onDenominationChange: (Double, Int) -> Unit,
    totalsByCurrency: Map<String, Double>,
) {
  SectionCard(title = "Conteo de efectivo") {
    if (countCurrencies.isEmpty()) {
      Text(
          text = "No cash payment methods are configured for this POS profile.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
      )
      return@SectionCard
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      DenominationCounter(
          denominations = denominations,
          onCountChange = onDenominationChange,
          total = totalsByCurrency[selectedCurrency] ?: 0.0,
          formatAmount = { amount -> formatCurrency(selectedCurrency, amount) },
          labels =
              DenominationCounterLabels(
                  title = "Detalle por denominación",
                  subtitle = "billetes y monedas",
                  billsLabel = "Billetes",
                  coinsLabel = "Monedas",
                  totalLabel = "Total contado",
              ),
          countCurrencies = countCurrencies,
          selectedCountCurrency = selectedCurrency,
          onCurrencyChange = onCurrencyChange,
      )

      Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          border =
              androidx.compose.foundation.BorderStroke(
                  1.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
              ),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Text(
              text = "Total efectivo por moneda",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(Modifier.height(6.dp))

          totalsByCurrency.forEach { (cur, total) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                  text = cur,
                  style =
                      MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                  text = formatCurrency(cur, total),
                  style =
                      MaterialTheme.typography.titleLarge.copy(
                          fontWeight = FontWeight.ExtraBold,
                          color = MaterialTheme.colorScheme.primary,
                      ),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
  OutlinedCard(
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      shape = RoundedCornerShape(12.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                  .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
      }
      Column(modifier = Modifier.padding(16.dp)) { content() }
    }
  }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
          text = label,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
    )
  }
}

private fun monthName(month: Int): String {
  return when (month) {
    1 -> "enero"
    2 -> "febrero"
    3 -> "marzo"
    4 -> "abril"
    5 -> "mayo"
    6 -> "junio"
    7 -> "julio"
    8 -> "agosto"
    9 -> "septiembre"
    10 -> "octubre"
    11 -> "noviembre"
    else -> "diciembre"
  }
}

private fun dayName(dayOfWeek: DayOfWeek): String {
  return when (dayOfWeek) {
    DayOfWeek.MONDAY -> "lunes"
    DayOfWeek.TUESDAY -> "martes"
    DayOfWeek.WEDNESDAY -> "miércoles"
    DayOfWeek.THURSDAY -> "jueves"
    DayOfWeek.FRIDAY -> "viernes"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
  }
}

internal fun applyDraftCounts(
    denominations: List<DenominationUi>,
    counts: List<DenominationCount>,
): List<DenominationUi> {
  if (counts.isEmpty()) return denominations
  val countMap = counts.associate { it.value to it.count }
  return denominations.map { denom -> denom.copy(count = countMap[denom.value] ?: 0) }
}
