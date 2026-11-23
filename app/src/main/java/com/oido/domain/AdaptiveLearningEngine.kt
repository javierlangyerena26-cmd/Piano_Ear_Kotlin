package com.oido.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

/** Per note performance recorded by the adaptive engine. */
@Serializable
data class NoteStats(
    val played: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
    val totalResponseMs: Long = 0L,
    /** Epoch day of the last time this note was practiced, or null. */
    val lastPracticedDay: Long? = null
) {
    val accuracy: Double
        get() = if (played == 0) 0.0 else correct.toDouble() / played

    val averageResponseMs: Long
        get() = if (correct == 0) 0L else totalResponseMs / correct
}

/**
 * Basic adaptive selection algorithm.
 *
 * The candidate notes are weighted by three simple, explainable factors:
 *  1. Weak notes (low accuracy) get more weight.
 *  2. Notes not practiced recently get more weight.
 *  3. A small random factor keeps the sequence unpredictable.
 *
 * On top of that the previous note is excluded whenever at least two notes are
 * active, so the answer can never be deduced from repetition.
 *
 * The algorithm is deterministic for a given seed, which makes it testable.
 */
class AdaptiveLearningEngine(
    private val random: Random = Random.Default,
    private val stalenessWeight: Double = 0.6,
    private val weaknessWeight: Double = 1.0,
    private val randomWeight: Double = 0.4
) {

    /**
     * Selects the next note to play.
     *
     * @param candidates notes active for the current level (size >= 2)
     * @param stats per note statistics, may be empty
     * @param previousNote the note played in the previous exercise, or null
     * @param todayEpochDay current day, used to measure staleness
     */
    fun selectNote(
        candidates: List<Note>,
        stats: Map<Note, NoteStats>,
        previousNote: Note?,
        todayEpochDay: Long
    ): Note {
        require(candidates.size >= 2) { "At least two candidate notes are required" }

        // Never repeat the previous note while alternatives exist.
        val pool = if (previousNote != null && candidates.size > 1) {
            candidates.filter { it != previousNote }.ifEmpty { candidates }
        } else {
            candidates
        }

        val weights = pool.map { note ->
            note to weightFor(stats[note], todayEpochDay)
        }
        val total = weights.sumOf { it.second }
        var target = random.nextDouble(total)
        for ((note, weight) in weights) {
            target -= weight
            if (target <= 0.0) return note
        }
        return weights.last().first
    }

    /**
     * Weight of a single note. Higher means more likely to be selected.
     *
     * - Never practiced notes get a friendly default so new users meet all notes.
     * - Weak notes (accuracy below 1.0) are boosted proportionally.
     * - Notes untouched for a week are boosted up to [stalenessWeight].
     * - A random jitter prevents a fully predictable order.
     */
    fun weightFor(stats: NoteStats?, todayEpochDay: Long): Double {
        if (stats == null || stats.played == 0) return 1.0 + randomWeight

        val weakness = (1.0 - stats.accuracy).coerceIn(0.0, 1.0)
        val daysIdle = (todayEpochDay - (stats.lastPracticedDay ?: todayEpochDay))
            .coerceAtLeast(0L)
        val staleness = (daysIdle / 7.0).coerceIn(0.0, 1.0)

        return 1.0 +
            weaknessWeight * weakness +
            stalenessWeight * staleness +
            random.nextDouble() * randomWeight
    }

    /**
     * Notes that need improvement: accuracy below [threshold] with at least
     * [minPlayed] attempts. Used by the session results screen.
     */
    fun notesToImprove(
        stats: Map<Note, NoteStats>,
        threshold: Double = 0.75,
        minPlayed: Int = 3
    ): List<Note> = stats.values
        .filter { it.played >= minPlayed && it.accuracy < threshold }
        .let { weak ->
            stats.filterValues { weak.contains(it) }.keys.toList()
        }
}
