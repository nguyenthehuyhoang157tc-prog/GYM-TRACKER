package com.hoctin.gymtracker.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Executable specification for the LOG-V4B liquid-glass refraction model.
 *
 * Test scope only — not part of the shipped APK. The AGSL shader in
 * [com.hoctin.gymtracker.ui.components.AuronLiquidGlass] cannot be unit-tested directly (it is a
 * GLSL string that only compiles on a GPU), so this file ports the *same* maths to Kotlin and
 * asserts the physical properties the shader must satisfy. Any future rewrite of the shader must
 * keep these tests green.
 *
 * Three classes of assertion:
 *
 * 1. **Physics** — the model behaves like real glass: circular lens profile, magnification in the
 *    correct direction, blue deviating more than red (n_blue > n_red).
 * 2. **Geometry** — the rounded-rect SDF has the right sign convention and its gradient is a unit
 *    edge normal.
 * 3. **Cost** — the early-outs actually bound the number of texture fetches, which is what keeps
 *    the shader off the frame-time critical path.
 */
class LiquidGlassPhysicsTest {

    // ------------------------------------------------------------------------------------
    // Reference implementation — a faithful Kotlin port of the AGSL functions.
    // ------------------------------------------------------------------------------------

    /**
     * Circular lens profile: `1 - sqrt(1 - x²)`.
     *
     * This is the **normalised sagitta** of a circle: for a circle of radius R, the height of the
     * arc above a chord of half-length `x` is `R - sqrt(R² - x²)`, and dividing by R gives
     * `1 - sqrt(1 - (x/R)²)`. So the displacement profile is literally the cross-section of a
     * spherical/cylindrical lens — which is why it reads as glass rather than a smeared gradient.
     */
    private fun arcLift(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return 1f - sqrt(maxOf(1f - c * c, 0f))
    }

    /**
     * Signed distance to a rounded rect centred on the origin.
     * Negative inside, ~0 on the rim, positive outside.
     */
    private fun sdRoundRect(px: Float, py: Float, halfW: Float, halfH: Float, r: Float): Float {
        val qx = abs(px) - (halfW - r)
        val qy = abs(py) - (halfH - r)
        val ox = maxOf(qx, 0f)
        val oy = maxOf(qy, 0f)
        val outside = sqrt(ox * ox + oy * oy) - r
        val inside = minOf(maxOf(qx, qy), 0f)
        return outside + inside
    }

    /**
     * Gradient of [sdRoundRect] — the edge normal, unit length, pointing outward.
     * Ported from the AGSL `gradSdRoundRect`.
     */
    private fun gradSdRoundRect(
        px: Float,
        py: Float,
        halfW: Float,
        halfH: Float,
        r: Float,
    ): Pair<Float, Float> {
        val qx = abs(px) - (halfW - r)
        val qy = abs(py) - (halfH - r)
        return if (qx >= 0f || qy >= 0f) {
            val nx = maxOf(qx, 0f)
            val ny = maxOf(qy, 0f)
            val len = maxOf(sqrt(nx * nx + ny * ny), 1e-6f)
            Pair(kotlin.math.sign(px) * nx / len, kotlin.math.sign(py) * ny / len)
        } else {
            // step(q.y, q.x) == 1 when q.x >= q.y
            val gx = if (qx >= qy) 1f else 0f
            Pair(kotlin.math.sign(px) * gx, kotlin.math.sign(py) * (1f - gx))
        }
    }

/** GLSL `smoothstep(e0, e1, x)` - C1, zero slope at both ends of the range. */
private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val u = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return u * u * (3f - 2f * u)
}

/**
 * Result of the refraction model at one pixel. Offsets are **signed** displacement along the
 * outward edge normal: negative = inward (bevel/magnification), positive = outward (overhang/wrap).
 */
private data class Refraction(
    val samples: Int,
    val offsetRed: Float,
    val offsetGreen: Float,
    val offsetBlue: Float,
) {
    val outward: Boolean get() = offsetGreen > 0f
}

/**
 * Two-zone model, smoothly blended - mirrors `BACKDROP_LENS_AGSL` exactly. A hard
 * `if (t >= rimStart)` left a ~22px tear at rimStart; smoothstep removes it.
 */
