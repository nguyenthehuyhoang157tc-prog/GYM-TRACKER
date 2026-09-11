package com.hoctin.gymtracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.SavedWorkoutExerciseDraft
import com.hoctin.gymtracker.data.SavedWorkoutExerciseEntity
import com.hoctin.gymtracker.data.SavedWorkoutRepository
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BuilderTab { ALL, SELECTED }
enum class BuilderStage { PICK, REVIEW }

data class BuilderExercise(
    val key: String,
    val name: String,
    val catalog: CatalogExercise?,
    val catalogSlug: String?,
    val canonicalExerciseKey: String,
    val targetSets: Int,
    val targetMinReps: Int,
    val targetMaxReps: Int,
)

data class SavedWorkoutBuilderUiState(
    val editId: Long? = null,
    val workoutName: String = "",
    val allExercises: List<CatalogExercise> = emptyList(),
    val selected: List<BuilderExercise> = emptyList(),
    val tab: BuilderTab = BuilderTab.ALL,
    val stage: BuilderStage = BuilderStage.PICK,
    val query: String = "",
    val muscle: MuscleGroup? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
)

sealed interface SavedWorkoutBuilderEvent {
    data class Saved(val id: Long) : SavedWorkoutBuilderEvent
}

class SavedWorkoutBuilderViewModel(
    private val editId: Long?,
    private val repository: SavedWorkoutRepository,
    private val catalogRepository: ExerciseCatalogRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedWorkoutBuilderUiState(editId = editId))
    val uiState: StateFlow<SavedWorkoutBuilderUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<SavedWorkoutBuilderEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SavedWorkoutBuilderEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            catalogRepository.ensureSeeded()
            catalogRepository.observeAll().collectLatest { all ->
                val current = _uiState.value
                val catalogBySlug = all.associateBy(CatalogExercise::slug)
                val loaded = if (current.loading && editId != null) repository.get(editId) else null
                _uiState.update { state ->
                    state.copy(
                        allExercises = all,
                        workoutName = loaded?.workout?.name ?: state.workoutName,
                        selected = loaded?.exercises?.map { it.toBuilder(catalogBySlug[it.catalogSlug]) }
                            ?: state.selected.map { item ->
                                if (item.catalog == null && item.catalogSlug != null) {
                                    item.copy(catalog = catalogBySlug[item.catalogSlug])
                                } else item
                            },
                        loading = false,
                    )
                }
            }
        }
    }

    fun setTab(tab: BuilderTab) = _uiState.update { it.copy(tab = tab) }
    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }
    fun setMuscle(muscle: MuscleGroup?) = _uiState.update { it.copy(muscle = muscle) }
    fun setName(name: String) = _uiState.update { it.copy(workoutName = name, error = null) }
    fun showReview() {
        if (_uiState.value.selected.isNotEmpty()) _uiState.update { it.copy(stage = BuilderStage.REVIEW) }
    }
    fun closeReview() = _uiState.update { it.copy(stage = BuilderStage.PICK, tab = BuilderTab.SELECTED) }

    fun toggle(exercise: CatalogExercise) {
        _uiState.update { state ->
            val key = "catalog:${exercise.slug}"
            val selected = if (state.selected.any { it.key == key }) {
                state.selected.filterNot { it.key == key }
            } else {
                state.selected + exercise.toBuilder()
            }
            state.copy(selected = selected, error = null)
        }
    }

    fun remove(key: String) = _uiState.update { state ->
        state.copy(selected = state.selected.filterNot { it.key == key })
    }

    fun reorder(items: List<BuilderExercise>) = _uiState.update { it.copy(selected = items) }

    fun save() {
        val state = _uiState.value
        if (state.workoutName.isBlank() || state.selected.isEmpty() || state.saving) {
            _uiState.update { it.copy(error = "Add a name and at least one exercise.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            runCatching {
                val drafts = state.selected.map(BuilderExercise::toDraft)
                if (editId == null) {
                    repository.create(state.workoutName, drafts)
                } else {
                    check(repository.rename(editId, state.workoutName))
                    check(repository.replaceExercises(editId, drafts))
                    editId
                }
            }.onSuccess { id ->
                _uiState.update { it.copy(saving = false) }
                _events.emit(SavedWorkoutBuilderEvent.Saved(id))
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(saving = false, error = throwable.message ?: "Workout could not be saved.")
                }
            }
        }
    }
}

private fun CatalogExercise.toBuilder() = BuilderExercise(
    key = "catalog:$slug",
    name = name,
    catalog = this,
    catalogSlug = slug,
    canonicalExerciseKey = "catalog:$slug",
    targetSets = defaultSets.coerceAtLeast(1),
    targetMinReps = defaultMinReps.coerceAtLeast(0),
    targetMaxReps = defaultMaxReps.coerceAtLeast(defaultMinReps.coerceAtLeast(0)),
)

private fun SavedWorkoutExerciseEntity.toBuilder(catalog: CatalogExercise?) = BuilderExercise(
    key = catalogSlug?.let { "catalog:$it" } ?: "saved:$id",
    name = exerciseNameSnapshot,
    catalog = catalog,
    catalogSlug = catalogSlug,
    canonicalExerciseKey = canonicalExerciseKey.ifBlank { CanonicalExerciseKey.customKey(exerciseNameSnapshot) },
    targetSets = targetSets,
    targetMinReps = targetMinReps,
    targetMaxReps = targetMaxReps,
)

private fun BuilderExercise.toDraft() = SavedWorkoutExerciseDraft(
    catalogSlug = catalogSlug,
    canonicalExerciseKey = canonicalExerciseKey,
    exerciseNameSnapshot = name,
    targetSets = targetSets,
    targetMinReps = targetMinReps,
    targetMaxReps = targetMaxReps,
)
