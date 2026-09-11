package com.hoctin.gymtracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.data.wearable.HealthConnectPermissions
import com.hoctin.gymtracker.data.wearable.WearableAvailability
import com.hoctin.gymtracker.data.wearable.WearableRecordType
import com.hoctin.gymtracker.data.wearable.WearableSyncError
import com.hoctin.gymtracker.data.wearable.WearableSyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileDataScreen(
    state: SettingsUiState,
    onUpdateImportText: (String) -> Unit,
    onGenerateExport: () -> Unit,
    onImport: () -> Unit,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val aiPrompt = AI_IMPORT_PROMPT.trim()

    ProfilePageShell(
        title = "Import & export",
        eyebrow = "Data & research",
        onBack = onBack,
    ) {
        state.message?.let { ProfileMessageCard(it) }

        ProfileContentCard {
            Text(
                text = "PORTABLE BACKUP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Export plan, Saved Workouts and workout history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Generate the same local JSON backup contract used by the existing Settings flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhiteProfileButton(
                text = if (state.busy) "Working…" else "Generate export",
                enabled = !state.busy,
                onClick = onGenerateExport,
                icon = Icons.Outlined.FileDownload,
            )
            if (state.exportText.isNotBlank()) {
                OutlinedTextField(
                    value = state.exportText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Export JSON") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp),
                )
            }
        }

        ProfileContentCard {
            Text(
                text = "RESTORE LOCAL DATA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Import an AURON backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Import replaces the local plan and history. New backups also replace Saved Workouts; older backups preserve Saved Workouts already on this device. Existing canonical-key backfill remains unchanged.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.importText,
                onValueChange = onUpdateImportText,
                label = { Text("Paste export JSON") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp),
            )
            WhiteProfileButton(
                text = if (state.busy) "Working…" else "Replace local data",
                enabled = !state.busy,
                onClick = onImport,
                icon = Icons.Outlined.FileUpload,
            )
        }

        ProfileContentCard {
            Text(
                text = "AI IMPORT HELPER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Prepare data with an external assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Copy AURON's existing prompt, use it in the assistant you choose, then review and paste the resulting JSON above. AURON does not send data for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WhiteProfileButton(
                text = "Copy AI import prompt",
                onClick = {
                    clipboard.setText(AnnotatedString(aiPrompt))
                    onMessage("AI import prompt copied. Paste it into the assistant you choose.")
                },
                icon = Icons.Outlined.ContentCopy,
            )
        }
    }
}

