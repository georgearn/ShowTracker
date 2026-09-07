package com.arno.showtracker.data.remote.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMultiResult>
)

@JsonClass(generateAdapter = true)
data class TmdbMultiResult(
    val id: Int,
    @Json(name = "media_type") val mediaType: String? = null, // "movie" | "tv" | "person" (search/multi + trending)
    val title: String? = null,          // movie
    val name: String? = null,           // tv
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,      // movie
    @Json(name = "first_air_date") val firstAirDate: String? = null,   // tv
    val overview: String? = null,
    @Json(name = "vote_average") val voteAverage: Double? = null
) {
    val resolvedTitle: String get() = title ?: name ?: "Untitled"
    val resolvedDate: String? get() = releaseDate ?: firstAirDate
}

@JsonClass(generateAdapter = true)
data class TmdbDiscoverResponse(
    val page: Int,
    val results: List<TmdbMultiResult>
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(val id: Int, val name: String)

@JsonClass(generateAdapter = true)
data class TmdbExternalIds(
    @Json(name = "imdb_id") val imdbId: String?
)

@JsonClass(generateAdapter = true)
data class TmdbDetailResponse(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = null,                       // movie only
    @Json(name = "episode_run_time") val episodeRunTime: List<Int>? = null, // tv only
    val genres: List<TmdbGenre> = emptyList(),
    @Json(name = "vote_average") val voteAverage: Double? = null,
    @Json(name = "imdb_id") val imdbId: String? = null, // present directly on movie detail
    @Json(name = "external_ids") val externalIds: TmdbExternalIds? = null, // present via append_to_response on tv
    @Json(name = "watch/providers") val watchProviders: TmdbWatchProvidersWrapper? = null
) {
    val resolvedTitle: String get() = title ?: name ?: "Untitled"
    val resolvedDate: String? get() = releaseDate ?: firstAirDate
    val resolvedRuntime: Int? get() = runtime ?: episodeRunTime?.firstOrNull()
    val resolvedImdbId: String? get() = imdbId ?: externalIds?.imdbId
}

@JsonClass(generateAdapter = true)
data class TmdbWatchProvidersWrapper(
    val results: Map<String, TmdbProviderRegion> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class TmdbProviderRegion(
    val link: String? = null,
    val flatrate: List<TmdbProvider>? = null,
    val rent: List<TmdbProvider>? = null,
    val buy: List<TmdbProvider>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbProvider(
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "logo_path") val logoPath: String?
)
