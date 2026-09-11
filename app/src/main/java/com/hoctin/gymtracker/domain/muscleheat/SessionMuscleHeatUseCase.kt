package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.catalogSlugsByName

/**
 * MUSCLE-1 integration point: compute the full Muscle Heat result for a **saved** workout session id,
 * with **no Room schema change** (one read-only `@Transaction @Query` powers [GymRepository.sessionWithSets]).
 *
 * This is the API MUSCLE-3 will call right after save (`LogWorkoutEvent.Saved(sessionId)`) or from a
 * session detail later. Orchestration only — all scoring lives in the pure [MuscleExposureCalculator]
 * (Layer A) and [MuscleHeatEngine] (Layer B); nothing here is persisted.
 *
 * Data joins performed here:
 *  - session sets are grouped by [LoggedSetEntity.canonicalExerciseKey] (tolerant fallback: resolve the
 *    name snapshot for pre-backfill rows), so identity is stable across plan days;
 *  - the session's plan day (when it still exists) supplies planned exercises → **skipped** detection
 *    (planned, zero sets) and `targetSets` for **partial** scaling;
 *  - `catalog:<slug>` keys join to catalog primary/secondary muscles + trackingType; `custom:` keys get
 *    the safe no-anatomy fallback (they surface as unattributed contributors, never crash).
 *
 * Documented V1 limits: the plan day reflects its CURRENT exercises (if the user edited the plan after
 * saving, skipped/target info follows today's plan); an exercise logged outside the current plan gets
 * `targetSets = 0` → its completed work counts as fully completed rather than mislabelled partial.
 */
class SessionMuscleHeatUseCase(
    private val gymRepository: GymRepository,
    private val catalogRepository: ExerciseCatalogRepository,
) {

    suspend fun forSession(sessionId: Long): SessionMuscleHeat? {
        val session = gymRepository.sessionWithSets(sessionId) ?: return null
        val catalog = catalogRepository.all()
        val bySlug = catalog.associateBy { it.slug }
        val slugsByName = catalogSlugsByName(catalog)

        val setsByKey = session.sets.groupBy { set ->
            set.canonicalExerciseKey?.takeIf { it.isNotBlank() }
                ?: CanonicalExerciseKey.resolve(set.exerciseNameSnapshot, slugsByName)
        }

        val snapshots = mutableListOf<ExerciseSessionSnapshot>()
        val seenKeys = mutableSetOf<String>()
        // Planned exercises first: gives skipped detection (no sets) + targetSets for partial scaling.
        val planned = session.session.dayId?.let { gymRepository.getExercisesForDay(it) }.orEmpty()
        planned.forEach { exercise ->
            val key = CanonicalExerciseKey.resolve(exercise.name, slugsByName)
            if (!seenKeys.add(key)) return@forEach
            snapshots += snapshot(
                key = key,
                name = exercise.name,
                targetSets = exercise.targetSets,
                sets = setsByKey[key].orEmpty(),
                bySlug = bySlug,
            )
        }
        // Logged work that isn't in the (current) plan: still counts, with no plan target known.
        setsByKey.forEach { (key, sets) ->
            if (seenKeys.add(key)) {
                snapshots += snapshot(
                    key = key,
                    name = sets.first().exerciseNameSnapshot,
                    targetSets = 0,
                    sets = sets,
                    bySlug = bySlug,
                )
            }
        }

        val exposure = MuscleExposureCalculator.compute(snapshots)
        return SessionMuscleHeat(exposure = exposure, heat = MuscleHeatEngine.analyze(exposure))
    }

    private fun snapshot(
        key: String,
        name: String,
        targetSets: Int,
        sets: List<LoggedSetEntity>,
        bySlug: Map<String, CatalogExercise>,
    ): ExerciseSessionSnapshot {
        val catalogEntry = key
            .takeIf { it.startsWith(CanonicalExerciseKey.CATALOG_PREFIX) }
            ?.removePrefix(CanonicalExerciseKey.CATALOG_PREFIX)
            ?.let(bySlug::get)
        return ExerciseSessionSnapshot(
            canonicalExerciseKey = key,
            exerciseName = name,
            trackingType = catalogEntry?.trackingType ?: TrackingType.REPS_WEIGHT,
            primaryMuscles = catalogEntry?.primaryMuscles.orEmpty(),
            secondaryMuscles = catalogEntry?.secondaryMuscles.orEmpty(),
            targetSets = targetSets,
            sets = sets,
        )
    }
}

/** The complete runtime result MUSCLE-3's review/share screen will render. Runtime-only, never stored. */
data class SessionMuscleHeat(
    val exposure: SessionMuscleExposure,
    val heat: List<MuscleHeatResult>,
)
