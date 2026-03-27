@file:OptIn(ExperimentalMaterial3Api::class)

package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.erpnext.pos.domain.models.CompanyBO
import com.erpnext.pos.domain.models.CustomerGroupBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.domain.models.TerritoryBO
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.MoneyTextField
import kotlinx.coroutines.delay

private enum class CustomerDialogTab(val label: String) {
  Personal("Principal"),
  Contact("Contacto"),
  Tax("Impuestos"),
  Accounting("Contabilidad"),
}

private enum class NicaraguanTaxRegime(val label: String, val hint: String) {
  Simplified("Régimen simplificado", "0013012120003D"),
  General("Régimen general", "J0310000000001"),
}

private data class RegionInputOption(
    val code: String,
    val dialCode: String,
    val country: String,
    val taxIdHint: String,
)

@Composable
internal fun NewCustomerDialog(
    onDismiss: () -> Unit,
    onSubmit: (CreateCustomerInput) -> Unit,
    customerGroups: List<CustomerGroupBO>,
    territories: List<TerritoryBO>,
    paymentTermsOptions: List<PaymentTermBO>,
    companies: List<CompanyBO>,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var customerType by rememberSaveable { mutableStateOf("Individual") }
  var customerGroup by rememberSaveable { mutableStateOf("") }
  var territory by rememberSaveable { mutableStateOf("") }
  var taxId by rememberSaveable { mutableStateOf("") }
  var taxCategory by rememberSaveable { mutableStateOf("") }
  var email by rememberSaveable { mutableStateOf("") }
  var mobile by rememberSaveable { mutableStateOf("") }
  var phone by rememberSaveable { mutableStateOf("") }
  var addressLine by rememberSaveable { mutableStateOf("") }
  var addressLine2 by rememberSaveable { mutableStateOf("") }
  var city by rememberSaveable { mutableStateOf("") }
  var state by rememberSaveable { mutableStateOf("") }
  var country by rememberSaveable { mutableStateOf("") }
  var creditLimit by rememberSaveable { mutableStateOf("") }
  var selectedPaymentTerm by rememberSaveable { mutableStateOf("") }
  var notes by rememberSaveable { mutableStateOf("") }
  var isInternalCustomer by rememberSaveable { mutableStateOf(false) }
  var internalCompany by rememberSaveable { mutableStateOf("") }

  var typeExpanded by remember { mutableStateOf(false) }
  var groupExpanded by remember { mutableStateOf(false) }
  var territoryExpanded by remember { mutableStateOf(false) }
  var paymentExpanded by remember { mutableStateOf(false) }
  var companyExpanded by remember { mutableStateOf(false) }
  var rucRegionExpanded by remember { mutableStateOf(false) }
  var phoneRegionExpanded by remember { mutableStateOf(false) }
  var niTaxRegimeExpanded by remember { mutableStateOf(false) }
  var selectedTab by rememberSaveable { mutableStateOf(CustomerDialogTab.Personal) }
  var niTaxRegime by rememberSaveable { mutableStateOf(NicaraguanTaxRegime.General.name) }
  var submitAttempted by rememberSaveable { mutableStateOf(false) }
  val regionOptions = remember { customerRegionOptions() }
  val defaultRegionCode =
      remember(companies) { resolveRegionCodeFromCountry(companies.firstOrNull()?.country) }
  var rucRegionCode by rememberSaveable { mutableStateOf(defaultRegionCode) }
  var phoneRegionCode by rememberSaveable { mutableStateOf(defaultRegionCode) }
  val selectedRucRegion =
      remember(rucRegionCode, regionOptions) {
        regionOptions.firstOrNull { it.code == rucRegionCode } ?: regionOptions.first()
      }
  val selectedPhoneRegion =
      remember(phoneRegionCode, regionOptions) {
        regionOptions.firstOrNull { it.code == phoneRegionCode } ?: regionOptions.first()
      }
  val selectedNicaraguanTaxRegime =
      remember(niTaxRegime) {
        NicaraguanTaxRegime.entries.firstOrNull { it.name == niTaxRegime }
            ?: NicaraguanTaxRegime.General
      }
  val taxIdentifierHint =
      remember(rucRegionCode, selectedRucRegion, selectedNicaraguanTaxRegime) {
        if (rucRegionCode == "NI") selectedNicaraguanTaxRegime.hint else selectedRucRegion.taxIdHint
      }
  val creditCurrency =
      remember(companies, internalCompany, isInternalCustomer) {
        val selectedCompany =
            if (isInternalCustomer && internalCompany.isNotBlank()) {
              companies.firstOrNull { it.company.equals(internalCompany, ignoreCase = true) }
            } else {
              companies.firstOrNull()
            }
        normalizeCurrency(selectedCompany?.defaultCurrency)
      }
  val emailTrimmed = email.trim()
  val emailInvalid = emailTrimmed.isNotBlank() && !isValidEmailAddress(emailTrimmed)
  val creditInvalid = creditLimit.isNotBlank() && creditLimit.toDoubleOrNull() == null
  val nameInvalid = submitAttempted && name.isBlank()
  val internalCompanyInvalid = submitAttempted && isInternalCustomer && internalCompany.isBlank()
  val isValid =
      name.isNotBlank() &&
          (!isInternalCustomer || internalCompany.isNotBlank()) &&
          !emailInvalid &&
          !creditInvalid
  val requiredCompleted =
      buildList {
            add(name.isNotBlank())
            add(!isInternalCustomer || internalCompany.isNotBlank())
          }
          .count { it }
  val tabBodyMinHeight = 380.dp
  val tabBodyMaxHeight = 560.dp
  val tabScrollState = rememberScrollState()
  val personalNameFocusRequester = remember { FocusRequester() }
  val contactMobileFocusRequester = remember { FocusRequester() }
  val taxIdFocusRequester = remember { FocusRequester() }
  val creditLimitFocusRequester = remember { FocusRequester() }

  LaunchedEffect(selectedTab) {
    tabScrollState.scrollTo(0)
    delay(90)
    when (selectedTab) {
      CustomerDialogTab.Personal -> personalNameFocusRequester.requestFocus()
      CustomerDialogTab.Contact -> contactMobileFocusRequester.requestFocus()
      CustomerDialogTab.Tax -> taxIdFocusRequester.requestFocus()
      CustomerDialogTab.Accounting -> creditLimitFocusRequester.requestFocus()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier.fillMaxWidth(0.92f)
                .widthIn(min = 360.dp, max = 900.dp)
                .heightIn(min = 700.dp, max = 700.dp),
    ) {
      Column(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        ) {
          Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            ) {
              Icon(
                  imageVector = Icons.Default.PersonAdd,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(10.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text("Nuevo cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
              Text(
                  "Completa la ficha para registrar rápidamente.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            ) {
              Text(
                  text = "Requeridos $requiredCompleted/2",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
        PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
          CustomerDialogTab.entries.forEachIndexed { index, tab ->
            Tab(
                selected = selectedTab.ordinal == index,
                onClick = { selectedTab = tab },
                text = {
                  Text(
                      text = tab.label,
                      fontWeight =
                          if (selectedTab.ordinal == index) FontWeight.SemiBold else FontWeight.Normal,
                  )
                },
            )
          }
        }
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .heightIn(min = tabBodyMinHeight, max = tabBodyMaxHeight)
                      .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                      .verticalScroll(tabScrollState),
              verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            when (selectedTab) {
              CustomerDialogTab.Personal -> PersonalTab(
                  name = name,
                  onNameChange = { name = it },
                  customerType = customerType,
                  onCustomerTypeChange = { customerType = it },
                  typeExpanded = typeExpanded,
                  onTypeExpandedChange = { typeExpanded = it },
                  customerGroups = customerGroups,
                  customerGroup = customerGroup,
                  onCustomerGroupChange = { customerGroup = it },
                  groupExpanded = groupExpanded,
                  onGroupExpandedChange = { groupExpanded = it },
                  territories = territories,
                  territory = territory,
                  onTerritoryChange = { territory = it },
                  territoryExpanded = territoryExpanded,
                  onTerritoryExpandedChange = { territoryExpanded = it },
                  isInternalCustomer = isInternalCustomer,
                  onInternalCustomerChange = { isInternalCustomer = it },
                  companies = companies,
                  internalCompany = internalCompany,
                  onInternalCompanyChange = { internalCompany = it },
                  companyExpanded = companyExpanded,
                  onCompanyExpandedChange = { companyExpanded = it },
                  notes = notes,
                  onNotesChange = { notes = it },
                  nameInvalid = nameInvalid,
                  internalCompanyInvalid = internalCompanyInvalid,
                  focusRequester = personalNameFocusRequester,
              )
              CustomerDialogTab.Contact -> ContactTab(
                  regionOptions = regionOptions,
                  selectedPhoneRegion = selectedPhoneRegion,
                  phoneRegionExpanded = phoneRegionExpanded,
                  onPhoneRegionExpandedChange = { phoneRegionExpanded = it },
                  onPhoneRegionCodeChange = { phoneRegionCode = it },
                  mobile = mobile,
                  onMobileChange = { mobile = it },
                  phone = phone,
                  onPhoneChange = { phone = it },
                  email = email,
                  onEmailChange = { email = it },
                  emailInvalid = emailInvalid,
                  addressLine = addressLine,
                  onAddressLineChange = { addressLine = it },
                  addressLine2 = addressLine2,
                  onAddressLine2Change = { addressLine2 = it },
                  city = city,
                  onCityChange = { city = it },
                  state = state,
                  onStateChange = { state = it },
                  country = country,
                  onCountryChange = { country = it },
                  focusRequester = contactMobileFocusRequester,
              )
              CustomerDialogTab.Tax -> TaxTab(
                  regionOptions = regionOptions,
                  selectedRucRegion = selectedRucRegion,
                  rucRegionCode = rucRegionCode,
                  rucRegionExpanded = rucRegionExpanded,
                  onRucRegionExpandedChange = { rucRegionExpanded = it },
                  onRucRegionCodeChange = { rucRegionCode = it },
                  niTaxRegimeExpanded = niTaxRegimeExpanded,
                  onNiTaxRegimeExpandedChange = { niTaxRegimeExpanded = it },
                  selectedNicaraguanTaxRegime = selectedNicaraguanTaxRegime,
                  onNiTaxRegimeChange = { niTaxRegime = it },
                  taxId = taxId,
                  onTaxIdChange = { taxId = it },
                  taxIdentifierHint = taxIdentifierHint,
                  taxCategory = taxCategory,
                  onTaxCategoryChange = { taxCategory = it },
                  focusRequester = taxIdFocusRequester,
              )
              CustomerDialogTab.Accounting -> AccountingTab(
                  creditCurrency = creditCurrency,
                  creditLimit = creditLimit,
                  onCreditLimitChange = { creditLimit = it },
                  creditInvalid = creditInvalid,
                  paymentTermsOptions = paymentTermsOptions,
                  selectedPaymentTerm = selectedPaymentTerm,
                  onSelectedPaymentTermChange = { selectedPaymentTerm = it },
                  paymentExpanded = paymentExpanded,
                  onPaymentExpandedChange = { paymentExpanded = it },
                  focusRequester = creditLimitFocusRequester,
              )
            }
          }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
          Button(
              onClick = {
                submitAttempted = true
                if (!isValid) return@Button
                onSubmit(
                    CreateCustomerInput(
                        customerName = name.trim(),
                        customerType = customerType,
                        customerGroup = customerGroup.trim().ifBlank { null },
                        territory = territory.trim().ifBlank { null },
                        isInternalCustomer = isInternalCustomer,
                        internalCompany = internalCompany.trim().ifBlank { null },
                        taxId = normalizeTaxIdentifier(taxId.trim()).ifBlank { null },
                        taxCategory = taxCategory.trim().ifBlank { null },
                        email = email.trim().ifBlank { null },
                        mobileNo =
                            buildRegionalPhone(selectedPhoneRegion.dialCode, mobile.trim()).ifBlank { null },
                        phone = buildRegionalPhone(selectedPhoneRegion.dialCode, phone.trim()).ifBlank { null },
                        addressLine1 = addressLine.trim().ifBlank { null },
                        addressLine2 = addressLine2.trim().ifBlank { null },
                        city = city.trim().ifBlank { null },
                        state = state.trim().ifBlank { null },
                        country = country.trim().ifBlank { null },
                        creditLimit = creditLimit.toDoubleOrNull(),
                        paymentTerms = selectedPaymentTerm.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                    )
                )
                onDismiss()
              },
              enabled = isValid || !submitAttempted,
              modifier = Modifier.weight(1f),
          ) {
            Text("Crear cliente")
          }
        }
      }
    }
  }
}

@Composable
private fun PersonalTab(
    name: String,
    onNameChange: (String) -> Unit,
    customerType: String,
    onCustomerTypeChange: (String) -> Unit,
    typeExpanded: Boolean,
    onTypeExpandedChange: (Boolean) -> Unit,
    customerGroups: List<CustomerGroupBO>,
    customerGroup: String,
    onCustomerGroupChange: (String) -> Unit,
    groupExpanded: Boolean,
    onGroupExpandedChange: (Boolean) -> Unit,
    territories: List<TerritoryBO>,
    territory: String,
    onTerritoryChange: (String) -> Unit,
    territoryExpanded: Boolean,
    onTerritoryExpandedChange: (Boolean) -> Unit,
    isInternalCustomer: Boolean,
    onInternalCustomerChange: (Boolean) -> Unit,
    companies: List<CompanyBO>,
    internalCompany: String,
    onInternalCompanyChange: (String) -> Unit,
    companyExpanded: Boolean,
    onCompanyExpandedChange: (Boolean) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    nameInvalid: Boolean,
    internalCompanyInvalid: Boolean,
    focusRequester: FocusRequester,
) {
  CustomerDialogTextField(
      value = name,
      onValueChange = onNameChange,
      label = "Nombre del cliente",
      placeholder = "Cliente S.A.",
      keyboardOptions =
          KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
      isError = nameInvalid,
      supportingText =
          if (nameInvalid) {
            {
              Text("El nombre es obligatorio.", style = MaterialTheme.typography.labelSmall)
            }
          } else {
            null
          },
      leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
      modifier = Modifier.focusRequester(focusRequester),
  )
  ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { onTypeExpandedChange(!typeExpanded) }) {
    CustomerDialogTextField(
        value = customerType,
        onValueChange = {},
        label = "Tipo de cliente",
        placeholder = "Seleccionar",
        readOnly = true,
        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
    )
    DropdownMenu(expanded = typeExpanded, onDismissRequest = { onTypeExpandedChange(false) }) {
      listOf("Individual", "Empresa").forEach { option ->
        DropdownMenuItem(text = { Text(option) }, onClick = {
          onCustomerTypeChange(option)
          onTypeExpandedChange(false)
        })
      }
    }
  }
  if (customerGroups.isNotEmpty()) {
    ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { onGroupExpandedChange(!groupExpanded) }) {
      CustomerDialogTextField(
          value = customerGroup,
          onValueChange = {},
          label = "Grupo de cliente",
          placeholder = "Seleccionar",
          readOnly = true,
          modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
      )
      DropdownMenu(expanded = groupExpanded, onDismissRequest = { onGroupExpandedChange(false) }) {
        customerGroups.forEach { option ->
          val label = option.displayName?.takeIf { it.isNotBlank() } ?: option.name
          DropdownMenuItem(text = { Text(label) }, onClick = {
            onCustomerGroupChange(option.name)
            onGroupExpandedChange(false)
          })
        }
      }
    }
  } else {
    CustomerDialogTextField(
        value = customerGroup,
        onValueChange = onCustomerGroupChange,
        label = "Grupo de cliente",
        placeholder = "Retail",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
    )
  }
  if (territories.isNotEmpty()) {
    ExposedDropdownMenuBox(expanded = territoryExpanded, onExpandedChange = { onTerritoryExpandedChange(!territoryExpanded) }) {
      CustomerDialogTextField(
          value = territory,
          onValueChange = {},
          label = "Territorio",
          placeholder = "Seleccionar",
          readOnly = true,
          modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = territoryExpanded) },
      )
      DropdownMenu(expanded = territoryExpanded, onDismissRequest = { onTerritoryExpandedChange(false) }) {
        territories.forEach { option ->
          val label = option.displayName?.takeIf { it.isNotBlank() } ?: option.name
          DropdownMenuItem(text = { Text(label) }, onClick = {
            onTerritoryChange(option.name)
            onTerritoryExpandedChange(false)
          })
        }
      }
    }
  } else {
    CustomerDialogTextField(
        value = territory,
        onValueChange = onTerritoryChange,
        label = "Territorio",
        placeholder = "Managua",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
    )
  }
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Checkbox(checked = isInternalCustomer, onCheckedChange = onInternalCustomerChange)
    Text("Cliente interno (intercompany)")
  }
  if (isInternalCustomer) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp),
      )
      Text(
          text = "Selecciona la compañía a la que pertenece este cliente interno.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (companies.isNotEmpty()) {
      ExposedDropdownMenuBox(expanded = companyExpanded, onExpandedChange = { onCompanyExpandedChange(!companyExpanded) }) {
        CustomerDialogTextField(
            value = internalCompany,
            onValueChange = {},
            label = "Compañía",
            placeholder = "Seleccionar",
            readOnly = true,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyExpanded) },
        )
        DropdownMenu(expanded = companyExpanded, onDismissRequest = { onCompanyExpandedChange(false) }) {
          companies.forEach { option ->
            DropdownMenuItem(text = { Text(option.company) }, onClick = {
              onInternalCompanyChange(option.company)
              onCompanyExpandedChange(false)
            })
          }
        }
      }
    } else {
      CustomerDialogTextField(
          value = internalCompany,
          onValueChange = onInternalCompanyChange,
          label = "Compañía",
          placeholder = "Mi empresa",
          isError = internalCompanyInvalid,
          supportingText =
              if (internalCompanyInvalid) {
                { Text("La compañía es obligatoria.", style = MaterialTheme.typography.labelSmall) }
              } else {
                null
              },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
      )
    }
  }
  CustomerDialogTextField(
      value = notes,
      onValueChange = onNotesChange,
      label = "Notas",
      placeholder = "Observaciones internas",
      singleLine = false,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
      leadingIcon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) },
  )
}

