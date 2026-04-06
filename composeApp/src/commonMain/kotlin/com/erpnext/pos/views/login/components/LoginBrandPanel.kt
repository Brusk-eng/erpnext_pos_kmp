package com.erpnext.pos.views.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandPanel(modifier: Modifier = Modifier, compact: Boolean = false) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
        text = "ERPNext POS",
        style =
            MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp,
            ),
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "Conecta tu instancia y continúa con tu caja sin fricción.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!compact) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FeatureRow(icon = Icons.Default.Security, text = "OAuth seguro con ERPNext")
        FeatureRow(icon = Icons.Default.Speed, text = "Sincronización rápida y confiable")
        FeatureRow(icon = Icons.Default.Language, text = "Soporte multi‑instancia y multi‑sucursal")
      }
    }
  }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
