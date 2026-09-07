package com.arno.showtracker.ui.screens.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arno.showtracker.data.local.WatchlistEntity
import com.arno.showtracker.data.model.MediaType
import com.arno.showtracker.data.repository.LengthPref
import com.arno.showtracker.data.repository.MediaRepository
import com.arno.showtracker.data.repository.SuggestionMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecStage { QUIZ, SWIPE }
enum class QuizType(val label: String, val mediaType: MediaType?) {
    MOVIE("Movie", MediaType.MOVIE),
    SERIES("Series", MediaType.TV),
    EITHER("Either", null)
}

data class ForYouState(
    val stage: RecStage = RecStage.QUIZ,
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
}

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ForYouState())
    val state: StateFlow<ForYouState> = _state

    init {
        viewModelScope.launch {
            val names = repository.genreNames().values.distinct().sorted()
            _state.value = _state.value.copy(genreOptions = names)
        }
    }

    fun setMood(mood: SuggestionMood) {
        _state.value = _state.value.copy(mood = mood)
    }

    fun setQuizType(type: QuizType) {
        _state.value = _state.value.copy(quizType = type)
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

    fun retakeQuiz() {
        _state.value = _state.value.copy(stage = RecStage.QUIZ)
    }

    /** liked is currently just an interaction cue - both like/skip advance to the next card. */
    fun decide(liked: Boolean) {
        _state.value = _state.value.copy(index = _state.value.index + 1)
    }
}
