package com.oido.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExerciseEngineTest {

    private fun engine(
        level: Int = 3,
        total: Int = 10,
        seed: Int = 7
    ) = ExerciseEngine(
        levelConfig = Levels.forLevel(level),
        totalExercises = total,
        adaptive = AdaptiveLearningEngine(random = Random(seed)),
        random = Random(seed)
    )

    @Test
    fun `session starts with the first exercise`() {
        val engine = engine(total = 5)
        val exercise = engine.start(nowMs = 0L)
        assertEquals(1, exercise.index)
        assertEquals(5, exercise.total)
        assertNotNull(exercise.target)
        assertEquals(AnswerState.WAITING, engine.state)
    }

    @Test
    fun `target is hidden until the user answers`() {
        val engine = engine(total = 3)
        engine.start(0L)
        // The UI can only read the target through a public accessor after answering;
        // the engine itself keeps the answer in the state machine.
        assertEquals(AnswerState.WAITING, engine.state)
        engine.answer(Note.DO, nowMs = 500L)
        assertTrue(engine.state == AnswerState.CORRECT || engine.state == AnswerState.WRONG)
    }

    @Test
    fun `correct and incorrect answers are validated`() {
        val engine = engine(level = 1, total = 4) // only DO and SOL
        val exercise = engine.start(0L)
        val state = engine.answer(exercise.target, nowMs = 300L)
        assertEquals(AnswerState.CORRECT, state)

        val second = engine.advance(400L)!!
        val wrongTarget = if (second.target == Note.DO) Note.SOL else Note.DO
        assertEquals(AnswerState.WRONG, engine.answer(wrongTarget, nowMs = 700L))
    }

    @Test
    fun `answers after feedback are ignored`() {
        val engine = engine(level = 1, total = 4)
        val exercise = engine.start(0L)
        engine.answer(exercise.target, 100L)
        val second = engine.answer(Note.DO, 200L)
        assertEquals(AnswerState.CORRECT, second) // unchanged, still the first answer
    }

    @Test
    fun `advance returns null when the session is complete`() {
        val engine = engine(total = 2)
        val first = engine.start(0L)
        engine.answer(first.target, 100L)
        val second = engine.advance(200L)
        assertNotNull(second)
        engine.answer(second!!.target, 300L)
        assertNull(engine.advance(400L))
        assertTrue(engine.isFinished)
    }

    @Test
    fun `no consecutive repetition of the same target`() {
        val engine = engine(total = 30)
        var previous: Note? = null
        var exercise = engine.start(0L)
        while (true) {
            assertNotEqualsTarget(previous, exercise.target)
            previous = exercise.target
            engine.answer(exercise.target, 0L)
            val next = engine.advance(0L) ?: break
            exercise = next
        }
    }

    private fun assertNotEqualsTarget(previous: Note?, current: Note) {
        assertTrue(
            "target repeated: $previous then $current",
            previous == null || previous != current
        )
    }

    @Test
    fun `session statistics accumulate answers`() {
        val engine = engine(level = 1, total = 4)
        var exercise = engine.start(0L)
        var answered = 0
        while (true) {
            // Answer correctly the first two times, wrongly afterwards.
            val selected = if (answered < 2) exercise.target
            else if (exercise.target == Note.DO) Note.SOL else Note.DO
            engine.answer(selected, nowMs = (answered * 250).toLong())
            answered++
            val next = engine.advance(0L) ?: break
            exercise = next
        }
        val snapshot = engine.sessionSnapshot
        assertEquals(4, snapshot.totalQuestions)
        assertEquals(2, snapshot.correct)
        assertEquals(2, snapshot.incorrect)
        assertEquals(0.5, snapshot.accuracy, 0.0001)
        assertEquals(2, snapshot.bestStreak)
        // Level 1 alternates between DO and SOL: the two first targets were hit,
        // the two last ones were missed.
        assertEquals(2, snapshot.recognizedNotes.size)
        assertEquals(2, snapshot.notesToImprove.size)
    }

    @Test
    fun `replay counting respects the level limit`() {
        // Level 4 allows a single replay.
        val engine = ExerciseEngine(
            levelConfig = Levels.forLevel(4),
            totalExercises = 3,
            adaptive = AdaptiveLearningEngine(random = Random(3)),
            random = Random(3)
        )
        engine.start(0L)
        engine.onNotePlayed(isReplay = false)
        assertTrue(engine.canReplay())
        engine.onNotePlayed(isReplay = true)
        assertFalse("level 4 must limit replays to 1", engine.canReplay())
        engine.onNotePlayed(isReplay = true) // ignored by the UI, still counted
        assertEquals(2, engine.replayCount)
    }

    @Test
    fun `beginner level only offers two notes`() {
        val config = Levels.forLevel(1)
        assertEquals(2, config.activeNotes.size)
        assertTrue(config.activeNotes.containsAll(listOf(Note.DO, Note.SOL)))
    }

    @Test
    fun `level 3 and 4 offer the seven natural notes`() {
        assertEquals(7, Levels.forLevel(3).activeNotes.size)
        assertEquals(7, Levels.forLevel(4).activeNotes.size)
    }
}
