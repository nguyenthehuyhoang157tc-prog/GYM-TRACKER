package com.hoctin.gymtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Soft purple accent family used across the premium identity.
val PurplePrimary = Color(0xFF9D7BF4)
val PurpleBright = Color(0xFFB79CFF)
val PurpleDeep = Color(0xFF6D4BD8)
val PurpleSurface = Color(0xFF1C1730)

// Supporting accents for badges / highlights.
private val GoldAccent = Color(0xFFF2C879)
private val GreenAccent = Color(0xFF5FD3A3)

private val DarkColors = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color(0xFF1A0F33),
    primaryContainer = PurpleSurface,
    onPrimaryContainer = Color(0xFFE3D7FF),
    secondary = GoldAccent,
    onSecondary = Color(0xFF2A1F00),
    secondaryContainer = Color(0xFF2A2210),
    onSecondaryContainer = Color(0xFFF7DCA0),
    tertiary = GreenAccent,
    onTertiary = Color(0xFF06231A),
    tertiaryContainer = Color(0xFF11281F),
    onTertiaryContainer = Color(0xFFB8F0D8),
    // OLED-friendly: pure black canvas, near-black raised cards.
    background = Color(0xFF000000),
    onBackground = Color(0xFFF4F3F7),
    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFEDECF2),
    surfaceVariant = Color(0xFF1A1C22),
    onSurfaceVariant = Color(0xFF9B9DA8),
    surfaceContainerHighest = Color(0xFF20232B),
    outline = Color(0xFF2C2F38),
    outlineVariant = Color(0xFF202229),
    scrim = Color(0xFF000000),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0002),
)

private val LightColors = lightColorScheme(
    primary = PurpleDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DEFF),
    onPrimaryContainer = Color(0xFF22115C),
    secondary = Color(0xFF8A6A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE6B6),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF1F7D5C),
    onTertiary = Color.White,
    background = Color(0xFFF7F5FC),
    onBackground = Color(0xFF17151F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17151F),
    surfaceVariant = Color(0xFFEDEAF4),
    onSurfaceVariant = Color(0xFF565360),
    surfaceContainerHighest = Color(0xFFE7E3F1),
    outline = Color(0xFFCAC4D6),
    outlineVariant = Color(0xFFE2DEEC),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val GymTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.6.sp,
    ),
)

private val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = GymShapes,
        typography = GymTypography,
        content = content,
    )
}
