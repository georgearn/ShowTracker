package com.georgearn.showtracker.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.repository.imageUrl

/** TMDB image with a crossfade, and a neutral placeholder icon when TMDB has no artwork. */
@Composable
fun PosterImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    size: String = "w500"
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val url = imageUrl(path, size)
        if (url == null) {
            Icon(
                Icons.Outlined.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.35f)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** Poster card with a round toggle in the corner (add-to-watchlist or notify-me), used in grids. */
@Composable
fun PosterOverlayCard(
    item: MediaSummary,
    isOn: Boolean,
    overlayIcon: OverlayIcon,
    caption: String?,
    onClick: () -> Unit,
    onOverlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            PosterImage(item.posterPath, contentDescription = null, modifier = Modifier.fillMaxSize())
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            OverlayToggle(
                isOn = isOn,
                overlayIcon = overlayIcon,
                onToggle = onOverlayClick,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
        )
    }
}

/** 32dp visual inside a full 48dp touch target, so the corner badge stays small but easy to hit. */
@Composable
private fun OverlayToggle(
    isOn: Boolean,
    overlayIcon: OverlayIcon,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (overlayIcon) {
        OverlayIcon.ADD -> "Watchlist"
        OverlayIcon.NOTIFY -> "Release alert"
    }
    val state = when (overlayIcon) {
        OverlayIcon.ADD -> if (isOn) "Saved" else "Not saved"
        OverlayIcon.NOTIFY -> if (isOn) "On" else "Off"
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .toggleable(
                value = isOn,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                role = Role.Switch,
                onValueChange = { onToggle() }
            )
            .semantics {
                contentDescription = label
                stateDescription = state
            },
        contentAlignment = Alignment.Center
    ) {
        val container = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
        val tint = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(container)
                .then(if (!isOn) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (overlayIcon) {
                OverlayIcon.ADD -> if (isOn) Icons.Default.Check else Icons.Default.Add
                OverlayIcon.NOTIFY -> if (isOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone
            }
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

enum class OverlayIcon { ADD, NOTIFY }
