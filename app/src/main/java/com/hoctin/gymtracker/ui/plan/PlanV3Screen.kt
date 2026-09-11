package com.hoctin.gymtracker.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding

@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onOpenRoutine: (Long) -> Unit,
    onOpenYourWorkouts: () -> Unit,
    onOpenGenerateWorkout: () -> Unit,
    catalogRepository: ExerciseCatalogRepository,
) {
    val plan = viewModel.plan.collectAsStateWithLifecycle().value
    val maxWeightByExerciseId = viewModel.maxWeightByExerciseId.collectAsStateWithLifecycle().value
    val catalog by catalogRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val catalogByName = remember(catalog) { catalogLookup(catalog) }
    val listState = rememberLazyListState()
    var addRoutine by remember { mutableStateOf(false) }
    var editRoutine by remember { mutableStateOf<WorkoutDayEntity?>(null) }
    var deleteRoutine by remember { mutableStateOf<WorkoutDayEntity?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 160f + listState.firstVisibleItemScrollOffset.toFloat()
            }
            .planV3PageEntrance(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = auronScreenTopPadding(16.dp),
            bottom = BottomNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Plan",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Your Routine stays separate from one-off workouts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "your-workouts") {
            PlanActionTile(
                icon = Icons.Outlined.FolderOpen,
                eyebrow = "ONE-OFF SESSIONS",
                title = "Your Workouts",
                subtitle = "Build, save and start workouts outside your Routine.",
                onClick = onOpenYourWorkouts,
            )
        }
        item(key = "generate") {
            PlanActionTile(
                icon = Icons.Outlined.AutoAwesome,
                eyebrow = "COMING SOON",
                title = "Generate Workout",
                subtitle = "A future guided workout builder — no fake generation today.",
                onClick = onOpenGenerateWorkout,
                muted = true,
            )
        }

        item(key = "routines-header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ROUTINES",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${plan.size} workout days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { addRoutine = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Routine")
                }
            }
        }

        if (plan.isEmpty()) {
            item(key = "empty") {
                EmptyRoutineState(onAdd = { addRoutine = true })
            }
        } else {
            items(plan, key = { it.day.id }) { routine ->
                RoutinePreviewCard(
                    routine = routine,
                    catalogByName = catalogByName,
                    maxWeightByExerciseId = maxWeightByExerciseId,
                    onOpen = { onOpenRoutine(routine.day.id) },
                    onEdit = { editRoutine = routine.day },
                    onDelete = { deleteRoutine = routine.day },
                )
            }
        }
    }

    if (addRoutine) {
        RoutineNameDialog(
            title = "Create Routine",
            initialName = "",
            onDismiss = { addRoutine = false },
            onSave = { name -> viewModel.addDay(name); addRoutine = false },
        )
    }
    editRoutine?.let { routine ->
        RoutineNameDialog(
            title = "Rename Routine",
            initialName = routine.name,
            onDismiss = { editRoutine = null },
            onSave = { name -> viewModel.updateDay(routine, name); editRoutine = null },
        )
    }
    deleteRoutine?.let { routine ->
        AlertDialog(
            onDismissRequest = { deleteRoutine = null },
            title = { Text("Delete ${routine.name}?") },
            text = { Text("This removes the Routine and its planned exercises. Saved History is not changed.") },
            dismissButton = { TextButton(onClick = { deleteRoutine = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDay(routine); deleteRoutine = null }) {
                    Text("Delete Routine", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun PlanActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (muted) planV3MutedSurface() else planV3Surface(),
        ),
        border = gymContainerBorder(alpha = if (muted) 0.06f else 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .auronCardElevation(22.dp)
            .planV3Pressable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (muted) 0.06f else 0.1f))
                    .border(gymContainerBorder(alpha = 0.08f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoutinePreviewCard(
    routine: WorkoutDayWithExercises,
    catalogByName: Map<String, com.hoctin.gymtracker.data.catalog.CatalogExercise>,
    maxWeightByExerciseId: Map<Long, Double>,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    var menuOpen by remember { mutableStateOf(false) }
    val preview = routine.exercises.take(4)
    val more = (routine.exercises.size - preview.size).coerceAtLeast(0)
    val totalSets = routine.exercises.sumOf { it.targetSets }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = planV3Surface()),
        border = gymContainerBorder(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .auronCardElevation(22.dp)
            .planV3Pressable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(start = 18.dp, top = 18.dp, end = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = routine.day.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "$totalSets sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Routine actions")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename Routine") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = { menuOpen = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Routine", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
            preview.forEach { exercise ->
                val catalog = catalogByName[com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey.normalizeName(exercise.name)]
                val maxWeight = maxWeightByExerciseId[exercise.id]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumbnail(
                        imageRef = catalog?.firstThumbnailRef(),
                        exerciseName = exercise.name,
                        size = 42.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // Omitted entirely (not "0 kg") when the exercise has no weighted history.
                        if (maxWeight != null) {
                            Text(
                                text = "Max Weight ${ProgressiveOverloadEngine.formatWeight(maxWeight)} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (routine.exercises.isEmpty()) {
                Text(
                    text = "No exercises yet — open this Routine to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (more > 0) {
                Text(
                    text = "and $more more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyRoutineState(onAdd: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(planV3Surface())
            .border(gymContainerBorder(alpha = 0.08f), shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(30.dp))
        Text("Build your first Routine", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            "Routines power Home, Weekly Progress and the normal progression lifecycle.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAdd) { Text("Add Routine") }
    }
}

@Composable
private fun RoutineNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Routine name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
    )
}
