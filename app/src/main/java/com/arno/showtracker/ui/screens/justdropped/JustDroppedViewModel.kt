package com.arno.showtracker.ui.screens.justdropped

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

enum class JustDroppedTypeFilter(val label: String, val type: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    SERIES("Series", MediaType.TV)
}

@HiltViewModel
class JustDroppedViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<MediaSummary>>>(UiState.Loading)
    private val _typeFilter = MutableStateFlow(JustDroppedTypeFilter.ALL)
    val typeFilter: StateFlow<JustDroppedTypeFilter> = _typeFilter

    private val _genreFilter = MutableStateFlow<Int?>(null)
    val genreFilter: StateFlow<Int?> = _genreFilter

    private val _genreNames = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** Genres actually present in the loaded list, sorted by name - only these are worth showing as chips. */
    val availableGenres: StateFlow<List<Pair<Int, String>>> = combine(_state, _genreNames) { state, names ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        list.flatMap { it.genreIds }.distinct()
            .mapNotNull { id -> names[id]?.let { id to it } }
            .sortedBy { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<MediaSummary>> = combine(_state, _typeFilter, _genreFilter) { state, typeFilter, genreFilter ->
        var list = (state as? UiState.Success)?.data.orEmpty()
        if (typeFilter.type != null) list = list.filter { it.mediaType == typeFilter.type }
        if (genreFilter != null) list = list.filter { genreFilter in it.genreIds }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading: StateFlow<Boolean> = _state.map { it is UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val savedIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                _genreNames.value = repository.genreNames()
                _state.value = UiState.Success(repository.recentlyReleased())
            } catch (t: Throwable) {
                _state.value = UiState.Error(t.message ?: "Couldn't load. Check your connection.")
            }
        }
    }

    fun setTypeFilter(filter: JustDroppedTypeFilter) {
        _typeFilter.value = filter
    }

    fun setGenreFilter(genreId: Int?) {
        _genreFilter.value = if (_genreFilter.value == genreId) null else genreId
    }

    fun toggleWatchlist(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleWatchlist(item) }
    }
}
