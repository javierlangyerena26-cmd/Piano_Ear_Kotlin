package com.oido.domain

/**
 * Pure, platform independent definition of a training level.
 *
 * Level progression depends on real performance (accuracy over a minimum number
 * of exercises), never on the mere number of exercises played.
 */
data class LevelConfig(
    val level: Int,
    /** Notes that take part in the exercises of this level. */
    val activeNotes: List<Note>,
    /** Minimum number of exercises required before the level can be passed. */
    val minExercisesToAdvance: Int,
    /** Accuracy (0..1) required over [minExercisesToAdvance] to advance. */
    val accuracyThreshold: Double,
    /** Maximum number of times the target note can be replayed in one exercise. */
    val maxReplays: Int,
    /** Human readable Spanish description shown in the UI. */
    val description: String
) {
    init {
        require(activeNotes.size >= 2) { "A level needs at least two notes" }
        require(activeNotes.all { it in Note.entries }) { "Unknown note in level $level" }
        require(minExercisesToAdvance > 0)
        require(accuracyThreshold in 0.0..1.0)
        require(maxReplays >= 0)
    }
}

/**
 * Progressive difficulty configuration of the MVP.
 *
 * Level 1 -> 2 notes, Level 2 -> 4 notes, Level 3 -> all 7 notes,
 * Level 4 -> all 7 notes with reduced replay assistance.
 *
 * All thresholds are configurable here, without touching the game logic.
 */
object Levels {

    /** Beginner note pair used by level 1. Configurable. */
    val beginnerNotes: List<Note> = listOf(Note.DO, Note.SOL)

    /** Second step of the beginner ladder. Configurable. */
    val intermediateNotes: List<Note> = listOf(Note.DO, Note.RE, Note.MI, Note.SOL)

    val all: List<LevelConfig> = listOf(
        LevelConfig(
            level = 1,
            activeNotes = beginnerNotes,
            minExercisesToAdvance = 12,
            accuracyThreshold = 0.75,
            maxReplays = Int.MAX_VALUE,
            description = "Dos notas: DO y SOL"
        ),
        LevelConfig(
            level = 2,
            activeNotes = intermediateNotes,
            minExercisesToAdvance = 16,
            accuracyThreshold = 0.75,
            maxReplays = Int.MAX_VALUE,
            description = "Cuatro notas: DO, RE, MI y SOL"
        ),
        LevelConfig(
            level = 3,
            activeNotes = Note.entries.toList(),
            minExercisesToAdvance = 20,
            accuracyThreshold = 0.80,
            maxReplays = Int.MAX_VALUE,
            description = "Las siete notas naturales"
        ),
        LevelConfig(
            level = 4,
            activeNotes = Note.entries.toList(),
            minExercisesToAdvance = 20,
            accuracyThreshold = 0.80,
            maxReplays = 1,
            description = "Siete notas con menos ayuda"
        )
    )

    val maxLevel: Int = all.size

    fun forLevel(level: Int): LevelConfig = all.firstOrNull { it.level == level }
        ?: all.last()

    /** Free practice: every note unlocked so far can be exercised. */
    fun unlockedNotes(unlockedLevel: Int): List<Note> =
        forLevel(unlockedLevel).activeNotes
}
