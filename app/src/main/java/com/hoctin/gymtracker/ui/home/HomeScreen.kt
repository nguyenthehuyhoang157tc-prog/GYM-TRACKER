package com.hoctin.gymtracker.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.WorkoutDayWithExercises
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.theme.auronAccentBright
import com.hoctin.gymtracker.ui.theme.auronAccentDeep
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronGlassSurface
import com.hoctin.gymtracker.ui.theme.auronGlassSurfaceVariant
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import com.hoctin.gymtracker.ui.workoutDayLabel

private const val MuscleRecoveryItemKey = "home_muscle_recovery"

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onViewDetail: (Long) -> Unit,
    onViewConsistency: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val boost = viewModel.dailyBoost.collectAsStateWithLifecycle().value
    val recoveryState = viewModel.muscleRecovery.collectAsStateWithLifecycle().value
    val healthContext = viewModel.healthContext.collectAsStateWithLifecycle().value
    // LOG-V4B: the "How it works" sheet and the exercise chooser sheet are gone. Start and Random
    // both go straight to the shared timer, so one flag is all the card needs.
    var showBoostTimer by remember { mutableStateOf(false) }
    var playStreakCelebration by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val recoveryCardVisible by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            layoutInfo.visibleItemsInfo.any { item ->
                item.key == MuscleRecoveryItemKey &&
                    item.offset < layoutInfo.viewportEndOffset &&
                    item.offset + item.size > layoutInfo.viewportStartOffset
            }
        }
    }
    LaunchedEffect(
        state.consistency.ready,
        state.consistency.todayEpochDay,
        state.consistency.isTodayActive,
    ) {
        if (state.consistency.ready && viewModel.consumeStreakCelebration()) {
            playStreakCelebration = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground {
                listState.firstVisibleItemIndex * 160f + listState.firstVisibleItemScrollOffset.toFloat()
            },
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = auronScreenTopPadding(16.dp),
            bottom = BottomNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { BrandHeader() }

        item {
            HeroWorkoutCard(
                day = state.todayPlan,
                dayNumber = state.todayPlan?.let { tp ->
                    state.availableDays.indexOfFirst { it.day.id == tp.day.id } + 1
                } ?: 0,
                context = state.context,
                onViewDetail = { state.todayPlan?.let { onViewDetail(it.day.id) } },
            )
        }

        item {
            HomeConsistencyModule(
                state = state.consistency,
                playFireCelebration = playStreakCelebration,
                onViewMore = onViewConsistency,
            )
        }

        // MUSCLE-4: Muscle Recovery — which muscles still carry fatigue and which are ready today.
        item(key = MuscleRecoveryItemKey) {
            MuscleRecoverySection(
                state = recoveryState,
                animationVisibleTrigger = recoveryCardVisible,
            )
        }

        item(key = "home_health_shadow_context") {
            HealthRecoveryContextCard(
                state = healthContext,
                onRefresh = viewModel::refreshHealthContext,
            )
        }

        // Daily Boost: an optional ~2-minute rest-day habit below Recovery. It remains fully
        // separate from the workout-only Weekly Progress Ring above.
        item {
            DailyBoostCard(
                exercise = boost.exercise,
                completedToday = boost.completedToday,
                onStart = { showBoostTimer = true },
                // Random only swaps the selected win — it must NOT open the timer. Start is the
                // only action that begins the 2:00 countdown.
                onRandom = viewModel::randomizeDailyBoostExercise,
            )
        }

    }

    if (showBoostTimer) {
        DailyWinTimerSheet(
            onCompleted = viewModel::markDailyBoostCompleted,
            onDismiss = { showBoostTimer = false },
        )
    }
}

@Composable
private fun BrandHeader() {
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
}

