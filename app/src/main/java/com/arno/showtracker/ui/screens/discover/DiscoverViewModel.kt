package com.arno.showtracker.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.ContentRefreshBus
import com.arno.showtracker.data.model.MediaSummary
import com.arno.showtracker.data.model.MediaType
import com.arno.showtracker.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiscoverFilter(val label: String, val type: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    SERIES("Series", MediaType.TV)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val refreshBus: ContentRefreshBus
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _filter = MutableStateFlow(DiscoverFilter.ALL)
    val filter: StateFlow<DiscoverFilter> = _filter

    private val _rawResults = MutableStateFlow<List<MediaSummary>>(emptyList())

    val results: StateFlow<List<MediaSummary>> = combine(_rawResults, _filter) { results, filter ->
        if (filter.type == null) results else results.filter { it.mediaType == filter.type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val savedIds: StateFlow<Set<Int>> = repository.observeWatchlist()
        .map { list -> list.map { it.tmdbId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        _query
            .debounce(350)
            .distinctUntilChanged()
            .onEach { q -> runSearch(q) }
            .launchIn(viewModelScope)
        refreshBus.events.onEach { runSearch(_query.value) }.launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onFilterChange(filter: DiscoverFilter) {
        _filter.value = filter
    }

    fun toggleWatchlist(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleWatchlist(item) }
    }

    private suspend fun runSearch(q: String) {
        if (q.isBlank()) {
            _rawResults.value = emptyList()
            return
        }
        _isLoading.value = true
        _rawResults.value = runCatching { repository.search(q) }.getOrDefault(emptyList())
        _isLoading.value = false
    }
}
