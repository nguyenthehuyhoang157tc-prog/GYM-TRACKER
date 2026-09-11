package com.hoctin.gymtracker.data.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearableContextSelectorTest {
    @Test
    fun napAndMainSleep_remainSeparate_andLatestEndingSessionIsPrimary() {
        val cutoff = 10_000_000L
        val main = revision("main", WearableRecordType.SLEEP_SESSION, cutoff - 8 * HOUR, cutoff - HOUR, 7 * HOUR)
        val nap = revision("nap", WearableRecordType.SLEEP_SESSION, cutoff - 12 * HOUR, cutoff - 11 * HOUR, HOUR)

        val result = WearableContextSelector.select(listOf(nap, main), cutoff)

        assertEquals("main", result.primarySleep?.revisionId)
        assertEquals(2, result.eligible.size)
        assertEquals(0, result.overlappingSleepAlternatives)
    }

    @Test
    fun previousDayLongSessionAndCurrentSplitSleep_selectLatestCurrentSessionWithoutMerging() {
        val cutoff = 100 * HOUR
        val previousDayLong = revision(
            "previous-day-long",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 34 * HOUR,
            cutoff - 27 * HOUR,
            7 * HOUR,
        )
        val currentFirst = revision(
            "current-first",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 8 * HOUR,
            cutoff - 5 * HOUR,
            3 * HOUR,
        )
        val currentLatest = revision(
            "current-latest",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 4 * HOUR,
            cutoff - HOUR,
            3 * HOUR,
        )

        val result = WearableContextSelector.select(
            listOf(previousDayLong, currentLatest, currentFirst),
            cutoff,
        )

        assertEquals("current-latest", result.primarySleep?.revisionId)
        assertEquals(3, result.eligible.size)
        assertEquals(
            setOf("previous-day-long", "current-first", "current-latest"),
            result.eligible.map { it.revisionId }.toSet(),
        )
        val home = WearableContextSelector.homeContext(
            revisions = listOf(previousDayLong, currentLatest, currentFirst),
            cutoffAtMillis = cutoff,
            connection = WearableConnectionSummary(
                recoveryResearchEnabled = true,
                shadowSyncEnabled = true,
                availability = WearableAvailability.AVAILABLE,
                grantedTypes = WearableRecordType.entries.toSet(),
            ),
        )
        assertEquals("current-latest", home.primaryPriorSleep?.revisionId)
        assertEquals(2, home.otherPriorSleepSessions)
    }

    @Test
    fun latestEndingSessionWinsBeforeDuration() {
        val cutoff = 80 * HOUR
        val olderLong = revision(
            "older-long",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 10 * HOUR,
            cutoff - 2 * HOUR,
            8 * HOUR,
        )
        val latestShort = revision(
            "latest-short",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 2 * HOUR,
            cutoff - HOUR,
            HOUR,
        )

        val result = WearableContextSelector.select(listOf(olderLong, latestShort), cutoff)

        assertEquals("latest-short", result.primarySleep?.revisionId)
        assertEquals(2, result.eligible.size)
    }

    @Test
    fun overlappingSources_areNotSummed_andTieBreakIsDeterministic() {
        val cutoff = 50 * HOUR
        val sourceB = revision(
            id = "b",
            type = WearableRecordType.SLEEP_SESSION,
            start = cutoff - 9 * HOUR,
            end = cutoff - HOUR,
            duration = 8 * HOUR,
            origin = "z.source",
        )
        val sourceA = sourceB.copy(
            revisionId = "a",
            healthConnectRecordId = "a-record",
            dataOriginPackageName = "a.source",
        )

        val first = WearableContextSelector.select(listOf(sourceB, sourceA), cutoff)
        val second = WearableContextSelector.select(listOf(sourceA, sourceB), cutoff)

        assertEquals("a", first.primarySleep?.revisionId)
        assertEquals(first.primarySleep?.revisionId, second.primarySleep?.revisionId)
        assertEquals(1, first.overlappingSleepAlternatives)
        assertEquals(2, first.eligible.size)
    }

    @Test
    fun cutoffFirstSeenAndPermissionMask_preventFutureOrRevokedData() {
        val cutoff = 100 * HOUR
        val sleep = revision("sleep", WearableRecordType.SLEEP_SESSION, cutoff - 8 * HOUR, cutoff - HOUR, 7 * HOUR)
        val lateHrv = revision(
            "late-hrv",
            WearableRecordType.HRV_RMSSD,
            cutoff - 2 * HOUR,
            cutoff - 2 * HOUR,
            null,
        ).copy(firstSeenAtMillis = cutoff + 1L, measurementTimeMillis = cutoff - 2 * HOUR, metricValue = 50.0)
        val knownHrv = lateHrv.copy(revisionId = "known-hrv", firstSeenAtMillis = cutoff - 1L)

        val sleepOnly = WearableContextSelector.select(
            listOf(sleep, lateHrv, knownHrv),
            cutoff,
            setOf(WearableRecordType.SLEEP_SESSION),
        )
        assertEquals(listOf("sleep"), sleepOnly.eligible.map { it.revisionId })
        assertNull(sleepOnly.recentHrv)

        val all = WearableContextSelector.select(listOf(sleep, lateHrv, knownHrv), cutoff)
        assertEquals("known-hrv", all.recentHrv?.revisionId)
        assertTrue(all.eligible.none { it.revisionId == "late-hrv" })
    }

    @Test
    fun homeAfterDisconnect_hidesRetainedValuesAndLabelsRetention() {
        val cutoff = 100 * HOUR
        val sleep = revision("sleep", WearableRecordType.SLEEP_SESSION, cutoff - 8 * HOUR, cutoff - HOUR, 7 * HOUR)
        val connection = WearableConnectionSummary(
            recoveryResearchEnabled = true,
            shadowSyncEnabled = false,
            availability = WearableAvailability.AVAILABLE,
            grantedTypes = emptySet(),
        )

        val home = WearableContextSelector.homeContext(listOf(sleep), cutoff, connection)

        assertNull(home.primaryPriorSleep)
        assertTrue(home.retainedDataHiddenByAccessState)
    }

    @Test
    fun homeAfterResearchRevocation_hidesRetainedValuesEvenIfSyncSettingWasStale() {
        val cutoff = 100 * HOUR
        val sleep = revision(
            "sleep",
            WearableRecordType.SLEEP_SESSION,
            cutoff - 8 * HOUR,
            cutoff - HOUR,
            7 * HOUR,
        )
        val connection = WearableConnectionSummary(
            recoveryResearchEnabled = false,
            shadowSyncEnabled = true,
            availability = WearableAvailability.AVAILABLE,
            grantedTypes = WearableRecordType.entries.toSet(),
        )

        val home = WearableContextSelector.homeContext(listOf(sleep), cutoff, connection)

        assertNull(home.primaryPriorSleep)
        assertTrue(home.retainedDataHiddenByAccessState)
    }

    @Test
    fun correctedSelectionIsVersionedWhileRawV1RemainsFrozen() {
        assertEquals("WEARABLE_2A_SHADOW_CONTEXT_V1", WearableShadowContract.CONTRACT_VERSION)
        assertEquals(
            "WEARABLE_2A_SHADOW_CONTEXT_V1_1",
            WearableShadowContract.SNAPSHOT_CONTRACT_VERSION,
        )
        assertEquals(
            "WEARABLE_2A_SLEEP_SELECTION_V2",
            WearableShadowContract.SLEEP_SELECTION_POLICY_VERSION,
        )
        assertEquals(36L, WearableShadowContract.PRIOR_SLEEP_LOOKBACK_HOURS)
    }

    private fun revision(
        id: String,
        type: WearableRecordType,
        start: Long,
        end: Long,
        duration: Long?,
        origin: String = "synthetic.source",
    ) = WearableRecordRevisionEntity(
        revisionId = id,
        subjectId = WearableShadowContract.SUBJECT_ID,
        contractVersion = WearableShadowContract.CONTRACT_VERSION,
        schemaVersion = 1,
        recordTypeCode = type.name,
        healthConnectRecordId = "$id-record",
        lifecycleEpoch = 0L,
        dataOriginPackageName = origin,
        dataOriginDisplayName = null,
        sourceLastModifiedAtMillis = end,
        clientRecordId = null,
        clientRecordVersion = 0L,
        recordingMethod = 0,
        deviceType = null,
        deviceManufacturer = null,
        deviceModel = null,
        payloadHash = "$id-hash",
        startTimeMillis = start.takeIf { type == WearableRecordType.SLEEP_SESSION },
        endTimeMillis = end.takeIf { type == WearableRecordType.SLEEP_SESSION },
        measurementTimeMillis = end.takeUnless { type == WearableRecordType.SLEEP_SESSION },
        effectiveTimeMillis = end,
        startZoneOffsetSeconds = null,
        endZoneOffsetSeconds = null,
        measurementZoneOffsetSeconds = null,
        sessionDurationMillis = duration,
        knownSleepDurationMillis = duration,
        awakeDurationMillis = 0L.takeIf { duration != null },
        lightDurationMillis = duration,
        deepDurationMillis = null,
        remDurationMillis = null,
        unspecifiedSleepDurationMillis = null,
        unknownStageDurationMillis = 0L.takeIf { duration != null },
        uncoveredStageDurationMillis = 0L.takeIf { duration != null },
        stageCoverage = 1.0.takeIf { duration != null },
        sleepEfficiency = 1.0.takeIf { duration != null },
        stageTimelineStatusCode = WearableStageTimelineStatus.VALID_COMPLETE.name.takeIf { duration != null },
        metricValue = 40.0.takeUnless { type == WearableRecordType.SLEEP_SESSION },
        metricUnitCode = "unit".takeUnless { type == WearableRecordType.SLEEP_SESSION },
        firstSeenAtMillis = end,
        lastSeenAtMillis = end,
        supersededAtMillis = null,
        deletedAtMillis = null,
    )

    private companion object {
        const val HOUR = 3_600_000L
    }
}