@Composable
fun RecoveryResearchScreen(
    state: SettingsUiState,
    onEnabledChanged: (Boolean) -> Unit,
    onPrepareExport: ((String) -> Unit) -> Unit,
    onDelete: () -> Unit,
    onHealthPermissionResult: (Set<String>) -> Unit,
    onRefreshHealthContext: () -> Unit,
    onDisconnectHealthConnect: () -> Unit,
    onDeleteImportedHealthData: () -> Unit,
    onManageHealthAccess: () -> android.content.Intent?,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmHealthDelete by remember { mutableStateOf(false) }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        HealthConnectPermissions.requestContract,
        onResult = onHealthPermissionResult,
    )
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri != null && payload != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use {
                            it.write(payload)
                        } ?: error("Could not open destination")
                    }
                }
                onMessage(
                    if (result.isSuccess) "Recovery research export saved."
                    else "Research export failed: ${result.exceptionOrNull()?.message ?: "unknown error"}",
                )
            }
        }
    }

    ProfilePageShell(
        title = "Recovery Research",
        eyebrow = "Data & research",
        onBack = onBack,
    ) {
        state.message?.let { ProfileMessageCard(it) }

        ProfileContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Optional local validation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (state.recoveryResearchEnabled) "Capture enabled" else "Capture disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.recoveryResearchEnabled,
                    onCheckedChange = onEnabledChanged,
                )
            }
            Text(
                text = "When enabled, AURON freezes one readiness and progression snapshot before the first valid working set, then links it to the saved or discarded outcome.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Workout validation remains local. Health Connect shadow capture is a separate read-only opt-in below; it is never used to calculate readiness. Turning capture off keeps existing local records until you delete them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProfileStatusPill("Local only")
        }

        val wearable = state.wearableContext
        val connection = wearable.connection
        ProfileContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Connect shadow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = wearableConnectionLabel(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "Reads only Sleep, HRV RMSSD and Resting heart rate. Partial permission is supported. No write, background, unrestricted-history or ordinary heart-rate permission is requested.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Shadow context · not used in readiness, Muscle Heat, Progressive Overload or recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            wearableSyncIssueLabel(connection.lastError)?.let { issue ->
                Text(
                    text = issue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!connection.shadowSyncEnabled) {
                WhiteProfileButton(
                    text = "Connect Health Connect",
                    enabled = state.recoveryResearchEnabled &&
                        connection.availability == WearableAvailability.AVAILABLE,
                    onClick = {
                        healthPermissionLauncher.launch(HealthConnectPermissions.requiredReadPermissions)
                    },
                    icon = Icons.Outlined.HealthAndSafety,
                )
                if (!state.recoveryResearchEnabled) {
                    Text(
                        text = "Enable Recovery Research first so every imported record has an explicit local consent boundary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                WhiteProfileButton(
                    text = if (connection.isRefreshing) "Refreshing…" else "Refresh shadow context",
                    enabled = !connection.isRefreshing,
                    onClick = onRefreshHealthContext,
                    icon = Icons.Outlined.Refresh,
                )
                WhiteProfileButton(
                    text = "Manage Health Connect access",
                    onClick = {
                        val intent = onManageHealthAccess()
                        if (intent == null) {
                            onMessage("Health Connect access settings are unavailable on this device.")
                        } else {
                            runCatching { context.startActivity(intent) }
                                .onFailure { onMessage("Could not open Health Connect settings.") }
                        }
                    },
                    icon = Icons.Outlined.Settings,
                )
                TextButton(onClick = onDisconnectHealthConnect) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = null)
                    Text("Disconnect and revoke access", fontWeight = FontWeight.Bold)
                }
            }

            if (wearable.primaryPriorSleep != null || wearable.recentHrvRmssd != null ||
                wearable.recentRestingHeartRate != null || wearable.retainedDataHiddenByAccessState
            ) {
                TextButton(onClick = { confirmHealthDelete = true }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Delete imported health context",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            ProfileStatusPill("Local only · separate from research export v1")
            state.latestWearableSnapshot?.let { snapshot ->
                Text(
                    text = "LATEST SHADOW SNAPSHOT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${snapshot.status.name} · ${snapshot.includedRecordCount} record(s) · " +
                        "${snapshot.includedSourceCount} source(s)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Cutoff ${formatHealthDiagnosticTime(snapshot.cutoffAtMillis)} · " +
                        "sealed ${formatHealthDiagnosticTime(snapshot.createdAtMillis)} · " +
                        "hash ${snapshot.payloadHash.take(12)}…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Integrity metadata only; the frozen Research JSON v1 remains unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ProfileContentCard {
            Text(
                text = "RESEARCH DATA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            WhiteProfileButton(
                text = if (state.busy) "Preparing…" else "Export research JSON",
                enabled = !state.busy,
                onClick = {
                    onPrepareExport { json ->
                        pendingExport = json
                        exportLauncher.launch("auron_recovery_research_v1.json")
                    }
                },
                icon = Icons.Outlined.FileDownload,
            )
            TextButton(onClick = { confirmDelete = true }) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Delete local research data",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete recovery research data?") },
            text = {
                Text("This permanently removes local research consent, frozen predictions and linked outcomes. Your plan and workout history are not changed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmHealthDelete) {
        AlertDialog(
            onDismissRequest = { confirmHealthDelete = false },
            title = { Text("Delete imported health context?") },
            text = {
                Text("This permanently removes AURON's local Health Connect revisions, sync tokens and wearable context snapshots. It does not delete data from Health Connect, your workouts or MUSCLE-4B predictions.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmHealthDelete = false
                        onDeleteImportedHealthData()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmHealthDelete = false }) { Text("Cancel") }
            },
        )
    }
}

private fun wearableConnectionLabel(state: SettingsUiState): String {
    val connection = state.wearableContext.connection
    return when {
        connection.availability == WearableAvailability.UNKNOWN -> "Checking availability"
        connection.availability == WearableAvailability.PROVIDER_UPDATE_REQUIRED ->
            "Provider install or update required"
        connection.availability == WearableAvailability.UNAVAILABLE -> "Unavailable on this device"
        !state.recoveryResearchEnabled -> "Research consent required"
        !connection.shadowSyncEnabled -> "Not connected"
        connection.status == WearableSyncStatus.SYNCING -> "Refreshing local shadow context"
        connection.grantedTypes.isEmpty() -> "No read permission granted"
        connection.grantedTypes.size < WearableRecordType.entries.size ->
            "Partially connected · ${connection.grantedTypes.size} of ${WearableRecordType.entries.size} types"
        else -> "Connected · all 3 read types granted"
    }
}

private fun wearableSyncIssueLabel(error: WearableSyncError): String? = when (error) {
    WearableSyncError.REMOTE_INTERRUPTION ->
        "Last refresh interrupted; retained local context was not rewritten."
    WearableSyncError.UNKNOWN ->
        "Last refresh did not complete; workout and readiness features remain available."
    WearableSyncError.TOKEN_EXPIRED ->
        "Health Connect sync token expired; bounded local reconciliation is required."
    else -> null
}

private fun formatHealthDiagnosticTime(epochMillis: Long): String =
    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT)
        .format(java.util.Date(epochMillis))

