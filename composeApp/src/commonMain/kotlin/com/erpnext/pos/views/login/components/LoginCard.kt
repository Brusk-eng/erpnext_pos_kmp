package com.erpnext.pos.views.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.utils.isValidUrlInput
import com.erpnext.pos.views.login.LoginAction
import com.erpnext.pos.views.login.LoginState

@Composable
fun LoginCard(
    state: LoginState,
    siteUrl: String,
    onSiteUrlChanged: (String) -> Unit,
    actions: LoginAction,
    compact: Boolean,
    useGridForSites: Boolean,
    isDesktop: Boolean,
    modifier: Modifier = Modifier,
) {
  Card(
      modifier = modifier,
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(if (compact) 20.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
          text = "Inicio de sesión",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
      )
      Text(
          text = "Selecciona una instancia o agrega una nueva URL.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Button(
          onClick = actions.clear,
          enabled = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
      ) {
        Text(text = "Limpiar instancias")
      }

      when (state) {
        is LoginState.Loading -> LoginLoadingState()
        is LoginState.Success ->
            LoginSuccessState(
                state = state,
                siteUrl = siteUrl,
                onSiteUrlChanged = onSiteUrlChanged,
                actions = actions,
                compact = compact,
                useGridForSites = useGridForSites,
                isDesktop = isDesktop,
            )
        is LoginState.Authenticated -> LoginAuthenticatingState()
        is LoginState.Error -> LoginErrorState(message = state.message, onRetry = actions.onReset)
      }
    }
  }
}

@Composable
private fun LoginLoadingState() {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    CircularProgressIndicator(
        modifier = Modifier.size(36.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
    )
  }
}

@Composable
private fun LoginSuccessState(
    state: LoginState.Success,
    siteUrl: String,
    onSiteUrlChanged: (String) -> Unit,
    actions: LoginAction,
    compact: Boolean,
    useGridForSites: Boolean,
    isDesktop: Boolean,
) {
  val sites = state.sites.orEmpty()
  if (sites.isNotEmpty()) {
    SavedSitesSection(
        sites = sites,
        onSelect = actions.onSiteSelected,
        onToggleFavorite = actions.onToggleFavorite,
        onDelete = actions.onDeleteSite,
        compact = compact,
        useGrid = useGridForSites,
        enableSwipeDelete = !isDesktop,
        showDesktopDelete = isDesktop,
    )
  }

  UrlInputField(url = siteUrl, onUrlChanged = onSiteUrlChanged)

  Button(
      onClick = { if (siteUrl.isNotBlank()) actions.onAddSite(siteUrl) },
      enabled = siteUrl.isNotBlank(),
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
  ) {
    Text(text = "Conectar instancia")
  }
}

@Composable
private fun LoginAuthenticatingState() {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 3.dp,
    )
  }
}

@Composable
private fun LoginErrorState(message: String, onRetry: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onRetry) { Text(text = "Reintentar") }
  }
}

@Composable
fun UrlInputField(
    url: String,
    onUrlChanged: (String) -> Unit,
) {
  var isError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }

  OutlinedTextField(
      value = url,
      onValueChange = { input ->
        onUrlChanged(input.trim())
        val isValid = isValidUrlInput(input)
        isError = !isValid
        errorMessage =
            if (isError) "URL inválida, usa http://ejemplo.com o https://ejemplo.com" else ""
      },
      label = { Text("URL del Sitio") },
      placeholder = { Text("https://erp.frappe.cloud") },
      isError = isError,
      singleLine = true,
      textStyle =
          MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.Medium,
              letterSpacing = 0.3.sp,
          ),
      leadingIcon = {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint =
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
      },
      supportingText = {
        Text(
            text = if (isError) errorMessage else "Ingresa la URL completa de tu instancia",
            color =
                if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
      },
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
  )
}
