package com.hoctin.gymtracker.ui.plan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.IconThumbnail
import com.hoctin.gymtracker.ui.components.SectionLabel

@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onOpenExercise: (Long) -> Unit,
) {
    val plan = viewModel.plan.collectAsStateWithLifecycle().value
    var dayDialog by remember { mutableStateOf<WorkoutDayEntity?>(null) }
    var isAddingDay by remember { mutableStateOf(false) }
    var exerciseDialog by remember { mutableStateOf<Pair<Long, ExerciseEntity?>?>(null) }
    val expandedState = remember { mutableStateMapOf<Long, Boolean>() }
    // Only the first day starts expanded so opening Plan does not compose every exercise row at once.
    val firstDayId = plan.firstOrNull()?.day?.id

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Plan",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${plan.size} workout days",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    FilledTonalIconButton(onClick = { isAddingDay = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add day")
                    }
                }
            }
        }

        items(plan, key = { it.day.id }) { dayWithExercises ->
            val expanded = expandedState[dayWithExercises.day.id] ?: (dayWithExercises.day.id == firstDayId)
            DayCard(
                dayWithExercises = dayWithExercises,
                expanded = expanded,
                onToggle = { expandedState[dayWithExercises.day.id] = !expanded },
                onEditDay = { dayDialog = dayWithExercises.day },
                onDeleteDay = { viewModel.deleteDay(dayWithExercises.day) },
                onOpenExercise = onOpenExercise,
                onEditExercise = { exerciseDialog = dayWithExercises.day.id to it },
                onDeleteExercise = { viewModel.deleteExercise(it) },
                onAddExercise = { exerciseDialog = dayWithExercises.day.id to null },
            )
        }
    }

    if (isAddingDay) {
        DayDialog(
            title = "Add day",
            initialName = "",
            onDismiss = { isAddingDay = false },
            onSave = {
                viewModel.addDay(it)
                isAddingDay = false
            },
        )
    }

    dayDialog?.let { day ->
        DayDialog(
            title = "Edit day",
            initialName = day.name,
            onDismiss = { dayDialog = null },
            onSave = {
                viewModel.updateDay(day, it)
                dayDialog = null
            },
        )
    }

    exerciseDialog?.let { (dayId, exercise) ->
        ExerciseDialog(
            title = if (exercise == null) "Add exercise" else "Edit exercise",
            exercise = exercise,
            onDismiss = { exerciseDialog = null },
            onSave = { name, sets, minReps, maxReps ->
                if (exercise == null) {
                    viewModel.addExercise(dayId, name, sets, minReps, maxReps)
                } else {
                    viewModel.updateExercise(exercise, name, sets, minReps, maxReps)
                }
                exerciseDialog = null
            },
        )
    }
}

@Composable
private fun DayCard(
    dayWithExercises: WorkoutDayWithExercises,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditDay: () -> Unit,
    onDeleteDay: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onEditExercise: (ExerciseEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconThumbnail(icon = Icons.Outlined.FitnessCenter, size = 44)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = dayWithExercises.day.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${dayWithExercises.exercises.size} exercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEditDay) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Edit", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = onDeleteDay) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Delete", modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    dayWithExercises.exercises.forEach { exercise ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ExerciseRow(
                            exercise = exercise,
                            onOpen = { onOpenExercise(exercise.id) },
                            onEdit = { onEditExercise(exercise) },
                            onDelete = { onDeleteExercise(exercise) },
                        )
                    }

                    OutlinedButton(
                        onClick = onAddExercise,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Add exercise", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconThumbnail(icon = Icons.Outlined.FitnessCenter, size = 40)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymBadge("${exercise.targetSets} sets")
                GymBadge("${exercise.targetMinReps}-${exercise.targetMaxReps} reps")
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit exercise", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete exercise", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DayDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
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
private fun ExerciseDialog(
    title: String,
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int) -> Unit,
) {
    var name by remember(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var sets by remember(exercise?.id) { mutableStateOf((exercise?.targetSets ?: 3).toString()) }
    var minReps by remember(exercise?.id) { mutableStateOf((exercise?.targetMinReps ?: 8).toString()) }
    var maxReps by remember(exercise?.id) { mutableStateOf((exercise?.targetMaxReps ?: 12).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Sets", sets, { sets = it }, Modifier.weight(1f))
                    NumberField("Min", minReps, { minReps = it }, Modifier.weight(1f))
                    NumberField("Max", maxReps, { maxReps = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        sets.toIntOrNull() ?: 3,
                        minReps.toIntOrNull() ?: 8,
                        maxReps.toIntOrNull() ?: 12,
                    )
                },
            ) {
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
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
    )
}
