package com.hoctin.gymtracker.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val restoredDraft = OnboardingDraftV3.fromPreferences(
        preferencesRepository.onboardingDraft.value,
    )
    private val receiptWasPending = preferencesRepository.onboardingReceiptPending.value

    private val draft = MutableStateFlow(
        restoredDraft ?: OnboardingDraftV3(
            profile = if (receiptWasPending) legacyReceiptProfile() else OnboardingTrainingProfileDraft(),
        ),
    )
    private val step = MutableStateFlow(
        when {
            receiptWasPending -> OnboardingStepV3.RECEIPT
            restoredDraft?.committedStep == OnboardingStepV3.RECEIPT -> OnboardingStepV3.REVIEW
            else -> restoredDraft?.committedStep ?: OnboardingStepV3.WELCOME
        },
    )
    private val planSummary = MutableStateFlow<OnboardingPlanSummary?>(null)
    private val isPreparing = MutableStateFlow(true)
    private val isSaving = MutableStateFlow(false)
    private val error = MutableStateFlow<OnboardingError?>(null)
    private val busyState = combine(isPreparing, isSaving, error) { preparing, saving, currentError ->
        Triple(preparing, saving, currentError)
    }

    internal val uiState: StateFlow<OnboardingUiState> = combine(
        step,
        draft,
        planSummary,
        busyState,
    ) { currentStep, currentDraft, currentPlan, busy ->
        OnboardingUiState(
            step = currentStep,
            draft = currentDraft,
            plan = currentPlan,
            isPreparing = busy.first,
            isSaving = busy.second,
            error = busy.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState(
            step = step.value,
            draft = draft.value,
            isPreparing = true,
        ),
    )

    private val _effects = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = 4)
    internal val effects = _effects.asSharedFlow()

    init {
        observePlan()
        preparePlan()
    }

    internal fun updateProfile(
        transform: (OnboardingTrainingProfileDraft) -> OnboardingTrainingProfileDraft,
    ): Boolean {
        if (isSaving.value) return false
        val before = draft.value.profile
        val after = transform(before).sanitized()
        if (before == after) return false
        error.value = null
        draft.update { it.copy(profile = after) }
        persistDraft()
        return true
    }

    internal fun setDisplayName(value: String): Boolean = updateProfile {
        it.copy(
            displayName = value.take(60),
            skippedFields = it.skippedFields - OptionalProfileField.DISPLAY_NAME,
        )
    }

    internal fun selectEnvironment(value: TrainingEnvironment): Boolean = updateProfile {
        it.withEnvironment(value)
    }

    internal fun toggleWeekday(day: Int): Boolean {
        if (isSaving.value || day !in 1..7) return false
        val current = draft.value.profile.preferredWeekdays
        val required = draft.value.profile.desiredDaysPerWeek?.takeIf { it in 1..7 } ?: return false
        val updated = when {
            day in current -> current - day
            current.size < required -> current + day
            else -> return false
        }.toSortedSet()
        return updateProfile { it.copy(preferredWeekdays = updated) }
    }

    internal fun toggleEquipment(value: AvailableEquipment): Boolean = updateProfile { profile ->
        val current = profile.equipment
        val updated = when {
            value == AvailableEquipment.FULL_GYM ->
                if (value in current) emptySet() else setOf(value)
            value == AvailableEquipment.BODYWEIGHT_ONLY ->
                if (value in current) emptySet() else setOf(value)
            value in current -> current - value
            else -> (current - AvailableEquipment.FULL_GYM - AvailableEquipment.BODYWEIGHT_ONLY) + value
        }
        profile.copy(equipment = updated)
    }

    internal fun togglePriorityMuscle(value: PriorityMuscle): Boolean = updateProfile { profile ->
        val current = profile.priorityMuscles
        val updated = when {
            value == PriorityMuscle.WHOLE_BODY ->
                if (value in current) emptySet() else setOf(value)
            value in current -> current - value
            else -> (current - PriorityMuscle.WHOLE_BODY) + value
        }
        profile.copy(
            priorityMuscles = updated,
            skippedFields = profile.skippedFields - OptionalProfileField.PRIORITY_MUSCLES,
        )
    }

    internal fun togglePainArea(value: PainArea): Boolean = updateProfile { it.togglePainArea(value) }

    internal fun setHeightCm(value: Double?): Boolean = updateProfile {
        it.copy(
            heightCm = value,
            skippedFields = it.skippedFields - OptionalProfileField.MEASUREMENTS,
        )
    }

    internal fun setWeightKg(value: Double?): Boolean = updateProfile {
        it.copy(
            weightKg = value,
            skippedFields = it.skippedFields - OptionalProfileField.MEASUREMENTS,
        )
    }

    internal fun setTargetWeightKg(value: Double?): Boolean = updateProfile {
        it.copy(
            targetWeightKg = value,
            skippedFields = it.skippedFields - OptionalProfileField.TARGET_WEIGHT,
        )
    }

    internal fun setFutureNote(value: String): Boolean = updateProfile {
        it.copy(
            futureAiNote = value.take(500),
            skippedFields = it.skippedFields - OptionalProfileField.CONSTRAINT_NOTE,
        )
    }

    internal fun setInjuryNotes(value: String): Boolean = updateProfile {
        it.copy(
            injuryNotes = value.take(500),
            skippedFields = it.skippedFields - OptionalProfileField.INJURIES,
        )
    }

    internal fun skipCurrentStep() {
        if (isSaving.value) return
        when (step.value) {
            OnboardingStepV3.NAME -> updateProfile {
                it.copy(displayName = "", skippedFields = it.skippedFields + OptionalProfileField.DISPLAY_NAME)
            }
            OnboardingStepV3.GENDER -> updateProfile {
                it.copy(
                    gender = GenderIdentity.PREFER_NOT_TO_SAY,
                    skippedFields = it.skippedFields + OptionalProfileField.GENDER,
                )
            }
            OnboardingStepV3.PRIORITY_MUSCLES -> updateProfile {
                it.copy(
                    priorityMuscles = emptySet(),
                    skippedFields = it.skippedFields + OptionalProfileField.PRIORITY_MUSCLES,
                )
            }
            OnboardingStepV3.PHYSIQUE -> updateProfile {
                it.copy(
                    physiqueDirection = null,
                    skippedFields = it.skippedFields + OptionalProfileField.PHYSIQUE_DIRECTION,
                )
            }
            OnboardingStepV3.MEASUREMENTS -> updateProfile {
                it.copy(
                    heightCm = null,
                    weightKg = null,
                    targetWeightKg = null,
                    skippedFields = it.skippedFields +
                        OptionalProfileField.MEASUREMENTS + OptionalProfileField.TARGET_WEIGHT,
                )
            }
            OnboardingStepV3.TARGET_WEIGHT -> updateProfile {
                it.copy(
                    targetWeightKg = null,
                    skippedFields = it.skippedFields + OptionalProfileField.TARGET_WEIGHT,
                )
            }
            OnboardingStepV3.CONSTRAINTS -> updateProfile {
                it.copy(
                    injuryNotes = "",
                    skippedFields = it.skippedFields + OptionalProfileField.INJURIES,
                )
            }
            else -> return
        }
        next()
    }

    internal fun next() {
        if (!uiState.value.canContinue) return
        val currentDraft = draft.value
        if (currentDraft.editReturnAfter == step.value) {
            draft.update { it.copy(editReturnAfter = null) }
            commitStep(OnboardingStepV3.REVIEW)
            return
        }
        if (step.value == OnboardingStepV3.REVIEW || step.value == OnboardingStepV3.RECEIPT) return
        commitStep(nextOnboardingStep(step.value))
    }

    internal fun back() {
        if (isSaving.value) return
        if (draft.value.editReturnAfter != null) {
            draft.update { it.copy(editReturnAfter = null) }
            commitStep(OnboardingStepV3.REVIEW)
            return
        }
        val previous = previousOnboardingStep(step.value)
        if (previous == step.value) return
        error.value = null
        commitStep(previous)
    }

    internal fun editIntent() = beginReviewEdit(OnboardingStepV3.GOAL, OnboardingStepV3.EXPERIENCE)
    internal fun editSchedule() = beginReviewEdit(OnboardingStepV3.FREQUENCY, OnboardingStepV3.DURATION)
    internal fun editEquipment() = beginReviewEdit(OnboardingStepV3.ENVIRONMENT, OnboardingStepV3.PHYSIQUE)
    internal fun editMeasurements() = beginReviewEdit(OnboardingStepV3.MEASUREMENTS, OnboardingStepV3.TARGET_WEIGHT)
    internal fun editRecovery() = beginReviewEdit(OnboardingStepV3.CONSTRAINTS, OnboardingStepV3.CONSTRAINTS)

    internal fun adjustSetup() {
        if (isSaving.value) return
        commitStep(OnboardingStepV3.REVIEW)
    }

    internal fun startOver() {
        if (isSaving.value) return
        preferencesRepository.resetOnboardingDraft()
        draft.value = OnboardingDraftV3()
        step.value = OnboardingStepV3.WELCOME
        error.value = null
        persistDraft()
    }

    internal fun confirmSetup() {
        if (isSaving.value) return
        val state = uiState.value
        val plan = state.plan
        if (plan == null || plan.requiredDays !in 1..7) {
            error.value = OnboardingError.PLAN_UNAVAILABLE
            return
        }
        val selectedDays = state.profile.desiredDaysPerWeek
        if (selectedDays == null || selectedDays !in 1..7 || state.profile.preferredWeekdays.size != selectedDays) {
            error.value = OnboardingError.SCHEDULE_CHANGED
            commitStep(OnboardingStepV3.SCHEDULE)
            return
        }
        if (!state.completeness.requiredReady) return

        val now = nowEpochMillis()
        val createdAt = OnboardingProfileJson.savedCreatedAt(
            preferencesRepository.trainingProfileJson.value,
        ) ?: now
        val saved = SavedTrainingProfile(
            profile = state.profile,
            createdAtEpochMillis = createdAt,
            updatedAtEpochMillis = now,
            currentWorkoutGoal = state.profile.currentWorkoutGoal,
            currentWeekdays = state.profile.preferredWeekdays,
            currentPlanDayNames = plan.dayNames,
            completion = state.completeness,
        )
        val profileJson = OnboardingProfileJson.encodeSaved(saved)

        isSaving.value = true
        error.value = null
        viewModelScope.launch {
            preferencesRepository.completeOnboardingV3(
                goal = state.profile.currentWorkoutGoal,
                weekdays = state.profile.preferredWeekdays,
                profileJson = profileJson,
            ).onSuccess {
                draft.update {
                    it.copy(committedStep = OnboardingStepV3.RECEIPT, editReturnAfter = null)
                }
                step.value = OnboardingStepV3.RECEIPT
                _effects.emit(OnboardingEffect.SuccessHaptic)
            }.onFailure {
                error.value = OnboardingError.SAVE_FAILED
            }
            isSaving.value = false
        }
    }

    internal fun finishToHome() = finishReceipt(OnboardingExit.Home)

    internal fun finishToReviewDay1() {
        val dayId = planSummary.value?.firstDayId
        finishReceipt(dayId?.let { OnboardingExit.ReviewDay1(it) } ?: OnboardingExit.Home)
    }

    internal fun retryPreparation() = preparePlan()

    private fun beginReviewEdit(start: OnboardingStepV3, end: OnboardingStepV3) {
        if (isSaving.value) return
        draft.update { it.copy(committedStep = start, editReturnAfter = end) }
        step.value = start
        persistDraft()
    }

    private fun finishReceipt(destination: OnboardingExit) {
        if (isSaving.value) return
        isSaving.value = true
        error.value = null
        viewModelScope.launch {
            preferencesRepository.finishOnboardingReceipt().onSuccess {
                _effects.emit(OnboardingEffect.Exit(destination))
            }.onFailure {
                error.value = OnboardingError.RECEIPT_EXIT_FAILED
            }
            isSaving.value = false
        }
    }

    private fun observePlan() {
        viewModelScope.launch {
            repository.observePlan().collect { plan ->
                val summary = plan.toOnboardingSummary()
                planSummary.value = summary
            }
        }
    }

    private fun preparePlan() {
        if (isPreparing.value && planSummary.value != null) return
        viewModelScope.launch {
            isPreparing.value = true
            error.value = null
            runCatching {
                repository.ensureSeedData()
                repository.observePlan().first().toOnboardingSummary()
            }.onSuccess { resolvedPlan ->
                planSummary.value = resolvedPlan
                if (resolvedPlan.requiredDays == 0) error.value = OnboardingError.PLAN_UNAVAILABLE
            }.onFailure {
                error.value = OnboardingError.PREPARATION_FAILED
            }
            isPreparing.value = false
        }
    }

    private fun commitStep(newStep: OnboardingStepV3) {
        step.value = newStep
        draft.update { it.copy(committedStep = newStep) }
        persistDraft()
    }

    private fun persistDraft() {
        val current = draft.value
        preferencesRepository.saveOnboardingDraft(
            step = current.committedStep.storageValue,
            profileJson = OnboardingProfileJson.encodeDraft(current.profile),
            editReturnAfter = current.editReturnAfter?.storageValue,
        )
    }

    private fun legacyReceiptProfile(): OnboardingTrainingProfileDraft =
        OnboardingTrainingProfileDraft(
            primaryGoal = preferencesRepository.workoutGoal.value.toPrimaryTrainingGoal(),
            preferredWeekdays = preferencesRepository.trainingWeekdays.value,
            desiredDaysPerWeek = preferencesRepository.trainingWeekdays.value.size.takeIf { it in 1..7 },
        )

    private fun WorkoutGoalSelection.toPrimaryTrainingGoal(): PrimaryTrainingGoal = when (this) {
        WorkoutGoalSelection.AUTO -> PrimaryTrainingGoal.RETURN_TO_TRAINING
        WorkoutGoalSelection.HYPERTROPHY -> PrimaryTrainingGoal.MUSCLE_GAIN
        WorkoutGoalSelection.STRENGTH -> PrimaryTrainingGoal.STRENGTH
        WorkoutGoalSelection.POWER -> PrimaryTrainingGoal.SPORT_PERFORMANCE
        WorkoutGoalSelection.ENDURANCE -> PrimaryTrainingGoal.MAINTAIN
        WorkoutGoalSelection.GENERAL_FITNESS -> PrimaryTrainingGoal.MAINTAIN
        WorkoutGoalSelection.FAT_LOSS_SUPPORT -> PrimaryTrainingGoal.FAT_LOSS
        WorkoutGoalSelection.SKILL -> PrimaryTrainingGoal.SPORT_PERFORMANCE
    }

    private fun OnboardingTrainingProfileDraft.sanitized(): OnboardingTrainingProfileDraft = copy(
        displayName = displayName.trimStart().take(60),
        preferredWeekdays = preferredWeekdays.filter { it in 1..7 }.toSortedSet(),
        desiredDaysPerWeek = desiredDaysPerWeek?.takeIf { it in 1..7 },
        heightCm = heightCm?.takeIf { it.isFinite() },
        weightKg = weightKg?.takeIf { it.isFinite() },
        targetWeightKg = targetWeightKg?.takeIf { it.isFinite() },
        injuryNotes = injuryNotes.take(500),
        futureAiNote = futureAiNote.take(500),
    )

    private fun List<com.hoctin.gymtracker.data.WorkoutDayWithExercises>.toOnboardingSummary() =
        OnboardingPlanSummary(
            dayIds = map { it.day.id },
            dayNames = map { it.day.name },
            requiredDays = size.coerceIn(0, 7),
        )
}
