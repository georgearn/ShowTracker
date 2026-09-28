package com.georgearn.showtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.ContentRefreshBus
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.ui.screens.common.UserMessage
import com.georgearn.showtracker.ui.screens.common.UserMessageBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val upcoming: List<MediaSummary> = emptyList(),
    val justDropped: List<MediaSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Only set when there is nothing to show - a failed refresh over existing content is a snackbar. */
    val error: String? = null
) {
    val hasContent: Boolean get() = upcoming.isNotEmpty() || justDropped.isNotEmpty()
}

enum class TypeFilter(val label: String, val type: MediaType?) {
    ALL("All", null),
    MOVIES("Movies", MediaType.MOVIE),
    SERIES("Series", MediaType.TV)
}

/** Movies/Series is per tab; genre applies to both tabs. */
data class HomeFilters(
    val newReleasesType: TypeFilter = TypeFilter.ALL,
    val upcomingType: TypeFilter = TypeFilter.ALL,
    val genreId: Int? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val refreshBus: ContentRefreshBus,
    private val messages: UserMessageBus
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val savedKeys: StateFlow<Set<String>> = repository.savedKeys

    val notifyKeys: StateFlow<Set<String>> = repository.notifyKeys

    val hasUnseenAlerts: StateFlow<Boolean> = repository.hasUnseenAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _filters = MutableStateFlow(HomeFilters())
    val filters: StateFlow<HomeFilters> = _filters.asStateFlow()

    private val genreNames = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** Genres present in either feed, sorted by name - only these are worth offering. */
    val availableGenres: StateFlow<List<Pair<Int, String>>> = combine(_state, genreNames) { state, names ->
        (state.justDropped + state.upcoming).flatMap { it.genreIds }.distinct()
            .mapNotNull { id -> names[id]?.let { id to it } }
            .sortedBy { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleNewReleases: StateFlow<List<MediaSummary>> = combine(_state, _filters) { state, f ->
        state.justDropped.applyFilters(f.newReleasesType, f.genreId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleUpcoming: StateFlow<List<MediaSummary>> = combine(_state, _filters) { state, f ->
        state.upcoming.applyFilters(f.upcomingType, f.genreId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun List<MediaSummary>.applyFilters(type: TypeFilter, genreId: Int?): List<MediaSummary> =
        filter { (type.type == null || it.mediaType == type.type) && (genreId == null || genreId in it.genreIds) }

    fun setNewReleasesType(filter: TypeFilter) = _filters.update { it.copy(newReleasesType = filter) }

    fun setUpcomingType(filter: TypeFilter) = _filters.update { it.copy(upcomingType = filter) }

    /** Tapping the selected genre again clears it. */
    fun setGenre(genreId: Int?) = _filters.update { it.copy(genreId = if (it.genreId == genreId) null else genreId) }

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false, userInitiated = false)
        // Filters changed in Settings - the cached feed was built with the old ones.
        refreshBus.events.onEach { load(forceRefresh = true, userInitiated = false) }.launchIn(viewModelScope)
    }

    fun retry() = load(forceRefresh = true, userInitiated = false)

    fun refresh() = load(forceRefresh = true, userInitiated = true)

    private fun load(forceRefresh: Boolean, userInitiated: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(isLoading = !it.hasContent, isRefreshing = userInitiated && it.hasContent, error = null)
            }
            try {
                coroutineScope {
                    val upcoming = async { repository.upcoming(forceRefresh = forceRefresh) }
                    val dropped = async { repository.recentlyReleased(forceRefresh = forceRefresh) }
                    val names = async { runCatching { repository.genreNames() }.getOrDefault(emptyMap()) }
                    val result = HomeUiState(upcoming = upcoming.await(), justDropped = dropped.await(), isLoading = false)
                    _state.value = result
                    genreNames.value = names.await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                val hadContent = _state.value.hasContent
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (hadContent) null else (t.message ?: "Couldn't load. Check your connection.")
                    )
                }
                if (hadContent) messages.post(UserMessage("Couldn't refresh. Check your connection."))
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
