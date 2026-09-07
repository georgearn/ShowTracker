package com.georgearn.showtracker.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.Countries
import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenreOption(val label: String, val ids: Set<Int>)

data class OnboardingState(
    val loading: Boolean = true,
    val genreOptions: List<GenreOption> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val selectedCountryCodes: Set<String> = Countries.ALL.map { it.code }.toSet() // everyone included by default
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    init {
        viewModelScope.launch {
            val names = repository.genreNames()
            val options = names.entries
                .groupBy({ it.value }, { it.key })
                .map { (label, ids) -> GenreOption(label, ids.toSet()) }
                .sortedBy { it.label }
            _state.value = _state.value.copy(loading = false, genreOptions = options)
        }
    }

    fun toggleGenre(option: GenreOption) {
        val current = _state.value.selectedGenreIds
        val next = if (current.containsAll(option.ids)) current - option.ids else current + option.ids
        _state.value = _state.value.copy(selectedGenreIds = next)
    }

    fun toggleCountry(code: String) {
        val current = _state.value.selectedCountryCodes
        val next = if (current.contains(code)) current - code else current + code
        _state.value = _state.value.copy(selectedCountryCodes = next)
    }

    fun setAllCountries(selected: Boolean) {
        _state.value = _state.value.copy(
            selectedCountryCodes = if (selected) Countries.ALL.map { it.code }.toSet() else emptySet()
        )
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            userPrefs.setPreferredGenreIds(_state.value.selectedGenreIds)
            val allCodes = Countries.ALL.map { it.code }.toSet()
            userPrefs.setBlockedCountries(allCodes - _state.value.selectedCountryCodes)
            userPrefs.setOnboarded()
            onDone()
        }
    }
}
