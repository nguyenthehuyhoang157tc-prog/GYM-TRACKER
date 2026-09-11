package com.hoctin.gymtracker.ui.onboarding

import com.hoctin.gymtracker.data.LATEST_ONBOARDING_VERSION
import com.hoctin.gymtracker.data.OnboardingDraftPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReducerTest {
    private val plan = OnboardingPlanSummary(
        dayIds = listOf(11L, 12L, 13L, 14L),
        dayNames = listOf("Push", "Pull", "Legs", "Full Body"),
        requiredDays = 4,
    )

    private val readyProfile = OnboardingTrainingProfileDraft(
        gender = GenderIdentity.MAN,
        ageGroup = AgeGroup.ADULT,
        primaryGoal = PrimaryTrainingGoal.STRENGTH,
        experience = TrainingExperience.INTERMEDIATE,
        desiredDaysPerWeek = 4,
        preferredWeekdays = setOf(1, 2, 4, 6),
        sessionDuration = SessionDuration.MIN_60,
        environment = TrainingEnvironment.SMALL_GYM,
        equipment = setOf(AvailableEquipment.DUMBBELLS),
    )

    @Test
    fun `progress is based on the shortened active V31 steps`() {
        val count = ACTIVE_ONBOARDING_STEPS.size.toFloat()
        assertEquals(1f / count, OnboardingUiState(step = OnboardingStepV3.WELCOME).progressFraction, 0.001f)
        assertEquals(
            (ACTIVE_ONBOARDING_STEPS.indexOf(OnboardingStepV3.SCHEDULE) + 1f) / count,
            OnboardingUiState(step = OnboardingStepV3.SCHEDULE).progressFraction,
            0.001f,
        )
        assertEquals(
            (ACTIVE_ONBOARDING_STEPS.indexOf(OnboardingStepV3.REVIEW) + 1f) / count,
            OnboardingUiState(step = OnboardingStepV3.REVIEW).progressFraction,
            0.001f,
        )
        assertEquals(1f, OnboardingUiState(step = OnboardingStepV3.RECEIPT).progressFraction, 0.001f)
    }

    @Test
    fun `binary gender choices stay visible and side by side on common phone content width`() {
        assertFalse(binaryChoicesUseSingleColumn(312f, 1f))
        assertFalse(binaryChoicesUseSingleColumn(300f, 1.3f))
        assertTrue(binaryChoicesUseSingleColumn(260f, 1f))
        assertTrue(binaryChoicesUseSingleColumn(312f, 1.6f))
    }

    @Test
    fun `equipment grid keeps two columns on phones and has a safe stacked fallback`() {
        assertFalse(multiChoiceUsesSingleColumn(364f, 1f))
        assertFalse(multiChoiceUsesSingleColumn(320f, 1.3f))
        assertTrue(multiChoiceUsesSingleColumn(274f, 1f))
        assertTrue(multiChoiceUsesSingleColumn(320f, 1.5f))
    }

    @Test
    fun `question typography uses compact tiers without defeating font scaling`() {
        assertEquals(34f, onboardingQuestionMetrics(364f, 1f).titleSp, 0.01f)
        assertEquals(30f, onboardingQuestionMetrics(312f, 1f).titleSp, 0.01f)
        assertEquals(27f, onboardingQuestionMetrics(274f, 1f).titleSp, 0.01f)
        assertEquals(28f, onboardingQuestionMetrics(312f, 1.6f).titleSp, 0.01f)
    }

    @Test
    fun `receipt preparation is ten seconds phased and monotonic for every cadence`() {
        assertEquals(1, receiptPreparationFrame(0L, 0).progress)
        assertEquals(ReceiptPreparationPhase.CALCULATING, receiptPreparationFrame(4_999L, 0).phase)
        assertEquals(45, receiptPreparationFrame(5_000L, 0).progress)
        assertEquals(ReceiptPreparationPhase.BUILDING, receiptPreparationFrame(5_000L, 0).phase)
        assertEquals(60, receiptPreparationFrame(6_000L, 0).progress)
        assertEquals(ReceiptPreparationPhase.PERSONALIZING, receiptPreparationFrame(6_000L, 0).phase)
        assertEquals(100, receiptPreparationFrame(10_000L, 0).progress)
        assertTrue(receiptPreparationFrame(10_000L, 0).complete)

        (0..5).forEach { seed ->
            val values = (0L..10_000L step 100L).map { receiptPreparationFrame(it, seed).progress }
            assertTrue(values.zipWithNext().all { (left, right) -> right >= left })
        }
    }

    @Test
    fun `one thesis steps require their exact consumer data`() {
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.GOAL, readyProfile.copy(primaryGoal = null), plan, false, false))
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.GOAL, readyProfile, plan, false, false))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.EQUIPMENT, readyProfile.copy(equipment = emptySet()), plan, false, false))
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.EQUIPMENT, readyProfile, plan, false, false))
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.CONSTRAINTS, readyProfile.copy(injuryNotes = ""), plan, false, false))
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.GENDER, readyProfile.copy(gender = GenderIdentity.MAN), plan, false, false))
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.GENDER, readyProfile.copy(gender = GenderIdentity.WOMAN), plan, false, false))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.GENDER, readyProfile.copy(gender = GenderIdentity.NON_BINARY), plan, false, false))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.GENDER, readyProfile.copy(gender = GenderIdentity.PREFER_NOT_TO_SAY), plan, false, false))
    }

    @Test
    fun `schedule requires exactly the selected days per week independent of plan size`() {
        assertTrue(canContinueOnboardingV3(OnboardingStepV3.SCHEDULE, readyProfile, plan, false, false))
        assertFalse(
            canContinueOnboardingV3(
                OnboardingStepV3.SCHEDULE,
                readyProfile.copy(preferredWeekdays = setOf(1, 2, 3, 4, 5)),
                plan,
                false,
                false,
            ),
        )
        assertTrue(
            canContinueOnboardingV3(
                OnboardingStepV3.SCHEDULE,
                readyProfile.copy(
                    desiredDaysPerWeek = 5,
                    preferredWeekdays = setOf(1, 2, 3, 4, 5),
                ),
                plan,
                false,
                false,
            ),
        )
        assertEquals(4, readyProfile.desiredDaysPerWeek)
        assertEquals(4, readyProfile.preferredWeekdays.size)
    }

    @Test
    fun `measurements are valid or explicitly skipped`() {
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.MEASUREMENTS, readyProfile, plan, false, false))
        assertTrue(
            canContinueOnboardingV3(
                OnboardingStepV3.MEASUREMENTS,
                readyProfile.copy(heightCm = 176.0, weightKg = 72.0),
                plan,
                false,
                false,
            ),
        )
        assertTrue(
            canContinueOnboardingV3(
                OnboardingStepV3.MEASUREMENTS,
                readyProfile.copy(skippedFields = setOf(OptionalProfileField.MEASUREMENTS)),
                plan,
                false,
                false,
            ),
        )
    }

    @Test
    fun `preparing saving and unavailable plan block progress`() {
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.WELCOME, readyProfile, null, false, false))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.REVIEW, readyProfile, plan, true, false))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.REVIEW, readyProfile, plan, false, true))
        assertFalse(canContinueOnboardingV3(OnboardingStepV3.WELCOME, readyProfile, plan.copy(requiredDays = 0), false, false))
    }

    @Test
    fun `back and forward navigation follow active steps and skip removed questionnaires`() {
        assertEquals(OnboardingStepV3.WELCOME, previousOnboardingStep(OnboardingStepV3.WELCOME))
        ACTIVE_ONBOARDING_STEPS.drop(1).forEachIndexed { index, step ->
            assertEquals(ACTIVE_ONBOARDING_STEPS[index], previousOnboardingStep(step))
        }
        ACTIVE_ONBOARDING_STEPS.dropLast(1).forEachIndexed { index, step ->
            assertEquals(ACTIVE_ONBOARDING_STEPS[index + 1], nextOnboardingStep(step))
        }
        assertEquals(OnboardingStepV3.FREQUENCY, normalizeActiveOnboardingStep(OnboardingStepV3.COMPOUND_EXPERIENCE))
        assertEquals(OnboardingStepV3.REVIEW, normalizeActiveOnboardingStep(OnboardingStepV3.EVIDENCE))
        assertEquals(OnboardingStepV3.CONSTRAINTS, normalizeActiveOnboardingStep(OnboardingStepV3.RECOVERY))
        assertEquals(OnboardingStepV3.RECEIPT, nextOnboardingStep(OnboardingStepV3.RECEIPT))
    }

    @Test
    fun `versioned draft round trips stable profile and review edit boundary`() {
        val profileJson = OnboardingProfileJson.encodeDraft(readyProfile)
        val restored = OnboardingDraftV3.fromPreferences(
            OnboardingDraftPreferences(
                step = "environment",
                profileJson = profileJson,
                editReturnAfter = "physique",
            ),
        )
        assertEquals(OnboardingStepV3.ENVIRONMENT, restored?.committedStep)
        assertEquals(OnboardingStepV3.PHYSIQUE, restored?.editReturnAfter)
        assertEquals(PrimaryTrainingGoal.STRENGTH, restored?.profile?.primaryGoal)
        assertEquals(setOf(1, 2, 4, 6), restored?.profile?.preferredWeekdays)
        assertEquals(LATEST_ONBOARDING_VERSION, restored?.version)

        assertNull(
            OnboardingDraftV3.fromPreferences(
                OnboardingDraftPreferences(
                    version = 999,
                    step = "goal",
                    profileJson = profileJson,
                ),
            ),
        )

        val oldCompoundDraft = OnboardingDraftV3.fromPreferences(
            OnboardingDraftPreferences(
                step = "compound_experience",
                profileJson = profileJson,
            ),
        )
        val oldEvidenceDraft = OnboardingDraftV3.fromPreferences(
            OnboardingDraftPreferences(
                step = "evidence",
                profileJson = profileJson,
            ),
        )
        assertEquals(OnboardingStepV3.FREQUENCY, oldCompoundDraft?.committedStep)
        assertEquals(OnboardingStepV3.REVIEW, oldEvidenceDraft?.committedStep)
    }
}
