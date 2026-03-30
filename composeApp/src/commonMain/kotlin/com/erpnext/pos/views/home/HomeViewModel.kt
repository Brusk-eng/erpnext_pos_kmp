package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.usecases.*
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.preferences.BootstrapContextPreferences
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.sync.*
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.reconciliation.ReconciliationMode
import kotlinx.coroutines.IO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
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
    private val observeHomeLiveShiftMetricsUseCase: ObserveHomeLiveShiftMetricsUseCase,
    private val posProfileGate: PosProfileGate,
    private val openingGate: OpeningGate,
    private val homeRefreshController: HomeRefreshController,
    private val sessionRefresher: SessionRefresher,
    private val syncContextProvider: SyncContextProvider,
    private val generalPreferences: GeneralPreferences,
    private val bootstrapContextPreferences: BootstrapContextPreferences,
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
    private var refreshMetricsJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) { contextManager.initializeContext() }
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
                val normalized = openingId.normalizedOpeningEntryId()
                if (normalized == null && contextManager.cashboxState.value && lastMetricsOpeningEntryId != null) {
                    return@collectLatest
                }
                if (normalized == lastMetricsOpeningEntryId) return@collectLatest
                lastMetricsOpeningEntryId = normalized
                requestMetricsRefresh()
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
                    requestMetricsRefresh()
                }
            }
        }
        viewModelScope.launch {
            homeRefreshController.events.collectLatest {
                if (_stateFlow.value is HomeState.POSProfiles) {
                    requestMetricsRefresh()
                } else {
                    loadInitialData()
                }
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
                    withContext(Dispatchers.Default) {
                        userInfo = fetchUserInfoUseCase.invoke(null)
                        AppLogger.info("HomeViewModel.loadInitialData -> userInfo loaded")
                        posProfileGate.ensureReady().requireReady()
                        AppLogger.info("HomeViewModel.loadInitialData -> gate ready")
                        posProfiles = fetchPosProfileUseCase.invoke(userInfo.email)
                        AppLogger.info("HomeViewModel.loadInitialData -> profiles loaded")
                        loadMetricsForActiveShift()
                        _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
                        AppLogger.info("HomeViewModel.loadInitialData -> done")
                    }
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

    private fun requestMetricsRefresh(showLoading: Boolean = false) {
        if (refreshMetricsJob?.isActive == true) return
        refreshMetricsJob =
            executeUseCase(
                action = {
                    withContext(Dispatchers.IO) {
                        loadMetricsForActiveShift()
                    }
                },
                exceptionHandler = { it.printStackTrace() },
                finallyHandler = { refreshMetricsJob = null },
                showLoading = showLoading,
                loadingMessage = "Actualizando métricas...",
            )
    }

    private suspend fun loadMetricsForActiveShift() {
        val openingId = resolveMetricsOpeningEntryId()
        val previous = _homeMetrics.value
        if (openingId.isNullOrBlank() && contextManager.cashboxState.value && previous != HomeMetrics()) {
            return
        }
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
        val fromReporting = contextManager.resolveOpeningEntryForReporting().normalizedOpeningEntryId()
        if (!fromReporting.isNullOrBlank()) return fromReporting

        val fromFlow = openingEntryId.value.normalizedOpeningEntryId()
        if (!fromFlow.isNullOrBlank()) return fromFlow
        val fromActiveCashbox =
            contextManager.getActiveCashboxWithDetails()?.cashbox?.openingEntryId.normalizedOpeningEntryId()
        if (!fromActiveCashbox.isNullOrBlank()) return fromActiveCashbox

        return bootstrapContextPreferences.load().posOpeningEntry.normalizedOpeningEntryId()
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
                _stateFlow.update {
                    HomeState.POSInfoLoaded(
                        posProfileInfo,
                        posProfileInfo.currency
                    )
                }
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
                openingGate.ensureReady(profileId).requireReady()
                val profile = posProfileDao.getPOSProfile(profileId)
                val methods = paymentMethodLocalRepository.getMethodsForProfile(profileId)
                val cashMethods =
                    paymentMethodLocalRepository.getCashMethodsGroupedByCurrency(
                        profileId,
                        profile.currency,
                    )
                _openingState.value =
                    buildOpeningProfileState(
                        profileId = profile.profileName,
                        company = profile.company,
                        baseCurrency = profile.currency,
                        methods = methods,
                        cashMethodsByCurrency = cashMethods,
                    )
            }
                .onFailure { error ->
                    _openingState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Unable to load profile data"
                        )
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
