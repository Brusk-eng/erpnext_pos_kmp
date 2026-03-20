package com.erpnext.pos.printing.application

import com.erpnext.pos.auth.AppLifecycleObserver
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.usecase.CheckPrinterConnectionUseCase
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.utils.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class PrinterMonitorConfig(
    val printerEnabled: Boolean,
    val profile: PrinterProfile?,
)

class PrinterConnectionMonitor(
    private val scope: CoroutineScope,
    private val profileRepository: IPrinterProfileRepository,
    private val generalPreferences: GeneralPreferences,
    private val lifecycleObserver: AppLifecycleObserver,
    private val checkPrinterConnectionUseCase: CheckPrinterConnectionUseCase,
    private val statusStore: PrinterConnectionStatusStore,
) {
  companion object {
    private const val probeIntervalMillis = 15_000L
    private const val initialProbeDelayMillis = 5_000L
  }

  private var observeJob: Job? = null
  private var probeJob: Job? = null
  private var currentConfig: PrinterMonitorConfig = PrinterMonitorConfig(false, null)

  fun start() {
    if (observeJob != null) return

    observeJob =
        scope.launch(Dispatchers.Default) {
          launch {
            combine(
                    generalPreferences.printerEnabled,
                    profileRepository.observeDefaultProfile(),
                ) { printerEnabled, profile ->
                  PrinterMonitorConfig(
                      printerEnabled = printerEnabled,
                      profile = profile?.takeIf { it.isEnabled },
                  )
                }
                .distinctUntilChanged()
                .collect { config ->
                  currentConfig = config
                  AppLogger.info(
                      "PrinterConnectionMonitor.config -> enabled=${config.printerEnabled}, profile=${config.profile?.name ?: "none"}"
                  )
                  restartProbe(config)
                }
          }

          launch {
            lifecycleObserver.onResume.collect {
              val config = currentConfig
              if (!config.printerEnabled || config.profile == null) return@collect
              AppLogger.info(
                  "PrinterConnectionMonitor.onResume -> probing profile=${config.profile.name}"
              )
              triggerImmediateProbe(config.profile)
            }
          }
        }
  }

  private fun restartProbe(config: PrinterMonitorConfig) {
    probeJob?.cancel()

    if (!config.printerEnabled || config.profile == null) {
      AppLogger.info("PrinterConnectionMonitor.restartProbe -> reset (disabled or no default profile)")
      statusStore.reset()
      return
    }

    probeJob =
        scope.launch(Dispatchers.Default) {
          probeProfile(config.profile)
          while (true) {
            delay(nextDelayMillis())
            probeProfile(config.profile)
          }
        }
  }

  private fun triggerImmediateProbe(profile: PrinterProfile) {
    probeJob?.cancel()
    probeJob =
        scope.launch(Dispatchers.Default) {
          probeProfile(profile)
          while (true) {
            delay(nextDelayMillis())
            probeProfile(profile)
          }
        }
  }

  private suspend fun probeProfile(profile: PrinterProfile) {
    try {
      AppLogger.info("PrinterConnectionMonitor.probe -> profile=${profile.name}, profileId=${profile.id}")
      checkPrinterConnectionUseCase(profile)
          .onSuccess {
            statusStore.markConnected(profile.id)
            AppLogger.info("PrinterConnectionMonitor.probe -> connected profile=${profile.name}")
          }
          .onFailure { error ->
            statusStore.markDisconnected(profile.id)
            AppLogger.warn(
                "PrinterConnectionMonitor.probe -> disconnected profile=${profile.name}",
                error,
                reportToSentry = false,
            )
          }
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Throwable) {
      statusStore.markDisconnected(profile.id)
      AppLogger.warn(
          "PrinterConnectionMonitor.probe -> unexpected failure profile=${profile.name}",
          error,
          reportToSentry = false,
      )
    }
  }

  private fun nextDelayMillis(): Long {
    return when (statusStore.snapshot.value.status) {
      PrinterConnectionStatus.UNKNOWN -> initialProbeDelayMillis
      else -> probeIntervalMillis
    }
  }
}
