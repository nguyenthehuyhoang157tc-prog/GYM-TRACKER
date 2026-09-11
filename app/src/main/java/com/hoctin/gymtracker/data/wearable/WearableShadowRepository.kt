package com.hoctin.gymtracker.data.wearable

import android.content.Intent
import android.os.RemoteException
import androidx.room.withTransaction
import com.hoctin.gymtracker.data.AppDatabase
import com.hoctin.gymtracker.data.research.RecoveryResearchDao
import com.hoctin.gymtracker.data.research.RecoveryResearchPredictionEntity
import com.hoctin.gymtracker.data.research.ResearchConsentState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private data class WearableRuntimeState(
    val availability: WearableAvailability = WearableAvailability.UNKNOWN,
    val grantedTypes: Set<WearableRecordType> = emptySet(),
    val isRefreshing: Boolean = false,
    val lastError: WearableSyncError = WearableSyncError.NONE,
)

class WearableShadowRepository(
    private val database: AppDatabase,
    private val dao: WearableShadowDao,
    private val recoveryResearchDao: RecoveryResearchDao,
    private val gateway: HealthConnectGateway,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val runtime = MutableStateFlow(WearableRuntimeState())
    private val syncMutex = Mutex()

    val connectionSummary: Flow<WearableConnectionSummary> = combine(
        recoveryResearchDao.observeConsent(),
        dao.observeSettings(WearableShadowContract.SUBJECT_ID),
        dao.observeSyncStates(WearableShadowContract.SUBJECT_ID),
        runtime,
    ) { consent, settings, syncStates, runtimeState ->
        val researchEnabled = consent?.stateCode == ResearchConsentState.GRANTED.name
        val lastSuccess = syncStates.mapNotNull { it.lastSuccessfulSyncAtMillis }.maxOrNull()
        val persistedError = syncStates
            .mapNotNull { state -> WearableSyncError.entries.firstOrNull { it.name == state.errorCode } }
            .firstOrNull { it != WearableSyncError.NONE }
        val availability = if (runtimeState.availability != WearableAvailability.UNKNOWN) {
            runtimeState.availability
        } else {
            settings?.lastAvailabilityCode?.let { code ->
                WearableAvailability.entries.firstOrNull { it.name == code }
            } ?: WearableAvailability.UNKNOWN
        }
        val granted = if (runtimeState.availability != WearableAvailability.UNKNOWN) {
            runtimeState.grantedTypes
        } else {
            WearableTypeMask.decode(settings?.grantedTypeMask.orEmpty())
        }
        val syncEnabled = settings?.syncEnabled == true
        val status = when {
            runtimeState.isRefreshing -> WearableSyncStatus.SYNCING
            availability == WearableAvailability.PROVIDER_UPDATE_REQUIRED ->
                WearableSyncStatus.PROVIDER_UNAVAILABLE
            availability == WearableAvailability.UNAVAILABLE -> WearableSyncStatus.PROVIDER_UNAVAILABLE
            !syncEnabled -> WearableSyncStatus.IDLE
            granted.isEmpty() -> WearableSyncStatus.PERMISSION_DENIED
            syncStates.any { it.statusCode == WearableSyncStatus.FAILED.name } -> WearableSyncStatus.FAILED
            granted.size < WearableRecordType.entries.size -> WearableSyncStatus.PARTIAL_PERMISSION
            lastSuccess != null -> WearableSyncStatus.SUCCESS
            else -> WearableSyncStatus.NEVER_SYNCED
        }
        WearableConnectionSummary(
            recoveryResearchEnabled = researchEnabled,
            shadowSyncEnabled = syncEnabled,
            availability = availability,
            grantedTypes = granted,
            isRefreshing = runtimeState.isRefreshing,
            status = status,
            lastSuccessfulSyncAtMillis = lastSuccess,
            lastError = runtimeState.lastError.takeUnless { it == WearableSyncError.NONE }
                ?: persistedError
                ?: WearableSyncError.NONE,
        )
    }.distinctUntilChanged()

    val homeContext: Flow<WearableHomeContext> = combine(
        connectionSummary,
        dao.observeActiveRevisions(),
    ) { connection, revisions ->
        WearableContextSelector.homeContext(
            revisions = revisions.map(WearableRevisionWithStages::revision),
            cutoffAtMillis = clock(),
            connection = connection,
        )
    }.distinctUntilChanged()

    val latestSnapshotDiagnostic: Flow<WearableSnapshotDiagnostic?> = dao.observeLatestSnapshot()
        .map { snapshot ->
            snapshot?.let {
                WearableSnapshotDiagnostic(
                    status = WearableSnapshotStatus.entries.firstOrNull { status ->
                        status.name == it.statusCode
                    } ?: WearableSnapshotStatus.CAPTURED_NO_ELIGIBLE_CONTEXT,
                    cutoffAtMillis = it.cutoffAtMillis,
                    createdAtMillis = it.createdAtMillis,
                    includedRecordCount = it.includedRecordCount,
                    includedSourceCount = it.includedSourceCount,
                    payloadHash = it.payloadHash,
                )
            }
        }
        .distinctUntilChanged()

    fun manageAccessIntent(): Intent? = gateway.manageAccessIntent()

    /** Status only: no health records are read and no permission prompt is launched. */
    suspend fun refreshConnectionState(reasonCode: String = "STATUS_CHECK") = withContext(Dispatchers.IO) {
        val now = clock()
        val availabilityResult = runCatching { gateway.availability() }
        if (availabilityResult.isFailure) {
            runtime.value = runtime.value.copy(
                lastError = classify(requireNotNull(availabilityResult.exceptionOrNull())),
            )
            return@withContext
        }
        val availability = availabilityResult.getOrThrow()
        val grantedResult = if (availability == WearableAvailability.AVAILABLE) {
            runCatching { gateway.grantedTypes() }
        } else {
            Result.success(emptySet())
        }
        if (grantedResult.isFailure) {
            // A provider interruption is not evidence of revocation. Keep the last known permission
            // boundary and retry later instead of manufacturing an empty grant event.
            runtime.value = runtime.value.copy(
                availability = availability,
                lastError = classify(requireNotNull(grantedResult.exceptionOrNull())),
            )
            return@withContext
        }
        val granted = grantedResult.getOrThrow()
        persistAccessState(
            availability = availability,
            grantedTypes = granted,
            enabledOverride = null,
            occurredAtMillis = now,
            reasonCode = reasonCode,
        )
        runtime.value = runtime.value.copy(
            availability = availability,
            grantedTypes = granted,
            lastError = availability.toError(),
        )
    }

    suspend fun enableAfterPermissionResult(grantedPermissions: Set<String>) = withContext(Dispatchers.IO) {
        if (!isResearchGranted()) return@withContext
        val now = clock()
        val availability = gateway.availability()
        val grantedTypes = HealthConnectPermissions.grantedTypes(grantedPermissions)
        persistAccessState(
            availability = availability,
            grantedTypes = grantedTypes,
            enabledOverride = grantedTypes.isNotEmpty(),
            occurredAtMillis = now,
            reasonCode = "PERMISSION_RESULT",
            forceEvent = true,
        )
        runtime.value = WearableRuntimeState(
            availability = availability,
            grantedTypes = grantedTypes,
            lastError = if (grantedTypes.isEmpty()) {
                WearableSyncError.PERMISSION_DENIED
            } else {
                WearableSyncError.NONE
            },
        )
        if (grantedTypes.isNotEmpty()) sync("PERMISSION_GRANTED")
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        val availability = runCatching { gateway.availability() }
            .getOrDefault(WearableAvailability.UNAVAILABLE)
        runCatching { gateway.revokeAllPermissions() }
        persistAccessState(
            availability = availability,
            grantedTypes = emptySet(),
            enabledOverride = false,
            occurredAtMillis = clock(),
            reasonCode = "USER_DISCONNECT",
            forceEvent = true,
        )
        runtime.value = WearableRuntimeState(
            availability = availability,
            grantedTypes = emptySet(),
            lastError = WearableSyncError.NONE,
        )
    }

    suspend fun pauseForResearchRevocation() = withContext(Dispatchers.IO) {
        val settings = dao.getSettings(WearableShadowContract.SUBJECT_ID) ?: return@withContext
        val now = clock()
        dao.upsertSettings(settings.copy(syncEnabled = false, updatedAtMillis = now))
        insertAccessEventIfChanged(
            enabled = false,
            availability = runtime.value.availability,
            grantedTypes = runtime.value.grantedTypes,
            occurredAtMillis = now,
            reasonCode = "RESEARCH_CONSENT_REVOKED",
            force = true,
        )
    }

    suspend fun deleteImportedHealthData() = withContext(Dispatchers.IO) {
        database.withTransaction { dao.deleteAllWearableShadowData() }
        runtime.value = runtime.value.copy(isRefreshing = false, lastError = WearableSyncError.NONE)
    }

    suspend fun syncForeground() = sync("FOREGROUND")

    suspend fun refreshExplicit() = sync("EXPLICIT_REFRESH")

    private suspend fun sync(reasonCode: String) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            refreshConnectionState("${reasonCode}_STATUS")
            if (!isResearchGranted()) return@withLock
            val settings = dao.getSettings(WearableShadowContract.SUBJECT_ID) ?: return@withLock
            val currentRuntime = runtime.value
            if (!settings.syncEnabled || currentRuntime.availability != WearableAvailability.AVAILABLE) {
                return@withLock
            }
            runtime.value = currentRuntime.copy(isRefreshing = true, lastError = WearableSyncError.NONE)
            var overallError = WearableSyncError.NONE
            try {
                WearableRecordType.entries.forEach { type ->
                    val error = if (type in currentRuntime.grantedTypes) {
                        syncType(type)
                    } else {
                        markPermissionMissing(type)
                        WearableSyncError.NONE
                    }
                    if (overallError == WearableSyncError.NONE && error != WearableSyncError.NONE) {
                        overallError = error
                    }
                }
                dao.pruneUnreferencedRevisionsBefore(
                    clock() - TimeUnit.DAYS.toMillis(WearableShadowContract.MAX_STORED_LOOKBACK_DAYS),
                )
            } catch (cancelled: CancellationException) {
                overallError = WearableSyncError.REMOTE_INTERRUPTION
                throw cancelled
            } catch (failure: Throwable) {
                overallError = classify(failure)
                // Shadow synchronization must never crash or block normal AURON behavior. Per-type
                // failures are persisted above; an unexpected outer failure remains a local status.
            } finally {
                runtime.value = runtime.value.copy(isRefreshing = false, lastError = overallError)
            }
        }
    }

    private suspend fun syncType(type: WearableRecordType): WearableSyncError {
        val now = clock()
        val existing = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name)
            ?: emptySyncState(type, now)
        dao.upsertSyncState(
            existing.copy(
                permissionGranted = true,
                statusCode = WearableSyncStatus.SYNCING.name,
                errorCode = WearableSyncError.NONE.name,
                lastAttemptAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        return try {
            val finalToken = if (!existing.initialSyncComplete || existing.changesToken == null) {
                initialReadAndCatchUp(type, now)
            } else {
                consumeChanges(type, existing.changesToken)
            }
            val successAt = clock()
            val latest = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name) ?: existing
            dao.upsertSyncState(
                latest.copy(
                    changesToken = finalToken,
                    initialSyncComplete = true,
                    permissionGranted = true,
                    statusCode = WearableSyncStatus.SUCCESS.name,
                    errorCode = WearableSyncError.NONE.name,
                    lastAttemptAtMillis = now,
                    lastSuccessfulSyncAtMillis = successAt,
                    updatedAtMillis = successAt,
                ),
            )
            WearableSyncError.NONE
        } catch (_: ExpiredChangesTokenException) {
            runCatching {
                val token = initialReadAndCatchUp(type, clock())
                val successAt = clock()
                val latest = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name) ?: existing
                dao.upsertSyncState(
                    latest.copy(
                        changesToken = token,
                        initialSyncComplete = true,
                        permissionGranted = true,
                        statusCode = WearableSyncStatus.SUCCESS.name,
                        errorCode = WearableSyncError.NONE.name,
                        lastAttemptAtMillis = now,
                        lastSuccessfulSyncAtMillis = successAt,
                        updatedAtMillis = successAt,
                    ),
                )
            }.fold(
                onSuccess = { WearableSyncError.NONE },
                onFailure = { failure -> markSyncFailure(type, existing, now, classify(failure)) },
            )
        } catch (failure: Throwable) {
            markSyncFailure(type, existing, now, classify(failure))
        }
    }

    /** Token-before-read plus immediate catch-up closes the initial-read update race. */
    private suspend fun initialReadAndCatchUp(type: WearableRecordType, observedAt: Long): String {
        val initialToken = gateway.getChangesToken(type)
        val readStart = observedAt - TimeUnit.DAYS.toMillis(
            WearableShadowContract.INITIAL_READ_LOOKBACK_DAYS,
        )
        val seenIds = linkedSetOf<String>()
        var pageToken: String? = null
        do {
            val page = gateway.readPage(type, readStart, observedAt, pageToken)
            seenIds += page.records.map { it.metadata.healthConnectRecordId }
            persistUpserts(type, page.records, clock())
            pageToken = page.nextPageToken
        } while (pageToken != null)

        // A one-day boundary margin avoids falsely tombstoning an overnight interval whose start
        // falls just outside the bounded read filter.
        val reconcileStart = readStart + TimeUnit.DAYS.toMillis(1)
        val reconciledAt = clock()
        val activeIds = dao.getActiveRecordIdsInWindow(type.name, reconcileStart, observedAt)
        database.withTransaction {
            activeIds.filterNot(seenIds::contains).forEach { missingId ->
                dao.markDeleted(type.name, missingId, reconciledAt)
            }
        }
        return consumeChanges(type, initialToken)
    }

    private suspend fun consumeChanges(
        type: WearableRecordType,
        startingToken: String,
    ): String {
        var token = startingToken
        do {
            val page = gateway.readChanges(token)
            if (page.tokenExpired) throw ExpiredChangesTokenException()
            val pageObservedAt = clock()
            database.withTransaction {
                persistUpsertsInTransaction(type, page.upserts, pageObservedAt)
                page.deletedRecordIds.forEach { recordId ->
                    dao.markDeleted(type.name, recordId, pageObservedAt)
                }
            }
            token = page.nextChangesToken
        } while (page.hasMore)
        return token
    }

    private suspend fun persistUpserts(
        expectedType: WearableRecordType,
        records: List<RawWearableRecord>,
        observedAt: Long,
    ) {
        database.withTransaction {
            persistUpsertsInTransaction(expectedType, records, observedAt)
        }
    }

    private suspend fun persistUpsertsInTransaction(
        expectedType: WearableRecordType,
        records: List<RawWearableRecord>,
        observedAt: Long,
    ) {
        val earliest = observedAt - TimeUnit.DAYS.toMillis(WearableShadowContract.MAX_STORED_LOOKBACK_DAYS)
        val latest = observedAt + TimeUnit.MINUTES.toMillis(5)
        val eligibleRecords = records.asSequence()
            .filter { it.type == expectedType }
            .filter { it.effectiveTimeMillis in earliest..latest }
            .toList()
        var rejected = 0L
        eligibleRecords.forEach { record ->
            val draft = WearableNormalizer.normalize(record)
            if (draft == null) {
                rejected += 1L
            } else {
                persistDraftInTransaction(draft, observedAt)
            }
        }
        if (rejected > 0L) {
            dao.incrementRejectedRecordCount(
                subjectId = WearableShadowContract.SUBJECT_ID,
                recordTypeCode = expectedType.name,
                count = rejected,
                updatedAtMillis = observedAt,
            )
        }
    }

    private suspend fun persistDraftInTransaction(
        draft: NormalizedWearableRevisionDraft,
        observedAt: Long,
    ) {
        val metadata = draft.metadata
        val active = dao.getActiveRevision(draft.type.name, metadata.healthConnectRecordId)
        if (active != null &&
            active.sourceLastModifiedAtMillis == metadata.sourceLastModifiedAtMillis &&
            active.payloadHash == draft.payloadHash
        ) {
            dao.touchRevision(active.revisionId, observedAt)
            return
        }
        val priorMatchingRevision = dao.getLatestMatchingRevision(
            recordTypeCode = draft.type.name,
            healthConnectRecordId = metadata.healthConnectRecordId,
            sourceLastModifiedAtMillis = metadata.sourceLastModifiedAtMillis,
            payloadHash = draft.payloadHash,
        )
        if (active != null && priorMatchingRevision != null) {
            // An out-of-order replay of a losing historical revision is idempotent. Only a true
            // delete->re-upsert (no active row) begins a fresh lifecycle epoch.
            dao.touchRevision(priorMatchingRevision.revisionId, observedAt)
            return
        }
        val lifecycleEpoch = (dao.getMaxLifecycleEpoch(
            draft.type.name,
            metadata.healthConnectRecordId,
        ) ?: -1L) + 1L
        val revisionId = WearableHashing.sha256(
            WearableShadowContract.CONTRACT_VERSION,
            draft.type.name,
            metadata.healthConnectRecordId,
            metadata.sourceLastModifiedAtMillis,
            draft.payloadHash,
            lifecycleEpoch,
        )
        val incomingWins = active == null ||
            metadata.sourceLastModifiedAtMillis > active.sourceLastModifiedAtMillis ||
            (
                metadata.sourceLastModifiedAtMillis == active.sourceLastModifiedAtMillis &&
                    draft.payloadHash > active.payloadHash
                )
        if (active != null && incomingWins) dao.markSuperseded(active.revisionId, observedAt)
        val inserted = dao.insertRevision(
            WearableRecordRevisionEntity(
                revisionId = revisionId,
                subjectId = WearableShadowContract.SUBJECT_ID,
                contractVersion = WearableShadowContract.CONTRACT_VERSION,
                schemaVersion = WearableShadowContract.SCHEMA_VERSION,
                recordTypeCode = draft.type.name,
                healthConnectRecordId = metadata.healthConnectRecordId,
                lifecycleEpoch = lifecycleEpoch,
                dataOriginPackageName = metadata.dataOriginPackageName,
                dataOriginDisplayName = metadata.dataOriginDisplayName,
                sourceLastModifiedAtMillis = metadata.sourceLastModifiedAtMillis,
                clientRecordId = metadata.clientRecordId,
                clientRecordVersion = metadata.clientRecordVersion,
                recordingMethod = metadata.recordingMethod,
                deviceType = metadata.deviceType,
                deviceManufacturer = metadata.deviceManufacturer,
                deviceModel = metadata.deviceModel,
                payloadHash = draft.payloadHash,
                startTimeMillis = draft.startTimeMillis,
                endTimeMillis = draft.endTimeMillis,
                measurementTimeMillis = draft.measurementTimeMillis,
                effectiveTimeMillis = draft.effectiveTimeMillis,
                startZoneOffsetSeconds = draft.startZoneOffsetSeconds,
                endZoneOffsetSeconds = draft.endZoneOffsetSeconds,
                measurementZoneOffsetSeconds = draft.measurementZoneOffsetSeconds,
                sessionDurationMillis = draft.sessionDurationMillis,
                knownSleepDurationMillis = draft.knownSleepDurationMillis,
                awakeDurationMillis = draft.awakeDurationMillis,
                lightDurationMillis = draft.lightDurationMillis,
                deepDurationMillis = draft.deepDurationMillis,
                remDurationMillis = draft.remDurationMillis,
                unspecifiedSleepDurationMillis = draft.unspecifiedSleepDurationMillis,
                unknownStageDurationMillis = draft.unknownStageDurationMillis,
                uncoveredStageDurationMillis = draft.uncoveredStageDurationMillis,
                stageCoverage = draft.stageCoverage,
                sleepEfficiency = draft.sleepEfficiency,
                stageTimelineStatusCode = draft.stageTimelineStatus?.name,
                metricValue = draft.metricValue,
                metricUnitCode = draft.metricUnitCode,
                firstSeenAtMillis = observedAt,
                lastSeenAtMillis = observedAt,
                supersededAtMillis = observedAt.takeUnless { incomingWins },
                deletedAtMillis = null,
            ),
        )
        if (inserted != -1L && draft.sleepStages.isNotEmpty()) {
            dao.insertSleepStages(
                draft.sleepStages.map { stage ->
                    WearableSleepStageEntity(
                        stageId = WearableHashing.sha256(
                            revisionId,
                            stage.ordinal,
                            stage.stageType.name,
                            stage.sourceStartTimeMillis,
                            stage.sourceEndTimeMillis,
                        ),
                        revisionId = revisionId,
                        ordinal = stage.ordinal,
                        stageTypeCode = stage.stageType.name,
                        sourceStartTimeMillis = stage.sourceStartTimeMillis,
                        sourceEndTimeMillis = stage.sourceEndTimeMillis,
                        normalizedStartTimeMillis = stage.normalizedStartTimeMillis,
                        normalizedEndTimeMillis = stage.normalizedEndTimeMillis,
                        durationMillis = stage.durationMillis,
                        wasClipped = stage.wasClipped,
                        isValid = stage.isValid,
                    )
                },
            )
        }
    }

    /**
     * Freezes locally available context only. It intentionally performs no Health Connect read, so
     * provider latency cannot move or influence the already-frozen production prediction cutoff.
     */
    suspend fun captureSnapshotForPrediction(prediction: RecoveryResearchPredictionEntity) =
        withContext(Dispatchers.IO) {
            database.withTransaction {
            if (dao.getSnapshotForPrediction(prediction.predictionId) != null) return@withTransaction
            val cutoff = prediction.inputDataCutoffAtMillis
            val access = dao.getLatestAccessEventAtOrBefore(
                WearableShadowContract.SUBJECT_ID,
                cutoff,
            )
            val availability = access?.availabilityCode?.let { code ->
                WearableAvailability.entries.firstOrNull { it.name == code }
            } ?: WearableAvailability.UNKNOWN
            val grantedTypes = WearableTypeMask.decode(access?.grantedTypeMask.orEmpty())
            val baseStatus = when {
                access == null -> WearableSnapshotStatus.NO_ACCESS_EVIDENCE
                !access.syncEnabled -> WearableSnapshotStatus.SHADOW_DISABLED
                availability != WearableAvailability.AVAILABLE -> WearableSnapshotStatus.PROVIDER_UNAVAILABLE
                grantedTypes.isEmpty() -> WearableSnapshotStatus.NO_GRANTED_PERMISSION
                else -> null
            }
            val knownRevisions = if (baseStatus == null) {
                dao.getRevisionsKnownAtCutoff(
                    startMillis = cutoff - TimeUnit.HOURS.toMillis(
                        WearableShadowContract.RECENT_METRIC_LOOKBACK_HOURS,
                    ),
                    cutoffAtMillis = cutoff,
                )
            } else {
                emptyList()
            }
            val selection = WearableContextSelector.select(knownRevisions, cutoff, grantedTypes)
            val status = baseStatus ?: if (selection.eligible.isEmpty()) {
                WearableSnapshotStatus.CAPTURED_NO_ELIGIBLE_CONTEXT
            } else {
                WearableSnapshotStatus.CAPTURED
            }
            val snapshotId = WearableHashing.sha256(
                WearableShadowContract.SNAPSHOT_CONTRACT_VERSION,
                prediction.predictionId,
            )
            val selectedIds = setOfNotNull(
                selection.primarySleep?.revisionId,
                selection.recentHrv?.revisionId,
                selection.recentRestingHeartRate?.revisionId,
            )
            val items = selection.eligible.map { revision ->
                val role = when (revision.recordTypeCode) {
                    WearableRecordType.SLEEP_SESSION.name -> WearableSnapshotRole.ELIGIBLE_SLEEP_SESSION
                    WearableRecordType.HRV_RMSSD.name -> WearableSnapshotRole.ELIGIBLE_HRV_RMSSD
                    else -> WearableSnapshotRole.ELIGIBLE_RESTING_HEART_RATE
                }
                WearableContextSnapshotItemEntity(
                    snapshotItemId = WearableHashing.sha256(snapshotId, revision.revisionId, role.name),
                    snapshotId = snapshotId,
                    revisionId = revision.revisionId,
                    roleCode = role.name,
                    selectedForSummary = revision.revisionId in selectedIds,
                )
            }
            val payloadHash = WearableHashing.sha256(
                WearableShadowContract.SNAPSHOT_CONTRACT_VERSION,
                WearableShadowContract.SLEEP_SELECTION_POLICY_VERSION,
                prediction.predictionId,
                prediction.targetWorkoutAttemptId,
                cutoff,
                status.name,
                access?.accessEventId,
                availability.name,
                WearableTypeMask.encode(grantedTypes),
                items.joinToString(";") { "${it.snapshotItemId}:${it.selectedForSummary}" },
                selection.primarySleep?.revisionId,
                selection.recentHrv?.revisionId,
                selection.recentRestingHeartRate?.revisionId,
                selection.overlappingSleepAlternatives,
            )
            if (items.isNotEmpty()) dao.insertSnapshotItems(items)
            dao.insertSnapshot(
                    WearableContextSnapshotEntity(
                        snapshotId = snapshotId,
                        predictionId = prediction.predictionId,
                        targetWorkoutAttemptId = prediction.targetWorkoutAttemptId,
                        contractVersion = WearableShadowContract.SNAPSHOT_CONTRACT_VERSION,
                        schemaVersion = WearableShadowContract.SCHEMA_VERSION,
                        cutoffAtMillis = cutoff,
                        createdAtMillis = clock().coerceAtLeast(cutoff),
                        statusCode = status.name,
                        accessEventId = access?.accessEventId,
                        availabilityCode = availability.name,
                        grantedTypeMask = WearableTypeMask.encode(grantedTypes),
                        includedRecordCount = items.size,
                        includedSourceCount = selection.eligible
                            .map { it.dataOriginPackageName }
                            .distinct()
                            .size,
                        primarySleepRevisionId = selection.primarySleep?.revisionId,
                        recentHrvRevisionId = selection.recentHrv?.revisionId,
                        recentRestingHeartRateRevisionId = selection.recentRestingHeartRate?.revisionId,
                        overlappingSleepAlternatives = selection.overlappingSleepAlternatives,
                        payloadHash = payloadHash,
                    ),
                )
            }
        }

    private suspend fun persistAccessState(
        availability: WearableAvailability,
        grantedTypes: Set<WearableRecordType>,
        enabledOverride: Boolean?,
        occurredAtMillis: Long,
        reasonCode: String,
        forceEvent: Boolean = false,
    ) {
        if (!hasResearchConsentRow()) return
        val existing = dao.getSettings(WearableShadowContract.SUBJECT_ID)
        val enabled = enabledOverride ?: existing?.syncEnabled ?: false
        dao.upsertSettings(
            WearableShadowSettingsEntity(
                subjectId = WearableShadowContract.SUBJECT_ID,
                contractVersion = WearableShadowContract.CONTRACT_VERSION,
                schemaVersion = WearableShadowContract.SCHEMA_VERSION,
                syncEnabled = enabled,
                lastAvailabilityCode = availability.name,
                grantedTypeMask = WearableTypeMask.encode(grantedTypes),
                createdAtMillis = existing?.createdAtMillis ?: occurredAtMillis,
                updatedAtMillis = occurredAtMillis,
            ),
        )
        WearableRecordType.entries.forEach { type ->
            val syncState = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name)
                ?: emptySyncState(type, occurredAtMillis)
            dao.upsertSyncState(
                syncState.copy(
                    permissionGranted = type in grantedTypes,
                    statusCode = when {
                        availability != WearableAvailability.AVAILABLE ->
                            WearableSyncStatus.PROVIDER_UNAVAILABLE.name
                        type !in grantedTypes -> WearableSyncStatus.PERMISSION_DENIED.name
                        else -> syncState.statusCode
                    },
                    errorCode = when {
                        availability == WearableAvailability.PROVIDER_UPDATE_REQUIRED ->
                            WearableSyncError.PROVIDER_UPDATE_REQUIRED.name
                        availability != WearableAvailability.AVAILABLE ->
                            WearableSyncError.PROVIDER_UNAVAILABLE.name
                        type !in grantedTypes -> WearableSyncError.PERMISSION_DENIED.name
                        else -> syncState.errorCode
                    },
                    updatedAtMillis = occurredAtMillis,
                ),
            )
        }
        insertAccessEventIfChanged(
            enabled = enabled,
            availability = availability,
            grantedTypes = grantedTypes,
            occurredAtMillis = occurredAtMillis,
            reasonCode = reasonCode,
            force = forceEvent,
        )
    }

    private suspend fun insertAccessEventIfChanged(
        enabled: Boolean,
        availability: WearableAvailability,
        grantedTypes: Set<WearableRecordType>,
        occurredAtMillis: Long,
        reasonCode: String,
        force: Boolean,
    ) {
        val mask = WearableTypeMask.encode(grantedTypes)
        val latest = dao.getLatestAccessEvent(WearableShadowContract.SUBJECT_ID)
        if (!force && latest != null && latest.syncEnabled == enabled &&
            latest.availabilityCode == availability.name && latest.grantedTypeMask == mask
        ) {
            return
        }
        val eventId = WearableHashing.sha256(
            WearableShadowContract.CONTRACT_VERSION,
            WearableShadowContract.SUBJECT_ID,
            occurredAtMillis,
            enabled,
            availability.name,
            mask,
            reasonCode,
        )
        dao.insertAccessEvent(
            WearableShadowAccessEventEntity(
                accessEventId = eventId,
                subjectId = WearableShadowContract.SUBJECT_ID,
                occurredAtMillis = occurredAtMillis,
                syncEnabled = enabled,
                availabilityCode = availability.name,
                grantedTypeMask = mask,
                reasonCode = reasonCode,
            ),
        )
    }

    private suspend fun markPermissionMissing(type: WearableRecordType) {
        val now = clock()
        val state = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name)
            ?: emptySyncState(type, now)
        dao.upsertSyncState(
            state.copy(
                permissionGranted = false,
                statusCode = WearableSyncStatus.PERMISSION_DENIED.name,
                errorCode = WearableSyncError.PERMISSION_DENIED.name,
                lastAttemptAtMillis = now,
                updatedAtMillis = now,
            ),
        )
    }

    private suspend fun markSyncFailure(
        type: WearableRecordType,
        existing: WearableShadowSyncStateEntity,
        attemptAt: Long,
        error: WearableSyncError,
    ): WearableSyncError {
        val now = clock()
        val latest = dao.getSyncState(WearableShadowContract.SUBJECT_ID, type.name) ?: existing
        dao.upsertSyncState(
            latest.copy(
                permissionGranted = error != WearableSyncError.PERMISSION_DENIED,
                statusCode = when (error) {
                    WearableSyncError.PERMISSION_DENIED -> WearableSyncStatus.PERMISSION_DENIED.name
                    WearableSyncError.REMOTE_INTERRUPTION -> WearableSyncStatus.INTERRUPTED.name
                    else -> WearableSyncStatus.FAILED.name
                },
                errorCode = error.name,
                lastAttemptAtMillis = attemptAt,
                updatedAtMillis = now,
            ),
        )
        return error
    }

    private fun emptySyncState(type: WearableRecordType, now: Long) =
        WearableShadowSyncStateEntity(
            subjectId = WearableShadowContract.SUBJECT_ID,
            recordTypeCode = type.name,
            changesToken = null,
            initialSyncComplete = false,
            permissionGranted = false,
            statusCode = WearableSyncStatus.NEVER_SYNCED.name,
            errorCode = WearableSyncError.NONE.name,
            lastAttemptAtMillis = null,
            lastSuccessfulSyncAtMillis = null,
            rejectedRecordCount = 0L,
            updatedAtMillis = now,
        )

    private suspend fun hasResearchConsentRow(): Boolean = recoveryResearchDao.getConsent() != null

    private suspend fun isResearchGranted(): Boolean =
        recoveryResearchDao.getConsent()?.stateCode == ResearchConsentState.GRANTED.name

    private fun classify(failure: Throwable): WearableSyncError = when (failure) {
        is SecurityException -> WearableSyncError.PERMISSION_DENIED
        is RemoteException -> WearableSyncError.REMOTE_INTERRUPTION
        is IllegalStateException -> WearableSyncError.PROVIDER_UNAVAILABLE
        else -> WearableSyncError.UNKNOWN
    }

    private fun WearableAvailability.toError(): WearableSyncError = when (this) {
        WearableAvailability.PROVIDER_UPDATE_REQUIRED -> WearableSyncError.PROVIDER_UPDATE_REQUIRED
        WearableAvailability.UNAVAILABLE -> WearableSyncError.PROVIDER_UNAVAILABLE
        else -> WearableSyncError.NONE
    }

    private class ExpiredChangesTokenException : RuntimeException()
}
