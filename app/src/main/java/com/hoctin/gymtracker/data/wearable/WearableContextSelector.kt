package com.hoctin.gymtracker.data.wearable

import java.util.Locale
import java.util.concurrent.TimeUnit

internal object WearableTypeMask {
    fun encode(types: Set<WearableRecordType>): String =
        types.map { it.name }.sorted().joinToString(",")

    fun decode(mask: String): Set<WearableRecordType> = mask
        .split(',')
        .mapNotNull { token ->
            token.trim().takeIf(String::isNotEmpty)?.let { value ->
                WearableRecordType.entries.firstOrNull { it.name == value.uppercase(Locale.ROOT) }
            }
        }
        .toSet()
}

data class WearableSelection(
    val eligible: List<WearableRecordRevisionEntity>,
    val primarySleep: WearableRecordRevisionEntity?,
    val recentHrv: WearableRecordRevisionEntity?,
    val recentRestingHeartRate: WearableRecordRevisionEntity?,
    val overlappingSleepAlternatives: Int,
)

object WearableContextSelector {
    fun select(
        revisions: List<WearableRecordRevisionEntity>,
        cutoffAtMillis: Long,
        grantedTypes: Set<WearableRecordType> = WearableRecordType.entries.toSet(),
    ): WearableSelection {
        val metricStart = cutoffAtMillis - TimeUnit.HOURS.toMillis(
            WearableShadowContract.RECENT_METRIC_LOOKBACK_HOURS,
        )
        val sleepStart = cutoffAtMillis - TimeUnit.HOURS.toMillis(
            WearableShadowContract.PRIOR_SLEEP_LOOKBACK_HOURS,
        )
        val eligible = revisions.filter { revision ->
            val type = revision.recordTypeOrNull() ?: return@filter false
            type in grantedTypes &&
                revision.effectiveTimeMillis <= cutoffAtMillis &&
                revision.firstSeenAtMillis <= cutoffAtMillis &&
                when (type) {
                    WearableRecordType.SLEEP_SESSION -> revision.effectiveTimeMillis >= sleepStart
                    else -> revision.effectiveTimeMillis >= metricStart
                }
        }
        val sleeps = eligible
            .filter { it.recordTypeCode == WearableRecordType.SLEEP_SESSION.name }
            .filter { it.startTimeMillis != null && it.endTimeMillis != null && it.sessionDurationMillis != null }
        val primarySleep = sleeps.sortedWith(
            compareByDescending<WearableRecordRevisionEntity> { it.endTimeMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.sessionDurationMillis ?: Long.MIN_VALUE }
                .thenBy { it.dataOriginPackageName }
                .thenBy { it.healthConnectRecordId }
                .thenBy { it.revisionId },
        ).firstOrNull()
        val overlapping = primarySleep?.let { primary ->
            sleeps.count { candidate ->
                candidate.revisionId != primary.revisionId && intervalsOverlap(primary, candidate)
            }
        } ?: 0
        val latestComparator = compareByDescending<WearableRecordRevisionEntity> { it.effectiveTimeMillis }
            .thenBy { it.dataOriginPackageName }
            .thenBy { it.healthConnectRecordId }
            .thenBy { it.revisionId }
        return WearableSelection(
            eligible = eligible.sortedWith(
                compareBy<WearableRecordRevisionEntity> { it.recordTypeCode }
                    .thenBy { it.effectiveTimeMillis }
                    .thenBy { it.dataOriginPackageName }
                    .thenBy { it.healthConnectRecordId }
                    .thenBy { it.revisionId },
            ),
            primarySleep = primarySleep,
            recentHrv = eligible
                .filter { it.recordTypeCode == WearableRecordType.HRV_RMSSD.name }
                .sortedWith(latestComparator)
                .firstOrNull(),
            recentRestingHeartRate = eligible
                .filter { it.recordTypeCode == WearableRecordType.RESTING_HEART_RATE.name }
                .sortedWith(latestComparator)
                .firstOrNull(),
            overlappingSleepAlternatives = overlapping,
        )
    }

