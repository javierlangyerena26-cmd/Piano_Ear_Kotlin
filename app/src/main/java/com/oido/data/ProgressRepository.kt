package com.oido.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oido.domain.LevelProgress
import com.oido.domain.LevelProgression
import com.oido.domain.Note
import com.oido.domain.NoteStats
import com.oido.domain.SessionSnapshot
import com.oido.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.oidoDataStore: DataStore<Preferences> by preferencesDataStore(name = "oido_progress")

/**
 * Single local source of truth for progress and statistics.
 *
 * Backed by Preferences DataStore with a JSON payload, which keeps persistence
 * simple, atomic and fully offline while still supporting structured objects.
 */
class ProgressRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("oido_state")
    private val sessionLengthKey = intPreferencesKey("session_length")

    /** Observable progress state; emits immediately with the stored value. */
    val state: Flow<OidoState> = context.oidoDataStore.data.map { prefs ->
        decode(prefs[stateKey])
    }

    val sessionLength: Flow<Int> = context.oidoDataStore.data.map { prefs ->
        when (prefs[sessionLengthKey]) {
            10, 30 -> prefs[sessionLengthKey]!!
            else -> DEFAULT_SESSION_LENGTH
        }
    }

    suspend fun currentState(): OidoState = state.first()

    suspend fun currentSessionLength(): Int = sessionLength.first()

    suspend fun setSessionLength(length: Int) {
        val allowed = length.takeIf { it in VALID_SESSION_LENGTHS } ?: DEFAULT_SESSION_LENGTH
        context.oidoDataStore.edit { it[sessionLengthKey] = allowed }
    }

    /**
     * Records one answered exercise: global totals, per note stats, level
     * progression and practice history. Streaks are updated by [recordSession]
     * from the session summary, plus the daily streak here.
     */
    suspend fun recordExercise(
        note: Note,
        correct: Boolean,
        responseMs: Long,
        level: Int,
        todayEpochDay: Long
    ): OidoState = mutate { current ->
        val noteKey = note.name
        val previous = current.noteStats[noteKey] ?: NoteStats()
        val updatedNote = previous.copy(
            played = previous.played + 1,
            correct = previous.correct + if (correct) 1 else 0,
            incorrect = previous.incorrect + if (correct) 0 else 1,
            totalResponseMs = previous.totalResponseMs + responseMs.coerceAtLeast(0L),
            lastPracticedDay = todayEpochDay
        )

        val previousLevel = current.levelProgress[level] ?: LevelProgress(level)
        val updatedLevel = LevelProgression.recordExercise(previousLevel, correct)

        val unlocked = LevelProgression.highestUnlockedLevel(
            current.levelProgress + (level to updatedLevel)
        )

        current.copy(
            totalExercises = current.totalExercises + 1,
            totalCorrect = current.totalCorrect + if (correct) 1 else 0,
            totalIncorrect = current.totalIncorrect + if (correct) 0 else 1,
            totalResponseMs = current.totalResponseMs + responseMs.coerceAtLeast(0L),
            responseCount = current.responseCount + 1,
            lastPracticeDay = todayEpochDay,
            practiceDays = appendDay(current.practiceDays, todayEpochDay),
            dailyStreak = StreakCalculator.updateDailyStreak(
                previousStreak = current.dailyStreak,
                lastPracticeDay = current.lastPracticeDay,
                todayEpochDay = todayEpochDay
            ),
            unlockedLevel = maxOf(current.unlockedLevel, unlocked),
            levelProgress = current.levelProgress + (level to updatedLevel),
            noteStats = current.noteStats + (noteKey to updatedNote)
        )
    }

    /** Merges the finished session: sessions counter and best answer streak. */
    suspend fun recordSession(snapshot: SessionSnapshot): OidoState = mutate { current ->
        current.copy(
            totalSessions = current.totalSessions + 1,
            bestStreak = maxOf(current.bestStreak, snapshot.bestStreak)
        )
    }

    /** Applies an arbitrary transformation atomically and returns the result. */
    private suspend fun mutate(transform: (OidoState) -> OidoState): OidoState {
        var updated: OidoState = OidoState()
        context.oidoDataStore.edit { prefs ->
            updated = transform(decode(prefs[stateKey]))
            prefs[stateKey] = json.encodeToString(OidoState.serializer(), updated)
        }
        return updated
    }

    private fun decode(raw: String?): OidoState {
        if (raw.isNullOrEmpty()) return OidoState()
        return runCatching {
            json.decodeFromString(OidoState.serializer(), raw)
        }.getOrDefault(OidoState())
    }

    private fun appendDay(days: List<Long>, day: Long): List<Long> =
        if (days.contains(day)) days else (days + day).takeLast(400)

    companion object {
        const val DEFAULT_SESSION_LENGTH = 20
        val VALID_SESSION_LENGTHS = setOf(10, 20, 30)

        /** Clears everything. Only used by tests and the debug menu. */
        suspend fun clear(context: Context) {
            context.oidoDataStore.edit { it.clear() }
        }
    }
}
