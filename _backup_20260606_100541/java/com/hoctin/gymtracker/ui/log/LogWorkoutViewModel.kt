package com.hoctin.gymtracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.DaySelectionOrigin
import com.hoctin.gymtracker.data.DraftSet
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseLogInput
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.SetInput
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.domain.OverloadSuggestion
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class SetDraft(
    val weight: String = "",
    val reps: String = "",
    val rir: String = "2",
    val notes: String = "",
)

data class ExerciseLogState(
    val exercise: ExerciseEntity,
    val previousSets: List<LoggedSetEntity>,
    val draftSets: List<SetDraft>,
    val trendLabel: String,
    val potentialPr: Boolean,
    val suggestion: OverloadSuggestion,
    val completedSetCount: Int = 0,
    val currentVolume: Double = 0.0,
    val isComplete: Boolean = false,
)

data class LogWorkoutUiState(
    val day: WorkoutDayEntity? = null,
    val exercises: List<ExerciseLogState> = emptyList(),
    // Active-exercise accordion: only this exercise renders full input fields. Null = all collapsed.
    val expandedExerciseId: Long? = null,
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val message: String? = null,
)

sealed interface LogWorkoutEvent {
    data class Saved(val sessionId: Long) : LogWorkoutEvent
    object Discarded : LogWorkoutEvent
}

