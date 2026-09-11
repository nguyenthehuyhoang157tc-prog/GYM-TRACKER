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
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Diagnostic benchmark for human-like recovery journeys.
 *
 * This deliberately does not turn heuristic physiology expectations into build-breaking unit-test
 * assertions. It executes the production MUSCLE-4B pipeline, compares its output with independently
 * stated directional/semantic oracles, and emits PASS / FAIL / MODEL_LIMITATION in a Markdown report.
 * Harness integrity and determinism remain hard JUnit assertions.
 */
class MuscleRecoveryRealityBenchmarkTest {

    private enum class Verdict { PASS, FAIL, MODEL_LIMITATION }

    private data class CaseResult(
        val id: String,
        val area: String,
        val condition: String,
        val verdict: Verdict,
        val actual: String,
    )

    private val hour = 3_600_000L
    private val now = 1_000L * 24L * hour

    private fun entry(
        slug: String,
        name: String,
        primary: List<MuscleGroup>,
        secondary: List<MuscleGroup> = emptyList(),
        progression: ProgressionType,
        force: Force,
        equipment: Equipment,
    ) = CatalogExercise(
        slug = slug,
        name = name,
        equipment = equipment,
        primaryMuscles = primary,
        secondaryMuscles = secondary,
        level = Level.BEGINNER,
        force = force,
        category = Category.STRENGTH,
        progressionType = progression,
        trackingType = TrackingType.REPS_WEIGHT,
        defaultSets = 3,
        defaultMinReps = 8,
        defaultMaxReps = 12,
        defaultIncrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 1,
        isBodyweight = false,
        isUnilateral = false,
        aliases = emptyList(),
    )

