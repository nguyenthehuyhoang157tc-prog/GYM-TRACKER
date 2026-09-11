package com.hoctin.gymtracker.data

import androidx.room.withTransaction
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class SetInput(
    val weight: Double,
    val reps: Int,
    val rir: Int,
    val notes: String?,
    // DB-6C: non-rep tracking. Null for normal reps/kg sets; set for time/distance, with reps/rir = 0.
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    // WEARABLE-1A: 1-based draft row identity before incomplete rows are filtered/renumbered on save.
    val sourceDraftOrdinal: Int? = null,
    // Stable row key survives removal/reordering; ordinal is retained only for audit display.
    val sourceDraftKey: String? = null,
)

data class ExerciseLogInput(
    val exercise: ExerciseEntity,
    val sets: List<SetInput>,
    // DB-PRE7B: catalog-aware identity persisted on every logged set (resolved from the exercise name by
    // the ViewModel). Defaults to a custom key so a save can never lose identity even if unresolved.
    val canonicalExerciseKey: String = CanonicalExerciseKey.customKey(exercise.name),
)

/** Logging input for a Saved One-Off template exercise; it never borrows a Routine exercise id. */
data class SavedWorkoutExerciseLogInput(
    val exercise: SavedWorkoutExerciseEntity,
    val sets: List<SetInput>,
)

private data class PersistableExerciseLogInput(
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val canonicalExerciseKey: String,
    val sets: List<SetInput>,
)

/** Internal transaction result used by WEARABLE-1A to bind research outcomes to the exact saved sets. */
data class PersistedWorkoutSession(
    val sessionId: Long,
    val sets: List<LoggedSetEntity>,
    val sourceDraftOrdinalBySetId: Map<Long, Int>,
    val sourceDraftKeyBySetId: Map<Long, String>,
)

internal fun loggedSetToExportJson(set: LoggedSetEntity): JSONObject =
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
        .put("isPrReps", set.isPrReps)
        .put("durationSeconds", set.durationSeconds ?: JSONObject.NULL)
        .put("distanceMeters", set.distanceMeters ?: JSONObject.NULL)
        // DB-PRE7B: canonical identity (schemaVersion 3). Older v1/v2 exports omit this -> null on import.
        .put("canonicalExerciseKey", set.canonicalExerciseKey ?: JSONObject.NULL)

internal fun loggedSetFromImportJson(item: JSONObject): LoggedSetEntity =
    LoggedSetEntity(
        id = item.getLong("id"),
        sessionId = item.getLong("sessionId"),
        exerciseId = item.optImportNullableLong("exerciseId"),
        exerciseNameSnapshot = item.getString("exerciseNameSnapshot"),
        setNumber = item.getInt("setNumber"),
        weight = item.getDouble("weight"),
        reps = item.getInt("reps"),
        rir = item.getInt("rir"),
        notes = item.optImportNullableString("notes"),
        isPrWeight = item.getBoolean("isPrWeight"),
        isPrReps = item.getBoolean("isPrReps"),
        durationSeconds = item.optImportNullableInt("durationSeconds"),
        distanceMeters = item.optImportNullableDouble("distanceMeters"),
        // Missing in v1/v2 exports -> null; the post-import backfill resolves it. v3 exports preserve it.
        canonicalExerciseKey = item.optImportNullableString("canonicalExerciseKey"),
    )

internal fun workoutSessionToExportJson(session: WorkoutSessionEntity): JSONObject =
    JSONObject()
        .put("id", session.id)
        .put("dayId", session.dayId ?: JSONObject.NULL)
        .put("dayNameSnapshot", session.dayNameSnapshot)
        .put("dateEpochDay", session.dateEpochDay)
        .put("startedAtMillis", session.startedAtMillis)
        .put("notes", session.notes ?: JSONObject.NULL)
        // MUSCLE-3B.1: additive optional field. Older importers ignore unknown keys.
        .put("durationSeconds", session.durationSeconds ?: JSONObject.NULL)
        // V3.6A: additive optional field. Older importers ignore unknown keys.
        .put("readiness", session.readiness ?: JSONObject.NULL)
        // MUSCLE-3C: additive optional saved-session title. Null keeps old fallback behavior.
        .put("customTitle", session.customTitle ?: JSONObject.NULL)
        // PLAN-V3A schemaVersion 6: typed provenance. Older importers ignore unknown keys.
        .put("sourceType", WorkoutSessionSource.fromStored(session.sourceType).name)
        .put("savedWorkoutId", session.savedWorkoutId ?: JSONObject.NULL)

