package com.hoctin.gymtracker.ui.log

import com.hoctin.gymtracker.data.catalog.ProgressionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestSuggestionHeuristicTest {
    @Test
    fun seventyFiveSecondsRendersAsOneMinuteFifteen() {
        assertEquals("1:15", formatRestDuration(75))
    }

    @Test
    fun compoundUsesLongerRangeThanIsolationAtSameEffort() {
        val isolation = suggestion(ProgressionType.ISOLATION_SLOW, rir = 2)
        val compound = suggestion(ProgressionType.COMPOUND_LOAD, rir = 2)
        assertTrue(compound > isolation)
    }

    @Test
    fun rirZeroGetsLongerRestThanRirTwo() {
        val rirTwo = suggestion(ProgressionType.COMPOUND_LOAD, rir = 2)
        val rirZero = suggestion(ProgressionType.COMPOUND_LOAD, rir = 0)
        assertTrue(rirZero > rirTwo)
    }

    @Test
    fun clearRepDropMovesSuggestionUpOneTier() {
        val stable = suggestion(ProgressionType.BODYWEIGHT, rir = 2, reps = 10, previous = 10)
        val fatigued = suggestion(ProgressionType.BODYWEIGHT, rir = 2, reps = 8, previous = 10)
        assertTrue(fatigued > stable)
    }

    @Test
    fun extraSetSurchargeCapsAtFortyFiveSeconds() {
        val programmed = suggestion(ProgressionType.ISOLATION_SLOW, rir = 2, ordinal = 3, target = 3)
        val manyExtras = suggestion(ProgressionType.ISOLATION_SLOW, rir = 2, ordinal = 10, target = 3)
        assertEquals(45, manyExtras - programmed)
    }

    private fun suggestion(
        type: ProgressionType,
        rir: Int,
        reps: Int = 10,
        previous: Int = 10,
        ordinal: Int = 1,
        target: Int = 3,
    ) = RestSuggestionHeuristic.seconds(
        RestSuggestionInput(type, rir, reps, previous, ordinal, target),
    )
}
