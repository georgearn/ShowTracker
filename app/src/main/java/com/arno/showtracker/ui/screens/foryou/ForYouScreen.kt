package com.arno.showtracker.ui.screens.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.model.ReleaseStatus
import com.arno.showtracker.data.repository.LengthPref
import com.arno.showtracker.data.repository.SuggestionMood
import com.arno.showtracker.data.repository.imageUrl
import com.arno.showtracker.ui.screens.common.ScoreRow
import com.arno.showtracker.util.DateUtils

@Composable
fun ForYouScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: ForYouViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(Modifier.padding(top = 20.dp, bottom = 6.dp)) {
            Text("What to Watch", style = MaterialTheme.typography.headlineSmall)
            Text("Picked from your watchlist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (state.stage) {
            RecStage.QUIZ -> QuizStage(state, viewModel)
            RecStage.SWIPE -> SwipeStage(state, viewModel, onOpenDetail)
        }
    }
}

@Composable
private fun QuizStage(state: ForYouState, viewModel: ForYouViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuizQuestionCard(title = "Mood") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MoodOption("Anything", SuggestionMood.ANYTHING, state.mood, viewModel::setMood)
                MoodOption("Light", SuggestionMood.LIGHT, state.mood, viewModel::setMood)
                MoodOption("Intense", SuggestionMood.INTENSE, state.mood, viewModel::setMood)
            }
        }
        QuizQuestionCard(title = "Movie or series?") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuizType.entries.forEach { t ->
                    FilterChip(selected = state.quizType == t, onClick = { viewModel.setQuizType(t) }, label = { Text(t.label) })
                }
            }
        }
        QuizQuestionCard(title = "How long do you have?") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LengthPref.entries.forEach { l ->
                    FilterChip(selected = state.length == l, onClick = { viewModel.setLength(l) }, label = { Text(l.label) })
                }
            }
        }
        Button(onClick = viewModel::startRecs, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("Get My Picks")
        }
        OutlinedButton(onClick = viewModel::startFullyRandom, modifier = Modifier.fillMaxWidth()) {
            Text("Surprise Me (Full Random)")
        }
    }
}

@Composable
private fun QuizQuestionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
private fun MoodOption(label: String, mood: SuggestionMood, selected: SuggestionMood, onSelect: (SuggestionMood) -> Unit) {
    FilterChip(selected = selected == mood, onClick = { onSelect(mood) }, label = { Text(label) })
}

@Composable
private fun SwipeStage(
    state: ForYouState,
    viewModel: ForYouViewModel,
    onOpenDetail: (Int, String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinedButton(onClick = viewModel::retakeQuiz) { Text("Retake quiz") }
        }

        val current = state.current
        when {
            state.queue.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nothing to suggest yet - handpick some titles into your watchlist first.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            current != null -> Column(
                Modifier.fillMaxSize().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RecCard(current, Modifier.clickable { onOpenDetail(current.tmdbId, current.mediaType) })
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    RoundActionButton(Icons.Default.Close, onClick = { viewModel.decide(false) })
                    RoundActionButton(Icons.Default.Check, primary = true, onClick = { viewModel.decide(true) })
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("You're all caught up. Reshuffle for more picks from your watchlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = viewModel::reshuffle) { Text("Reshuffle") }
                }
            }
        }
    }
}

@Composable
private fun RecCard(item: WatchlistEntity, modifier: Modifier = Modifier) {
    val isUpcoming = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.UPCOMING
    Card(
        modifier = modifier.width(300.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = imageUrl(item.posterPath),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(0.dp))
            )
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                if (isUpcoming) {
                    Text("Releases ${DateUtils.formatForDisplay(item.releaseDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    ScoreRow(item.imdbRating, item.rottenTomatoesScore, null)
                }
                Text(item.overview, style = MaterialTheme.typography.bodySmall, maxLines = 4)
            }
        }
    }
}

@Composable
private fun RoundActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
