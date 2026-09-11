package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.TrackingType

/**
 * MUSCLE-1 — Layer A: raw anatomical muscle exposure for ONE saved session.
 *
 * This layer is **pure counting** (DB-owned facts): canonical exercise identity → catalog
 * primary/secondary muscles → weighted set-equivalents, with skipped/partial/completed rules. It makes
 * NO training-quality judgment — quality/status/color interpretation belongs to [MuscleHeatEngine]
 * (Layer B). See `docs/muscle0_heatmap_contract_audit.md` §3.
 *
 * Everything here is **runtime-only**: never stored in Room, never exported, never shown as a raw score.
 */

/** Whether an exercise hits a muscle as a PRIMARY mover or a SECONDARY/assisting one. */
enum class MuscleRole { PRIMARY, SECONDARY }

/**
 * One exercise's state within the saved session, already joined with its catalog metadata by the caller
 * (see [SessionMuscleHeatUseCase]) so this layer stays a pure function:
 *  - a **skipped** exercise is a snapshot with an empty/invalid [sets] list (planned but never logged);
 *  - a **custom / unmatched** exercise has empty [primaryMuscles] + [secondaryMuscles] (safe fallback);
 *  - [targetSets] <= 0 means "no plan target known" (e.g. logged outside the current plan) — any
 *    completed work then counts as fully completed rather than being mislabelled partial.
 */
data class ExerciseSessionSnapshot(
    val canonicalExerciseKey: String,
    val exerciseName: String,
    val trackingType: TrackingType,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val targetSets: Int,
    val sets: List<LoggedSetEntity>,
)

/**
 * Explainability record: one exercise's contribution to one muscle (or to the unattributed bucket).
 * [avgRir]/[avgReps] are null for non-rep tracking types — a time/distance set stores the `reps = 0` /
 * `rir = 0` sentinels, which must NEVER be read as "0 reps at failure".
 */
data class ExerciseContributor(
    val canonicalExerciseKey: String,
    val exerciseName: String,
    val trackingType: TrackingType,
    val role: MuscleRole,
    val targetSets: Int,
    val completedSets: Int,
    /** completedSets / targetSets, clamped to 0..1 (1.0 when no target is known but work was logged). */
    val completionRatio: Double,
    /** This exercise's weighted set-equivalent contribution to THIS muscle (role weight applied). */
    val setEquivalent: Double,
    val avgRir: Double?,
    val avgReps: Double?,
    val avgWeight: Double?,
    val durationSeconds: Int?,
    val distanceMeters: Double?,
)

/** Raw exposure of one muscle across the whole session. [rawIntensityScore] is exposure ONLY. */
data class MuscleExposure(
    val muscleGroup: MuscleGroup,
    /** Σ set-equivalents from exercises where this muscle is PRIMARY (role weight 1.0). */
    val primarySetEquivalent: Double,
    /** Σ set-equivalents from exercises where this muscle is SECONDARY (role weight 0.5 applied). */
    val secondarySetEquivalent: Double,
    val completedExercises: Int,
    val partialExercises: Int,
    val skippedExercises: Int,
    /** Pure exposure = primarySetEquivalent + secondarySetEquivalent. No quality in here — ever. */
    val rawIntensityScore: Double,
    val exerciseContributors: List<ExerciseContributor>,
)

/**
 * The full Layer-A result: per-muscle exposures (sorted hottest-first) plus the safe bucket for
 * custom/unmatched exercises that map to no catalog muscles (listed so the user still sees them —
 * never dropped, never crashing, never mis-attributed).
 */
data class SessionMuscleExposure(
    val muscles: List<MuscleExposure>,
    val unattributedContributors: List<ExerciseContributor>,
)

/**
 * Pure, deterministic Layer-A calculator. No DAO, no I/O, no clock — the caller supplies snapshots.
 *
 * Exposure model (see the MUSCLE-0 contract):
 *  - a **set-equivalent** is one valid logged working set, for every tracking type (duration/distance
 *    are carried on the contributor for explainability and Layer-B quality, not inflated into extra
 *    volume — simple, conservative, explainable);
 *  - skipped exercise → 0 contribution (still listed as a contributor for explainability);
 *  - partial exercise → contributes exactly the sets that were done (fewer sets = less exposure);
 *  - primary muscles weigh [PRIMARY_ROLE_WEIGHT], secondary [SECONDARY_ROLE_WEIGHT].
 */
object MuscleExposureCalculator {

    const val PRIMARY_ROLE_WEIGHT = 1.0
    const val SECONDARY_ROLE_WEIGHT = 0.5