class LogWorkoutViewModel(
    private val dayId: Long,
    private val repository: GymRepository,
    private val draftRepository: WorkoutDraftRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogWorkoutUiState())
    val uiState: StateFlow<LogWorkoutUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogWorkoutEvent>()
    val events: SharedFlow<LogWorkoutEvent> = _events.asSharedFlow()

    private var currentDay: WorkoutDayEntity? = null
    private var currentExercises: List<ExerciseEntity> = emptyList()
    private val draftsByExercise = mutableMapOf<Long, MutableList<SetDraft>>()
    private val historyByExercise = mutableMapOf<Long, List<LoggedSetEntity>>()
    private var draftSaveJob: Job? = null
    private var draftDirty = false
    private var draftVersion = 0
    private var expandedExerciseId: Long? = null
    private var expansionInitialized = false

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            // Restore any unsaved draft for this day (survives navigation, background, process death).
            val savedDraft = draftRepository.loadDraft(dayId)
            combine(
                repository.observeWorkoutDay(dayId),
                repository.observeExercisesForDay(dayId),
            ) { day, exercises -> day to exercises }.collect { (day, exercises) ->
                currentDay = day
                currentExercises = exercises
                exercises.forEach { exercise ->
                    val recentSets = repository.recentSetsForExercise(exercise.id)
                    historyByExercise[exercise.id] = recentSets
                    draftsByExercise.getOrPut(exercise.id) {
                        savedDraft[exercise.id]
                            ?.takeIf { it.isNotEmpty() }
                            ?.toSetDrafts()
                            ?.toMutableList()
                            ?: defaultDrafts(exercise, latestSessionSets(recentSets)).toMutableList()
                    }
                }
                emitState()
            }
        }
    }

    fun updateSet(
        exerciseId: Long,
        index: Int,
        weight: String? = null,
        reps: String? = null,
        rir: String? = null,
        notes: String? = null,
    ) {
        val drafts = draftsByExercise[exerciseId] ?: return
        val current = drafts.getOrNull(index) ?: return
        drafts[index] = current.copy(
            weight = weight ?: current.weight,
            reps = reps ?: current.reps,
            rir = rir ?: current.rir,
            notes = notes ?: current.notes,
        )
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
        if (drafts.size <= 1) return
        drafts.removeAt(index)
        markDraftDirty()
        flushDraftNowAsync()
        emitState()
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
            cancelPendingDraftSave()
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

    fun saveWorkout() {
        val day = currentDay ?: return
        val inputs = buildInputs()
        if (inputs.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Enter at least one complete set.")
            flushDraftNowAsync()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            flushDraftNow()
            val sessionId = repository.saveWorkoutSession(day, inputs)
            // Workout is now in History, so the draft is no longer needed.
            cancelPendingDraftSave()
            clearPersistedDraft()
            // Guide the user to their next workout day.
            advanceToNextDay(day.id)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                message = "Workout saved.",
            )
            _events.emit(LogWorkoutEvent.Saved(sessionId))
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
        if (draftDirty) {
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
        val previousJob = draftSaveJob
        previousJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            previousJob?.join()
            delay(DRAFT_SAVE_DEBOUNCE_MS)
            persistDraftNow(version)
        }
    }

    private fun flushDraftNowAsync() {
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
        val entries = draftEntriesSnapshot()
        withContext(Dispatchers.IO) {
            draftRepository.saveDraft(dayId, entries)
        }
        if (version == draftVersion) {
            draftDirty = false
        }
    }

    private suspend fun clearPersistedDraft() {
        draftVersion += 1
        draftDirty = false
        withContext(Dispatchers.IO) {
            draftRepository.clearDraft(dayId)
        }
    }

    private fun draftEntriesSnapshot(): Map<Long, List<DraftSet>> {
        return currentExercises.associate { exercise ->
            exercise.id to draftsByExercise[exercise.id].orEmpty().map { draft ->
                DraftSet(
                    weight = draft.weight,
                    reps = draft.reps,
                    rir = draft.rir,
                    notes = draft.notes,
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
        )
    }

    private fun emitState() {
        val exerciseStates = currentExercises.map { exercise ->
            val history = historyByExercise[exercise.id].orEmpty()
            val previousSets = latestSessionSets(history)
            val drafts = draftsByExercise.getOrPut(exercise.id) {
                defaultDrafts(exercise, previousSets).toMutableList()
            }
            val currentVolume = drafts.sumOf { draft ->
                val weight = draft.weight.toDoubleOrNull() ?: 0.0
                val reps = draft.reps.toIntOrNull() ?: 0
                weight * reps
            }
            val previousVolume = previousSets.sumOf { it.weight * it.reps }
            val trendLabel = trendLabel(currentVolume, previousVolume, previousSets.isEmpty())
            val bestWeight = history.maxOfOrNull { it.weight } ?: -1.0
            val bestReps = history.maxOfOrNull { it.reps } ?: -1
            val potentialPr = drafts.any { draft ->
                val weight = draft.weight.toDoubleOrNull() ?: -1.0
                val reps = draft.reps.toIntOrNull() ?: -1
                weight > bestWeight || reps > bestReps
            }
            val completedSetCount = drafts.count { isSetComplete(it) }
            ExerciseLogState(
                exercise = exercise,
                previousSets = previousSets,
                draftSets = drafts.toList(),
                trendLabel = trendLabel,
                potentialPr = potentialPr,
                suggestion = ProgressiveOverloadEngine.suggestion(exercise, previousSets),
                completedSetCount = completedSetCount,
                currentVolume = currentVolume,
                isComplete = completedSetCount >= exercise.targetSets,
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
            exercises = exerciseStates,
            expandedExerciseId = expandedExerciseId,
            canSave = buildInputs().isNotEmpty(),
        )
    }

    private fun isSetComplete(draft: SetDraft): Boolean {
        val weight = draft.weight.toDoubleOrNull()
        val reps = draft.reps.toIntOrNull()
        val rir = draft.rir.toIntOrNull()
        return weight != null && reps != null && rir != null && weight >= 0.0 && reps > 0
    }

    private fun buildInputs(): List<ExerciseLogInput> {
        return currentExercises.mapNotNull { exercise ->
            val sets = draftsByExercise[exercise.id].orEmpty().mapNotNull { draft ->
                val weight = draft.weight.toDoubleOrNull()
                val reps = draft.reps.toIntOrNull()
                val rir = draft.rir.toIntOrNull()
                if (weight == null || reps == null || rir == null || weight < 0.0 || reps <= 0) {
                    null
                } else {
                    SetInput(
                        weight = weight,
                        reps = reps,
                        rir = rir.coerceIn(0, 5),
                        notes = draft.notes.takeIf { it.isNotBlank() },
                    )
                }
            }
            if (sets.isEmpty()) null else ExerciseLogInput(exercise, sets)
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

    private companion object {
        const val DRAFT_SAVE_DEBOUNCE_MS = 500L
    }
}
