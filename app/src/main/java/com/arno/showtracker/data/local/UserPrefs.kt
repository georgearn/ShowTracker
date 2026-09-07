package com.arno.showtracker.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "show_tracker_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class UserPrefs @Inject constructor(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color") // Monet on/off
        val WATCH_REGION = stringPreferencesKey("watch_region")    // ISO country for JustWatch/TMDB providers
        val BLOCKED_COUNTRIES = stringSetPreferencesKey("blocked_origin_countries") // ISO country codes to hide
    }

    val blockedCountries: Flow<Set<String>> = context.dataStore.data.map { it[Keys.BLOCKED_COUNTRIES] ?: emptySet() }

    suspend fun setCountryBlocked(code: String, blocked: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_COUNTRIES] ?: emptySet()
            prefs[Keys.BLOCKED_COUNTRIES] = if (blocked) current + code else current - code
        }
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
