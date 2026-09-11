package com.hoctin.gymtracker.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.ui.catalog.CatalogExerciseDetailSheet
import com.hoctin.gymtracker.ui.catalog.enumLabel
import com.hoctin.gymtracker.ui.components.AuronCompactModalSheet
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SavedWorkoutBuilderScreen(
    viewModel: SavedWorkoutBuilderViewModel,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is SavedWorkoutBuilderEvent.Saved) onSaved(event.id)
        }
    }

    val listState = rememberLazyListState()
    val filtered = remember(state.allExercises, state.query, state.muscle) {
        filterBuilderExercises(state.allExercises, state.query, state.muscle)
    }
    val selectedKeys = remember(state.selected) { state.selected.mapTo(hashSetOf()) { it.key } }
    var detailExercise by remember { mutableStateOf<CatalogExercise?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 150f + listState.firstVisibleItemScrollOffset.toFloat()
            }
            .planV3PageEntrance(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BuilderHeader(onBack)
                BuilderTabs(state.tab, viewModel::setTab)
                if (!state.loading && state.tab == BuilderTab.ALL) {
                    BuilderSearch(state.query, viewModel::setQuery)
                    MuscleFilters(state.muscle, viewModel::setMuscle)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 126.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.loading) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.tab == BuilderTab.ALL) {
                    if (filtered.isEmpty()) {
                        item(key = "empty-results") {
                            Text(
                                "No exercises match this search.",
                                modifier = Modifier.padding(vertical = 30.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(filtered, key = CatalogExercise::slug) { exercise ->
                            CatalogBuilderRow(
                                exercise = exercise,
                                selected = "catalog:${exercise.slug}" in selectedKeys,
                                onClick = { viewModel.toggle(exercise) },
                                onThumbnailClick = { detailExercise = exercise },
                            )
                        }
                    }
                } else {
                    item(key = "selected-title") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Selected Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(
                                "Hold and drag exercises to reorder your workout",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    item(key = "selected-list") {
                        if (state.selected.isEmpty()) {
                            Text(
                                "Select exercises from ALL to build this workout.",
                                modifier = Modifier.padding(vertical = 30.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            BuilderSelectedList(
                                items = state.selected,
                                onRemove = viewModel::remove,
                                onReordered = viewModel::reorder,
                            )
                        }
                    }
                }
            }
        }

        WhiteBottomCta(
            text = if (state.selected.isEmpty()) "Select exercises to continue" else "Reorder & Add these exercises",
            onClick = viewModel::showReview,
            enabled = state.selected.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    detailExercise?.let { exercise ->
        CatalogExerciseDetailSheet(exercise = exercise, onDismiss = { detailExercise = null })
    }
    if (state.stage == BuilderStage.REVIEW) {
        SaveWorkoutReview(
            state = state,
            onNameChanged = viewModel::setName,
            onRemove = viewModel::remove,
            onReordered = viewModel::reorder,
            onClose = viewModel::closeReview,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun BuilderHeader(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Text("Build a Workout", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BuilderTabs(selected: BuilderTab, onSelected: (BuilderTab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        BuilderTab.entries.forEach { tab ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelected(tab) }
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected == tab) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (selected == tab) Color.White else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun BuilderSearch(query: String, onQuery: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search for an exercise") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotBlank()) {
            { Icon(Icons.Outlined.Close, contentDescription = "Clear search", modifier = Modifier.clickable { onQuery("") }) }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = planV3Surface(),
            unfocusedContainerColor = planV3Surface(),
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
    )
}

@Composable
private fun MuscleFilters(selected: MuscleGroup?, onSelected: (MuscleGroup?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipLabel("All", selected == null) { onSelected(null) }
        builderFilterMuscles().forEach { muscle ->
            FilterChipLabel(enumLabel(muscle.name), selected == muscle) { onSelected(muscle) }
        }
    }
}

@Composable
private fun FilterChipLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Color.White else planV3Surface())
            .border(gymContainerBorder(alpha = 0.08f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
    ) {
        Text(label, color = if (selected) Color(0xFF111015) else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CatalogBuilderRow(
    exercise: CatalogExercise,
    selected: Boolean,
    onClick: () -> Unit,
    onThumbnailClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onThumbnailClick),
        ) {
            ExerciseThumbnail(
                imageRef = exercise.firstThumbnailRef(),
                exerciseName = exercise.name,
                size = 68.dp,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(exercise.metadataLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(exercise.muscleTags(4), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                .background(if (selected) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = Color(0xFF111015), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BuilderSelectedList(
    items: List<BuilderExercise>,
    onRemove: (String) -> Unit,
    onReordered: (List<BuilderExercise>) -> Unit,
    compact: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val local = remember(items) { mutableStateListOf<BuilderExercise>().apply { addAll(items) } }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val gapPx = with(LocalDensity.current) { (if (compact) 6.dp else 12.dp).toPx() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
        local.forEach { item ->
            key(item.key) {
                val dragging = draggedKey == item.key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) offset else 0f
                            scaleX = if (dragging) 1.015f else 1f
                            scaleY = if (dragging) 1.015f else 1f
                            shadowElevation = if (dragging) 14.dp.toPx() else 0f
                            shape = RoundedCornerShape(16.dp)
                        }
                        .onGloballyPositioned { heights[item.key] = it.size.height }
                        .pointerInput(items) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedKey = item.key
                                    offset = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = { draggedKey = null; offset = 0f },
                                onDragEnd = { draggedKey = null; offset = 0f; onReordered(local.toList()) },
                                onDrag = { change, amount ->
                                    change.consume()
                                    offset += amount.y
                                    val index = local.indexOfFirst { it.key == draggedKey }
                                    if (index in 0 until local.lastIndex && offset > 0f) {
                                        val next = heights[local[index + 1].key] ?: 0
                                        if (next > 0 && offset > next / 2f) {
                                            local.add(index + 1, local.removeAt(index))
                                            offset -= next + gapPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    } else if (index > 0 && offset < 0f) {
                                        val previous = heights[local[index - 1].key] ?: 0
                                        if (previous > 0 && offset < -previous / 2f) {
                                            local.add(index - 1, local.removeAt(index))
                                            offset += previous + gapPx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                            )
                        }
                        .background(if (dragging) planV3Surface() else Color.Transparent)
                        .padding(vertical = if (compact) 4.dp else 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExerciseThumbnail(
                        imageRef = item.catalog?.firstThumbnailRef(),
                        exerciseName = item.name,
                        size = if (compact) 48.dp else 68.dp,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!compact) {
                            Text(item.catalog?.metadataLabel().orEmpty().ifBlank { "Custom exercise" }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            Text(item.catalog?.muscleTags(4).orEmpty().ifBlank { "#custom_exercise" }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onRemove(item.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(25.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Remove ${item.name}", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaveWorkoutReview(
    state: SavedWorkoutBuilderUiState,
    onNameChanged: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReordered: (List<BuilderExercise>) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    AuronCompactModalSheet(
        onDismiss = onClose,
        maxHeightFraction = 0.46f,
        applyImePadding = true,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Save this workout", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = state.workoutName,
            onValueChange = onNameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        scope.launch {
                            // Wait for the IME inset to settle, then request the real focused bounds.
                            delay(180)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            placeholder = { Text("Enter a name e.g. 'Monday Heavy Push'") },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = planV3Surface(),
                unfocusedContainerColor = planV3Surface(),
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Selected Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Hold and drag exercises to reorder your workout", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BuilderSelectedList(state.selected, onRemove, onReordered, compact = true)
        state.error?.let { error -> Text(error, color = MaterialTheme.colorScheme.error) }
        ReviewBottomActions(
            onClose = onClose,
            onSave = onSave,
            enabled = state.workoutName.isNotBlank() && state.selected.isNotEmpty() && !state.saving,
            saving = state.saving,
        )
    }
}

@Composable
private fun ReviewBottomActions(
    onClose: () -> Unit,
    onSave: () -> Unit,
    enabled: Boolean,
    saving: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReviewAction("Close", onClose, Modifier.weight(1f), light = false, enabled = !saving)
        ReviewAction(if (saving) "Saving…" else "Save", onSave, Modifier.weight(1f), light = true, enabled = enabled)
    }
}

@Composable
private fun ReviewAction(text: String, onClick: () -> Unit, modifier: Modifier, light: Boolean, enabled: Boolean) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (light) Color.White.copy(alpha = if (enabled) 1f else 0.4f) else Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = 0.55f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (light) Color(0xFF111015) else Color.White, fontWeight = FontWeight.Black)
    }
}
