package com.arno.showtracker.ui.screens.upcoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.arno.showtracker.util.DateUtils

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
    val notifyIds by viewModel.notifyIds.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Releasing Soon") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UpcomingFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { viewModel.setFilter(f) },
                        label = { Text(f.label) }
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(Modifier.align(Alignment.TopCenter))
                    sections.isEmpty() -> Text(
                        "Nothing upcoming right now.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sections.forEach { (window, items) ->
                            item {
                                Text(
                                    "${window.label} (${items.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                                )
                            }
                            items.chunked(2).forEach { row ->
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { media ->
                                            UpcomingCard(
                                                item = media,
                                                isOn = notifyIds.contains(media.tmdbId),
                                                onClick = { onOpenDetail(media.tmdbId, media.mediaType.apiValue) },
                                                onToggle = { viewModel.toggleNotify(media) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (row.size < 2) Box(Modifier.weight(1f))
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
private fun UpcomingCard(
    item: MediaSummary,
    isOn: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val days = DateUtils.daysUntil(item.releaseDate)
    PosterOverlayCard(
        item = item,
        isOn = isOn,
        overlayIcon = OverlayIcon.NOTIFY,
        caption = if (days != null) "in ${days}d · ${DateUtils.formatForDisplay(item.releaseDate)}" else DateUtils.formatForDisplay(item.releaseDate),
        onClick = onClick,
        onOverlayClick = onToggle,
        modifier = modifier
    )
}
