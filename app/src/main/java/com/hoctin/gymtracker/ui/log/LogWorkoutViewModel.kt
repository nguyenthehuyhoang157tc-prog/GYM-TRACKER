package com.hoctin.gymtracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.DaySelectionOrigin
import com.hoctin.gymtracker.data.DraftSet
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseHistoryItem
import com.hoctin.gymtracker.data.ExerciseLogInput
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.SavedWorkoutExerciseLogInput
import com.hoctin.gymtracker.data.SavedWorkoutRepository
import com.hoctin.gymtracker.data.SavedWorkoutWithExercises
import com.hoctin.gymtracker.data.SetInput
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.data.WorkoutAttemptIdentity
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.ProgressionType
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.research.RecoveryResearchFreezeRequest
import com.hoctin.gymtracker.data.research.RecoveryResearchRepository
import com.hoctin.gymtracker.data.wearable.WearableShadowRepository
import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.domain.OverloadSuggestion
import com.hoctin.gymtracker.domain.ProgressionProfile
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import com.hoctin.gymtracker.domain.toWorkoutContext
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class SetDraft(
    val weight: String = "",
    val reps: String = "",
    val rir: String = "2",
    val notes: String = "",
    // DB-6C: raw time/distance inputs for non-rep tracking types (empty for normal reps/kg sets).
    val duration: String = "",
    val distance: String = "",
    val researchRowKey: String = UUID.randomUUID().toString(),
    // LOG-V4A foundation: presentation/session state only. Draft persistence intentionally omits it.
    val setType: LogSetType = LogSetType.STRAIGHT,
    val weightDistribution: LogWeightDistribution = LogWeightDistribution.TOTAL,
    val continuationOfKey: String? = null,
)

data class ExerciseLogState(
    val exercise: ExerciseEntity,
    val previousSets: List<LoggedSetEntity>,
    val history: List<ExerciseHistoryItem> = emptyList(),
    val draftSets: List<SetDraft>,
    val trendLabel: String,
    val potentialPr: Boolean,
    val suggestion: OverloadSuggestion,
    // DB-6C: which fields the Log row renders (resolved from the catalog; REPS_WEIGHT for custom).
    val trackingType: TrackingType = TrackingType.REPS_WEIGHT,
    val progressionType: ProgressionType = ProgressionType.COMPOUND_LOAD,
    val completedSetCount: Int = 0,
    val completedBaseSetCount: Int = 0,
    val currentReps: Int = 0,
    val currentVolume: Double = 0.0,
    val currentDurationSeconds: Int = 0,
    val currentDistanceMeters: Double = 0.0,
    val isComplete: Boolean = false,
    val repeatedDropSetCaution: Boolean = false,
    val canContinueLastSet: Boolean = false,
)

data class LogWorkoutUiState(
    val day: WorkoutDayEntity? = null,
    val workoutName: String = "Loading",
    val isSavedOneOff: Boolean = false,
    val exercises: List<ExerciseLogState> = emptyList(),
    // Active-exercise accordion: only this exercise renders full input fields. Null = all collapsed.
    val expandedExerciseId: Long? = null,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val message: String? = null,
    // Epoch millis when this logging session started, for the live duration timer. Persisted with the
    // draft so the timer survives screen-off, backgrounding, and returning to an active draft. 0 = not set.
    val startedAtMillis: Long = 0L,
    val restTimer: RestTimerUiState = RestTimerUiState(),
    val nextExercisePromptId: Long? = null,
    val scrollToExerciseId: Long? = null,
)

sealed interface LogWorkoutEvent {
    /**
     * [durationMillis] (MUSCLE-3) is the wall-clock time from opening this Log screen to saving —
     * a runtime-only proxy for workout duration (nothing is persisted; 0 = unknown).
     */
    data class Saved(val sessionId: Long, val durationMillis: Long = 0L) : LogWorkoutEvent
    object Discarded : LogWorkoutEvent
}

sealed interface LogWorkoutLaunch {
    data class Routine(val dayId: Long) : LogWorkoutLaunch
    data class SavedOneOff(val savedWorkoutId: Long) : LogWorkoutLaunch
}

internal fun parseSetDraftInput(
    draft: SetDraft,
    type: TrackingType,
    distanceUnitSystem: com.hoctin.gymtracker.data.DistanceUnitSystem,
    notes: String?,
): SetInput? {
    fun duration() = MeasurementFormatter.parseDurationSeconds(draft.duration)?.takeIf { it > 0 }
    fun distance() = MeasurementFormatter.parseDistanceMeters(draft.distance, distanceUnitSystem)?.takeIf { it > 0.0 }
    fun load() = draft.weight.toDoubleOrNull()?.takeIf { it >= 0.0 }
    return when (type) {
        TrackingType.REPS_WEIGHT -> {
            val weight = draft.weight.toDoubleOrNull()
            val reps = draft.reps.toIntOrNull()
            val rir = draft.rir.toIntOrNull()
            if (weight == null || reps == null || rir == null || weight < 0.0 || reps <= 0) null
            else SetInput(weight, reps, rir.coerceIn(0, 5), notes)
        }
        TrackingType.BODYWEIGHT_REPS, TrackingType.SKILL_FORM -> {
            val weight = draft.weight.toDoubleOrNull() ?: 0.0
            val reps = draft.reps.toIntOrNull()
            val rir = draft.rir.toIntOrNull()
            if (weight < 0.0 || reps == null || rir == null || reps <= 0) null
            else SetInput(weight, reps, rir.coerceIn(0, 5), notes)
        }
        TrackingType.TIME_ONLY ->
            duration()?.let { SetInput(0.0, 0, 0, notes, durationSeconds = it) }
        TrackingType.TIME_LOAD -> {
            val d = duration()
            val w = load()
            if (d == null || w == null) null else SetInput(w, 0, 0, notes, durationSeconds = d)
        }
        TrackingType.DISTANCE_LOAD -> {
            val dist = distance()
            val w = load()
            if (dist == null || w == null) null else SetInput(w, 0, 0, notes, distanceMeters = dist)
        }
        TrackingType.TIME_DISTANCE -> {
            val d = duration()
            val dist = distance()
            if (d == null || dist == null) null
            else SetInput(0.0, 0, 0, notes, durationSeconds = d, distanceMeters = dist)
        }
        TrackingType.TIME_DISTANCE_LOAD -> {
            val d = duration()
            val dist = distance()
            val w = load()
            if (d == null || dist == null || w == null) null
            else SetInput(w, 0, 0, notes, durationSeconds = d, distanceMeters = dist)
        }
    }
}

