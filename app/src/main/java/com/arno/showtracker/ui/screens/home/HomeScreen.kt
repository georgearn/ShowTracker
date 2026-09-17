package com.arno.showtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.data.repository.imageUrl
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
    var tab by remember { mutableStateOf(0) }

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
                Text("Show Tracker", style = MaterialTheme.typography.headlineSmall)
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

        TabRow(selectedTabIndex = tab, modifier = Modifier.padding(horizontal = 20.dp)) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Upcoming") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("New Releases") })
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
            is UiState.Success -> if (tab == 0) {
                UpcomingTimeline(
                    items = s.data.upcoming,
                    notifyIds = notifyIds,
                    onOpenDetail = onOpenDetail,
                    onToggleNotify = viewModel::toggleNotify
                )
            } else {
                NewReleasesList(
                    items = s.data.justDropped,
                    savedIds = savedIds,
                    onOpenDetail = onOpenDetail,
                    onToggleWatchlist = viewModel::toggleWatchlist
                )
            }
        }
    }
}

/** Vertical timeline grouped by relative-date bucket (Today / This Week / Next Week / ...). */
@Composable
private fun UpcomingTimeline(
    items: List<MediaSummary>,
    notifyIds: Set<Int>,
    onOpenDetail: (Int, String) -> Unit,
    onToggleNotify: (MediaSummary) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHint("Nothing upcoming on your list yet.\nAdd titles from Discover to see them here.")
        return
    }

    val grouped = remember(items) {
        val out = LinkedHashMap<String, MutableList<MediaSummary>>()
        items.forEach { out.getOrPut(DateUtils.upcomingBucket(it.releaseDate)) { mutableListOf() }.add(it) }
        out
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        grouped.forEach { (bucket, bucketItems) ->
            item(key = "header_$bucket") {
                Text(
                    text = bucket,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                )
            }
            items(bucketItems, key = { "u${it.tmdbId}" }) { item ->
                TimelineRow(
                    item = item,
                    isLast = bucketItems.last() == item,
                    isNotifyOn = notifyIds.contains(item.tmdbId),
                    onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                    onToggleNotify = { onToggleNotify(item) }
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: MediaSummary,
    isLast: Boolean,
    isNotifyOn: Boolean,
    onClick: () -> Unit,
    onToggleNotify: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
    ) {
        // timeline rail: dot + connecting line
        Column(
            modifier = Modifier.width(20.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 10.dp, bottom = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl(item.posterPath),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(52.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f)
            ) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    DateUtils.formatForDisplay(item.releaseDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CountdownChip(DateUtils.countdownLabel(item.releaseDate))
            IconButton(onClick = onToggleNotify) {
                Icon(
                    if (isNotifyOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    contentDescription = "Notify on release",
                    tint = if (isNotifyOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CountdownChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** Recency-first list: poster, title, "released X ago" tag, TMDB score, quick-add. */
@Composable
private fun NewReleasesList(
    items: List<MediaSummary>,
    savedIds: Set<Int>,
    onOpenDetail: (Int, String) -> Unit,
    onToggleWatchlist: (MediaSummary) -> Unit
) {
    if (items.isEmpty()) {
        EmptyHint("Nothing new dropped recently.")
        return
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
        items(items, key = { "d${it.tmdbId}" }) { item ->
            NewReleaseRow(
                item = item,
                isSaved = savedIds.contains(item.tmdbId),
                onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                onToggleWatchlist = { onToggleWatchlist(item) }
            )
        }
    }
}

@Composable
private fun NewReleaseRow(
    item: MediaSummary,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = imageUrl(item.posterPath),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = DateUtils.agoLabel(item.releaseDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .weight(1f)
        ) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.tmdbVoteAverage > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "%.1f".format(item.tmdbVoteAverage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        IconButton(
            onClick = onToggleWatchlist,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                if (isSaved) Icons.Default.Check else Icons.Default.Add,
                contentDescription = "Add to watchlist",
                tint = if (isSaved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
