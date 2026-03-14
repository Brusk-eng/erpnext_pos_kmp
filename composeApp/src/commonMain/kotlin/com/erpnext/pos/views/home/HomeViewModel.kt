package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.usecases.*
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.sync.*
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.notifications.notifySystem
import com.erpnext.pos.utils.notifications.scheduleDailyInventoryReminder
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.reconciliation.ReconciliationMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val fetchUserInfoUseCase: FetchUserInfoUseCase,
    private val fetchPosProfileUseCase: FetchPosProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fetchPosProfileInfoLocalUseCase: FetchPosProfileInfoLocalUseCase,
    private val contextManager: CashBoxManager,
    private val posProfileDao: POSProfileDao,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences,
    private val navManager: NavigationManager,
    private val loadHomeMetricsUseCase: LoadHomeMetricsUseCase,
    private val loadInventoryAlertsUseCase: LoadInventoryAlertsUseCase,
    private val observeHomeLiveShiftMetricsUseCase: ObserveHomeLiveShiftMetricsUseCase,
    private val posProfileGate: PosProfileGate,
    private val openingGate: OpeningGate,
    private val homeRefreshController: HomeRefreshController,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val generalPreferences: GeneralPreferences
) : BaseViewModel() {
  private val _stateFlow: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)
  val stateFlow = _stateFlow.asStateFlow()

  val syncState: StateFlow<SyncState> = syncManager.state
  private val _syncSettings =
      MutableStateFlow(
          SyncSettings(
              autoSync = true,
              syncOnStartup = true,
              wifiOnly = false,
              lastSyncAt = null,
              useTtl = false,
              ttlHours = 6,
          )
      )
  val syncSettings: StateFlow<SyncSettings> = _syncSettings.asStateFlow()
  private val _homeMetrics = MutableStateFlow(HomeMetrics())
  val homeMetrics: StateFlow<HomeMetrics> = _homeMetrics.asStateFlow()

  private val _inventoryAlertMessage = MutableStateFlow<String?>(null)
  val inventoryAlertMessage: StateFlow<String?> = _inventoryAlertMessage.asStateFlow()

  private val _openingState = MutableStateFlow(CashboxOpeningProfileState())
  val openingState: StateFlow<CashboxOpeningProfileState> = _openingState.asStateFlow()
  val openingEntryId =
      contextManager
          .activeOpeningEntryId()
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  private var userInfo: UserBO = UserBO()
  private var posProfiles: List<POSProfileSimpleBO> = emptyList()
  private var lastInventoryProfile: String? = null
  private var lastMetricsOpeningEntryId: String? = null
  private var isLoadingInitial: Boolean = false

  init {
    viewModelScope.launch { contextManager.initializeContext() }
    viewModelScope.launch {
      syncPreferences.settings.collectLatest { settings -> _syncSettings.value = settings }
    }
    viewModelScope.launch {
      isCashboxOpen().collectLatest {
        if (it && _syncSettings.value.syncOnStartup) {
          startInitialSync()
        }
        if (it) {
          val profile = contextManager.getContext()?.profileName
          if (!profile.isNullOrBlank() && profile != lastInventoryProfile) {
            lastInventoryProfile = profile
            syncManager.syncInventory(force = true)
          }
        }
      }
    }
    viewModelScope.launch {
      openingEntryId.collectLatest { openingId ->
        val normalized = openingId?.trim()?.takeIf { it.isNotBlank() }
        if (normalized == lastMetricsOpeningEntryId) return@collectLatest
        lastMetricsOpeningEntryId = normalized
        refreshMetrics()
      }
    }
    viewModelScope.launch {
      observeLiveShiftMetrics().collectLatest { live ->
        _homeMetrics.update { current -> mergeLiveShiftMetrics(current, live) }
      }
    }
    viewModelScope.launch {
      syncState.collectLatest { state ->
        if (state is SyncState.SUCCESS) {
          refreshMetrics()
        }
      }
    }
    viewModelScope.launch {
      homeRefreshController.events.collectLatest {
        if (_stateFlow.value is HomeState.POSProfiles) {
          refreshMetrics()
        } else {
          loadInitialData()
        }
      }
    }
    viewModelScope.launch {
      while (true) {
        val hour = generalPreferences.getInventoryAlertHour()
        val minute = generalPreferences.getInventoryAlertMinute()
        val delayMs = millisUntilNextAlert(hour, minute)
        delay(delayMs)
        refreshInventoryAlerts()
      }
    }
    loadInitialData()
  }

  fun startInitialSync() {
    if (!_syncSettings.value.autoSync) return
    executeUseCase(
        action = {
          if (sessionRefresher.ensureValidSession()) {
            syncManager.fullSync(ttlHours = _syncSettings.value.ttlHours, force = false)
          }
        },
        exceptionHandler = { it.printStackTrace() },
        loadingMessage = "Preparando sincronización...",
    )
  }

  fun syncNow() {
    executeUseCase(
        action = {
          if (sessionRefresher.ensureValidSession()) {
            syncManager.fullSync(force = true)
          }
        },
        exceptionHandler = { it.printStackTrace() },
        loadingMessage = "Iniciando sincronización...",
    )
  }

  fun cancelSync() {
    syncManager.cancelSync()
  }

  fun loadInitialData() {
    if (isLoadingInitial) return
    isLoadingInitial = true
    _stateFlow.update { HomeState.Loading }
    executeUseCase(
        action = {
          AppLogger.info("HomeViewModel.loadInitialData -> start")
          withTimeout(30_000) {
            userInfo = fetchUserInfoUseCase.invoke(null)
            AppLogger.info("HomeViewModel.loadInitialData -> userInfo loaded")
            when (val gate = posProfileGate.ensureReady()) {
              is GateResult.Failed -> error(gate.reason)
              is GateResult.Pending -> error(gate.reason)
              GateResult.Ready -> Unit
            }
            AppLogger.info("HomeViewModel.loadInitialData -> gate ready")
            posProfiles = fetchPosProfileUseCase.invoke(userInfo.email)
            AppLogger.info("HomeViewModel.loadInitialData -> profiles loaded")
            loadMetricsForActiveShift()
            refreshInventoryAlerts()
            _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
            AppLogger.info("HomeViewModel.loadInitialData -> done")
          }
        },
        exceptionHandler = { e ->
          AppLogger.warn("HomeViewModel.loadInitialData -> error", e)
          _stateFlow.update { HomeState.Error(e.message ?: "Error") }
        },
        finallyHandler = { isLoadingInitial = false },
        loadingMessage = "Cargando inicio...",
    )
  }

  fun refreshMetrics() {
    executeUseCase(
        action = {
          loadMetricsForActiveShift()
          refreshInventoryAlerts()
        },
        exceptionHandler = { it.printStackTrace() },
        loadingMessage = "Actualizando métricas...",
    )
  }

  private suspend fun loadMetricsForActiveShift() {
    val openingId = resolveMetricsOpeningEntryId()
    val refreshed =
        loadHomeMetricsUseCase(
            HomeMetricInput(
                days = 7,
                nowMillis = Clock.System.now().toEpochMilliseconds(),
                openingEntryId = openingId,
            )
        )
    _homeMetrics.value = refreshed.copy()
  }

  private suspend fun resolveMetricsOpeningEntryId(): String? {
    val fromReporting =
        contextManager.resolveOpeningEntryForReporting()?.trim()?.takeIf { it.isNotBlank() }
    if (!fromReporting.isNullOrBlank()) return fromReporting

    val fromFlow = openingEntryId.value?.trim()?.takeIf { it.isNotBlank() }
    if (!fromFlow.isNullOrBlank()) return fromFlow
    return contextManager.getActiveCashboxWithDetails()?.cashbox?.openingEntryId?.trim()?.takeIf {
      it.isNotBlank()
    }
  }

  private fun observeLiveShiftMetrics():
      Flow<HomeLiveShiftMetrics> {
    return openingEntryId
        .map { it?.trim()?.takeIf { value -> value.isNotBlank() } }
        .distinctUntilChanged()
        .flatMapLatest { openingId ->
          if (openingId.isNullOrBlank()) {
            emptyFlow()
          } else {
            observeHomeLiveShiftMetricsUseCase.observe(
                ObserveHomeLiveShiftMetricsInput(
                    postingDate =
                        Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                            .toString(),
                    openingEntryId = openingId,
                )
            )
          }
        }
  }

  private fun mergeLiveShiftMetrics(
      current: HomeMetrics,
      live: HomeLiveShiftMetrics,
  ): HomeMetrics {
    val liveByCurrency = live.byCurrency.associateBy { normalizeCurrency(it.currency) }

    val existingByCurrency = current.currencyMetrics.associateBy { normalizeCurrency(it.currency) }
    val allCurrencyKeys = (existingByCurrency.keys + liveByCurrency.keys).toList().sorted()

    val mergedCurrencies =
        allCurrencyKeys.map { key ->
          val existing = existingByCurrency[key]
          val liveCurrency = liveByCurrency[key]
          if (existing != null) {
            val liveTotal = liveCurrency?.totalSalesToday ?: 0.0
            val liveInvoices = liveCurrency?.invoicesToday ?: 0
            val liveCustomers = liveCurrency?.customersToday ?: 0
            val liveAvg = liveCurrency?.avgTicket ?: 0.0

            existing.copy(
                totalSalesToday = liveTotal,
                invoicesToday = liveInvoices,
                avgTicket = liveAvg,
                customersToday = liveCustomers,
            )
          } else {
            val currency = liveCurrency?.currency ?: key
            val liveTotal = liveCurrency?.totalSalesToday ?: 0.0
            val liveInvoices = liveCurrency?.invoicesToday ?: 0
            val liveCustomers = liveCurrency?.customersToday ?: 0
            val liveAvg = liveCurrency?.avgTicket ?: 0.0
            CurrencyHomeMetric(
                currency = currency,
                totalSalesToday = liveTotal,
                invoicesToday = liveInvoices,
                avgTicket = liveAvg,
                customersToday = liveCustomers,
                outstandingTotal = 0.0,
            )
          }
        }

    return current.copy(
        totalSalesToday = live.totalSalesToday,
        invoicesToday = live.invoicesToday,
        avgTicket = live.avgTicket,
        customersToday = live.customersToday,
        currencyMetrics = mergedCurrencies,
    )
  }

  private suspend fun refreshInventoryAlerts() {
    val ctx = syncContextProvider.buildContext() ?: return
    if (ctx.warehouseId.isBlank()) return
    val alertsEnabled = generalPreferences.getInventoryAlertsEnabled()
    val alertHour = generalPreferences.getInventoryAlertHour()
    val alertMinute = generalPreferences.getInventoryAlertMinute()
    if (!alertsEnabled) {
      _homeMetrics.update { it.copy(inventoryAlerts = emptyList()) }
      scheduleDailyInventoryReminder(false, "Inventory Alerts", "", alertHour, alertMinute)
      return
    }
    val alerts =
        loadInventoryAlertsUseCase(
            InventoryAlertInput(
                instanceId = ctx.instanceId,
                companyId = ctx.companyId,
                warehouseId = ctx.warehouseId,
                limit = 20,
            )
        )
    _homeMetrics.update { it.copy(inventoryAlerts = alerts) }
    val hasAlerts = alerts.isNotEmpty()
    val scheduledMessage =
        if (hasAlerts) "Alertas de inventario pendientes: ${alerts.size} (${ctx.warehouseId})"
        else ""
    scheduleDailyInventoryReminder(
        hasAlerts,
        "Inventory Alerts",
        scheduledMessage,
        alertHour,
        alertMinute,
    )
    if (!hasAlerts) return

    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    val lastDate = generalPreferences.getInventoryAlertDate()
    if (lastDate != today) {
      generalPreferences.setInventoryAlertDate(today)
      val message = "Alertas de inventario: ${alerts.size} (${ctx.warehouseId})"
      _inventoryAlertMessage.value = message
      notifySystem("Inventory Alerts", message)
    }
  }

  private fun millisUntilNextAlert(hour: Int, minute: Int): Long {
    val tz = TimeZone.currentSystemDefault()
    val nowInstant = Clock.System.now()
    val nowLocal = nowInstant.toLocalDateTime(tz)
    val targetToday = nowLocal.date.atTime(hour, minute)
    val targetLocal =
        if (targetToday <= nowLocal) {
          nowLocal.date.plus(DatePeriod(days = 1)).atTime(hour, minute)
        } else {
          targetToday
        }
    val targetInstant = targetLocal.toInstant(tz)
    return (targetInstant.toEpochMilliseconds() - nowInstant.toEpochMilliseconds()).coerceAtLeast(
        1_000L
    )
  }

  fun consumeInventoryAlertMessage() {
    _inventoryAlertMessage.value = null
  }

  fun isCashboxOpen(): StateFlow<Boolean> = contextManager.cashboxState

  fun resetToInitialState() {
    _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
  }

  fun logout() {
    executeUseCase(
        action = {
          logoutUseCase.invoke(null)
          _stateFlow.update { HomeState.Logout }
          navManager.navigateTo(NavRoute.Login)
        },
        exceptionHandler = { it.printStackTrace() },
        loadingMessage = "Cerrando sesión...",
    )
  }

  fun openSettings() {
    navManager.navigateTo(NavRoute.Settings)
  }

  fun openReconciliation() {
    navManager.navigateTo(NavRoute.Reconciliation(ReconciliationMode.Close))
  }

  fun openCloseCashbox() {
    navManager.navigateTo(NavRoute.Reconciliation(ReconciliationMode.Close))
  }

  fun onError(error: String) {
    _stateFlow.update { HomeState.Error(error) }
  }

  fun onPosSelected(pos: POSProfileSimpleBO) {
    _stateFlow.update { HomeState.POSInfoLoading }
    executeUseCase(
        action = {
          val posProfileInfo = fetchPosProfileInfoLocalUseCase(pos.name)
          _stateFlow.update { HomeState.POSInfoLoaded(posProfileInfo, posProfileInfo.currency) }
        },
        exceptionHandler = { it.printStackTrace() },
        loadingMessage = "Cargando perfil POS...",
    )
  }

  fun closeCashbox() {
    viewModelScope.launch { contextManager.closeCashBox() }
  }

  fun loadOpeningProfile(profileId: String?) {
    if (profileId.isNullOrBlank()) {
      _openingState.update {
        it.copy(profileId = null, methods = emptyList(), cashMethodsByCurrency = emptyMap())
      }
      return
    }
    viewModelScope.launch {
      _openingState.update { it.copy(isLoading = true, error = null) }
      runCatching {
            when (val gate = openingGate.ensureReady(profileId)) {
              is GateResult.Failed -> error(gate.reason)
              is GateResult.Pending -> error(gate.reason)
              GateResult.Ready -> Unit
            }
            val profile = posProfileDao.getPOSProfile(profileId)
            val methods = paymentMethodLocalRepository.getMethodsForProfile(profileId)
            val cashMethods =
                paymentMethodLocalRepository.getCashMethodsGroupedByCurrency(
                    profileId,
                    profile.currency,
                )
            _openingState.update {
              it.copy(
                  profileId = profile.profileName,
                  company = profile.company,
                  baseCurrency = profile.currency,
                  methods = methods,
                  cashMethodsByCurrency = cashMethods,
                  isLoading = false,
                  error = null,
              )
            }
          }
          .onFailure { error ->
            _openingState.update {
              it.copy(isLoading = false, error = error.message ?: "Unable to load profile data")
            }
          }
    }
  }

  fun openCashbox(entry: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) {
    viewModelScope.launch {
      val ctx = contextManager.openCashBox(entry, amounts)
      if (ctx == null) {
        _openingState.update {
          it.copy(isLoading = false, error = "Sesion expirada. Inicia sesion nuevamente.")
        }
      }
    }
  }
}
// 617