package com.georgearn.showtracker.di

import android.content.Context
import androidx.room.Room
import com.georgearn.showtracker.data.local.ShowTrackerDatabase
import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.local.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShowTrackerDatabase =
        Room.databaseBuilder(context, ShowTrackerDatabase::class.java, ShowTrackerDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWatchlistDao(db: ShowTrackerDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    @Singleton
    fun provideUserPrefs(@ApplicationContext context: Context): UserPrefs = UserPrefs(context)
}
