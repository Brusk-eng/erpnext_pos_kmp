package com.erpnext.pos.views.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.views.login.Site
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

@Composable
fun SavedSitesSection(
    sites: List<Site>,
    onSelect: (Site) -> Unit,
    onToggleFavorite: (Site) -> Unit,
    onDelete: (Site) -> Unit,
    compact: Boolean,
    useGrid: Boolean,
    enableSwipeDelete: Boolean,
    showDesktopDelete: Boolean,
) {
  val filteredSites = rememberFilteredSites(sites)

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
        text = "Instancias guardadas (${sites.size})",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (useGrid) {
      SavedSitesGrid(
          sites = filteredSites,
          onSelect = onSelect,
          onToggleFavorite = onToggleFavorite,
          onDelete = onDelete,
          showDesktopDelete = showDesktopDelete,
      )
    } else {
      SavedSitesList(
          sites = filteredSites,
          onSelect = onSelect,
          onToggleFavorite = onToggleFavorite,
          onDelete = onDelete,
          enableSwipeDelete = enableSwipeDelete,
          showDesktopDelete = showDesktopDelete,
      )
    }
    if (sites.isNotEmpty()) {
      LoginDivider()
    }
  }
}

@Composable
private fun rememberFilteredSites(sites: List<Site>): List<Site> {
  var query by remember { mutableStateOf("") }
  return remember(sites, query) {
    if (query.isBlank()) {
      sites
    } else {
      sites.filter {
        it.name.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
      }
    }
  }
}

@Composable
private fun SavedSitesList(
    sites: List<Site>,
    onSelect: (Site) -> Unit,
    onToggleFavorite: (Site) -> Unit,
    onDelete: (Site) -> Unit,
    enableSwipeDelete: Boolean,
    showDesktopDelete: Boolean,
) {
  LazyColumn(
      modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (sites.isEmpty()) {
      item { EmptySitesMessage() }
    } else {
      itemsIndexed(sites) { index, site ->
        StaggeredIn(index = index) {
          if (enableSwipeDelete) {
            SwipeToDeleteSiteRow(
                site = site,
                onClick = { onSelect(site) },
                onToggleFavorite = { onToggleFavorite(site) },
                onDelete = { onDelete(site) },
            )
          } else {
            SiteRow(
                site = site,
                onClick = { onSelect(site) },
                onToggleFavorite = { onToggleFavorite(site) },
                onDelete = if (showDesktopDelete) ({ onDelete(site) }) else null,
                showDeleteAction = showDesktopDelete,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SavedSitesGrid(
    sites: List<Site>,
    onSelect: (Site) -> Unit,
    onToggleFavorite: (Site) -> Unit,
    onDelete: (Site) -> Unit,
    showDesktopDelete: Boolean,
) {
  LazyVerticalGrid(
      modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
      columns = GridCells.Fixed(2),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (sites.isEmpty()) {
      item { EmptySitesMessage() }
    } else {
      itemsIndexed(sites) { index, site ->
        StaggeredIn(index = index) {
          SiteRow(
              site = site,
              onClick = { onSelect(site) },
              onToggleFavorite = { onToggleFavorite(site) },
              onDelete = if (showDesktopDelete) ({ onDelete(site) }) else null,
              showDeleteAction = showDesktopDelete,
          )
        }
      }
    }
  }
}

@Composable
private fun EmptySitesMessage() {
  Text(
      text = "No hay coincidencias",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(vertical = 6.dp),
  )
}

@Composable
private fun LoginDivider() {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline,
    )
    Text(
        text = "o",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline,
    )
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SwipeToDeleteSiteRow(
    site: Site,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
  val dismissState =
      rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
              onDelete()
              true
            } else {
              false
            }
          },
          positionalThreshold = { distance -> distance * 0.35f },
      )
  val backgroundColor by
      animateColorAsState(
          targetValue =
              if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.errorContainer
              }
      )
  SwipeToDismissBox(
      state = dismissState,
      enableDismissFromStartToEnd = false,
      enableDismissFromEndToStart = true,
      backgroundContent = {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
                text = "Eliminar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onError,
            )
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
            )
          }
        }
      },
  ) {
    SiteRow(site = site, onClick = onClick, onToggleFavorite = onToggleFavorite)
  }
}

@Composable
private fun SiteRow(
    site: Site,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)? = null,
    showDeleteAction: Boolean = false,
    modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isHovered by interactionSource.collectIsHoveredAsState()
  val scale = if (isPressed) 0.98f else if (isHovered) 1.01f else 1f
  Card(
      onClick = onClick,
      interactionSource = interactionSource,
      modifier =
          modifier.fillMaxWidth().graphicsLayer {
            scaleX = scale
            scaleY = scale
          },
      shape = RoundedCornerShape(20.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = if (isHovered) 8.dp else 3.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(14.dp),
          ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
              Text(
                  text = site.name.take(2).uppercase(),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                site.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              site.lastUsedAt?.let { InfoBadge(text = formatLastSession(it)) }
              if (site.isFavorite) {
                FavoriteBadge(onClick = onToggleFavorite)
              }
            }
          }
        }
        SiteRowActions(
            site = site,
            onDelete = onDelete,
            onToggleFavorite = onToggleFavorite,
            showDeleteAction = showDeleteAction,
        )
      }
    }
  }
}

@Composable
private fun SiteRowActions(
    site: Site,
    onDelete: (() -> Unit)?,
    onToggleFavorite: () -> Unit,
    showDeleteAction: Boolean,
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (showDeleteAction && onDelete != null) {
      ActionChip(
          label = "Eliminar",
          icon = Icons.Outlined.DeleteOutline,
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
          onClick = onDelete,
      )
    }
    if (!site.isFavorite) {
      ActionChip(
          label = "Fav",
          icon = Icons.Outlined.StarBorder,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          onClick = onToggleFavorite,
      )
    }
    Icon(
        Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
  Surface(
      color = containerColor,
      shape = RoundedCornerShape(999.dp),
      modifier = Modifier.clickable(onClick = onClick),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
      Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(14.dp),
      )
      Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = contentColor,
      )
    }
  }
}

@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay((index * 50L).coerceAtMost(300L))
    visible = true
  }
  AnimatedVisibility(
      visible = visible,
      enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 6 },
      exit = fadeOut(tween(120)),
  ) {
    content()
  }
}

private fun formatLastSession(epochMillis: Long): String {
  val ldt =
      Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
  val day = ldt.date.day.toString().padStart(2, '0')
  val month = ldt.date.month.number.toString().padStart(2, '0')
  val hour = ldt.hour.toString().padStart(2, '0')
  val minute = ldt.minute.toString().padStart(2, '0')
  return "Última sesión: $day/$month ${hour}:$minute"
}

@Composable
private fun InfoBadge(text: String) {
  Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(999.dp)) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun FavoriteBadge(onClick: () -> Unit) {
  Surface(color = Color(0xFFFFF1C2), shape = RoundedCornerShape(999.dp)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
      Icon(
          imageVector = Icons.Filled.Star,
          contentDescription = null,
          tint = Color(0xFFF4C430),
          modifier = Modifier.size(12.dp),
      )
      Text(
          text = "Favorito",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF8A6D00),
      )
    }
  }
}
