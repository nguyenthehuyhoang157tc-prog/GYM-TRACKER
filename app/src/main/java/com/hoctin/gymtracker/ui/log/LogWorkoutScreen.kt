package com.hoctin.gymtracker.ui.log

import android.animation.ValueAnimator
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.ExerciseHistoryItem
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.Readiness
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.thumbnailRefFor
import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.ui.format.SetDisplay
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.catalog.CatalogDetailHost
import com.hoctin.gymtracker.ui.catalog.rememberCatalogDetailController
import com.hoctin.gymtracker.ui.components.AuronCompactModalSheet
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.GoldSweepBorder
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.PrTrophyLottie
import com.hoctin.gymtracker.ui.components.RestTimerSheet
import com.hoctin.gymtracker.ui.components.SectionTitle
import com.hoctin.gymtracker.ui.components.StatChip
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import com.hoctin.gymtracker.ui.toLongDate
import kotlinx.coroutines.delay

private data class AdvancedSetSelection(val exerciseId: Long, val setKey: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogWorkoutScreen(
    viewModel: LogWorkoutViewModel,
    // MUSCLE-3: sessionId + runtime logging duration so the caller can open the Muscle Heat Review.
    onSaved: (sessionId: Long, durationMillis: Long) -> Unit,
    onDiscarded: () -> Unit,
    catalogRepository: ExerciseCatalogRepository,
    distanceUnitSystem: DistanceUnitSystem = DistanceUnitSystem.METRIC,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val maxWeightByExerciseId = viewModel.maxWeightByExerciseId.collectAsStateWithLifecycle().value
    val thumbnailRefs by catalogRepository.observeThumbnailRefsByName()
        .collectAsStateWithLifecycle(initialValue = emptyMap<String, String>())
    var showSaveSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var insightVisible by remember { mutableStateOf(true) }
    var progressionExerciseId by remember { mutableStateOf<Long?>(null) }
    var advancedSetSelection by remember { mutableStateOf<AdvancedSetSelection?>(null) }
    val listState = rememberLazyListState()
    val nextExerciseTopInsetPx = with(LocalDensity.current) { 148.dp.roundToPx() }
    // Reusable catalog detail popup: the exercise info icon looks the catalog up by name.
    val catalogDetail = rememberCatalogDetailController(catalogRepository)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LogWorkoutEvent.Saved -> {
                    showSaveSheet = false
                    onSaved(event.sessionId, event.durationMillis)
                }
                is LogWorkoutEvent.Discarded -> {
                    showDiscardDialog = false
                    onDiscarded()
                }
            }
        }
    }

    val firstExerciseItemIndex = (if (insightVisible) 1 else 0) + (if (state.message != null) 1 else 0)
    LaunchedEffect(state.scrollToExerciseId, state.exercises, firstExerciseItemIndex) {
        val targetId = state.scrollToExerciseId ?: return@LaunchedEffect
        val exerciseIndex = state.exercises.indexOfFirst { it.exercise.id == targetId }
        if (exerciseIndex >= 0) {
            listState.animateScrollToItem(
                firstExerciseItemIndex + exerciseIndex,
                scrollOffset = -nextExerciseTopInsetPx,
            )
        }
        viewModel.consumeScrollTarget()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground(),
    ) {
        // The identity block is a real opaque sibling of the scrolling list, not a narrow sticky
        // item inside LazyColumn content padding. Nothing from an exercise card can bleed around it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(logStickyHeaderColor())
                .padding(horizontal = 20.dp)
                .padding(top = auronScreenTopPadding(16.dp), bottom = 10.dp),
        ) {
            LogHeader(dayName = state.workoutName, startedAtMillis = state.startedAtMillis)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (insightVisible) {
                item(key = "insight") {
                    ProgressionInsightCard(onDismiss = { insightVisible = false })
                }
            }

            state.message?.let { message ->
                item(key = "message") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = logCardColor()),
                        border = logCardBorder(),
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
                    showNextExercisePrompt = exerciseState.exercise.id == state.nextExercisePromptId,
                    maxWeight = maxWeightByExerciseId[exerciseState.exercise.id],
                    onToggle = { viewModel.toggleExpanded(exerciseState.exercise.id) },
                    onNext = { viewModel.startRestBeforeNext(exerciseState.exercise.id) },
                    onAddSet = { viewModel.addSet(exerciseState.exercise.id) },
                    onContinueLastSet = { viewModel.continueLastSet(exerciseState.exercise.id) },
                    onSetOptions = { i ->
                        exerciseState.draftSets.getOrNull(i)?.let { draft ->
                            advancedSetSelection = AdvancedSetSelection(exerciseState.exercise.id, draft.researchRowKey)
                        }
                    },
                    onCommitSet = { i -> viewModel.commitSet(exerciseState.exercise.id, i) },
                    onWeight = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, weight = v.filterDecimal())
                    },
                    onReps = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, reps = v.filter(Char::isDigit))
                    },
                    onRir = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, rir = v.filter(Char::isDigit).take(1))
                    },
                    onDuration = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, duration = v.filter { it.isDigit() || it == ':' })
                    },
                    onDistance = { i, v ->
                        viewModel.updateSet(exerciseState.exercise.id, i, distance = v.filterDecimal())
                    },
                    // One exercise-level note, stored on the first draft set (mapped to the first valid set
                    // on save — Option B — so no LoggedSetEntity schema change is needed).
                    onExerciseNote = { v -> viewModel.updateSet(exerciseState.exercise.id, 0, notes = v) },
                    onInfo = { catalogDetail.openByName(exerciseState.exercise.name) },
                    onProgression = { progressionExerciseId = exerciseState.exercise.id },
                    imageRef = thumbnailRefs.thumbnailRefFor(exerciseState.exercise.name),
                    distanceUnitSystem = distanceUnitSystem,
                )
            }

            item(key = "footer") {
                SaveDiscardFooter(
                    canSave = state.canSave,
                    isSaving = state.isSaving,
                    onSave = {
                        if (state.canSave && !state.isSaving) {
                            showSaveSheet = true
                        }
                    },
                    onDiscard = { showDiscardDialog = true },
                )
            }
        }
    }

    progressionExerciseId?.let { exerciseId ->
        state.exercises.firstOrNull { it.exercise.id == exerciseId }?.let { exerciseState ->
            ProgressionHistorySheet(
                state = exerciseState,
                distanceUnitSystem = distanceUnitSystem,
                onNoteChanged = { viewModel.updateSet(exerciseId, 0, notes = it) },
                onDismiss = { progressionExerciseId = null },
            )
        }
    }

    advancedSetSelection?.let { selection ->
        val exerciseState = state.exercises.firstOrNull { it.exercise.id == selection.exerciseId }
        val setIndex = exerciseState?.draftSets?.indexOfFirst { it.researchRowKey == selection.setKey } ?: -1
        val draft = exerciseState?.draftSets?.getOrNull(setIndex)
        if (exerciseState != null && draft != null) {
            AdvancedSetSheet(
                exerciseName = exerciseState.exercise.name,
                setNumber = exerciseState.draftSets.take(setIndex + 1).count { it.continuationOfKey == null },
                draft = draft,
                onSetType = { viewModel.updateSetType(selection.exerciseId, setIndex, it) },
                onDistribution = { viewModel.updateWeightDistribution(selection.exerciseId, setIndex, it) },
                onContinue = {
                    viewModel.continueSet(selection.exerciseId, setIndex)
                    advancedSetSelection = null
                },
                onDelete = {
                    viewModel.removeSet(selection.exerciseId, setIndex)
                    advancedSetSelection = null
                },
                onDismiss = { advancedSetSelection = null },
            )
        }
    }

    if (state.restTimer.visible) {
        // LOG-V4B: the sheet now lives in ui/components and is shared with Daily Win. The Log call
        // site keeps its original green accent, 0.56f cap, smart toggle and Sec/Min entry.
        RestTimerSheet(
            state = state.restTimer,
            accent = Color(0xFF45D483),
            title = "Rest Timer",
            showSmartToggle = true,
            showDurationInput = true,
            showSkipContinue = state.restTimer.advanceAfterRest,
            maxHeightFraction = 0.56f,
            onSmartChanged = viewModel::setSmartRestEnabled,
            onInputChanged = viewModel::setRestTimerInput,
            onUnitChanged = viewModel::setRestTimerUnit,
            onReset = viewModel::resetRestTimer,
            onToggleRunning = viewModel::toggleRestTimerRunning,
            onFinish = viewModel::finishRestTimer,
            onDismiss = viewModel::dismissRestTimer,
        )
    }

    if (showSaveSheet) {
        SaveSummarySheet(
            exercises = state.exercises,
            startedAtMillis = state.startedAtMillis,
            distanceUnitSystem = distanceUnitSystem,
            canSave = state.canSave,
            isSaving = state.isSaving,
            // Confirm runs the existing save flow (snackbar + navigate + auto-advance) untouched,
            // now passing the readiness chosen in the sheet (default NORMAL).
            onConfirm = { readiness -> viewModel.saveWorkout(readiness) },
            // Cancel / swipe / back: just close the sheet — never saves, never clears the draft.
            onDismiss = { showSaveSheet = false },
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            // UI-1 Part F: neutral card surface (no purple tonal tint); clear destructive styling.
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            title = { Text("Discard workout?") },
            text = {
                Text(
                    "This clears the sets you've entered for ${state.workoutName}. " +
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
                    Text(
                        text = "Discard",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }

    CatalogDetailHost(catalogDetail)
}

/** AURON wordmark + workout day name + live session duration. */
@Composable
private fun LogHeader(dayName: String, startedAtMillis: Long) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "AURON",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
            )
            Text(
                text = "TRAINER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 6.sp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            DurationLabel(startedAtMillis = startedAtMillis)
        }
    }
}

