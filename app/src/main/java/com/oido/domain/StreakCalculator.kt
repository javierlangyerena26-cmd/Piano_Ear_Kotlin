package com.oido.domain

/**
 * Pure helpers to compute practice streaks from calendar days.
 * All inputs are epoch days (days since 1970-01-01) so no timezone logic leaks
 * into the game rules.
 */
object StreakCalculator {

    /**
     * Returns the streak after practicing on [todayEpochDay].
     *
     * - First ever practice            -> 1
     * - Practiced yesterday            -> previous + 1
     * - Practiced today already        -> unchanged
     * - Missed one or more days        -> 1 (streak restarts)
     */
    fun updateDailyStreak(
        previousStreak: Int,
        lastPracticeDay: Long?,
        todayEpochDay: Long
    ): Int {
        if (lastPracticeDay == null) return 1
        return when (todayEpochDay - lastPracticeDay) {
            0L -> previousStreak.coerceAtLeast(1)
            1L -> previousStreak + 1
            else -> 1
        }
    }

    /** Best of two streaks. */
    fun best(previousBest: Int, current: Int): Int = maxOf(previousBest, current)

    /** Best streak inside a session, given the sequence of answers. */
    fun bestStreakOf(answers: List<Boolean>): Int {
        var best = 0
        var run = 0
        for (answer in answers) {
            if (answer) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }

    /** Current (ending) streak of consecutive correct answers. */
    fun currentStreakOf(answers: List<Boolean>): Int {
        var run = 0
        for (answer in answers.asReversed()) {
            if (!answer) break
            run++
        }
        return run
    }
}
