package com.georgearn.showtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.ContentRefreshBus
import com.georgearn.showtracker.data.local.key
import com.georgearn.showtracker.data.model.MediaSummary
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val refreshBus: ContentRefreshBus,
    private val messages: UserMessageBus
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val savedKeys: StateFlow<Set<String>> = repository.observeWatchlist()
        .map { list -> list.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val notifyKeys: StateFlow<Set<String>> = repository.observeWatchlist()
        .map { list -> list.filter { it.notifyOnRelease }.map { it.key }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val hasUnseenAlerts: StateFlow<Boolean> = repository.hasUnseenAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
                    val result = HomeUiState(upcoming = upcoming.await(), justDropped = dropped.await(), isLoading = false)
                    _state.value = result
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
