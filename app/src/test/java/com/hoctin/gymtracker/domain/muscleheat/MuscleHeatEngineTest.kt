package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MUSCLE-1 — deterministic, pure-JVM verification of Layer A (raw muscle exposure) and Layer B
 * (baseline Muscle Heat Engine V1). No Room, no Android, no I/O — snapshots are built by hand with the
 * same shapes the [SessionMuscleHeatUseCase] joins at runtime.
 */
class MuscleHeatEngineTest {

    // ---- helpers ---------------------------------------------------------------------

    private fun sets(
        n: Int,
        reps: Int = 10,
        weight: Double = 40.0,
        rir: Int = 2,
        durationSeconds: Int? = null,
        distanceMeters: Double? = null,
        key: String = "catalog:test",
    ): List<LoggedSetEntity> = (1..n).map { i ->
        LoggedSetEntity(
            id = i.toLong(), sessionId = 1L, exerciseId = 1L, exerciseNameSnapshot = "x",
            setNumber = i, weight = weight, reps = reps, rir = rir, notes = null,
            isPrWeight = false, isPrReps = false,
            durationSeconds = durationSeconds, distanceMeters = distanceMeters,
            canonicalExerciseKey = key,
        )
    }

    private fun snap(
        name: String,
        type: TrackingType = TrackingType.REPS_WEIGHT,
        primaries: List<MuscleGroup>,
        secondaries: List<MuscleGroup> = emptyList(),
        target: Int = 3,
        sets: List<LoggedSetEntity>,
    ) = ExerciseSessionSnapshot(
        canonicalExerciseKey = "catalog:" + name.lowercase().replace(' ', '-'),
        exerciseName = name,
        trackingType = type,
        primaryMuscles = primaries,
        secondaryMuscles = secondaries,
        targetSets = target,
        sets = sets,
    )

    private fun exposureOf(result: SessionMuscleExposure, muscle: MuscleGroup): MuscleExposure =
        result.muscles.first { it.muscleGroup == muscle }

    private fun bench(setCount: Int = 3, rir: Int = 2, target: Int = 3) = snap(
        name = "Bench Press",
        primaries = listOf(MuscleGroup.CHEST),
        secondaries = listOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
        target = target,
        sets = sets(setCount, reps = 8, weight = 60.0, rir = rir),
    )

    // ---- Layer A: exposure rules ------------------------------------------------------

    @Test fun primaryMuscle_contributesMoreThanSecondary() {
        val result = MuscleExposureCalculator.compute(listOf(bench()))
        val chest = exposureOf(result, MuscleGroup.CHEST)
        val triceps = exposureOf(result, MuscleGroup.TRICEPS)
        assertEquals(3.0, chest.primarySetEquivalent, 1e-9)     // 3 sets x 1.0
        assertEquals(1.5, triceps.secondarySetEquivalent, 1e-9) // 3 sets x 0.5
        assertTrue(chest.rawIntensityScore > triceps.rawIntensityScore)
        assertEquals(MuscleRole.PRIMARY, chest.exerciseContributors.single().role)
        assertEquals(MuscleRole.SECONDARY, triceps.exerciseContributors.single().role)
    }

