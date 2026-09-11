package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.Force
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.catalogSlugsByName
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * MUSCLE-4B — Home Training Readiness (runtime-only, deterministic, explainable).
 *
 * ## Conceptual split (kept strictly, unchanged from MUSCLE-4)
 *  - **Session Heat** (MUSCLE-1..HEAT-1) = how much productive stimulus ONE workout produced.
 *  - **Readiness / Recovery** (this file) = how much recovery debt REMAINS across time. It re-uses the
 *    session pipeline's quality-weighted stimulus as the *seed* and then models its decay — it never
 *    repaints session heat, and it uses its OWN color/status system.
 *
 * ## What changed vs MUSCLE-4 (the upgrade)
 * V4 decayed each session's stimulus **linearly** to zero over one size-based window — effectively a
 * recovery timer. V4B replaces that with a **bi-exponential fitness-fatigue cost** in the Banister /
 * Busso impulse-response tradition (two exponential terms with different time constants), which is the
 * peer-reviewed standard for modelling how training strain dissipates:
 *
 * ```
 * ReadinessCost(t) = A·e^(−k_fast·t)  +  B·e^(−k_slow·max(0, t − t_delay))
 * ```
 *  - **A = acute session strain** (neuromuscular / metabolic) — large but fast-clearing (`k_fast`).
 *  - **B = slower local recovery cost** (structural muscle damage) — smaller, longer-lasting
 *    (`k_slow`), and **delayed** by `t_delay` so it stays elevated through the DOMS-like window before
 *    it decays (a plateau-then-decay soreness curve rather than V4's straight line).
 *  - `k_fast`/`k_slow`/`t_delay` come from a **size-based [RecoveryProfile]** (large compounds recover
 *    slowest — RP/NSCA guidance + DOMS-timeline literature), so size-aware differences are preserved.
 *  - **Exercise recovery cost** now scales `B` (structural), derived from existing catalog taxonomy
 *    (progression type + force + equipment) — a barbell hinge (RDL) carries more structural cost than a
 *    machine isolation (leg curl), addressing the review's `exerciseDamageFactor` WITHOUT new schema or
 *    invented per-exercise numbers.
 *  - **Grind / recovery-risk** work stretches `k_slow` (structural fatigue outlasts stimulus).
 *  - **Repeated sessions stack** (costs sum), matching accumulated-fatigue models.
 *  - **Neglected-muscle** nudges are supported (planned muscles untrained for a while).
 *
 * Everything is pure Kotlin (no DAO/clock inside): callers pass sessions + `nowMillis`. Nothing is
 * persisted; no schema change. The Progressive Overload Engine and MuscleHeat scoring are untouched.
 */

/** Readiness stage per muscle (monotone with [MuscleRecoveryResult.recoveryScore]). */
enum class RecoveryStatus { READY, PRIMED, RECOVERING, SORE, OVERLOADED, UNKNOWN }

/**
 * Recovery-specific color tokens — deliberately a SEPARATE type from [MuscleHeatColorToken]:
 * in session heat red = productive stimulus; in recovery red = remaining fatigue. READY/UNKNOWN are
 * not painted on the body (neutral figure = recovered).
 */
enum class RecoveryColorToken { RECOVERY_NEUTRAL, RECOVERY_READY, RECOVERY_LIGHT, RECOVERY_MODERATE, RECOVERY_HIGH, RECOVERY_MAX }

object RecoveryPalette {
    /** null = do not paint (neutral body). */
    fun argb(token: RecoveryColorToken): Long? = when (token) {
        RecoveryColorToken.RECOVERY_NEUTRAL -> null
        RecoveryColorToken.RECOVERY_READY -> null            // body stays neutral when recovered
        RecoveryColorToken.RECOVERY_LIGHT -> 0xFFB6E24A      // primed (bright lime)
        RecoveryColorToken.RECOVERY_MODERATE -> 0xFFF2C24B   // recovering (amber)
        RecoveryColorToken.RECOVERY_HIGH -> 0xFFF0813C       // sore (orange)
        RecoveryColorToken.RECOVERY_MAX -> 0xFFE0443C        // overloaded (red)
    }

    /** Dot/label/bar color for status lines (READY gets its green here, not on the body). */
    fun statusArgb(status: RecoveryStatus): Long = when (status) {
        RecoveryStatus.READY -> 0xFF4ED07A
        RecoveryStatus.PRIMED -> 0xFFB6E24A
        RecoveryStatus.RECOVERING -> 0xFFF2C24B
        RecoveryStatus.SORE -> 0xFFF0813C
        RecoveryStatus.OVERLOADED -> 0xFFE0443C
        RecoveryStatus.UNKNOWN -> 0xFF8A8F99
    }

    fun tokenFor(status: RecoveryStatus): RecoveryColorToken = when (status) {
        RecoveryStatus.READY -> RecoveryColorToken.RECOVERY_READY
        RecoveryStatus.PRIMED -> RecoveryColorToken.RECOVERY_LIGHT
        RecoveryStatus.RECOVERING -> RecoveryColorToken.RECOVERY_MODERATE
        RecoveryStatus.SORE -> RecoveryColorToken.RECOVERY_HIGH
        RecoveryStatus.OVERLOADED -> RecoveryColorToken.RECOVERY_MAX
        RecoveryStatus.UNKNOWN -> RecoveryColorToken.RECOVERY_NEUTRAL
    }
}

/** Short lifter-facing label for a stage. */
fun RecoveryStatus.label(): String = when (this) {
    RecoveryStatus.READY -> "Ready"
    RecoveryStatus.PRIMED -> "Primed"
    RecoveryStatus.RECOVERING -> "Recovering"
    RecoveryStatus.SORE -> "Sore"
    RecoveryStatus.OVERLOADED -> "Needs rest"
    RecoveryStatus.UNKNOWN -> "—"
}

/** One session's contribution to a muscle's current debt (kept for explainability). */
data class RecoveryContributor(
    val sessionId: Long,
    val startedAtMillis: Long,
    /** Quality-weighted stimulus this session put on the muscle (the seed). */
    val stimulus: Double,
    /** What is still left after bi-exponential decay (acute + structural), same units. */
    val residualFatigue: Double,
    val recoveryRisk: Boolean,
)

data class MuscleRecoveryResult(
    val muscleGroup: MuscleGroup,
    /** 0–100: 100 = fully recovered / ready. */
    val recoveryScore: Int,
    /** Total remaining readiness cost (acute + structural), quality-weighted set-equivalents. */
    val fatigueScore: Double,
    /** Fast-clearing acute strain still present. */
    val acuteResidual: Double,
    /** Slower structural (damage) cost still present. */
    val structuralResidual: Double,
    /** True in the delayed-soreness window (structural cost now dominates the acute strain). */
    val soreWindow: Boolean,
    /** A recent hit to this muscle was ground to failure (recovery-risk). */
    val grindCarried: Boolean,
    val colorToken: RecoveryColorToken,
    val status: RecoveryStatus,
    val lastTrainedAtMillis: Long?,
    val contributors: List<RecoveryContributor>,
    val reason: String,
)

/** Pure engine input: one session's per-muscle quality-weighted stimulus + its structural cost. */
data class MuscleStimulus(
    val muscleGroup: MuscleGroup,
    val stimulus: Double,
    val recoveryRisk: Boolean,
    /** Structural recovery-cost multiplier for the exercises that hit this muscle (≈0.7–1.4). */
    val exerciseCost: Double = 1.0,
)

data class SessionMuscleStimulus(
    val sessionId: Long,
    val startedAtMillis: Long,
    val perMuscle: List<MuscleStimulus>,
)

/** A planned muscle that has not been meaningfully trained for a while. */
data class NeglectedMuscle(val muscleGroup: MuscleGroup, val daysSinceTrained: Int?)

/** Readiness roll-up for one coarse body group (Legs / Back / …) — always emitted for all groups. */
data class RecoveryGroupSummary(
    val group: RecoveryBodyGroup,
    val recoveryScore: Int,
    val status: RecoveryStatus,
    /** true if at least one member muscle was trained inside the fatigue window. */
    val trained: Boolean,
)

enum class RecoveryBodyGroup(val display: String) {
    LEGS("Legs"), BACK("Back"), CHEST("Chest"), SHOULDERS("Shoulders"), ARMS("Arms"), CORE("Core")
}

data class MuscleRecoverySnapshot(
    /** Most-fatigued first. Only muscles trained inside the window appear. */
    val results: List<MuscleRecoveryResult>,
    val hasRecentTraining: Boolean,
    /** Coarse group roll-ups (all 6 groups, untrained → Ready). */
    val groups: List<RecoveryGroupSummary>,
    /** Average readiness across the 6 coarse groups (0–100). */
    val overallReadiness: Int,
    val overallStatus: RecoveryStatus,
    /** Muscles today's selected plan hits that are still SORE / OVERLOADED. */
    val planOverlap: List<MuscleGroup>,
    /** Planned muscles left unworked for a while (neglected). */
    val neglected: List<NeglectedMuscle>,
)

/** Per-size bi-exponential recovery profile (hours). */
data class RecoveryProfile(
    val fastHalfLifeH: Double,
    val slowHalfLifeH: Double,
    val delayH: Double,
    /** Fraction of the seed routed to the slower structural term (rest is acute). */
    val structuralFraction: Double,
)

object MuscleRecoveryEngine {

    private const val HOUR_MS = 3_600_000L
    private val LN2 = ln(2.0)

    /** Grinding sessions (recovery-risk) stretch the STRUCTURAL half-life — fatigue outlasts stimulus. */
    const val GRIND_SLOW_MULTIPLIER = 1.3

    /** Cost at/above this reads as ~0% recovered (a hard multi-set compound session). */
    const val FULL_COST_REF = 6.5

    /** Below this total cost the delayed-soreness flag is suppressed (trivial residuals aren't "sore"). */
    private const val SORE_MIN_COST = 1.1

    /** A planned muscle untrained for at least this many days triggers a neglect nudge. */
    const val NEGLECT_DAYS = 6

    /** Only scan this far back for "last trained" (perf guard for the neglect check). */
    private const val NEGLECT_LOOKBACK_DAYS = 60L

    /**
     * Size class → bi-exponential profile. Large compounds recover slowest with the longest delayed
     * (DOMS-like) structural tail; small arm/calf muscles clear fastest. Exhaustive over [MuscleGroup].
     */
    fun profile(muscle: MuscleGroup): RecoveryProfile = when (muscle) {
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES,
        MuscleGroup.BACK, MuscleGroup.LATS, MuscleGroup.MID_BACK, MuscleGroup.LOWER_BACK,
        MuscleGroup.CHEST, MuscleGroup.UPPER_CHEST, MuscleGroup.FULL_BODY,
        -> RecoveryProfile(fastHalfLifeH = 12.0, slowHalfLifeH = 42.0, delayH = 11.0, structuralFraction = 0.55)

        MuscleGroup.SHOULDERS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS,
        MuscleGroup.TRAPS, MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.CORE,
        MuscleGroup.ADDUCTORS, MuscleGroup.ABDUCTORS,
        -> RecoveryProfile(fastHalfLifeH = 10.0, slowHalfLifeH = 30.0, delayH = 7.0, structuralFraction = 0.50)

        MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS, MuscleGroup.CALVES,
        -> RecoveryProfile(fastHalfLifeH = 8.0, slowHalfLifeH = 20.0, delayH = 4.0, structuralFraction = 0.45)
    }

    fun bodyGroupOf(muscle: MuscleGroup): RecoveryBodyGroup = when (muscle) {
        MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES,
        MuscleGroup.ADDUCTORS, MuscleGroup.ABDUCTORS,
        -> RecoveryBodyGroup.LEGS

        MuscleGroup.BACK, MuscleGroup.LATS, MuscleGroup.MID_BACK, MuscleGroup.LOWER_BACK, MuscleGroup.TRAPS,
        -> RecoveryBodyGroup.BACK

        MuscleGroup.CHEST, MuscleGroup.UPPER_CHEST -> RecoveryBodyGroup.CHEST

        MuscleGroup.SHOULDERS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS,
        -> RecoveryBodyGroup.SHOULDERS

        MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> RecoveryBodyGroup.ARMS

        MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.CORE, MuscleGroup.FULL_BODY -> RecoveryBodyGroup.CORE
    }

    /** Status ladder — monotone on recovery% so status, color and the % bar always agree. */
    private fun statusFor(recoveryScore: Int): RecoveryStatus = when {
        recoveryScore < 22 -> RecoveryStatus.OVERLOADED
        recoveryScore < 50 -> RecoveryStatus.SORE
        recoveryScore < 72 -> RecoveryStatus.RECOVERING
        recoveryScore < 88 -> RecoveryStatus.PRIMED
        else -> RecoveryStatus.READY
    }

    /**
     * Bi-exponential decay of each contributor, summed per muscle. Deterministic, no clock — [nowMillis]
     * comes from the caller.
     */
    fun compute(sessions: List<SessionMuscleStimulus>, nowMillis: Long): List<MuscleRecoveryResult> {
        data class Acc(
            var acute: Double = 0.0,
            var structural: Double = 0.0,
            var lastTrainedAt: Long? = null,
            var grindCarried: Boolean = false,
            val contributors: MutableList<RecoveryContributor> = mutableListOf(),
        )

        val byMuscle = LinkedHashMap<MuscleGroup, Acc>()
        sessions.forEach { session ->
            val elapsedH = (nowMillis - session.startedAtMillis).toDouble() / HOUR_MS
            if (elapsedH < 0) return@forEach // future/clock-skewed rows are ignored
            session.perMuscle.forEach { s ->
                if (s.stimulus <= 0.0) return@forEach
                val p = profile(s.muscleGroup)
                // A: acute strain (fast). B: structural damage (slow, delayed, exercise-cost-scaled).
                val a = s.stimulus * (1.0 - p.structuralFraction)
                val bBase = s.stimulus * p.structuralFraction * s.exerciseCost
                val slowHl = p.slowHalfLifeH * (if (s.recoveryRisk) GRIND_SLOW_MULTIPLIER else 1.0)
                val acuteResidual = a * exp(-LN2 * elapsedH / p.fastHalfLifeH)
                val te = (elapsedH - p.delayH).coerceAtLeast(0.0)
                val structuralResidual = bBase * exp(-LN2 * te / slowHl)
                val residual = acuteResidual + structuralResidual

                val acc = byMuscle.getOrPut(s.muscleGroup) { Acc() }
                acc.acute += acuteResidual
                acc.structural += structuralResidual
                acc.lastTrainedAt = maxOf(acc.lastTrainedAt ?: 0L, session.startedAtMillis)
                if (s.recoveryRisk && residual > 0.5) acc.grindCarried = true
                acc.contributors += RecoveryContributor(
                    sessionId = session.sessionId,
                    startedAtMillis = session.startedAtMillis,
                    stimulus = s.stimulus,
                    residualFatigue = residual,
                    recoveryRisk = s.recoveryRisk,
                )
            }
        }

        return byMuscle.map { (muscle, acc) ->
            val total = acc.acute + acc.structural
            val recoveryScore = (100.0 * (1.0 - min(1.0, total / FULL_COST_REF))).roundToInt()
            val status = statusFor(recoveryScore)
            val soreWindow = acc.structural > acc.acute && total >= SORE_MIN_COST
            MuscleRecoveryResult(
                muscleGroup = muscle,
                recoveryScore = recoveryScore,
                fatigueScore = total,
                acuteResidual = acc.acute,
                structuralResidual = acc.structural,
                soreWindow = soreWindow,
                grindCarried = acc.grindCarried,
                colorToken = RecoveryPalette.tokenFor(status),
                status = status,
                lastTrainedAtMillis = acc.lastTrainedAt,
                contributors = acc.contributors.sortedByDescending { it.startedAtMillis },
                reason = reasonFor(status, soreWindow, acc.grindCarried, acc.lastTrainedAt, nowMillis),
            )
        }.sortedByDescending { it.fatigueScore }
    }

    /** All 6 coarse groups, always present. A group's readiness = its most-fatigued trained member. */
    fun summarizeGroups(results: List<MuscleRecoveryResult>): List<RecoveryGroupSummary> {
        val byGroup = results.groupBy { bodyGroupOf(it.muscleGroup) }
        return RecoveryBodyGroup.entries.map { group ->
            val members = byGroup[group].orEmpty()
            if (members.isEmpty()) {
                RecoveryGroupSummary(group, recoveryScore = 100, status = RecoveryStatus.READY, trained = false)
            } else {
                val worst = members.minByOrNull { it.recoveryScore }!!
                RecoveryGroupSummary(group, worst.recoveryScore, worst.status, trained = true)
            }
        }
    }

    private fun reasonFor(
        status: RecoveryStatus,
        soreWindow: Boolean,
        grind: Boolean,
        lastTrainedAt: Long?,
        nowMillis: Long,
    ): String {
        val ago = lastTrainedAt?.let { elapsedText(nowMillis - it) }
        val grindTag = if (grind) " (ground to failure)" else ""
        return when (status) {
            RecoveryStatus.OVERLOADED -> "Overloaded — trained $ago$grindTag. Give it rest."
            RecoveryStatus.SORE ->
                if (soreWindow) "Delayed soreness window — trained $ago." else "Still sore — trained $ago."
            RecoveryStatus.RECOVERING -> "Recovering — trained $ago."
            RecoveryStatus.PRIMED -> "Primed — trained $ago."
            RecoveryStatus.READY -> if (ago != null) "Recovered — trained $ago." else "Recovered."
            RecoveryStatus.UNKNOWN -> "No recent training."
        }
    }

    fun elapsedText(elapsedMillis: Long): String {
        val hours = elapsedMillis / HOUR_MS
        return when {
            hours < 1 -> "under an hour ago"
            hours < 48 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    }

    fun daysSince(elapsedMillis: Long): Int = (elapsedMillis / (24 * HOUR_MS)).toInt()

    fun neglectLookbackMs(): Long = NEGLECT_LOOKBACK_DAYS * 24 * HOUR_MS

    fun displayName(muscle: MuscleGroup): String =
        muscle.name.lowercase(Locale.ENGLISH).replace('_', ' ')
            .replaceFirstChar { it.uppercase(Locale.ENGLISH) }

    /**
     * Structural recovery-cost multiplier derived from EXISTING catalog taxonomy (no schema, no invented
     * per-exercise values). Compound + hinge/squat + barbell → highest; isolation + machine/cable →
     * lowest. Custom/unknown → neutral 1.0. Clamped to [0.7, 1.4].
     */
    fun exerciseRecoveryCost(entry: CatalogExercise?): Double {
        if (entry == null) return 1.0
        var cost = when (entry.progressionType) {
            ProgressionType.COMPOUND_LOAD -> 1.15
            ProgressionType.BODYWEIGHT -> 1.0
            ProgressionType.ISOLATION_SLOW -> 0.9
            ProgressionType.SKILL_FORM -> 0.8
        }
        cost += when (entry.force) {
            Force.SQUAT, Force.HINGE -> 0.20   // heaviest eccentric / systemic demand
            Force.CARRY -> 0.10
            Force.PUSH, Force.PULL -> 0.0
            Force.ROTATION, Force.ISOMETRIC, Force.OTHER -> -0.10
        }
        cost += when (entry.equipment) {
            Equipment.BARBELL -> 0.10          // axial loading
            Equipment.MACHINE, Equipment.CABLE -> -0.10 // supported / lower stabiliser cost
            else -> 0.0
        }
        return cost.coerceIn(0.7, 1.4)
    }
}

/**
 * Pure pipeline: recent saved sessions + catalog → per-muscle readiness. Reuses the session pipeline
 * unchanged (Layer A exposure → Layer B quality-weighted heat) as the seed for each session, attaches a
 * taxonomy-derived structural cost per muscle, then hands the seeds to [MuscleRecoveryEngine].
 */
object MuscleRecoveryPipeline {

    const val WINDOW_DAYS = 7
    private const val WINDOW_MS = WINDOW_DAYS * 24L * 3_600_000L

    fun compute(
        sessions: List<SessionWithSets>,
        catalog: List<CatalogExercise>,
        selectedPlanExerciseNames: List<String>,
        allPlanExerciseNames: List<String>,
        nowMillis: Long,
    ): MuscleRecoverySnapshot {
        val bySlug = catalog.associateBy { it.slug }
        val slugsByName = catalogSlugsByName(catalog)

        fun catalogFor(key: String): CatalogExercise? = key
            .takeIf { it.startsWith(CanonicalExerciseKey.CATALOG_PREFIX) }
            ?.removePrefix(CanonicalExerciseKey.CATALOG_PREFIX)
            ?.let(bySlug::get)

        val recent = sessions.filter {
            it.session.startedAtMillis in (nowMillis - WINDOW_MS)..nowMillis
        }
        val stimuli = recent.map { sws ->
            val setsByKey = sws.sets.groupBy { set ->
                set.canonicalExerciseKey?.takeIf { key -> key.isNotBlank() }
                    ?: CanonicalExerciseKey.resolve(set.exerciseNameSnapshot, slugsByName)
            }
            val snapshots = setsByKey.map { (key, sets) ->
                val catalogEntry = catalogFor(key)
                ExerciseSessionSnapshot(
                    canonicalExerciseKey = key,
                    exerciseName = sets.first().exerciseNameSnapshot,
                    trackingType = catalogEntry?.trackingType ?: TrackingType.REPS_WEIGHT,
                    primaryMuscles = catalogEntry?.primaryMuscles.orEmpty(),
                    secondaryMuscles = catalogEntry?.secondaryMuscles.orEmpty(),
                    targetSets = 0, // logged work counts as done
                    sets = sets,
                )
            }
            // Per-muscle structural cost = set-weighted mean exercise cost across exercises hitting it.
            val costAcc = HashMap<MuscleGroup, Pair<Double, Int>>() // muscle -> (Σ cost*sets, Σ sets)
            snapshots.forEach { snap ->
                val cost = MuscleRecoveryEngine.exerciseRecoveryCost(catalogFor(snap.canonicalExerciseKey))
                val sets = snap.sets.size
                (snap.primaryMuscles + snap.secondaryMuscles).toSet().forEach { m ->
                    val (sumCost, sumSets) = costAcc[m] ?: (0.0 to 0)
                    costAcc[m] = (sumCost + cost * sets) to (sumSets + sets)
                }
            }
            fun muscleCost(m: MuscleGroup): Double =
                costAcc[m]?.let { (sumCost, sumSets) -> if (sumSets > 0) sumCost / sumSets else 1.0 } ?: 1.0

            val heat = MuscleHeatEngine.analyze(MuscleExposureCalculator.compute(snapshots))
            SessionMuscleStimulus(
                sessionId = sws.session.id,
                startedAtMillis = sws.session.startedAtMillis,
                perMuscle = heat
                    .filter { it.finalIntensityScore > 0.0 }
                    .map {
                        MuscleStimulus(
                            muscleGroup = it.muscleGroup,
                            stimulus = it.finalIntensityScore,
                            recoveryRisk = it.status == MuscleStatus.RECOVERY_RISK,
                            exerciseCost = muscleCost(it.muscleGroup),
                        )
                    },
            )
        }

        val results = MuscleRecoveryEngine.compute(stimuli, nowMillis)
        val hasRecentTraining = stimuli.any { it.perMuscle.isNotEmpty() }
        val groups = MuscleRecoveryEngine.summarizeGroups(results)
        val overallReadiness = if (groups.isEmpty()) 100
        else groups.sumOf { it.recoveryScore } / groups.size
        val overallStatus = when {
            overallReadiness >= 88 -> RecoveryStatus.READY
            overallReadiness >= 72 -> RecoveryStatus.PRIMED
            overallReadiness >= 50 -> RecoveryStatus.RECOVERING
            overallReadiness >= 22 -> RecoveryStatus.SORE
            else -> RecoveryStatus.OVERLOADED
        }

        // Today's plan overlap: selected-day planned muscles still SORE/OVERLOADED right now.
        fun musclesOf(names: List<String>): Set<MuscleGroup> = names
            .mapNotNull { name -> slugsByName[CanonicalExerciseKey.normalizeName(name)]?.let(bySlug::get) }
            .flatMap { it.primaryMuscles }
            .toSet()

        val selectedMuscles = musclesOf(selectedPlanExerciseNames)
        val overlap = results
            .filter {
                it.muscleGroup in selectedMuscles &&
                    (it.status == RecoveryStatus.SORE || it.status == RecoveryStatus.OVERLOADED)
            }
            .map { it.muscleGroup }

        // Neglect: planned muscles (any day) not trained within NEGLECT_DAYS. lastTrained scans a
        // longer lookback than the fatigue window (perf-guarded). Only meaningful once training exists.
        val neglected = if (!hasRecentTraining) emptyList() else {
            val lookbackStart = nowMillis - MuscleRecoveryEngine.neglectLookbackMs()
            val lastTrained = HashMap<MuscleGroup, Long>()
            sessions.filter { it.session.startedAtMillis in lookbackStart..nowMillis }.forEach { sws ->
                val muscles = sws.sets.mapNotNull { set ->
                    val key = set.canonicalExerciseKey?.takeIf { k -> k.isNotBlank() }
                        ?: CanonicalExerciseKey.resolve(set.exerciseNameSnapshot, slugsByName)
                    catalogFor(key)
                }.flatMap { it.primaryMuscles }.toSet()
                muscles.forEach { m ->
                    lastTrained[m] = maxOf(lastTrained[m] ?: 0L, sws.session.startedAtMillis)
                }
            }
            musclesOf(allPlanExerciseNames).mapNotNull { m ->
                val last = lastTrained[m]
                val days = last?.let { MuscleRecoveryEngine.daysSince(nowMillis - it) }
                when {
                    last == null -> NeglectedMuscle(m, null)
                    days != null && days >= MuscleRecoveryEngine.NEGLECT_DAYS -> NeglectedMuscle(m, days)
                    else -> null
                }
            }.sortedByDescending { it.daysSinceTrained ?: Int.MAX_VALUE }
        }

        return MuscleRecoverySnapshot(
            results = results,
            hasRecentTraining = hasRecentTraining,
            groups = groups,
            overallReadiness = overallReadiness,
            overallStatus = overallStatus,
            planOverlap = overlap,
            neglected = neglected,
        )
    }
}

/**
 * Region paints for the body map: recovery results → paintable regions with the max-fatigue color
 * winning shared regions (same deterministic rule the heat map uses). READY/UNKNOWN paint nothing.
 */
data class RecoveryPaint(val argb: Long, val fatigue: Double)

fun recoveryRegionPaints(results: List<MuscleRecoveryResult>): Map<BodyRegionId, RecoveryPaint> {
    val paints = mutableMapOf<BodyRegionId, RecoveryPaint>()
    results.forEach { result ->
        val argb = RecoveryPalette.argb(result.colorToken) ?: return@forEach
        MuscleBodyRegions.regionsFor(result.muscleGroup).forEach { region ->
            val existing = paints[region]
            if (existing == null || result.fatigueScore > existing.fatigue) {
                paints[region] = RecoveryPaint(argb, result.fatigueScore)
            }
        }
    }
    return paints
}

/** Thin wrapper: loads the catalog once, then delegates to the pure [MuscleRecoveryPipeline]. */
class MuscleRecoveryUseCase(private val catalogRepository: ExerciseCatalogRepository) {
    suspend fun compute(
        sessions: List<SessionWithSets>,
        selectedPlanExerciseNames: List<String>,
        allPlanExerciseNames: List<String>,
        nowMillis: Long,
    ): MuscleRecoverySnapshot = MuscleRecoveryPipeline.compute(
        sessions = sessions,
        catalog = catalogRepository.all(),
        selectedPlanExerciseNames = selectedPlanExerciseNames,
        allPlanExerciseNames = allPlanExerciseNames,
        nowMillis = nowMillis,
    )
}
