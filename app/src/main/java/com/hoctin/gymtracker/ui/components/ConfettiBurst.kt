package com.hoctin.gymtracker.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hoctin.gymtracker.R

/**
 * Full-screen confetti burst shown once after a workout is saved. The composition plays to its
 * final frame and rests there. Does not block touches (it has no overlay semantics), so users
 * can scroll the body map / heat card underneath while the burst is animating.
 *
 * Respects the global motion preference: when system animators are disabled, only the final frame
 * is rendered.
 */
@Composable
fun ConfettiBurst(
    modifier: Modifier = Modifier,
    fillSize: Dp? = null,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.auron_save_confetti),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = motionEnabled,
        iterations = 1,
        speed = 1f,
        restartOnPlay = false,
    )
    val sizeModifier = if (fillSize != null) Modifier.fillMaxSize() else Modifier
    Box(modifier = modifier.then(sizeModifier)) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
        )
    }
}