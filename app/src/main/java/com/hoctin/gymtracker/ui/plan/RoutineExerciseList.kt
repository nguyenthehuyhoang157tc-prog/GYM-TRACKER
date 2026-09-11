package com.hoctin.gymtracker.ui.plan

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import kotlin.math.abs

/**
 * Open Routine list with deliberate gesture arbitration:
 * tap opens details, horizontal swipe commits edit/delete, and a long vertical press begins reorder.
 * No permanent drag handle is rendered.
 */
@Composable
internal fun RoutineExerciseList(
    exercises: List<ExerciseEntity>,
    catalogByName: Map<String, CatalogExercise>,
    onOpen: (ExerciseEntity) -> Unit,
    onEdit: (ExerciseEntity) -> Unit,
    onDelete: (ExerciseEntity) -> Unit,
    onReordered: (List<ExerciseEntity>) -> Unit,
    modifier: Modifier = Modifier,
    /** Heaviest logged load per exercise id; rows with no weighted history show no max line. */
    maxWeightByExerciseId: Map<Long, Double> = emptyMap(),
) {
    val haptics = LocalHapticFeedback.current
    val gapPx = with(LocalDensity.current) { 8.dp.toPx() }
    val items = remember(exercises) { mutableStateListOf<ExerciseEntity>().apply { addAll(exercises) } }
    val heights = remember { mutableStateMapOf<Long, Int>() }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { exercise ->
            key(exercise.id) {
                val dragging = draggedId == exercise.id
                val scale by animateFloatAsState(if (dragging) 1.018f else 1f, label = "routineRowScale")
                val elevation by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "routineRowLift")
                val catalog = catalogByName[CanonicalExerciseKey.normalizeName(exercise.name)]
                Box(
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffsetY else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation.toPx()
                            shape = RoundedCornerShape(18.dp)
                            clip = false
                        }
                        .onGloballyPositioned { heights[exercise.id] = it.size.height },
                ) {
                    SwipeRoutineExerciseRow(
                        exercise = exercise,
                        catalog = catalog,
                        maxWeight = maxWeightByExerciseId[exercise.id],
                        onOpen = { onOpen(exercise) },
                        onEdit = { onEdit(exercise) },
                        onDelete = { onDelete(exercise) },
                        dragModifier = Modifier.pointerInput(exercises) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = exercise.id
                                    dragOffsetY = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                    onReordered(items.toList())
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y
                                    val index = items.indexOfFirst { it.id == draggedId }
                                    if (index in 0 until items.lastIndex && dragOffsetY > 0f) {
                                        val nextHeight = heights[items[index + 1].id] ?: 0
                                        if (nextHeight > 0 && dragOffsetY > nextHeight / 2f) {
                                            items.add(index + 1, items.removeAt(index))
                                            dragOffsetY -= nextHeight + gapPx
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (index > 0 && dragOffsetY < 0f) {
                                        val previousHeight = heights[items[index - 1].id] ?: 0
                                        if (previousHeight > 0 && dragOffsetY < -previousHeight / 2f) {
                                            items.add(index - 1, items.removeAt(index))
                                            dragOffsetY += previousHeight + gapPx
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
private fun SwipeRoutineExerciseRow(
    exercise: ExerciseEntity,
    catalog: CatalogExercise?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier,
    maxWeight: Double? = null,
) {
    val density = LocalDensity.current
    val actionThreshold = with(density) { 76.dp.toPx() }
    var offsetX by remember(exercise.id) { mutableFloatStateOf(0f) }
    val swipeState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-actionThreshold * 1.25f, actionThreshold * 1.25f)
    }
    val actionColor = when {
        offsetX < 0f -> MaterialTheme.colorScheme.error
        offsetX > 0f -> Color.White
        else -> Color.Transparent
    }
    val actionIcon = if (offsetX < 0f) Icons.Outlined.Delete else Icons.Outlined.Edit
    val actionTint = if (offsetX < 0f) Color.White else Color(0xFF121014)
    val actionAlignment = if (offsetX < 0f) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(actionColor),
    ) {
        if (offsetX != 0f) {
            Box(
                modifier = Modifier
                    .align(actionAlignment)
                    .padding(horizontal = 22.dp)
                    .size(36.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(actionIcon, contentDescription = null, tint = actionTint, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offsetX }
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = swipeState,
                    onDragStopped = {
                        val action = when {
                            offsetX <= -actionThreshold -> onDelete
                            offsetX >= actionThreshold -> onEdit
                            else -> null
                        }
                        offsetX = 0f
                        action?.invoke()
                    },
                )
                .then(dragModifier)
                .clickable(enabled = abs(offsetX) < 4f, onClick = onOpen)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseThumbnail(
                imageRef = catalog?.firstThumbnailRef(),
                exerciseName = exercise.name,
                size = 52.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = catalog?.muscleTags().orEmpty().ifBlank { "#custom_exercise" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}
