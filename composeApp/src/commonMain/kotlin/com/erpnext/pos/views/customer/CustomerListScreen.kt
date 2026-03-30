@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.customer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.CustomerCounts
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.ReturnDestinationPolicy
import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.CurrencyService
import com.erpnext.pos.utils.QuickActions.customerQuickActions
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.resolveCompanyToTargetAmount
import com.erpnext.pos.utils.resolveInvoiceDisplayAmounts
import com.erpnext.pos.utils.resolvePaymentCurrencyForMode
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.AppTextField
import com.erpnext.pos.views.billing.MoneyTextField
import com.erpnext.pos.views.customer.components.FullReturnDialog
import com.erpnext.pos.views.customer.components.NewCustomerDialog
import com.erpnext.pos.views.customer.components.PartialReturnDialog
import com.erpnext.pos.views.customer.components.CustomerHistoryOverviewHeader
import com.erpnext.pos.views.customer.components.InvoiceHistorySummary
import com.erpnext.pos.views.customer.components.InvoiceHistoryRow
import com.erpnext.pos.views.customer.components.CustomerInvoiceHistorySheet
import com.erpnext.pos.views.customer.components.CustomerMobileDetailSheet
import com.erpnext.pos.views.customer.components.OutstandingInvoiceSelection
import com.erpnext.pos.views.customer.components.OutstandingInvoicesList
import com.erpnext.pos.views.customer.components.OutstandingPaymentForm
import com.erpnext.pos.views.customer.components.CustomerOutstandingInvoicesSheet
import com.erpnext.pos.views.customer.components.CustomerQuickActionsSheet
import com.erpnext.pos.views.customer.components.CustomerRightPanel
import com.erpnext.pos.views.customer.components.CustomerFilters
import com.erpnext.pos.views.customer.components.CustomerListPane
import com.erpnext.pos.views.customer.components.buildCustomerHistoryOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    state: CustomerState,
    customersPagingFlow: Flow<PagingData<CustomerBO>>,
    outstandingInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    historyInvoicesPagingFlow: Flow<PagingData<SalesInvoiceBO>>,
    invoicesState: CustomerInvoicesState,
    paymentState: CustomerPaymentState,
    historyState: CustomerInvoiceHistoryState,
    historyMessage: String?,
    returnInfoMessage: String?,
    historyBusy: Boolean,
    customerMessage: String?,
    dialogDataState: CustomerDialogDataState,
    returnPolicy: ReturnPolicySettings,
    actions: CustomerAction,
) {
  val strings = LocalAppStrings.current
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedState by rememberSaveable { mutableStateOf("Todos") }
  var quickActionsCustomer by remember { mutableStateOf<CustomerBO?>(null) }
  var outstandingCustomer by remember { mutableStateOf<CustomerBO?>(null) }
  var historyCustomer by remember { mutableStateOf<CustomerBO?>(null) }
  var selectedCustomer by remember { mutableStateOf<CustomerBO?>(null) }
  var rightPanelTab by rememberSaveable { mutableStateOf(CustomerPanelTab.Details) }
  var allowSheets by remember { mutableStateOf(true) }
  var showNewCustomerDialog by rememberSaveable { mutableStateOf(false) }

  val snackbar: SnackbarController = koinInject()
  val cashboxManager: CashBoxManager = koinInject()
  val posContext = cashboxManager.getContext()
  val posCurrency = normalizeCurrency(posContext?.currency)
  val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
  val supportedCurrencies =
      remember(posContext?.allowedCurrencies, posContext?.currency, paymentState.baseCurrency) {
        val fromModes = posContext?.allowedCurrencies?.map { it.code }.orEmpty()
        val merged =
            (fromModes + listOfNotNull(posContext?.currency, paymentState.baseCurrency))
                .map { normalizeCurrency(it) }
                .distinct()
        merged.ifEmpty { listOf(posCurrency) }
      }
  val customersPagingItems = customersPagingFlow.collectAsLazyPagingItems()
  var baseCounts by remember { mutableStateOf(CustomerCounts(0, 0)) }
  val isDesktop = getPlatformName() == "Desktop"
  val customerListState = rememberLazyListState()
  val showBackToTop by remember { derivedStateOf { customerListState.firstVisibleItemIndex > 0 } }

  val hasCustomersLoaded =
      customersPagingItems.itemCount > 0 ||
          customersPagingItems.loadState.refresh is LoadState.Loading
  val filterElevation by
      animateDpAsState(
          targetValue = if (hasCustomersLoaded) 4.dp else 0.dp,
          label = "filterElevation",
      )

  LaunchedEffect(outstandingCustomer?.name) {
    outstandingCustomer?.let { customer -> actions.loadOutstandingInvoices(customer) }
  }
  LaunchedEffect(state, searchQuery, selectedState) {
    if (searchQuery.isEmpty() && selectedState == "Todos" && state is CustomerState.Success) {
      baseCounts = CustomerCounts(total = state.totalCount, pending = state.pendingCount)
    }
  }

  LaunchedEffect(paymentState.successMessage) {
    paymentState.successMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
          snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
          actions.clearPaymentMessages()
        }
  }

  LaunchedEffect(paymentState.errorMessage) {
    paymentState.errorMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
          snackbar.show(message, SnackbarType.Error, position = SnackbarPosition.Top)
          actions.clearPaymentMessages()
        }
  }

  LaunchedEffect(historyCustomer?.name) {
    historyCustomer?.let { actions.onViewInvoiceHistory(it) }
  }

  LaunchedEffect(historyMessage) {
    historyMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
          snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
          actions.clearInvoiceHistoryMessages()
        }
  }

  LaunchedEffect(customerMessage) {
    customerMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
          snackbar.show(message, SnackbarType.Success, position = SnackbarPosition.Top)
          actions.clearCustomerMessages()
        }
  }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(onClick = { showNewCustomerDialog = true }) {
          Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo cliente")
        }
      }
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val isWideLayout = maxWidth >= 840.dp || isDesktop
      val contentPadding = if (isWideLayout) 24.dp else 16.dp
      SideEffect { allowSheets = !isWideLayout }

      LaunchedEffect(selectedCustomer?.name, rightPanelTab) {
        val customer = selectedCustomer ?: return@LaunchedEffect
        when (rightPanelTab) {
          CustomerPanelTab.Pending -> actions.loadOutstandingInvoices(customer)
          CustomerPanelTab.History -> actions.onViewInvoiceHistory(customer)
          else -> Unit
        }
      }

      Column(modifier = Modifier.fillMaxSize()) {
        // Filtros y búsqueda
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = filterElevation,
            shadowElevation = filterElevation,
        ) {
          CustomerFilters(
              searchQuery = searchQuery,
              selectedState = selectedState,
              isWideLayout = isWideLayout,
              totalCount = baseCounts.total,
              pendingCount = baseCounts.pending,
              onQueryChange = {
                searchQuery = it
                actions.onSearchQueryChanged(it)
              },
              onStateChange = {
                selectedState = it ?: "Todos"
                actions.onStateSelected(it ?: "Todos")
              },
              modifier = Modifier.padding(horizontal = contentPadding, vertical = 8.dp),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Contenido principal según estado
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
          when (state) {
            is CustomerState.Loading -> {
              CustomerShimmerList()
            }

            is CustomerState.Empty -> {
              EmptyStateMessage(
                  message = strings.customer.emptyCustomers,
                  icon = Icons.Filled.People,
              )
            }

            is CustomerState.Error -> {
              FullScreenErrorMessage(errorMessage = state.message, onRetry = actions.fetchAll)
            }

            is CustomerState.Success -> {
              val refreshState = customersPagingItems.loadState.refresh
              val appendState = customersPagingItems.loadState.append
              val isLoading = refreshState is LoadState.Loading
              val isEmpty = customersPagingItems.itemCount == 0 && !isLoading
              val hasError = refreshState is LoadState.Error || appendState is LoadState.Error

              if (hasError) {
                val errorMessage =
                    (refreshState as? LoadState.Error)?.error?.message
                        ?: (appendState as? LoadState.Error)?.error?.message
                        ?: "Error al cargar clientes"
                FullScreenErrorMessage(
                    errorMessage = errorMessage,
                    onRetry = {
                      customersPagingItems.refresh()
                      actions.fetchAll()
                    },
                )
              } else if (isLoading && customersPagingItems.itemCount == 0) {
                CustomerShimmerList()
              } else if (isEmpty) {
                EmptyStateMessage(
                    message =
                        if (searchQuery.isEmpty()) strings.customer.emptyCustomers
                        else strings.customer.emptySearchCustomers,
                    icon = Icons.Filled.People,
                )
              } else {
                LaunchedEffect(customersPagingItems.itemCount, isWideLayout) {
                  if (isWideLayout) {
                    val loaded = customersPagingItems.itemSnapshotList.items
                    if (
                        selectedCustomer == null ||
                            loaded.none { it.name == selectedCustomer?.name }
                    ) {
                      selectedCustomer = loaded.firstOrNull()
                    }
                  }
                }
                if (isWideLayout) {
                  Row(
                      modifier = Modifier.fillMaxSize(),
                      horizontalArrangement = Arrangement.spacedBy(16.dp),
                  ) {
                    Box(modifier = Modifier.weight(0.65f)) {
                      CustomerListPane(
                          customers = customersPagingItems,
                          posCurrency = posCurrency,
                          companyCurrency = companyCurrency,
                          cashboxManager = cashboxManager,
                          listState = customerListState,
                          showBackToTop = showBackToTop,
                          isWideLayout = false,
                          isDesktop = isDesktop,
                          onOpenQuickActions = {
                            selectedCustomer = it
                            rightPanelTab = CustomerPanelTab.Details
                          },
                          onSelect = { selectedCustomer = it },
                          onQuickAction = { customer, actionType ->
                            when (actionType) {
                              CustomerQuickActionType.PendingInvoices,
                              CustomerQuickActionType.RegisterPayment -> {
                                selectedCustomer = customer
                                rightPanelTab = CustomerPanelTab.Pending
                              }

                              CustomerQuickActionType.InvoiceHistory -> {
                                selectedCustomer = customer
                                rightPanelTab = CustomerPanelTab.History
                              }

                              else -> handleQuickAction(actions, customer, actionType)
                            }
                          },
                      )
                    }
                    Surface(
                        modifier = Modifier.weight(1.35f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 1.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                      CustomerRightPanel(
                          customer = selectedCustomer,
                          rightPanelTab = rightPanelTab,
                          onTabChange = { rightPanelTab = it },
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
                          posBaseCurrency = companyCurrency,
                          returnPolicy = returnPolicy,
                          onRegisterPayment = {
                              invoiceId,
                              mode,
                              enteredAmount,
                              enteredCurrency,
                              referenceNumber ->
                            actions.registerPayment(
                                selectedCustomer?.name.orEmpty(),
                                invoiceId,
                                mode,
                                enteredAmount,
                                enteredCurrency,
                                referenceNumber,
                            )
                          },
                          onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
                          onInvoiceHistoryAction = {
                              invoiceId,
                              action,
                              reason,
                              refundMode,
                              refundReference,
                              applyRefund,
                              affectInventory ->
                            actions.onInvoiceHistoryAction(
                                invoiceId,
                                action,
                                reason,
                                refundMode,
                                refundReference,
                                applyRefund,
                                affectInventory,
                            )
                          },
                          loadLocalInvoice = actions.loadInvoiceLocal,
                          onSubmitPartialReturn = actions.onInvoicePartialReturn,
                      )
                    }
                  }
                } else {
                  CustomerListPane(
                      customers = customersPagingItems,
                      posCurrency = posCurrency,
                      companyCurrency = companyCurrency,
                      cashboxManager = cashboxManager,
                      listState = customerListState,
                      showBackToTop = showBackToTop,
                      isWideLayout = isWideLayout,
                      isDesktop = isDesktop,
                      onOpenQuickActions = { customer ->
                        selectedCustomer = customer
                        rightPanelTab = CustomerPanelTab.Details
                      },
                      onSelect = { customer ->
                        selectedCustomer = customer
                        rightPanelTab = CustomerPanelTab.Details
                      },
                      onQuickAction = { customer, actionType ->
                        selectedCustomer = customer
                        when (actionType) {
                          CustomerQuickActionType.PendingInvoices,
                          CustomerQuickActionType.RegisterPayment -> {
                            rightPanelTab = CustomerPanelTab.Pending
                          }

                          CustomerQuickActionType.InvoiceHistory -> {
                            rightPanelTab = CustomerPanelTab.History
                          }

                          else -> handleQuickAction(actions, customer, actionType)
                        }
                      },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (allowSheets) {
    selectedCustomer?.let { customer ->
      CustomerMobileDetailSheet(
          customer = customer,
          rightPanelTab = rightPanelTab,
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
          posBaseCurrency = companyCurrency,
          returnPolicy = returnPolicy,
          onDismiss = {
            selectedCustomer = null
            rightPanelTab = CustomerPanelTab.Details
            actions.clearOutstandingInvoices()
            actions.clearInvoiceHistory()
          },
          onTabChange = { rightPanelTab = it },
          onRegisterPayment = { invoiceId, mode, enteredAmount, enteredCurrency, referenceNumber ->
            actions.registerPayment(
                customer.name,
                invoiceId,
                mode,
                enteredAmount,
                enteredCurrency,
                referenceNumber,
            )
          },
          onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
          onInvoiceHistoryAction = {
              invoiceId,
              action,
              reason,
              refundMode,
              refundReference,
              applyRefund,
              affectInventory ->
            actions.onInvoiceHistoryAction(
                invoiceId,
                action,
                reason,
                refundMode,
                refundReference,
                applyRefund,
                affectInventory,
            )
          },
          loadLocalInvoice = actions.loadInvoiceLocal,
          onSubmitPartialReturn = actions.onInvoicePartialReturn,
      )
    }

    quickActionsCustomer?.let { customer ->
      CustomerQuickActionsSheet(
          customer = customer,
          paymentState = paymentState,
          onDismiss = { quickActionsCustomer = null },
          onActionSelected = { actionType ->
            when (actionType) {
              CustomerQuickActionType.PendingInvoices,
              CustomerQuickActionType.RegisterPayment -> {
                outstandingCustomer = customer
              }

              CustomerQuickActionType.InvoiceHistory -> {
                historyCustomer = customer
              }

              else -> handleQuickAction(actions, customer, actionType)
            }
          },
      )
    }

    outstandingCustomer?.let { customer ->
      CustomerOutstandingInvoicesSheet(
          customer = customer,
          invoicesState = invoicesState,
          outstandingInvoicesPagingFlow = outstandingInvoicesPagingFlow,
          paymentState = paymentState,
          onDismiss = {
            outstandingCustomer = null
            actions.clearOutstandingInvoices()
          },
          onRegisterPayment = { invoiceId, mode, enteredAmount, enteredCurrency, referenceNumber ->
            actions.registerPayment(
                customer.name,
                invoiceId,
                mode,
                enteredAmount,
                enteredCurrency,
                referenceNumber,
            )
          },
          onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
      )
    }

    historyCustomer?.let { customer ->
      CustomerInvoiceHistorySheet(
          customer = customer,
          historyState = historyState,
          historyInvoicesPagingFlow = historyInvoicesPagingFlow,
          historyMessage = historyMessage,
          historyBusy = historyBusy,
          paymentState = paymentState,
          posBaseCurrency = companyCurrency,
          returnPolicy = returnPolicy,
          onAction = {
              invoiceId,
              action,
              reason,
              refundMode,
              refundReference,
              applyRefund,
              affectInventory ->
            actions.onInvoiceHistoryAction(
                invoiceId,
                action,
                reason,
                refundMode,
                refundReference,
                applyRefund,
                affectInventory,
            )
          },
          onDownloadInvoicePdf = actions.onDownloadInvoicePdf,
          onDismiss = {
            historyCustomer = null
            actions.clearInvoiceHistory()
          },
          loadLocalInvoice = actions.loadInvoiceLocal,
          onSubmitPartialReturn = actions.onInvoicePartialReturn,
      )
    }
  }

  if (showNewCustomerDialog) {
    NewCustomerDialog(
        onDismiss = { showNewCustomerDialog = false },
        onSubmit = { input -> actions.onCreateCustomer(input) },
        customerGroups = dialogDataState.customerGroups,
        territories = dialogDataState.territories,
        paymentTermsOptions = dialogDataState.paymentTerms,
        companies = dialogDataState.companies,
    )
  }
}

internal enum class CustomerPanelTab(val label: String) {
  Details("Resumen"),
  Pending("Pendientes"),
  History("Historial"),
}

@Composable
fun CustomerShimmerList() {
  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    repeat(6) {
      Box(
          modifier =
              Modifier.fillMaxWidth().height(96.dp).shimmerBackground(RoundedCornerShape(16.dp))
      )
    }
  }
}

@Composable
internal fun CustomerOutstandingInvoicesContent(
    customer: CustomerBO,
    invoicesState: CustomerInvoicesState,
    outstandingInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
    paymentState: CustomerPaymentState,
    onRegisterPayment:
        (
            invoiceId: String,
            modeOfPayment: String,
            enteredAmount: Double,
            enteredCurrency: String,
            referenceNumber: String,
        ) -> Unit,
    onDownloadInvoicePdf: (String, InvoicePdfActionOption) -> Unit,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val cashboxManager: CashBoxManager = koinInject()
  val scrollState = rememberScrollState()
  val windowSizeClass = rememberWindowSizeClass()
  val invoiceListMaxHeight =
      if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) 220.dp else 360.dp
  var selectedInvoice by remember { mutableStateOf<SalesInvoiceBO?>(null) }
  var amountRaw by remember { mutableStateOf("") }
  var amountValue by remember { mutableStateOf(0.0) }
  var lastAutoAmount by remember { mutableStateOf<String?>(null) }
  val companyCurrency = normalizeCurrency(paymentState.baseCurrency)
  val invoiceCurrency = normalizeCurrency(selectedInvoice?.currency)
  val paymentModes = paymentState.paymentModes
  val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
  val defaultMode = paymentModes.firstOrNull()?.modeOfPayment.orEmpty()
  var selectedMode by remember(modeOptions, defaultMode) { mutableStateOf(defaultMode) }
  val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
  val requiresReference = remember(selectedModeOption) { requiresReference(selectedModeOption) }
  var referenceInput by remember { mutableStateOf("") }

  LaunchedEffect(selectedMode) { referenceInput = "" }

  val preferredCurrencyByMode = paymentState.paymentModeCurrencyByMode ?: mapOf()
  var selectedCurrency by remember { mutableStateOf(invoiceCurrency.ifBlank { companyCurrency }) }
  LaunchedEffect(selectedMode, invoiceCurrency, companyCurrency) {
    val resolved =
        resolvePaymentCurrencyForMode(
            modeOfPayment = selectedMode,
            paymentModeDetails = paymentState.modeTypes ?: mapOf(),
            preferredCurrency = preferredCurrencyByMode[selectedMode],
            invoiceCurrency = invoiceCurrency,
        )
    selectedCurrency = resolved.ifBlank { companyCurrency }
  }

  var modeExpanded by remember { mutableStateOf(false) }

  val cachedRates =
      (invoicesState as? CustomerInvoicesState.Success)?.exchangeRateByCurrency.orEmpty()
  var rateBaseToPos by remember { mutableStateOf<Double?>(null) }
  LaunchedEffect(selectedCurrency, companyCurrency, cachedRates, selectedInvoice?.conversionRate) {
    rateBaseToPos =
        resolveBaseToSelectedRate(
            cashboxManager = cashboxManager,
            companyCurrency = companyCurrency,
            selectedCurrency = selectedCurrency,
            invoiceCurrency = invoiceCurrency,
            invoiceConversionRate = selectedInvoice?.conversionRate,
            cachedRates = cachedRates,
        )
  }

  val invoiceConversionRate = selectedInvoice?.conversionRate?.takeIf { it > 0.0 }
  val receivableCurrency =
      normalizeCurrency(selectedInvoice?.partyAccountCurrency ?: companyCurrency)
  val outstandingRc = selectedInvoice?.outstandingAmount ?: 0.0
  val baseToPosRate = rateBaseToPos
  val outstandingInSelectedCurrency =
      when {
        selectedCurrency.equals(receivableCurrency, ignoreCase = true) -> outstandingRc
        selectedCurrency.equals(invoiceCurrency, ignoreCase = true) -> {
          CurrencyService.amountReceivableToInvoice(outstandingRc, invoiceConversionRate)
        }

        receivableCurrency.equals(companyCurrency, ignoreCase = true) && baseToPosRate != null ->
            outstandingRc * baseToPosRate

        else -> {
          cachedRates[selectedCurrency]?.let { outstandingRc * it }
        }
      }
  val conversionError = outstandingInSelectedCurrency == null
  LaunchedEffect(selectedInvoice?.invoiceId, selectedCurrency, outstandingInSelectedCurrency) {
    val resolvedOutstanding = outstandingInSelectedCurrency ?: return@LaunchedEffect
    if (amountRaw.isBlank() || amountRaw == lastAutoAmount) {
      val formatted = formatAmountRawForCurrency(resolvedOutstanding)
      amountRaw = formatted
    }
  }
  val changeDue =
      outstandingInSelectedCurrency?.let { (amountValue - it).coerceAtLeast(0.0) } ?: 0.0
  val amountToApply = outstandingInSelectedCurrency?.let { minOf(amountValue, it) } ?: amountValue
  val amountInCompanyCurrency =
      when {
        selectedCurrency.equals(companyCurrency, ignoreCase = true) -> amountValue
        baseToPosRate != null && baseToPosRate > 0.0 -> amountValue / baseToPosRate
        else -> null
      }
  val isSubmitEnabled =
      !paymentState.isSubmitting &&
          selectedInvoice?.invoiceId?.isNotBlank() == true &&
          selectedMode.isNotBlank() &&
          amountValue > 0.0 &&
          !conversionError

  fun applyInvoiceSelection(invoice: SalesInvoiceBO, selection: OutstandingInvoiceSelection) {
    selectedInvoice = invoice
    val amountToUse =
        if (selectedCurrency.equals(selection.companyCurrency, ignoreCase = true)) {
          selection.outstandingCompany
        } else {
          selection.outstandingInvoice
        }
    val formatted = formatAmountRawForCurrency(amountToUse)
    amountRaw = formatted
    lastAutoAmount = formatted
  }

  Column(
      modifier = modifier.verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
        text = strings.customer.outstandingInvoicesTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )

    // ====== LISTADO DE FACTURAS ======
    OutstandingInvoicesList(
        invoicesState = invoicesState,
        outstandingInvoicesPagingItems = outstandingInvoicesPagingItems,
        companyCurrency = companyCurrency,
        invoiceListMaxHeight = invoiceListMaxHeight,
        selectedInvoiceId = selectedInvoice?.invoiceId,
        onInvoiceSelected = ::applyInvoiceSelection,
        onDownloadInvoicePdf = onDownloadInvoicePdf,
    )

    HorizontalDivider()

    // ====== REGISTRAR PAGO ======
    OutstandingPaymentForm(
        paymentState = paymentState,
        selectedInvoiceId = selectedInvoice?.invoiceId,
        selectedMode = selectedMode,
        paymentModes = paymentModes,
        selectedCurrency = selectedCurrency,
        requiresReference = requiresReference,
        referenceInput = referenceInput,
        onReferenceInputChange = { referenceInput = it },
        amountRaw = amountRaw,
        onAmountRawChange = {
          amountRaw = it
          lastAutoAmount = null
        },
        onAmountChanged = { amountValue = it },
        conversionError = conversionError,
        companyCurrency = companyCurrency,
        amountInCompanyCurrency = amountInCompanyCurrency,
        changeDue = changeDue,
        isSubmitEnabled = isSubmitEnabled,
        onModeSelected = { mode ->
          selectedMode = mode.name
          selectedCurrency =
              resolvePaymentCurrencyForMode(
                  modeOfPayment = mode.modeOfPayment,
                  paymentModeDetails = paymentState.modeTypes ?: mapOf(),
                  preferredCurrency = preferredCurrencyByMode[mode.name],
                  invoiceCurrency = invoiceCurrency,
              )
        },
        onSubmit = {
          val invoiceId = selectedInvoice?.invoiceId?.trim().orEmpty()
          onRegisterPayment(
              invoiceId,
              selectedMode,
              amountToApply,
              selectedCurrency,
              referenceInput,
          )
        },
    )
  }

  Spacer(modifier = Modifier.height(12.dp))
}

@Composable
internal fun CustomerInvoiceHistoryContent(
    customer: CustomerBO,
    historyState: CustomerInvoiceHistoryState,
    historyInvoicesPagingItems: androidx.paging.compose.LazyPagingItems<SalesInvoiceBO>,
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
        { _, _, _, _, _, _, _ ->
        },
    modifier: Modifier = Modifier,
    showDialogs: Boolean = true,
) {
  val scope = rememberCoroutineScope()
  var selectedRangeDays by remember { mutableStateOf(30) }
  var partialReturnState by remember {
    mutableStateOf(partialReturnState(returnPolicy))
  }
  var fullReturnState by remember {
    mutableStateOf(fullReturnState(returnPolicy))
  }

  val refundOptions =
      remember(paymentState.paymentModes) {
        paymentState.paymentModes
            .filter { it.allowInReturns }
            .mapNotNull { it.modeOfPayment.ifBlank { null } }
            .distinct()
      }

  fun canConfirmReturn(): Boolean = partialReturnState.qtyByItemCode.values.any { it > 0.0 }

  fun openPartialReturn(invoiceId: String) {
    partialReturnState = partialReturnState(returnPolicy, invoiceId = invoiceId, isOpen = true)

    scope.launch {
      partialReturnState = partialReturnState.copy(isLoading = true)
      try {
        val local = loadLocalInvoice(invoiceId)
        if (local == null) {
          partialReturnState =
              partialReturnState.copy(
                  isLoading = false,
                  errorMessage = "No se encontró la factura localmente.",
              )
        } else {
          partialReturnState =
              partialReturnState.copy(
                  invoiceLocal = local,
                  qtyByItemCode = local.items.map { it.itemCode }.distinct().associateWith { 0.0 },
                  refundMode = refundOptions.firstOrNull(),
                  isPhysicalReturn = defaultReturnIsPhysical(isPosInvoice = local.invoice.isPos),
                  isLoading = false,
              )
        }
      } catch (e: Exception) {
        partialReturnState =
            partialReturnState.copy(
                isLoading = false,
                errorMessage = e.message ?: "No se pudo cargar la factura.",
            )
      }
    }
  }

  fun openFullReturn(invoiceId: String) {
    val invoice =
        (historyState as? CustomerInvoiceHistoryState.Success)
            ?.invoices
            ?.firstOrNull { it.invoiceId == invoiceId }
    fullReturnState =
        fullReturnState(
            returnPolicy = returnPolicy,
            invoiceId = invoiceId,
            invoice = invoice,
            isOpen = true,
        )
    scope.launch {
      fullReturnState = fullReturnState.copy(isLoading = true)
      try {
        val local = loadLocalInvoice(invoiceId)
        if (local == null) {
          fullReturnState =
              fullReturnState.copy(
                  isLoading = false,
                  errorMessage = "No se encontró la factura localmente.",
              )
        } else {
          fullReturnState =
              fullReturnState.copy(
                  invoiceLocal = local,
                  isPhysicalReturn = defaultReturnIsPhysical(isPosInvoice = local.invoice.isPos),
                  isLoading = false,
              )
        }
      } catch (e: Exception) {
        fullReturnState =
            fullReturnState.copy(
                isLoading = false,
                errorMessage = e.message ?: "No se pudo cargar la factura.",
            )
      }
    }
  }

  fun closeFullReturnDialog() {
    fullReturnState = fullReturnState(returnPolicy)
  }

  fun closeReturnDialog() {
    partialReturnState = partialReturnState(returnPolicy)
  }

  LaunchedEffect(partialReturnState.isOpen, returnPolicy, partialReturnState.destinationTouched) {
    if (partialReturnState.isOpen && !partialReturnState.destinationTouched) {
      partialReturnState =
          partialReturnState.copy(destination = defaultReturnDestination(returnPolicy))
    }
  }

  LaunchedEffect(fullReturnState.isOpen, returnPolicy, fullReturnState.destinationTouched) {
    if (fullReturnState.isOpen && !fullReturnState.destinationTouched) {
      fullReturnState = fullReturnState.copy(destination = defaultReturnDestination(returnPolicy))
    }
  }

  if (showDialogs && partialReturnState.isOpen && partialReturnState.invoiceId != null) {
    PartialReturnDialog(
        invoiceId = partialReturnState.invoiceId!!,
        invoiceLocal = partialReturnState.invoiceLocal,
        returnLoading = partialReturnState.isLoading,
        returnError = partialReturnState.errorMessage,
        historyBusy = historyBusy,
        refundOptions = refundOptions,
        returnPolicy = returnPolicy,
        paymentState = paymentState,
        refundMode = partialReturnState.refundMode,
        onRefundModeChange = { partialReturnState = partialReturnState.copy(refundMode = it) },
        refundReference = partialReturnState.refundReference,
        onRefundReferenceChange = {
          partialReturnState = partialReturnState.copy(refundReference = it)
        },
        returnReason = partialReturnState.reason,
        onReturnReasonChange = { partialReturnState = partialReturnState.copy(reason = it) },
        qtyByItemCode = partialReturnState.qtyByItemCode,
        onQtyByItemCodeChange = { partialReturnState = partialReturnState.copy(qtyByItemCode = it) },
        returnDestination = partialReturnState.destination,
        onReturnDestinationChange = {
          partialReturnState =
              partialReturnState.copy(destination = it, destinationTouched = true)
        },
        isPhysicalReturn = partialReturnState.isPhysicalReturn,
        onIsPhysicalReturnChange = {
          partialReturnState = partialReturnState.copy(isPhysicalReturn = it)
        },
        onDismiss = { if (!historyBusy) closeReturnDialog() },
        onConfirm = {
          if (!historyBusy) {
            val resolvedReason =
                if (returnPolicy.requireReason) partialReturnState.reason.takeIf { it.isNotBlank() }
                else null
            onSubmitPartialReturn(
                partialReturnState.invoiceId!!,
                resolvedReason,
                if (it.applyRefund) partialReturnState.refundMode else null,
                if (it.applyRefund) partialReturnState.refundReference else null,
                it.applyRefund,
                partialReturnState.isPhysicalReturn,
                partialReturnState.qtyByItemCode.filterValues { qty -> qty > 0.0 },
            )
            closeReturnDialog()
          }
        },
    )
  }

  if (showDialogs && fullReturnState.isOpen && fullReturnState.invoiceId != null) {
    FullReturnDialog(
        invoiceId = fullReturnState.invoiceId!!,
        invoice = fullReturnState.invoice,
        invoiceLocal = fullReturnState.invoiceLocal,
        fullReturnLoading = fullReturnState.isLoading,
        fullReturnError = fullReturnState.errorMessage,
        historyBusy = historyBusy,
        refundOptions = refundOptions,
        returnPolicy = returnPolicy,
        paymentState = paymentState,
        refundMode = fullReturnState.refundMode,
        onRefundModeChange = { fullReturnState = fullReturnState.copy(refundMode = it) },
        refundReference = fullReturnState.refundReference,
        onRefundReferenceChange = {
          fullReturnState = fullReturnState.copy(refundReference = it)
        },
        returnReason = fullReturnState.reason,
        onReturnReasonChange = { fullReturnState = fullReturnState.copy(reason = it) },
        returnDestination = fullReturnState.destination,
        onReturnDestinationChange = {
          fullReturnState = fullReturnState.copy(destination = it, destinationTouched = true)
        },
        isPhysicalReturn = fullReturnState.isPhysicalReturn,
        onIsPhysicalReturnChange = {
          fullReturnState = fullReturnState.copy(isPhysicalReturn = it)
        },
        onDismiss = { if (!historyBusy) closeFullReturnDialog() },
        onConfirm = {
          if (!historyBusy) {
            val resolvedReason =
                if (returnPolicy.requireReason) fullReturnState.reason.takeIf { it.isNotBlank() }
                else null
            onAction(
                fullReturnState.invoiceId!!,
                InvoiceCancellationAction.RETURN,
                resolvedReason,
                fullReturnState.refundMode?.takeIf { it.isNotBlank() },
                fullReturnState.refundReference.takeIf { it.isNotBlank() },
                it.applyRefund,
                fullReturnState.isPhysicalReturn,
            )
            closeFullReturnDialog()
          }
        },
    )
  }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
    val overview =
        remember(
            historyState,
            historyInvoicesPagingItems.itemSnapshotList.items,
            selectedRangeDays,
        ) {
          buildCustomerHistoryOverview(
              historyState = historyState,
              snapshotInvoices = historyInvoicesPagingItems.itemSnapshotList.items,
              selectedRangeDays = selectedRangeDays,
          )
        }
    CustomerHistoryOverviewHeader(
        overview = overview,
        selectedRangeDays = selectedRangeDays,
        historyMessage = historyMessage,
        onRangeSelected = { selectedRangeDays = it },
    )
    when (historyState) {
      CustomerInvoiceHistoryState.Idle -> {
        Text("Abre la vista para cargar las facturas de los últimos 90 días.")
      }

      CustomerInvoiceHistoryState.Loading -> {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(40.dp))
          Spacer(Modifier.height(8.dp))
          Text("Cargando historial...")
        }
      }

      is CustomerInvoiceHistoryState.Error -> {
        Text(
            historyState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
      }

      is CustomerInvoiceHistoryState.Success -> {
        val refreshState = historyInvoicesPagingItems.loadState.refresh
        val appendState = historyInvoicesPagingItems.loadState.append
        val isLoading = refreshState is LoadState.Loading
        val hasError = refreshState is LoadState.Error || appendState is LoadState.Error
        val invoices = overview.filteredInvoices
        if (hasError) {
          Text(
              (refreshState as? LoadState.Error)?.error?.message
                  ?: (appendState as? LoadState.Error)?.error?.message
                  ?: "No se pudo cargar el historial de facturas.",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
          )
        } else if (isLoading && historyInvoicesPagingItems.itemCount == 0) {
          Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Cargando historial...")
          }
        } else if (historyInvoicesPagingItems.itemCount == 0 || invoices.isEmpty()) {
          Text("No se encontraron facturas en los últimos $selectedRangeDays días.")
        } else {
          InvoiceHistorySummary(
              invoices = invoices,
              posBaseCurrency = posBaseCurrency,
          )
          LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(bottom = 16.dp),
          ) {
            items(
                count = historyInvoicesPagingItems.itemCount,
                key = { index -> historyInvoicesPagingItems[index]?.invoiceId ?: "history_$index" },
            ) { index ->
              val invoice = historyInvoicesPagingItems[index] ?: return@items
              if (!isWithinDays(invoice.postingDate, selectedRangeDays)) return@items
              InvoiceHistoryRow(
                  invoice = invoice,
                  isBusy = historyBusy,
                  posBaseCurrency = posBaseCurrency,
                  returnPolicy = returnPolicy,
                  onCancel = { invoiceId ->
                    onAction(
                        invoiceId,
                        InvoiceCancellationAction.CANCEL,
                        null,
                        null,
                        null,
                        false,
                        false,
                    )
                  },
                  onReturnTotal = { invoiceId -> openFullReturn(invoiceId) },
                  onPartialReturn = { invoiceId -> openPartialReturn(invoiceId) },
                  onDownloadPdf = onDownloadInvoicePdf,
              )
            }
            if (historyInvoicesPagingItems.loadState.append is LoadState.Loading) {
              item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                  CircularProgressIndicator()
                }
              }
            }
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
  }
}

@Composable
internal fun SummaryStatChip(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      shape = RoundedCornerShape(12.dp),
  ) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          text = value,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
internal fun HeaderChip(
    label: String,
    value: String,
    isCritical: Boolean,
    modifier: Modifier = Modifier,
) {
  val background =
      if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      }
  val textColor =
      if (isCritical) {
        MaterialTheme.colorScheme.error
      } else {
        MaterialTheme.colorScheme.onSurfaceVariant
      }
  Surface(modifier = modifier, color = background, shape = RoundedCornerShape(999.dp)) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = textColor)
      Text(
          value,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = textColor,
      )
    }
  }
}


