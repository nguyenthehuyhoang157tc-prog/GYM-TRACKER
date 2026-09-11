package com.hoctin.gymtracker.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * LOG-V5 — AURON liquid glass.
 *
 * What this models, and what each part is responsible for visually:
 *
 * | Feature seen in the reference material | Produced by |
 * |---|---|
 * | Background magnified in the interior   | [GlassMaterial.magnify] (artistic, exact) |
 * | Rim band that bends straight lines     | Snell refraction through the bevel |
 * | Surrounding content wrapped around the outside of the shape | the overhang band, see below |
 * | Colour fringing at the rim only        | per-channel IOR (real dispersion) |
 * | Reads as a material over flat colour   | blur + saturation + adaptive film |
 * | Bright edge highlight following the contour | Blinn-Phong on the bevel normal |
 * | Two shapes merging with a smooth neck  | [GlassShape] blended with smooth-min |
 *
 * ## The overhang band — the piece that makes content wrap around the rim
 *
 * A glass slab whose top edge only rounds over to vertical (`edgeAngle == 90 deg`) refracts every
 * viewing ray *inward*. That magnifies whatever sits under the footprint, but it can never reach
 * content that lies outside the shape, so the surroundings never appear in the rim.
 *
 * A droplet's edge does not stop at vertical — it **overhangs**. Past 90 deg the surface normal has
 * `N.z < 0`, a vertical viewing ray strikes it from behind, and once the normal is flipped for the
 * Snell evaluation the refracted direction reverses: the sample now moves *outward*. Worked example
 * at `edgeAngle = 110 deg`, `ior = 1.5`:
 *
 * ```
 * N_raw = (0.940, 0, -0.342)   cosi = -0.342  ->  flip
 * N     = (-0.940, 0, 0.342)   cosi =  0.342
 * T     = ( 0.518, 0, -0.855)  ->  T.x > 0, i.e. outward
 * ```
 *
 * That band is what pulls the surroundings — a coloured bar, a card border, a photo — into a thin
 * compressed strip that follows the contour.
 *
 * One honest approximation lives here. At the overhang the local glass height is ~0, so
 * `offset = tan(deviation) * height` would collapse to zero and the effect would vanish again. In
 * real glass the light reaching that band travelled *sideways* through the body of the glass
 * (edge light-piping), a much longer path than the local height. [GlassMaterial.rimReach] stands in
 * for that path length. It is a model, not Snell.
 *
 * ## Magnification is deliberately not physical
 *
 * The interior of a thin UI panel barely magnifies: reaching M = 1.15 on a 200px-wide pill with
 * 22dp of glass would need roughly 75 deg of dome curvature — the panel would have to bulge like a
 * balloon. A raytraced hero render of a centimetre-thick block is a different object, which is why
 * its magnification is so obvious. So [GlassMaterial.magnify] is an exact, separate scale term
 * (`s = optical + (coord - optical) / M`) rather than a fake curvature, and the physical model is
 * left to do what it actually does well: the rim.
 *
 * ## Architecture
 *
 * The shader runs on the layer that **contains** the scrolling content. The glass surface itself is
 * a sibling drawn afterwards, so its icons and labels never pass through the shader. The glass
 * composable must have a **fully transparent** background — blur, tint, highlight and shadow are all
 * produced inside this shader, and an extra alpha layer on top only flattens them back out.
 */
object AuronGlass {

    /** Master kill switch. */
    const val ENABLED = true

    /** `RuntimeShader` inside a `RenderEffect` requires API 33+. */
    val BACKDROP_AVAILABLE: Boolean
        get() = ENABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Maximum number of primitives the scene SDF can blend. */
    const val MAX_BLOBS = 4
}

/**
 * One primitive of the glass shape, in the **pixel coordinates of the layer the modifier sits on**.
 *
 * A rounded rect with `cornerRadius == min(halfWidth, halfHeight)` is a capsule, which is the shape
 * used by every pill and toggle in the reference material.
 */
@Immutable
data class GlassBlob(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
) {
    companion object {
        /** Capsule: fully rounded on the short axis. */
        fun capsule(centerX: Float, centerY: Float, halfWidth: Float, halfHeight: Float) =
            GlassBlob(centerX, centerY, halfWidth, halfHeight, min(halfWidth, halfHeight))
    }
}

