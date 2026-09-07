package com.arno.showtracker.ui.screens.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.repository.imageUrl
import com.arno.showtracker.ui.screens.common.ScoreRow
import com.arno.showtracker.util.DateUtils

@Composable
fun WatchlistScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("My Watchlist", style = MaterialTheme.typography.headlineSmall)
            val count = state.readyToWatch.size + state.waitingOnRelease.size + state.watched.size
            Text("$count saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.readyToWatch.isEmpty() && state.waitingOnRelease.isEmpty() && state.watched.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing saved yet. Add titles from Home or Discover.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SectionHeader("Ready to watch", state.readyToWatch.size) }
                items(state.readyToWatch, key = { "r${it.tmdbId}" }) { item ->
                    WatchlistRow(item, onOpenDetail, viewModel)
                }
                item { SectionHeader("Waiting on release", state.waitingOnRelease.size) }
                items(state.waitingOnRelease, key = { "w${it.tmdbId}" }) { item ->
                    WatchlistRow(item, onOpenDetail, viewModel)
                }
                item { SectionHeader("Already watched", state.watched.size) }
                items(state.watched, key = { "d${it.tmdbId}" }) { item ->
                    WatchlistRow(item, onOpenDetail, viewModel)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    if (count == 0) return
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun WatchlistRow(
    item: WatchlistEntity,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: WatchlistViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail(item.tmdbId, item.mediaType) }
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = imageUrl(item.posterPath),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    DateUtils.formatForDisplay(item.releaseDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ScoreRow(item.imdbRating, item.rottenTomatoesScore, null)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (DateUtils.releaseStatus(item.releaseDate).name == "UPCOMING") {
                    IconButton(onClick = { viewModel.toggleNotify(item) }) {
                        Icon(
                            if (item.notifyOnRelease) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Notify on release"
                        )
                    }
                } else {
                    IconButton(onClick = { viewModel.toggleWatched(item) }) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Mark watched",
                            tint = if (item.watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { viewModel.remove(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }
}
