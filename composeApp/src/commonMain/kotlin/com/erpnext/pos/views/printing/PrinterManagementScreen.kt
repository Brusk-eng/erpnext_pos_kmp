@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.printing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.localization.AppLanguage

@Composable
fun PrinterManagementScreen(
    state: PrinterManagementUiState,
    language: AppLanguage,
    onCreateNew: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onUpdateForm: ((PrinterProfileFormState) -> PrinterProfileFormState) -> Unit,
    onUseDiscoveredDevice: (DiscoveredPrinterDevice) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onCheckConnection: () -> Unit,
    onPrintTest: () -> Unit,
    onClearMessage: () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val wideLayout = maxWidth >= 980.dp

    if (wideLayout) {
      Row(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(
            modifier = Modifier.weight(0.95f).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          DiscoveredPrintersPanel(state, language, onRefreshDiscovery, onUseDiscoveredDevice)
          SavedPrintersPanel(state, language, onCreateNew, onSelectProfile)
        }
        EditorPanel(
            modifier = Modifier.weight(1.2f).fillMaxSize(),
            state = state,
            language = language,
            scrollable = true,
            onUpdateForm = onUpdateForm,
            onSave = onSave,
            onDelete = onDelete,
            onSetDefault = onSetDefault,
            onCheckConnection = onCheckConnection,
            onPrintTest = onPrintTest,
            onClearMessage = onClearMessage,
        )
      }
    } else {
      LazyColumn(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item { DiscoveredPrintersPanel(state, language, onRefreshDiscovery, onUseDiscoveredDevice) }
        item { SavedPrintersPanel(state, language, onCreateNew, onSelectProfile) }
        item {
          EditorPanel(
              modifier = Modifier.fillMaxWidth(),
              state = state,
              language = language,
              scrollable = false,
              onUpdateForm = onUpdateForm,
              onSave = onSave,
              onDelete = onDelete,
              onSetDefault = onSetDefault,
              onCheckConnection = onCheckConnection,
              onPrintTest = onPrintTest,
              onClearMessage = onClearMessage,
          )
        }
      }
    }
  }
}