/**
 * The glass shape: one or more [GlassBlob]s fused by a smooth minimum.
 *
 * `smin(a, b, k)` is what produces the curved neck between two merging droplets. With
 * [mergeRadius] `== 0` the blobs are a hard union (visible crease where they cross); raising it
 * grows the fillet. Animating the blob centres toward each other while [mergeRadius] is non-zero
 * gives the surface-tension merge — and because refraction, lighting and the rim all read from the
 * same SDF, they follow the new outline for free.
 */
@Immutable
data class GlassShape(
    val blobs: List<GlassBlob>,
    val mergeRadius: Dp = 0.dp,
) {
    init {
        require(blobs.isNotEmpty() && blobs.size <= AuronGlass.MAX_BLOBS) {
            "GlassShape supports 1..${AuronGlass.MAX_BLOBS} blobs, got ${blobs.size}"
        }
    }

    /** Centroid of the blob centres — the optical centre of the magnification term. */
    internal val opticalCenterX: Float get() = blobs.map { it.centerX }.average().toFloat()
    internal val opticalCenterY: Float get() = blobs.map { it.centerY }.average().toFloat()

    companion object {
        fun single(blob: GlassBlob) = GlassShape(listOf(blob))
    }
}

/**
 * The glass material. All lengths are **dp**.
 *
 * Tuning order that actually converges: get [saturation] and [adaptive] right first (they are the
 * only terms visible over a flat-coloured backdrop), then [specular], then [edgeAngleDeg] and
 * [rimReach] for the wrap, and only then [thickness] and [ior].
 */
@Immutable
data class GlassMaterial(
    /** Glass thickness. The inward refraction offset is proportional to this. */
    val thickness: Dp = 24.dp,
    /** Width of the rounded edge band, measured inward from the outline. Must be `<= thickness`. */
    val bevel: Dp = 18.dp,
    /**
     * Maximum surface tilt at the outline, in degrees.
     *
     * - `< 90` — a chamfer. Refracts inward only. Magnifies, never wraps.
     * - `= 90` — the edge reaches vertical. The limiting case.
     * - `> 90` — an overhanging droplet edge. The band past 90 samples **outward** and is what
     *   wraps the surroundings around the contour. 105–120 is the useful range.
     */
    val edgeAngleDeg: Float = 112f,
    /**
     * Effective optical path length in the overhang band, standing in for edge light-piping.
     * Controls how far outside the shape the wrapped content is drawn from. 0 disables the wrap.
     */
    val rimReach: Dp = 26.dp,
    /** Refractive index. Glass 1.50, acrylic 1.49, water 1.33, heavy flint 1.70. */
    val ior: Float = 1.52f,
    /** Dispersion: red uses `ior - dispersion`, blue uses `ior + dispersion`. */
    val dispersion: Float = 0.035f,
    /** Interior magnification. Exact, non-physical, artistic. 1.0 = off. */
    val magnify: Float = 1.06f,
    /** Backdrop blur radius. */
    val blurRadius: Dp = 6.dp,
    /** Saturation multiplier. 1.5–2.0 gives the iOS vibrancy. */
    val saturation: Float = 1.7f,
    /** Lightens the glass over dark backdrops and darkens it over light ones. 0.08–0.15. */
    val adaptive: Float = 0.10f,
    /** Tint. Alpha is the blend strength — keep it very low. */
    val tint: Color = Color.White.copy(alpha = 0.05f),
    /** Light direction in degrees. -90 is straight down from above, -65 is upper-left. */
    val lightAngleDeg: Float = -65f,
    /** Strength of specular + fresnel + inner shadow. 0 turns lighting off. */
    val specular: Float = 0.8f,
    /** Contact shadow just outside the outline. */
    val shadow: Float = 0.10f,
    /** How far the contact shadow reaches. */
    val shadowSize: Dp = 8.dp,
)

/** Material for the nav dock. */
val NavDockGlass = GlassMaterial()

/** Material for the Daily Win card — thinner and calmer, since it carries its own text. */
val DailyWinGlass = GlassMaterial(
    thickness = 16.dp,
    bevel = 13.dp,
    edgeAngleDeg = 104f,
    rimReach = 16.dp,
    magnify = 1.03f,
    blurRadius = 8.dp,
    saturation = 1.4f,
    specular = 0.55f,
)

// ================================================================================================
// Shader
// ================================================================================================

