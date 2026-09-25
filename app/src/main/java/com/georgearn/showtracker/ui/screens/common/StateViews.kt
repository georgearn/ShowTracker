package com.georgearn.showtracker.ui.screens.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Grid cell width shared by every poster grid, so phones get 2-3 columns and tablets more. */
val PosterGridCells = GridCells.Adaptive(minSize = 140.dp)

@Composable
private fun pulseAlpha(): () -> Float {
    val transition = rememberInfiniteTransition(label = "placeholder")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "placeholderAlpha"
    )
    return { alpha }
}

@Composable
private fun PlaceholderBlock(modifier: Modifier, alphaProvider: () -> Float) {
    Box(
        modifier
            .graphicsLayer { alpha = alphaProvider() }
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}

/** Skeleton rows shown on the first load instead of a lone spinner. */
@Composable
fun PlaceholderList(modifier: Modifier = Modifier, rows: Int = 6) {
    val alpha = pulseAlpha()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaceholderBlock(Modifier.width(64.dp).aspectRatio(2f / 3f), alpha)
                Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceholderBlock(Modifier.fillMaxWidth(0.7f).height(16.dp), alpha)
                    PlaceholderBlock(Modifier.fillMaxWidth(0.4f).height(12.dp), alpha)
                }
            }
        }
    }
}

@Composable
fun PlaceholderGrid(modifier: Modifier = Modifier, cells: Int = 6) {
    val alpha = pulseAlpha()
    LazyVerticalGrid(
        columns = PosterGridCells,
        modifier = modifier.semantics { contentDescription = "Loading" },
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(cells) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PlaceholderBlock(Modifier.fillMaxWidth().aspectRatio(2f / 3f), alpha)
                PlaceholderBlock(Modifier.fillMaxWidth(0.8f).height(14.dp), alpha)
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (body != null) {
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (action != null) {
            Box(Modifier.padding(top = 20.dp)) { action() }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        title = "Something went wrong",
        body = message,
        modifier = modifier,
        action = { Button(onClick = onRetry) { Text("Retry") } }
    )
}
