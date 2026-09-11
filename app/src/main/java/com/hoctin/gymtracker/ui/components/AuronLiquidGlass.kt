package com.hoctin.gymtracker.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LOG-V4B — AURON glass treatment.
 *
 * # Reusing this on any box or bar
 *
 * There is no backdrop capture on Compose 1.6.8, so the effect is deliberately split into two
 * halves that any surface can reuse:
 *
 * 1. **`Modifier.auronBackdropLens(centerX, centerY, halfW, halfH, material)`** — put this on the
 *    layer that *contains* the content you want refracted. Everything inside it is rendered to an
 *    offscreen layer and the two-zone lens is applied to that layer.
 * 2. **`Modifier.auronLiquidGlass(lighting)`** — put this on the glass surface itself. It only draws
 *    lighting (halo, rim, top sweep, bottom shadow); it never touches content.
 *
 * The glass rect is just a measurement: call `onGloballyPositioned` on the glass, convert with
 * `positionInRoot()`, and feed centre + half-size into (1). See `GymTrackerApp.kt` (`showDock`,
 * `dockCenterX/Y`, `dockLensHalfWidth/Height`) for the working reference wiring.
 *
 * To retune a surface, copy `NavDockBackdropLens` / `NavDockGlassLighting` and change the `Lens`
 * fields — no shader or call-site changes required.
 *
 * Gotchas that cost real time here: never use the `graphicsLayer { }` lambda (see
 * `AuronGlass.DEBUG_EFFECT`), and `RuntimeShader.setFloatUniform()` throws for unknown uniforms.
 */
object AuronGlass {

    /**
     * Master kill switch. `false` removes both the backdrop refraction and the surface overlays
     * everywhere, with no call-site changes. Delete the feature entirely by removing the
     * `auronLiquidGlass(...)` / `auronBackdropLens(...)` call sites and this file (see
     * `_backup_20260905_220000_backdrop_lens/` for the prior state).
     */
    const val DEBUG_NAV = true

    const val ENABLED = true

    /**
     * Diagnostic level — **the single switch for GPU-pipeline triage.** Set one value, rebuild,
     * and read the screen. Three outcomes, mutually exclusive:
     *
     * | value | what is applied | if the lens layer turns… | conclusion |
     * |---|---|---|---|
     * | `0` | the real refraction shader | — | normal operation |
     * | `1` | **solid magenta** AGSL shader, no uniforms read, no SDF, no early return | **magenta** | AGSL + RenderEffect both work → the bug is inside `BACKDROP_LENS_AGSL` |
     * | `2` | **solid red** via a plain `ColorMatrixColorFilter` — **no shader involved at all** | **red** | RenderEffect attaches fine, but AGSL specifically fails to compile/execute |
     * | `3` | **alpha = 0.5**, no effect at all | **translucent** | the graphicsLayer is created and composited, so the failure is specific to RenderEffect |
     * | any | — | **nothing / normal UI** | the effect is not reaching the RenderNode at all |
     *
     * Level 2 is the decisive one: it uses no AGSL whatsoever, so if even that shows nothing the
     * problem is in how `Modifier.graphicsLayer` hands the effect to the layer, not in any shader.
     *
     * **All four levels go through the same direct-parameter `graphicsLayer(...)` overload**, never
     * the lambda `graphicsLayer { }` form. That matters: `BlockGraphicsLayerModifier` declares
     * `shouldAutoInvalidate = false` in Compose 1.6.8, so the lambda is not guaranteed to re-run. If
     * the diagnostics used it while the real path did not, a null result would prove nothing.
     *
     * **Level 1 writes no uniforms at all.** `RuntimeShader.setFloatUniform()` throws
     * `IllegalArgumentException` for a uniform the program does not declare, and the level-1 shader
     * declares only `content` — see `writesGeometryUniforms` in [auronBackdropLens].
     *
     * Never ship with a non-zero value.
     */
    const val DEBUG_EFFECT = 0

    /** Derived convenience: true whenever any diagnostic level is active (drives the logging). */
    val DEBUG_TINT: Boolean
        get() = DEBUG_EFFECT != 0

