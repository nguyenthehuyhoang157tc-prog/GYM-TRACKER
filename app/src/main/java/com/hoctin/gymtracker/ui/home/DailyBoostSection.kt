package com.hoctin.gymtracker.ui.home

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hoctin.gymtracker.R
import com.hoctin.gymtracker.domain.DailyBoostExercise
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.DailyWinLens
import com.hoctin.gymtracker.ui.components.RestTimerSheet
import com.hoctin.gymtracker.ui.components.SectionLabel
import com.hoctin.gymtracker.ui.components.auronLiquidGlass
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.log.RestTimerUiState
import com.hoctin.gymtracker.ui.log.RestTimerUnit
import com.hoctin.gymtracker.ui.theme.auronGlassSurfaceVariant
import com.hoctin.gymtracker.ui.theme.auronInsetColor
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme

/** Fixed Daily Boost duration for the MVP (2:00). */
private const val BOOST_TOTAL_MS = 120_000L

// ---------------------------------------------------------------------------
// Daily Win accent (lime refresh) — scoped to this file only. The app-wide
// primary stays purple for other screens; Daily Win deliberately speaks in the
// same lime/green family as Muscle Recovery so the Home stack reads as one set.
// ---------------------------------------------------------------------------

private val DailyWinLime = Color(0xFFB6E24A)
private val DailyWinLimeLight = Color(0xFF5C8A1E) // deeper lime so light mode stays legible
private val DailyWinOnLime = Color(0xFF0F0F12) // dark glyph on lime fills (~12:1 contrast)

/** Daily Win accent: bright lime in dark mode, a deeper lime in light mode. */
@Composable
private fun dailyWinAccent(): Color = if (isAuronDarkTheme()) DailyWinLime else DailyWinLimeLight

/** Signature Daily Win gradient: lime → completion green (tertiary). */
@Composable
private fun dailyWinBrush(): Brush =
    Brush.linearGradient(listOf(dailyWinAccent(), MaterialTheme.colorScheme.tertiary))

// ---------------------------------------------------------------------------
// Home card
// ---------------------------------------------------------------------------

/**
 * The Daily Boost card on Home. Tuned to sit in the same design family as the Weekly Progress block:
 * a gradient identity medallion, a status chip, a grouped "selected boost" tile, and clear actions.
 * Daily Boost is an optional rest-day habit — it never feeds the weekly ring or History, but a
 * completed boost does count toward the shared **activity streak** (shown on the Home streak card).
 *
 * LOG-V4B: the card body is wrapped in [auronLiquidGlass]. The lens refracts the card's **own**
 * painted pixels (medallion, type, buttons, status chip) — it does not sample the Home canvas
 * behind it, so the layout tree and hit-testing stay unwarped. There are no secondary popups:
 * Start and Random both go straight to the shared [RestTimerSheet].
 */
