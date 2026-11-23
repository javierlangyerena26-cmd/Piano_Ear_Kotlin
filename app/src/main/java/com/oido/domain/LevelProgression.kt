package com.oido.domain

import kotlinx.serialization.Serializable

/**
 * Performance accumulated on the current level. Used to decide when the next
 * level is unlocked.
 */
@Serializable
data class LevelProgress(
    val level: Int,
    val exercisesPlayed: Int = 0,
    val exercisesCorrect: Int = 0
) {
    val accuracy: Double
        get() = if (exercisesPlayed == 0) 0.0 else exercisesCorrect.toDouble() / exercisesPlayed
}

/**
 * Decides level progression from real performance.
 *
 * A level is passed when the user has played at least
 * [LevelConfig.minExercisesToAdvance] exercises on that level and reached
 * [LevelConfig.accuracyThreshold]. Progress is never based on attempts alone.
 */
object LevelProgression {

    fun canAdvance(progress: LevelProgress, config: LevelConfig): Boolean =
        progress.level == config.level &&
            progress.exercisesPlayed >= config.minExercisesToAdvance &&
            progress.accuracy >= config.accuracyThreshold

    /**
     * Returns the highest unlocked level given the per level progress map.
     * Level 1 is always unlocked.
     */
    fun highestUnlockedLevel(progressByLevel: Map<Int, LevelProgress>): Int {
        var unlocked = 1
        for (level in 1 until Levels.maxLevel) {
            val config = Levels.forLevel(level)
            val progress = progressByLevel[level] ?: LevelProgress(level)
            if (canAdvance(progress, config)) {
                unlocked = level + 1
            } else {
                break
            }
        }
        return unlocked
    }

    /** Applies one answered exercise to the progress of [level]. */
    fun recordExercise(
        progress: LevelProgress,
        correct: Boolean
    ): LevelProgress = progress.copy(
        exercisesPlayed = progress.exercisesPlayed + 1,
        exercisesCorrect = progress.exercisesCorrect + if (correct) 1 else 0
    )
}
