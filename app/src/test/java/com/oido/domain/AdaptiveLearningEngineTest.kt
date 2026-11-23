package com.oido.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AdaptiveLearningEngineTest {

    private val engine = AdaptiveLearningEngine(random = Random(seed = 1234))

    @Test
    fun `selection always belongs to the active candidates`() {
        val candidates = listOf(Note.DO, Note.RE)
        repeat(500) {
            val selected = engine.selectNote(candidates, emptyMap(), null, 0L)
            assertTrue(selected in candidates)
        }
    }

    @Test
    fun `never repeats the previous note while alternatives exist`() {
        val candidates = Note.entries.toList()
        var previous: Note? = null
        repeat(1000) {
            val selected = engine.selectNote(candidates, emptyMap(), previous, 0L)
            assertNotEquals("consecutive repetition detected", previous, selected)
            previous = selected
        }
    }

    @Test
    fun `selection varies across calls`() {
        val candidates = Note.entries.toList()
        val distinct = (0 until 300)
            .map { engine.selectNote(candidates, emptyMap(), null, 0L) }
            .toSet()
        assertTrue("expected several different notes, got $distinct", distinct.size >= 4)
    }

    @Test
    fun `weak notes are favoured over strong ones`() {
        val candidates = listOf(Note.DO, Note.SI)
        val stats = mapOf(
            Note.DO to NoteStats(played = 20, correct = 2, incorrect = 18, lastPracticedDay = 100L),
            Note.SI to NoteStats(played = 20, correct = 20, incorrect = 0, lastPracticedDay = 100L)
        )
        val selections = (0 until 600).map {
            engine.selectNote(candidates, stats, null, 100L)
        }
        val weakCount = selections.count { it == Note.DO }
        assertTrue(
            "weak note should be picked more often, got $weakCount/600",
            weakCount > 600 * 0.6
        )
    }

    @Test
    fun `stale notes are favoured over recently practised ones`() {
        val candidates = listOf(Note.DO, Note.SI)
        val stats = mapOf(
            // Same accuracy, but DO has not been practised for a month.
            Note.DO to NoteStats(played = 10, correct = 8, incorrect = 2, lastPracticedDay = 0L),
            Note.SI to NoteStats(played = 10, correct = 8, incorrect = 2, lastPracticedDay = 100L)
        )
        val selections = (0 until 600).map {
            engine.selectNote(candidates, stats, null, 100L)
        }
        val staleCount = selections.count { it == Note.DO }
        assertTrue("stale note should be picked more often, got $staleCount/600",
            staleCount > 600 * 0.6)
    }

    @Test
    fun `never practised notes get a friendly default weight`() {
        assertEquals(1.4, engine.weightFor(null, 0L), 0.001)
        assertEquals(1.4, engine.weightFor(NoteStats(), 0L), 0.001)
    }

    @Test
    fun `requires at least two candidates`() {
        try {
            engine.selectNote(listOf(Note.DO), emptyMap(), null, 0L)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `notesToImprove filters weak notes with enough attempts`() {
        val stats = mapOf(
            Note.DO to NoteStats(played = 10, correct = 4, incorrect = 6),
            Note.RE to NoteStats(played = 10, correct = 10, incorrect = 0),
            Note.MI to NoteStats(played = 2, correct = 0, incorrect = 2), // too few attempts
            Note.FA to NoteStats(played = 8, correct = 5, incorrect = 3)  // 62.5% < 75%
        )
        val weak = engine.notesToImprove(stats)
        assertTrue(weak.contains(Note.DO))
        assertTrue(weak.contains(Note.FA))
        assertFalse(weak.contains(Note.RE))
        assertFalse(weak.contains(Note.MI))
    }
}