    @Test fun skippedExercise_contributesZero_butIsVisible() {
        // Planned (target 3) but never logged: 0 exposure, still explained as skipped.
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 0)))
        val chest = exposureOf(result, MuscleGroup.CHEST)
        assertEquals(0.0, chest.rawIntensityScore, 1e-9)
        assertEquals(1, chest.skippedExercises)
        assertEquals(0, chest.completedExercises)
        assertEquals(0, chest.exerciseContributors.single().completedSets)
        val heat = MuscleHeatEngine.analyzeMuscle(chest)
        assertEquals(HeatColorBand.NONE, heat.colorBand)
        assertEquals(MuscleStatus.NONE, heat.status)
        assertTrue(heat.reason.contains("skipped"))
    }

    @Test fun partialExercise_scalesContribution() {
        val partial = MuscleExposureCalculator.compute(listOf(bench(setCount = 2, target = 4)))
        val full = MuscleExposureCalculator.compute(listOf(bench(setCount = 4, target = 4)))
        val partialChest = exposureOf(partial, MuscleGroup.CHEST)
        val fullChest = exposureOf(full, MuscleGroup.CHEST)
        assertEquals(2.0, partialChest.rawIntensityScore, 1e-9)
        assertEquals(4.0, fullChest.rawIntensityScore, 1e-9)
        assertEquals(0.5, partialChest.exerciseContributors.single().completionRatio, 1e-9)
        assertEquals(1, partialChest.partialExercises)
        assertEquals(0, partialChest.completedExercises)
    }

    @Test fun completedExercise_fullContribution() {
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 3, target = 3)))
        val chest = exposureOf(result, MuscleGroup.CHEST)
        assertEquals(1, chest.completedExercises)
        assertEquals(1.0, chest.exerciseContributors.single().completionRatio, 1e-9)
        assertEquals(3.0, chest.rawIntensityScore, 1e-9)
    }

    @Test fun multipleExercises_stackOnSameMuscle() {
        val pushups = snap(
            name = "Pushups", type = TrackingType.BODYWEIGHT_REPS,
            primaries = listOf(MuscleGroup.CHEST), secondaries = listOf(MuscleGroup.TRICEPS),
            sets = sets(3, reps = 15, weight = 0.0, rir = 2),
        )
        val result = MuscleExposureCalculator.compute(listOf(bench(), pushups))
        val chest = exposureOf(result, MuscleGroup.CHEST)
        assertEquals(6.0, chest.rawIntensityScore, 1e-9) // 3 + 3 primary set-equivalents
        assertEquals(2, chest.exerciseContributors.size)
        assertEquals(2, chest.completedExercises)
    }

    @Test fun customExercise_safeFallback_neverCrashes_neverMisattributes() {
        val custom = ExerciseSessionSnapshot(
            canonicalExerciseKey = "custom:weird-machine-thing",
            exerciseName = "Weird machine thing",
            trackingType = TrackingType.REPS_WEIGHT,
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            targetSets = 3,
            sets = sets(3, key = "custom:weird-machine-thing"),
        )
        val result = MuscleExposureCalculator.compute(listOf(custom))
        assertTrue(result.muscles.isEmpty())                       // no fake anatomy
        assertEquals(1, result.unattributedContributors.size)      // still visible to the user
        assertEquals("custom:weird-machine-thing", result.unattributedContributors.single().canonicalExerciseKey)
        assertTrue(MuscleHeatEngine.analyze(result).isEmpty())     // engine handles it without crashing
    }

    @Test fun sameCanonicalKey_stableAcrossComputations() {
        // The contributor identity is the canonical key, so joins across days/sessions stay stable.
        val a = MuscleExposureCalculator.compute(listOf(bench()))
        val b = MuscleExposureCalculator.compute(listOf(bench(setCount = 4, target = 4)))
        assertEquals(
            exposureOf(a, MuscleGroup.CHEST).exerciseContributors.single().canonicalExerciseKey,
            exposureOf(b, MuscleGroup.CHEST).exerciseContributors.single().canonicalExerciseKey,
        )
    }

    // ---- TrackingType behavior ---------------------------------------------------------

    @Test fun repsWeight_fullQualityScoring_productive() {
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 3, rir = 2)))
        val heat = MuscleHeatEngine.analyzeMuscle(exposureOf(result, MuscleGroup.CHEST))
        assertEquals(0.95, heat.qualityModifier, 1e-9)             // RIR 2 = productive window
        assertEquals(MuscleStatus.PRODUCTIVE, heat.status)
    }

    @Test fun bodyweightReps_fullQualityScoring() {
        val pullups = snap(
            name = "Pullups", type = TrackingType.BODYWEIGHT_REPS,
            primaries = listOf(MuscleGroup.LATS), secondaries = listOf(MuscleGroup.BICEPS),
            sets = sets(3, reps = 8, weight = 0.0, rir = 1),
        )
        val heat = MuscleHeatEngine.analyzeMuscle(
            exposureOf(MuscleExposureCalculator.compute(listOf(pullups)), MuscleGroup.LATS),
        )
        assertEquals(0.95, heat.qualityModifier, 1e-9)
        assertEquals(MuscleStatus.PRODUCTIVE, heat.status)
    }

    @Test fun skillForm_conservativeScoring_notProductive() {
        val pallof = snap(
            name = "Pallof Press", type = TrackingType.SKILL_FORM,
            primaries = listOf(MuscleGroup.CORE), secondaries = listOf(MuscleGroup.OBLIQUES),
            sets = sets(3, reps = 10, weight = 20.0, rir = 2),
        )
        val heat = MuscleHeatEngine.analyzeMuscle(
            exposureOf(MuscleExposureCalculator.compute(listOf(pallof)), MuscleGroup.CORE),
        )
        assertEquals(0.6, heat.qualityModifier, 1e-9)              // fixed conservative, form-focused
        assertEquals(MuscleStatus.STIMULATED, heat.status)          // never PRODUCTIVE off skill work alone
    }

    @Test fun timeOnly_exposureCounts_qualityLimited() {
        val plank = snap(
            name = "Plank", type = TrackingType.TIME_ONLY,
            primaries = listOf(MuscleGroup.ABS), secondaries = listOf(MuscleGroup.CORE),
            sets = sets(3, reps = 0, weight = 0.0, rir = 0, durationSeconds = 60),
        )
        val abs = exposureOf(MuscleExposureCalculator.compute(listOf(plank)), MuscleGroup.ABS)
        assertEquals(3.0, abs.rawIntensityScore, 1e-9)             // raw exposure allowed
        assertEquals(180, abs.exerciseContributors.single().durationSeconds)
        val heat = MuscleHeatEngine.analyzeMuscle(abs)
        assertEquals(0.5, heat.qualityModifier, 1e-9)              // limited quality
        assertEquals(MuscleStatus.STIMULATED, heat.status)
    }

    @Test fun distanceLoad_exposureCounts_qualityLimited() {
        val sled = snap(
            name = "Sled Push", type = TrackingType.DISTANCE_LOAD,
            primaries = listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
            secondaries = listOf(MuscleGroup.CALVES),
            sets = sets(4, reps = 0, weight = 80.0, rir = 0, distanceMeters = 20.0),
        )
        val quads = exposureOf(MuscleExposureCalculator.compute(listOf(sled)), MuscleGroup.QUADS)
        assertEquals(4.0, quads.rawIntensityScore, 1e-9)
        assertEquals(80.0, quads.exerciseContributors.single().avgWeight!!, 1e-9)
        assertEquals(80.0, quads.exerciseContributors.single().distanceMeters!!, 1e-9) // 4 x 20m
        val heat = MuscleHeatEngine.analyzeMuscle(quads)
        assertEquals(0.5, heat.qualityModifier, 1e-9)
        assertTrue(heat.status == MuscleStatus.STIMULATED)          // conservative, no failure inference
    }

    @Test fun timeDistanceLoad_exposureCounts_qualityLimited() {
        val farmers = snap(
            name = "Farmers Walk", type = TrackingType.TIME_DISTANCE_LOAD,
            primaries = listOf(MuscleGroup.FOREARMS, MuscleGroup.TRAPS),
            secondaries = listOf(MuscleGroup.CORE),
            sets = sets(3, reps = 0, weight = 32.0, rir = 0, durationSeconds = 45, distanceMeters = 30.0),
        )
        val forearms = exposureOf(MuscleExposureCalculator.compute(listOf(farmers)), MuscleGroup.FOREARMS)
        assertEquals(3.0, forearms.rawIntensityScore, 1e-9)
        val heat = MuscleHeatEngine.analyzeMuscle(forearms)
        assertEquals(0.5, heat.qualityModifier, 1e-9)
        assertNotEquals(MuscleStatus.RECOVERY_RISK, heat.status)
    }

    @Test fun repsZeroSentinel_neverReadAsFailedHypertrophyReps() {
        // TIME_ONLY stores reps=0 / rir=0 sentinels: they must never surface as avg metrics or grinding.
        val plank = snap(
            name = "Plank", type = TrackingType.TIME_ONLY,
            primaries = listOf(MuscleGroup.ABS),
            target = 5,
            sets = sets(5, reps = 0, weight = 0.0, rir = 0, durationSeconds = 60),
        )
        val abs = exposureOf(MuscleExposureCalculator.compute(listOf(plank)), MuscleGroup.ABS)
        val contributor = abs.exerciseContributors.single()
        assertNull(contributor.avgRir)                              // sentinel not interpreted
        assertNull(contributor.avgReps)
        val heat = MuscleHeatEngine.analyzeMuscle(abs)
        assertNotEquals(MuscleStatus.RECOVERY_RISK, heat.status)    // rir=0 sentinel is NOT "to failure"
    }

    // ---- Layer B: bands + status (separate axes) --------------------------------------

    @Test fun colorBand_thresholds_deterministic() {
        assertEquals(HeatColorBand.NONE, MuscleHeatEngine.colorBand(0.0))
        assertEquals(HeatColorBand.LIGHT_YELLOW, MuscleHeatEngine.colorBand(1.0))
        assertEquals(HeatColorBand.YELLOW_ORANGE, MuscleHeatEngine.colorBand(2.0))
        assertEquals(HeatColorBand.YELLOW_ORANGE, MuscleHeatEngine.colorBand(3.9))
        assertEquals(HeatColorBand.ORANGE_RED, MuscleHeatEngine.colorBand(4.0))
        assertEquals(HeatColorBand.ORANGE_RED, MuscleHeatEngine.colorBand(6.9))
        assertEquals(HeatColorBand.DEEP_RED, MuscleHeatEngine.colorBand(7.0))
    }

    @Test fun status_isSeparateFromColorBand_andQualityDrivesTheBand() {
        // MUSCLE-HEAT-1: same RAW exposure (5 sets) — quality now drives the band (stimulus), while
        // status stays its own axis (submaximal and grinding share a band but mean different things).
        fun heatAtRir(rir: Int): MuscleHeatResult {
            val snapshotList = listOf(bench(setCount = 5, rir = rir, target = 5))
            return MuscleHeatEngine.analyzeMuscle(
                exposureOf(MuscleExposureCalculator.compute(snapshotList), MuscleGroup.CHEST),
            )
        }
        val productive = heatAtRir(2)   // productive window (quality 0.95 -> 4.75)
        val submaximal = heatAtRir(4)   // lots left in reserve (0.40 -> 2.0)
        val grinding = heatAtRir(0)     // everything to failure (0.70 -> 3.5)
        assertEquals(HeatColorBand.ORANGE_RED, productive.colorBand)
        assertEquals(HeatColorBand.YELLOW_ORANGE, submaximal.colorBand) // same raw, cooler band
        assertEquals(HeatColorBand.YELLOW_ORANGE, grinding.colorBand)
        assertEquals(MuscleStatus.PRODUCTIVE, productive.status)
        assertEquals(MuscleStatus.STIMULATED, submaximal.status)    // status = meaning, differs
        assertEquals(MuscleStatus.RECOVERY_RISK, grinding.status)   // grind flagged, band not "productive-hot"
    }

    @Test fun recoveryRisk_fromGrindingEverySetToFailure() {
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 5, rir = 0, target = 5)))
        val heat = MuscleHeatEngine.analyzeMuscle(exposureOf(result, MuscleGroup.CHEST))
        assertEquals(MuscleStatus.RECOVERY_RISK, heat.status)
        assertNotNull(heat.recoveryNote)
    }

    @Test fun fatigued_fromVeryHighSessionVolume() {
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 8, rir = 2, target = 8)))
        val heat = MuscleHeatEngine.analyzeMuscle(exposureOf(result, MuscleGroup.CHEST))
        assertEquals(HeatColorBand.DEEP_RED, heat.colorBand)
        assertEquals(MuscleStatus.FATIGUED, heat.status)
        assertNotNull(heat.recoveryNote)
    }

    @Test fun undertrained_fromLightTouch() {
        val result = MuscleExposureCalculator.compute(listOf(bench(setCount = 1, target = 3)))
        val heat = MuscleHeatEngine.analyzeMuscle(exposureOf(result, MuscleGroup.CHEST))
        assertEquals(HeatColorBand.LIGHT_YELLOW, heat.colorBand)
        assertEquals(MuscleStatus.UNDERTRAINED, heat.status)
        assertNull(heat.recoveryNote)
    }

    // ---- Realistic session (push day + core + carry) -----------------------------------

    @Test fun realisticSession_pushPlankAndCarry_mapToExpectedMuscles() {
        val plank = snap(
            name = "Plank", type = TrackingType.TIME_ONLY,
            primaries = listOf(MuscleGroup.ABS), secondaries = listOf(MuscleGroup.CORE),
            sets = sets(3, reps = 0, weight = 0.0, rir = 0, durationSeconds = 60),
        )
        val farmers = snap(
            name = "Farmers Walk", type = TrackingType.TIME_DISTANCE_LOAD,
            primaries = listOf(MuscleGroup.FOREARMS, MuscleGroup.TRAPS),
            secondaries = listOf(MuscleGroup.CORE),
            sets = sets(2, reps = 0, weight = 32.0, rir = 0, durationSeconds = 45, distanceMeters = 30.0),
        )
        val result = MuscleExposureCalculator.compute(listOf(bench(), plank, farmers))
        val heat = MuscleHeatEngine.analyze(result)
        val byMuscle = heat.associateBy { it.muscleGroup }

        // Bench drives chest (primary) + triceps/front delts (secondary). Final = raw × quality (RIR 2 → 0.95).
        assertEquals(3.0 * 0.95, byMuscle.getValue(MuscleGroup.CHEST).finalIntensityScore, 1e-9)
        assertTrue(MuscleGroup.TRICEPS in byMuscle && MuscleGroup.FRONT_DELTS in byMuscle)
        assertEquals(MuscleStatus.PRODUCTIVE, byMuscle.getValue(MuscleGroup.CHEST).status)
        // Plank drives abs via TIME_ONLY exposure (limited quality 0.5).
        assertEquals(3.0 * 0.5, byMuscle.getValue(MuscleGroup.ABS).finalIntensityScore, 1e-9)
        // Farmer's walk drives forearms/traps via time+distance+load exposure (limited quality 0.5).
        assertEquals(2.0 * 0.5, byMuscle.getValue(MuscleGroup.FOREARMS).finalIntensityScore, 1e-9)
        assertEquals(2.0 * 0.5, byMuscle.getValue(MuscleGroup.TRAPS).finalIntensityScore, 1e-9)
        // CORE stacks two secondary contributors: plank 1.5 + carry 1.0 = raw 2.5 (both limited quality).
        assertEquals(2.5 * 0.5, byMuscle.getValue(MuscleGroup.CORE).finalIntensityScore, 1e-9)
        // Raw exposure itself is untouched by the quality calibration.
        assertEquals(3.0, result.muscles.first { it.muscleGroup == MuscleGroup.CHEST }.rawIntensityScore, 1e-9)
        assertEquals(2, result.muscles.first { it.muscleGroup == MuscleGroup.CORE }.exerciseContributors.size)
        // Results come back hottest-first for the "top muscles" UI.
        assertTrue(result.muscles.first().rawIntensityScore >= result.muscles.last().rawIntensityScore)
    }

    // ---- MUSCLE-HEAT-1: stimulus-quality color calibration ------------------------------

    private fun chestHeat(setCount: Int, rir: Int, rirs: List<Int>? = null): Pair<MuscleExposure, MuscleHeatResult> {
        val snapshot = ExerciseSessionSnapshot(
            canonicalExerciseKey = "catalog:bench-press",
            exerciseName = "Bench Press",
            trackingType = TrackingType.REPS_WEIGHT,
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = emptyList(),
            targetSets = setCount,
            sets = sets(setCount, reps = 10, weight = 60.0, rir = rir).let { base ->
                if (rirs == null) base else base.mapIndexed { i, s -> s.copy(rir = rirs[i]) }
            },
        )
        val exposure = exposureOf(MuscleExposureCalculator.compute(listOf(snapshot)), MuscleGroup.CHEST)
        return exposure to MuscleHeatEngine.analyzeMuscle(exposure)
    }

    @Test fun identicalRawExposure_highRir_hasLowerFinalIntensityAndCoolerBand() {
        val (easyExposure, easy) = chestHeat(setCount = 4, rir = 5)   // lots in reserve
        val (hardExposure, hard) = chestHeat(setCount = 4, rir = 2)   // productive window
        assertEquals(easyExposure.rawIntensityScore, hardExposure.rawIntensityScore, 1e-9) // same raw work
        assertTrue("final must drop with high RIR", easy.finalIntensityScore < hard.finalIntensityScore)
        assertTrue("band must cool with high RIR", easy.colorBand.ordinal < hard.colorBand.ordinal)
    }

    @Test fun easyCompletedSets_rir5_neverDeepRed() {
        // Even a big pile of easy sets (8 × RIR 5 = raw 8.0, quality 0.40 → 3.2) stays out of deep red.
        val (_, easy) = chestHeat(setCount = 8, rir = 5)
        assertEquals(HeatColorBand.YELLOW_ORANGE, easy.colorBand)
        assertNotEquals(HeatColorBand.DEEP_RED, easy.colorBand)
    }

    @Test fun hardProductiveSets_heatStrongerThanEasySets_rawUnchangedFinalDiffers() {
        val (easyExposure, easy) = chestHeat(setCount = 3, rir = 5)
        val (hardExposure, hard) = chestHeat(setCount = 3, rir = 0, rirs = listOf(2, 1, 0)) // avg RIR 1.0
        // Layer A raw exposure is identical — the calibration never rewrites anatomical work.
        assertEquals(3.0, easyExposure.rawIntensityScore, 1e-9)
        assertEquals(3.0, hardExposure.rawIntensityScore, 1e-9)
        // Layer B final stimulus differs purely through the quality modifier.
        assertEquals(3.0 * 0.40, easy.finalIntensityScore, 1e-9)
        assertEquals(3.0 * 0.95, hard.finalIntensityScore, 1e-9)
        assertTrue(hard.colorBand.ordinal > easy.colorBand.ordinal)
        assertEquals(HeatColorBand.LIGHT_YELLOW, easy.colorBand)     // 1.2
        assertEquals(HeatColorBand.YELLOW_ORANGE, hard.colorBand)    // 2.85
    }
}
