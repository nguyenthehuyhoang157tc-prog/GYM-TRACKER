package com.hoctin.gymtracker.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate8To9_preservesProductionRows_andCreatesEmptyValidatedResearchGraph() {
        helper.createDatabase(DB_NAME, 8).apply {
            execSQL("INSERT INTO workout_days(id,name,sortOrder) VALUES(1,'Push',0)")
            execSQL("INSERT INTO exercises(id,dayId,name,targetSets,targetMinReps,targetMaxReps,sortOrder) VALUES(11,1,'Bench Press',3,6,8,0)")
            execSQL(
                "INSERT INTO workout_sessions(id,dayId,dayNameSnapshot,dateEpochDay,startedAtMillis,notes,readiness,durationSeconds,customTitle) " +
                    "VALUES(21,1,'Push',20000,1700000000000,'kept','STRONG',3600,'Heavy day')",
            )
            execSQL(
                "INSERT INTO logged_sets(id,sessionId,exerciseId,exerciseNameSnapshot,setNumber,weight,reps,rir,notes,isPrWeight,isPrReps,durationSeconds,distanceMeters,canonicalExerciseKey) " +
                    "VALUES(31,21,11,'Bench Press',1,60.0,8,2,'kept set',1,1,NULL,NULL,'catalog:bench-press')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 9, true, AppDatabase.MIGRATION_8_9)
        db.query("SELECT customTitle, durationSeconds, readiness FROM workout_sessions WHERE id=21").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Heavy day", cursor.getString(0))
            assertEquals(3600, cursor.getInt(1))
            assertEquals("STRONG", cursor.getString(2))
        }
        db.query("SELECT canonicalExerciseKey, weight, reps FROM logged_sets WHERE id=31").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("catalog:bench-press", cursor.getString(0))
            assertEquals(60.0, cursor.getDouble(1), 0.0)
            assertEquals(8, cursor.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM recovery_research_predictions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_recovery_research_predictions_targetWorkoutAttemptId'",
        ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name='prevent_update_recovery_research_predictions'",
        ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        db.close()
    }

    @Test
    fun migrate9To10_preservesFrozenResearchGraph_andCreatesSealedWearableGraph() {
        helper.createDatabase(DB_NAME_2A, 9).apply {
            execSQL(
                "INSERT INTO recovery_research_consent(subjectId,stateCode,contractVersion,createdAtMillis,grantedAtMillis,revokedAtMillis) " +
                    "VALUES('local-device-subject','GRANTED','WEARABLE_1A_V1',1000,1000,NULL)",
            )
            execSQL(
                "INSERT INTO recovery_research_predictions(" +
                    "predictionId,targetWorkoutAttemptId,subjectId,sourceCode,workoutCreatedAtMillis," +
                    "workoutStartedAtMillis,predictionGeneratedAtMillis,inputDataCutoffAtMillis,frozenAtMillis," +
                    "recoveryModelVersion,muscleHeatModelVersion,progressiveOverloadModelVersion," +
                    "researchContractVersion,researchSchemaVersion,firstWorkingSetRuleVersion," +
                    "catalogContentVersion,planDayIdSnapshot,planDayNameSnapshot,overallReadiness," +
                    "overallStatusCode,inputSessionIdsJson,inputSessionMaxTimestampMillis,inputDigestSha256," +
                    "dataCompletenessCode,abstained,abstentionReasonCode,payloadSha256) VALUES(" +
                    "'prediction-kept','attempt-kept','local-device-subject','PRE_FIRST_VALID_WORKING_SET'," +
                    "900,900,1000,1000,1000,'MUSCLE_4B_BASELINE','MUSCLE_HEAT_1_STIMULUS_QUALITY'," +
                    "'PROGRESSIVE_OVERLOAD_V3_11D','WEARABLE_1A_V1',1,'TRACKING_AWARE_FIRST_WORKING_SET_V1'," +
                    "1,NULL,'Push',88,'READY','[]',NULL,'digest','NONE',0,NULL,'payload')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB_NAME_2A,
            10,
            true,
            AppDatabase.MIGRATION_9_10,
        )
        db.query("SELECT payloadSha256 FROM recovery_research_predictions WHERE predictionId='prediction-kept'")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("payload", cursor.getString(0))
            }
        db.query("SELECT COUNT(*) FROM wearable_shadow_record_revisions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='trigger' AND name='prevent_late_wearable_context_snapshot_items'",
        ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        db.close()
    }

    @Test
    fun migrate10To11_preservesPlanHistoryCanonicalAndWearable_addsSavedWorkoutContract() {
        helper.createDatabase(DB_NAME_PLAN_V3A, 10).apply {
            execSQL("INSERT INTO workout_days(id,name,sortOrder) VALUES(1,'Push',0)")
            execSQL(
                "INSERT INTO exercises(id,dayId,name,targetSets,targetMinReps,targetMaxReps,sortOrder) " +
                    "VALUES(11,1,'Bench Press',3,6,8,0)",
            )
            execSQL(
                "INSERT INTO workout_sessions(id,dayId,dayNameSnapshot,dateEpochDay,startedAtMillis," +
                    "notes,readiness,durationSeconds,customTitle) " +
                    "VALUES(21,1,'Push',20000,1700000000000,'kept','STRONG',3600,'Heavy day')",
            )
            execSQL(
                "INSERT INTO logged_sets(id,sessionId,exerciseId,exerciseNameSnapshot,setNumber,weight," +
                    "reps,rir,notes,isPrWeight,isPrReps,durationSeconds,distanceMeters,canonicalExerciseKey) " +
                    "VALUES(31,21,11,'Bench Press',1,60.0,8,2,'kept set',1,1,NULL,NULL," +
                    "'catalog:bench-press')",
            )
            execSQL(
                "INSERT INTO recovery_research_consent(subjectId,stateCode,contractVersion,createdAtMillis," +
                    "grantedAtMillis,revokedAtMillis) VALUES('local-device-subject','GRANTED'," +
                    "'WEARABLE_1A_V1',1000,1000,NULL)",
            )
            execSQL(
                "INSERT INTO wearable_shadow_settings(subjectId,contractVersion,schemaVersion,syncEnabled," +
                    "lastAvailabilityCode,grantedTypeMask,createdAtMillis,updatedAtMillis) VALUES(" +
                    "'local-device-subject','WEARABLE_2A_SHADOW_CONTEXT_V1',1,1,'AVAILABLE'," +
                    "'SLEEP_SESSION',1000,2000)",
            )
            execSQL(
                "INSERT INTO recovery_research_predictions(" +
                    "predictionId,targetWorkoutAttemptId,subjectId,sourceCode,workoutCreatedAtMillis," +
                    "workoutStartedAtMillis,predictionGeneratedAtMillis,inputDataCutoffAtMillis,frozenAtMillis," +
                    "recoveryModelVersion,muscleHeatModelVersion,progressiveOverloadModelVersion," +
                    "researchContractVersion,researchSchemaVersion,firstWorkingSetRuleVersion," +
                    "catalogContentVersion,planDayIdSnapshot,planDayNameSnapshot,overallReadiness," +
                    "overallStatusCode,inputSessionIdsJson,inputSessionMaxTimestampMillis,inputDigestSha256," +
                    "dataCompletenessCode,abstained,abstentionReasonCode,payloadSha256) VALUES(" +
                    "'prediction-v10','attempt-v10','local-device-subject','PRE_FIRST_VALID_WORKING_SET'," +
                    "900,900,1000,1000,1000,'MUSCLE_4B_BASELINE','MUSCLE_HEAT_1_STIMULUS_QUALITY'," +
                    "'PROGRESSIVE_OVERLOAD_V3_11D','WEARABLE_1A_V1',1," +
                    "'TRACKING_AWARE_FIRST_WORKING_SET_V1',16,1,'Push',88,'READY','[21]'," +
                    "1700000000000,'research-digest','NONE',0,NULL,'research-payload')",
            )
            execSQL(
                "INSERT INTO wearable_shadow_record_revisions(" +
                    "revisionId,subjectId,contractVersion,schemaVersion,recordTypeCode," +
                    "healthConnectRecordId,lifecycleEpoch,dataOriginPackageName,sourceLastModifiedAtMillis," +
                    "clientRecordVersion,recordingMethod,payloadHash,measurementTimeMillis," +
                    "effectiveTimeMillis,metricValue,metricUnitCode,firstSeenAtMillis,lastSeenAtMillis) VALUES(" +
                    "'wearable-revision-v10','local-device-subject','WEARABLE_2A_SHADOW_CONTEXT_V1',1," +
                    "'RESTING_HEART_RATE','hc-record-v10',1,'com.samsung.health',1900,0,1," +
                    "'wearable-payload',1800,1800,58.0,'BPM',1900,2000)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            DB_NAME_PLAN_V3A,
            11,
            true,
            AppDatabase.MIGRATION_10_11,
        )
        // MigrationTestHelper exposes the raw SQLite connection; mirror Room's runtime FK setting
        // before exercising ON DELETE behavior.
        db.execSQL("PRAGMA foreign_keys=ON")
        db.query("SELECT sourceType,savedWorkoutId,customTitle FROM workout_sessions WHERE id=21").use {
            assertTrue(it.moveToFirst())
            assertEquals("ROUTINE", it.getString(0))
            assertTrue(it.isNull(1))
            assertEquals("Heavy day", it.getString(2))
        }
        db.query("SELECT canonicalExerciseKey FROM logged_sets WHERE id=31").use {
            assertTrue(it.moveToFirst())
            assertEquals("catalog:bench-press", it.getString(0))
        }
        db.query("SELECT grantedTypeMask FROM wearable_shadow_settings WHERE subjectId='local-device-subject'")
            .use {
                assertTrue(it.moveToFirst())
                assertEquals("SLEEP_SESSION", it.getString(0))
            }
        db.query("SELECT payloadSha256 FROM recovery_research_predictions WHERE predictionId='prediction-v10'")
            .use {
                assertTrue(it.moveToFirst())
                assertEquals("research-payload", it.getString(0))
            }
        db.query("SELECT payloadHash,metricValue FROM wearable_shadow_record_revisions WHERE revisionId='wearable-revision-v10'")
            .use {
                assertTrue(it.moveToFirst())
                assertEquals("wearable-payload", it.getString(0))
                assertEquals(58.0, it.getDouble(1), 0.0)
            }
        db.execSQL(
            "INSERT INTO saved_workouts(id,name,createdAtMillis,updatedAtMillis,sortOrder) " +
                "VALUES(41,'Hotel workout',3000,3000,0)",
        )
        db.execSQL(
            "INSERT INTO saved_workout_exercises(id,savedWorkoutId,catalogSlug,canonicalExerciseKey," +
                "exerciseNameSnapshot,targetSets,targetMinReps,targetMaxReps,sortOrder) VALUES(" +
                "51,41,'bench-press','catalog:bench-press','Bench Press',3,6,8,0)",
        )
        db.execSQL(
            "INSERT INTO workout_sessions(id,dayId,dayNameSnapshot,dateEpochDay,startedAtMillis,notes," +
                "readiness,durationSeconds,customTitle,sourceType,savedWorkoutId) VALUES(" +
                "61,NULL,'Hotel workout',20001,1700086400000,NULL,NULL,1800,NULL,'SAVED_ONE_OFF',41)",
        )
        db.execSQL("DELETE FROM saved_workouts WHERE id=41")
        db.query("SELECT sourceType,savedWorkoutId FROM workout_sessions WHERE id=61").use {
            assertTrue(it.moveToFirst())
            assertEquals("SAVED_ONE_OFF", it.getString(0))
            assertEquals(41L, it.getLong(1))
        }
        db.query("SELECT COUNT(*) FROM saved_workout_exercises WHERE savedWorkoutId=41").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
        db.close()
    }

    private companion object {
        const val DB_NAME = "wearable1a-migration-test"
        const val DB_NAME_2A = "wearable2a-migration-test"
        const val DB_NAME_PLAN_V3A = "plan-v3a-migration-test"
    }
}
