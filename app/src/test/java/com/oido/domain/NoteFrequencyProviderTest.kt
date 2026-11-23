package com.oido.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteFrequencyProviderTest {

    @Test
    fun `reference frequencies match the specification`() {
        assertEquals(261.63, NoteFrequencyProvider.frequencyOf(Note.DO), 0.0001)
        assertEquals(293.66, NoteFrequencyProvider.frequencyOf(Note.RE), 0.0001)
        assertEquals(329.63, NoteFrequencyProvider.frequencyOf(Note.MI), 0.0001)
        assertEquals(349.23, NoteFrequencyProvider.frequencyOf(Note.FA), 0.0001)
        assertEquals(392.00, NoteFrequencyProvider.frequencyOf(Note.SOL), 0.0001)
        assertEquals(440.00, NoteFrequencyProvider.frequencyOf(Note.LA), 0.0001)
        assertEquals(493.88, NoteFrequencyProvider.frequencyOf(Note.SI), 0.0001)
    }

    @Test
    fun `there are exactly seven natural notes`() {
        assertEquals(7, Note.entries.size)
    }

    @Test
    fun `spanish display names are correct`() {
        assertEquals(listOf("DO", "RE", "MI", "FA", "SOL", "LA", "SI"),
            Note.entries.map { it.displayName })
    }

    @Test
    fun `frequencies increase monotonically`() {
        val frequencies = Note.entries.map { it.frequency }
        for (i in 1 until frequencies.size) {
            assertTrue(
                "frequency $i must be greater than ${i - 1}",
                frequencies[i] > frequencies[i - 1]
            )
        }
    }

    @Test
    fun `all frequencies belong to the fixed octave 4`() {
        assertTrue(Note.entries.all { it.octave == 4 })
    }

    @Test
    fun `allFrequencies contains every note`() {
        val map = NoteFrequencyProvider.allFrequencies()
        assertEquals(7, map.size)
        Note.entries.forEach { assertTrue(map.containsKey(it)) }
    }
}
