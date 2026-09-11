package com.hoctin.gymtracker.data.wearable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearableNormalizerTest {
    @Test
    fun completeStages_aggregateWithoutInventingTime_andComputeEfficiency() {
        val hour = 3_600_000L
        val record = sleep(
            start = 0L,
            end = 8 * hour,
            stages = listOf(
                stage(0, 1, WearableStageType.AWAKE),
                stage(1, 3, WearableStageType.LIGHT),
                stage(3, 5, WearableStageType.DEEP),
                stage(5, 8, WearableStageType.REM),
            ),
        )

        val result = requireNotNull(WearableNormalizer.normalize(record))

        assertEquals(WearableStageTimelineStatus.VALID_COMPLETE, result.stageTimelineStatus)
        assertEquals(7 * hour, result.knownSleepDurationMillis)
        assertEquals(1 * hour, result.awakeDurationMillis)
        assertEquals(2 * hour, result.lightDurationMillis)
        assertEquals(2 * hour, result.deepDurationMillis)
        assertEquals(3 * hour, result.remDurationMillis)
        assertEquals(0L, result.uncoveredStageDurationMillis)
        assertEquals(1.0, requireNotNull(result.stageCoverage), 0.0)
        assertEquals(0.875, requireNotNull(result.sleepEfficiency), 0.0)
    }

    @Test
    fun missingAndGappedStages_remainMissingOrGapped_notZeroFilled() {
        val hour = 3_600_000L
        val noStages = requireNotNull(WearableNormalizer.normalize(sleep(0, 5 * hour, emptyList())))
        assertEquals(WearableStageTimelineStatus.NO_STAGES, noStages.stageTimelineStatus)
        assertNull(noStages.knownSleepDurationMillis)
        assertNull(noStages.deepDurationMillis)
        assertNull(noStages.sleepEfficiency)
        assertEquals(5 * hour, noStages.uncoveredStageDurationMillis)

        val gapped = requireNotNull(
            WearableNormalizer.normalize(
                sleep(
                    0,
                    5 * hour,
                    listOf(
                        RawSleepStage(0, 2 * hour, WearableStageType.LIGHT),
                        RawSleepStage(3 * hour, 5 * hour, WearableStageType.DEEP),
                    ),
                ),
            ),
        )
        assertEquals(WearableStageTimelineStatus.VALID_WITH_GAPS, gapped.stageTimelineStatus)
        assertEquals(hour, gapped.uncoveredStageDurationMillis)
        assertEquals(0.8, requireNotNull(gapped.stageCoverage), 0.0)
        assertNull(gapped.sleepEfficiency)
    }

    @Test
    fun overnightDuration_usesInstantsAndPreservesOffsets() {
        val start = 1_800_000_000_000L
        val end = start + 7 * 3_600_000L + 30 * 60_000L
        val result = requireNotNull(
            WearableNormalizer.normalize(
                sleep(start, end, emptyList()).copy(
                    startZoneOffsetSeconds = 7 * 3_600,
                    endZoneOffsetSeconds = 8 * 3_600,
                ),
            ),
        )
        assertEquals(end - start, result.sessionDurationMillis)
        assertEquals(7 * 3_600, result.startZoneOffsetSeconds)
        assertEquals(8 * 3_600, result.endZoneOffsetSeconds)
    }

    @Test
    fun overlapAndClipping_abstainFromUnsafeAggregates() {
        val hour = 3_600_000L
        val overlap = requireNotNull(
            WearableNormalizer.normalize(
                sleep(
                    0,
                    4 * hour,
                    listOf(
                        RawSleepStage(0, 3 * hour, WearableStageType.LIGHT),
                        RawSleepStage(2 * hour, 4 * hour, WearableStageType.DEEP),
                    ),
                ),
            ),
        )
        assertEquals(WearableStageTimelineStatus.INVALID_OVERLAP, overlap.stageTimelineStatus)
        assertNull(overlap.knownSleepDurationMillis)
        assertNull(overlap.sleepEfficiency)

        val clipped = requireNotNull(
            WearableNormalizer.normalize(
                sleep(
                    0,
                    4 * hour,
                    listOf(RawSleepStage(-hour, 4 * hour, WearableStageType.LIGHT)),
                ),
            ),
        )
        assertEquals(WearableStageTimelineStatus.VALID_WITH_CLIPPED_STAGE, clipped.stageTimelineStatus)
        assertEquals(1.0, requireNotNull(clipped.stageCoverage), 0.0)
        assertNull(clipped.sleepEfficiency)
        assertTrue(clipped.sleepStages.single().wasClipped)
    }

    @Test
    fun hrvAndRestingHeartRate_useOnlySupportedRangesAndMetrics() {
        val hrv = WearableNormalizer.normalize(
            RawHrvRmssd(metadata("hrv"), 1_000L, 25_200, 42.5),
        )
        assertEquals(42.5, requireNotNull(requireNotNull(hrv).metricValue), 0.0)
        assertEquals("MILLISECONDS_RMSSD", hrv.metricUnitCode)
        assertNull(WearableNormalizer.normalize(RawHrvRmssd(metadata("bad-hrv"), 1_000L, null, 0.5)))

        val rhr = WearableNormalizer.normalize(
            RawRestingHeartRate(metadata("rhr"), 2_000L, null, 58L),
        )
        assertEquals(58.0, requireNotNull(requireNotNull(rhr).metricValue), 0.0)
        assertEquals("BEATS_PER_MINUTE", rhr.metricUnitCode)
        assertNull(
            WearableNormalizer.normalize(RawRestingHeartRate(metadata("bad-rhr"), 2_000L, null, 301L)),
        )
    }

    private fun stage(startHour: Long, endHour: Long, type: WearableStageType) = RawSleepStage(
        startTimeMillis = startHour * 3_600_000L,
        endTimeMillis = endHour * 3_600_000L,
        stageType = type,
    )

    private fun sleep(start: Long, end: Long, stages: List<RawSleepStage>) = RawSleepSession(
        metadata = metadata("sleep"),
        startTimeMillis = start,
        endTimeMillis = end,
        startZoneOffsetSeconds = null,
        endZoneOffsetSeconds = null,
        stages = stages,
    )

    private fun metadata(id: String) = WearableSourceMetadata(
        healthConnectRecordId = id,
        dataOriginPackageName = "synthetic.source",
        dataOriginDisplayName = "Synthetic",
        sourceLastModifiedAtMillis = 100L,
        clientRecordId = null,
        clientRecordVersion = 0L,
        recordingMethod = 0,
        deviceType = null,
        deviceManufacturer = null,
        deviceModel = null,
    )
}
