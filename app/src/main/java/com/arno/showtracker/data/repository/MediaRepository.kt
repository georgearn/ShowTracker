package com.arno.showtracker.data.repository

import com.arno.showtracker.BuildConfig
import com.arno.showtracker.data.local.UserPrefs
import com.arno.showtracker.data.local.WatchlistDao
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.model.MediaDetail
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.data.model.MediaType
import com.arno.showtracker.data.model.ProviderKind
import com.arno.showtracker.data.model.WatchProvider
import com.arno.showtracker.data.remote.omdb.OmdbApi
import com.arno.showtracker.data.remote.tmdb.TmdbApi
import com.arno.showtracker.data.remote.tmdb.TmdbDetailResponse
import com.arno.showtracker.data.remote.tmdb.TmdbMultiResult
import com.arno.showtracker.di.ApiConstants
import com.arno.showtracker.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val omdbApi: OmdbApi,
    private val watchlistDao: WatchlistDao,
    private val userPrefs: UserPrefs
) {
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
        return tmdbApi.trendingWeek().results
            .filter { it.mediaType == "movie" || it.mediaType == "tv" }
            .map { it.toSummary() }
            .filterNotBlocked(blocked)
    }

    /**
     * Home feed: titles released in the last [windowDays] days across movies + tv, newest first,
     * with IMDb/RT scores merged in (OMDb lookups capped at [ratingsCap] to bound request fan-out).
     */
    suspend fun recentlyReleased(windowDays: Long = 30, ratingsCap: Int = 24): List<MediaSummary> {
        val today = DateUtils.todayIso()
        val since = DateUtils.isoDaysAgo(windowDays)
        val blocked = userPrefs.blockedCountries.first()
        val movies = tmdbApi.discoverMovieReleased(lte = today, gte = since).results.map { it.toSummary(MediaType.MOVIE) }
        val tv = tmdbApi.discoverTvReleased(lte = today, gte = since).results.map { it.toSummary(MediaType.TV) }
        val combined = (movies + tv).sortedByDescending { it.releaseDate }.filterNotBlocked(blocked)
        val rated = enrichWithOmdbRatings(combined.take(ratingsCap))
        return rated + combined.drop(ratingsCap)
    }

    /** Upcoming feed: titles not out yet, soonest first - the "set a notification" pool. */
    suspend fun upcoming(): List<MediaSummary> {
        val today = DateUtils.todayIso()
        val blocked = userPrefs.blockedCountries.first()
        val movies = tmdbApi.discoverMovieUpcoming(gte = today).results.map { it.toSummary(MediaType.MOVIE) }
        val tv = tmdbApi.discoverTvUpcoming(gte = today).results.map { it.toSummary(MediaType.TV) }
        return (movies + tv).sortedBy { it.releaseDate }.filterNotBlocked(blocked)
    }

    private fun List<MediaSummary>.filterNotBlocked(blocked: Set<String>): List<MediaSummary> {
        if (blocked.isEmpty()) return this
        return filter { item -> item.originCountries.none { it in blocked } }
    }

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

    suspend fun getDetail(tmdbId: Int, mediaType: MediaType): MediaDetail {
        val detail = if (mediaType == MediaType.MOVIE) tmdbApi.movieDetail(tmdbId) else tmdbApi.tvDetail(tmdbId)
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
            releaseStatus = DateUtils.releaseStatus(detail.resolvedDate)
        )
    }

    private fun buildProviderList(
        region: com.arno.showtracker.data.remote.tmdb.TmdbProviderRegion?
    ): List<WatchProvider> {
        if (region == null) return emptyList()
        val stream = region.flatrate.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.STREAM) }
        val rent = region.rent.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.RENT) }
        val buy = region.buy.orEmpty().map { WatchProvider(it.providerName, it.logoPath, ProviderKind.BUY) }
        return (stream + rent + buy).distinctBy { it.providerName }
    }

    // ---------- Watchlist (handpicked saves) ----------

    fun observeWatchlist(): Flow<List<WatchlistEntity>> = watchlistDao.observeAll()

    fun observeIsSaved(tmdbId: Int): Flow<Boolean> = watchlistDao.observeById(tmdbId).map { it != null }

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
                addedAtEpochMillis = System.currentTimeMillis(),
                notifyOnRelease = notifyOnRelease && detail.releaseStatus == com.arno.showtracker.data.model.ReleaseStatus.UPCOMING,
                lastKnownReleaseStatus = detail.releaseStatus.name
            )
        )
    }

    suspend fun removeFromWatchlist(tmdbId: Int) = watchlistDao.deleteById(tmdbId)

    suspend fun setWatched(tmdbId: Int, watched: Boolean) =
        watchlistDao.setWatched(tmdbId, watched, if (watched) System.currentTimeMillis() else null)

    suspend fun setNotifyOnRelease(item: WatchlistEntity, enabled: Boolean) =
        watchlistDao.update(item.copy(notifyOnRelease = enabled))

    // ---------- Suggestion engine ("For You") ----------

    /** Picks a random unwatched, already-released title from the user's own saved list. */
    suspend fun suggestFromWatchlist(genre: String? = null): WatchlistEntity? {
        val all = watchlistDao.observeAll().first()
        val pool = all.filter { item ->
            !item.watched &&
                DateUtils.releaseStatus(item.releaseDate) == com.arno.showtracker.data.model.ReleaseStatus.RELEASED &&
                (genre == null || item.genres.split(",").map { it.trim() }.contains(genre))
        }
        return pool.randomOrNull()
    }

    /**
     * Builds a shuffled swipe queue from the user's own watchlist, filtered by mood (a genre
     * bucket) and media type. Falls back to the full unwatched watchlist if the filters match
     * nothing, so the queue is never empty just because of a narrow mood pick.
     */
    suspend fun suggestionQueue(mood: SuggestionMood, type: MediaType?): List<WatchlistEntity> {
        val all = watchlistDao.observeAll().first().filter { !it.watched }
        val genres = mood.genres
        var filtered = all
        if (type != null) filtered = filtered.filter { it.mediaType == type.apiValue }
        if (genres != null) {
            filtered = filtered.filter { item ->
                item.genres.split(",").map { it.trim() }.any { it in genres }
            }
        }
        if (filtered.isEmpty()) filtered = all
        return filtered.shuffled()
    }

    // ---------- Quick add from a list card (no full detail fetch) ----------

    suspend fun quickToggleWatchlist(summary: MediaSummary) {
        if (watchlistDao.observeById(summary.tmdbId).first() != null) {
            watchlistDao.deleteById(summary.tmdbId)
        } else {
            watchlistDao.upsert(summary.toWatchlistEntity())
        }
    }

    suspend fun quickToggleNotify(summary: MediaSummary) {
        val existing = watchlistDao.observeById(summary.tmdbId).first()
        if (existing != null) {
            watchlistDao.update(existing.copy(notifyOnRelease = !existing.notifyOnRelease))
        } else {
            watchlistDao.upsert(summary.toWatchlistEntity(notifyOnRelease = true))
        }
    }
}

enum class SuggestionMood(val genres: List<String>?) {
    ANYTHING(null),
    LIGHT(listOf("Comedy", "Fantasy", "Family", "Animation")),
    INTENSE(listOf("Thriller", "Horror", "Action", "Crime"))
}

private fun MediaSummary.toWatchlistEntity(notifyOnRelease: Boolean = false) = WatchlistEntity(
    tmdbId = tmdbId,
    mediaType = mediaType.apiValue,
    title = title,
    posterPath = posterPath,
    releaseDate = releaseDate,
    overview = overview,
    imdbId = null,
    imdbRating = null,
    rottenTomatoesScore = null,
    genres = "",
    addedAtEpochMillis = System.currentTimeMillis(),
    notifyOnRelease = notifyOnRelease && DateUtils.releaseStatus(releaseDate) == com.arno.showtracker.data.model.ReleaseStatus.UPCOMING,
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
    originCountries = originCountry.orEmpty()
)

fun imageUrl(path: String?, size: String = "w500"): String? =
    path?.let { "${ApiConstants.TMDB_IMAGE_BASE}$size$it" }
