package com.hoctin.gymtracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(
    val day: WorkoutDayWithExercises? = null,
    /** 1-based position of [day] in plan order (sortOrder, then id); 0 until resolved. */
    val dayNumber: Int = 0,
    val planSize: Int = 0,
    val loading: Boolean = true,
)

/**
 * Read-only preview of a single workout day, shown between Home and Log Workout
 * (Home → View Detail → Start Workout → Log Workout). It observes the live plan so renames / reorders
 * / exercise edits are reflected, and derives the day number from plan position so it stays correct
 * for custom days. It never mutates Room, drafts, progression, or workout state — "Start Workout" is
 * pure navigation.
 */
class WorkoutDetailViewModel(
    private val dayId: Long,
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _routineChanged = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val routineChanged: SharedFlow<Long> = _routineChanged.asSharedFlow()

    val uiState: StateFlow<WorkoutDetailUiState> = repository.observePlan()
        .map { plan ->
            val index = plan.indexOfFirst { it.day.id == dayId }
            if (index < 0) {
                WorkoutDetailUiState(day = null, dayNumber = 0, planSize = plan.size, loading = false)
            } else {
                WorkoutDetailUiState(
                    day = plan[index],
                    dayNumber = index + 1,
                    planSize = plan.size,
                    loading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutDetailUiState(),
        )

    /**
     * Heaviest logged load per plan exercise id, rendered as a "Max Weight" subtitle under each
     * exercise row. Stays empty until the user logs their first weighted set.
     */
    val maxWeightByExerciseId: StateFlow<Map<Long, Double>> = repository.observeMaxWeightByExerciseId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addExercise(name: String, sets: Int, minReps: Int, maxReps: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addExercise(dayId, name, sets, minReps, maxReps) }
    }

    fun updateExercise(exercise: ExerciseEntity, name: String, sets: Int, minReps: Int, maxReps: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.updateExercise(exercise, name, sets, minReps, maxReps) }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    fun reorderExercises(exercises: List<ExerciseEntity>) {
        viewModelScope.launch { repository.reorderExercises(exercises) }
    }

    fun skipToNextRoutine() {
        viewModelScope.launch {
            val routines = repository.observePlan().first()
            if (routines.size < 2) return@launch
            val current = routines.indexOfFirst { it.day.id == dayId }.takeIf { it >= 0 } ?: 0
            val nextId = routines[(current + 1) % routines.size].day.id
            preferencesRepository.setSelectedDayId(nextId)
            _routineChanged.emit(nextId)
        }
    }
}
