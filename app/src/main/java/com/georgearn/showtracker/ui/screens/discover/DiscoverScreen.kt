package com.georgearn.showtracker.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedKeys by viewModel.savedKeys.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()

    // Hide the keyboard as soon as the user starts browsing results.
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) keyboard?.hide()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Discover") }, windowInsets = WindowInsets(0, 0, 0, 0))

        Column(Modifier.padding(horizontal = 16.dp)) {
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search movies & series") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(
                contentPadding = PaddingValues(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DiscoverFilter.entries) { f ->
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { viewModel.onFilterChange(f) },
                        label = { Text(f.label) }
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            val visible = state.visibleItems
            when {
                state.showingTrending && state.isLoadingTrending -> PlaceholderGrid(Modifier.padding(horizontal = 16.dp))
                !state.showingTrending && state.searchFailed -> ErrorState(
                    message = "Search didn't go through. Check your connection.",
                    onRetry = viewModel::retrySearch
                )
                !state.showingTrending && state.isSearching && visible.isEmpty() -> PlaceholderGrid(Modifier.padding(horizontal = 16.dp))
                visible.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = if (state.showingTrending) "Nothing trending right now" else "No matches for \"${state.query}\"",
                    body = if (state.showingTrending) null else "Try another spelling or switch the filter."
                )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = PosterGridCells,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.showingTrending) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "trending_header") {
                            Text(
                                "Trending this week",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
                        }
                    }
                    items(visible, key = { it.key }) { item ->
                        PosterOverlayCard(
                            item = item,
                            isOn = item.key in savedKeys,
                            overlayIcon = OverlayIcon.ADD,
                            caption = if (item.tmdbVoteAverage > 0) "TMDB %.1f".format(item.tmdbVoteAverage) else null,
                            onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                            onOverlayClick = { viewModel.toggleWatchlist(item) }
                        )
                    }
                }
            }
            // Keep the old results on screen while a new search runs.
            if (state.isSearching && visible.isNotEmpty()) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