/**
 * ## Bevel profile
 *
 * With `u` the inward distance from the outline, `x = clamp(u / bevel, 0, 1)` and `k = 1 - x`:
 *
 * ```
 * phi    = edgeAngle * k
 * height = thickness * (1 - (1 - cos phi) / (1 - cos edgeAngle))
 * N      = float3(edgeNormal * sin phi, cos phi)      // already unit length
 * ```
 *
 * `phi -> 0` and `dz/du -> 0` together at the inner end of the bevel, so the profile is tangent to
 * the flat top and there is no seam. `height -> 0` at the outline. No early-out is needed and no
 * degenerate-centre guard is needed either: deep inside, `phi == 0` gives `N == (0,0,1)`, which
 * refracts nothing regardless of what the edge normal does on the medial axis.
 *
 * ## Refraction
 *
 * Viewing ray `I = (0,0,-1)`, `cosi = N.z`. Vector Snell, then project the transmitted ray onto the
 * backdrop plane over the local optical path:
 *
 * ```
 * T      = eta*I + (eta*cosi - sqrt(1 - eta^2 (1 - cosi^2))) * N
 * offset = T.xy / (-T.z) * travel
 * ```
 *
 * `ior == 1` makes `eta == 1` and collapses `T` to `I` exactly, so the whole model produces zero
 * displacement — pinned by `LiquidGlassPhysicsTest.ior1_producesNoDisplacementAnywhere`.
 *
 * ## Sampling safety
 *
 * `content.eval()` outside the layer bounds returns transparent. Every fetch goes through `tap()`,
 * which clamps into `[0.5, uSize - 0.5]`, so a lens touching the screen edge can no longer punch a
 * transparent halo.
 */
