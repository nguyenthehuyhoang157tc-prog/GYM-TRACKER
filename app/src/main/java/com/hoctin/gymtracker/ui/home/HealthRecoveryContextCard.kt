package com.hoctin.gymtracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.data.wearable.WearableAvailability
import com.hoctin.gymtracker.data.wearable.WearableHomeContext
import com.hoctin.gymtracker.data.wearable.WearableStageTimelineStatus
import com.hoctin.gymtracker.data.wearable.WearableSyncError
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** WEARABLE-2A read-only display. No value here feeds a production decision. */
@Composable
fun HealthRecoveryContextCard(
    state: WearableHomeContext,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connection = state.connection
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health / Recovery Context",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Shadow context · not used in readiness",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (connection.shadowSyncEnabled) {
                    TextButton(onClick = onRefresh, enabled = !connection.isRefreshing) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Text(if (connection.isRefreshing) "Syncing" else "Refresh")
                    }
                }
            }

            val setupMessage = setupMessage(state)
            if (setupMessage != null) {
                Text(
                    text = setupMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                state.primaryPriorSleep?.let { sleep ->
                    val sleepDuration = sleep.knownSleepDurationMillis
                    ContextValueRow(
                        label = if (sleepDuration == null) {
                            "Selected session duration"
                        } else {
                            "Selected-session sleep"
                        },
                        value = formatDuration(sleepDuration ?: sleep.sessionDurationMillis),
                    )
                    ContextValueRow("Deep", sleep.deepDurationMillis?.let(::formatDuration))
                    ContextValueRow("REM", sleep.remDurationMillis?.let(::formatDuration))
                    ContextValueRow("Light", sleep.lightDurationMillis?.let(::formatDuration))
                    ContextValueRow("Awake", sleep.awakeDurationMillis?.let(::formatDuration))
                    ContextValueRow(
                        "Sleep efficiency",
                        sleep.sleepEfficiency?.let { "${(it * 100.0).roundToInt()}%" },
                    )
                    val source = sleep.sourceDisplayName ?: sleep.sourcePackageName
                    ContextValueRow("Sleep source", source)
                    ContextValueRow("Sleep ended", formatTimestamp(sleep.endTimeMillis))
                    ContextValueRow("First seen by AURON", formatTimestamp(sleep.firstSeenAtMillis))
                    if (sleep.timelineStatus != WearableStageTimelineStatus.VALID_COMPLETE) {
                        Text(
                            text = sleepTimelineNote(sleep.timelineStatus),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } ?: Text(
                    text = "No usable prior sleep session is currently available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.recentHrvRmssd?.let { metric ->
                    ContextValueRow(
                        label = "HRV RMSSD",
                        value = "${formatDecimal(metric.value)} ms · ${metric.sourceDisplayName ?: metric.sourcePackageName}",
                    )
                    ContextValueRow("HRV measured", formatTimestamp(metric.measurementTimeMillis))
                    ContextValueRow("HRV first seen", formatTimestamp(metric.firstSeenAtMillis))
                }
                state.recentRestingHeartRate?.let { metric ->
                    ContextValueRow(
                        label = "Resting HR",
                        value = "${formatDecimal(metric.value)} bpm · ${metric.sourceDisplayName ?: metric.sourcePackageName}",
                    )
                    ContextValueRow("Resting HR measured", formatTimestamp(metric.measurementTimeMillis))
                    ContextValueRow("Resting HR first seen", formatTimestamp(metric.firstSeenAtMillis))
                }
                if (state.otherPriorSleepSessions > 0) {
                    val otherCount = state.otherPriorSleepSessions
                    val sessionNoun = if (otherCount == 1) "session" else "sessions"
                    val overlapNote = if (state.overlappingSleepAlternatives > 0) {
                        " ${state.overlappingSleepAlternatives} overlap the selected session."
                    } else {
                        ""
                    }
                    Text(
                        text = "$otherCount other eligible sleep $sessionNoun retained separately and not included in the selected-session value; none were summed.$overlapNote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!connection.allPermissionsGranted) {
                    Text(
                        text = "Partial Health Connect access. Values from ungranted types remain hidden and missing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            connection.lastSuccessfulSyncAtMillis?.let { syncedAt ->
                Text(
                    text = "Last local sync: ${formatTimestamp(syncedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (System.currentTimeMillis() - syncedAt > STALE_SYNC_AFTER_MILLIS) {
                    Text(
                        text = "Local shadow context may be stale. Refresh when Health Connect has finished syncing.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            syncIssueMessage(connection.lastError)?.let { issue ->
                Text(
                    text = issue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ContextValueRow(label: String, value: String?) {
    if (value == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun setupMessage(state: WearableHomeContext): String? {
    val connection = state.connection
    return when {
        !connection.recoveryResearchEnabled ->
            "Enable Recovery Research in Profile to opt in to local Health Connect shadow capture."
        connection.availability == WearableAvailability.UNKNOWN -> "Checking Health Connect availability…"
        connection.availability == WearableAvailability.PROVIDER_UPDATE_REQUIRED ->
            "Health Connect needs to be installed or updated."
        connection.availability == WearableAvailability.UNAVAILABLE ->
            "Health Connect is unavailable on this device. AURON readiness still works normally."
        !connection.shadowSyncEnabled -> if (state.retainedDataHiddenByAccessState) {
            "Health Connect is disconnected. Previously imported local context is retained but hidden."
        } else {
            "Not connected. Connect from Profile → Recovery Research."
        }
        connection.grantedTypes.isEmpty() ->
            "Health Connect access is not granted. Manage access in Profile → Recovery Research."
        state.primaryPriorSleep == null && state.recentHrvRmssd == null &&
            state.recentRestingHeartRate == null ->
            if (connection.allPermissionsGranted) {
                "Connected, but no supported recent records are available yet. Missing values are not treated as zero."
            } else {
                "Partially connected, but no supported recent records from granted types are available yet. Ungranted or missing values are not treated as zero."
            }
        else -> null
    }
}

private fun syncIssueMessage(error: WearableSyncError): String? = when (error) {
    WearableSyncError.REMOTE_INTERRUPTION ->
        "The last Health Connect refresh was interrupted. Retained context was not rewritten."
    WearableSyncError.UNKNOWN ->
        "The last shadow refresh did not complete. AURON readiness continues unchanged."
    WearableSyncError.TOKEN_EXPIRED ->
        "Health Connect history is being reconciled after an expired sync token."
    else -> null
}

private fun sleepTimelineNote(status: WearableStageTimelineStatus): String = when (status) {
    WearableStageTimelineStatus.NO_STAGES -> "Sleep stages were not supplied; stage values remain missing."
    WearableStageTimelineStatus.VALID_WITH_GAPS -> "Stage coverage is incomplete; gaps were not inferred."
    WearableStageTimelineStatus.VALID_WITH_CLIPPED_STAGE ->
        "A stage crossed the session boundary; efficiency is withheld."
    WearableStageTimelineStatus.INVALID_STAGE_INTERVAL ->
        "Invalid stage intervals were retained as an abstention; aggregates are withheld."
    WearableStageTimelineStatus.INVALID_OVERLAP ->
        "Overlapping stages were retained as an abstention; aggregates are withheld."
    WearableStageTimelineStatus.VALID_COMPLETE -> ""
}

private fun formatDuration(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val remainder = minutes % 60L
    return if (hours > 0L) "${hours}h ${remainder}m" else "${remainder}m"
}

private fun formatDecimal(value: Double): String =
    if (value % 1.0 == 0.0) value.roundToInt().toString()
    else String.format(Locale.ROOT, "%.1f", value)

private fun formatTimestamp(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

private val STALE_SYNC_AFTER_MILLIS = TimeUnit.HOURS.toMillis(24)
