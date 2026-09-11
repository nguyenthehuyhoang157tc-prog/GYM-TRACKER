package com.hoctin.gymtracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Gold-bordered rounded container whose perimeter sweeps once with a brighter arc when it first
 * becomes visible in the window. The trigger is one-shot — once the row enters the viewport the
 * sweep plays to completion and never repeats. Pure decoration: no click handling, no logic.
 *
 * The sweep is rendered as a `drawArc` on the bounding box, which means the brightest segment will
 * ride over the rounded corners. This is intentional: the arc is a thin bright accent sitting on
 * top of a dimmer static border, so the corner misalignment reads as motion rather than a glitch.
 *
 * Use [content] for whatever should sit inside (text, icons, etc.). Content is drawn under the
 * sweep overlay, so a contrasting background is the host composable's responsibility.
 */
@Composable
fun GoldSweepBorder(
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFFF2C86B),
    sweepColor: Color = Color(0xFFFFE7A8),
    borderWidth: Dp = 1.dp,
    sweepArcDegrees: Float = 95f,
    sweepDurationMillis: Int = 1400,
    innerPadding: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val animatedStart = remember { Animatable(-sweepArcDegrees) }
    var visible by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible && !played) {
            played = true
            animatedStart.animateTo(
                targetValue = 360f,
                animationSpec = tween(sweepDurationMillis, easing = LinearEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                if (!visible) {
                    val bounds = coords.boundsInWindow()
                    val windowHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
                    val visibleTop = max(bounds.top, 0f)
                    val visibleBottom = min(bounds.bottom, windowHeightPx)
                    if (visibleBottom - visibleTop > 4f && coords.size.width > 0) {
                        visible = true
                    }
                }
            },
    ) {
        // Content first so the border overlay paints on top of it.
        if (innerPadding.value > 0f) {
            Box(modifier = Modifier.padding(innerPadding)) { content() }
        } else {
            content()
        }
        // Border overlay sized to match the content's measured size.
        Canvas(Modifier.matchParentSize()) {
            val w = borderWidth.toPx()
            val r = cornerRadius.toPx()
            val rect = Rect(
                offset = Offset(w / 2f, w / 2f),
                size = Size(size.width - w, size.height - w),
            )
            // Dim static base border so the structure reads even before the sweep arrives.
            drawRoundRect(
                color = borderColor.copy(alpha = 0.55f),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(max(r - w / 2f, 0f), max(r - w / 2f, 0f)),
                style = Stroke(width = w),
            )
            if (played) {
                // Bright arc that runs from -sweepArcDegrees (just above 12 o'clock) clockwise to 360°.
                drawArc(
                    color = sweepColor,
                    startAngle = animatedStart.value - 90f,
                    sweepAngle = sweepArcDegrees,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = w * 1.7f),
                )
            }
        }
    }
}