@Composable
fun DailyBoostCard(
    exercise: DailyBoostExercise,
    completedToday: Boolean,
    onStart: () -> Unit,
    onRandom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One-shot entrance: fade + gentle rise, resolved in the draw phase via graphicsLayer.
    // rememberSaveable keeps it from replaying when Home flows re-emit (WhileSubscribed reset).
    var entered by rememberSaveable { mutableStateOf(false) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entered = true
        entrance.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing))
    }

    val darkTheme = isAuronDarkTheme()
    Card(
        shape = RoundedCornerShape(24.dp),
        // The translucent vertical gradient below paints the surface — the same panel language as
        // the Muscle Recovery / Weekly Progress boxes above — so the card itself stays clear and
        // tonal elevation is zeroed (no M3 primary tint bleeding through).
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = gymContainerBorder(alpha = if (darkTheme) 0.14f else 0.09f),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (entered) entrance.value else 0f
                translationY = (1f - entrance.value) * 24.dp.toPx()
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (darkTheme) 0.060f else 0.55f),
                            Color.White.copy(alpha = if (darkTheme) 0.018f else 0.20f),
                            Color.Transparent,
                        ),
                    ),
                )
                .clip(RoundedCornerShape(22.dp))
                // LOG-V4B: refracts this column's own pixels. Sampling is inward so the rim magnifies
                // instead of reading past the clipped layer. No-op below API 33 and when
                // AuronGlass.ENABLED is false - the gradient above is then the whole surface.
                .auronLiquidGlass(DailyWinLens)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: gradient medallion + title/subtitle + today's status chip. LOG-V4B removed the
            // "How it works" sheet, so the header is no longer a tap target.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoostMedallion()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Daily Win",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Keep your streak alive on rest days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BoostStatusChip(completedToday = completedToday)
            }

            // Grouped "selected boost" tile — a recessed well, same inset language as the
            // Recovery group tiles above. LOG-V4B removed the chooser sheet: picking a different
            // win now lives behind Random, so the row is a label with no chevron and no tap target.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(auronInsetColor())
                    .border(gymContainerBorder(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionLabel("Today's win")
                // Crossfade when the exercise changes (Random) so the swap feels alive.
                AnimatedContent(
                    targetState = exercise,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                    label = "dailyWinExercise",
                ) { ex ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = ex.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = ex.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Completion bar: empty while pending, animates smoothly to full once today's win is done.
            BoostProgressBar(completed = completedToday)

            // Exactly two actions: Start (filled lime) and Random (tonal, with a proper shuffle icon).
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BoostActionButton(
                    label = "Start",
                    icon = Icons.Filled.PlayArrow,
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    filled = true,
                )
                BoostActionButton(
                    label = "Random",
                    icon = BoostRandomIcon,
                    onClick = onRandom,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Slim completion bar for the Daily Win. Pending = an empty track; completed = a satisfying fill that
 * animates to 100% (lime → green sweep). Purely a visual completion indicator — no goals, no streak
 * logic, no gamification.
 */
@Composable
private fun BoostProgressBar(completed: Boolean) {
    val fill by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "boostCompletionFill",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(dailyWinAccent(), MaterialTheme.colorScheme.tertiary),
                        ),
                    ),
            )
        }
        Text(
            text = if (completed) "Win complete — see you tomorrow" else "Not done yet · about 2 minutes",
            style = MaterialTheme.typography.labelMedium,
            color = if (completed) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
/** Lime-gradient identity tile for Daily Win (echoes the Muscle Recovery accent family). */
@Composable
private fun BoostMedallion(size: Int = 52) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(dailyWinBrush()),
        contentAlignment = Alignment.Center,
    ) {
        // Lucide "trophy" (res/drawable/ic_daily_win_trophy.xml) — reads as "win" better than a bolt;
        // thin-stroke glyph suits the clean commercial look. Slightly oversized vs the old bolt
        // because the source icon keeps padding inside its 24dp viewport.
        Icon(
            painter = painterResource(R.drawable.ic_daily_win_trophy),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size / 2 + 2).dp),
        )
    }
}

/** "Done today" (green) once completed, otherwise a subtle "2 min" duration hint. */
@Composable
private fun BoostStatusChip(completedToday: Boolean) {
    if (completedToday) {
        GymBadge(
            text = "Done today",
            icon = Icons.Filled.Check,
            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
            content = MaterialTheme.colorScheme.tertiary,
        )
    } else {
        GymBadge(
            text = "2 min",
            icon = Icons.Outlined.Timer,
            container = dailyWinAccent().copy(alpha = 0.14f),
            content = dailyWinAccent(),
        )
    }
}

@Composable
private fun BoostActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    val container = if (filled) dailyWinAccent() else auronGlassSurfaceVariant()
    val content = if (filled) DailyWinOnLime else MaterialTheme.colorScheme.onSurface
    // iOS-style press feedback: a light springy dip layered on top of the ripple.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "boostButtonPress",
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(50))
            .background(container)
            .then(
                // Tonal variants pick up the same subtle theme border as the rest of the app.
                if (filled) Modifier else Modifier.border(gymContainerBorder(alpha = 0.12f), RoundedCornerShape(50)),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Choose bottom sheet
// ---------------------------------------------------------------------------
// ---------------------------------------------------------------------------
// Timer modal (full-screen)
// ---------------------------------------------------------------------------
/**
 * One-shot confetti burst when the countdown completes. Follows the shared ConfettiBurst pattern:
 * plays once, respects the system animator setting (static devices keep the badge-only completion),
 * and fades out at the end rather than resting on the (possibly transparent) final frame.
 */
@Composable
private fun DailyWinConfettiOverlay(modifier: Modifier = Modifier) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.auron_daily_win_confetti),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = motionEnabled,
        iterations = 1,
        restartOnPlay = false,
    )
    val fade by animateFloatAsState(
        targetValue = if (progress >= 1f) 0f else 1f,
        animationSpec = tween(durationMillis = 450, delayMillis = 250),
        label = "dailyWinConfettiFade",
    )
    if (motionEnabled) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier.graphicsLayer { alpha = fade },
        )
    }
}

