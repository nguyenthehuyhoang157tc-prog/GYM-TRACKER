package com.hoctin.gymtracker.data.wearable

import kotlin.math.max
import kotlin.math.min

object WearableNormalizer {
    fun normalize(record: RawWearableRecord): NormalizedWearableRevisionDraft? = when (record) {
        is RawSleepSession -> normalizeSleep(record)
        is RawHrvRmssd -> normalizeHrv(record)
        is RawRestingHeartRate -> normalizeRestingHeartRate(record)
    }

    private fun normalizeSleep(record: RawSleepSession): NormalizedWearableRevisionDraft? {
        val sessionDuration = record.endTimeMillis - record.startTimeMillis
        if (sessionDuration <= 0L) return null

        val stages = record.stages
            .sortedWith(
                compareBy(RawSleepStage::startTimeMillis, RawSleepStage::endTimeMillis)
                    .thenBy { it.stageType.name },
            )
            .mapIndexed { index, stage ->
                val validSourceInterval = stage.endTimeMillis > stage.startTimeMillis
                val clippedStart = max(stage.startTimeMillis, record.startTimeMillis)
                val clippedEnd = min(stage.endTimeMillis, record.endTimeMillis)
                val validClippedInterval = validSourceInterval && clippedEnd > clippedStart
                NormalizedSleepStageDraft(
                    ordinal = index + 1,
                    stageType = stage.stageType,
                    sourceStartTimeMillis = stage.startTimeMillis,
                    sourceEndTimeMillis = stage.endTimeMillis,
                    normalizedStartTimeMillis = clippedStart.takeIf { validClippedInterval },
                    normalizedEndTimeMillis = clippedEnd.takeIf { validClippedInterval },
                    durationMillis = (clippedEnd - clippedStart).takeIf { validClippedInterval },
                    wasClipped = validClippedInterval &&
                        (clippedStart != stage.startTimeMillis || clippedEnd != stage.endTimeMillis),
                    isValid = validClippedInterval,
                )
            }

        val invalidStage = stages.any { !it.isValid }
        val validStages = stages.filter { it.isValid }
        val overlap = validStages.zipWithNext().any { (left, right) ->
            requireNotNull(left.normalizedEndTimeMillis) > requireNotNull(right.normalizedStartTimeMillis)
        }
        val clipped = validStages.any { it.wasClipped }

        val timelineStatus: WearableStageTimelineStatus
        var knownSleep: Long? = null
        var awake: Long? = null
        var light: Long? = null
        var deep: Long? = null
        var rem: Long? = null
        var unspecifiedSleep: Long? = null
        var unknown: Long? = null
        var uncovered: Long? = null
        var coverage: Double? = null
        var efficiency: Double? = null

        when {
            stages.isEmpty() -> {
                timelineStatus = WearableStageTimelineStatus.NO_STAGES
                uncovered = sessionDuration
            }
            invalidStage -> timelineStatus = WearableStageTimelineStatus.INVALID_STAGE_INTERVAL
            overlap -> timelineStatus = WearableStageTimelineStatus.INVALID_OVERLAP
            else -> {
                val durationByType = WearableStageType.entries.associateWith { type ->
                    validStages.filter { it.stageType == type }.sumOf { requireNotNull(it.durationMillis) }
                }
                val lightValue = durationByType.getValue(WearableStageType.LIGHT)
                val deepValue = durationByType.getValue(WearableStageType.DEEP)
                val remValue = durationByType.getValue(WearableStageType.REM)
                val unspecifiedSleepValue = durationByType.getValue(WearableStageType.SLEEPING_UNSPECIFIED)
                val unknownValue = durationByType.getValue(WearableStageType.UNKNOWN)
                val awakeValue = durationByType.getValue(WearableStageType.AWAKE) +
                    durationByType.getValue(WearableStageType.AWAKE_IN_BED) +
                    durationByType.getValue(WearableStageType.OUT_OF_BED)
                val knownSleepValue = lightValue + deepValue + remValue + unspecifiedSleepValue
                light = lightValue
                deep = deepValue
                rem = remValue
                unspecifiedSleep = unspecifiedSleepValue
                unknown = unknownValue
                awake = awakeValue
                knownSleep = knownSleepValue
                val coveredMillis = validStages.sumOf { requireNotNull(it.durationMillis) }
                uncovered = (sessionDuration - coveredMillis).coerceAtLeast(0L)
                coverage = coveredMillis.toDouble() / sessionDuration.toDouble()
                timelineStatus = when {
                    clipped -> WearableStageTimelineStatus.VALID_WITH_CLIPPED_STAGE
                    uncovered > 0L -> WearableStageTimelineStatus.VALID_WITH_GAPS
                    else -> WearableStageTimelineStatus.VALID_COMPLETE
                }
                // V1 abstains unless the entire valid timeline is classified as known sleep/awake.
                if (!clipped && uncovered == 0L && unknownValue == 0L &&
                    knownSleepValue + awakeValue > 0L
                ) {
                    efficiency = knownSleepValue.toDouble() / (knownSleepValue + awakeValue).toDouble()
                }
            }
        }

        val stageCanonical = stages.joinToString(";") { stage ->
            listOf(
                stage.ordinal,
                stage.stageType.name,
                stage.sourceStartTimeMillis,
                stage.sourceEndTimeMillis,
                stage.normalizedStartTimeMillis,
                stage.normalizedEndTimeMillis,
                stage.wasClipped,
                stage.isValid,
            ).joinToString(",")
        }
        val payloadHash = WearableHashing.sha256(
            WearableShadowContract.CONTRACT_VERSION,
            record.type.name,
            record.startTimeMillis,
            record.endTimeMillis,
            record.startZoneOffsetSeconds,
            record.endZoneOffsetSeconds,
            timelineStatus.name,
            knownSleep,
            awake,
            light,
            deep,
            rem,
            unspecifiedSleep,
            unknown,
            uncovered,
            coverage,
            efficiency,
            stageCanonical,
            metadataCanonical(record.metadata),
        )
        return NormalizedWearableRevisionDraft(
            type = record.type,
            metadata = record.metadata,
            payloadHash = payloadHash,
            startTimeMillis = record.startTimeMillis,
            endTimeMillis = record.endTimeMillis,
            measurementTimeMillis = null,
            effectiveTimeMillis = record.endTimeMillis,
            startZoneOffsetSeconds = record.startZoneOffsetSeconds,
            endZoneOffsetSeconds = record.endZoneOffsetSeconds,
            measurementZoneOffsetSeconds = null,
            sessionDurationMillis = sessionDuration,
            knownSleepDurationMillis = knownSleep,
            awakeDurationMillis = awake,
            lightDurationMillis = light,
            deepDurationMillis = deep,
            remDurationMillis = rem,
            unspecifiedSleepDurationMillis = unspecifiedSleep,
            unknownStageDurationMillis = unknown,
            uncoveredStageDurationMillis = uncovered,
            stageCoverage = coverage,
            sleepEfficiency = efficiency,
            stageTimelineStatus = timelineStatus,
            metricValue = null,
            metricUnitCode = null,
            sleepStages = stages,
        )
    }