fun requiresReference(option: POSPaymentModeOption?): Boolean {
  val type = option?.type?.trim().orEmpty()
  return type.equals("Bank", ignoreCase = true) ||
      type.equals("Card", ignoreCase = true) ||
      option?.modeOfPayment?.contains("bank", ignoreCase = true) == true ||
      option?.modeOfPayment?.contains("card", ignoreCase = true) == true
}

internal fun toBaseAmount(
    amount: Double,
    invoiceCurrency: String,
    baseCurrency: String,
    conversionRate: Double?,
): Double {
  if (invoiceCurrency.equals(baseCurrency, ignoreCase = true)) return amount
  return if (conversionRate != null && conversionRate > 0.0) amount * conversionRate else amount
}

private fun isPaidStatus(status: String?): Boolean {
  return status?.trim()?.equals("paid", ignoreCase = true) == true
}

private fun formatAmountRawForCurrency(amount: Double): String {
  val rounded = roundToCurrency(amount)
  val decimals = 2
  return formatDoubleToString(rounded, decimals)
}

private suspend fun resolveBaseToSelectedRate(
    cashboxManager: CashBoxManager,
    companyCurrency: String,
    selectedCurrency: String,
    invoiceCurrency: String,
    invoiceConversionRate: Double?,
    cachedRates: Map<String, Double>,
): Double? {
  if (selectedCurrency.isBlank() || selectedCurrency.equals(companyCurrency, ignoreCase = true)) {
    return 1.0
  }
  cachedRates[selectedCurrency]
      ?.takeIf { it > 0.0 }
      ?.let {
        return it
      }
  invoiceConversionRate
      ?.takeIf { it > 0.0 }
      ?.let { rate ->
        if (selectedCurrency.equals(invoiceCurrency, ignoreCase = true)) {
          return 1.0 / rate
        }
      }

  val direct =
      cashboxManager.resolveExchangeRateBetween(
          fromCurrency = companyCurrency,
          toCurrency = selectedCurrency,
          allowNetwork = false,
      )
  if (direct != null && direct > 0.0) return direct

  val reverse =
      cashboxManager
          .resolveExchangeRateBetween(
              fromCurrency = selectedCurrency,
              toCurrency = companyCurrency,
              allowNetwork = false,
          )
          ?.takeIf { it > 0.0 }
          ?.let { 1.0 / it }
  if (reverse != null && reverse > 0.0) return reverse

  val ctx = cashboxManager.getContext()
  val ctxCurrency = normalizeCurrency(ctx?.currency)
  val ctxRate = ctx?.exchangeRate ?: 0.0
  if (ctxRate > 0.0) {
    if (companyCurrency.equals(ctxCurrency, true) && selectedCurrency.equals("USD", true)) {
      return 1.0 / ctxRate
    }
    if (selectedCurrency.equals(ctxCurrency, true) && companyCurrency.equals("USD", true)) {
      return ctxRate
    }
  }
  return null
}

