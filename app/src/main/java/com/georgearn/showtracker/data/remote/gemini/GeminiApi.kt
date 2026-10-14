package com.georgearn.showtracker.data.remote.gemini

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** Google AI Studio (Generative Language) REST API; the key is the user's own and travels in a header, never the URL. */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: @JvmSuppressWildcards Map<String, Any>
    ): GeminiResponse
}

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent? = null)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>? = null)

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String? = null)

/** One title as the model describes it, before it is matched to a real TMDB entry. */
@JsonClass(generateAdapter = true)
data class GeminiSuggestion(
    val title: String,
    val year: Int? = null,
    val type: String? = null, // "movie" | "tv"
    val reason: String? = null
)
