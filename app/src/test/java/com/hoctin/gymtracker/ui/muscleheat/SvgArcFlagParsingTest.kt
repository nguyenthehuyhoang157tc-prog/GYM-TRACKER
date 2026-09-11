package com.hoctin.gymtracker.ui.muscleheat

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MUSCLE-3B.1 — root cause + fix verification for the back-map artifact.
 *
 * Root cause (verified): the upstream SVG paths use the compact arc syntax with PACKED flags
 * (`a910.025 910.025 0 0136.94-.25` = flags `0`,`1`, then x `36.94`). Compose's [PathParser]
 * tokenizes `0136.94` as one number, shifting the remaining arc parameters — the 910-radius arcs
 * in the back `head` path then drew a displaced blob beside the figure.
 *
 * Fix: [normalizeSvgArcFlags] re-tokenizes with an arc-aware scanner and re-emits the same path
 * with explicit separators. These tests prove the normalized packed form parses to EXACTLY the
 * node list of the hand-separated form, and that the entire ported inventory parses cleanly.
 * (Pure JVM: `toNodes()` returns plain Kotlin data.)
 */
class SvgArcFlagParsingTest {

    private fun nodesOf(d: String) = PathParser().parsePathString(d).toNodes()

    @Test fun normalizer_fixesPackedFlags_simpleArc() {
        assertEquals(
            nodesOf("M0 0 a1 1 0 0 1 1 1 z"),
            nodesOf(normalizeSvgArcFlags("M0 0a1 1 0 011 1z")),
        )
    }

    @Test fun normalizer_fixesPackedFlags_negativeCoordinate_fromNeckPath() {
        // Exact shape found in the upstream back `neck` path: flags packed against `-.36`.
        assertEquals(
            nodesOf("M10 10 a 0.62 0.61 25.9 0 1 -0.36 -1.03 z"),
            nodesOf(normalizeSvgArcFlags("M10 10a.62.61 25.9 01-.36-1.03z")),
        )
    }

    @Test fun normalizer_fixesPackedFlags_hugeRadius_fromHeadPath() {
        // The 910-radius arc from the upstream back `head` path — the visible artifact source.
        assertEquals(
            nodesOf("M100 100 a 910.025 910.025 0 0 1 36.94 -0.25 z"),
            nodesOf(normalizeSvgArcFlags("M100 100a910.025 910.025 0 0136.94-.25z")),
        )
    }

    @Test fun normalizer_isNoOpForNonArcGeometry() {
        // Curves/lines/multi-dot shorthand must parse identically after normalization.
        val d = "M272.91 422.84c-18.95-17.19-22-57-12.64-78.79q20.36-2.26 37.02.75l1.5-2.5z"
        assertEquals(nodesOf(d), nodesOf(normalizeSvgArcFlags(d)))
    }

    @Test fun entirePortedInventory_parsesCleanly_afterNormalization() {
        // Regression net: every ported body path + both outlines parse to a non-empty node list.
        (BODY_FRONT_PATHS.values + BODY_BACK_PATHS.values).flatten().forEach { d ->
            assertTrue("empty parse for path: ${d.take(40)}…", nodesOf(normalizeSvgArcFlags(d)).isNotEmpty())
        }
        assertTrue(nodesOf(normalizeSvgArcFlags(BODY_FRONT_OUTLINE)).isNotEmpty())
        assertTrue(nodesOf(normalizeSvgArcFlags(BODY_BACK_OUTLINE)).isNotEmpty())
    }
}
