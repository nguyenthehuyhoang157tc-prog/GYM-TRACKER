package com.hoctin.gymtracker.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.catalog.thumbnailRefFor
import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.ui.format.MeasurementValue
import com.hoctin.gymtracker.ui.format.SetDisplay
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.BottomNavSpace
import com.hoctin.gymtracker.ui.catalog.CatalogDetailHost
import com.hoctin.gymtracker.ui.catalog.rememberCatalogDetailController
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.review.formatDurationMillis
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding
import com.hoctin.gymtracker.ui.toClockTime
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * HISTORY-2 — the Workout History archive. Month-based: a swipeable month selector (year over month,
 * neighbors peeking at the edges; pages ordered newest → oldest), each month a clean list of workouts
 * with a compact WORKOUT / VOLUME / REPS table — no expand/collapse cards. Tapping a workout opens the
 * Muscle Heat Review directly. Empty months show the thinking illustration.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    catalogRepository: ExerciseCatalogRepository,
    distanceUnitSystem: DistanceUnitSystem = DistanceUnitSystem.METRIC,
    onOpenSession: (Long) -> Unit = {},
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground(),
    ) {
        Text(
            text = "Workout History",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = auronScreenTopPadding(16.dp)),
        )

        val months = state.months
        if (months.isEmpty()) {
            // Initial load only — the ViewModel always emits at least the current month.
            Box(modifier = Modifier.fillMaxSize())
            return@Column
        }

        val pagerState = rememberPagerState(pageCount = { months.size })
        MonthHeader(
            months = months,
            pagerState = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(72.dp),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val month = months[page]
            if (month.sessions.isEmpty()) {
                EmptyMonthState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 14.dp,
                        bottom = BottomNavSpace,
                    ),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    items(month.sessions, key = { it.id }) { session ->
                        MonthSessionItem(
                            session = session,
                            distanceUnitSystem = distanceUnitSystem,
                            onClick = { onOpenSession(session.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The swipe-synced year/month selector: the current month sits centered with the adjacent months
 * peeking at the screen edges, translating live with the pager drag. Tapping a peek animates to it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthHeader(
    months: List<HistoryMonth>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val slotPx = with(LocalDensity.current) { (maxWidth * 0.62f).toPx() }
        val current = pagerState.currentPage
        val offset = pagerState.currentPageOffsetFraction
        for (i in (current - 1)..(current + 1)) {
            if (i !in months.indices) continue
            val month = months[i]
            val rel = (i - current) - offset
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = rel * slotPx
                        alpha = (1f - 0.62f * abs(rel).coerceAtMost(1.2f)).coerceAtLeast(0.15f)
                    }
                    .clickable(enabled = i != current) {
                        scope.launch { pagerState.animateScrollToPage(i) }
                    },
            ) {
                Text(
                    text = month.yearLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = month.monthLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}

/** Empty-month state (mockup): centered thinking illustration + caption. */
@Composable
private fun EmptyMonthState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.empty_history),
            contentDescription = null,
            modifier = Modifier.size(170.dp),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Interesting, you don't have any workouts for this period.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(96.dp)) // optical centering above the floating dock
    }
}

/**
 * One workout in the month list (mockup): bold day title, uppercase time/date/duration subtitle, then
 * a compact exercise table — rounded dark header row (WORKOUT / VOLUME / REPS) and tight rows with no
 * per-exercise cards. Tapping anywhere opens the Muscle Heat Review. Time/distance exercises show
 * meaningful duration/distance values, never fake reps/kg.
 */
@Composable
private fun MonthSessionItem(
    session: HistorySession,
    distanceUnitSystem: DistanceUnitSystem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = session.dayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = sessionSubtitle(session),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        // Header row in a dark rounded box.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthTableCell("WORKOUT", weight = 1.7f, header = true, alignEnd = false)
            MonthTableCell("VOLUME", weight = 1f, header = true)
            MonthTableCell("REPS", weight = 1f, header = true)
        }
        session.exercises.forEach { exercise ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1.7f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (exercise.hasPr) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = "Personal record",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                MonthTableCell(exerciseVolumeCell(exercise, distanceUnitSystem), weight = 1f)
                MonthTableCell(exerciseRepsCell(exercise, distanceUnitSystem), weight = 1f)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MonthTableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    alignEnd: Boolean = true,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
        color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        letterSpacing = if (header) 0.8.sp else 0.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** "5:58 PM, FEBRUARY 7TH 2026 — 3 HOURS, 6 MINS" (duration only when persisted). */
private fun sessionSubtitle(session: HistorySession): String {
    val date = LocalDate.ofEpochDay(session.dateEpochDay)
    val monthName = date.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val base = "${session.startedAtMillis.toClockTime()}, $monthName ${ordinalDay(date.dayOfMonth)} ${date.year}"
    val duration = session.wallDurationSeconds?.let { formatDurationMillis(it * 1000L) }
    return (if (duration != null) "$base — $duration" else base).uppercase(Locale.ENGLISH)
}

private fun ordinalDay(day: Int): String = when {
    day in 11..13 -> "${day}th"
    day % 10 == 1 -> "${day}st"
    day % 10 == 2 -> "${day}nd"
    day % 10 == 3 -> "${day}rd"
    else -> "${day}th"
}

/** VOLUME cell: rep work → total kg (N/A for pure bodyweight); time/distance → distance or duration. */
private fun exerciseVolumeCell(exercise: HistoryExercise, system: DistanceUnitSystem): String = when {
    exercise.isNonRep && exercise.distanceMeters > 0.0 ->
        MeasurementFormatter.distance(exercise.distanceMeters, system).inline()
    exercise.isNonRep && exercise.durationSeconds > 0 ->
        MeasurementFormatter.duration(exercise.durationSeconds).inline()
    exercise.isNonRep -> "—"
    exercise.totalVolume > 0.0 -> "${formatVolume(exercise.totalVolume)} kg"
    else -> "N/A"
}

/** REPS cell: rep work → total reps; time/distance → duration (when distance took the volume cell). */
private fun exerciseRepsCell(exercise: HistoryExercise, system: DistanceUnitSystem): String = when {
    !exercise.isNonRep -> "${exercise.totalReps} reps"
    exercise.distanceMeters > 0.0 && exercise.durationSeconds > 0 ->
        MeasurementFormatter.duration(exercise.durationSeconds).inline()
    else -> "—"
}

@Composable
internal fun ExerciseBlock( // MUSCLE-3: internal so the Session Review screen reuses the exact same breakdown
    exercise: HistoryExercise,
    imageRef: String?,
    onInfo: () -> Unit,
    distanceUnitSystem: DistanceUnitSystem,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(gymContainerBorder(alpha = 0.08f), MaterialTheme.shapes.medium)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseThumbnail(
                imageRef = imageRef,
                exerciseName = exercise.name,
                size = 44.dp,
            )
            Text(
                text = exercise.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Exercise info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onInfo),
            )
            if (exercise.hasPr) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "Personal record",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Text(
            text = if (exercise.isNonRep) {
                SetDisplay.summaryLine(
                    setCount = exercise.setCount,
                    totalReps = exercise.totalReps,
                    totalVolume = exercise.totalVolume,
                    durationSeconds = exercise.durationSeconds,
                    distanceMeters = exercise.distanceMeters,
                    hasRepSets = exercise.hasRepSets,
                    system = distanceUnitSystem,
                )
            } else {
                SetDisplay.summaryLine(
                    setCount = exercise.setCount,
                    totalReps = exercise.totalReps,
                    totalVolume = exercise.totalVolume,
                    durationSeconds = exercise.durationSeconds,
                    distanceMeters = exercise.distanceMeters,
                    hasRepSets = exercise.hasRepSets,
                    system = distanceUnitSystem,
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (exercise.isNonRep) {
            // DB-6C: time/distance set — render the stored value(s) inline (no fake reps=0 / volume / RIR).
            exercise.sets.forEach { s ->
                NonRepSetRow(
                    setNumber = s.setNumber,
                    measurements = SetDisplay.nonRepMeasurements(
                        s.durationSeconds, s.distanceMeters, s.weight, distanceUnitSystem,
                    ),
                    hasDuration = s.durationSeconds != null,
                    isPr = s.isPr,
                )
            }
        } else {
            TableRow(set = "SET", volume = "VOLUME", reps = "REPS", rir = "RIR", isHeader = true)
            exercise.sets.forEach { s ->
                TableRow(
                    set = s.setNumber.toString(),
                    volume = "${ProgressiveOverloadEngine.formatWeight(s.weight)} kg",
                    reps = s.reps.toString(),
                    rir = s.rir.toString(),
                    isHeader = false,
                    isPr = s.isPr,
                )
            }
        }
    }
}

/** DB-6C: one time/distance set in History — "SET n" then the inline value(s) (e.g. `40 kg • 20 m`). */
@Composable
private fun NonRepSetRow(
    setNumber: Int,
    measurements: List<com.hoctin.gymtracker.ui.format.Measurement>,
    hasDuration: Boolean,
    isPr: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Set $setNumber",
            modifier = Modifier.weight(0.6f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.weight(2.4f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            measurements.forEachIndexed { index, m ->
                if (index > 0) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MeasurementValue(m, valueStyle = MaterialTheme.typography.bodyMedium)
            }
            if (hasDuration) {
                Text(
                    text = " duration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isPr) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "PR",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    set: String,
    volume: String,
    reps: String,
    rir: String,
    isHeader: Boolean,
    isPr: Boolean = false,
) {
    val style = if (isHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(0.9f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = set, style = style, color = color, letterSpacing = if (isHeader) 0.6.sp else 0.sp)
            if (isPr) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = "PR",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Text(text = volume, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End, style = style, color = color)
        Text(text = reps, modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = style, color = color)
        Text(text = rir, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = style, color = color)
    }
}

private fun formatVolume(value: Double): String = "%,.0f".format(value)
