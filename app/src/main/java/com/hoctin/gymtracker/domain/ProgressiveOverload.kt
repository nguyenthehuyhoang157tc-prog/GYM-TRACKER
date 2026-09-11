package com.hoctin.gymtracker.domain

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

data class OverloadSuggestion(
    val title: String,
    val detail: String,
    val suggestedWeight: Double? = null,
)

data class SessionTrend(
    val currentVolume: Double,
    val previousVolume: Double,
    val volumeDelta: Double,
    val label: String,
)

/**
 * How an exercise should progress. Inferred from the exercise name for now — this is a temporary
 * heuristic until a future exercise database provides real metadata. No Room field is added in this
 * task ([ExerciseEntity] is unchanged).
 */
enum class ProgressionType {
    COMPOUND_LOAD,
    ISOLATION_SLOW,
    BODYWEIGHT,

    /**
     * V3.5: control/skill or anti-movement work (e.g. Pallof press, planks, L-sit). The core identifies
     * possible reps/variation progress, but V3.11B keeps the final result advisory because technique
     * quality is not logged. Only reachable via catalog metadata, so name inference never produces it.
     */
    SKILL_FORM,
}

/**
 * Runtime-only interpretation of the catalog tracking contract. Kept in the domain package so the
 * progression engine stays pure and does not depend on Room or catalog classes. DISTANCE_ONLY is a
 * safe future contract; the current catalog does not expose it yet. UNKNOWN never enables a load
 * recommendation and may only be inferred from a consistent set of logged metrics.
 */
enum class ProgressionTrackingType {
    REPS_WEIGHT,
    BODYWEIGHT_REPS,
    TIME_ONLY,
    TIME_LOAD,
    DISTANCE_ONLY,
    DISTANCE_LOAD,
    TIME_DISTANCE,
    TIME_DISTANCE_LOAD,
    SKILL_FORM,
    UNKNOWN;

    val isRepBased: Boolean
        get() = this == REPS_WEIGHT || this == BODYWEIGHT_REPS || this == SKILL_FORM

