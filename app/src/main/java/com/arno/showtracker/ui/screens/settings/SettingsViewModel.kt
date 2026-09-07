package com.arno.showtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.ThemeMode
import com.arno.showtracker.data.local.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = userPrefs.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val watchRegion: StateFlow<String> = userPrefs.watchRegion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "US")

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { userPrefs.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { userPrefs.setDynamicColorEnabled(enabled) }
    fun setWatchRegion(region: String) = viewModelScope.launch { userPrefs.setWatchRegion(region) }
}