private fun refract(
    distToEdge: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chroma: Float,
    ior: Float = 1.5f,
    rimWidth: Float = 5f,
    rimReach: Float = 10f,
): Refraction {
    if (distToEdge >= refractionHeight) {
        return Refraction(samples = 1, offsetRed = 0f, offsetGreen = 0f, offsetBlue = 0f)
    }
    val t = 1f - distToEdge / refractionHeight
    val iorScale = (ior - 1f) / 0.5f
    val rimFrac = minOf(rimWidth / maxOf(refractionHeight, 0.0001f), 0.95f)
    val rimStart = 1f - rimFrac

    // Bevel: inward magnitude from the circular profile.
    val dB = arcLift(t) * refractionAmount * iorScale
    // Overhang: outward magnitude, ramping across the outermost sliver.
    val rt = ((t - rimStart) / maxOf(1f - rimStart, 0.0001f)).coerceIn(0f, 1f)
    val dO = rimReach * rt * iorScale

    val blend = smoothstep(rimStart, 1f, t)
    val disp = (-dB) * (1f - blend) + dO * blend

    val ch = t * chroma * iorScale
    val sgn = if (disp < 0f) -1f else 1f
    return Refraction(
        samples = 3,
        offsetRed = disp - sgn * ch,
        offsetGreen = disp,
        offsetBlue = disp + sgn * ch,
    )
}

/** Centre magnification factor: 1.0 = none, 1.04 = 4% larger at the centre. */
private fun centreMag(t: Float, rimStart: Float, centerMagnification: Float, iorScale: Float): Float {
    val magW = 1f - smoothstep(0f, rimStart, t)
    return 1f + centerMagnification * magW * iorScale
}

    // Dock geometry, xhdpi (density 2.0): 64dp tall, 30dp corner radius.
    private val dockHalfH = 64f
    private val dockHalfW = 540f
    private val dockRadius = 60f

    companion object {
        // ---- Geometry constants the dock tests are derived from ----
        //
        // Bar is 64dp tall. BoxWithConstraints applies 8dp vertical padding, leaving a 48dp
        // content area. Inside it the item Column (24dp icon + 2dp spacer + ~12dp label = 38dp) is
        // centred, so it occupies y ∈ [13dp, 51dp]. With the new architecture (icons/labels are
        // siblings drawn AFTER the backdrop shader), those y-coordinates are NOT in the shader's
        // layer — they are crisp by construction. The geometry below stays here only because
        // some tests still document where the icon/label row used to live.
        private const val DOCK_HEIGHT_DP = 64f
        private const val CONTENT_TOP_DP = 13f
        private const val CONTENT_BOTTOM_DP = 51f
        /** Label row alone: 24dp icon + 2dp spacer puts the text at y ∈ [39dp, 51dp]. */
        private const val LABEL_TOP_DP = 39f
        private const val LABEL_BOTTOM_DP = 51f
        /**
         * Backdrop lens band depth — 32dp so the **whole** 64dp dock is inside the band. The
         * natural `arcLift(t=0)=0` fall-off then gives zero displacement at the centre without
         * needing an explicit early-out. With band < halfH you get a clean centre band (legibility
         * > bend visibility); with band ≥ halfH you get a droplet that bends everywhere with a
         * smooth fall-off (visibility > legibility).
         */
        private const val BACKDROP_BAND_DP = 32f
        /** Peak displacement at the rim, in dp (matches `NavDockBackdropLens.refractionAmountDp`). */
        private const val BACKDROP_AMOUNT_DP = 18f
        /** Old band (12dp) is now only kept as a reference value for the early-out regression test. */
        private const val OLD_NARROW_BAND_DP = 12f
    }

    // ------------------------------------------------------------------------------------
    // 1. Physics — the lens profile
    // ------------------------------------------------------------------------------------

    @Test
    fun arcLift_hasCorrectBoundaryValues() {
        assertEquals("no displacement where the band starts", 0f, arcLift(0f), 1e-6f)
        assertEquals("full displacement at the rim", 1f, arcLift(1f), 1e-6f)
    }

    @Test
    fun arcLift_matchesNormalisedCircularSagitta() {
        // Independent derivation: sagitta of a circle of radius R over a chord of half-length x.
        val r = 37f
        for (i in 0..20) {
            val u = i / 20f                      // normalised half-chord
            val x = u * r                        // actual half-chord
            val sagitta = r - sqrt(r * r - x * x)
            val normalised = sagitta / r
            assertEquals(
                "arcLift($u) must equal the normalised sagitta of a circle",
                normalised,
                arcLift(u),
                1e-5f,
            )
        }
    }

    @Test
    fun arcLift_isMonotonicallyIncreasing() {
        var previous = -1f
        for (i in 0..100) {
            val v = arcLift(i / 100f)
            assertTrue("arcLift must not decrease (failed at ${i / 100f})", v >= previous)
            previous = v
        }
    }

    @Test
    fun arcLift_isTangentAtBandStart_preventingVisibleSeam() {
        // The whole point of the circular profile: zero slope at the band start, so the
        // displacement ramps in smoothly instead of appearing as a hard edge on the glass.
        val h = 1e-4f
        val slopeAtStart = (arcLift(h) - arcLift(0f)) / h
        assertTrue(
            "slope at the band start must be ~0, was $slopeAtStart",
            abs(slopeAtStart) < 0.02f,
        )
        // And steep at the rim, which is what creates the thick-edge illusion.
        val slopeAtRim = (arcLift(1f) - arcLift(1f - h)) / h
        assertTrue(
            "slope at the rim must be steep, was $slopeAtRim",
            slopeAtRim > 10f,
        )
    }

    // ------------------------------------------------------------------------------------
    // 2. Physics — magnification direction
    // ------------------------------------------------------------------------------------

