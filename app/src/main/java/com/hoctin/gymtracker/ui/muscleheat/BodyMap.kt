package com.hoctin.gymtracker.ui.muscleheat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.hoctin.gymtracker.domain.muscleheat.BodyRegionId
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatColorToken
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiRegion
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme

/**
 * MUSCLE-3 — native Compose body-map renderer.
 *
 * Draws the front/back human figure from SVG path data ported from **react-muscle-highlighter**
 * (MIT — see `docs/third_party_react_muscle_highlighter.md`; path data in [BODY_FRONT_PATHS] /
 * [BODY_BACK_PATHS]). No WebView, no bitmaps: paths are parsed once per side with Compose's
 * [PathParser] and filled on a [Canvas].
 *
 * Colors come ONLY from the centralized [MuscleHeatPalette] (per [MuscleHeatColorToken]) — no heat
 * colors are hardcoded in screen composables. Regions without heat (and non-muscle body parts like
 * head/hands/feet) use the neutral fill, so unknown/custom exercises can never fake a highlight.
 */

/** Which figure to draw. (Distinct from [com.hoctin.gymtracker.domain.muscleheat.BodyRegionSide], which can be BOTH.) */
enum class BodyMapSide { FRONT, BACK }

/** Centralized heat color tokens as ARGB longs (pure + unit-testable; composables wrap in [Color]). */
object MuscleHeatPalette {
    /** null = no heat → the renderer's neutral body fill. */
    fun heatArgb(token: MuscleHeatColorToken): Long? = when (token) {
        MuscleHeatColorToken.HEAT_NONE -> null
        MuscleHeatColorToken.HEAT_LIGHT -> 0xFFF2DF8A      // light yellow
        MuscleHeatColorToken.HEAT_MODERATE -> 0xFFF6A94A   // yellow-orange
        MuscleHeatColorToken.HEAT_HIGH -> 0xFFEE6B3B       // orange-red
        MuscleHeatColorToken.HEAT_MAX -> 0xFFC92F2F        // deep red
    }
}

/**
 * MUSCLE-2 [BodyRegionId] → ported asset slug(s), per side. Pure data (unit-tested against the
 * generated path maps). Notes from the MUSCLE-3 asset audit:
 *  - the asset has ONE `upper-back` shape → both LATS and MID_BACK map to it (max color wins);
 *  - the asset has no abductor shape → ABDUCTORS is not paintable (still visible in top-muscle
 *    lists; documented gap);
 *  - all remaining asset slugs (head/hair/neck/hands/feet/knees/ankles/tibialis) are neutral body
 *    parts that always use the neutral fill.
 */
object BodyMapSlugs {
    val frontSlugsByRegion: Map<BodyRegionId, List<String>> = mapOf(
        BodyRegionId.CHEST to listOf("chest"),
        BodyRegionId.SHOULDERS to listOf("deltoids"),
        BodyRegionId.BICEPS to listOf("biceps"),
        BodyRegionId.TRICEPS to listOf("triceps"),
        BodyRegionId.FOREARMS to listOf("forearm"),
        BodyRegionId.ABS to listOf("abs"),
        BodyRegionId.OBLIQUES to listOf("obliques"),
        BodyRegionId.TRAPS to listOf("trapezius"),
        BodyRegionId.QUADS to listOf("quadriceps"),
        BodyRegionId.CALVES to listOf("calves"),
        BodyRegionId.ADDUCTORS to listOf("adductors"),
    )

    val backSlugsByRegion: Map<BodyRegionId, List<String>> = mapOf(
        BodyRegionId.SHOULDERS to listOf("deltoids"),
        BodyRegionId.TRICEPS to listOf("triceps"),
        BodyRegionId.FOREARMS to listOf("forearm"),
        BodyRegionId.TRAPS to listOf("trapezius"),
        BodyRegionId.LATS to listOf("upper-back"),
        BodyRegionId.MID_BACK to listOf("upper-back"),
        BodyRegionId.LOWER_BACK to listOf("lower-back"),
        BodyRegionId.GLUTES to listOf("gluteal"),
        BodyRegionId.HAMSTRINGS to listOf("hamstring"),
        BodyRegionId.CALVES to listOf("calves"),
        BodyRegionId.ADDUCTORS to listOf("adductors"),
    )

    fun slugsByRegion(side: BodyMapSide): Map<BodyRegionId, List<String>> =
        if (side == BodyMapSide.FRONT) frontSlugsByRegion else backSlugsByRegion

