package com.georgearn.showtracker.di

import com.georgearn.showtracker.BuildConfig
import com.georgearn.showtracker.data.remote.omdb.OmdbApi
import com.georgearn.showtracker.data.remote.tmdb.TmdbApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

object ApiConstants {
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    const val OMDB_BASE_URL = "https://www.omdbapi.com/"
    const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    @Named("tmdbAuth")
    fun provideTmdbAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    @Provides
    @Singleton
    @Named("tmdb")
    fun provideTmdbClient(
        logging: HttpLoggingInterceptor,
        @Named("tmdbAuth") auth: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(auth)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @Named("omdb")
    fun provideOmdbClient(logging: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    fun provideTmdbApi(@Named("tmdb") client: OkHttpClient, moshi: Moshi): TmdbApi =
        Retrofit.Builder()
            .baseUrl(ApiConstants.TMDB_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApi::class.java)

    @Provides
    @Singleton
    fun provideOmdbApi(@Named("omdb") client: OkHttpClient, moshi: Moshi): OmdbApi =
        Retrofit.Builder()
            .baseUrl(ApiConstants.OMDB_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OmdbApi::class.java)
}
