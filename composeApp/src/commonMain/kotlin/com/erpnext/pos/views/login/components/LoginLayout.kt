package com.erpnext.pos.views.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LoginLayoutState(
    val useSideLayout: Boolean,
    val useGridForSites: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val compactBrandPanel: Boolean,
    val compactCard: Boolean,
)

@Composable
fun LoginScreenContainer(
    layout: LoginLayoutState,
    brandContent: @Composable () -> Unit,
    cardContent: @Composable () -> Unit,
) {
  val background =
      Brush.linearGradient(
          listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
              MaterialTheme.colorScheme.background,
          )
      )

  Box(modifier = Modifier.fillMaxSize().background(background)) {
    if (layout.useSideLayout) {
      DesktopLoginLayout(
          horizontalPadding = layout.horizontalPadding,
          verticalPadding = layout.verticalPadding,
          brandContent = brandContent,
          cardContent = cardContent,
      )
    } else {
      MobileLoginLayout(
          horizontalPadding = layout.horizontalPadding,
          verticalPadding = layout.verticalPadding,
          brandContent = brandContent,
          cardContent = cardContent,
      )
    }
  }
}

@Composable
private fun DesktopLoginLayout(
    horizontalPadding: Dp,
    verticalPadding: Dp,
    brandContent: @Composable () -> Unit,
    cardContent: @Composable () -> Unit,
) {
  val scrollState = rememberScrollState()
  Row(
      modifier =
          Modifier.fillMaxSize()
              .padding(horizontal = horizontalPadding, vertical = verticalPadding)
              .verticalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(32.dp),
      verticalAlignment = Alignment.Top,
  ) {
    Column(modifier = Modifier.weight(1f).padding(top = 16.dp)) { brandContent() }
    Column(modifier = Modifier.weight(1f).fillMaxHeight()) { cardContent() }
  }
}

@Composable
private fun MobileLoginLayout(
    horizontalPadding: Dp,
    verticalPadding: Dp,
    brandContent: @Composable () -> Unit,
    cardContent: @Composable () -> Unit,
) {
  val scrollState = rememberScrollState()
  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(horizontal = horizontalPadding, vertical = verticalPadding)
              .verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) { brandContent() }
    Column(modifier = Modifier.fillMaxWidth()) { cardContent() }
  }
}
