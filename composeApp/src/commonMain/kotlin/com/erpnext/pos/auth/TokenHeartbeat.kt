package com.erpnext.pos.auth

import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Valida la sesión en momentos puntuales de bajo costo. El refresco real bajo demanda ocurre
 * cuando una operación protegida necesita credenciales válidas.
 */
class TokenHeartbeat(
    private val scope: CoroutineScope,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor,
    private val generalPreferences: GeneralPreferences,
    private val lifecycleObserver: AppLifecycleObserver,
) {
  fun start() {
    scope.launch {
      val offlineMode = generalPreferences.getOfflineMode()
      val isOnline = networkMonitor.isConnected.first()
      if (isOnline && !offlineMode) {
        sessionRefresher.ensureValidSession()
      }
    }

    scope.launch {
      lifecycleObserver.onResume.collectLatest {
        val offlineMode = generalPreferences.getOfflineMode()
        val currentlyOnline = networkMonitor.isConnected.first()
        if (currentlyOnline && !offlineMode) {
          sessionRefresher.ensureValidSession()
        }
      }
    }
  }
}
