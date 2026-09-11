package com.hoctin.gymtracker.ui.home

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

/**
 * The source that makes one calendar day active. BOTH remains one active day for streak math.
 */
enum class ActivitySource {
    NONE,
    WORKOUT,
    BOOST,
    BOTH,
}

/**
 * Workout-schedule state only. Daily Boost never changes this axis.
 */
enum class WorkoutDayStatus {
    COMPLETED,
    PLANNED,
    MISSED,
    REST,
}

/**
 * Plain-language continuity state for today.
 */
enum class TodayContinuityState {
    ACTIVE_TODAY,
    CONTINUE_TODAY,
    START_TODAY,
}

/**
 * Minimal saved-workout evidence needed by the Consistency UI.
 */
data class ConsistencyWorkoutEvidence(
    val sessionId: Long,
    val title: String,
    val startedAtMillis: Long,
    val epochDay: Long,
    val totalSets: Int = 0,
    /** Rep-based kg volume only. Null keeps bodyweight/time/distance sessions free of fake 0 kg. */
    val repVolumeKg: Double? = null,
    /** Meaningful saved workout or tracked-set duration; null when neither source is available. */
    val durationSeconds: Int? = null,
    val exerciseCount: Int = 0,
    val prCount: Int = 0,
    /** False only for Saved One-Off sessions: active evidence, but not Routine schedule adherence. */
    val countsTowardWeeklyProgress: Boolean = true,
)

/** Stable presentation order for the selected-day evidence receipt. */
internal enum class ConsistencyWorkoutSummaryMetric {
    SETS,
    REP_VOLUME,
    DURATION,
    EXERCISES,
    PERSONAL_RECORDS,
}

/**
 * Keeps chips useful across rep and non-rep sessions. In particular, storage sentinels never become
 * visible "0 kg", "0 reps", or empty duration/distance metrics.
 */
internal fun ConsistencyWorkoutEvidence.visibleSummaryMetrics():
    List<ConsistencyWorkoutSummaryMetric> = buildList {
    if (totalSets > 0) add(ConsistencyWorkoutSummaryMetric.SETS)
    if (repVolumeKg != null && repVolumeKg > 0.0) {
        add(ConsistencyWorkoutSummaryMetric.REP_VOLUME)
    }
    if (durationSeconds != null && durationSeconds > 0) {
        add(ConsistencyWorkoutSummaryMetric.DURATION)
    }
    if (exerciseCount > 0) add(ConsistencyWorkoutSummaryMetric.EXERCISES)
    if (prCount > 0) add(ConsistencyWorkoutSummaryMetric.PERSONAL_RECORDS)
}

/**
 * One date with two deliberately separate visual channels:
 * [workoutStatus] for the workout schedule and [activitySource] for continuity evidence.
 */
data class ConsistencyDay(
    val epochDay: Long,
    val dayOfMonth: Int,
    val dayOfWeek: Int,
    val workoutStatus: WorkoutDayStatus,
    val activitySource: ActivitySource,
    val workouts: List<ConsistencyWorkoutEvidence> = emptyList(),
    val isToday: Boolean = false,
    val isBeforeTracking: Boolean = false,
)

data class ConsistencyMilestone(
    val days: Int,
    val reached: Boolean,
    val isNext: Boolean,
)

data class ConsistencyUiState(
    val weeklyCompleted: Int = 0,
    val weeklyTarget: Int = 0,
    val weeklyProgress: Float = 0f,
    val currentStreak: Int = 0,
    val bestActiveStreak: Int = 0,
    val continuityState: TodayContinuityState = TodayContinuityState.START_TODAY,
    val todayEpochDay: Long = 0L,
    val trackingStartEpochDay: Long = 0L,
    val currentWeek: List<ConsistencyDay> = emptyList(),
    val calendarDays: List<ConsistencyDay> = emptyList(),
    val calendarMonths: List<YearMonth> = emptyList(),
    val milestones: List<ConsistencyMilestone> = emptyList(),
    val ready: Boolean = false,
) {
    val isTodayActive: Boolean
        get() = continuityState == TodayContinuityState.ACTIVE_TODAY

    fun day(epochDay: Long): ConsistencyDay? =
        calendarDays.firstOrNull { it.epochDay == epochDay }
            ?: currentWeek.firstOrNull { it.epochDay == epochDay }
}

internal data class ConsistencyInputs(
    val workouts: List<ConsistencyWorkoutEvidence>,
    val boostDays: Set<Long>,
    val trainingWeekdays: Set<Int>,
    val weeklyTargetFallback: Int,
    val trackingStartEpochDay: Long?,
    val storedBestActiveStreak: Int,
    val today: LocalDate,
)

/**
 * Pure calculator shared by Home and the full Consistency screen.
 *
 * Weekly Progress is workout-only and distinct by date. Active streak is the union of workout and
 * Daily Boost dates, also distinct by date. Today's pending grace anchors to yesterday only; a gap
 * anywhere behind that anchor still stops the streak immediately.
 */
internal object ConsistencyCalculator {
    private val milestoneTargets = listOf(7, 14, 30, 60)

