package com.hoctin.gymtracker.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseHistoryItem
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.domain.OverloadSuggestion
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val history: List<ExerciseHistoryItem> = emptyList(),
    val suggestion: OverloadSuggestion = OverloadSuggestion("No data", "Log this exercise to get a progression target."),
)

class ExerciseDetailViewModel(
    private val exerciseId: Long,
    private val repository: GymRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeExercise(exerciseId),
                repository.observeExerciseHistory(exerciseId),
            ) { exercise, history -> exercise to history }.collect { (exercise, history) ->
                val latestSessionId = history.firstOrNull()?.sessionId
                val latestSets = history.filter { it.sessionId == latestSessionId }
                    .map { it.toLoggedSet(exercise?.id) }
                val suggestion = if (exercise == null) {
                    OverloadSuggestion(
                        title = "Exercise removed from plan",
                        detail = "History is preserved. Add the exercise back to continue suggestions.",
                    )
                } else {
                    ProgressiveOverloadEngine.suggestion(exercise, latestSets)
                }
                _uiState.value = ExerciseDetailUiState(
                    exercise = exercise,
                    history = history,
                    suggestion = suggestion,
                )
            }
        }
    }

    private fun ExerciseHistoryItem.toLoggedSet(exerciseId: Long?): LoggedSetEntity {
        return LoggedSetEntity(
            id = setId,
            sessionId = sessionId,
            exerciseId = exerciseId,
            exerciseNameSnapshot = exerciseNameSnapshot,
            setNumber = setNumber,
            weight = weight,
            reps = reps,
            rir = rir,
            notes = notes,
            isPrWeight = isPrWeight,
            isPrReps = isPrReps,
        )
    }
}
