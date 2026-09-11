package com.hoctin.gymtracker.data.catalog

import com.hoctin.gymtracker.data.LoggedSetEntity

/**
 * DB-PRE7B — the single source of truth for an exercise's **canonical identity**.
 *
 * History/previous-performance/progression/PR lookups must key on this (a catalog-aware identity that is
 * stable across plan days), never on the day-specific `ExerciseEntity.id`. See
 * `docs/db_pre7_exercise_history_identity_audit.md`.
 *
 * Key format:
 * - `catalog:<slug>`  when the exercise name (or one of its aliases) resolves to a FED catalog entry.
 * - `custom:<normalized-name>`  otherwise (custom/unmatched exercises).
 *
 * Resolution mirrors [ExerciseCatalogRepository.findByName]: exact, case-insensitive match on the catalog
 * name or any alias. Build the name→slug map once with [catalogSlugsByName] and reuse it.
 */
object CanonicalExerciseKey {
    const val CATALOG_PREFIX = "catalog:"
    const val CUSTOM_PREFIX = "custom:"

    private val WHITESPACE = Regex("\\s+")

    /**
     * Deterministic name normalization for matching: trim + lowercase (matching the existing
     * `findByName` / `trackingLookupKey` behavior). ASCII lowercase is sufficient for catalog names.
     */
    fun normalizeName(name: String): String = name.trim().lowercase()

    fun catalogKey(slug: String): String = CATALOG_PREFIX + slug

    /** `custom:<normalized-name>` with internal whitespace collapsed to single hyphens (slug-like). */
    fun customKey(name: String): String = CUSTOM_PREFIX + normalizeName(name).replace(WHITESPACE, "-")

    /**
     * Resolve [name] to a canonical key using a prebuilt normalized-name → slug map (see
     * [catalogSlugsByName]). Returns `catalog:<slug>` on a catalog/alias hit, else `custom:<…>`.
     */
    fun resolve(name: String, slugsByName: Map<String, String>): String {
        val slug = slugsByName[normalizeName(name)]
        return if (slug != null) catalogKey(slug) else customKey(name)
    }
}

/**
 * Build the normalized-name → catalog-slug map (exact name + every alias). The first catalog entry wins
 * when aliases collide, matching [ExerciseCatalogRepository.findByName] and [catalogTrackingTypesByName].
 */
fun catalogSlugsByName(exercises: List<CatalogExercise>): Map<String, String> {
    val refs = LinkedHashMap<String, String>()
    exercises.forEach { exercise ->
        addSlug(refs, exercise.name, exercise.slug)
        exercise.aliases.forEach { alias -> addSlug(refs, alias, exercise.slug) }
    }
    return refs
}

private fun addSlug(refs: MutableMap<String, String>, name: String, slug: String) {
    val key = CanonicalExerciseKey.normalizeName(name)
    if (key.isNotEmpty() && key !in refs) refs[key] = slug
}

/**
 * Whether a stored [set] belongs to the canonical exercise identified by [key] / [name]. Mirrors the SQL
 * `WHERE` clause used by the key-scoped history queries in `GymDao`: an exact key match, OR a tolerant
 * fallback for not-yet-backfilled rows (`canonicalExerciseKey` null/blank) that match by normalized name.
 * Kept in sync with that SQL so the behavior is unit-testable without Room.
 */
fun loggedSetMatchesCanonical(set: LoggedSetEntity, key: String, name: String): Boolean {
    val normalized = CanonicalExerciseKey.normalizeName(name)
    return set.canonicalExerciseKey == key ||
        (set.canonicalExerciseKey.isNullOrBlank() &&
            CanonicalExerciseKey.normalizeName(set.exerciseNameSnapshot) == normalized)
}
