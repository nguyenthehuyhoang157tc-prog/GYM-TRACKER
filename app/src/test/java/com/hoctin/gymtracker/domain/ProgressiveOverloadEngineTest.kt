package com.hoctin.gymtracker.domain

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic verification of the Rule-Based Progressive Overload Engine — V2 rules (Task 2.4b)
 * plus the V3 trend/outlier/RIR/mixed-weight rules (Task 2.4c).
 *
 * Test scope only — not part of the shipped APK. Covers the QA scenarios from the tasks plus the
 * documented edge cases. Sessions are passed newest-first (matching recentSetsForExercise /
 * observeExerciseHistory ordering); sessionId boundaries are what the engine groups on.
 */
class ProgressiveOverloadEngineTest {

    private fun ex(name: String, sets: Int, min: Int, max: Int) = ExerciseEntity(
        id = 1, dayId = 1, name = name,
        targetSets = sets, targetMinReps = min, targetMaxReps = max, sortOrder = 0,
    )

    /** Build one session's sets. [weights]/[rirs] may be a single value (straight sets) or per-set. */
    private fun session(
        sessionId: Long,
        reps: List<Int>,
        weight: Double = 0.0,
        weights: List<Double>? = null,
        rir: Int = 2,
        rirs: List<Int>? = null,
    ): List<LoggedSetEntity> = reps.mapIndexed { i, r ->
        LoggedSetEntity(
            id = sessionId * 100 + i,
            sessionId = sessionId,
            exerciseId = 1,
            exerciseNameSnapshot = "x",
            setNumber = i + 1,
            weight = weights?.get(i) ?: weight,
            reps = r,
            rir = rirs?.get(i) ?: rir,
            notes = null,
            isPrWeight = false,
            isPrReps = false,
        )
    }

    /** V3.5 catalog profile helper. */
    private fun profile(
        type: ProgressionType,
        inc: Double = 2.5,
        sessions: Int = 1,
        bw: Boolean = false,
        uni: Boolean = false,
    ) = ProgressionProfile(type, inc, sessions, bw, uni)

    /** V3.6B helper: run the engine with an explicit profile/readiness on already-flattened sets. */
    private fun prog(
        exercise: ExerciseEntity,
        sets: List<LoggedSetEntity>,
        profile: ProgressionProfile? = null,
        readiness: Readiness? = null,
    ) = ProgressiveOverloadEngine.progression(exercise, sets, profile, readiness)

    /** V3.7B helper: run the engine with a readiness-per-session map (the readiness trend input). */
    private fun progRt(
        exercise: ExerciseEntity,
        sets: List<LoggedSetEntity>,
        readinessBySession: Map<Long, Readiness?>,
        profile: ProgressionProfile? = null,
        readiness: Readiness? = null,
    ) = ProgressiveOverloadEngine.progression(exercise, sets, profile, readiness, readinessBySession)

    /** V3.7C-H helper: run the engine with a whole-workout context (systemic/local + hypertrophy quality). */
    private fun progWc(
        exercise: ExerciseEntity,
        sets: List<LoggedSetEntity>,
        workoutContext: WorkoutContext,
        profile: ProgressionProfile? = null,
        readiness: Readiness? = null,
        readinessBySession: Map<Long, Readiness?>? = null,
    ) = ProgressiveOverloadEngine.progression(exercise, sets, profile, readiness, readinessBySession, workoutContext)

    private fun result(exercise: ExerciseEntity, vararg sessionsNewestFirst: List<LoggedSetEntity>): ProgressionSuggestion {
        val flat = sessionsNewestFirst.toList().flatten()
        val r = ProgressiveOverloadEngine.progression(exercise, flat)
        println("[${exercise.name}] -> ${r.action} | weight=${r.suggestedWeight} | type=${r.progressionType} | ${r.reason}")
        return r
    }

    // ---- Compound (target 3x10-12) ----------------------------------------------------

