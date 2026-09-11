package com.hoctin.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * MUSCLE-3B.1 — pure-JVM checks for the additive `workout_sessions.durationSeconds` column (DB v7).
 * (The v6→v7 migration + export/import round-trip are Room/DAO-backed and verified by build +
 * on-device upgrade QA, mirroring the readiness v3→v4 and duration/distance v4→v5 approach.)
 */
class SessionDurationFoundationTest {

    @Test
    fun newSession_durationSeconds_defaultsToNull_preFeatureRowsShowNoDuration() {
        val session = WorkoutSessionEntity(
            id = 1, dayId = null, dayNameSnapshot = "Push", dateEpochDay = 20000, startedAtMillis = 0L,
        )
        assertNull(session.durationSeconds)
    }

    @Test
    fun session_carriesPersistedWallClockDuration() {
        val session = WorkoutSessionEntity(
            id = 1, dayId = null, dayNameSnapshot = "Push", dateEpochDay = 20000, startedAtMillis = 0L,
            durationSeconds = 98 * 60,
        )
        assertEquals(98 * 60, session.durationSeconds)
    }
}
