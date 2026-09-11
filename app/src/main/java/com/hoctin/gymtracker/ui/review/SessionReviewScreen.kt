package com.hoctin.gymtracker.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.thumbnailRefFor
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiModel
import com.hoctin.gymtracker.ui.catalog.CatalogDetailHost
import com.hoctin.gymtracker.ui.catalog.rememberCatalogDetailController
import com.hoctin.gymtracker.ui.components.ConfettiBurst
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.history.ExerciseBlock
import com.hoctin.gymtracker.ui.muscleheat.BodyMapFigure
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSide
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import com.hoctin.gymtracker.ui.toClockTime
import com.hoctin.gymtracker.ui.toLongDate
import kotlinx.coroutines.launch

/**
 * MUSCLE-3B — Muscle Heat Review, laid out to the approved mockup: dark/OLED full screen with a
 * purple top gradient; header (back arrow, centered title, date/time — duration subtitle); one large
 * rounded near-black card (Duration → Top Muscles → front/back body maps → AURON wordmark); circular
 * Share/Edit buttons under the card; then the existing exercise/set breakdown. No calories in V1
 * (no real data). The card is an enhancement — the breakdown below is never removed.
 */
@Composable
fun SessionReviewScreen(
    viewModel: SessionReviewViewModel,
    durationMillis: Long,
    onBack: () -> Unit,
    catalogRepository: ExerciseCatalogRepository,
    distanceUnitSystem: DistanceUnitSystem = DistanceUnitSystem.METRIC,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val thumbnailRefs by viewModel.thumbnailRefs.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val catalogDetail = rememberCatalogDetailController(catalogRepository)
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val renameFocusRequester = remember { FocusRequester() }
    var renameDialogOpen by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    // MUSCLE-3B.1: the persisted duration (DB) wins so History/Home-opened reviews match the
    // after-save review; the nav argument remains a fallback for the just-saved transition.
    val effectiveDurationMillis = state.storedDurationMillis ?: durationMillis

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun shareCurrentHeatCard() {
        val session = state.session
        if (session == null) {
            showMessage("Session is still loading.")
            return
        }
        if (state.loading) {
            showMessage("Heat card is not ready to share.")
            return
        }
        scope.launch {
            runCatching {
                shareHeatCard(
                    context = context,
                    content = HeatCardShareContent(
                        heat = state.heat,
                        topMuscles = state.topMuscles,
                        durationText = formatDurationMillis(effectiveDurationMillis),
                    ),
                    sessionId = session.id,
                )
            }.onFailure {
                showMessage("Could not share heat card.")
            }
        }
    }

    fun openRenameDialog() {
        val session = state.session
        if (session == null) {
            showMessage("Session is still loading.")
            return
        }
        renameText = session.dayName
        renameDialogOpen = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // MUSCLE-3B.1 mockup polish: purple-tinted top fading into TRUE OLED black (#000000)
            // toward the bottom — this screen is intentionally a dark share-card surface.
            .background(
                Brush.verticalGradient(
                    0.0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                    0.55f to Color.Black,
                    1.0f to Color.Black,
                ),
            ),
    ) {

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = auronScreenTopPadding(8.dp),
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "review-header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.session?.dayName ?: "Workout",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.session?.let { session ->
                            val duration = formatDurationMillis(effectiveDurationMillis)
                            Text(
                                text = buildString {
                                    append(session.startedAtMillis.toClockTime())
                                    append(", ")
                                    append(session.dateEpochDay.toLongDate())
                                    if (duration != null) append(" — $duration")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.size(48.dp)) // balances the back button so the title stays centered
                }
            }

            item(key = "review-heat-card") {
                HeatCard(
                    heat = state.heat,
                    topMuscles = state.topMuscles,
                    durationText = formatDurationMillis(effectiveDurationMillis),
                    loading = state.loading,
                )
            }

            // Mockup: circular Share + Edit actions directly under the heat card.
            item(key = "review-actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                ) {
                    RoundAction(icon = { Icon(Icons.Outlined.Share, contentDescription = "Share") }) {
                        shareCurrentHeatCard()
                    }
                    RoundAction(icon = { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }) {
                        openRenameDialog()
                    }
                }
            }

            // The existing exercise breakdown stays fully available below the card.
            state.session?.exercises?.forEach { exercise ->
                item(key = "review-exercise-${exercise.name}") {
                    ExerciseBlock(
                        exercise = exercise,
                        imageRef = thumbnailRefs.thumbnailRefFor(exercise.name),
                        onInfo = { catalogDetail.openByName(exercise.name) },
                        distanceUnitSystem = distanceUnitSystem,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )

        // One-shot confetti burst layered above the body map. Plays to its final frame and rests
        // there; doesn't intercept touches, so the heat card / share / rename still work underneath.
        // Only fires for the just-saved transition — the saved flow passes the actual duration in
        // the nav arg, whereas History / Consistency open this screen with durationMillis = 0.
        if (durationMillis > 0L) {
            ConfettiBurst(modifier = Modifier.fillMaxSize())
        }
    }

    if (renameDialogOpen) {
        val trimmedTitle = renameText.trim()
        LaunchedEffect(renameDialogOpen) {
            renameFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = { renameDialogOpen = false },
            title = { Text("Rename session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.focusRequester(renameFocusRequester),
                    singleLine = true,
                    label = { Text("Session title") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = trimmedTitle.isNotBlank(),
                    onClick = {
                        viewModel.renameSession(trimmedTitle)
                        renameDialogOpen = false
                        showMessage("Session title updated.")
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogOpen = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    CatalogDetailHost(catalogDetail)
}

/** Near-OLED black card fixed to the mockup's share-card look in both app themes. */
private val HeatCardColor = Color(0xFF0B0C0E)
private val HeatCardOnColor = Color(0xFFF2F3F5)
private val HeatCardLabelColor = Color(0xFF9BA1AB)

@Composable
private fun HeatCard(
    heat: MuscleHeatUiModel?,
    topMuscles: List<String>,
    durationText: String?,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = HeatCardColor, contentColor = HeatCardOnColor),
        border = gymContainerBorder(alpha = 0.10f),
        modifier = modifier
            .fillMaxWidth()
            .auronCardElevation(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (durationText != null) {
                CardLabel("Duration")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = durationText.uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
            }

            CardLabel("Top Muscles")
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (topMuscles.isEmpty()) "—" else topMuscles.joinToString(", ").uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))
            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                heat != null -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    BodyMapFigure(
                        side = BodyMapSide.FRONT,
                        regions = heat.regions,
                        modifier = Modifier
                            .weight(1f)
                            .height(330.dp),
                    )
                    BodyMapFigure(
                        side = BodyMapSide.BACK,
                        regions = heat.regions,
                        modifier = Modifier
                            .weight(1f)
                            .height(330.dp),
                    )
                }
                // Heat unavailable (e.g. computation failed): degrade gracefully, no fake map.
                else -> Text(
                    text = "Muscle map unavailable for this session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HeatCardLabelColor,
                )
            }

            // Summary-only (e.g. whole-body) and custom/unmatched exercises stay visible — they are
            // never painted as fake regions.
            val footnotes = buildList {
                heat?.summaryOnly.orEmpty().forEach { add("${it.displayName}: ${it.reason}") }
                val unattributed = heat?.unattributedExercises.orEmpty()
                if (unattributed.isNotEmpty()) {
                    add("Not on the map (custom exercises): ${unattributed.joinToString(", ")}")
                }
            }
            if (footnotes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                footnotes.forEach { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = HeatCardLabelColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // AURON wordmark at the bottom of the card (mockup shows a brand line; never SLOPAX).
            Spacer(Modifier.height(20.dp))
            Text(
                text = "AURON",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                color = HeatCardOnColor,
            )
        }
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = HeatCardLabelColor,
    )
}

@Composable
private fun RoundAction(icon: @Composable () -> Unit, onClick: () -> Unit) {
    // Mockup: white circular actions with dark icons.
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        contentColor = Color(0xFF15171B),
        shadowElevation = 8.dp,
        modifier = Modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * Wall-clock duration when known (>= 1 minute); null hides the duration UI (e.g. from History).
 * Mockup style: "1 hour, 38 mins" (screen renders it uppercase).
 */
internal fun formatDurationMillis(durationMillis: Long): String? {
    if (durationMillis < 60_000L) return null
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val hourPart = when {
        hours == 1L -> "1 hour"
        hours > 1L -> "$hours hours"
        else -> null
    }
    val minutePart = when {
        minutes == 1L -> "1 min"
        minutes > 1L -> "$minutes mins"
        else -> null
    }
    return listOfNotNull(hourPart, minutePart).joinToString(", ").ifEmpty { null }
}
