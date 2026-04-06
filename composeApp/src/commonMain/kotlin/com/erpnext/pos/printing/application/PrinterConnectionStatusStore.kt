package com.erpnext.pos.printing.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PrinterConnectionStatus {
  UNKNOWN,
  CHECKING,
  CONNECTED,
  DISCONNECTED,
}

data class PrinterConnectionSnapshot(
    val profileId: String? = null,
    val status: PrinterConnectionStatus = PrinterConnectionStatus.UNKNOWN,
)

class PrinterConnectionStatusStore {
  private val _snapshot = MutableStateFlow(PrinterConnectionSnapshot())
  val snapshot: StateFlow<PrinterConnectionSnapshot> = _snapshot.asStateFlow()

  fun markChecking(profileId: String?) {
    _snapshot.value =
        PrinterConnectionSnapshot(
            profileId = profileId,
            status = PrinterConnectionStatus.CHECKING,
        )
  }

  fun markConnected(profileId: String?) {
    _snapshot.value =
        PrinterConnectionSnapshot(
            profileId = profileId,
            status = PrinterConnectionStatus.CONNECTED,
        )
  }

  fun markDisconnected(profileId: String?) {
    _snapshot.value =
        PrinterConnectionSnapshot(
            profileId = profileId,
            status = PrinterConnectionStatus.DISCONNECTED,
        )
  }

  fun reset() {
    _snapshot.value = PrinterConnectionSnapshot()
  }
}
