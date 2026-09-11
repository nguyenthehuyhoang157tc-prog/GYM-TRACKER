package com.hoctin.gymtracker.ui.onboarding

import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTrainingProfileTest {
    private val completeRequiredProfile = OnboardingTrainingProfileDraft(
        ageGroup = AgeGroup.ADULT,
        primaryGoal = PrimaryTrainingGoal.MUSCLE_GAIN,
        experience = TrainingExperience.INTERMEDIATE,
        desiredDaysPerWeek = 4,
        preferredWeekdays = setOf(1, 2, 4, 6),
        sessionDuration = SessionDuration.MIN_60,
        environment = TrainingEnvironment.SMALL_GYM,
        equipment = setOf(AvailableEquipment.DUMBBELLS, AvailableEquipment.MACHINES),
    )

    @Test
    fun `metric and imperial conversions are reversible within display precision`() {
        assertEquals(182.88, MeasurementConversion.inchesToCentimeters(72.0), 0.0001)
        assertEquals(72.0, MeasurementConversion.centimetersToInches(182.88), 0.0001)
        assertEquals(81.6466, MeasurementConversion.poundsToKilograms(180.0), 0.0001)
        assertEquals(180.0, MeasurementConversion.kilogramsToPounds(81.6466266), 0.001)
        assertTrue(MeasurementConversion.validHeightCm(120.0))
        assertTrue(MeasurementConversion.validHeightCm(230.0))
        assertFalse(MeasurementConversion.validHeightCm(119.9))
        assertTrue(MeasurementConversion.validWeightKg(35.0))
        assertFalse(MeasurementConversion.validWeightKg(Double.NaN))
    }

    @Test
    fun `adult BMI is a derived screening range and never body composition`() {
        val result = BmiCalculator.calculate(
            completeRequiredProfile.copy(heightCm = 175.0, weightKg = 70.0),
        )

        assertEquals(22.9, result.value ?: 0.0, 0.01)
        assertEquals(BmiStatus.ADULT_SCREENING_ONLY, result.status)
        assertEquals(AdultBmiRange.REFERENCE, result.adultRange)
    }

    @Test
    fun `teen measurement never receives an adult BMI classification`() {
        val result = BmiCalculator.calculate(
            completeRequiredProfile.copy(
                ageGroup = AgeGroup.TEEN,
                heightCm = 175.0,
                weightKg = 70.0,
            ),
        )

        assertEquals(22.9, result.value ?: 0.0, 0.01)
        assertEquals(BmiStatus.ADULT_CLASSIFICATION_UNAVAILABLE, result.status)
        assertNull(result.adultRange)
    }

    @Test
    fun `invalid or skipped measurements do not invent BMI`() {
        val result = BmiCalculator.calculate(completeRequiredProfile.copy(heightCm = 0.0, weightKg = 70.0))

        assertNull(result.value)
        assertEquals(BmiStatus.UNAVAILABLE, result.status)
        assertNull(result.adultRange)
    }

    @Test
    fun `adult target BMI blocks only below 18 point 5`() {
        val base = completeRequiredProfile.copy(
            ageGroup = AgeGroup.ADULT,
            heightCm = 200.0,
            weightKg = 80.0,
        )

        val below = BmiCalculator.targetWeightScreening(base.copy(targetWeightKg = 73.9))
        val boundary = BmiCalculator.targetWeightScreening(base.copy(targetWeightKg = 74.0))
        val high = BmiCalculator.targetWeightScreening(base.copy(targetWeightKg = 140.0))

        assertEquals(18.5, boundary.value ?: 0.0, 0.01)
        assertTrue(below.blocksContinue)
        assertFalse(boundary.blocksContinue)
        assertFalse(high.blocksContinue)
        assertTrue(high.adultThresholdApplied)
        assertTrue(high.isAboveReferenceRange)
    }

    @Test
    fun `adult BMI reference weight range rounds inward for display`() {
        val range = BmiCalculator.adultReferenceWeightRange(166.0)

        assertEquals(51, range?.minimumKg)
        assertEquals(68, range?.maximumKg)

        val screening = BmiCalculator.targetWeightScreening(
            completeRequiredProfile.copy(
                ageGroup = AgeGroup.ADULT,
                heightCm = 166.0,
                targetWeightKg = 50.5,
            ),
        )
        assertEquals(range, screening.referenceRange)
        assertTrue(screening.blocksContinue)
        assertFalse(screening.isAboveReferenceRange)
    }

    @Test
    fun `teen target BMI never applies adult blocking threshold`() {
        val result = BmiCalculator.targetWeightScreening(
            completeRequiredProfile.copy(
                ageGroup = AgeGroup.TEEN,
                heightCm = 175.0,
                weightKg = 60.0,
                targetWeightKg = 45.0,
            ),
        )

        assertTrue((result.value ?: 99.0) < 18.5)
        assertFalse(result.adultThresholdApplied)
        assertFalse(result.blocksContinue)
        assertNull(result.referenceRange)
        assertFalse(result.isAboveReferenceRange)
    }

    @Test
    fun `environment defaults are explicit and replacing environment replaces defaults`() {
        assertEquals(
            setOf(AvailableEquipment.FULL_GYM),
            completeRequiredProfile.withEnvironment(TrainingEnvironment.LARGE_GYM).equipment,
        )
        assertEquals(
            setOf(AvailableEquipment.BODYWEIGHT_ONLY),
            completeRequiredProfile.withEnvironment(TrainingEnvironment.HOME).equipment,
        )
        assertTrue(completeRequiredProfile.withEnvironment(TrainingEnvironment.CUSTOM).equipment.isEmpty())
    }

    @Test
    fun `none pain selection is mutually exclusive with named areas`() {
        val shoulder = completeRequiredProfile.copy(painAreas = emptySet()).togglePainArea(PainArea.SHOULDER)
        assertEquals(setOf(PainArea.SHOULDER), shoulder.painAreas)

        val none = shoulder.togglePainArea(PainArea.NONE)
        assertEquals(setOf(PainArea.NONE), none.painAreas)

        val knee = none.togglePainArea(PainArea.KNEE)
        assertEquals(setOf(PainArea.KNEE), knee.painAreas)
    }

    @Test
    fun `draft JSON round trips stable IDs optional skips and user punctuation`() {
        val original = completeRequiredProfile.copy(
            displayName = "An “AURON” lifter\nlocal-only",
            gender = GenderIdentity.NON_BINARY,
            heightCm = 176.5,
            weightKg = 74.25,
            targetWeightKg = 77.0,
            priorityMuscles = setOf(PriorityMuscle.BACK, PriorityMuscle.SHOULDERS),
            physiqueDirection = PhysiqueDirection.V_TAPER,
            injuryNotes = "Previous ankle sprain; ask before changing exercises.",
            futureAiNote = "Keep pull-ups; don't invent a plan.",
            skippedFields = setOf(OptionalProfileField.TARGET_WEIGHT),
        )

        val restored = OnboardingProfileJson.decodeDraft(OnboardingProfileJson.encodeDraft(original))

        assertEquals(original, restored)

        val legacyJson = JSONObject(OnboardingProfileJson.encodeDraft(original))
        legacyJson.getJSONObject("profile").getJSONObject("recoveryAndConstraints").remove("injuryNotes")
        val legacyRestored = OnboardingProfileJson.decodeDraft(legacyJson.toString())
        assertEquals("", legacyRestored?.injuryNotes)
        assertEquals(original.futureAiNote, legacyRestored?.futureAiNote)
        assertNull(OnboardingProfileJson.decodeDraft("not-json"))
        assertNull(OnboardingProfileJson.decodeDraft("{\"schemaVersion\":999,\"profile\":{}}"))
    }

    @Test
    fun `completeness is deterministic and distinguishes required from optional`() {
        val required = ProfileCompletenessCalculator.calculate(completeRequiredProfile, requiredWeekdays = 4)
        val enriched = ProfileCompletenessCalculator.calculate(
            completeRequiredProfile.copy(
                displayName = "Ari",
                gender = GenderIdentity.WOMAN,
                heightCm = 168.0,
                weightKg = 62.0,
                targetWeightKg = 64.0,
                priorityMuscles = setOf(PriorityMuscle.LEGS),
                physiqueDirection = PhysiqueDirection.BALANCED,
                injuryNotes = "Previous ankle sprain",
            ),
            requiredWeekdays = 4,
        )

        assertTrue(required.requiredReady)
        assertEquals(8, required.requiredComplete)
        assertEquals(8, required.requiredTotal)
        assertEquals(0, required.optionalProvided)
        assertEquals(7, enriched.optionalProvided)
        assertEquals(100, enriched.percentage)
        assertEquals(enriched, ProfileCompletenessCalculator.calculate(enrichedProfile(), 4))
    }

    @Test
    fun `saved profile records provenance and only current app effects`() {
        val completion = ProfileCompletenessCalculator.calculate(completeRequiredProfile, 4)
        val raw = OnboardingProfileJson.encodeSaved(
            SavedTrainingProfile(
                profile = completeRequiredProfile.copy(heightCm = 175.0, weightKg = 70.0),
                createdAtEpochMillis = 100L,
                updatedAtEpochMillis = 200L,
                currentWorkoutGoal = WorkoutGoalSelection.HYPERTROPHY,
                currentWeekdays = setOf(1, 2, 4, 6),
                currentPlanDayNames = listOf("Push", "Pull", "Legs", "Full Body"),
                completion = completion,
            ),
        )
        val root = JSONObject(raw)
        val effects = root.getJSONObject("currentAppEffects")
        val provenance = root.getJSONObject("provenance")

        assertEquals(1, root.getInt("schemaVersion"))
        assertEquals("hypertrophy", effects.getString("workoutGoal"))
        assertTrue(effects.getBoolean("startingPlanUnchanged"))
        assertTrue(effects.getBoolean("weeklyProgressWorkoutOnly"))
        assertTrue(effects.getBoolean("dailyBoostExcludedFromWeeklyProgress"))
        assertEquals("local_shared_preferences", provenance.getString("storage"))
        assertEquals("none", provenance.getString("planGenerator"))
        assertEquals(100L, OnboardingProfileJson.savedCreatedAt(raw))
    }

    @Test
    fun `primary goals map only to the existing coaching goal owner`() {
        assertEquals(WorkoutGoalSelection.HYPERTROPHY, PrimaryTrainingGoal.MUSCLE_GAIN.currentGoal)
        assertEquals(WorkoutGoalSelection.STRENGTH, PrimaryTrainingGoal.POWERBUILDING.currentGoal)
        assertEquals(WorkoutGoalSelection.POWER, PrimaryTrainingGoal.SPORT_PERFORMANCE.currentGoal)
        assertEquals(WorkoutGoalSelection.AUTO, PrimaryTrainingGoal.RETURN_TO_TRAINING.currentGoal)
    }

    private fun enrichedProfile(): OnboardingTrainingProfileDraft = completeRequiredProfile.copy(
        displayName = "Ari",
        gender = GenderIdentity.WOMAN,
        heightCm = 168.0,
        weightKg = 62.0,
        targetWeightKg = 64.0,
        priorityMuscles = setOf(PriorityMuscle.LEGS),
        physiqueDirection = PhysiqueDirection.BALANCED,
        injuryNotes = "Previous ankle sprain",
    )
}
