package com.georgearn.showtracker.data.repository

import com.georgearn.showtracker.BuildConfig
import com.georgearn.showtracker.data.local.Countries
import com.georgearn.showtracker.data.local.FeedQuality
import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.local.WatchlistDao
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.data.model.CastMember
import com.georgearn.showtracker.data.model.MediaDetail
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.model.ProviderKind
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.model.WatchProvider
import com.georgearn.showtracker.data.remote.omdb.OmdbApi
import com.georgearn.showtracker.data.remote.tmdb.TmdbApi
import com.georgearn.showtracker.data.remote.tmdb.TmdbDetailResponse
import com.georgearn.showtracker.data.remote.tmdb.TmdbMultiResult
import com.georgearn.showtracker.data.remote.tmdb.TmdbVideo
import com.georgearn.showtracker.di.ApiConstants
import com.georgearn.showtracker.ui.screens.common.UserMessage
import com.georgearn.showtracker.ui.screens.common.UserMessageBus
import com.georgearn.showtracker.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val omdbApi: OmdbApi,
    private val watchlistDao: WatchlistDao,
    private val userPrefs: UserPrefs,
    private val messages: UserMessageBus
) {
    private var genreNamesCache: Map<Int, String>? = null
    private var cachedRecentlyReleased: Pair<Long, List<MediaSummary>>? = null
    private var cachedUpcoming: Pair<Long, List<MediaSummary>>? = null
    private val CACHE_DURATION_MS = 3 * 24 * 60 * 60 * 1000L // 3 days
    private val SHORT_FILM_MAX_MINUTES = 40 // Academy cut-off for a short film

    /** id -> name for both movie and tv genres, fetched once and cached for the process lifetime. */
    suspend fun genreNames(): Map<Int, String> {
        genreNamesCache?.let { return it }
        val names = runCatching {
            val movie = tmdbApi.movieGenres().genres
            val tv = tmdbApi.tvGenres().genres
            (movie + tv).associate { it.id to it.name }
        }.getOrDefault(emptyMap())
        genreNamesCache = names
        return names
    }

    // ---------- Search / Discover ----------

    suspend fun search(query: String): List<MediaSummary> {
        if (query.isBlank()) return emptyList()
        val blocked = userPrefs.blockedCountries.first()
        return tmdbApi.searchMulti(query).results
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .map { it.toSummary() }
            .filterNotBlocked(blocked)
    }

    suspend fun trending(): List<MediaSummary> {
        val blocked = userPrefs.blockedCountries.first()
        val preferredGenres = userPrefs.preferredGenreIds.first()
        val quality = userPrefs.feedQuality.first()
        return tmdbApi.trendingWeek().results
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .map { it.toSummary() }
            .filterNotBlocked(blocked)
            .filter { hasReadableTitle(it.title) }
            .filterByPreferredGenres(preferredGenres)
            .filterQuality(quality)
    }

    /**
     * Home feed: titles released in the last [windowDays] days across movies + tv, newest first,
     * with IMDb/RT scores merged in (OMDb lookups capped at [ratingsCap] to bound request fan-out).
     * Cached in-memory for 3 days.
     */
    suspend fun recentlyReleased(
        windowDays: Long = 30,
        ratingsCap: Int = 24,
        pagesPerType: Int = 5,
        forceRefresh: Boolean = false
    ): List<MediaSummary> {
        val now = System.currentTimeMillis()
        cachedRecentlyReleased?.takeUnless { forceRefresh }?.let { (timestamp, list) ->
            if (now - timestamp < CACHE_DURATION_MS && list.isNotEmpty()) {
                return list
            }
        }
        val today = DateUtils.todayIso()
        val since = DateUtils.isoDaysAgo(windowDays)
        val blocked = userPrefs.blockedCountries.first()
        val preferredGenres = userPrefs.preferredGenreIds.first()
        val quality = userPrefs.feedQuality.first()
        val minRuntime = if (quality.hideShortFilms) SHORT_FILM_MAX_MINUTES else null
        val movies = coroutineScope {
            (1..pagesPerType).map { page ->
                async { tmdbApi.discoverMovieReleased(lte = today, gte = since, minRuntime = minRuntime, page = page).results }
            }.awaitAll()
        }.flatten().map { it.toSummary(MediaType.MOVIE) }
        val tv = coroutineScope {
            (1..pagesPerType).map { page ->
                async { tmdbApi.discoverTvReleased(lte = today, gte = since, page = page).results }
            }.awaitAll()
        }.flatten().map { it.toSummary(MediaType.TV) }
        val combined = (movies + tv)
            .distinctBy { it.mediaType to it.tmdbId }
            .sortedByDescending { it.releaseDate }
            .filterNotBlocked(blocked)
            .filter { hasReadableTitle(it.title) }
            .filterByPreferredGenres(preferredGenres)
            .filterQuality(quality)
        val rated = enrichWithOmdbRatings(combined.take(ratingsCap))
        val result = rated + combined.drop(ratingsCap)
        cachedRecentlyReleased = now to result
        return result
    }

    /**
     * Upcoming feed: titles not out yet, soonest first - the "set a notification" pool and the
     * source for the "Releasing Soon" time-window sections. Fetches a few pages per media type
     * since near-term releases alone can fill page 1, otherwise nothing further out ever shows.
     * Cached in-memory for 3 days.
     */
    suspend fun upcoming(pagesPerType: Int? = null, forceRefresh: Boolean = false): List<MediaSummary> {
        val now = System.currentTimeMillis()
        cachedUpcoming?.takeUnless { forceRefresh }?.let { (timestamp, list) ->
            if (now - timestamp < CACHE_DURATION_MS && list.isNotEmpty()) {
                return list
            }
        }
        val pages = pagesPerType ?: userPrefs.upcomingPagesPerType.first()
        val today = DateUtils.todayIso()
        val blocked = userPrefs.blockedCountries.first()
        val preferredGenres = userPrefs.preferredGenreIds.first()
        val quality = userPrefs.feedQuality.first()
        // Date-sorted pages are dominated by obscure entries; ranking a 12-month window by
        // popularity keeps the titles people actually anticipate. The UI re-sorts by date.
        val until = if (quality.popularUpcomingOnly) DateUtils.isoDaysAhead(365) else null
        val movieSort = if (quality.popularUpcomingOnly) "popularity.desc" else "primary_release_date.asc"
        val tvSort = if (quality.popularUpcomingOnly) "popularity.desc" else "first_air_date.asc"
        val movies = coroutineScope {
            (1..pages).map { page ->
                async { tmdbApi.discoverMovieUpcoming(sortBy = movieSort, gte = today, lte = until, page = page).results }
            }.awaitAll()
        }.flatten().map { it.toSummary(MediaType.MOVIE) }
        val tv = coroutineScope {
            (1..pages).map { page ->
                async { tmdbApi.discoverTvUpcoming(sortBy = tvSort, gte = today, lte = until, page = page).results }
            }.awaitAll()
        }.flatten().map { it.toSummary(MediaType.TV) }
        val result = (movies + tv)
            .distinctBy { it.mediaType to it.tmdbId }
            .sortedBy { it.releaseDate }
            .filterNotBlocked(blocked)
            .filter { hasReadableTitle(it.title) }
            .filterByPreferredGenres(preferredGenres)
            .filterQuality(quality)
        cachedUpcoming = now to result
        return result
    }

    /** Onboarding genre picks act as a hard filter when set - consistent across every passive feed. */
    private fun List<MediaSummary>.filterByPreferredGenres(preferred: Set<Int>): List<MediaSummary> {
        if (preferred.isEmpty()) return this
        return filter { item -> item.genreIds.any { it in preferred } }
    }

    /** Hidden genres win over preferred ones: a comedy talk show is still a talk show. */
    private fun List<MediaSummary>.filterQuality(quality: FeedQuality): List<MediaSummary> = filter { item ->
        val hasArtwork = item.posterPath != null && item.backdropPath != null
        (!quality.hideNoArtwork || hasArtwork) && item.genreIds.none { it in quality.hiddenGenreIds }
    }

    private fun List<MediaSummary>.filterNotBlocked(blocked: Set<String>): List<MediaSummary> {
        if (blocked.isEmpty()) return this
        val blockedLanguages = Countries.blockableLanguages(blocked)
        return filter { item ->
            val blockedByCountry = item.originCountries.any { it in blocked }
            // Movie list results don't carry origin_country from TMDB - fall back to original_language.
            val blockedByLanguage = item.originCountries.isEmpty() && item.originalLanguage in blockedLanguages
            !blockedByCountry && !blockedByLanguage
        }
    }

    /** Drops titles TMDB has no English translation for (falls back to a non-Latin original title). */
    private fun hasReadableTitle(title: String): Boolean = title.any { it in 'a'..'z' || it in 'A'..'Z' }

    /** Looks up IMDb rating + Rotten Tomatoes score per title via OMDb, in parallel, best-effort. */
    private suspend fun enrichWithOmdbRatings(items: List<MediaSummary>): List<MediaSummary> {
        if (BuildConfig.OMDB_API_KEY.isBlank()) return items
        return coroutineScope {
            items.map { item ->
                async {
                    val year = item.releaseDate?.take(4)
                    val omdb = runCatching { omdbApi.byTitle(item.title, year, BuildConfig.OMDB_API_KEY) }.getOrNull()
                    if (omdb == null || omdb.Response == "False") {
                        item
                    } else {
                        item.copy(
                            imdbRating = omdb.imdbRating?.takeIf { it != "N/A" }?.let { "$it/10" },
                            rottenTomatoesScore = omdb.Ratings?.firstOrNull { it.Source == "Rotten Tomatoes" }?.Value
                        )
                    }
                }
            }.map { it.await() }
        }
    }

    // ---------- Detail (TMDB + OMDb merge) ----------

    /** [bypassCache] is for the background worker, which must see date changes the same day. */
    suspend fun getDetail(tmdbId: Int, mediaType: MediaType, bypassCache: Boolean = false): MediaDetail {
        val cacheControl = if (bypassCache) "no-cache" else null
        val detail = if (mediaType == MediaType.MOVIE) {
            tmdbApi.movieDetail(tmdbId, cacheControl = cacheControl)
        } else {
            tmdbApi.tvDetail(tmdbId, cacheControl = cacheControl)
        }
        val region = userPrefs.watchRegion.first()
        val providers = detail.watchProviders?.results?.get(region)
        val imdbId = detail.resolvedImdbId

        var imdbRating: String? = null
        var rtScore: String? = null
        if (BuildConfig.OMDB_API_KEY.isNotBlank()) {
            var omdb = if (!imdbId.isNullOrBlank()) {
                runCatching { omdbApi.byImdbId(imdbId, BuildConfig.OMDB_API_KEY) }.getOrNull()
            } else null
            // Some TV titles don't resolve an IMDb ID from TMDB - fall back to a title+year lookup.
            if (omdb == null || omdb.Response == "False") {
                omdb = runCatching {
                    omdbApi.byTitle(detail.resolvedTitle, detail.resolvedDate?.take(4), BuildConfig.OMDB_API_KEY)
                }.getOrNull()
            }
            if (omdb != null && omdb.Response != "False") {
                imdbRating = omdb.imdbRating?.takeIf { it != "N/A" }?.let { "$it/10" }
                rtScore = omdb.Ratings?.firstOrNull { it.Source == "Rotten Tomatoes" }?.Value
            }
        }

        return MediaDetail(
            tmdbId = detail.id,
            imdbId = imdbId,
            mediaType = mediaType,
            title = detail.resolvedTitle,
            posterPath = detail.posterPath,
            backdropPath = detail.backdropPath,
            releaseDate = detail.resolvedDate,
            runtimeMinutes = detail.resolvedRuntime,
            genres = detail.genres.map { it.name },
            synopsis = detail.overview.orEmpty().ifBlank { "No synopsis available yet." },
            tmdbVoteAverage = detail.voteAverage ?: 0.0,
            imdbRating = imdbRating,
            rottenTomatoesScore = rtScore,
            watchProviders = buildProviderList(providers),
            watchProvidersRegion = region,
            releaseStatus = DateUtils.releaseStatus(detail.resolvedDate),
            cast = detail.credits?.cast.orEmpty()
                .sortedBy { it.order }
                .take(15)
                .map { CastMember(it.name, it.character, it.profilePath) },
            trailerYoutubeKey = detail.videos?.results.orEmpty()
                .filter { it.site == "YouTube" && (it.type == "Trailer" || it.type == "Teaser") }
                .sortedWith(compareByDescending<TmdbVideo> { it.type == "Trailer" }.thenByDescending { it.official })
                .firstOrNull()?.key,
            seasonCount = detail.numberOfSeasons,
            seriesStatus = detail.status,
            nextSeasonNumber = detail.nextEpisodeToAir?.takeIf { it.episodeNumber == 1 }?.seasonNumber,
            nextSeasonAirDate = detail.nextEpisodeToAir?.takeIf { it.episodeNumber == 1 }?.airDate?.ifBlank { null },
            latestAiredSeason = detail.lastEpisodeToAir?.seasonNumber,
            episodeCount = detail.numberOfEpisodes,
            recommendations = detail.recommendations?.results.orEmpty()
                .filter { it.posterPath != null }
                .map { it.toSummary(if (it.mediaType == "tv" || it.mediaType == "movie") null else mediaType) }
                .distinctBy { it.mediaType to it.tmdbId }
                .filterQuality(userPrefs.feedQuality.first())
                .take(12)
        )
    }

    private fun buildProviderList(
        region: com.georgearn.showtracker.data.remote.tmdb.TmdbProviderRegion?
    ): List<WatchProvider> {
        if (region == null) return emptyList()
        val stream = region.flatrate.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.STREAM) }
        val rent = region.rent.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.RENT) }
        val buy = region.buy.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.BUY) }
        return (stream + rent + buy).distinctBy { it.providerName }
    }

    // ---------- Watchlist (handpicked saves) ----------

    fun observeWatchlist(): Flow<List<WatchlistEntity>> = watchlistDao.observeAll()

    /**
     * Titles with the bell on, split into "out now" (released in the last 30 days) and "coming up".
     * Each alert carries a status-scoped key so a title re-badges once when it flips to released.
     */
    fun observeReleaseAlerts(): Flow<ReleaseAlerts> = watchlistDao.observeAll().map { list ->
        val watched = list.filter { it.notifyOnRelease }
        ReleaseAlerts(
            outNow = watched
                .filter { (DateUtils.daysSince(it.releaseDate) ?: Long.MAX_VALUE) <= 30 && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.RELEASED }
                .sortedByDescending { it.releaseDate },
            comingUp = watched
                .filter { DateUtils.releaseStatus(it.releaseDate) != ReleaseStatus.RELEASED }
                .sortedWith(compareBy(nullsLast<String>()) { it.releaseDate?.ifBlank { null } }),
            newSeasons = list
                .filter { it.followSeasons && it.nextSeasonNumber != null && it.nextSeasonAirDate != null }
                .filter { (DateUtils.daysSince(it.nextSeasonAirDate) ?: 0L) <= 30 }
                .sortedBy { it.nextSeasonAirDate }
        )
    }

    val hasUnseenAlerts: Flow<Boolean> = combine(observeReleaseAlerts(), userPrefs.seenAlertKeys) { alerts, seen ->
        (alerts.badgeKeys - seen).isNotEmpty()
    }

    suspend fun markAlertsSeen() {
        userPrefs.setSeenAlertKeys(observeReleaseAlerts().first().badgeKeys)
    }

    fun observeEntry(tmdbId: Int, mediaType: MediaType): Flow<WatchlistEntity?> =
        watchlistDao.observeById(tmdbId, mediaType.apiValue)

    suspend fun addToWatchlist(detail: MediaDetail, notifyOnRelease: Boolean) {
        watchlistDao.upsert(
            WatchlistEntity(
                tmdbId = detail.tmdbId,
                mediaType = detail.mediaType.apiValue,
                title = detail.title,
                posterPath = detail.posterPath,
                releaseDate = detail.releaseDate,
                overview = detail.synopsis,
                imdbId = detail.imdbId,
                imdbRating = detail.imdbRating,
                rottenTomatoesScore = detail.rottenTomatoesScore,
                genres = detail.genres.joinToString(","),
                runtimeMinutes = detail.runtimeMinutes,
                addedAtEpochMillis = System.currentTimeMillis(),
                notifyOnRelease = notifyOnRelease && detail.releaseStatus == ReleaseStatus.UPCOMING,
                lastKnownReleaseStatus = detail.releaseStatus.name
            )
        )
    }

    /**
     * Turns new-season alerts on or off, saving the series first if needed. Enabling records what
     * is already known (a dated season, the latest aired one) so only future changes notify.
     */
    suspend fun setFollowSeasons(detail: MediaDetail, enabled: Boolean) {
        if (detail.mediaType != MediaType.TV) return
        if (watchlistDao.getById(detail.tmdbId, detail.mediaType.apiValue) == null) {
            if (!enabled) return
            addToWatchlist(detail, notifyOnRelease = false)
        }
        val existing = watchlistDao.getById(detail.tmdbId, detail.mediaType.apiValue) ?: return
        watchlistDao.update(
            if (enabled) {
                existing.copy(
                    followSeasons = true,
                    nextSeasonNumber = detail.nextSeasonNumber ?: existing.nextSeasonNumber,
                    nextSeasonAirDate = detail.nextSeasonAirDate ?: existing.nextSeasonAirDate,
                    lastAnnouncedSeason = detail.nextSeasonNumber ?: existing.lastAnnouncedSeason,
                    lastReleasedSeason = detail.latestAiredSeason ?: existing.lastReleasedSeason ?: 0
                )
            } else {
                existing.copy(followSeasons = false)
            }
        )
    }

    /**
     * Copies fresh detail data onto a saved entry: genres, runtime and scores (quick adds start
     * without them) and, for followed series, the next season. Never notifies.
     */
    suspend fun refreshSavedEntry(detail: MediaDetail) {
        val existing = watchlistDao.getById(detail.tmdbId, detail.mediaType.apiValue) ?: return
        var updated = existing.copy(
            genres = detail.genres.joinToString(",").ifBlank { existing.genres },
            runtimeMinutes = detail.runtimeMinutes ?: existing.runtimeMinutes,
            imdbId = detail.imdbId ?: existing.imdbId,
            imdbRating = detail.imdbRating ?: existing.imdbRating,
            rottenTomatoesScore = detail.rottenTomatoesScore ?: existing.rottenTomatoesScore
        )
        if (existing.followSeasons && detail.nextSeasonNumber != null) {
            updated = updated.copy(nextSeasonNumber = detail.nextSeasonNumber, nextSeasonAirDate = detail.nextSeasonAirDate)
        }
        if (updated != existing) watchlistDao.update(updated)
    }

    /**
     * Fills in genres (and movie runtimes) for entries saved before quick adds carried them,
     * a few requests at a time. Series often have no episode runtime on TMDB, so a missing
     * tv runtime alone doesn't trigger a fetch.
     */
    suspend fun backfillWatchlistMetadata() {
        val missing = watchlistDao.observeAll().first().filter {
            it.genres.isBlank() || (it.mediaType == MediaType.MOVIE.apiValue && it.runtimeMinutes == null)
        }
        missing.chunked(4).forEach { chunk ->
            val details = coroutineScope {
                chunk.map { item ->
                    async { runCatching { getDetail(item.tmdbId, MediaType.from(item.mediaType)) }.getOrNull() }
                }.awaitAll()
            }
            details.filterNotNull().forEach { refreshSavedEntry(it) }
        }
    }

    /** The For You pool: released titles on the list that haven't been watched yet. */
    fun observeSuggestionPool(): Flow<List<WatchlistEntity>> = watchlistDao.observeAll().map { list ->
        list.filter { !it.watched && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.RELEASED }
    }

    suspend fun setWatched(item: WatchlistEntity, watched: Boolean) =
        watchlistDao.setWatched(item.tmdbId, item.mediaType, watched, if (watched) System.currentTimeMillis() else null)

    /** Details-screen bell: saves the title with the alert on if needed, otherwise flips the alert only. */
    suspend fun setNotifyOnRelease(detail: MediaDetail, enabled: Boolean) {
        val existing = watchlistDao.getById(detail.tmdbId, detail.mediaType.apiValue)
        if (existing == null) {
            if (enabled) addToWatchlist(detail, notifyOnRelease = true)
        } else {
            watchlistDao.update(existing.copy(notifyOnRelease = enabled))
        }
    }

    suspend fun setNotifyOnRelease(item: WatchlistEntity, enabled: Boolean) =
        watchlistDao.update(item.copy(notifyOnRelease = enabled))

    // ---------- Suggestion engine ("For You") ----------

    /** Picks a random unwatched, already-released title from the user's own saved list. */
    suspend fun suggestFromWatchlist(genre: String? = null): WatchlistEntity? {
        val all = watchlistDao.observeAll().first()
        val pool = all.filter { item ->
            !item.watched &&
                DateUtils.releaseStatus(item.releaseDate) == ReleaseStatus.RELEASED &&
                (genre == null || item.genres.split(",").map { it.trim() }.contains(genre))
        }
        return pool.randomOrNull()
    }

    /**
     * Builds a shuffled swipe queue from the user's own watchlist, filtered by mood (a genre
     * bucket) and media type. Falls back to the full unwatched watchlist if the filters match
     * nothing, so the queue is never empty just because of a narrow mood pick.
     */
    suspend fun suggestionQueue(
        mood: SuggestionMood,
        type: MediaType?,
        length: LengthPref = LengthPref.ANY,
        genreOverride: Set<String>? = null
    ): List<WatchlistEntity> {
        val all = watchlistDao.observeAll().first().filter {
            !it.watched && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.RELEASED
        }
        val genres = if (!genreOverride.isNullOrEmpty()) genreOverride else mood.genres
        var filtered = all
        if (type != null) filtered = filtered.filter { it.mediaType == type.apiValue }
        if (genres != null) {
            filtered = filtered.filter { item -> item.genreList().any { it.matchesAnyGenre(genres) } }
        }
        if (length != LengthPref.ANY) {
            val byLength = filtered.filter { item -> item.effectiveRuntime()?.let { length.matches(it) } == true }
            if (byLength.isNotEmpty()) filtered = byLength
        }
        if (filtered.isEmpty()) filtered = all
        return filtered.shuffled()
    }

    // ---------- Quick add from a list card (no full detail fetch) ----------

    suspend fun quickToggleWatchlist(summary: MediaSummary) {
        val existing = watchlistDao.getById(summary.tmdbId, summary.mediaType.apiValue)
        if (existing != null) {
            removeWithUndo(existing)
        } else {
            val added = summary.toWatchlistEntity(genreNames = genreNames())
            watchlistDao.upsert(added)
            messages.post(
                UserMessage(
                    text = "Added \"${added.title}\" to your list",
                    actionLabel = "Undo",
                    onAction = { watchlistDao.deleteById(added.tmdbId, added.mediaType) }
                )
            )
        }
    }

    /** Removes an entry and posts a snackbar whose "Undo" restores it exactly as it was. */
    suspend fun removeWithUndo(item: WatchlistEntity) {
        watchlistDao.deleteById(item.tmdbId, item.mediaType)
        messages.post(
            UserMessage(
                text = "Removed \"${item.title}\"",
                actionLabel = "Undo",
                onAction = { watchlistDao.upsert(item) }
            )
        )
    }

    suspend fun quickToggleNotify(summary: MediaSummary) {
        val existing = watchlistDao.getById(summary.tmdbId, summary.mediaType.apiValue)
        if (existing != null) {
            watchlistDao.update(existing.copy(notifyOnRelease = !existing.notifyOnRelease))
        } else {
            watchlistDao.upsert(summary.toWatchlistEntity(notifyOnRelease = true, genreNames = genreNames()))
        }
    }
}

