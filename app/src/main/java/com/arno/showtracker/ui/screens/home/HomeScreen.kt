package com.arno.showtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.ui.screens.common.OverlayIcon
import com.arno.showtracker.ui.screens.common.PosterOverlayCard
import com.arno.showtracker.util.DateUtils
import com.arno.showtracker.util.UiState

@Composable
fun HomeScreen(
    onOpenDetail: (Int, String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenJustDropped: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val notifyIds by viewModel.notifyIds.collectAsStateWithLifecycle()
    val hasNotifications by viewModel.hasNotifications.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text("Good evening", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("New Releases", style = MaterialTheme.typography.headlineSmall)
            }
            Row {
                Box {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    if (hasNotifications) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        when (val s = state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message)
                    Button(onClick = viewModel::load, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
            }
            is UiState.Success -> HomeContent(
                data = s.data,
                savedIds = savedIds,
                notifyIds = notifyIds,
                onOpenDetail = onOpenDetail,
                onToggleNotify = viewModel::toggleNotify,
                onToggleWatchlist = viewModel::toggleWatchlist,
                onOpenUpcoming = onOpenUpcoming,
                onOpenJustDropped = onOpenJustDropped
            )
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    savedIds: Set<Int>,
    notifyIds: Set<Int>,
    onOpenDetail: (Int, String) -> Unit,
    onToggleNotify: (MediaSummary) -> Unit,
    onToggleWatchlist: (MediaSummary) -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenJustDropped: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        if (data.upcoming.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Releasing Soon", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenUpcoming) { Text("See all") }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.upcoming.take(12), key = { "u${it.tmdbId}" }) { item ->
                        val days = DateUtils.daysUntil(item.releaseDate)
                        PosterOverlayCard(
                            item = item,
                            isOn = notifyIds.contains(item.tmdbId),
                            overlayIcon = OverlayIcon.NOTIFY,
                            caption = if (days != null) "in ${days}d" else null,
                            onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                            onOverlayClick = { onToggleNotify(item) },
                            modifier = Modifier.size(width = 110.dp, height = 190.dp)
                        )
                    }
                }
            }
        }

        if (data.justDropped.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Just Dropped", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenJustDropped) { Text("See all") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    data.justDropped.take(9).chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { item ->
                                val scoreCaption = listOfNotNull(
                                    item.imdbRating?.let { "IMDb ${it.removeSuffix("/10")}" },
                                    item.rottenTomatoesScore?.let { "RT $it" }
                                ).joinToString(" · ").ifBlank { null }
                                PosterOverlayCard(
                                    item = item,
                                    isOn = savedIds.contains(item.tmdbId),
                                    overlayIcon = OverlayIcon.ADD,
                                    caption = scoreCaption,
                                    onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                                    onOverlayClick = { onToggleWatchlist(item) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
