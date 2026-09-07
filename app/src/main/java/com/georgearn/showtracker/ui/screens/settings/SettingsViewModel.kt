package com.georgearn.showtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.ContentRefreshBus
import com.georgearn.showtracker.data.local.ThemeMode
import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenreToggleOption(val label: String, val ids: Set<Int>)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    private val refreshBus: ContentRefreshBus,
    private val repository: MediaRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = userPrefs.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val watchRegion: StateFlow<String> = userPrefs.watchRegion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "US")

    val blockedCountries: StateFlow<Set<String>> = userPrefs.blockedCountries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val upcomingPagesPerType: StateFlow<Int> = userPrefs.upcomingPagesPerType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs.DEFAULT_UPCOMING_PAGES)

    val preferredGenreIds: StateFlow<Set<Int>> = userPrefs.preferredGenreIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _genreOptions = MutableStateFlow<List<GenreToggleOption>>(emptyList())
    val genreOptions: StateFlow<List<GenreToggleOption>> = _genreOptions

    /** True right after a country toggle, until the user taps Refresh - nudges them that content is stale. */
    private val _pendingRefresh = MutableStateFlow(false)
    val pendingRefresh: StateFlow<Boolean> = _pendingRefresh

    init {
        viewModelScope.launch {
            _genreOptions.value = repository.genreNames().entries
                .groupBy({ it.value }, { it.key })
                .map { (label, ids) -> GenreToggleOption(label, ids.toSet()) }
                .sortedBy { it.label }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { userPrefs.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { userPrefs.setDynamicColorEnabled(enabled) }
    fun setWatchRegion(region: String) = viewModelScope.launch { userPrefs.setWatchRegion(region) }

    fun setCountryBlocked(code: String, blocked: Boolean) = viewModelScope.launch {
        userPrefs.setCountryBlocked(code, blocked)
        _pendingRefresh.value = true
    }

    fun refreshContent() = viewModelScope.launch {
        refreshBus.notifyChanged()
        _pendingRefresh.value = false
    }

    fun setUpcomingPagesPerType(pages: Int) = viewModelScope.launch {
        userPrefs.setUpcomingPagesPerType(pages)
        _pendingRefresh.value = true
    }

    fun toggleGenre(option: GenreToggleOption) = viewModelScope.launch {
        val current = preferredGenreIds.value
        val next = if (current.containsAll(option.ids)) current - option.ids else current + option.ids
        userPrefs.setPreferredGenreIds(next)
        _pendingRefresh.value = true
    }

    fun clearGenrePreference() = viewModelScope.launch {
        userPrefs.setPreferredGenreIds(emptySet())
        _pendingRefresh.value = true
    }
}
