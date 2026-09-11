package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.WorkoutSessionEntity
import com.hoctin.gymtracker.data.catalog.Category
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.Force
import com.hoctin.gymtracker.data.catalog.Level
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import com.hoctin.gymtracker.data.catalog.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MUSCLE-4B — deterministic, pure-JVM verification of the upgraded Home readiness model. Runs the full
 * pipeline (sessions → exposure → quality-weighted heat → bi-exponential readiness cost) with a
 * hand-built catalog, exercising RIR quality, size-based bi-exponential profiles, delayed soreness,
 * exercise-cost scaling, stacking, grind, neglect, group roll-ups and the recovery/heat semantic split.
 */
class MuscleRecoveryEngineTest {

    private val HOUR = 3_600_000L
    private val NOW = 1_000L * 24 * 3_600_000 // arbitrary fixed "now"

    private fun catalogEntry(
        slug: String,
        name: String,
        primaries: List<MuscleGroup>,
        secondaries: List<MuscleGroup> = emptyList(),
        tracking: TrackingType = TrackingType.REPS_WEIGHT,
        progression: ProgressionType = ProgressionType.COMPOUND_LOAD,
        force: Force = Force.OTHER,
        equipment: Equipment = Equipment.OTHER,
    ) = CatalogExercise(
        slug = slug, name = name, equipment = equipment,
        primaryMuscles = primaries, secondaryMuscles = secondaries,
        level = Level.BEGINNER, force = force, category = Category.STRENGTH,
        progressionType = progression, trackingType = tracking,
        defaultSets = 3, defaultMinReps = 8, defaultMaxReps = 12, defaultIncrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 1, isBodyweight = false, isUnilateral = false,
        aliases = emptyList(),
    )

    private val catalog = listOf(
        catalogEntry("bench", "Bench Press", listOf(MuscleGroup.CHEST)),          // LARGE
        catalogEntry("curl", "Barbell Curl", listOf(MuscleGroup.BICEPS)),         // SMALL
        catalogEntry("plank", "Plank", listOf(MuscleGroup.ABS), tracking = TrackingType.TIME_ONLY),
        catalogEntry("pallof", "Pallof Press", listOf(MuscleGroup.CORE), tracking = TrackingType.SKILL_FORM),
    )

    private fun session(
        id: Long,
        startedAt: Long,
        slug: String?,
        name: String,
        setCount: Int,
        rir: Int = 2,
        durationSeconds: Int? = null,
    ) = SessionWithSets(
        session = WorkoutSessionEntity(
            id = id, dayId = null, dayNameSnapshot = "d", dateEpochDay = 20000, startedAtMillis = startedAt,
        ),
        sets = (1..setCount).map { i ->
            LoggedSetEntity(
                id = id * 100 + i, sessionId = id, exerciseId = 1, exerciseNameSnapshot = name,
                setNumber = i, weight = if (durationSeconds == null) 60.0 else 0.0,
                reps = if (durationSeconds == null) 10 else 0, rir = if (durationSeconds == null) rir else 0,
                notes = null, isPrWeight = false, isPrReps = false,
                durationSeconds = durationSeconds,
                canonicalExerciseKey = slug?.let { "catalog:$it" } ?: "custom:$name",
            )
        },
    )

    private fun compute(
        vararg sessions: SessionWithSets,
        planned: List<String> = emptyList(),
        allPlanned: List<String> = planned,
    ) = MuscleRecoveryPipeline.compute(sessions.toList(), catalog, planned, allPlanned, NOW)

    private fun resultFor(snapshot: MuscleRecoverySnapshot, muscle: MuscleGroup) =
        snapshot.results.first { it.muscleGroup == muscle }

    // ---- Decay & stacking ----------------------------------------------------------------

    @Test fun fatigueDecaysOverTime() {
        val fresh = compute(session(1, NOW - 4 * HOUR, "bench", "Bench Press", 4))
        val later = compute(session(1, NOW - 48 * HOUR, "bench", "Bench Press", 4))
        assertTrue(resultFor(later, MuscleGroup.CHEST).fatigueScore < resultFor(fresh, MuscleGroup.CHEST).fatigueScore)
        assertTrue(resultFor(later, MuscleGroup.CHEST).recoveryScore > resultFor(fresh, MuscleGroup.CHEST).recoveryScore)
    }

