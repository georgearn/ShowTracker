package com.georgearn.showtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.key
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.ErrorState
import com.georgearn.showtracker.ui.screens.common.PlaceholderList
import com.georgearn.showtracker.ui.screens.common.PosterImage
import com.georgearn.showtracker.ui.screens.common.rememberNotificationPermissionGate
import com.georgearn.showtracker.util.DateUtils
import kotlinx.coroutines.launch

private val homeTabs = listOf("New Releases", "Upcoming")

@OptIn(ExperimentalMaterial3Api::class)
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
    val savedKeys by viewModel.savedKeys.collectAsStateWithLifecycle()
    val notifyKeys by viewModel.notifyKeys.collectAsStateWithLifecycle()
    val hasUnseenAlerts by viewModel.hasUnseenAlerts.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { homeTabs.size })
    val scope = rememberCoroutineScope()
    val withPermission = rememberNotificationPermissionGate()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Show Tracker") },
            actions = {
                IconButton(onClick = onOpenNotifications) {
                    BadgedBox(badge = { if (hasUnseenAlerts) Badge() }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = if (hasUnseenAlerts) "Notifications, new alerts" else "Notifications"
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            homeTabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        when {
            state.isLoading -> PlaceholderList()
            state.error != null -> ErrorState(state.error.orEmpty(), onRetry = viewModel::retry)
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    if (page == 0) {
                        NewReleasesList(
                            items = state.justDropped,
                            savedKeys = savedKeys,
                            onOpenDetail = onOpenDetail,
                            onToggleWatchlist = viewModel::toggleWatchlist,
                            onSeeAll = onOpenJustDropped
                        )
                    } else {
                        UpcomingTimeline(
                            items = state.upcoming,
                            notifyKeys = notifyKeys,
                            onOpenDetail = onOpenDetail,
                            onToggleNotify = { item ->
                                if (item.key in notifyKeys) viewModel.toggleNotify(item)
                                else withPermission { viewModel.toggleNotify(item) }
                            },
                            onSeeAll = onOpenUpcoming
                        )
                    }
                }
            }
        }
    }
}

/** Empty state inside a scrollable, so pull-to-refresh still works when the list is empty. */
@Composable
private fun RefreshableEmpty(icon: ImageVector, title: String, body: String) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { EmptyState(icon = icon, title = title, body = body, modifier = Modifier.fillParentMaxSize()) }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        TextButton(onClick = onSeeAll) { Text("See all") }
    }
}

/** Vertical timeline grouped by relative-date bucket (Today / This Week / Next Week / ...). */
@Composable
private fun UpcomingTimeline(
    items: List<MediaSummary>,
    notifyKeys: Set<String>,
    onOpenDetail: (Int, String) -> Unit,
    onToggleNotify: (MediaSummary) -> Unit,
    onSeeAll: () -> Unit
) {
    if (items.isEmpty()) {
        RefreshableEmpty(
            icon = Icons.Outlined.EventBusy,
            title = "Nothing announced yet",
            body = "Pull down to refresh, or check your genre and country filters in Settings."
        )
        return
    }

    val grouped = remember(items) {
        val out = LinkedHashMap<String, MutableList<MediaSummary>>()
        items.forEach { out.getOrPut(DateUtils.upcomingBucket(it.releaseDate)) { mutableListOf() }.add(it) }
        out
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item(key = "see_all") { SectionHeader("Releasing soon", onSeeAll) }
        grouped.forEach { (bucket, bucketItems) ->
            item(key = "header_$bucket") {
                Text(
                    text = bucket,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .semantics { heading() }
                )
            }
            items(bucketItems, key = { "u${it.key}" }) { item ->
                TimelineRow(
                    item = item,
                    isLast = bucketItems.last() == item,
                    isNotifyOn = item.key in notifyKeys,
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
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
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
                .padding(start = 12.dp, bottom = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterImage(
                path = item.posterPath,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(52.dp).aspectRatio(2f / 3f)
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
            IconToggleButton(
                checked = isNotifyOn,
                onCheckedChange = { onToggleNotify() },
                modifier = Modifier.semantics { contentDescription = "Release alert for ${item.title}" }
            ) {
                Icon(
                    if (isNotifyOn) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                    contentDescription = null,
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
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** Recency-first list: poster, title, "released X ago" tag, TMDB score, quick-add. */
@Composable
private fun NewReleasesList(
    items: List<MediaSummary>,
    savedKeys: Set<String>,
    onOpenDetail: (Int, String) -> Unit,
    onToggleWatchlist: (MediaSummary) -> Unit,
    onSeeAll: () -> Unit
) {
    if (items.isEmpty()) {
        RefreshableEmpty(
            icon = Icons.Outlined.NewReleases,
            title = "Nothing new dropped recently",
            body = "Pull down to refresh, or check your genre and country filters in Settings."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "see_all") { SectionHeader("Released in the last 30 days", onSeeAll) }
        items(items, key = { "d${it.key}" }) { item ->
            NewReleaseRow(
                item = item,
                isSaved = item.key in savedKeys,
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
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            PosterImage(
                path = item.posterPath,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(64.dp).aspectRatio(2f / 3f)
            )
            Text(
                text = DateUtils.agoLabel(item.releaseDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
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
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .semantics { contentDescription = "TMDB rating %.1f".format(item.tmdbVoteAverage) }
                    )
                }
            }
        }
        FilledTonalIconToggleButton(
            checked = isSaved,
            onCheckedChange = { onToggleWatchlist() },
            modifier = Modifier.semantics { contentDescription = "Watchlist: ${item.title}" }
        ) {
            Icon(if (isSaved) Icons.Default.Check else Icons.Default.Add, contentDescription = null)
        }
    }
}
