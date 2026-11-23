package com.oido.data

import com.oido.domain.LevelProgress
import com.oido.domain.Note
import com.oido.domain.NoteStats
import kotlinx.serialization.Serializable

/**
 * Persisted, serializable snapshot of everything the user has achieved.
 * Stored locally with DataStore; no account and no network involved.
 */
@Serializable
data class OidoState(
    val totalSessions: Int = 0,
    val totalExercises: Int = 0,
    val totalCorrect: Int = 0,
    val totalIncorrect: Int = 0,
    /** Best streak of consecutive correct answers ever achieved. */
    val bestStreak: Int = 0,
    /** Daily practice streak. */
    val dailyStreak: Int = 0,
    /** Epoch day of the last practice, used by the daily streak. */
    val lastPracticeDay: Long? = null,
    /** Epoch days on which at least one exercise was answered. */
    val practiceDays: List<Long> = emptyList(),
    /** Highest level unlocked so far (1..4). */
    val unlockedLevel: Int = 1,
    /** Per level performance used to decide progression. */
    val levelProgress: Map<Int, LevelProgress> = emptyMap(),
    /** Per note adaptive statistics, keyed by the note name. */
    val noteStats: Map<String, NoteStats> = emptyMap(),
    /** Total time spent answering, in milliseconds. */
    val totalResponseMs: Long = 0L,
    val responseCount: Int = 0
) {
    val accuracy: Double
        get() = if (totalExercises == 0) 0.0
        else totalCorrect.toDouble() / totalExercises

    val averageResponseMs: Long
        get() = if (responseCount == 0) 0L else totalResponseMs / responseCount

    fun noteStatsFor(note: Note): NoteStats = noteStats[note.name] ?: NoteStats()

    /** Notes never practiced or below [threshold] accuracy, sorted worst first. */
    fun weakNotes(threshold: Double = 0.75, minPlayed: Int = 3): List<Note> =
        noteStats.entries
            .filter { it.value.played >= minPlayed && it.value.accuracy < threshold }
            .sortedBy { it.value.accuracy }
            .mapNotNull { runCatching { Note.valueOf(it.key) }.getOrNull() }

    /** Notes with accuracy at or above [threshold], most practiced first. */
    fun strongNotes(threshold: Double = 0.75, minPlayed: Int = 3): List<Note> =
        noteStats.entries
            .filter { it.value.played >= minPlayed && it.value.accuracy >= threshold }
            .sortedByDescending { it.value.accuracy }
            .mapNotNull { runCatching { Note.valueOf(it.key) }.getOrNull() }
}