    @Test fun compound_allAtLeastMin_increases() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 11, 11), weight = 40.0))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(ProgressionType.COMPOUND_LOAD, r.progressionType)
        assertEquals(42.5, r.suggestedWeight!!, 0.001) // upper-body +2.5
    }

    @Test fun compound_oneSetBelowMin_keeps() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 10, 8), weight = 40.0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun compound_repsCollapse_decreases() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(5, 4, 3), weight = 40.0))
        assertEquals(Action.DECREASE_WEIGHT, r.action)
        assertTrue(r.suggestedWeight!! < 40.0)
    }

    @Test fun compound_lowerBody_usesLargerIncrement() {
        val r = result(ex("Leg press", 3, 10, 12), session(2, listOf(12, 12, 12), weight = 100.0))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(105.0, r.suggestedWeight!!, 0.001) // lower-body +5
    }

    @Test fun compound_fewerSetsThanTarget_isConservative() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 11), weight = 40.0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun compound_dropSetNotTopRange_keeps() {
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(2, listOf(12, 11, 11), weights = listOf(40.0, 35.0, 30.0)),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun compound_repsHitButAllFailure_keeps() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 11, 11), weight = 40.0, rir = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    // ---- Isolation (target 12-20) -----------------------------------------------------

    @Test fun isolation_midRange_doesNotIncrease() {
        val r = result(ex("Lateral raise", 4, 12, 20), session(2, listOf(12, 12, 12, 12), weight = 10.0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(ProgressionType.ISOLATION_SLOW, r.progressionType)
    }

    @Test fun isolation_singleTopSession_cautiousIncrease() {
        val r = result(ex("Lateral raise", 4, 12, 20), session(2, listOf(20, 20, 20, 20), weight = 10.0))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(12.5, r.suggestedWeight!!, 0.001)
    }

    @Test fun isolation_twoTopSessions_increase() {
        val r = result(
            ex("Pec fly", 3, 12, 20),
            session(3, listOf(20, 20, 20), weight = 15.0), // latest
            session(2, listOf(20, 20, 20), weight = 15.0), // previous
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
    }

    @Test fun isolation_topButPrevWeak_requiresAnotherSession() {
        val r = result(
            ex("Face pull", 3, 12, 20),
            session(3, listOf(20, 20, 20), weight = 20.0), // latest top
            session(2, listOf(14, 14, 14), weight = 20.0), // previous not top
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    // ---- Bodyweight (target 6-10, weight 0) -------------------------------------------

    @Test fun bodyweight_inRange_buildsRepsNotWeight() {
        val r = result(ex("Pull-up", 3, 6, 10), session(2, listOf(8, 8, 8), weight = 0.0))
        assertEquals(Action.INCREASE_REPS_OR_DIFFICULTY, r.action)
        assertEquals(ProgressionType.BODYWEIGHT, r.progressionType)
        assertNull(r.suggestedWeight) // never an automatic +2.5kg
    }

    @Test fun bodyweight_belowMin_keepsBuildsReps() {
        val r = result(ex("Pull-up", 3, 6, 10), session(2, listOf(5, 5, 5), weight = 0.0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertNull(r.suggestedWeight)
    }

    @Test fun bodyweight_topRange_progressDifficulty() {
        val r = result(ex("Pull-up", 3, 6, 10), session(2, listOf(10, 10, 10), weight = 0.0))
        assertEquals(Action.INCREASE_REPS_OR_DIFFICULTY, r.action)
        assertNull(r.suggestedWeight)
    }

    @Test fun bodyweight_withAddedLoad_behavesLikeCompound() {
        val r = result(ex("Pull-up", 3, 6, 10), session(2, listOf(10, 10, 10), weight = 20.0))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(ProgressionType.COMPOUND_LOAD, r.progressionType)
        assertEquals(22.5, r.suggestedWeight!!, 0.001)
    }

    // ---- No / empty history -----------------------------------------------------------

    @Test fun noHistory_logsFirstSession() {
        val s = ProgressiveOverloadEngine.suggestion(ex("Machine chest press", 3, 10, 12), emptyList())
        assertEquals("Log first session", s.title)
    }

    @Test fun onlyInvalidSets_asksForWorkingSet() {
        // sets present but all reps == 0 -> treated as no completed work
        val s = ProgressiveOverloadEngine.suggestion(ex("Machine chest press", 3, 10, 12), session(2, listOf(0, 0, 0), weight = 40.0))
        assertEquals("Log first session", s.title)
    }

    // ---- ProgressionType name inference audit (Task 2.4b) ------------------------------

    private fun typeOf(name: String) = ProgressiveOverloadEngine.progressionTypeForName(name)

    @Test fun nameInference_classifiesRequestedNames() {
        val expected = mapOf(
            // Isolation
            "Lateral raise" to ProgressionType.ISOLATION_SLOW,
            "lateral raise" to ProgressionType.ISOLATION_SLOW,
            "LATERAL RAISE" to ProgressionType.ISOLATION_SLOW,
            "Cable lateral raise" to ProgressionType.ISOLATION_SLOW,
            "Dumbbell lateral raises" to ProgressionType.ISOLATION_SLOW,
            "Side lateral raise" to ProgressionType.ISOLATION_SLOW,
            "Rear delt fly" to ProgressionType.ISOLATION_SLOW,
            "Pec fly" to ProgressionType.ISOLATION_SLOW,
            "Cable curl" to ProgressionType.ISOLATION_SLOW,
            "Triceps pushdown" to ProgressionType.ISOLATION_SLOW,
            // Bodyweight
            "Pull-up" to ProgressionType.BODYWEIGHT,
            "Pull up" to ProgressionType.BODYWEIGHT,
            "Push-up" to ProgressionType.BODYWEIGHT,
            "Push up" to ProgressionType.BODYWEIGHT,
            "Chin-up" to ProgressionType.BODYWEIGHT,
            "Dip" to ProgressionType.BODYWEIGHT,
            "Plank" to ProgressionType.BODYWEIGHT,
            // Compound
            "Lat pulldown" to ProgressionType.COMPOUND_LOAD,
            "Seated row" to ProgressionType.COMPOUND_LOAD,
            "Machine chest press" to ProgressionType.COMPOUND_LOAD,
            "Leg press" to ProgressionType.COMPOUND_LOAD,
            "Romanian deadlift" to ProgressionType.COMPOUND_LOAD,
        )
        expected.forEach { (name, type) ->
            assertEquals("classification of \"$name\"", type, typeOf(name))
        }
    }

    // ---- V3: trend & outlier awareness (Task 2.4c) -------------------------------------

    @Test fun v3_outlier_singleWeakSession_holdsToVerify() {
        // Baseline strong, latest ~47% lower but not at failure: possible bad day / new machine /
        // logging slip — must NOT knee-jerk into a decrease (the V2 too-heavy rule would have).
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(7, 6, 6), weight = 40.0),
            session(2, listOf(12, 12, 12), weight = 40.0),
            session(1, listOf(12, 11, 11), weight = 40.0),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("off day"))
        assertEquals("Hold & verify", r.titleOverride)
    }

    @Test fun v3_outlier_extremeCollapseAtFailure_reduces() {
        // QA case 4: baseline around 12/11/10, latest 5/4/3 at RIR 0 — extreme collapse at failure
        // justifies a confident reduce, floored to a real lower step (40 -> 35).
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(5, 4, 3), weight = 40.0, rir = 0),
            session(2, listOf(12, 11, 10), weight = 40.0),
        )
        assertEquals(Action.DECREASE_WEIGHT, r.action)
        assertEquals(35.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_outlier_repeatedCollapse_reduces() {
        // "Reduce only if this repeats": two sessions in a row well below the older baseline.
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(4, listOf(7, 6, 6), weight = 40.0, rir = 1),
            session(3, listOf(7, 7, 7), weight = 40.0, rir = 1),
            session(2, listOf(12, 12, 12), weight = 40.0),
        )
        assertEquals(Action.DECREASE_WEIGHT, r.action)
        assertEquals(35.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_normalLatestSession_isNotDraggedDownByOldWeakSessions() {
        // Latest carries the most weight: a strong latest session progresses normally even if
        // older history was weaker.
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(12, 11, 11), weight = 40.0),
            session(2, listOf(8, 7, 7), weight = 40.0),
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
    }

    // ---- V3: RIR-gated increases --------------------------------------------------------

    @Test fun v3_compound_repsMetButLowAverageRir_holds() {
        // QA case 2 variant: reps met but average RIR ~1 — hold, don't increase.
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 11, 11), weight = 40.0, rir = 1))
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun v3_compound_strongOvershoot_takesBiggerJump() {
        // QA case 3: 15/14/13 vs a 10-12 target with RIR >= 2 — a doubled (but still safe) jump.
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(15, 14, 13), weight = 40.0, rir = 2))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(45.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_isolation_topRangeAtFailure_holds() {
        // QA case 9: 20/20/20 at RIR 0 — top of range means nothing at failure; hold.
        val r = result(ex("Lateral raise", 3, 12, 20), session(2, listOf(20, 20, 20), weight = 10.0, rir = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    // ---- V3: mixed weights / drop sets / ramps ------------------------------------------

    @Test fun v3_mixedWeights_backoffSetDoesNotDriveSuggestion() {
        // EZ curl screenshot (QA case 12): 20x8, 20x7, 20x11, then a 15kg back-off set.
        // 20kg is the main working weight; the suggestion must not be based on the 15kg set,
        // and with 8/7 under the 10-rep minimum the engine should hold at 20kg.
        val r = result(
            ex("EZ curl optional", 2, 10, 15),
            session(2, listOf(8, 7, 11, 12), weights = listOf(20.0, 20.0, 20.0, 15.0), rir = 2),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(20.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_rampSession_evaluatesTopWeightOnly() {
        // Incline DB curl screenshot: ramped 5 -> 7.5kg with every set at RIR 0. 7.5kg is the
        // working weight (mode/heaviest repeated); the 5kg x12 opener is ramp work, so only 2 of 3
        // working sets exist -> conservative hold at 7.5kg, never an increase off the 5kg set.
        val r = result(
            ex("Incline dumbbell curl", 3, 10, 15),
            session(2, listOf(12, 6, 7), weights = listOf(5.0, 7.5, 7.5), rir = 0),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(7.5, r.suggestedWeight!!, 0.001)
    }

    // ---- V3: collapse handling & decrease rounding ---------------------------------------

    @Test fun v3_hammerCollapse_reducesToRealLowerStep() {
        // Hammer curl screenshot (QA case 13): 7.5kg x 3/4/6 at RIR 0. V2's "reduce 10%" rounded
        // 6.75 back UP to 7.5kg (same weight!); V3 must floor to a genuinely lower step: 5kg.
        val r = result(ex("Hammer curl", 3, 10, 15), session(2, listOf(3, 4, 6), weight = 7.5, rir = 0))
        assertEquals(Action.DECREASE_WEIGHT, r.action)
        assertEquals(5.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_isolation_severeUndershootWithReserve_flagsLogCheck() {
        // QA case E: reps far below target but RIR 3 logged — likely an input/machine mix-up;
        // still steps down, but tells the user to check the log entry first.
        val r = result(ex("Cable curl", 3, 10, 15), session(2, listOf(3, 3, 3), weight = 30.0, rir = 3))
        assertEquals(Action.DECREASE_WEIGHT, r.action)
        assertTrue(r.reason.contains("entered correctly"))
    }

    // ---- V3: bodyweight ------------------------------------------------------------------

    @Test fun v3_bodyweight_topRangeOnce_staysBodyweight() {
        // QA case 10: pull-up 15/15/15 once -> build reps/difficulty, never an immediate +2.5kg.
        val r = result(ex("Pull-up", 3, 10, 15), session(2, listOf(15, 15, 15), weight = 0.0))
        assertEquals(Action.INCREASE_REPS_OR_DIFFICULTY, r.action)
        assertNull(r.suggestedWeight)
    }

    @Test fun v3_outlier_skipsBodyweightVsWeightedComparison() {
        // First weighted session after bodyweight history: the scores aren't comparable, so the
        // outlier rule must not fire; weighted-bodyweight progression applies normally.
        val r = result(
            ex("Pull-up", 3, 6, 10),
            session(3, listOf(6, 6, 6), weight = 10.0, rir = 2),
            session(2, listOf(12, 12, 12), weight = 0.0),
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(12.5, r.suggestedWeight!!, 0.001)
    }

    @Test fun v3_bodyweight_outlierRepCollapse_holdsToVerify() {
        // Rep collapse on a bodyweight move is never a "weight cut" — hold & verify instead.
        val r = result(
            ex("Pull-up", 3, 6, 10),
            session(3, listOf(4, 3, 3), weight = 0.0, rir = 0),
            session(2, listOf(10, 10, 10), weight = 0.0),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertNull(r.suggestedWeight)
        assertEquals("Hold & verify", r.titleOverride)
    }

    @Test fun nameInference_handlesSeparatorAndSpacingVariants() {
        // underscores
        assertEquals(ProgressionType.BODYWEIGHT, typeOf("pull_up"))
        assertEquals(ProgressionType.BODYWEIGHT, typeOf("PUSH_UP"))
        assertEquals(ProgressionType.COMPOUND_LOAD, typeOf("leg_press"))
        // hyphen between separate words + unicode en-dash
        assertEquals(ProgressionType.ISOLATION_SLOW, typeOf("lateral-raise"))
        assertEquals(ProgressionType.BODYWEIGHT, typeOf("chin–up")) // en dash
        // irregular / doubled / surrounding whitespace
        assertEquals(ProgressionType.ISOLATION_SLOW, typeOf("lateral  raise"))
        assertEquals(ProgressionType.COMPOUND_LOAD, typeOf("  Machine  Chest  Press  "))
        // single-word spellings
        assertEquals(ProgressionType.BODYWEIGHT, typeOf("Pullup"))
        assertEquals(ProgressionType.BODYWEIGHT, typeOf("pushup"))
    }

    // ---- V3.5: catalog-aware intelligence ----------------------------------------------

    @Test fun v35_catalogProfile_overridesNameInference_andSkillFormNotCompoundPress() {
        // "Pallof press": name inference would call it a compound press; catalog says SKILL_FORM.
        val ex = ex("Pallof press", 3, 8, 12)
        val nameOnly = ProgressiveOverloadEngine.progression(ex, session(2, listOf(12, 12, 12), weight = 20.0, rir = 2))
        assertEquals(ProgressionType.COMPOUND_LOAD, nameOnly.progressionType)
        assertEquals(Action.INCREASE_WEIGHT, nameOnly.action) // name path would add load

        val withCatalog = ProgressiveOverloadEngine.progression(
            ex, session(2, listOf(12, 12, 12), weight = 20.0, rir = 2),
            profile = profile(ProgressionType.SKILL_FORM),
        )
        assertEquals(ProgressionType.SKILL_FORM, withCatalog.progressionType)
        assertTrue(withCatalog.action != Action.INCREASE_WEIGHT) // skill: never an automatic load bump
    }

    @Test fun v35_customExercise_usesNameInferenceFallback() {
        // No profile (custom exercise) -> existing name inference path is unchanged.
        val r = result(ex("Some weird machine thing", 3, 10, 12), session(2, listOf(12, 12, 12), weight = 40.0, rir = 2))
        assertEquals(ProgressionType.COMPOUND_LOAD, r.progressionType) // unknown name defaults to compound
        assertEquals(Action.INCREASE_WEIGHT, r.action)
    }

    @Test fun v35_mixedWeights_holdAtTopWorkingWeight() {
        // 50/55/55, target 3x8-12, final set short -> hold the 55kg working weight (not the 50kg opener).
        val r = result(ex("Machine chest press", 3, 8, 12), session(2, listOf(12, 12, 7), weights = listOf(50.0, 55.0, 55.0), rir = 1))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(55.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v35_hugeLoadJumpWithIncompleteReps_holdsPositively() {
        // Big improvement (15kg -> 55kg) but the last set fell below target: hold 55 with positive wording.
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(3, listOf(12, 12, 7), weights = listOf(50.0, 55.0, 55.0), rir = 1), // latest
            session(2, listOf(12, 12, 8), weight = 15.0, rir = 1),                       // previous (light)
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(55.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("Strong load increase"))
    }

    @Test fun v35_topRangeWithFailureSet_holdsWithSpecificWording() {
        // 20x12 @2/@2/@0 (target 8-12): top of range but a set hit RIR 0 -> softer, specific hold wording.
        val r = result(ex("Machine chest press", 3, 8, 12), session(2, listOf(12, 12, 12), weight = 20.0, rirs = listOf(2, 2, 0)))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(20.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("RIR 0"))
        assertTrue(r.reason.contains("reps in reserve"))
    }

    @Test fun v35_stableSuccess_increasesWithHigherConfidence() {
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(12, 12, 12), weight = 40.0, rir = 2),
            session(2, listOf(12, 12, 12), weight = 40.0, rir = 2),
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertTrue("confidence ${r.confidence}", r.confidence >= 75)
    }

    @Test fun v35_badDay_holdVerify_isLowerConfidence() {
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(7, 6, 6), weight = 40.0),
            session(2, listOf(12, 12, 12), weight = 40.0),
            session(1, listOf(12, 11, 11), weight = 40.0),
        )
        assertEquals("Hold & verify", r.titleOverride)
        assertTrue("confidence ${r.confidence}", r.confidence <= 70)
    }

    @Test fun v35_bodyweightCatalogProfile_noRandomLoad() {
        val r = ProgressiveOverloadEngine.progression(
            ex("Some plank variation", 3, 8, 12), session(2, listOf(12, 12, 12), weight = 0.0, rir = 2),
            profile = profile(ProgressionType.BODYWEIGHT, inc = 0.0, bw = true),
        )
        assertEquals(ProgressionType.BODYWEIGHT, r.progressionType)
        assertNull(r.suggestedWeight)
        assertTrue(r.action != Action.INCREASE_WEIGHT)
    }

    @Test fun v35_catalogIncrement_usedWhenLargerThanNameBased() {
        val r = ProgressiveOverloadEngine.progression(
            ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 12, 12), weight = 40.0, rir = 2),
            profile = profile(ProgressionType.COMPOUND_LOAD, inc = 5.0),
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(45.0, r.suggestedWeight!!, 0.001) // catalog 5.0 beats name-based 2.5
    }

    // ---- V3.6B: session quality + readiness modifier + baseline ------------------------

    @Test fun v36b_quality_excellent_forCleanTopCompletionGoodRir() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(12, 12, 12), weight = 40.0, rir = 2))
        assertEquals(SessionQuality.EXCELLENT, r.sessionQuality)
    }

    @Test fun v36b_quality_mixed_forFinalSetBelowTarget() {
        val r = result(ex("Machine chest press", 3, 8, 12), session(2, listOf(12, 12, 7), weight = 40.0, rir = 2))
        assertEquals(SessionQuality.MIXED, r.sessionQuality)
    }

    @Test fun v36b_quality_poor_forSevereCollapse() {
        val r = result(ex("Machine chest press", 3, 10, 12), session(2, listOf(4, 3, 3), weight = 40.0, rir = 0))
        assertEquals(SessionQuality.POOR, r.sessionQuality)
    }

    @Test fun v36b_readinessStrong_raisesConfidence_butDoesNotForceIncrease() {
        val ex = ex("Machine chest press", 3, 8, 12)
        val sets = session(2, listOf(12, 12, 12), weight = 20.0, rirs = listOf(1, 1, 1)) // top range, low reserve -> hold
        val normal = prog(ex, sets, readiness = Readiness.NORMAL)
        val strong = prog(ex, sets, readiness = Readiness.STRONG)
        assertEquals(Action.KEEP_WEIGHT, normal.action)
        assertEquals(Action.KEEP_WEIGHT, strong.action) // STRONG must NOT force an increase
        assertEquals(normal.suggestedWeight, strong.suggestedWeight)
        assertTrue("strong=${strong.confidence} normal=${normal.confidence}", strong.confidence > normal.confidence)
    }

    @Test fun v36b_readinessTired_lowersConfidence_butDoesNotForceDecrease() {
        val ex = ex("Machine chest press", 3, 8, 12)
        val sets = session(2, listOf(12, 12, 12), weight = 20.0, rirs = listOf(1, 1, 1))
        val normal = prog(ex, sets, readiness = Readiness.NORMAL)
        val tired = prog(ex, sets, readiness = Readiness.TIRED)
        assertEquals(Action.KEEP_WEIGHT, tired.action) // TIRED must NOT force a decrease
        assertEquals(normal.suggestedWeight, tired.suggestedWeight)
        assertTrue("tired=${tired.confidence} normal=${normal.confidence}", tired.confidence < normal.confidence)
    }

    @Test fun v36b_topRangeFinalRir0_holdsWithSofterWording() {
        val r = result(ex("Machine chest press", 3, 8, 12), session(2, listOf(12, 12, 12), weight = 20.0, rirs = listOf(2, 2, 0)))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(20.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("RIR 0"))
    }

    @Test fun v36b_loadJump_holds55kgPositively() {
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(3, listOf(12, 12, 7), weights = listOf(50.0, 55.0, 55.0), rir = 1),
            session(2, listOf(12, 12, 8), weight = 15.0, rir = 1),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(55.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("Strong load increase"))
    }

    @Test fun v36b_oneSessionDrop_holdAndVerify() {
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(7, 6, 6), weight = 40.0),
            session(2, listOf(12, 12, 12), weight = 40.0),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals("Hold & verify", r.titleOverride)
    }

    @Test fun v36b_repeatedDrop_stillReduces_evenWhenTired() {
        val sets = session(4, listOf(7, 6, 6), weight = 40.0, rir = 1) +
            session(3, listOf(7, 7, 7), weight = 40.0, rir = 1) +
            session(2, listOf(12, 12, 12), weight = 40.0)
        val r = prog(ex("Machine chest press", 3, 10, 12), sets, readiness = Readiness.TIRED)
        assertEquals(Action.DECREASE_WEIGHT, r.action) // safety wins: TIRED does not block a justified reduce
        assertEquals(35.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v36b_sustainedLowerLevel_becomesNewBaseline() {
        // Three recent sessions at the lower 30kg level; an older 40kg peak sits outside the 2-session
        // comparison window, so the latest 30kg session is judged at the new baseline, not as a drop.
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(13, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(12, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(11, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(10, listOf(12, 12, 12), weight = 40.0, rir = 2), // old peak, ignored
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(32.5, r.suggestedWeight!!, 0.001)
        assertNotEquals("Hold & verify", r.titleOverride)
    }

    @Test fun v36b_bodyweightWithReadiness_noRandomLoad() {
        val r = prog(ex("Pull-up", 3, 6, 10), session(2, listOf(10, 10, 10), weight = 0.0, rir = 2), readiness = Readiness.STRONG)
        assertEquals(ProgressionType.BODYWEIGHT, r.progressionType)
        assertNull(r.suggestedWeight)
        assertTrue(r.action != Action.INCREASE_WEIGHT)
    }

    @Test fun v36b_isolation_staysConservative_withStrongReadiness() {
        val r = prog(ex("Lateral raise", 4, 12, 20), session(2, listOf(12, 12, 12, 12), weight = 10.0, rir = 2), readiness = Readiness.STRONG)
        assertEquals(ProgressionType.ISOLATION_SLOW, r.progressionType)
        assertEquals(Action.KEEP_WEIGHT, r.action) // mid-range isolation: STRONG does not force an increase
    }

    @Test fun v36b_skillForm_staysConservative_withReadiness() {
        val r = prog(
            ex("Pallof press", 3, 8, 12),
            session(2, listOf(12, 12, 12), weight = 20.0, rir = 2),
            profile = profile(ProgressionType.SKILL_FORM),
            readiness = Readiness.STRONG,
        )
        assertEquals(ProgressionType.SKILL_FORM, r.progressionType)
        assertTrue(r.action != Action.INCREASE_WEIGHT)
    }

    @Test fun v36b_confidenceVariesWithReadiness_actionAndWeightInvariant() {
        val ex = ex("Machine chest press", 3, 10, 12)
        val sets = session(2, listOf(12, 12, 12), weight = 40.0, rir = 2) // clean increase
        val tired = prog(ex, sets, readiness = Readiness.TIRED)
        val normal = prog(ex, sets, readiness = Readiness.NORMAL)
        val strong = prog(ex, sets, readiness = Readiness.STRONG)
        // Safety-gated decision is invariant across readiness.
        assertEquals(Action.INCREASE_WEIGHT, normal.action)
        assertEquals(normal.action, tired.action)
        assertEquals(normal.action, strong.action)
        assertEquals(normal.suggestedWeight, tired.suggestedWeight)
        assertEquals(normal.suggestedWeight, strong.suggestedWeight)
        // Confidence varies: strong > normal > tired.
        assertTrue(strong.confidence > normal.confidence)
        assertTrue(tired.confidence < normal.confidence)
    }

    // ---- V3.6C Coach Tone Layer (deterministic paraphrase) ----------------------------

    @Test fun v36c_wordingIsDeterministic_sameExerciseRepeatsSameReason() {
        val ex = ex("Machine chest press", 3, 10, 12)
        val sets = session(2, listOf(12, 11, 11), weight = 40.0)
        // Same exercise + same history must always read the same way (no refresh-to-refresh churn).
        assertEquals(prog(ex, sets).reason, prog(ex, sets).reason)
    }

    @Test fun v36c_toneDoesNotChangeActionOrWeight_acrossExerciseNames() {
        // A clean compound increase for several differently-named exercises. A fixed COMPOUND profile
        // pins the type/increment (so weight does not vary by name-inference); wording may vary by name,
        // but the safety-gated action and the suggested weight stay identical — tone never moves either.
        val p = profile(ProgressionType.COMPOUND_LOAD, inc = 2.5)
        listOf("Machine chest press", "Incline Bench Press", "Seated Cable Rows", "Lat Pulldown", "Overhead Press")
            .map { prog(ex(it, 3, 10, 12), session(2, listOf(12, 11, 11), weight = 40.0), profile = p) }
            .forEach {
                assertEquals(Action.INCREASE_WEIGHT, it.action)
                assertEquals(42.5, it.suggestedWeight!!, 0.001)
            }
    }

    @Test fun v36c_increaseWording_hasMoreThanOneVariantAcrossExercises() {
        // The paraphrase layer should produce more than one phrasing across exercises for the same
        // scenario (otherwise it is robotic): stable per name, varied across names. No randomness.
        val reasons = listOf(
            "Machine chest press", "Barbell Squat", "Leg Press", "Seated Cable Rows", "Incline Bench",
            "Lat Pulldown", "Overhead Press", "Romanian Deadlift", "Hack Squat", "Chest Press",
            "Hip Thrust", "Pendlay Row",
        ).map { prog(ex(it, 3, 10, 12), session(2, listOf(12, 11, 11), weight = 40.0)).reason }.toSet()
        assertTrue("expected wording variety across exercises", reasons.size >= 2)
    }

    @Test fun v36c_firstSessionDetail_stablePerExercise_titlePreserved() {
        val first = ex("Barbell Squat", 3, 8, 12)
        val s1 = ProgressiveOverloadEngine.suggestion(first, emptyList())
        val s2 = ProgressiveOverloadEngine.suggestion(first, emptyList())
        assertEquals("Log first session", s1.title) // protected title unchanged
        assertEquals(s1.detail, s2.detail)           // deterministic detail, no churn
    }

    // ---- V3.7A long-term trend intelligence (per-exercise) ----------------------------

    @Test fun v37a_oneSessionBadDay_staysHoldAndVerify_taggedBadDay() {
        // One dip while the prior level was stable: still Hold & verify, now tagged BAD_DAY_VERIFY.
        val r = result(
            ex("Machine chest press", 3, 10, 12),
            session(3, listOf(7, 6, 6), weight = 40.0),
            session(2, listOf(12, 12, 12), weight = 40.0),
            session(1, listOf(12, 11, 11), weight = 40.0),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals("Hold & verify", r.titleOverride)
        assertEquals(CoachInsight.BAD_DAY_VERIFY, r.insight)
        assertTrue(r.reason.contains("off day")) // wording preserved
    }

    @Test fun v37a_repeatedLowerLevel_becomesNewBaseline_stabilize() {
        // Human case: old 55kg peak, then three comparable sessions settle at 40kg (set 1 RIR2, 2 RIR1,
        // 3 RIR0). Not a one-off — treat 40kg as the new baseline and rebuild; do NOT chase 55kg, do NOT
        // increase (40kg is not clearly comfortable), do NOT decrease (reps are handled).
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(13, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(12, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(11, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(10, listOf(10, 10, 10), weight = 55.0, rir = 2), // old peak
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.NEW_BASELINE, r.insight)
        assertEquals("New baseline", r.titleOverride)
        assertTrue(r.reason.contains("40"))
        assertTrue("must not chase the old peak", !r.reason.contains("55"))
    }

    @Test fun v37a_oldPeakDoesNotHauntForever() {
        // Only two recent sessions at the lower 40kg with a 55kg peak still in the naive window: must
        // recognize the settled lower level instead of forever flagging it as a drop vs 55kg.
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(12, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(11, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(10, listOf(10, 10, 10), weight = 55.0, rir = 2),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.NEW_BASELINE, r.insight)
        assertNotEquals("Hold & verify", r.titleOverride)
    }

    @Test fun v37a_plateau_rirImproving_suggestsPushReps() {
        // Same weight & reps for several sessions, RIR trending UP (0 -> 1): room to push, not a deload.
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)),
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0)),
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0)),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertTrue("improving plateau should not suggest a deload", !r.reason.contains("deload"))
    }

    @Test fun v37a_plateau_rirWorsening_suggestsDeload() {
        // Same weight & reps for several sessions, RIR trending DOWN to failure (2 -> 0): near limit.
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 1)),
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(2, 2, 2)),
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(2, 2, 2)),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertTrue(r.reason.contains("deload"))
    }

    @Test fun v37a_regressionNeverDecreases_andAdaptedLevelStillIncreases() {
        // Safety: a settled-lower regression must stay KEEP (never a decrease)...
        val regression = result(
            ex("Machine chest press", 3, 8, 12),
            session(13, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(12, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(11, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(10, listOf(10, 10, 10), weight = 55.0, rir = 2),
        )
        assertEquals(Action.KEEP_WEIGHT, regression.action)
        assertEquals(40.0, regression.suggestedWeight!!, 0.001)
        // ...while a user who has cleanly ADAPTED to the lower level (top range, RIR 2) still increases.
        val adapted = result(
            ex("Machine chest press", 3, 10, 12),
            session(13, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(12, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(11, listOf(12, 12, 12), weight = 30.0, rir = 2),
            session(10, listOf(12, 12, 12), weight = 40.0, rir = 2),
        )
        assertEquals(Action.INCREASE_WEIGHT, adapted.action)
        assertEquals(32.5, adapted.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.NONE, adapted.insight) // a clean increase is never a regression/plateau
    }

    @Test fun v37a_insufficientHistory_keepsV36cBehavior() {
        // Below MIN_TREND_SESSIONS comparable sessions the overlay is inert: no insight, V3.6C behavior.
        val r = result(
            ex("Machine chest press", 3, 8, 12),
            session(2, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)),
            session(1, listOf(10, 10, 10), weight = 55.0, rir = 2),
        )
        assertEquals(CoachInsight.NONE, r.insight)
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun v37a_bodyweight_safetyUnchanged_withDeepHistory() {
        val r = result(
            ex("Pull-up", 3, 6, 10),
            session(3, listOf(8, 8, 8), weight = 0.0),
            session(2, listOf(8, 8, 8), weight = 0.0),
            session(1, listOf(8, 8, 8), weight = 0.0),
        )
        assertEquals(ProgressionType.BODYWEIGHT, r.progressionType)
        assertNull(r.suggestedWeight) // never an automatic load bump
        assertEquals(Action.INCREASE_REPS_OR_DIFFICULTY, r.action)
    }

    @Test fun v37a_isolation_safetyUnchanged_withDeepHistory() {
        val r = result(
            ex("Lateral raise", 4, 12, 20),
            session(3, listOf(12, 12, 12, 12), weight = 10.0),
            session(2, listOf(12, 12, 12, 12), weight = 10.0),
            session(1, listOf(12, 12, 12, 12), weight = 10.0),
        )
        assertEquals(ProgressionType.ISOLATION_SLOW, r.progressionType)
        assertEquals(Action.KEEP_WEIGHT, r.action) // mid-range isolation: build reps, no load bump
        assertEquals(10.0, r.suggestedWeight!!, 0.001)
    }

    @Test fun v37a_skillForm_safetyUnchanged_withDeepHistory() {
        val sets = session(3, listOf(12, 12, 12), weight = 20.0, rir = 2) +
            session(2, listOf(12, 12, 12), weight = 20.0, rir = 2) +
            session(1, listOf(12, 12, 12), weight = 20.0, rir = 2)
        val r = prog(ex("Pallof press", 3, 8, 12), sets, profile = profile(ProgressionType.SKILL_FORM))
        assertEquals(ProgressionType.SKILL_FORM, r.progressionType)
        assertTrue(r.action != Action.INCREASE_WEIGHT) // skill: never an automatic load bump
        assertEquals(CoachInsight.NONE, r.insight)
        assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
        assertTrue(!r.reason.contains("failure", ignoreCase = true))
        assertTrue(!r.reason.contains("hypertrophy", ignoreCase = true))
    }

    // ---- V3.7B readiness trend intelligence -------------------------------------------

    @Test fun v37b_repeatedTired_lowersConfidence_andAddsRecoveryWording_notActionOrWeight() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1))
        val neutral = prog(exercise, sets)
        val tired = progRt(exercise, sets, mapOf(3L to Readiness.TIRED, 2L to Readiness.TIRED, 1L to Readiness.TIRED))
        assertEquals(neutral.action, tired.action)                 // action unchanged
        assertEquals(neutral.suggestedWeight, tired.suggestedWeight) // weight unchanged
        assertTrue("tired=${tired.confidence} neutral=${neutral.confidence}", tired.confidence < neutral.confidence)
        assertTrue(tired.reason.contains("recovery"))
    }

    @Test fun v37b_strongButPoor_keepsSafetyAction_lowersConfidence_changesVerifyWording() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(4, 3, 3), weight = 40.0, rir = 0) // single-session collapse -> decrease
        val plain = prog(exercise, sets)
        val strong = progRt(exercise, sets, mapOf(1L to Readiness.STRONG))
        assertEquals(plain.action, strong.action)                   // safety action unchanged (DECREASE)
        assertEquals(plain.suggestedWeight, strong.suggestedWeight)
        assertTrue(strong.confidence < plain.confidence)
        assertTrue(strong.reason.contains("double-check") || strong.reason.contains("form"))
    }

    @Test fun v37b_normalButPoor_flagsPossibleRealDrop_notActionOrWeight() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(4, 3, 3), weight = 40.0, rir = 0)
        val plain = prog(exercise, sets)
        val normal = progRt(exercise, sets, mapOf(1L to Readiness.NORMAL))
        assertEquals(plain.action, normal.action)
        assertEquals(plain.suggestedWeight, normal.suggestedWeight)
        assertTrue(normal.confidence < plain.confidence)
        assertTrue(normal.reason.contains("verify"))
    }

    @Test fun v37b_unknownReadiness_isNeutral() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1))
        val neutral = prog(exercise, sets)
        val unknown = progRt(exercise, sets, mapOf<Long, Readiness?>(3L to null, 2L to null, 1L to null))
        assertEquals(neutral.confidence, unknown.confidence) // never penalize missing readiness
        assertEquals(neutral.reason, unknown.reason)
    }

    @Test fun v37b_normalPlateau_remainsPlateau() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1))
        val r = progRt(exercise, sets, mapOf(3L to Readiness.NORMAL, 2L to Readiness.NORMAL, 1L to Readiness.NORMAL))
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(Action.KEEP_WEIGHT, r.action)
    }

    @Test fun v37b_tiredButGoodPerformance_doesNotBlockValidProgress() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 40.0, rir = 2) // clean session -> increase
        val r = progRt(exercise, sets, mapOf(1L to Readiness.TIRED), readiness = Readiness.TIRED)
        assertEquals(Action.INCREASE_WEIGHT, r.action) // TIRED must not block a valid increase
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("despite logging a tired day"))
    }

    @Test fun v37b_readinessTrendNeverBypassesRir0Gate() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 20.0, rir = 0) // top range but RIR 0 -> hold
        val strong = progRt(exercise, sets, mapOf(1L to Readiness.STRONG), readiness = Readiness.STRONG)
        assertEquals(Action.KEEP_WEIGHT, strong.action) // STRONG readiness can never unlock a RIR-0 session
        assertEquals(20.0, strong.suggestedWeight!!, 0.001)
    }

    @Test fun v37b_badDayWithRepeatedTired_holdsAndVerifies_withRecoveryWording() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(7, 6, 6), weight = 40.0) +
            session(2, listOf(12, 12, 12), weight = 40.0) +
            session(1, listOf(12, 11, 11), weight = 40.0)
        val tired = progRt(
            exercise, sets,
            mapOf(3L to Readiness.TIRED, 2L to Readiness.TIRED, 1L to Readiness.NORMAL),
            readiness = Readiness.TIRED,
        )
        assertEquals("Hold & verify", tired.titleOverride) // safety/verify action unchanged
        assertEquals(Action.KEEP_WEIGHT, tired.action)
        assertEquals(40.0, tired.suggestedWeight!!, 0.001)
        assertTrue(tired.reason.contains("off day"))    // V3.6B wording preserved
        assertTrue(tired.reason.contains("recovery"))   // V3.7B repeated-tired escalation added
    }

    // ---- V3.7C-H whole-workout + hypertrophy-quality context --------------------------

    // A per-exercise bad-day hold (down vs a stable baseline) reused across the systemic/local tests.
    private fun badDayHold() = ex("Machine chest press", 3, 10, 12) to (
        session(3, listOf(7, 6, 6), weight = 40.0) +
            session(2, listOf(12, 12, 12), weight = 40.0) +
            session(1, listOf(12, 11, 11), weight = 40.0)
    )

    @Test fun v37ch_multipleExercisesDown_systemicFatigue_holdsNotDecrease() {
        val (exercise, sets) = badDayHold()
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 3))
        assertEquals(Action.KEEP_WEIGHT, r.action)          // systemic fatigue never forces a decrease
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals("Hold & verify", r.titleOverride)
        assertEquals(CoachInsight.SYSTEMIC_FATIGUE, r.insight)
        assertTrue(r.reason.contains("whole-session fatigue"))
        assertTrue(r.reason.contains("off day"))            // per-exercise wording preserved
    }

    @Test fun v37ch_onlyOneExerciseDown_localDrop() {
        val (exercise, sets) = badDayHold()
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 1))
        assertEquals(Action.KEEP_WEIGHT, r.action)          // local drop never forces an unsafe increase
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.LOCAL_DROP, r.insight)
        assertTrue(r.reason.contains("this exercise") || r.reason.contains("this lift"))
    }

    @Test fun v37ch_systemicContextOnDecrease_keepsSafetyDecrease() {
        // Repeated collapse -> per-exercise DECREASE; systemic context tags it but never changes the action.
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(4, listOf(7, 6, 6), weight = 40.0, rir = 1) +
            session(3, listOf(7, 7, 7), weight = 40.0, rir = 1) +
            session(2, listOf(12, 12, 12), weight = 40.0)
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 3))
        assertEquals(Action.DECREASE_WEIGHT, r.action)      // safety decrease preserved
        assertEquals(35.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.SYSTEMIC_FATIGUE, r.insight)
    }

    @Test fun v37ch_highProductiveRatio_goodStimulusWording_increaseUnchanged() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(1, listOf(12, 11, 10, 9), weight = 40.0, rirs = listOf(2, 2, 2, 1))
        val r = progWc(exercise, sets, WorkoutContext())
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.HYPERTROPHY_QUALITY, r.insight)
        assertTrue(r.reason.contains("Clean stimulus"))
    }

    @Test fun v37ch_manyHighRirSets_lowerStimulus_notFailure_increaseUnchanged() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12, 12), weight = 40.0, rirs = listOf(4, 4, 4, 3))
        val r = progWc(exercise, sets, WorkoutContext())
        assertEquals(Action.INCREASE_WEIGHT, r.action)      // not auto-failure: reps met -> still an increase
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.HYPERTROPHY_QUALITY, r.insight)
        assertTrue(r.reason.contains("lighter hypertrophy stimulus"))
        assertTrue("must not read as a collapse/decrease", !r.reason.contains("came up short"))
    }

    @Test fun v37ch_isolationAfterCompound_higherRir_isNeutral_notRewarded() {
        // Order-safe: a light isolation with higher RIR is judged on its own — no cross-exercise fatigue
        // reward/penalty, no systemic label when siblings are fine.
        val exercise = ex("Lateral raise", 3, 12, 20)
        val sets = session(1, listOf(14, 14, 14), weight = 10.0, rir = 3)
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(CoachInsight.NONE, r.insight)          // neither rewarded nor penalized
        assertTrue(!r.reason.contains("recovery") && !r.reason.contains("fatigue"))
    }

    @Test fun v37ch_tiredButStrongWorkout_doesNotBlockProgress() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 40.0, rir = 2)
        val r = progWc(
            exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 0),
            readiness = Readiness.TIRED, readinessBySession = mapOf(1L to Readiness.TIRED),
        )
        assertEquals(Action.INCREASE_WEIGHT, r.action)      // TIRED never blocks a valid increase
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("despite logging a tired day"))
    }

    @Test fun v37ch_strongButPoorWholeWorkout_verifyWording_decreaseUnchanged() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(4, 3, 3), weight = 40.0, rir = 0)
        val r = progWc(
            exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 3),
            readiness = Readiness.STRONG, readinessBySession = mapOf(1L to Readiness.STRONG),
        )
        assertEquals(Action.DECREASE_WEIGHT, r.action)      // safety decrease preserved
        assertEquals(35.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("double-check"))       // strong-but-down verify wording (V3.7B)
        assertEquals(CoachInsight.SYSTEMIC_FATIGUE, r.insight)
    }

    @Test fun v37ch_nullContext_isInert_v37bBehaviorUnchanged() {
        // No workout context -> the V3.7C-H overlay is inert (a submaximal 4-set session gets no insight).
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12, 12), weight = 40.0, rirs = listOf(4, 4, 4, 3))
        val r = prog(exercise, sets)
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(CoachInsight.NONE, r.insight)
    }

    @Test fun v37ch_nonHypertrophyGoal_isInert() {
        // Future goal profiles are reserved and no-op for now: STRENGTH context must not add a stimulus tag.
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12, 12), weight = 40.0, rirs = listOf(4, 4, 4, 3))
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.STRENGTH))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(CoachInsight.NONE, r.insight)
    }

    @Test fun v37ch_latestSessionDownVsBaseline_pureHelper() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val down = session(3, listOf(6, 6, 6), weight = 40.0) +
            session(2, listOf(12, 12, 12), weight = 40.0) +
            session(1, listOf(12, 12, 12), weight = 40.0)
        val stable = session(3, listOf(12, 12, 12), weight = 40.0) +
            session(2, listOf(12, 12, 12), weight = 40.0)
        assertEquals(true, ProgressiveOverloadEngine.latestSessionDownVsBaseline(exercise, down))
        assertEquals(false, ProgressiveOverloadEngine.latestSessionDownVsBaseline(exercise, stable))
        assertNull(ProgressiveOverloadEngine.latestSessionDownVsBaseline(exercise, session(1, listOf(10, 10, 10), weight = 40.0)))
    }

    // ---- V3.8-H hypertrophy plateau-exit strategy -------------------------------------

    private val hyperCtx = WorkoutContext(siblingsEvaluated = 2, siblingsDown = 0)

    @Test fun v38h_plateauHighRir_increaseEffort() {
        // Stuck below the rep target while leaving 3 in reserve -> effort deficit.
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 3)
        val r = progWc(exercise, sets, hyperCtx)
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.INCREASE_EFFORT, r.plateauStrategy)
        assertTrue(r.reason.contains("closer to failure"))
    }

    @Test fun v38h_plateauImprovingRir_addRep() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0))
        val r = progWc(exercise, sets, hyperCtx)
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.ADD_REP, r.plateauStrategy)
        assertTrue(r.reason.contains("rep"))
    }

    @Test fun v38h_plateauRepeatedTired_deload_notForcedDecrease() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(0, 0, 0))
        val r = progWc(
            exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 2),
            readinessBySession = mapOf(3L to Readiness.TIRED, 2L to Readiness.TIRED, 1L to Readiness.TIRED),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)      // deload advice never forces a decrease
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.DELOAD, r.plateauStrategy)
        assertTrue(r.reason.contains("deload"))
    }

    @Test fun v38h_deepLocalPlateau_changeRepRange() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(4, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(3, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(1, listOf(8, 8, 8), weight = 50.0, rirs = listOf(1, 1, 1))
        val r = progWc(exercise, sets, hyperCtx)
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.CHANGE_REP_RANGE, r.plateauStrategy)
        assertTrue(r.reason.contains("rep range"))
    }

    @Test fun v38h_localPlateauNoFatigue_addSet() {
        // 3-session stall, adequate effort (RIR ~1), not improving, not deep -> add a set (guarded).
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(10, 10, 10), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(2, listOf(10, 10, 10), weight = 50.0, rirs = listOf(1, 1, 1)) +
            session(1, listOf(10, 10, 10), weight = 50.0, rirs = listOf(1, 1, 1))
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 3, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.ADD_SET, r.plateauStrategy)
        assertTrue(r.reason.contains("set"))
    }

    @Test fun v38h_oneBadDay_stabilizeBaseline_notDeload() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(7, 6, 6), weight = 40.0) +
            session(2, listOf(12, 12, 12), weight = 40.0) +
            session(1, listOf(12, 11, 11), weight = 40.0)
        val r = progWc(exercise, sets, WorkoutContext()) // no siblings -> stays a bad-day hold
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals("Hold & verify", r.titleOverride)
        assertEquals(CoachInsight.BAD_DAY_VERIFY, r.insight)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
        assertTrue("one bad day must not suggest a deload", !r.reason.contains("deload"))
    }

    @Test fun v38h_newBaseline_stabilizeBaseline_notPlateauSpam() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(13, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)) +
            session(12, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)) +
            session(11, listOf(10, 10, 10), weight = 40.0, rirs = listOf(2, 1, 0)) +
            session(10, listOf(10, 10, 10), weight = 55.0, rir = 2)
        val r = progWc(exercise, sets, WorkoutContext())
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(40.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.NEW_BASELINE, r.insight)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
    }

    @Test fun v38h_sparseHistory_noAggressiveStrategy() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(2, listOf(10, 10, 10), weight = 40.0, rir = 2) +
            session(1, listOf(10, 10, 10), weight = 40.0, rir = 2)
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.INCREASE_WEIGHT, r.action)
        assertEquals(42.5, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
    }

    @Test fun v38h_rir0Plateau_gateBlocksIncrease_actionWeightUnchanged() {
        // Top-range but at RIR 0: the safety gate holds (KEEP, not increase); strategy never overrides it.
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(3, listOf(12, 12, 12), weight = 50.0, rir = 0) +
            session(2, listOf(12, 12, 12), weight = 50.0, rir = 0) +
            session(1, listOf(12, 12, 12), weight = 50.0, rir = 0)
        val r = progWc(exercise, sets, hyperCtx)
        assertEquals(Action.KEEP_WEIGHT, r.action)      // RIR 0 blocks the increase despite top-range reps
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.plateauStrategy != PlateauStrategy.INCREASE_EFFORT) // never "push harder" at RIR 0
    }

    @Test fun v38h_isolationAndBodyweight_safetyUnchanged_underPlateauStrategy() {
        // Isolation plateau: action/weight untouched by any plateau strategy tag.
        val iso = progWc(
            ex("Lateral raise", 4, 12, 20),
            session(3, listOf(12, 12, 12, 12), weight = 10.0, rir = 1) +
                session(2, listOf(12, 12, 12, 12), weight = 10.0, rir = 1) +
                session(1, listOf(12, 12, 12, 12), weight = 10.0, rir = 1),
            hyperCtx,
        )
        assertEquals(ProgressionType.ISOLATION_SLOW, iso.progressionType)
        assertEquals(Action.KEEP_WEIGHT, iso.action)
        assertEquals(10.0, iso.suggestedWeight!!, 0.001)
        // Bodyweight plateau: never an automatic load bump.
        val bw = progWc(
            ex("Pull-up", 3, 6, 10),
            session(3, listOf(8, 8, 8), weight = 0.0) +
                session(2, listOf(8, 8, 8), weight = 0.0) +
                session(1, listOf(8, 8, 8), weight = 0.0),
            hyperCtx,
        )
        assertEquals(ProgressionType.BODYWEIGHT, bw.progressionType)
        assertNull(bw.suggestedWeight)
        assertTrue(bw.action != Action.INCREASE_WEIGHT)
    }

    // ---- V3.9-G goal-aware layer + wording fixes ---------------------------------------

    /** Duration-only (TIME_ONLY-style) sets: reps = 0 sentinel, real durationSeconds. */
    private fun durationSession(sessionId: Long, durations: List<Int>): List<LoggedSetEntity> =
        durations.mapIndexed { i, d ->
            LoggedSetEntity(
                id = sessionId * 100 + i,
                sessionId = sessionId,
                exerciseId = 1,
                exerciseNameSnapshot = "x",
                setNumber = i + 1,
                weight = 0.0,
                reps = 0,
                rir = 0,
                notes = null,
                isPrWeight = false,
                isPrReps = false,
                durationSeconds = d,
            )
        }

    private fun metricSession(
        sessionId: Long,
        durations: List<Int?> = emptyList(),
        distances: List<Double?> = emptyList(),
        weight: Double = 0.0,
    ): List<LoggedSetEntity> {
        val count = maxOf(durations.size, distances.size)
        return List(count) { i ->
            LoggedSetEntity(
                id = sessionId * 100 + i,
                sessionId = sessionId,
                exerciseId = 1,
                exerciseNameSnapshot = "x",
                setNumber = i + 1,
                weight = weight,
                reps = 0,
                rir = 0,
                notes = null,
                isPrWeight = false,
                isPrReps = false,
                durationSeconds = durations.getOrNull(i),
                distanceMeters = distances.getOrNull(i),
            )
        }
    }

    private fun metricEvidence(
        status: MetricProgressionStatus,
        dimension: MetricEvidenceDimension = MetricEvidenceDimension.NONE,
        trackingType: ProgressionTrackingType = ProgressionTrackingType.TIME_DISTANCE,
        inferred: Boolean = false,
    ) = MetricProgressionResult(
        trackingType = trackingType,
        status = status,
        suggestion = OverloadSuggestion("Neutral", "Neutral metric evidence."),
        evidenceDimension = dimension,
        trackingWasInferred = inferred,
    )

    private fun metricPolicy(
        evidence: MetricProgressionResult,
        goal: WorkoutGoalProfile,
        readiness: Readiness? = null,
    ) = ProgressiveOverloadEngine.metricGoalPolicy(evidence, goal, readiness)

    @Test fun v39g_rir0PluralWording_twoSetsAtFailure() {
        // Machine Bench Press QA screenshot: 60kg x 12 @2, 12 @0, 12 @0 — TWO sets hit RIR 0.
        val r = result(
            ex("Machine Bench Press", 3, 8, 12),
            session(1, listOf(12, 12, 12), weight = 60.0, rirs = listOf(2, 0, 0)),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(60.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("2 sets reached RIR 0"))
        assertTrue(!r.reason.contains("one set reached RIR 0"))
    }

    @Test fun v39g_rir0SingularWording_oneSetAtFailure() {
        val r = result(
            ex("Machine Bench Press", 3, 8, 12),
            session(1, listOf(12, 12, 12), weight = 60.0, rirs = listOf(2, 2, 0)),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertTrue(r.reason.contains("one set reached RIR 0"))
    }

    @Test fun v39g_productiveClause_suppressedWhenASetHitFailure() {
        // Avg effective reps is in the "productive" band, but one set actually hit RIR 0 — praising a
        // "clean stimulus" next to the RIR-0 caution would contradict it, so the clause must not appear.
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(1, listOf(12, 11, 10, 9), weight = 40.0, rirs = listOf(2, 1, 0, 1))
        val r = progWc(exercise, sets, WorkoutContext())
        assertTrue(r.reason.contains("RIR 0"))
        assertTrue(!r.reason.contains("Clean stimulus"))
        assertEquals(CoachInsight.NONE, r.insight)
    }

    @Test fun v39g_isolationTopRangeFinalRir0_holdsWithPlainWording() {
        // Cable Seated Lateral Raise QA screenshot: top-range reps with only the final set at RIR 0.
        // This must hold the load without a technical stimulus aside or a contradictory praise.
        val r = result(
            ex("Cable Seated Lateral Raise", 3, 12, 15),
            session(1, listOf(15, 15, 17), weight = 10.0, rirs = listOf(2, 2, 0)),
        )
        assertEquals(ProgressionType.ISOLATION_SLOW, r.progressionType)
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(10.0, r.suggestedWeight!!, 0.001)
        assertTrue(r.reason.contains("finished right at your limit"))
        assertTrue(!r.reason.contains("Clean stimulus"))
        assertTrue(!r.reason.contains("junk volume"))
    }

    @Test fun v39g_strengthPlateau_neverAddSet_patienceWording() {
        // Same input gives ADD_SET under hypertrophy; an explicit STRENGTH goal must not add junk volume.
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 2)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.STRENGTH, siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(CoachInsight.PLATEAU, r.insight)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
        assertTrue(r.reason.contains("Strength moves slower"))
    }

    @Test fun v39g_strengthInferredFromLowRepTargets_underDefaultGoal() {
        // No goal UI exists: a 3-5-rep prescription IS strength work, so the default-goal context
        // resolves the effective goal from the rep targets (max <= 6 -> STRENGTH).
        val exercise = ex("Barbell Back Squat", 3, 3, 5)
        val sets = session(3, listOf(3, 3, 3), weight = 100.0, rir = 1) +
            session(2, listOf(3, 3, 3), weight = 100.0, rir = 1) +
            session(1, listOf(3, 3, 3), weight = 100.0, rir = 1)
        val r = progWc(exercise, sets, WorkoutContext())
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(100.0, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
        assertTrue(r.reason.contains("Strength moves slower"))
    }

    @Test fun v310_task3_skillFormDefaultContext_staysOverlayInert() {
        val exercise = ex("Pallof press", 3, 8, 12)
        val sets = session(3, listOf(12, 12, 12), weight = 20.0, rir = 3) +
            session(2, listOf(12, 12, 12), weight = 20.0, rir = 3) +
            session(1, listOf(12, 12, 12), weight = 20.0, rir = 3)
        val catalogProfile = profile(ProgressionType.SKILL_FORM)
        val withoutContext = prog(exercise, sets, catalogProfile)
        val withDefaultContext = progWc(exercise, sets, WorkoutContext(), catalogProfile)

        assertEquals(ProgressionType.SKILL_FORM, withDefaultContext.progressionType)
        assertEquals(withoutContext.action, withDefaultContext.action)
        assertEquals(withoutContext.suggestedWeight, withDefaultContext.suggestedWeight)
        assertEquals(withoutContext.reason, withDefaultContext.reason)
        assertEquals(PlateauStrategy.NONE, withDefaultContext.plateauStrategy)
        assertTrue(!withDefaultContext.reason.contains("hypertrophy", ignoreCase = true))
        assertTrue(!withDefaultContext.reason.contains("closer to failure"))
    }

    @Test fun v310_task3_strengthHighReservePlateau_usesTechniqueNotFailureWording() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 3)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.STRENGTH, siblingsEvaluated = 2))

        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.INCREASE_EFFORT, r.plateauStrategy)
        assertTrue(r.reason.contains("crisp") || r.reason.contains("controlled"))
        assertTrue(!r.reason.contains("closer to failure"))
        assertTrue(!r.reason.contains("RIR"))
    }

    @Test fun v39g_powerPlateau_neverIncreaseEffort_noGrindAdvice() {
        // High-RIR plateau = INCREASE_EFFORT for hypertrophy, but power work must never be pushed
        // toward failure — grinding degrades speed work.
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 3) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 3)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.POWER, siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(50.0, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
        assertTrue(!r.reason.contains("closer to failure"))
        assertTrue(r.reason.contains("fast"))
    }

    @Test fun v39g_powerGrindCaution_lowRirSession() {
        // Power session ground out at RIR ~1: quality caution appears; action/weight stay safety-decided.
        val exercise = ex("Power Clean", 3, 3, 5)
        val sets = session(1, listOf(5, 5, 5), weight = 60.0, rir = 1)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.POWER))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(CoachInsight.GOAL_QUALITY, r.insight)
        assertTrue(r.reason.contains("reserve") || r.reason.contains("speed"))
    }

    @Test fun v39g_endurancePlateau_prefersRepsAndDensity() {
        // 20-30-rep prescription under the default goal reads as endurance: a stall with reserve left
        // progresses on reps/time/density, never "push closer to failure".
        val exercise = ex("Cable lateral raise", 3, 20, 30)
        val sets = session(3, listOf(22, 22, 22), weight = 10.0, rir = 3) +
            session(2, listOf(22, 22, 22), weight = 10.0, rir = 3) +
            session(1, listOf(22, 22, 22), weight = 10.0, rir = 3)
        val r = progWc(exercise, sets, WorkoutContext(siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(10.0, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.ADD_REP, r.plateauStrategy)
        assertTrue(r.reason.contains("more work, not more load") || r.reason.contains("volume density"))
        assertTrue(!r.reason.contains("closer to failure"))
    }

    @Test fun v39g_enduranceHighRir_noSubmaximalShaming() {
        // High reserve at high reps is normal endurance training — never judged a weak hypertrophy stimulus.
        val exercise = ex("Face pull", 3, 20, 30)
        val sets = session(1, listOf(21, 21, 21, 21), weight = 20.0, rir = 4)
        val r = progWc(exercise, sets, WorkoutContext())
        assertEquals(CoachInsight.NONE, r.insight)
        assertTrue(!r.reason.contains("lighter hypertrophy stimulus"))
    }

    @Test fun v39g_generalFitness_sustainableAdvice() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(9, 9, 9), weight = 30.0, rir = 2) +
            session(2, listOf(9, 9, 9), weight = 30.0, rir = 2) +
            session(1, listOf(9, 9, 9), weight = 30.0, rir = 2)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.GENERAL_FITNESS, siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(30.0, r.suggestedWeight!!, 0.001)
        assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
        assertTrue(r.reason.contains("consistency"))
    }

    @Test fun v39g_goalLayer_actionAndWeightInvariant_acrossGoals() {
        // The goal layer is wording/strategy only: same input, same action and suggested weight for
        // every goal (and for no context at all).
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 2)
        val base = ProgressiveOverloadEngine.progression(exercise, sets)
        val goals = listOf(
            WorkoutGoalProfile.HYPERTROPHY, WorkoutGoalProfile.STRENGTH, WorkoutGoalProfile.POWER,
            WorkoutGoalProfile.ENDURANCE, WorkoutGoalProfile.SKILL, WorkoutGoalProfile.GENERAL_FITNESS,
        )
        for (goal in goals) {
            val r = progWc(exercise, sets, WorkoutContext(goal = goal, siblingsEvaluated = 2, siblingsDown = 0))
            assertEquals("action must not vary by goal ($goal)", base.action, r.action)
            assertEquals("weight must not vary by goal ($goal)", base.suggestedWeight!!, r.suggestedWeight!!, 0.001)
        }
    }

    @Test fun v39g_skillGoal_noPlateauPrescription() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(3, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(2, listOf(8, 8, 8), weight = 50.0, rir = 2) +
            session(1, listOf(8, 8, 8), weight = 50.0, rir = 2)
        val r = progWc(exercise, sets, WorkoutContext(goal = WorkoutGoalProfile.SKILL, siblingsEvaluated = 2, siblingsDown = 0))
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
    }

    @Test fun v39g_nonRep_durationOnly_safeDegradation_withTrend() {
        // TIME_ONLY-style history (reps = 0 sentinels, real durations) must never produce
        // "Log first session … 0-0 reps" — it degrades to a load-free time read with a simple trend.
        val exercise = ex("Plank", 3, 0, 0)
        val sets = durationSession(2, listOf(60, 60, 60)) + durationSession(1, listOf(50, 50, 50))
        val s = ProgressiveOverloadEngine.suggestion(exercise, sets)
        assertEquals("Build time", s.title)
        assertNull(s.suggestedWeight)
        assertTrue(s.detail.contains("Up from 2:30 to 3:00"))
        assertTrue(!s.detail.contains("0-0"))
    }

    @Test fun v39g_nonRep_firstSession_noFakeRepTarget() {
        val exercise = ex("Plank", 3, 0, 0)
        val s = ProgressiveOverloadEngine.suggestion(exercise, durationSession(1, listOf(60, 60, 60)))
        assertEquals("Build time", s.title)
        assertNull(s.suggestedWeight)
        assertTrue(s.detail.contains("3:00 total"))
        assertTrue(!s.detail.contains("reps"))
    }

    // ---- V3.11 multi-metric progression contract -------------------------------------

    @Test fun v311_timeOnly_improvementPlateauAndIncomplete_areDeterministic() {
        val improved = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_ONLY,
            metricSession(2, durations = listOf(60, 65, 70)) +
                metricSession(1, durations = listOf(55, 60, 65)),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, improved.status)
        assertNull(improved.suggestion.suggestedWeight)

        val plateau = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_ONLY,
            metricSession(2, durations = listOf(60, 60)) +
                metricSession(1, durations = listOf(60, 60)),
        )
        assertEquals(MetricProgressionStatus.PLATEAU, plateau.status)

        val incomplete = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_ONLY,
            metricSession(2, durations = listOf(60, null)) +
                metricSession(1, durations = listOf(60, 60)),
        )
        assertEquals(MetricProgressionStatus.INCOMPLETE, incomplete.status)
        assertNull(incomplete.suggestion.suggestedWeight)
    }

    @Test fun v311_distanceOnly_sameSetsGreaterDistance_isImprovement() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.DISTANCE_ONLY,
            metricSession(2, distances = listOf(30.0, 30.0)) +
                metricSession(1, distances = listOf(25.0, 25.0)),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, r.status)
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311_distanceLoad_sameLoadGreaterDistance_isImprovementWithoutLoadAdvice() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.DISTANCE_LOAD,
            metricSession(2, distances = listOf(30.0, 30.0), weight = 40.0) +
                metricSession(1, distances = listOf(25.0, 25.0), weight = 40.0),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, r.status)
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311_distanceLoad_sameDistanceHigherLoad_isImprovementWithoutAutoIncrease() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.DISTANCE_LOAD,
            metricSession(2, distances = listOf(30.0, 30.0), weight = 44.0) +
                metricSession(1, distances = listOf(30.0, 30.0), weight = 40.0),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, r.status)
        assertNull(r.suggestion.suggestedWeight)
        assertTrue(r.suggestion.detail.contains("no automatic load increase"))
    }

    @Test fun v311_loadBearingMetric_zeroOrNegativeLoad_isIncomplete() {
        for (weight in listOf(0.0, -10.0)) {
            val r = ProgressiveOverloadEngine.metricProgression(
                ProgressionTrackingType.DISTANCE_LOAD,
                metricSession(2, distances = listOf(30.0), weight = weight) +
                    metricSession(1, distances = listOf(25.0), weight = weight),
            )
            assertEquals(weight.toString(), MetricProgressionStatus.INCOMPLETE, r.status)
            assertNull(weight.toString(), r.suggestion.suggestedWeight)
        }
    }

    @Test fun v311_timeDistance_sameDistanceFasterTime_isComparablePaceImprovement() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE,
            metricSession(2, durations = listOf(25, 25), distances = listOf(50.0, 50.0)) +
                metricSession(1, durations = listOf(30, 30), distances = listOf(50.0, 50.0)),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, r.status)
        assertEquals("Improve pace", r.suggestion.title)
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311_timeDistance_bothTimeAndDistanceChanged_isIncomparable() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE,
            metricSession(2, durations = listOf(25, 25), distances = listOf(60.0, 60.0)) +
                metricSession(1, durations = listOf(30, 30), distances = listOf(50.0, 50.0)),
        )
        assertEquals(MetricProgressionStatus.INCOMPARABLE, r.status)
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311_sled_loadChangeBlocksPaceComparisonAndNeverPrescribesLoad() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE_LOAD,
            metricSession(2, durations = listOf(25), distances = listOf(50.0), weight = 90.0) +
                metricSession(1, durations = listOf(30), distances = listOf(50.0), weight = 80.0),
        )
        assertEquals(MetricProgressionStatus.INCOMPARABLE, r.status)
        assertNull(r.suggestion.suggestedWeight)
        assertTrue(r.suggestion.detail.contains("Pace is only compared"))
    }

    @Test fun v311_sled_sameLoadDistanceFasterTime_isImprovementWithSetupCaveat() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE_LOAD,
            metricSession(2, durations = listOf(25), distances = listOf(50.0), weight = 80.0) +
                metricSession(1, durations = listOf(30), distances = listOf(50.0), weight = 80.0),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, r.status)
        assertNull(r.suggestion.suggestedWeight)
        assertTrue(r.suggestion.detail.contains("sled and surface matched"))

        val higherLoad = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE_LOAD,
            metricSession(2, durations = listOf(30), distances = listOf(50.0), weight = 90.0) +
                metricSession(1, durations = listOf(30), distances = listOf(50.0), weight = 80.0),
        )
        assertEquals(MetricProgressionStatus.IMPROVED, higherLoad.status)
        assertTrue(higherLoad.suggestion.detail.contains("if the sled and surface matched"))
        assertTrue(!higherLoad.suggestion.detail.contains("on the same setup"))
    }

    @Test fun v311_changedSetCount_degradesToIncomparable() {
        val r = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_ONLY,
            metricSession(2, durations = listOf(60, 60, 60)) +
                metricSession(1, durations = listOf(60, 60)),
        )
        assertEquals(MetricProgressionStatus.INCOMPARABLE, r.status)
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311_unknownConsistentMetrics_infersSafely_butMixedHistoryDoesNot() {
        val inferred = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.UNKNOWN,
            metricSession(2, distances = listOf(30.0), weight = 40.0) +
                metricSession(1, distances = listOf(25.0), weight = 40.0),
        )
        assertEquals(ProgressionTrackingType.DISTANCE_LOAD, inferred.trackingType)
        assertEquals(MetricProgressionStatus.IMPROVED, inferred.status)
        assertNull(inferred.suggestion.suggestedWeight)

        val mixed = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.UNKNOWN,
            metricSession(2, durations = listOf(60)) +
                metricSession(1, distances = listOf(50.0)),
        )
        assertEquals(MetricProgressionStatus.INCOMPARABLE, mixed.status)
        assertNull(mixed.suggestion.suggestedWeight)
    }

    @Test fun v311_nonRepProfileWithRepRows_neverFallsIntoLoadProgression() {
        val exercise = ex("Farmer's Walk", 3, 0, 0)
        val profile = profile(ProgressionType.COMPOUND_LOAD).copy(
            trackingType = ProgressionTrackingType.DISTANCE_LOAD,
        )
        val r = ProgressiveOverloadEngine.suggestion(
            exercise,
            session(1, listOf(10, 10, 10), weight = 40.0),
            profile,
        )
        assertEquals("Hold & compare", r.title)
        assertNull(r.suggestedWeight)
    }

    @Test fun v311_powerAndSkillRemainAdvisoryWithoutVelocityOrQualityMetric() {
        val skill = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.SKILL_FORM,
            emptyList(),
        )
        assertEquals(MetricProgressionStatus.ADVISORY_ONLY, skill.status)
        assertEquals("Form first", skill.suggestion.title)
        assertNull(skill.suggestion.suggestedWeight)

        val exercise = ex("Power Clean", 3, 3, 5)
        val profile = profile(ProgressionType.SKILL_FORM).copy(
            trackingType = ProgressionTrackingType.SKILL_FORM,
        )
        val power = progWc(
            exercise,
            session(1, listOf(5, 5, 5), weight = 60.0, rir = 1),
            WorkoutContext(goal = WorkoutGoalProfile.POWER),
            profile,
        )
        assertTrue(power.action != Action.INCREASE_WEIGHT)
        assertEquals(60.0, power.suggestedWeight!!, 0.001)
        assertTrue(!power.reason.contains("power output improved", ignoreCase = true))
    }

    @Test fun v311_repsLoadActionAndWeight_areUnchangedByExplicitTrackingContract() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 40.0, rir = 2)
        val legacy = ProgressiveOverloadEngine.progression(exercise, sets)
        val explicit = ProgressiveOverloadEngine.progression(
            exercise,
            sets,
            profile(ProgressionType.COMPOUND_LOAD).copy(
                trackingType = ProgressionTrackingType.REPS_WEIGHT,
            ),
        )
        assertEquals(legacy.action, explicit.action)
        assertEquals(legacy.suggestedWeight!!, explicit.suggestedWeight!!, 0.001)
    }

    // ---- V3.11B-MG multi-goal metric policy ------------------------------------------

    @Test fun v311b_hypertrophyMetricPolicy_preservesV311aOutput() {
        val evidence = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_ONLY,
            metricSession(2, durations = listOf(65, 65)) +
                metricSession(1, durations = listOf(60, 60)),
        )
        val policy = metricPolicy(evidence, WorkoutGoalProfile.HYPERTROPHY)

        assertEquals(evidence.suggestion, policy.suggestion)
        assertEquals(evidence, policy.evidence)
        assertEquals(PlateauStrategy.NONE, policy.plateauStrategy)
        assertNull(policy.suggestion.suggestedWeight)
    }

    @Test fun v311b_explicitHypertrophySource_bypassesLowAndHighRepGoalInference() {
        val fixtures = listOf(
            ex("Machine chest press", 3, 3, 5) to session(2, listOf(5, 5, 5), weight = 40.0, rir = 2),
            ex("Machine chest press", 3, 20, 25) to session(1, listOf(25, 25, 25), weight = 20.0, rir = 2),
        )
        for ((exercise, sets) in fixtures) {
            val auto = progWc(exercise, sets, WorkoutContext())
            val explicit = progWc(
                exercise,
                sets,
                WorkoutContext(goal = WorkoutGoalProfile.HYPERTROPHY, goalSource = GoalSource.EXPLICIT),
            )
            assertEquals(exercise.targetMinReps.toString(), auto.action, explicit.action)
            assertEquals(exercise.targetMinReps.toString(), auto.suggestedWeight, explicit.suggestedWeight)
            assertTrue(auto.insight != CoachInsight.HYPERTROPHY_QUALITY)
            assertEquals(CoachInsight.HYPERTROPHY_QUALITY, explicit.insight)
        }
    }

    @Test fun v311b_strengthImproved_distinguishesLoadFromWorkCapacity() {
        val loadEvidence = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.DISTANCE_LOAD,
            metricSession(2, distances = listOf(30.0), weight = 45.0) +
                metricSession(1, distances = listOf(30.0), weight = 40.0),
        )
        val loadPolicy = metricPolicy(loadEvidence, WorkoutGoalProfile.STRENGTH)
        assertEquals(MetricEvidenceDimension.LOAD, loadEvidence.evidenceDimension)
        assertEquals(MetricPolicyConfidence.MODERATE, loadPolicy.confidence)
        assertTrue(loadPolicy.suggestion.detail.contains("loaded-task progression signal"))
        assertTrue(!loadPolicy.suggestion.detail.contains("failure"))
        assertNull(loadPolicy.suggestion.suggestedWeight)

        val timePolicy = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.DURATION),
            WorkoutGoalProfile.STRENGTH,
        )
        assertEquals(MetricPolicyConfidence.LOW, timePolicy.confidence)
        assertTrue(timePolicy.suggestion.detail.contains("does not prove a strength gain"))
    }

    @Test fun v311b_strengthPlateauAndRegression_stabilizeWithoutFailureChasing() {
        for (status in listOf(MetricProgressionStatus.PLATEAU, MetricProgressionStatus.REGRESSED)) {
            val r = metricPolicy(
                metricEvidence(status, MetricEvidenceDimension.LOAD, ProgressionTrackingType.DISTANCE_LOAD),
                WorkoutGoalProfile.STRENGTH,
            )
            assertEquals(PlateauStrategy.STABILIZE_BASELINE, r.plateauStrategy)
            assertNull(r.suggestion.suggestedWeight)
            assertTrue(!r.suggestion.detail.contains("closer to failure"))
            assertTrue(!r.suggestion.detail.contains("RIR 0"))
        }
    }

    @Test fun v311b_strengthLowRepRir0_remainsConservative() {
        val exercise = ex("Barbell Back Squat", 3, 3, 5)
        val r = progWc(
            exercise,
            session(1, listOf(5, 5, 5), weight = 100.0, rirs = listOf(0, 3, 3)),
            WorkoutContext(goal = WorkoutGoalProfile.STRENGTH),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(100.0, r.suggestedWeight!!, 0.001)
        assertEquals("Strength: repeat cleanly", r.titleOverride)
        assertTrue(!r.reason.contains("closer to failure"))
        assertTrue(r.plateauStrategy != PlateauStrategy.INCREASE_EFFORT)
    }

    @Test fun v311b_powerMetricStatuses_areAlwaysAdvisoryWithoutMeasuredOutput() {
        for (status in listOf(
            MetricProgressionStatus.IMPROVED,
            MetricProgressionStatus.PLATEAU,
            MetricProgressionStatus.REGRESSED,
        )) {
            val r = metricPolicy(
                metricEvidence(status, MetricEvidenceDimension.PACE, ProgressionTrackingType.TIME_DISTANCE),
                WorkoutGoalProfile.POWER,
            )
            assertEquals(MetricPolicyConfidence.LOW, r.confidence)
            assertEquals("Power remains advisory", r.suggestion.title)
            assertTrue(r.suggestion.detail.contains("were not measured"))
            assertTrue(!r.suggestion.detail.contains("power improved", ignoreCase = true))
            assertTrue(!r.suggestion.detail.contains("output improved", ignoreCase = true))
            assertNull(r.suggestion.suggestedWeight)
        }
    }

    @Test fun v311b_powerRepCompletion_cannotAutoIncreaseLoadWithoutOutput() {
        val exercise = ex("Power Clean", 3, 8, 12)
        val actualCatalogShape = profile(ProgressionType.COMPOUND_LOAD).copy(
            trackingType = ProgressionTrackingType.REPS_WEIGHT,
        )
        val r = progWc(
            exercise,
            session(1, listOf(12, 12, 12), weight = 60.0, rir = 3),
            WorkoutContext(goal = WorkoutGoalProfile.POWER),
            actualCatalogShape,
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(60.0, r.suggestedWeight!!, 0.001)
        assertEquals("Power: hold load", r.titleOverride)
        assertTrue(r.reason.contains("cannot verify power output"))
        assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
    }

    @Test fun v311b_powerAnyRir0_blocksProgression_evenWhenAverageRirPasses() {
        val exercise = ex("Power Clean", 3, 8, 12)
        val r = progWc(
            exercise,
            session(1, listOf(12, 12, 12), weight = 60.0, rirs = listOf(0, 3, 3)),
            WorkoutContext(goal = WorkoutGoalProfile.POWER),
            profile(ProgressionType.COMPOUND_LOAD),
        )
        assertEquals(Action.KEEP_WEIGHT, r.action)
        assertEquals(60.0, r.suggestedWeight!!, 0.001)
        assertEquals("Power: keep reserve", r.titleOverride)
        assertTrue(r.reason.contains("RIR 0"))
        assertTrue(!r.reason.contains("increase", ignoreCase = true))
    }

    @Test fun v311b_repSkillAndUnknownGoals_cannotInheritAutomaticLoadProgression() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 60.0, rir = 3)
        for (goal in listOf(WorkoutGoalProfile.SKILL, WorkoutGoalProfile.UNKNOWN)) {
            val r = progWc(exercise, sets, WorkoutContext(goal = goal))
            assertEquals(goal.toString(), Action.KEEP_WEIGHT, r.action)
            assertEquals(goal.toString(), 60.0, r.suggestedWeight!!, 0.001)
            assertEquals(goal.toString(), PlateauStrategy.NONE, r.plateauStrategy)
            assertTrue(goal.toString(), !r.reason.contains("add weight", ignoreCase = true))
        }

        val catalogSkill = prog(
            ex("Pallof press", 3, 8, 12),
            session(2, listOf(12, 12, 12), weight = 20.0, rir = 3),
            profile(ProgressionType.SKILL_FORM),
        )
        assertEquals(Action.KEEP_WEIGHT, catalogSkill.action)
        assertEquals("Form first", catalogSkill.titleOverride)
        assertTrue(catalogSkill.reason.contains("do not measure technique quality"))
        assertTrue(!catalogSkill.reason.contains("failure", ignoreCase = true))
    }

    @Test fun v311b_enduranceTimeImprovement_isPolicyAndProductionReachable() {
        val exercise = ex("Plank", 2, 0, 0)
        val sets = metricSession(2, durations = listOf(70, 70)) +
            metricSession(1, durations = listOf(60, 60))
        val evidence = ProgressiveOverloadEngine.metricProgression(ProgressionTrackingType.TIME_ONLY, sets)
        val policy = metricPolicy(evidence, WorkoutGoalProfile.ENDURANCE)
        val integrated = ProgressiveOverloadEngine.suggestion(
            exercise,
            sets,
            profile = profile(ProgressionType.COMPOUND_LOAD).copy(
                trackingType = ProgressionTrackingType.TIME_ONLY,
            ),
            workoutContext = WorkoutContext(goal = WorkoutGoalProfile.ENDURANCE),
        )
        assertEquals(MetricEvidenceDimension.DURATION, evidence.evidenceDimension)
        assertEquals(MetricPolicyConfidence.HIGH, policy.confidence)
        assertEquals(policy.suggestion, integrated)
        assertNull(integrated.suggestedWeight)
    }

    @Test fun v311b_enduranceDistanceImprovement_isValidEvidence() {
        val evidence = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.DISTANCE_ONLY,
            metricSession(2, distances = listOf(60.0)) + metricSession(1, distances = listOf(50.0)),
        )
        val r = metricPolicy(evidence, WorkoutGoalProfile.ENDURANCE)
        assertEquals(MetricEvidenceDimension.DISTANCE, evidence.evidenceDimension)
        assertEquals(MetricPolicyConfidence.HIGH, r.confidence)
        assertTrue(r.suggestion.detail.contains("distance"))
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311b_endurancePaceImprovement_requiresComparableRecords() {
        val evidence = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE,
            metricSession(2, durations = listOf(50), distances = listOf(100.0)) +
                metricSession(1, durations = listOf(60), distances = listOf(100.0)),
        )
        val r = metricPolicy(evidence, WorkoutGoalProfile.ENDURANCE)
        assertEquals(MetricEvidenceDimension.PACE, evidence.evidenceDimension)
        assertEquals(MetricProgressionStatus.IMPROVED, evidence.status)
        assertEquals(MetricPolicyConfidence.HIGH, r.confidence)
        assertTrue(r.suggestion.detail.contains("pace"))
    }

    @Test fun v311b_enduranceMultiVariableAndLoadOnly_degradeSafely() {
        val incomparable = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.TIME_DISTANCE,
            metricSession(2, durations = listOf(50), distances = listOf(120.0)) +
                metricSession(1, durations = listOf(60), distances = listOf(100.0)),
        )
        val incomparablePolicy = metricPolicy(incomparable, WorkoutGoalProfile.ENDURANCE)
        assertEquals(MetricProgressionStatus.INCOMPARABLE, incomparable.status)
        assertEquals(MetricPolicyConfidence.LOW, incomparablePolicy.confidence)
        assertEquals(PlateauStrategy.NONE, incomparablePolicy.plateauStrategy)

        val loadOnly = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.LOAD, ProgressionTrackingType.DISTANCE_LOAD),
            WorkoutGoalProfile.ENDURANCE,
        )
        assertTrue(loadOnly.suggestion.detail.contains("not endurance progress"))
        assertEquals(MetricPolicyConfidence.LOW, loadOnly.confidence)
        assertNull(loadOnly.suggestion.suggestedWeight)
    }

    @Test fun v311b_generalFitnessImprovement_staysSustainable() {
        val r = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.DISTANCE),
            WorkoutGoalProfile.GENERAL_FITNESS,
            Readiness.NORMAL,
        )
        assertEquals("Sustainable progress", r.suggestion.title)
        assertEquals(MetricPolicyConfidence.MODERATE, r.confidence)
        assertTrue(r.suggestion.detail.contains("only one metric"))
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311b_generalFitnessTired_suppressesLoadAndDensityEscalation() {
        val r = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.PACE),
            WorkoutGoalProfile.GENERAL_FITNESS,
            Readiness.TIRED,
        )
        assertEquals(MetricPolicyConfidence.LOW, r.confidence)
        assertTrue(r.suggestion.detail.contains("recover"))
        assertTrue(r.suggestion.detail.contains("rather than increasing load or density"))
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311b_tiredRepGeneralFitnessAndFatLoss_holdInsteadOfIncreasingLoad() {
        val exercise = ex("Machine chest press", 3, 8, 12)
        val sets = session(1, listOf(12, 12, 12), weight = 60.0, rir = 3)
        for (goal in listOf(WorkoutGoalProfile.GENERAL_FITNESS, WorkoutGoalProfile.FAT_LOSS_SUPPORT)) {
            val r = progWc(exercise, sets, WorkoutContext(goal = goal), readiness = Readiness.TIRED)
            assertEquals(goal.toString(), Action.KEEP_WEIGHT, r.action)
            assertEquals(goal.toString(), 60.0, r.suggestedWeight!!, 0.001)
            assertEquals(goal.toString(), "Recover & repeat", r.titleOverride)
            assertTrue(goal.toString(), r.reason.contains("instead of increasing intensity"))
            if (goal == WorkoutGoalProfile.FAT_LOSS_SUPPORT) {
                assertTrue(r.reason.contains("does not measure fat loss"))
            }
        }

        val bodyweight = progWc(
            ex("Pull-up", 3, 6, 10),
            session(2, listOf(10, 10, 10), weight = 0.0, rir = 3),
            WorkoutContext(goal = WorkoutGoalProfile.GENERAL_FITNESS),
            readiness = Readiness.TIRED,
        )
        assertEquals(Action.KEEP_WEIGHT, bodyweight.action)
        assertNull(bodyweight.suggestedWeight)
        assertTrue(bodyweight.reason.contains("instead of increasing intensity"))
    }

    @Test fun v311b_fatLossSupport_neverPromisesAnOutcome() {
        val r = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.DURATION),
            WorkoutGoalProfile.FAT_LOSS_SUPPORT,
        )
        val text = r.suggestion.detail.lowercase()
        assertTrue(text.contains("does not measure fat loss"))
        assertTrue(text.contains("does not measure") && text.contains("body-composition"))
        assertTrue(!text.contains("burned fat"))
        assertTrue(!text.contains("will lose"))
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311b_skillEveryStatus_remainsFormFirstAndAdvisory() {
        for (status in MetricProgressionStatus.entries) {
            val r = metricPolicy(
                metricEvidence(status, MetricEvidenceDimension.LOAD, ProgressionTrackingType.SKILL_FORM),
                WorkoutGoalProfile.SKILL,
            )
            val text = r.suggestion.detail.lowercase()
            assertEquals("Form first", r.suggestion.title)
            assertEquals(MetricPolicyConfidence.LOW, r.confidence)
            assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
            assertTrue(text.contains("technique quality") && text.contains("not"))
            assertTrue(!text.contains("hypertrophy"))
            assertTrue(!text.contains("failure"))
            assertNull(r.suggestion.suggestedWeight)
        }
    }

    @Test fun v311b_unknownGoal_fallsBackWithoutConfidentProgression() {
        val r = metricPolicy(
            metricEvidence(MetricProgressionStatus.IMPROVED, MetricEvidenceDimension.PACE),
            WorkoutGoalProfile.UNKNOWN,
            Readiness.STRONG,
        )
        assertEquals("Hold & compare", r.suggestion.title)
        assertEquals(MetricPolicyConfidence.LOW, r.confidence)
        assertEquals(PlateauStrategy.NONE, r.plateauStrategy)
        assertTrue(r.suggestion.detail.contains("goal is unavailable"))
        assertNull(r.suggestion.suggestedWeight)
    }

    @Test fun v311b_unknownTrackingInference_isLowConfidenceAndNonPrescriptive() {
        val evidence = ProgressiveOverloadEngine.metricProgression(
            ProgressionTrackingType.UNKNOWN,
            metricSession(2, distances = listOf(60.0)) + metricSession(1, distances = listOf(50.0)),
        )
        assertTrue(evidence.trackingWasInferred)
        for (goal in WorkoutGoalProfile.entries) {
            val r = metricPolicy(evidence, goal)
            assertEquals(goal.toString(), MetricPolicyConfidence.LOW, r.confidence)
            assertEquals(goal.toString(), PlateauStrategy.NONE, r.plateauStrategy)
            assertTrue(goal.toString(), r.suggestion.detail.contains("confirm the exercise profile"))
            assertNull(goal.toString(), r.suggestion.suggestedWeight)
        }

        val integrated = ProgressiveOverloadEngine.suggestion(
            ex("Custom carry", 2, 0, 0),
            metricSession(2, distances = listOf(60.0)) + metricSession(1, distances = listOf(50.0)),
        )
        assertTrue(integrated.detail.contains("confirm the exercise profile"))
        assertNull(integrated.suggestedWeight)
    }

    @Test fun v311b_incomparableAndIncomplete_areLowConfidenceAcrossGoals() {
        val goals = WorkoutGoalProfile.entries
        for (goal in goals) {
            for (status in listOf(MetricProgressionStatus.INCOMPARABLE, MetricProgressionStatus.INCOMPLETE)) {
                val r = metricPolicy(metricEvidence(status), goal, Readiness.STRONG)
                assertEquals("$goal / $status", MetricPolicyConfidence.LOW, r.confidence)
                assertEquals("$goal / $status", PlateauStrategy.NONE, r.plateauStrategy)
                assertNull("$goal / $status", r.suggestion.suggestedWeight)
            }
        }
    }

    @Test fun v311b_hypertrophyActionAndWeight_invariantsCoverIncreaseKeepDecrease() {
        val exercise = ex("Machine chest press", 3, 10, 12)
        val fixtures = listOf(
            session(3, listOf(12, 12, 12), weight = 40.0, rir = 2),
            session(2, listOf(9, 9, 9), weight = 40.0, rir = 2),
            session(1, listOf(3, 3, 3), weight = 40.0, rir = 0),
        )
        for (sets in fixtures) {
            val legacy = prog(exercise, sets)
            val explicit = progWc(
                exercise,
                sets,
                WorkoutContext(goal = WorkoutGoalProfile.HYPERTROPHY, goalSource = GoalSource.EXPLICIT),
            )
            assertEquals(legacy.action, explicit.action)
            assertEquals(legacy.suggestedWeight, explicit.suggestedWeight)
        }
    }
}