    private fun normalizeHrv(record: RawHrvRmssd): NormalizedWearableRevisionDraft? {
        if (!record.rmssdMillis.isFinite() || record.rmssdMillis !in 1.0..200.0) return null
        return normalizeMetric(
            record = record,
            measurementTimeMillis = record.measurementTimeMillis,
            zoneOffsetSeconds = record.zoneOffsetSeconds,
            value = record.rmssdMillis,
            unitCode = "MILLISECONDS_RMSSD",
        )
    }

    private fun normalizeRestingHeartRate(record: RawRestingHeartRate): NormalizedWearableRevisionDraft? {
        if (record.beatsPerMinute !in 1L..300L) return null
        return normalizeMetric(
            record = record,
            measurementTimeMillis = record.measurementTimeMillis,
            zoneOffsetSeconds = record.zoneOffsetSeconds,
            value = record.beatsPerMinute.toDouble(),
            unitCode = "BEATS_PER_MINUTE",
        )
    }

    private fun normalizeMetric(
        record: RawWearableRecord,
        measurementTimeMillis: Long,
        zoneOffsetSeconds: Int?,
        value: Double,
        unitCode: String,
    ): NormalizedWearableRevisionDraft {
        val payloadHash = WearableHashing.sha256(
            WearableShadowContract.CONTRACT_VERSION,
            record.type.name,
            measurementTimeMillis,
            zoneOffsetSeconds,
            value,
            unitCode,
            metadataCanonical(record.metadata),
        )
        return NormalizedWearableRevisionDraft(
            type = record.type,
            metadata = record.metadata,
            payloadHash = payloadHash,
            startTimeMillis = null,
            endTimeMillis = null,
            measurementTimeMillis = measurementTimeMillis,
            effectiveTimeMillis = measurementTimeMillis,
            startZoneOffsetSeconds = null,
            endZoneOffsetSeconds = null,
            measurementZoneOffsetSeconds = zoneOffsetSeconds,
            sessionDurationMillis = null,
            knownSleepDurationMillis = null,
            awakeDurationMillis = null,
            lightDurationMillis = null,
            deepDurationMillis = null,
            remDurationMillis = null,
            unspecifiedSleepDurationMillis = null,
            unknownStageDurationMillis = null,
            uncoveredStageDurationMillis = null,
            stageCoverage = null,
            sleepEfficiency = null,
            stageTimelineStatus = null,
            metricValue = value,
            metricUnitCode = unitCode,
            sleepStages = emptyList(),
        )
    }

    private fun metadataCanonical(metadata: WearableSourceMetadata): String = listOf(
        metadata.healthConnectRecordId,
        metadata.dataOriginPackageName,
        metadata.sourceLastModifiedAtMillis,
        metadata.clientRecordId,
        metadata.clientRecordVersion,
        metadata.recordingMethod,
        metadata.deviceType,
        metadata.deviceManufacturer,
        metadata.deviceModel,
    ).joinToString("|")
}