@Composable
private fun ContactTab(
    regionOptions: List<RegionInputOption>,
    selectedPhoneRegion: RegionInputOption,
    phoneRegionExpanded: Boolean,
    onPhoneRegionExpandedChange: (Boolean) -> Unit,
    onPhoneRegionCodeChange: (String) -> Unit,
    mobile: String,
    onMobileChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    emailInvalid: Boolean,
    addressLine: String,
    onAddressLineChange: (String) -> Unit,
    addressLine2: String,
    onAddressLine2Change: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    state: String,
    onStateChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
  ExposedDropdownMenuBox(expanded = phoneRegionExpanded, onExpandedChange = { onPhoneRegionExpandedChange(!phoneRegionExpanded) }) {
    CustomerDialogField(
        value = "${selectedPhoneRegion.code} ${selectedPhoneRegion.dialCode}",
        onValueChange = {},
        label = "Región de teléfono",
        readOnly = true,
        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phoneRegionExpanded) },
    )
    DropdownMenu(expanded = phoneRegionExpanded, onDismissRequest = { onPhoneRegionExpandedChange(false) }) {
      regionOptions.forEach { option ->
        DropdownMenuItem(text = { Text("${option.code} ${option.dialCode} · ${option.country}") }, onClick = {
          onPhoneRegionCodeChange(option.code)
          onPhoneRegionExpandedChange(false)
        })
      }
    }
  }
  CustomerDialogTextField(
      value = mobile,
      onValueChange = onMobileChange,
      label = "Móvil",
      placeholder = "8888 8888",
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
      leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
      modifier = Modifier.focusRequester(focusRequester),
  )
  CustomerDialogTextField(
      value = phone,
      onValueChange = onPhoneChange,
      label = "Teléfono",
      placeholder = "2222 2222",
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
      leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
  )
  Text(
      text = "Se guardará con prefijo regional ${selectedPhoneRegion.dialCode}.",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  CustomerDialogTextField(
      value = email,
      onValueChange = onEmailChange,
      label = "Correo",
      placeholder = "cliente@correo.com",
      isError = emailInvalid,
      supportingText =
          if (emailInvalid) {
            { Text("Formato de correo inválido.", style = MaterialTheme.typography.labelSmall) }
          } else {
            null
          },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
      leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
  )
  CustomerDialogTextField(
      value = addressLine,
      onValueChange = onAddressLineChange,
      label = "Dirección línea 1",
      placeholder = "Calle principal",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
  )
  CustomerDialogTextField(
      value = addressLine2,
      onValueChange = onAddressLine2Change,
      label = "Dirección línea 2",
      placeholder = "Referencias, barrio",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
  )
  CustomerDialogTextField(
      value = city,
      onValueChange = onCityChange,
      label = "Ciudad",
      placeholder = "Managua",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
  )
  CustomerDialogTextField(
      value = state,
      onValueChange = onStateChange,
      label = "Departamento",
      placeholder = "Managua",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
  )
  CustomerDialogTextField(
      value = country,
      onValueChange = onCountryChange,
      label = "País",
      placeholder = "Nicaragua",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
  )
}

