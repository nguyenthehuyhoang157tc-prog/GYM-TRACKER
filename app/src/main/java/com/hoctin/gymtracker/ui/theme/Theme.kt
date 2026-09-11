package com.hoctin.gymtracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Soft purple accent family used across the premium identity.
val PurplePrimary = Color(0xFF9D7BF4)
val PurpleBright = Color(0xFFB79CFF)
val PurpleDeep = Color(0xFF6D4BD8)
val PurpleSurface = Color(0xFF1C1730)

// Clean blue accent family for Light mode.
val LightBluePrimary = Color(0xFF2563EB)
val LightBlueBright = Color(0xFF3B82F6)
val LightBlueDeep = Color(0xFF1D4ED8)
val LightPrimaryContainer = Color(0xFFDCEBFF)
val LightOnPrimaryContainer = Color(0xFF082E6F)
val LightBackground = Color(0xFFF7FAFF)
val LightSurfaceVariant = Color(0xFFE8EEF7)

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
    // UI-1 Part A: OLED dark *gray* page (not pure black) so the DB-2E hard-shadow cards read as
    // raised against it. Cards sit slightly lighter than this; recessed wells sit darker.
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFF4F3F7),
    surface = Color(0xFF16181D),
    onSurface = Color(0xFFEDECF2),
    surfaceVariant = Color(0xFF22252C),
    onSurfaceVariant = Color(0xFF9B9DA8),
    surfaceContainerHighest = Color(0xFF20232B),
    outline = Color(0xFF2C2F38),
    outlineVariant = Color(0xFF202229),
    scrim = Color(0xFF000000),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0002),
)

private val LightColors = lightColorScheme(
    primary = LightBluePrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF8A6A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE6B6),
    onSecondaryContainer = Color(0xFF2A1F00),
    tertiary = Color(0xFF1F7D5C),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF526071),
    surfaceContainerHighest = Color(0xFFDCE6F3),
    outline = Color(0xFFC3CCD9),
    outlineVariant = Color(0xFFD8E0EA),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val GymTypography = Typography(
    // DB-2E Part B: titles/headlines carry more weight and slightly wider tracking for a stronger,
    // more premium feel. Body/label styles are left readable (unchanged). Sizes and line heights are
    // untouched, so layout does not shift.
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
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

private val LocalAuronDarkTheme = staticCompositionLocalOf { false }

@Composable
fun Modifier.auronScreenBackground(scrollOffsetPx: () -> Float = { 0f }): Modifier {
    // HISTORY-2 polish: dark screens open on a hollow-purple top gradient fading into TRUE OLED black
    // (#000000) toward the bottom — depth still comes from the cards' contrast + hard shadow (UI-1).
    // Light theme keeps the flat background. [scrollOffsetPx] kept for call-site compatibility.
    return if (isAuronDarkTheme()) {
        this.background(
            Brush.verticalGradient(
                0.0f to Color(0xFF32083F),
                0.34f to Color(0xFF17051F),
                0.58f to Color.Black,
                1.0f to Color.Black,
            ),
        )
    } else {
        this.background(MaterialTheme.colorScheme.background)
    }
}

@Composable
fun auronScreenTopPadding(extra: Dp = 16.dp): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + extra

/**
 * Standard card fill. Opaque charcoal (slightly lighter than the page) in Dark, clean surface in
 * Light. The [alpha] parameter is retained for call-site compatibility but no longer thins the fill
 * (the page is flat now, so cards stay solid for clean DB-2E depth).
 */
@Composable
fun auronGlassSurface(alpha: Float = 1f): Color = MaterialTheme.colorScheme.surface

/** Slightly lighter tonal surface for chips, segmented controls, grouped tiles and tonal buttons. */
@Composable
fun auronGlassSurfaceVariant(alpha: Float = 1f): Color = MaterialTheme.colorScheme.surfaceVariant

/**
 * LOG-V4A focused charcoal surfaces. These tokens intentionally do not replace the global Material
 * surface roles: only the Log and Profile/Settings workspaces opt into the darker PLAN-V3B.1 family.
 */
@Composable
fun auronCharcoalSurface(): Color =
    if (LocalAuronDarkTheme.current) Color(0xFF0D0E11) else MaterialTheme.colorScheme.surface

@Composable
fun auronCharcoalSurfaceRaised(): Color =
    if (LocalAuronDarkTheme.current) Color(0xFF111218) else MaterialTheme.colorScheme.surfaceVariant

/** Recessed "well" fill — darker than a card in Dark — for inset groups (e.g. grouped detail tiles). */
@Composable
fun auronInsetColor(): Color =
    if (LocalAuronDarkTheme.current) Color(0xFF0B0C0E) else MaterialTheme.colorScheme.surfaceVariant

/**
 * UI-1 Part A — shared DB-2E card depth. Paints a hard, blur-free OLED-black panel offset to the
 * bottom-right behind the card (same corner radius so it hugs the rounded corners). The card's own
 * opaque fill covers the top-left; the panel peeks out bottom/right, reading as a raised card without
 * any glow/blur. Dark mode only — Light mode keeps its flat, bordered cards unchanged.
 *
 * Apply on the card's own modifier and match [cornerRadius] to the card shape.
 */
@Composable
fun Modifier.auronCardElevation(cornerRadius: Dp = 22.dp): Modifier {
    if (!LocalAuronDarkTheme.current) return this
    return this.drawWithContent {
        val r = cornerRadius.toPx()
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(x = 3.dp.toPx(), y = 4.dp.toPx()),
            size = size,
            cornerRadius = CornerRadius(r, r),
        )
        drawContent()
    }
}

@Composable
fun auronAccentBright(): Color =
    if (LocalAuronDarkTheme.current) PurpleBright else LightBlueBright

@Composable
fun auronAccentDeep(): Color =
    if (LocalAuronDarkTheme.current) PurpleDeep else LightBlueDeep

@Composable
fun isAuronDarkTheme(): Boolean = LocalAuronDarkTheme.current

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAuronDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = GymShapes,
            typography = GymTypography,
            content = content,
        )
    }
}
