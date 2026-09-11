package com.hoctin.gymtracker.data.catalog

/**
 * Pure, deterministic search/filter over a catalog list. Kept storage-agnostic (operates on a plain
 * `List<CatalogExercise>`) so it is trivially unit-testable and reusable by the future Catalog Picker.
 *
 * All provided filters are AND-combined; null/blank arguments are ignored. Text matching is
 * case-insensitive "contains" over the name and aliases. Results preserve input order.
 */
object CatalogSearch {
    fun filter(
        items: List<CatalogExercise>,
        query: String? = null,
        equipment: Equipment? = null,
        primaryMuscle: MuscleGroup? = null,
        level: Level? = null,
        progressionType: ProgressionType? = null,
    ): List<CatalogExercise> {
        val normalizedQuery = query?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        return items.filter { exercise ->
            (normalizedQuery == null || matchesText(exercise, normalizedQuery)) &&
                (equipment == null || exercise.equipment == equipment) &&
                (primaryMuscle == null || exercise.primaryMuscles.contains(primaryMuscle)) &&
                (level == null || exercise.level == level) &&
                (progressionType == null || exercise.progressionType == progressionType)
        }
    }

    private fun matchesText(exercise: CatalogExercise, normalizedQuery: String): Boolean {
        if (exercise.name.lowercase().contains(normalizedQuery)) return true
        return exercise.aliases.any { it.lowercase().contains(normalizedQuery) }
    }
}
