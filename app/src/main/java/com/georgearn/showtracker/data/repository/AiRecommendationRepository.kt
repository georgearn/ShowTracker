package com.georgearn.showtracker.data.repository

import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.local.WatchlistDao
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.remote.gemini.GeminiApi
import com.georgearn.showtracker.data.remote.gemini.GeminiSuggestion
import com.georgearn.showtracker.data.remote.tmdb.TmdbApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A title the model suggested, matched to a real TMDB entry, plus the model's one-line pitch. */
data class AiPick(val summary: MediaSummary, val reason: String)

/** Failure from the AI flow; [message] is already user-facing. */
class AiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NO_KEY, BAD_KEY, QUOTA, NETWORK, EMPTY, OTHER }
}

@Singleton
class AiRecommendationRepository @Inject constructor(
    private val gemini: GeminiApi,
    private val tmdb: TmdbApi,
    private val watchlistDao: WatchlistDao,
    private val userPrefs: UserPrefs,
    moshi: Moshi
) {
    private val suggestionsAdapter = moshi.adapter<List<GeminiSuggestion>>(
        Types.newParameterizedType(List::class.java, GeminiSuggestion::class.java)
    )

    /** Titles on the user's list, newest first - the default taste profile. */
    suspend fun watchlistTitles(limit: Int = SEED_LIMIT): List<String> =
        watchlistDao.observeAll().first().sortedByDescending { it.addedAtEpochMillis }.map { it.title }.take(limit)

    /**
     * Asks Gemini for titles similar to [seeds] and [note], skipping anything already saved.
     * Throws [AiException] with a user-facing message on any failure.
     */
    suspend fun suggest(seeds: List<String>, note: String): List<AiPick> {
        val key = userPrefs.geminiApiKey.first()
            ?: throw AiException(AiException.Kind.NO_KEY, "Add your Gemini API key first.")
        val saved = watchlistDao.observeAll().first()
        val savedTitles = saved.map { it.title.lowercase() }.toSet()

        val raw = try {
            gemini.generateContent(MODEL, key, requestBody(prompt(seeds, note, saved.map { it.title })))
        } catch (e: HttpException) {
            throw when (e.code()) {
                400, 401, 403 -> AiException(AiException.Kind.BAD_KEY, "Google rejected the API key. Check it in Settings.")
                429 -> AiException(AiException.Kind.QUOTA, "Gemini's free quota is used up for now. Try again in a minute.")
                else -> AiException(AiException.Kind.OTHER, "Gemini returned an error (${e.code()}).")
            }
        } catch (e: IOException) {
            throw AiException(AiException.Kind.NETWORK, "Couldn't reach Gemini. Check your connection.")
        }

        val text = raw.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("").orEmpty()
        val suggestions = runCatching { suggestionsAdapter.fromJson(text) }.getOrNull().orEmpty()
        if (suggestions.isEmpty()) {
            throw AiException(AiException.Kind.EMPTY, "Gemini didn't return any suggestions. Try rewording your request.")
        }

        val picks = coroutineScope {
            suggestions.map { s -> async { runCatching { resolve(s) }.getOrNull() } }.awaitAll()
        }.filterNotNull()
            .distinctBy { it.summary.mediaType to it.summary.tmdbId }
            .filter { it.summary.title.lowercase() !in savedTitles }
        if (picks.isEmpty()) {
            throw AiException(AiException.Kind.EMPTY, "None of the suggestions were new to you. Try again for a fresh batch.")
        }
        return picks
    }

    /** Looks the model's title up on TMDB; the year and movie/series hint pick between same-name results. */
    private suspend fun resolve(s: GeminiSuggestion): AiPick? {
        val wanted = when (s.type?.lowercase()) {
            "movie" -> "movie"
            "tv", "series" -> "tv"
            else -> null
        }
        val hits = tmdb.searchMulti(s.title).results.filter { it.mediaType == "movie" || it.mediaType == "tv" }
        val ofType = hits.filter { wanted == null || it.mediaType == wanted }
        val best = ofType.firstOrNull { s.year != null && it.resolvedDate?.startsWith(s.year.toString()) == true }
            ?: ofType.firstOrNull()
            ?: return null
        return AiPick(best.toSummary(), s.reason.orEmpty().trim())
    }

    private fun requestBody(prompt: String): Map<String, Any> = mapOf(
        "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
        "generationConfig" to mapOf(
            "responseMimeType" to "application/json",
            "temperature" to 0.9,
            "responseSchema" to mapOf(
                "type" to "ARRAY",
                "items" to mapOf(
                    "type" to "OBJECT",
                    "properties" to mapOf(
                        "title" to mapOf("type" to "STRING"),
                        "year" to mapOf("type" to "INTEGER"),
                        "type" to mapOf("type" to "STRING", "enum" to listOf("movie", "tv")),
                        "reason" to mapOf("type" to "STRING")
                    ),
                    "required" to listOf("title", "type", "reason")
                )
            )
        )
    )

    private fun prompt(seeds: List<String>, note: String, alreadySaved: List<String>): String = buildString {
        appendLine("You are a movie and TV recommender. Suggest $RESULT_COUNT real, released titles the user would enjoy.")
        if (seeds.isNotEmpty()) appendLine("They like: ${seeds.joinToString("; ")}.")
        if (note.isNotBlank()) appendLine("Their request: ${note.trim()}")
        if (alreadySaved.isNotEmpty()) {
            appendLine("Do not suggest any of these: ${alreadySaved.take(SEED_LIMIT * 2).joinToString("; ")}.")
        }
        appendLine("Mix well-known and lesser-known picks. Use the title's original English release name.")
        append("For each, give the release year, \"movie\" or \"tv\", and a reason of at most 18 words tied to what they like.")
    }

    companion object {
        /** Alias that tracks Google's current Flash model, so the app doesn't pin a retired version. */
        const val MODEL = "gemini-flash-latest"
        const val SEED_LIMIT = 25
        const val RESULT_COUNT = 10
    }
}
