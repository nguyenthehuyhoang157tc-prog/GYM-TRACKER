package com.hoctin.gymtracker.domain

/**
 * User-global training intent. AUTO preserves the existing rep-range/catalog inference; every other
 * value represents persisted explicit intent and must bypass that inference.
 */
enum class WorkoutGoalSelection(val storageValue: String) {
    AUTO("auto"),
    HYPERTROPHY("hypertrophy"),
    STRENGTH("strength"),
    POWER("power"),
    ENDURANCE("endurance"),
    GENERAL_FITNESS("general_fitness"),
    FAT_LOSS_SUPPORT("fat_loss_support"),
    SKILL("skill");

    companion object {
        /** Missing, legacy, blank, and unknown values are deliberately backward-compatible AUTO. */
        fun fromStorageValue(value: String?): WorkoutGoalSelection {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageValue == normalized } ?: AUTO
        }
    }
}

/** Single mapping used by every production caller; no persistence or data access enters the engine. */
fun WorkoutGoalSelection.toWorkoutContext(
    siblingsEvaluated: Int = 0,
    siblingsDown: Int = 0,
): WorkoutContext {
    val profile = when (this) {
        WorkoutGoalSelection.AUTO -> WorkoutGoalProfile.HYPERTROPHY
        WorkoutGoalSelection.HYPERTROPHY -> WorkoutGoalProfile.HYPERTROPHY
        WorkoutGoalSelection.STRENGTH -> WorkoutGoalProfile.STRENGTH
        WorkoutGoalSelection.POWER -> WorkoutGoalProfile.POWER
        WorkoutGoalSelection.ENDURANCE -> WorkoutGoalProfile.ENDURANCE
        WorkoutGoalSelection.GENERAL_FITNESS -> WorkoutGoalProfile.GENERAL_FITNESS
        WorkoutGoalSelection.FAT_LOSS_SUPPORT -> WorkoutGoalProfile.FAT_LOSS_SUPPORT
        WorkoutGoalSelection.SKILL -> WorkoutGoalProfile.SKILL
    }
    return WorkoutContext(
        goal = profile,
        siblingsEvaluated = siblingsEvaluated,
        siblingsDown = siblingsDown,
        goalSource = if (this == WorkoutGoalSelection.AUTO) GoalSource.AUTO else GoalSource.EXPLICIT,
    )
}
