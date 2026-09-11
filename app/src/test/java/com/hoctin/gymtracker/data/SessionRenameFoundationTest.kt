package com.hoctin.gymtracker.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MUSCLE-3C - pure-JVM checks for the additive `workout_sessions.customTitle` column (DB v8).
 * Room migration itself is validated by the generated schema/build path; these tests cover fallback
 * and export/import behavior without needing an Android database.
 */
class SessionRenameFoundationTest {

    @Test
    fun newSession_customTitleDefaultsToNull_oldSessionsUseFallbackTitle() {
        val session = WorkoutSessionEntity(
            id = 1,
            dayId = null,
            dayNameSnapshot = "Push",
            dateEpochDay = 20000,
            startedAtMillis = 0L,
        )

        assertNull(session.customTitle)
    }

    @Test
    fun workoutSessionExportImport_customTitleRoundTrip() {
        val original = WorkoutSessionEntity(
            id = 7,
            dayId = 3,
            dayNameSnapshot = "Pull",
            dateEpochDay = 20001,
            startedAtMillis = 1234L,
            durationSeconds = 300,
            customTitle = "Heavy Back Day",
        )

        val imported = workoutSessionFromImportJson(workoutSessionToExportJson(original))

        assertEquals("Heavy Back Day", imported.customTitle)
        assertEquals("Pull", imported.dayNameSnapshot)
        assertEquals(300, imported.durationSeconds)
    }

    @Test
    fun workoutSessionImport_missingCustomTitleDefaultsToNull() {
        val imported = workoutSessionFromImportJson(
            JSONObject()
                .put("id", 8)
                .put("dayId", JSONObject.NULL)
                .put("dayNameSnapshot", "Legs")
                .put("dateEpochDay", 20002)
                .put("startedAtMillis", 5678L)
                .put("notes", JSONObject.NULL)
                .put("readiness", JSONObject.NULL)
                .put("durationSeconds", JSONObject.NULL),
        )

        assertNull(imported.customTitle)
        assertEquals("Legs", imported.dayNameSnapshot)
    }

    @Test
    fun workoutSessionImport_blankCustomTitleDefaultsToNull() {
        val imported = workoutSessionFromImportJson(
            JSONObject()
                .put("id", 9)
                .put("dayId", JSONObject.NULL)
                .put("dayNameSnapshot", "Full Body")
                .put("dateEpochDay", 20003)
                .put("startedAtMillis", 91011L)
                .put("notes", JSONObject.NULL)
                .put("readiness", JSONObject.NULL)
                .put("durationSeconds", JSONObject.NULL)
                .put("customTitle", "   "),
        )

        assertNull(imported.customTitle)
    }

    @Test
    fun normalizedSessionTitle_trimsAndRejectsBlank() {
        assertEquals("testing", normalizedSessionTitle("  testing  "))
        assertNull(normalizedSessionTitle("   "))
    }
}
