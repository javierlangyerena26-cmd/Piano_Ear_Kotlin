package com.oido.ui.training

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oido.audio.AudioEngine
import com.oido.data.OidoState
import com.oido.data.ProgressRepository
import com.oido.domain.AnswerState
import com.oido.domain.Exercise
import com.oido.domain.ExerciseEngine
import com.oido.domain.LevelConfig
import com.oido.domain.Levels
import com.oido.domain.Note
import com.oido.domain.SessionSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What the training screen must render. */
data class TrainingUiState(
    val level: Int = 1,
    val levelDescription: String = "",
    val exerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    /** True once the target note has been played at least once. */
    val notePlayed: Boolean = false,
    val replayCount: Int = 0,
    val replaysRemaining: Int = Int.MAX_VALUE,
    /** The target note is only exposed to the UI after the user answers. */
    val answerState: AnswerState = AnswerState.WAITING,
    val selectedNote: Note? = null,
    val targetNote: Note? = null,
    val finished: Boolean = false,
    val sessionSnapshot: SessionSnapshot? = null,
    /** Session results already persisted, to refresh the home screen. */
    val persistedState: OidoState? = null,
    val awaitingAdvance: Boolean = false
) {
    /** True while the user may still interact with the exercise. */
    val canPlay: Boolean get() = answerState == AnswerState.WAITING
    val canReplay: Boolean
        get() = canPlay && notePlayed && replaysRemaining > 0
}

/**
 * Drives the training screen.
 *
 * The target note lives only inside this ViewModel (and the engine) until the
 * user answers, so no UI element, semantics or log can leak it beforehand.
 */
class TrainingViewModel(
    private val repository: ProgressRepository,
    private val audio: AudioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var engine: ExerciseEngine? = null
    private var advanceJob: Job? = null
    private var pendingResults: MutableList<PendingResult> = mutableListOf()

    private data class PendingResult(
        val note: Note,
        val correct: Boolean,
        val responseMs: Long,
        val level: Int
    )

    /** Prepares the audio engine and renders every note buffer. */
    fun initialize() {
        audio.prepare()
    }

    /** Starts (or restarts) a session of [length] exercises on [level]. */
    fun startSession(level: Int, length: Int) {
        advanceJob?.cancel()
        pendingResults.clear()

        val config: LevelConfig = Levels.forLevel(level)

        // Synchronous placeholder so the screen never shows "0 / 0".
        _uiState.value = TrainingUiState(
            level = config.level,
            levelDescription = config.description,
            exerciseIndex = 1,
            totalExercises = length
        )

        viewModelScope.launch {
            val stats = repository.currentState().noteStats
                .filterKeys { runCatching { Note.valueOf(it) }.isSuccess }
                .mapKeys { Note.valueOf(it.key) }

            val newEngine = ExerciseEngine(
                levelConfig = config,
                totalExercises = length,
                initialStats = stats
            )
            engine = newEngine
            exerciseStartElapsed = SystemClock.elapsedRealtime()
            val exercise = newEngine.start(nowMs())
            _uiState.value = TrainingUiState(
                level = config.level,
                levelDescription = config.description,
                exerciseIndex = exercise.index,
                totalExercises = exercise.total,
                notePlayed = false,
                replaysRemaining = newEngine.replaysRemaining
            )
            // The first note is played automatically so the exercise always begins by listening.
            playCurrent()
        }
    }

    /**
     * Plays the current target note. Every playback after the automatic first one
     * counts as a replay, so level 4 really does offer less assistance.
     */
    fun playCurrent() {
        val eng = engine ?: return
        val target = eng.currentTarget ?: return
        val isReplay = _uiState.value.notePlayed
        if (isReplay && !eng.canReplay()) return
        eng.onNotePlayed(isReplay = isReplay)
        audio.play(target)
        _uiState.update {
            it.copy(
                notePlayed = true,
                replayCount = eng.replayCount,
                replaysRemaining = eng.replaysRemaining
            )
        }
    }

    fun replay() {
        if (_uiState.value.answerState != AnswerState.WAITING) return
        if (!_uiState.value.notePlayed) return
        playCurrent()
    }

    /**
     * Validates the selected key. The target note is revealed only now, and the
     * correct note is played back as feedback.
     */
    fun answer(selected: Note) {
        val eng = engine ?: return
        if (eng.state != AnswerState.WAITING) return

        val responseMs = (SystemClock.elapsedRealtime() - exerciseStartElapsed).coerceAtLeast(0L)
        val state = eng.answer(selected, nowMs())
        val target = requireNotNull(eng.currentTarget)

        // Immediate audio feedback: play the identified note again.
        audio.play(target)

        pendingResults += PendingResult(
            note = target,
            correct = state == AnswerState.CORRECT,
            responseMs = responseMs,
            level = eng.level
        )

        _uiState.update {
            it.copy(
                answerState = state,
                selectedNote = selected,
                targetNote = target,
                replaysRemaining = eng.replaysRemaining
            )
        }

        persistResults()

        // Let the user read the feedback, then move on automatically.
        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(FEEDBACK_DELAY_MS)
            if (_uiState.value.answerState != AnswerState.WAITING) advance()
        }
    }

    /** Advances immediately when the user taps "Continuar". */
    fun continueManually() {
        if (_uiState.value.answerState == AnswerState.WAITING) return
        advanceJob?.cancel()
        advance()
    }

    private fun advance() {
        val eng = engine ?: return
        val next = eng.advance(nowMs())
        if (next == null) {
            finishSession()
        } else {
            exerciseStartElapsed = SystemClock.elapsedRealtime()
            _uiState.update {
                it.copy(
                    exerciseIndex = next.index,
                    totalExercises = next.total,
                    notePlayed = false,
                    replayCount = 0,
                    replaysRemaining = eng.replaysRemaining,
                    answerState = AnswerState.WAITING,
                    selectedNote = null,
                    targetNote = null,
                    awaitingAdvance = false
                )
            }
            playCurrent()
        }
    }

    private fun finishSession() {
        val eng = engine ?: return
        val snapshot = eng.sessionSnapshot
        advanceJob?.cancel()

        viewModelScope.launch {
            repository.recordSession(snapshot)
            val persisted = repository.currentState()
            _uiState.update {
                it.copy(
                    finished = true,
                    sessionSnapshot = snapshot,
                    persistedState = persisted
                )
            }
        }
    }

    /** Persists every answered exercise as soon as it happens. */
    private fun persistResults() {
        val batch = pendingResults.toList()
        pendingResults.clear()
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            batch.forEach { result ->
                repository.recordExercise(
                    note = result.note,
                    correct = result.correct,
                    responseMs = result.responseMs,
                    level = result.level,
                    todayEpochDay = today
                )
            }
        }
    }

    /** Stops audio when leaving the screen. */
    fun shutdown() {
        advanceJob?.cancel()
        audio.stop()
    }

    override fun onCleared() {
        advanceJob?.cancel()
        audio.stop()
        super.onCleared()
    }

    private var exerciseStartElapsed: Long = SystemClock.elapsedRealtime()

    private fun nowMs(): Long = SystemClock.elapsedRealtime()

    companion object {
        /** Time the feedback stays on screen before the next exercise. */
        const val FEEDBACK_DELAY_MS = 1600L
    }
}
