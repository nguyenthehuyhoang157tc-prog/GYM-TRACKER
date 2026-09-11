package com.hoctin.gymtracker.ui.onboarding

import com.hoctin.gymtracker.data.LATEST_ONBOARDING_VERSION
import com.hoctin.gymtracker.data.OnboardingDraftPreferences

internal enum class OnboardingStepV3(val storageValue: String, val accessibleName: String) {
    WELCOME("welcome", "Welcome"),
    NAME("name", "Your name"),
    GENDER("gender", "Gender identity"),
    AGE("age", "Age group"),
    GOAL("goal", "Primary training goal"),
    EXPERIENCE("experience", "Training experience"),
    COMPOUND_EXPERIENCE("compound_experience", "Compound-lift experience"),
    FREQUENCY("frequency", "Preferred training frequency"),
    SCHEDULE("schedule", "Starting-week schedule"),
    DURATION("duration", "Session duration"),
    ENVIRONMENT("environment", "Training environment"),
    EQUIPMENT("equipment", "Available equipment"),
    PRIORITY_MUSCLES("priority_muscles", "Priority muscles"),
    PHYSIQUE("physique", "Physique direction"),
    MEASUREMENTS("measurements", "Height and weight"),
    TARGET_WEIGHT("target_weight", "Target weight"),
    RECOVERY("recovery", "Recovery and cardio"),
    CONSTRAINTS("constraints", "Pain and constraints"),
    EVIDENCE("evidence", "Training principle"),
    PROFILE_READY("profile_ready", "Profile readiness"),
    REVIEW("review", "Review profile"),
    RECEIPT("receipt", "Profile ready");

    companion object {
        fun fromStorageValue(value: String?): OnboardingStepV3 =
            entries.firstOrNull { it.storageValue == value } ?: WELCOME
    }
}

/**
 * V3.1 keeps every V3 storage value readable while presenting a shorter active journey. Removed
 * steps remain in [OnboardingStepV3] so an interrupted V3 draft can always be resumed safely.
 */
internal val ACTIVE_ONBOARDING_STEPS = listOf(
    OnboardingStepV3.WELCOME,
    OnboardingStepV3.NAME,
    OnboardingStepV3.GENDER,
    OnboardingStepV3.AGE,
    OnboardingStepV3.GOAL,
    OnboardingStepV3.EXPERIENCE,
    OnboardingStepV3.FREQUENCY,
    OnboardingStepV3.SCHEDULE,
    OnboardingStepV3.DURATION,
    OnboardingStepV3.ENVIRONMENT,
    OnboardingStepV3.EQUIPMENT,
    OnboardingStepV3.PRIORITY_MUSCLES,
    OnboardingStepV3.PHYSIQUE,
    OnboardingStepV3.MEASUREMENTS,
    OnboardingStepV3.TARGET_WEIGHT,
    OnboardingStepV3.CONSTRAINTS,
    OnboardingStepV3.REVIEW,
    OnboardingStepV3.RECEIPT,
)

internal fun normalizeActiveOnboardingStep(step: OnboardingStepV3): OnboardingStepV3 = when (step) {
    OnboardingStepV3.COMPOUND_EXPERIENCE -> OnboardingStepV3.FREQUENCY
    OnboardingStepV3.RECOVERY -> OnboardingStepV3.CONSTRAINTS
    OnboardingStepV3.EVIDENCE,
    OnboardingStepV3.PROFILE_READY -> OnboardingStepV3.REVIEW
    else -> step
}

internal data class OnboardingDraftV3(
    val version: Int = LATEST_ONBOARDING_VERSION,
    val committedStep: OnboardingStepV3 = OnboardingStepV3.WELCOME,
    val profile: OnboardingTrainingProfileDraft = OnboardingTrainingProfileDraft(),
    val editReturnAfter: OnboardingStepV3? = null,
) {
    companion object {
        fun fromPreferences(value: OnboardingDraftPreferences?): OnboardingDraftV3? {
            if (value == null || value.version != LATEST_ONBOARDING_VERSION) return null
            val restoredProfile = OnboardingProfileJson.decodeDraft(value.profileJson) ?: return null
            return OnboardingDraftV3(
                version = value.version,
                committedStep = normalizeActiveOnboardingStep(OnboardingStepV3.fromStorageValue(value.step)),
                profile = restoredProfile,
                editReturnAfter = OnboardingStepV3.entries.firstOrNull {
                    it.storageValue == value.editReturnAfter
                }?.let(::normalizeActiveOnboardingStep),
            )
        }
    }
}

