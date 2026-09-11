package com.hoctin.gymtracker.data.wearable

import com.hoctin.gymtracker.data.research.RecoveryResearchVersions

/** Frozen public contract identifier for the first read-only Health Connect epoch. */
object WearableShadowContract {
    /** Raw ingestion/normalization contract. Kept frozen for persisted source revisions. */
    const val CONTRACT_VERSION = "WEARABLE_2A_SHADOW_CONTEXT_V1"
    /** Corrected primary-sleep semantics; V1 snapshots, if any, remain immutable historical rows. */
    const val SNAPSHOT_CONTRACT_VERSION = "WEARABLE_2A_SHADOW_CONTEXT_V1_1"
    const val SLEEP_SELECTION_POLICY_VERSION = "WEARABLE_2A_SLEEP_SELECTION_V2"
    const val SCHEMA_VERSION = 1
    // The additive shadow graph is a child of the existing explicit Recovery Research consent row.
    const val SUBJECT_ID = RecoveryResearchVersions.SUBJECT_ID
    const val INITIAL_READ_LOOKBACK_DAYS = 14L
    const val MAX_STORED_LOOKBACK_DAYS = 30L
    const val PRIOR_SLEEP_LOOKBACK_HOURS = 36L
    const val RECENT_METRIC_LOOKBACK_HOURS = 48L
    const val READ_PAGE_SIZE = 500
}

enum class WearableRecordType {
    SLEEP_SESSION,
    HRV_RMSSD,
    RESTING_HEART_RATE,
}

enum class WearableAvailability {
    UNKNOWN,
    AVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    UNAVAILABLE,
}

enum class WearableSyncStatus {
    NEVER_SYNCED,
    IDLE,
    SYNCING,
    SUCCESS,
    PARTIAL_PERMISSION,
    PERMISSION_DENIED,
    PROVIDER_UNAVAILABLE,
    INTERRUPTED,
    FAILED,
}

enum class WearableSyncError {
    NONE,
    PERMISSION_DENIED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED,
    TOKEN_EXPIRED,
    REMOTE_INTERRUPTION,
    INVALID_RECORD,
    UNKNOWN,
}

enum class WearableStageType {
    UNKNOWN,
    AWAKE,
    SLEEPING_UNSPECIFIED,
    OUT_OF_BED,
    LIGHT,
    DEEP,
    REM,
    AWAKE_IN_BED,
}

enum class WearableStageTimelineStatus {
    NO_STAGES,
    VALID_COMPLETE,
    VALID_WITH_GAPS,
    VALID_WITH_CLIPPED_STAGE,
    INVALID_STAGE_INTERVAL,
    INVALID_OVERLAP,
}

enum class WearableSnapshotStatus {
    CAPTURED,
    CAPTURED_NO_ELIGIBLE_CONTEXT,
    SHADOW_DISABLED,
    PROVIDER_UNAVAILABLE,
    NO_ACCESS_EVIDENCE,
    NO_GRANTED_PERMISSION,
}

enum class WearableSnapshotRole {
    ELIGIBLE_SLEEP_SESSION,
    ELIGIBLE_HRV_RMSSD,
    ELIGIBLE_RESTING_HEART_RATE,
}

data class WearableSourceMetadata(
    val healthConnectRecordId: String,
    val dataOriginPackageName: String,
    val dataOriginDisplayName: String?,
    val sourceLastModifiedAtMillis: Long,
    val clientRecordId: String?,
    val clientRecordVersion: Long,
    val recordingMethod: Int,
    val deviceType: Int?,
    val deviceManufacturer: String?,
    val deviceModel: String?,
)

sealed interface RawWearableRecord {
    val type: WearableRecordType
    val metadata: WearableSourceMetadata
    val effectiveTimeMillis: Long
}

data class RawSleepStage(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val stageType: WearableStageType,
)

data class RawSleepSession(
    override val metadata: WearableSourceMetadata,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val startZoneOffsetSeconds: Int?,
    val endZoneOffsetSeconds: Int?,
    val stages: List<RawSleepStage>,
) : RawWearableRecord {
    override val type: WearableRecordType = WearableRecordType.SLEEP_SESSION
    override val effectiveTimeMillis: Long = endTimeMillis
}

