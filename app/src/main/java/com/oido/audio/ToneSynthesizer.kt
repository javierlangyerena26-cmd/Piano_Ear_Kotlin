package com.oido.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders a piano-like tone as 16 bit PCM samples.
 *
 * The sound is synthesized, not sampled, which guarantees:
 *  - exact pitch for every note (integer harmonic series over the reference frequency),
 *  - an identical timbre across all notes, so the user learns pitch instead of
 *    recognizing recording quirks,
 *  - fully offline operation with no bundled assets,
 *  - a short attack and a natural, exponential decay,
 *  - silence at both ends of the buffer, so starting and stopping never clicks.
 *
 * This object is pure Kotlin (no Android APIs) so it can be unit tested on the JVM.
 */
object ToneSynthesizer {

    const val DEFAULT_SAMPLE_RATE = 44100
    private const val HARMONIC_COUNT = 10
    private const val ATTACK_MS = 5.0
    private const val RELEASE_MS = 60.0

    /**
     * Renders [frequency] Hz for [durationMs] milliseconds.
     *
     * @param durationMs total length of the buffer, including attack and release.
     * @param sampleRate output sample rate in Hz.
     * @param peak target peak amplitude in (0..1]; keeps all notes at a comparable
     *             loudness so volume is not a hint for identifying notes.
     * @param seed seed of the tiny deterministic excitation noise; a fixed seed keeps
     *             rendering reproducible for tests.
     */
    fun render(
        frequency: Double,
        durationMs: Int = 2600,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        peak: Double = 0.85,
        seed: Long = 42L
    ): ShortArray {
        require(frequency > 0.0) { "frequency must be positive" }
        require(durationMs > 0) { "durationMs must be positive" }
        require(sampleRate > 0) { "sampleRate must be positive" }

        val sampleCount = sampleRate * durationMs / 1000
        val samples = DoubleArray(sampleCount)
        val random = Random(seed)

        // Harmonic amplitudes: fixed rolloff shared by every note => consistent timbre.
        val amplitudes = DoubleArray(HARMONIC_COUNT + 1)
        for (n in 1..HARMONIC_COUNT) {
            amplitudes[n] = 1.0 / Math.pow(n.toDouble(), 1.35)
        }

        // Base decay time: higher notes decay faster, as on a real piano.
        val baseTauSeconds = (1.9 * Math.pow(440.0 / frequency, 0.6)).coerceIn(0.8, 3.2)
        val attackSamples = (sampleRate * ATTACK_MS / 1000.0).toInt().coerceAtLeast(1)
        val releaseSamples = (sampleRate * RELEASE_MS / 1000.0).toInt().coerceAtLeast(1)

        val twoPiF = 2.0 * PI / sampleRate

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            var sample = 0.0
            for (n in 1..HARMONIC_COUNT) {
                // Higher partials die out sooner.
                val tau = baseTauSeconds / (1.0 + 0.30 * (n - 1))
                val amplitude = amplitudes[n] * exp(-t / tau)
                sample += amplitude * sin(twoPiF * frequency * n * i)
            }

            // Extremely short, low level excitation burst: gives a piano-like onset.
            if (i < attackSamples) {
                sample += 0.05 * random.nextDouble(-1.0, 1.0) * (1.0 - i.toDouble() / attackSamples)
            }

            // Attack ramp (raised cosine) keeps the onset click free.
            val attack = if (i < attackSamples) {
                0.5 - 0.5 * cos(PI * i / attackSamples)
            } else {
                1.0
            }
            samples[i] = sample * attack
        }

        // Release ramp guarantees the buffer ends exactly at zero.
        val releaseStart = sampleCount - releaseSamples
        if (releaseStart > 0) {
            for (i in releaseStart until sampleCount) {
                val progress = (i - releaseStart).toDouble() / releaseSamples
                samples[i] *= (1.0 - progress)
            }
        }
        samples[0] = 0.0
        samples[sampleCount - 1] = 0.0

        // Normalize to the requested peak so loudness is identical across notes.
        var maxAbs = 0.0
        for (s in samples) {
            val a = kotlin.math.abs(s)
            if (a > maxAbs) maxAbs = a
        }
        val scale = if (maxAbs > 0.0) peak / maxAbs else 0.0

        val out = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val v = samples[i] * scale * Short.MAX_VALUE
            out[i] = v.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
