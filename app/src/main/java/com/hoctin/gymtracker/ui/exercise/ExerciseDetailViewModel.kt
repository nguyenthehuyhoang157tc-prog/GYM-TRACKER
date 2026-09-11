package com.hoctin.gymtracker.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseHistoryItem
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.domain.OverloadSuggestion
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.domain.toWorkoutContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val history: List<ExerciseHistoryItem> = emptyList(),
    val suggestion: OverloadSuggestion = OverloadSuggestion("No data", "Log this exercise to get a progression target."),
    val trackingType: TrackingType = TrackingType.REPS_WEIGHT,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModel(
    private val exerciseId: Long,
    private val repository: GymRepository,
    private val catalogRepository: ExerciseCatalogRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ensure the catalog is seeded so V3.5 can read progression metadata (idempotent).
            catalogRepository.ensureSeeded()
            // DB-PRE7B: build the slug map once so canonical-key resolution is synchronous below.
            val slugMap = catalogRepository.canonicalSlugsByName()
            repository.observeExercise(exerciseId)
                .flatMapLatest { exercise ->
                    if (exercise == null) {
                        // Removed from plan: no name to resolve a key, so fall back to the plan-row id
                        // history (unchanged behavior) so the screen still shows the preserved history.
                        repository.observeExerciseHistory(exerciseId).map { history -> null to history }
                    } else {
                        // DB-PRE7B: cross-day history for this exercise's canonical identity.
                        val key = CanonicalExerciseKey.resolve(exercise.name, slugMap)
                        repository.observeExerciseHistoryByKey(key, exercise.name)
                            .map { history -> exercise to history }
                    }
                }
                .combine(preferencesRepository.workoutGoal) { exerciseAndHistory, goalSelection ->
                    exerciseAndHistory to goalSelection
                }
                .collect { (exerciseAndHistory, goalSelection) ->
                    val (exercise, history) = exerciseAndHistory
                    var trackingType = TrackingType.REPS_WEIGHT
                    val suggestion = if (exercise == null) {
                        OverloadSuggestion(
                            title = "Exercise removed from plan",
                            detail = "History is preserved. Add the exercise back to continue suggestions.",
                        )
                    } else {
                    // Full history (all sessions, newest first) plus the catalog profile (V3.5): the
                    // engine uses catalog metadata when the exercise matches, else name inference.
                    val catalogExercise = catalogRepository.findByName(exercise.name)
                    trackingType = catalogExercise?.trackingType ?: TrackingType.REPS_WEIGHT
                    val profile = catalogExercise?.let { c ->
                        ProgressiveOverloadEngine.profileFromCatalog(
                            progressionTypeName = c.progressionType.name,
                            incrementKg = c.defaultIncrementKg,
                            sessionsRequiredBeforeIncrease = c.sessionsRequiredBeforeIncrease,
                            isBodyweight = c.isBodyweight,
                            isUnilateral = c.isUnilateral,
                            trackingTypeName = c.trackingType.name,
                        )
                    }
                    // V3.6B latest readiness + V3.7B readiness series (read-only) modify confidence/wording only.
                    val readiness = history.firstOrNull()?.let { repository.readinessForSession(it.sessionId) }
                    val readinessSeries = repository.readinessBySession(history.map { it.sessionId }.distinct())
                        // V3.11C: the same persisted AUTO/EXPLICIT goal source used by Log Workout.
                        // Exercise Detail has no sibling counts, so only goal policy is added here.
                        ProgressiveOverloadEngine.suggestion(
                            exercise,
                            history.map { it.toLoggedSet(exercise.id) },
                            profile,
                            readiness,
                            readinessSeries,
                            goalSelection.toWorkoutContext(),
                        )
                    }
                    _uiState.value = ExerciseDetailUiState(
                        exercise = exercise,
                        history = history,
                        suggestion = suggestion,
                        trackingType = trackingType,
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
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
        )
    }
}
