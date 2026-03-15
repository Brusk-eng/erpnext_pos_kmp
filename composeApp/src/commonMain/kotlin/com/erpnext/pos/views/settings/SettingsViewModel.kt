@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.localSource.preferences.*
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class SettingsViewModel(
    private val cashBoxManager: CashBoxManager,
    private val syncPreferences: SyncPreferences,
    private val syncLogPreferences: SyncLogPreferences,
    private val syncManager: SyncManager,
    private val generalPreferences: GeneralPreferences,
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences,
    private val returnPolicyPreferences: ReturnPolicyPreferences,
) : BaseViewModel() {

  private val _uiState: MutableStateFlow<POSSettingState> =
      MutableStateFlow(POSSettingState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
              cashBoxManager.contextFlow,
              cashBoxManager.activeOpeningEntryId(),
              syncPreferences.settings,
              syncManager.state,
              syncLogPreferences.log,
              languagePreferences.language,
              themePreferences.theme,
              themePreferences.themeMode,
              generalPreferences.taxesIncluded,
              generalPreferences.offlineMode,
              generalPreferences.printerEnabled,
              generalPreferences.cashDrawerEnabled,
              generalPreferences.allowNegativeStock,
              returnPolicyPreferences.settings,
          ) { args: Array<Any?> ->
            val ctx = args[0] as POSContext?
            val openingEntryId = args[1] as String?
            val syncSettings = args[2] as SyncSettings
            val syncState = args[3] as SyncState
            val syncLog = args[4] as List<*>
            val language = args[5] as AppLanguage
            val theme = args[6] as AppColorTheme
            val themeMode = args[7] as AppThemeMode
            val taxes = args[8] as Boolean
            val offline = args[9] as Boolean
            val printer = args[10] as Boolean
            val drawer = args[11] as Boolean
            val allowNegativeStock = args[12] as Boolean
            val returnPolicy = args[13] as com.erpnext.pos.domain.models.ReturnPolicySettings

            POSSettingState.Success(
                settings =
                    POSSettingBO(
                        company = ctx?.company ?: "-",
                        posProfile = ctx?.profileName ?: "-",
                        openingEntryId = openingEntryId ?: "-",
                        warehouse = ctx?.warehouse ?: "-",
                        priceList = ctx?.priceList ?: ctx?.currency ?: "-",
                        taxesIncluded = taxes,
                        offlineMode = offline,
                        printerEnabled = printer,
                        cashDrawerEnabled = drawer,
                        allowNegativeStock = allowNegativeStock,
                    ),
                hasContext = ctx != null,
                syncSettings = syncSettings,
                syncState = syncState,
                language = language,
                theme = theme,
                themeMode = themeMode,
                returnPolicy = returnPolicy,
                syncLog = syncLog.filterIsInstance<com.erpnext.pos.domain.models.SyncLogEntry>(),
            )
          }
          .collect { state -> _uiState.value = state }
    }
  }

  fun onSyncNow() {
    viewModelScope.launch { syncManager.fullSync(force = true) }
  }

  fun onCancelSync() {
    syncManager.cancelSync()
  }

  fun setAutoSync(enabled: Boolean) {
    viewModelScope.launch { syncPreferences.setAutoSync(enabled) }
  }

  fun setSyncOnStartup(enabled: Boolean) {
    viewModelScope.launch { syncPreferences.setSyncOnStartup(enabled) }
  }

  fun setWifiOnly(enabled: Boolean) {
    viewModelScope.launch { syncPreferences.setWifiOnly(enabled) }
  }

  fun setUseTtl(enabled: Boolean) {
    viewModelScope.launch { syncPreferences.setUseTtl(enabled) }
  }

  fun setTtlHours(hours: Int) {
    viewModelScope.launch { syncPreferences.setTtlHours(hours) }
  }

  fun setTaxesIncluded(enabled: Boolean) {
    viewModelScope.launch { generalPreferences.setTaxesIncluded(enabled) }
  }

  fun setOfflineMode(enabled: Boolean) {
    viewModelScope.launch { generalPreferences.setOfflineMode(enabled) }
  }

  fun setPrinterEnabled(enabled: Boolean) {
    viewModelScope.launch { generalPreferences.setPrinterEnabled(enabled) }
  }

  fun setCashDrawerEnabled(enabled: Boolean) {
    viewModelScope.launch { generalPreferences.setCashDrawerEnabled(enabled) }
  }

  fun setReturnPolicy(settings: com.erpnext.pos.domain.models.ReturnPolicySettings) {
    viewModelScope.launch { returnPolicyPreferences.save(settings) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { languagePreferences.setLanguage(language) }
  }

  fun setTheme(theme: AppColorTheme) {
    viewModelScope.launch { themePreferences.setTheme(theme) }
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { themePreferences.setThemeMode(mode) }
  }

}
 // 270