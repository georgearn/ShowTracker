package com.georgearn.showtracker.data.local

import org.json.JSONArray
import org.json.JSONObject

/** Plain-JSON export/import of the watchlist, so it survives a reinstall or a new phone. */
object WatchlistBackup {
    private const val VERSION = 1

    fun toJson(items: List<WatchlistEntity>): String {
        val array = JSONArray()
        items.forEach { e ->
            array.put(
                JSONObject()
                    .put("tmdbId", e.tmdbId)
                    .put("mediaType", e.mediaType)
                    .put("title", e.title)
                    .put("posterPath", e.posterPath)
                    .put("releaseDate", e.releaseDate)
                    .put("overview", e.overview)
                    .put("imdbId", e.imdbId)
                    .put("imdbRating", e.imdbRating)
                    .put("rottenTomatoesScore", e.rottenTomatoesScore)
                    .put("genres", e.genres)
                    .put("runtimeMinutes", e.runtimeMinutes)
                    .put("addedAtEpochMillis", e.addedAtEpochMillis)
                    .put("watched", e.watched)
                    .put("watchedAtEpochMillis", e.watchedAtEpochMillis)
                    .put("notifyOnRelease", e.notifyOnRelease)
                    .put("followSeasons", e.followSeasons)
            )
        }
        return JSONObject().put("version", VERSION).put("watchlist", array).toString(2)
    }

    /** Throws [org.json.JSONException] on a file that isn't a ShowTracker export. */
    fun fromJson(json: String): List<WatchlistEntity> {
        val array = JSONObject(json).getJSONArray("watchlist")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            WatchlistEntity(
                tmdbId = o.getInt("tmdbId"),
                mediaType = o.getString("mediaType"),
                title = o.getString("title"),
                posterPath = o.optStringOrNull("posterPath"),
                releaseDate = o.optStringOrNull("releaseDate"),
                overview = o.optString("overview"),
                imdbId = o.optStringOrNull("imdbId"),
                imdbRating = o.optStringOrNull("imdbRating"),
                rottenTomatoesScore = o.optStringOrNull("rottenTomatoesScore"),
                genres = o.optString("genres"),
                runtimeMinutes = if (o.isNull("runtimeMinutes")) null else o.optInt("runtimeMinutes"),
                addedAtEpochMillis = o.optLong("addedAtEpochMillis", System.currentTimeMillis()),
                watched = o.optBoolean("watched"),
                watchedAtEpochMillis = if (o.isNull("watchedAtEpochMillis")) null else o.optLong("watchedAtEpochMillis"),
                notifyOnRelease = o.optBoolean("notifyOnRelease"),
                followSeasons = o.optBoolean("followSeasons")
            )
        }
    }

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)
}
