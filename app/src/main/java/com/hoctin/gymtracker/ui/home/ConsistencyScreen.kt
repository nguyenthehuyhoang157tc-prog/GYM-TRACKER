package com.hoctin.gymtracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import com.hoctin.gymtracker.ui.toClockTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CalendarPurple = Color(0xFFB79CFF)
private val CalendarMint = Color(0xFF61D8A5)
private val CalendarGold = Color(0xFFFFC64A)
private val CalendarMissed = Color(0xFF5A3038)
private val CalendarPlanned = Color(0xFF2D2448)
private val CalendarRest = Color(0xFF0D0B12)
private val fullDateFormatter =
    DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)

/**
 * Focused continuity route. Home carries the seven-day streak glance; this screen explains
 * milestones, workout-only weekly progress, schedule history, and only the selected day's details.
 */
@Composable
fun ConsistencyScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state = viewModel.consistency.collectAsStateWithLifecycle().value
    var monthIndex by rememberSaveable { mutableIntStateOf(-1) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(Long.MIN_VALUE) }

    LaunchedEffect(state.calendarMonths, state.todayEpochDay) {
        if (state.calendarMonths.isNotEmpty() && monthIndex !in state.calendarMonths.indices) {
            monthIndex = state.calendarMonths.lastIndex
        }
        if (selectedEpochDay == Long.MIN_VALUE && state.todayEpochDay > 0L) {
            selectedEpochDay = state.todayEpochDay
        }
    }

    val selectedMonth = state.calendarMonths.getOrNull(monthIndex)
    val selectedDay = state.day(selectedEpochDay)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = auronScreenTopPadding(8.dp),
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { StreaksHeader(onBack = onBack) }
        item { StreakHero(state = state) }
        item { MilestonePanel(state = state) }
        item { WeeklyWorkoutPanel(state = state) }
        if (selectedMonth != null) {
            item {
                CalendarPanel(
                    state = state,
                    month = selectedMonth,
                    canGoPrevious = monthIndex > 0,
                    canGoNext = monthIndex < state.calendarMonths.lastIndex,
                    selectedEpochDay = selectedEpochDay,
                    onPrevious = {
                        val newIndex = (monthIndex - 1).coerceAtLeast(0)
                        monthIndex = newIndex
                        selectedEpochDay = preferredEpochForMonth(
                            state = state,
                            month = state.calendarMonths[newIndex],
                        )
                    },
                    onNext = {
                        val newIndex = (monthIndex + 1).coerceAtMost(state.calendarMonths.lastIndex)
                        monthIndex = newIndex
                        selectedEpochDay = preferredEpochForMonth(
                            state = state,
                            month = state.calendarMonths[newIndex],
                        )
                    },
                    onSelectDay = { selectedEpochDay = it },
                )
            }
            item {
                SelectedDayDetail(
                    day = selectedDay,
                    onOpenSession = onOpenSession,
                )
            }
        }
    }
}

@Composable
private fun StreaksHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterStart),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Text(
            text = "Streaks",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun StreakHero(state: ConsistencyUiState) {
    ConsistencyAmbientPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 148.dp)
                .semantics {
                    contentDescription =
                        "${state.currentStreak} ${if (state.currentStreak == 1) "day" else "days"}, current streak"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = state.currentStreak.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "current streak",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            StreakFire(
                active = state.currentStreak > 0,
                playCelebration = false,
                size = 112.dp,
                contentDescription = "Static streak flame",
            )
        }
    }
}

