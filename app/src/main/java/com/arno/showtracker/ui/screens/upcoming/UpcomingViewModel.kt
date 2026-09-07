package com.arno.showtracker.ui.screens.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.data.model.MediaType
import com.arno.showtracker.data.repository.MediaRepository
import com.arno.showtracker.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UpcomingFilter(val label: String, val type: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    SERIES("Series", MediaType.TV)
}

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MediaSummary>>>(UiState.Loading)
    private val _filter = MutableStateFlow(UpcomingFilter.ALL)
    val filter: StateFlow<UpcomingFilter> = _filter

    val items: StateFlow<List<MediaSummary>> = combine(_state, _filter) { state, filter ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        if (filter.type == null) list else list.filter { it.mediaType == filter.type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = _state.map { it is UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val savedIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val notifyIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.filter { it.notifyOnRelease }.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                _state.value = UiState.Success(repository.upcoming())
            } catch (t: Throwable) {
                _state.value = UiState.Error(t.message ?: "Couldn't load. Check your connection.")
            }
        }
    }

    fun setFilter(filter: UpcomingFilter) {
        _filter.value = filter
    }

    fun toggleNotify(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleNotify(item) }
    }
}
