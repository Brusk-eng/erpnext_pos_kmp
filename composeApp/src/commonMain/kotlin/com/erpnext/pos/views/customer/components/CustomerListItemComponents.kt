package com.erpnext.pos.views.customer.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.CustomerQuickActionType
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.QuickActions.customerQuickActions
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.views.customer.HeaderChip

@Composable
internal fun SearchTextField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar...",
    onSearchAction: (() -> Unit)? = null,
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  OutlinedTextField(
      value = searchQuery,
      onValueChange = { query -> onSearchQueryChange(query) },
      modifier = modifier.fillMaxWidth(),
      placeholder = {
        Text(
            placeholderText,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
      },
      leadingIcon = {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Buscar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
              onClick = {
                onSearchQueryChange("")
                keyboardController?.show()
              }
          ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = "Limpiar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      },
      singleLine = true,
      keyboardOptions =
          KeyboardOptions.Default.copy(
              imeAction = if (onSearchAction != null) ImeAction.Search else ImeAction.Done
          ),
      keyboardActions =
          KeyboardActions(
              onSearch = {
                onSearchAction?.invoke()
                keyboardController?.hide()
              },
              onDone = { keyboardController?.hide() },
          ),
      colors =
          TextFieldDefaults.colors(
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              unfocusedContainerColor =
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
              cursorColor = MaterialTheme.colorScheme.primary,
          ),
      shape = RoundedCornerShape(18.dp),
  )
}

@Composable
internal fun CustomerItem(
    customer: CustomerBO,
    posCurrency: String,
    companyCurrency: String,
    contextExchangeRate: Double?,
    isDesktop: Boolean,
    onSelect: (CustomerBO) -> Unit,
    onOpenQuickActions: () -> Unit,
    onQuickAction: (CustomerQuickActionType) -> Unit,
) {
  val strings = LocalAppStrings.current
  val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0
  val pendingInvoices = customer.pendingInvoices ?: 0
  var isMenuExpanded by remember { mutableStateOf(false) }
  val quickActions = remember { customerQuickActions() }
  val avatarSize = if (isDesktop) 48.dp else 40.dp
  val companyCurr = normalizeCurrency(companyCurrency)
  val posCurr = normalizeCurrency(posCurrency)
  val pendingCompany = bd(customer.totalPendingAmount ?: customer.currentBalance ?: 0.0).toDouble(2)
  val pendingPos =
      remember(pendingCompany, companyCurr, posCurr, contextExchangeRate) {
        when {
          posCurr.equals(companyCurr, ignoreCase = true) -> pendingCompany
          contextExchangeRate == null || contextExchangeRate <= 0.0 -> null
          companyCurr.equals("USD", ignoreCase = true) -> pendingCompany * contextExchangeRate
          posCurr.equals("USD", ignoreCase = true) -> pendingCompany / contextExchangeRate
          else -> null
        }
      }
  val statusLabel =
      when {
        isOverLimit -> strings.customer.overdueLabel
        pendingInvoices > 0 || pendingCompany > 0.0 -> strings.customer.pendingLabel
        else -> strings.customer.activeLabel
      }
  val statusColor =
      when {
        isOverLimit -> MaterialTheme.colorScheme.error
        pendingInvoices > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
      }
  val emphasis = pendingInvoices > 0 || isOverLimit
  val cardShape = RoundedCornerShape(18.dp)
  val cardBrush =
      Brush.linearGradient(
          colors =
              listOf(
                  MaterialTheme.colorScheme.surface,
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              )
      )
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 104.dp)
              .clip(cardShape)
              .clickable { onSelect(customer) }
              .pointerInput(customer.name, isDesktop) {
                if (!isDesktop) {
                  val totalDrag = 0f
                  detectHorizontalDragGestures(
                      onDragEnd = {
                        if (kotlin.math.abs(totalDrag) > 64) {
                          onOpenQuickActions()
                        }
                      },
                      onHorizontalDrag = { _, _ -> },
                  )
                }
              },
      shape = cardShape,
      border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.35f)),
      colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(cardBrush, cardShape)
                .border(1.dp, Color.Transparent, shape = cardShape)
                .padding(if (isDesktop) 10.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      val context = LocalPlatformContext.current
      Row(
          modifier = Modifier.fillMaxWidth().height(42.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!customer.image.isNullOrEmpty()) {
          AsyncImage(
              modifier = Modifier.size(60.dp).clip(RoundedCornerShape(50.dp)),
              model =
                  remember(customer.image) {
                    ImageRequest.Builder(context)
                        .data(customer.image.ifBlank { "https://placehold.co/600x400" })
                        .crossfade(true)
                        .build()
                  },
              contentDescription = customer.name,
              contentScale = ContentScale.Crop,
          )
        } else {
          Icon(
              Icons.Filled.Person,
              contentDescription = customer.customerName,
              modifier = Modifier.size(avatarSize).clip(CircleShape).padding(12.dp),
              tint = statusColor,
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
              customer.customerName,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface,
          )
          if (customer.mobileNo?.isNotEmpty() == true) {
            Text(
                text = customer.mobileNo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
          }
          Text(
              text = "Codigo: ${customer.name}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }

        IconButton(onClick = { isMenuExpanded = true }) {
          Icon(
              Icons.Filled.MoreVert,
              contentDescription = strings.customer.moreActions,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
          quickActions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label) },
                leadingIcon = { Icon(action.icon, contentDescription = null) },
                onClick = {
                  isMenuExpanded = false
                  onQuickAction(action.type)
                },
            )
          }
        }
      }

      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusPill(label = statusLabel, isCritical = emphasis)
        Column {
          Text(
              text = formatCurrency(posCurr, pendingPos ?: pendingCompany),
              style = MaterialTheme.typography.titleSmall,
              color =
                  if (emphasis) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.onSurface,
          )
          if (pendingPos != null && !posCurr.equals(companyCurr, ignoreCase = true)) {
            Text(
                text = formatCurrency(companyCurr, pendingCompany),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (pendingInvoices > 0) {
          HeaderChip(label = "Pend.", value = pendingInvoices.toString(), isCritical = true)
        }
      }
    }
  }
}

@Composable
private fun StatusPill(label: String, isCritical: Boolean) {
  val background =
      if (isCritical) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
      } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
      }
  val textColor =
      if (isCritical) {
        MaterialTheme.colorScheme.error
      } else {
        MaterialTheme.colorScheme.secondary
      }
  Surface(color = background, shape = RoundedCornerShape(12.dp), tonalElevation = 0.dp) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}
