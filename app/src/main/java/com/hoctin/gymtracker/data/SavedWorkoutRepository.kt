package com.hoctin.gymtracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Snapshot accepted from the real catalog or a user-authored exercise when building a template. */
data class SavedWorkoutExerciseDraft(
    val catalogSlug: String?,
    val canonicalExerciseKey: String,
    val exerciseNameSnapshot: String,
    val targetSets: Int,
    val targetMinReps: Int,
    val targetMaxReps: Int,
)

/** Persistence owner for Saved One-Off Workout templates. It never reads or writes Routine rows. */
class SavedWorkoutRepository(
    private val dao: GymDao,
    private val database: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observeAll(): Flow<List<SavedWorkoutWithExercises>> =
        dao.observeSavedWorkouts().map { workouts -> workouts.map(::ordered) }

    fun observeList(): Flow<List<SavedWorkoutListItem>> = dao.observeSavedWorkoutList()

    fun observe(savedWorkoutId: Long): Flow<SavedWorkoutWithExercises?> =
        dao.observeSavedWorkout(savedWorkoutId).map { it?.let(::ordered) }

    suspend fun get(savedWorkoutId: Long): SavedWorkoutWithExercises? =
        dao.getSavedWorkout(savedWorkoutId)?.let(::ordered)

    suspend fun create(name: String, exercises: List<SavedWorkoutExerciseDraft>): Long =
        database.withTransaction {
            val normalizedName = requireName(name)
            val now = clock()
            val id = dao.insertSavedWorkout(
                SavedWorkoutEntity(
                    name = normalizedName,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    sortOrder = dao.getAllSavedWorkouts().size,
                ),
            )
            dao.insertSavedWorkoutExercises(exercises.toEntities(id))
            id
        }

    suspend fun rename(savedWorkoutId: Long, name: String): Boolean = database.withTransaction {
        val existing = dao.getSavedWorkout(savedWorkoutId)?.workout ?: return@withTransaction false
        dao.updateSavedWorkout(existing.copy(name = requireName(name), updatedAtMillis = clock()))
        true
    }

    suspend fun replaceExercises(
        savedWorkoutId: Long,
        exercises: List<SavedWorkoutExerciseDraft>,
    ): Boolean = database.withTransaction {
        val existing = dao.getSavedWorkout(savedWorkoutId)?.workout ?: return@withTransaction false
        dao.getSavedWorkoutExercises(savedWorkoutId).forEach { dao.deleteSavedWorkoutExercise(it) }
        dao.insertSavedWorkoutExercises(exercises.toEntities(savedWorkoutId))
        dao.updateSavedWorkout(existing.copy(updatedAtMillis = clock()))
        true
    }

    suspend fun addExercise(savedWorkoutId: Long, exercise: SavedWorkoutExerciseDraft): Long =
        database.withTransaction {
            val existing = dao.getSavedWorkout(savedWorkoutId)?.workout
                ?: error("Saved Workout $savedWorkoutId does not exist")
            val nextOrder = dao.getSavedWorkoutExercises(savedWorkoutId).size
            val id = dao.insertSavedWorkoutExercise(exercise.toEntity(savedWorkoutId, nextOrder))
            dao.updateSavedWorkout(existing.copy(updatedAtMillis = clock()))
            id
        }

    suspend fun removeExercise(exercise: SavedWorkoutExerciseEntity): Boolean = database.withTransaction {
        val existing = dao.getSavedWorkout(exercise.savedWorkoutId)?.workout
            ?: return@withTransaction false
        dao.deleteSavedWorkoutExercise(exercise)
        val compacted = dao.getSavedWorkoutExercises(exercise.savedWorkoutId)
            .mapIndexed { index, item -> item.copy(sortOrder = index) }
        dao.insertSavedWorkoutExercises(compacted)
        dao.updateSavedWorkout(existing.copy(updatedAtMillis = clock()))
        true
    }

    suspend fun reorderExercises(
        savedWorkoutId: Long,
        orderedExercises: List<SavedWorkoutExerciseEntity>,
    ): Boolean = database.withTransaction {
        val existing = dao.getSavedWorkout(savedWorkoutId)?.workout
            ?: return@withTransaction false
        require(orderedExercises.all { it.savedWorkoutId == savedWorkoutId }) {
            "Cannot move exercises between Saved Workouts during reorder"
        }
        val persistedIds = dao.getSavedWorkoutExercises(savedWorkoutId).map { it.id }.toSet()
        require(
            orderedExercises.size == persistedIds.size &&
                orderedExercises.map { it.id }.toSet() == persistedIds
        ) {
            "Reorder must contain every persisted exercise exactly once"
        }
        dao.insertSavedWorkoutExercises(
            orderedExercises.mapIndexed { index, item -> item.copy(sortOrder = index) },
        )
        dao.updateSavedWorkout(existing.copy(updatedAtMillis = clock()))
        true
    }

    /** Cascades template exercises only. Session History has no FK to the template and is untouched. */
    suspend fun deleteTemplate(savedWorkoutId: Long): Boolean = database.withTransaction {
        val existing = dao.getSavedWorkout(savedWorkoutId)?.workout
            ?: return@withTransaction false
        dao.deleteSavedWorkout(existing)
        val remaining = dao.getAllSavedWorkouts().mapIndexed { index, item -> item.copy(sortOrder = index) }
        remaining.forEach { dao.updateSavedWorkout(it) }
        true
    }

    private fun List<SavedWorkoutExerciseDraft>.toEntities(savedWorkoutId: Long) =
        mapIndexed { index, draft -> draft.toEntity(savedWorkoutId, index) }

    private fun SavedWorkoutExerciseDraft.toEntity(savedWorkoutId: Long, order: Int): SavedWorkoutExerciseEntity {
        val normalizedName = exerciseNameSnapshot.trim()
        require(normalizedName.isNotEmpty()) { "Exercise name cannot be blank" }
        require(canonicalExerciseKey.isNotBlank()) { "Canonical exercise key cannot be blank" }
        // Multi-tracking prescriptions use 0/0 as a stored sentinel for time/distance exercises.
        // Preserve that snapshot exactly; PLAN-V3B will render it through TrackingType metadata.
        val min = targetMinReps.coerceAtLeast(0)
        return SavedWorkoutExerciseEntity(
            savedWorkoutId = savedWorkoutId,
            catalogSlug = catalogSlug?.trim()?.takeIf { it.isNotEmpty() },
            canonicalExerciseKey = canonicalExerciseKey,
            exerciseNameSnapshot = normalizedName,
            targetSets = targetSets.coerceAtLeast(1),
            targetMinReps = min,
            targetMaxReps = targetMaxReps.coerceAtLeast(min),
            sortOrder = order,
        )
    }

    private fun ordered(value: SavedWorkoutWithExercises): SavedWorkoutWithExercises = value.copy(
        exercises = value.exercises.sortedWith(
            compareBy<SavedWorkoutExerciseEntity> { it.sortOrder }.thenBy { it.id },
        ),
    )

    private fun requireName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Saved Workout name cannot be blank" } }
}
