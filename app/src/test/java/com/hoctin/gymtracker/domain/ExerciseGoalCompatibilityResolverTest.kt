package com.hoctin.gymtracker.domain

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGoalCompatibilityResolverTest {
    private fun context(goal: WorkoutGoalProfile, source: GoalSource = GoalSource.EXPLICIT) =
        WorkoutContext(goal = goal, goalSource = source)

    private fun resolve(
        goal: WorkoutGoalProfile,
        progression: ProgressionType,
        tracking: ProgressionTrackingType,
        min: Int = 8,
        max: Int = 12,
        source: GoalSource = GoalSource.EXPLICIT,
    ) = ExerciseGoalCompatibilityResolver.resolve(
        programContext = context(goal, source),
        progressionType = progression,
        trackingType = tracking,
        targetMinReps = min,
        targetMaxReps = max,
    )

    private fun exercise(name: String, min: Int = 8, max: Int = 12) = ExerciseEntity(
        id = 1,
        dayId = 1,
        name = name,
        targetSets = 3,
        targetMinReps = min,
        targetMaxReps = max,
        sortOrder = 0,
    )

    private fun profile(
        progression: ProgressionType,
        tracking: ProgressionTrackingType,
        isBodyweight: Boolean = false,
    ) = ProgressionProfile(
        progressionType = progression,
        incrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 1,
        isBodyweight = isBodyweight,
        isUnilateral = false,
        trackingType = tracking,
    )

    private fun repSession(
        sessionId: Long,
        reps: List<Int>,
        weight: Double,
        rir: Int = 2,
    ) = reps.mapIndexed { index, rep ->
        LoggedSetEntity(
            sessionId = sessionId,
            exerciseId = 1,
            exerciseNameSnapshot = "Exercise",
            setNumber = index + 1,
            weight = weight,
            reps = rep,
            rir = rir,
            isPrWeight = false,
            isPrReps = false,
        )
    }

    private fun metricSession(
        sessionId: Long,
        durationSeconds: Int,
        distanceMeters: Double? = null,
        weight: Double = 0.0,
    ) = listOf(
        LoggedSetEntity(
            sessionId = sessionId,
            exerciseId = 1,
            exerciseNameSnapshot = "Metric exercise",
            setNumber = 1,
            weight = weight,
            reps = 0,
            rir = 0,
            isPrWeight = false,
            isPrReps = false,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
        ),
    )

    @Test
    fun autoInference_isUnchangedAcrossRepMetricAndSkillModalities() {
        assertEquals(WorkoutGoalProfile.STRENGTH, resolve(
            WorkoutGoalProfile.HYPERTROPHY, ProgressionType.COMPOUND_LOAD,
            ProgressionTrackingType.REPS_WEIGHT, min = 3, max = 5, source = GoalSource.AUTO,
        ))
        assertEquals(WorkoutGoalProfile.ENDURANCE, resolve(
            WorkoutGoalProfile.HYPERTROPHY, ProgressionType.COMPOUND_LOAD,
            ProgressionTrackingType.REPS_WEIGHT, min = 20, max = 25, source = GoalSource.AUTO,
        ))
        assertEquals(WorkoutGoalProfile.HYPERTROPHY, resolve(
            WorkoutGoalProfile.HYPERTROPHY, ProgressionType.COMPOUND_LOAD,
            ProgressionTrackingType.REPS_WEIGHT, source = GoalSource.AUTO,
        ))
        assertEquals(WorkoutGoalProfile.HYPERTROPHY, resolve(
            WorkoutGoalProfile.HYPERTROPHY, ProgressionType.BODYWEIGHT,
            ProgressionTrackingType.TIME_ONLY, min = 0, max = 0, source = GoalSource.AUTO,
        ))
        assertEquals(WorkoutGoalProfile.SKILL, resolve(
            WorkoutGoalProfile.HYPERTROPHY, ProgressionType.SKILL_FORM,
            ProgressionTrackingType.SKILL_FORM, source = GoalSource.AUTO,
        ))
    }

    @Test
    fun skillFormModality_alwaysUsesSkillPolicy() {
        WorkoutGoalProfile.entries.forEach { programGoal ->
            assertEquals(programGoal.name, WorkoutGoalProfile.SKILL, resolve(
                programGoal, ProgressionType.SKILL_FORM, ProgressionTrackingType.REPS_WEIGHT,
            ))
            assertEquals(programGoal.name, WorkoutGoalProfile.SKILL, resolve(
                programGoal, ProgressionType.COMPOUND_LOAD, ProgressionTrackingType.SKILL_FORM,
            ))
        }
    }

    @Test
    fun timedAccessory_usesSustainablePolicyInsidePowerProgram() {
        assertEquals(WorkoutGoalProfile.GENERAL_FITNESS, resolve(
            WorkoutGoalProfile.POWER, ProgressionType.BODYWEIGHT,
            ProgressionTrackingType.TIME_ONLY, min = 0, max = 0,
        ))
        assertEquals(WorkoutGoalProfile.GENERAL_FITNESS, resolve(
            WorkoutGoalProfile.POWER, ProgressionType.ISOLATION_SLOW,
            ProgressionTrackingType.TIME_LOAD, min = 0, max = 0,
        ))
    }

    @Test
    fun alreadyCompatibleGoalsAndCompoundModalities_remainUnchanged() {
        for (goal in WorkoutGoalProfile.entries) {
            assertEquals(goal.name, goal, resolve(
                goal, ProgressionType.COMPOUND_LOAD, ProgressionTrackingType.REPS_WEIGHT,
            ))
            assertEquals(goal.name, goal, resolve(
                goal, ProgressionType.COMPOUND_LOAD, ProgressionTrackingType.TIME_DISTANCE_LOAD,
                min = 0, max = 0,
            ))
        }
        for (goal in listOf(
            WorkoutGoalProfile.HYPERTROPHY,
            WorkoutGoalProfile.STRENGTH,
            WorkoutGoalProfile.ENDURANCE,
            WorkoutGoalProfile.GENERAL_FITNESS,
            WorkoutGoalProfile.FAT_LOSS_SUPPORT,
        )) {
            assertEquals(goal.name, goal, resolve(
                goal, ProgressionType.BODYWEIGHT, ProgressionTrackingType.TIME_ONLY, min = 0, max = 0,
            ))
        }
    }

    @Test
    fun hypertrophyPlank_preservesNeutralTimeProgressionWithoutStimulusOverlay() {
        val sets = metricSession(2, 70) + metricSession(1, 60)
        val evidence = ProgressiveOverloadEngine.metricProgression(ProgressionTrackingType.TIME_ONLY, sets)
        val integrated = ProgressiveOverloadEngine.suggestion(
            exercise("Plank", min = 0, max = 0),
            sets,
            profile(ProgressionType.BODYWEIGHT, ProgressionTrackingType.TIME_ONLY, isBodyweight = true),
            workoutContext = context(WorkoutGoalProfile.HYPERTROPHY),
        )

        assertEquals(MetricProgressionStatus.IMPROVED, evidence.status)
        assertEquals(MetricEvidenceDimension.DURATION, evidence.evidenceDimension)
        assertEquals(evidence.suggestion, integrated)
        assertEquals("Build time", integrated.title)
        assertFalse(integrated.detail.contains("hypertrophy", ignoreCase = true))
        assertNull(integrated.suggestedWeight)
    }

    @Test
    fun powerProgramPlank_usesAccessoryWorkCapacityPolicyNotPowerPolicy() {
        val integrated = ProgressiveOverloadEngine.suggestion(
            exercise("Plank", min = 0, max = 0),
            metricSession(2, 70) + metricSession(1, 60),
            profile(ProgressionType.BODYWEIGHT, ProgressionTrackingType.TIME_ONLY, isBodyweight = true),
            workoutContext = context(WorkoutGoalProfile.POWER),
        )

        assertEquals("Sustainable progress", integrated.title)
        assertTrue(integrated.detail.contains("work metric"))
        assertFalse(integrated.detail.contains("power", ignoreCase = true))
        assertFalse(integrated.detail.contains("hypertrophy", ignoreCase = true))
        assertNull(integrated.suggestedWeight)
    }

    @Test
    fun powerProgramCompoundSled_keepsExistingAdvisoryPowerPolicy() {
        val integrated = ProgressiveOverloadEngine.suggestion(
            exercise("Sled Push", min = 0, max = 0),
            metricSession(2, 25, distanceMeters = 50.0, weight = 80.0) +
                metricSession(1, 30, distanceMeters = 50.0, weight = 80.0),
            profile(ProgressionType.COMPOUND_LOAD, ProgressionTrackingType.TIME_DISTANCE_LOAD),
            workoutContext = context(WorkoutGoalProfile.POWER),
        )

        assertEquals("Power remains advisory", integrated.title)
        assertTrue(integrated.detail.contains("were not measured"))
        assertNull(integrated.suggestedWeight)
    }

    @Test
    fun explicitProgramCannotOverrideSkillFormSafety() {
        val result = ProgressiveOverloadEngine.progression(
            exercise("Clean and Jerk", min = 5, max = 8),
            repSession(1, listOf(8, 8, 8), weight = 60.0),
            profile(ProgressionType.SKILL_FORM, ProgressionTrackingType.SKILL_FORM),
            workoutContext = context(WorkoutGoalProfile.POWER),
        )

        assertEquals(Action.KEEP_WEIGHT, result.action)
        assertEquals(60.0, result.suggestedWeight ?: -1.0, 0.001)
        assertEquals("Form first", result.titleOverride)
        assertTrue(result.reason.contains("do not measure technique quality"))
        assertFalse(result.reason.contains("power", ignoreCase = true))
    }

    @Test
    fun repsLoadCompound_keepsExistingGoalActionAndWeightSafety() {
        val ex = exercise("Power Clean")
        val sets = repSession(1, listOf(12, 12, 12), weight = 60.0, rir = 3)
        val result = ProgressiveOverloadEngine.progression(
            ex,
            sets,
            profile(ProgressionType.COMPOUND_LOAD, ProgressionTrackingType.REPS_WEIGHT),
            workoutContext = context(WorkoutGoalProfile.POWER),
        )

        assertEquals(Action.KEEP_WEIGHT, result.action)
        assertEquals(60.0, result.suggestedWeight ?: -1.0, 0.001)
        assertEquals("Power: hold load", result.titleOverride)
    }
}
