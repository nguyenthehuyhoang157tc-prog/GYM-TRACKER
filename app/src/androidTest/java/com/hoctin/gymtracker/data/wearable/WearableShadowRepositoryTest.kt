package com.hoctin.gymtracker.data.wearable

import android.content.Context
import android.content.Intent
import android.os.RemoteException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.research.FrozenRecoveryResearchRecord
import com.hoctin.gymtracker.data.research.RecoveryResearchConsentEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchOutcomeEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionEntity
import com.hoctin.gymtracker.data.research.RecoveryResearchSchema
import com.hoctin.gymtracker.data.research.RecoveryResearchVersions
import com.hoctin.gymtracker.data.research.ResearchCompleteness
import com.hoctin.gymtracker.data.research.ResearchConsentState
import com.hoctin.gymtracker.data.research.ResearchOutcomeStatus
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearableShadowRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val time = AtomicLong(BASE)
    private lateinit var db: AppDatabase
    private lateinit var gateway: FakeHealthConnectGateway
    private lateinit var repository: WearableShadowRepository

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        RecoveryResearchSchema.createTriggers(db)
                        WearableShadowSchema.createTriggers(db)
                    }
                },
            )
            .build()
        db.recoveryResearchDao().insertConsentIgnore(
            RecoveryResearchConsentEntity(
                subjectId = RecoveryResearchVersions.SUBJECT_ID,
                stateCode = ResearchConsentState.GRANTED.name,
                contractVersion = RecoveryResearchVersions.CONTRACT,
                createdAtMillis = BASE - 1_000L,
                grantedAtMillis = BASE - 1_000L,
                revokedAtMillis = null,
            ),
        )
        gateway = FakeHealthConnectGateway()
        repository = WearableShadowRepository(
            database = db,
            dao = db.wearableShadowDao(),
            recoveryResearchDao = db.recoveryResearchDao(),
            gateway = gateway,
            clock = time::get,
        )
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    @Test
    fun syncIsIdempotent_updatesAreImmutable_andDeleteReupsertStartsNewEpoch() = runBlocking {
        val sleep = sleep("sleep-1", "source.a", BASE - 9 * HOUR, BASE - HOUR, BASE - 500)
        val hrvV1 = hrv("hrv-1", "source.a", BASE - 2 * HOUR, BASE - 400, 44.0)
        gateway.seed(sleep)
        gateway.seed(hrvV1)
        connectAll()

        assertEquals(2, db.wearableShadowDao().countRevisions())
        time.incrementAndGet()
        repository.refreshExplicit()
        assertEquals("Exact re-read must be idempotent", 2, db.wearableShadowDao().countRevisions())

        val hrvV2 = hrvV1.copy(
            metadata = hrvV1.metadata.copy(sourceLastModifiedAtMillis = BASE - 300),
            rmssdMillis = 47.5,
        )
        gateway.upsert(hrvV2)
        time.incrementAndGet()
        repository.refreshExplicit()
        var lifecycle = db.wearableShadowDao().getRecordLifecycle(
            WearableRecordType.HRV_RMSSD.name,
            "hrv-1",
        )
        assertEquals(2, lifecycle.size)
        assertEquals(0L, lifecycle[0].lifecycleEpoch)
        assertEquals(1L, lifecycle[1].lifecycleEpoch)
        assertTrue(lifecycle[0].supersededAtMillis != null)
        assertNull(lifecycle[1].supersededAtMillis)

        gateway.delete(WearableRecordType.HRV_RMSSD, "hrv-1")
        time.incrementAndGet()
        repository.refreshExplicit()
        lifecycle = db.wearableShadowDao().getRecordLifecycle(
            WearableRecordType.HRV_RMSSD.name,
            "hrv-1",
        )
        assertTrue(lifecycle.last().deletedAtMillis != null)

        gateway.upsert(hrvV2)
        time.incrementAndGet()
        repository.refreshExplicit()
        lifecycle = db.wearableShadowDao().getRecordLifecycle(
            WearableRecordType.HRV_RMSSD.name,
            "hrv-1",
        )
        assertEquals(3, lifecycle.size)
        assertEquals(2L, lifecycle.last().lifecycleEpoch)
        assertNull(lifecycle.last().deletedAtMillis)
        assertNull(lifecycle.last().supersededAtMillis)

        gateway.upsert(hrv("bad-hrv", "source.b", BASE - HOUR, BASE - 100, 500.0))
        time.incrementAndGet()
        repository.refreshExplicit()
        val hrvState = requireNotNull(
            db.wearableShadowDao().getSyncState(
                WearableShadowContract.SUBJECT_ID,
                WearableRecordType.HRV_RMSSD.name,
            ),
        )
        assertEquals(1L, hrvState.rejectedRecordCount)

        gateway.expireNextChangesFor = WearableRecordType.HRV_RMSSD
        time.incrementAndGet()
        repository.refreshExplicit()
        val recovered = requireNotNull(
            db.wearableShadowDao().getSyncState(
                WearableShadowContract.SUBJECT_ID,
                WearableRecordType.HRV_RMSSD.name,
            ),
        )
        assertEquals(WearableSyncStatus.SUCCESS.name, recovered.statusCode)
        assertEquals(WearableSyncError.NONE.name, recovered.errorCode)
    }

    @Test
    fun lateArrivalNeverEntersOldSnapshot_butCanEnterLaterSnapshot() = runBlocking {
        connectAll()
        time.set(BASE + 100L)
        gateway.upsert(sleep("late-sleep", "source.a", BASE - 8 * HOUR, BASE - HOUR, BASE - 100))
        repository.refreshExplicit()

        val oldPrediction = insertPrediction("old", BASE + 50L)
        repository.captureSnapshotForPrediction(oldPrediction)
        val oldSnapshot = requireNotNull(db.wearableShadowDao().getSnapshotForPrediction("prediction-old"))
        assertEquals(WearableShadowContract.SNAPSHOT_CONTRACT_VERSION, oldSnapshot.contractVersion)
        assertEquals(WearableSnapshotStatus.CAPTURED_NO_ELIGIBLE_CONTEXT.name, oldSnapshot.statusCode)
        assertEquals(0, oldSnapshot.includedRecordCount)

        time.set(BASE + 250L)
        val laterPrediction = insertPrediction("later", BASE + 200L)
        repository.captureSnapshotForPrediction(laterPrediction)
        val laterSnapshot = requireNotNull(
            db.wearableShadowDao().getSnapshotForPrediction("prediction-later"),
        )
        assertEquals(WearableSnapshotStatus.CAPTURED.name, laterSnapshot.statusCode)
        assertEquals(1, laterSnapshot.includedRecordCount)
        assertEquals(WearableShadowContract.SNAPSHOT_CONTRACT_VERSION, laterSnapshot.contractVersion)
        assertEquals(1, db.wearableShadowDao().getSnapshotItems(laterSnapshot.snapshotId).size)

        val oldHash = oldSnapshot.payloadHash
        gateway.upsert(
            sleep("late-sleep", "source.a", BASE - 8 * HOUR, BASE - HOUR, BASE + 225L),
        )
        time.incrementAndGet()
        repository.refreshExplicit()
        assertEquals(
            oldHash,
            db.wearableShadowDao().getSnapshotForPrediction("prediction-old")?.payloadHash,
        )
        assertEquals(0, db.wearableShadowDao().getSnapshotForPrediction("prediction-old")?.includedRecordCount)
        assertTrue(db.wearableShadowDao().getSnapshotItems(oldSnapshot.snapshotId).isEmpty())
    }

    @Test
    fun snapshotKeepsAllEligibleSleepSessions_andMarksLatestEndingPrimary() = runBlocking {
        gateway.seed(
            sleep(
                "previous-day-long",
                "source.a",
                BASE - 34 * HOUR,
                BASE - 27 * HOUR,
                BASE - 27 * HOUR,
            ),
        )
        gateway.seed(
            sleep(
                "current-first",
                "source.a",
                BASE - 8 * HOUR,
                BASE - 5 * HOUR,
                BASE - 5 * HOUR,
            ),
        )
        gateway.seed(
            sleep(
                "current-latest",
                "source.a",
                BASE - 4 * HOUR,
                BASE - HOUR,
                BASE - HOUR,
            ),
        )
        connectAll()

        val prediction = insertPrediction("split-sleep", BASE)
        repository.captureSnapshotForPrediction(prediction)

        val dao = db.wearableShadowDao()
        val snapshot = requireNotNull(dao.getSnapshotForPrediction(prediction.predictionId))
        val items = dao.getSnapshotItems(snapshot.snapshotId)
        val known = dao.getRevisionsKnownAtCutoff(BASE - 48 * HOUR, BASE)
        val primary = known.single { it.revisionId == snapshot.primarySleepRevisionId }

        assertEquals(WearableShadowContract.SNAPSHOT_CONTRACT_VERSION, snapshot.contractVersion)
        assertEquals(3, snapshot.includedRecordCount)
        assertEquals(3, items.size)
        assertTrue(items.all { it.roleCode == WearableSnapshotRole.ELIGIBLE_SLEEP_SESSION.name })
        assertEquals(1, items.count { it.selectedForSummary })
        assertEquals("current-latest", primary.healthConnectRecordId)
        assertEquals(
            known.map { it.revisionId }.toSet(),
            items.map { it.revisionId }.toSet(),
        )
    }

    @Test
    fun revocationUnsupportedAndRemoteFailure_areExplicitAndNeverLeakRetainedValues() = runBlocking {
        gateway.seed(sleep("sleep", "source.a", BASE - 8 * HOUR, BASE - HOUR, BASE - 100))
        gateway.seed(hrv("hrv", "source.a", BASE - 2 * HOUR, BASE - 100, 45.0))
        connectAll()

        gateway.granted = emptySet()
        time.incrementAndGet()
        repository.refreshExplicit()
        val revoked = repository.homeContext.first()
        assertNull(revoked.primaryPriorSleep)
        assertTrue(revoked.retainedDataHiddenByAccessState)
        assertFalse(revoked.connection.isRefreshing)

        gateway.granted = setOf(WearableRecordType.SLEEP_SESSION)
        time.incrementAndGet()
        repository.refreshConnectionState("TEST_PARTIAL")
        val partial = repository.homeContext.first()
        assertEquals(WearableSyncStatus.PARTIAL_PERMISSION, partial.connection.status)
        assertTrue(partial.primaryPriorSleep != null)
        assertNull(partial.recentHrvRmssd)
        assertTrue(partial.retainedDataHiddenByAccessState)

        gateway.upsert(
            sleep("sleep", "source.a", BASE - 8 * HOUR, BASE - HOUR, BASE - 50),
        )
        gateway.failNextChanges = true
        time.incrementAndGet()
        repository.refreshExplicit()
        val failed = requireNotNull(
            db.wearableShadowDao().getSyncState(
                WearableShadowContract.SUBJECT_ID,
                WearableRecordType.SLEEP_SESSION.name,
            ),
        )
        assertEquals(WearableSyncStatus.INTERRUPTED.name, failed.statusCode)
        assertEquals(WearableSyncError.REMOTE_INTERRUPTION.name, failed.errorCode)
        assertFalse(repository.connectionSummary.first().isRefreshing)

        gateway.availabilityState = WearableAvailability.UNAVAILABLE
        time.incrementAndGet()
        repository.refreshConnectionState("TEST_UNSUPPORTED")
        val unsupported = repository.connectionSummary.first()
        assertEquals(WearableAvailability.UNAVAILABLE, unsupported.availability)
        assertEquals(WearableSyncStatus.PROVIDER_UNAVAILABLE, unsupported.status)
    }

    private suspend fun connectAll() {
        gateway.availabilityState = WearableAvailability.AVAILABLE
        gateway.granted = WearableRecordType.entries.toSet()
        repository.enableAfterPermissionResult(HealthConnectPermissions.requiredReadPermissions)
    }

    private suspend fun insertPrediction(suffix: String, cutoff: Long): RecoveryResearchPredictionEntity {
        val prediction = RecoveryResearchPredictionEntity(
            predictionId = "prediction-$suffix",
            targetWorkoutAttemptId = "attempt-$suffix",
            subjectId = RecoveryResearchVersions.SUBJECT_ID,
            sourceCode = RecoveryResearchVersions.SOURCE,
            workoutCreatedAtMillis = cutoff - 10,
            workoutStartedAtMillis = cutoff - 10,
            predictionGeneratedAtMillis = cutoff,
            inputDataCutoffAtMillis = cutoff,
            frozenAtMillis = cutoff,
            recoveryModelVersion = RecoveryResearchVersions.RECOVERY_MODEL,
            muscleHeatModelVersion = RecoveryResearchVersions.MUSCLE_HEAT_MODEL,
            progressiveOverloadModelVersion = RecoveryResearchVersions.PROGRESSIVE_OVERLOAD_MODEL,
            researchContractVersion = RecoveryResearchVersions.CONTRACT,
            researchSchemaVersion = RecoveryResearchVersions.RESEARCH_SCHEMA,
            firstWorkingSetRuleVersion = RecoveryResearchVersions.FIRST_WORKING_SET_RULE,
            catalogContentVersion = 1,
            planDayIdSnapshot = 1L,
            planDayNameSnapshot = "Synthetic",
            overallReadiness = 100,
            overallStatusCode = "READY",
            inputSessionIdsJson = "[]",
            inputSessionMaxTimestampMillis = null,
            inputDigestSha256 = "digest-$suffix",
            dataCompletenessCode = ResearchCompleteness.NONE.name,
            abstained = false,
            abstentionReasonCode = null,
            payloadSha256 = "payload-$suffix",
        )
        return db.recoveryResearchDao().getOrInsertFrozen(
            FrozenRecoveryResearchRecord(
                prediction = prediction,
                groups = emptyList(),
                muscles = emptyList(),
                targetExercises = emptyList(),
                initialOutcome = RecoveryResearchOutcomeEntity(
                    outcomeId = "outcome-$suffix",
                    predictionId = prediction.predictionId,
                    targetWorkoutAttemptId = prediction.targetWorkoutAttemptId,
                    sessionId = null,
                    statusCode = ResearchOutcomeStatus.ACTIVE.name,
                    statusReasonCode = null,
                    workoutCompletedAtMillis = null,
                    workoutAbandonedAtMillis = null,
                    firstValidSetObservedAtMillis = null,
                    firstWorkingSetLoggedAtMillis = null,
                    outcomeCapturedAtMillis = null,
                    dataCompletenessCode = ResearchCompleteness.NONE.name,
                    validForEvaluation = false,
                    exclusionReasonCode = null,
                ),
            ),
        )
    }

    private fun sleep(id: String, origin: String, start: Long, end: Long, modified: Long) =
        RawSleepSession(
            metadata = metadata(id, origin, modified),
            startTimeMillis = start,
            endTimeMillis = end,
            startZoneOffsetSeconds = 25_200,
            endZoneOffsetSeconds = 25_200,
            stages = emptyList(),
        )

    private fun hrv(id: String, origin: String, measured: Long, modified: Long, value: Double) =
        RawHrvRmssd(
            metadata = metadata(id, origin, modified),
            measurementTimeMillis = measured,
            zoneOffsetSeconds = 25_200,
            rmssdMillis = value,
        )

    private fun metadata(id: String, origin: String, modified: Long) = WearableSourceMetadata(
        healthConnectRecordId = id,
        dataOriginPackageName = origin,
        dataOriginDisplayName = "Synthetic",
        sourceLastModifiedAtMillis = modified,
        clientRecordId = null,
        clientRecordVersion = 0L,
        recordingMethod = 0,
        deviceType = null,
        deviceManufacturer = null,
        deviceModel = null,
    )

    private class FakeHealthConnectGateway : HealthConnectGateway {
        var availabilityState = WearableAvailability.AVAILABLE
        var granted: Set<WearableRecordType> = WearableRecordType.entries.toSet()
        var failNextChanges = false
        var expireNextChangesFor: WearableRecordType? = null
        private val current = WearableRecordType.entries.associateWith {
            linkedMapOf<String, RawWearableRecord>()
        }
        private val changes = WearableRecordType.entries.associateWith {
            mutableListOf<FakeChange>()
        }

        fun seed(record: RawWearableRecord) {
            current.getValue(record.type)[record.metadata.healthConnectRecordId] = record
        }

        fun upsert(record: RawWearableRecord) {
            current.getValue(record.type)[record.metadata.healthConnectRecordId] = record
            changes.getValue(record.type) += FakeChange.Upsert(record)
        }

        fun delete(type: WearableRecordType, recordId: String) {
            current.getValue(type).remove(recordId)
            changes.getValue(type) += FakeChange.Delete(recordId)
        }

        override fun availability() = availabilityState
        override suspend fun grantedTypes() = granted
        override suspend fun revokeAllPermissions() {
            granted = emptySet()
        }

        override fun manageAccessIntent(): Intent? = null

        override suspend fun getChangesToken(type: WearableRecordType): String =
            "${type.name}:${changes.getValue(type).size}"

        override suspend fun readPage(
            type: WearableRecordType,
            startTimeMillis: Long,
            endTimeMillis: Long,
            pageToken: String?,
        ) = WearableRawPage(
            records = if (pageToken == null) {
                current.getValue(type).values.filter { it.effectiveTimeMillis in startTimeMillis..endTimeMillis }
            } else {
                emptyList()
            },
            nextPageToken = null,
        )

        override suspend fun readChanges(token: String): WearableChangePage {
            if (failNextChanges) {
                failNextChanges = false
                throw RemoteException("synthetic interruption")
            }
            val separator = token.lastIndexOf(':')
            val type = WearableRecordType.valueOf(token.substring(0, separator))
            val index = token.substring(separator + 1).toInt()
            val available = changes.getValue(type)
            if (expireNextChangesFor == type) {
                expireNextChangesFor = null
                return WearableChangePage(
                    upserts = emptyList(),
                    deletedRecordIds = emptyList(),
                    nextChangesToken = token,
                    hasMore = false,
                    tokenExpired = true,
                )
            }
            val pending = available.drop(index)
            return WearableChangePage(
                upserts = pending.mapNotNull { (it as? FakeChange.Upsert)?.record },
                deletedRecordIds = pending.mapNotNull { (it as? FakeChange.Delete)?.recordId },
                nextChangesToken = "${type.name}:${available.size}",
                hasMore = false,
                tokenExpired = false,
            )
        }
    }

    private sealed interface FakeChange {
        data class Upsert(val record: RawWearableRecord) : FakeChange
        data class Delete(val recordId: String) : FakeChange
    }

    private companion object {
        const val BASE = 1_900_000_000_000L
        const val HOUR = 3_600_000L
    }
}
