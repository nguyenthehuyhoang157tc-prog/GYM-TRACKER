package com.hoctin.gymtracker.data

/**
 * Typed launch/persistence boundary for workout logging.
 *
 * Routine and Saved One-Off share the same History/LoggedSet storage, but only Routine is allowed to
 * participate in the selected-day lifecycle. Keeping that difference in the type prevents callers
 * from fabricating a [WorkoutDayEntity] for a one-off session.
 */
sealed interface WorkoutLaunchSource {
    val displayNameSnapshot: String
    val sessionSource: WorkoutSessionSource

    data class Routine(
        val day: WorkoutDayEntity,
    ) : WorkoutLaunchSource {
        override val displayNameSnapshot: String = day.name
        override val sessionSource: WorkoutSessionSource = WorkoutSessionSource.ROUTINE
    }

    data class SavedOneOff(
        val workout: SavedWorkoutEntity,
    ) : WorkoutLaunchSource {
        override val displayNameSnapshot: String = workout.name
        override val sessionSource: WorkoutSessionSource = WorkoutSessionSource.SAVED_ONE_OFF
    }
}

internal val WorkoutLaunchSource.routineDayId: Long?
    get() = (this as? WorkoutLaunchSource.Routine)?.day?.id

internal val WorkoutLaunchSource.savedWorkoutTemplateId: Long?
    get() = (this as? WorkoutLaunchSource.SavedOneOff)?.workout?.id

/** Weekly Progress is schedule adherence, so only completed Routine sessions are eligible. */
fun WorkoutSessionEntity.countsTowardWeeklyProgress(): Boolean =
    WorkoutSessionSource.fromStored(sourceType) == WorkoutSessionSource.ROUTINE

/** Both provenance types are real completed workouts and therefore continuity evidence. */
fun WorkoutSessionEntity.countsTowardActiveStreak(): Boolean =
    WorkoutSessionSource.fromStored(sourceType) in setOf(
        WorkoutSessionSource.ROUTINE,
        WorkoutSessionSource.SAVED_ONE_OFF,
    )
