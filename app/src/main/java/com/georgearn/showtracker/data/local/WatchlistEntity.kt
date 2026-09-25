package com.georgearn.showtracker.data.local

import androidx.room.Entity

/**
 * A title the user has handpicked into their own list - whether it's already released
 * and waiting to be watched, or still upcoming and waiting for a release notification.
 */
@Entity(tableName = "watchlist", primaryKeys = ["tmdbId", "mediaType"])
data class WatchlistEntity(
    val tmdbId: Int,
    val mediaType: String,          // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,       // yyyy-MM-dd, null/blank = TBA
    val overview: String,
    val imdbId: String?,
    val imdbRating: String?,
    val rottenTomatoesScore: String?,
    val genres: String,             // comma-separated cache, refreshed on detail fetch
    val runtimeMinutes: Int? = null, // movie runtime or single-episode runtime, when known from a detail fetch
    val addedAtEpochMillis: Long,
    val watched: Boolean = false,
    val watchedAtEpochMillis: Long? = null,
    val notifyOnRelease: Boolean = false,
    val lastKnownReleaseStatus: String = "UNKNOWN", // ReleaseStatus.name cache for the background worker diff
    val releaseNotificationSent: Boolean = false
)

/** TMDB reuses numeric ids across movies and tv, so identity is always the (type, id) pair. */
val WatchlistEntity.key: String get() = mediaKey(mediaType, tmdbId)

fun mediaKey(mediaType: String, tmdbId: Int): String = "$mediaType:$tmdbId"