    @Test fun sameMuscleHitTwice_stacksFatigue() {
        val once = compute(session(1, NOW - 20 * HOUR, "bench", "Bench Press", 3))
        val twice = compute(
            session(1, NOW - 20 * HOUR, "bench", "Bench Press", 3),
            session(2, NOW - 44 * HOUR, "bench", "Bench Press", 3),
        )
        assertTrue(resultFor(twice, MuscleGroup.CHEST).fatigueScore > resultFor(once, MuscleGroup.CHEST).fatigueScore)
        assertEquals(2, resultFor(twice, MuscleGroup.CHEST).contributors.size)
    }

    @Test fun largeMuscles_decaySlowerThanSmall() {
        // Same stimulus, same elapsed 40h: biceps (small profile) clears; chest (large) still carries debt.
        val snapshot = compute(
            session(1, NOW - 40 * HOUR, "bench", "Bench Press", 3),
            session(2, NOW - 40 * HOUR, "curl", "Barbell Curl", 3),
        )
        val chest = resultFor(snapshot, MuscleGroup.CHEST)
        val biceps = resultFor(snapshot, MuscleGroup.BICEPS)
        assertTrue(chest.fatigueScore > biceps.fatigueScore)
        assertEquals(RecoveryStatus.READY, biceps.status)
        assertTrue(chest.status != RecoveryStatus.READY)
    }

    @Test fun recoveryRisk_grindDecaysSlower_thanProductiveRir() {
        val grind = compute(session(1, NOW - 60 * HOUR, "bench", "Bench Press", 5, rir = 0))
        val productive = compute(session(1, NOW - 60 * HOUR, "bench", "Bench Press", 5, rir = 2))
        val grindChest = resultFor(grind, MuscleGroup.CHEST)
        val productiveChest = resultFor(productive, MuscleGroup.CHEST)
        assertTrue(grindChest.grindCarried)
        assertFalse(productiveChest.grindCarried)
        // Grinding seeds LESS stimulus (0.70 vs 0.95) yet retains a HIGHER FRACTION at 60h — slower decay.
        assertTrue(grindChest.fatigueScore / (5 * 0.70) > productiveChest.fatigueScore / (5 * 0.95))
    }

    @Test fun easyRir5Work_createsLowerFatigue() {
        val easy = compute(session(1, NOW - 6 * HOUR, "bench", "Bench Press", 3, rir = 5))
        val hard = compute(session(1, NOW - 6 * HOUR, "bench", "Bench Press", 3, rir = 2))
        assertTrue(resultFor(easy, MuscleGroup.CHEST).fatigueScore < resultFor(hard, MuscleGroup.CHEST).fatigueScore)
    }

    @Test fun oldSessions_fallToReady() {
        val snapshot = compute(session(1, NOW - 120 * HOUR, "bench", "Bench Press", 5, rir = 2))
        val chest = resultFor(snapshot, MuscleGroup.CHEST)
        // Bi-exponential never hits exactly zero, but 5 days out reads READY with high recovery.
        assertEquals(RecoveryStatus.READY, chest.status)
        assertTrue("recovery should be high, was ${chest.recoveryScore}", chest.recoveryScore >= 88)
        assertTrue(chest.reason.contains("Recovered"))
    }

    // ---- Delayed soreness (bi-exponential upgrade) -----------------------------------------

    @Test fun freshHardSession_showsDelayedSorenessWindow_structuralDominates() {
        // 8h after a hard 5-set compound: acute has partly cleared, structural is at its plateau.
        val chest = resultFor(compute(session(1, NOW - 8 * HOUR, "bench", "Bench Press", 5, rir = 2)), MuscleGroup.CHEST)
        assertTrue(chest.soreWindow)
        assertTrue(chest.structuralResidual > chest.acuteResidual)
        assertTrue(chest.reason.contains("8h ago"))
        assertTrue(
            chest.status == RecoveryStatus.OVERLOADED || chest.status == RecoveryStatus.SORE ||
                chest.status == RecoveryStatus.RECOVERING,
        )
    }

    @Test fun veryFreshSession_acuteStrainPresent_notYetPastDelay() {
        // 1h after training: acute strain is high and the delayed structural term is still at plateau.
        val chest = resultFor(compute(session(1, NOW - 1 * HOUR, "bench", "Bench Press", 5, rir = 2)), MuscleGroup.CHEST)
        assertTrue(chest.acuteResidual > 0.5)
        assertTrue(chest.status == RecoveryStatus.OVERLOADED || chest.status == RecoveryStatus.SORE)
    }

    // ---- Exercise recovery cost (taxonomy-derived, review's exerciseDamageFactor) ----------

