package com.georgearn.showtracker.ui.screens.details

import android.content.Intent
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.model.CastMember
import com.georgearn.showtracker.data.model.MediaDetail
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.ProviderKind
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.model.WatchProvider
import com.georgearn.showtracker.data.model.key
import com.georgearn.showtracker.ui.screens.common.ErrorState
import com.georgearn.showtracker.ui.screens.common.PosterImage
import com.georgearn.showtracker.ui.screens.common.ScoreRow
import com.georgearn.showtracker.ui.screens.common.rememberNotificationPermissionGate
import com.georgearn.showtracker.util.DateUtils
import com.georgearn.showtracker.util.UiState

private val HeroHeight = 240.dp

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val withPermission = rememberNotificationPermissionGate()

    when (val s = state) {
        is UiState.Loading -> Column(Modifier.fillMaxSize()) {
            DetailsTopBar(title = null, overImage = false, onBack = onBack, onShare = null)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is UiState.Error -> Column(Modifier.fillMaxSize()) {
            DetailsTopBar(title = null, overImage = false, onBack = onBack, onShare = null)
            ErrorState(s.message, onRetry = viewModel::load)
        }
        is UiState.Success -> DetailsContent(
            detail = s.data,
            entry = entry,
            onBack = onBack,
            onOpenDetail = onOpenDetail,
            onToggleSaved = viewModel::toggleSaved,
            onToggleWatched = viewModel::toggleWatched,
            onToggleNotify = { enable ->
                if (enable) withPermission { viewModel.setNotify(true) } else viewModel.setNotify(false)
            },
            onToggleFollowSeasons = { enable ->
                if (enable) withPermission { viewModel.setFollowSeasons(true) } else viewModel.setFollowSeasons(false)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    title: String?,
    overImage: Boolean,
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val container by animateColorAsState(
        if (overImage) Color.Transparent else MaterialTheme.colorScheme.surface,
        label = "topBarContainer"
    )
    // Over artwork the icons sit on a small scrim so they stay legible on any image.
    val iconColors = if (overImage) {
        IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
            contentColor = Color.White
        )
    } else {
        IconButtonDefaults.iconButtonColors()
    }
    TopAppBar(
        title = {
            if (title != null) Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            IconButton(onClick = onBack, colors = iconColors) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            if (onShare != null) {
                IconButton(onClick = onShare, colors = iconColors) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = container),
        modifier = modifier
    )
}

@Composable
private fun DetailsContent(
    detail: MediaDetail,
    entry: WatchlistEntity?,
    onBack: () -> Unit,
    onOpenDetail: (Int, String) -> Unit,
    onToggleSaved: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleNotify: (Boolean) -> Unit,
    onToggleFollowSeasons: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val collapsed by remember {
        derivedStateOf { scrollState.value > with(density) { (HeroHeight - 64.dp).toPx() } }
    }
    val tmdbUrl = "https://www.themoviedb.org/${detail.mediaType.apiValue}/${detail.tmdbId}"

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            DetailsHero(detail)

            Column(Modifier.padding(horizontal = 16.dp)) {
                DetailsActions(
                    detail = detail,
                    entry = entry,
                    onToggleSaved = onToggleSaved,
                    onToggleWatched = onToggleWatched,
                    onToggleNotify = onToggleNotify
                )

                if (detail.canFollowSeasons) {
                    SeasonAlertCard(
                        detail = detail,
                        following = entry?.followSeasons == true,
                        onToggle = onToggleFollowSeasons,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                detail.trailerYoutubeKey?.let { key ->
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://www.youtube.com/watch?v=$key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Watch trailer")
                    }
                }

                SectionTitle("Synopsis")
                ExpandableText(detail.synopsis)

                if (detail.cast.isNotEmpty()) {
                    SectionTitle("Cast")
                }
            }

            if (detail.cast.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(detail.cast) { member -> CastMemberCard(member) }
                }
            }

            WhereToWatch(
                detail = detail,
                onOpenAll = { uriHandler.openUri("$tmdbUrl/watch?locale=${detail.watchProvidersRegion}") }
            )

            if (detail.recommendations.isNotEmpty()) {
                SectionTitle("More like this", Modifier.padding(horizontal = 16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(detail.recommendations, key = { it.key }) { item ->
                        RecommendationCard(item) { onOpenDetail(item.tmdbId, item.mediaType.apiValue) }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        DetailsTopBar(
            title = if (collapsed) detail.title else null,
            overImage = !collapsed,
            onBack = onBack,
            onShare = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${detail.title} - $tmdbUrl")
                }
                context.startActivity(Intent.createChooser(send, null))
            }
        )
    }
}

@Composable
private fun DetailsHero(detail: MediaDetail) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backdropHeight = HeroHeight + statusBarTop
    val heroShape = MaterialTheme.shapes.extraLarge.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
    val scrim = MaterialTheme.colorScheme.scrim

    Box(Modifier.fillMaxWidth()) {
        PosterImage(
            path = detail.backdropPath ?: detail.posterPath,
            contentDescription = null,
            shape = heroShape,
            size = "w780",
            modifier = Modifier
                .fillMaxWidth()
                .height(backdropHeight)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(backdropHeight)
                .background(
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = 0.35f),
                        0.3f to Color.Transparent,
                        1f to scrim.copy(alpha = 0.55f)
                    ),
                    heroShape
                )
        )
        // Poster overlaps the bottom of the backdrop; padding (not offset) so no blank gap is left below.
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = backdropHeight - 56.dp)
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.size(width = 108.dp, height = 162.dp)
            ) {
                PosterImage(detail.posterPath, contentDescription = null, shape = RectangleShape, modifier = Modifier.fillMaxSize())
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(top = 64.dp)
            ) {
                Text(detail.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text(
                    metaLine(detail),
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
                Box(Modifier.padding(top = 8.dp)) {
                    ScoreRow(detail.imdbRating, detail.rottenTomatoesScore, detail.tmdbVoteAverage)
                }
            }
        }
    }
}

