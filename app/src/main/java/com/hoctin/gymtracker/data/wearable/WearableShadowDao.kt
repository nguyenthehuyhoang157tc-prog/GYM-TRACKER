package com.hoctin.gymtracker.data.wearable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WearableShadowDao {
    @Query("SELECT * FROM wearable_shadow_settings WHERE subjectId=:subjectId LIMIT 1")
    fun observeSettings(subjectId: String): Flow<WearableShadowSettingsEntity?>

    @Query("SELECT * FROM wearable_shadow_settings WHERE subjectId=:subjectId LIMIT 1")
    suspend fun getSettings(subjectId: String): WearableShadowSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(entity: WearableShadowSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccessEvent(entity: WearableShadowAccessEventEntity): Long

    @Query(
        "SELECT * FROM wearable_shadow_access_events " +
            "WHERE subjectId=:subjectId AND occurredAtMillis<=:cutoffAtMillis " +
            "ORDER BY occurredAtMillis DESC, accessEventId DESC LIMIT 1",
    )
    suspend fun getLatestAccessEventAtOrBefore(
        subjectId: String,
        cutoffAtMillis: Long,
    ): WearableShadowAccessEventEntity?

    @Query(
        "SELECT * FROM wearable_shadow_access_events WHERE subjectId=:subjectId " +
            "ORDER BY occurredAtMillis DESC, accessEventId DESC LIMIT 1",
    )
    suspend fun getLatestAccessEvent(subjectId: String): WearableShadowAccessEventEntity?

    @Query("SELECT * FROM wearable_shadow_sync_state WHERE subjectId=:subjectId ORDER BY recordTypeCode")
    fun observeSyncStates(subjectId: String): Flow<List<WearableShadowSyncStateEntity>>

    @Query(
        "SELECT * FROM wearable_shadow_sync_state " +
            "WHERE subjectId=:subjectId AND recordTypeCode=:recordTypeCode LIMIT 1",
    )
    suspend fun getSyncState(
        subjectId: String,
        recordTypeCode: String,
    ): WearableShadowSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(entity: WearableShadowSyncStateEntity)

    @Query("SELECT * FROM wearable_shadow_record_revisions WHERE revisionId=:revisionId LIMIT 1")
    suspend fun getRevision(revisionId: String): WearableRecordRevisionEntity?

    @Query("SELECT COUNT(*) FROM wearable_shadow_record_revisions")
    suspend fun countRevisions(): Int

    @Query(
        "SELECT * FROM wearable_shadow_record_revisions " +
            "WHERE recordTypeCode=:recordTypeCode AND healthConnectRecordId=:healthConnectRecordId " +
            "ORDER BY lifecycleEpoch",
    )
    suspend fun getRecordLifecycle(
        recordTypeCode: String,
        healthConnectRecordId: String,
    ): List<WearableRecordRevisionEntity>

    @Query(
        "SELECT * FROM wearable_shadow_record_revisions WHERE recordTypeCode=:recordTypeCode " +
            "AND healthConnectRecordId=:healthConnectRecordId " +
            "AND sourceLastModifiedAtMillis=:sourceLastModifiedAtMillis AND payloadHash=:payloadHash " +
            "ORDER BY lifecycleEpoch DESC LIMIT 1",
    )
    suspend fun getLatestMatchingRevision(
        recordTypeCode: String,
        healthConnectRecordId: String,
        sourceLastModifiedAtMillis: Long,
        payloadHash: String,
    ): WearableRecordRevisionEntity?

    @Query(
        "SELECT * FROM wearable_shadow_record_revisions " +
            "WHERE recordTypeCode=:recordTypeCode AND healthConnectRecordId=:healthConnectRecordId " +
            "AND supersededAtMillis IS NULL AND deletedAtMillis IS NULL " +
            "ORDER BY sourceLastModifiedAtMillis DESC, payloadHash DESC LIMIT 1",
    )
    suspend fun getActiveRevision(
        recordTypeCode: String,
        healthConnectRecordId: String,
    ): WearableRecordRevisionEntity?

    @Query(
        "SELECT MAX(lifecycleEpoch) FROM wearable_shadow_record_revisions " +
            "WHERE recordTypeCode=:recordTypeCode AND healthConnectRecordId=:healthConnectRecordId",
    )
    suspend fun getMaxLifecycleEpoch(
        recordTypeCode: String,
        healthConnectRecordId: String,
    ): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRevision(entity: WearableRecordRevisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSleepStages(entities: List<WearableSleepStageEntity>)

    @Query(
        "UPDATE wearable_shadow_record_revisions SET lastSeenAtMillis=:lastSeenAtMillis " +
            "WHERE revisionId=:revisionId AND lastSeenAtMillis<:lastSeenAtMillis",
    )
    suspend fun touchRevision(revisionId: String, lastSeenAtMillis: Long)

    @Query(
        "UPDATE wearable_shadow_record_revisions SET supersededAtMillis=:supersededAtMillis " +
            "WHERE revisionId=:revisionId AND supersededAtMillis IS NULL",
    )
    suspend fun markSuperseded(revisionId: String, supersededAtMillis: Long)

    @Query(
        "UPDATE wearable_shadow_record_revisions SET deletedAtMillis=:deletedAtMillis " +
            "WHERE recordTypeCode=:recordTypeCode AND healthConnectRecordId=:healthConnectRecordId " +
            "AND deletedAtMillis IS NULL",
    )
    suspend fun markDeleted(
        recordTypeCode: String,
        healthConnectRecordId: String,
        deletedAtMillis: Long,
    )

    @Query(
        "UPDATE wearable_shadow_sync_state SET rejectedRecordCount=rejectedRecordCount+:count, " +
            "updatedAtMillis=:updatedAtMillis WHERE subjectId=:subjectId AND recordTypeCode=:recordTypeCode",
    )
    suspend fun incrementRejectedRecordCount(
        subjectId: String,
        recordTypeCode: String,
        count: Long,
        updatedAtMillis: Long,
    )

    @Query(
        "DELETE FROM wearable_shadow_record_revisions WHERE effectiveTimeMillis<:cutoffAtMillis " +
            "AND revisionId NOT IN (SELECT revisionId FROM wearable_context_snapshot_items)",
    )
    suspend fun pruneUnreferencedRevisionsBefore(cutoffAtMillis: Long): Int

    @Query(
        "SELECT DISTINCT healthConnectRecordId FROM wearable_shadow_record_revisions " +
            "WHERE recordTypeCode=:recordTypeCode AND effectiveTimeMillis BETWEEN :startMillis AND :endMillis " +
            "AND supersededAtMillis IS NULL AND deletedAtMillis IS NULL",
    )
    suspend fun getActiveRecordIdsInWindow(
        recordTypeCode: String,
        startMillis: Long,
        endMillis: Long,
    ): List<String>

    @Transaction
    @Query(
        "SELECT * FROM wearable_shadow_record_revisions " +
            "WHERE supersededAtMillis IS NULL AND deletedAtMillis IS NULL " +
            "ORDER BY effectiveTimeMillis DESC, dataOriginPackageName, healthConnectRecordId",
    )
    fun observeActiveRevisions(): Flow<List<WearableRevisionWithStages>>

    @Query(
        "SELECT * FROM wearable_context_snapshots ORDER BY createdAtMillis DESC, snapshotId DESC LIMIT 1",
    )
    fun observeLatestSnapshot(): Flow<WearableContextSnapshotEntity?>

    @Query(
        "SELECT * FROM wearable_shadow_record_revisions " +
            "WHERE effectiveTimeMillis BETWEEN :startMillis AND :cutoffAtMillis " +
            "AND firstSeenAtMillis<=:cutoffAtMillis " +
            "AND sourceLastModifiedAtMillis<=:cutoffAtMillis " +
            "AND (supersededAtMillis IS NULL OR supersededAtMillis>:cutoffAtMillis) " +
            "AND (deletedAtMillis IS NULL OR deletedAtMillis>:cutoffAtMillis) " +
            "ORDER BY effectiveTimeMillis, recordTypeCode, dataOriginPackageName, healthConnectRecordId, revisionId",
    )
    suspend fun getRevisionsKnownAtCutoff(
        startMillis: Long,
        cutoffAtMillis: Long,
    ): List<WearableRecordRevisionEntity>

    @Query("SELECT * FROM wearable_context_snapshots WHERE predictionId=:predictionId LIMIT 1")
    suspend fun getSnapshotForPrediction(predictionId: String): WearableContextSnapshotEntity?

    @Query("SELECT * FROM wearable_context_snapshot_items WHERE snapshotId=:snapshotId ORDER BY snapshotItemId")
    suspend fun getSnapshotItems(snapshotId: String): List<WearableContextSnapshotItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(entity: WearableContextSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshotItems(entities: List<WearableContextSnapshotItemEntity>)

    @Query("SELECT COUNT(*) FROM wearable_context_snapshots")
    suspend fun countSnapshots(): Int

    @Query("DELETE FROM wearable_context_snapshot_items")
    suspend fun deleteSnapshotItems()

    @Query("DELETE FROM wearable_context_snapshots")
    suspend fun deleteSnapshots()

    @Query("DELETE FROM wearable_shadow_record_revisions")
    suspend fun deleteRevisions()

    @Query("DELETE FROM wearable_shadow_access_events")
    suspend fun deleteAccessEvents()

    @Query("DELETE FROM wearable_shadow_sync_state")
    suspend fun deleteSyncStates()

    @Query("DELETE FROM wearable_shadow_settings")
    suspend fun deleteSettings()

    @Transaction
    suspend fun deleteAllWearableShadowData() {
        deleteSnapshotItems()
        deleteSnapshots()
        // Sleep stages cascade from revision deletion.
        deleteRevisions()
        deleteAccessEvents()
        deleteSyncStates()
        deleteSettings()
    }
}
