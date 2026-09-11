package com.hoctin.gymtracker.data.wearable

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

object HealthConnectPermissions {
    val byRecordType: Map<WearableRecordType, String> = mapOf(
        WearableRecordType.SLEEP_SESSION to
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        WearableRecordType.HRV_RMSSD to
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        WearableRecordType.RESTING_HEART_RATE to
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
    )
    val requiredReadPermissions: Set<String> = byRecordType.values.toSet()
    val requestContract get() = PermissionController.createRequestPermissionResultContract()

    fun grantedTypes(grantedPermissions: Set<String>): Set<WearableRecordType> =
        byRecordType.filterValues(grantedPermissions::contains).keys
}

interface HealthConnectGateway {
    fun availability(): WearableAvailability
    suspend fun grantedTypes(): Set<WearableRecordType>
    suspend fun revokeAllPermissions()
    fun manageAccessIntent(): Intent?
    suspend fun getChangesToken(type: WearableRecordType): String
    suspend fun readPage(
        type: WearableRecordType,
        startTimeMillis: Long,
        endTimeMillis: Long,
        pageToken: String?,
    ): WearableRawPage

    suspend fun readChanges(token: String): WearableChangePage
}

/** Android-only adapter. It never logs record contents and exposes no write operation. */
class AndroidHealthConnectGateway(context: Context) : HealthConnectGateway {
    private val appContext = context.applicationContext

    override fun availability(): WearableAvailability = when (
        HealthConnectClient.getSdkStatus(appContext, PROVIDER_PACKAGE_NAME)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> WearableAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            WearableAvailability.PROVIDER_UPDATE_REQUIRED
        else -> WearableAvailability.UNAVAILABLE
    }

    override suspend fun grantedTypes(): Set<WearableRecordType> {
        val client = availableClient() ?: return emptySet()
        return HealthConnectPermissions.grantedTypes(
            client.permissionController.getGrantedPermissions(),
        )
    }

    override suspend fun revokeAllPermissions() {
        availableClient()?.permissionController?.revokeAllPermissions()
    }

    override fun manageAccessIntent(): Intent? {
        if (availability() == WearableAvailability.UNAVAILABLE) return null
        return HealthConnectClient.getHealthConnectManageDataIntent(
            appContext,
            PROVIDER_PACKAGE_NAME,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    override suspend fun getChangesToken(type: WearableRecordType): String {
        val client = requireNotNull(availableClient()) { "Health Connect unavailable" }
        return client.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(recordClass(type))),
        )
    }

