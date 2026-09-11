package com.hoctin.gymtracker.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.WorkoutDayEntity
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.thumbnailRefFor
import com.hoctin.gymtracker.data.catalog.trackingTypeFor
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.components.AuronModalSheet
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.IconThumbnail
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.format.SetDisplay
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronGlassSurface
import com.hoctin.gymtracker.ui.theme.auronGlassSurfaceVariant
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding

@Composable
private fun LegacyPlanScreen(
    viewModel: PlanViewModel,
    onOpenExercise: (Long) -> Unit,
    onOpenCatalog: (Long) -> Unit,
    catalogRepository: ExerciseCatalogRepository,
) {
    val plan = viewModel.plan.collectAsStateWithLifecycle().value
    val thumbnailRefs by catalogRepository.observeThumbnailRefsByName()
        .collectAsStateWithLifecycle(initialValue = emptyMap<String, String>())
    val trackingTypes by catalogRepository.observeTrackingTypesByName()
        .collectAsStateWithLifecycle(initialValue = emptyMap<String, TrackingType>())
    var dayDialog by remember { mutableStateOf<WorkoutDayEntity?>(null) }
    var isAddingDay by remember { mutableStateOf(false) }
    var exerciseDialog by remember { mutableStateOf<Pair<Long, ExerciseEntity?>?>(null) }
    // Which day is choosing how to add an exercise (catalog vs custom). Null = no chooser shown.
    var addChoiceForDay by remember { mutableStateOf<Long?>(null) }
    val expandedState = remember { mutableStateMapOf<Long, Boolean>() }
    val listState = rememberLazyListState()
    // Only the first day starts expanded so opening Plan does not compose every exercise row at once.
    val firstDayId = plan.firstOrNull()?.day?.id

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
                            fontWeight = FontWeight.Black,
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
                onReorderExercises = { viewModel.reorderExercises(it) },
                onAddExercise = { addChoiceForDay = dayWithExercises.day.id },
                thumbnailRefs = thumbnailRefs,
                trackingTypes = trackingTypes,
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

    addChoiceForDay?.let { dayId ->
        AddExerciseChoiceSheet(
            onDismiss = { addChoiceForDay = null },
            onBrowseCatalog = {
                addChoiceForDay = null
                onOpenCatalog(dayId)
            },
            onCreateCustom = {
                addChoiceForDay = null
                exerciseDialog = dayId to null
            },
        )
    }
}

/**
 * Bottom-sheet chooser shown when adding an exercise to a day: browse the catalog (DB-1) or create a
 * custom one (the existing dialog flow, unchanged).
 */
@Composable
private fun AddExerciseChoiceSheet(
    onDismiss: () -> Unit,
    onBrowseCatalog: () -> Unit,
    onCreateCustom: () -> Unit,
) {
    AuronModalSheet(onDismiss = onDismiss) {
        Text(
            text = "Add exercise",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        AddChoiceRow(
            icon = Icons.Outlined.Search,
            title = "Browse catalog",
            subtitle = "Pick from the AURON exercise library",
            onClick = onBrowseCatalog,
        )
        AddChoiceRow(
            icon = Icons.Outlined.Edit,
            title = "Create custom exercise",
            subtitle = "Enter your own name, sets and reps",
            onClick = onCreateCustom,
        )
    }
}

@Composable
private fun AddChoiceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val rowShape = MaterialTheme.shapes.large
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(auronGlassSurfaceVariant())
            .border(gymContainerBorder(alpha = 0.06f), rowShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconThumbnail(icon = icon, size = 44)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    onReorderExercises: (List<ExerciseEntity>) -> Unit,
    onAddExercise: () -> Unit,
    thumbnailRefs: Map<String, String>,
    trackingTypes: Map<String, TrackingType>,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = auronGlassSurface()),
        border = gymContainerBorder(),
        modifier = modifier
            .fillMaxWidth()
            .auronCardElevation(22.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(160)),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Day-level actions: equal-width and balanced across the card. Delete is
                    // red-accented (tasteful destructive styling), Edit stays neutral.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onEditDay,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Edit", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = onDeleteDay,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Delete", modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    ReorderableExerciseColumn(
                        exercises = dayWithExercises.exercises,
                        onReordered = onReorderExercises,
                        onOpenExercise = onOpenExercise,
                        onEditExercise = onEditExercise,
                        onDeleteExercise = onDeleteExercise,
                        thumbnailRefs = thumbnailRefs,
                        trackingTypes = trackingTypes,
                    )

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

