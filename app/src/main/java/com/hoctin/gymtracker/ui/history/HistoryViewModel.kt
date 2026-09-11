package com.hoctin.gymtracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.SessionWithSets
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ---------------------------------------------------------------------------
// HISTORY-2: month-based Workout History (premium archive). All grouping/aggregation happens here in
// the ViewModel so composables only render already-prepared, lightweight state. The old week-collapse
// grouping was replaced by swipeable months; sessions/exercises/sets models are unchanged (the Session
// Review screen reuses them via HistorySessionMapper).
// ---------------------------------------------------------------------------

data class HistorySet(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rir: Int,
    val isPr: Boolean,
    // DB-6C: present only for time/distance sets; when set, the row renders the value (not reps/RIR).
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
)

data class HistoryExercise(
    val name: String,
    val setCount: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val hasRepSets: Boolean,
    val hasPr: Boolean,
    val sets: List<HistorySet>,
    // DB-6C: true when every set is time/distance — the UI then never shows fake reps/kg for it.
    val isNonRep: Boolean = false,
)

data class HistorySession(
    val id: Long,
    val dayName: String,
    val dateEpochDay: Long,
    val startedAtMillis: Long,
    val totalSets: Int,
    val totalVolume: Double,
    /** Σ of time-tracked SET durations (plank/sled work) — not the workout wall clock. */
    val durationSeconds: Int,
    val distanceMeters: Double,
    val hasRepSets: Boolean,
    val prCount: Int,
    val exerciseCount: Int,
    val exercises: List<HistoryExercise>,
    /** MUSCLE-3B.1 wall-clock workout duration (persisted at save); null for pre-feature sessions. */
    val wallDurationSeconds: Int? = null,
)

/** One swipeable month page. Months exist even when empty (the archive shows an empty state). */
data class HistoryMonth(
    val year: Int,
    val month: Int, // 1..12
    val yearLabel: String,  // "2026"
    val monthLabel: String, // "JULY"
    val sessions: List<HistorySession>,
)

data class HistoryUiState(
    val totalSessions: Int = 0,
    /** Newest month first (index 0 = current month); swiping forward moves into the past. */
    val months: List<HistoryMonth> = emptyList(),
)

class HistoryViewModel(
    repository: GymRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repository.observeSessionHistory()
        .map { sessions -> buildHistory(sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /**
     * Groups sessions into calendar months, newest first, spanning from the CURRENT month back to the
     * earliest saved session's month — months with no workouts are included so the pager can show the
     * empty-month state. No schema change; everything derives from `workout_sessions` timestamps.
     */
    private fun buildHistory(sessions: List<SessionWithSets>): HistoryUiState {
        val currentMonth = YearMonth.now()
        val byMonth = sessions.groupBy { sws ->
            YearMonth.from(LocalDate.ofEpochDay(sws.session.dateEpochDay))
        }
        val earliestMonth = byMonth.keys.minOrNull() ?: currentMonth

        val months = mutableListOf<HistoryMonth>()
        var cursor = currentMonth
        while (!cursor.isBefore(earliestMonth)) {
            val monthSessions = byMonth[cursor].orEmpty()
                .sortedByDescending { it.session.startedAtMillis }
                .map { historySessionOf(it) }
            months += HistoryMonth(
                year = cursor.year,
                month = cursor.monthValue,
                yearLabel = cursor.year.toString(),
                monthLabel = cursor.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH),
                sessions = monthSessions,
            )
            cursor = cursor.minusMonths(1)
        }
        return HistoryUiState(totalSessions = sessions.size, months = months)
    }
}
