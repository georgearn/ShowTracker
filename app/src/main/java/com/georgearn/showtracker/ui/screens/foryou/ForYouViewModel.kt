package com.georgearn.showtracker.ui.screens.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgearn.showtracker.data.local.WatchlistEntity
import com.georgearn.showtracker.data.model.MediaType
import com.georgearn.showtracker.data.repository.LengthPref
import com.georgearn.showtracker.data.repository.MediaRepository
import com.georgearn.showtracker.data.repository.genreList
import com.georgearn.showtracker.data.repository.SuggestionMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecStage { INTRO, QUIZ, SWIPE }

enum class QuizType(val label: String, val mediaType: MediaType?) {
    MOVIE("Movie", MediaType.MOVIE),
    SERIES("Series", MediaType.TV),
    EITHER("Either", null)
}

data class ForYouState(
    val stage: RecStage = RecStage.INTRO,
    val quizStep: Int = 0,
    val mood: SuggestionMood = SuggestionMood.ANYTHING,
    val quizType: QuizType = QuizType.EITHER,
    val length: LengthPref = LengthPref.ANY,
    val genreOptions: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val queue: List<WatchlistEntity> = emptyList(),
    val index: Int = 0
) {
    val current: WatchlistEntity? get() = queue.getOrNull(index)
    val exhausted: Boolean get() = queue.isNotEmpty() && index >= queue.size
    val totalSteps: Int get() = if (genreOptions.isNotEmpty()) 4 else 3
}

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForYouState())
    val state: StateFlow<ForYouState> = _state

    private var pool: List<WatchlistEntity> = emptyList()

    init {
        // Quick adds made before genres were stored have none - fetch them once in the background.
        viewModelScope.launch { runCatching { repository.backfillWatchlistMetadata() } }
        repository.observeSuggestionPool()
            .onEach { released ->
                pool = released
                refreshGenreOptions()
            }
            .launchIn(viewModelScope)
    }

    /**
     * Genre chips come from what's actually pickable: released, unwatched titles on the list,
     * narrowed to the chosen type. Picks that no longer exist in the pool are dropped.
     */
    private fun refreshGenreOptions() {
        val s = _state.value
        val type = s.quizType.mediaType
        val options = pool
            .filter { type == null || it.mediaType == type.apiValue }
            .flatMap { it.genreList() }
            .distinct()
            .sorted()
        _state.value = s.copy(genreOptions = options, selectedGenres = s.selectedGenres.intersect(options.toSet()))
    }

    fun startQuiz() {
        _state.value = _state.value.copy(stage = RecStage.QUIZ, quizStep = 0)
    }

    fun nextQuizStep() {
        val s = _state.value
        if (s.quizStep < s.totalSteps - 1) {
            _state.value = s.copy(quizStep = s.quizStep + 1)
        } else {
            startRecs()
        }
    }

    fun previousQuizStep() {
        val s = _state.value
        if (s.quizStep > 0) {
            _state.value = s.copy(quizStep = s.quizStep - 1)
        } else {
            _state.value = s.copy(stage = RecStage.INTRO)
        }
    }

    fun setMood(mood: SuggestionMood) {
        _state.value = _state.value.copy(mood = mood)
    }

    fun setQuizType(type: QuizType) {
        _state.value = _state.value.copy(quizType = type)
        refreshGenreOptions()
    }

    fun setLength(length: LengthPref) {
        _state.value = _state.value.copy(length = length)
    }

    fun toggleGenre(genre: String) {
        val current = _state.value.selectedGenres
        _state.value = _state.value.copy(selectedGenres = if (genre in current) current - genre else current + genre)
    }

    fun startRecs() {
        viewModelScope.launch {
            val s = _state.value
            val queue = repository.suggestionQueue(s.mood, s.quizType.mediaType, s.length, s.selectedGenres.ifEmpty { null })
            _state.value = s.copy(stage = RecStage.SWIPE, queue = queue, index = 0)
        }
    }

    /** Skips the quiz entirely: random mood, media type and length, shuffled queue. */
    fun startFullyRandom() {
        viewModelScope.launch {
            val mood = SuggestionMood.entries.random()
            val type = QuizType.entries.random()
            val length = LengthPref.entries.random()
            val queue = repository.suggestionQueue(mood, type.mediaType, length)
            _state.value = _state.value.copy(
                stage = RecStage.SWIPE,
                mood = mood,
                quizType = type,
                length = length,
                selectedGenres = emptySet(),
                queue = queue,
                index = 0
            )
        }
    }

    fun reshuffle() {
        viewModelScope.launch {
            val s = _state.value
            val queue = repository.suggestionQueue(s.mood, s.quizType.mediaType, s.length, s.selectedGenres.ifEmpty { null })
            _state.value = s.copy(queue = queue, index = 0)
        }
    }

    fun resetToIntro() {
        _state.value = _state.value.copy(stage = RecStage.INTRO, quizStep = 0)
    }

    fun decide(liked: Boolean) {
        _state.value = _state.value.copy(index = _state.value.index + 1)
    }
}
