package com.georgearn.showtracker.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.ContentRefreshBus
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DiscoverFilter(val label: String, val type: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    SERIES("Series", MediaType.TV)
}

data class DiscoverUiState(
    val query: String = "",
    val filter: DiscoverFilter = DiscoverFilter.ALL,
    val searchResults: List<MediaSummary> = emptyList(),
    val trending: List<MediaSummary> = emptyList(),
    val isSearching: Boolean = false,
    val isLoadingTrending: Boolean = true,
    val searchFailed: Boolean = false
) {
    val showingTrending: Boolean get() = query.isBlank()

    val visibleItems: List<MediaSummary>
        get() {
            val source = if (showingTrending) trending else searchResults
            return if (filter.type == null) source else source.filter { it.mediaType == filter.type }
        }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val refreshBus: ContentRefreshBus
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    val savedKeys: StateFlow<Set<String>> = repository.observeWatchlist()
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val query = MutableStateFlow("")

    init {
        loadTrending()
        // mapLatest cancels an in-flight search as soon as the user types again.
        query
            .debounce(350)
            .distinctUntilChanged()
            .mapLatest { q -> runSearch(q) }
            .launchIn(viewModelScope)
        refreshBus.events.onEach {
            loadTrending()
            runSearch(query.value)
        }.launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        query.value = newQuery
    }

    fun onFilterChange(filter: DiscoverFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun retrySearch() {
        viewModelScope.launch { runSearch(query.value) }
    }

    fun toggleWatchlist(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleWatchlist(item) }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingTrending = it.trending.isEmpty()) }
            val trending = runCatching { repository.trending() }.getOrDefault(_state.value.trending)
            _state.update { it.copy(trending = trending, isLoadingTrending = false) }
        }
    }

    private suspend fun runSearch(q: String) {
        if (q.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false, searchFailed = false) }
            return
        }
        _state.update { it.copy(isSearching = true, searchFailed = false) }
        try {
            val results = repository.search(q)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            _state.update { it.copy(isSearching = false, searchFailed = true) }
        }
    }
}
