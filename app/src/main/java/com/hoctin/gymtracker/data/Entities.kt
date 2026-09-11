package com.hoctin.gymtracker.data

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dayId")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val name: String,
    val targetSets: Int,
    val targetMinReps: Int,
    val targetMaxReps: Int,
    val sortOrder: Int,
)

/** Stable persisted provenance for a completed workout session. */
enum class WorkoutSessionSource {
    ROUTINE,
    SAVED_ONE_OFF;

    companion object {
        fun fromStored(value: String?): WorkoutSessionSource =
            entries.firstOrNull { it.name == value } ?: ROUTINE
    }
}

/**
 * User-authored one-off workout template. It is deliberately not a [WorkoutDayEntity]: templates do
 * not participate in the Routine order, selected day, Home hero, or Weekly Progress.
 */
@Entity(tableName = "saved_workouts")
data class SavedWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sortOrder: Int,
)

/**
 * Immutable-enough prescription snapshot owned by a Saved Workout template. Catalog metadata stays
 * reference data; only the stable slug/key, display name, prescription, and order are copied here.
 */
@Entity(
    tableName = "saved_workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SavedWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedWorkoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("savedWorkoutId"), Index("canonicalExerciseKey")],
)
data class SavedWorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedWorkoutId: Long,
    val catalogSlug: String? = null,
    val canonicalExerciseKey: String,
    val exerciseNameSnapshot: String,
    val targetSets: Int,
    val targetMinReps: Int,
    val targetMaxReps: Int,
    val sortOrder: Int,
)

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index("dayId"),
        Index("dateEpochDay"),
        Index("startedAtMillis"),
        Index("sourceType"),
        Index("savedWorkoutId"),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long?,
    val dayNameSnapshot: String,
    val dateEpochDay: Long,
    val startedAtMillis: Long,
    val notes: String? = null,
    // V3.6A: subjective readiness (Readiness enum name) captured at save; null = unknown / pre-feature.
    // Added in DB v4 via an additive ALTER TABLE ADD COLUMN; the engine does not read it yet (V3.6B).
    val readiness: String? = null,
    // MUSCLE-3B.1: wall-clock workout duration (Log screen open → save), persisted so the Muscle Heat
    // Review shows the same duration when a session is reopened from History/Home. Null = unknown
    // (pre-feature sessions). Added in DB v7 via an additive ALTER TABLE ADD COLUMN.
    val durationSeconds: Int? = null,
    // MUSCLE-3C: optional user-facing saved-session title. Null keeps the original dayNameSnapshot
    // fallback, so old sessions continue displaying normally.
    val customTitle: String? = null,
    // PLAN-V3A: all pre-v11 rows migrate to ROUTINE. Kept as a stored enum name so corrupt/future
    // values can conservatively decode as ROUTINE instead of silently entering one-off behavior.
    @ColumnInfo(defaultValue = "'ROUTINE'")
    val sourceType: String = WorkoutSessionSource.ROUTINE.name,
    // Snapshot association only: intentionally no FK to saved_workouts. Completed History must remain
    // valid after its template is deleted, so a non-null id may outlive the template. Null for Routine
    // or sessions whose one-off template association was unavailable at save/import time.
    val savedWorkoutId: Long? = null,
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index("canonicalExerciseKey")],
)
data class LoggedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rir: Int,
    val notes: String? = null,
    val isPrWeight: Boolean,
    val isPrReps: Boolean,
    // DB-6B multi-tracking foundation: optional fields for non-rep exercises (sled/carry/hold/distance).
    // Nullable, default null, added in DB v5 via an additive ALTER (old rows stay null). The Log UI and
    // engine do not read them yet; for time/distance sets `reps`/`rir` use the 0 sentinel so the
    // existing weight*reps volume math stays correct.
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    // DB-PRE7B canonical exercise identity: a catalog-aware key that is stable across plan days, so the
    // same exercise in different days shares previous-performance/progression/PR history. Format is
    // `catalog:<slug>` when the name/alias resolves to the FED catalog, else `custom:<normalized-name>`.
    // Nullable, added in DB v6 via an additive ALTER (old rows stay null until the one-time backfill).
    // `exerciseId` (plan row) and `exerciseNameSnapshot` (historical display name) are kept unchanged.
    val canonicalExerciseKey: String? = null,
)

/**
 * Read-only projection backing the "Max Weight" subtitle on Plan / Routine Detail exercise rows.
 * Both fields are nullable on purpose: `exerciseId` is nullable in [LoggedSetEntity], and
 * `MAX(weight)` returns NULL for an empty group. Callers filter the nulls out.
 */
data class ExerciseMaxWeightRow(
    val exerciseId: Long?,
    val maxWeight: Double?,
)

data class WorkoutDayWithExercises(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId",
    )
    val exercises: List<ExerciseEntity>,
)

data class SavedWorkoutWithExercises(
    @Embedded val workout: SavedWorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "savedWorkoutId",
    )
    val exercises: List<SavedWorkoutExerciseEntity>,
)

/** Compact list projection for PLAN-V3B; completion count is derived from immutable session rows. */
data class SavedWorkoutListItem(
    @Embedded val workout: SavedWorkoutEntity,
    val completionCount: Int,
)

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sets: List<LoggedSetEntity>,
)

data class ExerciseHistoryItem(
    val setId: Long,
    val sessionId: Long,
    val dateEpochDay: Long,
    val startedAtMillis: Long,
    val dayNameSnapshot: String,
    val exerciseNameSnapshot: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rir: Int,
    val notes: String?,
    val isPrWeight: Boolean,
    val isPrReps: Boolean,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
)
