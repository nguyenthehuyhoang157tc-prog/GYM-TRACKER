package com.hoctin.gymtracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.DaySelectionOrigin
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Describes why the hero workout is shown, so the UI can label it appropriately. */
enum class WorkoutContext { TODAY, SELECTED, NEXT }

/** Snapshot of the preference inputs that influence Home wording. */
private data class HomePrefs(
    val selectedDayId: Long?,
    val origin: DaySelectionOrigin,
    val trainingWeekdays: Set<Int>,
)

data class HomeUiState(
    val todayPlan: WorkoutDayWithExercises? = null,
    val availableDays: List<WorkoutDayWithExercises> = emptyList(),
    val context: WorkoutContext = WorkoutContext.TODAY,
    val sessionsThisWeek: Int = 0,
    val totalSessions: Int = 0,
    val currentStreak: Int = 0,
    val recentSessions: List<SessionWithSets> = emptyList(),
)

class HomeViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val ready = MutableStateFlow(false)

    // Grouped preference inputs, kept in a nested combine so the outer combine stays type-safe.
    private val homePrefs = combine(
        preferencesRepository.selectedDayId,
        preferencesRepository.selectionOrigin,
        preferencesRepository.trainingWeekdays,
    ) { selectedDayId, origin, trainingWeekdays ->
        HomePrefs(selectedDayId, origin, trainingWeekdays)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        ready,
        repository.observePlan(),
        repository.observeSessionHistory(),
        homePrefs,
    ) { _, plan, sessions, prefs ->
        val selected = plan.firstOrNull { it.day.id == prefs.selectedDayId }
        val today = selected ?: pickTodayPlan(plan)
        val todayWeekday = LocalDate.now().dayOfWeek.value // ISO: Monday = 1 … Sunday = 7
        val context = when {
            // Manual pick after auto-advance keeps "Next workout"; other manual picks are "Selected".
            selected != null && prefs.origin == DaySelectionOrigin.ADVANCED -> WorkoutContext.NEXT
            selected != null -> WorkoutContext.SELECTED
            // No manual selection: derive wording from the chosen training weekdays.
            prefs.trainingWeekdays.isEmpty() -> WorkoutContext.TODAY
            prefs.trainingWeekdays.contains(todayWeekday) -> WorkoutContext.TODAY
            else -> WorkoutContext.NEXT
        }
        val weekStart = LocalDate.now().minusDays(6).toEpochDay()
        HomeUiState(
            todayPlan = today,
            availableDays = plan,
            context = context,
            sessionsThisWeek = sessions.count { it.session.dateEpochDay >= weekStart },
            totalSessions = sessions.size,
            currentStreak = computeStreak(sessions.map { it.session.dateEpochDay }),
            recentSessions = sessions.take(3),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            ready.value = true
        }
    }

    /** Manually pick the current workout day and persist the choice. */
    fun selectDay(dayId: Long) {
        preferencesRepository.setSelectedDayId(dayId)
    }

    /** Advance to the next workout day in the plan, cycling back to the first. */
    fun skipToNextWorkoutDay() {
        val state = uiState.value
        val days = state.availableDays
        if (days.isEmpty()) return
        val currentId = state.todayPlan?.day?.id
        val currentIndex = days.indexOfFirst { it.day.id == currentId }
        val nextIndex = (currentIndex + 1).mod(days.size)
        preferencesRepository.setSelectedDayId(days[nextIndex].day.id)
    }

    private fun pickTodayPlan(plan: List<WorkoutDayWithExercises>): WorkoutDayWithExercises? {
        if (plan.isEmpty()) return null
        val dayIndex = (LocalDate.now().dayOfWeek.value - 1) % plan.size
        return plan[dayIndex]
    }

    /**
     * Current streak = consecutive calendar days (counting back from today) that have at least one
     * logged session. A one-day grace is allowed so the streak does not reset before today's workout.
     */
    private fun computeStreak(epochDays: List<Long>): Int {
        if (epochDays.isEmpty()) return 0
        val workoutDays = epochDays.toHashSet()
        val today = LocalDate.now().toEpochDay()
        var anchor = when {
            workoutDays.contains(today) -> today
            workoutDays.contains(today - 1) -> today - 1
            else -> return 0
        }
        var streak = 0
        while (workoutDays.contains(anchor)) {
            streak++
            anchor--
        }
        return streak
    }
}
