package com.hoctin.gymtracker.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A circular progress ring with an optional centered [content] slot.
 *
 * The fill animates smoothly toward [progress]. The animated value is read **inside the Canvas draw
 * lambda**, so progress changes only re-run the draw phase of this small composable — no
 * recomposition of the surrounding screen, and no continuous/per-frame work when idle.
 *
 * @param progress target fill in the range 0f..1f (values outside are clamped).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 92.dp,
    strokeWidth: Dp = 10.dp,
    trackColor: Color,
    progressBrush: Brush,
    content: @Composable () -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "progressRing",
    )
    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            // Track (full circle background).
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Progress arc — reading animated.value here keeps the animation in the draw phase.
            val sweep = animated.value * 360f
            if (sweep > 0f) {
                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
