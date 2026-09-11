package com.hoctin.gymtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** V3.7B read-only projection: a session's id + its stored readiness (enum name or null). */
data class SessionReadiness(val sessionId: Long, val readiness: String?)

@Dao
abstract class GymDao {
    @Transaction
    @Query("SELECT * FROM workout_days ORDER BY sortOrder, id")
    abstract fun observePlan(): Flow<List<WorkoutDayWithExercises>>

    @Query("SELECT * FROM workout_days ORDER BY sortOrder, id")
    abstract fun observeWorkoutDays(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    abstract fun observeWorkoutDay(dayId: Long): Flow<WorkoutDayEntity?>

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY sortOrder, id")
    abstract fun observeExercisesForDay(dayId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    abstract fun observeExercise(exerciseId: Long): Flow<ExerciseEntity?>

    @Transaction
    @Query("SELECT * FROM saved_workouts ORDER BY sortOrder, id")
    abstract fun observeSavedWorkouts(): Flow<List<SavedWorkoutWithExercises>>

    @Query(
        "SELECT saved_workouts.*, " +
            "(SELECT COUNT(*) FROM workout_sessions " +
            "WHERE sourceType='SAVED_ONE_OFF' AND savedWorkoutId=saved_workouts.id) AS completionCount " +
            "FROM saved_workouts ORDER BY sortOrder, id",
    )
    abstract fun observeSavedWorkoutList(): Flow<List<SavedWorkoutListItem>>

    @Transaction
    @Query("SELECT * FROM saved_workouts WHERE id = :savedWorkoutId")
    abstract fun observeSavedWorkout(savedWorkoutId: Long): Flow<SavedWorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis DESC")
    abstract fun observeSessionHistory(): Flow<List<SessionWithSets>>

    /** WEARABLE-1A: prospective input boundary. No session saved after the cutoff can enter a snapshot. */
    @Transaction
    @Query(
        "SELECT * FROM workout_sessions WHERE startedAtMillis <= :inputDataCutoffAtMillis " +
            "ORDER BY startedAtMillis DESC, id DESC"
    )
    abstract suspend fun getSessionHistoryAtOrBefore(inputDataCutoffAtMillis: Long): List<SessionWithSets>

    @Query(
        """
        SELECT
            logged_sets.id AS setId,
            workout_sessions.id AS sessionId,
            workout_sessions.dateEpochDay AS dateEpochDay,
            workout_sessions.startedAtMillis AS startedAtMillis,
            workout_sessions.dayNameSnapshot AS dayNameSnapshot,
            logged_sets.exerciseNameSnapshot AS exerciseNameSnapshot,
            logged_sets.setNumber AS setNumber,
            logged_sets.weight AS weight,
            logged_sets.reps AS reps,
            logged_sets.rir AS rir,
            logged_sets.notes AS notes,
            logged_sets.isPrWeight AS isPrWeight,
            logged_sets.isPrReps AS isPrReps,
            logged_sets.durationSeconds AS durationSeconds,
            logged_sets.distanceMeters AS distanceMeters
        FROM logged_sets
        INNER JOIN workout_sessions ON workout_sessions.id = logged_sets.sessionId
        WHERE logged_sets.exerciseId = :exerciseId
        ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC
        """
    )
    abstract fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryItem>>

    /**
     * DB-PRE7B canonical-identity history: the same exercise across every plan day. Matches on
     * `canonicalExerciseKey`, with a tolerant fallback for not-yet-backfilled rows (null key) that match
     * by normalized name. [normalizedName] must be passed pre-normalized (`name.trim().lowercase()`), to
     * match `TRIM(LOWER(...))`. Mirrored by `loggedSetMatchesCanonical` for unit tests.
     */
    @Query(
        """
        SELECT
            logged_sets.id AS setId,
            workout_sessions.id AS sessionId,
            workout_sessions.dateEpochDay AS dateEpochDay,
            workout_sessions.startedAtMillis AS startedAtMillis,
            workout_sessions.dayNameSnapshot AS dayNameSnapshot,
            logged_sets.exerciseNameSnapshot AS exerciseNameSnapshot,
            logged_sets.setNumber AS setNumber,
            logged_sets.weight AS weight,
            logged_sets.reps AS reps,
            logged_sets.rir AS rir,
            logged_sets.notes AS notes,
            logged_sets.isPrWeight AS isPrWeight,
            logged_sets.isPrReps AS isPrReps,
            logged_sets.durationSeconds AS durationSeconds,
            logged_sets.distanceMeters AS distanceMeters
        FROM logged_sets
        INNER JOIN workout_sessions ON workout_sessions.id = logged_sets.sessionId
        WHERE logged_sets.canonicalExerciseKey = :key
           OR (logged_sets.canonicalExerciseKey IS NULL
               AND TRIM(LOWER(logged_sets.exerciseNameSnapshot)) = :normalizedName)
        ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC
        """
    )
    abstract fun observeExerciseHistoryByCanonicalKey(
        key: String,
        normalizedName: String,
    ): Flow<List<ExerciseHistoryItem>>

    @Query("SELECT COUNT(*) FROM workout_days")
    abstract suspend fun workoutDayCount(): Int

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    abstract suspend fun getWorkoutDay(dayId: Long): WorkoutDayEntity?

    /** V3.6B: read-only lookup of a session's stored readiness (enum name or null). No schema change. */
    @Query("SELECT readiness FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun getSessionReadiness(sessionId: Long): String?

    /** V3.7B: read-only readiness for a set of sessions (for the engine's readiness trend). No schema change. */
    @Query("SELECT id AS sessionId, readiness FROM workout_sessions WHERE id IN (:sessionIds)")
    abstract suspend fun getReadinessForSessions(sessionIds: List<Long>): List<SessionReadiness>

    /** MUSCLE-1: read-only lookup of one saved session with its sets (Muscle Heat Map). No schema change. */
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    abstract suspend fun getSessionWithSets(sessionId: Long): SessionWithSets?

    /** MUSCLE-3C: persisted saved-session rename. Null clears the override and restores fallback. */
    @Query("UPDATE workout_sessions SET customTitle = :customTitle WHERE id = :sessionId")
    abstract suspend fun updateSessionCustomTitle(sessionId: Long, customTitle: String?): Int

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY sortOrder, id")
    abstract suspend fun getExercisesForDay(dayId: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    abstract suspend fun getExercise(exerciseId: Long): ExerciseEntity?

    @Transaction
    @Query("SELECT * FROM saved_workouts WHERE id = :savedWorkoutId")
    abstract suspend fun getSavedWorkout(savedWorkoutId: Long): SavedWorkoutWithExercises?

    @Query("SELECT * FROM saved_workouts ORDER BY sortOrder, id")
    abstract suspend fun getAllSavedWorkouts(): List<SavedWorkoutEntity>

    @Query("SELECT * FROM saved_workout_exercises ORDER BY savedWorkoutId, sortOrder, id")
    abstract suspend fun getAllSavedWorkoutExercises(): List<SavedWorkoutExerciseEntity>

    @Query("SELECT * FROM saved_workout_exercises WHERE savedWorkoutId = :savedWorkoutId ORDER BY sortOrder, id")
    abstract suspend fun getSavedWorkoutExercises(savedWorkoutId: Long): List<SavedWorkoutExerciseEntity>

    /**
     * DB-4B.2: one-time FED-canonical name normalization for active Plan exercise rows. Renames only the
     * mutable Plan table; History (`logged_sets.exerciseNameSnapshot`) is deliberately left untouched so
     * past sessions keep the name they were logged under. Returns the number of rows updated.
     */
    @Query("UPDATE exercises SET name = :newName WHERE name = :oldName")
    abstract suspend fun renameExercisesByName(oldName: String, newName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutDay(day: WorkoutDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Update
    abstract suspend fun updateWorkoutDay(day: WorkoutDayEntity)

    @Delete
    abstract suspend fun deleteWorkoutDay(day: WorkoutDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Update
    abstract suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    abstract suspend fun deleteExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSavedWorkout(workout: SavedWorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSavedWorkouts(workouts: List<SavedWorkoutEntity>)

    @Update
    abstract suspend fun updateSavedWorkout(workout: SavedWorkoutEntity)

    @Delete
    abstract suspend fun deleteSavedWorkout(workout: SavedWorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSavedWorkoutExercise(exercise: SavedWorkoutExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSavedWorkoutExercises(exercises: List<SavedWorkoutExerciseEntity>)

    @Update
    abstract suspend fun updateSavedWorkoutExercise(exercise: SavedWorkoutExerciseEntity)

    @Delete
    abstract suspend fun deleteSavedWorkoutExercise(exercise: SavedWorkoutExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLoggedSets(sets: List<LoggedSetEntity>): List<Long>

    @Query(
        """
        SELECT logged_sets.*
        FROM logged_sets
        INNER JOIN workout_sessions ON workout_sessions.id = logged_sets.sessionId
        WHERE logged_sets.exerciseId = :exerciseId
        ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC
        """
    )
    abstract suspend fun getRecentSetsForExercise(exerciseId: Long): List<LoggedSetEntity>

    @Query("SELECT MAX(weight) FROM logged_sets WHERE exerciseId = :exerciseId")
    abstract suspend fun maxWeightForExercise(exerciseId: Long): Double?

    @Query("SELECT MAX(reps) FROM logged_sets WHERE exerciseId = :exerciseId")
    abstract suspend fun maxRepsForExercise(exerciseId: Long): Int?

    /**
     * Heaviest load ever logged per plan exercise, observed so Plan / Routine Detail rows can render
     * a live "Max Weight" subtitle. `weight > 0` excludes bodyweight-only and time/distance sets,
     * where a "max load" figure would be meaningless rather than merely small.
     */
    @Query(
        """
        SELECT exerciseId, MAX(weight) AS maxWeight
        FROM logged_sets
        WHERE exerciseId IS NOT NULL AND weight > 0
        GROUP BY exerciseId
        """
    )
    abstract fun observeMaxWeightByExerciseId(): Flow<List<ExerciseMaxWeightRow>>

    // ---- DB-PRE7B canonical-identity lookups (key match, with null-key normalized-name fallback) ----

    @Query(
        """
        SELECT logged_sets.*
        FROM logged_sets
        INNER JOIN workout_sessions ON workout_sessions.id = logged_sets.sessionId
        WHERE logged_sets.canonicalExerciseKey = :key
           OR (logged_sets.canonicalExerciseKey IS NULL
               AND TRIM(LOWER(logged_sets.exerciseNameSnapshot)) = :normalizedName)
        ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC
        """
    )
    abstract suspend fun getRecentSetsForCanonicalKey(
        key: String,
        normalizedName: String,
    ): List<LoggedSetEntity>

    @Query(
        """
        SELECT MAX(weight) FROM logged_sets
        WHERE canonicalExerciseKey = :key
           OR (canonicalExerciseKey IS NULL AND TRIM(LOWER(exerciseNameSnapshot)) = :normalizedName)
        """
    )
    abstract suspend fun maxWeightForCanonicalKey(key: String, normalizedName: String): Double?

    @Query(
        """
        SELECT MAX(reps) FROM logged_sets
        WHERE canonicalExerciseKey = :key
           OR (canonicalExerciseKey IS NULL AND TRIM(LOWER(exerciseNameSnapshot)) = :normalizedName)
        """
    )
    abstract suspend fun maxRepsForCanonicalKey(key: String, normalizedName: String): Int?

    /** DB-PRE7B backfill: distinct snapshot names whose canonical key is still null/blank. */
    @Query(
        "SELECT DISTINCT exerciseNameSnapshot FROM logged_sets " +
            "WHERE canonicalExerciseKey IS NULL OR canonicalExerciseKey = ''"
    )
    abstract suspend fun distinctNamesMissingCanonicalKey(): List<String>

    /** DB-PRE7B backfill: set the resolved key for all null/blank rows of one snapshot name. Idempotent. */
    @Query(
        "UPDATE logged_sets SET canonicalExerciseKey = :key " +
            "WHERE (canonicalExerciseKey IS NULL OR canonicalExerciseKey = '') AND exerciseNameSnapshot = :name"
    )
    abstract suspend fun backfillCanonicalKeyForName(name: String, key: String): Int

    @Query("SELECT * FROM workout_days ORDER BY sortOrder, id")
    abstract suspend fun getAllWorkoutDays(): List<WorkoutDayEntity>

    @Query("SELECT * FROM exercises ORDER BY dayId, sortOrder, id")
    abstract suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis")
    abstract suspend fun getAllWorkoutSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM logged_sets ORDER BY sessionId, setNumber, id")
    abstract suspend fun getAllLoggedSets(): List<LoggedSetEntity>

    @Query("DELETE FROM logged_sets")
    abstract suspend fun clearLoggedSets()

    @Query("DELETE FROM workout_sessions")
    abstract suspend fun clearWorkoutSessions()

    @Query("DELETE FROM exercises")
    abstract suspend fun clearExercises()

    @Query("DELETE FROM workout_days")
    abstract suspend fun clearWorkoutDays()

    @Query("DELETE FROM saved_workout_exercises")
    abstract suspend fun clearSavedWorkoutExercises()

    @Query("DELETE FROM saved_workouts")
    abstract suspend fun clearSavedWorkouts()

    @Transaction
    open suspend fun replaceAllData(
        days: List<WorkoutDayEntity>,
        exercises: List<ExerciseEntity>,
        sessions: List<WorkoutSessionEntity>,
        sets: List<LoggedSetEntity>,
        savedWorkouts: List<SavedWorkoutEntity>? = null,
        savedWorkoutExercises: List<SavedWorkoutExerciseEntity>? = null,
    ) {
        clearLoggedSets()
        clearWorkoutSessions()
        clearExercises()
        clearWorkoutDays()
        insertWorkoutDays(days)
        insertExercises(exercises)
        insertWorkoutSessions(sessions)
        insertLoggedSets(sets)
        // Missing collections mean an old (pre-PLAN-V3A) backup. Preserve locally-authored Saved
        // Workouts because that legacy payload had no way to express whether they should be erased.
        if (savedWorkouts != null && savedWorkoutExercises != null) {
            clearSavedWorkoutExercises()
            clearSavedWorkouts()
            insertSavedWorkouts(savedWorkouts)
            insertSavedWorkoutExercises(savedWorkoutExercises)
        }
    }
}
