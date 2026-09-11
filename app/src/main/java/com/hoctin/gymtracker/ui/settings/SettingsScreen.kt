package com.hoctin.gymtracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.Straighten
import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.ThemeMode
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.components.AuronModalSheet
import com.hoctin.gymtracker.ui.components.EXERCISE_MODALITY_DETAIL
import com.hoctin.gymtracker.ui.components.EXERCISE_MODALITY_TITLE
import com.hoctin.gymtracker.ui.components.GOAL_CHANGE_SCOPE_DETAIL
import com.hoctin.gymtracker.ui.components.GOAL_CHANGE_SCOPE_TITLE
import com.hoctin.gymtracker.ui.components.GoalOptionContent
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.SecondaryGymButton
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.WeekdayPicker
import com.hoctin.gymtracker.ui.components.autoGoalOption
import com.hoctin.gymtracker.ui.components.explicitGoalOptions
import com.hoctin.gymtracker.ui.components.formatWeekdays
import com.hoctin.gymtracker.ui.components.goalOptionContent
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.components.modeLabel
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronCharcoalSurface
import com.hoctin.gymtracker.ui.theme.auronCharcoalSurfaceRaised
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var editingTrainingDays by remember { mutableStateOf(false) }
    var editingTrainingGoal by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val aiImportPrompt = AI_IMPORT_PROMPT.trim()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var pendingResearchExport by remember { mutableStateOf<String?>(null) }
    var confirmResearchDelete by remember { mutableStateOf(false) }
    val researchExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = pendingResearchExport
        pendingResearchExport = null
        if (uri != null && payload != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { writer ->
                            writer.write(payload)
                        } ?: error("Could not open destination")
                    }
                }
                viewModel.setMessage(
                    if (result.isSuccess) "Recovery research export saved."
                    else "Research export failed: ${result.exceptionOrNull()?.message ?: "unknown error"}",
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 160f + listState.firstVisibleItemScrollOffset.toFloat()
            },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = auronScreenTopPadding(16.dp),
            bottom = BottomNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Display and local data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        state.message?.let { message ->
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    // UI-1 Part F: neutral card (was a purple primaryContainer banner).
                    colors = CardDefaults.cardColors(containerColor = auronCharcoalSurface()),
                    border = gymContainerBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .auronCardElevation(22.dp),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        item { SectionLabel("Appearance") }
        item {
            SettingsCard {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Choose how the app looks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SegmentedThemeControl(
                    selected = state.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }
        }

        item { SectionLabel("Units") }
        item {
            SettingsCard {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Used for carries, sleds, and distance work. Stored values are unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                SegmentedDistanceControl(
                    selected = state.distanceUnitSystem,
                    onSelect = viewModel::setDistanceUnitSystem,
                )
            }
        }

        item { SectionLabel("Training") }
        item {
            SettingsCard {
                Text(
                    text = "Training days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Days you plan to train each week.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatWeekdays(state.trainingWeekdays),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                SecondaryGymButton(
                    text = "Edit training days",
                    onClick = { editingTrainingDays = true },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                )
            }
        }

        item {
            val goalContent = state.workoutGoal.goalOptionContent()
            SettingsCard {
                Text(
                    text = "Training goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.workoutGoal.modeLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = goalContent.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = goalContent.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Changes future coaching only. Your plan and workout history stay unchanged.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryGymButton(
                    text = "Review training goal",
                    onClick = { editingTrainingGoal = true },
                    icon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                )
            }
        }

        item { SectionLabel("Data") }
        item {
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recovery research",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Optional validation records kept in AURON's private app data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.recoveryResearchEnabled,
                        onCheckedChange = viewModel::setRecoveryResearchEnabled,
                    )
                }
                Text(
                    text = "When enabled, AURON freezes one readiness and progression snapshot before the first valid working set, then links it to the saved or discarded outcome. Optional Health Connect shadow data can be connected separately, stays local, and is not used in readiness. Android framework automatic cloud backup and device transfer are disabled for app data by app policy. Research export is manual; the document provider you choose controls where that file is stored. Turning this off stops future capture and keeps existing records until you delete them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SecondaryGymButton(
                    text = "Export research JSON",
                    enabled = !state.busy,
                    onClick = {
                        viewModel.prepareRecoveryResearchExport { json ->
                            pendingResearchExport = json
                            researchExportLauncher.launch("auron_recovery_research_v1.json")
                        }
                    },
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                )
                TextButton(onClick = { confirmResearchDelete = true }) {
                    Text("Delete local research data")
                }
            }
        }
        item {
            SettingsCard {
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Generate a JSON backup of your plan and history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryGymButton(
                    text = if (state.busy) "Working…" else "Generate export",
                    enabled = !state.busy,
                    onClick = viewModel::exportData,
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
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
                            .heightIn(min = 180.dp),
                    )
                }
            }
        }

        item {
            SettingsCard {
                Text(
                    text = "Import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Replace local data with a previously exported backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.importText,
                    onValueChange = viewModel::updateImportText,
                    label = { Text("Paste export JSON") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                )
                PrimaryGymButton(
                    text = if (state.busy) "Working…" else "Replace local data",
                    enabled = !state.busy,
                    onClick = viewModel::importData,
                    icon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                )
            }
        }

        item {
            AiImportHelperCard(
                onCopyPrompt = {
                    clipboardManager.setText(AnnotatedString(aiImportPrompt))
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "AI import prompt copied. Paste it into ChatGPT, Gemini, Claude, etc.",
                        )
                    }
                },
            )
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = BottomNavSpace),
        )
    }

    if (editingTrainingDays) {
        EditTrainingDaysDialog(
            initialSelection = state.trainingWeekdays,
            requiredDays = state.requiredDays,
            onDismiss = { editingTrainingDays = false },
            onSave = { days ->
                viewModel.setTrainingWeekdays(days)
                editingTrainingDays = false
            },
        )
    }

    if (confirmResearchDelete) {
        AlertDialog(
            onDismissRequest = { confirmResearchDelete = false },
            title = { Text("Delete recovery research data?") },
            text = { Text("This permanently removes the local research consent, frozen predictions, and linked outcomes. Your plan and workout history are not changed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmResearchDelete = false
                        viewModel.deleteRecoveryResearchData()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResearchDelete = false }) { Text("Cancel") }
            },
        )
    }

    if (editingTrainingGoal) {
        GoalSelectorSheet(
            initialSelection = state.workoutGoal,
            onDismiss = { editingTrainingGoal = false },
            onSave = { selection ->
                viewModel.setWorkoutGoal(selection)
                editingTrainingGoal = false
            },
        )
    }
}

