package com.arno.showtracker.ui.screens.justdropped

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.ui.screens.common.OverlayIcon
import com.arno.showtracker.ui.screens.common.PosterOverlayCard

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
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Just Dropped") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                "Released in the last 30 days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JustDroppedTypeFilter.entries.forEach { f ->
                    FilterChip(
                        selected = typeFilter == f,
                        onClick = { viewModel.setTypeFilter(f) },
                        label = { Text(f.label) }
                    )
                }
            }
            if (availableGenres.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
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

            Box(Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(Modifier.align(Alignment.TopCenter))
                    sections.isEmpty() -> Text(
                        "Nothing matches these filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sections.forEach { (type, sectionItems) ->
                            item {
                                Text(
                                    "${if (type.apiValue == "movie") "Movies" else "Series"} (${sectionItems.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                                )
                            }
                            sectionItems.chunked(3).forEach { row ->
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        row.forEach { media -> JustDroppedCard(media, savedIds.contains(media.tmdbId), onOpenDetail, viewModel, Modifier.weight(1f)) }
                                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JustDroppedCard(
    item: MediaSummary,
    isSaved: Boolean,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: JustDroppedViewModel,
    modifier: Modifier = Modifier
) {
    val scoreCaption = listOfNotNull(
        item.imdbRating?.let { "IMDb ${it.removeSuffix("/10")}" },
        item.rottenTomatoesScore?.let { "RT $it" }
    ).joinToString(" · ").ifBlank { null }
    PosterOverlayCard(
        item = item,
        isOn = isSaved,
        overlayIcon = OverlayIcon.ADD,
        caption = scoreCaption,
        onClick = { onOpenDetail(item.tmdbId, item.mediaType.apiValue) },
        onOverlayClick = { viewModel.toggleWatchlist(item) },
        modifier = modifier
    )
}