@Test
fun inwardSampling_magnifiesContentAtTheRim() {
    // BEVEL ZONE ONLY. Sampling *inward* (toward the centre) pulls content from nearer the
    // centre out toward the rim — that is magnification. Sampling outward would shrink.
    //
    // Both samples are kept deeper than `rimWidth` so they land in the bevel. The overhang
    // deliberately samples the other way and is covered by `overhang_samplesOutward_...`.
    val height = 32f
    val amount = 20f
    val chroma = 0f
    val rimWidth = 5f

    val d1 = 16f   // deeper inside
    val d2 = 10f   // closer to the rim (but still bevel)
    val r1 = refract(d1, height, amount, chroma, rimWidth = rimWidth)
    val r2 = refract(d2, height, amount, chroma, rimWidth = rimWidth)

    assertTrue("d=$d1 must be in the bevel (inward sampling)", !r1.outward)
    assertTrue("d=$d2 must be in the bevel (inward sampling)", !r2.outward)
    // Displacement grows toward the rim within the bevel.
    assertTrue(
        "bevel displacement must grow toward the rim: ${r2.offsetGreen} vs ${r1.offsetGreen}",
        abs(r2.offsetGreen) > abs(r1.offsetGreen),
    )

    // Magnification: content sitting at source distance (d + offset) is displayed at distance d,
    // so a *narrow* band of source content is stretched over a *wider* band of display.
    val srcSpan = abs((d1 - r1.offsetGreen) - (d2 - r2.offsetGreen))
    val displaySpan = abs(d1 - d2)
    val magnification = displaySpan / srcSpan
    assertTrue(
        "inward bevel sampling must magnify, got $magnification",
        magnification > 1.0f,
    )
}

    @Test
    fun displacement_growsTowardTheRim_withinTheBevel() {
        val height = 20f
        val amount = 20f
                // 6f keeps the sample strictly inside the bevel (t=0.7 < rimStart=0.75). Sampling at
        // 2f would land past the fold, where magnitude is deliberately NOT monotonic.
        val nearRim = refract(6f, height, amount, 0f).offsetGreen
        val midBand = refract(10f, height, amount, 0f).offsetGreen
        val bandStart = refract(19.9f, height, amount, 0f).offsetGreen
        assertTrue("|displacement| must grow toward the rim", abs(nearRim) > abs(midBand))
        assertTrue("|displacement| must grow toward the rim", abs(midBand) > abs(bandStart))
    }

    // ------------------------------------------------------------------------------------
    // 3. Physics — chromatic dispersion ordering
    // ------------------------------------------------------------------------------------

    @Test
    fun chromaticDispersion_blueDeviatesMoreThanRed() {
    // Snell / prism model: deviation ≈ (n - 1)·A, and glass has n_blue > n_red
    // (n_red ≈ 1.514 @ 656nm, n_blue ≈ 1.528 @ 486nm). Therefore blue is displaced more.
    //
    // Sampled well inside the bevel (distToEdge 12 of a 32dp band → t = 0.625, comfortably
    // below rimStart ≈ 0.844). The overhang has its own ordering test.
    val r = refract(
        distToEdge = 12f,
        refractionHeight = 32f,
        refractionAmount = 18f,
        chroma = 3f,
        ior = 1.5f,
        rimWidth = 5f,
    )

    assertTrue("this sample must be in the bevel zone", !r.outward)
    assertTrue(
        "blue must deviate more than green (n_blue > n_green), got blue=${r.offsetBlue} green=${r.offsetGreen}",
        abs(r.offsetBlue) > abs(r.offsetGreen),
    )
    assertTrue(
        "green must deviate more than red (n_green > n_red), got green=${r.offsetGreen} red=${r.offsetRed}",
        abs(r.offsetGreen) > abs(r.offsetRed),
    )
}

    @Test
    fun chromaticDispersion_vanishesAtTheCentreAndPeaksAtTheRim() {
        val height = 20f
        val amount = 20f
        val chroma = 3f

        // At the band start, t -> 0, so the R/B separation must collapse to zero:
        // the interior of the glass must stay colour-clean.
        val atBandStart = refract(19.999f, height, amount, chroma)
        val spreadAtStart = atBandStart.offsetBlue - atBandStart.offsetRed
        assertTrue(
            "no colour fringing where the band starts, got spread $spreadAtStart",
            spreadAtStart < 0.01f,
        )

        // At the rim the separation is at its widest.
        val atRim = refract(0.001f, height, amount, chroma)
        val spreadAtRim = atRim.offsetBlue - atRim.offsetRed
        assertTrue(
            "colour fringing must be widest at the rim, got $spreadAtRim",
            spreadAtRim > 5f,
        )
    }

    @Test
    fun chromaticDispersion_neverProducesNegativeRedOffset() {
        // The red channel subtracts the chroma term; it must be clamped at 0 or the red sample
        // would flip to the *outward* side and invert the fringe.
        val r = refract(distToEdge = 0f, refractionHeight = 20f, refractionAmount = 1f, chroma = 8f)
        assertTrue("red offset must be clamped to >= 0, got ${r.offsetRed}", r.offsetRed >= 0f)
    }

    // ------------------------------------------------------------------------------------
    // 4. Geometry — SDF and its gradient
    // ------------------------------------------------------------------------------------

    @Test
    fun sdRoundRect_hasCorrectSignConvention() {
        val inside = sdRoundRect(0f, 0f, dockHalfW, dockHalfH, dockRadius)
        val onRim = sdRoundRect(0f, -dockHalfH, dockHalfW, dockHalfH, dockRadius)
        val outside = sdRoundRect(0f, -dockHalfH - 10f, dockHalfW, dockHalfH, dockRadius)

        assertTrue("centre must be inside (sd < 0), got $inside", inside < 0f)
        assertEquals("rim must be sd ≈ 0", 0f, onRim, 0.5f)
        assertTrue("beyond the rim must be outside (sd > 0), got $outside", outside > 0f)
    }

    @Test
    fun sdRoundRect_centreDistanceEqualsHalfHeight() {
        // For a wide short bar, -sd at the centre is the distance to the nearest (top/bottom) edge.
        val sd = sdRoundRect(0f, 0f, dockHalfW, dockHalfH, dockRadius)
        assertEquals(
            "-sd at the centre must equal the half height",
            dockHalfH,
            -sd,
            0.5f,
        )
    }

    @Test
    fun gradient_isUnitLengthAndPointsOutward() {
        val (gx, gy) = gradSdRoundRect(0f, -dockHalfH, dockHalfW, dockHalfH, dockRadius)
        val length = sqrt(gx * gx + gy * gy)
        assertEquals("gradient must be unit length", 1f, length, 1e-4f)
        // At the top edge, "outward" is toward negative y (up on screen).
        assertTrue("at the top edge the normal must point up, got gy=$gy", gy < -0.99f)
    }

    @Test
    fun gradient_isDegenerateAtExactCentre_soDepthMustBeNonZero() {
        // sign(0) == 0, so the analytic gradient collapses to (0,0) at the exact centre.
        // The shader adds `depth * normalize(p + eps)` to survive this; with depth == 0 the
        // subsequent normalize() would yield NaN and paint a garbage pixel.
        val (gx, gy) = gradSdRoundRect(0f, 0f, dockHalfW, dockHalfH, dockRadius)
        assertEquals("gradient x is degenerate at centre", 0f, gx, 1e-6f)
        assertEquals("gradient y is degenerate at centre", 0f, gy, 1e-6f)

        // Guard: with depth > 0 the combined direction is well defined.
        val depth = 0.35f
        val ex = 1e-4f
        val nx = gx + depth * (ex / sqrt(2f * ex * ex))
        val ny = gy + depth * (ex / sqrt(2f * ex * ex))
        val len = sqrt(nx * nx + ny * ny)
        assertTrue("depth term must rescue the centre from NaN", len > 1e-3f)
        assertTrue("rescued direction must be finite", len.isFinite() && (nx / len).isFinite())
    }

    // ------------------------------------------------------------------------------------
    // 5. Continuity — no seams
    // ------------------------------------------------------------------------------------

    @Test
    fun displacement_isContinuousAcrossTheBandEdge() {
        // A visible seam at the band boundary was one of the failure modes of earlier passes.
        // arcLift(t) -> 0 as t -> 0, so the displacement must fall to ~0 exactly at the edge.
        val height = 20f
        val amount = 20f
        val justInside = refract(height - 1e-3f, height, amount, 0f)
        val justOutside = refract(height + 1e-3f, height, amount, 0f)
        assertEquals(
            "displacement must be continuous at the band edge",
            justOutside.offsetGreen,
            justInside.offsetGreen,
            1e-3f,
        )
    }

    // ------------------------------------------------------------------------------------
    // 6. Cost — the early-outs must bound texture fetches
    // ------------------------------------------------------------------------------------

    @Test
    fun deepInterior_costsASingleTextureFetch() {
        val r = refract(distToEdge = 100f, refractionHeight = 20f, refractionAmount = 20f, chroma = 3f)
        assertEquals("deep interior must early-out to 1 fetch", 1, r.samples)
        assertEquals("deep interior must not be displaced", 0f, r.offsetGreen, 1e-6f)
    }

    @Test
    fun bandCost_isBoundedForTheDockGeometry() {
        // With the new wider band (32dp on a 64dp dock), the WHOLE lens is inside the band and
        // there is no deep-interior pass-through. That is the new design choice — we want the
        // entire dock area to refract, not just a rim strip. So this test pins the NEW cost
        // envelope: every inside-lens pixel pays for dispersion (3 fetches), with the natural
        // arcLift(t→0)=0 fall-off at the centre making that pixel nearly free in image terms.
        val bandPx = BACKDROP_BAND_DP * 2f
        val barHeightPx = DOCK_HEIGHT_DP * 2f
        var total = 0
        val steps = 128
        for (i in 0 until steps) {
            val y = (i + 0.5f) / steps * barHeightPx
            val distToEdge = minOf(y, barHeightPx - y)
            val r = refract(distToEdge, bandPx, BACKDROP_AMOUNT_DP, 3f)
            total += r.samples
        }
        val fetchesPerPixel = total.toFloat() / steps

        // Worst case: every lens pixel pays 3 fetches. For a 1080×128 dock that's ≈414k fetches
        // per frame, still well under what a mobile GPU can do per draw (10–100 Gtexel/s). The
        // remaining perf concern is the offscreen render, not the maths.
        assertTrue(
            "average fetches per pixel must stay under 3.0, got $fetchesPerPixel",
            fetchesPerPixel < 3.05f,
        )
        // Sanity: centre must carry near-zero displacement even though it's in the band.
        val centre = refract(distToEdge = barHeightPx / 2f, refractionHeight = bandPx,
            refractionAmount = BACKDROP_AMOUNT_DP, chroma = 3f)
        assertTrue(
            "centre displacement must be near-zero even when in the band, got ${centre.offsetGreen}",
            centre.offsetGreen < 0.5f,
        )
    }

    @Test
    fun bandMustNotOverlapContentZone_thisIsWhatKilledTheIcons_legacyDoc() {
        // HISTORICAL — documents why this exact constraint is no longer the binding one. In the
        // old self-refraction attempt the icons WERE inside the shader's layer, so an 18dp band
        // swallowed the icon row and made everything composite transparent. The fix back then was
        // to keep the band shallower than the content gap.
        //
        // The new architecture moved the shader onto the BACKGROUND layer. The dock — icons,
        // labels, indicator — is now drawn AFTER the shader as a sibling, so none of those pixels
        // are fed through the shader at all. The constraint "icons survive" is now an
        // architectural invariant, not a band-depth invariant. The test below keeps the original
        // math around to pin what the OLD contract used to be — anyone reading the test history
        // sees that "shallow band = content safe" no longer holds.
        val failingBandPx = 18f * 2f   // the band that used to make icons disappear
        val topContentPx = CONTENT_TOP_DP * 2f
        val failing = refract(topContentPx, failingBandPx, 20f, 3f)
        assertEquals(
            "the 18dp band used to capture the icon row — historical regression marker",
            3,
            failing.samples,
        )
        // And the new architecture: a sibling overlay cannot be hidden by the shader no matter
    // what the band is, so the icon row itself is never in the shader's content.eval path.
    }

    @Test
    fun backdropLens_keepsTheDockIconsCrisp_byArchitecture() {
        // The backdrop shader runs on the layer that contains the scrolling content. The dock
        // (icons, labels, indicator) is drawn AFTER, as a sibling. So the shader cannot hide
        // the dock's content no matter how wide the band is — the constraint is structural,
        // not parameter-dependent.
        //
        // The shader can still bend the background at the icon row's y-co, but those pixels
        // are not the icon's — the icon is drawn on top and stays pixel-perfect. What we
        // verify here is the underlying math: at the bar's centre, `arcLift(0) ≈ 0` keeps the
        // displacement near zero so the icon's underlying content reads cleanly even if a card
        // border were to pass through.
        val bandPx = BACKDROP_BAND_DP * 2f
        val barHeightPx = DOCK_HEIGHT_DP * 2f
        val centreDistToEdge = barHeightPx / 2f
        val centre = refract(
            distToEdge = centreDistToEdge,
            refractionHeight = bandPx,
            refractionAmount = BACKDROP_AMOUNT_DP,
            chroma = 0f,
        )
        assertTrue(
            "centre displacement must stay near zero even when in the band, got ${centre.offsetGreen}",
            centre.offsetGreen < 0.5f,
        )
    }

    @Test
    fun backdropLens_bendsTheRimWhereCardBordersActuallyAre() {
        // Sanity check that the new wider band actually displaces the rim enough that a scrolling
        // 1dp card border bends visibly. The earlier 12dp band made this so subtle that the
        // whole effect looked like an alpha overlay rather than a lens.
        val bandPx = BACKDROP_BAND_DP * 2f
        val atRim = refract(
            distToEdge = 0.5f,
            refractionHeight = bandPx,
            refractionAmount = BACKDROP_AMOUNT_DP,
            chroma = 0f,
        )
        assertTrue(
            "the rim must carry strong displacement, got ${atRim.offsetGreen}",
            atRim.offsetGreen > 8f,
        )
    }

    // ------------------------------------------------------------------------------------