@Composable
private fun TaxTab(
    regionOptions: List<RegionInputOption>,
    selectedRucRegion: RegionInputOption,
    rucRegionCode: String,
    rucRegionExpanded: Boolean,
    onRucRegionExpandedChange: (Boolean) -> Unit,
    onRucRegionCodeChange: (String) -> Unit,
    niTaxRegimeExpanded: Boolean,
    onNiTaxRegimeExpandedChange: (Boolean) -> Unit,
    selectedNicaraguanTaxRegime: NicaraguanTaxRegime,
    onNiTaxRegimeChange: (String) -> Unit,
    taxId: String,
    onTaxIdChange: (String) -> Unit,
    taxIdentifierHint: String,
    taxCategory: String,
    onTaxCategoryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
  ExposedDropdownMenuBox(expanded = rucRegionExpanded, onExpandedChange = { onRucRegionExpandedChange(!rucRegionExpanded) }) {
    CustomerDialogField(
        value = selectedRucRegion.code,
        onValueChange = {},
        label = "Región",
        readOnly = true,
        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rucRegionExpanded) },
    )
    DropdownMenu(expanded = rucRegionExpanded, onDismissRequest = { onRucRegionExpandedChange(false) }) {
      regionOptions.forEach { option ->
        DropdownMenuItem(text = { Text("${option.code} · ${option.country}") }, onClick = {
          onRucRegionCodeChange(option.code)
          onRucRegionExpandedChange(false)
        })
      }
    }
  }
  if (rucRegionCode == "NI") {
    ExposedDropdownMenuBox(expanded = niTaxRegimeExpanded, onExpandedChange = { onNiTaxRegimeExpandedChange(!niTaxRegimeExpanded) }) {
      CustomerDialogField(
          value = selectedNicaraguanTaxRegime.label,
          onValueChange = {},
          label = "Tipo de RUC",
          readOnly = true,
          modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = niTaxRegimeExpanded) },
      )
      DropdownMenu(expanded = niTaxRegimeExpanded, onDismissRequest = { onNiTaxRegimeExpandedChange(false) }) {
        NicaraguanTaxRegime.entries.forEach { regime ->
          DropdownMenuItem(text = { Text(regime.label) }, onClick = {
            onNiTaxRegimeChange(regime.name)
            onNiTaxRegimeExpandedChange(false)
          })
        }
      }
    }
  }
  CustomerDialogTextField(
      value = taxId,
      onValueChange = onTaxIdChange,
      label = "RUC / NIT",
      placeholder = taxIdentifierHint,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
      leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
      modifier = Modifier.focusRequester(focusRequester),
  )
  Text(
      text =
          if (rucRegionCode == "NI") {
            "Formato sugerido (${selectedNicaraguanTaxRegime.label}): $taxIdentifierHint"
          } else {
            "Formato sugerido: $taxIdentifierHint"
          },
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  CustomerDialogTextField(
      value = taxCategory,
      onValueChange = onTaxCategoryChange,
      label = "Categoría de impuesto",
      placeholder = "IVA General",
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
  )
}