    /**
     * Resolve each asset slug's heat token for [side] from the merged UI regions. When several
     * regions share a slug (LATS + MID_BACK → `upper-back`), the **max** token wins — deterministic
     * because [MuscleHeatColorToken] is declared coldest→hottest. Slugs with no heat are omitted
     * (→ neutral fill), so missing regions degrade gracefully instead of crashing.
     */
    fun slugTokens(side: BodyMapSide, regions: List<MuscleHeatUiRegion>): Map<String, MuscleHeatColorToken> {
        val mapping = slugsByRegion(side)
        val bySlug = mutableMapOf<String, MuscleHeatColorToken>()
        regions.forEach { region ->
            mapping[region.regionId].orEmpty().forEach { slug ->
                val existing = bySlug[slug]
                if (existing == null || region.colorToken.ordinal > existing.ordinal) {
                    bySlug[slug] = region.colorToken
                }
            }
        }
        return bySlug
    }

    /**
     * MUSCLE-4: recovery paints → per-side slug colors, max-fatigue winning shared slugs (e.g. the
     * one upstream `upper-back` shape shared by LATS/MID_BACK). Pure and unit-testable.
     */
    fun recoverySlugArgb(
        side: BodyMapSide,
        paints: Map<com.hoctin.gymtracker.domain.muscleheat.BodyRegionId, com.hoctin.gymtracker.domain.muscleheat.RecoveryPaint>,
    ): Map<String, Long> {
        val mapping = slugsByRegion(side)
        val bySlug = mutableMapOf<String, com.hoctin.gymtracker.domain.muscleheat.RecoveryPaint>()
        paints.forEach { (region, paint) ->
            mapping[region].orEmpty().forEach { slug ->
                val existing = bySlug[slug]
                if (existing == null || paint.fatigue > existing.fatigue) bySlug[slug] = paint
            }
        }
        return bySlug.mapValues { it.value.argb }
    }
}

private const val VIEWPORT_W = 724f
private const val VIEWPORT_H = 1448f

/**
 * MUSCLE-3B.1 — Android path-port correction (root cause of the back-map artifact).
 *
 * The upstream SVG paths use the compact arc syntax where the two arc FLAGS are written with no
 * separator before the next value (e.g. `a910.025 910.025 0 0136.94-.25`): per the SVG grammar the
 * flags are SINGLE characters, so `0136.94` must tokenize as `0`, `1`, `36.94`. Compose's
 * [PathParser] reads it as one number instead (verified by [SvgArcFlagParsingTest]), which shifts
 * every following arc parameter — the giant 910-radius arcs in the back `head` path then draw a
 * displaced blob next to the figure.
 *
 * This normalizer re-tokenizes the path with an arc-aware scanner and re-emits it with explicit
 * separators (flags split into single characters). It changes NOTHING about the geometry the
 * upstream asset specifies — it only writes the same path in the unambiguous form Compose parses
 * correctly.
 */
internal fun normalizeSvgArcFlags(d: String): String {
    val out = StringBuilder(d.length + 32)
    var i = 0
    var command = ' '
    var argIndex = 0
    fun isNumberStart(c: Char) = c.isDigit() || c == '-' || c == '+' || c == '.'
    while (i < d.length) {
        val c = d[i]
        when {
            c.isLetter() -> {
                command = c
                argIndex = 0
                out.append(c)
                i++
            }
            isNumberStart(c) -> {
                val isArc = command == 'a' || command == 'A'
                val isFlagPosition = isArc && (argIndex % 7 == 3 || argIndex % 7 == 4)
                if (isFlagPosition) {
                    // SVG flags are exactly one character ('0' or '1'), even when glued to the
                    // next number ("0136.94" = 0, 1, 36.94).
                    out.append(' ').append(c)
                    i++
                } else {
                    // Consume one full number: optional sign, digits, at most one decimal point
                    // (".62.61" is two numbers), stopping at signs/letters/separators.
                    val start = i
                    if (d[i] == '-' || d[i] == '+') i++
                    var dotSeen = false
                    while (i < d.length) {
                        val ch = d[i]
                        when {
                            ch.isDigit() -> i++
                            ch == '.' && !dotSeen -> { dotSeen = true; i++ }
                            else -> break
                        }
                    }
                    out.append(' ').append(d, start, i)
                }
                argIndex++
            }
            else -> {
                // Separators (spaces/commas) — the re-emitter adds its own, just skip.
                i++
            }
        }
    }
    return out.toString()
}

