package com.hoctin.gymtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.DaySelectionOrigin
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.data.countsTowardWeeklyProgress
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.wearable.WearableHomeContext
import com.hoctin.gymtracker.data.wearable.WearableShadowRepository
import com.hoctin.gymtracker.domain.DailyBoostExercise
import com.hoctin.gymtracker.domain.muscleheat.MuscleRecoveryEngine
import com.hoctin.gymtracker.domain.muscleheat.MuscleRecoverySnapshot
import com.hoctin.gymtracker.domain.muscleheat.MuscleRecoveryUseCase
import com.hoctin.gymtracker.domain.muscleheat.RecoveryPalette
import com.hoctin.gymtracker.domain.muscleheat.RecoveryStatus
import com.hoctin.gymtracker.domain.muscleheat.label
import com.hoctin.gymtracker.domain.muscleheat.recoveryRegionPaints
import com.hoctin.gymtracker.ui.history.historySessionOf
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSide
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSlugs
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Describes why the hero workout is shown, so the UI can label it appropriately. */
enum class WorkoutContext { TODAY, SELECTED, NEXT }

/** Preference inputs that influence Home workout selection and wording. */
private data class HomePrefs(
    val selectedDayId: Long?,
    val origin: DaySelectionOrigin,
    val trainingWeekdays: Set<Int>,
)

/** Preference inputs consumed only by the pure Consistency calculator. */
private data class ConsistencyPrefs(
    val trainingWeekdays: Set<Int>,
    val boostDays: Set<Long>,
    val trackingStartEpochDay: Long?,
    val storedBestActiveStreak: Int,
)

data class HomeUiState(
    val todayPlan: WorkoutDayWithExercises? = null,
    val availableDays: List<WorkoutDayWithExercises> = emptyList(),
    val context: WorkoutContext = WorkoutContext.TODAY,
    val consistency: ConsistencyUiState = ConsistencyUiState(),
)

/**
 * Daily Boost state for Home. It remains independent from workout history and Weekly Progress.
 */
data class DailyBoostUiState(
    val exercise: DailyBoostExercise = DailyBoostExercise.DEFAULT,
    val completedToday: Boolean = false,
)

/** MUSCLE-4B: one coarse body-group readiness tile (Legs / Back / etc.). */
data class RecoveryGroupTile(
    val name: String,
    val recoveryPercent: Int,
    val statusLabel: String,
    val argb: Long,
    val trained: Boolean,
)

/** MUSCLE-4B: one intelligence line (limited muscle, plan overlap, or neglect nudge). */
data class RecoveryNudge(val text: String, val argb: Long)

/** MUSCLE-4B: everything the Home readiness dashboard renders. Built here, never in the UI. */
data class MuscleRecoveryUiState(
    val hasData: Boolean = false,
    val frontSlugArgb: Map<String, Long> = emptyMap(),
    val backSlugArgb: Map<String, Long> = emptyMap(),
    val overallReadiness: Int = 100,
    val overallLabel: String = "Ready",
    val overallArgb: Long = 0xFF4ED07A,
    val groups: List<RecoveryGroupTile> = emptyList(),
    val nudges: List<RecoveryNudge> = emptyList(),
    val readyLine: String? = null,
)

class HomeViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
    catalogRepository: ExerciseCatalogRepository,
    private val consistencyPresentationStore: ConsistencyPresentationStore,
    private val wearableShadowRepository: WearableShadowRepository,
) : ViewModel() {
    private val ready = MutableStateFlow(false)

    private val homePrefs = combine(
        preferencesRepository.selectedDayId,
        preferencesRepository.selectionOrigin,
        preferencesRepository.trainingWeekdays,
    ) { selectedDayId, origin, trainingWeekdays ->
        HomePrefs(selectedDayId, origin, trainingWeekdays)
    }

    private val consistencyPrefs = combine(
        preferencesRepository.trainingWeekdays,
        preferencesRepository.dailyBoostCompletionDays,
        preferencesRepository.trackingStartEpochDay,
        consistencyPresentationStore.bestActiveStreak,
    ) { trainingWeekdays, boostDays, trackingStartEpochDay, storedBestActiveStreak ->
        ConsistencyPrefs(
            trainingWeekdays = trainingWeekdays,
            boostDays = boostDays,
            trackingStartEpochDay = trackingStartEpochDay,
            storedBestActiveStreak = storedBestActiveStreak,
        )
    }

    /**
     * One shared, calculator-backed source for both the compact Home module and the full Consistency
     * route. Weekly progress never receives Boost data; the active-day union is contained inside the
     * pure calculator. The only write is a max-only presentation preference for best streak.
     */
    val consistency: StateFlow<ConsistencyUiState> = combine(
        repository.observeSessionHistory(),
        repository.observePlan(),
        consistencyPrefs,
    ) { sessions, plan, prefs ->
        ConsistencyCalculator.calculate(
            ConsistencyInputs(
                workouts = sessions.map { sessionWithSets ->
                    val historySession = historySessionOf(sessionWithSets)
                    ConsistencyWorkoutEvidence(
                        sessionId = historySession.id,
                        title = historySession.dayName,
                        startedAtMillis = historySession.startedAtMillis,
                        epochDay = historySession.dateEpochDay,
                        totalSets = historySession.totalSets,
                        repVolumeKg = historySession.totalVolume.takeIf {
                            historySession.hasRepSets && it > 0.0
                        },
                        durationSeconds = (
                            historySession.wallDurationSeconds
                                ?: historySession.durationSeconds.takeIf { it > 0 }
                            ),
                        exerciseCount = historySession.exerciseCount,
                        prCount = historySession.prCount,
                        countsTowardWeeklyProgress = sessionWithSets.session
                            .countsTowardWeeklyProgress(),
                    )
                },
                boostDays = prefs.boostDays,
                trainingWeekdays = prefs.trainingWeekdays,
                weeklyTargetFallback = plan.size,
                trackingStartEpochDay = prefs.trackingStartEpochDay,
                storedBestActiveStreak = prefs.storedBestActiveStreak,
                today = LocalDate.now(),
            ),
        )
    }.onEach { state ->
        consistencyPresentationStore.updateBestActiveStreak(state.bestActiveStreak)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConsistencyUiState(),
    )

    val uiState: StateFlow<HomeUiState> = combine(
        ready,
        repository.observePlan(),
        homePrefs,
        consistency,
    ) { _, plan, prefs, consistencyState ->
        val selected = plan.firstOrNull { it.day.id == prefs.selectedDayId }
        val today = selected ?: pickTodayPlan(plan)
        val todayWeekday = LocalDate.now().dayOfWeek.value
        val context = when {
            selected != null && prefs.origin == DaySelectionOrigin.ADVANCED -> WorkoutContext.NEXT
            selected != null -> WorkoutContext.SELECTED
            prefs.trainingWeekdays.isEmpty() -> WorkoutContext.TODAY
            prefs.trainingWeekdays.contains(todayWeekday) -> WorkoutContext.TODAY
            else -> WorkoutContext.NEXT
        }
        HomeUiState(
            todayPlan = today,
            availableDays = plan,
            context = context,
            consistency = consistencyState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    val dailyBoost: StateFlow<DailyBoostUiState> = combine(
        preferencesRepository.dailyBoostExerciseKey,
        preferencesRepository.dailyBoostCompletionDays,
    ) { key, boostDays ->
        DailyBoostUiState(
            exercise = DailyBoostExercise.fromKey(key),
            completedToday = boostDays.contains(LocalDate.now().toEpochDay()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyBoostUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            ready.value = true
        }
    }

    /** WEARABLE-2A informational shadow state. It is never combined with readiness flows. */
    val healthContext: StateFlow<WearableHomeContext> = wearableShadowRepository.homeContext.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WearableHomeContext(),
    )

    fun refreshHealthContext() {
        viewModelScope.launch {
            runCatching { wearableShadowRepository.refreshExplicit() }
        }
    }

    // ---- MUSCLE-4B: Home Muscle Recovery (runtime-only, derived from saved sessions) ----

    private val recoveryUseCase = MuscleRecoveryUseCase(catalogRepository)

    val muscleRecovery: StateFlow<MuscleRecoveryUiState> = combine(
        repository.observeSessionHistory(),
        repository.observePlan(),
        homePrefs,
    ) { sessions, plan, prefs ->
        val selectedNames = plan.firstOrNull { it.day.id == prefs.selectedDayId }
            ?.exercises?.map { it.name }.orEmpty()
        val allNames = plan.flatMap { day -> day.exercises.map { it.name } }
        buildRecoveryUiState(
            recoveryUseCase.compute(sessions, selectedNames, allNames, System.currentTimeMillis()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MuscleRecoveryUiState())

    private fun buildRecoveryUiState(snapshot: MuscleRecoverySnapshot): MuscleRecoveryUiState {
        if (!snapshot.hasRecentTraining) return MuscleRecoveryUiState(hasData = false)
        val paints = recoveryRegionPaints(snapshot.results)

        val groups = snapshot.groups.map { group ->
            RecoveryGroupTile(
                name = group.group.display,
                recoveryPercent = group.recoveryScore,
                statusLabel = group.status.label(),
                argb = RecoveryPalette.statusArgb(group.status),
                trained = group.trained,
            )
        }

        val nudges = mutableListOf<RecoveryNudge>()
        snapshot.results
            .filter {
                it.status == RecoveryStatus.OVERLOADED ||
                    it.status == RecoveryStatus.SORE ||
                    it.status == RecoveryStatus.RECOVERING
            }
            .take(2)
            .forEach { result ->
                nudges += RecoveryNudge(
                    text = "${MuscleRecoveryEngine.displayName(result.muscleGroup)} · ${result.reason}",
                    argb = RecoveryPalette.statusArgb(result.status),
                )
            }
        if (snapshot.planOverlap.isNotEmpty()) {
            val names = snapshot.planOverlap.joinToString(", ") {
                MuscleRecoveryEngine.displayName(it)
            }
            nudges += RecoveryNudge(
                text = "Today's plan hits $names — still recovering. Consider swapping.",
                argb = RecoveryPalette.statusArgb(RecoveryStatus.SORE),
            )
        }
        snapshot.neglected.take(2).forEach { neglected ->
            val name = MuscleRecoveryEngine.displayName(neglected.muscleGroup)
            val text = neglected.daysSinceTrained?.let {
                "You haven't trained $name in $it days."
            } ?: "You haven't trained $name recently."
            nudges += RecoveryNudge(text = text, argb = 0xFF9B9DA8)
        }

        val readyGroups = snapshot.groups.filter { it.status == RecoveryStatus.READY }
        val readyLine = when {
            snapshot.results.all { it.status == RecoveryStatus.READY } ->
                "Every trained muscle has recovered — great day to push."
            readyGroups.isNotEmpty() ->
                "${readyGroups.joinToString(", ") { it.group.display }} ready to train."
            else -> null
        }

        return MuscleRecoveryUiState(
            hasData = true,
            frontSlugArgb = BodyMapSlugs.recoverySlugArgb(BodyMapSide.FRONT, paints),
            backSlugArgb = BodyMapSlugs.recoverySlugArgb(BodyMapSide.BACK, paints),
            overallReadiness = snapshot.overallReadiness,
            overallLabel = snapshot.overallStatus.label(),
            overallArgb = RecoveryPalette.statusArgb(snapshot.overallStatus),
            groups = groups,
            nudges = nudges,
            readyLine = readyLine,
        )
    }

    fun selectDay(dayId: Long) {
        preferencesRepository.setSelectedDayId(dayId)
    }

    fun skipToNextWorkoutDay() {
        val state = uiState.value
        val days = state.availableDays
        if (days.isEmpty()) return
        val currentId = state.todayPlan?.day?.id
        val currentIndex = days.indexOfFirst { it.day.id == currentId }
        val nextIndex = (currentIndex + 1).mod(days.size)
        preferencesRepository.setSelectedDayId(days[nextIndex].day.id)
    }

    fun selectDailyBoostExercise(exercise: DailyBoostExercise) {
        preferencesRepository.setDailyBoostExercise(exercise.key)
    }

    fun randomizeDailyBoostExercise() {
        // Read the persisted pick directly from the repository StateFlow — NOT dailyBoost.value,
        // whose stateIn initialValue (catalog default) can still be current before the first
        // emission / after a WhileSubscribed reset, which made Random re-pick the visible exercise.
        val current = DailyBoostExercise.fromKey(preferencesRepository.dailyBoostExerciseKey.value)
        val next = DailyBoostExercise.entries.filter { it != current }.random()
        preferencesRepository.setDailyBoostExercise(next.key)
    }

    fun markDailyBoostCompleted() {
        preferencesRepository.markDailyBoostCompleted()
    }

    /**
     * Atomically marks today's fire as consumed before the animation starts. This prevents a replay
     * after navigation, activity recreation, or process restart, and also prevents a second activity
     * source on the same day from triggering again.
     */
    fun consumeStreakCelebration(): Boolean {
        val state = consistency.value
        if (!state.ready) return false
        return consistencyPresentationStore.consumeCelebration(
            todayEpochDay = state.todayEpochDay,
            isActiveToday = state.isTodayActive,
        )
    }

    private fun pickTodayPlan(
        plan: List<WorkoutDayWithExercises>,
    ): WorkoutDayWithExercises? {
        if (plan.isEmpty()) return null
        val dayIndex = (LocalDate.now().dayOfWeek.value - 1) % plan.size
        return plan[dayIndex]
    }
}
