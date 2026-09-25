package com.georgearn.showtracker.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.PosterImage
import com.georgearn.showtracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    LaunchedEffect(alerts) {
        if (alerts != null) viewModel.markSeen()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        val current = alerts
        when {
            current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            current.outNow.isEmpty() && current.comingUp.isEmpty() -> EmptyState(
                icon = Icons.Outlined.NotificationsNone,
                title = "No release alerts",
                body = "Tap the bell on any upcoming title and it shows up here, then again when it's out.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                alertSection("Out now", "out", current.outNow, released = true, onOpenDetail)
                alertSection("Coming up", "soon", current.comingUp, released = false, onOpenDetail)
            }
        }
    }
}

private fun LazyListScope.alertSection(
    title: String,
    keyPrefix: String,
    items: List<WatchlistEntity>,
    released: Boolean,
    onOpenDetail: (Int, String) -> Unit
) {
    if (items.isEmpty()) return
    item(key = "header_$keyPrefix") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() }
        )
    }
    items(items, key = { "$keyPrefix${it.key}" }) { item ->
        NotificationRow(item, released, onOpenDetail)
    }
}

@Composable
private fun NotificationRow(item: WatchlistEntity, released: Boolean, onOpenDetail: (Int, String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .clickable { onOpenDetail(item.tmdbId, item.mediaType) }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PosterImage(
                path = item.posterPath,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(44.dp).aspectRatio(2f / 3f)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    releaseLine(item, released),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun releaseLine(item: WatchlistEntity, released: Boolean): String {
    val date = DateUtils.formatForDisplay(item.releaseDate)
    if (released) {
        return when (DateUtils.daysSince(item.releaseDate)) {
            0L -> "Out today"
            1L -> "Out since yesterday"
            null -> "Out now"
            else -> "Out since $date"
        }
    }
    val days = DateUtils.daysUntil(item.releaseDate) ?: return "Release date TBA"
    return when (days) {
        0L -> "Releases today"
        1L -> "Releases tomorrow"
        else -> "Releases in $days days · $date"
    }
}
