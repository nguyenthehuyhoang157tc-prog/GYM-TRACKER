package com.hoctin.gymtracker.ui.home

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsistencyCalculatorTest {
    @Test
    fun weeklyProgressColor_usesExactThresholdBands() {
        assertEquals(WeeklyRed, weeklyProgressColor(0f))
        assertEquals(WeeklyRed, weeklyProgressColor(0.249f))
        assertEquals(WeeklyOrange, weeklyProgressColor(0.25f))
        assertEquals(WeeklyOrange, weeklyProgressColor(0.499f))
        assertEquals(WeeklyYellowGreen, weeklyProgressColor(0.50f))
        assertEquals(WeeklyYellowGreen, weeklyProgressColor(0.749f))
        assertEquals(WeeklyGreen, weeklyProgressColor(0.75f))
        assertEquals(WeeklyGreen, weeklyProgressColor(1f))
    }

    @Test
    fun selectedDaySummaryMetrics_keepRequiredOrder_andOmitMeaninglessZeros() {
        val complete = ConsistencyWorkoutEvidence(
            sessionId = 1,
            title = "Pull",
            startedAtMillis = 0L,
            epochDay = 0L,
            totalSets = 19,
            repVolumeKg = 4_916.0,
            durationSeconds = 3_720,
            exerciseCount = 8,
            prCount = 4,
        )
        val nonRep = complete.copy(
            repVolumeKg = null,
            durationSeconds = 180,
            prCount = 0,
        )
        val empty = complete.copy(
            totalSets = 0,
            repVolumeKg = 0.0,
            durationSeconds = 0,
            exerciseCount = 0,
            prCount = 0,
        )

        assertEquals(
            listOf(
                ConsistencyWorkoutSummaryMetric.SETS,
                ConsistencyWorkoutSummaryMetric.REP_VOLUME,
                ConsistencyWorkoutSummaryMetric.DURATION,
                ConsistencyWorkoutSummaryMetric.EXERCISES,
                ConsistencyWorkoutSummaryMetric.PERSONAL_RECORDS,
            ),
            complete.visibleSummaryMetrics(),
        )
        assertEquals(
            listOf(
                ConsistencyWorkoutSummaryMetric.SETS,
                ConsistencyWorkoutSummaryMetric.DURATION,
                ConsistencyWorkoutSummaryMetric.EXERCISES,
            ),
            nonRep.visibleSummaryMetrics(),
        )
        assertTrue(empty.visibleSummaryMetrics().isEmpty())
    }

    @Test
    fun weeklyProgress_countsDistinctWorkoutDates_andExcludesBoost() {
        val today = LocalDate.of(2026, 7, 30)
        val monday = LocalDate.of(2026, 7, 27).toEpochDay()
        val tuesday = LocalDate.of(2026, 7, 28).toEpochDay()
        val wednesday = LocalDate.of(2026, 7, 29).toEpochDay()

        val state = calculate(
            today = today,
            workouts = listOf(
                workout(monday, id = 1),
                workout(monday, id = 2),
                workout(tuesday, id = 3),
            ),
            boostDays = setOf(wednesday),
            trainingWeekdays = setOf(1, 2, 4, 6),
        )

        assertEquals(2, state.weeklyCompleted)
        assertEquals(4, state.weeklyTarget)
        assertEquals(0.5f, state.weeklyProgress)
    }

    @Test
    fun savedOneOff_isActiveStreakEvidence_butNotWeeklyRoutineCompletion() {
        val today = LocalDate.of(2026, 8, 12)
        val oneOff = ConsistencyWorkoutEvidence(
            sessionId = 501,
            title = "Hotel workout",
            startedAtMillis = 10_000L,
            epochDay = today.toEpochDay(),
            countsTowardWeeklyProgress = false,
        )

        val state = calculate(
            workouts = listOf(oneOff),
            trainingWeekdays = setOf(today.dayOfWeek.value),
            today = today,
        )

        assertEquals(0, state.weeklyCompleted)
        assertEquals(0f, state.weeklyProgress)
        assertEquals(1, state.currentStreak)
        assertEquals(TodayContinuityState.ACTIVE_TODAY, state.continuityState)
        assertEquals(ActivitySource.WORKOUT, state.day(today.toEpochDay())?.activitySource)
        assertEquals(WorkoutDayStatus.PLANNED, state.day(today.toEpochDay())?.workoutStatus)
    }

    @Test
    fun weeklyProgress_rollsOverOnMonday() {
        val today = LocalDate.of(2026, 7, 27)
        val sunday = today.minusDays(1).toEpochDay()
        val monday = today.toEpochDay()

        val state = calculate(
            today = today,
            workouts = listOf(workout(sunday), workout(monday, id = 2)),
        )

        assertEquals(1, state.weeklyCompleted)
    }

    @Test
    fun activitySource_unionsWorkoutAndBoost_butCountsOneStreakDay() {
        val today = LocalDate.of(2026, 7, 27)
        val todayEpoch = today.toEpochDay()
        val state = calculate(
            today = today,
            workouts = listOf(
                workout(todayEpoch - 2),
                workout(todayEpoch - 1, id = 2),
                workout(todayEpoch, id = 3),
            ),
            boostDays = setOf(todayEpoch),
        )

        assertEquals(ActivitySource.BOTH, state.day(todayEpoch)?.activitySource)
        assertEquals(3, state.currentStreak)
        assertEquals(TodayContinuityState.ACTIVE_TODAY, state.continuityState)
    }

    @Test
    fun currentStreak_todayPending_anchorsYesterday_withoutBridgingHistoricalGap() {
        val today = LocalDate.of(2026, 7, 27)
        val epoch = today.toEpochDay()
        val state = calculate(
            today = today,
            workouts = listOf(
                workout(epoch - 1),
                workout(epoch - 2, id = 2),
                workout(epoch - 4, id = 3),
            ),
        )

        assertEquals(2, state.currentStreak)
        assertEquals(TodayContinuityState.CONTINUE_TODAY, state.continuityState)
    }

    @Test
    fun currentStreak_resetsAfterTodayAndYesterdayAreBothInactive() {
        val today = LocalDate.of(2026, 7, 27)
        val state = calculate(
            today = today,
            workouts = listOf(workout(today.minusDays(2).toEpochDay())),
        )

        assertEquals(0, state.currentStreak)
        assertEquals(TodayContinuityState.START_TODAY, state.continuityState)
    }

    @Test
    fun bestStreak_isLongestObserved_andNeverDropsBelowStoredBest() {
        val today = LocalDate.of(2026, 7, 27)
        val epoch = today.toEpochDay()
        val observed = calculate(
            today = today,
            workouts = (0L..3L).map { workout(epoch - it, id = it + 1) },
        )
        val retained = calculate(
            today = today,
            workouts = listOf(workout(epoch)),
            storedBest = 9,
        )

        assertEquals(4, observed.bestActiveStreak)
        assertEquals(9, retained.bestActiveStreak)
    }

    @Test
    fun milestones_useBestRecorded_andMarkOnlyNextBoundary() {
        val today = LocalDate.of(2026, 7, 27)
        val atSeven = calculate(today = today, storedBest = 7)
        val atFourteen = calculate(today = today, storedBest = 14)
        val atThirty = calculate(today = today, storedBest = 30)
        val atSixty = calculate(today = today, storedBest = 60)

        assertMilestones(atSeven, reached = setOf(7), next = 14)
        assertMilestones(atFourteen, reached = setOf(7, 14), next = 30)
        assertMilestones(atThirty, reached = setOf(7, 14, 30), next = 60)
        assertMilestones(atSixty, reached = setOf(7, 14, 30, 60), next = null)
    }

    @Test
    fun calendar_distinguishesBeforeTracking_missed_planned_rest_andCompleted() {
        val today = LocalDate.of(2026, 7, 15)
        val trackingStart = LocalDate.of(2026, 7, 8).toEpochDay()
        val completedUnscheduled = LocalDate.of(2026, 7, 14).toEpochDay()
        val state = calculate(
            today = today,
            workouts = listOf(workout(completedUnscheduled)),
            trainingWeekdays = setOf(1, 3, 5),
            trackingStart = trackingStart,
        )

        val beforeTracking = state.day(LocalDate.of(2026, 7, 6).toEpochDay())!!
        val missed = state.day(LocalDate.of(2026, 7, 13).toEpochDay())!!
        val plannedToday = state.day(today.toEpochDay())!!
        val plannedFuture = state.day(LocalDate.of(2026, 7, 17).toEpochDay())!!
        val rest = state.day(LocalDate.of(2026, 7, 16).toEpochDay())!!
        val completed = state.day(completedUnscheduled)!!

        assertTrue(beforeTracking.isBeforeTracking)
        assertEquals(WorkoutDayStatus.REST, beforeTracking.workoutStatus)
        assertEquals(WorkoutDayStatus.MISSED, missed.workoutStatus)
        assertEquals(WorkoutDayStatus.PLANNED, plannedToday.workoutStatus)
        assertEquals(WorkoutDayStatus.PLANNED, plannedFuture.workoutStatus)
        assertEquals(WorkoutDayStatus.REST, rest.workoutStatus)
        assertEquals(WorkoutDayStatus.COMPLETED, completed.workoutStatus)
    }

    @Test
    fun calendar_spansTrackingStartMonthThroughCurrentMonth() {
        val today = LocalDate.of(2026, 7, 27)
        val state = calculate(
            today = today,
            trackingStart = LocalDate.of(2026, 5, 31).toEpochDay(),
        )

        assertEquals(
            listOf(
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 6),
                YearMonth.of(2026, 7),
            ),
            state.calendarMonths,
        )
        assertEquals(LocalDate.of(2026, 5, 1).toEpochDay(), state.calendarDays.first().epochDay)
        assertEquals(LocalDate.of(2026, 7, 31).toEpochDay(), state.calendarDays.last().epochDay)
    }

    @Test
    fun presentationStore_bestIsMaxOnly() {
        var persistedBest = 3
        var lastCelebrated: Long? = null
        val store = ConsistencyPresentationStateStore(
            readBest = { persistedBest },
            writeBest = { persistedBest = it },
            readLastCelebrated = { lastCelebrated },
            writeLastCelebrated = { lastCelebrated = it },
        )

        store.updateBestActiveStreak(2)
        assertEquals(3, persistedBest)
        store.updateBestActiveStreak(8)
        assertEquals(8, persistedBest)
        store.updateBestActiveStreak(5)
        assertEquals(8, persistedBest)
    }

    @Test
    fun celebration_isConsumedOncePerDay_forWorkoutThenBoost() {
        var persistedBest = 0
        var lastCelebrated: Long? = null
        val today = LocalDate.of(2026, 7, 27).toEpochDay()
        val store = ConsistencyPresentationStateStore(
            readBest = { persistedBest },
            writeBest = { persistedBest = it },
            readLastCelebrated = { lastCelebrated },
            writeLastCelebrated = { lastCelebrated = it },
        )

        assertFalse(store.consumeCelebration(today, isActiveToday = false))
        assertTrue(store.consumeCelebration(today, isActiveToday = true))
        assertFalse(store.consumeCelebration(today, isActiveToday = true))
        assertTrue(store.consumeCelebration(today + 1, isActiveToday = true))
    }

    private fun calculate(
        today: LocalDate,
        workouts: List<ConsistencyWorkoutEvidence> = emptyList(),
        boostDays: Set<Long> = emptySet(),
        trainingWeekdays: Set<Int> = setOf(1, 2, 4, 6),
        trackingStart: Long? = null,
        storedBest: Int = 0,
    ): ConsistencyUiState = ConsistencyCalculator.calculate(
        ConsistencyInputs(
            workouts = workouts,
            boostDays = boostDays,
            trainingWeekdays = trainingWeekdays,
            weeklyTargetFallback = 4,
            trackingStartEpochDay = trackingStart,
            storedBestActiveStreak = storedBest,
            today = today,
        ),
    )

    private fun workout(
        epochDay: Long,
        id: Long = 1,
    ): ConsistencyWorkoutEvidence = ConsistencyWorkoutEvidence(
        sessionId = id,
        title = "Push",
        startedAtMillis = epochDay * 86_400_000L,
        epochDay = epochDay,
    )

    private fun assertMilestones(
        state: ConsistencyUiState,
        reached: Set<Int>,
        next: Int?,
    ) {
        assertEquals(reached, state.milestones.filter { it.reached }.map { it.days }.toSet())
        assertEquals(next, state.milestones.singleOrNull { it.isNext }?.days)
    }
}
