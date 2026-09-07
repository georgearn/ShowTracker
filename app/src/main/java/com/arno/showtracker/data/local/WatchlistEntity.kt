package com.arno.showtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A title the user has handpicked into their own list - whether it's already released
 * and waiting to be watched, or still upcoming and waiting for a release notification.
 */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val tmdbId: Int,
    val mediaType: String,          // "movie" | "tv"
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,       // yyyy-MM-dd, null/blank = TBA
    val overview: String,
    val imdbId: String?,
    val imdbRating: String?,
    val rottenTomatoesScore: String?,
    val genres: String,             // comma-separated cache, refreshed on detail fetch
    val addedAtEpochMillis: Long,
    val watched: Boolean = false,
    val notifyOnRelease: Boolean = false,
    val lastKnownReleaseStatus: String = "UNKNOWN", // ReleaseStatus.name cache for the background worker diff
    val releaseNotificationSent: Boolean = false
)
