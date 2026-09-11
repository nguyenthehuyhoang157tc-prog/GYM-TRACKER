package com.hoctin.gymtracker.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.hoctin.gymtracker.R

internal data class OnboardingPalette(
    val canvasTop: Color,
    val canvasMiddle: Color,
    val canvasBottom: Color,
    val ambientViolet: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hairline: Color,
    val selectedFill: Color,
    val unselectedFill: Color,
    val panelFill: Color,
    val selectedBorder: Color,
    val primaryButton: Color,
    val primaryButtonText: Color,
    val error: Color,
)

@Composable
internal fun onboardingPalette(): OnboardingPalette {
    // ONBOARDING-V3 intentionally uses one canonical dark commercial canvas in both app themes.
    // This keeps the first-run flow visually stable while the rest of AURON retains theme choice.
    return OnboardingPalette(
        canvasTop = Color(0xFF1B0324),
        canvasMiddle = Color(0xFF0B0410),
        canvasBottom = Color.Black,
        ambientViolet = Color(0xFF8F27F4).copy(alpha = 0.32f),
        textPrimary = Color(0xFFFCF9FF),
        textSecondary = Color(0xFFC5BACB),
        hairline = Color.White.copy(alpha = 0.14f),
        selectedFill = Color(0xFF1D1422).copy(alpha = 0.96f),
        unselectedFill = Color(0xFF0D0A10).copy(alpha = 0.94f),
        panelFill = Color(0xFF100C13).copy(alpha = 0.88f),
        selectedBorder = Color(0xFFB98AF4),
        primaryButton = Color(0xFFFCFAFD),
        primaryButtonText = Color(0xFF120815),
        error = Color(0xFFFF6E78),
    )
}

/** Onboarding-local atmospheric canvas. The static texture avoids per-frame procedural noise. */
@Composable
internal fun OnboardingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = onboardingPalette()
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.canvasTop, palette.canvasMiddle, palette.canvasBottom),
                    endY = size.height,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.ambientViolet, Color.Transparent),
                    center = Offset(0f, size.height * 0.15f),
                    radius = size.maxDimension * 0.72f,
                ),
                radius = size.maxDimension * 0.72f,
                center = Offset(0f, size.height * 0.15f),
            )
        }
        Image(
            painter = painterResource(R.drawable.auron_dark_texture),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = 0.045f,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
        )
        content()
    }
}