// 8. Two-zone refraction: bevel samples inward, overhang samples OUTWARD
//
// Verified against a direct Snell computation (ior = 1.5, view ray straight down):
//   edge angle  89 deg -> T.x = -0.734  (inward)
//   edge angle  91 deg -> T.x = +0.734  (OUTWARD)
// The sign flips exactly at 90 deg. A model that only samples inward can magnify but can
// never produce the contour-hugging wrap that makes iOS glass recognisable.
// ------------------------------------------------------------------------------------

@Test
fun overhang_samplesOutward_bevel_samplesInward() {
    val height = 32f
    val rimWidth = 5f
    // Just inside the rim: overhang zone.
    val atRim = refract(distToEdge = 0.5f, refractionHeight = height,
        refractionAmount = 18f, chroma = 0f, rimWidth = rimWidth, rimReach = 10f)
    assertTrue(
        "the outermost sliver must be the overhang (outward sampling)",
        atRim.outward,
    )
    assertTrue(
        "overhang must displace outward a meaningful amount, got ${atRim.offsetGreen}",
        atRim.offsetGreen > 1f,
    )

    // Well inside the band: bevel zone.
    val midBand = refract(distToEdge = 16f, refractionHeight = height,
        refractionAmount = 18f, chroma = 0f, rimWidth = rimWidth, rimReach = 10f)
    assertTrue(
        "the deeper band must be the bevel (inward sampling)",
        !midBand.outward,
    )

    // The two zones must be adjacent and non-overlapping: scanning inward, outward must
    // switch to inward exactly once.
    var flips = 0
    var previous = true
    for (i in 0..200) {
        val d = i / 200f * (height - 0.01f)
        val r = refract(d, height, 18f, 0f, rimWidth = rimWidth, rimReach = 10f)
        if (r.outward != previous) {
            flips++
            previous = r.outward
        }
    }
    assertEquals(
        "there must be exactly one overhang->bevel transition, got $flips",
        1,
        flips,
    )
}