@Composable
private fun AccountingTab(
    creditCurrency: String,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    creditInvalid: Boolean,
    paymentTermsOptions: List<PaymentTermBO>,
    selectedPaymentTerm: String,
    onSelectedPaymentTermChange: (String) -> Unit,
    paymentExpanded: Boolean,
    onPaymentExpandedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
  MoneyTextField(
      currencyCode = creditCurrency,
      rawValue = creditLimit,
      onRawValueChange = onCreditLimitChange,
      label = "Límite de crédito",
      isError = creditInvalid,
      supportingText =
          if (creditInvalid) {
            { Text("Debe ser un número válido.", style = MaterialTheme.typography.labelSmall) }
          } else {
            {
              Text(
                  text = "Moneda empresa: ${creditCurrency.toCurrencySymbol()} $creditCurrency",
                  style = MaterialTheme.typography.labelSmall,
              )
            }
          },
      imeAction = ImeAction.Next,
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
  )
  if (paymentTermsOptions.isNotEmpty()) {
    ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = { onPaymentExpandedChange(!paymentExpanded) }) {
      CustomerDialogTextField(
          value = selectedPaymentTerm,
          onValueChange = {},
          label = "Términos de pago",
          placeholder = "Seleccionar",
          readOnly = true,
          modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
      )
      DropdownMenu(expanded = paymentExpanded, onDismissRequest = { onPaymentExpandedChange(false) }) {
        paymentTermsOptions.forEach { option ->
          DropdownMenuItem(text = { Text(option.name) }, onClick = {
            onSelectedPaymentTermChange(option.name)
            onPaymentExpandedChange(false)
          })
        }
      }
    }
  } else {
    CustomerDialogTextField(
        value = selectedPaymentTerm,
        onValueChange = onSelectedPaymentTermChange,
        label = "Términos de pago",
        placeholder = "Contado / 30 días",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
    )
  }
}