    @Test fun exerciseRecoveryCost_compoundHingeBarbell_exceedsIsolationMachine() {
        val rdl = catalogEntry(
            "rdl", "Romanian Deadlift", listOf(MuscleGroup.HAMSTRINGS),
            progression = ProgressionType.COMPOUND_LOAD, force = Force.HINGE, equipment = Equipment.BARBELL,
        )
        val legCurl = catalogEntry(
            "legcurl", "Leg Curl", listOf(MuscleGroup.HAMSTRINGS),
            progression = ProgressionType.ISOLATION_SLOW, force = Force.OTHER, equipment = Equipment.MACHINE,
        )
        val rdlCost = MuscleRecoveryEngine.exerciseRecoveryCost(rdl)
        val curlCost = MuscleRecoveryEngine.exerciseRecoveryCost(legCurl)
        assertTrue("RDL ($rdlCost) should out-cost leg curl ($curlCost)", rdlCost > curlCost)
        assertEquals(1.4, rdlCost, 1e-9)   // clamped max
        assertEquals(1.0, MuscleRecoveryEngine.exerciseRecoveryCost(null), 1e-9) // custom/unknown neutral
        assertTrue(curlCost in 0.7..1.4)
    }

    @Test fun higherExerciseCost_leavesMoreStructuralFatigueLater() {
        val heavy = catalogEntry(
            "sq", "Back Squat", listOf(MuscleGroup.QUADS),
            progression = ProgressionType.COMPOUND_LOAD, force = Force.SQUAT, equipment = Equipment.BARBELL,
        )
        val light = catalogEntry(
            "ext", "Leg Extension", listOf(MuscleGroup.QUADS),
            progression = ProgressionType.ISOLATION_SLOW, force = Force.OTHER, equipment = Equipment.MACHINE,
        )
        val cat = listOf(heavy, light)
        fun quadFatigue(slug: String) = MuscleRecoveryPipeline.compute(
            listOf(session(1, NOW - 40 * HOUR, slug, if (slug == "sq") "Back Squat" else "Leg Extension", 4, rir = 2)),
            cat, emptyList(), emptyList(), NOW,
        ).results.first { it.muscleGroup == MuscleGroup.QUADS }
        assertTrue(quadFatigue("sq").structuralResidual > quadFatigue("ext").structuralResidual)
    }

    // ---- Conservatism & safety -----------------------------------------------------------

    @Test fun timeAndSkillWork_seedConservativeFatigue() {
        val plank = compute(session(1, NOW - 4 * HOUR, "plank", "Plank", 3, durationSeconds = 60))
        val bench = compute(session(1, NOW - 4 * HOUR, "bench", "Bench Press", 3, rir = 2))
        assertTrue(resultFor(plank, MuscleGroup.ABS).fatigueScore < resultFor(bench, MuscleGroup.CHEST).fatigueScore)
        val pallof = compute(session(1, NOW - 4 * HOUR, "pallof", "Pallof Press", 3, rir = 2))
        assertTrue(resultFor(pallof, MuscleGroup.CORE).fatigueScore < resultFor(bench, MuscleGroup.CHEST).fatigueScore)
    }

    @Test fun customUnknownExercises_doNotCrash_andSeedNothing() {
        val snapshot = compute(session(1, NOW - 4 * HOUR, null, "Weird Machine Thing", 3))
        assertTrue(snapshot.results.isEmpty())
        assertFalse(snapshot.hasRecentTraining)
    }

    @Test fun noWorkouts_emptySnapshot() {
        val snapshot = compute()
        assertTrue(snapshot.results.isEmpty())
        assertFalse(snapshot.hasRecentTraining)
        assertTrue(snapshot.planOverlap.isEmpty())
        assertTrue(snapshot.neglected.isEmpty())
        // Groups still emitted (all 6, Ready) so the dashboard has a stable shape.
        assertEquals(6, snapshot.groups.size)
        assertTrue(snapshot.groups.all { it.status == RecoveryStatus.READY })
        assertEquals(100, snapshot.overallReadiness)
    }

    @Test fun sessionsOutsideSevenDayWindow_ignored() {
        val snapshot = compute(session(1, NOW - 9L * 24 * HOUR, "bench", "Bench Press", 5))
        assertTrue(snapshot.results.isEmpty())
    }

    // ---- Group roll-ups & overall ----------------------------------------------------------

