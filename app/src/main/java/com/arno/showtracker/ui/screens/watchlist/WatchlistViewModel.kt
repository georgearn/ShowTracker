package com.arno.showtracker.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.model.ReleaseStatus
import com.arno.showtracker.data.repository.MediaRepository
import com.arno.showtracker.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WatchlistOrganization(val label: String) {
    PLAIN("Plain list"),
    BY_TYPE("Movies / Series"),
    BY_TYPE_AND_GENRE("Type & genre")
}

enum class WatchlistTab(val label: String) {
    LIST("My List"),
    HISTORY("History")
}

data class WatchlistGroup(val title: String?, val items: List<WatchlistEntity>)

data class WatchlistSection(val title: String, val groups: List<WatchlistGroup>) {
    val count: Int get() = groups.sumOf { it.items.size }
}

data class WatchlistUiState(
    val tab: WatchlistTab = WatchlistTab.LIST,
    val organization: WatchlistOrganization = WatchlistOrganization.PLAIN,
    val readyToWatch: WatchlistSection = WatchlistSection("Ready to watch", emptyList()),
    val waitingOnRelease: WatchlistSection = WatchlistSection("Waiting on release", emptyList()),
    val history: WatchlistSection = WatchlistSection("Watched", emptyList())
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _organization = MutableStateFlow(WatchlistOrganization.PLAIN)
    val organization: StateFlow<WatchlistOrganization> = _organization

    private val _tab = MutableStateFlow(WatchlistTab.LIST)
    val tab: StateFlow<WatchlistTab> = _tab

    val uiState: StateFlow<WatchlistUiState> = combine(
        repository.observeWatchlist(),
        _organization,
        _tab
    ) { list, org, tab ->
        WatchlistUiState(
            tab = tab,
            organization = org,
            readyToWatch = WatchlistSection(
                "Ready to watch",
                group(list.filter { !it.watched && DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.RELEASED }, org)
            ),
            waitingOnRelease = WatchlistSection(
                "Waiting on release",
                group(list.filter { DateUtils.releaseStatus(it.releaseDate) == ReleaseStatus.UPCOMING }, org)
            ),
            history = WatchlistSection(
                "Watched",
                group(list.filter { it.watched }.sortedByDescending { it.watchedAtEpochMillis ?: 0L }, org)
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WatchlistUiState())

    private fun group(items: List<WatchlistEntity>, org: WatchlistOrganization): List<WatchlistGroup> {
        return when (org) {
            WatchlistOrganization.PLAIN -> if (items.isEmpty()) emptyList() else listOf(WatchlistGroup(null, items))
            WatchlistOrganization.BY_TYPE -> items
                .groupBy { it.mediaType }
                .toSortedMap()
                .map { (type, entries) -> WatchlistGroup(typeLabel(type), entries) }
            WatchlistOrganization.BY_TYPE_AND_GENRE -> items
                .groupBy { it.mediaType to (it.genres.split(",").map { g -> g.trim() }.firstOrNull { g -> g.isNotBlank() } ?: "Other") }
                .toSortedMap(compareBy({ it.first }, { it.second }))
                .map { (key, entries) -> WatchlistGroup("${typeLabel(key.first)} · ${key.second}", entries) }
        }
    }

    private fun typeLabel(mediaType: String) = if (mediaType == "movie") "Movies" else "Series"

    fun setOrganization(org: WatchlistOrganization) {
        _organization.value = org
    }

    fun setTab(tab: WatchlistTab) {
        _tab.value = tab
    }

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
