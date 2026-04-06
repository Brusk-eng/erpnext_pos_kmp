package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.utils.CurrencyService
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.resolveCompanyToTargetAmount
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.customer.CustomerInvoiceHistoryContent
import com.erpnext.pos.views.customer.CustomerInvoiceHistoryState
import com.erpnext.pos.views.customer.CustomerInvoicesState
import com.erpnext.pos.views.customer.CustomerOutstandingInvoicesContent
import com.erpnext.pos.views.customer.CustomerOutstandingSummary
import com.erpnext.pos.views.customer.CustomerPanelTab
import com.erpnext.pos.views.customer.CustomerPaymentState
import com.erpnext.pos.views.customer.EmptyStateMessage
import com.erpnext.pos.views.customer.HeaderChip
import com.erpnext.pos.views.customer.InvoicePdfActionOption
import com.erpnext.pos.views.customer.SummaryStatChip
import com.erpnext.pos.views.customer.isWithinDays
import com.erpnext.pos.views.customer.parsePostingDate
import com.erpnext.pos.views.customer.toBaseAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Composable
internal fun CustomerRightPanel(
    customer: CustomerBO?,
    rightPanelTab: CustomerPanelTab,
    onTabChange: (CustomerPanelTab) -> Unit,
    paymentState: CustomerPaymentState,
    invoicesState: CustomerInvoicesState,
    outstandingInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    historyMessage: String?,
    returnInfoMessage: String?,
    historyBusy: Boolean,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
    posBaseCurrency: String,
    returnPolicy: ReturnPolicySettings,
    onRegisterPayment:
        (
            invoiceId: String,
            modeOfPayment: String,
            enteredAmount: Double,
            enteredCurrency: String,
            referenceNumber: String,
        ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    onInvoiceHistoryAction:
        (
            invoiceId: String,
            action: InvoiceCancellationAction,
            reason: String?,
            refundModeOfPayment: String?,
            refundReferenceNo: String?,
            applyRefund: Boolean,
            affectInventory: Boolean,
        ) -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments?,
    onSubmitPartialReturn:
        (
            invoiceId: String,
            reason: String?,
            refundModeOfPayment: String?,
            refundReferenceNo: String?,
            applyRefund: Boolean,
            affectInventory: Boolean,
            itemsToReturnByCode: Map<String, Double>,
        ) -> Unit,
) {
  val tabs = remember {
    listOf(CustomerPanelTab.Details, CustomerPanelTab.Pending, CustomerPanelTab.History)
  }
  val selectedIndex = tabs.indexOf(rightPanelTab).coerceAtLeast(0)

  Column(modifier = Modifier.fillMaxSize()) {
    CustomerPanelHeader(
        customer = customer,
        cashboxManager = cashboxManager,
        baseCurrency = paymentState.baseCurrency,
    )

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
      tabs.forEach { tab ->
        val enabled = customer != null
        Tab(
            selected = tab == rightPanelTab,
            onClick = { if (enabled) onTabChange(tab) },
            enabled = enabled,
            text = { Text(tab.label) },
        )
      }
    }

    if (rightPanelTab == CustomerPanelTab.Details && !returnInfoMessage.isNullOrBlank()) {
      Surface(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
      ) {
        Text(
            text = returnInfoMessage,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      when (rightPanelTab) {
        CustomerPanelTab.Details -> {
          if (customer != null) {
            CustomerDetailPanel(
                customer = customer,
                paymentState = paymentState,
                historyState = historyState,
                supportedCurrencies = supportedCurrencies,
                cashboxManager = cashboxManager,
            )
          } else {
            EmptyStateMessage(
                message = "Selecciona un cliente",
                icon = Icons.Filled.People,
                modifier = Modifier.fillMaxSize(),
            )
          }
        }

        CustomerPanelTab.Pending -> {
          if (customer != null) {
            val outstandingInvoicesPagingItems =
                outstandingInvoicesPagingFlow.collectAsLazyPagingItems()
            CustomerOutstandingInvoicesContent(
                customer = customer,
                invoicesState = invoicesState,
                outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
                paymentState = paymentState,
                onRegisterPayment = onRegisterPayment,
                onDownloadInvoicePdf = onDownloadInvoicePdf,
                modifier = Modifier.fillMaxSize().padding(20.dp),
            )
          } else {
            EmptyStateMessage(
                message = "Selecciona un cliente",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                modifier = Modifier.fillMaxSize(),
            )
          }
        }

        CustomerPanelTab.History -> {
          if (customer != null) {
            val historyInvoicesPagingItems = historyInvoicesPagingFlow.collectAsLazyPagingItems()
            CustomerInvoiceHistoryContent(
                customer = customer,
                historyState = historyState,
                historyInvoicesPagingItems = historyInvoicesPagingItems,
                historyMessage = historyMessage,
                historyBusy = historyBusy,
                paymentState = paymentState,
                posBaseCurrency = posBaseCurrency,
                returnPolicy = returnPolicy,
                onAction = onInvoiceHistoryAction,
                onDownloadInvoicePdf = onDownloadInvoicePdf,
                loadLocalInvoice = loadLocalInvoice,
                onSubmitPartialReturn = onSubmitPartialReturn,
                modifier = Modifier.fillMaxSize().padding(20.dp),
            )
          } else {
            EmptyStateMessage(
                message = "Selecciona un cliente",
                icon = Icons.Filled.History,
                modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerDetailPanel(
    customer: CustomerBO,
    paymentState: CustomerPaymentState,
    historyState: CustomerInvoiceHistoryState,
    supportedCurrencies: List<String>,
    cashboxManager: CashBoxManager,
) {
  val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
  val pendingAmount =
      bd(customer.totalPendingAmount ?: customer.currentBalance ?: 0.0).moneyScale(2).toDouble(2)
  val displayCurrencies =
      remember(supportedCurrencies, companyCurrency) {
        CurrencyService.resolveDisplayCurrencies(
            supported = supportedCurrencies,
            invoiceCurrency = null,
            receivableCurrency = companyCurrency,
            posCurrency = companyCurrency,
        )
      }

  LaunchedEffect(displayCurrencies, companyCurrency, pendingAmount) {
    val resolved = mutableMapOf<String, Double>()
    displayCurrencies.forEach { currency ->
      val converted =
          CurrencyService.convertFromReceivable(
              amount = pendingAmount,
              receivableCurrency = companyCurrency,
              targetCurrency = currency,
              invoiceCurrency = null,
              conversionRate = null,
              customExchangeRate = null,
              rateResolver = { from, to ->
                cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
              },
          )
      if (converted != null) {
        resolved[currency] = converted
      }
    }
  }

  val historyInvoices = (historyState as? CustomerInvoiceHistoryState.Success)?.invoices.orEmpty()
  val recentInvoices = historyInvoices.filter { isWithinDays(it.postingDate, 90) }
  val totalSpentBase =
      recentInvoices.sumOf {
        val invoiceCurrency = normalizeCurrency(it.currency)
        toBaseAmount(it.total, invoiceCurrency, companyCurrency, it.conversionRate)
      }
  val avgTicket = if (recentInvoices.isNotEmpty()) totalSpentBase / recentInvoices.size else 0.0
  val lastPurchase =
      recentInvoices
          .maxByOrNull { parsePostingDate(it.postingDate) ?: LocalDate(1970, 1, 1) }
          ?.postingDate
  val creditAvailable = customer.availableCredit

  Column(
      modifier = Modifier.fillMaxSize().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      item { SummaryStatChip(label = "Compras 90d", value = recentInvoices.size.toString()) }
      item {
        SummaryStatChip(
            label = "Total 90d",
            value = if (recentInvoices.isNotEmpty()) formatCurrency(companyCurrency, totalSpentBase) else "—",
        )
      }
      item {
        SummaryStatChip(
            label = "Ticket prom.",
            value = if (recentInvoices.isNotEmpty()) formatCurrency(companyCurrency, avgTicket) else "—",
        )
      }
      item { SummaryStatChip(label = "Última compra", value = lastPurchase ?: "—") }
      item {
        SummaryStatChip(
            label = "Crédito disp.",
            value = creditAvailable?.let { formatCurrency(companyCurrency, it) } ?: "—",
        )
      }
    }

    if ((customer.totalPendingAmount ?: customer.currentBalance ?: 0.0) > 0.0) {
      CustomerOutstandingSummary(
          customer = customer,
          invoices = emptyList(),
          posBaseCurrency = companyCurrency,
      )
    }
  }
}

@Composable
private fun CustomerPanelHeader(
    customer: CustomerBO?,
    cashboxManager: CashBoxManager,
    baseCurrency: String,
) {
  val companyCurrency = normalizeCurrency(baseCurrency)
  val pendingCount = customer?.pendingInvoices ?: 0
  val invoiceCurrency = companyCurrency
  val pendingCompanyAmount = customer?.totalPendingAmount ?: customer?.currentBalance ?: 0.0
  val pendingCompany = bd(customer?.totalPendingAmount ?: customer?.currentBalance ?: 0.0).toDouble(2)

  var pendingPos by remember { mutableStateOf<Double?>(null) }
  LaunchedEffect(pendingCompany, companyCurrency, invoiceCurrency) {
    pendingPos =
        if (invoiceCurrency.equals(companyCurrency, ignoreCase = true)) {
          pendingCompany
        } else {
          resolveCompanyToTargetAmount(
              amountCompany = pendingCompany,
              companyCurrency = companyCurrency,
              targetCurrency = invoiceCurrency,
              rateResolver = { from, to ->
                cashboxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
              },
          )
        }
  }

  Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
          Text(
              text = customer?.customerName?.take(1)?.uppercase() ?: "?",
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
              text = customer?.customerName ?: "Clientes",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
          )
          Text(
              text = customer?.mobileNo ?: "Selecciona un cliente para ver detalle",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (customer != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          HeaderChip(
              label = "Pendientes",
              value = pendingCount.toString(),
              isCritical = pendingCount > 0,
          )
          HeaderChip(
              label = invoiceCurrency,
              value = formatCurrency(invoiceCurrency, bd(pendingPos ?: 0.0).toDouble(0)),
              isCritical = pendingCount > 0,
          )
          HeaderChip(
              label = companyCurrency,
              value = formatCurrency(companyCurrency, pendingCompanyAmount),
              isCritical = pendingCount > 0,
          )
        }
      }
    }
  }
}
