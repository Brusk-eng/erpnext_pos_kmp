@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, FlowPreview::class)

package com.erpnext.pos.views.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.DenominationCount
import com.erpnext.pos.domain.models.OpeningSessionDraft
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.preferences.OpeningSessionPreferences
import com.erpnext.pos.utils.DecimalFormatter
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.PaymentModeWithAmount
import com.erpnext.pos.views.components.DenominationCounter
import com.erpnext.pos.views.components.DenominationCounterLabels
import com.erpnext.pos.views.components.DenominationUi
import com.erpnext.pos.views.components.buildDenominationsForCurrency
import com.erpnext.pos.views.home.components.CompactOpeningActionBar
import com.erpnext.pos.views.home.components.CompactOpeningStepSelector
import com.erpnext.pos.views.home.components.OpeningCashContent
import com.erpnext.pos.views.home.components.OpeningFormSection
import com.erpnext.pos.views.home.components.applyDraftCounts
import kotlin.math.max
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal enum class OpeningStep {
  Details,
  Count,
}

@Composable
fun CashboxOpeningScreen(
    uiState: HomeState,
    profiles: List<POSProfileSimpleBO>,
    user: UserBO?,
    openingState: CashboxOpeningProfileState,
    onLoadOpeningProfile: (String?) -> Unit,
    onOpenCashbox: suspend (POSProfileSimpleBO, List<PaymentModeWithAmount>) -> Unit,
    onSelectProfile: (POSProfileSimpleBO) -> Unit,
    onDismiss: () -> Unit,
    snackbar: SnackbarController,
) {
  var isSubmitting by remember { mutableStateOf(false) }
  var selectedProfile by remember { mutableStateOf<POSProfileSimpleBO?>(null) }
  var profileMenuExpanded by remember { mutableStateOf(false) }
  var selectedCurrency by remember { mutableStateOf("") }
  var compactStep by remember { mutableStateOf(OpeningStep.Details) }
  var lastDraftKey by remember { mutableStateOf<String?>(null) }
  val denominationState = remember {
    androidx.compose.runtime.mutableStateMapOf<String, List<DenominationUi>>()
  }
  val openingSessionPreferences: OpeningSessionPreferences = org.koin.compose.koinInject()
  val openingDraft by openingSessionPreferences.draft.collectAsState(initial = null)
  val formatter = remember { DecimalFormatter() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(profiles) {
    if (profiles.size == 1) {
      selectedProfile = profiles.first()
      onSelectProfile(profiles.first())
      profileMenuExpanded = false
    }
  }

  LaunchedEffect(selectedProfile?.name) {
    selectedCurrency = ""
    denominationState.clear()
    lastDraftKey = null
    onLoadOpeningProfile(selectedProfile?.name)
  }

  val paymentModes =
      openingState.methods.map {
        com.erpnext.pos.domain.models.PaymentModesBO(name = it.mopName, modeOfPayment = it.mopName)
      }
  val baseCurrency =
      openingState.baseCurrency.takeIf { it.isNotBlank() }
          ?: selectedProfile?.currency
          ?: profiles.firstOrNull()?.currency
          ?: "USD"
  val normalizedBaseCurrency = normalizeCurrency(baseCurrency)
  val cashMethodsByCurrency = openingState.cashMethodsByCurrency
  val cashMethodByCurrency = cashMethodsByCurrency.mapValues { it.value.first() }
  val cashMethodsMissingCurrency =
      openingState.methods.filter { method ->
        method.currency.isNullOrBlank() &&
            method.type?.equals("Cash", ignoreCase = true) == true &&
            method.enabled &&
            method.enabledInProfile
      }
  val openingCurrencies = remember(cashMethodsByCurrency) { cashMethodsByCurrency.keys.sorted() }

  LaunchedEffect(openingDraft, profiles, user?.email) {
    val userEmail = user?.email ?: return@LaunchedEffect
    val draftProfileId = openingDraft?.takeIf { it.user == userEmail }?.posProfileId
    if (draftProfileId != null && selectedProfile == null) {
      profiles
          .firstOrNull { it.name == draftProfileId }
          ?.let { profile ->
            selectedProfile = profile
            onSelectProfile(profile)
            profileMenuExpanded = false
          }
    }
  }

  LaunchedEffect(openingCurrencies) {
    if (openingCurrencies.isNotEmpty() && selectedCurrency !in openingCurrencies) {
      selectedCurrency = openingCurrencies.first()
    }
    openingCurrencies.forEach { currency ->
      if (!denominationState.containsKey(currency)) {
        denominationState[currency] =
            buildDenominationsForCurrency(
                currency = currency,
                symbolOverride = currency.toCurrencySymbol(),
                formatter = formatter,
            )
      }
    }
  }

  LaunchedEffect(openingDraft, selectedProfile?.name, user?.email, openingCurrencies) {
    val profileId = selectedProfile?.name ?: return@LaunchedEffect
    val userEmail = user?.email ?: return@LaunchedEffect
    val draft = openingDraft?.takeIf { it.posProfileId == profileId && it.user == userEmail }
    val draftKey = draft?.let { "${it.posProfileId}-${it.user}" }
    if (draft != null && draftKey != null && draftKey != lastDraftKey) {
      openingCurrencies.forEach { currency ->
        val baseDenoms =
            buildDenominationsForCurrency(
                currency = currency,
                symbolOverride = currency.toCurrencySymbol(),
                formatter = formatter,
            )
        val updated = applyDraftCounts(baseDenoms, draft.denominationCounts[currency].orEmpty())
        denominationState[currency] = updated
      }
      lastDraftKey = draftKey
    }
  }

  LaunchedEffect(selectedProfile?.name, user?.email, openingCurrencies) {
    val profileId = selectedProfile?.name ?: return@LaunchedEffect
    val userEmail = user?.email ?: return@LaunchedEffect
    snapshotFlow { denominationState.toMap().mapValues { (_, denoms) -> denoms.map { it.copy() } } }
        .debounce(500)
        .collectLatest { current ->
          if (current.isEmpty()) return@collectLatest
          val totals = current.mapValues { entry -> entry.value.sumOf { it.value * it.count } }
          val counts =
              current.mapValues { entry ->
                entry.value.map { DenominationCount(value = it.value, count = it.count) }
              }
          openingSessionPreferences.saveDraft(
              OpeningSessionDraft(
                  posProfileId = profileId,
                  user = userEmail,
                  createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                  openingCashByCurrency = totals,
                  denominationCounts = counts,
              )
          )
        }
  }

  val totalsByCurrency: Map<String, Double> =
      openingCurrencies.associateWith { currency ->
        denominationState[currency].orEmpty().sumOf { it.value * it.count }
      }
  val canOpen = selectedProfile != null && cashMethodsByCurrency.isNotEmpty()

  val handleOpen: () -> Unit = {
    val profile = selectedProfile
    if (profile == null) {
      scope.launch {
        snackbar.show("Selecciona un perfil de POS", SnackbarType.Error, SnackbarPosition.Top)
      }
    } else {
      val amountByMode =
          cashMethodByCurrency.entries.associate { (currency, resolved) ->
            resolved.mopName to (totalsByCurrency[currency] ?: 0.0)
          }
      val amounts =
          paymentModes.map { mode ->
            val amount = amountByMode[mode.name] ?: 0.0
            PaymentModeWithAmount(mode = mode, amount = amount)
          }
      isSubmitting = true
      scope.launch {
        val totalsMsg =
            totalsByCurrency.entries.joinToString(" · ") { (cur, total) ->
              formatCurrency(cur, total)
            }
        try {
          onOpenCashbox(profile, amounts)
          openingSessionPreferences.clearDraft()
          snackbar.show("Caja abierta: $totalsMsg", SnackbarType.Success, SnackbarPosition.Top)
          onDismiss()
        } catch (e: Exception) {
          snackbar.show(
              e.message ?: "Error al abrir caja",
              SnackbarType.Error,
              SnackbarPosition.Top,
          )
        }
        isSubmitting = false
      }
    }
  }

  Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val windowSizeClass = rememberWindowSizeClass()
      val isPhoneCompact =
          windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
              (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium &&
                  windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact)
      val hasCompactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
      val activeCurrency = selectedCurrency.ifBlank { normalizedBaseCurrency }
      val compactScrollState = rememberScrollState()

      Column(modifier = Modifier.fillMaxSize()) {
        OpeningHeader(onDismiss = onDismiss)

        val bottomPadding =
            if (isPhoneCompact) {
              if (hasCompactHeight) 128.dp else 148.dp
            } else {
              24.dp
            }

        if (isPhoneCompact) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = if (hasCompactHeight) 14.dp else 20.dp, vertical = 16.dp)
                      .padding(bottom = bottomPadding)
                      .verticalScroll(compactScrollState),
              verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            CompactOpeningStepSelector(
                currentStep = compactStep,
                totalsByCurrency = totalsByCurrency,
            ) { compactStep = it }

            when (compactStep) {
              OpeningStep.Details ->
                  OpeningFormSection(
                      user = user,
                      profiles = profiles,
                      selectedProfile = selectedProfile,
                      onSelectProfile = {
                        selectedProfile = it
                        onSelectProfile(it)
                      },
                      expanded = profileMenuExpanded,
                      onExpandedChange = { profileMenuExpanded = it },
                      totalsByCurrency = totalsByCurrency,
                      isLoading =
                          isSubmitting ||
                              openingState.isLoading ||
                              uiState is HomeState.POSInfoLoading,
                      canOpen = canOpen && !isSubmitting,
                      cashModesMissingCurrency = cashMethodsMissingCurrency.map { it.mopName },
                      onOpen = handleOpen,
                      onCancel = onDismiss,
                      showActions = false,
                  )
              OpeningStep.Count ->
                  OpeningCashContent(
                      countCurrencies = openingCurrencies,
                      selectedCurrency = activeCurrency,
                      denominations = denominationState[activeCurrency].orEmpty(),
                      onCurrencyChange = { selectedCurrency = it },
                      onDenominationChange = { value, count ->
                        val current = denominationState[activeCurrency].orEmpty()
                        denominationState[activeCurrency] =
                            current.map { denom ->
                              if (denom.value == value) denom.copy(count = max(0, count))
                              else denom
                            }
                      },
                      totalsByCurrency = totalsByCurrency,
                  )
            }
          }
        } else {
          Row(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = 20.dp, vertical = 16.dp)
                      .padding(bottom = bottomPadding),
              horizontalArrangement = Arrangement.spacedBy(24.dp),
          ) {
            Column(
                modifier = Modifier.weight(4f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OpeningFormSection(
                  user = user,
                  profiles = profiles,
                  selectedProfile = selectedProfile,
                  onSelectProfile = {
                    selectedProfile = it
                    onSelectProfile(it)
                  },
                  expanded = profileMenuExpanded,
                  onExpandedChange = { profileMenuExpanded = it },
                  totalsByCurrency = totalsByCurrency,
                  isLoading =
                      isSubmitting || openingState.isLoading || uiState is HomeState.POSInfoLoading,
                  canOpen = canOpen && !isSubmitting,
                  cashModesMissingCurrency = cashMethodsMissingCurrency.map { it.mopName },
                  onOpen = handleOpen,
                  onCancel = onDismiss,
              )
            }

            Column(
                modifier = Modifier.weight(8f).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OpeningCashContent(
                  countCurrencies = openingCurrencies,
                  selectedCurrency = activeCurrency,
                  denominations = denominationState[activeCurrency].orEmpty(),
                  onCurrencyChange = { selectedCurrency = it },
                  onDenominationChange = { value, count ->
                    val current = denominationState[activeCurrency].orEmpty()
                    denominationState[activeCurrency] =
                        current.map { denom ->
                          if (denom.value == value) denom.copy(count = max(0, count)) else denom
                        }
                  },
                  totalsByCurrency = totalsByCurrency,
              )
            }
          }
        }
      }

      if (isPhoneCompact) {
        CompactOpeningActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            step = compactStep,
            canOpen = canOpen && !isSubmitting,
            totalsByCurrency = totalsByCurrency,
            onBack = {
              if (compactStep == OpeningStep.Details) onDismiss() else compactStep = OpeningStep.Details
            },
            onNext = { compactStep = OpeningStep.Count },
            onOpen = handleOpen,
        )
      }

      SnackbarHost(
          snackbar = snackbar.snackbar.collectAsState().value,
          onDismiss = { snackbar.dismiss() },
          modifier = Modifier.fillMaxSize(),
      )

      if (isSubmitting) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
    }
  }
}

@Composable
private fun OpeningHeader(onDismiss: () -> Unit) {
  Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
  ) {
    Column {
      Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        IconButton(onClick = onDismiss) {
          Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column {
          Text(
              text = "Apertura de caja",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
              text = "Nueva entrada",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

private fun applyDraftCounts(
    denominations: List<DenominationUi>,
    counts: List<DenominationCount>,
): List<DenominationUi> {
  if (counts.isEmpty()) return denominations
  val countMap = counts.associate { it.value to it.count }
  return denominations.map { denom -> denom.copy(count = countMap[denom.value] ?: 0) }
}