private fun metaLine(detail: MediaDetail): String = listOfNotNull(
    DateUtils.formatForDisplay(detail.releaseDate),
    detail.runtimeMinutes?.let { "$it min" },
    detail.seasonCount?.let { if (it == 1) "1 season" else "$it seasons" },
    detail.episodeCount?.let { "$it episodes" }
).joinToString(" · ")

@Composable
private fun DetailsActions(
    detail: MediaDetail,
    entry: WatchlistEntity?,
    onToggleSaved: () -> Unit,
    onToggleWatched: () -> Unit,
    onToggleNotify: (Boolean) -> Unit
) {
    val isSaved = entry != null
    val isUpcoming = detail.releaseStatus == ReleaseStatus.UPCOMING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSaved) {
            FilledTonalButton(onClick = onToggleSaved, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("In watchlist")
            }
        } else {
            Button(onClick = onToggleSaved, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add to watchlist")
            }
        }
        if (isUpcoming) {
            val notifyOn = entry?.notifyOnRelease == true
            FilledTonalIconToggleButton(
                checked = notifyOn,
                onCheckedChange = onToggleNotify,
                modifier = Modifier.semantics { contentDescription = "Release alert" }
            ) {
                Icon(if (notifyOn) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone, contentDescription = null)
            }
        } else if (entry != null) {
            FilledTonalIconToggleButton(
                checked = entry.watched,
                onCheckedChange = { onToggleWatched() },
                modifier = Modifier.semantics { contentDescription = "Watched" }
            ) {
                Icon(if (entry.watched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline, contentDescription = null)
            }
        }
    }
}

/** Whole card toggles, so the switch's small target isn't the only way to flip it. */
@Composable
private fun SeasonAlertCard(
    detail: MediaDetail,
    following: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextSeason = detail.nextSeasonNumber
    val nextDate = detail.nextSeasonAirDate
    val status = when {
        nextSeason != null && nextDate != null -> "Season $nextSeason premieres ${DateUtils.formatForDisplay(nextDate)}"
        nextSeason != null -> "Season $nextSeason is coming, date TBA"
        else -> "No new season dated yet"
    }
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.outlinedShape)
            .toggleable(value = following, role = Role.Switch, onValueChange = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.NewReleases,
                contentDescription = null,
                tint = if (following) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text("New season alerts", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (following) "$status. We'll tell you when a season is dated and when it airs." else status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Visual only: the card carries the toggle semantics and click.
            Switch(checked = following, onCheckedChange = null)
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(top = 24.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ExpandableText(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
    )
    if (overflows || expanded) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Show less" else "Show more")
        }
    }
}

@Composable
private fun WhereToWatch(detail: MediaDetail, onOpenAll: () -> Unit) {
    val isUpcoming = detail.releaseStatus == ReleaseStatus.UPCOMING
    if (detail.watchProviders.isEmpty() && isUpcoming) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle("Where to watch (${detail.watchProvidersRegion})", Modifier.weight(1f))
        if (detail.watchProviders.isNotEmpty()) {
            TextButton(onClick = onOpenAll, modifier = Modifier.padding(top = 16.dp)) { Text("All options") }
        }
    }

    if (detail.watchProviders.isEmpty()) {
        Text(
            "No streaming or rental info for your region yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        return
    }

    ProviderKind.entries.forEach { kind ->
        val providers = detail.watchProviders.filter { it.kind == kind }
        if (providers.isEmpty()) return@forEach
        Text(
            when (kind) {
                ProviderKind.STREAM -> "Stream"
                ProviderKind.RENT -> "Rent"
                ProviderKind.BUY -> "Buy"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(providers, key = { it.providerName }) { ProviderLogo(it) }
        }
    }
}

/** Information only - TMDB gives no per-provider deep links, so these are deliberately not buttons. */
@Composable
private fun ProviderLogo(provider: WatchProvider) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PosterImage(
            path = provider.logoPath,
            contentDescription = null,
            size = "w92",
            modifier = Modifier.size(48.dp)
        )
        Text(
            provider.providerName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CastMemberCard(member: CastMember) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        PosterImage(
            path = member.profilePath,
            contentDescription = null,
            shape = CircleShape,
            size = "w185",
            modifier = Modifier.size(80.dp)
        )
        Text(
            member.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (!member.character.isNullOrBlank()) {
            Text(
                member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecommendationCard(item: MediaSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        PosterImage(
            path = item.posterPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
        Text(
            item.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
        )
    }
}
