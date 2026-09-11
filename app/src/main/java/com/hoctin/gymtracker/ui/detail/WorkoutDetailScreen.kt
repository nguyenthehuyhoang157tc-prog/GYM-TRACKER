package com.hoctin.gymtracker.ui.detail

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.data.catalog.thumbnailRefFor
import com.hoctin.gymtracker.data.catalog.trackingTypeFor
import com.hoctin.gymtracker.ui.catalog.CatalogDetailHost
import com.hoctin.gymtracker.ui.catalog.rememberCatalogDetailController
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.format.SetDisplay
import com.hoctin.gymtracker.ui.theme.auronAccentBright
import com.hoctin.gymtracker.ui.theme.auronAccentDeep
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronGlassSurface
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import com.hoctin.gymtracker.ui.workoutDayLabel
import com.hoctin.gymtracker.ui.workoutDaySubtitle

/**
 * View Detail screen: a read-only preview of the selected workout day shown between Home and the
 * Log Workout screen (Home → View Detail → Start Workout → Log Workout). Nothing here mutates data;
 * styling follows the current AURON language (glass surfaces, accent, OLED/dotted background).
 */
@Composable
private fun LegacyWorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onStartWorkout: (Long) -> Unit,
    catalogRepository: ExerciseCatalogRepository,
    onBack: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val day = state.day
    // UI-1 Part C: confirm before starting a workout.
    var showStartConfirm by remember { mutableStateOf(false) }
    // Reusable catalog detail popup: the exercise info icon looks the catalog up by name.
    val catalogDetail = rememberCatalogDetailController(catalogRepository)
    val thumbnailRefs by catalogRepository.observeThumbnailRefsByName()
        .collectAsStateWithLifecycle(initialValue = emptyMap<String, String>())
    val trackingTypes by catalogRepository.observeTrackingTypesByName()
        .collectAsStateWithLifecycle(initialValue = emptyMap<String, TrackingType>())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground(),
    ) {
        when {
            day != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "hero") {
                        DetailHero(
                            dayName = day.day.name,
                            dayNumber = state.dayNumber,
                            onBack = onBack,
                        )
                    }

                    item(key = "cta") {
                        PrimaryGymButton(
                            text = "Start Workout",
                            onClick = { showStartConfirm = true },
                            modifier = Modifier.padding(horizontal = 20.dp),
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                        )
                    }

                    item(key = "section") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionLabel("In this session")
                            Text(
                                text = "${day.exercises.size} exercises",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(day.exercises, key = { "exercise-${it.id}" }) { exercise ->
                        DetailExerciseRow(
                            exercise = exercise,
                            imageRef = thumbnailRefs.thumbnailRefFor(exercise.name),
                            trackingType = trackingTypes.trackingTypeFor(exercise.name) ?: TrackingType.REPS_WEIGHT,
                            onClick = { catalogDetail.openByName(exercise.name) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }

            state.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            else -> {
                // Defensive: the day was deleted while the screen was open.
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Workout not found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    PrimaryGymButton(text = "Back to Home", onClick = onBack)
                }
            }
        }

        CatalogDetailHost(catalogDetail)

        if (showStartConfirm && day != null) {
            StartWorkoutDialog(
                workoutName = day.day.name,
                onConfirm = {
                    showStartConfirm = false
                    onStartWorkout(day.day.id)
                },
                onCancel = { showStartConfirm = false },
            )
        }
    }
}

/**
 * UI-1 Part C — "ready to start?" confirmation. Uses the new card surface (no purple-heavy styling);
 * the primary action is a clean white button, Cancel is a neutral outlined pill.
 */
@Composable
private fun StartWorkoutDialog(
    workoutName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val cardShape = MaterialTheme.shapes.extraLarge
    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(gymContainerBorder(alpha = 0.12f), cardShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Let's get it",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Are you ready to start \"$workoutName\"?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Cancel — neutral outlined pill.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(gymContainerBorder(alpha = 0.22f), RoundedCornerShape(50))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // Start Workout — clean white primary.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Start Workout",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF101013),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHero(
    dayName: String,
    dayNumber: Int,
    onBack: () -> Unit,
) {
    val heroRes = detailHeroResForDay(dayName)
    val darkTheme = isAuronDarkTheme()
    // Theme-aware gradient base — also the graceful fallback for custom days with no photo.
    val heroMid = if (darkTheme) Color(0xFF3A2A66) else Color(0xFF1E5ACF)
    val heroEnd = if (darkTheme) Color(0xFF120D22) else Color(0xFF0B1F4A)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(
                Brush.linearGradient(colors = listOf(auronAccentDeep(), heroMid, heroEnd)),
            ),
    ) {
        if (heroRes != null) {
            Image(
                painter = painterResource(heroRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        // Top + bottom scrim: darkens the top for the back button and the bottom for the copy.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.32f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )

        // Back affordance (returns to Home). statusBarsPadding keeps it clear of the status bar.
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 10.dp, start = 14.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "START NEXT WORKOUT NOW",
                style = MaterialTheme.typography.labelMedium,
                color = auronAccentBright(),
                letterSpacing = 1.5.sp,
            )
            Text(
                text = workoutDayLabel(dayNumber, dayName),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                text = workoutDaySubtitle(dayName),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DetailExerciseRow(
    exercise: ExerciseEntity,
    imageRef: String?,
    trackingType: TrackingType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = auronGlassSurface()),
        border = gymContainerBorder(),
        modifier = modifier
            .fillMaxWidth()
            .auronCardElevation(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseThumbnail(
                imageRef = imageRef,
                exerciseName = exercise.name,
                size = 56.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = SetDisplay.targetLabel(
                        targetSets = exercise.targetSets,
                        targetMinReps = exercise.targetMinReps,
                        targetMaxReps = exercise.targetMaxReps,
                        trackingType = trackingType,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Exercise details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Maps a workout day name to its large detail-hero photo. Keyword-based and case-insensitive so
 * renamed days still resolve; returns null for custom/unknown days so the hero falls back gracefully
 * to the AURON gradient instead of a stretched placeholder.
 */
@DrawableRes
private fun detailHeroResForDay(name: String?): Int? {
    val normalized = name?.lowercase()?.trim().orEmpty()
    return when {
        normalized.contains("push") -> R.drawable.detail_hero_push
        normalized.contains("pull") -> R.drawable.detail_hero_pull
        normalized.contains("leg") -> R.drawable.detail_hero_legs
        normalized.contains("full") -> R.drawable.detail_hero_full_body
        else -> null
    }
}
