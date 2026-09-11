package com.hoctin.gymtracker.data.wearable

import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive Room v9 -> v10 schema for local-only WEARABLE-2A context. */
object WearableShadowSchema {
    fun createTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_shadow_settings` (
                `subjectId` TEXT NOT NULL,
                `contractVersion` TEXT NOT NULL,
                `schemaVersion` INTEGER NOT NULL,
                `syncEnabled` INTEGER NOT NULL,
                `lastAvailabilityCode` TEXT NOT NULL,
                `grantedTypeMask` TEXT NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`subjectId`),
                FOREIGN KEY(`subjectId`) REFERENCES `recovery_research_consent`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_shadow_sync_state` (
                `subjectId` TEXT NOT NULL,
                `recordTypeCode` TEXT NOT NULL,
                `changesToken` TEXT,
                `initialSyncComplete` INTEGER NOT NULL,
                `permissionGranted` INTEGER NOT NULL,
                `statusCode` TEXT NOT NULL,
                `errorCode` TEXT NOT NULL,
                `lastAttemptAtMillis` INTEGER,
                `lastSuccessfulSyncAtMillis` INTEGER,
                `rejectedRecordCount` INTEGER NOT NULL,
                `updatedAtMillis` INTEGER NOT NULL,
                PRIMARY KEY(`subjectId`, `recordTypeCode`),
                FOREIGN KEY(`subjectId`) REFERENCES `recovery_research_consent`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_shadow_access_events` (
                `accessEventId` TEXT NOT NULL,
                `subjectId` TEXT NOT NULL,
                `occurredAtMillis` INTEGER NOT NULL,
                `syncEnabled` INTEGER NOT NULL,
                `availabilityCode` TEXT NOT NULL,
                `grantedTypeMask` TEXT NOT NULL,
                `reasonCode` TEXT NOT NULL,
                PRIMARY KEY(`accessEventId`),
                FOREIGN KEY(`subjectId`) REFERENCES `recovery_research_consent`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_shadow_record_revisions` (
                `revisionId` TEXT NOT NULL,
                `subjectId` TEXT NOT NULL,
                `contractVersion` TEXT NOT NULL,
                `schemaVersion` INTEGER NOT NULL,
                `recordTypeCode` TEXT NOT NULL,
                `healthConnectRecordId` TEXT NOT NULL,
                `lifecycleEpoch` INTEGER NOT NULL,
                `dataOriginPackageName` TEXT NOT NULL,
                `dataOriginDisplayName` TEXT,
                `sourceLastModifiedAtMillis` INTEGER NOT NULL,
                `clientRecordId` TEXT,
                `clientRecordVersion` INTEGER NOT NULL,
                `recordingMethod` INTEGER NOT NULL,
                `deviceType` INTEGER,
                `deviceManufacturer` TEXT,
                `deviceModel` TEXT,
                `payloadHash` TEXT NOT NULL,
                `startTimeMillis` INTEGER,
                `endTimeMillis` INTEGER,
                `measurementTimeMillis` INTEGER,
                `effectiveTimeMillis` INTEGER NOT NULL,
                `startZoneOffsetSeconds` INTEGER,
                `endZoneOffsetSeconds` INTEGER,
                `measurementZoneOffsetSeconds` INTEGER,
                `sessionDurationMillis` INTEGER,
                `knownSleepDurationMillis` INTEGER,
                `awakeDurationMillis` INTEGER,
                `lightDurationMillis` INTEGER,
                `deepDurationMillis` INTEGER,
                `remDurationMillis` INTEGER,
                `unspecifiedSleepDurationMillis` INTEGER,
                `unknownStageDurationMillis` INTEGER,
                `uncoveredStageDurationMillis` INTEGER,
                `stageCoverage` REAL,
                `sleepEfficiency` REAL,
                `stageTimelineStatusCode` TEXT,
                `metricValue` REAL,
                `metricUnitCode` TEXT,
                `firstSeenAtMillis` INTEGER NOT NULL,
                `lastSeenAtMillis` INTEGER NOT NULL,
                `supersededAtMillis` INTEGER,
                `deletedAtMillis` INTEGER,
                PRIMARY KEY(`revisionId`),
                FOREIGN KEY(`subjectId`) REFERENCES `recovery_research_consent`(`subjectId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_shadow_sleep_stages` (
                `stageId` TEXT NOT NULL,
                `revisionId` TEXT NOT NULL,
                `ordinal` INTEGER NOT NULL,
                `stageTypeCode` TEXT NOT NULL,
                `sourceStartTimeMillis` INTEGER NOT NULL,
                `sourceEndTimeMillis` INTEGER NOT NULL,
                `normalizedStartTimeMillis` INTEGER,
                `normalizedEndTimeMillis` INTEGER,
                `durationMillis` INTEGER,
                `wasClipped` INTEGER NOT NULL,
                `isValid` INTEGER NOT NULL,
                PRIMARY KEY(`stageId`),
                FOREIGN KEY(`revisionId`) REFERENCES `wearable_shadow_record_revisions`(`revisionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_context_snapshots` (
                `snapshotId` TEXT NOT NULL,
                `predictionId` TEXT NOT NULL,
                `targetWorkoutAttemptId` TEXT NOT NULL,
                `contractVersion` TEXT NOT NULL,
                `schemaVersion` INTEGER NOT NULL,
                `cutoffAtMillis` INTEGER NOT NULL,
                `createdAtMillis` INTEGER NOT NULL,
                `statusCode` TEXT NOT NULL,
                `accessEventId` TEXT,
                `availabilityCode` TEXT NOT NULL,
                `grantedTypeMask` TEXT NOT NULL,
                `includedRecordCount` INTEGER NOT NULL,
                `includedSourceCount` INTEGER NOT NULL,
                `primarySleepRevisionId` TEXT,
                `recentHrvRevisionId` TEXT,
                `recentRestingHeartRateRevisionId` TEXT,
                `overlappingSleepAlternatives` INTEGER NOT NULL,
                `payloadHash` TEXT NOT NULL,
                PRIMARY KEY(`snapshotId`),
                FOREIGN KEY(`predictionId`) REFERENCES `recovery_research_predictions`(`predictionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wearable_context_snapshot_items` (
                `snapshotItemId` TEXT NOT NULL,
                `snapshotId` TEXT NOT NULL,
                `revisionId` TEXT NOT NULL,
                `roleCode` TEXT NOT NULL,
                `selectedForSummary` INTEGER NOT NULL,
                PRIMARY KEY(`snapshotItemId`),
                FOREIGN KEY(`revisionId`) REFERENCES `wearable_shadow_record_revisions`(`revisionId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )

        createIndices(db)
        createTriggers(db)
    }

    private fun createIndices(db: SupportSQLiteDatabase) {
        listOf(
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_sync_state_subjectId` ON `wearable_shadow_sync_state` (`subjectId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_sync_state_lastSuccessfulSyncAtMillis` ON `wearable_shadow_sync_state` (`lastSuccessfulSyncAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_access_events_subjectId` ON `wearable_shadow_access_events` (`subjectId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_access_events_occurredAtMillis` ON `wearable_shadow_access_events` (`occurredAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_subjectId` ON `wearable_shadow_record_revisions` (`subjectId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_recordTypeCode` ON `wearable_shadow_record_revisions` (`recordTypeCode`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_healthConnectRecordId` ON `wearable_shadow_record_revisions` (`healthConnectRecordId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_recordTypeCode_healthConnectRecordId` ON `wearable_shadow_record_revisions` (`recordTypeCode`, `healthConnectRecordId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_recordTypeCode_healthConnectRecordId_lifecycleEpoch` ON `wearable_shadow_record_revisions` (`recordTypeCode`, `healthConnectRecordId`, `lifecycleEpoch`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_effectiveTimeMillis` ON `wearable_shadow_record_revisions` (`effectiveTimeMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_firstSeenAtMillis` ON `wearable_shadow_record_revisions` (`firstSeenAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_record_revisions_dataOriginPackageName` ON `wearable_shadow_record_revisions` (`dataOriginPackageName`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_sleep_stages_revisionId` ON `wearable_shadow_sleep_stages` (`revisionId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_shadow_sleep_stages_stageTypeCode` ON `wearable_shadow_sleep_stages` (`stageTypeCode`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_wearable_context_snapshots_predictionId` ON `wearable_context_snapshots` (`predictionId`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_wearable_context_snapshots_targetWorkoutAttemptId` ON `wearable_context_snapshots` (`targetWorkoutAttemptId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_context_snapshots_cutoffAtMillis` ON `wearable_context_snapshots` (`cutoffAtMillis`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_context_snapshots_accessEventId` ON `wearable_context_snapshots` (`accessEventId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_context_snapshot_items_snapshotId` ON `wearable_context_snapshot_items` (`snapshotId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_context_snapshot_items_revisionId` ON `wearable_context_snapshot_items` (`revisionId`)",
            "CREATE INDEX IF NOT EXISTS `index_wearable_context_snapshot_items_roleCode` ON `wearable_context_snapshot_items` (`roleCode`)",
        ).forEach(db::execSQL)
    }

    fun createTriggers(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `prevent_update_wearable_context_snapshots`
            BEFORE UPDATE ON `wearable_context_snapshots`
            BEGIN SELECT RAISE(ABORT, 'Wearable context snapshots are immutable'); END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `prevent_update_wearable_context_snapshot_items`
            BEFORE UPDATE ON `wearable_context_snapshot_items`
            BEGIN SELECT RAISE(ABORT, 'Wearable context snapshot items are immutable'); END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS `prevent_late_wearable_context_snapshot_items`
            BEFORE INSERT ON `wearable_context_snapshot_items`
            WHEN EXISTS (
                SELECT 1 FROM `wearable_context_snapshots`
                WHERE `snapshotId` = NEW.`snapshotId`
            )
            BEGIN SELECT RAISE(ABORT, 'Wearable snapshot is already sealed'); END
            """.trimIndent(),
        )
    }
}