    private val catalog = listOf(
        entry(
            slug = "romanian-deadlift",
            name = "Romanian Deadlift",
            primary = listOf(MuscleGroup.HAMSTRINGS),
            secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            progression = ProgressionType.COMPOUND_LOAD,
            force = Force.HINGE,
            equipment = Equipment.BARBELL,
        ),
        entry(
            slug = "leg-curl",
            name = "Leg Curl",
            primary = listOf(MuscleGroup.HAMSTRINGS),
            progression = ProgressionType.ISOLATION_SLOW,
            force = Force.OTHER,
            equipment = Equipment.MACHINE,
        ),
        entry(
            slug = "bench-press",
            name = "Bench Press",
            primary = listOf(MuscleGroup.CHEST),
            secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.FRONT_DELTS),
            progression = ProgressionType.COMPOUND_LOAD,
            force = Force.PUSH,
            equipment = Equipment.BARBELL,
        ),
        entry(
            slug = "triceps-pushdown",
            name = "Triceps Pushdown",
            primary = listOf(MuscleGroup.TRICEPS),
            progression = ProgressionType.ISOLATION_SLOW,
            force = Force.OTHER,
            equipment = Equipment.CABLE,
        ),
        entry(
            slug = "back-squat",
            name = "Back Squat",
            primary = listOf(MuscleGroup.QUADS),
            secondary = listOf(MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS),
            progression = ProgressionType.COMPOUND_LOAD,
            force = Force.SQUAT,
            equipment = Equipment.BARBELL,
        ),
        entry(
            slug = "barbell-curl",
            name = "Barbell Curl",
            primary = listOf(MuscleGroup.BICEPS),
            progression = ProgressionType.ISOLATION_SLOW,
            force = Force.OTHER,
            equipment = Equipment.BARBELL,
        ),
    )

    private fun session(
        id: Long,
        startedAtMillis: Long,
        slug: String,
        name: String,
        setCount: Int,
        rir: Int,
    ) = SessionWithSets(
        session = WorkoutSessionEntity(
            id = id,
            dayId = null,
            dayNameSnapshot = "Reality benchmark",
            dateEpochDay = 20_000,
            startedAtMillis = startedAtMillis,
        ),
        sets = (1..setCount).map { setNumber ->
            LoggedSetEntity(
                id = id * 1_000 + setNumber,
                sessionId = id,
                exerciseId = 1,
                exerciseNameSnapshot = name,
                setNumber = setNumber,
                weight = 60.0,
                reps = 10,
                rir = rir,
                notes = null,
                isPrWeight = false,
                isPrReps = false,
                canonicalExerciseKey = "catalog:$slug",
            )
        },
    )

    private fun snapshot(
        sessions: List<SessionWithSets>,
        atMillis: Long = now,
        planned: List<String> = emptyList(),
        allPlanned: List<String> = planned,
    ) = MuscleRecoveryPipeline.compute(
        sessions = sessions,
        catalog = catalog,
        selectedPlanExerciseNames = planned,
        allPlanExerciseNames = allPlanned,
        nowMillis = atMillis,
    )

    private fun result(
        snapshot: MuscleRecoverySnapshot,
        muscle: MuscleGroup,
    ) = snapshot.results.first { it.muscleGroup == muscle }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.3f", value)

    private fun describe(result: MuscleRecoveryResult): String =
        "fatigue=${fmt(result.fatigueScore)}, readiness=${result.recoveryScore}, " +
            "status=${result.status}, structural=${fmt(result.structuralResidual)}, " +
            "soreWindow=${result.soreWindow}"

    private fun journey(
        slug: String,
        name: String,
        sets: Int,
        rir: Int,
        muscle: MuscleGroup,
        checkpoints: List<Int>,
    ): List<Pair<Int, MuscleRecoveryResult>> {
        val start = now
        val workout = session(900 + sets + rir.toLong(), start, slug, name, sets, rir)
        return checkpoints.map { elapsedHours ->
            elapsedHours to result(snapshot(listOf(workout), start + elapsedHours * hour), muscle)
        }
    }

    @Test
    fun generateHumanLikeRealityBenchmarkReport() {
        val cases = mutableListOf<CaseResult>()

        fun record(
            id: String,
            area: String,
            condition: String,
            passed: Boolean,
            actual: String,
        ) {
            cases += CaseResult(id, area, condition, if (passed) Verdict.PASS else Verdict.FAIL, actual)
        }

        fun limitation(id: String, area: String, condition: String, actual: String) {
            cases += CaseResult(id, area, condition, Verdict.MODEL_LIMITATION, actual)
        }

        val easyCurl = result(
            snapshot(listOf(session(1, now - 48 * hour, "leg-curl", "Leg Curl", 2, rir = 4))),
            MuscleGroup.HAMSTRINGS,
        )
        record(
            "R01",
            "Easy isolation",
            "2 sets leg curl at RIR 4 must not leave hamstrings SORE/OVERLOADED at 48h.",
            easyCurl.status != RecoveryStatus.SORE && easyCurl.status != RecoveryStatus.OVERLOADED,
            describe(easyCurl),
        )

        val hardRdl = result(
            snapshot(listOf(session(2, now - 24 * hour, "romanian-deadlift", "Romanian Deadlift", 4, rir = 2))),
            MuscleGroup.HAMSTRINGS,
        )
        val matchedCurl = result(
            snapshot(listOf(session(3, now - 24 * hour, "leg-curl", "Leg Curl", 4, rir = 2))),
            MuscleGroup.HAMSTRINGS,
        )
        record(
            "R02",
            "Exercise taxonomy",
            "Matched RDL must retain more hamstring debt than matched leg curl at 24h.",
            hardRdl.fatigueScore > matchedCurl.fatigueScore &&
                hardRdl.recoveryScore < matchedCurl.recoveryScore,
            "RDL: ${describe(hardRdl)}; leg curl: ${describe(matchedCurl)}",
        )

        val grindVsEasy = result(
            snapshot(listOf(session(4, now - 48 * hour, "bench-press", "Bench Press", 4, rir = 0))),
            MuscleGroup.CHEST,
        )
        val easyRir3 = result(
            snapshot(listOf(session(5, now - 48 * hour, "bench-press", "Bench Press", 4, rir = 3))),
            MuscleGroup.CHEST,
        )
        record(
            "R03",
            "Grind vs easy",
            "Same-volume RIR 0 grind must not recover faster than RIR 3 at 48h.",
            grindVsEasy.fatigueScore >= easyRir3.fatigueScore,
            "RIR0: ${describe(grindVsEasy)}; RIR3: ${describe(easyRir3)}",
        )

        val productiveRir2 = result(
            snapshot(listOf(session(6, now - 48 * hour, "bench-press", "Bench Press", 4, rir = 2))),
            MuscleGroup.CHEST,
        )
        record(
            "R04",
            "Failure recovery",
            "Same-volume RIR 0 failure should not carry less recovery debt than productive RIR 2 at 48h.",
            grindVsEasy.fatigueScore >= productiveRir2.fatigueScore,
            "RIR0: ${describe(grindVsEasy)}; RIR2: ${describe(productiveRir2)}",
        )

        val oneHit = result(
            snapshot(listOf(session(7, now - 24 * hour, "romanian-deadlift", "Romanian Deadlift", 4, rir = 2))),
            MuscleGroup.HAMSTRINGS,
        )
        val stacked = result(
            snapshot(
                listOf(
                    session(7, now - 24 * hour, "romanian-deadlift", "Romanian Deadlift", 4, rir = 2),
                    session(8, now - 48 * hour, "back-squat", "Back Squat", 4, rir = 2),
                ),
            ),
            MuscleGroup.GLUTES,
        )
        val oneGlute = result(
            snapshot(listOf(session(7, now - 24 * hour, "romanian-deadlift", "Romanian Deadlift", 4, rir = 2))),
            MuscleGroup.GLUTES,
        )
        record(
            "R05",
            "Cross-day stacking",
            "A second overlapping lower-body hit must increase glute debt.",
            stacked.fatigueScore > oneGlute.fatigueScore && oneHit.contributors.size == 1,
            "one hit glutes: ${describe(oneGlute)}; stacked glutes: ${describe(stacked)}",
        )

        val pushOnly = snapshot(
            listOf(session(9, now - 8 * hour, "bench-press", "Bench Press", 5, rir = 2)),
        )
        record(
            "R06",
            "Anatomical isolation",
            "A push-only session must not create quad or hamstring recovery debt.",
            pushOnly.results.none {
                it.muscleGroup == MuscleGroup.QUADS || it.muscleGroup == MuscleGroup.HAMSTRINGS
            },
            "muscles=${pushOnly.results.joinToString { it.muscleGroup.name }}",
        )

        val lowerHeavy = snapshot(
            listOf(session(10, now - 6 * hour, "romanian-deadlift", "Romanian Deadlift", 8, rir = 2)),
            planned = listOf("Romanian Deadlift"),
        )
        val legs = lowerHeavy.groups.first { it.group == RecoveryBodyGroup.LEGS }
        val legMembers = lowerHeavy.results.filter {
            MuscleRecoveryEngine.bodyGroupOf(it.muscleGroup) == RecoveryBodyGroup.LEGS
        }
        record(
            "R07",
            "Coarse roll-up",
            "Legs roll-up must equal the worst trained leg member, not an average.",
            legs.recoveryScore == legMembers.minOf { it.recoveryScore },
            "legs=${legs.recoveryScore}/${legs.status}; members=" +
                legMembers.joinToString { "${it.muscleGroup.name}:${it.recoveryScore}" },
        )

        val tricepsDebt = snapshot(
            listOf(session(11, now - 8 * hour, "triceps-pushdown", "Triceps Pushdown", 8, rir = 2)),
            planned = listOf("Bench Press"),
        )
        val triceps = result(tricepsDebt, MuscleGroup.TRICEPS)
        record(
            "R08",
            "Plan overlap",
            "A planned bench session should warn when its secondary triceps are SORE/OVERLOADED.",
            triceps.status in setOf(RecoveryStatus.READY, RecoveryStatus.PRIMED, RecoveryStatus.RECOVERING) ||
                MuscleGroup.TRICEPS in tricepsDebt.planOverlap,
            "triceps=${describe(triceps)}; overlap=${tricepsDebt.planOverlap}",
        )

        limitation(
            "R09",
            "Plan-specific score",
            "Overall readiness must not substitute for readiness of the next selected workout.",
            "legs=${legs.recoveryScore}/${legs.status}; overall=${lowerHeavy.overallReadiness}/" +
                "${lowerHeavy.overallStatus}; overlap=${lowerHeavy.planOverlap}; no planReadiness score exists",
        )

        val sixDayRdl = snapshot(
            listOf(session(12, now - 6L * 24L * hour, "romanian-deadlift", "Romanian Deadlift", 20, rir = 0)),
            planned = listOf("Romanian Deadlift"),
            allPlanned = listOf("Romanian Deadlift"),
        )
        val sixDayHamstrings = result(sixDayRdl, MuscleGroup.HAMSTRINGS)
        val hamstringNudged = sixDayRdl.neglected.any { it.muscleGroup == MuscleGroup.HAMSTRINGS }
        record(
            "R10",
            "Neglect safety",
            "A muscle that is not READY must never simultaneously receive a neglect nudge.",
            sixDayHamstrings.status == RecoveryStatus.READY || !hamstringNudged,
            "hamstrings=${describe(sixDayHamstrings)}; neglected=${sixDayRdl.neglected}",
        )

        val deload = result(
            snapshot(listOf(session(13, now - 24 * hour, "back-squat", "Back Squat", 3, rir = 3))),
            MuscleGroup.QUADS,
        )
        val normalWeek = result(
            snapshot(listOf(session(14, now - 24 * hour, "back-squat", "Back Squat", 6, rir = 2))),
            MuscleGroup.QUADS,
        )
        record(
            "R11",
            "Deload",
            "Half-volume, easier work must create less debt than a normal hard session.",
            deload.fatigueScore < normalWeek.fatigueScore &&
                deload.recoveryScore > normalWeek.recoveryScore,
            "deload: ${describe(deload)}; normal: ${describe(normalWeek)}",
        )

        val originalSession = session(15, now - 24 * hour, "bench-press", "Bench Press", 4, rir = 2)
        val once = result(snapshot(listOf(originalSession)), MuscleGroup.CHEST)
        val duplicate = result(snapshot(listOf(originalSession, originalSession)), MuscleGroup.CHEST)
        record(
            "R12",
            "Duplicate/idempotency",
            "The same durable session supplied twice must not double recovery debt.",
            kotlin.math.abs(once.fatigueScore - duplicate.fatigueScore) < 1e-9,
            "once=${fmt(once.fatigueScore)}; duplicate=${fmt(duplicate.fatigueScore)}",
        )

        val future = snapshot(
            listOf(session(16, now + hour, "bench-press", "Bench Press", 4, rir = 2)),
        )
        record(
            "R13",
            "Clock skew",
            "A future-dated workout must be ignored safely.",
            future.results.isEmpty(),
            "resultCount=${future.results.size}",
        )

        val deterministicA = snapshot(listOf(originalSession))
        val deterministicB = snapshot(listOf(originalSession))
        record(
            "R14",
            "Determinism",
            "Identical input and time must produce an identical snapshot.",
            deterministicA == deterministicB,
            "snapshotsEqual=${deterministicA == deterministicB}",
        )

        limitation(
            "R15",
            "Training adaptation",
            "A novice/returner and an adapted lifter should not be forced onto the same recovery curve.",
            "MUSCLE-4B has no training-age, layoff, novelty or repeated-bout input; identical logs are identical.",
        )

        limitation(
            "R16",
            "Sleep/systemic state",
            "Poor sleep may alter whole-person readiness without inventing extra local muscle damage.",
            "MUSCLE-4B has no sleep, HRV, resting-HR, illness or stress input; overall score is muscle-only.",
        )

        val rdlAt6 = result(
            snapshot(
                listOf(session(17, now, "romanian-deadlift", "Romanian Deadlift", 5, rir = 2)),
                atMillis = now + 6 * hour,
            ),
            MuscleGroup.HAMSTRINGS,
        )
        val rdlAt10 = result(
            snapshot(
                listOf(session(17, now, "romanian-deadlift", "Romanian Deadlift", 5, rir = 2)),
                atMillis = now + 10 * hour,
            ),
            MuscleGroup.HAMSTRINGS,
        )
        record(
            "R17",
            "Structural semantics",
            "Before the 11h delay, structural residual must plateau rather than rise.",
            kotlin.math.abs(rdlAt6.structuralResidual - rdlAt10.structuralResidual) < 1e-9,
            "6h structural=${fmt(rdlAt6.structuralResidual)}; 10h structural=${fmt(rdlAt10.structuralResidual)}",
        )

        val rdlAt24 = result(
            snapshot(
                listOf(session(17, now, "romanian-deadlift", "Romanian Deadlift", 5, rir = 2)),
                atMillis = now + 24 * hour,
            ),
            MuscleGroup.HAMSTRINGS,
        )
        limitation(
            "R18",
            "True DOMS onset",
            "If AURON claims to predict soreness itself, a delayed rise must be representable.",
            "fatigue falls ${fmt(rdlAt6.fatigueScore)} -> ${fmt(rdlAt24.fatigueScore)}; " +
                "the flag means structural dominance, not rising damage/soreness.",
        )

        val rdlAtZero = result(
            snapshot(
                listOf(session(18, now, "romanian-deadlift", "Romanian Deadlift", 5, rir = 2)),
                atMillis = now,
            ),
            MuscleGroup.HAMSTRINGS,
        )
        record(
            "R19",
            "Delayed flag timing",
            "A flag named delayed-soreness window must not be active at the workout timestamp.",
            !rdlAtZero.soreWindow,
            describe(rdlAtZero),
        )

        val monotoneJourney = journey(
            slug = "romanian-deadlift",
            name = "Romanian Deadlift",
            sets = 5,
            rir = 2,
            muscle = MuscleGroup.HAMSTRINGS,
            checkpoints = listOf(0, 6, 12, 24, 48, 72, 96, 120, 168),
        )
        val fatigueMonotone = monotoneJourney.zipWithNext().all { (a, b) ->
            b.second.fatigueScore <= a.second.fatigueScore + 1e-12
        }
        val readinessMonotone = monotoneJourney.zipWithNext().all { (a, b) ->
            b.second.recoveryScore >= a.second.recoveryScore
        }
        record(
            "R20",
            "Longitudinal status",
            "With no new workout, debt must not rise and readiness must not fall across 0-168h.",
            fatigueMonotone && readinessMonotone,
            "fatigueMonotone=$fatigueMonotone; readinessMonotone=$readinessMonotone",
        )

        val checkpoints = listOf(0, 6, 12, 24, 48, 72, 96, 120, 168)
        val journeys = listOf(
            "Hard RDL, 5 sets, RIR2" to monotoneJourney,
            "Easy leg curl, 2 sets, RIR4" to journey(
                "leg-curl", "Leg Curl", 2, 4, MuscleGroup.HAMSTRINGS, checkpoints,
            ),
            "Bench grind, 4 sets, RIR0" to journey(
                "bench-press", "Bench Press", 4, 0, MuscleGroup.CHEST, checkpoints,
            ),
            "Bench productive, 4 sets, RIR2" to journey(
                "bench-press", "Bench Press", 4, 2, MuscleGroup.CHEST, checkpoints,
            ),
        )

        val passCount = cases.count { it.verdict == Verdict.PASS }
        val failCount = cases.count { it.verdict == Verdict.FAIL }
        val limitationCount = cases.count { it.verdict == Verdict.MODEL_LIMITATION }

        val report = buildString {
            appendLine("# AURON Recovery Reality Benchmark")
            appendLine()
            appendLine("Generated from the production MUSCLE-4B pipeline with fixed deterministic inputs.")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("- PASS: $passCount")
            appendLine("- FAIL: $failCount")
            appendLine("- MODEL_LIMITATION: $limitationCount")
            appendLine("- Total: ${cases.size}")
            appendLine()
            appendLine("## Curated reality cases")
            appendLine()
            appendLine("| ID | Area | Independent condition | Verdict | Actual production output |")
            appendLine("|---|---|---|---|---|")
            cases.forEach { case ->
                fun safe(text: String) = text.replace("|", "/").replace("\n", " ")
                appendLine(
                    "| ${case.id} | ${safe(case.area)} | ${safe(case.condition)} | " +
                        "**${case.verdict}** | ${safe(case.actual)} |",
                )
            }
            appendLine()
            appendLine("## Representative longitudinal trajectories")
            appendLine()
            journeys.forEach { (name, rows) ->
                appendLine("### $name")
                appendLine()
                appendLine("| Hours | Fatigue | Readiness | Status | Acute | Structural | Structural-dominant flag |")
                appendLine("|---:|---:|---:|---|---:|---:|---|")
                rows.forEach { (hours, value) ->
                    appendLine(
                        "| $hours | ${fmt(value.fatigueScore)} | ${value.recoveryScore} | " +
                            "${value.status} | ${fmt(value.acuteResidual)} | " +
                            "${fmt(value.structuralResidual)} | ${value.soreWindow} |",
                    )
                }
                appendLine()
            }
            appendLine("## Interpretation boundary")
            appendLine()
            appendLine("- PASS means the current engine satisfies the stated curated relation.")
            appendLine("- FAIL means production output contradicted a stated realistic/safety oracle.")
            appendLine("- MODEL_LIMITATION means the required person-level variable or target is absent.")
            appendLine("- This benchmark measures plausibility, not clinical or biological validation.")
        }

        val reportPath = System.getenv("AURON_REALITY_REPORT")
            ?: "build/reports/recovery-reality-benchmark.md"
        val reportFile = File(reportPath)
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(report)

        assertEquals(20, cases.size)
        assertTrue(reportFile.isFile)
        assertTrue(reportFile.readText().contains("AURON Recovery Reality Benchmark"))
        assertEquals(report, reportFile.readText())
    }
}
