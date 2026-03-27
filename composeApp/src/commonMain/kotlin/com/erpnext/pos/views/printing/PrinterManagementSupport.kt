package com.erpnext.pos.views.printing

import com.erpnext.pos.domain.printing.model.DiscoveredPrinterDevice
import com.erpnext.pos.domain.printing.model.LabelDocument
import com.erpnext.pos.domain.printing.model.LabelField
import com.erpnext.pos.domain.printing.model.PrintAlignment
import com.erpnext.pos.domain.printing.model.PrintJob
import com.erpnext.pos.domain.printing.model.PrintJobStatus
import com.erpnext.pos.domain.printing.model.PrinterCapabilities
import com.erpnext.pos.domain.printing.model.PrinterFamily
import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.printing.model.ReceiptLine
import com.erpnext.pos.domain.printing.model.ReceiptSection
import com.erpnext.pos.domain.printing.model.ReceiptTotals
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.printing.platform.PrintingPlatform
import kotlin.time.Clock

internal data class PrinterUiCoreState(
    val profiles: List<PrinterProfile>,
    val jobs: List<PrintJob>,
    val selectedProfileId: String?,
    val form: PrinterProfileFormState,
)

internal fun PrinterUiCoreState.toUiState(
    discoveredDevices: List<DiscoveredPrinterDevice>,
    message: String?,
    isBusy: Boolean,
    isCheckingConnection: Boolean,
): PrinterManagementUiState {
  val activeProfile = profiles.firstOrNull { it.id == selectedProfileId }
  val defaultProfile = profiles.firstOrNull { it.isDefault }
  return PrinterManagementUiState(
      profiles = profiles,
      selectedProfileId = selectedProfileId,
      selectedProfileName = activeProfile?.name,
      defaultProfileName = defaultProfile?.name,
      form =
          if (selectedProfileId == form.id || activeProfile == null) {
            form
          } else {
            activeProfile.toFormState()
          },
      jobs = jobs.take(10),
      discoveredDevices = discoveredDevices,
      message = message,
      isBusy = isBusy,
      isCheckingConnection = isCheckingConnection,
      platformSummary = buildPlatformSummary(),
  )
}

internal fun buildPlatformSummary(): String =
    "Platform ${PrintingPlatform.capabilities.platform} supports ${PrintingPlatform.capabilities.supportedTransports.joinToString()}"

internal fun PrinterProfile.toFormState(): PrinterProfileFormState =
    PrinterProfileFormState(
        id = id,
        name = name,
        brandHint = brandHint.orEmpty(),
        modelHint = modelHint.orEmpty(),
        family = family,
        language = language,
        supportedTransports = supportedTransports,
        preferredTransport = preferredTransport,
        host = host.orEmpty(),
        port = (port ?: 9100).toString(),
        bluetoothMacAddress = bluetoothMacAddress.orEmpty(),
        bluetoothName = bluetoothName.orEmpty(),
        paperWidthMm = paperWidthMm.toString(),
        charactersPerLine = charactersPerLine.toString(),
        codePage = codePage,
        autoCut = autoCut,
        openDrawer = openDrawer,
        isDefault = isDefault,
        isEnabled = isEnabled,
        notes = notes.orEmpty(),
    )

internal fun PrinterProfileFormState.toProfile(idProvider: () -> String): PrinterProfile {
  val resolvedId = id ?: idProvider()
  return PrinterProfile(
      id = resolvedId,
      name = name.trim(),
      brandHint = brandHint.trim().takeIf { it.isNotBlank() },
      modelHint = modelHint.trim().takeIf { it.isNotBlank() },
      family = family,
      language = language,
      supportedTransports = supportedTransports.ifEmpty { setOf(TransportType.TCP_RAW) },
      preferredTransport = preferredTransport,
      host = host.trim().takeIf { it.isNotBlank() },
      port = port.toIntOrNull() ?: 9100,
      bluetoothMacAddress = bluetoothMacAddress.trim().takeIf { it.isNotBlank() },
      bluetoothName = bluetoothName.trim().takeIf { it.isNotBlank() },
      capabilities =
          PrinterCapabilities(
              paperWidthMm = paperWidthMm.toIntOrNull() ?: 80,
              charactersPerLine = charactersPerLine.toIntOrNull() ?: 32,
              codePage = codePage.trim().ifBlank { "CP437" },
              autoCut = autoCut,
              openDrawer = openDrawer,
          ),
      isDefault = isDefault,
      isEnabled = isEnabled,
      notes = notes.trim().takeIf { it.isNotBlank() },
  )
}

internal fun buildTestDocument(
    form: PrinterProfileFormState,
    nowProvider: () -> Long,
): com.erpnext.pos.domain.printing.model.PrintDocument =
    if (form.family == PrinterFamily.RECEIPT) {
      ReceiptDocument(
          documentId = "test-${nowProvider()}",
          header =
              ReceiptSection(
                  lines = listOf("ERPNext POS", "Printer Test"),
                  alignment = PrintAlignment.CENTER,
              ),
          bodyLines =
              listOf(
                  ReceiptLine("Coffee beans", "C$ 120.00"),
                  ReceiptLine("Notebook", "C$ 35.00"),
              ),
          totals = ReceiptTotals(subTotal = "C$ 155.00", tax = "C$ 0.00", total = "C$ 155.00"),
          footer =
              ReceiptSection(
                  lines = listOf("Offline-first printing baseline", "Thank you"),
                  alignment = PrintAlignment.CENTER,
              ),
      )
    } else {
      LabelDocument(
          documentId = "label-${nowProvider()}",
          fields =
              listOf(
                  LabelField(30, 30, text = "ERPNext POS"),
                  LabelField(30, 80, text = "LABEL TEST"),
                  LabelField(30, 130, text = form.name.ifBlank { "Printer" }),
              ),
      )
    }

internal fun createPrintJob(
    jobId: String,
    profileId: String,
    documentId: String,
    documentType: String,
    summary: String,
    nowEpochMs: Long,
    status: PrintJobStatus = PrintJobStatus.PENDING,
    attempts: Int = 0,
    lastError: String? = null,
    completedAtEpochMs: Long? = null,
): PrintJob =
    PrintJob(
        id = jobId,
        profileId = profileId,
        documentId = documentId,
        documentType = documentType,
        summary = summary,
        status = status,
        attempts = attempts,
        lastError = lastError,
        createdAtEpochMs = nowEpochMs,
        completedAtEpochMs = completedAtEpochMs,
    )

internal inline fun nextPrinterId(nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }): String =
    "printer-${nowProvider()}"
