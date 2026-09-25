package com.georgearn.showtracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "show_tracker_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Filters that drop likely-junk titles from the passive feeds (never from explicit search). */
data class FeedQuality(
    val hideNoArtwork: Boolean = true,
    val hideShortFilms: Boolean = true,
    val popularUpcomingOnly: Boolean = true,
    val hiddenGenreIds: Set<Int> = UserPrefs.DEFAULT_HIDDEN_GENRES
)

@Singleton
class UserPrefs @Inject constructor(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color") // Monet on/off
        val WATCH_REGION = stringPreferencesKey("watch_region")    // ISO country for JustWatch/TMDB providers
        val BLOCKED_COUNTRIES = stringSetPreferencesKey("blocked_origin_countries") // ISO country codes to hide
        val PREFERRED_GENRES = stringSetPreferencesKey("preferred_genre_ids") // TMDB genre ids, as strings
        val HAS_ONBOARDED = booleanPreferencesKey("has_onboarded")
        val UPCOMING_PAGES = intPreferencesKey("upcoming_pages_per_type") // TMDB pages (20 results each) fetched per media type
        val SEEN_ALERTS = stringSetPreferencesKey("seen_alert_keys") // alert keys already shown on the Notifications screen
        val HIDE_NO_ARTWORK = booleanPreferencesKey("hide_no_artwork")
        val HIDE_SHORTS = booleanPreferencesKey("hide_short_films")
        val POPULAR_UPCOMING = booleanPreferencesKey("popular_upcoming_only")
        val HIDDEN_GENRES = stringSetPreferencesKey("hidden_genre_ids") // TMDB genre ids, as strings
    }

    companion object {
        const val DEFAULT_UPCOMING_PAGES = 3
        const val MAX_UPCOMING_PAGES = 15 // ~300 titles/type - generous without hammering TMDB's rate limit

        /** TMDB tv genres News, Reality, Soap, Talk - the bulk of non-film, non-series noise. */
        val DEFAULT_HIDDEN_GENRES = setOf(10763, 10764, 10766, 10767)
    }

    val upcomingPagesPerType: Flow<Int> = context.dataStore.data.map { it[Keys.UPCOMING_PAGES] ?: DEFAULT_UPCOMING_PAGES }

    suspend fun setUpcomingPagesPerType(pages: Int) {
        context.dataStore.edit { it[Keys.UPCOMING_PAGES] = pages.coerceIn(1, MAX_UPCOMING_PAGES) }
    }

    val feedQuality: Flow<FeedQuality> = context.dataStore.data.map { prefs ->
        FeedQuality(
            hideNoArtwork = prefs[Keys.HIDE_NO_ARTWORK] ?: true,
            hideShortFilms = prefs[Keys.HIDE_SHORTS] ?: true,
            popularUpcomingOnly = prefs[Keys.POPULAR_UPCOMING] ?: true,
            hiddenGenreIds = prefs[Keys.HIDDEN_GENRES]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: DEFAULT_HIDDEN_GENRES
        )
    }

    suspend fun setHideNoArtwork(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_NO_ARTWORK] = enabled }
    }

    suspend fun setHideShortFilms(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_SHORTS] = enabled }
    }

    suspend fun setPopularUpcomingOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.POPULAR_UPCOMING] = enabled }
    }

    suspend fun setHiddenGenreIds(ids: Set<Int>) {
        context.dataStore.edit { it[Keys.HIDDEN_GENRES] = ids.map { id -> id.toString() }.toSet() }
    }

    val seenAlertKeys: Flow<Set<String>> = context.dataStore.data.map { it[Keys.SEEN_ALERTS] ?: emptySet() }

    suspend fun setSeenAlertKeys(keys: Set<String>) {
        context.dataStore.edit { it[Keys.SEEN_ALERTS] = keys }
    }

    val blockedCountries: Flow<Set<String>> = context.dataStore.data.map { it[Keys.BLOCKED_COUNTRIES] ?: emptySet() }

    suspend fun setCountryBlocked(code: String, blocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_COUNTRIES] ?: emptySet()
            prefs[Keys.BLOCKED_COUNTRIES] = if (blocked) current + code else current - code
        }
    }

    suspend fun setBlockedCountries(codes: Set<String>) {
        context.dataStore.edit { it[Keys.BLOCKED_COUNTRIES] = codes }
    }

    val preferredGenreIds: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PREFERRED_GENRES]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun setPreferredGenreIds(ids: Set<Int>) {
        context.dataStore.edit { it[Keys.PREFERRED_GENRES] = ids.map { id -> id.toString() }.toSet() }
    }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAS_ONBOARDED] ?: false }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[Keys.HAS_ONBOARDED] = true }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }

    val watchRegion: Flow<String> = context.dataStore.data.map { it[Keys.WATCH_REGION] ?: "US" }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setWatchRegion(region: String) {
        context.dataStore.edit { it[Keys.WATCH_REGION] = region }
    }
}
