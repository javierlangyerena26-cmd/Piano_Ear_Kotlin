package com.oido.domain

/**
 * Result of one answered exercise.
 */
data class ExerciseResult(
    val note: Note,
    val selected: Note,
    val responseMs: Long
) {
    val isCorrect: Boolean get() = note == selected
}

/**
 * Accumulates the results of a single training session and computes the
 * statistics shown on the results screen.
 */
class SessionAccumulator {

    private val results = mutableListOf<ExerciseResult>()
    private var currentStreak = 0

    private var bestStreakValue: Int = 0

    val size: Int get() = results.size
    val correct: Int get() = results.count { it.isCorrect }
    val incorrect: Int get() = results.size - correct
    val bestStreak: Int get() = bestStreakValue

    val accuracy: Double
        get() = if (results.isEmpty()) 0.0 else correct.toDouble() / results.size

    val averageResponseMs: Long
        get() = if (results.isEmpty()) 0L else results.map { it.responseMs }.average().toLong()

    fun add(result: ExerciseResult) {
        results += result
        if (result.isCorrect) {
            currentStreak++
            if (currentStreak > bestStreakValue) bestStreakValue = currentStreak
        } else {
            currentStreak = 0
        }
    }

    /** Notes answered correctly at least once, in note order. */
    fun recognizedNotes(): List<Note> =
        results.filter { it.isCorrect }.map { it.note }.distinct()

    /** Notes missed at least once, ordered by number of misses (worst first). */
    fun notesToImprove(): List<Note> =
        results.filter { !it.isCorrect }
            .groupingBy { it.note }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

    /** Current streak of consecutive correct answers. */
    fun currentStreak(): Int = currentStreak

    fun snapshot(): SessionSnapshot = SessionSnapshot(
        totalQuestions = results.size,
        correct = correct,
        incorrect = incorrect,
        accuracy = accuracy,
        averageResponseMs = averageResponseMs,
        bestStreak = bestStreakValue,
        recognizedNotes = recognizedNotes(),
        notesToImprove = notesToImprove()
    )
}

/**
 * Immutable summary of a finished session, consumed by the results UI.
 */
data class SessionSnapshot(
    val totalQuestions: Int,
    val correct: Int,
    val incorrect: Int,
    val accuracy: Double,
    val averageResponseMs: Long,
    val bestStreak: Int,
    val recognizedNotes: List<Note>,
    val notesToImprove: List<Note>
)
