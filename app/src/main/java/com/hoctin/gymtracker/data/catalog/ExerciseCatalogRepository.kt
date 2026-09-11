package com.hoctin.gymtracker.data.catalog

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for the Exercise Catalog (DB-0 foundation; DB-1.5 content + detail fields).
 *
 * Responsibilities:
 * - Seed / re-seed the bundled catalog into Room ([ensureSeeded]).
 * - Read access as the [CatalogExercise] domain model.
 * - Deterministic search/filter via [CatalogSearch] and a precise [findByName] lookup for the detail popup.
 *
 * The catalog is reference data: it is never written to History, never included in Import/Export, and
 * never consumed by Progression Engine V3.
 */
class ExerciseCatalogRepository(
    private val dao: CatalogDao,
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Seeds the bundled catalog, and re-seeds (clear + insert) whenever the bundled content version
     * changes or the table is empty. This makes future content batches a one-line version bump with no
     * schema change. Re-seeding only ever touches the catalog table (reference data) — never user data.
     *
     * DB-4B.2: the content is read from the bundled FED-canonical JSON asset
     * (`catalog/catalog_fed_v1.json`) via [CatalogJsonLoader], not from hand-authored Kotlin.
     */
    suspend fun ensureSeeded() {
        val storedVersion = prefs.getInt(KEY_VERSION, 0)
        if (storedVersion == CatalogSeed.CONTENT_VERSION && dao.count() > 0) return
        val json = appContext.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
        val exercises = CatalogJsonLoader.parse(json)
        dao.clearAll()
        dao.insertAll(exercises.map { it.toEntity() })
        prefs.edit().putInt(KEY_VERSION, CatalogSeed.CONTENT_VERSION).apply()
    }

    suspend fun all(): List<CatalogExercise> = dao.getAll().map { it.toDomain() }

    fun observeAll(): Flow<List<CatalogExercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /**
     * DB-PRE7B: the normalized-name → catalog-slug map used to resolve [CanonicalExerciseKey]s. Build once
     * (the library is small, ~600 entries) and reuse for the save path, history lookups, and backfill.
     */
    suspend fun canonicalSlugsByName(): Map<String, String> = catalogSlugsByName(all())

    /** DB-PRE7B convenience: resolve one exercise name to its canonical key (`catalog:` or `custom:`). */
    suspend fun canonicalKeyFor(name: String): String =
        CanonicalExerciseKey.resolve(name, canonicalSlugsByName())

    /** Lightweight image lookup for exercise-row thumbnails. Exact names and aliases only. */
    fun observeThumbnailRefsByName(): Flow<Map<String, String>> =
        observeAll().map(::catalogThumbnailRefsByName)

    /** Lightweight tracking-type lookup for plan/detail target labels. Exact names and aliases only. */
    fun observeTrackingTypesByName(): Flow<Map<String, TrackingType>> =
        observeAll().map(::catalogTrackingTypesByName)

    /**
     * Precise lookup used by the detail popup when a surface only knows an exercise name (e.g. a plan
     * exercise). Matches an exact (case-insensitive) name or alias; returns null when there is no clear
     * catalog match, so the caller can show a graceful "no details yet" state instead of a wrong match.
     */
    suspend fun findByName(name: String): CatalogExercise? {
        val query = name.trim().lowercase()
        if (query.isEmpty()) return null
        return all().firstOrNull { exercise ->
            exercise.name.trim().lowercase() == query ||
                exercise.aliases.any { it.trim().lowercase() == query }
        }
    }

    /**
     * Search/filter the catalog. All non-null arguments are AND-combined; [query] matches name/aliases.
     * Loads the (small) library and filters in memory via the pure [CatalogSearch].
     */
    suspend fun search(
        query: String? = null,
        equipment: Equipment? = null,
        primaryMuscle: MuscleGroup? = null,
        level: Level? = null,
        progressionType: ProgressionType? = null,
    ): List<CatalogExercise> = CatalogSearch.filter(
        items = all(),
        query = query,
        equipment = equipment,
        primaryMuscle = primaryMuscle,
        level = level,
        progressionType = progressionType,
    )

    private companion object {
        const val PREFS_NAME = "auron_catalog"
        const val KEY_VERSION = "catalog_content_version"
        const val CATALOG_ASSET = "catalog/catalog_fed_v1.json"
    }
}