    fun compute(snapshots: List<ExerciseSessionSnapshot>): SessionMuscleExposure {
        val perMuscle = LinkedHashMap<MuscleGroup, MutableMuscle>()
        val unattributed = mutableListOf<ExerciseContributor>()

        snapshots.forEach { snapshot ->
            val valid = snapshot.sets.filter { isValidSet(snapshot.trackingType, it) }
            val completedSets = valid.size
            val completionRatio = when {
                completedSets == 0 -> 0.0
                snapshot.targetSets <= 0 -> 1.0
                else -> (completedSets.toDouble() / snapshot.targetSets).coerceAtMost(1.0)
            }
            val baseSetEquivalent = completedSets.toDouble()
            val repBased = snapshot.trackingType.isRepBased
            // Averages only where they mean something: rep-based types own reps/RIR; the reps=0 / rir=0
            // sentinels on time/distance sets are never surfaced as training metrics.
            val avgRir = if (repBased && valid.isNotEmpty()) valid.map { s -> s.rir }.average() else null
            val avgReps = if (repBased && valid.isNotEmpty()) valid.map { s -> s.reps }.average() else null
            val avgWeight = if (valid.any { s -> s.weight > 0.0 }) valid.map { s -> s.weight }.average() else null
            val totalDuration = valid.sumOf { s -> s.durationSeconds ?: 0 }.takeIf { sum -> sum > 0 }
            val totalDistance = valid.sumOf { s -> s.distanceMeters ?: 0.0 }.takeIf { sum -> sum > 0.0 }

            fun contributor(role: MuscleRole, weight: Double) = ExerciseContributor(
                canonicalExerciseKey = snapshot.canonicalExerciseKey,
                exerciseName = snapshot.exerciseName,
                trackingType = snapshot.trackingType,
                role = role,
                targetSets = snapshot.targetSets,
                completedSets = completedSets,
                completionRatio = completionRatio,
                setEquivalent = baseSetEquivalent * weight,
                avgRir = avgRir,
                avgReps = avgReps,
                avgWeight = avgWeight,
                durationSeconds = totalDuration,
                distanceMeters = totalDistance,
            )

            if (snapshot.primaryMuscles.isEmpty() && snapshot.secondaryMuscles.isEmpty()) {
                // Custom / unmatched: no catalog anatomy — represent safely, attribute to no region.
                unattributed += contributor(MuscleRole.PRIMARY, PRIMARY_ROLE_WEIGHT)
                return@forEach
            }
            snapshot.primaryMuscles.distinct().forEach { muscle ->
                perMuscle.getOrPut(muscle) { MutableMuscle() }
                    .add(contributor(MuscleRole.PRIMARY, PRIMARY_ROLE_WEIGHT))
            }
            snapshot.secondaryMuscles.distinct()
                .filterNot { it in snapshot.primaryMuscles }
                .forEach { muscle ->
                    perMuscle.getOrPut(muscle) { MutableMuscle() }
                        .add(contributor(MuscleRole.SECONDARY, SECONDARY_ROLE_WEIGHT))
                }
        }

        val muscles = perMuscle.map { (muscle, acc) -> acc.toExposure(muscle) }
            .sortedByDescending { it.rawIntensityScore }
        return SessionMuscleExposure(muscles = muscles, unattributedContributors = unattributed)
    }

    /**
     * A "valid" (counted) set per tracking type. Rep-based types need reps > 0; time/distance types need
     * a positive duration or distance — their `reps = 0` sentinel is never read as a failed rep set.
     */
    private fun isValidSet(type: TrackingType, set: LoggedSetEntity): Boolean = if (type.isRepBased) {
        set.reps > 0
    } else {
        (set.durationSeconds ?: 0) > 0 || (set.distanceMeters ?: 0.0) > 0.0
    }

    private class MutableMuscle {
        var primary = 0.0
        var secondary = 0.0
        var completed = 0
        var partial = 0
        var skipped = 0
        val contributors = mutableListOf<ExerciseContributor>()

        fun add(contributor: ExerciseContributor) {
            contributors += contributor
            if (contributor.role == MuscleRole.PRIMARY) primary += contributor.setEquivalent
            else secondary += contributor.setEquivalent
            when {
                contributor.completedSets == 0 -> skipped++
                contributor.completionRatio >= 1.0 -> completed++
                else -> partial++
            }
        }

        fun toExposure(muscle: MuscleGroup) = MuscleExposure(
            muscleGroup = muscle,
            primarySetEquivalent = primary,
            secondarySetEquivalent = secondary,
            completedExercises = completed,
            partialExercises = partial,
            skippedExercises = skipped,
            rawIntensityScore = primary + secondary,
            exerciseContributors = contributors.toList(),
        )
    }
}
