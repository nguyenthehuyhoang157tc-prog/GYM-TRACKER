package com.hoctin.gymtracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanViewModel(
    private val repository: GymRepository,
) : ViewModel() {
    val plan: StateFlow<List<WorkoutDayWithExercises>> = repository.observePlan()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Heaviest logged load per plan exercise id, rendered as a "Max Weight" subtitle under each
     * exercise row. Stays empty until the user logs their first weighted set.
     */
    val maxWeightByExerciseId: StateFlow<Map<Long, Double>> = repository.observeMaxWeightByExerciseId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
        }
    }

    fun addDay(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addWorkoutDay(name) }
    }

    fun updateDay(day: WorkoutDayEntity, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.updateWorkoutDay(day, name) }
    }

    fun deleteDay(day: WorkoutDayEntity) {
        viewModelScope.launch { repository.deleteWorkoutDay(day) }
    }

    fun addExercise(dayId: Long, name: String, sets: Int, minReps: Int, maxReps: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addExercise(dayId, name, sets, minReps, maxReps)
        }
    }

    fun updateExercise(exercise: ExerciseEntity, name: String, sets: Int, minReps: Int, maxReps: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateExercise(exercise, name, sets, minReps, maxReps)
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    /** Persist the user's drag-reordered exercises for a day. */
    fun reorderExercises(orderedExercises: List<ExerciseEntity>) {
        viewModelScope.launch { repository.reorderExercises(orderedExercises) }
    }
}
