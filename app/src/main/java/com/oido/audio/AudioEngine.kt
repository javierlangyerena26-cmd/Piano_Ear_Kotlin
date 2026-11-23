package com.oido.audio

import com.oido.domain.Note

/**
 * Abstraction of the note playback component.
 *
 * The UI never talks to Android audio APIs directly; it only depends on this
 * interface, which also makes the engine replaceable and testable.
 */
interface AudioEngine {

    /** Pre-renders every note buffer. Safe to call more than once. */
    fun prepare()

    /** Plays [note] from the beginning, interrupting any note currently sounding. */
    fun play(note: Note)

    /** Fades out and stops whatever is playing. Never clicks. */
    fun stop()

    /** True while a note buffer is still being emitted to the audio output. */
    fun isPlaying(): Boolean

    /** Stops playback and releases the underlying audio resources. */
    fun release()
}