    fun homeContext(
        revisions: List<WearableRecordRevisionEntity>,
        cutoffAtMillis: Long,
        connection: WearableConnectionSummary,
    ): WearableHomeContext {
        val currentlyReadableTypes = if (
            connection.recoveryResearchEnabled && connection.shadowSyncEnabled
        ) {
            connection.grantedTypes
        } else {
            emptySet()
        }
        val selection = select(revisions, cutoffAtMillis, currentlyReadableTypes)
        val sleep = selection.primarySleep?.toSleepSummary()
        return WearableHomeContext(
            connection = connection,
            primaryPriorSleep = sleep,
            recentHrvRmssd = selection.recentHrv?.toMetricSummary(),
            recentRestingHeartRate = selection.recentRestingHeartRate?.toMetricSummary(),
            overlappingSleepAlternatives = selection.overlappingSleepAlternatives,
            otherPriorSleepSessions = selection.eligible.count {
                it.recordTypeCode == WearableRecordType.SLEEP_SESSION.name
            }.minus(if (sleep == null) 0 else 1).coerceAtLeast(0),
            retainedDataHiddenByAccessState = revisions.any { revision ->
                revision.recordTypeOrNull() !in currentlyReadableTypes
            },
        )
    }

    private fun intervalsOverlap(
        left: WearableRecordRevisionEntity,
        right: WearableRecordRevisionEntity,
    ): Boolean {
        val leftStart = left.startTimeMillis ?: return false
        val leftEnd = left.endTimeMillis ?: return false
        val rightStart = right.startTimeMillis ?: return false
        val rightEnd = right.endTimeMillis ?: return false
        return leftStart < rightEnd && rightStart < leftEnd
    }

    private fun WearableRecordRevisionEntity.toSleepSummary(): WearableSleepSummary? {
        val start = startTimeMillis ?: return null
        val end = endTimeMillis ?: return null
        val duration = sessionDurationMillis ?: return null
        val status = stageTimelineStatusCode?.let { code ->
            WearableStageTimelineStatus.entries.firstOrNull { it.name == code }
        } ?: WearableStageTimelineStatus.NO_STAGES
        return WearableSleepSummary(
            revisionId = revisionId,
            startTimeMillis = start,
            endTimeMillis = end,
            sessionDurationMillis = duration,
            knownSleepDurationMillis = knownSleepDurationMillis,
            awakeDurationMillis = awakeDurationMillis,
            lightDurationMillis = lightDurationMillis,
            deepDurationMillis = deepDurationMillis,
            remDurationMillis = remDurationMillis,
            unspecifiedSleepDurationMillis = unspecifiedSleepDurationMillis,
            unknownStageDurationMillis = unknownStageDurationMillis,
            uncoveredStageDurationMillis = uncoveredStageDurationMillis,
            stageCoverage = stageCoverage,
            sleepEfficiency = sleepEfficiency,
            timelineStatus = status,
            sourcePackageName = dataOriginPackageName,
            sourceDisplayName = dataOriginDisplayName,
            firstSeenAtMillis = firstSeenAtMillis,
        )
    }

    private fun WearableRecordRevisionEntity.toMetricSummary(): WearableMetricSummary? {
        val time = measurementTimeMillis ?: return null
        val value = metricValue ?: return null
        val unit = metricUnitCode ?: return null
        return WearableMetricSummary(
            revisionId = revisionId,
            measurementTimeMillis = time,
            value = value,
            unitCode = unit,
            sourcePackageName = dataOriginPackageName,
            sourceDisplayName = dataOriginDisplayName,
            firstSeenAtMillis = firstSeenAtMillis,
        )
    }

    private fun WearableRecordRevisionEntity.recordTypeOrNull(): WearableRecordType? =
        WearableRecordType.entries.firstOrNull { it.name == recordTypeCode }
}
