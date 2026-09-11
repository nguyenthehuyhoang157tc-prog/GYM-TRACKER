package com.hoctin.gymtracker.data

import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository

/**
 * DB-PRE7B — one-time, idempotent backfill of [LoggedSetEntity.canonicalExerciseKey] for sets that were
 * logged before the v5→v6 migration (their key is null). Resolves each distinct snapshot name to a
 * canonical key via the seeded catalog (slug where possible, else custom), then updates only the
 * null/blank rows. History display names (`exerciseNameSnapshot`) are never rewritten and no row is
 * deleted.
 *
 * Safe to run multiple times: it only ever touches null/blank rows, and the catalog → key resolution is
 * stable. Guarded by [FLAG_KEY] at startup; also re-run after Import (which may add un-keyed v1/v2 rows).
 *
 * Requires the catalog to be seeded first ([ExerciseCatalogRepository.ensureSeeded]).
 */
object CanonicalKeyBackfill {
    /** SharedPreferences flag set by the caller once the startup backfill has run. */
    const val FLAG_KEY = "canonical_exercise_key_backfill_v1_done"

    /** Resolve + persist canonical keys for all un-keyed logged sets. Returns the number of rows updated. */
    suspend fun run(
        repository: GymRepository,
        catalogRepository: ExerciseCatalogRepository,
    ): Int = repository.backfillCanonicalKeys(catalogRepository.canonicalSlugsByName())
}
