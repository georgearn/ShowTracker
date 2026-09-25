package com.georgearn.showtracker.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.model.MediaDetail
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tmdbId: Int = checkNotNull(savedStateHandle["tmdbId"])
    private val mediaType: MediaType = MediaType.from(checkNotNull(savedStateHandle["mediaType"]))

    private val _state = MutableStateFlow<UiState<MediaDetail>>(UiState.Loading)
    val state: StateFlow<UiState<MediaDetail>> = _state

    /** The saved row, if any - carries both "is saved" and "is the bell on", which are independent. */
    val entry: StateFlow<WatchlistEntity?> = repository.observeEntry(tmdbId, mediaType)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val detail = repository.getDetail(tmdbId, mediaType)
                repository.refreshSeasonInfo(detail)
                UiState.Success(detail)
            } catch (t: Throwable) {
                UiState.Error(t.message ?: "Couldn't load details.")
            }
        }
    }

    fun toggleSaved() {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val existing = entry.value
            if (existing != null) repository.removeWithUndo(existing)
            else repository.addToWatchlist(detail, notifyOnRelease = false)
        }
    }

    fun toggleWatched() {
        val existing = entry.value ?: return
        viewModelScope.launch { repository.setWatched(existing, !existing.watched) }
    }

    fun setNotify(enabled: Boolean) {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { repository.setNotifyOnRelease(detail, enabled) }
    }

    fun setFollowSeasons(enabled: Boolean) {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { repository.setFollowSeasons(detail, enabled) }
    }
}
