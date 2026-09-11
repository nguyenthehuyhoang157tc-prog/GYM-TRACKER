package com.hoctin.gymtracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.muscleheat.BodyMapFigureCustom
import com.hoctin.gymtracker.ui.muscleheat.BodyMapSide
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private val RecoveryLimeAccent = Color(0xFFB6E24A)
private val RecoveryPanelHighlight = Color.White.copy(alpha = 0.06f)
private val RecoveryPanelBase = Color.White.copy(alpha = 0.018f)
private val RecoveryTileHighlight = Color.White.copy(alpha = 0.048f)
private val RecoveryTileBase = Color.White.copy(alpha = 0.012f)
private val RecoveryTrackDark = Color.White.copy(alpha = 0.085f)
private const val ReadinessBarEntranceDurationMillis = 1200
private val ReadinessBarBottomDockGuard = 96.dp

private enum class ReadinessBarAnimationPhase {
    WaitingForVisibility,
    Running,
    Settled,
}

private object HomeRecoveryReadinessAnimationGate {
    private var started = false

    fun hasStarted(): Boolean = started

    fun startIfPending(): Boolean {
        if (started) return false
        started = true
        return true
    }
}

/**
 * MUSCLE-4B — Home "Muscle Recovery" readiness dashboard. Redesigned from the MUSCLE-4 text-list card
 * into a premium OLED panel inspired by the provided "body strain" references (adapted to phone):
 *  - a hero readiness score + stage word,
 *  - the front/back body map as the hero visual on a near-black stage (recovery colors — a separate
 *    system from session heat: red/orange = fatigue still carried, neutral = recovered),
 *  - coarse body-group tiles with thin colored readiness bars + %,
 *  - intelligence nudges (limited muscles, plan overlap, neglected muscles) and a recovery legend.
 * Purely presentational — all readiness math lives in the recovery engine / ViewModel.
 */
