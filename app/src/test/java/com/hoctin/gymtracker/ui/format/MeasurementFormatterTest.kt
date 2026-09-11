package com.hoctin.gymtracker.ui.format

import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.DistanceUnitSystem.IMPERIAL
import com.hoctin.gymtracker.data.DistanceUnitSystem.METRIC
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.catalog.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DB-6C: the unified formatter + set-display + tracking-type field-config contract. */
class MeasurementFormatterTest {

    // ---- structured value+unit (no raw concatenation) -------------------------------------------

    @Test
    fun weightAndDuration() {
        assertEquals(Measurement("80", "kg"), MeasurementFormatter.weight(80.0))
        assertEquals(Measurement("82.5", "kg"), MeasurementFormatter.weight(82.5))
        // Duration is self-labeled (no unit).
        assertEquals(Measurement("01:30", ""), MeasurementFormatter.duration(90))
        assertEquals(Measurement("00:45", ""), MeasurementFormatter.duration(45))
        assertEquals("01:01:01", MeasurementFormatter.duration(3661).value)
    }

    @Test
    fun distanceRespectsUnitSystem() {
        assertEquals(Measurement("125", "m"), MeasurementFormatter.distance(125.0, METRIC))
        assertEquals(Measurement("1.5", "km"), MeasurementFormatter.distance(1500.0, METRIC))
        // 30.48 m = 100 ft; 2000 m ≈ 1.2 mi (well past the mile boundary).
        assertEquals(Measurement("100", "ft"), MeasurementFormatter.distance(30.48, IMPERIAL))
        assertEquals(Measurement("1.2", "mi"), MeasurementFormatter.distance(2000.0, IMPERIAL))
    }

    @Test
    fun velocityIsFutureReady() {
        assertEquals(Measurement("1.2", "m/s"), MeasurementFormatter.velocity(1.2, METRIC))
        assertEquals(Measurement("4.3", "km/h"), MeasurementFormatter.velocity(1.2, IMPERIAL))
    }

    @Test
    fun inlineCombinesValueAndUnit_unitMayBeEmpty() {
        assertEquals("80 kg", Measurement("80", "kg").inline())
        assertEquals("01:30", Measurement("01:30", "").inline())
    }

    @Test
    fun parsersRoundTripUserInput() {
        assertEquals(3, MeasurementFormatter.parseDurationSeconds("3"))
        assertEquals(55, MeasurementFormatter.parseDurationSeconds("55"))
        assertEquals(90, MeasurementFormatter.parseDurationSeconds("130"))
        assertEquals(1812, MeasurementFormatter.parseDurationSeconds("3012"))
        assertEquals(3600, MeasurementFormatter.parseDurationSeconds("010000"))
        assertEquals(90, MeasurementFormatter.parseDurationSeconds("1:30"))
        assertEquals(90, MeasurementFormatter.parseDurationSeconds("01:30"))
        assertEquals(3600, MeasurementFormatter.parseDurationSeconds("1:00:00"))
        assertNull(MeasurementFormatter.parseDurationSeconds(""))
        assertNull(MeasurementFormatter.parseDurationSeconds("abc"))
        assertNull(MeasurementFormatter.parseDurationSeconds("60"))
        assertNull(MeasurementFormatter.parseDurationSeconds("199"))
        assertNull(MeasurementFormatter.parseDurationSeconds("1:99"))
        assertNull(MeasurementFormatter.parseDurationSeconds("90:00"))
        assertNull(MeasurementFormatter.parseDurationSeconds("1:02:99"))
        assertNull(MeasurementFormatter.parseDurationSeconds("1000000"))
        assertEquals(20.0, MeasurementFormatter.parseDistanceMeters("20", METRIC)!!, 0.001)
        // Imperial entry is in ft → stored as meters.
        assertEquals(6.096, MeasurementFormatter.parseDistanceMeters("20", IMPERIAL)!!, 0.01)
    }

    // ---- per-type set display (hidden reps=0; load · distance · duration order) ------------------

    @Test
    fun durationNormalization_formatsValidDraftInput() {
        assertEquals("00:03", MeasurementFormatter.normalizeDurationInput("3"))
        assertEquals("01:30", MeasurementFormatter.normalizeDurationInput("130"))
        assertEquals("30:12", MeasurementFormatter.normalizeDurationInput("3012"))
        assertEquals("01:00:00", MeasurementFormatter.normalizeDurationInput("010000"))
        assertEquals("01:00:00", MeasurementFormatter.normalizeDurationInput("1:00:00"))
        assertNull(MeasurementFormatter.normalizeDurationInput("1:99"))
    }

    private fun set(weight: Double, reps: Int, durationSeconds: Int? = null, distanceMeters: Double? = null) =
        LoggedSetEntity(
            sessionId = 1, exerciseId = 1, exerciseNameSnapshot = "x", setNumber = 1,
            weight = weight, reps = reps, rir = 0, isPrWeight = false, isPrReps = false,
            durationSeconds = durationSeconds, distanceMeters = distanceMeters,
        )