private const val GLASS_AGSL = """
uniform shader content;

uniform float2 uSize;

uniform float4 uShape0;
uniform float4 uShape1;
uniform float4 uShape2;
uniform float4 uShape3;
uniform float4 uRadii;
uniform float  uCount;
uniform float  uSmooth;

uniform float2 uOptical;
uniform float  uMagnify;

uniform float  uThickness;
uniform float  uBevel;
uniform float  uEdgeAngle;
uniform float  uRimReach;
uniform float  uIor;
uniform float  uDispersion;

uniform float  uBlur;
uniform float  uSaturation;
uniform float  uAdaptive;
uniform float4 uTint;
uniform float2 uLightDir;
uniform float  uSpecular;
uniform float  uShadow;
uniform float  uShadowSize;
uniform float  uDebug;

float sdRoundRect(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

// Smooth minimum (quadratic). k == 0 degenerates to min(), i.e. a hard union with a visible crease.
float smin(float a, float b, float k) {
    if (k <= 0.0) {
        return min(a, b);
    }
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

float sceneSdf(float2 c) {
    float d = sdRoundRect(c - uShape0.xy, uShape0.zw, uRadii.x);
    if (uCount > 1.5) {
        d = smin(d, sdRoundRect(c - uShape1.xy, uShape1.zw, uRadii.y), uSmooth);
    }
    if (uCount > 2.5) {
        d = smin(d, sdRoundRect(c - uShape2.xy, uShape2.zw, uRadii.z), uSmooth);
    }
    if (uCount > 3.5) {
        d = smin(d, sdRoundRect(c - uShape3.xy, uShape3.zw, uRadii.w), uSmooth);
    }
    return d;
}

// Outward unit normal, by central difference. No sign() anywhere, so there is no artificial
// singularity; on the true medial axis the gradient is genuinely zero and phi is zero there too.
float2 sceneNormal(float2 c) {
    float e = 1.0;
    float dx = sceneSdf(c + float2(e, 0.0)) - sceneSdf(c - float2(e, 0.0));
    float dy = sceneSdf(c + float2(0.0, e)) - sceneSdf(c - float2(0.0, e));
    float2 g = float2(dx, dy);
    float l = length(g);
    if (l < 0.00001) {
        return float2(0.0, 0.0);
    }
    return g / l;
}

float4 tap(float2 uv) {
    return float4(content.eval(clamp(uv, float2(0.5), uSize - float2(0.5))));
}

float2 refractOffset(float3 n, float cosi, float eta, float travel) {
    float k = 1.0 - eta * eta * (1.0 - cosi * cosi);
    if (k < 0.0) {
        return float2(0.0);           // total internal reflection: no transmitted ray
    }
    float3 i = float3(0.0, 0.0, -1.0);
    float3 t = eta * i + (eta * cosi - sqrt(k)) * n;
    return t.xy / max(-t.z, 0.001) * travel;
}

half4 main(float2 coord) {
    float d = sceneSdf(coord);

    // ---- Outside the glass: pass through, plus an optional contact shadow. ----
    if (d > 0.0) {
        float4 src = tap(coord);
        if (uShadow > 0.001 && d < uShadowSize) {
            float t = 1.0 - smoothstep(0.0, uShadowSize, d);
            src.rgb = src.rgb * (1.0 - t * t * uShadow);
        }
        return half4(half3(src.rgb), half(src.a));
    }

    // ---- 1. Bevel geometry ----
    float u = -d;
    float x = clamp(u / max(uBevel, 0.5), 0.0, 1.0);
    float k = 1.0 - x;

    float phi = uEdgeAngle * k;
    float sp  = sin(phi);
    float cp  = cos(phi);

    float denom  = max(1.0 - cos(uEdgeAngle), 0.001);
    float height = max(uThickness * (1.0 - (1.0 - cp) / denom), 0.0);

    float2 n2 = sceneNormal(coord);
    float3 nLight = float3(n2 * sp, cp);        // true outward surface normal, kept for lighting

    // ---- 2. Overhang: past 90 degrees the ray strikes the surface from behind. Flipping the
    //         normal reverses T.xy, which is what samples OUTSIDE the shape and wraps the
    //         surroundings into the rim. ----
    float3 n = nLight;
    float cosi = n.z;
    float overhang = 0.0;
    if (cosi < 0.0) {
        overhang = clamp(-cosi / 0.35, 0.0, 1.0);
        n = -n;
        cosi = -cosi;
    }
    // Local height collapses to 0 at the outline, so the overhang band borrows an effective path
    // length instead. Model, not Snell — see the KDoc.
    float travel = mix(height, max(height, uRimReach), overhang);

    // ---- 3. Refraction, one evaluation per channel ----
    float2 offR = refractOffset(n, cosi, 1.0 / max(uIor - uDispersion, 1.0), travel);
    float2 offG = refractOffset(n, cosi, 1.0 / max(uIor,               1.0), travel);
    float2 offB = refractOffset(n, cosi, 1.0 / max(uIor + uDispersion, 1.0), travel);

    // ---- 4. Interior magnification: exact, applied about the optical centre ----
    float2 base = coord - (coord - uOptical) * (1.0 - 1.0 / max(uMagnify, 0.01));

    // ---- 5. Backdrop fetch ----
    float3 col;
    float  alpha;
    if (uBlur < 0.5) {
        float4 g = tap(base + offG);
        col   = float3(tap(base + offR).r, g.g, tap(base + offB).b);
        alpha = g.a;
    } else {
        float3 acc = float3(0.0);
        for (int i = 0; i < 8; i++) {
            float fi  = float(i);
            float ang = 2.39996323 * fi;                    // golden angle -> even Poisson disc
            float rad = uBlur * sqrt((fi + 0.5) / 8.0);
            float2 o  = float2(cos(ang), sin(ang)) * rad;
            acc.r = acc.r + tap(base + offR + o).r;
            acc.g = acc.g + tap(base + offG + o).g;
            acc.b = acc.b + tap(base + offB + o).b;
        }
        col   = acc / 8.0;
        alpha = tap(base + offG).a;
    }

    // ---- 6. Vibrancy. These lines are the only ones visible over a flat-coloured backdrop, which
    //         is why refraction alone always reads as "just transparent". ----
    float lum = dot(col, float3(0.2126, 0.7152, 0.0722));
    col = mix(float3(lum), col, uSaturation);
    col = col + float3((0.5 - lum) * uAdaptive);
    col = mix(col, uTint.rgb, uTint.a);

    // ---- 7. Lighting, from the same normal the refraction used, so the highlight sits exactly on
    //         the curvature instead of being a separate gradient laid on top. ----
    float3 l = normalize(float3(uLightDir, 0.9));
    float3 v = float3(0.0, 0.0, 1.0);
    float3 h = normalize(l + v);
    float nz = clamp(nLight.z, 0.0, 1.0);

    float fres = pow(1.0 - nz, 4.0);
    float spec = pow(max(dot(nLight, h), 0.0), 64.0);
    col = col + float3(fres * 0.20 + spec * 0.90) * uSpecular;

    float shade = max(dot(nLight.xy, -uLightDir), 0.0) * (1.0 - nz);
    col = col * (1.0 - shade * 0.28 * uSpecular);

    if (uDebug > 0.5) {
        col = mix(col, float3(1.0, 0.0, 0.2), 0.45);
    }

    return half4(half3(clamp(col, 0.0, 1.0)), half(alpha));
}
"""

