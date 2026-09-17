package com.georgearn.showtracker.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.georgearn.showtracker.data.model.MediaDetail
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.repository.imageUrl
import com.georgearn.showtracker.ui.screens.common.ScoreRow
import com.georgearn.showtracker.util.DateUtils
import com.georgearn.showtracker.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> Text(s.message, Modifier.align(Alignment.Center).padding(24.dp))
                is UiState.Success -> DetailsContent(s.data, isSaved, viewModel::toggleSaved, onBack)
            }
        }
    }
}

@Composable
private fun DetailsContent(
    detail: MediaDetail,
    isSaved: Boolean,
    onToggleSaved: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val isUpcoming = detail.releaseStatus == ReleaseStatus.UPCOMING

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            AsyncImage(
                model = imageUrl(detail.backdropPath ?: detail.posterPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                            startY = 0.3f * 780f
                        )
                    )
            )
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-48).dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(width = 108.dp, height = 162.dp)
            ) {
                AsyncImage(
                    model = imageUrl(detail.posterPath),
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 58.dp)
            ) {
                Text(detail.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${DateUtils.formatForDisplay(detail.releaseDate)}" +
                        (detail.runtimeMinutes?.let { " · ${it} min" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (detail.genres.isNotEmpty()) {
                    Text(
                        detail.genres.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Box(Modifier.padding(top = 10.dp)) {
                    ScoreRow(detail.imdbRating, detail.rottenTomatoesScore, detail.tmdbVoteAverage)
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offset(y = (-32).dp).padding(top = 8.dp)) {
        Button(
            onClick = { onToggleSaved(true) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Icon(
                if (isUpcoming) Icons.Default.NotificationsActive
                else if (isSaved) Icons.Default.BookmarkRemove
                else Icons.Default.BookmarkAdd,
                contentDescription = null
            )
            Text(
                if (isUpcoming) {
                    if (isSaved) "  Notification Set ✓" else "  Notify Me on Release"
                } else {
                    if (isSaved) "  In Watchlist ✓" else "  Add to Watchlist"
                }
            )
        }

        Text(
            "Synopsis",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
        )
        Text(detail.synopsis, style = MaterialTheme.typography.bodyLarge)

        if (detail.watchProviders.isNotEmpty()) {
            Text(
                "Where to watch (${detail.watchProvidersRegion})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                detail.watchProviders.distinctBy { it.providerName }.forEach { p ->
                    FilterChip(selected = false, onClick = {}, label = { Text(p.providerName) })
                }
            }
        } else if (!isUpcoming) {
            Text(
                "No streaming/rental info available for your region yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 32.dp))
        }
    }
}
