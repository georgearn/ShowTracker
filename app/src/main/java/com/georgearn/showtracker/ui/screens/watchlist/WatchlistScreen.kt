package com.georgearn.showtracker.ui.screens.watchlist

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import com.georgearn.showtracker.ui.screens.common.GroupHeader
import com.georgearn.showtracker.ui.screens.common.MediaRow
import com.georgearn.showtracker.ui.screens.common.MediaRowCaption
import com.georgearn.showtracker.ui.screens.common.Pill
import com.georgearn.showtracker.ui.screens.common.PillTone
import com.georgearn.showtracker.ui.screens.common.RootTopBar
import com.georgearn.showtracker.ui.screens.common.RowToggle
import com.georgearn.showtracker.ui.screens.common.SectionHeader
import androidx.compose.material3.Tab
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.PosterImage
import com.georgearn.showtracker.ui.screens.common.ScoreRow
import com.georgearn.showtracker.ui.screens.common.rememberNotificationPermissionGate
import com.georgearn.showtracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val withPermission = rememberNotificationPermissionGate()
    val actions = remember(viewModel) {
        RowActions(
            open = { item -> onOpenDetail(item.tmdbId, item.mediaType) },
            toggleWatched = viewModel::toggleWatched,
            remove = viewModel::remove,
            toggleNotify = { item ->
                if (item.notifyOnRelease) viewModel.toggleNotify(item) else withPermission { viewModel.toggleNotify(item) }
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        val count = state.readyToWatch.count + state.waitingOnRelease.count + state.history.count
        RootTopBar(
            title = "My Watchlist",
            subtitle = "$count saved",
            actions = { OrganizationMenu(state.organization, viewModel::setOrganization) }
        )

        PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
            WatchlistTab.entries.forEach { t ->
                Tab(
                    selected = state.tab == t,
                    onClick = { viewModel.setTab(t) },
                    text = { Text(if (t == WatchlistTab.HISTORY) "${t.label} (${state.history.count})" else t.label) }
                )
            }
        }

        val isEmpty = when (state.tab) {
            WatchlistTab.LIST -> state.readyToWatch.count == 0 && state.waitingOnRelease.count == 0
            WatchlistTab.HISTORY -> state.history.count == 0
        }

        if (isEmpty) {
            when (state.tab) {
                WatchlistTab.LIST -> EmptyState(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = "Nothing saved yet",
                    body = "Tap + on any poster in Home or Discover to save it here."
                )
                WatchlistTab.HISTORY -> EmptyState(
                    icon = Icons.Outlined.History,
                    title = "Nothing watched yet",
                    body = "Swipe a title right, or tap its check mark, once you've watched it."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (state.tab) {
                    WatchlistTab.LIST -> {
                        section("r", state.readyToWatch, actions)
                        section("w", state.waitingOnRelease, actions)
                    }
                    WatchlistTab.HISTORY -> section("h", state.history, actions)
                }
            }
        }
    }
}

private class RowActions(
    val open: (WatchlistEntity) -> Unit,
    val toggleWatched: (WatchlistEntity) -> Unit,
    val remove: (WatchlistEntity) -> Unit,
    val toggleNotify: (WatchlistEntity) -> Unit
)

@Composable
private fun OrganizationMenu(current: WatchlistOrganization, onSelect: (WatchlistOrganization) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Group list")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            WatchlistOrganization.entries.forEach { org ->
                DropdownMenuItem(
                    text = { Text(org.label) },
                    onClick = {
                        onSelect(org)
                        open = false
                    },
                    leadingIcon = {
                        if (org == current) Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
            }
        }
    }
}

private fun LazyListScope.section(
    keyPrefix: String,
    section: WatchlistSection,
    actions: RowActions
) {
    if (section.count == 0) return
    item(key = "section_$keyPrefix") { SectionHeader("${section.title} (${section.count})") }
    section.groups.forEach { group ->
        if (group.title != null) {
            item(key = "group_${keyPrefix}_${group.title}") { GroupHeader(group.title) }
        }
        items(group.items, key = { "$keyPrefix${it.key}" }) { item ->
            SwipeableWatchlistRow(item, actions, Modifier.animateItem())
        }
    }
}

/** Swipe left removes (with Undo), swipe right marks watched - released titles only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableWatchlistRow(item: WatchlistEntity, actions: RowActions, modifier: Modifier = Modifier) {
    val isReleased = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.RELEASED
    val dismissState = rememberSwipeToDismissBoxState(
        // Require a deliberate swipe: past half the row width before an action fires.
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    actions.remove(item)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    actions.toggleWatched(item)
                    false // the row stays; it just moves between sections
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = isReleased,
        modifier = modifier,
        backgroundContent = {
            val towardsEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val container = if (towardsEnd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            val content = if (towardsEnd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(container)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (towardsEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (towardsEnd) Icons.Default.CheckCircle else Icons.Default.Delete,
                    contentDescription = null,
                    tint = content
                )
            }
        }
    ) {
        WatchlistRow(item, isReleased, actions)
    }
}

@Composable
private fun WatchlistRow(item: WatchlistEntity, isReleased: Boolean, actions: RowActions) {
    MediaRow(
        posterPath = item.posterPath,
        title = item.title,
        onClick = { actions.open(item) },
        // Opaque so the swipe background only shows where the row has moved away.
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Remove from watchlist") {
                        actions.remove(item)
                        true
                    }
                )
            },
        trailing = {
            if (isReleased) {
                RowToggle(
                    checked = item.watched,
                    onToggle = { actions.toggleWatched(item) },
                    onIcon = Icons.Default.Check,
                    offIcon = Icons.Outlined.CheckCircleOutline,
                    description = "Watched"
                )
            } else {
                RowToggle(
                    checked = item.notifyOnRelease,
                    onToggle = { actions.toggleNotify(item) },
                    onIcon = Icons.Default.NotificationsActive,
                    offIcon = Icons.Outlined.NotificationsNone,
                    description = "Release alert"
                )
            }
        }
    ) {
        MediaRowCaption(
            if (isReleased) DateUtils.formatForDisplay(item.releaseDate) else "Releases ${DateUtils.countdownLabel(item.releaseDate)}"
        )
        ScoreRow(item.imdbRating, item.rottenTomatoesScore, null)
        seasonChipText(item)?.let { Pill(it, tone = PillTone.ACCENT) }
    }
}

/** "S3 · Mar 12, 2026" for a followed series with a dated season, "S3 out now" for 30 days after. */
private fun seasonChipText(item: WatchlistEntity): String? {
    if (!item.followSeasons) return null
    val season = item.nextSeasonNumber ?: return null
    val date = item.nextSeasonAirDate ?: return "S$season · date TBA"
    return if (DateUtils.releaseStatus(date) == ReleaseStatus.RELEASED) {
        if ((DateUtils.daysSince(date) ?: Long.MAX_VALUE) <= 30) "S$season out now" else null
    } else {
        "S$season · ${DateUtils.formatForDisplay(date)}"
    }
}

