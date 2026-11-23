package com.oido.ui.familiar

import androidx.lifecycle.ViewModel
import com.oido.audio.AudioEngine
import com.oido.domain.Note

/**
 * Backing logic for the "Nota familiar" mode: free exploration where every key
 * shows its name and plays its sound.
 */
class FamiliarViewModel(
    private val audio: AudioEngine
) : ViewModel() {

    /** Last key pressed, used to drive the pressed state of the keyboard. */
    var lastPressed: Note? = null
        private set

    fun initialize() {
        audio.prepare()
    }

    fun onKeyPressed(note: Note) {
        lastPressed = note
        audio.play(note)
    }

    fun stop() {
        audio.stop()
    }

    override fun onCleared() {
        audio.stop()
        super.onCleared()
    }
}