internal data class OnboardingPlanSummary(
    val dayIds: List<Long> = emptyList(),
    val dayNames: List<String> = emptyList(),
    val requiredDays: Int = 0,
) {
    val firstDayId: Long? get() = dayIds.firstOrNull()
}

internal enum class OnboardingError {
    PREPARATION_FAILED,
    PLAN_UNAVAILABLE,
    SCHEDULE_CHANGED,
    INVALID_MEASUREMENTS,
    SAVE_FAILED,
    RECEIPT_EXIT_FAILED,
}

internal data class OnboardingUiState(
    val step: OnboardingStepV3 = OnboardingStepV3.WELCOME,
    val draft: OnboardingDraftV3 = OnboardingDraftV3(),
    val plan: OnboardingPlanSummary? = null,
    val isPreparing: Boolean = true,
    val isSaving: Boolean = false,
    val error: OnboardingError? = null,
) {
    val profile: OnboardingTrainingProfileDraft get() = draft.profile

    val completeness: ProfileCompleteness
        get() = ProfileCompletenessCalculator.calculate(profile, plan?.requiredDays ?: 0)

    val bmi: BmiResult get() = BmiCalculator.calculate(profile)

    val targetBmiScreening: TargetBmiScreening get() = BmiCalculator.targetWeightScreening(profile)

    val canContinue: Boolean
        get() = canContinueOnboardingV3(step, profile, plan, isPreparing, isSaving)

    val progressFraction: Float
        get() {
            val activeStep = normalizeActiveOnboardingStep(step)
            val index = ACTIVE_ONBOARDING_STEPS.indexOf(activeStep).coerceAtLeast(0)
            return (index + 1).toFloat() / ACTIVE_ONBOARDING_STEPS.size.toFloat()
        }

    val isEditingReviewSection: Boolean get() = draft.editReturnAfter != null
}

sealed interface OnboardingExit {
    data object Home : OnboardingExit
    data class ReviewDay1(val dayId: Long) : OnboardingExit
}

internal sealed interface OnboardingEffect {
    data object SuccessHaptic : OnboardingEffect
    data class Exit(val destination: OnboardingExit) : OnboardingEffect
}

internal enum class ReceiptPreparationPhase {
    CALCULATING,
    BUILDING,
    PERSONALIZING,
}

internal data class ReceiptPreparationFrame(
    val progress: Int,
    val phase: ReceiptPreparationPhase,
    val complete: Boolean,
)

/**
 * A presentation-only ten-second receipt timeline. The final four seconds use rotated progress
 * increments so each entry has a slightly different cadence while remaining monotonic and fully
 * deterministic for tests. It does not represent remote work or an AI response.
 */
internal fun receiptPreparationFrame(
    elapsedMillis: Long,
    cadenceSeed: Int,
): ReceiptPreparationFrame {
    val elapsed = elapsedMillis.coerceIn(0L, 10_000L)
    val progress = when {
        elapsed <= 5_000L -> 1.0 + 44.0 * (elapsed / 5_000.0)
        elapsed <= 6_000L -> 45.0 + 15.0 * ((elapsed - 5_000L) / 1_000.0)
        else -> {
            val durations = intArrayOf(420, 700, 520, 820, 600, 940)
            val baseIncrements = intArrayOf(5, 9, 4, 10, 5, 7)
            val rotation = Math.floorMod(cadenceSeed, baseIncrements.size)
            val increments = IntArray(baseIncrements.size) { index ->
                baseIncrements[(index + rotation) % baseIncrements.size]
            }
            var remaining = (elapsed - 6_000L).toDouble()
            var value = 60.0
            for (index in durations.indices) {
                val duration = durations[index].toDouble()
                if (remaining >= duration) {
                    value += increments[index]
                    remaining -= duration
                } else {
                    value += increments[index] * (remaining / duration)
                    break
                }
            }
            value
        }
    }
    val phase = when {
        elapsed < 5_000L -> ReceiptPreparationPhase.CALCULATING
        elapsed < 6_000L -> ReceiptPreparationPhase.BUILDING
        else -> ReceiptPreparationPhase.PERSONALIZING
    }
    return ReceiptPreparationFrame(
        progress = progress.toInt().coerceIn(1, 100),
        phase = phase,
        complete = elapsed >= 10_000L,
    )
}

