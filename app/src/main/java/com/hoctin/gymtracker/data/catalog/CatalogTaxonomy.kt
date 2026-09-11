package com.hoctin.gymtracker.data.catalog

/**
 * Exercise Catalog taxonomy (DB-0 foundation).
 *
 * These enums are **catalog reference metadata** for search/filter and future engine/AI features.
 * They are deliberately independent of Progression Engine V3: V3 stays frozen and keeps inferring its
 * own progression behavior by exercise name. [ProgressionType] here is descriptive metadata only and
 * is not consumed by V3 in DB-0.
 *
 * Stored in Room as the enum `name` string (see CatalogExerciseEntity mapping), so adding new values
 * at the END of an enum is backward-compatible. Unknown/blank values decode via the `fromName` helpers
 * to a safe fallback so old or future rows never crash the UI.
 */
enum class Equipment {
    BARBELL,
    DUMBBELL,
    CABLE,
    MACHINE,
    BODYWEIGHT,
    KETTLEBELL,
    BAND,
    MEDICINE_BALL,
    EXERCISE_BALL,
    EZ_BAR,
    FOAM_ROLL,
    OTHER;

    companion object {
        fun fromName(value: String?): Equipment =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class MuscleGroup {
    CHEST,
    UPPER_CHEST,
    BACK,
    LATS,
    MID_BACK,
    LOWER_BACK,
    TRAPS,
    SHOULDERS,
    FRONT_DELTS,
    SIDE_DELTS,
    REAR_DELTS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    ABS,
    OBLIQUES,
    CORE,
    ADDUCTORS,
    ABDUCTORS,
    FULL_BODY;

    companion object {
        fun fromName(value: String?): MuscleGroup? =
            entries.firstOrNull { it.name == value }
    }
}

enum class Level {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    PRO;

    companion object {
        fun fromName(value: String?): Level =
            entries.firstOrNull { it.name == value } ?: BEGINNER
    }
}

enum class Force {
    PUSH,
    PULL,
    SQUAT,
    HINGE,
    CARRY,
    ROTATION,
    ISOMETRIC,
    OTHER;

    companion object {
        fun fromName(value: String?): Force =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class Category {
    STRENGTH;

    companion object {
        fun fromName(value: String?): Category =
            entries.firstOrNull { it.name == value } ?: STRENGTH
    }
}

/**
 * Catalog progression metadata. Mirrors the concepts V3 infers by name today, but is **not** wired
 * into V3 in DB-0 — it exists so a future engine upgrade can read it explicitly.
 */
enum class ProgressionType {
    COMPOUND_LOAD,
    ISOLATION_SLOW,
    BODYWEIGHT,
    SKILL_FORM;

    companion object {
        fun fromName(value: String?): ProgressionType =
            entries.firstOrNull { it.name == value } ?: COMPOUND_LOAD
    }
}

/**
 * DB-6B: how an exercise is **logged** (which fields the Log UI should show), independent of
 * [ProgressionType] (how the engine progresses it). Catalog metadata only — DB-6B does not yet wire
 * this into the Log Workout UI or the engine; it is the data foundation for non-rep exercises
 * (sled / carry / hold / distance), per `docs/db6a_multi_tracking_foundation_audit.md`.
 *
 * The current 568 entries use only REPS_WEIGHT / BODYWEIGHT_REPS / SKILL_FORM (+ TIME_ONLY for the two
 * isometric holds Plank / Side Bridge); the time/distance values exist for future imports. Stored
 * as the enum `name`; unknown/blank decodes to the safe `REPS_WEIGHT` default.
 */
enum class TrackingType {
    REPS_WEIGHT,
    BODYWEIGHT_REPS,
    TIME_ONLY,
    TIME_LOAD,
    DISTANCE_LOAD,
    TIME_DISTANCE,
    TIME_DISTANCE_LOAD,
    SKILL_FORM;

    /** Rep/weight-logged types — render exactly like the original Log row (reps + weight + RIR). */
    val isRepBased: Boolean
        get() = this == REPS_WEIGHT || this == BODYWEIGHT_REPS || this == SKILL_FORM

    // Field-config contract: which inputs a Log row shows for this type. Drives the UI with no
    // per-screen when-blocks, and stays future-proof (a new metric adds a flag, not a UI rewrite).
    val showsReps: Boolean get() = isRepBased
    val showsRir: Boolean get() = isRepBased
    val showsLoad: Boolean
        get() = isRepBased || this == TIME_LOAD || this == DISTANCE_LOAD || this == TIME_DISTANCE_LOAD
    val showsDuration: Boolean
        get() = this == TIME_ONLY || this == TIME_LOAD || this == TIME_DISTANCE || this == TIME_DISTANCE_LOAD
    val showsDistance: Boolean
        get() = this == DISTANCE_LOAD || this == TIME_DISTANCE || this == TIME_DISTANCE_LOAD

    companion object {
        fun fromName(value: String?): TrackingType =
            entries.firstOrNull { it.name == value } ?: REPS_WEIGHT
    }
}