    fun calculate(inputs: ConsistencyInputs): ConsistencyUiState {
        val todayEpoch = inputs.today.toEpochDay()
        val workoutsByDay = inputs.workouts
            .groupBy { it.epochDay }
            .mapValues { (_, sessions) -> sessions.sortedByDescending { it.startedAtMillis } }
        // Every real workout maintains activity continuity. Weekly Progress is narrower: only Routine
        // sessions represent the scheduled plan contract.
        val workoutDays = workoutsByDay.keys
        val weeklyRoutineWorkoutDays = inputs.workouts
            .asSequence()
            .filter { it.countsTowardWeeklyProgress }
            .map { it.epochDay }
            .toSet()
        val boostDays = inputs.boostDays.filterTo(mutableSetOf()) { it <= todayEpoch }
        val activeDays = (workoutDays + boostDays).filterTo(mutableSetOf()) { it <= todayEpoch }

        val weeklyTarget = inputs.trainingWeekdays.size
            .takeIf { it > 0 }
            ?: inputs.weeklyTargetFallback.coerceAtLeast(0)
        val weekMonday = inputs.today
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()
        val weeklyCompleted = weeklyRoutineWorkoutDays
            .asSequence()
            .filter { it in weekMonday..todayEpoch }
            .distinct()
            .count()
            .coerceAtMost(weeklyTarget)
        val weeklyProgress = if (weeklyTarget > 0) {
            weeklyCompleted.toFloat() / weeklyTarget
        } else {
            0f
        }

        val currentStreak = currentStreak(activeDays, todayEpoch)
        val observedBest = longestStreak(activeDays)
        val bestActiveStreak = max(
            inputs.storedBestActiveStreak.coerceAtLeast(0),
            max(observedBest, currentStreak),
        )
        val continuityState = when {
            todayEpoch in activeDays -> TodayContinuityState.ACTIVE_TODAY
            todayEpoch - 1 in activeDays -> TodayContinuityState.CONTINUE_TODAY
            else -> TodayContinuityState.START_TODAY
        }

        val earliestKnownDay = sequenceOf(
            workoutDays.minOrNull(),
            boostDays.minOrNull(),
        ).filterNotNull().minOrNull()
        val trackingStart = (
            inputs.trackingStartEpochDay
                ?: earliestKnownDay
                ?: todayEpoch
            ).coerceAtMost(todayEpoch)

        fun buildDay(date: LocalDate): ConsistencyDay {
            val epochDay = date.toEpochDay()
            val dayWorkouts = workoutsByDay[epochDay].orEmpty()
            val hasWorkout = dayWorkouts.isNotEmpty()
            val hasRoutineWorkout = dayWorkouts.any { it.countsTowardWeeklyProgress }
            val hasBoost = epochDay in boostDays
            val source = when {
                hasWorkout && hasBoost -> ActivitySource.BOTH
                hasWorkout -> ActivitySource.WORKOUT
                hasBoost -> ActivitySource.BOOST
                else -> ActivitySource.NONE
            }
            val beforeTracking = epochDay < trackingStart
            val isTrainingDay = date.dayOfWeek.value in inputs.trainingWeekdays
            val workoutStatus = when {
                hasRoutineWorkout -> WorkoutDayStatus.COMPLETED
                beforeTracking -> WorkoutDayStatus.REST
                isTrainingDay && epochDay < todayEpoch -> WorkoutDayStatus.MISSED
                isTrainingDay -> WorkoutDayStatus.PLANNED
                else -> WorkoutDayStatus.REST
            }
            return ConsistencyDay(
                epochDay = epochDay,
                dayOfMonth = date.dayOfMonth,
                dayOfWeek = date.dayOfWeek.value,
                workoutStatus = workoutStatus,
                activitySource = source,
                workouts = dayWorkouts,
                isToday = epochDay == todayEpoch,
                isBeforeTracking = beforeTracking,
            )
        }

        val currentWeek = (0L..6L).map { offset ->
            buildDay(LocalDate.ofEpochDay(weekMonday).plusDays(offset))
        }
        val firstMonth = YearMonth.from(LocalDate.ofEpochDay(trackingStart))
        val currentMonth = YearMonth.from(inputs.today)
        val calendarMonths = generateMonths(firstMonth, currentMonth)
        val calendarStart = firstMonth.atDay(1)
        val calendarEnd = currentMonth.atEndOfMonth()
        val calendarDays = generateSequence(calendarStart) { current ->
            current.plusDays(1).takeIf { it <= calendarEnd }
        }.map(::buildDay).toList()

        var nextMarked = false
        val milestones = milestoneTargets.map { target ->
            val reached = bestActiveStreak >= target
            val isNext = !reached && !nextMarked
            if (isNext) nextMarked = true
            ConsistencyMilestone(days = target, reached = reached, isNext = isNext)
        }

        return ConsistencyUiState(
            weeklyCompleted = weeklyCompleted,
            weeklyTarget = weeklyTarget,
            weeklyProgress = weeklyProgress.coerceIn(0f, 1f),
            currentStreak = currentStreak,
            bestActiveStreak = bestActiveStreak,
            continuityState = continuityState,
            todayEpochDay = todayEpoch,
            trackingStartEpochDay = trackingStart,
            currentWeek = currentWeek,
            calendarDays = calendarDays,
            calendarMonths = calendarMonths,
            milestones = milestones,
            ready = true,
        )
    }

    internal fun currentStreak(activeDays: Set<Long>, todayEpochDay: Long): Int {
        var cursor = when {
            todayEpochDay in activeDays -> todayEpochDay
            todayEpochDay - 1 in activeDays -> todayEpochDay - 1
            else -> return 0
        }
        var count = 0
        while (cursor in activeDays) {
            count += 1
            cursor -= 1
        }
        return count
    }

    internal fun longestStreak(activeDays: Set<Long>): Int {
        if (activeDays.isEmpty()) return 0
        var best = 0
        var run = 0
        var previous: Long? = null
        activeDays.sorted().forEach { day ->
            run = if (previous != null && day == previous!! + 1) run + 1 else 1
            best = max(best, run)
            previous = day
        }
        return best
    }

    private fun generateMonths(first: YearMonth, last: YearMonth): List<YearMonth> {
        if (first > last) return listOf(last)
        return generateSequence(first) { month ->
            month.plusMonths(1).takeIf { it <= last }
        }.toList()
    }
}
