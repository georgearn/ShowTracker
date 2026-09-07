package com.arno.showtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class], version = 2, exportSchema = false)
abstract class ShowTrackerDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val DB_NAME = "showtracker.db"
    }
}