    /**
     * True when the backdrop refraction can run at all: enabled, and on a device whose `RenderEffect`
     * supports programmable (`RuntimeShader`) effects, i.e. API 33+.
     *
     * Call sites use this to switch between the translucent "real glass" appearance and the opaque
     * fallback, so the two can never drift apart.
     */
    val BACKDROP_AVAILABLE: Boolean
        get() = ENABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/** Geometry + intensity for a glass surface, in **dp**. Same shape as before for API stability. */
@Immutable
data class Lens(
    /** Corner radius of the surface. */
    val cornerRadiusDp: Dp = 22.dp,
    /** Depth of the rim refraction band (in pixels: refractionHeightDp × density). */
    val refractionHeightDp: Dp = 16.dp,
    /** Peak displacement at the rim (in pixels: refractionAmountDp × density). */
    val refractionAmountDp: Dp = 6.dp,
    /** R/B chromatic separation at the rim (in pixels: chromaDp × density). */
    val chromaDp: Dp = 0.8.dp,
    /** Intensity multiplier for the rim, top sweep and bottom shadow (0 = none, 1 = strong). */
    val specular: Float = 0.20f,
    /** Radial blend for the droplet profile. 0 = pure bevel, ~0.3 = slight bulge. */
    val depth: Float = 0.35f,
    /**
     * Index of refraction. 1.0 = air (optically invisible), 1.33 = water, 1.5 = glass.
     * Displacement scales with `(ior - 1.0) / 0.5`, matching the small-angle result that
     * deviation is proportional to `(n - 1)`. Having a real IOR is what makes the model
     * falsifiable: at `ior = 1.0` every displacement must be exactly zero.
     */
    val ior: Float = 1.5f,
    /**
     * Width of the overhang zone — the outermost sliver of the band where the surface has curved
     * past vertical, and therefore samples **outward**. Larger = thicker wrap band.
     */
    val rimWidthDp: Dp = 5.dp,
    /**
     * How far outward the overhang samples. This stands in for the light path *through* the glass
     * body (light piping), not for surface height — at the overhang the height has already
     * collapsed to ~0, so a height-derived offset would vanish. Explicitly an approximation.
     */
    val rimReachDp: Dp = 10.dp,
    /** Extra centre magnification as a fraction (0.04 = 4% larger at the centre). */
    val centerMagnification: Float = 0.06f,
    /** Width of the soft Fresnel-style halo hugging the rim. */
    val haloWidthDp: Dp = 6.dp,
    /** Peak alpha of that halo. Kept low so it never reads as a glow bar. */
    val haloAlpha: Float = 0.26f,
)

/**
 * Surface lighting for the dock **without** the backdrop lens (kill switch off, API < 33).
 *
 * Kept as the original behaviour so the degraded path is exactly what the app looked like
 * before the feature existed.
 */
val NavDockLens = Lens(
    cornerRadiusDp = 30.dp,
    refractionHeightDp = 18.dp,
    specular = 0.45f,
)

/** Surface lighting for the Daily Win card without the backdrop lens. */
val DailyWinLens = Lens(
    cornerRadiusDp = 22.dp,
    refractionHeightDp = 22.dp,
    specular = 0.30f,
)

/**
 * Refraction geometry for the dock's **backdrop** lens.
 *
 * `refractionHeightDp` is set to 32dp so the **whole** 64dp dock sits inside the band — the
 * centre pixel has `-sd = 32`, so the early-out never fires; `arcLift(t) = 1 - sqrt(1 - t²)` at
 * `t = 0.0` returns 0, which gives a natural zero-displacement fall-off at the centre while the
 * rim bends at full strength. `refractionAmountDp` is 18px of peak displacement — strong enough
 * that a 1dp card border scrolling under the rim is visibly bent rather than barely nudged.
 */
val NavDockBackdropLens = Lens(
    cornerRadiusDp = 30.dp,
    refractionHeightDp = 32.dp,
    refractionAmountDp = 18.dp,
    chromaDp = 2.4.dp,   // colour splay at the distorted rim
    specular = 0f,        // unused by the backdrop shader
    depth = 0.30f,
    ior = 1.5f,           // glass
    rimWidthDp = 5.dp,    // 5dp overhang at top and bottom
    rimReachDp = 12.dp,   // pulls up to 12dp of outside background into the rim
)

/**
 * Surface lighting for the dock **with** the backdrop lens active.
 *
 * Lower `specular` than [NavDockLens] because the refraction now carries the visual weight —
 * a strong white sweep on top of it would wash out exactly the bent content we want to show.
 */
val NavDockGlassLighting = Lens(
    cornerRadiusDp = 30.dp,
    refractionHeightDp = 18.dp,
    specular = 0.22f,
)

/**
 * Adds the three glass overlays (top sweep, bottom shadow, rim stroke) on top of the receiver's
 * children. The caller is responsible for any background fill — typically `Modifier.background(...)`
 * *before* this modifier, so the background sits below the children and the glass overlays sit on
 * top.
 */
@Composable
fun Modifier.auronLiquidGlass(lens: Lens): Modifier {
    if (!AuronGlass.ENABLED) return this
    val density = LocalDensity.current
    val bandPx = with(density) { lens.refractionHeightDp.toPx() }
    val cornerPx = with(density) { lens.cornerRadiusDp.toPx() }
    val strokePx = with(density) { 1.dp.toPx() }
    val haloPx = with(density) { lens.haloWidthDp.toPx() }
    val spec = lens.specular
    return this.drawWithCache {
        // Pre-build the brushes once per size change. drawWithCache invalidates the cache on
        // size change, lens change, or any state read inside the block, so these recompute when
        // needed but not every frame.
        val topSweep = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = spec * 0.55f),
                0.55f to Color.White.copy(alpha = spec * 0.10f),
                1.00f to Color.Transparent,
            ),
            startY = 0f,
            endY = bandPx,
        )
        val bottomShadow = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.45f to Color.Black.copy(alpha = spec * 0.18f),
                1.00f to Color.Black.copy(alpha = spec * 0.35f),
            ),
            startY = size.height - bandPx,
            endY = size.height,
        )
        // Fresnel-style outer halo: a wide, very low-alpha stroke hugging the rim so the edge
        // reads as a raised glass bevel. Deliberately NOT a blur — a broad blur would wash out
        // the refraction signature that is the whole point of this effect.
        val haloBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = lens.haloAlpha),
                0.50f to Color.White.copy(alpha = lens.haloAlpha * 0.5f),
                1.00f to Color.White.copy(alpha = lens.haloAlpha * 0.3f),
            ),
            startY = 0f,
            endY = size.height,
        )
        val rimBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = spec * 0.55f),
                0.50f to Color.White.copy(alpha = spec * 0.20f),
                1.00f to Color.White.copy(alpha = spec * 0.10f),
            ),
            startY = 0f,
            endY = size.height,
        )

        onDrawWithContent {
            // 1. Children draw first (icons, labels, indicator, card body) — fully visible.
            drawContent()
            // 2. Top sweep — a soft white band hugging the top edge.
            drawRect(brush = topSweep)
            // 3. Bottom shadow — a thin dark band at the bottom edge.
            drawRect(brush = bottomShadow)
            // 4. Rim stroke — 1dp rounded-rect outline that reads as the glass edge. Centred on
            //    the path by default, so inset by half the stroke width on each side to keep it
            //    visually flush with the receiver's `.clip(RoundedCornerShape)`.
            translate(strokePx / 2f, strokePx / 2f) {
                drawRoundRect(
                    brush = haloBrush,
                    topLeft = Offset.Zero,
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = haloPx),
                )
            }
            translate(strokePx / 2f, strokePx / 2f) {
                drawRoundRect(
                    brush = rimBrush,
                    topLeft = Offset.Zero,
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
}

// ================================================================================================
// Diagnostic effects — prove which stage of the GPU pipeline is broken.
// ================================================================================================

/**
 * Level-1 diagnostic: the simplest AGSL program that still satisfies the
 * `createRuntimeShaderEffect` contract. `content` is sampled and then multiplied by zero so the
 * result is *always* solid magenta — but the input is genuinely referenced, so the binding cannot
 * be optimised away and invalidate the comparison against the real shader.
 */
private const val DIAGNOSTIC_SOLID_MAGENTA_AGSL = """
uniform shader content;

half4 main(float2 coord) {
    half4 c = content.eval(coord);
    return half4(1.0, 0.0, 1.0, 1.0) + c * 0.0;
}
"""

/** Red = R forced to 1, G and B forced to 0, alpha preserved. */
private fun solidRedColorFilter() = android.graphics.ColorMatrixColorFilter(
    floatArrayOf(
        0f, 0f, 0f, 0f, 1f,   // R' = 1
        0f, 0f, 0f, 0f, 0f,   // G' = 0
        0f, 0f, 0f, 0f, 0f,   // B' = 0
        0f, 0f, 0f, 1f, 0f,   // A' = A
    ),
)

// ================================================================================================
// Backdrop refraction — the part that makes it read as real glass.
// ================================================================================================

/**
 * Refraction shader that runs on the **background** layer, not on the glass.
 *
 * ## Why the shader lives on the background
 *
 * `RenderEffect.createRuntimeShaderEffect(shader, "content")` binds `content` to the layer of
 * whichever node the modifier sits on — it has no way to sample a sibling or anything drawn in an
 * earlier pass. That is why every earlier attempt failed: putting this shader on the dock meant
 * `content` was the dock's own pixels, so it bent a fake gradient instead of the Home screen
 * scrolling underneath.
 *
 * Inverting it fixes the input problem without any new API: attach the shader to the layer that
 * *contains* the background, and give it the dock's rect as a uniform. The shader then reads real
 * background pixels — card borders, thumbnails, text — and bends them under the glass.
 *
 * ## Pass-through, never discard
 *
 * Outside the lens the shader returns `content.eval(coord)` **unchanged**. The earlier version
 * discarded those pixels, which painted a transparent halo around the bar. Pass-through also makes
 * the effect nearly free: ~95% of the screen takes one texture fetch and no maths.
 *
 * ## The lens rect is a constant
 *
 * The dock is anchored to the bottom of the screen, so its rect in this layer's coordinate space
 * does not move when the content scrolls — content slides *within* the layer while the lens stays
 * put. Uniforms are therefore rewritten on layout/config change only.
 *
 * ## Droplet profile
 *
 * The displacement direction blends the edge normal with a radial term:
 * `g = normalize(edgeNormal + uDepth * normalize(p))`. At the rim the edge normal already points
 * outward and the radial term agrees with it, so the displacement stays clean. Near the centre the
 * analytic edge gradient collapses to (0,0) (`sign(0) == 0`), and the radial term is what carries
 * the direction. With `uDepth > 0` the surface stops reading as a flat bevelled panel and starts
 * reading as a slightly bulged lens — the iOS-style "thick glass" look.
 *
 * ## Physics
 *
 * Same model pinned by `LiquidGlassPhysicsTest`: circular profile `arcLift(t) = 1 - sqrt(1 - t²)`
 * (the normalised sagitta of a circle), sampling *inward* so the rim magnifies, blue carrying the
 * larger offset because n_blue > n_red in real glass.
 */
private const val BACKDROP_LENS_AGSL = """
uniform shader content;

uniform float2 uLensCenter;
uniform float2 uLensHalf;
uniform float uRadius;
uniform float uRefractionHeight;
uniform float uRefractionAmount;
uniform float uChroma;
uniform float uDepth;
uniform float uIor;
uniform float uRimWidth;
uniform float uRimReach;
uniform float uCenterMag;
uniform float uDebugTint;

float arcLift(float x) {
    return 1.0 - sqrt(max(1.0 - x * x, 0.0));
}

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - (b - float2(r));
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

float2 gradSdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - (b - float2(r));
    if (q.x >= 0.0 || q.y >= 0.0) {
        return sign(p) * normalize(max(q, float2(0.0)) + float2(0.00001));
    }
    float gx = step(q.y, q.x);
    return sign(p) * float2(gx, 1.0 - gx);
}

half4 main(float2 coord) {
    // DIAGNOSTIC STAGE 1 — must come BEFORE the SDF checks. If DEBUG_TINT is on, the whole layer
    // turns magenta. If the user can see magenta at all, the shader IS running and the
    // RenderEffect IS attached. If they see none, the shader is not being invoked — and the
    // Kotlin-side Log.d calls (gated on DEBUG_TINT) tell them which guard returned this. See
    // `auronBackdropLens` for the matching log lines.
    if (uDebugTint > 0.5) {
        return half4(1.0, 0.0, 1.0, 1.0);
    }

    float2 p = coord - uLensCenter;
    float sd = sdRoundRect(p, uLensHalf, uRadius);

    // Outside the lens: pass through untouched. Deliberately NOT a discard - discarding is what
    // painted the transparent halo around the bar in the earlier version.
    if (sd > 0.0) {
        return content.eval(coord);
    }
    // Deep-interior early-out: only fires when `refractionHeightDp` is shallower than the lens.
    // When the band covers the whole lens the natural arcLift(t=0)=0 fall-off already gives zero
    // displacement at the centre, so the early-out is an opt-in legibility knob rather than the
    // default.
    if (-sd >= uRefractionHeight) {
        return content.eval(coord);
    }

    float t = 1.0 - (-sd / max(uRefractionHeight, 0.0001));

    // Deviation is proportional to (n - 1) at small angles, so scaling by (ior-1)/0.5 makes
    // ior=1.0 exactly identity and ior=1.5 exactly the authored displacement. This is what makes
    // the model falsifiable: glass with the index of air must be optically invisible.
    float iorScale = (uIor - 1.0) / 0.5;

    // Displacement direction: edge normal blended with the radial. The edge normal is what makes
    // the rim feel sharp; the radial term is what makes the centre feel like part of a curved
    // surface rather than a hole punched through it. Both can be degenerate on this surface, so
    // each is guarded before use.
    float2 gv = gradSdRoundRect(p, uLensHalf, uRadius);
    float2 g = float2(0.0, -1.0);
    if (dot(gv, gv) > 0.000001) {
        g = normalize(gv);
    }
    float2 radial = float2(0.0, 1.0);
    if (dot(p, p) > 0.000001) {
        radial = normalize(p);
    }
    g = normalize(g + uDepth * radial);

    // Where the overhang begins, expressed as a value of t. In a real 3D surface this is the
    // point where the edge angle passes 90 deg; in this 2D SDF model the width of the outermost
    // sliver stands in for that angle.
    float rimFrac = clamp(uRimWidth / max(uRefractionHeight, 0.0001), 0.0, 0.95);
    float rimStart = 1.0 - rimFrac;

    // Bevel: inward magnitude from the circular profile (steepest nearest the rim).
    float dB = arcLift(t) * uRefractionAmount * iorScale;
    float rt = clamp((t - rimStart) / max(1.0 - rimStart, 0.0001), 0.0, 1.0);
    float dO = uRimReach * rt * iorScale;

    // Smooth blend, NOT a hard branch. A hard `if (t >= rimStart)` leaves a ~22px tear at
    // rimStart (bevel still ~-21px while overhang restarts at ~0). smoothstep is C1 and has zero
    // value AND zero slope at both ends, so the fold is continuous and tangent-continuous.
    float blend = smoothstep(rimStart, 1.0, t);
    // Signed displacement along +g (outward): negative = inward sample, positive = outward sample.
    float disp = mix(-dB, dO, blend);

    // Optional centre magnification: pull the source coordinate toward the lens centre so the
    // body of the lens enlarges slightly. Purely an art term (WWDC-hero look) — physical
    // magnification for UI-thin glass is ~0. Fades to nothing by rimStart so it never fights the
    // overhang, and scales with iorScale so ior=1 stays invisible.
    float magW = 1.0 - smoothstep(0.0, rimStart, t);
    float mag = 1.0 + uCenterMag * magW * iorScale;
    float2 magCoord = uLensCenter + (coord - uLensCenter) / mag;

    // Chromatic dispersion: blue deviates more than red (n_blue > n_red) -> larger magnitude.
    // Sign-aware, and scaled by iorScale so dispersion vanishes when there is no refraction
    // (dispersion is *caused by* the index varying with wavelength).
    float ch = t * uChroma * iorScale;
    float sgn = 1.0;
    if (disp < 0.0) {
        sgn = -1.0;
    }

    float2 sR = magCoord + g * (disp - sgn * ch);
    float2 sG = magCoord + g * disp;
    float2 sB = magCoord + g * (disp + sgn * ch);

    return half4(content.eval(sR).r, content.eval(sG).g, content.eval(sB).b, content.eval(sG).a);
}
"""

/**
 * Applies the refraction lens to a **background** layer.
 *
 * Put this on the composable that contains the scrolling content (the NavHost), *not* on the
 * floating glass itself. The glass is a sibling drawn afterwards, so its icons and labels are
 * never touched by the shader and stay perfectly crisp.
 *
 * @param centerX / [centerY] centre of the lens, in this layer's own pixel coordinates.
 * @param halfWidth / [halfHeight] half the lens size, in pixels.
 * @param lens geometry; [Lens.cornerRadiusDp], [Lens.refractionHeightDp],
 *   [Lens.refractionAmountDp], [Lens.chromaDp] and [Lens.depth] are read.
 *
 * No-op when [AuronGlass.ENABLED] is false, below API 33, or before the lens has been measured
 * ([halfWidth] / [halfHeight] still zero) — so it is safe to call on the very first frame.
 */
@Composable
fun Modifier.auronBackdropLens(
    centerX: Float,
    centerY: Float,
    halfWidth: Float,
    halfHeight: Float,
    lens: Lens,
): Modifier {
    // DIAGNOSTIC — log every call so the user can see in logcat (tag: AuronGlass) exactly which
    // path the modifier took on this frame. Only emits when DEBUG_TINT is on, so production
    // builds stay silent.
    if (AuronGlass.DEBUG_TINT) {
        android.util.Log.d(
            "AuronGlass",
            "auronBackdropLens called: centerX=$centerX centerY=$centerY " +
                "halfW=$halfWidth halfH=$halfHeight ior=${lens.ior} " +
                "api=${Build.VERSION.SDK_INT} enabled=${AuronGlass.ENABLED} " +
                "debugTint=${AuronGlass.DEBUG_TINT}",
        )
    }

    if (!AuronGlass.ENABLED) {
        if (AuronGlass.DEBUG_TINT) android.util.Log.d("AuronGlass", "  -> ENABLED=false, returning this")
        return this
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        if (AuronGlass.DEBUG_TINT) android.util.Log.d("AuronGlass", "  -> API<33, returning this")
        return this
    }
    // Guard the first frame: the dock has not been measured yet, so a zero-sized lens would
    // collapse every SDF to "outside" and the shader would be a pure (but costly) copy.
    if (halfWidth <= 0f || halfHeight <= 0f) {
        if (AuronGlass.DEBUG_TINT) {
            android.util.Log.d("AuronGlass", "  -> halfW/halfH <= 0 (dock not yet measured), returning this")
        }
        return this
    }

    val density = LocalDensity.current

    // DIAGNOSTIC: level 2 bypasses AGSL entirely with a plain colour-filter RenderEffect, so it
    // isolates "does any effect reach the layer" from "does the shader compile".
    //
    // Uses the SAME direct-parameter `graphicsLayer(...)` overload as the real path. Earlier this
    // level used the lambda `graphicsLayer { }` form, which would have exercised a different code
    // path and made the three-way comparison invalid — a null result here would then have proved
    // nothing about the real path.
    if (AuronGlass.DEBUG_EFFECT == 2) {
        val colorFilterEffect = remember {
            AndroidRenderEffect.createColorFilterEffect(solidRedColorFilter())
                .asComposeRenderEffect()
        }
        if (AuronGlass.DEBUG_TINT) {
            android.util.Log.d(
                "AuronGlass",
                "  -> DEBUG_EFFECT=2: NON-SHADER colour filter (solid red), direct graphicsLayer overload",
            )
        }
        return this.graphicsLayer(
            renderEffect = colorFilterEffect,
            compositingStrategy = CompositingStrategy.Offscreen,
        )
    }

    // DIAGNOSTIC: level 3 applies NO effect at all — just a layer property. If the content even
    // goes translucent, the graphicsLayer is being created and composited, which means the failure
    // is specific to RenderEffect rather than to layer creation. Same direct overload as above.
    if (AuronGlass.DEBUG_EFFECT == 3) {
        if (AuronGlass.DEBUG_TINT) {
            android.util.Log.d(
                "AuronGlass",
                "  -> DEBUG_EFFECT=3: alpha=0.5 only, NO effect, direct graphicsLayer overload",
            )
        }
        return this.graphicsLayer(alpha = 0.5f)
    }

    // Level 1 swaps in the trivial magenta shader; level 0 uses the real refraction shader.
    val shaderSource = if (AuronGlass.DEBUG_EFFECT == 1) {
        DIAGNOSTIC_SOLID_MAGENTA_AGSL
    } else {
        BACKDROP_LENS_AGSL
    }

    // CRASH GUARD: `RuntimeShader.setFloatUniform()` throws IllegalArgumentException when the named
    // uniform does not exist in the program. The level-1 diagnostic shader deliberately declares
    // only `uniform shader content`, so writing the 11 geometry uniforms into it would crash the
    // app the moment the dock is measured. Only the real refraction shader declares them, so the
    // writes are gated on the identity of the source. (Wrapping in try/catch was rejected: it would
    // silently swallow a genuinely missing uniform in the real shader.)
    val writesGeometryUniforms = shaderSource === BACKDROP_LENS_AGSL

    // Uniforms are baked in AT SHADER CREATION, and the shader is keyed on the full geometry.
    // Two reasons this is more robust than writing them inside a `graphicsLayer { }` block:
    //
    // 1. `BlockGraphicsLayerModifier.shouldAutoInvalidate` is false in Compose 1.6.8, so the lambda
    //    is not guaranteed to re-run when the surrounding state changes. If it does not run, no
    //    uniform is ever written and `renderEffect` is never assigned — which presents exactly as
    //    "logs fire, nothing renders".
    // 2. Keying on geometry means a *new* RuntimeShader and a *new* RenderEffect are built whenever
    //    the dock rect changes. A different RenderEffect instance makes the modifier's update()
    //    actually re-apply, so the layer is re-recorded instead of silently reusing a stale one.
    val shader = remember(
        shaderSource, centerX, centerY, halfWidth, halfHeight, lens, density,
    ) {
        RuntimeShader(shaderSource).apply {
            if (writesGeometryUniforms) {
                setFloatUniform("uLensCenter", centerX, centerY)
                setFloatUniform("uLensHalf", halfWidth, halfHeight)
                setFloatUniform("uRadius", with(density) { lens.cornerRadiusDp.toPx() })
                setFloatUniform("uRefractionHeight", with(density) { lens.refractionHeightDp.toPx() })
                setFloatUniform("uRefractionAmount", with(density) { lens.refractionAmountDp.toPx() })
                setFloatUniform("uChroma", with(density) { lens.chromaDp.toPx() })
                setFloatUniform("uDepth", lens.depth)
                setFloatUniform("uIor", lens.ior)
                setFloatUniform("uRimWidth", with(density) { lens.rimWidthDp.toPx() })
                setFloatUniform("uRimReach", with(density) { lens.rimReachDp.toPx() })
                setFloatUniform("uCenterMag", lens.centerMagnification)
                setFloatUniform("uDebugTint", if (AuronGlass.DEBUG_TINT) 1f else 0f)
            }
        }
    }
    val effect = remember(shader) {
        AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    if (AuronGlass.DEBUG_TINT) {
        android.util.Log.d(
            "AuronGlass",
            "  -> DEBUG_EFFECT=${AuronGlass.DEBUG_EFFECT}: source=" +
                if (shaderSource === DIAGNOSTIC_SOLID_MAGENTA_AGSL) {
                    "DIAGNOSTIC_SOLID_MAGENTA"
                } else {
                    "BACKDROP_LENS"
                } +
                "; uniforms baked at creation: uLensCenter=($centerX,$centerY) " +
                "uLensHalf=($halfWidth,$halfHeight) " +
                "uDebugTint=${if (AuronGlass.DEBUG_TINT) 1f else 0f}",
        )
    }

    // Direct-parameter overload — no lambda, so nothing here depends on the block being re-invoked.
    // Offscreen compositing is required for a RenderEffect to be applied at all.
    return this.graphicsLayer(
        renderEffect = effect,
        compositingStrategy = CompositingStrategy.Offscreen,
    )
}