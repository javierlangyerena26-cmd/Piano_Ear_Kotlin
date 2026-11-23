package com.oido.domain

import kotlin.random.Random

/** One exercise about to be presented to the user. */
data class Exercise(
    val index: Int,
    val total: Int,
    val target: Note
)

/** State of the answer phase of an exercise. */
enum class AnswerState { WAITING, CORRECT, WRONG }

/**
 * Orchestrates a whole training session: picks targets with the adaptive engine,
 * validates answers, counts replays and accumulates statistics.
 *
 * Pure Kotlin: no Android types, no audio, no clock. Time is injected as
 * milliseconds so the logic is fully unit testable.
 */
class ExerciseEngine(
    private val levelConfig: LevelConfig,
    private val totalExercises: Int,
    private val adaptive: AdaptiveLearningEngine = AdaptiveLearningEngine(),
    private val random: Random = Random.Default,
    initialStats: Map<Note, NoteStats> = emptyMap()
) {
    private val stats: MutableMap<Note, NoteStats> = initialStats.toMutableMap()
    private val accumulator = SessionAccumulator()
    private var currentIndex = 0
    private var previousTarget: Note? = null
    private var target: Note? = null
    private var answerState: AnswerState = AnswerState.WAITING
    private var replaysUsed = 0
    private var exerciseStartedAtMs: Long = 0L

    val level: Int get() = levelConfig.level
    val total: Int get() = totalExercises
    val index: Int get() = currentIndex + 1
    val isFinished: Boolean get() = currentIndex >= totalExercises
    val state: AnswerState get() = answerState
    val currentTarget: Note? get() = target
    val replayCount: Int get() = replaysUsed
    val replaysRemaining: Int
        get() = when {
            levelConfig.maxReplays == Int.MAX_VALUE -> Int.MAX_VALUE
            else -> (levelConfig.maxReplays - replaysUsed).coerceAtLeast(0)
        }

    val sessionSnapshot: SessionSnapshot get() = accumulator.snapshot()

    /** Starts the session and creates the first exercise. */
    fun start(nowMs: Long): Exercise {
        require(!isFinished) { "Session already finished" }
        return newExercise(nowMs)
    }

    /**
     * Creates the next exercise. The target never equals the previous one while
     * the level has more than one note, so the answer stays unpredictable.
     */
    fun newExercise(nowMs: Long): Exercise {
        require(!isFinished) { "No more exercises in this session" }
        val target = adaptive.selectNote(
            candidates = levelConfig.activeNotes,
            stats = stats,
            previousNote = previousTarget,
            todayEpochDay = 0L
        )
        this.target = target
        this.previousTarget = target
        this.answerState = AnswerState.WAITING
        this.replaysUsed = 0
        this.exerciseStartedAtMs = nowMs
        return Exercise(index = currentIndex + 1, total = totalExercises, target = target)
    }

    /** Registers that the target note was played (first play or replay). */
    fun onNotePlayed(isReplay: Boolean) {
        if (isReplay) replaysUsed++
    }

    fun canReplay(): Boolean = replaysRemaining > 0

    /**
     * Validates the user's answer and records the outcome.
     * Returns the [AnswerState] reached; answers after feedback are ignored.
     */
    fun answer(selected: Note, nowMs: Long): AnswerState {
        if (answerState != AnswerState.WAITING) return answerState
        val target = requireNotNull(target) { "No active exercise" }
        val responseMs = (nowMs - exerciseStartedAtMs).coerceAtLeast(0L)
        val correct = selected == target

        answerState = if (correct) AnswerState.CORRECT else AnswerState.WRONG
        accumulator.add(
            ExerciseResult(note = target, selected = selected, responseMs = responseMs)
        )
        stats[target] = (stats[target] ?: NoteStats()).let { previous ->
            previous.copy(
                played = previous.played + 1,
                correct = previous.correct + if (correct) 1 else 0,
                incorrect = previous.incorrect + if (correct) 0 else 1,
                totalResponseMs = previous.totalResponseMs + responseMs
            )
        }
        return answerState
    }

    /** Advances to the next exercise, if any. Returns null when finished. */
    fun advance(nowMs: Long): Exercise? {
        if (answerState == AnswerState.WAITING) return null
        currentIndex++
        return if (isFinished) null else newExercise(nowMs)
    }

    fun statsSnapshot(): Map<Note, NoteStats> = stats.toMap()

    companion object {
        fun allowedSelections(config: LevelConfig): List<Note> = config.activeNotes
    }
}
