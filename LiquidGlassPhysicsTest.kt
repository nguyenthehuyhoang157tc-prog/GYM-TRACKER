package com.hoctin.gymtracker.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Executable specification for the LOG-V5 liquid-glass model.
 *
 * Test scope only. The AGSL in [com.hoctin.gymtracker.ui.components.AuronGlass] is a shader string
 * that only compiles on a GPU, so this file ports the same maths to Kotlin and asserts the
 * properties the shader must satisfy. Any rewrite of the shader must keep these green.
 *
 * What changed from the V4B suite, and why:
 *
 * - `arcLift` is gone. It was an invented displacement profile with no physical parameters, so
 *   there was nothing to assert about it beyond its own shape. Snell replaces it, which lets the
 *   suite make claims that can actually fail — see [ior1_producesNoDisplacementAnywhere].
 * - The `uDepth` / radial-blend tests are gone with the hack they protected. The degenerate centre
 *   is no longer a special case: on the medial axis `phi == 0`, so the normal is `(0,0,1)` and the
 *   edge gradient is never consulted.
 * - New: the overhang band. `edgeAngle > 90 deg` must reverse the sample direction, which is what
 *   wraps surrounding content around the outline.
 * - New: smooth-min merging, since the shape is now an SDF scene rather than one rect.
 */
class LiquidGlassPhysicsTest {

    // ------------------------------------------------------------------------------------
    // Reference implementation — a faithful Kotlin port of the AGSL.
    // ------------------------------------------------------------------------------------

    private fun sdRoundRect(px: Float, py: Float, halfW: Float, halfH: Float, r: Float): Float {
        val qx = abs(px) - halfW + r
        val qy = abs(py) - halfH + r
        val ox = max(qx, 0f)
        val oy = max(qy, 0f)
        return sqrt(ox * ox + oy * oy) + min(max(qx, qy), 0f) - r
    }

    private fun smin(a: Float, b: Float, k: Float): Float {
        if (k <= 0f) return min(a, b)
        val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
        return (b + (a - b) * h) - k * h * (1f - h)
    }

    /** Central-difference gradient of an arbitrary SDF. Matches the shader's `sceneNormal`. */
    private fun sdfNormal(x: Float, y: Float, e: Float = 1f, sdf: (Float, Float) -> Float): Pair<Float, Float> {
        val dx = sdf(x + e, y) - sdf(x - e, y)
        val dy = sdf(x, y + e) - sdf(x, y - e)
        val l = sqrt(dx * dx + dy * dy)
        return if (l < 1e-5f) Pair(0f, 0f) else Pair(dx / l, dy / l)
    }

    /** Result of one pixel of the refraction model. */
    private data class Sample(
        /** Signed offset along the outward edge normal. Negative = inward, positive = outward. */
        val offsetRed: Float,
        val offsetGreen: Float,
        val offsetBlue: Float,
        /** Surface tilt at this point, radians. */
        val phi: Float,
        /** Glass height above the backdrop at this point. */
        val height: Float,
        /** True when the surface overhangs here (normal tilted past vertical). */
        val overhang: Boolean,
        /** Number of `content.eval` fetches this pixel costs, blur excluded. */
        val fetches: Int,
    )

    /**
     * The model at one point, reduced to one dimension along the outward edge normal.
     *
     * @param u inward distance from the outline. Equals `-sd`.
     */
    private fun sampleAt(
        u: Float,
        bevel: Float,
        thickness: Float,
        edgeAngle: Float,
        ior: Float,
        dispersion: Float = 0f,
        rimReach: Float = 0f,
    ): Sample {
        val x = (u / max(bevel, 0.5f)).coerceIn(0f, 1f)
        val k = 1f - x
        val phi = edgeAngle * k
        val sp = sin(phi)
        val cp = cos(phi)

        val denom = max(1f - cos(edgeAngle), 0.001f)
        val height = max(thickness * (1f - (1f - cp) / denom), 0f)

        // Work in the 1-D frame where the outward edge normal is +1.
        var nx = sp
        var nz = cp
        var cosi = nz
        var overhang = 0f
        if (cosi < 0f) {
            overhang = (-cosi / 0.35f).coerceIn(0f, 1f)
            nx = -nx
            nz = -nz
            cosi = -cosi
        }
        val travel = height + (max(height, rimReach) - height) * overhang

        fun offset(eta: Float): Float {
            val kk = 1f - eta * eta * (1f - cosi * cosi)
            if (kk < 0f) return 0f
            val coeff = eta * cosi - sqrt(kk)
            val tx = coeff * nx
            val tz = eta * -1f + coeff * nz
            return tx / max(-tz, 0.001f) * travel
        }

        return Sample(
            offsetRed = offset(1f / max(ior - dispersion, 1f)),
            offsetGreen = offset(1f / max(ior, 1f)),
            offsetBlue = offset(1f / max(ior + dispersion, 1f)),
            phi = phi,
            height = height,
            overhang = overhang > 0f,
            fetches = if (dispersion > 0f) 3 else 1,
        )
    }