@Composable
private fun MilestonePanel(state: ConsistencyUiState) {
    ConsistencyAmbientPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Milestones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = state.milestones.firstOrNull { it.isNext }?.let { "Next: ${it.days} days" }
                    ?: "All reached",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 13.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                state.milestones.forEach { milestone ->
                    MilestoneCheckpoint(
                        milestone = milestone,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneCheckpoint(
    milestone: ConsistencyMilestone,
    modifier: Modifier = Modifier,
) {
    val color = when {
        milestone.reached -> CalendarMint
        milestone.isNext -> CalendarPurple
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier.semantics {
            contentDescription = when {
                milestone.reached -> "${milestone.days} day milestone reached"
                milestone.isNext -> "${milestone.days} day milestone is next"
                else -> "${milestone.days} day milestone"
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    color.copy(
                        alpha = if (milestone.reached) 0.25f else if (milestone.isNext) 0.12f else 0.06f,
                    ),
                )
                .border(
                    width = if (milestone.isNext) 2.dp else 1.dp,
                    color = color.copy(alpha = if (milestone.reached || milestone.isNext) 0.78f else 0.20f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (milestone.reached) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = milestone.days.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun WeeklyWorkoutPanel(state: ConsistencyUiState) {
    ConsistencyAmbientPanel {
        Text(
            text = "Weekly workout progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "${state.weeklyCompleted} of ${state.weeklyTarget}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
        )
        AnimatedWeeklyProgressRail(
            progress = state.weeklyProgress,
            modifier = Modifier.fillMaxWidth(),
        )
        WeeklyScheduleRow(days = state.currentWeek)
        ScheduleLegend()
    }
}

@Composable
private fun WeeklyScheduleRow(days: List<ConsistencyDay>) {
    if (days.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = weekdayShort(day.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.isToday) FontWeight.Black else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ScheduleTile(
                    day = day,
                    selected = false,
                    onClick = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarPanel(
    state: ConsistencyUiState,
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    selectedEpochDay: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSelectDay: (Long) -> Unit,
) {
    val days = state.calendarDays.filter { day ->
        YearMonth.from(LocalDate.ofEpochDay(day.epochDay)) == month
    }
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val slots: List<ConsistencyDay?> = List(leadingBlanks) { null } + days

    ConsistencyAmbientPanel(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = month.year.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
            }
            IconButton(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            slots.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        if (day == null) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            )
                        } else {
                            ScheduleTile(
                                day = day,
                                selected = day.epochDay == selectedEpochDay,
                                onClick = { onSelectDay(day.epochDay) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            )
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        )
                    }
                }
            }
        }
        ScheduleLegend()
    }
}

private data class SchedulePalette(
    val background: Color,
    val content: Color,
)

@Composable
private fun schedulePalette(state: ScheduleVisualState): SchedulePalette {
    val darkTheme = isAuronDarkTheme()
    return when (state) {
        ScheduleVisualState.DONE -> SchedulePalette(
            background = CalendarMint.copy(alpha = if (darkTheme) 0.24f else 0.18f),
            content = if (darkTheme) CalendarMint else Color(0xFF14764F),
        )
        ScheduleVisualState.MISSED -> SchedulePalette(
            background = if (darkTheme) CalendarMissed else MaterialTheme.colorScheme.errorContainer,
            content = if (darkTheme) Color(0xFFFFE6E8) else MaterialTheme.colorScheme.onErrorContainer,
        )
        ScheduleVisualState.PLANNED -> SchedulePalette(
            background = if (darkTheme) CalendarPlanned else MaterialTheme.colorScheme.secondaryContainer,
            content = if (darkTheme) CalendarPurple else MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ScheduleVisualState.REST -> SchedulePalette(
            background = if (darkTheme) CalendarRest else MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScheduleVisualState.BEFORE_TRACKING -> SchedulePalette(
            background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f),
            content = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
        )
    }
}

@Composable
private fun ScheduleTile(
    day: ConsistencyDay,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val visualState = scheduleVisualState(day)
    val palette = schedulePalette(visualState)
    val shape = RoundedCornerShape(11.dp)
    val highlight = selected || day.isToday
    val description = buildString {
        append(LocalDate.ofEpochDay(day.epochDay).format(fullDateFormatter))
        append(", ")
        append(scheduleStateLabel(visualState))
        if (day.isToday) append(", today")
        if (selected) append(", selected")
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = description }
            .clip(shape)
            .background(palette.background)
            .then(
                if (highlight) {
                    Modifier.border(
                        width = if (selected) 2.dp else 1.dp,
                        color = CalendarPurple.copy(alpha = if (selected) 0.94f else 0.72f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (visualState) {
            ScheduleVisualState.DONE -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(19.dp),
            )
            ScheduleVisualState.MISSED -> Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = palette.content,
                modifier = Modifier.size(18.dp),
            )
            ScheduleVisualState.PLANNED,
            ScheduleVisualState.REST,
            ScheduleVisualState.BEFORE_TRACKING,
            -> Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (highlight) FontWeight.Black else FontWeight.SemiBold,
                color = palette.content,
            )
        }
    }
}

@Composable
private fun ScheduleLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = "WORKOUT SCHEDULE",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ScheduleLegendItem(ScheduleVisualState.DONE, "Done")
            ScheduleLegendItem(ScheduleVisualState.MISSED, "Missed")
            ScheduleLegendItem(ScheduleVisualState.PLANNED, "Planned")
            ScheduleLegendItem(ScheduleVisualState.REST, "Rest")
        }
    }
}

@Composable
private fun ScheduleLegendItem(
    state: ScheduleVisualState,
    label: String,
) {
    val palette = schedulePalette(state)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.background),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectedDayDetail(
    day: ConsistencyDay?,
    onOpenSession: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (day == null) {
            Text(
                text = "Select a date to view details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        val date = LocalDate.ofEpochDay(day.epochDay)
        Text(
            text = date.format(fullDateFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        day.workouts.forEach { workout ->
            WorkoutDetailRow(
                workout = workout,
                onClick = { onOpenSession(workout.sessionId) },
            )
        }
        if (day.activitySource == ActivitySource.BOOST || day.activitySource == ActivitySource.BOTH) {
            BoostDetailLine()
        }
        if (day.activitySource == ActivitySource.NONE) {
            Text(
                text = if (day.isBeforeTracking) {
                    "Tracking had not started yet."
                } else {
                    "No activity recorded."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun WorkoutDetailRow(
    workout: ConsistencyWorkoutEvidence,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val darkTheme = isAuronDarkTheme()
    val containerColor = if (darkTheme) {
        Color(0xE61A1524)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val outlineColor = if (darkTheme) {
        CalendarPurple.copy(alpha = 0.17f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val summaryMetrics = workout.visibleSummaryMetrics()
    val summaryDescription = summaryMetrics.joinToString(", ") { metric ->
        workout.summaryMetricText(metric)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, outlineColor, shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(workout.title)
                    append(", saved ")
                    append(workout.startedAtMillis.toClockTime())
                    if (summaryDescription.isNotEmpty()) {
                        append(", ")
                        append(summaryDescription)
                    }
                    append(". Open workout review.")
                }
            }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (darkTheme) {
                            Color(0xFF30274B)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = if (darkTheme) CalendarPurple else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = workout.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = workout.startedAtMillis.toClockTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (summaryMetrics.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                summaryMetrics.forEach { metric ->
                    WorkoutSummaryChip(
                        workout = workout,
                        metric = metric,
                        darkTheme = darkTheme,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummaryChip(
    workout: ConsistencyWorkoutEvidence,
    metric: ConsistencyWorkoutSummaryMetric,
    darkTheme: Boolean,
) {
    val neutralContainer = if (darkTheme) {
        Color.Black.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val (container, content) = when (metric) {
        ConsistencyWorkoutSummaryMetric.REP_VOLUME -> {
            val greenContainer = if (darkTheme) {
                Color(0xFF10291F)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            greenContainer to if (darkTheme) CalendarMint else MaterialTheme.colorScheme.onTertiaryContainer
        }
        ConsistencyWorkoutSummaryMetric.DURATION -> {
            val purpleContainer = if (darkTheme) {
                Color(0xFF251D39)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            purpleContainer to if (darkTheme) CalendarPurple else MaterialTheme.colorScheme.onPrimaryContainer
        }
        ConsistencyWorkoutSummaryMetric.PERSONAL_RECORDS -> {
            val goldContainer = if (darkTheme) {
                Color(0xFF2C2412)
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
            goldContainer to if (darkTheme) CalendarGold else MaterialTheme.colorScheme.onSecondaryContainer
        }
        ConsistencyWorkoutSummaryMetric.SETS,
        ConsistencyWorkoutSummaryMetric.EXERCISES,
        -> neutralContainer to MaterialTheme.colorScheme.onSurface
    }
    GymBadge(
        text = workout.summaryMetricText(metric),
        icon = if (metric == ConsistencyWorkoutSummaryMetric.PERSONAL_RECORDS) {
            Icons.Outlined.EmojiEvents
        } else {
            null
        },
        container = container,
        content = content,
    )
}

private fun ConsistencyWorkoutEvidence.summaryMetricText(
    metric: ConsistencyWorkoutSummaryMetric,
): String = when (metric) {
    ConsistencyWorkoutSummaryMetric.SETS -> "$totalSets sets"
    ConsistencyWorkoutSummaryMetric.REP_VOLUME ->
        MeasurementFormatter.weight(requireNotNull(repVolumeKg)).inline()
    ConsistencyWorkoutSummaryMetric.DURATION ->
        "${MeasurementFormatter.duration(requireNotNull(durationSeconds)).inline()} duration"
    ConsistencyWorkoutSummaryMetric.EXERCISES -> "$exerciseCount exercises"
    ConsistencyWorkoutSummaryMetric.PERSONAL_RECORDS -> "$prCount PR"
}

@Composable
private fun BoostDetailLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Bolt,
            contentDescription = null,
            tint = CalendarPurple,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = "Daily Boost completed",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun preferredEpochForMonth(
    state: ConsistencyUiState,
    month: YearMonth,
): Long {
    val monthDays = state.calendarDays.filter {
        YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month
    }
    return monthDays.firstOrNull { it.activitySource != ActivitySource.NONE }?.epochDay
        ?: monthDays.firstOrNull { it.epochDay >= state.trackingStartEpochDay }?.epochDay
        ?: month.atDay(1).toEpochDay()
}

private fun scheduleStateLabel(state: ScheduleVisualState): String = when (state) {
    ScheduleVisualState.DONE -> "workout completed"
    ScheduleVisualState.MISSED -> "workout missed"
    ScheduleVisualState.PLANNED -> "workout planned"
    ScheduleVisualState.REST -> "rest day"
    ScheduleVisualState.BEFORE_TRACKING -> "before tracking"
}

private fun weekdayShort(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Mo"
    2 -> "Tu"
    3 -> "We"
    4 -> "Th"
    5 -> "Fr"
    6 -> "Sa"
    else -> "Su"
}