internal fun defaultReturnDestination(policy: ReturnPolicySettings): ReturnDestination {
  return if (policy.allowRefunds && policy.defaultDestination == ReturnDestinationPolicy.REFUND) {
    ReturnDestination.RETURN
  } else {
    ReturnDestination.CREDIT
  }
}

internal data class ReturnSelectionItemUi(
    val itemCode: String,
    val itemName: String,
    val soldQty: Double,
    val unitAmount: Double,
)

@Composable
internal fun ReturnAccountingSummary(
    returnTotal: Double,
    currency: String?,
    refundEnabled: Boolean,
    creditEnabled: Boolean,
    projectedOutstanding: Double?,
    affectInventory: Boolean,
) {
  val normalizedCurrency = normalizeCurrency(currency)
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                  RoundedCornerShape(10.dp),
              )
              .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
        text = "Resumen contable",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Total devolución")
      Text(formatCurrency(normalizedCurrency, returnTotal))
    }
    if (refundEnabled) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Reembolso")
        Text(formatCurrency(normalizedCurrency, returnTotal))
      }
    } else if (creditEnabled) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Crédito a favor")
        Text(formatCurrency(normalizedCurrency, returnTotal))
      }
    }
    if (projectedOutstanding != null) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Saldo estimado")
        Text(formatCurrency(normalizedCurrency, projectedOutstanding))
      }
      Text(
          text =
              "Nota cajero: el saldo en ERPNext puede verse igual hasta la conciliación o cierre de caja.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
        if (affectInventory) {
          "Stock: se reintegra al almacén en el retorno."
        } else {
          "Stock: no se afecta inventario; ajuste contable únicamente."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

internal enum class ReturnDestination(val label: String) {
  RETURN("Reembolso"),
  CREDIT("Crédito a favor"),
}

internal fun defaultReturnIsPhysical(isPosInvoice: Boolean): Boolean = isPosInvoice

private data class PartialReturnUiState(
    val isOpen: Boolean,
    val invoiceId: String?,
    val invoiceLocal: SalesInvoiceWithItemsAndPayments?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val refundMode: String?,
    val refundReference: String,
    val reason: String,
    val qtyByItemCode: Map<String, Double>,
    val destination: ReturnDestination,
    val destinationTouched: Boolean,
    val isPhysicalReturn: Boolean,
)

private data class FullReturnUiState(
    val isOpen: Boolean,
    val invoiceId: String?,
    val invoice: SalesInvoiceBO?,
    val invoiceLocal: SalesInvoiceWithItemsAndPayments?,
    val isLoading: Boolean,
    val errorMessage: String?,
    val refundMode: String?,
    val refundReference: String,
    val reason: String,
    val destination: ReturnDestination,
    val destinationTouched: Boolean,
    val isPhysicalReturn: Boolean,
)

private fun partialReturnState(
    returnPolicy: ReturnPolicySettings,
    invoiceId: String? = null,
    isOpen: Boolean = false,
): PartialReturnUiState =
    PartialReturnUiState(
        isOpen = isOpen,
        invoiceId = invoiceId,
        invoiceLocal = null,
        isLoading = false,
        errorMessage = null,
        refundMode = null,
        refundReference = "",
        reason = "",
        qtyByItemCode = emptyMap(),
        destination = defaultReturnDestination(returnPolicy),
        destinationTouched = false,
        isPhysicalReturn = defaultReturnIsPhysical(isPosInvoice = true),
    )

private fun fullReturnState(
    returnPolicy: ReturnPolicySettings,
    invoiceId: String? = null,
    invoice: SalesInvoiceBO? = null,
    isOpen: Boolean = false,
): FullReturnUiState =
    FullReturnUiState(
        isOpen = isOpen,
        invoiceId = invoiceId,
        invoice = invoice,
        invoiceLocal = null,
        isLoading = false,
        errorMessage = null,
        refundMode = null,
        refundReference = "",
        reason = "",
        destination = defaultReturnDestination(returnPolicy),
        destinationTouched = false,
        isPhysicalReturn = defaultReturnIsPhysical(isPosInvoice = invoice?.isPos == true),
    )

@Composable
internal fun CustomerOutstandingSummary(
    customer: CustomerBO,
    invoices: List<SalesInvoiceBO>,
    posBaseCurrency: String,
) {
  val strings = LocalAppStrings.current
  val companyCurrency = normalizeCurrency(posBaseCurrency)
  val invoiceCurrency = normalizeCurrency(invoices.firstOrNull()?.currency)
  val totalBase =
      if (invoices.isNotEmpty()) {
        invoices.sumOf { resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingCompany }
      } else {
        customer.totalPendingAmount ?: customer.currentBalance ?: 0.0
      }
  val totalPos =
      if (invoices.isNotEmpty()) {
        invoices.sumOf { resolveInvoiceDisplayAmounts(it, companyCurrency).outstandingInvoice }
      } else {
        totalBase
      }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  RoundedCornerShape(12.dp),
              )
              .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = strings.customer.outstandingSummaryTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(strings.customer.outstandingSummaryInvoicesLabel)
      Text("${if (invoices.isNotEmpty()) invoices.size else (customer.pendingInvoices ?: 0)}")
    }
    if (totalBase <= 0.0) {
      Text(strings.customer.outstandingSummaryAmountLabel)
    } else {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (invoiceCurrency.isNotBlank()) {
          Text(invoiceCurrency)
          Text(formatCurrency(invoiceCurrency, totalPos))
        } else {
          Text(companyCurrency)
          Text(formatCurrency(companyCurrency, totalBase))
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(companyCurrency)
        Text(formatCurrency(companyCurrency, totalBase))
      }
    }
  }
}