/**
 * Applies the glass to a **backdrop** layer.
 *
 * Put this on the composable that contains the scrolling content, not on the glass surface itself.
 * The glass surface is a sibling drawn afterwards and must have a transparent background.
 *
 * All coordinates in [shape] are in this layer's own pixel space. If the layer is offset from the
 * window (a status-bar inset, for example), measuring the glass with `boundsInRoot()` instead of
 * `boundsInParent()` places the lens off by exactly that inset — set [debugTint] and check.
 *
 * No-op when disabled, below API 33, or before the shape has been measured.
 */
@Composable
fun Modifier.auronGlass(
    shape: GlassShape,
    material: GlassMaterial,
    debugTint: Boolean = false,
): Modifier {
    if (!AuronGlass.ENABLED) return this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    if (shape.blobs.any { it.halfWidth <= 0f || it.halfHeight <= 0f }) return this

    val density = LocalDensity.current
    val shader = remember { RuntimeShader(GLASS_AGSL) }
    val effect = remember(shader) {
        AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }

    val px = remember(material, shape, density) {
        with(density) {
            // The bevel cannot be deeper than the glass is thick, nor deeper than the thinnest
            // blob's inradius, or the profile would never reach its flat top.
            val minInradius = shape.blobs.minOf { min(it.halfWidth, it.halfHeight) }
            val thickness = material.thickness.toPx()
            MaterialPx(
                thickness = thickness,
                bevel = material.bevel.toPx().coerceIn(0.5f, min(thickness, minInradius)),
                edgeAngle = Math.toRadians(material.edgeAngleDeg.toDouble()).toFloat()
                    .coerceIn(0.05f, 2.7f),
                rimReach = material.rimReach.toPx(),
                blur = material.blurRadius.toPx(),
                shadowSize = material.shadowSize.toPx().coerceAtLeast(0.5f),
                merge = shape.mergeRadius.toPx(),
            )
        }
    }

    val lightRad = Math.toRadians(material.lightAngleDeg.toDouble())
    val lightX = cos(lightRad).toFloat()
    val lightY = sin(lightRad).toFloat()

    // Uniforms are written inside the graphicsLayer block. Re-applying the modifier re-runs the
    // block, and re-assigning renderEffect marks the RenderNode dirty so the layer is re-recorded
    // with the new values. A LaunchedEffect that only calls setFloatUniform is not enough.
    return this.graphicsLayer {
        shader.setFloatUniform("uSize", size.width, size.height)

        // Unused slots are collapsed onto blob 0 so every branch in sceneSdf stays well defined
        // even though uCount gates them.
        val blobs = shape.blobs
        for (i in 0 until AuronGlass.MAX_BLOBS) {
            val b = blobs.getOrElse(i) { blobs[0] }
            shader.setFloatUniform("uShape$i", b.centerX, b.centerY, b.halfWidth, b.halfHeight)
        }
        shader.setFloatUniform(
            "uRadii",
            blobs[0].cornerRadius,
            blobs.getOrElse(1) { blobs[0] }.cornerRadius,
            blobs.getOrElse(2) { blobs[0] }.cornerRadius,
            blobs.getOrElse(3) { blobs[0] }.cornerRadius,
        )
        shader.setFloatUniform("uCount", blobs.size.toFloat())
        shader.setFloatUniform("uSmooth", px.merge)

        shader.setFloatUniform("uOptical", shape.opticalCenterX, shape.opticalCenterY)
        shader.setFloatUniform("uMagnify", material.magnify)

        shader.setFloatUniform("uThickness", px.thickness)
        shader.setFloatUniform("uBevel", px.bevel)
        shader.setFloatUniform("uEdgeAngle", px.edgeAngle)
        shader.setFloatUniform("uRimReach", px.rimReach)
        shader.setFloatUniform("uIor", material.ior)
        shader.setFloatUniform("uDispersion", material.dispersion)

        shader.setFloatUniform("uBlur", px.blur)
        shader.setFloatUniform("uSaturation", material.saturation)
        shader.setFloatUniform("uAdaptive", material.adaptive)
        shader.setFloatUniform(
            "uTint",
            material.tint.red, material.tint.green, material.tint.blue, material.tint.alpha,
        )
        shader.setFloatUniform("uLightDir", lightX, lightY)
        shader.setFloatUniform("uSpecular", material.specular)
        shader.setFloatUniform("uShadow", material.shadow)
        shader.setFloatUniform("uShadowSize", px.shadowSize)
        shader.setFloatUniform("uDebug", if (debugTint) 1f else 0f)

        renderEffect = effect
    }
}

private data class MaterialPx(
    val thickness: Float,
    val bevel: Float,
    val edgeAngle: Float,
    val rimReach: Float,
    val blur: Float,
    val shadowSize: Float,
    val merge: Float,
)