data class ReleaseAlerts(
    val outNow: List<WatchlistEntity>,
    val comingUp: List<WatchlistEntity>,
    /** Followed series with a dated season premiering soon or in the last 30 days. */
    val newSeasons: List<WatchlistEntity> = emptyList()
) {
    /**
     * Only fresh releases and titles due within a week earn a badge - not every bell ever set.
     * Season keys include the season number and phase, so a newly dated season badges once and
     * its premiere badges again.
     */
    val badgeKeys: Set<String>
        get() = outNow.map { "out:${it.key}" }.toSet() +
            comingUp.filter { (DateUtils.daysUntil(it.releaseDate) ?: Long.MAX_VALUE) <= 7 }.map { "soon:${it.key}" } +
            newSeasons.map { item ->
                val phase = if (DateUtils.releaseStatus(item.nextSeasonAirDate) == ReleaseStatus.RELEASED) "aired" else "dated"
                "season:${item.key}:${item.nextSeasonNumber}:$phase"
            }
}

/** TMDB often lists no episode length for series; assume a typical ~45 min hour-long episode. */
private const val DEFAULT_EPISODE_MINUTES = 45

private fun WatchlistEntity.effectiveRuntime(): Int? =
    runtimeMinutes ?: if (mediaType == MediaType.TV.apiValue) DEFAULT_EPISODE_MINUTES else null

