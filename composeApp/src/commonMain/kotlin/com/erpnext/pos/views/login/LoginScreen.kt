@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.login

import AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.views.login.components.BrandPanel
import com.erpnext.pos.views.login.components.LoginCard
import com.erpnext.pos.views.login.components.LoginLayoutState
import com.erpnext.pos.views.login.components.LoginScreenContainer
import kotlin.time.ExperimentalTime
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreen(state: LoginState, actions: LoginAction) {
  var siteUrl by remember { mutableStateOf("") }
  val layout = rememberLoginLayoutState()
  val isDesktopPlatform = getPlatformName() == "Desktop"

  LaunchedEffect(Unit) { actions.existingSites() }

  LoginScreenContainer(
      layout = layout,
      brandContent = { BrandPanel(compact = layout.compactBrandPanel) },
      cardContent = {
        LoginCard(
            state = state,
            siteUrl = siteUrl,
            onSiteUrlChanged = { siteUrl = it },
            actions = actions,
            compact = layout.compactCard,
            useGridForSites = layout.useGridForSites,
            isDesktop = isDesktopPlatform,
        )
      },
  )
}

@Composable
private fun rememberLoginLayoutState(): LoginLayoutState {
  val sizeClass = rememberWindowSizeClass()
  val compactWidth =
      sizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
          sizeClass.widthSizeClass == WindowWidthSizeClass.Medium
  val expandedWidth = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
  val expandedHeight = sizeClass.heightSizeClass == WindowHeightSizeClass.Expanded
  val useSideLayout = !compactWidth
  val useGridForSites = expandedWidth && expandedHeight
  return LoginLayoutState(
      useSideLayout = useSideLayout,
      useGridForSites = useGridForSites,
      horizontalPadding = if (useSideLayout) 56.dp else 24.dp,
      verticalPadding = if (useSideLayout) 48.dp else 24.dp,
      compactBrandPanel = !useSideLayout,
      compactCard = !useGridForSites,
  )
}

@Composable
@Preview
fun LoginPreview() {
  AppTheme {
    LoginScreen(
        state =
            LoginState.Success(
                listOf(
                    Site("http://localhost:8000", "La Casita del Queso", null, true),
                    Site("httsp://staging.clothingcenterni.com", "Clothing Center", null, false),
                    Site("httsp://staging.gamezonenic.com", "Game Zone", null, false),
                )
            ),
        actions = LoginAction(),
    )
  }
}
