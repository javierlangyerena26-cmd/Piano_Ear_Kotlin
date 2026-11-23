package com.oido.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAndStatsTest {

    @Test
    fun `accuracy is correct`() {
        val accumulator = SessionAccumulator()
        accumulator.add(ExerciseResult(Note.DO, Note.DO, 900))
        accumulator.add(ExerciseResult(Note.RE, Note.MI, 1200))
        accumulator.add(ExerciseResult(Note.FA, Note.FA, 700))
        val snapshot = accumulator.snapshot()
        assertEquals(3, snapshot.totalQuestions)
        assertEquals(2, snapshot.correct)
        assertEquals(1, snapshot.incorrect)
        assertEquals(2.0 / 3.0, snapshot.accuracy, 0.0001)
    }

    @Test
    fun `average response time is computed over correct answers and all answers`() {
        val accumulator = SessionAccumulator()
        accumulator.add(ExerciseResult(Note.DO, Note.DO, 1000))
        accumulator.add(ExerciseResult(Note.RE, Note.MI, 2000))
        assertEquals(1500L, accumulator.averageResponseMs)
    }

    @Test
    fun `best streak counts consecutive correct answers`() {
        val accumulator = SessionAccumulator()
        accumulator.add(ExerciseResult(Note.DO, Note.DO, 1)) // 1
        accumulator.add(ExerciseResult(Note.RE, Note.RE, 1)) // 2
        accumulator.add(ExerciseResult(Note.MI, Note.FA, 1)) // reset
        accumulator.add(ExerciseResult(Note.FA, Note.FA, 1)) // 1
        accumulator.add(ExerciseResult(Note.SOL, Note.SOL, 1)) // 2
        accumulator.add(ExerciseResult(Note.LA, Note.LA, 1)) // 3
        assertEquals(3, accumulator.bestStreak)
        assertEquals(3, accumulator.currentStreak())
    }

    @Test
    fun `empty session has zero accuracy and no notes`() {
        val snapshot = SessionAccumulator().snapshot()
        assertEquals(0, snapshot.totalQuestions)
        assertEquals(0.0, snapshot.accuracy, 0.0001)
        assertTrue(snapshot.recognizedNotes.isEmpty())
        assertTrue(snapshot.notesToImprove.isEmpty())
    }

    @Test
    fun `recognized and weak notes are grouped`() {
        val accumulator = SessionAccumulator()
        accumulator.add(ExerciseResult(Note.DO, Note.DO, 1))      // hit DO
        accumulator.add(ExerciseResult(Note.DO, Note.RE, 1))      // miss DO (1)
        accumulator.add(ExerciseResult(Note.SI, Note.SI, 1))      // hit SI
        accumulator.add(ExerciseResult(Note.SI, Note.LA, 1))      // miss SI (1)
        accumulator.add(ExerciseResult(Note.SI, Note.DO, 1))      // miss SI (2)
        val snapshot = accumulator.snapshot()
        assertEquals(listOf(Note.DO, Note.SI), snapshot.recognizedNotes.sorted())
        // Worst first: SI was missed twice, DO once.
        assertEquals(listOf(Note.SI, Note.DO), snapshot.notesToImprove)
    }

    @Test
    fun `daily streak continues, breaks and restarts`() {
        assertEquals(1, StreakCalculator.updateDailyStreak(0, null, 100))
        assertEquals(5, StreakCalculator.updateDailyStreak(4, 99, 100))   // yesterday
        assertEquals(5, StreakCalculator.updateDailyStreak(5, 100, 100))  // today again
        assertEquals(1, StreakCalculator.updateDailyStreak(5, 97, 100))   // gap
    }

    @Test
    fun `best of two streaks`() {
        assertEquals(7, StreakCalculator.best(7, 3))
        assertEquals(9, StreakCalculator.best(4, 9))
    }

    @Test
    fun `best streak from an answer sequence`() {
        val answers = listOf(true, true, false, true, true, true, true)
        assertEquals(4, StreakCalculator.bestStreakOf(answers))
        assertEquals(4, StreakCalculator.currentStreakOf(answers))
        assertEquals(0, StreakCalculator.bestStreakOf(emptyList()))
        assertEquals(0, StreakCalculator.bestStreakOf(listOf(false, false)))
    }
}