private fun handleQuickAction(
    actions: CustomerAction,
    customer: CustomerBO,
    actionType: CustomerQuickActionType,
) {
  when (actionType) {
    CustomerQuickActionType.PendingInvoices -> actions.onViewPendingInvoices(customer)
    CustomerQuickActionType.CreateQuotation -> actions.onCreateQuotation(customer)
    CustomerQuickActionType.CreateSalesOrder -> actions.onCreateSalesOrder(customer)
    CustomerQuickActionType.CreateDeliveryNote -> actions.onCreateDeliveryNote(customer)
    CustomerQuickActionType.CreateInvoice -> actions.onCreateInvoice(customer)
    CustomerQuickActionType.RegisterPayment -> actions.onRegisterPayment(customer)
    else -> {}
  }
}

@Composable
private fun FullScreenErrorMessage(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
          Icons.Filled.Error,
          "Error",
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.error,
      )
      Text(
          errorMessage,
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.error,
      )
      Button(
          onClick = onRetry,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      ) {
        Text("Reintentar")
      }
    }
  }
}

@Composable
internal fun EmptyStateMessage(message: String, icon: ImageVector, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(
          icon,
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
          message,
          style = MaterialTheme.typography.headlineSmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

fun Modifier.shimmerBackground(
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    baseAlpha: Float = 0.6f,
    highlightAlpha: Float = 0.2f,
    gradientWidth: Float = 400f,
    durationMillis: Int = 1200,
): Modifier = composed {
  val transition = rememberInfiniteTransition(label = "shimmerTransition")
  val translateAnim by
      transition.animateFloat(
          initialValue = 0f,
          targetValue = gradientWidth,
          animationSpec =
              infiniteRepeatable(
                  tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
                  RepeatMode.Restart,
              ),
          label = "shimmerTranslateAnim",
      )

  val shimmerColors =
      listOf(
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha),
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = highlightAlpha),
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha),
      )

  this.background(
      brush =
          Brush.linearGradient(
              colors = shimmerColors,
              start = Offset(translateAnim - gradientWidth, translateAnim - gradientWidth),
              end = Offset(translateAnim, translateAnim),
          ),
      shape = shape,
  )
}

internal fun isWithinDays(postingDate: String?, days: Int): Boolean {
  val invoiceDate = parsePostingDate(postingDate) ?: return false
  val threshold = currentLocalDate().minus(DatePeriod(days = days))
  return invoiceDate >= threshold
}

internal fun parsePostingDate(value: String?): LocalDate? {
  val raw = value?.substringBefore('T')?.substringBefore(' ')?.trim()
  if (raw.isNullOrBlank()) return null
  return runCatching { LocalDate.parse(raw) }.getOrNull()
}

private fun currentLocalDate(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
