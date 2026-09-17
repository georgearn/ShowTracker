package com.georgearn.showtracker.ui.screens.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val isSaved: StateFlow<Boolean> = repository.observeIsSaved(tmdbId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.getDetail(tmdbId, mediaType))
            } catch (t: Throwable) {
                UiState.Error(t.message ?: "Couldn't load details.")
            }
        }
    }

    fun toggleSaved(notifyOnRelease: Boolean) {
        val detail = (_state.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            if (isSaved.value) {
                repository.removeFromWatchlist(tmdbId)
            } else {
                repository.addToWatchlist(detail, notifyOnRelease)
            }
        }
    }
}
