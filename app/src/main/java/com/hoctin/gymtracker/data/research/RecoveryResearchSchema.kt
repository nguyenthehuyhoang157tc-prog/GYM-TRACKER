package com.hoctin.gymtracker.data.research

import androidx.sqlite.db.SupportSQLiteDatabase

/** Manual v8 -> v9 SQL plus immutable-payload triggers used by upgrades and fresh installs. */
object RecoveryResearchSchema {
    fun createTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_consent` (
                `subjectId` TEXT NOT NULL,
                `stateCode` TEXT NOT NULL,
                `contractVersion` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `grantedAtMillis` INTEGER,
                `revokedAtMillis` INTEGER,
                PRIMARY KEY(`subjectId`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_predictions` (
                `predictionId` TEXT NOT NULL,
                `targetWorkoutAttemptId` TEXT NOT NULL,
                `subjectId` TEXT NOT NULL,
                `sourceCode` TEXT NOT NULL,
                `workoutCreatedAtMillis` INTEGER NOT NULL,
                `workoutStartedAtMillis` INTEGER NOT NULL,
                `predictionGeneratedAtMillis` INTEGER NOT NULL,
                `inputDataCutoffAtMillis` INTEGER NOT NULL,
                `frozenAtMillis` INTEGER NOT NULL,
                `recoveryModelVersion` TEXT NOT NULL,
                `muscleHeatModelVersion` TEXT NOT NULL,
                `progressiveOverloadModelVersion` TEXT NOT NULL,
                `researchContractVersion` TEXT NOT NULL,
                `researchSchemaVersion` INTEGER NOT NULL,
                `firstWorkingSetRuleVersion` TEXT NOT NULL,
                `catalogContentVersion` INTEGER NOT NULL,
                `planDayIdSnapshot` INTEGER,
                `planDayNameSnapshot` TEXT NOT NULL,
                `overallReadiness` INTEGER NOT NULL,
                `overallStatusCode` TEXT NOT NULL,
                `inputSessionIdsJson` TEXT NOT NULL,
                `inputSessionMaxTimestampMillis` INTEGER,
                `inputDigestSha256` TEXT NOT NULL,
                `dataCompletenessCode` TEXT NOT NULL,
                `abstained` INTEGER NOT NULL,
                `abstentionReasonCode` TEXT,
                `payloadSha256` TEXT NOT NULL,
                PRIMARY KEY(`predictionId`),
                FOREIGN KEY(`subjectId`) REFERENCES `recovery_research_consent`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_prediction_groups` (
                `predictionId` TEXT NOT NULL,
                `groupCode` TEXT NOT NULL,
                `readiness` INTEGER NOT NULL,
                `statusCode` TEXT NOT NULL,
                `trained` INTEGER NOT NULL,
                PRIMARY KEY(`predictionId`, `groupCode`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_prediction_muscles` (
                `predictionId` TEXT NOT NULL,
                `muscleCode` TEXT NOT NULL,
                `readiness` INTEGER NOT NULL,
                `fatigueScore` REAL NOT NULL,
                `acuteResidual` REAL NOT NULL,
                `structuralResidual` REAL NOT NULL,
                `statusCode` TEXT NOT NULL,
                `colorSemanticCode` TEXT NOT NULL,
                `soreWindow` INTEGER NOT NULL,
                `grindCarried` INTEGER NOT NULL,
                `lastTrainedAtMillis` INTEGER,
                PRIMARY KEY(`predictionId`, `muscleCode`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_target_exercises` (
                `targetExerciseId` TEXT NOT NULL,
                `predictionId` TEXT NOT NULL,
                `targetOrdinal` INTEGER NOT NULL,
                `planExerciseIdSnapshot` INTEGER,
                `exerciseNameSnapshot` TEXT NOT NULL,
                `canonicalExerciseKey` TEXT NOT NULL,
                `trackingTypeCode` TEXT NOT NULL,
                `targetSetsSnapshot` INTEGER NOT NULL,
                `targetMinRepsSnapshot` INTEGER NOT NULL,
                `targetMaxRepsSnapshot` INTEGER NOT NULL,
                `primaryMuscleCodesJson` TEXT NOT NULL,
                `secondaryMuscleCodesJson` TEXT NOT NULL,
                `primaryReadinessMin` INTEGER,
                `primaryReadinessMean` REAL,
                `secondaryReadinessMin` INTEGER,
                `suggestedLoadKg` REAL,
                `suggestedMinReps` INTEGER,
                `suggestedMaxReps` INTEGER,
                `suggestedRir` INTEGER,
                `suggestionActionCode` TEXT,
                `progressionTypeCode` TEXT,
                `suggestionStrategyCode` TEXT,
                `suggestionTargetText` TEXT,
                `suggestionReasonText` TEXT,
                `suggestionTitleText` TEXT,
                `suggestionDetailText` TEXT,
                `suggestionConfidence` INTEGER,
                `sessionQualityCode` TEXT,
                `coachInsightCode` TEXT,
                `referenceSessionId` INTEGER,
                `referenceSessionStartedAtMillis` INTEGER,
                `referenceSetNumber` INTEGER,
                `referenceLoadKg` REAL,
                `referenceReps` INTEGER,
                `referenceRir` INTEGER,
                `referenceDurationSeconds` INTEGER,
                `referenceDistanceMeters` REAL,
                `referencePerformanceValue` REAL,
                `dataCompletenessCode` TEXT NOT NULL,
                `abstained` INTEGER NOT NULL,
                `abstentionReasonCode` TEXT,
                PRIMARY KEY(`targetExerciseId`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_set_observations` (
                `setObservationId` TEXT NOT NULL,
                `predictionId` TEXT NOT NULL,
                `targetWorkoutAttemptId` TEXT NOT NULL,
                `planExerciseIdSnapshot` INTEGER NOT NULL,
                `canonicalExerciseKey` TEXT NOT NULL,
                `draftSetKey` TEXT NOT NULL,
                `draftSetOrdinal` INTEGER NOT NULL,
                `firstValidAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`setObservationId`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_outcomes` (
                `outcomeId` TEXT NOT NULL,
                `predictionId` TEXT NOT NULL,
                `targetWorkoutAttemptId` TEXT NOT NULL,
                `sessionId` INTEGER,
                `statusCode` TEXT NOT NULL,
                `statusReasonCode` TEXT,
                `workoutCompletedAtMillis` INTEGER,
                `workoutAbandonedAtMillis` INTEGER,
                `firstValidSetObservedAtMillis` INTEGER,
                `firstWorkingSetLoggedAtMillis` INTEGER,
                `outcomeCapturedAtMillis` INTEGER,
                `dataCompletenessCode` TEXT NOT NULL,
                `validForEvaluation` INTEGER NOT NULL,
                `exclusionReasonCode` TEXT,
                PRIMARY KEY(`outcomeId`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recovery_research_exercise_outcomes` (
                `exerciseOutcomeId` TEXT NOT NULL,
                `outcomeId` TEXT NOT NULL,
                `targetExerciseId` TEXT NOT NULL,
                `canonicalExerciseKey` TEXT NOT NULL,
                `exerciseNameSnapshot` TEXT NOT NULL,
                `exerciseSequence` INTEGER NOT NULL,
                `trackingTypeCode` TEXT NOT NULL,
                `firstWorkingSetId` INTEGER,
                `firstWorkingSetSetNumber` INTEGER,
                `firstWorkingSetRuleVersion` TEXT NOT NULL,
                `firstWorkingSetObservedAtMillis` INTEGER,
                `achievedLoadKg` REAL,
                `achievedReps` INTEGER,
                `achievedRir` INTEGER,
                `achievedDurationSeconds` INTEGER,
                `achievedDistanceMeters` REAL,
                `suggestedLoadDeltaKg` REAL,
                `suggestedRepsDelta` INTEGER,
                `loadReduced` INTEGER,
                `repsUnexpectedlyUnderperformed` INTEGER,
                `completedValidSets` INTEGER NOT NULL,
                `targetSetsSnapshot` INTEGER NOT NULL,
                `exerciseCompleted` INTEGER NOT NULL,
                `sessionQualityCode` TEXT,
                `matchedPlannedTarget` INTEGER NOT NULL,
                `firstWorkingSetConfidenceCode` TEXT NOT NULL,
                `validForEvaluation` INTEGER NOT NULL,
                `exclusionReasonCode` TEXT,
                `outcomeCapturedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`exerciseOutcomeId`),
                FOREIGN KEY(`outcomeId`) REFERENCES `recovery_research_outcomes`(`outcomeId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`targetExerciseId`) REFERENCES `recovery_research_target_exercises`(`targetExerciseId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        val indexes = listOf(
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_consent_stateCode` ON `recovery_research_consent` (`stateCode`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_predictions_targetWorkoutAttemptId` ON `recovery_research_predictions` (`targetWorkoutAttemptId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_predictions_subjectId` ON `recovery_research_predictions` (`subjectId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_predictions_predictionGeneratedAtMillis` ON `recovery_research_predictions` (`predictionGeneratedAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_predictions_inputDataCutoffAtMillis` ON `recovery_research_predictions` (`inputDataCutoffAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_predictions_recoveryModelVersion_predictionGeneratedAtMillis` ON `recovery_research_predictions` (`recoveryModelVersion`, `predictionGeneratedAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_prediction_groups_groupCode` ON `recovery_research_prediction_groups` (`groupCode`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_prediction_muscles_muscleCode` ON `recovery_research_prediction_muscles` (`muscleCode`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_prediction_muscles_lastTrainedAtMillis` ON `recovery_research_prediction_muscles` (`lastTrainedAtMillis`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_target_exercises_predictionId_targetOrdinal` ON `recovery_research_target_exercises` (`predictionId`, `targetOrdinal`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_target_exercises_canonicalExerciseKey` ON `recovery_research_target_exercises` (`canonicalExerciseKey`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_target_exercises_planExerciseIdSnapshot` ON `recovery_research_target_exercises` (`planExerciseIdSnapshot`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_target_exercises_referenceSessionId` ON `recovery_research_target_exercises` (`referenceSessionId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_set_observations_predictionId_planExerciseIdSnapshot_draftSetKey` ON `recovery_research_set_observations` (`predictionId`, `planExerciseIdSnapshot`, `draftSetKey`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_set_observations_targetWorkoutAttemptId` ON `recovery_research_set_observations` (`targetWorkoutAttemptId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_set_observations_canonicalExerciseKey` ON `recovery_research_set_observations` (`canonicalExerciseKey`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_set_observations_firstValidAtMillis` ON `recovery_research_set_observations` (`firstValidAtMillis`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_outcomes_predictionId` ON `recovery_research_outcomes` (`predictionId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_outcomes_targetWorkoutAttemptId` ON `recovery_research_outcomes` (`targetWorkoutAttemptId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recovery_research_outcomes_sessionId` ON `recovery_research_outcomes` (`sessionId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_outcomes_statusCode` ON `recovery_research_outcomes` (`statusCode`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_outcomes_outcomeCapturedAtMillis` ON `recovery_research_outcomes` (`outcomeCapturedAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_exercise_outcomes_outcomeId` ON `recovery_research_exercise_outcomes` (`outcomeId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_exercise_outcomes_targetExerciseId` ON `recovery_research_exercise_outcomes` (`targetExerciseId`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_exercise_outcomes_canonicalExerciseKey` ON `recovery_research_exercise_outcomes` (`canonicalExerciseKey`)",
            "CREATE INDEX IF NOT EXISTS `index_recovery_research_exercise_outcomes_firstWorkingSetId` ON `recovery_research_exercise_outcomes` (`firstWorkingSetId`)",
        )
        indexes.forEach(db::execSQL)
        createTriggers(db)
    }

    fun createTriggers(db: SupportSQLiteDatabase) {
        val immutableTables = listOf(
            "recovery_research_predictions",
            "recovery_research_prediction_groups",
            "recovery_research_prediction_muscles",
            "recovery_research_target_exercises",
            "recovery_research_set_observations",
            "recovery_research_exercise_outcomes",
        )
        immutableTables.forEach { table ->
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS `prevent_update_$table` " +
                    "BEFORE UPDATE ON `$table` BEGIN SELECT RAISE(ABORT, 'immutable research payload'); END",
            )
        }
    }
}