private fun isValidEmailAddress(value: String): Boolean {
  val normalized = value.trim()
  if (normalized.isBlank()) return false
  return "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex().matches(normalized)
}

private fun customerRegionOptions(): List<RegionInputOption> =
    listOf(
        RegionInputOption("NI", "+505", "Nicaragua", "J0310000000001"),
        RegionInputOption("CR", "+506", "Costa Rica", "3-101-123456"),
        RegionInputOption("HN", "+504", "Honduras", "08011999123456"),
        RegionInputOption("SV", "+503", "El Salvador", "0614-290180-101-3"),
        RegionInputOption("GT", "+502", "Guatemala", "1234567-8"),
        RegionInputOption("PA", "+507", "Panamá", "1556789-1-123456"),
        RegionInputOption("MX", "+52", "México", "XAXX010101000"),
        RegionInputOption("US", "+1", "Estados Unidos", "12-3456789"),
    )

private fun resolveRegionCodeFromCountry(country: String?): String {
  val normalized = country?.trim()?.lowercase().orEmpty()
  return when {
    normalized.contains("nicaragua") -> "NI"
    normalized.contains("costa rica") -> "CR"
    normalized.contains("honduras") -> "HN"
    normalized.contains("el salvador") -> "SV"
    normalized.contains("guatemala") -> "GT"
    normalized.contains("panama") || normalized.contains("panamá") -> "PA"
    normalized.contains("mexico") || normalized.contains("méxico") -> "MX"
    normalized.contains("united states") || normalized.contains("estados unidos") -> "US"
    else -> "NI"
  }
}

private fun normalizeTaxIdentifier(value: String): String {
  if (value.isBlank()) return ""
  return value.trim().replace(Regex("^[A-Za-z]{2}\\s*-\\s*"), "")
}

private fun buildRegionalPhone(dialCode: String, value: String): String {
  if (value.isBlank()) return ""
  val normalized = value.trim()
  if (normalized.startsWith("+")) return normalized
  return "$dialCode $normalized"
}

@Composable
private fun CustomerDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
  CustomerDialogField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      modifier = modifier,
      placeholder = placeholder,
      singleLine = singleLine,
      enabled = enabled,
      readOnly = readOnly,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
      textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
      label = {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        )
      },
      placeholder =
          placeholder?.let {
            {
              Text(
                  text = it,
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
              )
            }
          },
      singleLine = singleLine,
      enabled = enabled,
      readOnly = readOnly,
      isError = isError,
      supportingText = supportingText,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
              focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
              unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedLabelColor = MaterialTheme.colorScheme.primary,
              unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
  )
}