    @Test
    fun setDisplay_perType_inlineStrings() {
        // TIME_ONLY (plank): just the duration, no fake reps.
        assertEquals("01:30", SetDisplay.nonRepInline(set(0.0, 0, durationSeconds = 90), METRIC))
        // DISTANCE_LOAD (carry): "40 kg • 20 m".
        assertEquals("40 kg • 20 m", SetDisplay.nonRepInline(set(40.0, 0, distanceMeters = 20.0), METRIC))
        // TIME_DISTANCE_LOAD (sled): "80 kg • 30 m • 00:40".
        assertEquals(
            "80 kg • 30 m • 00:40",
            SetDisplay.nonRepInline(set(80.0, 0, durationSeconds = 40, distanceMeters = 30.0), METRIC),
        )
    }

    @Test
    fun setDisplay_isNonRep_classifiesByStoredFields() {
        assertTrue(SetDisplay.isNonRep(set(0.0, 0, durationSeconds = 30)))
        assertTrue(SetDisplay.isNonRep(set(40.0, 0, distanceMeters = 20.0)))
        assertFalse(SetDisplay.isNonRep(set(50.0, 10))) // normal reps/kg set
    }

    @Test
    fun setDisplay_summaryLine_hidesSentinelForTimeOnly() {
        val line = SetDisplay.summaryLine(
            setCount = 3,
            totalReps = 0,
            totalVolume = 0.0,
            durationSeconds = 165,
            distanceMeters = 0.0,
            hasRepSets = false,
            system = METRIC,
            targetSets = 3,
        )

        assertEquals("3/3 sets \u00b7 02:45 duration", line)
        assertFalse(line.contains("0 reps"))
        assertFalse(line.contains("0 kg"))
    }

    @Test
    fun setDisplay_summaryLine_mixedWorkoutIncludesDurationAndRepWork() {
        val line = SetDisplay.summaryLine(
            setCount = 4,
            totalReps = 10,
            totalVolume = 500.0,
            durationSeconds = 55,
            distanceMeters = 0.0,
            hasRepSets = true,
            system = METRIC,
        )

        assertEquals("4 sets \u00b7 10 reps \u00b7 500 kg \u00b7 00:55 duration", line)
    }

    @Test
    fun setDisplay_summaryLine_distanceAndDurationAvoidsFakeRepWork() {
        val line = SetDisplay.summaryLine(
            setCount = 3,
            totalReps = 0,
            totalVolume = 0.0,
            durationSeconds = 120,
            distanceMeters = 60.0,
            hasRepSets = false,
            system = METRIC,
        )

        assertEquals("3 sets \u00b7 02:00 duration \u00b7 60 m distance", line)
        assertFalse(line.contains("0 reps"))
        assertFalse(line.contains("0 kg"))
    }

    @Test
    fun setDisplay_targetLabel_tracksNonRepTypes() {
        assertEquals("3 sets \u00b7 8-12 reps", SetDisplay.targetLabel(3, 8, 12, TrackingType.REPS_WEIGHT))
        // DB-6F: the duration field token is the concise "time" (matches the Log "Time" field label).
        assertEquals("3 sets \u00b7 time", SetDisplay.targetLabel(3, 8, 12, TrackingType.TIME_ONLY))
        assertEquals("3 sets \u00b7 load + distance", SetDisplay.targetLabel(3, 8, 12, TrackingType.DISTANCE_LOAD))
        assertEquals(
            "3 sets \u00b7 load + distance + time",
            SetDisplay.targetLabel(3, 0, 0, TrackingType.TIME_DISTANCE_LOAD),
        )
    }

    // ---- tracking-type field config (drives the Log UI) -----------------------------------------

    @Test
    fun trackingType_fieldConfig() {
        TrackingType.REPS_WEIGHT.let {
            assertTrue(it.isRepBased && it.showsReps && it.showsRir && it.showsLoad)
            assertFalse(it.showsDuration || it.showsDistance)
        }
        TrackingType.TIME_ONLY.let {
            assertFalse(it.isRepBased || it.showsReps || it.showsRir || it.showsLoad || it.showsDistance)
            assertTrue(it.showsDuration)
        }
        TrackingType.DISTANCE_LOAD.let { assertTrue(it.showsLoad && it.showsDistance && !it.showsDuration) }
        TrackingType.TIME_DISTANCE_LOAD.let { assertTrue(it.showsLoad && it.showsDistance && it.showsDuration) }
    }

    @Test
    fun distanceUnitSystem_storageRoundTrip() {
        assertEquals(METRIC, DistanceUnitSystem.fromStorageValue("metric"))
        assertEquals(IMPERIAL, DistanceUnitSystem.fromStorageValue("imperial"))
        assertEquals(METRIC, DistanceUnitSystem.fromStorageValue(null)) // safe default
        assertEquals("imperial", IMPERIAL.storageValue)
    }
}
