package com.hoctin.gymtracker.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedWorkoutRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var gymRepository: GymRepository
    private lateinit var savedRepository: SavedWorkoutRepository

    @Before
    fun setUp() {
        database = openDatabase()
        gymRepository = GymRepository(database.gymDao(), database)
        savedRepository = SavedWorkoutRepository(database.gymDao(), database) { 1_000L }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun crudReorderAndDeleteTemplate_preservesCompletedHistoryAndCanonicalEvidence() = runBlocking {
        val savedWorkoutId = savedRepository.create(
            name = " Hotel workout ",
            exercises = listOf(
                draft("bench-press", "Bench Press", 3),
                draft("lat-pulldown", "Lat Pulldown", 4),
            ),
        )
        assertTrue(savedRepository.rename(savedWorkoutId, "Travel strength"))
        val created = requireNotNull(savedRepository.get(savedWorkoutId))
        assertEquals("Travel strength", created.workout.name)
        assertEquals(listOf("Bench Press", "Lat Pulldown"), created.exercises.map { it.exerciseNameSnapshot })

        val nonRepId = savedRepository.addExercise(
            savedWorkoutId,
            SavedWorkoutExerciseDraft(
                catalogSlug = "plank",
                canonicalExerciseKey = "catalog:plank",
                exerciseNameSnapshot = "Plank",
                targetSets = 3,
                targetMinReps = 0,
                targetMaxReps = 0,
            ),
        )
        val nonRep = requireNotNull(savedRepository.get(savedWorkoutId)).exercises.single { it.id == nonRepId }
        assertEquals(0, nonRep.targetMinReps)
        assertEquals(0, nonRep.targetMaxReps)

        val beforeReorder = requireNotNull(savedRepository.get(savedWorkoutId))
        assertTrue(savedRepository.reorderExercises(savedWorkoutId, beforeReorder.exercises.reversed()))
        val reordered = requireNotNull(savedRepository.get(savedWorkoutId))
        assertEquals(
            listOf("Plank", "Lat Pulldown", "Bench Press"),
            reordered.exercises.map { it.exerciseNameSnapshot },
        )
        assertEquals(listOf(0, 1, 2), reordered.exercises.map { it.sortOrder })

        val sessionId = gymRepository.saveSavedOneOffWorkoutSession(
            workout = reordered.workout,
            inputs = listOf(
                SavedWorkoutExerciseLogInput(
                    exercise = reordered.exercises.first { it.exerciseNameSnapshot == "Lat Pulldown" },
                    sets = listOf(SetInput(50.0, 10, 2, null)),
                ),
            ),
            date = LocalDate.of(2026, 8, 12),
        )
        val completed = savedRepository.observeList().first().single()
        assertEquals(1, completed.completionCount)

        val beforeDelete = requireNotNull(gymRepository.sessionWithSets(sessionId))
        assertEquals(WorkoutSessionSource.SAVED_ONE_OFF.name, beforeDelete.session.sourceType)
        assertNull(beforeDelete.session.dayId)
        assertEquals(savedWorkoutId, beforeDelete.session.savedWorkoutId)
        assertEquals("catalog:lat-pulldown", beforeDelete.sets.single().canonicalExerciseKey)
        assertNull(beforeDelete.sets.single().exerciseId)

        assertTrue(savedRepository.deleteTemplate(savedWorkoutId))
        assertNull(savedRepository.get(savedWorkoutId))
        val retainedHistory = gymRepository.sessionWithSets(sessionId)
        assertNotNull(retainedHistory)
        assertEquals("Lat Pulldown", retainedHistory?.sets?.single()?.exerciseNameSnapshot)
    }

    @Test
    fun exportRoundTrip_preservesSavedDefinitionsAndOrder_oldBackupPreservesLocalTemplates() = runBlocking {
        val savedWorkoutId = savedRepository.create(
            "Travel strength",
            listOf(
                draft("lat-pulldown", "Lat Pulldown", 4),
                draft("bench-press", "Bench Press", 3),
            ),
        )
        val sourceWorkout = requireNotNull(savedRepository.get(savedWorkoutId))
        val exportedSessionId = gymRepository.saveSavedOneOffWorkoutSession(
            workout = sourceWorkout.workout,
            inputs = listOf(
                SavedWorkoutExerciseLogInput(
                    exercise = sourceWorkout.exercises.first(),
                    sets = listOf(SetInput(52.5, 8, 2, null)),
                ),
            ),
            date = LocalDate.of(2026, 8, 12),
        )
        val exported = gymRepository.exportJson()
        assertEquals(6, JSONObject(exported).getInt("schemaVersion"))

        val importedDatabase = openDatabase()
        try {
            val importedGym = GymRepository(importedDatabase.gymDao(), importedDatabase)
            val importedSaved = SavedWorkoutRepository(importedDatabase.gymDao(), importedDatabase)
            importedGym.importJson(exported)
            val imported = requireNotNull(importedSaved.get(savedWorkoutId))
            assertEquals("Travel strength", imported.workout.name)
            assertEquals(
                listOf("Lat Pulldown", "Bench Press"),
                imported.exercises.map { it.exerciseNameSnapshot },
            )
            val importedSession = requireNotNull(importedGym.sessionWithSets(exportedSessionId))
            assertEquals(WorkoutSessionSource.SAVED_ONE_OFF.name, importedSession.session.sourceType)
            assertEquals(savedWorkoutId, importedSession.session.savedWorkoutId)
            assertNull(importedSession.session.dayId)
            assertEquals("catalog:lat-pulldown", importedSession.sets.single().canonicalExerciseKey)
        } finally {
            importedDatabase.close()
        }

        val oldBackup = JSONObject()
            .put("schemaVersion", 5)
            .put("workoutDays", JSONArray())
            .put("exercises", JSONArray())
            .put("workoutSessions", JSONArray())
            .put("loggedSets", JSONArray())
            .toString()
        gymRepository.importJson(oldBackup)
        assertNotNull(savedRepository.get(savedWorkoutId))
    }

    private fun draft(slug: String, name: String, sets: Int) = SavedWorkoutExerciseDraft(
        catalogSlug = slug,
        canonicalExerciseKey = "catalog:$slug",
        exerciseNameSnapshot = name,
        targetSets = sets,
        targetMinReps = 6,
        targetMaxReps = 10,
    )

    private fun openDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
