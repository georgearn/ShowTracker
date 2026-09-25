package com.georgearn.showtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE notifyOnRelease = 1 AND releaseNotificationSent = 0")
    suspend fun getPendingReleaseWatches(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    suspend fun getById(tmdbId: Int, mediaType: String): WatchlistEntity?

    @Query("SELECT * FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType LIMIT 1")
    fun observeById(tmdbId: Int, mediaType: String): Flow<WatchlistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchlistEntity)

    @Update
    suspend fun update(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun deleteById(tmdbId: Int, mediaType: String)

    @Query("UPDATE watchlist SET watched = :watched, watchedAtEpochMillis = :watchedAt WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun setWatched(tmdbId: Int, mediaType: String, watched: Boolean, watchedAt: Long?)

    @Query("SELECT * FROM watchlist WHERE watched = 1 ORDER BY watchedAtEpochMillis DESC")
    fun observeHistory(): Flow<List<WatchlistEntity>>

    @Query("UPDATE watchlist SET lastKnownReleaseStatus = :status WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun updateStatus(tmdbId: Int, mediaType: String, status: String)

    @Query("UPDATE watchlist SET releaseNotificationSent = 1 WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun markNotified(tmdbId: Int, mediaType: String)
}
