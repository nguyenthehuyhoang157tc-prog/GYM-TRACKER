package com.hoctin.gymtracker.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.IconThumbnail
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.StatCard
import com.hoctin.gymtracker.ui.prCount
import com.hoctin.gymtracker.ui.theme.PurpleBright
import com.hoctin.gymtracker.ui.theme.PurpleDeep
import com.hoctin.gymtracker.ui.theme.PurplePrimary
import com.hoctin.gymtracker.ui.toShortDate
import com.hoctin.gymtracker.ui.totalVolume

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: (Long) -> Unit,
    onOpenExercise: (Long) -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { BrandHeader() }

        item {
            HeroWorkoutCard(
                day = state.todayPlan,
                context = state.context,
                onQuickStart = { state.todayPlan?.let { onStartWorkout(it.day.id) } },
            )
        }

        if (state.availableDays.isNotEmpty()) {
            item {
                DayChipRow(
                    days = state.availableDays,
                    selectedId = state.todayPlan?.day?.id,
                    onSelect = viewModel::selectDay,
                    onSkip = viewModel::skipToNextWorkoutDay,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Your progress")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        icon = Icons.Outlined.FitnessCenter,
                        value = state.totalSessions.toString(),
                        label = "Workouts",
                        modifier = Modifier.weight(1f),
                        accent = PurplePrimary,
                    )
                    StatCard(
                        icon = Icons.Outlined.CalendarMonth,
                        value = state.sessionsThisWeek.toString(),
                        label = "Last 7 days",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                    StatCard(
                        icon = Icons.Outlined.LocalFireDepartment,
                        value = state.currentStreak.toString(),
                        label = "Day streak",
                        modifier = Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        state.todayPlan?.let { today ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (state.context) {
                            WorkoutContext.TODAY -> "Today's workout"
                            WorkoutContext.SELECTED -> "Selected workout"
                            WorkoutContext.NEXT -> "Next workout"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${today.exercises.size} exercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(today.exercises, key = { "exercise-${it.id}" }) { exercise ->
                ExerciseRowCard(
                    name = exercise.name,
                    target = "${exercise.targetSets} sets · ${exercise.targetMinReps}-${exercise.targetMaxReps} reps",
                    onClick = { onOpenExercise(exercise.id) },
                    modifier = Modifier.animateItemPlacement(),
                )
            }
        }

        if (state.recentSessions.isNotEmpty()) {
            item { SectionLabel("Recent sessions") }
            items(state.recentSessions, key = { "session-${it.session.id}" }) { session ->
                RecentSessionCard(
                    title = session.session.dayNameSnapshot,
                    subtitle = session.session.dateEpochDay.toShortDate(),
                    sets = session.sets.size,
                    volume = ProgressiveOverloadEngine.formatWeight(session.totalVolume()),
                    prs = session.prCount(),
                    modifier = Modifier.animateItemPlacement(),
                )
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "GYMATE",
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
}

@Composable
private fun HeroWorkoutCard(
    day: WorkoutDayWithExercises?,
    context: WorkoutContext,
    onQuickStart: () -> Unit,
) {
    val totalSets = day?.exercises?.sumOf { it.targetSets } ?: 0
    val heroRes = heroResForDay(day?.day?.name)
    val heroShape = MaterialTheme.shapes.extraLarge
    Card(
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            PurpleDeep,
                            Color(0xFF3A2A66),
                            Color(0xFF120D22),
                        ),
                    ),
                ),
        ) {
            // Workout-specific hero artwork sits behind the text on the right side of the card.
            // The slight overscale crops the bitmap's white outer canvas before the rounded clip.
            Image(
                painter = painterResource(heroRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.04f),
            )
            // Left-to-right scrim keeps the title, badges and Quick Start button readable.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color(0xFF160F2B).copy(alpha = 0.72f),
                            0.5f to Color(0xFF160F2B).copy(alpha = 0.18f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when (context) {
                        WorkoutContext.TODAY -> "TODAY'S WORKOUT"
                        WorkoutContext.SELECTED -> "SELECTED WORKOUT"
                        WorkoutContext.NEXT -> "NEXT WORKOUT"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = PurpleBright,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    text = day?.day?.name ?: "Plan not ready",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymBadge(
                        text = "${day?.exercises?.size ?: 0} exercises",
                        icon = Icons.Outlined.FitnessCenter,
                        container = Color.White.copy(alpha = 0.12f),
                        content = Color.White,
                    )
                    GymBadge(
                        text = "$totalSets sets",
                        container = Color.White.copy(alpha = 0.12f),
                        content = Color.White,
                    )
                }
                Spacer(Modifier.height(2.dp))
                QuickStartButton(enabled = day != null, onClick = onQuickStart)
            }
        }
    }
}

/**
 * Maps a workout day name to its hero artwork. Matching is keyword-based and case-insensitive so
 * renamed days still resolve, and anything unknown falls back to the neutral dumbbell hero. Always
 * returns a valid drawable, so the hero card can never crash on a missing image.
 */
@DrawableRes
private fun heroResForDay(name: String?): Int {
    val normalized = name?.lowercase()?.trim().orEmpty()
    return when {
        normalized.contains("push") -> R.drawable.hero_push
        normalized.contains("pull") -> R.drawable.hero_pull
        normalized.contains("leg") -> R.drawable.hero_legs
        normalized.contains("full") -> R.drawable.hero_full_body
        else -> R.drawable.hero_default
    }
}

@Composable
private fun QuickStartButton(enabled: Boolean, onClick: () -> Unit) {
    val container = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = PurpleDeep,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Quick Start",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PurpleDeep,
        )
    }
}

@Composable
private fun DayChipRow(
    days: List<WorkoutDayWithExercises>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        days.forEach { day ->
            DayChip(
                label = day.day.name,
                selected = day.day.id == selectedId,
                onClick = { onSelect(day.day.id) },
            )
        }
        // Skip / next workout day control.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onSkip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = "Skip to next workout day",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
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

@Composable
private fun ExerciseRowCard(
    name: String,
    target: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconThumbnail(icon = Icons.Outlined.FitnessCenter)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    title: String,
    subtitle: String,
    sets: Int,
    volume: String,
    prs: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconThumbnail(
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                tint = MaterialTheme.colorScheme.tertiary,
                container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$subtitle · $sets sets · ${volume}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prs > 0) {
                GymBadge(
                    text = "$prs PR",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
