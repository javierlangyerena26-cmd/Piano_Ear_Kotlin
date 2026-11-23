package com.oido.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oido.data.OidoState
import com.oido.data.ProgressRepository
import com.oido.data.ProgressSummary
import com.oido.domain.Levels
import com.oido.domain.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home screen state: overall progress, unlocked level and daily goal progress.
 */
class HomeViewModel(
    private val repository: ProgressRepository
) : ViewModel() {

    data class HomeUiState(
        val loaded: Boolean = false,
        val totalSessions: Int = 0,
        val totalExercises: Int = 0,
        val accuracy: Double = 0.0,
        val dailyStreak: Int = 0,
        val bestStreak: Int = 0,
        val unlockedLevel: Int = 1,
        val unlockedNotes: List<Note> = emptyList(),
        val todayExercises: Int = 0,
        val todayCorrect: Int = 0,
        val averageResponseMs: Long = 0L,
        val weakNotes: List<Note> = emptyList(),
        val sessionLength: Int = ProgressRepository.DEFAULT_SESSION_LENGTH
    ) {
        /** Progress towards the daily goal of 20 answered exercises. */
        val dailyGoalProgress: Float
            get() = (todayExercises / DAILY_GOAL.toFloat()).coerceIn(0f, 1f)

        val todayAccuracy: Double
            get() = if (todayExercises == 0) 0.0
            else todayCorrect.toDouble() / todayExercises

        companion object {
            const val DAILY_GOAL = 20
        }
    }

    val uiState: StateFlow<HomeUiState> = repository.state
        .map { state -> buildState(state, repository.currentSessionLength()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private fun buildState(state: OidoState, sessionLength: Int): HomeUiState {
        val summary = ProgressSummary.from(state)
        val today = java.time.LocalDate.now().toEpochDay()
        val todayStats = state.noteStats.values.filter { it.lastPracticedDay == today }
        return HomeUiState(
            loaded = true,
            totalSessions = summary.totalSessions,
            totalExercises = summary.totalExercises,
            accuracy = summary.accuracy,
            dailyStreak = summary.dailyStreak,
            bestStreak = summary.bestStreak,
            unlockedLevel = summary.unlockedLevel,
            unlockedNotes = summary.unlockedNotes,
            todayExercises = todayStats.sumOf { it.played },
            todayCorrect = todayStats.sumOf { it.correct },
            averageResponseMs = summary.averageResponseMs,
            weakNotes = state.weakNotes(),
            sessionLength = sessionLength
        )
    }

    fun setSessionLength(length: Int) {
        viewModelScope.launch { repository.setSessionLength(length) }
    }
}
