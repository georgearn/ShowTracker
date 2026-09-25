package com.georgearn.showtracker.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.georgearn.showtracker.BuildConfig
import com.georgearn.showtracker.data.remote.omdb.OmdbApi
import com.georgearn.showtracker.data.remote.tmdb.TmdbApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
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

    private const val THREE_DAYS_SECONDS = 3 * 24 * 60 * 60 // 259,200 seconds (3 days)

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

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
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheSize = 50L * 1024L * 1024L // 50 MB
        return Cache(File(context.cacheDir, "http_cache"), cacheSize)
    }

    @Provides
    @Singleton
    @Named("cacheInterceptor")
    fun provideCacheInterceptor(): Interceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=$THREE_DAYS_SECONDS, max-stale=$THREE_DAYS_SECONDS")
            .build()
    }

    @Provides
    @Singleton
    @Named("offlineCacheInterceptor")
    fun provideOfflineCacheInterceptor(@ApplicationContext context: Context): Interceptor = Interceptor { chain ->
        var request = chain.request()
        if (!isNetworkAvailable(context)) {
            val cacheControl = CacheControl.Builder()
                .onlyIfCached()
                .maxStale(3, TimeUnit.DAYS)
                .build()
            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }
        chain.proceed(request)
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
        cache: Cache,
        logging: HttpLoggingInterceptor,
        @Named("tmdbAuth") auth: Interceptor,
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        @Named("offlineCacheInterceptor") offlineCacheInterceptor: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(auth)
        .addInterceptor(offlineCacheInterceptor)
        .addNetworkInterceptor(cacheInterceptor)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @Named("omdb")
    fun provideOmdbClient(
        cache: Cache,
        logging: HttpLoggingInterceptor,
        @Named("cacheInterceptor") cacheInterceptor: Interceptor,
        @Named("offlineCacheInterceptor") offlineCacheInterceptor: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        .addInterceptor(offlineCacheInterceptor)
        .addNetworkInterceptor(cacheInterceptor)
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
