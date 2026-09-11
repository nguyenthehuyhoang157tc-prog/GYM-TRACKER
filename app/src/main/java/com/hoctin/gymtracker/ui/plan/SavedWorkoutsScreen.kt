package com.hoctin.gymtracker.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.SavedWorkoutExerciseEntity
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SavedWorkoutsScreen(
    viewModel: SavedWorkoutsViewModel,
    catalogRepository: ExerciseCatalogRepository,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: (Long) -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val catalog by catalogRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val catalogBySlug = remember(catalog) { catalog.associateBy(CatalogExercise::slug) }
    val listState = rememberLazyListState()
    var expandedId by remember { mutableLongStateOf(-1L) }
    var deleteId by remember { mutableLongStateOf(-1L) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 170f + listState.firstVisibleItemScrollOffset.toFloat()
            }
            .planV3PageEntrance(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = auronScreenTopPadding(10.dp),
                bottom = 130.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            "Your Workouts",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        "Create and use reusable one-off workouts here. They stay separate from your Plan and never enter the Home Routine rotation.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.loading) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.workouts.isEmpty()) {
                item(key = "empty") {
                    SavedWorkoutEmptyState(onCreate)
                }
            } else {
                items(state.workouts, key = { it.template.workout.id }) { item ->
                    val expanded = expandedId == item.template.workout.id
                    SavedWorkoutCard(
                        item = item,
                        expanded = expanded,
                        catalogBySlug = catalogBySlug,
                        onToggle = {
                            expandedId = if (expanded) -1L else item.template.workout.id
                        },
                        onEdit = { onEdit(item.template.workout.id) },
                        onDelete = { deleteId = item.template.workout.id },
                        onStart = { onStart(item.template.workout.id) },
                    )
                }
            }
        }

        WhiteBottomCta(
            text = "Create New Workout",
            onClick = onCreate,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (deleteId >= 0L) {
        val name = state.workouts.firstOrNull { it.template.workout.id == deleteId }?.template?.workout?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteId = -1L },
            title = { Text("Delete $name?", fontWeight = FontWeight.Black) },
            text = { Text("Only this reusable template is deleted. Completed workout History stays intact.") },
            dismissButton = { TextButton(onClick = { deleteId = -1L }) { Text("Cancel") } },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTemplate(deleteId); deleteId = -1L }) {
                    Text("Delete Workout", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun SavedWorkoutCard(
    item: SavedWorkoutCardUi,
    expanded: Boolean,
    catalogBySlug: Map<String, CatalogExercise>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val workout = item.template.workout
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auronCardElevation(18.dp)
            .clip(shape)
            .background(planV3Surface())
            .border(gymContainerBorder(alpha = 0.1f), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .planV3Pressable(onClick = onToggle)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    workout.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Created ${formatSavedWorkoutDate(workout.createdAtMillis)} · completed ${item.completionCount} ${if (item.completionCount == 1) "time" else "times"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SavedWorkoutAction(
                        text = "DELETE WORKOUT",
                        onClick = onDelete,
                        destructive = true,
                        modifier = Modifier.weight(1f),
                    )
                    SavedWorkoutAction(
                        text = "EDIT WORKOUT",
                        onClick = onEdit,
                        light = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                SavedWorkoutAction(
                    text = "START SESSION",
                    onClick = onStart,
                    light = true,
                    enabled = item.template.exercises.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (item.template.exercises.isEmpty()) {
                    Text(
                        "This template has no exercises yet. Edit it before starting.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    item.template.exercises.forEach { exercise ->
                        SavedWorkoutExerciseRow(exercise, exercise.catalogSlug?.let(catalogBySlug::get))
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedWorkoutExerciseRow(exercise: SavedWorkoutExerciseEntity, catalog: CatalogExercise?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseThumbnail(
            imageRef = catalog?.firstThumbnailRef(),
            exerciseName = exercise.exerciseNameSnapshot,
            size = 48.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                exercise.exerciseNameSnapshot,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                catalog?.muscleTags().orEmpty().ifBlank { "#custom_exercise" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SavedWorkoutAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    light: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (light) Color.White else Color.Transparent
    val foreground = when {
        destructive -> MaterialTheme.colorScheme.error
        light -> Color(0xFF111015)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (destructive) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.18f),
                shape = shape,
            )
            .height(56.dp)
            .planV3Pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = foreground,
            fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun WhiteBottomCta(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.94f))
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.35f))
                .planV3Pressable(enabled = enabled, onClick = onClick)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color(0xFF111015), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SavedWorkoutEmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("No one-off workouts yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            "Create one without changing your Routine or Home selection.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(onClick = onCreate) {
            Text("Create New Workout")
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}

private fun formatSavedWorkoutDate(millis: Long): String = runCatching {
    DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
}.getOrDefault("recently")
