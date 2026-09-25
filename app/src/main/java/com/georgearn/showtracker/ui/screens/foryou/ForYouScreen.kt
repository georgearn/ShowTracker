package com.georgearn.showtracker.ui.screens.foryou

import androidx.compose.foundation.background
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.ui.screens.common.PosterImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.repository.LengthPref
import com.georgearn.showtracker.data.repository.SuggestionMood
import com.georgearn.showtracker.ui.screens.common.ScoreRow
import com.georgearn.showtracker.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    onOpenDetail: (Int, String) -> Unit,
    viewModel: ForYouViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("What to Watch")
                    Text(
                        "Picked from your watchlist",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (state.stage) {
                RecStage.INTRO -> IntroStage(viewModel)
                RecStage.QUIZ -> QuizStepStage(state, viewModel)
                RecStage.SWIPE -> SwipeStage(state, viewModel, onOpenDetail)
            }
        }
    }
}

@Composable
private fun IntroStage(viewModel: ForYouViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    "Need help deciding?",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    "Take a quick 1-minute quiz to find the perfect movie or show from your saved watchlist, or let us drop a random pick for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = viewModel::startQuiz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Take Quick Quiz")
                }

                OutlinedButton(
                    onClick = viewModel::startFullyRandom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Drop Random Pick")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizStepStage(state: ForYouState, viewModel: ForYouViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousQuizStep) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous question")
            }
            Text(
                "Question ${state.quizStep + 1} of ${state.totalSteps}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LinearProgressIndicator(
            progress = { (state.quizStep + 1).toFloat() / state.totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(CircleShape)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state.quizStep) {
                    0 -> {
                        Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizChoiceOption("Anything (Open to whatever)", state.mood == SuggestionMood.ANYTHING) {
                                viewModel.setMood(SuggestionMood.ANYTHING)
                            }
                            QuizChoiceOption("Light & Fun (Comedy, Family, Fantasy)", state.mood == SuggestionMood.LIGHT) {
                                viewModel.setMood(SuggestionMood.LIGHT)
                            }
                            QuizChoiceOption("Intense & Thrilling (Action, Horror, Crime)", state.mood == SuggestionMood.INTENSE) {
                                viewModel.setMood(SuggestionMood.INTENSE)
                            }
                        }
                    }
                    1 -> {
                        Text("What type of content do you want?", style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizType.entries.forEach { t ->
                                QuizChoiceOption(t.label, state.quizType == t) {
                                    viewModel.setQuizType(t)
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("How much time do you have?", style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LengthPref.entries.forEach { l ->
                                QuizChoiceOption(l.label, state.length == l) {
                                    viewModel.setLength(l)
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("Any specific genres in mind?", style = MaterialTheme.typography.titleMedium)
                        Text("Genres of the released titles on your list. Pick any, or none for all.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.genreOptions.forEach { genre ->
                                FilterChip(
                                    selected = genre in state.selectedGenres,
                                    onClick = { viewModel.toggleGenre(genre) },
                                    label = { Text(genre) }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = viewModel::nextQuizStep,
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text(if (state.quizStep == state.totalSteps - 1) "Get Picks" else "Next")
            }
        }
    }
}

@Composable
private fun QuizChoiceOption(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SwipeStage(
    state: ForYouState,
    viewModel: ForYouViewModel,
    onOpenDetail: (Int, String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = viewModel::resetToIntro) { Text("Start over") }
        }

        val current = state.current
        when {
            state.queue.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nothing matching your picks yet - add more titles to your watchlist!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                    Button(onClick = viewModel::resetToIntro, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Back to Start")
                    }
                }
            }
            current != null -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SwipeableRecCard(
                    item = current,
                    onDecide = viewModel::decide,
                    onOpen = { onOpenDetail(current.tmdbId, current.mediaType) }
                )
                Text(
                    "Swipe right to keep, left to skip",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    RoundActionButton(Icons.Default.Close, label = "Skip", onClick = { viewModel.decide(false) })
                    RoundActionButton(Icons.Default.Check, label = "Keep", primary = true, onClick = { viewModel.decide(true) })
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("You're all caught up. Reshuffle for more picks from your watchlist.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = viewModel::reshuffle) { Text("Reshuffle") }
                    OutlinedButton(onClick = viewModel::resetToIntro) { Text("Start Over") }
                }
            }
        }
    }
}

@Composable
private fun RecCard(item: WatchlistEntity, modifier: Modifier = Modifier) {
    val isUpcoming = DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.UPCOMING
    val genres = item.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
    Card(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                PosterImage(
                    path = item.posterPath,
                    contentDescription = null,
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                )
                if (!item.imdbRating.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "★ ${item.imdbRating.removeSuffix("/10")}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                if (genres.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.forEach { genre ->
                            Text(
                                genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
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
private fun RoundActionButton(icon: ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(icon, contentDescription = label, tint = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Drag sideways past the threshold to decide; the card flies off, then the next one appears.
 * Tapping opens the detail page. The buttons below stay as the non-gesture alternative.
 */
@Composable
private fun SwipeableRecCard(item: WatchlistEntity, onDecide: (Boolean) -> Unit, onOpen: () -> Unit) {
    val offsetX = remember(item.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    Box(contentAlignment = Alignment.TopCenter) {
        RecCard(
            item,
            Modifier
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / 40f
                }
                .pointerInput(item.key) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val x = offsetX.value
                                if (abs(x) > threshold) {
                                    offsetX.animateTo(sign(x) * size.width * 1.5f, tween(durationMillis = 200))
                                    onDecide(x > 0)
                                } else {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, spring()) } },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        }
                    )
                }
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onOpen)
        )
        // Keep / Skip stamp fades in with the drag distance.
        val x = offsetX.value
        if (x != 0f) {
            val keep = x > 0
            Text(
                if (keep) "KEEP" else "SKIP",
                style = MaterialTheme.typography.titleLarge,
                color = if (keep) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .graphicsLayer { alpha = (abs(offsetX.value) / threshold).coerceIn(0f, 1f) }
                    .clip(MaterialTheme.shapes.small)
                    .background(if (keep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