    override suspend fun readPage(
        type: WearableRecordType,
        startTimeMillis: Long,
        endTimeMillis: Long,
        pageToken: String?,
    ): WearableRawPage {
        val client = requireNotNull(availableClient()) { "Health Connect unavailable" }
        val filter = TimeRangeFilter.between(
            Instant.ofEpochMilli(startTimeMillis),
            Instant.ofEpochMilli(endTimeMillis),
        )
        return when (type) {
            WearableRecordType.SLEEP_SESSION -> {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = SleepSessionRecord::class,
                        timeRangeFilter = filter,
                        pageSize = WearableShadowContract.READ_PAGE_SIZE,
                        pageToken = pageToken,
                    ),
                )
                WearableRawPage(response.records.map(::mapSleep), response.pageToken)
            }
            WearableRecordType.HRV_RMSSD -> {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateVariabilityRmssdRecord::class,
                        timeRangeFilter = filter,
                        pageSize = WearableShadowContract.READ_PAGE_SIZE,
                        pageToken = pageToken,
                    ),
                )
                WearableRawPage(response.records.map(::mapHrv), response.pageToken)
            }
            WearableRecordType.RESTING_HEART_RATE -> {
                val response = client.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = filter,
                        pageSize = WearableShadowContract.READ_PAGE_SIZE,
                        pageToken = pageToken,
                    ),
                )
                WearableRawPage(response.records.map(::mapRestingHeartRate), response.pageToken)
            }
        }
    }

    override suspend fun readChanges(token: String): WearableChangePage {
        val client = requireNotNull(availableClient()) { "Health Connect unavailable" }
        val response = client.getChanges(token)
        return WearableChangePage(
            upserts = response.changes.mapNotNull { change ->
                (change as? UpsertionChange)?.record?.let(::mapRecord)
            },
            deletedRecordIds = response.changes.mapNotNull { change ->
                (change as? DeletionChange)?.recordId
            },
            nextChangesToken = response.nextChangesToken,
            hasMore = response.hasMore,
            tokenExpired = response.changesTokenExpired,
        )
    }

    private fun availableClient(): HealthConnectClient? =
        if (availability() == WearableAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(appContext, PROVIDER_PACKAGE_NAME)
        } else {
            null
        }

    private fun mapRecord(record: Record): RawWearableRecord? = when (record) {
        is SleepSessionRecord -> mapSleep(record)
        is HeartRateVariabilityRmssdRecord -> mapHrv(record)
        is RestingHeartRateRecord -> mapRestingHeartRate(record)
        else -> null
    }

    private fun mapSleep(record: SleepSessionRecord) = RawSleepSession(
        metadata = mapMetadata(record.metadata),
        startTimeMillis = record.startTime.toEpochMilli(),
        endTimeMillis = record.endTime.toEpochMilli(),
        startZoneOffsetSeconds = record.startZoneOffset?.totalSeconds,
        endZoneOffsetSeconds = record.endZoneOffset?.totalSeconds,
        stages = record.stages.map { stage ->
            RawSleepStage(
                startTimeMillis = stage.startTime.toEpochMilli(),
                endTimeMillis = stage.endTime.toEpochMilli(),
                stageType = when (stage.stage) {
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> WearableStageType.AWAKE
                    SleepSessionRecord.STAGE_TYPE_SLEEPING -> WearableStageType.SLEEPING_UNSPECIFIED
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> WearableStageType.OUT_OF_BED
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> WearableStageType.LIGHT
                    SleepSessionRecord.STAGE_TYPE_DEEP -> WearableStageType.DEEP
                    SleepSessionRecord.STAGE_TYPE_REM -> WearableStageType.REM
                    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> WearableStageType.AWAKE_IN_BED
                    else -> WearableStageType.UNKNOWN
                },
            )
        },
    )

    private fun mapHrv(record: HeartRateVariabilityRmssdRecord) = RawHrvRmssd(
        metadata = mapMetadata(record.metadata),
        measurementTimeMillis = record.time.toEpochMilli(),
        zoneOffsetSeconds = record.zoneOffset?.totalSeconds,
        rmssdMillis = record.heartRateVariabilityMillis,
    )

    private fun mapRestingHeartRate(record: RestingHeartRateRecord) = RawRestingHeartRate(
        metadata = mapMetadata(record.metadata),
        measurementTimeMillis = record.time.toEpochMilli(),
        zoneOffsetSeconds = record.zoneOffset?.totalSeconds,
        beatsPerMinute = record.beatsPerMinute,
    )

    private fun mapMetadata(metadata: Metadata): WearableSourceMetadata {
        val packageName = metadata.dataOrigin.packageName
        val displayName = runCatching {
            val info = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(info).toString()
        }.getOrNull()
        return WearableSourceMetadata(
            healthConnectRecordId = metadata.id,
            dataOriginPackageName = packageName,
            dataOriginDisplayName = displayName,
            sourceLastModifiedAtMillis = metadata.lastModifiedTime.toEpochMilli(),
            clientRecordId = metadata.clientRecordId,
            clientRecordVersion = metadata.clientRecordVersion,
            recordingMethod = metadata.recordingMethod,
            deviceType = metadata.device?.type,
            deviceManufacturer = metadata.device?.manufacturer,
            deviceModel = metadata.device?.model,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun recordClass(type: WearableRecordType): KClass<out Record> = when (type) {
        WearableRecordType.SLEEP_SESSION -> SleepSessionRecord::class
        WearableRecordType.HRV_RMSSD -> HeartRateVariabilityRmssdRecord::class
        WearableRecordType.RESTING_HEART_RATE -> RestingHeartRateRecord::class
    }

    companion object {
        const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
}
