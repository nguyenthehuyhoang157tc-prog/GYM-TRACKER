package com.hoctin.gymtracker.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiMapper
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiModel
import com.hoctin.gymtracker.domain.muscleheat.SessionMuscleHeatUseCase
import com.hoctin.gymtracker.ui.history.HistorySession
import com.hoctin.gymtracker.ui.history.historySessionOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MUSCLE-3 — after-save / saved-session Muscle Heat Review.
 *
 * Loads one saved session (read-only) and computes the runtime muscle-heat model through the
 * MUSCLE-1/2 pipeline: `SessionMuscleHeatUseCase.forSession(sessionId)` → `MuscleHeatUiMapper`.
 * Heat computation is wrapped in `runCatching`: if anything fails, [SessionReviewUiState.heat] stays
 * null and the screen still shows the normal session breakdown — the heat card is an enhancement,
 * never a gate. MUSCLE-3C adds the only write path here: persisted session rename through
 * [GymRepository.renameWorkoutSession].
 */
data class SessionReviewUiState(
    val loading: Boolean = true,
    val session: HistorySession? = null,
    val heat: MuscleHeatUiModel? = null,
    /** Top region display names by intensity (max 2), for the "Top Muscles" line. */
    val topMuscles: List<String> = emptyList(),
    /**
     * MUSCLE-3B.1: the persisted wall-clock workout duration (`workout_sessions.durationSeconds`),
     * in millis; null for pre-feature sessions. The screen prefers this over the nav argument so the
     * after-save review and a History/Home-opened review show the SAME stored value.
     */
    val storedDurationMillis: Long? = null,
)

class SessionReviewViewModel(
    private val sessionId: Long,
    private val repository: GymRepository,
    private val catalogRepository: ExerciseCatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionReviewUiState())
    val uiState: StateFlow<SessionReviewUiState> = _uiState

    /** Exercise thumbnails for the breakdown rows (same lookup History uses). */
    val thumbnailRefs: StateFlow<Map<String, String>> = catalogRepository
        .observeThumbnailRefsByName()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        viewModelScope.launch {
            loadSession()
        }
    }

    fun renameSession(title: String) {
        viewModelScope.launch {
            repository.renameWorkoutSession(sessionId, title)
            loadSession()
        }
    }

    private suspend fun loadSession() {
        val sessionWithSets = repository.sessionWithSets(sessionId)
        val heat = runCatching {
            SessionMuscleHeatUseCase(repository, catalogRepository)
                .forSession(sessionId)
                ?.let(MuscleHeatUiMapper::toUiModel)
        }.getOrNull()
        val topMuscles = heat?.regions.orEmpty()
            .filter { it.intensity > 0.0 }
            .sortedByDescending { it.intensity }
            .take(2)
            .map { it.displayName }
        _uiState.value = SessionReviewUiState(
            loading = false,
            session = sessionWithSets?.let(::historySessionOf),
            heat = heat,
            topMuscles = topMuscles,
            storedDurationMillis = sessionWithSets?.session?.durationSeconds
                ?.takeIf { it > 0 }
                ?.times(1000L),
        )
    }
}
