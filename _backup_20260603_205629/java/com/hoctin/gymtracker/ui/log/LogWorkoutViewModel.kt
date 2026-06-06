package com.hoctin.gymtracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseLogInput
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.SetInput
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.domain.OverloadSuggestion
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
)

data class LogWorkoutUiState(
    val day: WorkoutDayEntity? = null,
    val exercises: List<ExerciseLogState> = emptyList(),
    val isSaving: Boolean = false,
    val canSave: Boolean = false,
    val message: String? = null,
)

sealed interface LogWorkoutEvent {
    data class Saved(val sessionId: Long) : LogWorkoutEvent
}

class LogWorkoutViewModel(
    private val dayId: Long,
    private val repository: GymRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogWorkoutUiState())
    val uiState: StateFlow<LogWorkoutUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LogWorkoutEvent>()
    val events: SharedFlow<LogWorkoutEvent> = _events.asSharedFlow()

    private var currentDay: WorkoutDayEntity? = null
    private var currentExercises: List<ExerciseEntity> = emptyList()
    private val draftsByExercise = mutableMapOf<Long, MutableList<SetDraft>>()
    private val historyByExercise = mutableMapOf<Long, List<LoggedSetEntity>>()

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
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
                        defaultDrafts(exercise, latestSessionSets(recentSets)).toMutableList()
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
        emitState()
    }

    fun addSet(exerciseId: Long) {
        val drafts = draftsByExercise[exerciseId] ?: mutableListOf<SetDraft>().also {
            draftsByExercise[exerciseId] = it
        }
        drafts += SetDraft(weight = drafts.lastOrNull()?.weight.orEmpty())
        emitState()
    }

    fun removeSet(exerciseId: Long, index: Int) {
        val drafts = draftsByExercise[exerciseId] ?: return
        if (drafts.size <= 1) return
        drafts.removeAt(index)
        emitState()
    }

    fun saveWorkout() {
        val day = currentDay ?: return
        val inputs = buildInputs()
        if (inputs.isEmpty()) {
            _uiState.value = _uiState.value.copy(message = "Enter at least one complete set.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            val sessionId = repository.saveWorkoutSession(day, inputs)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                message = "Workout saved.",
            )
            _events.emit(LogWorkoutEvent.Saved(sessionId))
        }
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
            ExerciseLogState(
                exercise = exercise,
                previousSets = previousSets,
                draftSets = drafts.toList(),
                trendLabel = trendLabel,
                potentialPr = potentialPr,
                suggestion = ProgressiveOverloadEngine.suggestion(exercise, previousSets),
            )
        }
        _uiState.value = _uiState.value.copy(
            day = currentDay,
            exercises = exerciseStates,
            canSave = buildInputs().isNotEmpty(),
        )
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
}
