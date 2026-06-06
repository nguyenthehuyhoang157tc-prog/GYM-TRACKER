package com.hoctin.gymtracker.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.SectionTitle
import com.hoctin.gymtracker.ui.components.StatChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    onSaved: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is LogWorkoutEvent.Saved) {
                onSaved()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
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
            item {
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

        items(state.exercises, key = { it.exercise.id }) { exerciseState ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = exerciseState.exercise.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatChip("${exerciseState.exercise.targetSets} sets")
                                StatChip("${exerciseState.exercise.targetMinReps}-${exerciseState.exercise.targetMaxReps} reps")
                                StatChip(exerciseState.trendLabel)
                                if (exerciseState.potentialPr) {
                                    StatChip("Potential PR")
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.addSet(exerciseState.exercise.id) }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Add set")
                        }
                    }

                    PreviousPerformance(exerciseState.previousSets)

                    exerciseState.draftSets.forEachIndexed { index, draft ->
                        SetDraftRow(
                            index = index,
                            draft = draft,
                            onWeight = { value ->
                                viewModel.updateSet(
                                    exerciseState.exercise.id,
                                    index,
                                    weight = value.filterDecimal(),
                                )
                            },
                            onReps = { value ->
                                viewModel.updateSet(
                                    exerciseState.exercise.id,
                                    index,
                                    reps = value.filter(Char::isDigit),
                                )
                            },
                            onRir = { value ->
                                viewModel.updateSet(
                                    exerciseState.exercise.id,
                                    index,
                                    rir = value.filter(Char::isDigit).take(1),
                                )
                            },
                            onNotes = { value ->
                                viewModel.updateSet(exerciseState.exercise.id, index, notes = value)
                            },
                            onDelete = { viewModel.removeSet(exerciseState.exercise.id, index) },
                        )
                    }

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
                                text = exerciseState.suggestion.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = exerciseState.suggestion.detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            exerciseState.suggestion.suggestedWeight?.let { weight ->
                                Text(
                                    text = "Next: ${ProgressiveOverloadEngine.formatWeight(weight)}kg",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            PrimaryGymButton(
                text = if (state.isSaving) "Saving" else "Save workout",
                onClick = viewModel::saveWorkout,
                enabled = state.canSave && !state.isSaving,
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
            )
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
        ) {
            OutlinedTextField(
                value = draft.weight,
                onValueChange = onWeight,
                label = { Text("Kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.reps,
                onValueChange = onReps,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.rir,
                onValueChange = onRir,
                label = { Text("RIR") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove set ${index + 1}")
            }
        }
        OutlinedTextField(
            value = draft.notes,
            onValueChange = onNotes,
            label = { Text("Set ${index + 1} notes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null)
            },
        )
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
