package com.arno.showtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.ContentRefreshBus
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.data.repository.MediaRepository
import com.arno.showtracker.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeData(
    val upcoming: List<MediaSummary>,
    val justDropped: List<MediaSummary>
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val refreshBus: ContentRefreshBus
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state

    /** tmdbId -> saved-to-watchlist, for the "just dropped" add button. */
    val savedIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** tmdbId -> notify-on-release enabled, for the "releasing soon" bell button. */
    val notifyIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.filter { it.notifyOnRelease }.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hasNotifications: StateFlow<Boolean> = notifyIds
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        load()
        refreshBus.events.onEach { load() }.launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                val upcoming = repository.upcoming()
                val dropped = repository.recentlyReleased()
                _state.value = UiState.Success(HomeData(upcoming, dropped))
            } catch (t: Throwable) {
                _state.value = UiState.Error(t.message ?: "Couldn't load. Check your connection.")
            }
        }
    }

    fun toggleWatchlist(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleWatchlist(item) }
    }

    fun toggleNotify(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleNotify(item) }
    }
}