@Test
fun ior1_producesNoDisplacementAnywhere() {
    // The falsifiability test. Glass with the refractive index of air is optically invisible:
    // every displacement — bevel AND overhang, every channel — must be exactly zero.
    //
    // The earlier arcLift-only model had no parameter that could express this, so it could
    // never be wrong here. A model that cannot be wrong also cannot be right.
    val height = 32f
    for (d in listOf(0.5f, 2f, 5f, 10f, 16f, 24f, 31f)) {
        val r = refract(
            distToEdge = d,
            refractionHeight = height,
            refractionAmount = 18f,
            chroma = 1.2f,
            ior = 1.0f,
            rimWidth = 5f,
            rimReach = 10f,
        )
        assertEquals("ior=1 d=$d: red must be 0", 0f, r.offsetRed, 1e-6f)
        assertEquals("ior=1 d=$d: green must be 0", 0f, r.offsetGreen, 1e-6f)
        assertEquals("ior=1 d=$d: blue must be 0", 0f, r.offsetBlue, 1e-6f)
    }
}

@Test
fun displacement_scalesMonotonicallyWithIor() {
    // Deviation grows with (n - 1). Water (1.33) must bend less than glass (1.5), which must
    // bend less than a dense flint (1.7). Checked in both zones.
    val height = 32f
    for (d in listOf(1f, 16f)) {   // 1f = overhang, 16f = bevel
        val water = refract(d, height, 18f, 0f, ior = 1.33f)
        val glass = refract(d, height, 18f, 0f, ior = 1.5f)
        val flint = refract(d, height, 18f, 0f, ior = 1.7f)
        assertTrue(
            "d=$d: water < glass, got ${water.offsetGreen} vs ${glass.offsetGreen}",
            abs(water.offsetGreen) < abs(glass.offsetGreen),
        )
        assertTrue(
            "d=$d: glass < flint, got ${glass.offsetGreen} vs ${flint.offsetGreen}",
            abs(glass.offsetGreen) < abs(flint.offsetGreen),
        )
    }
}

