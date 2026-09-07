package com.arno.showtracker.data.local

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

    @Query("SELECT * FROM watchlist WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getById(tmdbId: Int): WatchlistEntity?

    @Query("SELECT * FROM watchlist WHERE tmdbId = :tmdbId LIMIT 1")
    fun observeById(tmdbId: Int): Flow<WatchlistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchlistEntity)

    @Update
    suspend fun update(item: WatchlistEntity)

    @Delete
    suspend fun delete(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE tmdbId = :tmdbId")
    suspend fun deleteById(tmdbId: Int)

    @Query("UPDATE watchlist SET watched = :watched WHERE tmdbId = :tmdbId")
    suspend fun setWatched(tmdbId: Int, watched: Boolean)

    @Query("UPDATE watchlist SET lastKnownReleaseStatus = :status WHERE tmdbId = :tmdbId")
    suspend fun updateStatus(tmdbId: Int, status: String)

    @Query("UPDATE watchlist SET releaseNotificationSent = 1 WHERE tmdbId = :tmdbId")
    suspend fun markNotified(tmdbId: Int)
}
