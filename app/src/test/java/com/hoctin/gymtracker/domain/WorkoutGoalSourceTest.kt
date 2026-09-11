package com.hoctin.gymtracker.domain

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.WorkoutGoalPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutGoalSourceTest {

    private fun exercise(name: String, min: Int, max: Int) = ExerciseEntity(
        id = 1,
        dayId = 1,
        name = name,
        targetSets = 3,
        targetMinReps = min,
        targetMaxReps = max,
        sortOrder = 0,
    )

    private fun repSession(
        sessionId: Long,
        reps: List<Int>,
        weight: Double,
        rirs: List<Int> = List(reps.size) { 2 },
    ): List<LoggedSetEntity> = reps.mapIndexed { index, value ->
        LoggedSetEntity(
            sessionId = sessionId,
            exerciseId = 1,
            exerciseNameSnapshot = "Exercise",
            setNumber = index + 1,
            weight = weight,
            reps = value,
            rir = rirs[index],
            isPrWeight = false,
            isPrReps = false,
        )
    }

    private fun metricSession(
        sessionId: Long,
        durationSeconds: Int? = null,
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

    private fun profile(
        progressionType: ProgressionType = ProgressionType.COMPOUND_LOAD,
        trackingType: ProgressionTrackingType = ProgressionTrackingType.REPS_WEIGHT,
    ) = ProgressionProfile(
        progressionType = progressionType,
        incrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 1,
        isBodyweight = false,
        isUnilateral = false,
        trackingType = trackingType,
    )

    @Test
    fun missingLegacyBlankAndInvalidStorage_fallBackToAuto() {
        for (raw in listOf<String?>(null, "", "  ", "legacy_default", "UNKNOWN", "powerlifting")) {
            assertEquals(raw, WorkoutGoalSelection.AUTO, WorkoutGoalSelection.fromStorageValue(raw))
        }
    }

    @Test
    fun validStoredGoals_roundTripAsExplicitIntent() {
        WorkoutGoalSelection.entries.filterNot { it == WorkoutGoalSelection.AUTO }.forEach { selection ->
            val decoded = WorkoutGoalSelection.fromStorageValue("  ${selection.storageValue.uppercase()}  ")
            val context = decoded.toWorkoutContext()
            assertEquals(selection, decoded)
            assertEquals(GoalSource.EXPLICIT, context.goalSource)
            assertEquals(WorkoutGoalProfile.valueOf(selection.name), context.goal)
        }
    }

    @Test
    fun preferenceStore_persistsAndReloadsGoal() {
        var persisted: String? = null
        val firstProcess = WorkoutGoalPreferenceStore(
            read = { persisted },
            write = { persisted = it },
        )
        assertEquals(WorkoutGoalSelection.AUTO, firstProcess.selection.value)

        firstProcess.set(WorkoutGoalSelection.POWER)
        assertEquals("power", persisted)
        assertEquals(WorkoutGoalSelection.POWER, firstProcess.selection.value)

        val reloadedProcess = WorkoutGoalPreferenceStore(
            read = { persisted },
            write = { persisted = it },
        )
        assertEquals(WorkoutGoalSelection.POWER, reloadedProcess.selection.value)
    }

    @Test
    fun autoContext_preservesRepRangeAndSkillInference() {
        val auto = WorkoutGoalSelection.AUTO.toWorkoutContext()
        assertEquals(GoalSource.AUTO, auto.goalSource)

        val lowRep = ProgressiveOverloadEngine.progression(
            exercise("Machine press", 3, 5),
            repSession(1, listOf(5, 5, 5), 60.0),
            profile(),
            workoutContext = auto,
        )
        val highRep = ProgressiveOverloadEngine.progression(
            exercise("Machine press", 20, 25),
            repSession(2, listOf(25, 25, 25), 30.0),
            profile(),
            workoutContext = auto,
        )
        val hypertrophy = ProgressiveOverloadEngine.progression(
            exercise("Machine press", 8, 12),
            repSession(3, listOf(12, 12, 12), 40.0),
            profile(),
            workoutContext = auto,
        )
        val skill = ProgressiveOverloadEngine.progression(
            exercise("Pallof press", 8, 12),
            repSession(4, listOf(12, 12, 12), 20.0),
            profile(ProgressionType.SKILL_FORM, ProgressionTrackingType.SKILL_FORM),
            workoutContext = auto,
        )

        assertTrue(lowRep.insight != CoachInsight.HYPERTROPHY_QUALITY)
        assertTrue(highRep.insight != CoachInsight.HYPERTROPHY_QUALITY)
        assertEquals(CoachInsight.HYPERTROPHY_QUALITY, hypertrophy.insight)
        assertEquals("Form first", skill.titleOverride)
        assertEquals(Action.KEEP_WEIGHT, skill.action)
    }

    @Test
    fun explicitHypertrophy_bypassesLowAndHighRepInference() {
        val explicit = WorkoutGoalSelection.HYPERTROPHY.toWorkoutContext()
        for ((min, max, weight) in listOf(Triple(3, 5, 60.0), Triple(20, 25, 30.0))) {
            val result = ProgressiveOverloadEngine.progression(
                exercise("Machine press", min, max),
                repSession(min.toLong(), listOf(max, max, max), weight),
                profile(),
                workoutContext = explicit,
            )
            assertEquals(GoalSource.EXPLICIT, explicit.goalSource)
            assertEquals(CoachInsight.HYPERTROPHY_QUALITY, result.insight)
        }
    }

    @Test
    fun explicitStrength_reachesStrengthSafetyPolicy() {
        val result = ProgressiveOverloadEngine.progression(
            exercise("Barbell squat", 3, 5),
            repSession(1, listOf(5, 5, 5), 100.0, listOf(0, 3, 3)),
            profile(),
            workoutContext = WorkoutGoalSelection.STRENGTH.toWorkoutContext(),
        )
        assertEquals(Action.KEEP_WEIGHT, result.action)
        assertEquals(100.0, result.suggestedWeight ?: -1.0, 0.001)
        assertEquals("Strength: repeat cleanly", result.titleOverride)
    }

    @Test
    fun explicitPower_reachesPowerSafetyPolicy() {
        val result = ProgressiveOverloadEngine.progression(
            exercise("Power Clean", 8, 12),
            repSession(1, listOf(12, 12, 12), 60.0, listOf(3, 3, 3)),
            profile(),
            workoutContext = WorkoutGoalSelection.POWER.toWorkoutContext(),
        )
        assertEquals(Action.KEEP_WEIGHT, result.action)
        assertEquals(60.0, result.suggestedWeight ?: -1.0, 0.001)
        assertEquals("Power: hold load", result.titleOverride)
    }

    @Test
    fun explicitEndurance_reachesMetricPolicy() {
        val result = ProgressiveOverloadEngine.suggestion(
            exercise("Plank", 0, 0),
            metricSession(2, durationSeconds = 70) + metricSession(1, durationSeconds = 60),
            profile(trackingType = ProgressionTrackingType.TIME_ONLY),
            workoutContext = WorkoutGoalSelection.ENDURANCE.toWorkoutContext(),
        )
        assertEquals("Endurance work improved", result.title)
        assertNull(result.suggestedWeight)
    }

    @Test
    fun explicitGeneralFitness_reachesSustainableMetricPolicy() {
        val result = ProgressiveOverloadEngine.suggestion(
            exercise("Carry", 0, 0),
            metricSession(2, distanceMeters = 60.0) + metricSession(1, distanceMeters = 50.0),
            profile(trackingType = ProgressionTrackingType.DISTANCE_ONLY),
            readiness = Readiness.NORMAL,
            workoutContext = WorkoutGoalSelection.GENERAL_FITNESS.toWorkoutContext(),
        )
        assertEquals("Sustainable progress", result.title)
        assertNull(result.suggestedWeight)
    }

    @Test
    fun explicitFatLossSupport_makesNoOutcomeClaim() {
        val result = ProgressiveOverloadEngine.suggestion(
            exercise("Plank", 0, 0),
            metricSession(2, durationSeconds = 70) + metricSession(1, durationSeconds = 60),
            profile(trackingType = ProgressionTrackingType.TIME_ONLY),
            workoutContext = WorkoutGoalSelection.FAT_LOSS_SUPPORT.toWorkoutContext(),
        )
        val detail = result.detail.lowercase()
        assertTrue(detail.contains("does not measure fat loss"))
        assertTrue(!detail.contains("will lose"))
        assertNull(result.suggestedWeight)
    }

    @Test
    fun explicitSkill_staysFormFirst() {
        val result = ProgressiveOverloadEngine.progression(
            exercise("Machine press", 8, 12),
            repSession(1, listOf(12, 12, 12), 40.0),
            profile(),
            workoutContext = WorkoutGoalSelection.SKILL.toWorkoutContext(),
        )
        assertEquals(Action.KEEP_WEIGHT, result.action)
        assertEquals("Form first", result.titleOverride)
        assertTrue(result.reason.contains("do not measure technique quality"))
    }

    @Test
    fun sharedResolver_givesLogAndDetailTheSameGoalAndSource() {
        WorkoutGoalSelection.entries.forEach { selection ->
            val logContext = selection.toWorkoutContext(siblingsEvaluated = 4, siblingsDown = 2)
            val detailContext = selection.toWorkoutContext()
            assertEquals(selection.name, logContext.goal, detailContext.goal)
            assertEquals(selection.name, logContext.goalSource, detailContext.goalSource)
            assertEquals(4, logContext.siblingsEvaluated)
            assertEquals(0, detailContext.siblingsEvaluated)
        }
    }

    @Test
    fun sameExplicitSource_drivesBothMetricAndRepsLoadPaths() {
        val context = WorkoutGoalSelection.POWER.toWorkoutContext()
        val repsResult = ProgressiveOverloadEngine.suggestion(
            exercise("Power Clean", 8, 12),
            repSession(1, listOf(12, 12, 12), 60.0, listOf(3, 3, 3)),
            profile(),
            workoutContext = context,
        )
        val metricResult = ProgressiveOverloadEngine.suggestion(
            exercise("Power task", 0, 0),
            metricSession(2, durationSeconds = 50) + metricSession(1, durationSeconds = 60),
            profile(trackingType = ProgressionTrackingType.TIME_ONLY),
            workoutContext = context,
        )
        assertEquals("Power: hold load", repsResult.title)
        assertEquals("Power remains advisory", metricResult.title)
        assertNull(metricResult.suggestedWeight)
    }

    @Test
    fun explicitHypertrophy_preservesActionAndSuggestedWeightSafety() {
        val ex = exercise("Machine chest press", 8, 12)
        val sets = repSession(1, listOf(12, 12, 12), 40.0, listOf(2, 2, 2))
        val auto = ProgressiveOverloadEngine.progression(
            ex, sets, profile(), workoutContext = WorkoutGoalSelection.AUTO.toWorkoutContext(),
        )
        val explicit = ProgressiveOverloadEngine.progression(
            ex, sets, profile(), workoutContext = WorkoutGoalSelection.HYPERTROPHY.toWorkoutContext(),
        )
        assertEquals(auto.action, explicit.action)
        assertEquals(auto.suggestedWeight, explicit.suggestedWeight)
    }
}
