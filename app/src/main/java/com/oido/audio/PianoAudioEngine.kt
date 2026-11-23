package com.oido.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import com.oido.domain.Note
import kotlin.math.max

/**
 * [AudioEngine] backed by a streaming [AudioTrack].
 *
 * All note buffers are synthesized once (see [ToneSynthesizer]) and then pumped
 * into the audio output by a dedicated writer thread, which keeps audio work
 * completely away from the UI thread.
 *
 * Click free behaviour:
 *  - every buffer starts at zero with a raised cosine attack and ends at zero
 *    with a 60 ms release,
 *  - switching to another note applies a 10 ms fade out over the previous note,
 *  - [stop] fades out instead of cutting the signal.
 */
class PianoAudioEngine(
    private val sampleRate: Int = ToneSynthesizer.DEFAULT_SAMPLE_RATE,
    private val renderDurationMs: Int = 2600
) : AudioEngine {

    companion object {
        private const val TAG = "PianoAudioEngine"
        private const val CHUNK_SAMPLES = 1024
        private const val FADE_SAMPLES = 441 // ~10 ms at 44.1 kHz
    }

    private val buffers = HashMap<Note, ShortArray>()
    private val bufferLock = Any()

    @Volatile private var pendingNote: Note? = null
    @Volatile private var stopRequested = false
    @Volatile private var running = false
    @Volatile private var playing = false
    @Volatile private var audioAvailable = true

    private var writerThread: Thread? = null
    private var track: AudioTrack? = null

    // Accessed only from the writer thread.
    private var currentBuffer: ShortArray? = null
    private var currentPos = 0
    private var pendingSwitch: ShortArray? = null
    private var fadingOut = false
    private var fadePos = 0

    override fun prepare() {
        synchronized(bufferLock) {
            if (buffers.size == Note.entries.size) {
                ensureThread()
                return
            }
        }
        // Render off the main thread; play() renders on demand as a fallback.
        Thread({
            synchronized(bufferLock) {
                for (note in Note.entries) {
                    if (!buffers.containsKey(note)) {
                        buffers[note] = ToneSynthesizer.render(
                            frequency = note.frequency,
                            durationMs = renderDurationMs,
                            sampleRate = sampleRate
                        )
                    }
                }
            }
        }, "oido-render").apply {
            priority = Thread.NORM_PRIORITY - 1
            start()
        }
        ensureThread()
    }

    override fun play(note: Note) {
        ensureThread()
        pendingNote = note
        stopRequested = false
        wake()
    }

    override fun stop() {
        stopRequested = true
        wake()
    }

    override fun isPlaying(): Boolean = playing

    override fun release() {
        running = false
        wake()
        val thread = writerThread
        writerThread = null
        if (thread != null && thread.isAlive) {
            try {
                thread.join(1000)
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        val t = track
        track = null
        try {
            t?.release()
        } catch (ignored: IllegalStateException) {
            Log.w(TAG, "AudioTrack already released")
        }
        playing = false
    }

    // ---------------------------------------------------------------- internal

    private fun ensureThread() {
        if (writerThread?.isAlive == true) return
        synchronized(this) {
            if (writerThread?.isAlive == true) return
            running = true
            writerThread = Thread(::writerLoop, "oido-audio").apply {
                start()
            }
        }
    }

    private fun wake() {
        // The writer thread polls with a short sleep; interrupting shortens the latency.
        writerThread?.interrupt()
    }

    private fun writerLoop() {
        try {
            if (!ensureTrack()) return
            while (running) {
                val request = pendingNote
                if (request != null) {
                    pendingNote = null
                    startNote(request)
                }
                if (stopRequested) {
                    stopRequested = false
                    if (currentBuffer != null) {
                        pendingSwitch = null
                        fadingOut = true
                        fadePos = 0
                    }
                }
                val wrote = writeChunk()
                if (!wrote) {
                    try {
                        Thread.sleep(8)
                    } catch (ignored: InterruptedException) {
                        // Re-evaluate pending work immediately.
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Audio writer failed", t)
            audioAvailable = false
        } finally {
            playing = false
            currentBuffer = null
            val t = track
            track = null
            try {
                t?.pause()
                t?.flush()
                t?.release()
            } catch (ignored: IllegalStateException) {
                Log.w(TAG, "AudioTrack released during shutdown")
            }
        }
    }

    private fun startNote(note: Note) {
        val buffer = bufferFor(note)
        val active = currentBuffer
        if (active != null && currentPos < active.size) {
            // Fade the current note out first, then swap.
            pendingSwitch = buffer
            fadingOut = true
            fadePos = 0
        } else {
            currentBuffer = buffer
            currentPos = 0
            fadingOut = false
            fadePos = 0
            playing = true
        }
    }

    private fun finishSwitch() {
        currentBuffer = pendingSwitch
        currentPos = 0
        pendingSwitch = null
        fadingOut = false
        fadePos = 0
        playing = currentBuffer != null
        if (currentBuffer != null) {
            try {
                track?.play()
            } catch (ignored: IllegalStateException) {
                audioAvailable = false
            }
        }
    }

    /** Writes one chunk. Returns true when samples were pushed to the track. */
    private fun writeChunk(): Boolean {
        val t = track ?: return false
        val buf = currentBuffer ?: return false
        if (!audioAvailable) return false

        val chunk = ShortArray(CHUNK_SAMPLES)
        var n = 0
        while (n < CHUNK_SAMPLES) {
            if (fadingOut) {
                if (currentPos >= buf.size) {
                    finishSwitch()
                    break
                }
                val gain = 1.0 - fadePos.toDouble() / FADE_SAMPLES
                chunk[n] = (buf[currentPos] * gain).toInt().toShort()
                currentPos++
                fadePos++
                n++
                if (fadePos >= FADE_SAMPLES) {
                    finishSwitch()
                    break
                }
            } else {
                if (currentPos >= buf.size) {
                    currentBuffer = null
                    playing = false
                    break
                }
                chunk[n] = buf[currentPos]
                currentPos++
                n++
            }
        }
        if (n == 0) return false

        val written = t.write(chunk, 0, n)
        if (written <= 0) {
            audioAvailable = false
            return false
        }
        try {
            if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
        } catch (ignored: IllegalStateException) {
            audioAvailable = false
        }
        return true
    }

    private fun ensureTrack(): Boolean {
        if (track != null) return true
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            audioAvailable = false
            Log.e(TAG, "Unsupported audio configuration")
            return false
        }
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuf, CHUNK_SAMPLES * 2) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }

        val created = try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Cannot create AudioTrack", e)
            audioAvailable = false
            return false
        }

        if (created.state != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack not initialized")
            created.release()
            audioAvailable = false
            return false
        }
        try {
            created.play()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Cannot start AudioTrack", e)
            created.release()
            audioAvailable = false
            return false
        }
        track = created
        return true
    }

    private fun bufferFor(note: Note): ShortArray {
        synchronized(bufferLock) {
            buffers[note]?.let { return it }
        }
        // First playback of this note before the background render finished.
        val rendered = ToneSynthesizer.render(
            frequency = note.frequency,
            durationMs = renderDurationMs,
            sampleRate = sampleRate
        )
        synchronized(bufferLock) {
            buffers[note] = rendered
        }
        return rendered
    }
}