/**
 * One body figure (front or back), scaled to fit and centered. [regions] is the merged MUSCLE-2 UI
 * model output; anything not mapped for this side simply stays neutral.
 *
 * MUSCLE-3B rendering order (matches upstream + the approved mockup):
 *  1. the upstream silhouette **outline** (stroke, light gray) — drawn first, exactly like the
 *     upstream `SvgMaleWrapper` renders its border `<g>` before the parts;
 *  2. every body part **fill** (heat color or neutral charcoal), each with a thin dark **separator
 *     stroke** so adjacent highlighted regions (e.g. trapezius + upper-back) never fuse into one
 *     unreadable patch. Strokes only emphasize the upstream shape boundaries — the region geometry
 *     itself is the upstream SVG path, untouched.
 */
@Composable
fun BodyMapFigure(
    side: BodyMapSide,
    regions: List<MuscleHeatUiRegion>,
    modifier: Modifier = Modifier,
    neutralColor: Color = if (isAuronDarkTheme()) Color(0xFF3B3E44) else Color(0xFFC7CBD2),
    outlineColor: Color = Color(0xFF9BA1AB),
    separatorColor: Color = Color(0xFF14161A),
) {
    val slugArgb = remember(side, regions) {
        BodyMapSlugs.slugTokens(side, regions)
            .mapNotNull { (slug, token) -> MuscleHeatPalette.heatArgb(token)?.let { slug to it } }
            .toMap()
    }
    BodyMapFigureCustom(
        side = side,
        slugArgb = slugArgb,
        modifier = modifier,
        neutralColor = neutralColor,
        outlineColor = outlineColor,
        separatorColor = separatorColor,
    )
}

/**
 * MUSCLE-4: core renderer taking pre-resolved slug colors, so BOTH color systems (session-heat
 * tokens and the separate recovery tokens) can drive the same figure without mixing semantics.
 * Slugs without a color use the neutral fill.
 */
@Composable
fun BodyMapFigureCustom(
    side: BodyMapSide,
    slugArgb: Map<String, Long>,
    modifier: Modifier = Modifier,
    neutralColor: Color = if (isAuronDarkTheme()) Color(0xFF3B3E44) else Color(0xFFC7CBD2),
    outlineColor: Color = Color(0xFF9BA1AB),
    separatorColor: Color = Color(0xFF14161A),
) {
    // Parse each side's path strings once; re-parsing on every recomposition would be wasteful.
    // normalizeSvgArcFlags fixes Compose's packed-arc-flag tokenization (see its doc) before parsing.
    val parsed: Map<String, List<Path>> = remember(side) {
        val source = if (side == BodyMapSide.FRONT) BODY_FRONT_PATHS else BODY_BACK_PATHS
        source.mapValues { (_, pathData) ->
            pathData.map { PathParser().parsePathString(normalizeSvgArcFlags(it)).toPath() }
        }
    }
    val outline: Path = remember(side) {
        val d = if (side == BodyMapSide.FRONT) BODY_FRONT_OUTLINE else BODY_BACK_OUTLINE
        PathParser().parsePathString(normalizeSvgArcFlags(d)).toPath()
    }

    Canvas(modifier = modifier) {
        val scaleFactor = minOf(size.width / VIEWPORT_W, size.height / VIEWPORT_H)
        val offsetX = (size.width - VIEWPORT_W * scaleFactor) / 2f
        val offsetY = (size.height - VIEWPORT_H * scaleFactor) / 2f
        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            // Back figure paths (and outline) are authored at x in [724, 1448] in the upstream sheet.
            if (side == BodyMapSide.BACK) translate(left = -VIEWPORT_W, top = 0f)
        }) {
            // 1. Silhouette outline underneath (upstream draws its border before the parts).
            drawPath(path = outline, color = outlineColor, style = Stroke(width = 3f))
            // 2. Body parts: resolved fill (heat or recovery color) + thin dark separator stroke.
            parsed.forEach { (slug, paths) ->
                val fill = slugArgb[slug]?.let(::Color) ?: neutralColor
                paths.forEach { path ->
                    drawPath(path = path, color = fill)
                    drawPath(path = path, color = separatorColor, style = Stroke(width = 1.6f))
                }
            }
        }
    }
}