fun WatchlistEntity.genreList(): List<String> = genres.split(",").map { it.trim() }.filter { it.isNotBlank() }

/** TV genres are compound ("Action & Adventure", "Sci-Fi & Fantasy"), so "Action" should match them. */
private fun String.matchesAnyGenre(wanted: Collection<String>): Boolean =
    wanted.any { w -> this == w || split(" & ").any { part -> part == w } || (w.contains(" & ") && w.split(" & ").any { it == this }) }

enum class SuggestionMood(val genres: List<String>?) {
    ANYTHING(null),
    LIGHT(listOf("Comedy", "Fantasy", "Family", "Animation")),
    INTENSE(listOf("Thriller", "Horror", "Action", "Crime"))
}

/** Runtime-minutes bucket for the "how long" quiz question - movie runtime or single-episode length. */
enum class LengthPref(val label: String) {
    ANY("Any"),
    SHORT("< 45m"),
    MEDIUM("45-100m"),
    LONG("> 100m");

    fun matches(minutes: Int): Boolean = when (this) {
        ANY -> true
        SHORT -> minutes < 45
        MEDIUM -> minutes in 45..100
        LONG -> minutes > 100
    }
}

private fun MediaSummary.toWatchlistEntity(
    notifyOnRelease: Boolean = false,
    genreNames: Map<Int, String> = emptyMap()
) = WatchlistEntity(
    tmdbId = tmdbId,
    mediaType = mediaType.apiValue,
    title = title,
    posterPath = posterPath,
    releaseDate = releaseDate,
    overview = overview,
    imdbId = null,
    imdbRating = null,
    rottenTomatoesScore = null,
    // List results carry genre ids only; names are what the For You quiz filters on.
    genres = genreIds.mapNotNull { genreNames[it] }.distinct().joinToString(","),
    addedAtEpochMillis = System.currentTimeMillis(),
    notifyOnRelease = notifyOnRelease && DateUtils.releaseStatus(releaseDate) == ReleaseStatus.UPCOMING,
    lastKnownReleaseStatus = DateUtils.releaseStatus(releaseDate).name
)

fun TmdbMultiResult.toSummary(forcedType: MediaType? = null): MediaSummary = MediaSummary(
    tmdbId = id,
    mediaType = forcedType ?: MediaType.from(mediaType ?: "movie"),
    title = resolvedTitle,
    posterPath = posterPath,
    releaseDate = resolvedDate,
    overview = overview.orEmpty(),
    tmdbVoteAverage = voteAverage ?: 0.0,
    originCountries = originCountry.orEmpty(),
    originalLanguage = originalLanguage,
    genreIds = genreIds.orEmpty(),
    backdropPath = backdropPath
)

fun imageUrl(path: String?, size: String = "w500"): String? =
    path?.let { "${ApiConstants.TMDB_IMAGE_BASE}$size$it" }
