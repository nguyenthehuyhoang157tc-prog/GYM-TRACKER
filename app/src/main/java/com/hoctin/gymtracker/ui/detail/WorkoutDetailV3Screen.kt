package com.hoctin.gymtracker.ui.detail

import android.animation.ValueAnimator
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.ui.catalog.CatalogDetailHost
import com.hoctin.gymtracker.ui.catalog.rememberCatalogDetailController
import com.hoctin.gymtracker.ui.components.AuronModalSheet
import com.hoctin.gymtracker.ui.components.IconThumbnail
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.plan.RoutineExerciseList
import com.hoctin.gymtracker.ui.plan.catalogLookup
import com.hoctin.gymtracker.ui.plan.planV3PageEntrance
import com.hoctin.gymtracker.ui.plan.planV3Pressable
import com.hoctin.gymtracker.ui.plan.planV3Surface
import com.hoctin.gymtracker.ui.theme.auronAccentBright
import com.hoctin.gymtracker.ui.theme.auronAccentDeep
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import com.hoctin.gymtracker.ui.workoutDayLabel
import com.hoctin.gymtracker.ui.workoutDaySubtitle
import kotlinx.coroutines.launch

@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onStartWorkout: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenCatalog: (Long) -> Unit,
    onOpenYourWorkouts: () -> Unit,
    onRoutineChanged: (Long) -> Unit,
    catalogRepository: ExerciseCatalogRepository,
    onBack: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val maxWeightByExerciseId = viewModel.maxWeightByExerciseId.collectAsStateWithLifecycle().value
    val day = state.day
    val catalog by catalogRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val catalogByName = remember(catalog) { catalogLookup(catalog) }
    val catalogDetail = rememberCatalogDetailController(catalogRepository)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showStartConfirm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    var deleting by remember { mutableStateOf<ExerciseEntity?>(null) }
    var addChoice by remember { mutableStateOf(false) }
    var addCustom by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.routineChanged.collect(onRoutineChanged)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 170f + listState.firstVisibleItemScrollOffset.toFloat()
            }
            .planV3PageEntrance(),
    ) {
        when {
            day != null -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item(key = "hero") {
                    RoutineDetailHero(day.day.name, state.dayNumber, onBack)
                }
                item(key = "start") {
                    PrimaryGymButton(
                        text = "Start Workout",
                        onClick = { showStartConfirm = true },
                        modifier = Modifier.padding(horizontal = 20.dp),
                        icon = {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
                item(key = "variety") {
                    ChangeRoutineCard(
                        routineCount = state.planSize,
                        onSkip = viewModel::skipToNextRoutine,
                        onModify = {
                            scope.launch {
                                if (ValueAnimator.areAnimatorsEnabled()) listState.animateScrollToItem(4)
                                else listState.scrollToItem(4)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(key = "single-workout") {
                    StartSingleWorkoutCard(
                        onClick = onOpenYourWorkouts,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(key = "exercises") {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                SectionLabel("Routine exercises")
                                Text(
                                    "Swipe to edit or delete · hold to reorder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${day.exercises.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RoutineExerciseList(
                            exercises = day.exercises,
                            catalogByName = catalogByName,
                            onOpen = { onOpenExercise(it.id) },
                            onEdit = { editing = it },
                            onDelete = { deleting = it },
                            onReordered = viewModel::reorderExercises,
                            maxWeightByExerciseId = maxWeightByExerciseId,
                        )
                        TextButton(
                            onClick = { addChoice = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("Add exercise", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Routine not found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                PrimaryGymButton("Back to Plan", onClick = onBack)
            }
        }

        CatalogDetailHost(catalogDetail)
    }

    if (showStartConfirm && day != null) {
        RoutineStartDialog(
            workoutName = day.day.name,
            onConfirm = { showStartConfirm = false; onStartWorkout(day.day.id) },
            onCancel = { showStartConfirm = false },
        )
    }
    editing?.let { exercise ->
        RoutineExerciseDialog(
            title = "Edit exercise",
            exercise = exercise,
            onDismiss = { editing = null },
            onSave = { name, sets, min, max ->
                viewModel.updateExercise(exercise, name, sets, min, max)
                editing = null
            },
        )
    }
    deleting?.let { exercise ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Remove ${exercise.name}?") },
            text = { Text("This removes the exercise from ${day?.day?.name.orEmpty()}. Saved History is unchanged.") },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteExercise(exercise); deleting = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
    if (addChoice && day != null) {
        AuronModalSheet(onDismiss = { addChoice = false }) {
            Text("Add exercise", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            RoutineAddChoice(
                icon = Icons.Outlined.Search,
                title = "Browse FED catalog",
                subtitle = "Choose a real catalog exercise and prescription",
                onClick = { addChoice = false; onOpenCatalog(day.day.id) },
            )
            RoutineAddChoice(
                icon = Icons.Outlined.Edit,
                title = "Create custom exercise",
                subtitle = "Add a user-authored exercise to this Routine",
                onClick = { addChoice = false; addCustom = true },
            )
        }
    }
    if (addCustom) {
        RoutineExerciseDialog(
            title = "Custom exercise",
            exercise = null,
            onDismiss = { addCustom = false },
            onSave = { name, sets, min, max ->
                viewModel.addExercise(name, sets, min, max)
                addCustom = false
            },
        )
    }
}

@Composable
private fun ChangeRoutineCard(
    routineCount: Int,
    onSkip: () -> Unit,
    onModify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(planV3Surface())
            .border(gymContainerBorder(alpha = 0.1f), shape)
            .padding(top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "VARIETY AND SUBSTITUTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text("Change today's workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Move to the next Routine in your actual Plan order, or edit this Routine directly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .planV3Pressable(enabled = routineCount > 1, onClick = onSkip)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (routineCount > 1) "Skip this workout" else "Only Routine",
                    color = if (routineCount > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .planV3Pressable(onClick = onModify)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Modify this workout", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun StartSingleWorkoutCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(planV3Surface())
            .border(gymContainerBorder(alpha = 0.1f), shape)
            .planV3Pressable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "🏋️",
            fontSize = 28.sp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Start single workout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "Start a single workout outside of your plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RoutineAddChoice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconThumbnail(icon, size = 44)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun RoutineExerciseDialog(
    title: String,
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int) -> Unit,
) {
    var name by remember(exercise) { mutableStateOf(exercise?.name.orEmpty()) }
    var sets by remember(exercise) { mutableStateOf(exercise?.targetSets?.toString() ?: "3") }
    var min by remember(exercise) { mutableStateOf(exercise?.targetMinReps?.toString() ?: "8") }
    var max by remember(exercise) { mutableStateOf(exercise?.targetMaxReps?.toString() ?: "12") }
    val parsedSets = sets.toIntOrNull()
    val parsedMin = min.toIntOrNull()
    val parsedMax = max.toIntOrNull()
    val valid = name.isNotBlank() && parsedSets != null && parsedSets > 0 && parsedMin != null && parsedMin >= 0 && parsedMax != null && parsedMax >= parsedMin
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Exercise name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        sets, { sets = it.filter(Char::isDigit) }, label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        min, { min = it.filter(Char::isDigit) }, label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        max, { max = it.filter(Char::isDigit) }, label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(name, parsedSets ?: 3, parsedMin ?: 8, parsedMax ?: 12) },
            ) { Text("Save") }
        },
    )
}

@Composable
private fun RoutineStartDialog(workoutName: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(gymContainerBorder(alpha = 0.12f), shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Let's get it", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "Are you ready to start \"$workoutName\"?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(gymContainerBorder(alpha = 0.22f), RoundedCornerShape(50))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Start Workout", color = Color(0xFF101013), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun RoutineDetailHero(dayName: String, dayNumber: Int, onBack: () -> Unit) {
    val heroRes = routineHeroRes(dayName)
    val dark = isAuronDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.linearGradient(
                    listOf(auronAccentDeep(), if (dark) Color(0xFF35245E) else Color(0xFF315FD2), Color(0xFF09040F)),
                ),
            ),
    ) {
        heroRes?.let {
            Image(painterResource(it), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.42f),
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.9f),
                ),
            ),
        )
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 10.dp, start = 14.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("ROUTINE", style = MaterialTheme.typography.labelMedium, color = auronAccentBright(), letterSpacing = 1.6.sp)
            Text(
                workoutDayLabel(dayNumber, dayName),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                workoutDaySubtitle(dayName),
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@DrawableRes
private fun routineHeroRes(name: String): Int? = when {
    name.contains("push", true) -> R.drawable.detail_hero_push
    name.contains("pull", true) -> R.drawable.detail_hero_pull
    name.contains("leg", true) -> R.drawable.detail_hero_legs
    name.contains("full", true) -> R.drawable.detail_hero_full_body
    else -> null
}
