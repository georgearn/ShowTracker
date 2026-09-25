package com.georgearn.showtracker.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import com.georgearn.showtracker.ui.screens.common.UserMessage
import com.georgearn.showtracker.ui.screens.common.UserMessageBus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolves whether onboarding is needed before the NavHost picks a start destination. Null = still checking. */
@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    messageBus: UserMessageBus
) : ViewModel() {

    val messages: Flow<UserMessage> = messageBus.messages

    /** Runs a snackbar action in this activity-scoped VM so it survives the screen that posted it. */
    fun runAction(message: UserMessage) {
        val action = message.onAction ?: return
        viewModelScope.launch { action() }
    }

    private val _needsOnboarding = MutableStateFlow<Boolean?>(null)
    val needsOnboarding: StateFlow<Boolean?> = _needsOnboarding

    init {
        viewModelScope.launch {
            _needsOnboarding.value = !userPrefs.hasOnboarded.first()
        }
    }
}
