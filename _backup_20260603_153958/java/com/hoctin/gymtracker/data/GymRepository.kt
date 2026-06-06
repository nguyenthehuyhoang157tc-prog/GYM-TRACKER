package com.hoctin.gymtracker.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class SetInput(
    val weight: Double,
    val reps: Int,
    val rir: Int,
    val notes: String?,
)

data class ExerciseLogInput(
    val exercise: ExerciseEntity,
    val sets: List<SetInput>,
)

class GymRepository(private val dao: GymDao) {
    fun observePlan(): Flow<List<WorkoutDayWithExercises>> {
        return dao.observePlan().map { days ->
            days.map { day ->
                day.copy(
                    exercises = day.exercises.sortedWith(compareBy<ExerciseEntity> { it.sortOrder }.thenBy { it.id }),
                )
            }
        }
    }

    fun observeWorkoutDays(): Flow<List<WorkoutDayEntity>> = dao.observeWorkoutDays()
    fun observeWorkoutDay(dayId: Long): Flow<WorkoutDayEntity?> = dao.observeWorkoutDay(dayId)
    fun observeExercisesForDay(dayId: Long): Flow<List<ExerciseEntity>> = dao.observeExercisesForDay(dayId)
    fun observeSessionHistory(): Flow<List<SessionWithSets>> {
        return dao.observeSessionHistory().map { sessions ->
            sessions.map { session ->
                session.copy(
                    sets = session.sets.sortedWith(
                        compareBy<LoggedSetEntity> { it.exerciseNameSnapshot }
                            .thenBy { it.setNumber }
                            .thenBy { it.id },
                    ),
                )
            }
        }
    }
    fun observeExercise(exerciseId: Long): Flow<ExerciseEntity?> = dao.observeExercise(exerciseId)
    fun observeExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryItem>> =
        dao.observeExerciseHistory(exerciseId)

    suspend fun ensureSeedData() {
        if (dao.workoutDayCount() > 0) return
        SeedData.defaultSplit.forEachIndexed { dayIndex, seedDay ->
            val dayId = dao.insertWorkoutDay(
                WorkoutDayEntity(
                    name = seedDay.name,
                    sortOrder = dayIndex,
                ),
            )
            val exercises = seedDay.exercises.mapIndexed { exerciseIndex, seedExercise ->
                ExerciseEntity(
                    dayId = dayId,
                    name = seedExercise.name,
                    targetSets = seedExercise.targetSets,
                    targetMinReps = seedExercise.targetMinReps,
                    targetMaxReps = seedExercise.targetMaxReps,
                    sortOrder = exerciseIndex,
                )
            }
            dao.insertExercises(exercises)
        }
    }

    suspend fun getWorkoutDay(dayId: Long): WorkoutDayEntity? = dao.getWorkoutDay(dayId)
    suspend fun getExercisesForDay(dayId: Long): List<ExerciseEntity> = dao.getExercisesForDay(dayId)
    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = dao.getExercise(exerciseId)
    suspend fun recentSetsForExercise(exerciseId: Long): List<LoggedSetEntity> =
        dao.getRecentSetsForExercise(exerciseId)

    suspend fun addWorkoutDay(name: String) {
        val nextOrder = dao.getAllWorkoutDays().size
        dao.insertWorkoutDay(WorkoutDayEntity(name = name.trim(), sortOrder = nextOrder))
    }

    suspend fun updateWorkoutDay(day: WorkoutDayEntity, name: String) {
        dao.updateWorkoutDay(day.copy(name = name.trim()))
    }

    suspend fun deleteWorkoutDay(day: WorkoutDayEntity) {
        dao.deleteWorkoutDay(day)
    }

    suspend fun addExercise(
        dayId: Long,
        name: String,
        targetSets: Int,
        targetMinReps: Int,
        targetMaxReps: Int,
    ) {
        val nextOrder = dao.getExercisesForDay(dayId).size
        dao.insertExercise(
            ExerciseEntity(
                dayId = dayId,
                name = name.trim(),
                targetSets = targetSets.coerceAtLeast(1),
                targetMinReps = targetMinReps.coerceAtLeast(1),
                targetMaxReps = targetMaxReps.coerceAtLeast(targetMinReps.coerceAtLeast(1)),
                sortOrder = nextOrder,
            ),
        )
    }

    suspend fun updateExercise(
        exercise: ExerciseEntity,
        name: String,
        targetSets: Int,
        targetMinReps: Int,
        targetMaxReps: Int,
    ) {
        val minReps = targetMinReps.coerceAtLeast(1)
        dao.updateExercise(
            exercise.copy(
                name = name.trim(),
                targetSets = targetSets.coerceAtLeast(1),
                targetMinReps = minReps,
                targetMaxReps = targetMaxReps.coerceAtLeast(minReps),
            ),
        )
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        dao.deleteExercise(exercise)
    }

