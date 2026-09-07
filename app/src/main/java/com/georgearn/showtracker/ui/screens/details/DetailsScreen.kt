package com.georgearn.showtracker.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.georgearn.showtracker.data.model.CastMember
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
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Error -> Text(s.message, Modifier.align(Alignment.Center).padding(24.dp))
                is UiState.Success -> DetailsContent(s.data, isSaved, viewModel::toggleSaved)
            }
        }
    }
}

@Composable
private fun DetailsContent(detail: MediaDetail, isSaved: Boolean, onToggleSaved: (Boolean) -> Unit) {
    val isUpcoming = detail.releaseStatus == ReleaseStatus.UPCOMING

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = imageUrl(detail.posterPath),
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
            )
            Column(Modifier.weight(2f)) {
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

        if (detail.cast.isNotEmpty()) {
            Text(
                "Cast",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                detail.cast.forEach { member -> CastMemberCard(member) }
            }
        }

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

@Composable
private fun CastMemberCard(member: CastMember) {
    Column(modifier = Modifier.width(80.dp)) {
        val photoUrl = imageUrl(member.profilePath, size = "w185")
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        Text(
            member.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (!member.character.isNullOrBlank()) {
            Text(
                member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
