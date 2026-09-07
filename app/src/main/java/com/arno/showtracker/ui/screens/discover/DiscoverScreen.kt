package com.arno.showtracker.ui.screens.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.ui.screens.common.OverlayIcon
import com.arno.showtracker.ui.screens.common.PosterOverlayCard

@Composable
fun DiscoverScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Discover", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search titles") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DiscoverFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { viewModel.onFilterChange(f) },
                    label = { Text(f.label) }
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.TopCenter))
                query.isBlank() -> Text(
                    "Find something new, then handpick it into your list.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                results.isEmpty() -> Text("No matches for \"$query\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.tmdbId }) { item ->
                        PosterOverlayCard(
                            item = item,
                            isOn = savedIds.contains(item.tmdbId),
                            overlayIcon = OverlayIcon.ADD,
                            caption = if (item.tmdbVoteAverage > 0) "TMDB %.1f".format(item.tmdbVoteAverage) else null,
                            onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
                            onOverlayClick = { viewModel.toggleWatchlist(item) }
                        )
                    }
                }
            }
        }
    }
}
