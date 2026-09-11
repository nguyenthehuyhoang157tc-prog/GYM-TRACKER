package com.hoctin.gymtracker.data.catalog

/**
 * Catalog content version marker.
 *
 * DB-4B.2: the exercise catalog is now the single source of truth from free-exercise-db
 * (public domain). It is no longer hand-authored in Kotlin — the 515 SUPPORTED_STRENGTH entries
 * live in the bundled asset `assets/catalog/catalog_fed_v1.json` and are loaded by
 * [CatalogJsonLoader] in [ExerciseCatalogRepository.ensureSeeded].
 *
 * Bump [CONTENT_VERSION] whenever the bundled JSON changes so the catalog table is re-seeded
 * (clear + insert) on next launch. Re-seeding only ever touches the reference catalog table — never
 * user Plan/History data.
 *
 * Version history: 1–9 = the legacy in-Kotlin curated + DB-3C import seed (607 entries).
 * 10 = FED-canonical JSON rebuild (515 entries). 11 = DB-5B SAFE_ADD import (+33 → 548 entries).
 * 12 = DB-5D MAYBE SETS_REPS_WEIGHT import (+5 → 553 entries).
 * 13 = DB-5D.2 high/medium-confidence level corrections (6 entries re-leveled; count unchanged).
 * 14 = DB-5F final coverage polish (+15 entries → 568; search aliases added to 10 existing entries).
 * 15 = DB-6B multi-tracking foundation: added `trackingType` to all 568 entries (count unchanged).
 * 16 = DB-6E time/distance import (+9 sled/carry entries -> 577 entries).
 */
object CatalogSeed {
    const val CONTENT_VERSION = 16
}
