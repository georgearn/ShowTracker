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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
                onToggleWatchlist = viewModel::toggleWatchlist
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
    onToggleWatchlist: (MediaSummary) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        if (data.upcoming.isNotEmpty()) {
            Column {
                Text(
                    "Releasing Soon",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.upcoming, key = { "u${it.tmdbId}" }) { item ->
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
                Text(
                    "Just Dropped",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(data.justDropped, key = { "d${it.tmdbId}" }) { item ->
                        PosterOverlayCard(
                            item = item,
                            isOn = savedIds.contains(item.tmdbId),
                            overlayIcon = OverlayIcon.ADD,
                            caption = null,
                            onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                            onOverlayClick = { onToggleWatchlist(item) }
                        )
                    }
                }
            }
        }
    }
}
