package com.georgearn.showtracker.ui.screens.justdropped

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
import androidx.compose.material.icons.outlined.NewReleases
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
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.model.key
import com.georgearn.showtracker.ui.screens.common.EmptyState
import com.georgearn.showtracker.ui.screens.common.ErrorState
import com.georgearn.showtracker.ui.screens.common.OverlayIcon
import com.georgearn.showtracker.ui.screens.common.PlaceholderGrid
import com.georgearn.showtracker.ui.screens.common.PosterGridCells
import com.georgearn.showtracker.ui.screens.common.PosterOverlayCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JustDroppedScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: JustDroppedViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val genreFilter by viewModel.genreFilter.collectAsStateWithLifecycle()
    val availableGenres by viewModel.availableGenres.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val savedKeys by viewModel.savedKeys.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Just Dropped")
                    Text(
                        "Released in the last 30 days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(JustDroppedTypeFilter.entries) { f ->
                FilterChip(
                    selected = typeFilter == f,
                    onClick = { viewModel.setTypeFilter(f) },
                    label = { Text(f.label) }
                )
            }
        }
        if (availableGenres.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableGenres, key = { it.first }) { (id, name) ->
                    FilterChip(
                        selected = genreFilter == id,
                        onClick = { viewModel.setGenreFilter(id) },
                        label = { Text(name) }
                    )
                }
            }
        }

        when {
            isLoading -> PlaceholderGrid(Modifier.padding(horizontal = 16.dp))
            error != null -> ErrorState(error.orEmpty(), onRetry = { viewModel.load() })
            sections.isEmpty() -> EmptyState(
                icon = Icons.Outlined.NewReleases,
                title = "Nothing matches these filters",
                body = "Clear a filter to see more."
            )
            else -> LazyVerticalGrid(
                columns = PosterGridCells,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sections.forEach { (type, sectionItems) ->
                    item(key = "header_${type.name}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${if (type == MediaType.MOVIE) "Movies" else "Series"} (${sectionItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .semantics { heading() }
                        )
                    }
                    items(sectionItems, key = { it.key }) { media ->
                        PosterOverlayCard(
                            item = media,
                            isOn = media.key in savedKeys,
                            overlayIcon = OverlayIcon.ADD,
                            caption = scoreCaption(media),
                            onClick = { onOpenDetail(media.tmdbId, media.mediaType.apiValue) },
                            onOverlayClick = { viewModel.toggleWatchlist(media) }
                        )
                    }
                }
            }
        }
    }
}

private fun scoreCaption(item: MediaSummary): String? = listOfNotNull(
    item.imdbRating?.let { "IMDb ${it.removeSuffix("/10")}" },
    item.rottenTomatoesScore?.let { "RT $it" }
).joinToString(" · ").ifBlank { null }