    suspend fun saveWorkoutSession(
        day: WorkoutDayEntity,
        inputs: List<ExerciseLogInput>,
        date: LocalDate = LocalDate.now(),
    ): Long {
        val sessionId = dao.insertWorkoutSession(
            WorkoutSessionEntity(
                dayId = day.id,
                dayNameSnapshot = day.name,
                dateEpochDay = date.toEpochDay(),
                startedAtMillis = System.currentTimeMillis(),
            ),
        )

        val loggedSets = mutableListOf<LoggedSetEntity>()
        inputs.forEach { input ->
            var bestWeight = dao.maxWeightForExercise(input.exercise.id) ?: -1.0
            var bestReps = dao.maxRepsForExercise(input.exercise.id) ?: -1
            input.sets.forEachIndexed { index, set ->
                val isWeightPr = set.weight > bestWeight
                val isRepsPr = set.reps > bestReps
                if (isWeightPr) bestWeight = set.weight
                if (isRepsPr) bestReps = set.reps
                loggedSets += LoggedSetEntity(
                    sessionId = sessionId,
                    exerciseId = input.exercise.id,
                    exerciseNameSnapshot = input.exercise.name,
                    setNumber = index + 1,
                    weight = set.weight,
                    reps = set.reps,
                    rir = set.rir,
                    notes = set.notes?.trim()?.takeIf { it.isNotBlank() },
                    isPrWeight = isWeightPr,
                    isPrReps = isRepsPr,
                )
            }
        }
        if (loggedSets.isNotEmpty()) {
            dao.insertLoggedSets(loggedSets)
        }
        return sessionId
    }

    suspend fun exportJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put(
            "workoutDays",
            JSONArray().also { array ->
                dao.getAllWorkoutDays().forEach { day ->
                    array.put(
                        JSONObject()
                            .put("id", day.id)
                            .put("name", day.name)
                            .put("sortOrder", day.sortOrder),
                    )
                }
            },
        )
        root.put(
            "exercises",
            JSONArray().also { array ->
                dao.getAllExercises().forEach { exercise ->
                    array.put(
                        JSONObject()
                            .put("id", exercise.id)
                            .put("dayId", exercise.dayId)
                            .put("name", exercise.name)
                            .put("targetSets", exercise.targetSets)
                            .put("targetMinReps", exercise.targetMinReps)
                            .put("targetMaxReps", exercise.targetMaxReps)
                            .put("sortOrder", exercise.sortOrder),
                    )
                }
            },
        )
        root.put(
            "workoutSessions",
            JSONArray().also { array ->
                dao.getAllWorkoutSessions().forEach { session ->
                    array.put(
                        JSONObject()
                            .put("id", session.id)
                            .put("dayId", session.dayId ?: JSONObject.NULL)
                            .put("dayNameSnapshot", session.dayNameSnapshot)
                            .put("dateEpochDay", session.dateEpochDay)
                            .put("startedAtMillis", session.startedAtMillis)
                            .put("notes", session.notes ?: JSONObject.NULL),
                    )
                }
            },
        )
        root.put(
            "loggedSets",
            JSONArray().also { array ->
                dao.getAllLoggedSets().forEach { set ->
                    array.put(
                        JSONObject()
                            .put("id", set.id)
                            .put("sessionId", set.sessionId)
                            .put("exerciseId", set.exerciseId ?: JSONObject.NULL)
                            .put("exerciseNameSnapshot", set.exerciseNameSnapshot)
                            .put("setNumber", set.setNumber)
                            .put("weight", set.weight)
                            .put("reps", set.reps)
                            .put("rir", set.rir)
                            .put("notes", set.notes ?: JSONObject.NULL)
                            .put("isPrWeight", set.isPrWeight)
                            .put("isPrReps", set.isPrReps),
                    )
                }
            },
        )
        return root.toString(2)
    }

    suspend fun importJson(json: String) {
        val root = JSONObject(json)
        val days = root.getJSONArray("workoutDays").mapObjects { item ->
            WorkoutDayEntity(
                id = item.getLong("id"),
                name = item.getString("name"),
                sortOrder = item.getInt("sortOrder"),
            )
        }
        val exercises = root.getJSONArray("exercises").mapObjects { item ->
            ExerciseEntity(
                id = item.getLong("id"),
                dayId = item.getLong("dayId"),
                name = item.getString("name"),
                targetSets = item.getInt("targetSets"),
                targetMinReps = item.getInt("targetMinReps"),
                targetMaxReps = item.getInt("targetMaxReps"),
                sortOrder = item.getInt("sortOrder"),
            )
        }
        val sessions = root.getJSONArray("workoutSessions").mapObjects { item ->
            WorkoutSessionEntity(
                id = item.getLong("id"),
                dayId = item.optNullableLong("dayId"),
                dayNameSnapshot = item.getString("dayNameSnapshot"),
                dateEpochDay = item.getLong("dateEpochDay"),
                startedAtMillis = item.getLong("startedAtMillis"),
                notes = item.optNullableString("notes"),
            )
        }
        val sets = root.getJSONArray("loggedSets").mapObjects { item ->
            LoggedSetEntity(
                id = item.getLong("id"),
                sessionId = item.getLong("sessionId"),
                exerciseId = item.optNullableLong("exerciseId"),
                exerciseNameSnapshot = item.getString("exerciseNameSnapshot"),
                setNumber = item.getInt("setNumber"),
                weight = item.getDouble("weight"),
                reps = item.getInt("reps"),
                rir = item.getInt("rir"),
                notes = item.optNullableString("notes"),
                isPrWeight = item.getBoolean("isPrWeight"),
                isPrReps = item.getBoolean("isPrReps"),
            )
        }
        dao.replaceAllData(days, exercises, sessions, sets)
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (isNull(name)) null else optString(name)
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        return if (isNull(name)) null else optLong(name)
    }
}
