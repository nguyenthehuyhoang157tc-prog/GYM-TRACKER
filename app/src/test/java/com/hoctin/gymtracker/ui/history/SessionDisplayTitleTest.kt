package com.hoctin.gymtracker.ui.history

import com.hoctin.gymtracker.data.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDisplayTitleTest {

    @Test
    fun displayTitle_oldSessionFallsBackToDayNameSnapshot() {
        val session = WorkoutSessionEntity(
            id = 1,
            dayId = null,
            dayNameSnapshot = "Push",
            dateEpochDay = 20000,
            startedAtMillis = 0L,
        )

        assertEquals("Push", sessionDisplayTitle(session))
    }

    @Test
    fun displayTitle_usesRenamedSessionTitle() {
        val session = WorkoutSessionEntity(
            id = 1,
            dayId = null,
            dayNameSnapshot = "Push",
            dateEpochDay = 20000,
            startedAtMillis = 0L,
            customTitle = "Upper Pump",
        )

        assertEquals("Upper Pump", sessionDisplayTitle(session))
    }
}
