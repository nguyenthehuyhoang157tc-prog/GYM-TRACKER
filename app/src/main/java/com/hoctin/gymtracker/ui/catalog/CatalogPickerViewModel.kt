package com.hoctin.gymtracker.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.CatalogSearch
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.Level
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogPickerUiState(
    val dayName: String = "",
    val query: String = "",
    val equipment: Equipment? = null,
    val primaryMuscle: MuscleGroup? = null,
    val level: Level? = null,
    val progressionType: ProgressionType? = null,
    val results: List<CatalogExercise> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Drives the Catalog Picker. Reads the catalog via [ExerciseCatalogRepository] (loaded once into
 * memory) and filters deterministically with the shared [CatalogSearch]. Adding an exercise creates a
 * brand-new **plan** snapshot via [GymRepository.addExercise] using the catalog defaults — there is no
 * stored dependency from the plan exercise back to the catalog (DB-1 rule). Nothing here touches Room
 * schema, Log Workout, History, Progression V3, or Import/Export.
 */
class CatalogPickerViewModel(
    private val dayId: Long,
    private val catalogRepository: ExerciseCatalogRepository,
    private val gymRepository: GymRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CatalogPickerUiState())
    val uiState: StateFlow<CatalogPickerUiState> = _uiState.asStateFlow()

    // Emits the name of an exercise just added, so the screen can show a confirmation snackbar.
    private val _added = MutableSharedFlow<String>()
    val added: SharedFlow<String> = _added.asSharedFlow()

    private var allCatalog: List<CatalogExercise> = emptyList()

    init {
        viewModelScope.launch {
            val day = gymRepository.getWorkoutDay(dayId)
            allCatalog = catalogRepository.all()
            val base = _uiState.value.copy(dayName = day?.name.orEmpty(), loading = false)
            _uiState.value = base.copy(results = applyFilters(base))
        }
    }

    fun setQuery(query: String) = setState { it.copy(query = query) }
    fun setEquipment(equipment: Equipment?) = setState { it.copy(equipment = equipment) }
    fun setPrimaryMuscle(muscle: MuscleGroup?) = setState { it.copy(primaryMuscle = muscle) }
    fun setLevel(level: Level?) = setState { it.copy(level = level) }
    fun setProgressionType(type: ProgressionType?) = setState { it.copy(progressionType = type) }

    fun clearFilters() = setState {
        it.copy(query = "", equipment = null, primaryMuscle = null, level = null, progressionType = null)
    }

    /** Adds the catalog exercise to the target day as a new, fully-editable plan exercise snapshot. */
    fun add(exercise: CatalogExercise) {
        viewModelScope.launch {
            gymRepository.addExercise(
                dayId = dayId,
                name = exercise.name,
                targetSets = exercise.defaultSets,
                targetMinReps = exercise.defaultMinReps,
                targetMaxReps = exercise.defaultMaxReps,
            )
            _added.emit(exercise.name)
        }
    }

    private fun setState(transform: (CatalogPickerUiState) -> CatalogPickerUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next.copy(results = applyFilters(next))
    }

    private fun applyFilters(state: CatalogPickerUiState): List<CatalogExercise> =
        CatalogSearch.filter(
            items = allCatalog,
            query = state.query,
            equipment = state.equipment,
            primaryMuscle = state.primaryMuscle,
            level = state.level,
            progressionType = state.progressionType,
        )
}
