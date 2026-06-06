package com.hoctin.gymtracker.domain

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import kotlin.math.roundToInt

data class OverloadSuggestion(
    val title: String,
    val detail: String,
    val suggestedWeight: Double? = null,
)

data class SessionTrend(
    val currentVolume: Double,
    val previousVolume: Double,
    val volumeDelta: Double,
    val label: String,
)

object ProgressiveOverloadEngine {
    fun suggestion(
        exercise: ExerciseEntity,
        latestSessionSets: List<LoggedSetEntity>,
    ): OverloadSuggestion {
        if (latestSessionSets.isEmpty()) {
            return OverloadSuggestion(
                title = "Log first session",
                detail = "No history yet. Start near ${exercise.targetMinReps}-${exercise.targetMaxReps} reps with 1-3 RIR.",
            )
        }

        val workingSets = latestSessionSets.take(exercise.targetSets)
        val allTopRange = workingSets.size >= exercise.targetSets &&
            workingSets.all { it.reps >= exercise.targetMaxReps }
        val averageRir = workingSets.map { it.rir }.average()
        val heaviest = workingSets.maxOfOrNull { it.weight } ?: 0.0

        return when {
            allTopRange && averageRir >= 2.0 -> {
                val nextWeight = roundToNearestIncrement(heaviest * 1.025)
                OverloadSuggestion(
                    title = "Increase weight next time",
                    detail = "You hit the top rep target with enough reps in reserve. Move up while keeping form clean.",
                    suggestedWeight = nextWeight,
                )
            }

            allTopRange -> OverloadSuggestion(
                title = "Hold weight",
                detail = "Reps are on target, but RIR is tight. Repeat this load until it feels cleaner.",
                suggestedWeight = heaviest,
            )

            workingSets.any { it.rir <= 0 } -> OverloadSuggestion(
                title = "Recover or reduce slightly",
                detail = "A set reached failure. Keep the same weight or reduce 2.5-5% next time.",
                suggestedWeight = roundToNearestIncrement(heaviest * 0.975),
            )

            else -> OverloadSuggestion(
                title = "Build reps first",
                detail = "Stay with this load and add reps until all working sets reach ${exercise.targetMaxReps}.",
                suggestedWeight = heaviest,
            )
        }
    }

    fun trend(
        currentSets: List<LoggedSetEntity>,
        previousSets: List<LoggedSetEntity>,
    ): SessionTrend {
        val currentVolume = currentSets.sumOf { it.weight * it.reps }
        val previousVolume = previousSets.sumOf { it.weight * it.reps }
        val delta = currentVolume - previousVolume
        val label = when {
            previousSets.isEmpty() -> "First logged session"
            delta > 0.0 -> "+${formatWeight(delta)} volume"
            delta < 0.0 -> "${formatWeight(delta)} volume"
            else -> "Volume matched"
        }
        return SessionTrend(currentVolume, previousVolume, delta, label)
    }

    fun formatWeight(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    private fun roundToNearestIncrement(value: Double, increment: Double = 2.5): Double {
        return (value / increment).roundToInt() * increment
    }
}
