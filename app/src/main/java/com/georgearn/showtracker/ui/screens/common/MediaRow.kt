package com.georgearn.showtracker.ui.screens.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Shared list-row anatomy: 64dp poster, title + caller-supplied details, trailing controls.
 * Screens differ in what goes in [details] and [trailing], not in layout.
 */
@Composable
fun MediaRow(
    posterPath: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterOverlay: @Composable BoxScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    details: @Composable ColumnScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            PosterImage(
                path = posterPath,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(64.dp).aspectRatio(2f / 3f)
            )
            posterOverlay()
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            details()
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            trailing()
        }
    }
}

/** Secondary line under a [MediaRow] title (date, countdown). */
@Composable
fun MediaRowCaption(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The one toggle style for row actions (save, watched, release alert). */
@Composable
fun RowToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    onIcon: ImageVector,
    offIcon: ImageVector,
    description: String
) {
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        Icon(if (checked) onIcon else offIcon, contentDescription = null)
    }
}
