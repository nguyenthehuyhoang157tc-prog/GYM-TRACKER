package com.hoctin.gymtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hoctin.gymtracker.data.catalog.CatalogDao
import com.hoctin.gymtracker.data.catalog.CatalogExerciseEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchConsentEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchDao
import com.hoctin.gymtracker.data.research.RecoveryResearchExerciseOutcomeEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchOutcomeEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionGroupEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionMuscleEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchSchema
import com.hoctin.gymtracker.data.research.RecoveryResearchSetObservationEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchTargetExerciseEntity
import com.hoctin.gymtracker.data.wearable.WearableContextSnapshotEntity
import com.hoctin.gymtracker.data.wearable.WearableContextSnapshotItemEntity
import com.hoctin.gymtracker.data.wearable.WearableRecordRevisionEntity
import com.hoctin.gymtracker.data.wearable.WearableShadowAccessEventEntity
import com.hoctin.gymtracker.data.wearable.WearableShadowDao
import com.hoctin.gymtracker.data.wearable.WearableShadowSchema
import com.hoctin.gymtracker.data.wearable.WearableShadowSettingsEntity
import com.hoctin.gymtracker.data.wearable.WearableShadowSyncStateEntity
import com.hoctin.gymtracker.data.wearable.WearableSleepStageEntity

