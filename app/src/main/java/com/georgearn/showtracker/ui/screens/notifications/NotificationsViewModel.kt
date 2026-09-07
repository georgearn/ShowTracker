package com.georgearn.showtracker.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.model.ReleaseStatus
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    repository: MediaRepository
) : ViewModel() {

    val items: StateFlow<List<WatchlistEntity>> = repository.observeWatchlist()
        .map { list ->
            list.filter { it.notifyOnRelease && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.UPCOMING }
                .sortedBy { it.releaseDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