@Composable
fun MuscleRecoverySection(
    state: MuscleRecoveryUiState,
    modifier: Modifier = Modifier,
    animationVisibleTrigger: Boolean = true,
) {
    val darkTheme = isAuronDarkTheme()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val viewportHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val viewportBottomAboveDockPx = viewportHeightPx - with(density) { ReadinessBarBottomDockGuard.toPx() }
    var readinessBarsVisible by remember { mutableStateOf(false) }
    var readinessBarAnimationPhase by remember {
        mutableStateOf(
            if (HomeRecoveryReadinessAnimationGate.hasStarted()) {
                ReadinessBarAnimationPhase.Settled
            } else {
                ReadinessBarAnimationPhase.WaitingForVisibility
            },
        )
    }
    val panelShape = RoundedCornerShape(28.dp)
    val panelBrush = if (darkTheme) {
        Brush.verticalGradient(
            0f to RecoveryPanelHighlight,
            0.34f to RecoveryPanelBase,
            1f to Color.Transparent,
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
            ),
        )
    }
    LaunchedEffect(state.hasData, animationVisibleTrigger, readinessBarsVisible) {
        if (!state.hasData) return@LaunchedEffect
        if (readinessBarAnimationPhase == ReadinessBarAnimationPhase.WaitingForVisibility &&
            HomeRecoveryReadinessAnimationGate.hasStarted()
        ) {
            readinessBarAnimationPhase = ReadinessBarAnimationPhase.Settled
            return@LaunchedEffect
        }
        if (readinessBarAnimationPhase == ReadinessBarAnimationPhase.WaitingForVisibility &&
            animationVisibleTrigger &&
            readinessBarsVisible &&
            HomeRecoveryReadinessAnimationGate.startIfPending()
        ) {
            readinessBarAnimationPhase = ReadinessBarAnimationPhase.Running
        }
    }
    LaunchedEffect(readinessBarAnimationPhase) {
        if (readinessBarAnimationPhase == ReadinessBarAnimationPhase.Running) {
            delay(ReadinessBarEntranceDurationMillis.toLong())
            readinessBarAnimationPhase = ReadinessBarAnimationPhase.Settled
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(panelBrush)
            .border(gymContainerBorder(alpha = if (darkTheme) 0.14f else 0.09f), panelShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
            // Header: title + READINESS pill.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Muscle Recovery",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "READINESS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = RecoveryLimeAccent,
                    modifier = Modifier
                        .background(RecoveryLimeAccent.copy(alpha = if (darkTheme) 0.08f else 0.14f), CircleShape)
                        .border(gymContainerBorder(alpha = 0.36f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }

            if (!state.hasData) {
                Text(
                    text = "No workouts in the last 7 days — every muscle is fresh and ready to train.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // Hero readiness score.
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${state.overallReadiness}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(state.overallArgb),
                )
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "% ${state.overallLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(state.overallArgb),
                    )
                    Text(
                        text = "Overall readiness",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Body-map hero stage (near-OLED inset with a thin edge).
            val stageShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(stageShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (darkTheme) 0.035f else 0.12f),
                                Color.Black.copy(alpha = if (darkTheme) 0.82f else 0.02f),
                            ),
                        ),
                    )
                    .border(gymContainerBorder(alpha = 0.13f), stageShape)
                    .padding(vertical = 14.dp, horizontal = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                ) {
                    BodyMapFigureCustom(
                        side = BodyMapSide.FRONT,
                        slugArgb = state.frontSlugArgb,
                        modifier = Modifier.weight(1f).height(238.dp),
                    )
                    BodyMapFigureCustom(
                        side = BodyMapSide.BACK,
                        slugArgb = state.backSlugArgb,
                        modifier = Modifier.weight(1f).height(238.dp),
                    )
                }
            }

            // Coarse group tiles — 2-column grid (manual pairs; 6 tiles, no nested scroll).
            state.groups.chunked(2).forEachIndexed { index, pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEachIndexed { tileIndex, tile ->
                        val trackVisibilityModifier = if (index == 0 && tileIndex == 0) {
                            Modifier.onGloballyPositioned { coordinates ->
                                if (!readinessBarsVisible) {
                                    val bounds = coordinates.boundsInWindow()
                                    val visibleTop = max(bounds.top, 0f)
                                    val visibleBottom = min(bounds.bottom, viewportBottomAboveDockPx)
                                    readinessBarsVisible = visibleBottom > visibleTop
                                }
                            }
                        } else {
                            Modifier
                        }
                        GroupTile(
                            tile = tile,
                            animationPhase = readinessBarAnimationPhase,
                            readinessTrackModifier = trackVisibilityModifier,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Intelligence nudges.
            if (state.nudges.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    state.nudges.forEach { nudge ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(nudge.argb)),
                            )
                            Text(
                                text = nudge.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            state.readyLine?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            RecoveryLegend()
    }
}

@Composable
private fun GroupTile(
    tile: RecoveryGroupTile,
    animationPhase: ReadinessBarAnimationPhase,
    modifier: Modifier = Modifier,
    readinessTrackModifier: Modifier = Modifier,
) {
    val darkTheme = isAuronDarkTheme()
    val tileShape = RoundedCornerShape(18.dp)
    val targetFraction = (tile.recoveryPercent / 100f).coerceIn(0f, 1f)
    val animatedFraction = remember(tile.name) {
        Animatable(
            if (animationPhase == ReadinessBarAnimationPhase.Settled) {
                targetFraction
            } else {
                0f
            },
        )
    }
    LaunchedEffect(tile.name, animationPhase, targetFraction) {
        when (animationPhase) {
            ReadinessBarAnimationPhase.WaitingForVisibility -> animatedFraction.snapTo(0f)
            ReadinessBarAnimationPhase.Running -> {
                animatedFraction.snapTo(0f)
                animatedFraction.animateTo(
                    targetValue = targetFraction,
                    animationSpec = tween(
                        durationMillis = ReadinessBarEntranceDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            ReadinessBarAnimationPhase.Settled -> animatedFraction.snapTo(targetFraction)
        }
    }
    val targetColor = Color(tile.argb)
    val trackColor = if (darkTheme) RecoveryTrackDark else MaterialTheme.colorScheme.surfaceVariant
    val fillColor = if (animationPhase == ReadinessBarAnimationPhase.Running && targetFraction > 0f) {
        lerp(trackColor, targetColor, (animatedFraction.value / targetFraction).coerceIn(0f, 1f))
    } else {
        targetColor
    }

    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (darkTheme) {
                    Brush.verticalGradient(
                        0f to RecoveryTileHighlight,
                        1f to RecoveryTileBase,
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                        ),
                    )
                },
            )
            .border(gymContainerBorder(alpha = if (darkTheme) 0.11f else 0.08f), tileShape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${tile.recoveryPercent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(tile.argb),
            )
        }
        // Thin readiness bar.
        Box(
            modifier = readinessTrackModifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(trackColor),
        ) {
            if (animatedFraction.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(fillColor),
                )
            }
        }
        Text(
            text = tile.statusLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecoveryLegend() {
    val items = listOf(
        "Ready" to 0xFF4ED07AL,
        "Primed" to 0xFFB6E24AL,
        "Recovering" to 0xFFF2C24BL,
        "Sore" to 0xFFF0813CL,
        "Rest" to 0xFFE0443CL,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(gymContainerBorder(alpha = 0.07f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { (label, argb) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(argb)),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