// ---------------------------------------------------------------------------
// Random action glyph
// ---------------------------------------------------------------------------

/**
 * LOG-V4B replacement for `Icons.Outlined.Shuffle`.
 *
 * Material's shuffle puts an arrowhead at both ends of both shafts — four heads inside 20dp, which
 * collapses into a smudge at action-button size. This glyph keeps the same crossed-S shuffle
 * semantic but heads only the *outgoing* ends, so the shape stays legible and the weight matches
 * the hand-built medallion glyph above it.
 */
private val BoostRandomIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "BoostRandom",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
        .addPath(
            pathData = PathData {
                moveTo(3f, 7f)
                curveTo(8.5f, 7f, 9.5f, 17f, 15f, 17f)
                lineTo(21f, 17f)
            },
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            pathData = PathData {
                moveTo(18.2f, 14.2f)
                lineTo(21f, 17f)
                lineTo(18.2f, 19.8f)
            },
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            pathData = PathData {
                moveTo(3f, 17f)
                curveTo(8.5f, 17f, 9.5f, 7f, 15f, 7f)
                lineTo(21f, 7f)
            },
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .addPath(
            pathData = PathData {
                moveTo(18.2f, 4.2f)
                lineTo(21f, 7f)
                lineTo(18.2f, 9.8f)
            },
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        .build()
}

// ---------------------------------------------------------------------------
// Timer
// ---------------------------------------------------------------------------

/**
 * LOG-V4B Daily Win timer. It drives the fixed 2:00 countdown and renders the **shared**
 * [RestTimerSheet] at 60% height, so Daily Win and Log Workout cannot drift visually.
 *
 * Confetti is handed to the sheet as its [overlay] because a Dialog renders into its own window —
 * drawing it here would put it *behind* the sheet.
 */
@Composable
fun DailyWinTimerSheet(
    onCompleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }

    // Real elapsed time is accumulated per frame while running, so pause/resume continue from the
    // frozen value and the countdown cannot drift if a frame is dropped.
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (running && elapsedMs < BOOST_TOTAL_MS) {
            val now = withFrameNanos { it }
            elapsedMs = (elapsedMs + (now - last) / 1_000_000L).coerceAtMost(BOOST_TOTAL_MS)
            last = now
        }
        if (elapsedMs >= BOOST_TOTAL_MS && running) {
            running = false
            finished = true
            onCompleted() // marked complete exactly once, when the countdown naturally reaches 0:00
        }
    }

    val totalSeconds = (BOOST_TOTAL_MS / 1_000L).toInt()
    val state = RestTimerUiState(
        visible = true,
        totalSeconds = totalSeconds,
        remainingSeconds = ((BOOST_TOTAL_MS - elapsedMs) / 1_000L).toInt(),
        running = running,
        smartEnabled = false,
        inputValue = totalSeconds.toString(),
        unit = RestTimerUnit.SEC,
        advanceAfterRest = false,
    )

    RestTimerSheet(
        state = state,
        accent = dailyWinAccent(),
        title = "Daily Win",
        showSmartToggle = false,
        showDurationInput = false,
        showSkipContinue = false,
        maxHeightFraction = 0.60f,
        overlay = if (finished) {
            { DailyWinConfettiOverlay(Modifier.matchParentSize()) }
        } else {
            null
        },
        onSmartChanged = {},
        onInputChanged = {},
        onUnitChanged = {},
        onReset = { elapsedMs = 0L },
        onToggleRunning = { running = !running },
        onFinish = {
            onCompleted()
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}