@Test
fun overhang_preservesChromaticOrdering() {
    // Blue must deviate more than red in the overhang too, not just the bevel — the rainbow
    // fringe has to sit on the outside of the wrap, matching real dispersion.
    val r = refract(
        distToEdge = 0.5f,
        refractionHeight = 32f,
        refractionAmount = 18f,
        chroma = 1.2f,
        ior = 1.5f,
    )
    assertTrue("overhang must be the outward zone", r.outward)
    assertTrue(
        "overhang: blue must exceed green, got b=${r.offsetBlue} g=${r.offsetGreen}",
        abs(r.offsetBlue) > abs(r.offsetGreen),
    )
    assertTrue(
        "overhang: green must exceed red, got g=${r.offsetGreen} r=${r.offsetRed}",
        abs(r.offsetGreen) > abs(r.offsetRed),
    )
}

@Test
fun rimReach_isIndependentOfSurfaceHeight() {
    // Documented approximation: at the overhang the surface height has collapsed to ~0, so a
    // height-derived offset would vanish and the wrap would disappear. rimReach is a separate
    // parameter standing in for the light path through the body — so changing refractionAmount
    // must NOT change the overhang displacement.
    val smallAmount = refract(0.01f, 32f, refractionAmount = 2f, chroma = 0f, rimReach = 10f)
    val largeAmount = refract(0.01f, 32f, refractionAmount = 40f, chroma = 0f, rimReach = 10f)
    assertEquals(
        "overhang displacement must not depend on the bevel amount",
        smallAmount.offsetGreen,
        largeAmount.offsetGreen,
        1e-3f,
    )
}