@Database(
    entities = [
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        LoggedSetEntity::class,
        SavedWorkoutEntity::class,
        SavedWorkoutExerciseEntity::class,
        // DB-0: Exercise Catalog reference table (v2). DB-1.5 adds detail columns (v3).
        CatalogExerciseEntity::class,
        // WEARABLE-1A: isolated, local-only recovery validation graph. No wearable data.
        RecoveryResearchConsentEntity::class,
        RecoveryResearchPredictionEntity::class,
        RecoveryResearchPredictionGroupEntity::class,
        RecoveryResearchPredictionMuscleEntity::class,
        RecoveryResearchTargetExerciseEntity::class,
        RecoveryResearchSetObservationEntity::class,
        RecoveryResearchOutcomeEntity::class,
        RecoveryResearchExerciseOutcomeEntity::class,
        // WEARABLE-2A: separate local-only Health Connect shadow context. It is not an input to
        // production readiness/Heat/PO and is not part of the frozen v1 research export.
        WearableShadowSettingsEntity::class,
        WearableShadowSyncStateEntity::class,
        WearableShadowAccessEventEntity::class,
        WearableRecordRevisionEntity::class,
        WearableSleepStageEntity::class,
        WearableContextSnapshotEntity::class,
        WearableContextSnapshotItemEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gymDao(): GymDao
    abstract fun catalogDao(): CatalogDao
    abstract fun recoveryResearchDao(): RecoveryResearchDao
    abstract fun wearableShadowDao(): WearableShadowDao

    companion object {
        /**
         * DB-0 additive migration: v1 → v2.
         *
         * Creates ONLY the new `catalog_exercises` reference table. It never touches the existing
         * workout/session/log tables, so all user plans and history are preserved on upgrade. No
         * `fallbackToDestructiveMigration` is used anywhere, so a missing/failed migration would surface
         * loudly rather than silently wiping data.
         *
         * The CREATE TABLE matches the Room-generated schema for [CatalogExerciseEntity] (all columns
         * NOT NULL, TEXT primary key `slug`); verified against the exported schema JSON (v2).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_exercises` (
                        `slug` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL,
                        `primaryMuscles` TEXT NOT NULL,
                        `secondaryMuscles` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `force` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `progressionType` TEXT NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultMinReps` INTEGER NOT NULL,
                        `defaultMaxReps` INTEGER NOT NULL,
                        `defaultIncrementKg` REAL NOT NULL,
                        `sessionsRequiredBeforeIncrease` INTEGER NOT NULL,
                        `isBodyweight` INTEGER NOT NULL,
                        `isUnilateral` INTEGER NOT NULL,
                        `aliases` TEXT NOT NULL,
                        PRIMARY KEY(`slug`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * DB-1.5 additive migration: v2 → v3.
         *
         * Adds the catalog detail columns (`instructions`, `tips`, `imageRefs`). Because the catalog is
         * **reference data** that is always re-seeded from the bundled content (see
         * [com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository.ensureSeeded]), this migration
         * simply DROP+CREATEs the `catalog_exercises` table with the new schema — far safer than a
         * column-by-column ALTER, and it cannot lose user data because it only touches the catalog table.
         * It never reads or writes the workout/session/log tables. No `fallbackToDestructiveMigration`.
         *
         * The CREATE TABLE matches the Room-generated v3 schema for [CatalogExerciseEntity]; verified
         * against the exported schema JSON (v3).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `catalog_exercises`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_exercises` (
                        `slug` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL,
                        `primaryMuscles` TEXT NOT NULL,
                        `secondaryMuscles` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `force` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `progressionType` TEXT NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultMinReps` INTEGER NOT NULL,
                        `defaultMaxReps` INTEGER NOT NULL,
                        `defaultIncrementKg` REAL NOT NULL,
                        `sessionsRequiredBeforeIncrease` INTEGER NOT NULL,
                        `isBodyweight` INTEGER NOT NULL,
                        `isUnilateral` INTEGER NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `tips` TEXT NOT NULL,
                        `imageRefs` TEXT NOT NULL,
                        PRIMARY KEY(`slug`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * V3.6A additive migration: v3 → v4.
         *
         * Adds a single nullable `readiness` TEXT column to the existing `workout_sessions` table for
         * the subjective how-did-today-feel signal. This is the first migration to touch a **user**
         * table, so it is intentionally the safest possible kind: `ALTER TABLE ... ADD COLUMN` adds the
         * column with a NULL default in place — **no row is read, moved, or deleted**, so every existing
         * session and its logged sets are preserved (old rows simply get `readiness = NULL` = unknown).
         * It does not touch the logged_sets / workout_days / exercises / catalog tables. No
         * `fallbackToDestructiveMigration` anywhere, so a failure surfaces loudly instead of wiping data.
         *
         * The resulting column (`readiness TEXT`, nullable) matches the Room-generated v4 schema for
         * [WorkoutSessionEntity] (`val readiness: String? = null`).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `readiness` TEXT")
            }
        }

        /**
         * DB-6B multi-tracking foundation: v4 → v5. Purely additive, no destructive fallback.
         *
         * 1. `logged_sets`: adds two **nullable** columns (`durationSeconds INTEGER`,
         *    `distanceMeters REAL`) via in-place `ALTER TABLE ADD COLUMN` — no row is read, moved, or
         *    deleted, so every existing session/set is preserved (old rows get NULL). Same proven shape
         *    as the [MIGRATION_3_4] readiness column.
         * 2. `catalog_exercises` (reference data, always re-seeded by `ensureSeeded`): DROP+CREATE with
         *    the added `trackingType TEXT NOT NULL` column — the same safe pattern as [MIGRATION_2_3]
         *    (it only touches the catalog table; the table is repopulated on next launch via the
         *    `CONTENT_VERSION 15` bump). The CREATE matches the Room-generated v5 schema for
         *    [CatalogExerciseEntity].
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `logged_sets` ADD COLUMN `durationSeconds` INTEGER")
                db.execSQL("ALTER TABLE `logged_sets` ADD COLUMN `distanceMeters` REAL")
                db.execSQL("DROP TABLE IF EXISTS `catalog_exercises`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalog_exercises` (
                        `slug` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL,
                        `primaryMuscles` TEXT NOT NULL,
                        `secondaryMuscles` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `force` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `progressionType` TEXT NOT NULL,
                        `defaultSets` INTEGER NOT NULL,
                        `defaultMinReps` INTEGER NOT NULL,
                        `defaultMaxReps` INTEGER NOT NULL,
                        `defaultIncrementKg` REAL NOT NULL,
                        `sessionsRequiredBeforeIncrease` INTEGER NOT NULL,
                        `isBodyweight` INTEGER NOT NULL,
                        `isUnilateral` INTEGER NOT NULL,
                        `aliases` TEXT NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `tips` TEXT NOT NULL,
                        `imageRefs` TEXT NOT NULL,
                        `trackingType` TEXT NOT NULL,
                        PRIMARY KEY(`slug`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * DB-PRE7B canonical exercise identity: v5 → v6. Purely additive, no destructive fallback.
         *
         * Adds a single **nullable** `canonicalExerciseKey TEXT` column to `logged_sets` via in-place
         * `ALTER TABLE ADD COLUMN` (no row is read, moved, or deleted — every existing session/set is
         * preserved with `canonicalExerciseKey = NULL` until the one-time backfill resolves it), plus the
         * matching `index_logged_sets_canonicalExerciseKey` index. Same proven shape as the readiness
         * (v3→v4) and duration/distance (v4→v5) additive columns. No `fallbackToDestructiveMigration`.
         *
         * The resulting column + index match the Room-generated v6 schema for [LoggedSetEntity]
         * (`val canonicalExerciseKey: String? = null` with `Index("canonicalExerciseKey")`).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `logged_sets` ADD COLUMN `canonicalExerciseKey` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_logged_sets_canonicalExerciseKey` " +
                        "ON `logged_sets` (`canonicalExerciseKey`)",
                )
            }
        }

        /**
         * MUSCLE-3B.1 additive migration: v6 → v7.
         *
         * Adds a single **nullable** `durationSeconds INTEGER` column to `workout_sessions` (wall-clock
         * workout duration captured at save) via in-place `ALTER TABLE ADD COLUMN` — no row is read,
         * moved, or deleted; every existing session keeps its data with `durationSeconds = NULL`
         * (= unknown, shown as no duration). Identical proven shape to the readiness (v3→v4) column.
         * No `fallbackToDestructiveMigration` anywhere.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `durationSeconds` INTEGER")
            }
        }

        /**
         * MUSCLE-3C additive migration: v7 -> v8.
         *
         * Adds nullable `customTitle TEXT` to `workout_sessions` for saved-session renames. Existing
         * rows stay NULL and keep displaying their historical `dayNameSnapshot`; no row is read, moved,
         * or deleted. No `fallbackToDestructiveMigration`.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `customTitle` TEXT")
            }
        }

        /**
         * WEARABLE-1A additive migration: v8 -> v9.
         *
         * Creates only isolated recovery-research tables, indexes, foreign keys inside that research
         * graph, and immutable-payload triggers. Existing plan, catalog, workout session, logged set,
         * custom title, duration, readiness, and canonical-key rows are not altered. There is no
         * destructive fallback and no research foreign key into mutable production/import tables.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                RecoveryResearchSchema.createTables(db)
            }
        }

        /**
         * WEARABLE-2A additive migration: v9 -> v10.
         *
         * Creates only the isolated wearable shadow/revision/snapshot graph. Existing production
         * tables and the eight frozen WEARABLE-1A collections are neither rewritten nor reinterpreted.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                WearableShadowSchema.createTables(db)
            }
        }

        /**
         * PLAN-V3A additive migration: v10 -> v11.
         *
         * Adds the isolated Saved One-Off template graph and typed session provenance. Every existing
         * session receives the conservative ROUTINE default. `savedWorkoutId` is deliberately not a
         * foreign key: deleting a template can never cascade into completed History. Existing plan,
         * catalog, research, and wearable tables are neither rewritten nor reinterpreted.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_workouts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_workout_exercises` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `savedWorkoutId` INTEGER NOT NULL,
                        `catalogSlug` TEXT,
                        `canonicalExerciseKey` TEXT NOT NULL,
                        `exerciseNameSnapshot` TEXT NOT NULL,
                        `targetSets` INTEGER NOT NULL,
                        `targetMinReps` INTEGER NOT NULL,
                        `targetMaxReps` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        FOREIGN KEY(`savedWorkoutId`) REFERENCES `saved_workouts`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_saved_workout_exercises_savedWorkoutId` " +
                        "ON `saved_workout_exercises` (`savedWorkoutId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_saved_workout_exercises_canonicalExerciseKey` " +
                        "ON `saved_workout_exercises` (`canonicalExerciseKey`)",
                )
                db.execSQL(
                    "ALTER TABLE `workout_sessions` ADD COLUMN `sourceType` " +
                        "TEXT NOT NULL DEFAULT 'ROUTINE'",
                )
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `savedWorkoutId` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_sourceType` " +
                        "ON `workout_sessions` (`sourceType`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_savedWorkoutId` " +
                        "ON `workout_sessions` (`savedWorkoutId`)",
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gym_tracker.db",
            )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11,
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
        }
    }
}