    @Test fun groups_alwaysSix_untrainedGroupsReady_overallComputed() {
        val snapshot = compute(session(1, NOW - 6 * HOUR, "bench", "Bench Press", 5, rir = 2))
        assertEquals(6, snapshot.groups.size)
        val chestGroup = snapshot.groups.first { it.group == RecoveryBodyGroup.CHEST }
        assertTrue(chestGroup.trained)
        assertTrue(chestGroup.recoveryScore < 100)
        val legs = snapshot.groups.first { it.group == RecoveryBodyGroup.LEGS }
        assertFalse(legs.trained)
        assertEquals(RecoveryStatus.READY, legs.status)
        assertEquals(100, legs.recoveryScore)
        assertTrue(snapshot.overallReadiness in 0..100)
    }

    // ---- Neglect nudges --------------------------------------------------------------------

    @Test fun neglectedPlanMuscle_flaggedWithDaysSince() {
        // Trained chest recently; biceps last trained 9 days ago but is in the plan → neglected.
        val snapshot = compute(
            session(1, NOW - 6 * HOUR, "bench", "Bench Press", 4, rir = 2),
            session(2, NOW - 9L * 24 * HOUR, "curl", "Barbell Curl", 4, rir = 2),
            planned = listOf("Bench Press"),
            allPlanned = listOf("Bench Press", "Barbell Curl"),
        )
        val neglect = snapshot.neglected.firstOrNull { it.muscleGroup == MuscleGroup.BICEPS }
        assertTrue("biceps should be flagged neglected", neglect != null)
        assertEquals(9, neglect!!.daysSinceTrained)
    }

    @Test fun neverTrainedPlanMuscle_neglectedWithNullDays_onlyWhenTrainingExists() {
        val withTraining = compute(
            session(1, NOW - 6 * HOUR, "bench", "Bench Press", 4, rir = 2),
            allPlanned = listOf("Bench Press", "Barbell Curl"),
        )
        val bicep = withTraining.neglected.firstOrNull { it.muscleGroup == MuscleGroup.BICEPS }
        assertTrue(bicep != null)
        assertNull(bicep!!.daysSinceTrained)
        // No recent training at all → no nagging.
        val noTraining = compute(allPlanned = listOf("Bench Press", "Barbell Curl"))
        assertTrue(noTraining.neglected.isEmpty())
    }

    // ---- Plan overlap ----------------------------------------------------------------------

    @Test fun planOverlap_flagsStillFatiguedPlannedMuscles() {
        val snapshot = compute(
            session(1, NOW - 8 * HOUR, "bench", "Bench Press", 5, rir = 2),
            planned = listOf("Bench Press"),
        )
        val chest = resultFor(snapshot, MuscleGroup.CHEST)
        assertTrue(chest.status == RecoveryStatus.SORE || chest.status == RecoveryStatus.OVERLOADED)
        assertTrue(MuscleGroup.CHEST in snapshot.planOverlap)
        // An unmatched planned name never crashes and never flags anything.
        val custom = compute(
            session(1, NOW - 8 * HOUR, "bench", "Bench Press", 5, rir = 2),
            planned = listOf("Totally Custom Move"),
        )
        assertTrue(custom.planOverlap.isEmpty())
    }

    // ---- Semantics: recovery colors/statuses are their own system --------------------------

    @Test fun recoveryColors_areNotHeatSemantics() {
        assertNull(RecoveryPalette.argb(RecoveryColorToken.RECOVERY_READY))
        assertNull(RecoveryPalette.argb(RecoveryColorToken.RECOVERY_NEUTRAL))
        assertTrue(RecoveryPalette.argb(RecoveryColorToken.RECOVERY_MAX) != null)
        // A recovered muscle produces no region paint at all (neutral body = ready).
        val recovered = compute(session(1, NOW - 200 * HOUR, "bench", "Bench Press", 5))
        assertTrue(recoveryRegionPaints(recovered.results).isEmpty())
        // A fresh heavy session paints its regions with recovery colors.
        val fresh = compute(session(1, NOW - 4 * HOUR, "bench", "Bench Press", 5, rir = 2))
        assertTrue(recoveryRegionPaints(fresh.results).containsKey(BodyRegionId.CHEST))
        // Status ladder is monotone with recovery% (color/bar/status always agree).
        assertEquals(RecoveryColorToken.RECOVERY_MAX, RecoveryPalette.tokenFor(RecoveryStatus.OVERLOADED))
        assertEquals(RecoveryColorToken.RECOVERY_READY, RecoveryPalette.tokenFor(RecoveryStatus.READY))
    }
}
