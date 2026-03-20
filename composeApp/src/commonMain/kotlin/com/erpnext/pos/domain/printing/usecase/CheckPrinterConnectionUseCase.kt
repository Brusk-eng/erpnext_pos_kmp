package com.erpnext.pos.domain.printing.usecase

import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.domain.printing.ports.PrinterTransportFactory
import com.erpnext.pos.printing.application.PrinterTargetResolver
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers

class CheckPrinterConnectionUseCase(
    private val targetResolver: PrinterTargetResolver,
    private val transportFactory: PrinterTransportFactory,
) : UseCase<PrinterProfile, Result<Unit>>() {
  override suspend fun useCaseFunction(input: PrinterProfile): Result<Unit> {
    return withContext(Dispatchers.Default) {
      runCatching {
        withTimeout(5_000) {
          val resolvedTarget = targetResolver.resolve(input)
          require(transportFactory.supports(resolvedTarget.transportType)) {
            "Transport ${resolvedTarget.transportType} not available on this platform."
          }
          val transport = transportFactory.getTransport(resolvedTarget.transportType)
          transport.connect(resolvedTarget.target).getOrThrow()
          try {
            Unit
          } finally {
            runCatching { transport.disconnect().getOrThrow() }
          }
        }
      }
    }
  }
}
