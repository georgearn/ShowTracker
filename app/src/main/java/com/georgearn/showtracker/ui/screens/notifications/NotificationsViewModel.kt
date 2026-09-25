package com.georgearn.showtracker.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.data.repository.ReleaseAlerts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    val alerts: StateFlow<ReleaseAlerts?> = repository.observeReleaseAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Opening the screen counts as seeing every current alert, which clears the Home badge. */
    fun markSeen() {
        viewModelScope.launch { repository.markAlertsSeen() }
    }
}
