package com.hoctin.gymtracker.data

import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.ui.totalVolume
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DB-6B multi-tracking foundation — pure-JVM checks that the additive data-model changes are safe and
 * the existing reps/kg volume math is unchanged. (Room migration + export/import round-trip are
 * Room/DAO-backed and verified by build + on-device upgrade QA, mirroring the readiness v3→v4 approach;
 * they are not part of this JVM test harness.)
 */
class MultiTrackingFoundationTest {

    private fun session(vararg sets: LoggedSetEntity) = SessionWithSets(
        session = WorkoutSessionEntity(
            id = 1, dayId = null, dayNameSnapshot = "Push", dateEpochDay = 20000, startedAtMillis = 0L,
        ),
        sets = sets.toList(),
    )

    private fun set(
        weight: Double, reps: Int, durationSeconds: Int? = null, distanceMeters: Double? = null,
    ) = LoggedSetEntity(
        sessionId = 1, exerciseId = 1, exerciseNameSnapshot = "x", setNumber = 1,
        weight = weight, reps = reps, rir = 0, isPrWeight = false, isPrReps = false,
        durationSeconds = durationSeconds, distanceMeters = distanceMeters,
    )

    @Test
    fun newLoggedSet_durationAndDistance_defaultToNull() {
        val s = LoggedSetEntity(
            sessionId = 1, exerciseId = 1, exerciseNameSnapshot = "x", setNumber = 1,
            weight = 50.0, reps = 10, rir = 2, isPrWeight = false, isPrReps = false,
        )
        assertNull(s.durationSeconds)
        assertNull(s.distanceMeters)
    }

    @Test
    fun repsZeroSet_contributesZeroToVolume_existingMathUnchanged() {
        // A time/distance set uses reps=0 (sentinel); a loaded carry still records weight.
        val timeDistanceSet = set(weight = 80.0, reps = 0, durationSeconds = 30, distanceMeters = 20.0)
        val normalSet = set(weight = 50.0, reps = 10)
        // reps=0 -> weight*reps = 0, so it adds nothing to kg-volume.
        assertEquals(0.0, session(timeDistanceSet).totalVolume(), 0.0)
        // Mixed session: only the rep-based set counts (50*10 = 500).
        assertEquals(500.0, session(normalSet, timeDistanceSet).totalVolume(), 0.0)
        // Existing rep-based volume is identical to before (no behavior change).
        assertEquals(1100.0, session(set(50.0, 10), set(60.0, 10)).totalVolume(), 0.0)
    }

    @Test
    fun draftSet_durationAndDistance_defaultToEmpty() {
        val d = DraftSet(weight = "50", reps = "10", rir = "2", notes = "")
        assertEquals("", d.duration)
        assertEquals("", d.distance)
        val d2 = d.copy(duration = "30", distance = "20")
        assertEquals("30", d2.duration)
        assertEquals("20", d2.distance)
    }

    @Test
    fun draftJson_durationRoundTrip() {
        val raw = serializeDraftEntries(
            mapOf(
                7L to listOf(
                    DraftSet(weight = "40", reps = "", rir = "0", notes = "carry", duration = "010000", distance = "20"),
                ),
            ),
        )

        val parsed = parseDraftEntries(raw)
        assertEquals("010000", parsed.getValue(7L).single().duration)
        assertEquals(3600, MeasurementFormatter.parseDurationSeconds(parsed.getValue(7L).single().duration))
        assertEquals("20", parsed.getValue(7L).single().distance)
        assertEquals("40", parsed.getValue(7L).single().weight)
    }

    @Test
    fun loggedSetExportImport_durationRoundTrip() {
        val original = set(weight = 0.0, reps = 0, durationSeconds = 55).copy(
            id = 10,
            sessionId = 20,
            exerciseId = 30,
            exerciseNameSnapshot = "Plank",
            notes = "steady",
        )

        val imported = loggedSetFromImportJson(loggedSetToExportJson(original))

        assertEquals(55, imported.durationSeconds)
        assertNull(imported.distanceMeters)
        assertEquals(0, imported.reps)
        assertEquals("Plank", imported.exerciseNameSnapshot)
    }

