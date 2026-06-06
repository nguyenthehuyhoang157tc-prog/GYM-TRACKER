package com.hoctin.gymtracker.data

import androidx.room.Embedded
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

@Entity(
    tableName = "workout_sessions",
    indices = [Index("dayId"), Index("dateEpochDay"), Index("startedAtMillis")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long?,
    val dayNameSnapshot: String,
    val dateEpochDay: Long,
    val startedAtMillis: Long,
    val notes: String? = null,
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
    indices = [Index("sessionId"), Index("exerciseId")],
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
)

data class WorkoutDayWithExercises(
    @Embedded val day: WorkoutDayEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "dayId",
    )
    val exercises: List<ExerciseEntity>,
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
)
