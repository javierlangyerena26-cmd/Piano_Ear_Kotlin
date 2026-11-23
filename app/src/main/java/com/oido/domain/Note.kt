package com.oido.domain

/**
 * The seven natural notes used by the first version of OIDO.
 *
 * Each note carries its Spanish display name and the reference frequency of the
 * fixed initial octave (octave 4). Sharps, flats and other octaves are deliberately
 * out of scope for the MVP, but the model is ready to be extended later.
 */
enum class Note(
    val displayName: String,
    val frequency: Double,
    val octave: Int
) {
    DO("DO", 261.63, 4),
    RE("RE", 293.66, 4),
    MI("MI", 329.63, 4),
    FA("FA", 349.23, 4),
    SOL("SOL", 392.00, 4),
    LA("LA", 440.00, 4),
    SI("SI", 493.88, 4);

    /** Human readable label including the octave, e.g. "DO4". */
    val labelWithOctave: String get() = "$displayName$octave"
}

/**
 * Maps [Note] values to their reference frequency in Hz.
 *
 * This is the single source of truth for pitch so the audio engine, the UI and
 * the tests all agree on the exact frequencies.
 */
object NoteFrequencyProvider {
    fun frequencyOf(note: Note): Double = note.frequency

    fun allFrequencies(): Map<Note, Double> = Note.entries.associateWith { it.frequency }
}