internal fun workoutSessionFromImportJson(item: JSONObject): WorkoutSessionEntity =
    WorkoutSessionEntity(
        id = item.getLong("id"),
        dayId = item.optImportNullableLong("dayId"),
        dayNameSnapshot = item.getString("dayNameSnapshot"),
        dateEpochDay = item.getLong("dateEpochDay"),
        startedAtMillis = item.getLong("startedAtMillis"),
        notes = item.optImportNullableString("notes"),
        // V3.6A: tolerant - missing key or unrecognized value both decode to null (UNKNOWN).
        readiness = Readiness.fromName(item.optImportNullableString("readiness"))?.name,
        // MUSCLE-3B.1: tolerant - missing in older exports -> null (no duration shown).
        durationSeconds = item.optImportNullableInt("durationSeconds")?.takeIf { it > 0 },
        // MUSCLE-3C: tolerant - missing in older exports -> null (display dayNameSnapshot).
        customTitle = item.optImportNullableString("customTitle")?.trim()?.takeIf { it.isNotBlank() },
        sourceType = WorkoutSessionSource.fromStored(
            item.optImportNullableString("sourceType"),
        ).name,
        savedWorkoutId = item.optImportNullableLong("savedWorkoutId"),
    )

internal fun savedWorkoutToExportJson(workout: SavedWorkoutEntity): JSONObject =
    JSONObject()
        .put("id", workout.id)
        .put("name", workout.name)
        .put("createdAtMillis", workout.createdAtMillis)
        .put("updatedAtMillis", workout.updatedAtMillis)
        .put("sortOrder", workout.sortOrder)

internal fun savedWorkoutFromImportJson(item: JSONObject): SavedWorkoutEntity =
    SavedWorkoutEntity(
        id = item.getLong("id"),
        name = item.getString("name").trim().also {
            require(it.isNotEmpty()) { "Saved Workout name cannot be blank" }
        },
        createdAtMillis = item.getLong("createdAtMillis"),
        updatedAtMillis = item.getLong("updatedAtMillis"),
        sortOrder = item.getInt("sortOrder"),
    )

internal fun savedWorkoutExerciseToExportJson(exercise: SavedWorkoutExerciseEntity): JSONObject =
    JSONObject()
        .put("id", exercise.id)
        .put("savedWorkoutId", exercise.savedWorkoutId)
        .put("catalogSlug", exercise.catalogSlug ?: JSONObject.NULL)
        .put("canonicalExerciseKey", exercise.canonicalExerciseKey)
        .put("exerciseNameSnapshot", exercise.exerciseNameSnapshot)
        .put("targetSets", exercise.targetSets)
        .put("targetMinReps", exercise.targetMinReps)
        .put("targetMaxReps", exercise.targetMaxReps)
        .put("sortOrder", exercise.sortOrder)

internal fun savedWorkoutExerciseFromImportJson(item: JSONObject): SavedWorkoutExerciseEntity =
    SavedWorkoutExerciseEntity(
        id = item.getLong("id"),
        savedWorkoutId = item.getLong("savedWorkoutId"),
        catalogSlug = item.optImportNullableString("catalogSlug")?.trim()?.takeIf { it.isNotEmpty() },
        canonicalExerciseKey = item.getString("canonicalExerciseKey").also {
            require(it.isNotBlank()) { "Canonical exercise key cannot be blank" }
        },
        exerciseNameSnapshot = item.getString("exerciseNameSnapshot").trim().also {
            require(it.isNotEmpty()) { "Exercise name cannot be blank" }
        },
        targetSets = item.getInt("targetSets").coerceAtLeast(1),
        targetMinReps = item.getInt("targetMinReps").coerceAtLeast(1),
        targetMaxReps = item.getInt("targetMaxReps").coerceAtLeast(
            item.getInt("targetMinReps").coerceAtLeast(1),
        ),
        sortOrder = item.getInt("sortOrder"),
    )

