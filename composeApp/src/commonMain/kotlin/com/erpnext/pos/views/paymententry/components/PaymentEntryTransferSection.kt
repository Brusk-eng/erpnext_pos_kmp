package com.erpnext.pos.views.paymententry.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun TransferFlowSection(
    sourceValue: String,
    destinationValue: String,
    options: List<String>,
    onSourceSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    fieldShape: RoundedCornerShape,
    fieldColors: TextFieldColors,
) {
  val scheme = MaterialTheme.colorScheme
  val pulse =
      rememberInfiniteTransition(label = "transferPulse")
          .animateFloat(
              initialValue = 0.96f,
              targetValue = 1.06f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                      repeatMode = RepeatMode.Reverse,
                  ),
              label = "pulse",
          )
          .value

  ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      val horizontal = maxWidth > 700.dp
      if (horizontal) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          TransferModeCard(
              modifier = Modifier.weight(1f),
              title = "Cuenta origen",
              label = "Desde (cuenta)",
              value = sourceValue,
              options = options,
              accent = scheme.primary,
              shape = fieldShape,
              colors = fieldColors,
              onSelected = onSourceSelected,
          )

          TransferDirectionIndicator(
              pulse = pulse,
              gradient = listOf(scheme.primary, scheme.secondary),
              modifier = Modifier.size(40.dp),
          )

          TransferModeCard(
              modifier = Modifier.weight(1f),
              title = "Cuenta destino",
              label = "Hacia (cuenta)",
              value = destinationValue,
              options = options,
              accent = scheme.secondary,
              shape = fieldShape,
              colors = fieldColors,
              onSelected = onDestinationSelected,
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          TransferModeCard(
              modifier = Modifier.fillMaxWidth(),
              title = "Cuenta origen",
              label = "Desde (cuenta)",
              value = sourceValue,
              options = options,
              accent = scheme.primary,
              shape = fieldShape,
              colors = fieldColors,
              onSelected = onSourceSelected,
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TransferDirectionIndicator(
                pulse = pulse,
                gradient = listOf(scheme.primary, scheme.secondary),
                modifier = Modifier.size(36.dp),
            )
          }
          TransferModeCard(
              modifier = Modifier.fillMaxWidth(),
              title = "Cuenta destino",
              label = "Hacia (cuenta)",
              value = destinationValue,
              options = options,
              accent = scheme.secondary,
              shape = fieldShape,
              colors = fieldColors,
              onSelected = onDestinationSelected,
          )
        }
      }
    }
  }
}

@Composable
private fun TransferDirectionIndicator(
    pulse: Float,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier.clip(CircleShape).background(Brush.horizontalGradient(gradient)),
      contentAlignment = Alignment.Center,
  ) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.scale(pulse),
    )
  }
}

@Composable
private fun TransferModeCard(
    modifier: Modifier,
    title: String,
    label: String,
    value: String,
    options: List<String>,
    accent: Color,
    shape: RoundedCornerShape,
    colors: TextFieldColors,
    onSelected: (String) -> Unit,
) {
  Column(
      modifier =
          modifier
              .clip(RoundedCornerShape(14.dp))
              .background(accent.copy(alpha = 0.08f))
              .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
              .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
          modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(accent),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            imageVector = Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp),
        )
      }
      Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
    }

    ModeSelectorField(
        label = label,
        value = value,
        options = options,
        shape = shape,
        colors = colors,
        onSelected = onSelected,
    )
  }
}
