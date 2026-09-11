package com.hoctin.gymtracker.data.research

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.WorkoutAttemptIdentity
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutSessionEntity
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.Category
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.Force
import com.hoctin.gymtracker.data.catalog.Level
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryResearchContractTest {
    @Test
    fun versionRegistry_isPersistedOnFrozenPrediction() {
        val frozen = buildFrozen(emptyList(), emptyList())
        assertEquals(RecoveryResearchVersions.RECOVERY_MODEL, frozen.prediction.recoveryModelVersion)
        assertEquals(RecoveryResearchVersions.MUSCLE_HEAT_MODEL, frozen.prediction.muscleHeatModelVersion)
        assertEquals(RecoveryResearchVersions.PROGRESSIVE_OVERLOAD_MODEL, frozen.prediction.progressiveOverloadModelVersion)
        assertEquals(RecoveryResearchVersions.CONTRACT, frozen.prediction.researchContractVersion)
        assertEquals(RecoveryResearchVersions.RESEARCH_SCHEMA, frozen.prediction.researchSchemaVersion)
        assertTrue(frozen.prediction.payloadSha256.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun cutoff_rejectsAnyFutureInputSession() {
        val future = SessionWithSets(
            session = WorkoutSessionEntity(
                id = 2,
                dayId = 1,
                dayNameSnapshot = "Push",
                dateEpochDay = 0,
                startedAtMillis = CUTOFF + 1,
            ),
            sets = emptyList(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            buildFrozen(listOf(future), emptyList())
        }
    }

    @Test
    fun frozenPayloadHash_isDeterministic_andChangesWithPayload() {
        val first = buildFrozen(emptyList(), emptyList())
        val second = buildFrozen(emptyList(), emptyList())
        assertEquals(first.prediction.payloadSha256, second.prediction.payloadSha256)
        val changed = RecoveryResearchRecordBuilder.hashFrozenPayload(
            prediction = first.prediction.copy(overallReadiness = 17),
            groups = first.groups,
            muscles = first.muscles,
            targets = first.targetExercises,
        )
        assertNotEquals(first.prediction.payloadSha256, changed)
    }

    @Test
    fun targetSnapshot_preservesNullUnavailableAndExplicitAbstention() {
        val frozen = buildFrozen(emptyList(), listOf(benchCatalog()))
        val target = frozen.targetExercises.single()
        assertNull(target.suggestedRir)
        assertTrue(target.abstained)
        assertEquals(ResearchReason.NO_BASELINE_HISTORY.name, target.abstentionReasonCode)
        assertEquals(ResearchCompleteness.NONE.name, target.dataCompletenessCode)
        assertEquals("[\"CHEST\"]", target.primaryMuscleCodesJson)
        assertEquals(100, target.primaryReadinessMin)
    }

    @Test
    fun loadedRule_skipsInvalidAndWarmupRows_thenUsesModalLoad() {
        val sets = listOf(
            set(id = 1, number = 1, weight = 20.0, reps = 0), // incomplete/skipped
            set(id = 2, number = 2, weight = 30.0, reps = 8), // warm-up
            set(id = 3, number = 3, weight = 60.0, reps = 8),
            set(id = 4, number = 4, weight = 60.0, reps = 7),
        )
        val selected = selectFirstWorkingSet(sets, TrackingType.REPS_WEIGHT)!!
        assertEquals(3L, selected.set.id)
        assertEquals(RecoveryResearchVersions.LOADED_SET_RULE, selected.ruleVersion)
        assertEquals(ResearchConfidence.MODERATE, selected.confidence)
    }

    @Test
    fun loadedRule_tieChoosesHeaviestRatherThanFirstVisibleRow() {
        val selected = selectFirstWorkingSet(
            listOf(set(1, 1, 20.0, 10), set(2, 2, 60.0, 8)),
            TrackingType.REPS_WEIGHT,
        )!!
        assertEquals(2L, selected.set.id)
    }

    @Test
    fun bodyweightAndMetricRules_areTrackingAware() {
        val bodyweight = selectFirstWorkingSet(
            listOf(set(1, 1, 0.0, 0), set(2, 2, 0.0, 9)),
            TrackingType.BODYWEIGHT_REPS,
        )!!
        assertEquals(2L, bodyweight.set.id)

        val timed = selectFirstWorkingSet(
            listOf(set(3, 1, 0.0, 0, durationSeconds = null), set(4, 2, 0.0, 0, durationSeconds = 45)),
            TrackingType.TIME_ONLY,
        )!!
        assertEquals(4L, timed.set.id)
        assertEquals(ResearchConfidence.LOW, timed.confidence)
    }

    @Test
    fun suggestedActualDeltas_areExplicitAndNullable() {
        val delta = suggestedActualDelta(57.5, 7, 60.0, 8)
        assertEquals(-2.5, delta.loadDeltaKg!!, 0.0)
        assertEquals(-1, delta.repsDelta)
        assertEquals(true, delta.loadReduced)
        assertEquals(true, delta.repsUnexpectedlyUnderperformed)

        val unavailable = suggestedActualDelta(57.5, 7, null, null)
        assertNull(unavailable.loadDeltaKg)
        assertNull(unavailable.repsDelta)
        assertNull(unavailable.loadReduced)
        assertNull(unavailable.repsUnexpectedlyUnderperformed)
    }

    @Test
    fun unsupportedFutureTrackingCode_abstainsInsteadOfFallingBack() {
        assertNull(trackingTypeForEvaluation("FUTURE_SENSOR_ONLY"))
        assertEquals(TrackingType.TIME_DISTANCE, trackingTypeForEvaluation("TIME_DISTANCE"))
    }

    private fun buildFrozen(
        sessions: List<SessionWithSets>,
        catalog: List<CatalogExercise>,
    ): FrozenRecoveryResearchRecord {
        var id = 0
        val target = ExerciseEntity(11, 1, "Bench Press", 3, 6, 8, 0)
        return RecoveryResearchRecordBuilder.build(
            request = RecoveryResearchFreezeRequest(
                attempt = WorkoutAttemptIdentity("attempt-1", CUTOFF - 10, CUTOFF - 10),
                day = WorkoutDayEntity(1, "Push", 0),
                targetExercises = if (catalog.isEmpty()) emptyList() else listOf(target),
                allPlanExercises = if (catalog.isEmpty()) emptyList() else listOf(target),
                goalSelection = WorkoutGoalSelection.HYPERTROPHY,
                inputDataCutoffAtMillis = CUTOFF,
            ),
            sessionsAtOrBeforeCutoff = sessions,
            catalog = catalog,
            predictionGeneratedAtMillis = CUTOFF + 10,
            idFactory = ResearchIdFactory { "id-${++id}" },
        )
    }

    private fun benchCatalog() = CatalogExercise(
        slug = "bench-press",
        name = "Bench Press",
        equipment = Equipment.BARBELL,
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = listOf(MuscleGroup.TRICEPS),
        level = Level.INTERMEDIATE,
        force = Force.PUSH,
        category = Category.STRENGTH,
        progressionType = ProgressionType.COMPOUND_LOAD,
        trackingType = TrackingType.REPS_WEIGHT,
        defaultSets = 3,
        defaultMinReps = 6,
        defaultMaxReps = 8,
        defaultIncrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 1,
        isBodyweight = false,
        isUnilateral = false,
        aliases = emptyList(),
    )

    private fun set(
        id: Long,
        number: Int,
        weight: Double,
        reps: Int,
        durationSeconds: Int? = null,
    ) = LoggedSetEntity(
        id = id,
        sessionId = 1,
        exerciseId = 11,
        exerciseNameSnapshot = "Bench Press",
        setNumber = number,
        weight = weight,
        reps = reps,
        rir = 2,
        isPrWeight = false,
        isPrReps = false,
        durationSeconds = durationSeconds,
        canonicalExerciseKey = "catalog:bench-press",
    )

    private companion object {
        const val CUTOFF = 1_800_000_000_000L
    }
}
