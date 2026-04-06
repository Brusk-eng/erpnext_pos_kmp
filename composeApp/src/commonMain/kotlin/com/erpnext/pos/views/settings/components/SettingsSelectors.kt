package com.erpnext.pos.views.settings.components

import AppColorTheme
import AppThemeMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    compact: Boolean,
) {
  val strings = LocalAppStrings.current
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
    OutlinedTextField(
        value =
            when (currentLanguage) {
              AppLanguage.Spanish -> strings.settings.languageSpanish
              AppLanguage.English -> strings.settings.languageEnglish
            },
        onValueChange = {},
        modifier =
            Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        readOnly = true,
        label = { Text(strings.settings.languageLabel) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        textStyle =
            if (compact) {
              MaterialTheme.typography.bodyMedium
            } else {
              MaterialTheme.typography.bodyLarge
            },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      DropdownMenuItem(
          text = { Text(strings.settings.languageSpanish) },
          onClick = {
            onLanguageSelected(AppLanguage.Spanish)
            expanded = false
          },
      )
      DropdownMenuItem(
          text = { Text(strings.settings.languageEnglish) },
          onClick = {
            onLanguageSelected(AppLanguage.English)
            expanded = false
          },
      )
    }
  }
}

@Composable
internal fun ThemeChipSelector(
    currentTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit,
) {
  val tokens = settingsTokens()
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AppColorTheme.entries.forEach { theme ->
      FilterChip(
          selected = theme == currentTheme,
          onClick = { onThemeSelected(theme) },
          label = { Text(theme.label) },
          colors =
              FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  containerColor = tokens.cardContainer,
                  labelColor = tokens.titleColor,
              ),
      )
    }
  }
}

@Composable
internal fun ThemeModeChipSelector(
    currentMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit,
) {
  val tokens = settingsTokens()
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AppThemeMode.entries.forEach { mode ->
      FilterChip(
          selected = mode == currentMode,
          onClick = { onModeSelected(mode) },
          label = { Text(mode.label) },
          colors =
              FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                  containerColor = tokens.cardContainer,
                  labelColor = tokens.titleColor,
              ),
          elevation = FilterChipDefaults.elevatedFilterChipElevation(),
      )
    }
  }
}
