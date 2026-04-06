@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.navigation.LocalTopBarController
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.view.SnackbarController
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeState,
    actions: HomeAction,
) {
  var showOpeningView by remember { mutableStateOf(false) }
  var currentProfiles by remember { mutableStateOf(emptyList<POSProfileSimpleBO>()) }
  var currentUser by remember { mutableStateOf<UserBO?>(null) }
  val snackbar: SnackbarController = koinInject()
  val syncState by actions.syncState.collectAsState()
  val strings = LocalAppStrings.current
  val topBarController = LocalTopBarController.current
  val homeMetrics by actions.homeMetrics.collectAsState()
  val openingState by actions.openingState.collectAsState()
  val isCashboxOpen by actions.isCashboxOpen().collectAsState()
  val windowSizeClass = rememberWindowSizeClass()
  val isCompactWidthPhone = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
  val isCompactHeightPhone =
      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact &&
          (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
              windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium)

  LaunchedEffect(uiState) {
    if (uiState is HomeState.POSProfiles) {
      currentProfiles = uiState.posProfiles
      currentUser = uiState.user
    }
  }

  LaunchedEffect(isCashboxOpen) {
    if (isCashboxOpen && showOpeningView) {
      showOpeningView = false
    }
  }

  LaunchedEffect(showOpeningView) { topBarController.update(isVisible = !showOpeningView) }
  DisposableEffect(Unit) { onDispose { topBarController.reset() } }

  if (showOpeningView) {
    CashboxOpeningScreen(
        uiState = uiState,
        profiles = currentProfiles,
        user = currentUser,
        openingState = openingState,
        onLoadOpeningProfile = actions.onLoadOpeningProfile,
        onOpenCashbox = actions.onOpenCashbox,
        onSelectProfile = { actions.onPosSelected(it) },
        onDismiss = {
          showOpeningView = false
          actions.initialState()
        },
        snackbar,
    )
    return
  }

  Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
    Column(
        modifier =
            Modifier.padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 12.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (uiState) {
        is HomeState.Loading -> FullScreenLoadingIndicator()
        is HomeState.Error -> FullScreenErrorMessage(uiState.message, onRetry = { actions.loadInitialData() })
        is HomeState.POSProfiles ->
            HomeProfilesContent(
                uiState = uiState,
                actions = actions,
                homeMetrics = homeMetrics,
                syncState = syncState,
                strings = strings,
                isCashboxOpen = isCashboxOpen,
                isCompactHeightPhone = isCompactHeightPhone,
                isCompactWidthPhone = isCompactWidthPhone,
                canOpenCashbox = currentProfiles.isNotEmpty(),
                onOpenCashbox = { showOpeningView = true },
            )
        else -> Unit
      }
    }
  }
}

@Composable
private fun HomeProfilesContent(
    uiState: HomeState.POSProfiles,
    actions: HomeAction,
    homeMetrics: HomeMetrics,
    syncState: SyncState,
    strings: com.erpnext.pos.localization.AppStrings,
    isCashboxOpen: Boolean,
    isCompactHeightPhone: Boolean,
    isCompactWidthPhone: Boolean,
    canOpenCashbox: Boolean,
    onOpenCashbox: () -> Unit,
) {
  if (isCashboxOpen) {
    HomeOpenedContent(
        userName = uiState.user.firstName,
        isCompactHeightPhone = isCompactHeightPhone,
        metrics = homeMetrics,
        actions = actions,
        syncState = syncState,
        strings = strings,
        modifier =
            Modifier.fillMaxWidth(),
    )
  } else {
    HomeClosedContent(
        modifier =
            if (isCompactHeightPhone) {
              Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            } else {
              Modifier.fillMaxWidth()
            }
    )
  }

  OpenCashboxButton(
      isCashboxOpen = isCashboxOpen,
      isCompactWidthPhone = isCompactWidthPhone,
      isSyncing = syncState is SyncState.SYNCING,
      onClick = {
        if (!isCashboxOpen && canOpenCashbox) {
          onOpenCashbox()
        }
      },
  )
}
