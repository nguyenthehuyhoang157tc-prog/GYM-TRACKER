package com.hoctin.gymtracker.ui.log

import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.catalog.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogWorkoutTrackingInputTest {

    @Test
    fun timeOnlyDraft_acceptsSecondsAndMmSs() {
        val seconds = parseSetDraftInput(
            draft = SetDraft(duration = "55"),
            type = TrackingType.TIME_ONLY,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )
        val mmss = parseSetDraftInput(
            draft = SetDraft(duration = "01:30"),
            type = TrackingType.TIME_ONLY,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )
        val digits = parseSetDraftInput(
            draft = SetDraft(duration = "3012"),
            type = TrackingType.TIME_ONLY,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )
        val hours = parseSetDraftInput(
            draft = SetDraft(duration = "010000"),
            type = TrackingType.TIME_ONLY,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )

        assertEquals(55, seconds?.durationSeconds)
        assertEquals(90, mmss?.durationSeconds)
        assertEquals(1812, digits?.durationSeconds)
        assertEquals(3600, hours?.durationSeconds)
        assertEquals(0, seconds?.reps)
        assertEquals(0.0, seconds?.weight ?: -1.0, 0.0)
    }

    @Test
    fun timeOnlyDraft_rejectsInvalidDuration() {
        assertNull(
            parseSetDraftInput(
                draft = SetDraft(duration = "1:99"),
                type = TrackingType.TIME_ONLY,
                distanceUnitSystem = DistanceUnitSystem.METRIC,
                notes = null,
            ),
        )
    }

    @Test
    fun distanceLoadDraft_acceptsLoadAndDistance() {
        val parsed = parseSetDraftInput(
            draft = SetDraft(weight = "40", distance = "20"),
            type = TrackingType.DISTANCE_LOAD,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = "carry",
        )

        assertEquals(40.0, parsed?.weight ?: -1.0, 0.0)
        assertEquals(20.0, parsed?.distanceMeters ?: -1.0, 0.001)
        assertEquals(0, parsed?.reps)
        assertEquals(0, parsed?.rir)
        assertNull(parsed?.durationSeconds)
        assertEquals("carry", parsed?.notes)
    }

    @Test
    fun timeDistanceLoadDraft_acceptsLoadDistanceAndDuration() {
        val parsed = parseSetDraftInput(
            draft = SetDraft(weight = "80", distance = "30", duration = "40"),
            type = TrackingType.TIME_DISTANCE_LOAD,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )

        assertEquals(80.0, parsed?.weight ?: -1.0, 0.0)
        assertEquals(30.0, parsed?.distanceMeters ?: -1.0, 0.001)
        assertEquals(40, parsed?.durationSeconds)
        assertEquals(0, parsed?.reps)
        assertEquals(0, parsed?.rir)
    }

    @Test
    fun repDraftBehaviorUnchanged() {
        val parsed = parseSetDraftInput(
            draft = SetDraft(weight = "80", reps = "10", rir = "2"),
            type = TrackingType.REPS_WEIGHT,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = "top set",
        )

        assertEquals(80.0, parsed?.weight ?: -1.0, 0.0)
        assertEquals(10, parsed?.reps)
        assertEquals(2, parsed?.rir)
        assertEquals("top set", parsed?.notes)
    }

    @Test
    fun bodyweightRepDraft_acceptsBlankExternalLoad() {
        val parsed = parseSetDraftInput(
            draft = SetDraft(reps = "12", rir = "2"),
            type = TrackingType.BODYWEIGHT_REPS,
            distanceUnitSystem = DistanceUnitSystem.METRIC,
            notes = null,
        )

        assertEquals(0.0, parsed?.weight ?: -1.0, 0.0)
        assertEquals(12, parsed?.reps)
        assertEquals(2, parsed?.rir)
    }

    @Test
    fun weightedRepDraft_stillRequiresExternalLoad() {
        assertNull(
            parseSetDraftInput(
                draft = SetDraft(reps = "12", rir = "2"),
                type = TrackingType.REPS_WEIGHT,
                distanceUnitSystem = DistanceUnitSystem.METRIC,
                notes = null,
            ),
        )
    }
}