internal fun canContinueOnboardingV3(
    step: OnboardingStepV3,
    profile: OnboardingTrainingProfileDraft,
    plan: OnboardingPlanSummary?,
    isPreparing: Boolean,
    isSaving: Boolean,
): Boolean {
    if (isPreparing || isSaving) return false
    return when (step) {
        OnboardingStepV3.WELCOME -> plan?.requiredDays in 1..7
        OnboardingStepV3.NAME -> true
        OnboardingStepV3.GENDER -> profile.gender == GenderIdentity.MAN || profile.gender == GenderIdentity.WOMAN
        OnboardingStepV3.AGE -> profile.ageGroup != null
        OnboardingStepV3.GOAL -> profile.primaryGoal != null
        OnboardingStepV3.EXPERIENCE -> profile.experience != null
        OnboardingStepV3.COMPOUND_EXPERIENCE -> true
        OnboardingStepV3.FREQUENCY -> profile.desiredDaysPerWeek in 1..7
        OnboardingStepV3.SCHEDULE ->
            profile.desiredDaysPerWeek in 1..7 &&
                profile.preferredWeekdays.size == profile.desiredDaysPerWeek
        OnboardingStepV3.DURATION -> profile.sessionDuration != null
        OnboardingStepV3.ENVIRONMENT -> profile.environment != null
        OnboardingStepV3.EQUIPMENT -> profile.equipment.isNotEmpty()
        OnboardingStepV3.PRIORITY_MUSCLES,
        OnboardingStepV3.PHYSIQUE -> true
        OnboardingStepV3.TARGET_WEIGHT ->
            OptionalProfileField.TARGET_WEIGHT in profile.skippedFields ||
                (MeasurementConversion.validWeightKg(profile.targetWeightKg) &&
                    !BmiCalculator.targetWeightScreening(profile).blocksContinue)
        OnboardingStepV3.MEASUREMENTS -> {
            OptionalProfileField.MEASUREMENTS in profile.skippedFields ||
                (MeasurementConversion.validHeightCm(profile.heightCm) &&
                    MeasurementConversion.validWeightKg(profile.weightKg))
        }
        OnboardingStepV3.RECOVERY,
        OnboardingStepV3.CONSTRAINTS -> true
        OnboardingStepV3.EVIDENCE -> profile.desiredDaysPerWeek in 2..6
        OnboardingStepV3.PROFILE_READY ->
            ProfileCompletenessCalculator.calculate(profile, plan?.requiredDays ?: 0).requiredReady
        OnboardingStepV3.REVIEW ->
            plan?.requiredDays in 1..7 &&
                profile.desiredDaysPerWeek in 1..7 &&
                profile.preferredWeekdays.size == profile.desiredDaysPerWeek &&
                ProfileCompletenessCalculator.calculate(profile, plan?.requiredDays ?: 0).requiredReady
        OnboardingStepV3.RECEIPT -> true
    }
}

internal fun nextOnboardingStep(step: OnboardingStepV3): OnboardingStepV3 {
    val activeStep = normalizeActiveOnboardingStep(step)
    val index = ACTIVE_ONBOARDING_STEPS.indexOf(activeStep).coerceAtLeast(0)
    return ACTIVE_ONBOARDING_STEPS.getOrElse(index + 1) { OnboardingStepV3.RECEIPT }
}

internal fun previousOnboardingStep(step: OnboardingStepV3): OnboardingStepV3 {
    val activeStep = normalizeActiveOnboardingStep(step)
    val index = ACTIVE_ONBOARDING_STEPS.indexOf(activeStep).coerceAtLeast(0)
    return ACTIVE_ONBOARDING_STEPS.getOrElse(index - 1) { OnboardingStepV3.WELCOME }
}

internal const val WELCOME_SUPPORT_COPY =
    "Set your training profile, schedule and equipment. You will start from the plan already in AURON."
internal const val SCHEDULE_SCOPE_COPY =
    "These days set Weekly Progress. It counts distinct Routine workout dates only. Saved One-Off workouts never count, and Daily Boost never counts."
internal const val REVIEW_TRUTH_COPY =
    "Only your coaching goal and current training weekdays affect AURON today. The rest stays local for a future, separately validated plan generator."
internal const val INJURY_DISCLAIMER_COPY =
    "This note is not a diagnosis, treatment plan, or clearance to train. Seek qualified medical care for severe, persistent, or worsening symptoms."
internal const val TARGET_BMI_WARNING_COPY =
    "BMI = kg / m². For adults, a value below 18.5 is below the standard screening threshold. BMI is a screening value, not a diagnosis. Choose a higher target weight to continue."
internal const val RECEIPT_BOUNDARY_COPY =
    "Your goal guides future coaching. Weekly Progress counts Routine workout dates only. Saved One-Off workouts never count, and Daily Boost never counts. The current split is unchanged and is not assigned to specific weekdays."
