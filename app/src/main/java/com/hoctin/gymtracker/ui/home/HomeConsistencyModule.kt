package com.hoctin.gymtracker.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.ui.components.ProgressRing
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import java.time.LocalDate
import kotlin.math.roundToInt

internal val WeeklyRed = Color(0xFFFF5D67)
internal val WeeklyOrange = Color(0xFFFF9D42)
internal val WeeklyYellowGreen = Color(0xFFC6D94C)
internal val WeeklyGreen = Color(0xFF55D68B)
private val FlameOrange = Color(0xFFFF7A2E)
private val FlameGold = Color(0xFFFFC64A)
private val BoostPurple = Color(0xFFB79CFF)
private val WorkoutMint = Color(0xFF61D8A5)
private val WorkoutActivityRed = Color(0xFFFF665E)
private val StreakMissed = Color(0xFF5A3038)

internal fun weeklyProgressColor(progress: Float): Color = when {
    progress < 0.25f -> WeeklyRed
    progress < 0.50f -> WeeklyOrange
    progress < 0.75f -> WeeklyYellowGreen
    else -> WeeklyGreen
}

internal enum class HomeStreakVisualState {
    ACTIVE,
    MISSED,
    TODAY_PENDING,
    BEFORE_TRACKING,
}

internal fun homeStreakVisualState(
    activitySource: ActivitySource,
    isToday: Boolean,
    isBeforeTracking: Boolean,
): HomeStreakVisualState = when {
    isBeforeTracking -> HomeStreakVisualState.BEFORE_TRACKING
    activitySource != ActivitySource.NONE -> HomeStreakVisualState.ACTIVE
    isToday -> HomeStreakVisualState.TODAY_PENDING
    else -> HomeStreakVisualState.MISSED
}

internal enum class ScheduleVisualState {
    DONE,
    MISSED,
    PLANNED,
    REST,
    BEFORE_TRACKING,
}

internal fun scheduleVisualState(day: ConsistencyDay): ScheduleVisualState = when {
    day.isBeforeTracking -> ScheduleVisualState.BEFORE_TRACKING
    day.workoutStatus == WorkoutDayStatus.COMPLETED -> ScheduleVisualState.DONE
    day.workoutStatus == WorkoutDayStatus.MISSED -> ScheduleVisualState.MISSED
    day.workoutStatus == WorkoutDayStatus.PLANNED -> ScheduleVisualState.PLANNED
    else -> ScheduleVisualState.REST
}

/**
 * Compact Home module: one Ring-based workout summary and one horizontal activity-streak signal.
 * The workout schedule remains on the pushed Streaks detail route, so Home never repeats the same
 * progress as both a Ring and a rail.
 */
@Composable
fun HomeConsistencyModule(
    state: ConsistencyUiState,
    playFireCelebration: Boolean,
    onViewMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConsistencyAmbientPanel(modifier = modifier) {
        WeeklyProgressRingSummary(
            state = state,
            onViewMore = onViewMore,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        HomeStreakStrip(
            state = state,
            playFireCelebration = playFireCelebration,
        )
    }
}

@Composable
private fun WeeklyProgressRingSummary(
    state: ConsistencyUiState,
    onViewMore: () -> Unit,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val clampedProgress = state.weeklyProgress.coerceIn(0f, 1f)
    val ringColor by animateColorAsState(
        targetValue = weeklyProgressColor(clampedProgress),
        animationSpec = tween(
            durationMillis = if (motionEnabled) 500 else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "weeklyProgressRingColor",
    )
    val progressPercent = (clampedProgress * 100f).roundToInt().coerceIn(0, 100)
    val progressDescription =
        "${state.weeklyCompleted} of ${state.weeklyTarget} scheduled workout days completed this week"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onViewMore)
            .semantics {
                role = Role.Button
                contentDescription = "$progressDescription. Open streak details."
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(
            progress = clampedProgress,
            ringSize = 96.dp,
            strokeWidth = 11.dp,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            progressBrush = Brush.sweepGradient(
                listOf(
                    ringColor.copy(alpha = 0.72f),
                    ringColor,
                ),
            ),
            modifier = Modifier.semantics {
                contentDescription = progressDescription
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = clampedProgress,
                    range = 0f..1f,
                )
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.weeklyCompleted}/${state.weeklyTarget}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ringColor,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Weekly Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "${state.weeklyCompleted} of ${state.weeklyTarget} workout days",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onViewMore,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text("View More", fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun ConsistencyAmbientPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val darkTheme = isAuronDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    val brush = if (darkTheme) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.055f),
            0.38f to Color(0xFF281A45).copy(alpha = 0.22f),
            1f to Color.Black.copy(alpha = 0.16f),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            ),
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush)
            .border(gymContainerBorder(alpha = if (darkTheme) 0.15f else 0.10f), shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
internal fun AnimatedWeeklyProgressRail(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(
            durationMillis = if (motionEnabled) 760 else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "weeklyProgressFill",
    )
    val animatedColor by animateColorAsState(
        targetValue = weeklyProgressColor(clamped),
        animationSpec = tween(
            durationMillis = if (motionEnabled) 460 else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "weeklyProgressColor",
    )
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .semantics {
                contentDescription = "${(clamped * 100).toInt()} percent of weekly workout goal"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(animatedColor.copy(alpha = 0.72f), animatedColor),
                    ),
                ),
        )
    }
}