@Composable
internal fun GoalSelectorSheet(
    initialSelection: WorkoutGoalSelection,
    onDismiss: () -> Unit,
    onSave: (WorkoutGoalSelection) -> Unit,
) {
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }
    AuronModalSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Training goal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Choose how AURON frames your future progression coaching.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close goal selector",
                )
            }
        }

        GoalSelectorInfoBand(
            title = GOAL_CHANGE_SCOPE_TITLE,
            detail = GOAL_CHANGE_SCOPE_DETAIL,
        )

        GoalSelectorSectionLabel("AUTO")
        GoalOptionRow(
            option = autoGoalOption,
            selected = selected == autoGoalOption.selection,
            onSelect = { selected = autoGoalOption.selection },
        )

        GoalSelectorSectionLabel("EXPLICIT GOALS")
        explicitGoalOptions.forEach { option ->
            GoalOptionRow(
                option = option,
                selected = selected == option.selection,
                onSelect = { selected = option.selection },
            )
        }

        GoalSelectorInfoBand(
            title = EXERCISE_MODALITY_TITLE,
            detail = EXERCISE_MODALITY_DETAIL,
        )

        PrimaryGymButton(
            text = "Use ${selected.goalOptionContent().title}",
            onClick = { onSave(selected) },
        )
    }
}

@Composable
private fun GoalSelectorSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GoalOptionRow(
    option: GoalOptionContent,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else Color.Transparent,
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)
                },
                shape = shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalSelectorInfoBand(
    title: String,
    detail: String,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f),
                shape = shape,
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun EditTrainingDaysDialog(
    initialSelection: Set<Int>,
    requiredDays: Int,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit,
) {
    var selected by remember { mutableStateOf(initialSelection) }
    val isValid = requiredDays in 1..7 && selected.size == requiredDays
    AlertDialog(
        onDismissRequest = onDismiss,
        // UI-1 Part F: neutral card surface, no purple tonal tint.
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text("Training days") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (requiredDays in 1..7) {
                        "Choose $requiredDays training ${if (requiredDays == 1) "day" else "days"} (${selected.size}/$requiredDays)."
                    } else {
                        "Choose your training days."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeekdayPicker(
                    selected = selected,
                    onToggle = { day ->
                        selected = if (day in selected) selected - day else selected + day
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }, enabled = isValid) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = auronCharcoalSurface()),
        border = gymContainerBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .auronCardElevation(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun AiImportHelperCard(onCopyPrompt: () -> Unit) {
    SettingsCard {
        Text(
            text = "Need help creating JSON?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Use ChatGPT, Gemini, Claude, or another AI tool to convert screenshots or workout notes into an AURON import JSON.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AI_IMPORT_STEPS.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = "Review the JSON before pasting and importing. AURON will not call an AI service or import automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryGymButton(
            text = "Copy AI Prompt",
            onClick = onCopyPrompt,
            icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
        )
    }
}

private val AI_IMPORT_STEPS = listOf(
    "Copy AI Prompt",
    "Open an AI chat",
    "Send the prompt with screenshots/notes",
    "Copy the JSON result",
    "Paste and import",
)

@Composable
internal fun SegmentedDistanceControl(
    selected: DistanceUnitSystem,
    onSelect: (DistanceUnitSystem) -> Unit,
) {
    val options = listOf(
        DistanceUnitSystem.METRIC to "Metric (m, km)",
        DistanceUnitSystem.IMPERIAL to "Imperial (ft, mi)",
    )
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "distanceSegment",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(auronCharcoalSurfaceRaised())
            .padding(4.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val itemWidth = maxWidth / options.size
            Box(
                modifier = Modifier
                    .offset(x = itemWidth * animatedIndex)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEach { (system, label) ->
                    SegmentItem(
                        label = label,
                        icon = Icons.Outlined.Straighten,
                        selected = selected == system,
                        onClick = { onSelect(system) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SegmentedThemeControl(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.BrightnessAuto),
        Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
    )
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    // Same sliding-pill motion as the other selectors: equal-width segments, the pill's x animates to
    // the selected index (no per-item measurement).
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "themeSegment",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(auronCharcoalSurfaceRaised())
            .padding(4.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            val itemWidth = maxWidth / options.size
            Box(
                modifier = Modifier
                    .offset(x = itemWidth * animatedIndex)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEach { (mode, label, icon) ->
                    SegmentItem(
                        label = label,
                        icon = icon,
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Background is provided by the sliding pill behind; only the content colour cross-fades.
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "segItem-$label",
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}
