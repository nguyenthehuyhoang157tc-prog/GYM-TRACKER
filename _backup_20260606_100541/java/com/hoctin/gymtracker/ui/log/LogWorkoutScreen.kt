package com.hoctin.gymtracker.ui.log

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.SectionTitle
import com.hoctin.gymtracker.ui.components.StatChip

@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LogWorkoutEvent.Saved -> onSaved()
                is LogWorkoutEvent.Discarded -> onDiscarded()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Log workout",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.day?.name ?: "Loading",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.message?.let { message ->
                item(key = "message") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            val exercises = state.exercises
            itemsIndexed(exercises, key = { _, item -> item.exercise.id }) { index, exerciseState ->
                ExerciseCard(
                    state = exerciseState,
                    expanded = exerciseState.exercise.id == state.expandedExerciseId,
                    hasNext = index < exercises.lastIndex,
                    onToggle = { viewModel.toggleExpanded(exerciseState.exercise.id) },
                    onNext = { viewModel.expandNext() },
                    onAddSet = { viewModel.addSet(exerciseState.exercise.id) },
                    onRemoveSet = { i -> viewModel.removeSet(exerciseState.exercise.id, i) },
                    onWeight = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, weight = v.filterDecimal())
                    },
                    onReps = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, reps = v.filter(Char::isDigit))
                    },
                    onRir = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, rir = v.filter(Char::isDigit).take(1))
                    },
                    onNotes = { i, v -> viewModel.updateSet(exerciseState.exercise.id, i, notes = v) },
                )
            }
        }

        SaveBar(
            canSave = state.canSave,
            isSaving = state.isSaving,
            onSave = viewModel::saveWorkout,
            onDiscard = { showDiscardDialog = true },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard workout?") },
            text = {
                Text(
                    "This clears the sets you've entered for ${state.day?.name ?: "this workout"}. " +
                        "It will not be saved to History. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardDraft()
                    },
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Active-exercise accordion card. When [expanded] it renders full input fields (the only place set
 * rows/text fields exist), otherwise it renders a lightweight summary with no text fields. This keeps
 * fast scrolling cheap because only one exercise composes heavy input UI at a time.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    state: ExerciseLogState,
    expanded: Boolean,
    hasNext: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onWeight: (Int, String) -> Unit,
    onReps: (Int, String) -> Unit,
    onRir: (Int, String) -> Unit,
    onNotes: (Int, String) -> Unit,
) {
    val exercise = state.exercise
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        // Flat (no shadow) keeps repeated collapsed cards cheap and matches the OLED design.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Subtle, low-duration size animation only fires on tap (never during scroll).
                .animateContentSize(animationSpec = tween(durationMillis = 180))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 14.dp else 6.dp),
        ) {
            // Header (always present) — tapping toggles expand/collapse.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(completed = state.completedSetCount, target = exercise.targetSets)
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            if (expanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("${exercise.targetSets} sets")
                    StatChip("${exercise.targetMinReps}-${exercise.targetMaxReps} reps")
                    StatChip(state.trendLabel)
                    if (state.potentialPr) {
                        StatChip("Potential PR")
                    }
                }

                PreviousPerformance(state.previousSets)

                state.draftSets.forEachIndexed { i, draft ->
                    SetDraftRow(
                        index = i,
                        draft = draft,
                        onWeight = { onWeight(i, it) },
                        onReps = { onReps(i, it) },
                        onRir = { onRir(i, it) },
                        onNotes = { onNotes(i, it) },
                        onDelete = { onRemoveSet(i) },
                    )
                }

                OutlinedButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Add set", modifier = Modifier.padding(start = 8.dp))
                }

                SuggestionBox(
                    title = state.suggestion.title,
                    detail = state.suggestion.detail,
                    suggestedWeight = state.suggestion.suggestedWeight,
                )

                if (state.isComplete && hasNext) {
                    FilledTonalButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                        Text("Next exercise", fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = "${exercise.targetSets} sets · ${exercise.targetMinReps}-${exercise.targetMaxReps} reps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.completedSetCount > 0 || state.currentVolume > 0.0) {
                    Text(
                        text = buildString {
                            append("${state.completedSetCount}/${exercise.targetSets} sets logged")
                            if (state.currentVolume > 0.0) {
                                append(" · ${ProgressiveOverloadEngine.formatWeight(state.currentVolume)} kg")
                            }
                            if (state.potentialPr) append(" · PR")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(completed: Int, target: Int) {
    val colors = MaterialTheme.colorScheme
    val (label, container, content) = when {
        completed == 0 -> Triple("Not started", colors.surfaceVariant, colors.onSurfaceVariant)
        completed >= target -> Triple("Done", colors.tertiaryContainer, colors.onTertiaryContainer)
        else -> Triple("In progress", colors.primaryContainer, colors.onPrimaryContainer)
    }
    GymBadge(text = label, container = container, content = content)
}

@Composable
private fun SuggestionBox(title: String, detail: String, suggestedWeight: Double?) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            suggestedWeight?.let { weight ->
                Text(
                    text = "Next: ${ProgressiveOverloadEngine.formatWeight(weight)}kg",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SaveBar(
    canSave: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryGymButton(
                text = if (isSaving) "Saving" else "Save workout",
                onClick = onSave,
                enabled = canSave && !isSaving,
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
            )
            TextButton(onClick = onDiscard, enabled = !isSaving) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Discard workout", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviousPerformance(previousSets: List<LoggedSetEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("Previous")
        if (previousSets.isEmpty()) {
            Text(
                text = "No previous sets",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                previousSets.forEach { set ->
                    StatChip(
                        "${ProgressiveOverloadEngine.formatWeight(set.weight)}kg x ${set.reps} @${set.rir}",
                    )
                }
            }
        }
    }
}

@Composable
private fun SetDraftRow(
    index: Int,
    draft: SetDraft,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onRir: (String) -> Unit,
    onNotes: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactField(
                value = draft.weight,
                onValueChange = onWeight,
                placeholder = "Kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
            CompactField(
                value = draft.reps,
                onValueChange = onReps,
                placeholder = "Reps",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            CompactField(
                value = draft.rir,
                onValueChange = onRir,
                placeholder = "RIR",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove set ${index + 1}")
            }
        }
        CompactField(
            value = draft.notes,
            onValueChange = onNotes,
            placeholder = "Set ${index + 1} notes",
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.EmojiEvents,
        )
    }
}

/**
 * Lightweight text field used for the repeated set inputs. Uses [BasicTextField] with a static
 * surfaceVariant box instead of OutlinedTextField, so there is no animated border/label work on
 * focus — this keeps scrolling and field focus smooth on real devices when many fields are visible.
 */
@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        }
    }
}

private fun String.filterDecimal(): String {
    var dotUsed = false
    return buildString {
        this@filterDecimal.forEach { char ->
            when {
                char.isDigit() -> append(char)
                char == '.' && !dotUsed -> {
                    append(char)
                    dotUsed = true
                }
            }
        }
    }
}
