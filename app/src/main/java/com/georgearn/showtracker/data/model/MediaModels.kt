package com.georgearn.showtracker.data.model

/** Lightweight result row used in search / discover / trending lists. */
data class MediaSummary(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?, // yyyy-MM-dd, blank/null if TBA
    val overview: String,
    val tmdbVoteAverage: Double,
    val originCountries: List<String> = emptyList(), // ISO country codes, when the source list provides them (tv only)
    val originalLanguage: String? = null, // ISO 639-1, present on both movie and tv - used to filter movies by likely origin
    val genreIds: List<Int> = emptyList(),
    val imdbRating: String? = null,       // e.g. "7.4/10", filled in only where callers enrich it
    val rottenTomatoesScore: String? = null // e.g. "88%", filled in only where callers enrich it
)

/** Full detail screen payload: TMDB detail + watch providers + OMDb (IMDb/RT) ratings merged. */
data class MediaDetail(
    val tmdbId: Int,
    val imdbId: String?,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val synopsis: String,
    val tmdbVoteAverage: Double,
    val imdbRating: String?,       // e.g. "8.4/10" or null if not found
    val rottenTomatoesScore: String?, // e.g. "91%" or null if not found
    val watchProviders: List<WatchProvider>,
    val watchProvidersRegion: String,
    val releaseStatus: ReleaseStatus
)

data class WatchProvider(
    val providerName: String,
    val logoPath: String?,
    val kind: ProviderKind
)

enum class ProviderKind { STREAM, RENT, BUY }

/** What the "Suggest something to watch" feature draws from - the saved/handpicked list, released only. */
data class SuggestionCriteria(
    val onlyUnwatched: Boolean = true,
    val genreFilter: String? = null
)