internal fun normalizedSessionTitle(title: String): String? =
    title.trim().takeIf { it.isNotBlank() }

private fun JSONObject.optImportNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null

private fun JSONObject.optImportNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

private fun JSONObject.optImportNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.optImportNullableDouble(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null

class GymRepository(
    private val dao: GymDao,
    private val database: AppDatabase? = null,
) {
    fun observePlan(): Flow<List<WorkoutDayWithExercises>> {
        return dao.observePlan().map { days ->
            days.map { day ->
                day.copy(
                    exercises = day.exercises.sortedWith(compareBy<ExerciseEntity> { it.sortOrder }.thenBy { it.id }),
                )
            }
        }
    }

    /**
     * Heaviest logged load per plan exercise id, used only for the read-only "Max Weight" subtitle on
     * Plan and Routine Detail rows. Reactive: it recomputes whenever a session is logged. Exercises
     * with no weighted history are simply absent from the map.
     */
    fun observeMaxWeightByExerciseId(): Flow<Map<Long, Double>> =
        dao.observeMaxWeightByExerciseId().map { rows ->
            rows.mapNotNull { row ->
                val id = row.exerciseId ?: return@mapNotNull null
                val weight = row.maxWeight ?: return@mapNotNull null
                if (weight <= 0.0) null else id to weight
            }.toMap()
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

    /**
     * DB-PRE7B: cross-day history for a canonical exercise [key], resolved from [exerciseName]. This is
     * the identity-correct replacement for the per-plan-row [observeExerciseHistory]; the same exercise
     * in two days shares one history. [exerciseName] drives the null-key fallback for un-backfilled rows.
     */
    fun observeExerciseHistoryByKey(key: String, exerciseName: String): Flow<List<ExerciseHistoryItem>> =
        dao.observeExerciseHistoryByCanonicalKey(key, CanonicalExerciseKey.normalizeName(exerciseName))

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

    /** All workout days in plan order (by sortOrder then id). Used to compute the next day. */
    suspend fun workoutDaysOrdered(): List<WorkoutDayEntity> = dao.getAllWorkoutDays()

    suspend fun getWorkoutDay(dayId: Long): WorkoutDayEntity? = dao.getWorkoutDay(dayId)
    suspend fun getExercisesForDay(dayId: Long): List<ExerciseEntity> = dao.getExercisesForDay(dayId)
    suspend fun getAllExercises(): List<ExerciseEntity> = dao.getAllExercises()
    suspend fun getExercise(exerciseId: Long): ExerciseEntity? = dao.getExercise(exerciseId)
    suspend fun recentSetsForExercise(exerciseId: Long): List<LoggedSetEntity> =
        dao.getRecentSetsForExercise(exerciseId)

    /** DB-PRE7B: recent sets for a canonical exercise [key] across all plan days (newest session first). */
    suspend fun recentSetsForExerciseKey(key: String, exerciseName: String): List<LoggedSetEntity> =
        dao.getRecentSetsForCanonicalKey(key, CanonicalExerciseKey.normalizeName(exerciseName))

    /**
     * DB-PRE7B one-time/idempotent backfill: resolve a canonical key for every logged set whose key is
     * still null/blank, using [slugsByName] (catalog name/alias → slug). Only null/blank rows are touched
     * — `exerciseNameSnapshot` and all other fields are never rewritten. Returns rows updated. Safe to run
     * repeatedly (and after Import). The keys it writes are stable (same catalog → same resolution).
     */
    suspend fun backfillCanonicalKeys(slugsByName: Map<String, String>): Int {
        var updated = 0
        dao.distinctNamesMissingCanonicalKey().forEach { name ->
            val key = CanonicalExerciseKey.resolve(name, slugsByName)
            updated += dao.backfillCanonicalKeyForName(name, key)
        }
        return updated
    }

    /** V3.6B: latest-known readiness for a saved session (null = unknown). Read-only, no schema change. */
    suspend fun readinessForSession(sessionId: Long): Readiness? =
        Readiness.fromName(dao.getSessionReadiness(sessionId))

    /** MUSCLE-1: one saved session + its sets, for the Muscle Heat Map. Read-only, no schema change. */
    suspend fun sessionWithSets(sessionId: Long): SessionWithSets? =
        dao.getSessionWithSets(sessionId)

    /** MUSCLE-3C: rename one saved session. Blank input clears the override and restores fallback. */
    suspend fun renameWorkoutSession(sessionId: Long, title: String): Boolean {
        return dao.updateSessionCustomTitle(sessionId, normalizedSessionTitle(title)) > 0
    }

    /** V3.7B: readiness per session id (for the engine's readiness trend). Read-only, no schema change. */
    suspend fun readinessBySession(sessionIds: List<Long>): Map<Long, Readiness?> {
        if (sessionIds.isEmpty()) return emptyMap()
        return dao.getReadinessForSessions(sessionIds)
            .associate { it.sessionId to Readiness.fromName(it.readiness) }
    }

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

    /**
     * Persist a new ordering for a day's exercises: each exercise's [ExerciseEntity.sortOrder] is set to
     * its position in [orderedExercises]. One REPLACE batch keeps it atomic and preserves every other
     * field/id. No schema change — `sortOrder` already exists and drives `observePlan` ordering.
     */
    suspend fun reorderExercises(orderedExercises: List<ExerciseEntity>) {
        if (orderedExercises.isEmpty()) return
        val renumbered = orderedExercises.mapIndexed { index, exercise -> exercise.copy(sortOrder = index) }
        dao.insertExercises(renumbered)
    }

    suspend fun saveWorkoutSession(
        day: WorkoutDayEntity,
        inputs: List<ExerciseLogInput>,
        date: LocalDate = LocalDate.now(),
        readiness: Readiness? = null,
        // MUSCLE-3B.1: wall-clock workout duration (Log open → save); null = unknown.
        durationSeconds: Int? = null,
    ): Long = if (database != null) {
        database.withTransaction {
            persistWorkoutSession(
                source = WorkoutLaunchSource.Routine(day),
                inputs = inputs.map { it.toPersistable() },
                date = date,
                readiness = readiness,
                durationSeconds = durationSeconds,
            ).sessionId
        }
    } else {
        // Kept for isolated tests/fakes. Production always supplies the database and is transactional.
        persistWorkoutSession(
            source = WorkoutLaunchSource.Routine(day),
            inputs = inputs.map { it.toPersistable() },
            date = date,
            readiness = readiness,
            durationSeconds = durationSeconds,
        ).sessionId
    }

    /**
     * PLAN-V3A foundation for a real one-off launch. It shares normal History/canonical evidence but
     * stores no Routine day id and cannot advance selected Routine state (that behavior is UI-owned).
     */
    suspend fun saveSavedOneOffWorkoutSession(
        workout: SavedWorkoutEntity,
        inputs: List<SavedWorkoutExerciseLogInput>,
        date: LocalDate = LocalDate.now(),
        readiness: Readiness? = null,
        durationSeconds: Int? = null,
    ): Long {
        val save: suspend () -> Long = {
            persistWorkoutSession(
                source = WorkoutLaunchSource.SavedOneOff(workout),
                inputs = inputs.map { it.toPersistable() },
                date = date,
                readiness = readiness,
                durationSeconds = durationSeconds,
            ).sessionId
        }
        return if (database != null) database.withTransaction { save() } else save()
    }

    /** Caller must already own the Room transaction when using this from recovery research. */
    internal suspend fun persistWorkoutSession(
        day: WorkoutDayEntity,
        inputs: List<ExerciseLogInput>,
        date: LocalDate = LocalDate.now(),
        readiness: Readiness? = null,
        durationSeconds: Int? = null,
    ): PersistedWorkoutSession = persistWorkoutSession(
        source = WorkoutLaunchSource.Routine(day),
        inputs = inputs.map { it.toPersistable() },
        date = date,
        readiness = readiness,
        durationSeconds = durationSeconds,
    )

    /** Caller must own the transaction. Recovery Research intentionally uses only the Routine wrapper. */
    private suspend fun persistWorkoutSession(
        source: WorkoutLaunchSource,
        inputs: List<PersistableExerciseLogInput>,
        date: LocalDate,
        readiness: Readiness?,
        durationSeconds: Int?,
    ): PersistedWorkoutSession {
        val sessionId = dao.insertWorkoutSession(
            WorkoutSessionEntity(
                dayId = source.routineDayId,
                dayNameSnapshot = source.displayNameSnapshot,
                dateEpochDay = date.toEpochDay(),
                startedAtMillis = System.currentTimeMillis(),
                readiness = readiness?.name,
                durationSeconds = durationSeconds?.takeIf { it > 0 },
                sourceType = source.sessionSource.name,
                savedWorkoutId = source.savedWorkoutTemplateId,
            ),
        )

        val loggedSets = mutableListOf<LoggedSetEntity>()
        val sourceDraftOrdinals = mutableListOf<Int>()
        val sourceDraftKeys = mutableListOf<String>()
        inputs.forEach { input ->
            // DB-PRE7B: PR baseline is now the canonical (cross-day) max, not the per-plan-row max, so a
            // PR is judged against the exercise's whole history. The name drives the null-key fallback.
            val normalizedName = CanonicalExerciseKey.normalizeName(input.exerciseNameSnapshot)
            var bestWeight = dao.maxWeightForCanonicalKey(input.canonicalExerciseKey, normalizedName) ?: -1.0
            var bestReps = dao.maxRepsForCanonicalKey(input.canonicalExerciseKey, normalizedName) ?: -1
            input.sets.forEachIndexed { index, set ->
                // DB-6C: time/distance sets have no rep/weight PR concept yet — never flag them.
                val isNonRep = set.durationSeconds != null || set.distanceMeters != null
                val isWeightPr = !isNonRep && set.weight > bestWeight
                val isRepsPr = !isNonRep && set.reps > bestReps
                if (isWeightPr) bestWeight = set.weight
                if (isRepsPr) bestReps = set.reps
                loggedSets += LoggedSetEntity(
                    sessionId = sessionId,
                    exerciseId = input.exerciseId,
                    exerciseNameSnapshot = input.exerciseNameSnapshot,
                    setNumber = index + 1,
                    weight = set.weight,
                    reps = set.reps,
                    rir = set.rir,
                    notes = set.notes?.trim()?.takeIf { it.isNotBlank() },
                    isPrWeight = isWeightPr,
                    isPrReps = isRepsPr,
                    durationSeconds = set.durationSeconds,
                    distanceMeters = set.distanceMeters,
                    canonicalExerciseKey = input.canonicalExerciseKey,
                )
                sourceDraftOrdinals += set.sourceDraftOrdinal ?: (index + 1)
                sourceDraftKeys += set.sourceDraftKey.orEmpty()
            }
        }
        val persistedSets = if (loggedSets.isNotEmpty()) {
            val ids = dao.insertLoggedSets(loggedSets)
            loggedSets.zip(ids).map { (set, id) -> set.copy(id = id) }
        } else {
            emptyList()
        }
        return PersistedWorkoutSession(
            sessionId = sessionId,
            sets = persistedSets,
            sourceDraftOrdinalBySetId = persistedSets.mapIndexed { index, set ->
                set.id to sourceDraftOrdinals[index]
            }.toMap(),
            sourceDraftKeyBySetId = persistedSets.mapIndexedNotNull { index, set ->
                sourceDraftKeys[index].takeIf { it.isNotBlank() }?.let { set.id to it }
            }.toMap(),
        )
    }

    private fun ExerciseLogInput.toPersistable() = PersistableExerciseLogInput(
        exerciseId = exercise.id,
        exerciseNameSnapshot = exercise.name,
        canonicalExerciseKey = canonicalExerciseKey,
        sets = sets,
    )

    private fun SavedWorkoutExerciseLogInput.toPersistable() = PersistableExerciseLogInput(
        // A Saved Workout exercise id belongs to its template table, not the Routine exercises table.
        exerciseId = null,
        exerciseNameSnapshot = exercise.exerciseNameSnapshot,
        canonicalExerciseKey = exercise.canonicalExerciseKey,
        sets = sets,
    )

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        // DB-6B: 2 for the additive logged-set duration/distance keys. DB-PRE7B: 3 for the additive
        // canonicalExerciseKey. The importer below stays tolerant of v1/v2 (and any future) exports —
        // missing keys decode to null (and the canonical key is re-derived by the post-import backfill).
        // MUSCLE-3B.1: v4 adds optional workoutSessions.durationSeconds. MUSCLE-3C: v5 adds optional
        // workoutSessions.customTitle. PLAN-V3A: v6 adds Saved Workout definitions and typed session
        // provenance. Importer stays tolerant of all older versions.
        root.put("schemaVersion", 6)
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
                    array.put(workoutSessionToExportJson(session))
                }
            },
        )
        root.put(
            "loggedSets",
            JSONArray().also { array ->
                dao.getAllLoggedSets().forEach { set ->
                    array.put(loggedSetToExportJson(set))
                }
            },
        )
        root.put(
            "savedWorkouts",
            JSONArray().also { array ->
                dao.getAllSavedWorkouts().forEach { array.put(savedWorkoutToExportJson(it)) }
            },
        )
        root.put(
            "savedWorkoutExercises",
            JSONArray().also { array ->
                dao.getAllSavedWorkoutExercises().forEach {
                    array.put(savedWorkoutExerciseToExportJson(it))
                }
            },
        )
        root.toString(2)
    }

    suspend fun importJson(json: String) = withContext(Dispatchers.IO) {
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
                // V3.6A: tolerant — missing key or unrecognized value both decode to null (UNKNOWN).
                readiness = Readiness.fromName(item.optNullableString("readiness"))?.name,
                // MUSCLE-3B.1: tolerant — missing in older exports -> null (no duration shown).
                durationSeconds = if (item.has("durationSeconds") && !item.isNull("durationSeconds")) {
                    item.optInt("durationSeconds").takeIf { it > 0 }
                } else {
                    null
                },
                customTitle = item.optNullableString("customTitle")?.trim()?.takeIf { it.isNotBlank() },
                sourceType = WorkoutSessionSource.fromStored(
                    item.optNullableString("sourceType"),
                ).name,
                savedWorkoutId = item.optNullableLong("savedWorkoutId"),
            )
        }
        val sets = root.getJSONArray("loggedSets").mapObjects(::loggedSetFromImportJson)
        val savedWorkouts = root.optJSONArray("savedWorkouts")?.mapObjects(::savedWorkoutFromImportJson)
        val savedWorkoutExercises = root.optJSONArray("savedWorkoutExercises")
            ?.mapObjects(::savedWorkoutExerciseFromImportJson)
        require((savedWorkouts == null) == (savedWorkoutExercises == null)) {
            "Saved Workout backup collections must be present together"
        }
        if (savedWorkouts != null && savedWorkoutExercises != null) {
            val workoutIds = savedWorkouts.map { it.id }.toSet()
            require(savedWorkoutExercises.all { it.savedWorkoutId in workoutIds }) {
                "Saved Workout exercise references a missing template"
            }
        }
        dao.replaceAllData(
            days = days,
            exercises = exercises,
            sessions = sessions,
            sets = sets,
            savedWorkouts = savedWorkouts,
            savedWorkoutExercises = savedWorkoutExercises,
        )
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (has(name) && !isNull(name)) optString(name) else null
    }

    private fun JSONObject.optNullableLong(name: String): Long? {
        return if (has(name) && !isNull(name)) optLong(name) else null
    }

}
