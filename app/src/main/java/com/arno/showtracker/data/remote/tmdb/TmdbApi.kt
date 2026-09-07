package com.arno.showtracker.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** TMDB v3 REST API. Auth is the v4 read-access-token, sent as a Bearer header (see NetworkModule). */
interface TmdbApi {

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("trending/all/week")
    suspend fun trendingWeek(): TmdbSearchResponse

    /** "New releases" style feed: already-released titles, most recent first. */
    @GET("discover/movie")
    suspend fun discoverMovieReleased(
        @Query("sort_by") sortBy: String = "primary_release_date.desc",
        @Query("primary_release_date.lte") lte: String,
        @Query("vote_count.gte") minVotes: Int = 5,
        @Query("page") page: Int = 1
    ): TmdbDiscoverResponse

    @GET("discover/tv")
    suspend fun discoverTvReleased(
        @Query("sort_by") sortBy: String = "first_air_date.desc",
        @Query("first_air_date.lte") lte: String,
        @Query("vote_count.gte") minVotes: Int = 5,
        @Query("page") page: Int = 1
    ): TmdbDiscoverResponse

    /** Upcoming feed for the "not released yet, notify me" flow. */
    @GET("discover/movie")
    suspend fun discoverMovieUpcoming(
        @Query("sort_by") sortBy: String = "primary_release_date.asc",
        @Query("primary_release_date.gte") gte: String,
        @Query("page") page: Int = 1
    ): TmdbDiscoverResponse

    @GET("discover/tv")
    suspend fun discoverTvUpcoming(
        @Query("sort_by") sortBy: String = "first_air_date.asc",
        @Query("first_air_date.gte") gte: String,
        @Query("page") page: Int = 1
    ): TmdbDiscoverResponse

    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "watch/providers"
    ): TmdbDetailResponse

    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "watch/providers,external_ids"
    ): TmdbDetailResponse
}
