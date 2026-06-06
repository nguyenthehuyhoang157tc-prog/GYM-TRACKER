package com.hoctin.gymtracker.ui.home

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                isManualSelection = state.isManualSelection,
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
                        text = "Today's workout",
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
            text = "GYMTRACKER",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
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
    isManualSelection: Boolean,
    onQuickStart: () -> Unit,
) {
    val totalSets = day?.exercises?.sumOf { it.targetSets } ?: 0
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
            // Decorative oversized icon in the corner.
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(140.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (isManualSelection) "SELECTED WORKOUT" else "NEXT WORKOUT",
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
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
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
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
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
