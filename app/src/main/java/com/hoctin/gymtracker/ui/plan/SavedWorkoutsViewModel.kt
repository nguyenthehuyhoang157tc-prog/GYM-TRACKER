package com.hoctin.gymtracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.SavedWorkoutListItem
import com.hoctin.gymtracker.data.SavedWorkoutRepository
import com.hoctin.gymtracker.data.SavedWorkoutWithExercises
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedWorkoutCardUi(
    val template: SavedWorkoutWithExercises,
    val completionCount: Int,
)

data class SavedWorkoutsUiState(
    val workouts: List<SavedWorkoutCardUi> = emptyList(),
    val loading: Boolean = true,
)

class SavedWorkoutsViewModel(
    private val repository: SavedWorkoutRepository,
) : ViewModel() {
    val uiState: StateFlow<SavedWorkoutsUiState> = combine(
        repository.observeAll(),
        repository.observeList(),
    ) { templates, listItems ->
        val counts = listItems.associateBy(SavedWorkoutListItem::workout).mapKeys { it.key.id }
        SavedWorkoutsUiState(
            workouts = templates.map { template ->
                SavedWorkoutCardUi(
                    template = template,
                    completionCount = counts[template.workout.id]?.completionCount ?: 0,
                )
            },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedWorkoutsUiState(),
    )

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