data class RawHrvRmssd(
    override val metadata: WearableSourceMetadata,
    val measurementTimeMillis: Long,
    val zoneOffsetSeconds: Int?,
    val rmssdMillis: Double,
) : RawWearableRecord {
    override val type: WearableRecordType = WearableRecordType.HRV_RMSSD
    override val effectiveTimeMillis: Long = measurementTimeMillis
}

data class RawRestingHeartRate(
    override val metadata: WearableSourceMetadata,
    val measurementTimeMillis: Long,
    val zoneOffsetSeconds: Int?,
    val beatsPerMinute: Long,
) : RawWearableRecord {
    override val type: WearableRecordType = WearableRecordType.RESTING_HEART_RATE
    override val effectiveTimeMillis: Long = measurementTimeMillis
}

data class NormalizedSleepStageDraft(
    val ordinal: Int,
    val stageType: WearableStageType,
    val sourceStartTimeMillis: Long,
    val sourceEndTimeMillis: Long,
    val normalizedStartTimeMillis: Long?,
    val normalizedEndTimeMillis: Long?,
    val durationMillis: Long?,
    val wasClipped: Boolean,
    val isValid: Boolean,
)

/**
 * Versioned normalized payload before ingestion timestamps and local lifecycle fields are attached.
 * Null sleep aggregates are deliberate abstentions, never implicit zeroes.
 */
data class NormalizedWearableRevisionDraft(
    val type: WearableRecordType,
    val metadata: WearableSourceMetadata,
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
    val stageTimelineStatus: WearableStageTimelineStatus?,
    val metricValue: Double?,
    val metricUnitCode: String?,
    val sleepStages: List<NormalizedSleepStageDraft>,
)

data class WearableRawPage(
    val records: List<RawWearableRecord>,
    val nextPageToken: String?,
)

data class WearableChangePage(
    val upserts: List<RawWearableRecord>,
    val deletedRecordIds: List<String>,
    val nextChangesToken: String,
    val hasMore: Boolean,
    val tokenExpired: Boolean,
)

data class WearableAccessState(
    val enabled: Boolean = false,
    val availability: WearableAvailability = WearableAvailability.UNKNOWN,
    val grantedTypes: Set<WearableRecordType> = emptySet(),
    val checkedAtMillis: Long? = null,
)

data class WearableConnectionSummary(
    val recoveryResearchEnabled: Boolean = false,
    val shadowSyncEnabled: Boolean = false,
    val availability: WearableAvailability = WearableAvailability.UNKNOWN,
    val grantedTypes: Set<WearableRecordType> = emptySet(),
    val isRefreshing: Boolean = false,
    val status: WearableSyncStatus = WearableSyncStatus.NEVER_SYNCED,
    val lastSuccessfulSyncAtMillis: Long? = null,
    val lastError: WearableSyncError = WearableSyncError.NONE,
) {
    val allPermissionsGranted: Boolean
        get() = grantedTypes.containsAll(WearableRecordType.entries)
}

data class WearableSleepSummary(
    val revisionId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val sessionDurationMillis: Long,
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
    val timelineStatus: WearableStageTimelineStatus,
    val sourcePackageName: String,
    val sourceDisplayName: String?,
    val firstSeenAtMillis: Long,
)

data class WearableMetricSummary(
    val revisionId: String,
    val measurementTimeMillis: Long,
    val value: Double,
    val unitCode: String,
    val sourcePackageName: String,
    val sourceDisplayName: String?,
    val firstSeenAtMillis: Long,
)

data class WearableHomeContext(
    val connection: WearableConnectionSummary = WearableConnectionSummary(),
    val primaryPriorSleep: WearableSleepSummary? = null,
    val recentHrvRmssd: WearableMetricSummary? = null,
    val recentRestingHeartRate: WearableMetricSummary? = null,
    val overlappingSleepAlternatives: Int = 0,
    val otherPriorSleepSessions: Int = 0,
    val retainedDataHiddenByAccessState: Boolean = false,
)

/** Value-free integrity summary shown locally for physical-pipeline verification. */
data class WearableSnapshotDiagnostic(
    val status: WearableSnapshotStatus,
    val cutoffAtMillis: Long,
    val createdAtMillis: Long,
    val includedRecordCount: Int,
    val includedSourceCount: Int,
    val payloadHash: String,
)
