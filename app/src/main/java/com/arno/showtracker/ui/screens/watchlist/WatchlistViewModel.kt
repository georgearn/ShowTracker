package com.arno.showtracker.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.model.ReleaseStatus
import com.arno.showtracker.data.repository.MediaRepository
import com.arno.showtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val readyToWatch: List<WatchlistEntity> = emptyList(),
    val waitingOnRelease: List<WatchlistEntity> = emptyList(),
    val watched: List<WatchlistEntity> = emptyList()
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<WatchlistUiState> = repository.observeWatchlist()
        .map { list ->
            WatchlistUiState(
                readyToWatch = list.filter { !it.watched && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.RELEASED },
                waitingOnRelease = list.filter { DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.UPCOMING },
                watched = list.filter { it.watched }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WatchlistUiState())

    fun toggleWatched(item: WatchlistEntity) {
        viewModelScope.launch { repository.setWatched(item.tmdbId, !item.watched) }
    }

    fun remove(item: WatchlistEntity) {
        viewModelScope.launch { repository.removeFromWatchlist(item.tmdbId) }
    }

    fun toggleNotify(item: WatchlistEntity) {
        viewModelScope.launch { repository.setNotifyOnRelease(item, !item.notifyOnRelease) }
    }
}
