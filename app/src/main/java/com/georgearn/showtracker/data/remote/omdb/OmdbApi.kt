package com.georgearn.showtracker.data.remote.omdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** OMDb (omdbapi.com) - gives IMDb rating + (when the title has one) a Rotten Tomatoes score. */
interface OmdbApi {
    @GET(".")
    suspend fun byImdbId(
        @Query("i") imdbId: String,
        @Query("apikey") apiKey: String
    ): OmdbResponse

    @GET(".")
    suspend fun byTitle(
        @Query("t") title: String,
        @Query("y") year: String? = null,
        @Query("apikey") apiKey: String
    ): OmdbResponse
}

@JsonClass(generateAdapter = true)
data class OmdbResponse(
    @Json(name = "imdbID") val imdbId: String? = null,
    @Json(name = "imdbRating") val imdbRating: String? = null, // "8.4" or "N/A"
    val Ratings: List<OmdbRatingSource>? = null,
    val Response: String? = null, // "True" | "False"
    val Error: String? = null
)

@JsonClass(generateAdapter = true)
data class OmdbRatingSource(
    val Source: String,   // "Internet Movie Database" | "Rotten Tomatoes" | "Metacritic"
    val Value: String      // "8.4/10" | "91%" | "70/100"
)