@Composable
internal fun WeekRibbon(
    days: List<ConsistencyDay>,
    showActivityLane: Boolean,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Spacer(Modifier.width(62.dp))
            days.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = weekdayShort(day.dayOfWeek),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Black else FontWeight.Medium,
                        color = if (day.isToday) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        RibbonLane(label = "WORKOUT", days = days) { day ->
            WorkoutDayMarker(day)
        }
        if (showActivityLane) {
            RibbonLane(label = "ACTIVE", days = days) { day ->
                ActivityDayMarker(day)
            }
            ActivityLegend()
        }
    }
}

@Composable
private fun RibbonLane(
    label: String,
    days: List<ConsistencyDay>,
    marker: @Composable (ConsistencyDay) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(62.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        days.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                marker(day)
            }
        }
    }
}

@Composable
private fun WorkoutDayMarker(day: ConsistencyDay) {
    val label = when (day.workoutStatus) {
        WorkoutDayStatus.COMPLETED -> "Workout completed"
        WorkoutDayStatus.PLANNED -> "Workout planned"
        WorkoutDayStatus.MISSED -> "Planned workout missed"
        WorkoutDayStatus.REST -> "Rest day"
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        when (day.workoutStatus) {
            WorkoutDayStatus.COMPLETED -> {
                MarkerContainer(color = WorkoutMint, shape = RoundedCornerShape(8.dp))
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = WorkoutMint,
                    modifier = Modifier.size(15.dp),
                )
            }
            WorkoutDayStatus.PLANNED -> {
                MarkerOutline(color = BoostPurple, shape = RoundedCornerShape(8.dp))
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = BoostPurple,
                    modifier = Modifier.size(13.dp),
                )
            }
            WorkoutDayStatus.MISSED -> {
                MarkerOutline(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp),
                )
            }
            WorkoutDayStatus.REST -> {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)),
                )
            }
        }
    }
}

@Composable
private fun ActivityDayMarker(day: ConsistencyDay) {
    val label = when (day.activitySource) {
        ActivitySource.WORKOUT -> "Active from workout"
        ActivitySource.BOOST -> "Active from Daily Boost"
        ActivitySource.BOTH -> "Active from workout and Daily Boost, counted once"
        ActivitySource.NONE -> if (day.isToday) "Today is pending" else "No activity"
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        ActivityMarkerGraphic(source = day.activitySource, pendingToday = day.isToday)
    }
}

@Composable
internal fun ActivityMarkerGraphic(
    source: ActivitySource,
    pendingToday: Boolean = false,
    compact: Boolean = false,
) {
    val outerSize = if (compact) 18.dp else 24.dp
    when (source) {
        ActivitySource.WORKOUT -> Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(WorkoutActivityRed.copy(alpha = 0.12f))
                .border(1.dp, WorkoutActivityRed.copy(alpha = 0.82f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = WorkoutActivityRed,
                modifier = Modifier.size(if (compact) 10.dp else 14.dp),
            )
        }
        ActivitySource.BOOST -> Box(
            modifier = Modifier.size(outerSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 13.dp else 17.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BoostPurple.copy(alpha = 0.18f))
                    .border(1.dp, BoostPurple.copy(alpha = 0.82f), RoundedCornerShape(4.dp)),
            )
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = BoostPurple,
                modifier = Modifier.size(if (compact) 9.dp else 12.dp),
            )
        }
        ActivitySource.BOTH -> Box(
            modifier = Modifier
                .size(outerSize)
                .clip(CircleShape)
                .background(WorkoutActivityRed.copy(alpha = 0.08f))
                .border(1.dp, WorkoutActivityRed.copy(alpha = 0.90f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 8.dp else 11.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BoostPurple),
            )
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(if (compact) 6.dp else 8.dp),
            )
        }
        ActivitySource.NONE -> if (pendingToday) {
            Box(
                modifier = Modifier
                    .size(outerSize)
                    .clip(CircleShape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(if (compact) 10.dp else 14.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)),
            )
        }
    }
}

@Composable
private fun MarkerContainer(color: Color, shape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.76f), shape),
    )
}

@Composable
private fun MarkerOutline(color: Color, shape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.72f), shape),
    )
}

@Composable
private fun ActivityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityLegendItem(ActivitySource.WORKOUT, "Workout")
        ActivityLegendItem(ActivitySource.BOOST, "Boost")
        ActivityLegendItem(ActivitySource.BOTH, "Both")
    }
}