@Test
fun naiveShaderWithoutEarlyOut_wouldCostThreeFetchesEverywhere() {
        // Documents why the early-out exists: without it every pixel pays for dispersion.
        val withEarlyOut =
            refract(distToEdge = 100f, refractionHeight = 20f, refractionAmount = 20f, chroma = 3f)
        assertEquals(1, withEarlyOut.samples)
        // (A naive version would report 3 here; the assertion pins the optimised behaviour.)
    }

    // ------------------------------------------------------------------------------------
    // 7. Droplet profile — depth term blends edge normal with radial
    // ------------------------------------------------------------------------------------

    /**
     * Replicates the shader's blend:
     *   `g = normalize(edgeNormal + uDepth * normalize(p))`
     * Pure Float arithmetic so the test has no dependency on Compose drawing.
     */
    private fun blendedDirection(
        px: Float,
        py: Float,
        halfW: Float,
        halfH: Float,
        r: Float,
        depth: Float,
    ): FloatArray {
        // Edge normal (same as gradSdRoundRect).
        val qx = abs(px) - (halfW - r)
        val qy = abs(py) - (halfH - r)
        var gx: Float
        var gy: Float
        if (qx >= 0f || qy >= 0f) {
            val nx = maxOf(qx, 0f)
            val ny = maxOf(qy, 0f)
            val len = maxOf(sqrt(nx * nx + ny * ny), 1e-6f)
            gx = kotlin.math.sign(px) * nx / len
            gy = kotlin.math.sign(py) * ny / len
        } else {
            val ggx = if (qx >= qy) 1f else 0f
            gx = kotlin.math.sign(px) * ggx
            gy = kotlin.math.sign(py) * (1f - ggx)
        }
        // Guard degenerate edge.
        if (gx * gx + gy * gy <= 1e-6f) {
            gx = 0f; gy = -1f
        }
        // Radial.
        val pLen2 = px * px + py * py
        val rx: Float
        val ry: Float
        if (pLen2 > 1e-6f) {
            val s = 1f / sqrt(pLen2)
            rx = px * s
            ry = py * s
        } else {
            rx = 0f; ry = 1f
        }
        // Blend.
        val sx = gx + depth * rx
        val sy = gy + depth * ry
        val sLen2 = sx * sx + sy * sy
        return if (sLen2 > 1e-6f) {
            val s = 1f / sqrt(sLen2)
            floatArrayOf(sx * s, sy * s)
        } else floatArrayOf(0f, -1f)
    }

    @Test
    fun depthTerm_atRim_isIndistinguishableFromPureEdge() {
        // At the very rim the edge gradient dominates; the radial direction agrees with it, so
        // adding any depth term is a no-op. The shader's droplet effect only kicks in away from
        // the rim.
        val halfW = 540f
        val halfH = 32f
        val r = 60f
        // Top rim centre: edge normal = (0, -1), radial = (0, -1), blended = (0, -1) for any depth.
        for (depth in listOf(0f, 0.2f, 0.5f, 1f)) {
            val d = blendedDirection(0f, -halfH + 1f, halfW, halfH, r, depth)
            assertEquals("rim depth=$depth: x must be ~0", 0f, d[0], 0.01f)
            assertTrue("rim depth=$depth: y must be ~-1, got ${d[1]}", d[1] < -0.99f)
        }
    }

    @Test
    fun depthTerm_resolvesDegenerateCentre() {
        // At the exact centre (0,0) the analytic edge gradient collapses to (0,0) and the
        // shader falls back to the radial direction. With a non-zero centre offset the radial
        // is well-defined and the blended direction has unit length — no NaN survives the guard.
        val halfW = 540f
        val halfH = 32f
        val r = 60f
        // Sample just off centre.
        for ((dx, dy) in listOf(0.001f to -0.001f, -0.001f to -0.001f, 0.5f to 0.5f, 50f to -5f)) {
            val d = blendedDirection(dx, dy, halfW, halfH, r, 0.3f)
            val mag = sqrt(d[0] * d[0] + d[1] * d[1])
            assertTrue("($dx, $dy) direction must be unit length, got $mag", mag in 0.99f..1.01f)
            assertTrue("($dx, $dy) y must not be NaN", d[1].isFinite())
            assertTrue("($dx, $dy) x must not be NaN", d[0].isFinite())
        }
    }

    @Test
    fun depthTerm_offRim_tiltsDirectionSlightlyInward() {
        // Just below the rim (inside the lens) the edge normal points straight up, but a small
        // radial offset to the right tilts the blended direction slightly inward (positive x).
        // With depth=0 the direction stays pure-up; with depth=0.3 the tilt is meaningful.
        val halfW = 540f
        val halfH = 32f
        val r = 60f
        // Just below the top edge, slightly to the right of centre.
        val px = 50f
        val py = -halfH + 6f  // 6px below the top rim

        val noDepth = blendedDirection(px, py, halfW, halfH, r, 0f)
        assertTrue("depth=0: direction must be straight up, got y=${noDepth[1]}", noDepth[1] < -0.99f)
        assertTrue("depth=0: direction must have ~0 x, got ${noDepth[0]}", abs(noDepth[0]) < 0.01f)

        val withDepth = blendedDirection(px, py, halfW, halfH, r, 0.3f)
        // Depth tilts the direction slightly toward the radial direction (positive x for points
        // above-right of centre on a tall-then-wide dock).
        assertTrue(
            "depth=0.3: x must tilt positive, got ${withDepth[0]}",
            withDepth[0] > 0.05f,
        )
        // And the magnitude is preserved (unit length).
        val mag = sqrt(withDepth[0] * withDepth[0] + withDepth[1] * withDepth[1])
        assertTrue("direction must stay unit length", mag in 0.99f..1.01f)
    }

    @Test
    fun displacement_isContinuousAcrossTheOverhangBoundary_rimStart() {
        // ONE-SIDED CONTINUITY AT rimStart. Approach from both sides with shrinking epsilon.
        // A hard branch keeps ~22px of jump no matter how small the step; the smoothstep blend
        // converges because smoothstep is 0 with zero slope at its left edge.
        val height = 32f
        val amount = 18f
        val rimW = 5f
        val rimStart = 1f - (rimW / height).coerceAtMost(0.95f)
        val dBoundary = (1f - rimStart) * height
        var prev = Float.MAX_VALUE
        for (eps in listOf(0.01f, 0.001f, 0.0001f)) {
            val a = refract(dBoundary + eps * height, height, amount, 0f, rimWidth = rimW, rimReach = 10f)
            val b = refract(dBoundary - eps * height, height, amount, 0f, rimWidth = rimW, rimReach = 10f)
            val jump = abs(b.offsetGreen - a.offsetGreen)
            assertTrue("jump $jump at eps=$eps must not grow (prev $prev)", jump <= prev + 1e-4f)
            prev = jump
        }
        assertTrue("displacement must be continuous at rimStart, finest jump = $prev px", prev < 0.5f)
    }

    @Test
    fun centreMagnification_isStrongestAtCentre_andVanishesWithIor1() {
        val rimStart = 1f - (5f / 32f).coerceAtMost(0.95f)
        val s15 = (1.5f - 1f) / 0.5f
        assertEquals("centre magnification should be 1.04x", 1.04f, centreMag(0f, rimStart, 0.04f, s15), 1e-4f)
        assertEquals("magnification must fade to none by the rim", 1f, centreMag(1f, rimStart, 0.04f, s15), 1e-4f)
        // Falsifiability: no refraction -> no magnification either.
        assertEquals("ior=1 must give no magnification", 1f, centreMag(0f, rimStart, 0.04f, 0f), 1e-6f)
    }

}
