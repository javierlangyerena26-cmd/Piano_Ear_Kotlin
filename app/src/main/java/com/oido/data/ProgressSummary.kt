package com.oido.data

import com.oido.domain.LevelConfig
import com.oido.domain.Levels
import com.oido.domain.Note
import com.oido.domain.NoteStats
import java.time.LocalDate

/**
 * Pure, testable rules that turn raw [OidoState] into the data shown by the UI.
 * Keeping them outside the ViewModels allows unit testing without Android.
 */
object ProgressSummary {

    data class Summary(
        val totalSessions: Int,
        val accuracy: Double,
        val dailyStreak: Int,
        val bestStreak: Int,
        val unlockedLevel: Int,
        val unlockedNotes: List<Note>,
        val totalExercises: Int,
        val averageResponseMs: Long,
        val levelConfig: LevelConfig
    )

    fun from(state: OidoState): Summary {
        val config = Levels.forLevel(state.unlockedLevel)
        return Summary(
            totalSessions = state.totalSessions,
            accuracy = state.accuracy,
            dailyStreak = state.dailyStreak,
            bestStreak = state.bestStreak,
            unlockedLevel = state.unlockedLevel,
            unlockedNotes = config.activeNotes,
            totalExercises = state.totalExercises,
            averageResponseMs = state.averageResponseMs,
            levelConfig = config
        )
    }

    /** Notes recognized correctly today (used on the home screen). */
    fun notesRecognizedToday(state: OidoState, today: LocalDate = LocalDate.now()): List<Note> {
        val epochDay = today.toEpochDay()
        return state.noteStats.entries
            .filter { it.value.lastPracticedDay == epochDay && it.value.correct > 0 }
            .mapNotNull { runCatching { Note.valueOf(it.key) }.getOrNull() }
            .sorted()
    }

    /** Convenience view of a single note's stats. */
    fun statsFor(state: OidoState, note: Note): NoteStats = state.noteStatsFor(note)
}