    companion object {
        fun fromName(value: String?): ProgressionTrackingType =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class MetricProgressionStatus {
    BASELINE,
    IMPROVED,
    PLATEAU,
    REGRESSED,
    INCOMPLETE,
    INCOMPARABLE,
    ADVISORY_ONLY,
}

/** Which single dimension produced the neutral comparison result. */
enum class MetricEvidenceDimension { NONE, DURATION, DISTANCE, PACE, LOAD }

/** Structured non-rep result for tests and future goal branches; no derived score is persisted. */
data class MetricProgressionResult(
    val trackingType: ProgressionTrackingType,
    val status: MetricProgressionStatus,
    val suggestion: OverloadSuggestion,
    val evidenceDimension: MetricEvidenceDimension = MetricEvidenceDimension.NONE,
    val trackingWasInferred: Boolean = false,
)

/** Bounded policy confidence; deliberately categorical so no raw score or fake precision is exposed. */
enum class MetricPolicyConfidence { LOW, MODERATE, HIGH }

/** Runtime-only goal interpretation of a neutral [MetricProgressionResult]. */
data class MetricGoalPolicyResult(
    val goal: WorkoutGoalProfile,
    val evidence: MetricProgressionResult,
    val suggestion: OverloadSuggestion,
    val insight: CoachInsight = CoachInsight.NONE,
    val plateauStrategy: PlateauStrategy = PlateauStrategy.NONE,
    val confidence: MetricPolicyConfidence = MetricPolicyConfidence.LOW,
)

/** The concrete, deterministic coaching action the engine recommends. */
enum class Action {
    INCREASE_WEIGHT,
    KEEP_WEIGHT,
    DECREASE_WEIGHT,
    INCREASE_REPS_OR_DIFFICULTY,
}

/**
 * V3.6B objective quality of the latest logged session, derived purely from existing per-session
 * metrics (rep completion, RIR, working-weight stability, collapse). **Runtime-only — never stored**
 * (it is recomputable from the sets). It nudges [ProgressionSuggestion.confidence]; it never changes
 * the action or suggested weight.
 */
enum class SessionQuality { POOR, MIXED, SOLID, EXCELLENT }

/**
 * V3.7A long-term, per-exercise trend classification, derived at runtime from the existing logged
 * history (**never stored**). It is an internal label that refines wording/title only — it never
 * changes the [Action] or the suggested weight, and the safety gates are decided entirely upstream:
 *  - [BAD_DAY_VERIFY]: one session dipped while the prior level was stable (the existing "Hold & verify").
 *  - [NEW_BASELINE]: recent sessions have settled below an older peak and the lower load is handled —
 *    accept the lower level and rebuild here instead of forever chasing the old peak.
 *  - [PLATEAU]: same weight and reps across several sessions with no sharp drop (RIR trend disambiguates).
 *
 * V3.7C-H adds whole-workout / hypertrophy-quality context labels (runtime-only, wording/confidence only):
 *  - [SYSTEMIC_FATIGUE]: this lift is down AND several of the day's other lifts are down too — a
 *    whole-session fatigue signal, not an exercise-specific problem. Never forces a decrease.
 *  - [LOCAL_DROP]: this lift is down while the day's other lifts held up — likely a machine/form/setup
 *    issue local to this exercise rather than systemic fatigue.
 *  - [HYPERTROPHY_QUALITY]: a hypertrophy stimulus-quality note on the latest session (productive vs
 *    submaximal effective-reps), used only when no stronger trend/fatigue label applies.
 *
 * V3.9-G adds one goal-quality label for the non-hypertrophy goals:
 *  - [GOAL_QUALITY]: a goal-specific session-quality note (currently only the POWER "keep reps crisp,
 *    don't grind" caution). Wording/confidence only, like every other insight.
 */
enum class CoachInsight {
    NONE,
    BAD_DAY_VERIFY,
    NEW_BASELINE,
    PLATEAU,
    SYSTEMIC_FATIGUE,
    LOCAL_DROP,
    HYPERTROPHY_QUALITY,
    GOAL_QUALITY,
}

/**
 * Training-goal profile for the whole-workout quality context (V3.7C), goal-aware since V3.9-G.
 * V3.11C.1 combines this program intent with the exercise's declared modality before selecting a policy:
 * skill/form always stays form-first, AUTO keeps the existing rep-range inference, and an explicit goal
 * applies wherever that modality is compatible. FAT_LOSS_SUPPORT shares the sustainable
 * GENERAL_FITNESS policy but uses wording that avoids claiming measured fat loss or body-composition change.
 * Goal policy is runtime-only. The existing hypertrophy action/weight rules stay intact; a final safety
 * stage may only suppress unsupported non-H progression and never suppress a safety decrease.
 */
enum class WorkoutGoalProfile {
    HYPERTROPHY,
    STRENGTH,
    POWER,
    ENDURANCE,
    SKILL,
    GENERAL_FITNESS,
    FAT_LOSS_SUPPORT,
    UNKNOWN,
}

/** Distinguishes AUTO inference from the user's persisted explicit program intent. */
enum class GoalSource { AUTO, EXPLICIT }

/**
 * V3.7C-H runtime-only whole-workout context for the latest session, computed by the ViewModel from data
 * it already holds (no Room schema change, no new DAO query, no save/UI change) and passed to the engine
 * as optional data — the engine stays pure. It carries only aggregate, same-day sibling signals:
 *  - [siblingsEvaluated]: how many of the day's exercises had enough history to judge this session.
 *  - [siblingsDown]: of those, how many were down vs their own recent baseline.
 * It is used to distinguish systemic fatigue from a local drop and to add hypertrophy-quality wording.
 * It NEVER changes the action or the suggested weight; it only refines confidence, wording, and insight.
 */
data class WorkoutContext(
    val goal: WorkoutGoalProfile = WorkoutGoalProfile.HYPERTROPHY,
    val siblingsEvaluated: Int = 0,
    val siblingsDown: Int = 0,
    val goalSource: GoalSource = GoalSource.AUTO,
)

/**
 * Rich, typed result. The existing UI only needs [OverloadSuggestion] (title/detail/weight), so
 * [ProgressiveOverloadEngine.suggestion] maps this down — but [ProgressionSuggestion] is exposed for
 * tests and future callers that want the structured decision.
 *
 * [titleOverride] lets a rule refine the generic action title (e.g. "Hold & verify" for a suspected
 * outlier session) without any new Action or schema concept — wording only.
 */
data class ProgressionSuggestion(
    val action: Action,
    val suggestedWeight: Double?,
    val targetText: String,
    val reason: String,
    val progressionType: ProgressionType,
    val titleOverride: String? = null,
    /**
     * V3.5/V3.6B deterministic confidence (0–100): higher = more comparable history, a consistent
     * working weight, RIR data, catalog metadata, objective session quality, and (small) readiness
     * agreement behind the call. Engine-only for now (no UI change).
     */
    val confidence: Int = 50,
    /** V3.6B objective quality of the latest session (runtime-only; never stored). */
    val sessionQuality: SessionQuality = SessionQuality.MIXED,
    /** V3.7A long-term trend classification (runtime-only; never stored; wording/title only). */
    val insight: CoachInsight = CoachInsight.NONE,
    /** V3.8-H plateau-exit strategy (runtime-only; never stored; wording/confidence only). */
    val plateauStrategy: PlateauStrategy = PlateauStrategy.NONE,
)

/**
 * V3.8-H hypertrophy plateau-exit strategy — the actionable "how to get unstuck" recommendation, derived
 * at runtime (**never stored**, no raw score shown). It is chosen from existing signals (the V3.7A
 * [CoachInsight.PLATEAU] trigger + RIR trend, V3.7B readiness trend, V3.7C-H [WorkoutContext] scope +
 * hypertrophy stimulus). Like every coach signal it only refines confidence/wording/the tag — it NEVER
 * changes the [Action] or the suggested weight, and safety gates always win.
 *  - [INCREASE_EFFORT]: a hypertrophy stall while leaving a lot in reserve (high RIR) — take sets
 *    closer to failure. Strength uses the same tag for controlled, repeatable load work, never failure.
 *  - [ADD_REP]: stalled but effort is improving — double progression (chase one more rep, then load).
 *  - [ADD_SET]: stalled with adequate effort and no systemic fatigue — one more working set (guarded).
 *  - [DELOAD]: stalled with accumulated fatigue (repeated tired / systemic) — a lighter week, then retest.
 *  - [CHANGE_REP_RANGE]: a deep, persistent local stall — rotate the rep range (keep the exercise).
 *  - [STABILIZE_BASELINE]: a one-off dip / a forming new baseline — hold and gather data, don't intervene.
 */
enum class PlateauStrategy {
    NONE,
    INCREASE_EFFORT,
    ADD_REP,
    ADD_SET,
    DELOAD,
    CHANGE_REP_RANGE,
    STABILIZE_BASELINE,
    /** Metric policy only: advance duration, distance, or pace while every other dimension stays fixed. */
    PROGRESS_ONE_METRIC,
}

/**
 * V3.5 catalog-derived progression metadata. The UI layer builds it from a matched catalog exercise
 * via [ProgressiveOverloadEngine.profileFromCatalog]; it is null for custom / unmatched exercises, in
 * which case the engine falls back to name inference exactly as V3 did. Keeping this a plain domain
 * type (built from primitives) means the engine never depends on the catalog data package.
 */
data class ProgressionProfile(
    val progressionType: ProgressionType,
    val incrementKg: Double,
    val sessionsRequiredBeforeIncrease: Int,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    val trackingType: ProgressionTrackingType = ProgressionTrackingType.REPS_WEIGHT,
)

/**
 * Rule-Based Progressive Overload Engine V3 (trend-aware + outlier-aware).
 *
 * Deterministic, explainable coaching — no AI/LLM, no backend, no Room schema change. V3 builds on
 * the V2 type rules (COMPOUND_LOAD / ISOLATION_SLOW / BODYWEIGHT, name-inferred) and adds:
 *
 *  1. **Main-working-weight selection.** Within a session the working weight is the most frequent
 *     load (ties → heaviest). Lighter sets before it are ramp-up sets, lighter sets after it are
 *     back-off/drop sets; neither drives the decision, so a final 15kg pump set can't drag a 20kg
 *     suggestion down.
 *  2. **Recent-session trend + outlier detection.** The latest session is compared against the
 *     strongest of the previous two comparable sessions (score = working weight × average reps, or
 *     average reps for bodyweight). A ≥30% drop is treated as a possible bad day / unfamiliar
 *     machine / logging slip → "Hold & verify" instead of a knee-jerk decision. The engine only
 *     reduces confidently when the collapse is extreme (≥50%) at RIR 0, or when it repeats across
 *     two sessions. The latest session still carries the most weight: a normal latest session is
 *     never overridden by older history.
 *  3. **RIR-gated increases.** Hitting the rep target is not enough: increases need average RIR
 *     ≥ 1.5 across working sets. Strong overshoot (every set ≥ top of range, average a full rep
 *     past it, RIR ≥ 2) earns a doubled — still conservative — increment on compounds.
 *  4. **Real lower steps on decreases.** "Drop ~10%" now floors to the next lower 2.5kg step that
 *     is strictly below the current load (7.5kg → 5kg, never back to 7.5kg).
 *
 * RIR is a *modifier* (confidence / failure check), never the sole decision, because users estimate
 * it poorly. Suggestions stay conservative: when there is not enough data, KEEP weight.
 */
object ProgressiveOverloadEngine {

    // ---- Tunable deterministic thresholds ----------------------------------------------

    /** Latest-vs-baseline score drop that marks a possible outlier session. */
    private const val OUTLIER_DROP = 0.30

    /** Drop beyond which a collapse at RIR 0 justifies an immediate, confident reduction. */
    private const val EXTREME_DROP = 0.50

    /** A session at or below this fraction of the older baseline counts as a repeated collapse. */
    private const val REPEAT_COLLAPSE_FACTOR = 0.70

    /** Minimum average RIR across working sets before any load increase is suggested. */
    private const val RIR_INCREASE_GATE = 1.5

    /** Average RIR at/below this means the session was essentially at failure. */
    private const val RIR_AT_FAILURE = 0.5

    private const val DEFAULT_INCREMENT = 2.5
    private const val WEIGHT_EPSILON = 0.01
    private const val DISTANCE_EPSILON = 0.01

    /** V3.5: fractional working-weight jump vs the last comparable session that counts as a big jump. */
    private const val LOAD_JUMP_FRACTION = 0.10

    // V3.7A long-term trend thresholds (per-exercise, runtime-only).
    /** How many recent comparable sessions the trend overlay looks back over. */
    private const val TREND_WINDOW = 6

    /** Below this many comparable sessions the overlay is inert — exact V3.6C behavior. */
    private const val MIN_TREND_SESSIONS = 3

    /** Recent level must sit at least this fraction below an older peak to be a regression/new baseline. */
    private const val REGRESSION_DROP = 0.15

    /** The two most recent sessions are "settled" when their scores are within this fraction of each other. */
    private const val STABLE_BAND = 0.15

    /** Average-rep spread across the plateau window that still counts as "no rep progress". */
    private const val PLATEAU_REP_BAND = 1.0

    /** Latest avg RIR must beat the older window average by this much to read as "getting easier". */
    private const val RIR_TREND_DELTA = 0.5

    // V3.7C-H whole-workout / hypertrophy-quality thresholds (runtime-only, wording/confidence only).
    /** Minimum day-exercises judged before a systemic-vs-local call is meaningful. */
    private const val WORKOUT_MIN_SIBLINGS = 2
    /** This many of the day's lifts down (incl. this one) reads as systemic fatigue rather than local. */
    private const val SYSTEMIC_MIN_DOWN = 2
    /** Minimum working sets before a hypertrophy stimulus-quality read is reliable (avoids 1-2 set noise). */
    private const val QUALITY_MIN_SETS = 3
    /** Avg effective reps (max(0,5-RIR)) below this = submaximal/junk-ish stimulus (lots left in reserve). */
    private const val STIMULUS_SUBMAXIMAL_EFF = 1.5
    /** Avg effective reps at/above this = a clean, productive hypertrophy stimulus. */
    private const val STIMULUS_PRODUCTIVE_EFF = 2.5

    // V3.8-H plateau-strategy thresholds (runtime-only, wording/confidence only).
    /** Latest avg RIR at/above this on a plateau = effort deficit (leaving too much in reserve). */
    private const val PLATEAU_EFFORT_DEFICIT_RIR = 3.0
    /** A plateau persisting this many comparable sessions is "deep" → rotate the rep range. */
    private const val PLATEAU_DEEP_SESSIONS = 4
    /** Do not suggest ADD_SET once the latest session already has this many working sets (junk-volume guard). */
    private const val PLATEAU_MAX_SETS_FOR_ADD = 5

    // V3.9-G goal-aware thresholds (runtime-only). Grounded in current consensus: proximity to failure
    // meaningfully drives hypertrophy but has a negligible relationship with strength gain (Robinson et
    // al. 2024 meta-regression), and power work degrades with grinding — non-failure, low-velocity-loss
    // sets are preferred for power (velocity-based-training literature).
    // AUTO rep-range thresholds now live in ExerciseGoalCompatibilityResolver so metric and rep-based
    // policy paths share one precedence contract instead of maintaining separate goal resolvers.
    /** POWER: average working-set RIR below this means reserve is too low for conservative speed work. */
    private const val POWER_MIN_RIR = 2.0

    // ---- Public API (unchanged shape for existing callers) ----------------------------

    /**
     * V3 suggestion mapped to the UI-facing [OverloadSuggestion].
     *
     * [recentSets] is this exercise's recent logged sets, **newest session first** (exactly as
     * returned by `recentSetsForExercise` / `observeExerciseHistory`). The engine groups them by
     * session internally; passing the full history enables the trend/outlier rules and the
     * isolation "two strong sessions" rule. A single session degrades gracefully to the
     * conservative single-session rules.
     */
    fun suggestion(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        profile: ProgressionProfile? = null,
        readiness: Readiness? = null,
        readinessBySession: Map<Long, Readiness?>? = null,
        workoutContext: WorkoutContext? = null,
    ): OverloadSuggestion {
        val explicitTracking = profile?.trackingType
        if (recentSets.none { it.reps > 0 }) {
            // V3.9-G: time/distance work (plank holds, sleds, carries) logs reps = 0 sentinels but real
            // durationSeconds/distanceMeters. Those sets are NOT "no history" — degrade to a safe,
            // load-free time/distance read instead of the misleading "Log first session … 0-0 reps".
            val nonRep = recentSets.filter { it.durationSeconds != null || it.distanceMeters != null }
            if (nonRep.isNotEmpty()) {
                // Keep UNKNOWN intact until metricProgression resolves it so policy can retain the
                // inferred-tracking provenance and lower confidence in the integrated UI path.
                val evidence = metricProgression(explicitTracking ?: ProgressionTrackingType.UNKNOWN, nonRep)
                val goal = effectiveGoal(
                    exercise = exercise,
                    ctx = workoutContext ?: WorkoutContext(),
                    progressionType = profile?.progressionType ?: inferTypeFromName(exercise.name),
                    trackingType = evidence.trackingType,
                )
                return metricGoalPolicy(evidence, goal, readiness).suggestion
            }
            return OverloadSuggestion(
                title = "Log first session",
                detail = firstSessionDetail(exercise),
            )
        }
        if (explicitTracking != null && !explicitTracking.isRepBased &&
            explicitTracking != ProgressionTrackingType.UNKNOWN
        ) {
            val evidence = metricResult(
                explicitTracking,
                MetricProgressionStatus.INCOMPARABLE,
                "Hold & compare",
                "This history mixes rep-based and metric-based sets. Log one consistent tracking format before progressing it.",
            )
            val goal = effectiveGoal(
                exercise = exercise,
                ctx = workoutContext ?: WorkoutContext(),
                progressionType = profile.progressionType,
                trackingType = explicitTracking,
            )
            return metricGoalPolicy(evidence, goal, readiness).suggestion
        }
        return progression(exercise, recentSets, profile, readiness, readinessBySession, workoutContext)
            .toOverloadSuggestion()
    }

    /**
     * V3.11A metric-aware progression contract. Only the latest two sessions are compared, required
     * fields must be complete, and set count must match. One dimension changes at a time; otherwise
     * the result is INCOMPARABLE. No metric type auto-prescribes load.
     */
    fun metricProgression(
        trackingType: ProgressionTrackingType,
        nonRepSets: List<LoggedSetEntity>,
    ): MetricProgressionResult {
        val wasInferred = trackingType == ProgressionTrackingType.UNKNOWN
        fun finalize(result: MetricProgressionResult): MetricProgressionResult =
            if (wasInferred) result.copy(trackingWasInferred = true) else result
        val resolvedType = if (wasInferred) {
            inferMetricTrackingType(nonRepSets)
        } else {
            trackingType
        }
        if (resolvedType.isRepBased) {
            val skill = resolvedType == ProgressionTrackingType.SKILL_FORM
            return finalize(metricResult(
                resolvedType,
                MetricProgressionStatus.ADVISORY_ONLY,
                if (skill) "Form first" else "Use reps progression",
                if (skill) {
                    "Keep technique and control as the progression target. Load, reps, or RIR alone cannot prove better skill."
                } else {
                    "This tracking type belongs to the reps/load engine, not the metric progression contract."
                },
            ))
        }
        if (resolvedType == ProgressionTrackingType.UNKNOWN) {
            return finalize(metricResult(
                resolvedType,
                MetricProgressionStatus.INCOMPARABLE,
                "Hold & compare",
                "The logged metrics do not map to one supported tracking contract. Keep the next entry consistent before comparing sessions.",
            ))
        }

        val sessions = groupSessionsNewestFirst(nonRepSets)
        val latest = sessions.firstOrNull()?.let(::metricSnapshot)
            ?: return finalize(incompleteMetric(resolvedType))
        if (!latest.isCompleteFor(resolvedType)) return finalize(incompleteMetric(resolvedType))
        val previous = sessions.getOrNull(1)?.let(::metricSnapshot)
            ?: return finalize(baselineMetric(resolvedType, latest))
        if (!previous.isCompleteFor(resolvedType)) return finalize(incompleteMetric(resolvedType))
        if (latest.setCount != previous.setCount) {
            return finalize(incomparableMetric(
                resolvedType,
                "Set count changed (${previous.setCount} to ${latest.setCount}), so session totals are not a clean comparison.",
            ))
        }

        return finalize(when (resolvedType) {
            ProgressionTrackingType.TIME_ONLY -> compareTimeOnly(resolvedType, latest, previous)
            ProgressionTrackingType.DISTANCE_ONLY -> compareDistanceOnly(resolvedType, latest, previous)
            ProgressionTrackingType.TIME_LOAD -> compareTimeLoad(resolvedType, latest, previous)
            ProgressionTrackingType.DISTANCE_LOAD -> compareDistanceLoad(resolvedType, latest, previous)
            ProgressionTrackingType.TIME_DISTANCE ->
                compareTimeDistance(resolvedType, latest, previous, requiresSameLoad = false)
            ProgressionTrackingType.TIME_DISTANCE_LOAD ->
                compareTimeDistance(resolvedType, latest, previous, requiresSameLoad = true)
            else -> incomparableMetric(resolvedType, "This tracking contract is not supported by metric progression.")
        })
    }

    /**
     * V3.11B-MG goal policy. Consumes only neutral metric evidence; it never re-reads sets or runs a
     * second detector. The policy changes wording/metadata only and never emits a suggested weight.
     */
    fun metricGoalPolicy(
        evidence: MetricProgressionResult,
        goal: WorkoutGoalProfile,
        readiness: Readiness? = null,
    ): MetricGoalPolicyResult {
        val base = when (goal) {
            WorkoutGoalProfile.HYPERTROPHY -> MetricGoalPolicyResult(
                goal = goal,
                evidence = evidence,
                suggestion = evidence.suggestion,
                confidence = neutralMetricConfidence(evidence.status),
            )
            WorkoutGoalProfile.STRENGTH -> strengthMetricPolicy(evidence)
            WorkoutGoalProfile.POWER -> powerMetricPolicy(evidence)
            WorkoutGoalProfile.ENDURANCE -> enduranceMetricPolicy(evidence, readiness)
            WorkoutGoalProfile.GENERAL_FITNESS -> generalFitnessMetricPolicy(evidence, readiness, fatLoss = false)
            WorkoutGoalProfile.FAT_LOSS_SUPPORT -> generalFitnessMetricPolicy(evidence, readiness, fatLoss = true)
            WorkoutGoalProfile.SKILL -> skillMetricPolicy(evidence)
            WorkoutGoalProfile.UNKNOWN -> unknownMetricPolicy(evidence)
        }
        return if (evidence.trackingWasInferred) {
            base.copy(
                suggestion = base.suggestion.copy(
                    detail = base.suggestion.detail +
                        " The tracking type was inferred, so confirm the exercise profile before progressing.",
                ),
                plateauStrategy = PlateauStrategy.NONE,
                confidence = MetricPolicyConfidence.LOW,
            )
        } else {
            base
        }
    }

    private fun strengthMetricPolicy(evidence: MetricProgressionResult): MetricGoalPolicyResult =
        when (evidence.status) {
            MetricProgressionStatus.IMPROVED -> if (evidence.evidenceDimension == MetricEvidenceDimension.LOAD) {
                policyResult(
                    evidence, WorkoutGoalProfile.STRENGTH, "Repeat strength work",
                    "The matched task handled more load. That supports a loaded-task progression signal, but technique was not measured; repeat it cleanly before changing load again.",
                    confidence = MetricPolicyConfidence.MODERATE,
                )
            } else {
                policyResult(
                    evidence, WorkoutGoalProfile.STRENGTH, "Work capacity improved",
                    "A comparable task metric improved, but ${metricDimensionName(evidence.evidenceDimension)} alone does not prove a strength gain. Keep the load stable and repeat with crisp, controlled reps.",
                    confidence = MetricPolicyConfidence.LOW,
                )
            }
            MetricProgressionStatus.PLATEAU -> policyResult(
                evidence, WorkoutGoalProfile.STRENGTH, "Stabilize strength work",
                "The comparable metric repeated. Hold the setup and load, keep every rep controlled, and do not chase failure to force progress.",
                PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.MODERATE,
            )
            MetricProgressionStatus.REGRESSED -> policyResult(
                evidence, WorkoutGoalProfile.STRENGTH, "Rebuild strength work",
                "The recorded metric was lower. Check setup and recovery, then repeat the same task cleanly before changing the prescription.",
                PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.LOW,
            )
            else -> insufficientMetricPolicy(evidence, WorkoutGoalProfile.STRENGTH)
        }

    private fun powerMetricPolicy(evidence: MetricProgressionResult): MetricGoalPolicyResult {
        val statusText = when (evidence.status) {
            MetricProgressionStatus.IMPROVED -> "A comparable task metric improved"
            MetricProgressionStatus.PLATEAU -> "The logged task metric repeated"
            MetricProgressionStatus.REGRESSED -> "The logged task metric was lower"
            MetricProgressionStatus.INCOMPARABLE -> "The sessions are not comparable"
            else -> "There is not enough comparable output data"
        }
        val strategy = when (evidence.status) {
            MetricProgressionStatus.PLATEAU, MetricProgressionStatus.REGRESSED ->
                PlateauStrategy.STABILIZE_BASELINE
            else -> PlateauStrategy.NONE
        }
        return policyResult(
            evidence, WorkoutGoalProfile.POWER, "Power remains advisory",
            "$statusText, but velocity, jump height, throw distance, and power output were not measured. Keep execution fast and controlled, stop before fatigue threatens speed, and do not escalate load from this result.",
            strategy, MetricPolicyConfidence.LOW,
        )
    }

    private fun enduranceMetricPolicy(
        evidence: MetricProgressionResult,
        readiness: Readiness?,
    ): MetricGoalPolicyResult = when (evidence.status) {
        MetricProgressionStatus.IMPROVED -> {
            val supported = evidence.evidenceDimension in setOf(
                MetricEvidenceDimension.DURATION,
                MetricEvidenceDimension.DISTANCE,
                MetricEvidenceDimension.PACE,
            )
            if (!supported) {
                policyResult(
                    evidence, WorkoutGoalProfile.ENDURANCE, "Hold endurance target",
                    "Higher load alone is not endurance progress. Keep the task comparable and establish duration, distance, or pace before changing load again.",
                    PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.LOW,
                )
            } else {
                val next = if (readiness == Readiness.TIRED) {
                    "Bank the result and repeat it after recovery instead of adding work now."
                } else {
                    "Repeat it, then progress only one metric in a small step while the others stay fixed."
                }
                policyResult(
                    evidence, WorkoutGoalProfile.ENDURANCE, "Endurance work improved",
                    "Comparable ${metricDimensionName(evidence.evidenceDimension)} improved. $next",
                    confidence = MetricPolicyConfidence.HIGH,
                )
            }
        }
        MetricProgressionStatus.PLATEAU -> policyResult(
            evidence, WorkoutGoalProfile.ENDURANCE, "Build one metric",
            "The comparable work repeated. Add a small amount of duration or distance, or improve pace, while keeping every other dimension fixed.",
            PlateauStrategy.PROGRESS_ONE_METRIC, MetricPolicyConfidence.MODERATE,
        )
        MetricProgressionStatus.REGRESSED -> policyResult(
            evidence, WorkoutGoalProfile.ENDURANCE, "Rebuild work capacity",
            "The comparable metric was lower. Match the prior task under the same setup before extending duration, distance, pace, or load.",
            PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.MODERATE,
        )
        else -> insufficientMetricPolicy(evidence, WorkoutGoalProfile.ENDURANCE)
    }

    private fun generalFitnessMetricPolicy(
        evidence: MetricProgressionResult,
        readiness: Readiness?,
        fatLoss: Boolean,
    ): MetricGoalPolicyResult {
        val goal = if (fatLoss) WorkoutGoalProfile.FAT_LOSS_SUPPORT else WorkoutGoalProfile.GENERAL_FITNESS
        val outcomeGuard = if (fatLoss) {
            " This supports the training plan; it does not measure fat loss or body-composition change."
        } else {
            ""
        }
        return when (evidence.status) {
            MetricProgressionStatus.IMPROVED -> {
                val next = if (readiness == Readiness.TIRED) {
                    "Bank it, recover, and repeat rather than increasing load or density now."
                } else {
                    "Repeat it consistently, then nudge only one metric when it still feels sustainable."
                }
                policyResult(
                    evidence, goal, "Sustainable progress",
                    "A comparable work metric improved. $next$outcomeGuard",
                    confidence = if (readiness == Readiness.TIRED) MetricPolicyConfidence.LOW else MetricPolicyConfidence.MODERATE,
                )
            }
            MetricProgressionStatus.PLATEAU -> policyResult(
                evidence, goal, "Keep it sustainable",
                "Holding comparable work is acceptable. Prioritize consistency and change only one small variable when recovery is good.$outcomeGuard",
                PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.MODERATE,
            )
            MetricProgressionStatus.REGRESSED -> policyResult(
                evidence, goal, "Recover & repeat",
                "The comparable metric was lower. Choose an achievable repeat, check recovery, and avoid forcing intensity.$outcomeGuard",
                PlateauStrategy.STABILIZE_BASELINE, MetricPolicyConfidence.LOW,
            )
            else -> insufficientMetricPolicy(evidence, goal, outcomeGuard)
        }
    }

    private fun skillMetricPolicy(evidence: MetricProgressionResult): MetricGoalPolicyResult = policyResult(
        evidence, WorkoutGoalProfile.SKILL, "Form first",
        "The logged metric describes completed work, not technique quality. Prioritize form, control, and repeatability; do not escalate load without quality evidence.",
        PlateauStrategy.NONE, MetricPolicyConfidence.LOW,
    )

    private fun unknownMetricPolicy(evidence: MetricProgressionResult): MetricGoalPolicyResult = policyResult(
        evidence, WorkoutGoalProfile.UNKNOWN, "Hold & compare",
        "The training goal is unavailable, so keep the neutral evidence conservative. Repeat comparable work before making a progression decision.",
        PlateauStrategy.NONE, MetricPolicyConfidence.LOW, CoachInsight.NONE,
    )

    private fun insufficientMetricPolicy(
        evidence: MetricProgressionResult,
        goal: WorkoutGoalProfile,
        suffix: String = "",
    ): MetricGoalPolicyResult {
        val detail = when (evidence.status) {
            MetricProgressionStatus.INCOMPARABLE ->
                "The sessions are not comparable. Keep the setup fixed and change only one logged dimension.$suffix"
            MetricProgressionStatus.INCOMPLETE ->
                "Required metric data is incomplete. Finish the tracked fields before making a progression decision.$suffix"
            MetricProgressionStatus.BASELINE ->
                "This is a baseline only. Repeat the same setup and set count before progressing.$suffix"
            else ->
                "The evidence is advisory only. Gather another comparable session before progressing.$suffix"
        }
        return policyResult(
            evidence, goal,
            if (evidence.status == MetricProgressionStatus.INCOMPARABLE) "Hold & compare" else "Gather comparable data",
            detail,
            PlateauStrategy.NONE, MetricPolicyConfidence.LOW,
        )
    }

    private fun policyResult(
        evidence: MetricProgressionResult,
        goal: WorkoutGoalProfile,
        title: String,
        detail: String,
        plateauStrategy: PlateauStrategy = PlateauStrategy.NONE,
        confidence: MetricPolicyConfidence = MetricPolicyConfidence.LOW,
        insight: CoachInsight = CoachInsight.GOAL_QUALITY,
    ): MetricGoalPolicyResult = MetricGoalPolicyResult(
        goal = goal,
        evidence = evidence,
        suggestion = OverloadSuggestion(title = title, detail = detail, suggestedWeight = null),
        insight = insight,
        plateauStrategy = plateauStrategy,
        confidence = confidence,
    )

    private fun neutralMetricConfidence(status: MetricProgressionStatus): MetricPolicyConfidence = when (status) {
        MetricProgressionStatus.IMPROVED -> MetricPolicyConfidence.HIGH
        MetricProgressionStatus.PLATEAU, MetricProgressionStatus.REGRESSED -> MetricPolicyConfidence.MODERATE
        else -> MetricPolicyConfidence.LOW
    }

    private fun metricDimensionName(dimension: MetricEvidenceDimension): String = when (dimension) {
        MetricEvidenceDimension.DURATION -> "duration"
        MetricEvidenceDimension.DISTANCE -> "distance"
        MetricEvidenceDimension.PACE -> "pace"
        MetricEvidenceDimension.LOAD -> "load"
        MetricEvidenceDimension.NONE -> "work"
    }

    private data class MetricSessionSnapshot(
        val setCount: Int,
        val durationSeconds: Int,
        val distanceMeters: Double,
        val loadKg: Double?,
        val allDurationValid: Boolean,
        val allDistanceValid: Boolean,
        val loadIsStable: Boolean,
    ) {
        fun isCompleteFor(type: ProgressionTrackingType): Boolean = when (type) {
            ProgressionTrackingType.TIME_ONLY -> allDurationValid
            ProgressionTrackingType.TIME_LOAD -> allDurationValid && hasPositiveStableLoad()
            ProgressionTrackingType.DISTANCE_ONLY -> allDistanceValid
            ProgressionTrackingType.DISTANCE_LOAD -> allDistanceValid && hasPositiveStableLoad()
            ProgressionTrackingType.TIME_DISTANCE -> allDurationValid && allDistanceValid
            ProgressionTrackingType.TIME_DISTANCE_LOAD ->
                allDurationValid && allDistanceValid && hasPositiveStableLoad()
            else -> false
        }

        private fun hasPositiveStableLoad(): Boolean =
            loadIsStable && loadKg != null && loadKg > WEIGHT_EPSILON
    }

    private fun metricSnapshot(sets: List<LoggedSetEntity>): MetricSessionSnapshot {
        val loads = sets.map { it.weight }
        val stableLoad = loads.isNotEmpty() && (loads.maxOrNull()!! - loads.minOrNull()!!) <= WEIGHT_EPSILON
        return MetricSessionSnapshot(
            setCount = sets.size,
            durationSeconds = sets.sumOf { it.durationSeconds ?: 0 },
            distanceMeters = sets.sumOf { it.distanceMeters ?: 0.0 },
            loadKg = loads.firstOrNull()?.takeIf { stableLoad },
            allDurationValid = sets.isNotEmpty() && sets.all { (it.durationSeconds ?: 0) > 0 },
            allDistanceValid = sets.isNotEmpty() && sets.all { (it.distanceMeters ?: 0.0) > 0.0 },
            loadIsStable = stableLoad,
        )
    }

    private fun inferMetricTrackingType(sets: List<LoggedSetEntity>): ProgressionTrackingType {
        val sessions = groupSessionsNewestFirst(sets)
        if (sessions.isEmpty()) return ProgressionTrackingType.UNKNOWN
        fun shape(session: List<LoggedSetEntity>): ProgressionTrackingType {
            if (session.isEmpty()) return ProgressionTrackingType.UNKNOWN
            val hasDuration = session.all { (it.durationSeconds ?: 0) > 0 }
            val hasDistance = session.all { (it.distanceMeters ?: 0.0) > 0.0 }
            val hasLoad = session.any { it.weight > WEIGHT_EPSILON }
            return when {
                hasDuration && hasDistance && hasLoad -> ProgressionTrackingType.TIME_DISTANCE_LOAD
                hasDuration && hasDistance -> ProgressionTrackingType.TIME_DISTANCE
                hasDuration && hasLoad -> ProgressionTrackingType.TIME_LOAD
                hasDuration -> ProgressionTrackingType.TIME_ONLY
                hasDistance && hasLoad -> ProgressionTrackingType.DISTANCE_LOAD
                hasDistance -> ProgressionTrackingType.DISTANCE_ONLY
                else -> ProgressionTrackingType.UNKNOWN
            }
        }
        val shapes = sessions.take(2).map(::shape)
        return shapes.firstOrNull()
            ?.takeIf { first -> first != ProgressionTrackingType.UNKNOWN && shapes.all { it == first } }
            ?: ProgressionTrackingType.UNKNOWN
    }

    private fun compareTimeOnly(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
        previous: MetricSessionSnapshot,
    ): MetricProgressionResult = when {
        latest.durationSeconds > previous.durationSeconds -> metricResult(
            type, MetricProgressionStatus.IMPROVED, "Build time",
            "Up from ${formatDuration(previous.durationSeconds)} to ${formatDuration(latest.durationSeconds)} total with the same set count. Keep form stable and build gradually.",
            MetricEvidenceDimension.DURATION,
        )
        latest.durationSeconds < previous.durationSeconds -> metricResult(
            type, MetricProgressionStatus.REGRESSED, "Hold time",
            "Below the previous comparable total (${formatDuration(previous.durationSeconds)}). Match it with the same form before adding time.",
            MetricEvidenceDimension.DURATION,
        )
        else -> metricResult(
            type, MetricProgressionStatus.PLATEAU, "Build time",
            "Matched ${formatDuration(latest.durationSeconds)} total with the same set count. Add a small amount of time while keeping form stable.",
            MetricEvidenceDimension.DURATION,
        )
    }

    private fun compareDistanceOnly(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
        previous: MetricSessionSnapshot,
    ): MetricProgressionResult = when {
        latest.distanceMeters > previous.distanceMeters + DISTANCE_EPSILON -> metricResult(
            type, MetricProgressionStatus.IMPROVED, "Build distance",
            "Distance increased from ${formatDistance(previous.distanceMeters)} to ${formatDistance(latest.distanceMeters)} with the same set count.",
            MetricEvidenceDimension.DISTANCE,
        )
        latest.distanceMeters < previous.distanceMeters - DISTANCE_EPSILON -> metricResult(
            type, MetricProgressionStatus.REGRESSED, "Hold distance",
            "Below the previous comparable distance (${formatDistance(previous.distanceMeters)}). Match it before extending the work.",
            MetricEvidenceDimension.DISTANCE,
        )
        else -> metricResult(
            type, MetricProgressionStatus.PLATEAU, "Build distance",
            "Matched ${formatDistance(latest.distanceMeters)} with the same set count. Add distance in a small step.",
            MetricEvidenceDimension.DISTANCE,
        )
    }

    private fun compareTimeLoad(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
        previous: MetricSessionSnapshot,
    ): MetricProgressionResult {
        val sameLoad = sameLoad(latest, previous)
        val sameTime = latest.durationSeconds == previous.durationSeconds
        return when {
            sameLoad -> compareTimeOnly(type, latest, previous)
            sameTime && latest.loadKg!! > previous.loadKg!! + WEIGHT_EPSILON -> metricResult(
                type, MetricProgressionStatus.IMPROVED, "Hold & build",
                "Matched ${formatDuration(latest.durationSeconds)} at a higher load. Keep this load until the hold is repeatable; no automatic load increase is inferred.",
                MetricEvidenceDimension.LOAD,
            )
            sameTime && latest.loadKg!! < previous.loadKg!! - WEIGHT_EPSILON -> metricResult(
                type, MetricProgressionStatus.REGRESSED, "Hold & rebuild",
                "Matched the time at a lower load. Rebuild one dimension before changing another.",
                MetricEvidenceDimension.LOAD,
            )
            else -> incomparableMetric(type, "Time and load changed together, so the sessions are not a clean comparison.")
        }
    }

    private fun compareDistanceLoad(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
        previous: MetricSessionSnapshot,
    ): MetricProgressionResult {
        val sameLoad = sameLoad(latest, previous)
        val sameDistance = sameDistance(latest, previous)
        return when {
            sameLoad -> compareDistanceOnly(type, latest, previous)
            sameDistance && latest.loadKg!! > previous.loadKg!! + WEIGHT_EPSILON -> metricResult(
                type, MetricProgressionStatus.IMPROVED, "Hold & build",
                "Matched ${formatDistance(latest.distanceMeters)} at a higher load. Repeat it on the same setup; no automatic load increase is inferred.",
                MetricEvidenceDimension.LOAD,
            )
            sameDistance && latest.loadKg!! < previous.loadKg!! - WEIGHT_EPSILON -> metricResult(
                type, MetricProgressionStatus.REGRESSED, "Hold & rebuild",
                "Matched the distance at a lower load. Rebuild one dimension before changing another.",
                MetricEvidenceDimension.LOAD,
            )
            else -> incomparableMetric(type, "Distance and load changed together. Repeat one variable on the same setup before progressing the other.")
        }
    }

    private fun compareTimeDistance(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
        previous: MetricSessionSnapshot,
        requiresSameLoad: Boolean,
    ): MetricProgressionResult {
        val loadMatches = !requiresSameLoad || sameLoad(latest, previous)
        val distanceMatches = sameDistance(latest, previous)
        val timeMatches = latest.durationSeconds == previous.durationSeconds
        val setupClause = if (requiresSameLoad) " if the sled and surface matched" else ""
        return when {
            loadMatches && distanceMatches && latest.durationSeconds < previous.durationSeconds -> metricResult(
                type, MetricProgressionStatus.IMPROVED, "Improve pace",
                "Same distance completed faster (${formatDuration(previous.durationSeconds)} to ${formatDuration(latest.durationSeconds)})$setupClause. Keep the setup unchanged.",
                MetricEvidenceDimension.PACE,
            )
            loadMatches && distanceMatches && latest.durationSeconds > previous.durationSeconds -> metricResult(
                type, MetricProgressionStatus.REGRESSED, "Hold pace",
                "The same distance took longer than the previous comparable session. Match that pace before changing distance or load.",
                MetricEvidenceDimension.PACE,
            )
            loadMatches && timeMatches && latest.distanceMeters > previous.distanceMeters + DISTANCE_EPSILON -> metricResult(
                type, MetricProgressionStatus.IMPROVED, "Build distance",
                "More distance in the same time$setupClause. Repeat it before changing another variable.",
                MetricEvidenceDimension.DISTANCE,
            )
            loadMatches && timeMatches && latest.distanceMeters < previous.distanceMeters - DISTANCE_EPSILON -> metricResult(
                type, MetricProgressionStatus.REGRESSED, "Hold distance",
                "Less distance in the same time. Match the previous distance before changing another variable.",
                MetricEvidenceDimension.DISTANCE,
            )
            loadMatches && distanceMatches && timeMatches -> metricResult(
                type, MetricProgressionStatus.PLATEAU, "Hold & build",
                "Matched time and distance$setupClause. Change only one variable in a small step next session.",
                MetricEvidenceDimension.PACE,
            )
            requiresSameLoad && distanceMatches && timeMatches && latest.loadKg!! > previous.loadKg!! + WEIGHT_EPSILON -> metricResult(
                type, MetricProgressionStatus.IMPROVED, "Hold & build",
                "Matched time and distance at a higher load if the sled and surface matched. Repeat it; no automatic load increase is inferred.",
                MetricEvidenceDimension.LOAD,
            )
            else -> incomparableMetric(
                type,
                "Multiple variables or the load changed together. Pace is only compared when distance${if (requiresSameLoad) " and load" else ""} stay the same.",
            )
        }
    }

    private fun baselineMetric(
        type: ProgressionTrackingType,
        latest: MetricSessionSnapshot,
    ): MetricProgressionResult {
        val logged = when (type) {
            ProgressionTrackingType.TIME_ONLY, ProgressionTrackingType.TIME_LOAD ->
                "${formatDuration(latest.durationSeconds)} total"
            ProgressionTrackingType.DISTANCE_ONLY, ProgressionTrackingType.DISTANCE_LOAD ->
                formatDistance(latest.distanceMeters)
            ProgressionTrackingType.TIME_DISTANCE, ProgressionTrackingType.TIME_DISTANCE_LOAD ->
                "${formatDistance(latest.distanceMeters)} in ${formatDuration(latest.durationSeconds)}"
            else -> "one session"
        }
        val title = when (type) {
            ProgressionTrackingType.TIME_ONLY, ProgressionTrackingType.TIME_LOAD -> "Build time"
            ProgressionTrackingType.DISTANCE_ONLY, ProgressionTrackingType.DISTANCE_LOAD -> "Build distance"
            else -> "Set baseline"
        }
        return metricResult(
            type, MetricProgressionStatus.BASELINE, title,
            "You logged $logged. Repeat the same set count and setup once before changing one progression variable.",
        )
    }

    private fun incompleteMetric(type: ProgressionTrackingType): MetricProgressionResult = metricResult(
        type,
        MetricProgressionStatus.INCOMPLETE,
        "Complete the metrics",
        "At least one set is missing a required duration or distance value. Complete the tracked fields before comparing progression.",
    )

    private fun incomparableMetric(type: ProgressionTrackingType, detail: String): MetricProgressionResult =
        metricResult(type, MetricProgressionStatus.INCOMPARABLE, "Hold & compare", detail)

    private fun metricResult(
        type: ProgressionTrackingType,
        status: MetricProgressionStatus,
        title: String,
        detail: String,
        evidenceDimension: MetricEvidenceDimension = MetricEvidenceDimension.NONE,
    ): MetricProgressionResult = MetricProgressionResult(
        trackingType = type,
        status = status,
        suggestion = OverloadSuggestion(title = title, detail = detail, suggestedWeight = null),
        evidenceDimension = evidenceDimension,
    )

    private fun sameLoad(a: MetricSessionSnapshot, b: MetricSessionSnapshot): Boolean =
        a.loadKg != null && b.loadKg != null && abs(a.loadKg - b.loadKg) <= WEIGHT_EPSILON

    private fun sameDistance(a: MetricSessionSnapshot, b: MetricSessionSnapshot): Boolean =
        abs(a.distanceMeters - b.distanceMeters) <= DISTANCE_EPSILON

    private fun formatDistance(meters: Double): String = "${formatWeight(meters)} m"

    /** mm:ss (or h:mm:ss), deterministic and locale-independent. */
    private fun formatDuration(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "$h:${pad2(m)}:${pad2(s)}" else "$m:${pad2(s)}"
    }

    private fun pad2(v: Int): String = if (v < 10) "0$v" else v.toString()

    /**
     * Full typed result. Deterministic and rule-based. [profile] (V3.5) supplies catalog metadata and
     * [readiness] (V3.6B) the latest session's subjective signal — when null the engine falls back to
     * name inference / no-readiness exactly as before. A deterministic [ProgressionSuggestion.confidence]
     * and an objective [ProgressionSuggestion.sessionQuality] are attached on top of the core decision;
     * readiness normally modifies confidence/wording only. V3.11B adds one explicit exception: TIRED
     * general-fitness/fat-loss-support work cannot auto-increase load in the same recommendation.
     */
    fun progression(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        profile: ProgressionProfile? = null,
        readiness: Readiness? = null,
        readinessBySession: Map<Long, Readiness?>? = null,
        workoutContext: WorkoutContext? = null,
    ): ProgressionSuggestion {
        val core = progressionImpl(exercise, recentSets, profile, readiness)
        val quality = sessionQualityFor(exercise, recentSets)
        // V3.7B: readiness *trend* (multi-session + readiness-vs-quality mismatch). Confidence/wording only.
        val trend = readinessTrend(recentSets, readinessBySession, quality, core)
        val withMeta = core.copy(
            confidence = (confidenceFor(exercise, recentSets, profile, readiness, core, quality) +
                readinessTrendConfidenceDelta(trend)).coerceIn(0, 100),
            sessionQuality = quality,
        )
        // V3.7A: read-only long-term trend overlay (wording/title/insight only; action & weight intact).
        val withInsight = applyTrendInsight(exercise, recentSets, withMeta)
        // V3.7B: append the readiness-trend clause once, to the final reason (never touches action/weight).
        val clause = readinessTrendClause(trend)
        val withReadiness = if (clause.isEmpty()) withInsight else withInsight.copy(reason = withInsight.reason + clause)
        // V3.7C-H: whole-workout + hypertrophy-quality context overlay (confidence/wording/insight only).
        val withWorkout = applyWorkoutContext(exercise, recentSets, withReadiness, workoutContext)
        // V3.8-H: hypertrophy plateau-exit strategy overlay (confidence/wording/strategy tag only).
        val withStrategy = applyPlateauStrategy(exercise, recentSets, withWorkout, workoutContext, trend)
        return applyGoalActionSafety(exercise, recentSets, withStrategy, workoutContext, readiness)
    }

    /** Final, deterministic goal-safety guard. A pre-existing decrease is never suppressed. */
    private fun applyGoalActionSafety(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        core: ProgressionSuggestion,
        ctx: WorkoutContext?,
        readiness: Readiness?,
    ): ProgressionSuggestion {
        if (core.action == Action.DECREASE_WEIGHT) return core
        val goal = when {
            ctx != null -> effectiveGoal(exercise, ctx, core.progressionType)
            core.progressionType == ProgressionType.SKILL_FORM -> WorkoutGoalProfile.SKILL
            else -> return core
        }
        val latest = comparableWindow(exercise, recentSets).firstOrNull()
        val currentLoad = latest?.mainWeight?.takeIf { it > 0.0 }
        val reachedFailure = latest?.workingSets?.any { it.rir <= 0 } == true
        val attemptedProgression = core.action == Action.INCREASE_WEIGHT ||
            core.action == Action.INCREASE_REPS_OR_DIFFICULTY

        if (goal == WorkoutGoalProfile.POWER && reachedFailure) return core.copy(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = currentLoad,
            titleOverride = "Power: keep reserve",
            reason = "A set reached RIR 0. Hold the load and finish power sets with reserve so fatigue does not turn speed work into a grind.",
            insight = CoachInsight.GOAL_QUALITY,
            plateauStrategy = PlateauStrategy.NONE,
            confidence = minOf(core.confidence, 40),
        )
        if (goal == WorkoutGoalProfile.POWER && attemptedProgression) return core.copy(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = currentLoad,
            titleOverride = "Power: hold load",
            reason = "Rep and load completion alone cannot verify power output. Hold the current task, keep execution fast and controlled, and do not progress load without measured output or reliable quality evidence.",
            insight = CoachInsight.GOAL_QUALITY,
            plateauStrategy = PlateauStrategy.NONE,
            confidence = minOf(core.confidence, 45),
        )
        if (goal == WorkoutGoalProfile.STRENGTH && reachedFailure && attemptedProgression) return core.copy(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = currentLoad,
            titleOverride = "Strength: repeat cleanly",
            reason = "A working set reached RIR 0. Hold this load and repeat the prescribed reps with stable technique and some reserve before progressing.",
            insight = CoachInsight.GOAL_QUALITY,
            plateauStrategy = PlateauStrategy.NONE,
            confidence = minOf(core.confidence, 50),
        )
        if (goal == WorkoutGoalProfile.SKILL && attemptedProgression) return core.copy(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = currentLoad,
            titleOverride = "Form first",
            reason = "Reps and load do not measure technique quality. Repeat this task unless form, control, and repeatability are verified; only then choose one small progression.",
            insight = CoachInsight.NONE,
            plateauStrategy = PlateauStrategy.NONE,
            confidence = minOf(core.confidence, 40),
        )
        if (goal == WorkoutGoalProfile.UNKNOWN && attemptedProgression) return core.copy(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = currentLoad,
            titleOverride = "Hold & compare",
            reason = "The training goal is unavailable. Repeat comparable work before choosing a progression policy.",
            insight = CoachInsight.NONE,
            plateauStrategy = PlateauStrategy.NONE,
            confidence = minOf(core.confidence, 35),
        )
        if ((goal == WorkoutGoalProfile.GENERAL_FITNESS || goal == WorkoutGoalProfile.FAT_LOSS_SUPPORT) &&
            readiness == Readiness.TIRED && attemptedProgression
        ) {
            val outcomeGuard = if (goal == WorkoutGoalProfile.FAT_LOSS_SUPPORT) {
                " This logged work does not measure fat loss or body-composition change."
            } else {
                ""
            }
            return core.copy(
                action = Action.KEEP_WEIGHT,
                suggestedWeight = currentLoad,
                titleOverride = "Recover & repeat",
                reason = "You logged a tired day. Bank the completed work, recover, and repeat this task instead of increasing intensity now.$outcomeGuard",
                insight = CoachInsight.GOAL_QUALITY,
                plateauStrategy = PlateauStrategy.NONE,
                confidence = minOf(core.confidence, 45),
            )
        }
        return core
    }

    private fun progressionImpl(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        profile: ProgressionProfile?,
        readiness: Readiness?,
    ): ProgressionSuggestion {
        val sessions = groupSessionsNewestFirst(recentSets)
            .map { session -> session.filter { it.reps > 0 } }
        val nameType = inferTypeFromName(exercise.name)
        val latestEval = evalSession(sessions.getOrNull(0).orEmpty())
            ?: return ProgressionSuggestion(
                action = Action.KEEP_WEIGHT,
                suggestedWeight = null,
                targetText = repsTarget(exercise),
                reason = "Log a completed working set and I'll set your next target.",
                progressionType = profile?.progressionType ?: nameType,
            )

        val m = metrics(exercise, latestEval)
        val usingAddedWeight = m.currentWeight > 0.0
        // V3.5: catalog profile decides the type when present; otherwise infer from the name. A
        // bodyweight move logged WITH added load still behaves like a (conservative) compound lift.
        val baseType = profile?.progressionType ?: nameType
        val isBodyweightMove = profile?.isBodyweight ?: (nameType == ProgressionType.BODYWEIGHT)
        val type = when {
            isBodyweightMove && !usingAddedWeight -> ProgressionType.BODYWEIGHT
            isBodyweightMove && usingAddedWeight -> ProgressionType.COMPOUND_LOAD
            else -> baseType
        }

        // Trend/outlier check runs first: a session far below the recent baseline should not be
        // judged by the normal single-session rules (possible bad day / new machine / log slip).
        val previousEval = evalSession(sessions.getOrNull(1).orEmpty())
        val previous2Eval = evalSession(sessions.getOrNull(2).orEmpty())
        outlierSuggestion(exercise, type, m, previousEval, previous2Eval, readiness)?.let { return it }

        // V3.5: a large load jump vs the last comparable session that did NOT fully succeed — hold the
        // new load positively rather than punishing the jump or increasing off an unfinished session.
        loadIncreaseHold(exercise, type, m, previousEval)?.let { return it }

        return when (type) {
            ProgressionType.BODYWEIGHT -> bodyweightRule(exercise, m)
            ProgressionType.ISOLATION_SLOW -> isolationRule(exercise, m, previousEval, profile)
            ProgressionType.SKILL_FORM -> skillRule(exercise, m)
            ProgressionType.COMPOUND_LOAD ->
                compoundRule(exercise, m, weightedBodyweight = isBodyweightMove, profile = profile)
        }
    }

    /**
     * V3.5 load-jump awareness. When the latest comparable session used a clearly heavier working
     * weight than the previous one but did not cleanly succeed, the right call is to *hold the new
     * load with positive framing* — not to punish a big improvement, and not to increase off an
     * unfinished session. A clean success at the heavier load falls through to the normal increase
     * rules. Bodyweight/skill types and non-comparable (bodyweight↔loaded) histories are skipped.
     */
    private fun loadIncreaseHold(
        exercise: ExerciseEntity,
        type: ProgressionType,
        m: Metrics,
        previous: SessionEval?,
    ): ProgressionSuggestion? {
        if (type == ProgressionType.BODYWEIGHT || type == ProgressionType.SKILL_FORM) return null
        val latest = m.eval
        if (latest.isBodyweight || latest.workingSets.isEmpty()) return null
        if (previous == null || previous.isBodyweight != latest.isBodyweight || previous.mainWeight <= 0.0) return null
        if (latest.mainWeight <= previous.mainWeight * (1.0 + LOAD_JUMP_FRACTION)) return null
        val cleanSuccess = m.enoughSets && m.allSetsAtLeastMin && m.avgRir >= RIR_INCREASE_GATE
        if (cleanSuccess) return null
        return ProgressionSuggestion(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = m.currentWeight.takeIf { it > 0.0 },
            targetText = repsTarget(exercise),
            reason = "Strong load increase (${formatWeight(previous.mainWeight)}kg → ${formatWeight(latest.mainWeight)}kg) — nice jump. " +
                "Stay at this load and aim for ${m.minRep}+ reps on every set before going heavier.",
            progressionType = type,
        )
    }

    /**
     * Build a [ProgressionProfile] from a matched catalog exercise's raw fields. Takes primitives (the
     * progression-type *name* string) so the engine never imports the catalog data package. Unknown
     * type names fall back to the conservative COMPOUND_LOAD.
     */
    fun profileFromCatalog(
        progressionTypeName: String,
        incrementKg: Double,
        sessionsRequiredBeforeIncrease: Int,
        isBodyweight: Boolean,
        isUnilateral: Boolean,
        trackingTypeName: String = "REPS_WEIGHT",
    ): ProgressionProfile = ProgressionProfile(
        progressionType = when (progressionTypeName) {
            "COMPOUND_LOAD" -> ProgressionType.COMPOUND_LOAD
            "ISOLATION_SLOW" -> ProgressionType.ISOLATION_SLOW
            "BODYWEIGHT" -> ProgressionType.BODYWEIGHT
            "SKILL_FORM" -> ProgressionType.SKILL_FORM
            else -> ProgressionType.COMPOUND_LOAD
        },
        incrementKg = incrementKg,
        sessionsRequiredBeforeIncrease = sessionsRequiredBeforeIncrease,
        isBodyweight = isBodyweight,
        isUnilateral = isUnilateral,
        trackingType = ProgressionTrackingType.fromName(trackingTypeName),
    )

    /**
     * Deterministic confidence (0–100) for [core], from how much agreeing evidence backs it:
     * comparable history depth, a settled working weight, RIR data, and whether catalog metadata was
     * available. "Hold & verify" outliers are explicitly low-confidence. Engine-only (no UI change).
     */
    private fun confidenceFor(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        profile: ProgressionProfile?,
        readiness: Readiness?,
        core: ProgressionSuggestion,
        quality: SessionQuality,
    ): Int {
        val sessions = groupSessionsNewestFirst(recentSets).map { s -> s.filter { it.reps > 0 } }
        val latest = evalSession(sessions.getOrNull(0).orEmpty()) ?: return 25
        var c = 50
        if (profile != null) c += 12
        val comparable = listOf(1, 2)
            .mapNotNull { evalSession(sessions.getOrNull(it).orEmpty()) }
            .count { it.isBodyweight == latest.isBodyweight && it.score > 0.0 }
        c += when {
            comparable >= 2 -> 16
            comparable == 1 -> 8
            else -> 0
        }
        val m = metrics(exercise, latest)
        if (m.enoughSets) c += 10
        if (latest.rampSetCount == 0 && latest.backoffSetCount == 0) c += 6
        if (latest.workingSets.any { it.rir > 0 }) c += 8
        val needed = (profile?.sessionsRequiredBeforeIncrease ?: 1).coerceAtLeast(1)
        if (comparable + 1 < needed) c -= 8
        val holdVerify = core.titleOverride == "Hold & verify"
        if (holdVerify) c -= 25
        // V3.6B: objective session quality nudges confidence.
        c += when (quality) {
            SessionQuality.EXCELLENT -> 10
            SessionQuality.SOLID -> 4
            SessionQuality.MIXED -> -4
            SessionQuality.POOR -> -10
        }
        // V3.6B: readiness is a small, bounded modifier. On a suspected off-day (Hold & verify) TIRED
        // corroborates the dip (more confident hold) while STRONG contradicts it (less confident, verify);
        // otherwise STRONG lifts and TIRED tempers confidence. It never moves the action or the weight.
        c += when (readiness) {
            Readiness.STRONG -> if (holdVerify) -6 else 5
            Readiness.TIRED -> if (holdVerify) 4 else -5
            else -> 0
        }
        return c.coerceIn(0, 100)
    }

    // ---- V3.6B session quality + readiness helpers -------------------------------------

    /** Objective quality of the latest session, derived from existing metrics. Runtime-only. */
    private fun sessionQualityFor(exercise: ExerciseEntity, recentSets: List<LoggedSetEntity>): SessionQuality {
        val sessions = groupSessionsNewestFirst(recentSets).map { s -> s.filter { it.reps > 0 } }
        val latest = evalSession(sessions.getOrNull(0).orEmpty()) ?: return SessionQuality.MIXED
        return sessionQuality(metrics(exercise, latest))
    }

    private fun sessionQuality(m: Metrics): SessionQuality {
        if (!m.enoughSets) return SessionQuality.MIXED
        if (m.tooHeavy || m.severelyTooHeavy) return SessionQuality.POOR
        val clean = m.eval.rampSetCount == 0 && m.eval.backoffSetCount == 0
        return when {
            m.allSetsAtTopRange && m.avgRir >= 2.0 && clean -> SessionQuality.EXCELLENT
            m.allSetsAtLeastMin && !m.mostlyFailed -> SessionQuality.SOLID
            else -> SessionQuality.MIXED
        }
    }

    /**
     * Short, optional readiness context appended to a reason. Empty for NORMAL/UNKNOWN so existing
     * wording (and tests) are unchanged when no readiness is supplied. [dropContext] = the call is a
     * drop/hold-&-verify/decrease situation, where TIRED explains the dip and STRONG flags a mismatch.
     */
    private fun readinessClause(readiness: Readiness?, dropContext: Boolean): String = when (readiness) {
        Readiness.TIRED -> if (dropContext) " You logged a tired day, so this dip makes sense — check again when you're fresh." else ""
        Readiness.STRONG -> if (dropContext) " You logged a strong day, though, so it's worth double-checking the machine or the entry." else ""
        else -> ""
    }

    // ---- V3.7B Readiness trend (multi-session + readiness-vs-quality mismatch) ----------
    //
    // Latest readiness (V3.6B) is not enough. Given a read-only map of readiness per recent session, this
    // reads the *trend* and the readiness↔objective-quality *mismatch*, and feeds it to confidence and
    // wording only — it NEVER changes the action or the suggested weight, and it can never bypass a safety
    // gate (those are decided upstream on objective performance/RIR). Readiness is subjective context, not
    // truth; objective performance still dominates. To avoid double-counting the V3.6B latest-session
    // handling, `strongButDown` deliberately excludes the outlier "Hold & verify" case (V3.6B owns that).

    private data class ReadinessTrend(
        val repeatedTired: Boolean,
        val strongButDown: Boolean,
        val tiredButExcellent: Boolean,
        val normalButPoor: Boolean,
    )

    private val NEUTRAL_READINESS_TREND = ReadinessTrend(false, false, false, false)

    private fun readinessTrend(
        recentSets: List<LoggedSetEntity>,
        readinessBySession: Map<Long, Readiness?>?,
        quality: SessionQuality,
        core: ProgressionSuggestion,
    ): ReadinessTrend {
        if (readinessBySession.isNullOrEmpty()) return NEUTRAL_READINESS_TREND
        // Per-session readiness aligned to the comparable window (newest first).
        val points = groupSessionsNewestFirst(recentSets)
            .map { g -> g.filter { it.reps > 0 } }
            .mapNotNull { g ->
                val eval = evalSession(g) ?: return@mapNotNull null
                eval to readinessBySession[g.firstOrNull()?.sessionId]
            }
        val latest = points.firstOrNull() ?: return NEUTRAL_READINESS_TREND
        val window = points
            .filter { it.first.isBodyweight == latest.first.isBodyweight && it.first.score > 0.0 }
            .take(TREND_WINDOW)
        val latestReadiness = window.firstOrNull()?.second
        // Repeated TIRED = at least two of the recent comparable sessions were logged tired (multi-session
        // signal the V3.6B latest-only path cannot see). UNKNOWN/null never counts and never penalizes.
        val repeatedTired = window.take(3).count { it.second == Readiness.TIRED } >= 2
        // STRONG but the objective session collapsed (POOR), outside the outlier hold V3.6B already covers.
        val strongButDown = latestReadiness == Readiness.STRONG &&
            quality == SessionQuality.POOR && core.titleOverride != "Hold & verify"
        // Strong performance despite fatigue — a positive mismatch worth acknowledging.
        val tiredButExcellent = latestReadiness == Readiness.TIRED && quality == SessionQuality.EXCELLENT
        // Poor session on a normal-feeling day — could be a bad day or the start of a real drop.
        val normalButPoor = latestReadiness == Readiness.NORMAL && quality == SessionQuality.POOR
        return ReadinessTrend(repeatedTired, strongButDown, tiredButExcellent, normalButPoor)
    }

    /** Bounded, single (non-stacking) confidence nudge — readiness never dominates objective performance. */
    private fun readinessTrendConfidenceDelta(trend: ReadinessTrend): Int = when {
        trend.strongButDown -> -6
        trend.repeatedTired -> -5
        trend.normalButPoor -> -5
        trend.tiredButExcellent -> 3
        else -> 0
    }

    /** One short, optional readiness-trend clause (empty = no signal, so wording is unchanged). */
    private fun readinessTrendClause(trend: ReadinessTrend): String = when {
        trend.strongButDown -> " You felt strong but the reps came up short — double-check the machine, your form, or the logged numbers."
        trend.repeatedTired -> " You've logged a few tired days lately, so treat this as a recovery signal — rest up before pushing again."
        trend.normalButPoor -> " This came in low on a normal-feeling day — verify next session; if it sticks it may be a real drop rather than an off day."
        trend.tiredButExcellent -> " Strong work despite logging a tired day — that's a good sign."
        else -> ""
    }

    // ---- V3.7C-H Whole-workout + hypertrophy-quality context ---------------------------
    //
    // A read-only overlay that layers WHOLE-SESSION context on top of the per-exercise decision. It is
    // hypertrophy-oriented but goal-gated (WorkoutGoalProfile) so STRENGTH/POWER/ENDURANCE/SKILL can plug
    // in later without a core rewrite; when the context is null or the goal isn't HYPERTROPHY it is inert
    // (exact V3.7B behavior — every existing test passes null and is unaffected). It NEVER changes the
    // action or the suggested weight — only confidence, wording, and the insight tag. Safety gates and the
    // per-exercise decision are decided entirely upstream.
    //
    // Design notes from the WQS research (used as inspiration, not copied):
    //  - The raw composite Workout Quality Score is rejected — no stored/exposed number, opaque + biased.
    //  - Effective Reps = max(0,5-RIR) and the EVR "productive volume" idea are reused only as an internal,
    //    per-exercise stimulus read over already-warmup-filtered working sets (never a gate).
    //  - Cross-exercise FAI (fooled by compound->isolation order) is rejected; systemic fatigue is inferred
    //    from how many of the day's lifts are independently down vs their own baselines.
    //  - The subjective multiplier is rejected (readiness is already handled by V3.6B/V3.7B — no double-count).

    /**
     * Pure, deterministic helper for the ViewModel to build [WorkoutContext] sibling counts WITHOUT any
     * DAO access in the engine: is this exercise's latest comparable session notably below its own recent
     * baseline? Returns null when there isn't enough comparable history to judge. Reuses the same
     * comparable window and [OUTLIER_DROP] threshold the per-exercise rules use.
     */
    fun latestSessionDownVsBaseline(exercise: ExerciseEntity, recentSets: List<LoggedSetEntity>): Boolean? {
        val window = comparableWindow(exercise, recentSets)
        if (window.size < 2) return null
        val latest = window.first()
        val baseline = window.drop(1).take(2).maxByOrNull { it.score } ?: return null
        if (baseline.score <= 0.0) return null
        return (1.0 - latest.score / baseline.score) >= OUTLIER_DROP
    }

    private enum class Stimulus { PRODUCTIVE, SUBMAXIMAL, NEUTRAL }

    /**
     * V3.11C.1 delegates program-goal x modality precedence to one pure resolver. The engine still owns
     * neither persistence nor a second detector; it supplies only existing target/profile metadata and
     * then runs the already-established policy selected by the resolver.
     */
    private fun effectiveGoal(
        exercise: ExerciseEntity,
        ctx: WorkoutContext,
        progressionType: ProgressionType,
        trackingType: ProgressionTrackingType = ProgressionTrackingType.REPS_WEIGHT,
    ): WorkoutGoalProfile = ExerciseGoalCompatibilityResolver.resolve(
        programContext = ctx,
        progressionType = progressionType,
        trackingType = trackingType,
        targetMinReps = exercise.targetMinReps,
        targetMaxReps = exercise.targetMaxReps,
    )

    private fun applyWorkoutContext(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        core: ProgressionSuggestion,
        ctx: WorkoutContext?,
    ): ProgressionSuggestion {
        // Inert without a context — keeps every existing (null-context) call intact. SKILL work gets no
        // goal-quality overlay either: control/skill moves progress on form, not effort math.
        if (ctx == null) return core
        val goal = effectiveGoal(exercise, ctx, core.progressionType)
        if (goal == WorkoutGoalProfile.SKILL) return core

        // Systemic vs local is goal-agnostic (every goal compares each lift to its own baseline):
        // only meaningful when THIS lift is down and we judged enough siblings.
        val thisDown = core.action == Action.DECREASE_WEIGHT ||
            core.titleOverride == "Hold & verify" ||
            core.insight == CoachInsight.BAD_DAY_VERIFY
        if (thisDown && ctx.siblingsEvaluated >= WORKOUT_MIN_SIBLINGS) {
            return if (ctx.siblingsDown >= SYSTEMIC_MIN_DOWN) {
                core.copy(insight = CoachInsight.SYSTEMIC_FATIGUE, reason = core.reason + SYSTEMIC_CLAUSE)
            } else {
                core.copy(insight = CoachInsight.LOCAL_DROP, reason = core.reason + localDropClause(exercise))
            }
        }

        // Goal-quality note — only when no stronger trend/fatigue label already applies.
        if (core.insight != CoachInsight.NONE) return core
        return when (goal) {
            WorkoutGoalProfile.HYPERTROPHY -> when (hypertrophyStimulus(exercise, recentSets)) {
                Stimulus.SUBMAXIMAL -> core.copy(
                    insight = CoachInsight.HYPERTROPHY_QUALITY,
                    reason = core.reason + SUBMAXIMAL_CLAUSE,
                    confidence = (core.confidence - 3).coerceIn(0, 100),
                )
                Stimulus.PRODUCTIVE -> {
                    // Contradiction guard: never praise a "clean stimulus" in the same breath as a
                    // set-hit-RIR-0 caution — praise only applies when nothing actually hit failure.
                    val latest = comparableWindow(exercise, recentSets).firstOrNull()
                    if (latest != null && latest.workingSets.none { it.rir <= 0 }) {
                        core.copy(
                            insight = CoachInsight.HYPERTROPHY_QUALITY,
                            reason = core.reason + PRODUCTIVE_CLAUSE,
                            confidence = (core.confidence + 2).coerceIn(0, 100),
                        )
                    } else {
                        core
                    }
                }
                Stimulus.NEUTRAL -> core
            }
            // POWER: quality over effort — grinding (low RIR) degrades speed work, so caution, never praise.
            WorkoutGoalProfile.POWER -> {
                val latest = comparableWindow(exercise, recentSets).firstOrNull()
                if (latest != null && latest.workingSets.size >= QUALITY_MIN_SETS && latest.avgRir < POWER_MIN_RIR) {
                    core.copy(
                        insight = CoachInsight.GOAL_QUALITY,
                        reason = core.reason + POWER_GRIND_CLAUSE,
                        confidence = (core.confidence - 3).coerceIn(0, 100),
                    )
                } else {
                    core
                }
            }
            // STRENGTH: proximity to failure has a negligible relationship with strength gain — no
            // effective-reps stimulus judgment. ENDURANCE / GENERAL_FITNESS: plenty of reps in reserve
            // at high reps is normal, sustainable training — never judged as a weak stimulus.
            else -> core
        }
    }

    /**
     * Per-exercise hypertrophy stimulus read over the latest session's WORKING sets (ramp/back-off already
     * excluded by [evalSession]). Uses avg effective reps = mean of max(0, 5 - RIR): high reserve (RIR 4-5)
     * = a lighter, submaximal stimulus; RIR ~1-2 = a productive one. Never a gate — wording/confidence only,
     * and inherently order-safe (per exercise, not cross-exercise), so a light isolation after a heavy
     * compound is judged on its own and never mistaken for "recovery" or rewarded.
     */
    private fun hypertrophyStimulus(exercise: ExerciseEntity, recentSets: List<LoggedSetEntity>): Stimulus {
        val latest = comparableWindow(exercise, recentSets).firstOrNull() ?: return Stimulus.NEUTRAL
        val working = latest.workingSets
        if (working.size < QUALITY_MIN_SETS) return Stimulus.NEUTRAL
        val avgEffective = working.map { maxOf(0, 5 - it.rir) }.average()
        return when {
            avgEffective < STIMULUS_SUBMAXIMAL_EFF -> Stimulus.SUBMAXIMAL
            avgEffective >= STIMULUS_PRODUCTIVE_EFF -> Stimulus.PRODUCTIVE
            else -> Stimulus.NEUTRAL
        }
    }

    private const val SYSTEMIC_CLAUSE =
        " Several of today's lifts are down too, so this looks like whole-session fatigue rather than a " +
            "problem with this exercise — hold, recover, and don't read too much into one lift."

    private fun localDropClause(exercise: ExerciseEntity): String = pick(exercise, listOf(
        " Your other lifts held up today, so this looks specific to this exercise — check the machine, setup, form, or the logged numbers.",
        " Only this lift dipped while the rest of your session was fine — worth checking the setup/form here rather than your overall recovery.",
    ))

    private const val SUBMAXIMAL_CLAUSE =
        " You hit the reps but kept a lot in reserve, so it was a lighter hypertrophy stimulus — " +
            "work a little closer to your limit next time."

    private const val PRODUCTIVE_CLAUSE =
        " Clean stimulus — every working set was close enough to your limit to count, without burning you out."

    private const val POWER_GRIND_CLAUSE =
        " Your reserve was low; for power work, keep 2-3 reps in reserve so speed intent stays the priority."

    // ---- V3.8-H Hypertrophy plateau-exit strategy -------------------------------------
    //
    // Turns the V3.7A [CoachInsight.PLATEAU] detection into an actionable "how to get unstuck" strategy,
    // chosen from signals the engine already computes — the RIR trend (effort deficit vs improving), the
    // V3.7B readiness trend (repeated tired = fatigue accumulation), and the V3.7C-H [WorkoutContext]
    // scope. It runs last and is GATED behind a hypertrophy [WorkoutContext] (null context = exact V3.7B
    // behavior, so every existing test is byte-identical). It NEVER changes the action or the suggested
    // weight — only the wording, confidence, and the [PlateauStrategy] tag; safety gates always win.
    //
    // Inspired by (not copied from) the plateau-strategy research: rejected its EVL/ΔP second detector
    // (reuse the existing PLATEAU trigger), its weekly-MRV volume math and escalation/cooldown state
    // machine (need stored state the runtime engine can't access), and dynamic rep-range-target mutation
    // (targets are static). Kept: double progression, deload-for-fatigue, rep-range rotation, and the
    // effort-deficit idea — each realized on top of the engine's own deterministic signals.

    private fun applyPlateauStrategy(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        core: ProgressionSuggestion,
        ctx: WorkoutContext?,
        trend: ReadinessTrend,
    ): ProgressionSuggestion {
        // Inert without a context; SKILL work never gets a load/volume plateau prescription.
        if (ctx == null) return core
        val goal = effectiveGoal(exercise, ctx, core.progressionType)
        if (goal == WorkoutGoalProfile.SKILL) return core
        return when (core.insight) {
            CoachInsight.PLATEAU -> {
                val window = comparableWindow(exercise, recentSets)
                if (window.isEmpty()) return core
                val strategy = selectPlateauStrategy(goal, window, ctx, trend)
                core.copy(
                    plateauStrategy = strategy,
                    reason = plateauStrategyMessage(exercise, goal, strategy, window.first()),
                )
            }
            // A one-off dip or a forming new baseline: hold and gather data — never a plateau strategy.
            CoachInsight.NEW_BASELINE, CoachInsight.BAD_DAY_VERIFY ->
                core.copy(plateauStrategy = PlateauStrategy.STABILIZE_BASELINE)
            else -> core
        }
    }

    /**
     * V3.9-G goal-aware plateau-exit selection. Accumulated fatigue always deloads first regardless of
     * goal; after that each goal gets the strategies that match its science:
     *  - HYPERTROPHY (V3.8-H, unchanged): effort → double progression → rep-range rotation → add-set.
     *  - STRENGTH: load quality over volume — effort/double progression, never ADD_SET (junk volume),
     *    and a stall is often just strength's slower timescale (patience beats intervention).
     *  - POWER: never pushed toward failure (grinding degrades speed work) — vary the block or hold quality.
     *  - ENDURANCE: progress reps/time/density before load — a high-rep stall never reads as failure.
     *  - GENERAL_FITNESS: sustainability first — gentle rep nudges when it's getting easier, else hold.
     */
    private fun selectPlateauStrategy(
        goal: WorkoutGoalProfile,
        window: List<SessionEval>,
        ctx: WorkoutContext,
        trend: ReadinessTrend,
    ): PlateauStrategy {
        val latest = window.first()
        val systemic = ctx.siblingsEvaluated >= WORKOUT_MIN_SIBLINGS && ctx.siblingsDown >= SYSTEMIC_MIN_DOWN
        // 1. Accumulated fatigue dominates for every goal → deload (a confirmed plateau, never a single bad day).
        if (trend.repeatedTired || systemic) return PlateauStrategy.DELOAD
        return when (goal) {
            WorkoutGoalProfile.HYPERTROPHY -> when {
                // 2. Effort deficit → push effort. The RIR gate guarantees this never fires at an already-low RIR.
                latest.avgRir >= PLATEAU_EFFORT_DEFICIT_RIR -> PlateauStrategy.INCREASE_EFFORT
                // 3. Getting easier within range → double progression.
                plateauRirImproving(window) -> PlateauStrategy.ADD_REP
                // 4. Deep, persistent local stall → rotate the rep range (stimulus staleness).
                window.size >= PLATEAU_DEEP_SESSIONS -> PlateauStrategy.CHANGE_REP_RANGE
                // 5. Otherwise a local stall with adequate effort → add a set, guarded against junk volume.
                else -> if (latest.workingSets.size in QUALITY_MIN_SETS until PLATEAU_MAX_SETS_FOR_ADD) {
                    PlateauStrategy.ADD_SET
                } else {
                    PlateauStrategy.CHANGE_REP_RANGE
                }
            }
            WorkoutGoalProfile.STRENGTH -> when {
                latest.avgRir >= PLATEAU_EFFORT_DEFICIT_RIR -> PlateauStrategy.INCREASE_EFFORT
                plateauRirImproving(window) -> PlateauStrategy.ADD_REP
                window.size >= PLATEAU_DEEP_SESSIONS -> PlateauStrategy.CHANGE_REP_RANGE
                else -> PlateauStrategy.STABILIZE_BASELINE // strength moves slower — never junk volume.
            }
            WorkoutGoalProfile.POWER -> when {
                window.size >= PLATEAU_DEEP_SESSIONS -> PlateauStrategy.CHANGE_REP_RANGE
                else -> PlateauStrategy.STABILIZE_BASELINE // quality/speed first — never a grind prescription.
            }
            WorkoutGoalProfile.ENDURANCE -> when {
                // More work before more load: reserve left or getting easier → add reps/time/density.
                latest.avgRir >= PLATEAU_EFFORT_DEFICIT_RIR || plateauRirImproving(window) -> PlateauStrategy.ADD_REP
                latest.workingSets.size in QUALITY_MIN_SETS until PLATEAU_MAX_SETS_FOR_ADD -> PlateauStrategy.ADD_SET
                else -> PlateauStrategy.CHANGE_REP_RANGE
            }
            WorkoutGoalProfile.GENERAL_FITNESS, WorkoutGoalProfile.FAT_LOSS_SUPPORT ->
                if (plateauRirImproving(window)) PlateauStrategy.ADD_REP else PlateauStrategy.STABILIZE_BASELINE
            WorkoutGoalProfile.SKILL -> PlateauStrategy.NONE // unreachable (guarded upstream)
            WorkoutGoalProfile.UNKNOWN -> PlateauStrategy.STABILIZE_BASELINE
        }
    }

    /** Reuses the V3.7A plateau RIR-trend check: latest avg RIR clearly above the older window average. */
    private fun plateauRirImproving(window: List<SessionEval>): Boolean {
        val olderRir = window.drop(1).take(2).map { it.avgRir }.average()
        return window.first().avgRir > olderRir + RIR_TREND_DELTA
    }

    private fun plateauStrategyMessage(
        exercise: ExerciseEntity,
        goal: WorkoutGoalProfile,
        strategy: PlateauStrategy,
        latest: SessionEval,
    ): String {
        val w = formatWeight(latest.mainWeight)
        val loadNote = if (latest.isBodyweight) "" else " at ${w}kg"
        // Goal-specific phrasings first (V3.9-G); HYPERTROPHY keeps the V3.8-H texts verbatim.
        when (goal) {
            WorkoutGoalProfile.STRENGTH -> when (strategy) {
                PlateauStrategy.INCREASE_EFFORT -> return pick(exercise, listOf(
                    "You're stalled$loadNote but the sets still look comfortable — keep every rep crisp and repeatable, then add weight only after you own that quality.",
                    "Strength has flattened but the sets still have reserve — keep the prescribed reps controlled and stable; don't chase failure to force progress.",
                ))
                PlateauStrategy.ADD_REP -> return pick(exercise, listOf(
                    "Stalled$loadNote but it's getting easier — add one clean rep at a time; when every set hits the top of the range, step the load up.",
                    "The weight is moving better — double progression: chase a rep per set, then go heavier once the range is owned.",
                ))
                PlateauStrategy.CHANGE_REP_RANGE -> return pick(exercise, listOf(
                    "You've ground the same heavy zone for a while — run 2-3 weeks in a slightly different rep range, then come back to the heavy work fresh.",
                    "A deep strength stall usually wants a block change — shift the rep range for a couple of weeks, then retest the heavy sets.",
                ))
                PlateauStrategy.STABILIZE_BASELINE -> return "Strength moves slower than it feels — hold this load, keep the reps crisp, and give it another week or two before changing anything."
                else -> Unit
            }
            WorkoutGoalProfile.POWER -> when (strategy) {
                PlateauStrategy.CHANGE_REP_RANGE -> return pick(exercise, listOf(
                    "The logged work has repeated — change the stimulus for a block: slightly lighter with fast intent, or slightly heavier for low, controlled reps, then reassess.",
                    "The same power task has repeated for a while — rotate the load/speed-intent emphasis, but do not call it an output change without a measured output.",
                ))
                PlateauStrategy.STABILIZE_BASELINE -> return "Power lives on quality, not grinding — keep every rep fast, rest fully between sets, and end a set the moment speed drops."
                else -> Unit
            }
            WorkoutGoalProfile.ENDURANCE -> when (strategy) {
                PlateauStrategy.ADD_REP -> return pick(exercise, listOf(
                    "Progress this with more work, not more load — add a rep or two per set, and only go heavier once the completed work is repeatable.",
                    "For endurance, add a small amount of completed work before any load change; rest time is not measured here.",
                ))
                PlateauStrategy.ADD_SET -> return "Add one more set to build your work capacity — for endurance, total work matters more than load."
                PlateauStrategy.CHANGE_REP_RANGE -> return "Change the challenge for a couple of weeks — a different rep range or tempo — then come back and rebuild the volume."
                else -> Unit
            }
            WorkoutGoalProfile.GENERAL_FITNESS, WorkoutGoalProfile.FAT_LOSS_SUPPORT -> when (strategy) {
                PlateauStrategy.ADD_REP -> return "It's getting easier — add a rep or two per set when you feel fresh. No rush on the load; consistency is doing the work."
                PlateauStrategy.STABILIZE_BASELINE -> return "You're holding steady — that's a win for consistency. Keep showing up, and nudge the reps up whenever it feels comfortable."
                else -> Unit
            }
            else -> Unit
        }
        return when (strategy) {
            PlateauStrategy.INCREASE_EFFORT -> pick(exercise, listOf(
                "You're stalled$loadNote but leaving reps in reserve — take these sets closer to failure (aim RIR 1-2) before touching the load.",
                "Progress has flattened and effort's a bit light — push a rep or two closer to failure each set to get the stimulus moving again.",
            ))
            PlateauStrategy.ADD_REP -> pick(exercise, listOf(
                "You're stalled$loadNote but it's getting easier — chase one more rep per set (double progression); once every set hits the top of the range, add load.",
                "Effort's improving here — keep the weight and add a rep at a time; when all sets reach the top of the range, go heavier.",
            ))
            PlateauStrategy.ADD_SET -> pick(exercise, listOf(
                "You're stalled$loadNote and your effort's fine — add one more working set at this load to bump the stimulus (keep RIR 2-3 on it).",
                "Progress has flattened with solid effort — one extra working set is the simplest next step before changing anything else.",
            ))
            PlateauStrategy.DELOAD -> pick(exercise, listOf(
                "This looks like accumulated fatigue, not a weight problem — take a deload week (about half the sets, keep the weight, RIR 4-5), then come back and retest.",
                "Stalled and running tired — deload for a week (drop the volume, hold the load, stay well shy of failure) so the fatigue clears, then push again.",
            ))
            PlateauStrategy.CHANGE_REP_RANGE -> pick(exercise, listOf(
                "You've been stuck in this rep range a while — rotate to a different range for 2-3 weeks (same exercise), then come back to break the plateau.",
                "Same stimulus for several sessions now — try a new rep range for a couple of weeks to give the muscle a fresh challenge, then return.",
            ))
            PlateauStrategy.STABILIZE_BASELINE, PlateauStrategy.NONE ->
                "Hold here and keep logging — not enough of a pattern yet to change anything."
            PlateauStrategy.PROGRESS_ONE_METRIC ->
                "Progress one measured metric in a small step while every other dimension stays fixed."
        }
    }

    // ---- V3.6C Coach Tone Layer ---------------------------------------------------------
    //
    // Deterministic phrasing variety. Different scenarios already read differently; what this solves is
    // several exercises in one session sharing the *same* sentence for the same action (robotic
    // repetition). Each scenario below offers a few coach-toned variants, and we pick one by a stable
    // fold of the exercise name — so the SAME exercise always reads the same way (no refresh-to-refresh
    // churn) while DIFFERENT exercises vary. No randomness, no clock, no I/O. Safety/verify scenarios are
    // intentionally single-string (keeping their protected substrings) — variety there buys nothing and
    // would risk the wording contracts the tests depend on.

    /** Stable, non-negative variant index folded from the exercise name. Same name → same index. */
    private fun toneIndex(exercise: ExerciseEntity, count: Int): Int {
        if (count <= 1) return 0
        var h = 0
        for (ch in exercise.name) h = (h * 31 + ch.code) and 0x7fffffff
        return h % count
    }

    private fun <T> pick(exercise: ExerciseEntity, variants: List<T>): T =
        variants[toneIndex(exercise, variants.size)]

    private fun firstSessionDetail(exercise: ExerciseEntity): String {
        val lo = exercise.targetMinReps
        val hi = exercise.targetMaxReps
        // Non-rep targets store 0/0 sentinels — never tell the user to "start around 0-0 reps".
        if (hi <= 0) {
            return "No history yet — log your first session and I'll track your progress from it."
        }
        return pick(exercise, listOf(
            "No history yet — start around $lo-$hi reps and keep 1-3 in reserve. We'll build from here.",
            "Fresh start: pick a weight you can take to $lo-$hi clean reps with a couple left in the tank.",
            "First time logging this — aim for $lo-$hi controlled reps, stopping 1-3 shy of failure.",
        ))
    }

    private fun buildRepsToMin(exercise: ExerciseEntity, m: Metrics): String = pick(exercise, listOf(
        "Not every set hit ${m.minRep} reps yet — stay at this load and build them up first.",
        "Close. Get all ${m.targetSets} sets to ${m.minRep}+ reps before the load goes up.",
        "Hold here and bring every set up to ${m.minRep} reps before adding weight.",
    ))

    private fun buildRepsToMax(exercise: ExerciseEntity, m: Metrics): String = pick(exercise, listOf(
        "Keep adding reps toward ${m.maxRep} on every set before adding load.",
        "Chase ${m.maxRep} reps across all sets first — then we load it.",
        "A little more rep volume: aim for ${m.maxRep} on every set before going heavier.",
    ))

    // ---- V3.7A Long-term trend intelligence (per-exercise) -----------------------------
    //
    // A read-only overlay on the V3.6C decision. Using the FULL per-exercise history already available
    // (no new data, no schema, no save/UI change), it widens the comparison window to recognize three
    // long-term patterns the single-session rules cannot name — see [CoachInsight]. It NEVER changes the
    // action or the suggested weight; it only refines wording, the title, and the internal insight tag.
    // Below [MIN_TREND_SESSIONS] comparable sessions it is inert, so sparse-history users keep exact
    // V3.6C behavior. RIR is used here only as a trend *modifier* (plateau easier-vs-grinding), never as
    // the sole truth, and it can never flip a safety-gated action.

    private fun applyTrendInsight(
        exercise: ExerciseEntity,
        recentSets: List<LoggedSetEntity>,
        core: ProgressionSuggestion,
    ): ProgressionSuggestion {
        // Skill/form work has no measured technique-quality signal. Do not reinterpret repeated reps/load
        // as a plateau, regression, or new baseline; the dedicated form-first rule remains authoritative.
        if (core.progressionType == ProgressionType.SKILL_FORM) return core
        val window = comparableWindow(exercise, recentSets)
        if (window.size < MIN_TREND_SESSIONS) return core
        val latest = window.first()
        val m = metrics(exercise, latest)
        return when (classifyTrend(window, m, core)) {
            CoachInsight.NEW_BASELINE -> core.copy(
                insight = CoachInsight.NEW_BASELINE,
                titleOverride = "New baseline",
                reason = newBaselineReason(exercise, latest, m),
            )
            CoachInsight.PLATEAU -> core.copy(
                insight = CoachInsight.PLATEAU,
                reason = plateauReason(exercise, window, latest),
            )
            CoachInsight.BAD_DAY_VERIFY -> core.copy(insight = CoachInsight.BAD_DAY_VERIFY)
            else -> core
        }
    }

    /** Latest-first comparable session evals (same bodyweight/loaded class, score>0), capped at the window. */
    private fun comparableWindow(exercise: ExerciseEntity, recentSets: List<LoggedSetEntity>): List<SessionEval> {
        val evals = groupSessionsNewestFirst(recentSets)
            .map { s -> s.filter { it.reps > 0 } }
            .mapNotNull { evalSession(it) }
        val latest = evals.firstOrNull() ?: return emptyList()
        return evals.filter { it.isBodyweight == latest.isBodyweight && it.score > 0.0 }.take(TREND_WINDOW)
    }

    private fun classifyTrend(window: List<SessionEval>, m: Metrics, core: ProgressionSuggestion): CoachInsight {
        val latest = window.first()

        // New baseline / regression: only ever refines a KEEP (it can never create an increase or a
        // decrease). The recent two sessions must be settled at a level clearly below an older peak, and
        // the lower load must be *handled* (reps at/above min, not collapsing). A clean top-range success
        // is excluded so a user who has adapted to the lower level still gets a normal increase upstream.
        if (core.action == Action.KEEP_WEIGHT && window.size >= 3) {
            val recent = window.take(2)
            val peak = window.drop(2).maxByOrNull { it.score }
            if (peak != null) {
                val recentTop = maxOf(recent[0].score, recent[1].score)
                val band = STABLE_BAND * maxOf(recent[0].score, recent[1].score, 1.0)
                val settled = abs(recent[0].score - recent[1].score) <= band
                val regressed = recentTop <= peak.score * (1.0 - REGRESSION_DROP)
                val handled = m.allSetsAtLeastMin && !m.tooHeavy
                val cleanIncrease = m.allSetsAtTopRange && m.avgRir >= RIR_INCREASE_GATE && !m.mostlyFailed
                if (settled && regressed && handled && !cleanIncrease) return CoachInsight.NEW_BASELINE
            }
        }

        // Plateau: same working weight and flat reps across the recent window (no sharp drop — those are
        // handled upstream). Only refines a hold or a reps/difficulty cue, never an increase/decrease.
        if ((core.action == Action.KEEP_WEIGHT || core.action == Action.INCREASE_REPS_OR_DIFFICULTY) &&
            window.size >= 3
        ) {
            val k = window.take(3)
            val sameWeight = k.all { abs(it.mainWeight - latest.mainWeight) < WEIGHT_EPSILON }
            val avgReps = k.map { it.avgReps }
            val repRange = (avgReps.maxOrNull() ?: 0.0) - (avgReps.minOrNull() ?: 0.0)
            if (sameWeight && repRange <= PLATEAU_REP_BAND) return CoachInsight.PLATEAU
        }

        // Otherwise tag an existing bad-day hold so callers can tell it apart from a plain hold.
        if (core.titleOverride == "Hold & verify") return CoachInsight.BAD_DAY_VERIFY
        return CoachInsight.NONE
    }

    private fun newBaselineReason(exercise: ExerciseEntity, latest: SessionEval, m: Metrics): String {
        val base = if (latest.isBodyweight) {
            pick(exercise, listOf(
                "Your rep level has settled here for a few sessions — let's treat this as your baseline now and rebuild from it, not chase the old high.",
                "This looks like your current baseline, not a one-off dip. Groove it, then start adding reps again.",
            ))
        } else {
            val w = formatWeight(latest.mainWeight)
            pick(exercise, listOf(
                "Your working weight has settled around ${w}kg over recent sessions — let's treat that as the new baseline and rebuild from here, not chase the old peak.",
                "${w}kg looks like your current baseline now, not a one-off dip. Stabilize here, then build back up.",
            ))
        }
        val tail = when {
            m.eval.workingSets.any { it.rir <= 0 } -> " Your last set hit RIR 0, so groove this load before adding weight."
            m.avgRir < RIR_INCREASE_GATE -> " Keep a rep or two in reserve here before adding weight."
            else -> ""
        }
        return base + tail
    }

    private fun plateauReason(exercise: ExerciseEntity, window: List<SessionEval>, latest: SessionEval): String {
        val olderRir = window.drop(1).take(2).map { it.avgRir }.average()
        val improving = latest.avgRir > olderRir + RIR_TREND_DELTA
        return if (improving) {
            pick(exercise, listOf(
                "Same weight and reps for a few sessions, but your RIR is improving — you're adapting. Push for an extra rep, then add load.",
                "You've held here a while and it's getting easier (RIR trending up). Add a rep or an extra set before going heavier.",
            ))
        } else {
            pick(exercise, listOf(
                "Stalled at this weight for a few sessions and the sets are near failure — take a lighter week (deload), then rebuild.",
                "No progress here for a while and the reps are grinding. Try a deload or an extra set rather than forcing more load.",
            ))
        }
    }

    // ---- Trend & outlier detection ------------------------------------------------------

    /**
     * Compares the latest session score against the strongest of the previous two comparable
     * sessions. Returns a suggestion when the latest session is an outlier (≥[OUTLIER_DROP] below
     * baseline); null means "no outlier — apply the normal rules".
     */
    private fun outlierSuggestion(
        exercise: ExerciseEntity,
        type: ProgressionType,
        m: Metrics,
        previous: SessionEval?,
        previous2: SessionEval?,
        readiness: Readiness?,
    ): ProgressionSuggestion? {
        if (!m.enoughSets) return null
        val latest = m.eval
        if (latest.score <= 0.0) return null
        // Only compare like with like: bodyweight sessions against bodyweight, loaded against loaded.
        val comparable = listOfNotNull(previous, previous2)
            .filter { it.isBodyweight == latest.isBodyweight && it.score > 0.0 }
        val baseline = comparable.maxByOrNull { it.score } ?: return null
        val drop = 1.0 - latest.score / baseline.score
        if (drop < OUTLIER_DROP) return null

        val dropPct = (drop * 100).roundToInt()
        val loadChanged = !latest.isBodyweight &&
            abs(latest.mainWeight - baseline.mainWeight) > 0.10 * baseline.mainWeight
        val loadNote = if (loadChanged) {
            " The load also changed (${formatWeight(baseline.mainWeight)}kg → ${formatWeight(latest.mainWeight)}kg)."
        } else {
            ""
        }

        if (type == ProgressionType.BODYWEIGHT || type == ProgressionType.SKILL_FORM) {
            return ProgressionSuggestion(
                action = Action.KEEP_WEIGHT,
                suggestedWeight = null,
                targetText = repsTarget(exercise),
                reason = "About $dropPct% below your recent reps here — could be an off day or a tougher variation. " +
                    "Nothing to change at this load; just rebuild the reps and check again next session." +
                    readinessClause(readiness, dropContext = true),
                progressionType = type,
                titleOverride = "Hold & verify",
            )
        }

        val repeated = previous != null && previous2 != null &&
            previous.isBodyweight == previous2.isBodyweight && previous2.score > 0.0 &&
            previous.score <= REPEAT_COLLAPSE_FACTOR * previous2.score &&
            latest.score <= REPEAT_COLLAPSE_FACTOR * previous2.score
        val extremeAtFailure = drop >= EXTREME_DROP && latest.avgRir <= RIR_AT_FAILURE

        if (repeated || extremeAtFailure) {
            val why = if (repeated) {
                "Two sessions running now below your baseline"
            } else {
                "About $dropPct% below your recent baseline with nothing left in the tank (RIR 0)"
            }
            return ProgressionSuggestion(
                action = Action.DECREASE_WEIGHT,
                suggestedWeight = reducedWeight(m.currentWeight),
                targetText = repsTarget(exercise),
                reason = "$why — let's ease the load down and rebuild toward ${m.minRep}-${m.maxRep} clean reps.$loadNote" +
                    readinessClause(readiness, dropContext = true),
                progressionType = type,
            )
        }

        return ProgressionSuggestion(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = m.currentWeight.takeIf { it > 0.0 },
            targetText = repsTarget(exercise),
            reason = "This came in about $dropPct% below your recent baseline.$loadNote " +
                "Could be an off day, a different machine, or a logging slip — hold the load, " +
                "check again next session, and only drop it if it repeats." +
                readinessClause(readiness, dropContext = true),
            progressionType = type,
            titleOverride = "Hold & verify",
        )
    }

    // ---- Per-type rules ---------------------------------------------------------------

    private fun compoundRule(
        exercise: ExerciseEntity,
        m: Metrics,
        weightedBodyweight: Boolean,
        profile: ProgressionProfile?,
    ): ProgressionSuggestion {
        val type = ProgressionType.COMPOUND_LOAD
        if (!m.enoughSets) {
            return keep(type, m, notEnoughSetsReason(m))
        }
        if (m.tooHeavy) {
            return ProgressionSuggestion(
                action = Action.DECREASE_WEIGHT,
                suggestedWeight = reducedWeight(m.currentWeight),
                targetText = repsTarget(exercise),
                reason = buildString {
                    append("Reps came in well under the ${m.minRep}-${m.maxRep} target — ease the load down and rebuild with clean form.")
                    if (m.severelyTooHeavy && m.avgRir >= 2.0) {
                        append(" You still logged reps in reserve, so double-check the weight/machine entry first — fix the log if it's just a typo.")
                    }
                },
                progressionType = type,
            )
        }
        if (m.allSetsAtLeastMin && (m.mostlyFailed || m.avgRir < RIR_INCREASE_GATE)) {
            // V3.9-G: count the failed sets so the wording never says "one set" when several hit RIR 0.
            val failedSets = m.eval.workingSets.count { it.rir <= 0 }
            val reason = buildString {
                append(if (m.allSetsAtTopRange) "Nice — you hit the top of the range" else "You hit the rep target")
                append(
                    when {
                        failedSets > 1 -> ", but $failedSets sets reached RIR 0"
                        failedSets == 1 -> ", but one set reached RIR 0"
                        else -> ", but with little in reserve"
                    },
                )
                append(". Repeat this load and keep 1-2 reps in reserve before adding weight.")
            }
            return keep(type, m, reason)
        }
        if (m.allSetsAtLeastMin) {
            val baseInc = resolveIncrement(profile, increment(exercise, lowerBodyAllowed = !weightedBodyweight))
            val strongOvershoot = !weightedBodyweight && m.allSetsAtTopRange &&
                m.eval.avgReps >= m.maxRep + 1.0 && m.avgRir >= 2.0
            val inc = if (strongOvershoot) baseInc * 2 else baseInc
            val next = roundToNearestIncrement(m.currentWeight + inc)
            val reason = buildString {
                if (strongOvershoot) {
                    append(pick(exercise, listOf(
                        "Every set cleared the top of the range with room to spare — take a bigger ${formatWeight(inc)}kg jump and settle back into ${m.minRep}-${m.maxRep} reps.",
                        "You comfortably beat the top of the range — go up ${formatWeight(inc)}kg and rebuild toward ${m.minRep}-${m.maxRep} reps.",
                    )))
                } else {
                    append(pick(exercise, listOf(
                        "All ${m.targetSets} working sets cleared ${m.minRep}+ reps — add ${formatWeight(inc)}kg and settle in.",
                        "Every set hit at least ${m.minRep} reps. Add ${formatWeight(inc)}kg and rebuild from there.",
                        "That's your green light — bump the load ${formatWeight(inc)}kg.",
                    )))
                    append(if (m.avgRir >= 2.0) " You kept reps in reserve, so it's a confident step up." else " Add it only if your form stays strict.")
                }
                if (weightedBodyweight) append(" This is added-weight ${exercise.name.lowercase()} — progress the load slowly.")
            }
            return ProgressionSuggestion(
                action = Action.INCREASE_WEIGHT,
                suggestedWeight = next,
                targetText = repsTarget(exercise),
                reason = reason,
                progressionType = type,
            )
        }
        return keep(type, m, buildRepsToMin(exercise, m))
    }

    private fun isolationRule(
        exercise: ExerciseEntity,
        m: Metrics,
        previous: SessionEval?,
        profile: ProgressionProfile?,
    ): ProgressionSuggestion {
        val type = ProgressionType.ISOLATION_SLOW
        val inc = resolveIncrement(profile, DEFAULT_INCREMENT)
        if (!m.enoughSets) {
            return keep(type, m, notEnoughSetsReason(m))
        }
        if (m.severelyTooHeavy) {
            return ProgressionSuggestion(
                action = Action.DECREASE_WEIGHT,
                suggestedWeight = reducedWeight(m.currentWeight),
                targetText = repsTarget(exercise),
                reason = buildString {
                    append("Reps dropped well below target — ease the load down and rebuild.")
                    if (m.avgRir >= 2.0) {
                        append(" You still logged reps in reserve, so check the weight/unit/machine was entered correctly first.")
                    }
                },
                progressionType = type,
            )
        }
        if (m.tooHeavy) {
            return keep(
                type, m,
                buildString {
                    append("Reps are a touch under target — hold this load and build reps before changing anything.")
                    if (m.avgRir <= RIR_AT_FAILURE) {
                        append(" Every set was near failure under target, so this load may simply be heavy for the ${m.minRep}-${m.maxRep} range.")
                    }
                },
            )
        }
        if (m.allSetsAtTopRange && !m.mostlyFailed && m.avgRir >= RIR_INCREASE_GATE) {
            val prevTop = previous != null && isTopRangeSession(exercise, previous)
            val next = roundToNearestIncrement(m.currentWeight + inc)
            return when {
                previous == null -> ProgressionSuggestion(
                    action = Action.INCREASE_WEIGHT,
                    suggestedWeight = next.takeIf { m.currentWeight > 0.0 },
                    targetText = repsTarget(exercise),
                    reason = "Top of the range this session — if the machine/cable stack allows a small ${formatWeight(inc)}kg step and your form stays strict, take it.",
                    progressionType = type,
                )
                prevTop -> ProgressionSuggestion(
                    action = Action.INCREASE_WEIGHT,
                    suggestedWeight = next.takeIf { m.currentWeight > 0.0 },
                    targetText = repsTarget(exercise),
                    reason = "Two sessions at the top of the range — add a small ${formatWeight(inc)}kg if the stack allows and keep your form strict.",
                    progressionType = type,
                )
                else -> keep(
                    type, m,
                    "Top of the range this time, but last session wasn't — repeat one more strong session up here before adding load.",
                )
            }
        }
        if (m.allSetsAtTopRange) {
            return keep(
                type, m,
                "Top reps, but you finished right at your limit — hold this weight one more session with a rep in reserve, then add.",
            )
        }
        if (m.allSetsAtLeastMin) {
            return keep(type, m, buildRepsToMax(exercise, m))
        }
        return keep(type, m, buildRepsToMin(exercise, m))
    }

    private fun bodyweightRule(exercise: ExerciseEntity, m: Metrics): ProgressionSuggestion {
        val type = ProgressionType.BODYWEIGHT
        if (!m.enoughSets) {
            return ProgressionSuggestion(
                action = Action.KEEP_WEIGHT,
                suggestedWeight = null,
                targetText = repsTarget(exercise),
                reason = "Log all ${m.targetSets} sets so I can track your bodyweight progress.",
                progressionType = type,
            )
        }
        if (!m.allSetsAtLeastMin) {
            return ProgressionSuggestion(
                action = Action.KEEP_WEIGHT,
                suggestedWeight = null,
                targetText = repsTarget(exercise),
                reason = "Build reps until every set reaches ${m.minRep} — bodyweight moves up on reps first.",
                progressionType = type,
            )
        }
        if (!m.allSetsAtTopRange) {
            return ProgressionSuggestion(
                action = Action.INCREASE_REPS_OR_DIFFICULTY,
                suggestedWeight = null,
                targetText = repsTarget(exercise),
                reason = buildRepsToMax(exercise, m),
                progressionType = type,
            )
        }
        val reason = if (m.mostlyFailed || m.avgRir < RIR_INCREASE_GATE) {
            "Top reps, but little in reserve — hold here, then progress with a slower tempo or a harder variation."
        } else {
            "All sets at ${m.maxRep}+ — progress with more reps, a slower tempo, or a harder variation; add weight only once reps are comfortably above range."
        }
        return ProgressionSuggestion(
            action = Action.INCREASE_REPS_OR_DIFFICULTY,
            suggestedWeight = null,
            targetText = repsTarget(exercise),
            reason = reason,
            progressionType = type,
        )
    }

    /**
     * V3.5 skill/control movements (catalog SKILL_FORM only — Pallof press, planks, L-sit, etc.).
     * The core can identify a possible rep/hold/variation step, but final goal safety keeps it
     * advisory because logged reps/load/RIR cannot verify control. This is why a "Pallof press" is
     * not treated as a compound press despite the name.
     */
    private fun skillRule(exercise: ExerciseEntity, m: Metrics): ProgressionSuggestion {
        val type = ProgressionType.SKILL_FORM
        if (!m.enoughSets) return keep(type, m, notEnoughSetsReason(m))
        if (!m.allSetsAtLeastMin) {
            return keep(
                type, m,
                "Build clean reps to ${m.minRep} first — this is a control/skill move, so form comes before load.",
            )
        }
        return ProgressionSuggestion(
            action = Action.INCREASE_REPS_OR_DIFFICULTY,
            suggestedWeight = m.currentWeight.takeIf { it > 0.0 },
            targetText = repsTarget(exercise),
            reason = "Progress with more reps, longer holds, or a harder variation — add load only in small steps once your control is solid.",
            progressionType = type,
        )
    }

    /** Effective load step: the catalog increment when it is larger, never weaker than the smart step. */
    private fun resolveIncrement(profile: ProgressionProfile?, nameBased: Double): Double =
        maxOf(profile?.incrementKg ?: 0.0, nameBased)

    // ---- Session evaluation (main working weight, ramp/back-off classification) --------

    /**
     * One session reduced to its working sets. The main working weight is the most frequent load
     * (ties → heaviest); lighter sets before the first working set are ramp-up sets and lighter
     * sets after the last working set are back-off/drop sets — both excluded from the decision.
     */
    private data class SessionEval(
        val mainWeight: Double,
        val workingSets: List<LoggedSetEntity>,
        val rampSetCount: Int,
        val backoffSetCount: Int,
    ) {
        val avgReps: Double = workingSets.map { it.reps }.average()
        val avgRir: Double = workingSets.map { it.rir }.average()
        val isBodyweight: Boolean = mainWeight <= 0.0

        /** Comparable per-set performance proxy: load × avg reps (avg reps alone at bodyweight). */
        val score: Double = if (isBodyweight) avgReps else mainWeight * avgReps
    }

    private fun evalSession(validSets: List<LoggedSetEntity>): SessionEval? {
        if (validSets.isEmpty()) return null
        val main = modeWeight(validSets.map { it.weight })
        val isMain = { set: LoggedSetEntity -> abs(set.weight - main) < WEIGHT_EPSILON }
        val working = validSets.filter(isMain)
        val firstMain = validSets.indexOfFirst(isMain)
        val lastMain = validSets.indexOfLast(isMain)
        var ramp = 0
        var backoff = 0
        validSets.forEachIndexed { index, set ->
            if (!isMain(set) && set.weight < main) {
                if (index < firstMain) ramp++ else if (index > lastMain) backoff++
            }
        }
        return SessionEval(
            mainWeight = main,
            workingSets = working,
            rampSetCount = ramp,
            backoffSetCount = backoff,
        )
    }

    // ---- Metrics & helpers ------------------------------------------------------------

    private data class Metrics(
        val targetSets: Int,
        val minRep: Int,
        val maxRep: Int,
        val eval: SessionEval,
        val enoughSets: Boolean,
        val allSetsAtLeastMin: Boolean,
        val allSetsAtTopRange: Boolean,
        val tooHeavy: Boolean,
        val severelyTooHeavy: Boolean,
        val avgRir: Double,
        val mostlyFailed: Boolean,
        val currentWeight: Double,
    )

    private fun metrics(exercise: ExerciseEntity, eval: SessionEval): Metrics {
        val targetSets = exercise.targetSets.coerceAtLeast(1)
        val minRep = exercise.targetMinReps.coerceAtLeast(1)
        val maxRep = exercise.targetMaxReps.coerceAtLeast(minRep)
        val working = eval.workingSets
        val enough = working.size >= targetSets
        val allMin = enough && working.all { it.reps >= minRep }
        val allTop = enough && working.all { it.reps >= maxRep }
        // Avg-based thresholds are robust to extra working sets beyond the target count.
        val tooHeavy = eval.avgReps < minRep * 0.70
        val severe = eval.avgReps < minRep * 0.50
        val failed = working.count { it.rir <= 0 }
        val mostlyFailed = working.isNotEmpty() && failed * 2 >= working.size
        return Metrics(
            targetSets = targetSets,
            minRep = minRep,
            maxRep = maxRep,
            eval = eval,
            enoughSets = enough,
            allSetsAtLeastMin = allMin,
            allSetsAtTopRange = allTop,
            tooHeavy = tooHeavy,
            severelyTooHeavy = severe,
            avgRir = eval.avgRir,
            mostlyFailed = mostlyFailed,
            currentWeight = eval.mainWeight,
        )
    }

    private fun keep(type: ProgressionType, m: Metrics, reason: String): ProgressionSuggestion =
        ProgressionSuggestion(
            action = Action.KEEP_WEIGHT,
            suggestedWeight = m.currentWeight.takeIf { it > 0.0 },
            targetText = "${m.minRep}-${m.maxRep} reps x ${m.targetSets}",
            reason = reason,
            progressionType = type,
        )

    private fun notEnoughSetsReason(m: Metrics): String = buildString {
        append("Only ${m.eval.workingSets.size} of ${m.targetSets} working sets")
        if (m.currentWeight > 0.0) append(" at ${formatWeight(m.currentWeight)}kg")
        append(" — settle on one load and finish all your sets so I can read the progression.")
        if (m.eval.rampSetCount > 0) append(" Earlier lighter sets count as ramp-up.")
        if (m.eval.backoffSetCount > 0) append(" Later lighter sets count as back-off work.")
    }

    private fun isTopRangeSession(exercise: ExerciseEntity, eval: SessionEval): Boolean {
        val targetSets = exercise.targetSets.coerceAtLeast(1)
        val maxRep = exercise.targetMaxReps.coerceAtLeast(exercise.targetMinReps.coerceAtLeast(1))
        return eval.workingSets.size >= targetSets && eval.workingSets.all { it.reps >= maxRep }
    }

    /** Most relevant working weight: the most common load; ties resolve to the heaviest (top set). */
    private fun modeWeight(weights: List<Double>): Double {
        if (weights.isEmpty()) return 0.0
        val counts = weights.groupingBy { it }.eachCount()
        val maxCount = counts.values.maxOrNull() ?: return 0.0
        return counts.filterValues { it == maxCount }.keys.maxOrNull() ?: 0.0
    }

    /** Group sets into sessions, preserving the newest-first ordering of [recentSets]. */
    private fun groupSessionsNewestFirst(recentSets: List<LoggedSetEntity>): List<List<LoggedSetEntity>> {
        if (recentSets.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<Long, MutableList<LoggedSetEntity>>()
        for (set in recentSets) {
            grouped.getOrPut(set.sessionId) { mutableListOf() }.add(set)
        }
        return grouped.values.map { it.toList() }
    }

    private fun increment(exercise: ExerciseEntity, lowerBodyAllowed: Boolean): Double {
        val name = normalizeName(exercise.name)
        val heavyLowerBody = LOWER_BODY_KEYWORDS.any { name.contains(it) }
        return if (lowerBodyAllowed && heavyLowerBody) 5.0 else DEFAULT_INCREMENT
    }

    private fun repsTarget(exercise: ExerciseEntity): String =
        "${exercise.targetMinReps}-${exercise.targetMaxReps} reps x ${exercise.targetSets}"

    /**
     * Exposed for tests/diagnostics: the raw name-based progression type, before the
     * weighted-bodyweight remap that [progression] applies when added load > 0.
     */
    fun progressionTypeForName(name: String): ProgressionType = inferTypeFromName(name)

    /**
     * Temporary name-based progression-type inference. Order matters: bodyweight first (so a 0kg
     * pull-up never gets a +2.5kg nudge), then compound (so "Romanian deadlift or leg curl" reads
     * as a hinge), then isolation; unknown names default to the conservative COMPOUND_LOAD path.
     *
     * The name is [normalizeName]d first, so "Pull-up", "pull up", "pull_up", "PULL UP" and
     * "lateral  raise" (irregular spacing) all match the same keywords.
     */
    private fun inferTypeFromName(name: String): ProgressionType {
        val n = normalizeName(name)
        if (BODYWEIGHT_KEYWORDS.any { n.contains(it) }) return ProgressionType.BODYWEIGHT
        if (COMPOUND_KEYWORDS.any { n.contains(it) }) return ProgressionType.COMPOUND_LOAD
        if (ISOLATION_KEYWORDS.any { n.contains(it) }) return ProgressionType.ISOLATION_SLOW
        return ProgressionType.COMPOUND_LOAD
    }

    /**
     * Normalize an exercise name for keyword matching: lowercase, convert hyphens, underscores,
     * slashes and unicode dashes (en/em/etc.) to spaces, then collapse whitespace runs and trim.
     * Keyword tables are written in this normalized (space-separated, lowercase) form.
     */
    private fun normalizeName(name: String): String =
        name.lowercase()
            .replace(NAME_SEPARATORS, " ")
            .replace(NAME_WHITESPACE, " ")
            .trim()

    private fun ProgressionSuggestion.toOverloadSuggestion(): OverloadSuggestion {
        val title = titleOverride ?: when (action) {
            Action.INCREASE_WEIGHT -> "Increase weight"
            Action.KEEP_WEIGHT -> "Hold weight"
            Action.DECREASE_WEIGHT -> "Reduce & recover"
            Action.INCREASE_REPS_OR_DIFFICULTY -> "Build reps / difficulty"
        }
        return OverloadSuggestion(title = title, detail = reason, suggestedWeight = suggestedWeight)
    }

    // ---- Unchanged helpers (used across the app) --------------------------------------

    fun trend(
        currentSets: List<LoggedSetEntity>,
        previousSets: List<LoggedSetEntity>,
    ): SessionTrend {
        val currentVolume = currentSets.sumOf { it.weight * it.reps }
        val previousVolume = previousSets.sumOf { it.weight * it.reps }
        val delta = currentVolume - previousVolume
        val label = when {
            previousSets.isEmpty() -> "First logged session"
            delta > 0.0 -> "+${formatWeight(delta)} volume"
            delta < 0.0 -> "${formatWeight(delta)} volume"
            else -> "Volume matched"
        }
        return SessionTrend(currentVolume, previousVolume, delta, label)
    }

    fun formatWeight(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    private fun roundToNearestIncrement(value: Double, increment: Double = DEFAULT_INCREMENT): Double {
        return (value / increment).roundToInt() * increment
    }

    /**
     * "Drop about 10%" mapped to a load that actually exists: floor to the next [DEFAULT_INCREMENT]
     * step strictly below the current weight. Plain rounding could land back on the same weight
     * (7.5kg × 0.9 = 6.75 → rounds to 7.5kg again); flooring gives 5kg.
     */
    private fun reducedWeight(current: Double): Double? {
        if (current <= 0.0) return null
        var stepped = floor(current * 0.90 / DEFAULT_INCREMENT) * DEFAULT_INCREMENT
        if (stepped >= current) stepped = current - DEFAULT_INCREMENT
        return stepped.takeIf { it > 0.0 }
    }

    // ---- Name keyword tables (temporary, until a real exercise DB lands) ---------------
    // Written in normalized form (lowercase, space-separated) to match normalizeName() output.

    // Hyphen/underscore variants ("pull-up", "pull_up") are normalized to the space form before
    // matching, so only the space and single-word spellings need to be listed here.
    private val NAME_SEPARATORS = Regex("[-_/\\u2010-\\u2015]") // hyphen, underscore, slash, unicode dashes
    private val NAME_WHITESPACE = Regex("\\s+")

    private val BODYWEIGHT_KEYWORDS = listOf(
        "pull up", "pullup",
        "chin up", "chinup",
        "push up", "pushup",
        "muscle up", "muscleup",
        "dip", "plank", "hanging leg raise", "bodyweight",
    )

    private val COMPOUND_KEYWORDS = listOf(
        "chest press", "machine press", "bench", "squat", "deadlift", "rdl",
        "romanian deadlift", "leg press", "hack squat", "row", "pulldown", "press",
    )

    private val ISOLATION_KEYWORDS = listOf(
        "lateral raise", "rear delt", "fly", "curl", "face pull", "extension",
        "pushdown", "triceps", "biceps", "calf raise", "cable raise", "wrist",
    )

    private val LOWER_BODY_KEYWORDS = listOf(
        "squat", "leg press", "hack squat", "hack", "deadlift", "rdl", "romanian",
    )
}
