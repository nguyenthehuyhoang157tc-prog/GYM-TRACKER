package com.hoctin.gymtracker.ui.settings

import com.hoctin.gymtracker.ui.history.HistoryExercise
import com.hoctin.gymtracker.ui.history.HistoryMonth
import com.hoctin.gymtracker.ui.history.HistorySession
import com.hoctin.gymtracker.ui.history.HistoryUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsModelsTest {
    @Test
    fun hubExposesEveryDeclaredDestinationExactlyOnce() {
        val items = profileHubSections().flatMap { it.items }

        assertEquals(ProfileDestination.entries.size, items.size)
        assertEquals(ProfileDestination.entries.toSet(), items.map { it.destination }.toSet())
        assertEquals(items.size, items.map { it.destination }.distinct().size)
        assertTrue(items.all { it.title.isNotBlank() && it.supportingText.isNotBlank() })
    }

    @Test
    fun notificationsCommunicatesUnavailableInsteadOfPretendingToBeFunctional() {
        val notification = profileHubSections()
            .flatMap { it.items }
            .single { it.destination == ProfileDestination.NOTIFICATIONS }

        assertEquals("Unavailable", notification.status)
    }

    @Test
    fun insightsAggregateCanonicalHistoryWithoutInventingNonRepVolume() {
        val firstDay = LocalDate.of(2026, 7, 1).toEpochDay()
        val state = HistoryUiState(
            totalSessions = 2,
            months = listOf(
                HistoryMonth(
                    year = 2026,
                    month = 7,
                    yearLabel = "2026",
                    monthLabel = "JULY",
                    sessions = listOf(
                        session(
                            id = 2,
                            day = firstDay + 2,
                            totalSets = 4,
                            totalVolume = 420.0,
                            duration = 1_800,
                            prCount = 2,
                            exercises = listOf(exercise("Bench Press", reps = 32, volume = 420.0)),
                        ),
                        session(
                            id = 1,
                            day = firstDay,
                            totalSets = 3,
                            totalVolume = 0.0,
                            duration = 600,
                            prCount = 0,
                            exercises = listOf(exercise("Farmer Carry", reps = 0, volume = 0.0, nonRep = true)),
                        ),
                    ),
                ),
            ),
        )

        val summary = profileInsightsOf(state)

        assertEquals(2, summary.totalWorkouts)
        assertEquals(7, summary.totalSets)
        assertEquals(420.0, summary.totalVolumeKg, 0.0)
        assertEquals(32, summary.totalReps)
        assertEquals(2, summary.personalRecords)
        assertEquals(1_200, summary.averageDurationSeconds)
        assertEquals("Bench Press", summary.favoriteExercise)
        assertEquals("Jul 1, 2026", summary.firstSavedWorkout)
    }

    @Test
    fun emptyHistoryReturnsHonestEmptySummary() {
        val summary = profileInsightsOf(HistoryUiState())

        assertEquals(0, summary.totalWorkouts)
        assertEquals(0.0, summary.totalVolumeKg, 0.0)
        assertNull(summary.averageDurationSeconds)
        assertNull(summary.favoriteExercise)
        assertNull(summary.firstSavedWorkout)
    }

    private fun session(
        id: Long,
        day: Long,
        totalSets: Int,
        totalVolume: Double,
        duration: Int,
        prCount: Int,
        exercises: List<HistoryExercise>,
    ) = HistorySession(
        id = id,
        dayName = "Workout",
        dateEpochDay = day,
        startedAtMillis = 0L,
        totalSets = totalSets,
        totalVolume = totalVolume,
        durationSeconds = 0,
        distanceMeters = 0.0,
        hasRepSets = exercises.any { it.hasRepSets },
        prCount = prCount,
        exerciseCount = exercises.size,
        exercises = exercises,
        wallDurationSeconds = duration,
    )

    private fun exercise(
        name: String,
        reps: Int,
        volume: Double,
        nonRep: Boolean = false,
    ) = HistoryExercise(
        name = name,
        setCount = 1,
        totalReps = reps,
        totalVolume = volume,
        durationSeconds = if (nonRep) 600 else 0,
        distanceMeters = if (nonRep) 100.0 else 0.0,
        hasRepSets = !nonRep,
        hasPr = false,
        sets = emptyList(),
        isNonRep = nonRep,
    )
}
