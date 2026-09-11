package com.hoctin.gymtracker.domain

/**
 * Chooses which existing goal policy is compatible with one exercise's declared modality.
 *
 * This resolver does not inspect logged sets, compare metrics, or create coaching advice. It only
 * combines the persisted program intent with catalog-derived progression/tracking metadata before the
 * existing reps/load or metric policy runs.
 */
object ExerciseGoalCompatibilityResolver {
    private const val AUTO_STRENGTH_MAX_REPS = 6
    private const val AUTO_ENDURANCE_MIN_REPS = 20

    fun resolve(
        programContext: WorkoutContext,
        progressionType: ProgressionType,
        trackingType: ProgressionTrackingType,
        targetMinReps: Int,
        targetMaxReps: Int,
    ): WorkoutGoalProfile {
        // Technique quality is not logged, so skill/form modality always remains form-first.
        if (progressionType == ProgressionType.SKILL_FORM ||
            trackingType == ProgressionTrackingType.SKILL_FORM
        ) {
            return WorkoutGoalProfile.SKILL
        }

        // Preserve the complete pre-V3.11C.1 AUTO contract. Metric rows use rep sentinels and never
        // enter rep-range inference; reps/load rows keep the existing strength/endurance thresholds.
        if (programContext.goalSource == GoalSource.AUTO &&
            programContext.goal == WorkoutGoalProfile.HYPERTROPHY
        ) {
            if (!trackingType.isRepBased) return WorkoutGoalProfile.HYPERTROPHY
            val minRep = targetMinReps.coerceAtLeast(1)
            val maxRep = targetMaxReps.coerceAtLeast(minRep)
            return when {
                maxRep <= AUTO_STRENGTH_MAX_REPS -> WorkoutGoalProfile.STRENGTH
                minRep >= AUTO_ENDURANCE_MIN_REPS -> WorkoutGoalProfile.ENDURANCE
                else -> WorkoutGoalProfile.HYPERTROPHY
            }
        }

        // A timed bodyweight/isolation accessory does not become a power exercise merely because the
        // whole program uses that goal. Reuse the existing sustainable work-capacity policy. Strength's
        // existing metric policy already identifies these results as work capacity, not strength gains.
        val accessoryMetric = !trackingType.isRepBased &&
            (progressionType == ProgressionType.BODYWEIGHT ||
                progressionType == ProgressionType.ISOLATION_SLOW)
        if (accessoryMetric && programContext.goal == WorkoutGoalProfile.POWER) {
            return WorkoutGoalProfile.GENERAL_FITNESS
        }

        // Compound reps/load, compound metric work, and already-compatible explicit goals stay intact.
        return programContext.goal
    }
}