@Composable
fun ProfileResourcesScreen(onBack: () -> Unit) {
    ProfilePageShell(
        title = "Help & resources",
        eyebrow = "Help & AURON",
        onBack = onBack,
    ) {
        ProfileContentCard {
            Text("QUICK GUIDE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HelpEntry("How does Weekly Progress work?", "It counts distinct Routine workout dates in the current ISO week, up to your selected weekday target. Saved One-Off workouts and Daily Boost never increase it.")
            HelpEntry("Where is my workout archive?", "History V2 is the canonical archive. Open Workout history from the Profile hub to browse monthly sessions and Session Review.")
            HelpEntry("What does the coaching goal change?", "It changes future coaching context in Log Workout and Exercise Detail. It does not rewrite your plan or past sessions.")
        }
        ProfileContentCard {
            Text("DATA & SUPPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HelpEntry("Can I back up my data?", "Yes. Import & export generates and restores the existing local AURON JSON contract.")
            HelpEntry("Does AURON use a cloud account?", "No account or cloud-sync system is implemented. Your app data remains local unless you explicitly export a document.")
        }
        ProfilePlaceholderState(
            title = "Contact and community are not connected",
            explanation = "AURON has no verified support address, Discord or public community destination in the current project. This status remains visible instead of opening a dead or invented link.",
            status = "Unavailable",
        )
    }
}

@Composable
fun ProfilePrivacyScreen(onBack: () -> Unit) {
    ProfilePageShell(
        title = "Privacy & local data",
        eyebrow = "Help & AURON",
        onBack = onBack,
    ) {
        ProfileContentCard {
            Text("LOCAL-FIRST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("AURON stores plans, workout history, preferences and the onboarding training profile locally on this device.", style = MaterialTheme.typography.bodyLarge)
            Text("The app does not currently provide an account, cloud sync or a backend upload flow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProfileContentCard {
            Text("USER-CONTROLLED EXPORT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Plan/history export produces text you can copy. Recovery Research uses Android's document picker, and the destination provider you choose controls the saved file.", style = MaterialTheme.typography.bodyLarge)
        }
        ProfileContentCard {
            Text("PROTECTED RESEARCH AREA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Recovery Research is opt-in. Capture can be disabled, exported manually or deleted without changing the workout plan or History V2.", style = MaterialTheme.typography.bodyLarge)
        }
        ProfilePlaceholderState(
            title = "Formal legal documents are not bundled",
            explanation = "The current project does not contain verified hosted Privacy Policy or Terms of Use URLs. AURON therefore presents an honest local-data summary instead of a dead legal link.",
            status = "Unavailable",
        )
    }
}

@Composable
fun AboutAuronScreen(onBack: () -> Unit) {
    ProfilePageShell(
        title = "About AURON",
        eyebrow = "Help & AURON",
        onBack = onBack,
    ) {
        ProfileContentCard {
            ProfileStatusPill("Local training intelligence")
            Text("Built around evidence you actually log", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "AURON combines workout planning, detailed logging, History V2, Weekly Progress, activity continuity, Daily Boost and muscle-recovery presentation without creating fake sessions or claiming a cloud coach.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProfileContentCard {
            Text("CURRENT PRODUCT BOUNDARY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HelpEntry("Progression", "Future coaching can use saved sets, reps, load and RIR through the existing protected progression system.")
            HelpEntry("Recovery", "Muscle Recovery is an app model derived from workout evidence, not medical advice or wearable data.")
            HelpEntry("Daily Boost", "A separate optional activity signal. It can maintain Active Streak but never counts as a Weekly Progress workout.")
        }
    }
}

@Composable
private fun HelpEntry(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WhiteProfileButton(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        ),
    ) {
        Icon(icon, contentDescription = null)
        Text(text, fontWeight = FontWeight.Bold)
    }
}
