package com.hoctin.gymtracker.ui.log

import com.hoctin.gymtracker.data.catalog.ProgressionType
import kotlin.math.roundToInt

/** Presentation-only set labels for LOG-V4A. They intentionally have no Engine/Room semantics. */
enum class LogSetType { STRAIGHT, WARMUP, DROP }

/** Presentation-only interpretation of the entered load. Stored only for the active Log ViewModel. */
enum class LogWeightDistribution { TOTAL, EACH_SIDE, LEFT_SIDE, RIGHT_SIDE }

enum class RestTimerUnit { SEC, MIN }

data class RestTimerUiState(
    val visible: Boolean = false,
    val totalSeconds: Int = 90,
    val remainingSeconds: Int = 90,
    val running: Boolean = false,
    val smartEnabled: Boolean = true,
    val inputValue: String = "90",
    val unit: RestTimerUnit = RestTimerUnit.SEC,
    val advanceAfterRest: Boolean = false,
)

data class RestSuggestionInput(
    val progressionType: ProgressionType,
    val rir: Int?,
    val reps: Int?,
    val previousComparableReps: Int?,
    val workingSetOrdinal: Int,
    val programmedSetCount: Int,
)

/**
 * LOG-V4A rest guidance. This is an AURON UI-domain helper, not Progressive Overload logic.
 * Values are rounded to five seconds so the suggestion remains easy to scan and edit.
 */
object RestSuggestionHeuristic {
    fun seconds(input: RestSuggestionInput): Int {
        val range = when (input.progressionType) {
            ProgressionType.COMPOUND_LOAD -> 150..240
            ProgressionType.ISOLATION_SLOW -> 75..120
            ProgressionType.BODYWEIGHT -> 90..150
            ProgressionType.SKILL_FORM -> 120..180
        }
        val effortTier = when (input.rir) {
            null -> 1
            in 3..Int.MAX_VALUE -> 0
            2 -> 1
            1 -> 2
            else -> 3
        }
        val clearRepDrop = input.reps != null && input.previousComparableReps != null &&
            input.previousComparableReps > 0 && input.reps <= input.previousComparableReps * 0.8
        val tier = (effortTier + if (clearRepDrop) 1 else 0).coerceAtMost(3)
        val fractions = floatArrayOf(0f, 0.34f, 0.72f, 1f)
        val withinRange = range.first + (range.last - range.first) * fractions[tier]
        val extraSetSurcharge = ((input.workingSetOrdinal - input.programmedSetCount).coerceAtLeast(0) * 15)
            .coerceAtMost(45)
        return (((withinRange.roundToInt() + extraSetSurcharge) / 5f).roundToInt() * 5)
            .coerceAtLeast(5)
    }
}

internal fun formatRestDuration(totalSeconds: Int): String =
    "%d:%02d".format(
        totalSeconds.coerceAtLeast(0) / 60,
        totalSeconds.coerceAtLeast(0) % 60,
    )
