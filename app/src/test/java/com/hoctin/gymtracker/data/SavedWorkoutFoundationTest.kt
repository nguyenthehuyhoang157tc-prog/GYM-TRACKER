package com.hoctin.gymtracker.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedWorkoutFoundationTest {
    @Test
    fun historicalSessionImport_defaultsToRoutine() {
        val imported = workoutSessionFromImportJson(
            JSONObject()
                .put("id", 1L)
                .put("dayId", 2L)
                .put("dayNameSnapshot", "Push")
                .put("dateEpochDay", 20_000L)
                .put("startedAtMillis", 100L),
        )

        assertEquals(WorkoutSessionSource.ROUTINE.name, imported.sourceType)
        assertNull(imported.savedWorkoutId)
        assertTrue(imported.countsTowardWeeklyProgress())
        assertTrue(imported.countsTowardActiveStreak())
    }

    @Test
    fun savedOneOffSessionProvenance_roundTripsAndSeparatesWeeklyFromActive() {
        val original = WorkoutSessionEntity(
            id = 3L,
            dayId = null,
            dayNameSnapshot = "Hotel workout",
            dateEpochDay = 20_001L,
            startedAtMillis = 200L,
            sourceType = WorkoutSessionSource.SAVED_ONE_OFF.name,
            savedWorkoutId = 9L,
        )
        val imported = workoutSessionFromImportJson(workoutSessionToExportJson(original))

        assertEquals(original, imported)
        assertFalse(imported.countsTowardWeeklyProgress())
        assertTrue(imported.countsTowardActiveStreak())
    }

    @Test
    fun savedWorkoutDefinition_roundTripsWithoutCatalogDuplication() {
        val workout = SavedWorkoutEntity(4L, "Travel", 10L, 20L, 0)
        val exercise = SavedWorkoutExerciseEntity(
            id = 5L,
            savedWorkoutId = 4L,
            catalogSlug = "bench-press",
            canonicalExerciseKey = "catalog:bench-press",
            exerciseNameSnapshot = "Bench Press",
            targetSets = 3,
            targetMinReps = 6,
            targetMaxReps = 8,
            sortOrder = 0,
        )

        assertEquals(workout, savedWorkoutFromImportJson(savedWorkoutToExportJson(workout)))
        assertEquals(
            exercise,
            savedWorkoutExerciseFromImportJson(savedWorkoutExerciseToExportJson(exercise)),
        )
    }
}