@Composable
private fun ActivityLegendItem(source: ActivitySource, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityMarkerGraphic(source = source, compact = true)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeStreakStrip(
    state: ConsistencyUiState,
    playFireCelebration: Boolean,
) {
    val todayEpochDay = state.todayEpochDay.takeIf { it > 0L } ?: LocalDate.now().toEpochDay()
    val days = (6L downTo 0L).map { offset ->
        val epochDay = todayEpochDay - offset
        val day = state.day(epochDay)
        HomeStreakDay(
            epochDay = epochDay,
            dayOfWeek = LocalDate.ofEpochDay(epochDay).dayOfWeek.value,
            isToday = epochDay == todayEpochDay,
            visualState = homeStreakVisualState(
                activitySource = day?.activitySource ?: ActivitySource.NONE,
                isToday = epochDay == todayEpochDay,
                isBeforeTracking = epochDay < state.trackingStartEpochDay,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Streak, ${state.currentStreak} ${if (state.currentStreak == 1) "day" else "days"}"
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Streak",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { day ->
                HomeStreakCell(
                    day = day,
                    playCelebration = playFireCelebration &&
                        day.epochDay == todayEpochDay &&
                        day.visualState == HomeStreakVisualState.ACTIVE,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private data class HomeStreakDay(
    val epochDay: Long,
    val dayOfWeek: Int,
    val isToday: Boolean,
    val visualState: HomeStreakVisualState,
)

@Composable
private fun HomeStreakCell(
    day: HomeStreakDay,
    playCelebration: Boolean,
    modifier: Modifier = Modifier,
) {
    val date = LocalDate.ofEpochDay(day.epochDay)
    val stateLabel = when (day.visualState) {
        HomeStreakVisualState.ACTIVE -> "Active day"
        HomeStreakVisualState.MISSED -> "Missed day"
        HomeStreakVisualState.TODAY_PENDING -> "Today pending"
        HomeStreakVisualState.BEFORE_TRACKING -> "Before tracking"
    }
    Column(
        modifier = modifier.semantics {
            contentDescription = "${date.dayOfWeek}, $stateLabel"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = weekdayShort(day.dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) {
                FontWeight.Black
            } else {
                FontWeight.SemiBold
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    when (day.visualState) {
                        HomeStreakVisualState.ACTIVE -> Modifier.background(
                            FlameOrange.copy(alpha = if (isAuronDarkTheme()) 0.09f else 0.07f),
                        )
                        HomeStreakVisualState.MISSED -> Modifier.background(StreakMissed)
                        HomeStreakVisualState.TODAY_PENDING -> Modifier
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f))
                            .border(2.dp, BoostPurple.copy(alpha = 0.72f), CircleShape)
                        HomeStreakVisualState.BEFORE_TRACKING -> Modifier.background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (day.visualState) {
                HomeStreakVisualState.ACTIVE -> {
                    if (playCelebration) {
                        StreakFire(
                            active = true,
                            playCelebration = true,
                            size = 40.dp,
                            contentDescription = "Today's active streak fire",
                        )
                    } else {
                        SettledStreakFlame(active = true, size = 38.dp)
                    }
                }
                HomeStreakVisualState.MISSED -> Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(19.dp),
                )
                HomeStreakVisualState.TODAY_PENDING -> Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(BoostPurple.copy(alpha = 0.72f)),
                )
                HomeStreakVisualState.BEFORE_TRACKING -> Text(
                    text = "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
                )
            }
        }
    }
}

/**
 * The imported fire plays five 26-frame loops at 24 fps and 1.08x speed (about five seconds total),
 * then swaps to a guaranteed-visible static flame. The Lottie asset's final frame is transparent, so
 * holding progress at 1f would make the fire disappear. System animator scale 0 skips both Lottie
 * playback and the spring and renders the same static state immediately.
 */
@Composable
internal fun StreakFire(
    active: Boolean,
    playCelebration: Boolean,
    size: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.auron_streak_fire),
    )
    val shouldPlay = active && playCelebration && motionEnabled
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = shouldPlay,
        restartOnPlay = true,
        iterations = 5,
        speed = 1.08f,
    )
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            popScale.snapTo(0.88f)
            popScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
            )
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            popScale.snapTo(1f)
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .scale(popScale.value)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        val showAnimatedFire = shouldPlay && composition != null && progress < 0.999f
        if (showAnimatedFire) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(size),
            )
        } else {
            SettledStreakFlame(active = active, size = size)
        }
    }
}

@Composable
internal fun SettledStreakFlame(
    active: Boolean,
    size: Dp,
) {
    if (!active) {
        Icon(
            imageVector = Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.56f),
        )
        return
    }
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = FlameGold.copy(alpha = 0.28f),
            modifier = Modifier.size(size * 0.82f),
        )
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = FlameGold,
            modifier = Modifier.size(size * 0.64f),
        )
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = FlameOrange,
            modifier = Modifier.size(size * 0.38f),
        )
    }
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