/**
 * Live workout duration. Ticks once a second in its own composable so only this label recomposes,
 * never the heavy set list. Elapsed = now − session start; the start time is owned by the ViewModel
 * (persisted with the draft), so the value stays correct across screen-off / backgrounding / return.
 */
@Composable
private fun DurationLabel(startedAtMillis: Long) {
    val now by produceState(initialValue = System.currentTimeMillis(), startedAtMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = if (startedAtMillis > 0L) ((now - startedAtMillis) / 1000).coerceAtLeast(0) else 0L
    Text(
        text = "Duration: ${formatDuration(seconds)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds % 60
    val m = (totalSeconds / 60) % 60
    val h = totalSeconds / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/**
 * Dismissible session-level insight that explains why accurate logging matters and ties the screen to
 * the V3 progression engine. Static premium copy — no algorithm, no AI; the per-exercise progression
 * target (below) carries the real V3 suggestion.
 */
@Composable
private fun ProgressionInsightCard(onDismiss: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = logCardColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = logCardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Progression coaching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Log clean sets so AURON can fine-tune your next progression target.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.cactusforprogressoncoaching),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 60.dp, height = 96.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
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
    showNextExercisePrompt: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onAddSet: () -> Unit,
    onContinueLastSet: () -> Unit,
    onSetOptions: (Int) -> Unit,
    onCommitSet: (Int) -> Unit,
    onWeight: (Int, String) -> Unit,
    onReps: (Int, String) -> Unit,
    onRir: (Int, String) -> Unit,
    onDuration: (Int, String) -> Unit,
    onDistance: (Int, String) -> Unit,
    onExerciseNote: (String) -> Unit,
    onInfo: () -> Unit,
    onProgression: () -> Unit,
    imageRef: String?,
    distanceUnitSystem: DistanceUnitSystem,
    maxWeight: Double? = null,
) {
    val exercise = state.exercise
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Card(
        shape = logCardShape(),
        colors = CardDefaults.cardColors(containerColor = logCardColor()),
        // Flat (no Material blur shadow); logCardElevation() paints a hard OLED-black bottom-right
        // depth panel so repeated cards stay cheap but read as raised. Both states use the same card.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = logCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .logCardElevation(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Subtle, low-duration size animation only fires on tap (never during scroll).
                .animateContentSize(animationSpec = tween(durationMillis = 180))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 14.dp else 6.dp),
        ) {
            // Header (always present): thumbnail + name opens shared detail; chevron alone toggles.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onInfo),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExerciseThumbnail(imageRef = imageRef, exerciseName = exercise.name, size = 46.dp)
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                StatusBadge(completed = state.completedSetCount, target = exercise.targetSets)
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse ${exercise.name}" else "Expand ${exercise.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }

            if (expanded) {
                // Per-exercise progression insight at the top, derived from the existing V3 suggestion.
                ProgressionTarget(
                    title = state.suggestion.title,
                    suggestedWeight = state.suggestion.suggestedWeight,
                    onClick = onProgression,
                )

                ExerciseBasicInfoLine(
                    sets = exercise.targetSets,
                    reps = if (state.trackingType.showsReps) {
                        "${exercise.targetMinReps}-${exercise.targetMaxReps}"
                    } else {
                        null
                    },
                    trendLabel = state.trendLabel,
                )

                if (state.potentialPr) {
                    PotentialPrChip()
                }

                PreviousPerformance(
                    exerciseName = exercise.name,
                    previousSets = state.previousSets,
                    distanceUnitSystem = distanceUnitSystem,
                )

                // Compact set table. Rep-based types keep the original reps/weight/RIR layout exactly
                // (no regression); time/distance types render the inline unit fields they actually use.
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (state.trackingType.isRepBased) {
                        SetTableHeader()
                        var baseNumber = 0
                        state.draftSets.forEachIndexed { i, draft ->
                            if (draft.continuationOfKey == null) baseNumber += 1
                            HorizontalDivider(thickness = 1.dp, color = logDividerColor())
                            SetRow(
                                setNumber = if (draft.continuationOfKey == null) baseNumber else null,
                                draft = draft,
                                onReps = { onReps(i, it) },
                                onWeight = { onWeight(i, it) },
                                onRir = { onRir(i, it) },
                                onOptions = { onSetOptions(i) },
                                onCommit = { onCommitSet(i) },
                            )
                        }
                    } else {
                        var baseNumber = 0
                        state.draftSets.forEachIndexed { i, draft ->
                            if (draft.continuationOfKey == null) baseNumber += 1
                            HorizontalDivider(thickness = 1.dp, color = logDividerColor())
                            TrackingSetRow(
                                setNumber = if (draft.continuationOfKey == null) baseNumber else null,
                                draft = draft,
                                trackingType = state.trackingType,
                                distanceUnitSystem = distanceUnitSystem,
                                onWeight = { onWeight(i, it) },
                                onDuration = { onDuration(i, it) },
                                onDistance = { onDistance(i, it) },
                                onOptions = { onSetOptions(i) },
                                onCommit = { onCommitSet(i) },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onAddSet, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                        Text("Add Set", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    if (state.canContinueLastSet) {
                        TextButton(
                            onClick = onContinueLastSet,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                        ) {
                            Text("Continue last set", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (state.repeatedDropSetCaution) {
                    Text(
                        text = "Repeated drop sets can create high fatigue and make technique harder to maintain on compound lifts. Consider keeping advanced intensity techniques limited and prioritize clean reps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                // One exercise-level note (replaces the old per-set note fields).
                CompactField(
                    value = state.draftSets.firstOrNull()?.notes.orEmpty(),
                    onValueChange = onExerciseNote,
                    placeholder = "Exercise note (optional)",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Outlined.EditNote,
                )

                if (state.isComplete && hasNext) {
                    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
                    val pulse = if (showNextExercisePrompt && motionEnabled) {
                        val transition = rememberInfiniteTransition(label = "nextExercisePulse")
                        transition.animateFloat(
                            initialValue = 0.14f,
                            targetValue = 0.30f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                            label = "nextExercisePulseAlpha",
                        ).value
                    } else 0.14f
                    FilledTonalButton(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                        ),
                    ) {
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
                    text = SetDisplay.targetLabel(
                        targetSets = exercise.targetSets,
                        targetMinReps = exercise.targetMinReps,
                        targetMaxReps = exercise.targetMaxReps,
                        trackingType = state.trackingType,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (
                    state.completedSetCount > 0 ||
                    state.currentVolume > 0.0 ||
                    state.currentDurationSeconds > 0 ||
                    state.currentDistanceMeters > 0.0
                ) {
                    Text(
                        text = SetDisplay.summaryLine(
                            setCount = state.completedSetCount,
                            totalReps = state.currentReps,
                            totalVolume = state.currentVolume,
                            durationSeconds = state.currentDurationSeconds,
                            distanceMeters = state.currentDistanceMeters,
                            hasRepSets = state.trackingType.isRepBased && state.completedSetCount > 0,
                            system = distanceUnitSystem,
                            targetSets = exercise.targetSets,
                        ) + if (state.potentialPr) " · PR" else "",
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

/**
 * Single-row basic info summary, e.g. `3 sets | 12-15 reps | Previous 630kg volume`.
 *
 * Numeric runs use the bright on-surface colour while the word labels stay on the dimmed
 * variant colour, so the eye lands on the values first. [trendLabel] arrives already
 * formatted from the ViewModel ("Previous 630kg volume", "+40kg volume", "First session",
 * …), so only its numeric run is emphasised here.
 */
@Composable
private fun ExerciseBasicInfoLine(
    sets: Int,
    reps: String?,
    trendLabel: String,
    modifier: Modifier = Modifier,
) {
    val numberStyle = SpanStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
    val labelStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val numberPattern = remember { Regex("[-+~]?\\d[\\d.,]*") }

    val text = buildAnnotatedString {
        fun segment(value: String, label: String) {
            if (length > 0) append(" | ")
            withStyle(numberStyle) { append(value) }
            append(" ")
            withStyle(labelStyle) { append(label) }
        }

        segment(sets.toString(), "sets")
        if (!reps.isNullOrBlank()) segment(reps, "reps")

        if (trendLabel.isNotBlank()) {
            if (length > 0) append(" | ")
            var cursor = 0
            numberPattern.findAll(trendLabel).forEach { match ->
                if (match.range.first > cursor) {
                    withStyle(labelStyle) { append(trendLabel.substring(cursor, match.range.first)) }
                }
                withStyle(numberStyle) { append(match.value) }
                cursor = match.range.last + 1
            }
            if (cursor < trendLabel.length) {
                withStyle(labelStyle) { append(trendLabel.substring(cursor)) }
            }
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/**
 * Celebratory pill shown when the current draft is on track to beat the stored best.
 *
 * One gold accent is used for the trophy, the label and the border so the chip reads as a
 * reward rather than another neutral metadata tag. Pure presentation — no engine, save or
 * draft behaviour is involved.
 */
@Composable
private fun PotentialPrChip(modifier: Modifier = Modifier) {
    val accent = Color(0xFFF2C86B)
    val pillShape = RoundedCornerShape(50)
    val contentBorderWidth = 1.dp
    Box(
        modifier = modifier
            // Wrap with a sweep-on-visible gold border. The sweep plays once when the chip first
            // enters the window and never repeats — pure decoration, no behaviour change.
            .clip(pillShape),
    ) {
        GoldSweepBorder(
            cornerRadius = 50.dp,
            borderColor = accent,
            sweepColor = Color(0xFFFFE7A8),
            borderWidth = contentBorderWidth,
            sweepArcDegrees = 110f,
            sweepDurationMillis = 1400,
        ) {
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(accent.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PrTrophyLottie(
                    size = 16.dp,
                )
                Text(
                    text = "Potential PR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Compact, top-of-card progression hint built from the existing OverloadSuggestion (no new logic).
 *
 * DB-2C Part B (display-only): the existing V3 suggestion title is mapped to an icon + accent so the
 * action reads at a glance — first (number "1", purple), increase (up, green), reduce (down, red),
 * hold/verify/build (flat, neutral gray). A subtle gray outline replaces the all-purple styling. None
 * of this touches the engine, the suggested weight, or any input/save behavior.
 */
@Composable
private fun ProgressionTarget(title: String, suggestedWeight: Double?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val normalized = title.lowercase()
    val (icon, accent) = when {
        normalized.contains("first") ->
            Icons.Outlined.LooksOne to colors.primary
        normalized.contains("increase") ->
            Icons.AutoMirrored.Outlined.TrendingUp to colors.tertiary
        normalized.contains("reduce") || normalized.contains("recover") || normalized.contains("decrease") ->
            Icons.AutoMirrored.Outlined.TrendingDown to colors.error
        else ->
            // hold / hold & verify / maintain / build reps → neutral
            Icons.AutoMirrored.Outlined.TrendingFlat to colors.onSurfaceVariant
    }
    Surface(
        color = logInsetColor(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colors.onSurfaceVariant.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "PROGRESSION TARGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
            suggestedWeight?.let { weight ->
                Text(
                    text = "${ProgressiveOverloadEngine.formatWeight(weight)}kg",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun SaveDiscardFooter(
    canSave: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuronWhiteButton(
            text = if (isSaving) "Saving" else "Save workout",
            onClick = onSave,
            enabled = canSave && !isSaving,
            icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
        )
        TextButton(
            onClick = onDiscard,
            enabled = !isSaving,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            ),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Discard workout", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun AuronWhiteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(999.dp),
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF111218),
            disabledContainerColor = Color.White.copy(alpha = 0.32f),
            disabledContentColor = Color(0xFF111218).copy(alpha = 0.56f),
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Black)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviousPerformance(
    exerciseName: String,
    previousSets: List<LoggedSetEntity>,
    distanceUnitSystem: DistanceUnitSystem,
) {
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
                    StatChip(formatPreviousSet(exerciseName, set, distanceUnitSystem))
                }
            }
        }
    }
}

/**
 * Exercise-name keywords that are normally performed at bodyweight. Display-only heuristic: when a
 * "Previous" set for one of these has weight 0, we show "BW" instead of "0kg" so a bodyweight pull-up
 * reads clearly. This is a local name match only — no DB field, no schema change — and it never
 * touches the stored weight, volume math, PR detection, or progressive-overload suggestions.
 */
private val BODYWEIGHT_EXERCISE_KEYWORDS = listOf(
    "pull-up", "pull up", "pullup",
    "chin-up", "chin up", "chinup",
    "push-up", "push up", "pushup",
    "muscle-up", "muscle up", "muscleup",
    "dip",
    "plank",
)

private fun isBodyweightLike(exerciseName: String): Boolean {
    val normalized = exerciseName.lowercase()
    return BODYWEIGHT_EXERCISE_KEYWORDS.any { normalized.contains(it) }
}

/** "BW x 12 @0" for a bodyweight-like set logged at 0kg, otherwise "60kg x 12 @0". */
private fun formatPreviousSet(
    exerciseName: String,
    set: LoggedSetEntity,
    distanceUnitSystem: DistanceUnitSystem,
): String {
    if (SetDisplay.isNonRep(set)) {
        return SetDisplay.nonRepInline(set, distanceUnitSystem) +
            if (set.durationSeconds != null) " duration" else ""
    }
    val weightLabel = if (set.weight == 0.0 && isBodyweightLike(exerciseName)) {
        "BW"
    } else {
        "${ProgressiveOverloadEngine.formatWeight(set.weight)}kg"
    }
    return "$weightLabel x ${set.reps} @${set.rir}"
}

/** Column labels for the compact set table, aligned with [SetRow] (index + 3 fields + overflow). */
@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(30.dp))
        SetHeaderLabel("REPS", Modifier.weight(1f))
        SetHeaderLabel("KG", Modifier.weight(1f))
        SetHeaderLabel("RIR", Modifier.weight(1f))
        Box(modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun SetHeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        letterSpacing = 1.sp,
    )
}

/**
 * DB-6C inline value+unit input: a compact field with its **bold** unit rendered right beside it (never
 * a detached column). Empty [unit] (duration) shows no suffix. Shared by every non-rep tracking field so
 * the inline-unit style is consistent and future units (km/h, m/s) drop in with no layout change.
 */
@Composable
private fun UnitInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    unit: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onFocusLost: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CompactField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            keyboardType = keyboardType,
            modifier = Modifier.weight(1f),
            centered = true,
            flat = true,
            onFocusLost = onFocusLost,
            imeAction = imeAction,
            keyboardActions = keyboardActions,
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One non-rep tracking field (load / distance / time) rendered as an inline value + bold unit. */
private data class TrackingFieldSpec(
    val value: String,
    val onValueChange: (String) -> Unit,
    val placeholder: String,
    val unit: String,
    val keyboardType: KeyboardType,
    val onFocusLost: (() -> Unit)? = null,
    val imeAction: ImeAction = ImeAction.Default,
    val keyboardActions: KeyboardActions = KeyboardActions(),
)

/**
 * DB-6C/DB-6F: one set row for a non-rep [trackingType] — renders only the fields the type uses (load /
 * distance / time) with inline units, a set index, and the same overflow ⋮ delete menu the rep rows
 * use. Irrelevant fields are never shown, so there are no dead controls and no `reps = 0` leaks. The
 * fields are arranged by [TrackingFieldsRow] so they reflow to two lines on narrow screens and a
 * label/word can never crush into a vertical letter stack.
 */
@Composable
private fun TrackingSetRow(
    setNumber: Int?,
    draft: SetDraft,
    trackingType: TrackingType,
    distanceUnitSystem: DistanceUnitSystem,
    onWeight: (String) -> Unit,
    onDuration: (String) -> Unit,
    onDistance: (String) -> Unit,
    onOptions: () -> Unit,
    onCommit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val rawFields = buildList {
        if (trackingType.showsLoad) {
            add(
                TrackingFieldSpec(
                    value = draft.weight,
                    onValueChange = onWeight,
                    placeholder = "Load",
                    unit = "kg",
                    keyboardType = KeyboardType.Decimal,
                ),
            )
        }
        if (trackingType.showsDistance) {
            add(
                TrackingFieldSpec(
                    value = draft.distance,
                    onValueChange = onDistance,
                    placeholder = "Dist",
                    unit = MeasurementFormatter.shortDistanceUnit(distanceUnitSystem),
                    keyboardType = KeyboardType.Decimal,
                ),
            )
        }
        if (trackingType.showsDuration) {
            add(
                TrackingFieldSpec(
                    value = draft.duration,
                    onValueChange = onDuration,
                    placeholder = "Time",
                    unit = "mm:ss",
                    // Keep Text so "1:30" colon entry still works (DB-6C.2); digits auto-format on blur.
                    keyboardType = KeyboardType.Text,
                    onFocusLost = {
                        MeasurementFormatter.normalizeDurationInput(draft.duration)?.let(onDuration)
                    },
                ),
            )
        }
    }
    val fields = rawFields.mapIndexed { index, field ->
        if (index != rawFields.lastIndex) field
        else field.copy(
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                onCommit()
            }),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetIndexBadge(setNumber)
        TrackingFieldsRow(fields = fields, modifier = Modifier.weight(1f))
        SetTypeBadge(draft.setType)
        SetRowMenu(setNumber = setNumber, onClick = onOptions)
    }
}

/**
 * Responsive container for the inline tracking fields: one row when every field can keep enough width
 * to show its label + inline unit, otherwise a two-per-line grid. Only the 3-field TIME_DISTANCE_LOAD
 * reflows on a normal phone; 1–2 field types always fit. Centralizing the arrangement here keeps the
 * layout rule from drifting per tracking type (TIME_ONLY / TIME_LOAD / DISTANCE_LOAD / TIME_DISTANCE /
 * TIME_DISTANCE_LOAD all flow through this one composable).
 */
@Composable
private fun TrackingFieldsRow(
    fields: List<TrackingFieldSpec>,
    modifier: Modifier = Modifier,
) {
    if (fields.isEmpty()) return
    BoxWithConstraints(modifier) {
        // Each inline field needs ~104dp to show its value + bold unit without crushing. If they can't
        // all get that on one line, fall back to a two-per-line grid (no vertical letter stacking).
        val perFieldMin = 104.dp
        val gaps = 8.dp * (fields.size - 1).coerceAtLeast(0)
        val oneLineFits = maxWidth >= perFieldMin * fields.size + gaps
        if (oneLineFits) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEach { TrackingFieldCell(it, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.chunked(2).forEach { rowFields ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowFields.forEach { TrackingFieldCell(it, Modifier.weight(1f)) }
                        // Pad a trailing odd cell so widths stay aligned with the row above.
                        if (rowFields.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingFieldCell(field: TrackingFieldSpec, modifier: Modifier = Modifier) {
    UnitInputField(
        value = field.value,
        onValueChange = field.onValueChange,
        placeholder = field.placeholder,
        unit = field.unit,
        keyboardType = field.keyboardType,
        modifier = modifier,
        onFocusLost = field.onFocusLost,
        imeAction = field.imeAction,
        keyboardActions = field.keyboardActions,
    )
}

/** Circular set-number badge shared by the rep and non-rep set rows. */
@Composable
private fun SetIndexBadge(setNumber: Int?) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (setNumber == null) {
            Icon(
                imageVector = Icons.Outlined.SubdirectoryArrowRight,
                contentDescription = "Continuation set",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                text = setNumber.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Overflow (⋮) menu whose only action is Delete set — shared by the rep and non-rep set rows so the
 * delete affordance is identical and hard to mis-tap (no always-visible ✕ button). (DB-6F PART C.)
 */
@Composable
private fun SetRowMenu(setNumber: Int?, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = setNumber?.let { "Set $it options" } ?: "Continuation set options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SetTypeBadge(type: LogSetType) {
    val label = when (type) {
        LogSetType.STRAIGHT -> null
        LogSetType.WARMUP -> "W"
        LogSetType.DROP -> "D"
    } ?: return
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
    }
}

/**
 * One compact set row: index badge, centered reps / kg / RIR fields, and an overflow (⋮) menu whose
 * only action is Delete set. No always-visible trash icon, so accidental deletes are far less likely.
 */
@Composable
private fun SetRow(
    setNumber: Int?,
    draft: SetDraft,
    onReps: (String) -> Unit,
    onWeight: (String) -> Unit,
    onRir: (String) -> Unit,
    onOptions: () -> Unit,
    onCommit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetIndexBadge(setNumber)
        CompactField(
            value = draft.reps,
            onValueChange = onReps,
            placeholder = "Reps",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            centered = true,
            flat = true,
            imeAction = ImeAction.Next,
        )
        CompactField(
            value = draft.weight,
            onValueChange = onWeight,
            placeholder = "Kg",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
            centered = true,
            flat = true,
            imeAction = ImeAction.Next,
        )
        CompactField(
            value = draft.rir,
            onValueChange = onRir,
            placeholder = "RIR",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            centered = true,
            flat = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                onCommit()
            }),
        )
        SetTypeBadge(draft.setType)
        SetRowMenu(setNumber = setNumber, onClick = onOptions)
    }
}

/**
 * Lightweight text field used for the repeated set inputs. Uses [BasicTextField] with a static
 * surfaceVariant box instead of OutlinedTextField, so there is no animated border/label work on
 * focus — this keeps scrolling and field focus smooth on real devices when many fields are visible.
 */
@Composable
internal fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    centered: Boolean = false,
    // flat = borderless, transparent set-row input integrated into the row; otherwise a recessed well.
    flat: Boolean = false,
    onFocusLost: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
) {
    val colors = MaterialTheme.colorScheme
    val fieldShape = MaterialTheme.shapes.medium
    val containerColor = if (flat) Color.Transparent else logInsetColor()
    var wasFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = colors.onSurface,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        ),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        modifier = modifier.onFocusChanged { state ->
            if (wasFocused && !state.isFocused) {
                onFocusLost?.invoke()
            }
            wasFocused = state.isFocused
        },
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .clip(fieldShape)
                .background(containerColor)
                .padding(horizontal = if (flat) 6.dp else 14.dp, vertical = 14.dp),
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
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        modifier = if (centered) Modifier.fillMaxWidth() else Modifier,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                        // The placeholder must never wrap into a vertical letter stack on narrow fields
                        // (DB-6F). singleLine on the BasicTextField only governs typed text, not this.
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                }
                innerTextField()
            }
        }
    }
}

@Composable
private fun ProgressionHistorySheet(
    state: ExerciseLogState,
    distanceUnitSystem: DistanceUnitSystem,
    onNoteChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val exercise = state.exercise
    val historyBySession = state.history.groupBy { it.sessionId }
    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.78f,
        applyImePadding = true,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        fixedFooter = {
            AuronWhiteButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.height(64.dp),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(
                SetDisplay.targetLabel(
                    targetSets = exercise.targetSets,
                    targetMinReps = exercise.targetMinReps,
                    targetMaxReps = exercise.targetMaxReps,
                    trackingType = state.trackingType,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = logCardColor()),
            border = logCardBorder(),
            shape = logCardShape(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null)
                    Text(state.suggestion.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                // Engine wording is rendered verbatim; this sheet performs no recommendation logic.
                Text(
                    state.suggestion.detail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.suggestion.suggestedWeight?.let { weight ->
                    StatChip("Suggested ${ProgressiveOverloadEngine.formatWeight(weight)}kg")
                }
            }
        }
        Text("Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        CompactField(
            value = state.draftSets.firstOrNull()?.notes.orEmpty(),
            onValueChange = onNoteChanged,
            placeholder = "Add a note for your next session",
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.EditNote,
        )
        Text("Exercise History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        if (historyBySession.isEmpty()) {
            Text("No history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            historyBySession.values.forEach { sessionSets ->
                ExerciseHistoryReceipt(sessionSets, distanceUnitSystem)
            }
        }
    }
}

@Composable
private fun ExerciseHistoryReceipt(sets: List<ExerciseHistoryItem>, distanceUnitSystem: DistanceUnitSystem) {
    val first = sets.first()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "${first.dateEpochDay.toLongDate()} · ${first.dayNameSnapshot}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        ExerciseHistoryTableRow(set = "SET", volume = "VOLUME", reps = "REPS", header = true)
        sets.forEach { set ->
            val nonRep = SetDisplay.isNonRep(set.durationSeconds, set.distanceMeters)
            val measurements = if (nonRep) {
                SetDisplay.nonRepMeasurements(
                    durationSeconds = set.durationSeconds,
                    distanceMeters = set.distanceMeters,
                    load = set.weight,
                    system = distanceUnitSystem,
                )
            } else {
                emptyList()
            }
            ExerciseHistoryTableRow(
                set = "Set ${set.setNumber}",
                volume = if (nonRep) measurements.firstOrNull()?.inline() ?: "—"
                else "${ProgressiveOverloadEngine.formatWeight(set.weight)} kg",
                reps = if (nonRep) measurements.drop(1).joinToString(" · ") { it.inline() }.ifBlank { "—" }
                else "${set.reps} reps · RIR ${set.rir}",
                isPr = set.isPrWeight || set.isPrReps,
            )
        }
    }
}

@Composable
private fun ExerciseHistoryTableRow(
    set: String,
    volume: String,
    reps: String,
    header: Boolean = false,
    isPr: Boolean = false,
) {
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    if (isPr) {
        // PR row gets its own treatment: gold border with a one-shot sweep on first visibility,
        // gold-tinted background, and the trophy Lottie anchored on the left. The rest of the row
        // reuses the same Set / Volume / Reps columns so non-PR rows and PR rows stay aligned.
        GoldSweepBorder(
            cornerRadius = 12.dp,
            borderColor = Color(0xFFF2C86B),
            sweepColor = Color(0xFFFFE7A8),
            borderWidth = 1.dp,
            sweepArcDegrees = 110f,
            sweepDurationMillis = 1500,
            innerPadding = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2C86B).copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrTrophyLottie(
                    size = 20.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(set, Modifier.weight(0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = style, color = color)
                Text(
                    volume,
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = style,
                    color = color,
                    textAlign = TextAlign.End,
                )
                Row(
                    Modifier.weight(1.45f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        reps,
                        Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = style,
                        color = color,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (header) Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF20242B))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                else Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(set, Modifier.weight(0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = style, color = color)
        Text(
            volume,
            Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = style,
            color = color,
            textAlign = TextAlign.End,
        )
        Row(
            Modifier.weight(1.45f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // fill = false + single line keeps the row intact even when the measurement string
            // is long (time + distance + load all render inline here).
            Text(
                reps,
                Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = style,
                color = color,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun AdvancedSetSheet(
    exerciseName: String,
    setNumber: Int,
    draft: SetDraft,
    onSetType: (LogSetType) -> Unit,
    onDistribution: (LogWeightDistribution) -> Unit,
    onContinue: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val continuation = draft.continuationOfKey != null
    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.68f,
        // The two actions stay pinned outside the scroll region so a short screen can never
        // render the sheet looking "finished" while the exit is off-screen.
        fixedFooter = {
            if (!continuation) {
                AuronWhiteButton(
                    text = "Continue this set",
                    onClick = onContinue,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(64.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete Set", fontWeight = FontWeight.Black)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                if (continuation) "Continuation of Set #$setNumber" else "Set #$setNumber",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Set Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        SegmentedChoices(
            choices = listOf("Straight Set", "Warmup Set", "Dropset"),
            selectedIndex = draft.setType.ordinal,
            enabled = !continuation,
            onSelected = { onSetType(LogSetType.values()[it]) },
        )
        Text(setTypeDescription(draft.setType), style = MaterialTheme.typography.bodyLarge)
        Text("Weight Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        SegmentedChoices(
            choices = listOf("Total", "Each Side", "Left Side", "Right Side"),
            selectedIndex = draft.weightDistribution.ordinal,
            enabled = !continuation,
            onSelected = { onDistribution(LogWeightDistribution.values()[it]) },
        )
        Text(weightDistributionDescription(draft.weightDistribution), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun setTypeDescription(type: LogSetType): String = when (type) {
    LogSetType.STRAIGHT -> "A straight set uses the same working load for a planned number of reps, followed by rest before the next set."
    LogSetType.WARMUP -> "A warm-up set is a lighter set performed before your main work to prepare for heavier lifting."
    LogSetType.DROP -> "A dropset continues the exercise after reducing load with little or no rest."
}

private fun weightDistributionDescription(distribution: LogWeightDistribution): String = when (distribution) {
    LogWeightDistribution.TOTAL -> "The total load moved, including both sides of the implement."
    LogWeightDistribution.EACH_SIDE -> "The entered value represents the load used on each side."
    LogWeightDistribution.LEFT_SIDE -> "The entered value represents the left-side load."
    LogWeightDistribution.RIGHT_SIDE -> "The entered value represents the right-side load."
}

@Composable
internal fun SegmentedChoices(
    choices: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B1E23))
            .padding(4.dp),
    ) {
        choices.forEachIndexed { index, choice ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) Color.White.copy(alpha = if (enabled) 1f else 0.62f) else Color.Transparent)
                    .clickable(enabled = enabled) { onSelected(index) }
                    .padding(horizontal = 5.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    choice,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color(0xFF111218) else MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.42f),
                )
            }
        }
    }
}

// ---- Save Workout summary bottom sheet ------------------------------------------------------------

private enum class SummaryStatus { COMPLETED, PARTIAL, SKIPPED }

private data class ExerciseSummary(
    val name: String,
    val validSets: Int,
    val targetSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val hasRepSets: Boolean,
    val status: SummaryStatus,
)

/**
 * Per-exercise review numbers, using the exact same "valid set" rule the ViewModel uses to build the
 * save inputs (weight & reps & RIR all parse, weight >= 0, reps > 0). No new save logic — this only
 * reflects what will be saved. Status: Completed (>= target valid sets), Partial (1..<target),
 * Skipped (0).
 */
private fun summarizeExercise(
    state: ExerciseLogState,
    distanceUnitSystem: DistanceUnitSystem,
): ExerciseSummary {
    val parsedSets = state.draftSets.mapNotNull {
        parseSetDraftInput(
            draft = it,
            type = state.trackingType,
            distanceUnitSystem = distanceUnitSystem,
            notes = null,
        )
    }
    val valid = parsedSets.size
    val hasRepSets = state.trackingType.isRepBased && valid > 0
    val reps = parsedSets.sumOf { it.reps }
    val volume = parsedSets
        .filter { it.durationSeconds == null && it.distanceMeters == null }
        .sumOf { it.weight * it.reps }
    val durationSeconds = parsedSets.sumOf { it.durationSeconds ?: 0 }
    val distanceMeters = parsedSets.sumOf { it.distanceMeters ?: 0.0 }
    val target = state.exercise.targetSets
    val status = when {
        valid == 0 -> SummaryStatus.SKIPPED
        valid >= target -> SummaryStatus.COMPLETED
        else -> SummaryStatus.PARTIAL
    }
    return ExerciseSummary(
        name = state.exercise.name,
        validSets = valid,
        targetSets = target,
        totalReps = reps,
        totalVolume = volume,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        hasRepSets = hasRepSets,
        status = status,
    )
}

/**
 * V3.6A readiness selector for the review sheet: "How did today feel?" with Tired/Normal/Strong.
 * Preselected NORMAL, fully optional — it only changes local state and never gates the Save button.
 */
@Composable
private fun ReadinessSelector(selected: Readiness, onSelect: (Readiness) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "How did today feel?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Readiness.entries.forEach { option ->
                val label = when (option) {
                    Readiness.TIRED -> "Tired"
                    Readiness.NORMAL -> "Normal"
                    Readiness.STRONG -> "Strong"
                }
                ReadinessPill(
                    label = label,
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReadinessPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else logInsetColor()
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

/**
 * Premium half-screen review sheet shown before saving. It never saves on its own — only the explicit
 * "Save workout" button calls [onConfirm] (the existing save flow). Dismissing only closes the sheet.
 */
@Composable
private fun SaveSummarySheet(
    exercises: List<ExerciseLogState>,
    startedAtMillis: Long,
    distanceUnitSystem: DistanceUnitSystem,
    canSave: Boolean,
    isSaving: Boolean,
    onConfirm: (Readiness) -> Unit,
    onDismiss: () -> Unit,
) {
    val summaries = exercises.map { summarizeExercise(it, distanceUnitSystem) }
    // V3.6A: subjective readiness, preselected NORMAL. Non-blocking — Save works whatever is chosen.
    var readiness by remember { mutableStateOf(Readiness.NORMAL) }
    val totalSets = summaries.sumOf { it.validSets }
    val totalReps = summaries.sumOf { it.totalReps }
    val totalVolume = summaries.sumOf { it.totalVolume }
    val totalDurationSeconds = summaries.sumOf { it.durationSeconds }
    val totalDistanceMeters = summaries.sumOf { it.distanceMeters }
    val durationSeconds =
        if (startedAtMillis > 0L) ((System.currentTimeMillis() - startedAtMillis) / 1000).coerceAtLeast(0) else 0L

    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.78f,
        // Save + Cancel sit pinned at the bottom so the long metric grid scrolls above and the
        // primary action is never the thing that gets pushed off-screen. Mirrors the Continue /
        // Delete pattern in AdvancedSetSheet.
        fixedFooter = {
            AuronWhiteButton(
                text = if (isSaving) "Saving" else "Save workout",
                onClick = { onConfirm(readiness) },
                enabled = canSave && !isSaving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(64.dp),
                icon = { Icon(Icons.Outlined.Save, contentDescription = null) },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text("Cancel", fontWeight = FontWeight.Black)
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Review workout",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Check your sets before saving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (startedAtMillis > 0L) {
                Text(
                    text = "Duration: ${formatDuration(durationSeconds)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SummaryMetricsGrid(
            totalSets = totalSets,
            totalReps = totalReps,
            totalVolume = totalVolume,
            totalDurationSeconds = totalDurationSeconds,
            totalDistanceMeters = totalDistanceMeters,
            hasRepSets = summaries.any { it.hasRepSets },
            distanceUnitSystem = distanceUnitSystem,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            summaries.forEachIndexed { i, summary ->
                if (i > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                    )
                }
                SummaryExerciseRow(summary, distanceUnitSystem)
            }
        }

        ReadinessSelector(selected = readiness, onSelect = { readiness = it })
    }
}

private data class SummaryMetricItem(val label: String, val value: String)

@Composable
private fun SummaryMetricsGrid(
    totalSets: Int,
    totalReps: Int,
    totalVolume: Double,
    totalDurationSeconds: Int,
    totalDistanceMeters: Double,
    hasRepSets: Boolean,
    distanceUnitSystem: DistanceUnitSystem,
) {
    val metrics = buildList {
        add(SummaryMetricItem("Sets", totalSets.toString()))
        if (hasRepSets) {
            add(SummaryMetricItem("Reps", totalReps.toString()))
            add(SummaryMetricItem("Volume", "${ProgressiveOverloadEngine.formatWeight(totalVolume)} kg"))
        }
        if (totalDurationSeconds > 0) {
            add(SummaryMetricItem("Duration", MeasurementFormatter.duration(totalDurationSeconds).inline()))
        }
        if (totalDistanceMeters > 0.0) {
            add(SummaryMetricItem("Distance", MeasurementFormatter.distance(totalDistanceMeters, distanceUnitSystem).inline()))
        }
    }

    if (metrics.size == 3 && totalDurationSeconds == 0 && totalDistanceMeters == 0.0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            metrics.forEach { metric ->
                SummaryMetric(metric.label, metric.value, Modifier.weight(1f))
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowMetrics.forEach { metric ->
                    SummaryMetric(metric.label, metric.value, Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(logInsetColor())
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun SummaryExerciseRow(
    summary: ExerciseSummary,
    distanceUnitSystem: DistanceUnitSystem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = SetDisplay.summaryLine(
                    setCount = summary.validSets,
                    totalReps = summary.totalReps,
                    totalVolume = summary.totalVolume,
                    durationSeconds = summary.durationSeconds,
                    distanceMeters = summary.distanceMeters,
                    hasRepSets = summary.hasRepSets,
                    system = distanceUnitSystem,
                    targetSets = summary.targetSets,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusIcon(summary.status)
    }
}

@Composable
private fun StatusIcon(status: SummaryStatus) {
    val (icon, tint, desc) = when (status) {
        SummaryStatus.COMPLETED -> Triple(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.tertiary, "Completed")
        SummaryStatus.PARTIAL -> Triple(Icons.Outlined.Schedule, MaterialTheme.colorScheme.secondary, "Partial")
        SummaryStatus.SKIPPED -> Triple(Icons.Outlined.Cancel, MaterialTheme.colorScheme.error, "Skipped")
    }
    Icon(imageVector = icon, contentDescription = desc, tint = tint, modifier = Modifier.size(24.dp))
}

// ---- Local Log Workout visual tokens (dark-first; light mode keeps the standard surfaces) ----------
// Dark mode uses neutral charcoal cards over the OLED-black background with darker recessed "wells"
// for insets, so the screen reads flatter and less blue/bright than the shared surface tokens.

/** Main card fill (exercise cards, insight card, message) over the shared OLED/plum canvas. */
@Composable
private fun logCardColor(): Color =
    if (isAuronDarkTheme()) Color(0xFF0D0E11) else MaterialTheme.colorScheme.surface

/** Recessed inset fill, darker than the card (progression target, exercise note). */
@Composable
private fun logInsetColor(): Color =
    if (isAuronDarkTheme()) Color(0xFF111218) else MaterialTheme.colorScheme.surfaceVariant

/** Opaque plum header so scrolling exercise rows pass underneath without reducing legibility. */
@Composable
private fun logStickyHeaderColor(): Color =
    if (isAuronDarkTheme()) Color(0xFF19051F) else MaterialTheme.colorScheme.background

/** Slightly more visible row divider for scanning set rows, without going harsh. */
@Composable
private fun logDividerColor(): Color =
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isAuronDarkTheme()) 0.18f else 0.22f)

/** Subtle top/side card outline — a touch more visible than a hairline so cards read as defined. */
@Composable
private fun logCardBorder(): BorderStroke =
    gymContainerBorder(alpha = if (isAuronDarkTheme()) 0.08f else 0.10f)

/** Slightly rectangular shape for Log exercise cards — cleaner depth edges than a large radius. */
private fun logCardShape() = RoundedCornerShape(14.dp)

/**
 * OLED-style depth for Log exercise cards (DB-2E Part C). Paints a hard, blur-free OLED-black panel
 * offset to the bottom-right behind the card, using the same corner radius so it hugs the rounded
 * corners cleanly (no edge artifacts). The card surface covers the top-left; the panel peeks out on
 * the bottom and right, reading as a raised/floating card. Dark mode only; light mode is unchanged.
 */
@Composable
private fun Modifier.logCardElevation(): Modifier {
    if (!isAuronDarkTheme()) return this
    return drawWithContent {
        val radius = 14.dp.toPx()
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(x = 3.dp.toPx(), y = 4.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
        )
        drawContent()
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