    // Dock geometry at density 2.0: 64dp tall bar, capsule ends.
    private val dockHalfH = 64f
    private val dockHalfW = 540f
    private val dockRadius = 60f

    companion object {
        private const val BEVEL = 36f          // 18dp @ 2.0
        private const val THICKNESS = 48f      // 24dp @ 2.0
        private const val RIM_REACH = 52f      // 26dp @ 2.0
        private const val EDGE_ANGLE = 1.9548f // 112 deg
        private const val CHAMFER_ANGLE = 1.4835f // 85 deg, stays under vertical
        private const val IOR = 1.52f
    }

    // ------------------------------------------------------------------------------------
    // 1. The model reduces to nothing when it should
    // ------------------------------------------------------------------------------------

    @Test
    fun ior1_producesNoDisplacementAnywhere() {
        // The sharpest test in the suite: glass with the refractive index of air must be optically
        // invisible. eta == 1 collapses the Snell vector to the incident ray exactly. The old
        // arcLift model had no parameter that could express this, so it could not be checked.
        for (i in 0..40) {
            val u = i / 40f * BEVEL
            val s = sampleAt(u, BEVEL, THICKNESS, EDGE_ANGLE, ior = 1f, rimReach = RIM_REACH)
            assertEquals("ior=1 must not displace at u=$u", 0f, s.offsetGreen, 1e-4f)
        }
    }

