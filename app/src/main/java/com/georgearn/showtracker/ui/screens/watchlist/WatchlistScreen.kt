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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
        TopAppBar(
            title = {
                Column {
                    Text("My Watchlist")
                    Text(
                        "$count saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = { OrganizationMenu(state.organization, viewModel::setOrganization) },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            WatchlistTab.entries.forEachIndexed { index, t ->
                SegmentedButton(
                    selected = state.tab == t,
                    onClick = { viewModel.setTab(t) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = WatchlistTab.entries.size)
                ) {
                    Text(if (t == WatchlistTab.HISTORY) "${t.label} (${state.history.count})" else t.label)
                }
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
    item(key = "section_$keyPrefix") { SectionHeader(section.title, section.count) }
    section.groups.forEach { group ->
        if (group.title != null) {
            item(key = "group_${keyPrefix}_${group.title}") { GroupHeader(group.title) }
        }
        items(group.items, key = { "$keyPrefix${it.key}" }) { item ->
            SwipeableWatchlistRow(item, actions, Modifier.animateItem())
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

/** Swipe left removes (with Undo), swipe right marks watched - released titles only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableWatchlistRow(item: WatchlistEntity, actions: RowActions, modifier: Modifier = Modifier) {
    val isReleased = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.RELEASED
    val dismissState = rememberSwipeToDismissBoxState(
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
                    .clip(CardDefaults.shape)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .clickable { actions.open(item) }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Remove from watchlist") {
                        actions.remove(item)
                        true
                    }
                )
            }
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PosterImage(
                path = item.posterPath,
                contentDescription = null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(64.dp).aspectRatio(2f / 3f)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (isReleased) DateUtils.formatForDisplay(item.releaseDate) else "Releases ${DateUtils.countdownLabel(item.releaseDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ScoreRow(item.imdbRating, item.rottenTomatoesScore, null)
            }
            if (isReleased) {
                IconToggleButton(
                    checked = item.watched,
                    onCheckedChange = { actions.toggleWatched(item) },
                    modifier = Modifier.semantics { contentDescription = "Watched" }
                ) {
                    Icon(
                        if (item.watched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = if (item.watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconToggleButton(
                    checked = item.notifyOnRelease,
                    onCheckedChange = { actions.toggleNotify(item) },
                    modifier = Modifier.semantics { contentDescription = "Release alert" }
                ) {
                    Icon(
                        if (item.notifyOnRelease) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = if (item.notifyOnRelease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
