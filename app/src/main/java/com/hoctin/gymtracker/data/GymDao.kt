package com.hoctin.gymtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

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
    @Query("SELECT * FROM workout_sessions ORDER BY startedAtMillis DESC")
    abstract fun observeSessionHistory(): Flow<List<SessionWithSets>>

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
            logged_sets.isPrReps AS isPrReps
        FROM logged_sets
        INNER JOIN workout_sessions ON workout_sessions.id = logged_sets.sessionId
        WHERE logged_sets.exerciseId = :exerciseId
        ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC
        """
    )
    abstract fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryItem>>

    @Query("SELECT COUNT(*) FROM workout_days")
    abstract suspend fun workoutDayCount(): Int

    @Query("SELECT * FROM workout_days WHERE id = :dayId")
    abstract suspend fun getWorkoutDay(dayId: Long): WorkoutDayEntity?

    @Query("SELECT * FROM exercises WHERE dayId = :dayId ORDER BY sortOrder, id")
    abstract suspend fun getExercisesForDay(dayId: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    abstract suspend fun getExercise(exerciseId: Long): ExerciseEntity?

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
    abstract suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertLoggedSets(sets: List<LoggedSetEntity>)

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

    @Transaction
    open suspend fun replaceAllData(
        days: List<WorkoutDayEntity>,
        exercises: List<ExerciseEntity>,
        sessions: List<WorkoutSessionEntity>,
        sets: List<LoggedSetEntity>,
    ) {
        clearLoggedSets()
        clearWorkoutSessions()
        clearExercises()
        clearWorkoutDays()
        insertWorkoutDays(days)
        insertExercises(exercises)
        insertWorkoutSessions(sessions)
        insertLoggedSets(sets)
    }
}
