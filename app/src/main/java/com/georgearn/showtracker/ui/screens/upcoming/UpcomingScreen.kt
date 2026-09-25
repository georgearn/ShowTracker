package com.georgearn.showtracker.ui.screens.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.model.key
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.ErrorState
import com.georgearn.showtracker.ui.screens.common.OverlayIcon
import com.georgearn.showtracker.ui.screens.common.PlaceholderGrid
import com.georgearn.showtracker.ui.screens.common.PosterGridCells
import com.georgearn.showtracker.ui.screens.common.PosterOverlayCard
import com.georgearn.showtracker.ui.screens.common.rememberNotificationPermissionGate
import com.georgearn.showtracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val notifyKeys by viewModel.notifyKeys.collectAsStateWithLifecycle()
    val withPermission = rememberNotificationPermissionGate()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Releasing Soon") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(UpcomingFilter.entries) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.setFilter(f) },
                    label = { Text(f.label) }
                )
            }
        }

        when {
            isLoading -> PlaceholderGrid(Modifier.padding(horizontal = 16.dp))
            error != null -> ErrorState(error.orEmpty(), onRetry = { viewModel.load() })
            sections.isEmpty() -> EmptyState(
                icon = Icons.Outlined.EventBusy,
                title = "Nothing upcoming right now",
                body = "Try another filter, or check your genre and country filters in Settings."
            )
            else -> LazyVerticalGrid(
                columns = PosterGridCells,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { (window, windowItems) ->
                    item(key = "header_${window.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${window.label} (${windowItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .semantics { heading() }
                        )
                    }
                    items(windowItems, key = { it.key }) { media ->
                        val isOn = media.key in notifyKeys
                        val days = DateUtils.daysUntil(media.releaseDate)
                        PosterOverlayCard(
                            item = media,
                            isOn = isOn,
                            overlayIcon = OverlayIcon.NOTIFY,
                            caption = if (days != null) "${DateUtils.countdownLabel(media.releaseDate)} · ${DateUtils.formatForDisplay(media.releaseDate)}"
                            else DateUtils.formatForDisplay(media.releaseDate),
                            onClick = { onOpenDetail(media.tmdbId, media.mediaType.apiValue) },
                            onOverlayClick = {
                                if (isOn) viewModel.toggleNotify(media) else withPermission { viewModel.toggleNotify(media) }
                            }
                        )
                    }
                }
            }
        }
    }
}