@Composable
private fun HeroWorkoutCard(
    day: WorkoutDayWithExercises?,
    dayNumber: Int,
    context: WorkoutContext,
    onViewDetail: () -> Unit,
) {
    val totalSets = day?.exercises?.sumOf { it.targetSets } ?: 0
    val heroRes = heroResForDay(day?.day?.name)
    val heroShape = MaterialTheme.shapes.extraLarge
    val darkTheme = isAuronDarkTheme()
    val heroMid = if (darkTheme) Color(0xFF3A2A66) else Color(0xFF1E5ACF)
    val heroEnd = if (darkTheme) Color(0xFF120D22) else Color(0xFF0B1F4A)
    val heroScrim = if (darkTheme) Color(0xFF160F2B) else Color(0xFF071A3C)
    Card(
        shape = heroShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = gymContainerBorder(alpha = 0.09f),
        modifier = Modifier
            .fillMaxWidth()
            .auronCardElevation(28.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(heroShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            auronAccentDeep(),
                            heroMid,
                            heroEnd,
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
                            0f to heroScrim.copy(alpha = 0.72f),
                            0.5f to heroScrim.copy(alpha = 0.18f),
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
                // Subtle cross-fade on just the hero label + title when the selected workout changes
                // (small text elements only — no whole-screen transition).
                val contextLabel = when (context) {
                    WorkoutContext.TODAY -> "TODAY'S WORKOUT"
                    WorkoutContext.SELECTED -> "SELECTED WORKOUT"
                    WorkoutContext.NEXT -> "NEXT WORKOUT"
                }
                Crossfade(targetState = contextLabel, animationSpec = tween(200), label = "heroContext") { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = auronAccentBright(),
                        letterSpacing = 1.5.sp,
                    )
                }
                Crossfade(
                    targetState = if (day != null) workoutDayLabel(dayNumber, day.day.name) else "Plan not ready",
                    animationSpec = tween(220),
                    label = "heroTitle",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                }
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
                ViewDetailButton(enabled = day != null, onClick = onViewDetail)
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
private fun ViewDetailButton(enabled: Boolean, onClick: () -> Unit) {
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
        Text(
            text = "View Detail",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = auronAccentDeep(),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = auronAccentDeep(),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Measured geometry of one day chip in the chip-row coordinate space (px). Drives the sliding pill. */
private data class ChipBounds(val x: Float, val width: Float, val height: Float)

@Composable
private fun DayChipRow(
    days: List<WorkoutDayWithExercises>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    // A single primary "pill" slides behind the selected chip. We measure each chip's position/size
    // (stable after first layout, even inside horizontalScroll) and animate the pill's x/width to the
    // selected chip. Works for any number of workout days from the DB, not a fixed four.
    val chipBounds = remember { mutableStateMapOf<Long, ChipBounds>() }
    val selectedBounds = selectedId?.let { chipBounds[it] }

    val pillX by animateDpAsState(
        targetValue = with(density) { (selectedBounds?.x ?: 0f).toDp() },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dayPillX",
    )
    val pillWidth by animateDpAsState(
        targetValue = with(density) { (selectedBounds?.width ?: 0f).toDp() },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dayPillWidth",
    )
    val pillHeight = with(density) { (selectedBounds?.height ?: 0f).toDp() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            // Sliding indicator, drawn behind the chips. Hidden until the selected chip is measured.
            if (selectedBounds != null && pillWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = pillX)
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                days.forEach { day ->
                    DayChip(
                        label = day.day.name,
                        selected = day.day.id == selectedId,
                        onClick = { onSelect(day.day.id) },
                        onMeasured = { x, w, h -> chipBounds[day.day.id] = ChipBounds(x, w, h) },
                    )
                }
            }
        }
        // Skip / next workout day control.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(auronGlassSurfaceVariant())
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
    onMeasured: (x: Float, width: Float, height: Float) -> Unit,
) {
    // Selected chip's own background fades to transparent so the sliding primary pill shows through;
    // unselected chips keep their resting surface container. Text colour cross-fades.
    val container by animateColorAsState(
        targetValue = if (selected) Color.Transparent else auronGlassSurface(),
        animationSpec = tween(220),
        label = "dayChipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "dayChipContent",
    )
    Box(
        modifier = Modifier
            .onGloballyPositioned {
                val pos = it.positionInParent()
                onMeasured(pos.x, it.size.width.toFloat(), it.size.height.toFloat())
            }
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