    @Test
    fun loggedSetExportImport_durationDistanceRoundTrip() {
        val original = set(weight = 80.0, reps = 0, durationSeconds = 40, distanceMeters = 30.0).copy(
            id = 11,
            sessionId = 21,
            exerciseId = 31,
            exerciseNameSnapshot = "Sled Push",
        )

        val imported = loggedSetFromImportJson(loggedSetToExportJson(original))

        assertEquals(40, imported.durationSeconds)
        assertEquals(30.0, imported.distanceMeters ?: -1.0, 0.001)
        assertEquals(80.0, imported.weight, 0.0)
        assertEquals(0, imported.reps)
        assertEquals("Sled Push", imported.exerciseNameSnapshot)
    }

    @Test
    fun loggedSetImport_v1MissingDurationKeysDefaultsToNull() {
        val imported = loggedSetFromImportJson(
            JSONObject()
                .put("id", 1)
                .put("sessionId", 2)
                .put("exerciseId", JSONObject.NULL)
                .put("exerciseNameSnapshot", "Bench Press")
                .put("setNumber", 1)
                .put("weight", 80.0)
                .put("reps", 10)
                .put("rir", 2)
                .put("notes", JSONObject.NULL)
                .put("isPrWeight", false)
                .put("isPrReps", false),
        )

        assertNull(imported.durationSeconds)
        assertNull(imported.distanceMeters)
        assertEquals(10, imported.reps)
    }

    @Test
    fun loggedSetExportImport_canonicalKeyRoundTrip() {
        // DB-PRE7B (schemaVersion 3): the canonical identity survives export -> import.
        val original = set(weight = 60.0, reps = 10).copy(
            id = 12,
            sessionId = 22,
            exerciseId = 32,
            exerciseNameSnapshot = "Incline Machine Chest Press",
            canonicalExerciseKey = "catalog:incline-machine-chest-press",
        )

        val imported = loggedSetFromImportJson(loggedSetToExportJson(original))

        assertEquals("catalog:incline-machine-chest-press", imported.canonicalExerciseKey)
        assertEquals("Incline Machine Chest Press", imported.exerciseNameSnapshot)
        assertEquals(32L, imported.exerciseId)
    }

    @Test
    fun loggedSetImport_v1v2MissingCanonicalKeyDefaultsToNull() {
        // An older v1/v2 export (no canonicalExerciseKey) imports safely; the post-import backfill fills it.
        val imported = loggedSetFromImportJson(
            JSONObject()
                .put("id", 1)
                .put("sessionId", 2)
                .put("exerciseId", JSONObject.NULL)
                .put("exerciseNameSnapshot", "Bench Press")
                .put("setNumber", 1)
                .put("weight", 80.0)
                .put("reps", 10)
                .put("rir", 2)
                .put("notes", JSONObject.NULL)
                .put("isPrWeight", false)
                .put("isPrReps", false)
                .put("durationSeconds", JSONObject.NULL)
                .put("distanceMeters", JSONObject.NULL),
        )

        assertNull(imported.canonicalExerciseKey)
        assertEquals("Bench Press", imported.exerciseNameSnapshot)
    }

    @Test
    fun measurementFormatter_durationIsSelfLabeledMmSs() {
        // Duration carries no unit (mm:ss is self-evident).
        MeasurementFormatter.duration(45).let { assertEquals("00:45", it.value); assertEquals("", it.unit) }
        MeasurementFormatter.duration(90).let { assertEquals("01:30", it.value); assertEquals("", it.unit) }
        MeasurementFormatter.duration(3661).let { assertEquals("01:01:01", it.value) }
    }
}
