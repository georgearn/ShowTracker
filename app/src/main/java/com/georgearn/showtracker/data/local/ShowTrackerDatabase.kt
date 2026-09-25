package com.georgearn.showtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WatchlistEntity::class], version = 4, exportSchema = false)
abstract class ShowTrackerDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val DB_NAME = "showtracker.db"

        /** Primary key widened from tmdbId to (tmdbId, mediaType) - SQLite can't alter a PK in place. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `watchlist_new` (
                        `tmdbId` INTEGER NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `posterPath` TEXT,
                        `releaseDate` TEXT,
                        `overview` TEXT NOT NULL,
                        `imdbId` TEXT,
                        `imdbRating` TEXT,
                        `rottenTomatoesScore` TEXT,
                        `genres` TEXT NOT NULL,
                        `runtimeMinutes` INTEGER,
                        `addedAtEpochMillis` INTEGER NOT NULL,
                        `watched` INTEGER NOT NULL,
                        `watchedAtEpochMillis` INTEGER,
                        `notifyOnRelease` INTEGER NOT NULL,
                        `lastKnownReleaseStatus` TEXT NOT NULL,
                        `releaseNotificationSent` INTEGER NOT NULL,
                        PRIMARY KEY(`tmdbId`, `mediaType`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `watchlist_new` (tmdbId, mediaType, title, posterPath, releaseDate, overview,
                        imdbId, imdbRating, rottenTomatoesScore, genres, runtimeMinutes, addedAtEpochMillis, watched,
                        watchedAtEpochMillis, notifyOnRelease, lastKnownReleaseStatus, releaseNotificationSent)
                    SELECT tmdbId, mediaType, title, posterPath, releaseDate, overview,
                        imdbId, imdbRating, rottenTomatoesScore, genres, runtimeMinutes, addedAtEpochMillis, watched,
                        watchedAtEpochMillis, notifyOnRelease, lastKnownReleaseStatus, releaseNotificationSent
                    FROM `watchlist`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `watchlist`")
                db.execSQL("ALTER TABLE `watchlist_new` RENAME TO `watchlist`")
            }
        }
    }
}
