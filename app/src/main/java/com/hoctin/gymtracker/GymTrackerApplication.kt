package com.hoctin.gymtracker

import android.app.Application
import android.content.Context
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.CanonicalKeyBackfill
import com.hoctin.gymtracker.data.GymDao
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.SavedWorkoutRepository
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.WorkoutDraftRepository
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.PlanNameMigration
import com.hoctin.gymtracker.data.research.RecoveryResearchRepository
import com.hoctin.gymtracker.data.wearable.AndroidHealthConnectGateway
import com.hoctin.gymtracker.data.wearable.WearableShadowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymTrackerApplication : Application() {
    lateinit var repository: GymRepository
        private set
    lateinit var savedWorkoutRepository: SavedWorkoutRepository
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var workoutDraftRepository: WorkoutDraftRepository
        private set
    // DB-0: Exercise Catalog reference library (separate from the Plan / workout tables).
    lateinit var catalogRepository: ExerciseCatalogRepository
        private set
    lateinit var recoveryResearchRepository: RecoveryResearchRepository
        private set
    lateinit var wearableShadowRepository: WearableShadowRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        val gymDao = database.gymDao()
        repository = GymRepository(gymDao, database)
        savedWorkoutRepository = SavedWorkoutRepository(gymDao, database)
        preferencesRepository = UserPreferencesRepository(this)
        workoutDraftRepository = WorkoutDraftRepository(this)
        catalogRepository = ExerciseCatalogRepository(database.catalogDao(), this)
        recoveryResearchRepository = RecoveryResearchRepository(
            database = database,
            dao = database.recoveryResearchDao(),
            gymDao = gymDao,
            gymRepository = repository,
            catalogRepository = catalogRepository,
        )
        wearableShadowRepository = WearableShadowRepository(
            database = database,
            dao = database.wearableShadowDao(),
            recoveryResearchDao = database.recoveryResearchDao(),
            gateway = AndroidHealthConnectGateway(this),
        )
        // Seed the FED-canonical catalog once (no-op when already populated at the current content
        // version). Reference data only — does not touch the Plan, workout sessions, or Import/Export.
        // Then run the one-time, flag-guarded FED name migration for active Plan rows (DB-4B.2).
        applicationScope.launch {
            catalogRepository.ensureSeeded()
            runFedPlanNameMigrationOnce(gymDao)
            // DB-PRE7B: backfill canonical exercise identity for pre-v6 logged sets (catalog must be
            // seeded first). One-time, flag-guarded, idempotent — only touches null/blank keys.
            runCanonicalKeyBackfillOnce()
        }
    }

    /**
     * DB-4B.2: renames active Plan exercise rows to FED-canonical names exactly once, guarded by a
     * persisted flag. Idempotent and History-safe (see [PlanNameMigration]).
     */
    private suspend fun runFedPlanNameMigrationOnce(gymDao: GymDao) {
        val prefs = getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PlanNameMigration.FLAG_KEY, false)) return
        PlanNameMigration.run(gymDao)
        prefs.edit().putBoolean(PlanNameMigration.FLAG_KEY, true).apply()
    }

    /**
     * DB-PRE7B: resolves [com.hoctin.gymtracker.data.LoggedSetEntity.canonicalExerciseKey] for all
     * pre-v6 logged sets exactly once, guarded by a persisted flag. Idempotent and History-safe (only
     * null/blank keys are written; display names are untouched).
     */
    private suspend fun runCanonicalKeyBackfillOnce() {
        val prefs = getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CanonicalKeyBackfill.FLAG_KEY, false)) return
        CanonicalKeyBackfill.run(repository, catalogRepository)
        prefs.edit().putBoolean(CanonicalKeyBackfill.FLAG_KEY, true).apply()
    }

    private companion object {
        const val MIGRATION_PREFS = "auron_migrations"
    }
}