class LogWorkoutViewModel(
    private val launch: LogWorkoutLaunch,
    private val repository: GymRepository,
    private val savedWorkoutRepository: SavedWorkoutRepository,
    private val draftRepository: WorkoutDraftRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val catalogRepository: ExerciseCatalogRepository,
    private val recoveryResearchRepository: RecoveryResearchRepository,
    private val wearableShadowRepository: WearableShadowRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogWorkoutUiState())
    val uiState: StateFlow<LogWorkoutUiState> = _uiState.asStateFlow()

    /**
     * Heaviest logged load per plan exercise id, rendered as a "Max Weight" subtitle under the
     * exercise name. Stays empty until the first weighted set is logged in any session.
     */
    val maxWeightByExerciseId: StateFlow<Map<Long, Double>> = repository.observeMaxWeightByExerciseId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _events = MutableSharedFlow<LogWorkoutEvent>()
    val events: SharedFlow<LogWorkoutEvent> = _events.asSharedFlow()

    private var currentDay: WorkoutDayEntity? = null
    private var currentSavedWorkout: SavedWorkoutWithExercises? = null
    private var currentExercises: List<ExerciseEntity> = emptyList()
    private val draftsByExercise = mutableMapOf<Long, MutableList<SetDraft>>()
    private val historyByExercise = mutableMapOf<Long, List<LoggedSetEntity>>()
    private val historyItemsByExercise = mutableMapOf<Long, List<ExerciseHistoryItem>>()
    // V3.5: catalog progression metadata per plan exercise (null = custom/unmatched -> name fallback).
    private val profilesByExercise = mutableMapOf<Long, ProgressionProfile?>()
    // DB-6C: catalog tracking type per plan exercise (resolved by name; REPS_WEIGHT for custom/unmatched).
    private val trackingTypeByExercise = mutableMapOf<Long, TrackingType>()
    private val progressionTypeByExercise = mutableMapOf<Long, ProgressionType>()
    // DB-PRE7B: canonical exercise identity per plan exercise (catalog:<slug> or custom:<name>), used for
    // cross-day history/PR lookup and persisted on save. Resolved from the catalog slug map (built once).
    private val canonicalKeyByExercise = mutableMapOf<Long, String>()
    private var catalogSlugMap: Map<String, String> = emptyMap()
    // MUSCLE-3: when this Log screen was opened — runtime-only proxy for workout duration at save.
    private val loggingStartedAtMillis = System.currentTimeMillis()
    // V3.6B: latest saved session's readiness per exercise (null = unknown), fed to the engine.
    private val readinessByExercise = mutableMapOf<Long, Readiness?>()
    // V3.7B: readiness per recent session id per exercise (null = unknown), fed to the readiness trend.
    private val readinessSeriesByExercise = mutableMapOf<Long, Map<Long, Readiness?>>()
    private var draftSaveJob: Job? = null
    private var draftDirty = false
    private var draftVersion = 0
    private var expandedExerciseId: Long? = null
    private var expansionInitialized = false
    // Session start time for the duration timer (persisted alongside the draft, never in Room).
    private var startedAtMillis: Long = 0L
    private var currentGoalSelection: WorkoutGoalSelection = preferencesRepository.workoutGoal.value
    private var currentAttempt: WorkoutAttemptIdentity? = null
    private var researchFreezeResolved = false
    private val pendingResearchObservationJobs = mutableListOf<Job>()
    private val committedSetKeys = mutableSetOf<String>()
    private var timerJob: Job? = null
    private var restTimer = RestTimerUiState()
    private var manualRestSeconds = 90
    private var lastSuggestedRestSeconds = 90
    private var pendingAdvanceFromExerciseId: Long? = null
    private var nextExercisePromptId: Long? = null
    private var scrollToExerciseId: Long? = null

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            // Ensure the exercise catalog is seeded so V3.5 can read progression metadata (idempotent).
            catalogRepository.ensureSeeded()
            // DB-PRE7B: build the name→slug map once so canonical-key resolution is synchronous below.
            catalogSlugMap = catalogRepository.canonicalSlugsByName()
            if (launch is LogWorkoutLaunch.SavedOneOff) {
                initializeSavedOneOff(launch.savedWorkoutId)
                return@launch
            }
            val dayId = (launch as LogWorkoutLaunch.Routine).dayId
            // Restore any unsaved draft for this day (survives navigation, background, process death).
            val savedDraft = draftRepository.loadDraft(dayId)
            // WEARABLE-1A: this identity is synchronously durable before any prediction can be frozen.
            val attempt = draftRepository.loadOrCreateAttempt(dayId)
            currentAttempt = attempt
            startedAtMillis = attempt.startedAtMillis
            combine(
                repository.observeWorkoutDay(dayId),
                repository.observeExercisesForDay(dayId),
                preferencesRepository.workoutGoal,
            ) { day, exercises, goalSelection -> Triple(day, exercises, goalSelection) }
                .collect { (day, exercises, goalSelection) ->
                currentDay = day
                currentExercises = exercises
                currentGoalSelection = goalSelection
                exercises.forEach { exercise ->
                    // DB-PRE7B: history/PR are now keyed on the canonical identity (shared across days),
                    // not the plan-row id. The catalog slug map is resolved synchronously here.
                    val canonicalKey = canonicalKeyByExercise.getOrPut(exercise.id) {
                        CanonicalExerciseKey.resolve(exercise.name, catalogSlugMap)
                    }
                    val recentSets = repository.recentSetsForExerciseKey(canonicalKey, exercise.name)
                    historyByExercise[exercise.id] = recentSets
                    if (!historyItemsByExercise.containsKey(exercise.id)) {
                        historyItemsByExercise[exercise.id] = repository
                            .observeExerciseHistoryByKey(canonicalKey, exercise.name)
                            .first()
                    }
                    if (!profilesByExercise.containsKey(exercise.id)) {
                        profilesByExercise[exercise.id] = catalogProfileFor(exercise.name)
                    }
                    if (!trackingTypeByExercise.containsKey(exercise.id)) {
                        val catalogExercise = catalogRepository.findByName(exercise.name)
                        trackingTypeByExercise[exercise.id] = catalogExercise?.trackingType ?: TrackingType.REPS_WEIGHT
                        progressionTypeByExercise[exercise.id] =
                            catalogExercise?.progressionType ?: ProgressionType.COMPOUND_LOAD
                    }
                    readinessByExercise[exercise.id] = recentSets.firstOrNull()?.sessionId
                        ?.let { repository.readinessForSession(it) }
                    // V3.7B: readiness across the recent sessions (read-only) for the engine's trend.
                    readinessSeriesByExercise[exercise.id] =
                        repository.readinessBySession(recentSets.map { it.sessionId }.distinct())
                    draftsByExercise.getOrPut(exercise.id) {
                        savedDraft[exercise.id]
                            ?.takeIf { it.isNotEmpty() }
                            ?.toSetDrafts()
                            ?.toMutableList()
                            ?: defaultDrafts(exercise, latestSessionSets(recentSets)).toMutableList()
                    }
                }
                if (!researchFreezeResolved && day != null) {
                    // A resumed draft with an already-valid row has crossed the prospective boundary.
                    // Do not reconstruct a pre-set prediction from hindsight.
                    val savedDraftAlreadyValid = exercises.any { exercise ->
                        val tracking = trackingTypeByExercise[exercise.id] ?: TrackingType.REPS_WEIGHT
                        savedDraft[exercise.id].orEmpty().any { draft ->
                            parseSetDraftInput(
                                draft = draft.toSetDraft(),
                                type = tracking,
                                distanceUnitSystem = preferencesRepository.distanceUnitSystem.value,
                                notes = null,
                            ) != null
                        }
                    }
                    if (!savedDraftAlreadyValid) {
                        val frozenPrediction = runCatching {
                            recoveryResearchRepository.freezePredictionIfEligible(
                                RecoveryResearchFreezeRequest(
                                    attempt = attempt,
                                    day = day,
                                    targetExercises = exercises,
                                    allPlanExercises = repository.getAllExercises(),
                                    goalSelection = goalSelection,
                                    inputDataCutoffAtMillis = System.currentTimeMillis(),
                                ),
                            )
                        }.getOrNull()
                        // WEARABLE-2A attaches an immutable, locally-known shadow snapshot only
                        // after the protected prediction is frozen. Failure is non-interventional.
                        frozenPrediction?.let { prediction ->
                            runCatching {
                                wearableShadowRepository.captureSnapshotForPrediction(prediction)
                            }
                        }
                    }
                    researchFreezeResolved = true
                }
                emitState()
            }
        }
    }

    private suspend fun initializeSavedOneOff(savedWorkoutId: Long) {
        val template = savedWorkoutRepository.get(savedWorkoutId)
        if (template == null) {
            _uiState.value = _uiState.value.copy(
                workoutName = "Workout unavailable",
                message = "This Saved Workout no longer exists.",
            )
            return
        }
        currentSavedWorkout = template
        currentDay = null
        startedAtMillis = loggingStartedAtMillis
        currentGoalSelection = preferencesRepository.workoutGoal.value
        // Presentation-only adapters let the existing Log UI and unchanged Engine display the
        // prescription. They are never inserted, updated, or used as Routine identity.
        currentExercises = template.exercises.map { saved ->
            ExerciseEntity(
                id = saved.id,
                dayId = 0L,
                name = saved.exerciseNameSnapshot,
                targetSets = saved.targetSets,
                targetMinReps = saved.targetMinReps,
                targetMaxReps = saved.targetMaxReps,
                sortOrder = saved.sortOrder,
            )
        }
        currentExercises.forEach { exercise ->
            val saved = template.exercises.first { it.id == exercise.id }
            val canonicalKey = saved.canonicalExerciseKey.ifBlank {
                CanonicalExerciseKey.resolve(exercise.name, catalogSlugMap)
            }
            canonicalKeyByExercise[exercise.id] = canonicalKey
            val recentSets = repository.recentSetsForExerciseKey(canonicalKey, exercise.name)
            historyByExercise[exercise.id] = recentSets
            historyItemsByExercise[exercise.id] = repository
                .observeExerciseHistoryByKey(canonicalKey, exercise.name)
                .first()
            profilesByExercise[exercise.id] = catalogProfileFor(exercise.name)
            val catalogExercise = catalogRepository.findByName(exercise.name)
            trackingTypeByExercise[exercise.id] = catalogExercise?.trackingType ?: TrackingType.REPS_WEIGHT
            progressionTypeByExercise[exercise.id] =
                catalogExercise?.progressionType ?: ProgressionType.COMPOUND_LOAD
            readinessByExercise[exercise.id] = recentSets.firstOrNull()?.sessionId
                ?.let { repository.readinessForSession(it) }
            readinessSeriesByExercise[exercise.id] =
                repository.readinessBySession(recentSets.map { it.sessionId }.distinct())
            draftsByExercise[exercise.id] =
                defaultDrafts(exercise, latestSessionSets(recentSets)).toMutableList()
        }
        emitState()
    }

    fun updateSet(
        exerciseId: Long,
        index: Int,
        weight: String? = null,
        reps: String? = null,
        rir: String? = null,
        notes: String? = null,
        duration: String? = null,
        distance: String? = null,
    ) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val current = drafts.getOrNull(index) ?: return
        val tracking = trackingTypeByExercise[exerciseId] ?: TrackingType.REPS_WEIGHT
        val wasValid = isSetComplete(current, tracking)
        drafts[index] = current.copy(
            weight = weight ?: current.weight,
            reps = reps ?: current.reps,
            rir = rir ?: current.rir,
            notes = notes ?: current.notes,
            duration = duration ?: current.duration,
            distance = distance ?: current.distance,
        )
        if (!isSetComplete(drafts[index], tracking)) {
            // Re-arm the one-shot completion boundary only after the user deliberately makes the
            // row incomplete. Recomposition can never reopen the timer, while a real correction can.
            committedSetKeys.remove(drafts[index].researchRowKey)
        }
        if (!wasValid && isSetComplete(drafts[index], tracking)) {
            val attemptId = currentAttempt?.attemptId
            val canonicalKey = canonicalKeyByExercise[exerciseId]
            if (attemptId != null && canonicalKey != null) {
                val job = viewModelScope.launch {
                    runCatching {
                        recoveryResearchRepository.recordFirstValidSet(
                            attemptId = attemptId,
                            planExerciseId = exerciseId,
                            canonicalExerciseKey = canonicalKey,
                            draftSetKey = drafts[index].researchRowKey,
                            draftSetOrdinal = index + 1,
                        )
                    }
                }
                pendingResearchObservationJobs += job
                job.invokeOnCompletion { pendingResearchObservationJobs.remove(job) }
            }
        }
        scheduleDraftPersist(markDraftDirty())
        emitState()
    }

    fun addSet(exerciseId: Long) {
        val drafts = draftsByExercise[exerciseId] ?: mutableListOf<SetDraft>().also {
            draftsByExercise[exerciseId] = it
        }
        drafts += SetDraft(weight = drafts.lastOrNull()?.weight.orEmpty())
        markDraftDirty()
        flushDraftNowAsync()
        emitState()
    }

    fun removeSet(exerciseId: Long, index: Int) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val target = drafts.getOrNull(index) ?: return
        val baseCount = drafts.count { it.continuationOfKey == null }
        if (target.continuationOfKey == null && baseCount <= 1) return
        drafts.removeAll { it.continuationOfKey == target.researchRowKey }
        drafts.remove(target)
        markDraftDirty()
        flushDraftNowAsync()
        emitState()
    }

    /** Explicit row completion boundary. Typing alone never opens the timer. */
    fun commitSet(exerciseId: Long, index: Int) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val draft = drafts.getOrNull(index) ?: return
        val tracking = trackingTypeByExercise[exerciseId] ?: TrackingType.REPS_WEIGHT
        if (!isSetComplete(draft, tracking) || !committedSetKeys.add(draft.researchRowKey)) return

        val exercise = currentExercises.firstOrNull { it.id == exerciseId } ?: return
        val hasNext = currentExercises.indexOfFirst { it.id == exerciseId }
            .let { it >= 0 && it < currentExercises.lastIndex }
        val baseOrdinal = drafts.take(index + 1).count { it.continuationOfKey == null }
        val reachedLastProgrammedSet = draft.continuationOfKey == null &&
            baseOrdinal >= exercise.targetSets
        if (reachedLastProgrammedSet && hasNext) {
            nextExercisePromptId = exerciseId
            emitState()
            return
        }
        if (reachedLastProgrammedSet && !hasNext) return
        openRestTimer(exerciseId, index, advanceAfterRest = false)
    }

    fun updateSetType(exerciseId: Long, index: Int, type: LogSetType) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val draft = drafts.getOrNull(index) ?: return
        if (draft.continuationOfKey != null) return
        drafts[index] = draft.copy(setType = type)
        emitState()
    }

    fun updateWeightDistribution(exerciseId: Long, index: Int, distribution: LogWeightDistribution) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val draft = drafts.getOrNull(index) ?: return
        if (draft.continuationOfKey != null) return
        drafts[index] = draft.copy(weightDistribution = distribution)
        emitState()
    }

    fun continueSet(exerciseId: Long, index: Int) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val base = drafts.getOrNull(index) ?: return
        if (base.continuationOfKey != null || drafts.any { it.continuationOfKey == base.researchRowKey }) return
        drafts.add(
            index + 1,
            SetDraft(
                weight = base.weight,
                rir = base.rir,
                continuationOfKey = base.researchRowKey,
            ),
        )
        markDraftDirty()
        flushDraftNowAsync()
        emitState()
    }

    fun continueLastSet(exerciseId: Long) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val tracking = trackingTypeByExercise[exerciseId] ?: TrackingType.REPS_WEIGHT
        val index = drafts.indices.reversed().firstOrNull { i ->
            val candidate = drafts[i]
            candidate.continuationOfKey == null && isSetComplete(candidate, tracking) &&
                drafts.none { it.continuationOfKey == candidate.researchRowKey }
        } ?: return
        continueSet(exerciseId, index)
    }

    fun startRestBeforeNext(exerciseId: Long) {
        if (nextExercisePromptId != exerciseId) return
        val drafts = draftsByExercise[exerciseId] ?: return
        val index = drafts.indexOfLast { isSetComplete(it, trackingTypeByExercise[exerciseId] ?: TrackingType.REPS_WEIGHT) }
        if (index < 0) return
        pendingAdvanceFromExerciseId = exerciseId
        openRestTimer(exerciseId, index, advanceAfterRest = true)
    }

    fun dismissRestTimer() {
        restTimer = restTimer.copy(visible = false)
        emitState()
    }

    fun setRestTimerUnit(unit: RestTimerUnit) {
        restTimer = restTimer.copy(
            unit = unit,
            inputValue = if (unit == RestTimerUnit.SEC) {
                restTimer.totalSeconds.toString()
            } else {
                ((restTimer.totalSeconds + 59) / 60).coerceAtLeast(1).toString()
            },
        )
        emitState()
    }

    fun setRestTimerInput(raw: String) {
        val value = raw.filter(Char::isDigit).take(4)
        val amount = value.toIntOrNull() ?: 0
        val seconds = if (restTimer.unit == RestTimerUnit.SEC) amount else amount * 60
        val safeSeconds = if (amount == 0) 0 else seconds.coerceIn(5, 3600)
        manualRestSeconds = safeSeconds.coerceAtLeast(5)
        restTimer = restTimer.copy(
            smartEnabled = false,
            inputValue = value,
            totalSeconds = safeSeconds,
            remainingSeconds = safeSeconds,
            running = false,
        )
        timerJob?.cancel()
        emitState()
    }

    fun setSmartRestEnabled(enabled: Boolean) {
        val seconds = if (enabled) lastSuggestedRestSeconds else manualRestSeconds
        restTimer = restTimer.copy(
            smartEnabled = enabled,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            running = false,
            inputValue = timerInput(seconds, restTimer.unit),
        )
        timerJob?.cancel()
        emitState()
    }

    fun resetRestTimer() {
        timerJob?.cancel()
        restTimer = restTimer.copy(remainingSeconds = restTimer.totalSeconds, running = false)
        emitState()
    }

    fun toggleRestTimerRunning() {
        if (restTimer.running) {
            timerJob?.cancel()
            restTimer = restTimer.copy(running = false)
            emitState()
            return
        }
        if (restTimer.remainingSeconds <= 0) {
            restTimer = restTimer.copy(remainingSeconds = restTimer.totalSeconds.coerceAtLeast(5))
        }
        restTimer = restTimer.copy(running = true)
        emitState()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (restTimer.running && restTimer.remainingSeconds > 0) {
                delay(1000)
                if (!restTimer.running) break
                val remaining = (restTimer.remainingSeconds - 1).coerceAtLeast(0)
                restTimer = restTimer.copy(remainingSeconds = remaining)
                if (remaining == 0) {
                    val shouldAdvance = restTimer.advanceAfterRest
                    restTimer = restTimer.copy(running = false, visible = false)
                    if (shouldAdvance) advanceAfterRest()
                }
                emitState()
            }
        }
    }

    fun finishRestTimer() {
        timerJob?.cancel()
        val shouldAdvance = restTimer.advanceAfterRest
        restTimer = restTimer.copy(remainingSeconds = 0, running = false, visible = false)
        if (shouldAdvance) advanceAfterRest()
        emitState()
    }

    fun consumeScrollTarget() {
        scrollToExerciseId = null
        emitState()
    }

    private fun openRestTimer(exerciseId: Long, index: Int, advanceAfterRest: Boolean) {
        timerJob?.cancel()
        val exercise = currentExercises.firstOrNull { it.id == exerciseId } ?: return
        val drafts = draftsByExercise[exerciseId].orEmpty()
        val draft = drafts.getOrNull(index) ?: return
        val tracking = trackingTypeByExercise[exerciseId] ?: TrackingType.REPS_WEIGHT
        val previousComparable = drafts.take(index).asReversed().firstOrNull { candidate ->
            candidate.continuationOfKey == null && isSetComplete(candidate, tracking)
        }
        val workingSetOrdinal = drafts.take(index + 1).count { it.continuationOfKey == null }
        val suggestion = RestSuggestionHeuristic.seconds(
            RestSuggestionInput(
                progressionType = progressionTypeByExercise[exerciseId] ?: ProgressionType.COMPOUND_LOAD,
                rir = draft.rir.toIntOrNull(),
                reps = draft.reps.toIntOrNull(),
                previousComparableReps = previousComparable?.reps?.toIntOrNull(),
                workingSetOrdinal = workingSetOrdinal,
                programmedSetCount = exercise.targetSets,
            ),
        )
        lastSuggestedRestSeconds = suggestion
        val seconds = if (restTimer.smartEnabled) suggestion else manualRestSeconds
        restTimer = restTimer.copy(
            visible = true,
            totalSeconds = seconds,
            remainingSeconds = seconds,
            running = false,
            inputValue = timerInput(seconds, restTimer.unit),
            advanceAfterRest = advanceAfterRest,
        )
        emitState()
    }

    private fun timerInput(seconds: Int, unit: RestTimerUnit): String =
        if (unit == RestTimerUnit.SEC) seconds.toString()
        else ((seconds + 59) / 60).coerceAtLeast(1).toString()

    private fun advanceAfterRest() {
        val fromId = pendingAdvanceFromExerciseId ?: expandedExerciseId ?: return
        val ids = currentExercises.map { it.id }
        val currentIndex = ids.indexOf(fromId)
        if (currentIndex < 0 || currentIndex >= ids.lastIndex) return
        expansionInitialized = true
        expandedExerciseId = ids[currentIndex + 1]
        nextExercisePromptId = null
        pendingAdvanceFromExerciseId = null
        scrollToExerciseId = expandedExerciseId
    }

    /** Expand the tapped exercise (collapsing any other), or collapse it if it is already expanded. */
    fun toggleExpanded(exerciseId: Long) {
        expansionInitialized = true
        expandedExerciseId = if (expandedExerciseId == exerciseId) null else exerciseId
        emitState()
    }

    /** Collapse the current exercise and expand the next one in order (used by "Next exercise"). */
    fun expandNext() {
        val ids = currentExercises.map { it.id }
        val currentIndex = ids.indexOf(expandedExerciseId)
        if (currentIndex >= 0 && currentIndex + 1 < ids.size) {
            expansionInitialized = true
            expandedExerciseId = ids[currentIndex + 1]
            emitState()
        }
    }

    /** Abandon the in-progress workout: clear the saved draft and reset the form to defaults. */
    fun discardDraft() {
        viewModelScope.launch {
            timerJob?.cancel()
            restTimer = RestTimerUiState()
            committedSetKeys.clear()
            nextExercisePromptId = null
            pendingAdvanceFromExerciseId = null
            cancelPendingDraftSave()
            currentAttempt?.let { runCatching { recoveryResearchRepository.abandonAttempt(it.attemptId) } }
            clearPersistedDraft()
            draftsByExercise.clear()
            currentExercises.forEach { exercise ->
                val previous = latestSessionSets(historyByExercise[exercise.id].orEmpty())
                draftsByExercise[exercise.id] = defaultDrafts(exercise, previous).toMutableList()
            }
            emitState()
            _events.emit(LogWorkoutEvent.Discarded)
        }
    }

    /** [readiness] is the subjective how-did-today-feel choice from the review sheet (default NORMAL). */
    fun saveWorkout(readiness: Readiness = Readiness.NORMAL) {
        if (_uiState.value.isSaving) return
        val day = currentDay
        val savedWorkout = currentSavedWorkout
        val routineInputs = if (day != null) buildInputs() else emptyList()
        val savedInputs = if (savedWorkout != null) buildSavedInputs() else emptyList()
        if (routineInputs.isEmpty() && savedInputs.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Enter at least one complete set.")
            flushDraftNowAsync()
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            // MUSCLE-3B.1: persist the wall-clock workout duration so the review shows the same value
            // when this session is reopened later from History/Home.
            val durationSeconds = ((System.currentTimeMillis() - loggingStartedAtMillis) / 1000L)
                .toInt()
                .coerceAtLeast(0)
            val sessionId = if (savedWorkout != null) {
                // Accepted PLAN-V3A boundary: real History/Muscle Recovery/Active Streak session,
                // but no Recovery Research observation and no Routine lifecycle side effects.
                repository.saveSavedOneOffWorkoutSession(
                    workout = savedWorkout.workout,
                    inputs = savedInputs,
                    readiness = readiness,
                    durationSeconds = durationSeconds,
                )
            } else {
                val routineDay = day ?: return@launch
                flushDraftNow()
                pendingResearchObservationJobs.toList().forEach { it.join() }
                val attemptId = currentAttempt?.attemptId ?: return@launch
                val id = recoveryResearchRepository.saveWorkoutAndCaptureOutcome(
                    attemptId = attemptId,
                    day = routineDay,
                    inputs = routineInputs,
                    readiness = readiness,
                    durationSeconds = durationSeconds,
                )
                cancelPendingDraftSave()
                clearPersistedDraft()
                advanceToNextDay(routineDay.id)
                id
            }
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                message = "Workout saved.",
            )
            _events.emit(
                LogWorkoutEvent.Saved(
                    sessionId = sessionId,
                    durationMillis = System.currentTimeMillis() - loggingStartedAtMillis,
                ),
            )
        }
    }

    /** After completing [savedDayId], select the next day in plan order and mark it auto-advanced. */
    private suspend fun advanceToNextDay(savedDayId: Long) {
        val days = repository.workoutDaysOrdered()
        if (days.size < 2) return
        val index = days.indexOfFirst { it.id == savedDayId }
        if (index < 0) return
        val nextDay = days[(index + 1) % days.size]
        preferencesRepository.setSelectedDayId(nextDay.id, DaySelectionOrigin.ADVANCED)
    }

    override fun onCleared() {
        timerJob?.cancel()
        if (launch is LogWorkoutLaunch.Routine && draftDirty) {
            runBlocking {
                flushDraftNow()
            }
        } else {
            draftSaveJob?.cancel()
            draftSaveJob = null
        }
        super.onCleared()
    }

    private fun markDraftDirty(): Int {
        draftDirty = true
        draftVersion += 1
        return draftVersion
    }

    private fun scheduleDraftPersist(version: Int) {
        if (launch !is LogWorkoutLaunch.Routine) return
        val previousJob = draftSaveJob
        previousJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            previousJob?.join()
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            persistDraftNow(version)
        }
    }

    private fun flushDraftNowAsync() {
        if (launch !is LogWorkoutLaunch.Routine) return
        val previousJob = draftSaveJob
        val version = draftVersion
        previousJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            previousJob?.join()
            if (draftDirty) {
                persistDraftNow(version)
            }
        }
    }

    private suspend fun flushDraftNow() {
        if (launch !is LogWorkoutLaunch.Routine) return
        cancelPendingDraftSave()
        if (draftDirty) {
            persistDraftNow(draftVersion)
        }
    }

    private suspend fun cancelPendingDraftSave() {
        val pendingJob = draftSaveJob
        draftSaveJob = null
        pendingJob?.cancelAndJoin()
    }

    private suspend fun persistDraftNow(version: Int) {
        val dayId = (launch as? LogWorkoutLaunch.Routine)?.dayId ?: return
        val entries = draftEntriesSnapshot()
        withContext(Dispatchers.IO) {
            draftRepository.saveDraft(dayId, entries)
        }
        if (version == draftVersion) {
            draftDirty = false
        }
    }

    private suspend fun clearPersistedDraft() {
        val dayId = (launch as? LogWorkoutLaunch.Routine)?.dayId ?: return
        draftVersion += 1
        draftDirty = false
        withContext(Dispatchers.IO) {
            draftRepository.clearDraft(dayId)
        }
    }

    private fun draftEntriesSnapshot(): Map<Long, List<DraftSet>> {
        return currentExercises.associate { exercise ->
            // LOG-V4A advanced metadata is deliberately transient. A continuation row is therefore
            // not serialized as a misleading ordinary base set when the process is restored.
            exercise.id to draftsByExercise[exercise.id].orEmpty()
                .filter { it.continuationOfKey == null }
                .map { draft ->
                DraftSet(
                    weight = draft.weight,
                    reps = draft.reps,
                    rir = draft.rir,
                    notes = draft.notes,
                    duration = draft.duration,
                    distance = draft.distance,
                    researchRowKey = draft.researchRowKey,
                )
                }
        }.filterValues { it.isNotEmpty() }
    }

    private fun List<DraftSet>.toSetDrafts(): List<SetDraft> = map { draft ->
        SetDraft(
            weight = draft.weight,
            reps = draft.reps,
            rir = draft.rir,
            notes = draft.notes,
            duration = draft.duration,
            distance = draft.distance,
            researchRowKey = draft.researchRowKey.ifBlank { UUID.randomUUID().toString() },
        )
    }

    private fun DraftSet.toSetDraft(): SetDraft = SetDraft(
        weight = weight,
        reps = reps,
        rir = rir,
        notes = notes,
        duration = duration,
        distance = distance,
        researchRowKey = researchRowKey.ifBlank { UUID.randomUUID().toString() },
    )

    private fun emitState() {
        // V3.7C-H: whole-workout context computed once from in-memory history (no new DAO query). Counts
        // how many of the day's exercises are currently down vs their own baselines → systemic vs local.
        val siblingDownFlags = currentExercises.map { ex ->
            ProgressiveOverloadEngine.latestSessionDownVsBaseline(ex, historyByExercise[ex.id].orEmpty())
        }
        val workoutContext = currentGoalSelection.toWorkoutContext(
            siblingsEvaluated = siblingDownFlags.count { it != null },
            siblingsDown = siblingDownFlags.count { it == true },
        )
        val exerciseStates = currentExercises.map { exercise ->
            val history = historyByExercise[exercise.id].orEmpty()
            val previousSets = latestSessionSets(history)
            val drafts = draftsByExercise.getOrPut(exercise.id) {
                defaultDrafts(exercise, previousSets).toMutableList()
            }
            val trackingType = trackingTypeByExercise[exercise.id] ?: TrackingType.REPS_WEIGHT
            val parsedSets = drafts.mapNotNull {
                parseSetDraftInput(
                    draft = it,
                    type = trackingType,
                    distanceUnitSystem = preferencesRepository.distanceUnitSystem.value,
                    notes = null,
                )
            }
            val currentReps = parsedSets.sumOf { it.reps }
            val currentVolume = parsedSets
                .filter { it.durationSeconds == null && it.distanceMeters == null }
                .sumOf { it.weight * it.reps }
            val currentDurationSeconds = parsedSets.sumOf { it.durationSeconds ?: 0 }
            val currentDistanceMeters = parsedSets.sumOf { it.distanceMeters ?: 0.0 }
            val previousVolume = previousSets.sumOf { it.weight * it.reps }
            val trendLabel = if (trackingType.isRepBased) {
                trendLabel(currentVolume, previousVolume, previousSets.isEmpty())
            } else {
                nonRepTrendLabel(trackingType, previousSets.isEmpty())
            }
            val bestWeight = history.maxOfOrNull { it.weight } ?: -1.0
            val bestReps = history.maxOfOrNull { it.reps } ?: -1
            val potentialPr = trackingType.isRepBased && drafts.any { draft ->
                val weight = draft.weight.toDoubleOrNull() ?: -1.0
                val reps = draft.reps.toIntOrNull() ?: -1
                weight > bestWeight || reps > bestReps
            }
            val completedSetCount = parsedSets.size
            val completedBaseSetCount = drafts.count { draft ->
                draft.continuationOfKey == null && isSetComplete(draft, trackingType)
            }
            val progressionType = progressionTypeByExercise[exercise.id] ?: ProgressionType.COMPOUND_LOAD
            ExerciseLogState(
                exercise = exercise,
                previousSets = previousSets,
                history = historyItemsByExercise[exercise.id].orEmpty(),
                draftSets = drafts.toList(),
                trendLabel = trendLabel,
                potentialPr = potentialPr,
                trackingType = trackingType,
                progressionType = progressionType,
                // Pass the full recent history (newest first) plus the catalog profile (V3.5), the latest
                // session's readiness (V3.6B), and the readiness series (V3.7B). Readiness (latest + trend)
                // only modifies confidence/wording — never the action or suggested weight.
                suggestion = ProgressiveOverloadEngine.suggestion(
                    exercise,
                    history,
                    profilesByExercise[exercise.id],
                    readinessByExercise[exercise.id],
                    readinessSeriesByExercise[exercise.id],
                    workoutContext,
                ),
                completedSetCount = completedSetCount,
                completedBaseSetCount = completedBaseSetCount,
                currentReps = currentReps,
                currentVolume = currentVolume,
                currentDurationSeconds = currentDurationSeconds,
                currentDistanceMeters = currentDistanceMeters,
                isComplete = completedBaseSetCount >= exercise.targetSets,
                repeatedDropSetCaution = progressionType == ProgressionType.COMPOUND_LOAD &&
                    drafts.count { it.setType == LogSetType.DROP } >= 3,
                canContinueLastSet = drafts.any { candidate ->
                    candidate.continuationOfKey == null && isSetComplete(candidate, trackingType) &&
                        drafts.none { it.continuationOfKey == candidate.researchRowKey }
                },
            )
        }
        // Default expansion (computed once): first exercise with an incomplete draft, else the first.
        if (!expansionInitialized && exerciseStates.isNotEmpty()) {
            expandedExerciseId = exerciseStates.firstOrNull { !it.isComplete }?.exercise?.id
                ?: exerciseStates.first().exercise.id
            expansionInitialized = true
        }
        _uiState.value = _uiState.value.copy(
            day = currentDay,
            workoutName = currentSavedWorkout?.workout?.name ?: currentDay?.name ?: "Loading",
            isSavedOneOff = currentSavedWorkout != null,
            exercises = exerciseStates,
            expandedExerciseId = expandedExerciseId,
            canSave = if (currentSavedWorkout != null) buildSavedInputs().isNotEmpty() else buildInputs().isNotEmpty(),
            startedAtMillis = startedAtMillis,
            restTimer = restTimer,
            nextExercisePromptId = nextExercisePromptId,
            scrollToExerciseId = scrollToExerciseId,
        )
    }

    private fun isSetComplete(draft: SetDraft, type: TrackingType): Boolean =
        parseSetDraftInput(draft, type, preferencesRepository.distanceUnitSystem.value, notes = null) != null

    /**
     * DB-6C single source of truth for validating + converting a draft set into a [SetInput] per its
     * [TrackingType]. Rep types are byte-for-byte the same as before (no regression); time/distance
     * types store their fields with reps/rir = 0 (the hidden sentinel). Returns null when incomplete.
     */
    private fun parsedSetInput(draft: SetDraft, type: TrackingType, notes: String?): SetInput? {
        return parseSetDraftInput(draft, type, preferencesRepository.distanceUnitSystem.value, notes)
    }

    private fun buildInputs(): List<ExerciseLogInput> {
        return currentExercises.mapNotNull { exercise ->
            val type = trackingTypeByExercise[exercise.id] ?: TrackingType.REPS_WEIGHT
            val drafts = draftsByExercise[exercise.id].orEmpty()
            val parsed = drafts.mapIndexedNotNull { index, draft ->
                parsedSetInput(draft, type, notes = null)?.copy(
                    sourceDraftOrdinal = index + 1,
                    sourceDraftKey = draft.researchRowKey,
                )
            }
            if (parsed.isEmpty()) return@mapNotNull null
            // Single exercise-level note (stored on the first set only; all others save with no note).
            val exerciseNote = drafts.firstOrNull()?.notes?.takeIf { it.isNotBlank() }
            val sets = if (exerciseNote == null) parsed
            else listOf(parsed.first().copy(notes = exerciseNote)) + parsed.drop(1)
            // DB-PRE7B: persist the canonical identity with every saved set.
            val canonicalKey = canonicalKeyByExercise[exercise.id]
                ?: CanonicalExerciseKey.resolve(exercise.name, catalogSlugMap)
            ExerciseLogInput(exercise, sets, canonicalKey)
        }
    }

    private fun buildSavedInputs(): List<SavedWorkoutExerciseLogInput> {
        val savedById = currentSavedWorkout?.exercises?.associateBy { it.id }.orEmpty()
        return currentExercises.mapNotNull { exercise ->
            val saved = savedById[exercise.id] ?: return@mapNotNull null
            val type = trackingTypeByExercise[exercise.id] ?: TrackingType.REPS_WEIGHT
            val drafts = draftsByExercise[exercise.id].orEmpty()
            val parsed = drafts.mapIndexedNotNull { index, draft ->
                parsedSetInput(draft, type, notes = null)?.copy(
                    sourceDraftOrdinal = index + 1,
                    sourceDraftKey = draft.researchRowKey,
                )
            }
            if (parsed.isEmpty()) return@mapNotNull null
            val note = drafts.firstOrNull()?.notes?.takeIf(String::isNotBlank)
            val sets = if (note == null) parsed else listOf(parsed.first().copy(notes = note)) + parsed.drop(1)
            SavedWorkoutExerciseLogInput(saved, sets)
        }
    }

    private fun defaultDrafts(
        exercise: ExerciseEntity,
        previousSets: List<LoggedSetEntity>,
    ): List<SetDraft> {
        return List(exercise.targetSets) { index ->
            val previous = previousSets.getOrNull(index)
            SetDraft(weight = previous?.weight?.let(ProgressiveOverloadEngine::formatWeight).orEmpty())
        }
    }

    /** Catalog-derived progression profile for [name]; null for custom/unmatched exercises. */
    private suspend fun catalogProfileFor(name: String): ProgressionProfile? =
        catalogRepository.findByName(name)?.let { c ->
            ProgressiveOverloadEngine.profileFromCatalog(
                progressionTypeName = c.progressionType.name,
                incrementKg = c.defaultIncrementKg,
                sessionsRequiredBeforeIncrease = c.sessionsRequiredBeforeIncrease,
                isBodyweight = c.isBodyweight,
                isUnilateral = c.isUnilateral,
                trackingTypeName = c.trackingType.name,
            )
        }

    private fun latestSessionSets(sets: List<LoggedSetEntity>): List<LoggedSetEntity> {
        val latestSessionId = sets.firstOrNull()?.sessionId ?: return emptyList()
        return sets.filter { it.sessionId == latestSessionId }
    }

    private fun trendLabel(currentVolume: Double, previousVolume: Double, noPrevious: Boolean): String {
        return when {
            noPrevious -> "First session"
            currentVolume == 0.0 -> "Previous ${ProgressiveOverloadEngine.formatWeight(previousVolume)}kg volume"
            currentVolume > previousVolume -> "+${ProgressiveOverloadEngine.formatWeight(currentVolume - previousVolume)}kg volume"
            currentVolume < previousVolume -> "-${ProgressiveOverloadEngine.formatWeight(previousVolume - currentVolume)}kg volume"
            else -> "Volume matched"
        }
    }

    private fun nonRepTrendLabel(type: TrackingType, noPrevious: Boolean): String {
        if (noPrevious) return "First session"
        return when {
            type.showsDuration && type.showsDistance -> "Time + distance"
            type.showsDuration -> "Duration tracking"
            type.showsDistance -> "Distance tracking"
            else -> "Tracking"
        }
    }

    private companion object {
        const val DRAFT_SAVE_DEBOUNCE_MS = 500L
    }
}
