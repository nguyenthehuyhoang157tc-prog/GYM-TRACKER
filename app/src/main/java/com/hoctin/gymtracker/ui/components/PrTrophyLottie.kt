package com.hoctin.gymtracker.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hoctin.gymtracker.R
import kotlin.math.max
import kotlin.math.min

/**
 * Tiny reward Lottie in place of the static trophy icon. Plays the composition once to its final
 * frame, then rests there.
 *
 * The animation is gated on first visibility in the window — same pattern the muscle-recovery
 * bars use. This way, rows below the fold don't spend their animation budget while still
 * off-screen, and reopening the screen plays the burst fresh.
 *
 * Respects the global motion preference: when system animators are disabled, only the final frame
 * is rendered.
 */
@Composable
fun PrTrophyLottie(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.auron_pr_trophy),
    )
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    var visible by remember { mutableStateOf(false) }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = visible && motionEnabled,
        iterations = 1,
        speed = 1f,
        restartOnPlay = false,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
            .size(size)
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
    )
}