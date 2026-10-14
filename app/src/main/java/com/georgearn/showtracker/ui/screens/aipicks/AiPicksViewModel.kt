package com.georgearn.showtracker.ui.screens.aipicks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.UserPrefs
import com.georgearn.showtracker.data.model.MediaSummary
import com.georgearn.showtracker.data.repository.AiException
import com.georgearn.showtracker.data.repository.AiPick
import com.georgearn.showtracker.data.repository.AiRecommendationRepository
import com.georgearn.showtracker.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SeedMode(val label: String) { WATCHLIST("My list"), TITLE("A title") }

data class AiPicksState(
    val seedMode: SeedMode = SeedMode.WATCHLIST,
    val seedTitle: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val picks: List<AiPick> = emptyList(),
    val error: AiException? = null
) {
    val canSubmit: Boolean get() = !isLoading && (seedMode == SeedMode.WATCHLIST || seedTitle.isNotBlank())
}

@HiltViewModel
class AiPicksViewModel @Inject constructor(
    private val ai: AiRecommendationRepository,
    private val repository: MediaRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(AiPicksState())
    val state: StateFlow<AiPicksState> = _state

    /** null = still reading prefs, so the screen doesn't flash the setup card for a user who has a key. */
    val hasKey: StateFlow<Boolean?> = userPrefs.geminiApiKey.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedKeys: StateFlow<Set<String>> = repository.savedKeys

    fun saveKey(key: String) {
        viewModelScope.launch {
            userPrefs.setGeminiApiKey(key)
            _state.update { it.copy(error = null) }
        }
    }

    fun setSeedMode(mode: SeedMode) = _state.update { it.copy(seedMode = mode) }
    fun setSeedTitle(value: String) = _state.update { it.copy(seedTitle = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    fun suggest() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val seeds = if (s.seedMode == SeedMode.WATCHLIST) ai.watchlistTitles() else listOf(s.seedTitle.trim())
                if (seeds.isEmpty() && s.note.isBlank()) {
                    throw AiException(AiException.Kind.EMPTY, "Your list is empty. Save a few titles, or switch to \"A title\".")
                }
                val picks = ai.suggest(seeds, s.note)
                _state.update { it.copy(isLoading = false, picks = picks) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AiException) {
                _state.update { it.copy(isLoading = false, error = e) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = AiException(AiException.Kind.OTHER, "Something went wrong. Try again."))
                }
            }
        }
    }

    fun toggleWatchlist(item: MediaSummary) {
        viewModelScope.launch { repository.quickToggleWatchlist(item) }
    }
}
