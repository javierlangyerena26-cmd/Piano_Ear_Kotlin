package com.oido.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelProgressionTest {

    @Test
    fun `level 1 is always unlocked`() {
        assertEquals(1, LevelProgression.highestUnlockedLevel(emptyMap()))
    }

    @Test
    fun `advancing requires the minimum number of exercises`() {
        val config = Levels.forLevel(1)
        val tooFew = LevelProgress(1, exercisesPlayed = config.minExercisesToAdvance - 1,
            exercisesCorrect = config.minExercisesToAdvance) // perfect accuracy, but not enough
        assertFalse(LevelProgression.canAdvance(tooFew, config))
    }

    @Test
    fun `advancing requires the accuracy threshold`() {
        val config = Levels.forLevel(1)
        val notAccurateEnough = LevelProgress(
            level = 1,
            exercisesPlayed = 50,
            exercisesCorrect = 30 // 60% < 75%
        )
        assertFalse(LevelProgression.canAdvance(notAccurateEnough, config))
    }

    @Test
    fun `advancing happens with enough exercises and accuracy`() {
        val config = Levels.forLevel(1)
        val good = LevelProgress(
            level = 1,
            exercisesPlayed = 20,
            exercisesCorrect = 17 // 85% >= 75%
        )
        assertTrue(LevelProgression.canAdvance(good, config))
    }

    @Test
    fun `progress is never based on attempts alone`() {
        val config = Levels.forLevel(3)
        val manyAttempts = LevelProgress(3, exercisesPlayed = 500, exercisesCorrect = 100)
        assertFalse(LevelProgression.canAdvance(manyAttempts, config))
    }

    @Test
    fun `highest unlocked level walks through every passed level`() {
        val progress = mapOf(
            1 to LevelProgress(1, 30, 28),
            2 to LevelProgress(2, 30, 25),
            3 to LevelProgress(3, 10, 5) // failed level 3
        )
        assertEquals(3, LevelProgression.highestUnlockedLevel(progress))
    }

    @Test
    fun `a failed early level blocks further unlocks`() {
        val progress = mapOf(
            1 to LevelProgress(1, 30, 10), // 33% -> stuck on level 1
            2 to LevelProgress(2, 30, 30)
        )
        assertEquals(1, LevelProgression.highestUnlockedLevel(progress))
    }

    @Test
    fun `recordExercise increments the counters`() {
        val progress = LevelProgression.recordExercise(LevelProgress(1), correct = true)
        assertEquals(1, progress.exercisesPlayed)
        assertEquals(1, progress.exercisesCorrect)

        val missed = LevelProgression.recordExercise(progress, correct = false)
        assertEquals(2, missed.exercisesPlayed)
        assertEquals(1, missed.exercisesCorrect)
        assertEquals(0.5, missed.accuracy, 0.0001)
    }

    @Test
    fun `four levels exist`() {
        assertEquals(4, Levels.maxLevel)
        assertEquals(1, Levels.all.first().level)
        assertEquals(4, Levels.all.last().level)
    }

    @Test
    fun `level 4 reduces replay assistance`() {
        assertTrue(Levels.forLevel(3).maxReplays > Levels.forLevel(4).maxReplays)
        assertEquals(1, Levels.forLevel(4).maxReplays)
    }

    @Test
    fun `unlocked notes grow with the level`() {
        assertEquals(2, Levels.unlockedNotes(1).size)
        assertEquals(4, Levels.unlockedNotes(2).size)
        assertEquals(7, Levels.unlockedNotes(3).size)
        assertEquals(7, Levels.unlockedNotes(4).size)
    }

    @Test
    fun `notes of a lower level are included in the next one`() {
        assertTrue(Levels.unlockedNotes(2).containsAll(Levels.unlockedNotes(1)))
    }
}
