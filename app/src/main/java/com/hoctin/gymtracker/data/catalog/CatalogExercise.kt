package com.hoctin.gymtracker.data.catalog

/**
 * A single Exercise Catalog entry (domain model).
 *
 * This is **reference data** — a read-only library used for search/filter and (later) adding to a
 * plan. It is intentionally separate from [com.hoctin.gymtracker.data.ExerciseEntity], which remains
 * the user-editable Plan Exercise snapshot used by workout days and Log Workout. DB-0 does not link,
 * migrate, or replace plan exercises with catalog entries.
 *
 * [slug] is the stable identifier (e.g. "bench-press") and the Room primary key.
 */
data class CatalogExercise(
    val slug: String,
    val name: String,
    val equipment: Equipment,
    val primaryMuscles: List<MuscleGroup>,
    val secondaryMuscles: List<MuscleGroup>,
    val level: Level,
    val force: Force,
    val category: Category,
    val progressionType: ProgressionType,
    /**
     * DB-6B: which fields the Log UI should show for this exercise (reps/weight vs time/distance/load).
     * Independent of [progressionType]. Defaults to [TrackingType.REPS_WEIGHT] so older data / custom
     * constructions stay safe. Not yet consumed by the Log UI or engine (foundation only).
     */
    val trackingType: TrackingType = TrackingType.REPS_WEIGHT,
    val defaultSets: Int,
    val defaultMinReps: Int,
    val defaultMaxReps: Int,
    val defaultIncrementKg: Double,
    val sessionsRequiredBeforeIncrease: Int,
    val isBodyweight: Boolean,
    val isUnilateral: Boolean,
    /** Extra search keywords / alternate names (e.g. "bench", "flat bench press"). */
    val aliases: List<String>,
    /** Concise, ordered how-to steps shown in the detail popup. */
    val instructions: List<String> = emptyList(),
    /** Short practical coaching cues (optional). */
    val tips: List<String> = emptyList(),
    /**
     * Media references for the detail popup's image slot (e.g. drawable/asset names), carousel-ready.
     * Empty for now — the popup shows a safe placeholder so real images can be added later with no
     * layout change.
     */
    val imageRefs: List<String> = emptyList(),
)
