@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.utils.QuickActions.customerQuickActions
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.customer.CustomerInvoiceHistoryState
import com.erpnext.pos.views.customer.CustomerInvoicesState
import com.erpnext.pos.views.customer.CustomerOutstandingSummary
import com.erpnext.pos.views.customer.CustomerPanelTab
import com.erpnext.pos.views.customer.CustomerPaymentState
import com.erpnext.pos.views.customer.CustomerOutstandingInvoicesContent
import com.erpnext.pos.views.customer.CustomerInvoiceHistoryContent
import com.erpnext.pos.views.customer.InvoicePdfActionOption
import kotlinx.coroutines.flow.Flow

@Composable
internal fun CustomerMobileDetailSheet(
    customer: CustomerBO,
    rightPanelTab: CustomerPanelTab,
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
    onDismiss: () -> Unit,
    onTabChange: (CustomerPanelTab) -> Unit,
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
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = 420.dp, max = 760.dp)
                .padding(bottom = 12.dp)
    ) {
      CustomerRightPanel(
          customer = customer,
          rightPanelTab = rightPanelTab,
          onTabChange = onTabChange,
          paymentState = paymentState,
          invoicesState = invoicesState,
          outstandingInvoicesPagingFlow = outstandingInvoicesPagingFlow,
          historyState = historyState,
          historyInvoicesPagingFlow = historyInvoicesPagingFlow,
          historyMessage = historyMessage,
          returnInfoMessage = returnInfoMessage,
          historyBusy = historyBusy,
          supportedCurrencies = supportedCurrencies,
          cashboxManager = cashboxManager,
          posBaseCurrency = posBaseCurrency,
          returnPolicy = returnPolicy,
          onRegisterPayment = onRegisterPayment,
          onDownloadInvoicePdf = onDownloadInvoicePdf,
          onInvoiceHistoryAction = onInvoiceHistoryAction,
          loadLocalInvoice = loadLocalInvoice,
          onSubmitPartialReturn = onSubmitPartialReturn,
      )
    }
  }
}

@Composable
internal fun CustomerQuickActionsSheet(
    customer: CustomerBO,
    paymentState: CustomerPaymentState,
    onDismiss: () -> Unit,
    onActionSelected: (CustomerQuickActionType) -> Unit,
) {
  val quickActions = remember { customerQuickActions() }

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
          text = customer.customerName,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
      )
      CustomerOutstandingSummary(
          customer = customer,
          invoices = emptyList(),
          posBaseCurrency = paymentState.baseCurrency,
      )
      HorizontalDivider()
      quickActions.forEach { action ->
        ListItem(
            headlineContent = { Text(action.label) },
            leadingContent = { Icon(action.icon, contentDescription = null) },
            modifier = Modifier.clickable { onActionSelected(action.type) },
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
internal fun CustomerOutstandingInvoicesSheet(
    customer: CustomerBO,
    invoicesState: CustomerInvoicesState,
    outstandingInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    paymentState: CustomerPaymentState,
    onDismiss: () -> Unit,
    onRegisterPayment:
        (
            invoiceId: String,
            modeOfPayment: String,
            enteredAmount: Double,
            enteredCurrency: String,
            referenceNumber: String,
        ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
) {
  val outstandingInvoicesPagingItems = outstandingInvoicesPagingFlow.collectAsLazyPagingItems()
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    CustomerOutstandingInvoicesContent(
        customer = customer,
        invoicesState = invoicesState,
        outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
        paymentState = paymentState,
        onRegisterPayment = onRegisterPayment,
        onDownloadInvoicePdf = onDownloadInvoicePdf,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    )
  }
}

@Composable
internal fun CustomerInvoiceHistorySheet(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    historyMessage: String?,
    historyBusy: Boolean,
    paymentState: CustomerPaymentState,
    posBaseCurrency: String,
    returnPolicy: ReturnPolicySettings,
    onAction:
        (
            String,
            InvoiceCancellationAction,
            String?,
            String?,
            String?,
            Boolean,
            Boolean,
        ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    onDismiss: () -> Unit,
    loadLocalInvoice: suspend (String) -> SalesInvoiceWithItemsAndPayments? = { null },
    onSubmitPartialReturn:
        (
            invoiceId: String,
            reason: String?,
            refundModeOfPayment: String?,
            refundReferenceNo: String?,
            applyRefund: Boolean,
            affectInventory: Boolean,
            itemsToReturnByCode: Map<String, Double>,
        ) -> Unit =
        { _, _, _, _, _, _, _ -> },
) {
  val historyInvoicesPagingItems = historyInvoicesPagingFlow.collectAsLazyPagingItems()
  ModalBottomSheet(
      onDismissRequest = onDismiss,
      dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    CustomerInvoiceHistoryContent(
        customer = customer,
        historyState = historyState,
        historyInvoicesPagingItems = historyInvoicesPagingItems,
        historyMessage = historyMessage,
        historyBusy = historyBusy,
        paymentState = paymentState,
        posBaseCurrency = posBaseCurrency,
        returnPolicy = returnPolicy,
        onAction = onAction,
        onDownloadInvoicePdf = onDownloadInvoicePdf,
        loadLocalInvoice = loadLocalInvoice,
        onSubmitPartialReturn = onSubmitPartialReturn,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
    )
  }
}
