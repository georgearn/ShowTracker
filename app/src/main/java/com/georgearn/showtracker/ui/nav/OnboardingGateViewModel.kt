package com.georgearn.showtracker.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolves whether onboarding is needed before the NavHost picks a start destination. Null = still checking. */
@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _needsOnboarding = MutableStateFlow<Boolean?>(null)
    val needsOnboarding: StateFlow<Boolean?> = _needsOnboarding

    init {
        viewModelScope.launch {
            _needsOnboarding.value = !userPrefs.hasOnboarded.first()
        }
    }
}
