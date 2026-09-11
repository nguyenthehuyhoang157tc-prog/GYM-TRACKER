package com.hoctin.gymtracker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsistencyPresentationTest {
    @Test
    fun homeStrip_workoutBoostAndBothShareOneActiveVisual() {
        listOf(
            ActivitySource.WORKOUT,
            ActivitySource.BOOST,
            ActivitySource.BOTH,
        ).forEach { source ->
            assertEquals(
                HomeStreakVisualState.ACTIVE,
                homeStreakVisualState(
                    activitySource = source,
                    isToday = false,
                    isBeforeTracking = false,
                ),
            )
        }
    }

    @Test
    fun homeStrip_pastInactiveTodayPendingAndBeforeTrackingStayDistinct() {
        assertEquals(
            HomeStreakVisualState.MISSED,
            homeStreakVisualState(
                activitySource = ActivitySource.NONE,
                isToday = false,
                isBeforeTracking = false,
            ),
        )
        assertEquals(
            HomeStreakVisualState.TODAY_PENDING,
            homeStreakVisualState(
                activitySource = ActivitySource.NONE,
                isToday = true,
                isBeforeTracking = false,
            ),
        )
        assertEquals(
            HomeStreakVisualState.BEFORE_TRACKING,
            homeStreakVisualState(
                activitySource = ActivitySource.NONE,
                isToday = false,
                isBeforeTracking = true,
            ),
        )
    }

    @Test
    fun calendarVisual_usesWorkoutScheduleEvenForBoostOnlyDay() {
        val boostOnlyRestDay = day(
            workoutStatus = WorkoutDayStatus.REST,
            activitySource = ActivitySource.BOOST,
        )
        val boostOnlyPlannedDay = day(
            workoutStatus = WorkoutDayStatus.PLANNED,
            activitySource = ActivitySource.BOOST,
        )

        assertEquals(ScheduleVisualState.REST, scheduleVisualState(boostOnlyRestDay))
        assertEquals(ScheduleVisualState.PLANNED, scheduleVisualState(boostOnlyPlannedDay))
    }

    @Test
    fun calendarVisual_beforeTrackingNeverLooksMissed() {
        val beforeTracking = day(
            workoutStatus = WorkoutDayStatus.MISSED,
            activitySource = ActivitySource.NONE,
            isBeforeTracking = true,
        )

        assertEquals(ScheduleVisualState.BEFORE_TRACKING, scheduleVisualState(beforeTracking))
    }

    private fun day(
        workoutStatus: WorkoutDayStatus,
        activitySource: ActivitySource,
        isBeforeTracking: Boolean = false,
    ) = ConsistencyDay(
        epochDay = 1L,
        dayOfMonth = 2,
        dayOfWeek = 2,
        workoutStatus = workoutStatus,
        activitySource = activitySource,
        isBeforeTracking = isBeforeTracking,
    )
}