/**
 * The day's exercises as a long-press drag-to-reorder list. The dragged row lifts (shadow + scale +
 * higher z), neighbours reflow as it crosses their mid-point, and the new order is persisted on drop.
 * A local [mutableStateListOf] mirrors the persisted list and is re-seeded whenever [exercises] changes
 * (add / delete / a persisted reorder), keeping UI and DB in sync. Only the per-frame [dragOffsetY] is
 * read in the draw/layout phase, so dragging doesn't recompose the list each frame.
 */
@Composable
private fun ReorderableExerciseColumn(
    exercises: List<ExerciseEntity>,
    onReordered: (List<ExerciseEntity>) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onEditExercise: (ExerciseEntity) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit,
    thumbnailRefs: Map<String, String>,
    trackingTypes: Map<String, TrackingType>,
) {
    val haptics = LocalHapticFeedback.current
    val gapPx = with(LocalDensity.current) { 10.dp.toPx() }
    val rowShape = MaterialTheme.shapes.medium
    val items = remember(exercises) { mutableStateListOf<ExerciseEntity>().apply { addAll(exercises) } }
    val rowHeights = remember { mutableStateMapOf<Long, Int>() }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { exercise ->
            key(exercise.id) {
                val isDragged = exercise.id == draggedId
                val elevation by animateDpAsState(if (isDragged) 10.dp else 0.dp, label = "dragElevation")
                val scale by animateFloatAsState(if (isDragged) 1.03f else 1f, label = "dragScale")
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffsetY else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation.toPx()
                            shape = rowShape
                            clip = false
                        }
                        .onGloballyPositioned { rowHeights[exercise.id] = it.size.height },
                ) {
                    ExerciseRow(
                        exercise = exercise,
                        dragging = isDragged,
                        onOpen = { onOpenExercise(exercise.id) },
                        onEdit = { onEditExercise(exercise) },
                        onDelete = { onDeleteExercise(exercise) },
                        imageRef = thumbnailRefs.thumbnailRefFor(exercise.name),
                        trackingType = trackingTypes.trackingTypeFor(exercise.name) ?: TrackingType.REPS_WEIGHT,
                        // Keyed on [exercises] so the gesture closure re-binds to the fresh local copy
                        // after a persisted reorder (during a drag, [exercises] is stable so it survives).
                        dragHandleModifier = Modifier.pointerInput(exercises) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = exercise.id
                                    dragOffsetY = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                    onReordered(items.toList())
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                    onReordered(items.toList())
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val curIndex = items.indexOfFirst { it.id == draggedId }
                                    // Swap with a neighbour once dragged past half its height, then offset
                                    // the running delta by that slot so the lifted row stays under the finger.
                                    if (curIndex in 0 until items.lastIndex && dragOffsetY > 0) {
                                        val nextH = rowHeights[items[curIndex + 1].id] ?: 0
                                        if (nextH > 0 && dragOffsetY > nextH / 2f) {
                                            items.add(curIndex + 1, items.removeAt(curIndex))
                                            dragOffsetY -= (nextH + gapPx)
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (curIndex > 0 && dragOffsetY < 0) {
                                        val prevH = rowHeights[items[curIndex - 1].id] ?: 0
                                        if (prevH > 0 && dragOffsetY < -prevH / 2f) {
                                            items.add(curIndex - 1, items.removeAt(curIndex))
                                            dragOffsetY += (prevH + gapPx)
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    dragging: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    imageRef: String?,
    trackingType: TrackingType,
    dragHandleModifier: Modifier,
) {
    val rowShape = MaterialTheme.shapes.medium
    val container = if (dragging) auronGlassSurfaceVariant(0.88f) else auronGlassSurfaceVariant()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(container)
            .border(gymContainerBorder(alpha = if (dragging) 0.20f else 0.05f), rowShape)
            .clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drag handle: long-press here to reorder within this day.
        Box(
            modifier = dragHandleModifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
        ExerciseThumbnail(
            imageRef = imageRef,
            exerciseName = exercise.name,
            size = 44.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GymBadge("${exercise.targetSets} sets")
                if (trackingType.isRepBased) {
                    GymBadge("${exercise.targetMinReps}-${exercise.targetMaxReps} reps")
                } else {
                    GymBadge(
                        SetDisplay.targetLabel(
                            targetSets = exercise.targetSets,
                            targetMinReps = exercise.targetMinReps,
                            targetMaxReps = exercise.targetMaxReps,
                            trackingType = trackingType,
                        ).substringAfter("·").trim(),
                    )
                }
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit exercise", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            // Red-accented destructive action.
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete exercise",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
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
        // UI-1 Part F: neutral card surface, no purple tonal tint.
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
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
        // UI-1 Part F: neutral card surface, no purple tonal tint.
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
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