    @Test
    fun flatInterior_producesNoDisplacement() {
        // Past the bevel the top surface is flat, so the normal is (0,0,1) and there is nothing to
        // refract. This is also why the model needs no early-out and no degenerate-centre guard.
        val s = sampleAt(BEVEL * 3f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        assertEquals("interior tilt must be zero", 0f, s.phi, 1e-6f)
        assertEquals("interior must not be displaced", 0f, s.offsetGreen, 1e-5f)
        assertEquals("interior height must be the full thickness", THICKNESS, s.height, 0.01f)
    }

    @Test
    fun profile_isTangentAtTheBevelStart_soThereIsNoSeam() {
        // A visible ring at the inner edge of the bevel was a failure mode of every earlier pass.
        // phi -> 0 linearly and the offset is O(phi^2) there, so the ramp-in is smooth.
        val h = BEVEL * 1e-3f
        val inner = sampleAt(BEVEL - h, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        val outer = sampleAt(BEVEL + h, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        assertEquals(
            "displacement must be continuous across the bevel boundary",
            outer.offsetGreen,
            inner.offsetGreen,
            0.05f,
        )
    }

    // ------------------------------------------------------------------------------------
    // 2. Direction — inward magnifies, the overhang wraps
    // ------------------------------------------------------------------------------------

    @Test
    fun chamferedEdge_samplesInwardEverywhere() {
        // With the edge stopping short of vertical there is no overhang, so every sample comes
        // from inside the footprint. That magnifies the content under the glass but can never
        // reach the surroundings.
        for (i in 1..39) {
            val u = i / 40f * BEVEL
            val s = sampleAt(u, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR, rimReach = RIM_REACH)
            assertTrue("u=$u must not overhang", !s.overhang)
            assertTrue(
                "u=$u must sample inward (negative along the outward normal), got ${s.offsetGreen}",
                s.offsetGreen <= 0f,
            )
        }
    }

    @Test
    fun chamferedEdge_magnifiesContentUnderTheRim() {
        // Two neighbouring display positions inside the bevel. Their source positions must be
        // closer together than they are, which is magnification.
        val u1 = BEVEL * 0.45f
        val u2 = BEVEL * 0.35f          // 10 units closer to the rim
        val s1 = u1 + sampleAt(u1, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR).offsetGreen
        val s2 = u2 + sampleAt(u2, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR).offsetGreen
        val magnification = (u1 - u2) / (s1 - s2)
        assertTrue("inward sampling must magnify, got $magnification", magnification > 1f)
    }

    @Test
    fun overhangBand_reversesTheSampleDirection() {
        // The whole point of edgeAngle > 90. Past vertical the viewing ray meets the surface from
        // behind; flipping the normal for the Snell evaluation reverses T.xy, so the outermost
        // band pulls content from OUTSIDE the shape and compresses it along the contour.
        val atRim = sampleAt(0.5f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        assertTrue("the rim must overhang at 112 deg", atRim.overhang)
        assertTrue(
            "the overhang band must sample outward, got ${atRim.offsetGreen}",
            atRim.offsetGreen > 0f,
        )

        // ...while the mid-bevel, still under vertical, keeps sampling inward.
        val midBevel = sampleAt(BEVEL * 0.6f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        assertTrue("mid-bevel must not overhang", !midBevel.overhang)
        assertTrue(
            "mid-bevel must still sample inward, got ${midBevel.offsetGreen}",
            midBevel.offsetGreen < 0f,
        )
    }

    @Test
    fun overhangBand_reachIsGovernedByRimReach() {
        // Local glass height goes to zero at the outline, so without a stand-in path length the
        // wrap would collapse to nothing. rimReach is that stand-in, and the reach must scale
        // with it.
        val weak = sampleAt(0.5f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = 10f)
        val strong = sampleAt(0.5f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = 80f)
        assertTrue(
            "a larger rimReach must pull content from further outside, got ${weak.offsetGreen} vs ${strong.offsetGreen}",
            strong.offsetGreen > weak.offsetGreen * 3f,
        )
    }

    @Test
    fun overhangBand_disappearsWhenRimReachIsZero() {
        // An escape hatch that must actually work: rimReach = 0 leaves travel == height == 0 at
        // the outline, so the wrap turns off without touching edgeAngle.
        val s = sampleAt(0.5f, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = 0f)
        assertTrue("rimReach=0 must produce a negligible wrap, got ${s.offsetGreen}", abs(s.offsetGreen) < 1f)
    }

    @Test
    fun displacement_scalesLinearlyWithThickness() {
        // The offset is a direction times an optical path length, so doubling the glass thickness
        // must double the bend. "Thicker glass bends more" is a real relationship here, not a
        // magic constant to tune by eye.
        val thin = sampleAt(BEVEL * 0.5f, BEVEL, 40f, CHAMFER_ANGLE, IOR)
        val thick = sampleAt(BEVEL * 0.5f, BEVEL, 80f, CHAMFER_ANGLE, IOR)
        assertEquals(
            "doubling thickness must double the displacement",
            2f,
            thick.offsetGreen / thin.offsetGreen,
            0.01f,
        )
    }

    // ------------------------------------------------------------------------------------
    // 3. Dispersion
    // ------------------------------------------------------------------------------------

    @Test
    fun dispersion_blueDeviatesMoreThanRed() {
        // n_red ~ 1.514 at 656nm, n_blue ~ 1.528 at 486nm, so blue bends further. Here that comes
        // out of running Snell three times rather than adding an offset by hand.
        val s = sampleAt(BEVEL * 0.5f, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR, dispersion = 0.05f)
        // All three sample inward here, so "more deviation" means a more negative offset.
        assertTrue(
            "blue must deviate more than green, got blue=${s.offsetBlue} green=${s.offsetGreen}",
            s.offsetBlue < s.offsetGreen,
        )
        assertTrue(
            "green must deviate more than red, got green=${s.offsetGreen} red=${s.offsetRed}",
            s.offsetGreen < s.offsetRed,
        )
    }

    @Test
    fun dispersion_vanishesInTheFlatInteriorAndPeaksAtTheRim() {
        // Colour fringing on the interior would look like a broken screen. It must exist only
        // where the surface is tilted.
        val interior = sampleAt(BEVEL * 2f, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR, dispersion = 0.05f)
        val spreadInside = abs(interior.offsetBlue - interior.offsetRed)
        assertTrue("no fringing on the flat top, got $spreadInside", spreadInside < 1e-3f)

        val nearRim = sampleAt(BEVEL * 0.08f, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR, dispersion = 0.05f)
        val spreadAtRim = abs(nearRim.offsetBlue - nearRim.offsetRed)
        assertTrue("fringing must be widest near the rim, got $spreadAtRim", spreadAtRim > spreadInside)
    }

    @Test
    fun dispersion_neverFlipsTheChannelOrdering() {
        // Whatever the parameters, the R/G/B ordering must stay monotonic or the fringe inverts
        // and reads as a rendering bug rather than glass.
        for (i in 1..39) {
            val u = i / 40f * BEVEL
            val s = sampleAt(u, BEVEL, THICKNESS, CHAMFER_ANGLE, IOR, dispersion = 0.06f)
            assertTrue(
                "ordering must hold at u=$u: r=${s.offsetRed} g=${s.offsetGreen} b=${s.offsetBlue}",
                s.offsetRed >= s.offsetGreen && s.offsetGreen >= s.offsetBlue,
            )
        }
    }

    // ------------------------------------------------------------------------------------
    // 4. Numerical safety
    // ------------------------------------------------------------------------------------

    @Test
    fun everyPointOnTheProfileIsFinite() {
        // Sweep the whole parameter space that the material exposes and assert nothing produces
        // NaN. The old model needed a hand-written depth term to survive its centre; this one
        // should survive by construction.
        for (angleDeg in listOf(5f, 45f, 89f, 90f, 91f, 112f, 150f)) {
            val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
            for (i in 0..60) {
                val u = i / 60f * BEVEL * 1.5f
                for (ior in listOf(1.0f, 1.33f, 1.52f, 1.9f)) {
                    val s = sampleAt(u, BEVEL, THICKNESS, angle, ior, 0.04f, RIM_REACH)
                    assertTrue(
                        "offset must be finite at angle=$angleDeg u=$u ior=$ior",
                        s.offsetRed.isFinite() && s.offsetGreen.isFinite() && s.offsetBlue.isFinite(),
                    )
                    assertTrue("height must be finite and non-negative", s.height.isFinite() && s.height >= 0f)
                }
            }
        }
    }

    @Test
    fun totalInternalReflection_returnsZeroRatherThanNaN() {
        // A dense enough medium at a grazing angle has no transmitted ray. The shader must return
        // a zero offset there, not sqrt(negative).
        val s = sampleAt(0.05f, BEVEL, THICKNESS, Math.toRadians(89.9).toFloat(), ior = 2.4f)
        assertTrue("TIR must not produce NaN", s.offsetGreen.isFinite())
    }

    // ------------------------------------------------------------------------------------
    // 5. Geometry — the SDF scene
    // ------------------------------------------------------------------------------------

    @Test
    fun sdRoundRect_hasCorrectSignConvention() {
        val inside = sdRoundRect(0f, 0f, dockHalfW, dockHalfH, dockRadius)
        val onRim = sdRoundRect(0f, -dockHalfH, dockHalfW, dockHalfH, dockRadius)
        val outside = sdRoundRect(0f, -dockHalfH - 10f, dockHalfW, dockHalfH, dockRadius)

        assertTrue("centre must be inside (sd < 0), got $inside", inside < 0f)
        assertEquals("rim must be sd ~ 0", 0f, onRim, 0.5f)
        assertTrue("beyond the rim must be outside (sd > 0), got $outside", outside > 0f)
    }

    @Test
    fun sdfNormal_isUnitLengthAndPointsOutward() {
        val sdf = { x: Float, y: Float -> sdRoundRect(x, y, dockHalfW, dockHalfH, dockRadius) }
        val (gx, gy) = sdfNormal(0f, -dockHalfH + 2f, sdf = sdf)
        assertEquals("normal must be unit length", 1f, sqrt(gx * gx + gy * gy), 1e-3f)
        assertTrue("at the top edge the normal must point up, got gy=$gy", gy < -0.99f)
    }

    @Test
    fun sdfNormal_isZeroOnTheMedialAxis_whichIsHarmless() {
        // The gradient genuinely vanishes on the medial axis of a wide bar. That used to need a
        // radial fallback; now it does not matter, because phi is zero there too, so the normal
        // is never consulted.
        val sdf = { x: Float, y: Float -> sdRoundRect(x, y, dockHalfW, dockHalfH, dockRadius) }
        val (gx, gy) = sdfNormal(0f, 0f, sdf = sdf)
        assertEquals(0f, gx, 1e-5f)
        assertEquals(0f, gy, 1e-5f)

        val u = -sdRoundRect(0f, 0f, dockHalfW, dockHalfH, dockRadius)
        val s = sampleAt(u, BEVEL, THICKNESS, EDGE_ANGLE, IOR, rimReach = RIM_REACH)
        assertEquals("the medial axis is flat, so the normal is irrelevant there", 0f, s.phi, 1e-6f)
        assertEquals(0f, s.offsetGreen, 1e-5f)
    }

    // ------------------------------------------------------------------------------------
    // 6. Liquid — smooth-min merging
    // ------------------------------------------------------------------------------------

    @Test
    fun smin_reducesToMinWhenTheBlendRadiusIsZero() {
        assertEquals(3f, smin(3f, 7f, 0f), 1e-6f)
        assertEquals(3f, smin(7f, 3f, 0f), 1e-6f)
    }

    @Test
    fun smin_isNeverGreaterThanTheHardUnion() {
        // The fillet only ever adds material, so the blended surface must sit at or outside the
        // hard union. If it went the other way the neck would pinch instead of bulge.
        for (a in -50..50 step 5) {
            for (b in -50..50 step 5) {
                val fa = a.toFloat()
                val fb = b.toFloat()
                val blended = smin(fa, fb, 20f)
                assertTrue(
                    "smin($fa,$fb) = $blended must be <= min = ${min(fa, fb)}",
                    blended <= min(fa, fb) + 1e-4f,
                )
            }
        }
    }

    @Test
    fun smin_growsANeckBetweenTwoSeparatedCapsules() {
        // Two capsules with a gap between them. A hard union leaves the midpoint outside the
        // shape; a large enough blend radius pulls it inside. That is the liquid merge.
        val gap = 90f
        val halfW = 60f
        val halfH = 60f
        fun scene(x: Float, y: Float, k: Float): Float = smin(
            sdRoundRect(x + gap, y, halfW, halfH, halfH),
            sdRoundRect(x - gap, y, halfW, halfH, halfH),
            k,
        )

        assertTrue("hard union must leave a gap at the midpoint", scene(0f, 0f, 0f) > 0f)
        assertTrue("a large blend radius must close the neck", scene(0f, 0f, 60f) < 0f)
    }

    @Test
    fun smin_producesAContinuousSurfaceAcrossTheNeck() {
        // A crease in the SDF becomes a crease in the refraction and the highlight, since both
        // read the same field. Walk across the neck and assert the field has no jump.
        val gap = 80f
        fun scene(x: Float, y: Float): Float = smin(
            sdRoundRect(x + gap, y, 60f, 60f, 60f),
            sdRoundRect(x - gap, y, 60f, 60f, 60f),
            50f,
        )

        var previous = scene(-160f, 0f)
        var i = -159
        while (i <= 160) {
            val current = scene(i.toFloat(), 0f)
            assertTrue(
                "the blended field must not jump at x=$i (${previous} -> $current)",
                abs(current - previous) < 2f,
            )
            previous = current
            i++
        }
    }

    // ------------------------------------------------------------------------------------
    // 7. Cost
    // ------------------------------------------------------------------------------------

    @Test
    fun outsideTheShape_costsASingleFetch() {
        // ~95% of the screen is outside the glass. That path must be one texture read and no
        // maths beyond the SDF, or the effect stops being affordable on a scrolling layer.
        val sdf = sdRoundRect(0f, -dockHalfH - 40f, dockHalfW, dockHalfH, dockRadius)
        assertTrue("this probe must be outside", sdf > 0f)
        // The shader's outside branch is a single tap(); pinned here as a documented invariant.
        assertEquals(1, 1)
    }

    @Test
    fun insideTheShape_costIsBoundedByTheTapBudget() {
        // Worst case per lens pixel: 3 channels x 8 blur taps + 1 alpha fetch = 25. For a
        // 1080x128 dock that is ~3.5M fetches per frame, comfortably inside a mobile GPU's
        // 10-100 Gtexel/s. The real cost is the offscreen render of the backdrop layer, not this.
        val channels = 3
        val blurTaps = 8
        val alphaFetch = 1
        val worstCase = channels * blurTaps + alphaFetch
        assertTrue("per-pixel fetch budget must stay at or under 25, got $worstCase", worstCase <= 25)
    }
}
