package com.oido.audio

import com.oido.domain.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Validates the synthesized audio: exact pitch, duration, amplitude limits and
 * click free boundaries. These tests run on the JVM, no device needed.
 */
class ToneSynthesizerTest {

    private fun countZeroCrossings(samples: ShortArray, from: Int, to: Int): Int {
        var crossings = 0
        for (i in (from + 1) until to) {
            val previous = samples[i - 1].toInt()
            val current = samples[i].toInt()
            if (previous <= 0 && current > 0) crossings++
        }
        return crossings
    }

    @Test
    fun `buffer has the requested length`() {
        val samples = ToneSynthesizer.render(Note.LA.frequency, durationMs = 2000)
        assertEquals(44100 * 2, samples.size)
    }

    @Test
    fun `fundamental frequency matches the reference of every note`() {
        // Measure zero crossings of a 1 second window in the middle of the buffer
        // (after the attack, well before the release) and compare with the target.
        Note.entries.forEach { note ->
            val samples = ToneSynthesizer.render(note.frequency, durationMs = 2000)
            val from = 8000
            val to = from + 22050 // 0.5 seconds
            val crossings = countZeroCrossings(samples, from, to)
            val measured = crossings / 0.5 // positive crossings per second
            val error = abs(measured - note.frequency) / note.frequency
            assertTrue(
                "${note.displayName}: expected ${note.frequency} Hz, measured ~$measured Hz",
                error < 0.05
            )
        }
    }

    @Test
    fun `buffer starts and ends at silence so playback never clicks`() {
        Note.entries.forEach { note ->
            val samples = ToneSynthesizer.render(note.frequency, durationMs = 1500)
            assertEquals("${note.displayName} first sample", 0, samples.first().toInt())
            assertEquals("${note.displayName} last sample", 0, samples.last().toInt())

            // Continuity check: catches hard cuts and 16 bit overflow wraps.
            // A legitimate waveform with partials up to ~10x the fundamental can
            // move quickly, but never by a full scale jump.
            var maxDelta = 0
            for (i in 1 until 4000) {
                val delta = kotlin.math.abs(samples[i].toInt() - samples[i - 1].toInt())
                if (delta > maxDelta) maxDelta = delta
            }
            assertTrue(
                "${note.displayName} discontinuity detected: delta=$maxDelta",
                maxDelta < 12000
            )
        }
    }

    @Test
    fun `samples stay within the 16 bit range and use a consistent peak`() {
        Note.entries.forEach { note ->
            val samples = ToneSynthesizer.render(note.frequency, durationMs = 1500)
            val maxAbs = samples.maxOf { abs(it.toInt()) }
            assertTrue("${note.displayName} clips", maxAbs <= Short.MAX_VALUE)
            // Peak is normalized to 0.85 => around 27800 +/- rounding.
            assertTrue(
                "${note.displayName} unexpected peak $maxAbs",
                maxAbs in 27000..28600
            )
        }
    }

    @Test
    fun `the tone has energy throughout its duration (natural decay)`() {
        val samples = ToneSynthesizer.render(Note.DO.frequency, durationMs = 2000)
        fun rms(from: Int, to: Int): Double {
            var sum = 0.0
            for (i in from until to) {
                val v = samples[i].toDouble()
                sum += v * v
            }
            return kotlin.math.sqrt(sum / (to - from))
        }
        val start = rms(4000, 8000)
        val middle = rms(40000, 44000)
        val late = rms(70000, 74000)
        assertTrue("start should be loud", start > 3000)
        assertTrue("should still sound in the middle ($middle)", middle > 500)
        assertTrue("should still sound late ($late)", late > 150)
        assertTrue("must decay over time", middle < start)
    }

    @Test
    fun `rendering is deterministic for a fixed seed`() {
        val a = ToneSynthesizer.render(Note.SOL.frequency, durationMs = 400, seed = 7)
        val b = ToneSynthesizer.render(Note.SOL.frequency, durationMs = 400, seed = 7)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `invalid parameters are rejected`() {
        try {
            ToneSynthesizer.render(-1.0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `every note uses the same duration and sample rate`() {
        val sizes = Note.entries.map {
            ToneSynthesizer.render(it.frequency, durationMs = 2600).size
        }.toSet()
        assertEquals(setOf(44100 * 2600 / 1000), sizes)
    }
}
