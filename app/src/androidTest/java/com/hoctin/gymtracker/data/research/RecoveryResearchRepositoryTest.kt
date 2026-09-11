package com.hoctin.gymtracker.data.research

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.ExerciseLogInput
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.SetInput
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import com.hoctin.gymtracker.data.wearable.WearableShadowSchema
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecoveryResearchRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "wearable1a-repository-${System.nanoTime()}.db"
    private val time = AtomicLong(1_800_000_000_100L)
    private lateinit var db: AppDatabase
    private lateinit var gym: GymRepository
    private lateinit var catalog: ExerciseCatalogRepository
    private lateinit var research: RecoveryResearchRepository
    private lateinit var draft: WorkoutDraftRepository

    @Before
    fun setUp() = runBlocking {
        db = openDatabase()
        wireRepositories()
        catalog.ensureSeeded()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized && db.isOpen) db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun frozenPrediction_survivesDatabaseAndRepositoryRecreation_andLinksCompletedPartialAbandoned() = runBlocking {
        val dayId = db.gymDao().insertWorkoutDay(WorkoutDayEntity(name = "Research Push", sortOrder = 0))
        draft.clearDraft(dayId)
        val exerciseId = db.gymDao().insertExercise(
            ExerciseEntity(dayId = dayId, name = "Barbell Bench Press - Medium Grip", targetSets = 3, targetMinReps = 6, targetMaxReps = 8, sortOrder = 0),
        )
        val day = db.gymDao().getWorkoutDay(dayId)!!
        val exercise = db.gymDao().getExercise(exerciseId)!!
        val canonicalKey = catalog.canonicalKeyFor(exercise.name)

        // Historical baseline exists before the prospective cutoff.
        gym.saveWorkoutSession(
            day = day,
            inputs = listOf(
                ExerciseLogInput(
                    exercise,
                    listOf(SetInput(60.0, 8, 2, null), SetInput(60.0, 8, 2, null)),
                    canonicalKey,
                ),
            ),
            readiness = Readiness.NORMAL,
        )
        research.setConsent(true)
        val attempt = draft.loadOrCreateAttempt(dayId, CUTOFF - 100)
        val request = request(attempt, day, exercise)

        val repeated = listOf(
            async { research.freezePredictionIfEligible(request)!! },
            async { research.freezePredictionIfEligible(request)!! },
        ).awaitAll()
        assertEquals(repeated[0].predictionId, repeated[1].predictionId)
        assertEquals(1, db.recoveryResearchDao().getAllPredictions().size)
        val original = repeated[0]
        val originalTarget = db.recoveryResearchDao().getTargetExercises(original.predictionId).single()
        assertFalse(originalTarget.abstained)

        // Close the entire Room graph and recreate every repository. SharedPreferences restores attempt.
        db.close()
        db = openDatabase()
        wireRepositories()
        catalog.ensureSeeded()
        val restoredAttempt = WorkoutDraftRepository(context).loadOrCreateAttempt(dayId)
        assertEquals(attempt.attemptId, restoredAttempt.attemptId)
        val restored = research.freezePredictionIfEligible(request(restoredAttempt, day, exercise))!!
        assertEquals(original.predictionId, restored.predictionId)
        assertEquals(original.payloadSha256, restored.payloadSha256)
        assertEquals(1, db.recoveryResearchDao().getAllPredictions().size)

        research.recordFirstValidSet(restoredAttempt.attemptId, exerciseId, canonicalKey, "completed-row-1", 1, CUTOFF + 200)
        val completedSession = research.saveWorkoutAndCaptureOutcome(
            attemptId = restoredAttempt.attemptId,
            day = day,
            inputs = listOf(
                ExerciseLogInput(
                    exercise,
                    listOf(
                        SetInput(62.5, 8, 2, null, sourceDraftOrdinal = 1, sourceDraftKey = "completed-row-1"),
                        SetInput(62.5, 7, 1, null, sourceDraftOrdinal = 2, sourceDraftKey = "completed-row-2"),
                        SetInput(62.5, 6, 1, null, sourceDraftOrdinal = 3, sourceDraftKey = "completed-row-3"),
                    ),
                    canonicalKey,
                ),
            ),
            readiness = Readiness.STRONG,
            completedAtMillis = CUTOFF + 500,
        )
        val completed = db.recoveryResearchDao().getOutcomeForPrediction(original.predictionId)!!
        assertEquals(completedSession, completed.sessionId)
        assertEquals(ResearchOutcomeStatus.COMPLETED.name, completed.statusCode)
        assertTrue(completed.validForEvaluation)
        val completedExercise = db.recoveryResearchDao().getExerciseOutcomes(completed.outcomeId).single()
        assertTrue(completedExercise.exerciseCompleted)
        assertTrue(completedExercise.validForEvaluation)
        assertNotNull(completedExercise.sessionQualityCode)

        // Production plan mutation and a fresh readiness computation cannot rewrite the snapshot.
        db.gymDao().updateExercise(exercise.copy(name = "Renamed after freeze", targetSets = 1))
        val unchanged = db.recoveryResearchDao().getPrediction(original.predictionId)!!
        val unchangedTarget = db.recoveryResearchDao().getTargetExercises(original.predictionId).single()
        assertEquals(original.payloadSha256, unchanged.payloadSha256)
        assertEquals("Barbell Bench Press - Medium Grip", unchangedTarget.exerciseNameSnapshot)
        assertEquals(3, unchangedTarget.targetSetsSnapshot)
        assertThrows(SQLiteException::class.java) {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE recovery_research_predictions SET overallReadiness=0 WHERE predictionId='${original.predictionId}'",
            )
        }

        val export1 = research.exportDeterministicJson()
        val export2 = research.exportDeterministicJson()
        assertEquals(export1, export2)
        assertTrue(export1.contains(original.predictionId))
        assertTrue(export1.contains(completed.outcomeId))
        assertTrue(export1.contains(RecoveryResearchVersions.RECOVERY_MODEL))

        // Partial save gets a distinct prediction and remains evaluable, not mislabelled completed.
        draft.clearDraft(dayId)
        val partialAttempt = draft.loadOrCreateAttempt(dayId, CUTOFF + 600)
        val snapshotExercise = exercise.copy(name = "Barbell Bench Press - Medium Grip", targetSets = 3)
        val partialPrediction = research.freezePredictionIfEligible(request(partialAttempt, day, snapshotExercise))!!
        research.recordFirstValidSet(partialAttempt.attemptId, exerciseId, canonicalKey, "partial-row-1", 1, CUTOFF + 700)
        research.saveWorkoutAndCaptureOutcome(
            partialAttempt.attemptId,
            day,
            listOf(
                ExerciseLogInput(
                    snapshotExercise,
                    listOf(SetInput(60.0, 6, 1, null, sourceDraftOrdinal = 1, sourceDraftKey = "partial-row-1")),
                    canonicalKey,
                ),
            ),
            completedAtMillis = CUTOFF + 800,
        )
        val partial = db.recoveryResearchDao().getOutcomeForPrediction(partialPrediction.predictionId)!!
        assertEquals(ResearchOutcomeStatus.PARTIAL.name, partial.statusCode)
        assertTrue(partial.validForEvaluation)

        // Explicit discard closes ACTIVE once; it is retained and excluded, never called recovery failure.
        draft.clearDraft(dayId)
        val abandonedAttempt = draft.loadOrCreateAttempt(dayId, CUTOFF + 900)
        val abandonedPrediction = research.freezePredictionIfEligible(request(abandonedAttempt, day, snapshotExercise))!!
        research.abandonAttempt(abandonedAttempt.attemptId, CUTOFF + 1_000)
        research.abandonAttempt(abandonedAttempt.attemptId, CUTOFF + 1_001)
        val abandoned = db.recoveryResearchDao().getOutcomeForPrediction(abandonedPrediction.predictionId)!!
        assertEquals(ResearchOutcomeStatus.ABANDONED.name, abandoned.statusCode)
        assertEquals(ResearchReason.USER_CONFIRMED_DISCARD.name, abandoned.statusReasonCode)
        assertFalse(abandoned.validForEvaluation)
        assertEquals(ResearchReason.SESSION_INTERRUPTED.name, abandoned.exclusionReasonCode)

        draft.clearDraft(dayId)
    }

    private suspend fun wireRepositories() {
        val gymDao = db.gymDao()
        gym = GymRepository(gymDao, db)
        catalog = ExerciseCatalogRepository(db.catalogDao(), context)
        research = RecoveryResearchRepository(
            database = db,
            dao = db.recoveryResearchDao(),
            gymDao = gymDao,
            gymRepository = gym,
            catalogRepository = catalog,
            clock = { time.incrementAndGet() },
            idFactory = ResearchIdFactory { "test-${time.incrementAndGet()}" },
        )
        draft = WorkoutDraftRepository(context)
    }

    private fun openDatabase(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
        )
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    RecoveryResearchSchema.createTriggers(db)
                    WearableShadowSchema.createTriggers(db)
                }
            },
        )
        .build()

    private fun request(
        attempt: com.hoctin.gymtracker.data.WorkoutAttemptIdentity,
        day: WorkoutDayEntity,
        exercise: ExerciseEntity,
    ) = RecoveryResearchFreezeRequest(
        attempt = attempt,
        day = day,
        targetExercises = listOf(exercise),
        allPlanExercises = listOf(exercise),
        goalSelection = WorkoutGoalSelection.HYPERTROPHY,
        inputDataCutoffAtMillis = maxOf(CUTOFF, attempt.startedAtMillis),
    )

    private companion object {
        const val CUTOFF = 1_800_000_000_000L
    }
}
