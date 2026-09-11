package com.hoctin.gymtracker.data.wearable

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.hoctin.gymtracker.data.research.RecoveryResearchConsentEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionEntity

@Entity(
    tableName = "wearable_shadow_settings",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchConsentEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["subjectId"],
)
data class WearableShadowSettingsEntity(
    val subjectId: String,
    val contractVersion: String,
    val schemaVersion: Int,
    val syncEnabled: Boolean,
    val lastAvailabilityCode: String,
    val grantedTypeMask: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "wearable_shadow_sync_state",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchConsentEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index("lastSuccessfulSyncAtMillis")],
    primaryKeys = ["subjectId", "recordTypeCode"],
)
data class WearableShadowSyncStateEntity(
    val subjectId: String,
    val recordTypeCode: String,
    val changesToken: String?,
    val initialSyncComplete: Boolean,
    val permissionGranted: Boolean,
    val statusCode: String,
    val errorCode: String,
    val lastAttemptAtMillis: Long?,
    val lastSuccessfulSyncAtMillis: Long?,
    val rejectedRecordCount: Long,
    val updatedAtMillis: Long,
)

/** Append-only access evidence used to reconstruct what AURON knew at a prospective cutoff. */
@Entity(
    tableName = "wearable_shadow_access_events",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchConsentEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index("occurredAtMillis")],
    primaryKeys = ["accessEventId"],
)
data class WearableShadowAccessEventEntity(
    val accessEventId: String,
    val subjectId: String,
    val occurredAtMillis: Long,
    val syncEnabled: Boolean,
    val availabilityCode: String,
    val grantedTypeMask: String,
    val reasonCode: String,
)

/**
 * One immutable normalized payload revision from Health Connect plus mutable local lifecycle marks.
 * A changed Health Connect record inserts a new row; it never overwrites payload used by a snapshot.
 */
@Entity(
    tableName = "wearable_shadow_record_revisions",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchConsentEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("subjectId"),
        Index("recordTypeCode"),
        Index("healthConnectRecordId"),
        Index(value = ["recordTypeCode", "healthConnectRecordId"]),
        Index(value = ["recordTypeCode", "healthConnectRecordId", "lifecycleEpoch"], unique = true),
        Index("effectiveTimeMillis"),
        Index("firstSeenAtMillis"),
        Index("dataOriginPackageName"),
    ],
    primaryKeys = ["revisionId"],
)
data class WearableRecordRevisionEntity(
    val revisionId: String,
    val subjectId: String,
    val contractVersion: String,
    val schemaVersion: Int,
    val recordTypeCode: String,
    val healthConnectRecordId: String,
    /** Monotone local epoch; a delete followed by an identical upsert creates a new epoch. */
    val lifecycleEpoch: Long,
    val dataOriginPackageName: String,
    val dataOriginDisplayName: String?,
    val sourceLastModifiedAtMillis: Long,
    val clientRecordId: String?,
    val clientRecordVersion: Long,
    val recordingMethod: Int,
    val deviceType: Int?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
    val payloadHash: String,
    val startTimeMillis: Long?,
    val endTimeMillis: Long?,
    val measurementTimeMillis: Long?,
    val effectiveTimeMillis: Long,
    val startZoneOffsetSeconds: Int?,
    val endZoneOffsetSeconds: Int?,
    val measurementZoneOffsetSeconds: Int?,
    val sessionDurationMillis: Long?,
    val knownSleepDurationMillis: Long?,
    val awakeDurationMillis: Long?,
    val lightDurationMillis: Long?,
    val deepDurationMillis: Long?,
    val remDurationMillis: Long?,
    val unspecifiedSleepDurationMillis: Long?,
    val unknownStageDurationMillis: Long?,
    val uncoveredStageDurationMillis: Long?,
    val stageCoverage: Double?,
    val sleepEfficiency: Double?,
    val stageTimelineStatusCode: String?,
    val metricValue: Double?,
    val metricUnitCode: String?,
    val firstSeenAtMillis: Long,
    val lastSeenAtMillis: Long,
    val supersededAtMillis: Long?,
    val deletedAtMillis: Long?,
)

@Entity(
    tableName = "wearable_shadow_sleep_stages",
    foreignKeys = [
        ForeignKey(
            entity = WearableRecordRevisionEntity::class,
            parentColumns = ["revisionId"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("revisionId"), Index("stageTypeCode")],
    primaryKeys = ["stageId"],
)
data class WearableSleepStageEntity(
    val stageId: String,
    val revisionId: String,
    val ordinal: Int,
    val stageTypeCode: String,
    val sourceStartTimeMillis: Long,
    val sourceEndTimeMillis: Long,
    val normalizedStartTimeMillis: Long?,
    val normalizedEndTimeMillis: Long?,
    val durationMillis: Long?,
    val wasClipped: Boolean,
    val isValid: Boolean,
)

@Entity(
    tableName = "wearable_context_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = RecoveryResearchPredictionEntity::class,
            parentColumns = ["predictionId"],
            childColumns = ["predictionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["predictionId"], unique = true),
        Index(value = ["targetWorkoutAttemptId"], unique = true),
        Index("cutoffAtMillis"),
        Index("accessEventId"),
    ],
    primaryKeys = ["snapshotId"],
)
data class WearableContextSnapshotEntity(
    val snapshotId: String,
    val predictionId: String,
    val targetWorkoutAttemptId: String,
    val contractVersion: String,
    val schemaVersion: Int,
    val cutoffAtMillis: Long,
    val createdAtMillis: Long,
    val statusCode: String,
    val accessEventId: String?,
    val availabilityCode: String,
    val grantedTypeMask: String,
    val includedRecordCount: Int,
    val includedSourceCount: Int,
    val primarySleepRevisionId: String?,
    val recentHrvRevisionId: String?,
    val recentRestingHeartRateRevisionId: String?,
    val overlappingSleepAlternatives: Int,
    val payloadHash: String,
)

/**
 * Items are inserted before their header in one transaction. There is intentionally no header FK:
 * a trigger rejects any later item insertion after the immutable header exists, while rollback keeps
 * the graph atomic. Revision deletion is allowed only through the explicit privacy deletion flow.
 */
@Entity(
    tableName = "wearable_context_snapshot_items",
    foreignKeys = [
        ForeignKey(
            entity = WearableRecordRevisionEntity::class,
            parentColumns = ["revisionId"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshotId"), Index("revisionId"), Index("roleCode")],
    primaryKeys = ["snapshotItemId"],
)
data class WearableContextSnapshotItemEntity(
    val snapshotItemId: String,
    val snapshotId: String,
    val revisionId: String,
    val roleCode: String,
    val selectedForSummary: Boolean,
)

data class WearableRevisionWithStages(
    @Embedded val revision: WearableRecordRevisionEntity,
    @Relation(
        parentColumn = "revisionId",
        entityColumn = "revisionId",
    )
    val stages: List<WearableSleepStageEntity>,
)
