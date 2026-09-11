package com.hoctin.gymtracker.ui.muscleheat

import com.hoctin.gymtracker.domain.muscleheat.BodyRegionId
import com.hoctin.gymtracker.domain.muscleheat.BodyRegionSide
import com.hoctin.gymtracker.domain.muscleheat.HeatColorBand
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatColorToken
import com.hoctin.gymtracker.domain.muscleheat.MuscleHeatUiRegion
import com.hoctin.gymtracker.domain.muscleheat.MuscleStatus
import com.hoctin.gymtracker.ui.review.formatDurationMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MUSCLE-3 — deterministic contract tests for the body-map port: ported slug/path data, the
 * BodyRegionId → slug mapping, the centralized heat palette, and graceful missing-region behavior.
 * Pure JVM (path data are plain strings; palette is ARGB longs; no Compose runtime).
 */
class BodyMapContractTest {

    private fun region(
        id: BodyRegionId,
        token: MuscleHeatColorToken,
        intensity: Double = 3.0,
    ) = MuscleHeatUiRegion(
        regionId = id,
        displayName = id.displayName,
        side = BodyRegionSide.BOTH,
        colorBand = HeatColorBand.YELLOW_ORANGE,
        colorToken = token,
        intensity = intensity,
        status = MuscleStatus.STIMULATED,
        reason = "r",
        contributingMuscles = emptyList(),
        contributingExercises = emptyList(),
    )

    // ---- Ported asset data -------------------------------------------------------------

    @Test fun portedPathData_isPresent_andEveryPathIsAnSvgPath() {
        assertTrue(BODY_FRONT_PATHS.isNotEmpty())
        assertTrue(BODY_BACK_PATHS.isNotEmpty())
        (BODY_FRONT_PATHS + BODY_BACK_PATHS).forEach { (slug, paths) ->
            assertTrue("empty path list for $slug", paths.isNotEmpty())
            paths.forEach { d ->
                assertTrue("path for $slug must start with M/m", d.startsWith("M") || d.startsWith("m"))
            }
        }
    }

    @Test fun everyMappedSlug_existsInPortedPathData() {
        BodyMapSlugs.frontSlugsByRegion.forEach { (regionId, slugs) ->
            slugs.forEach { slug ->
                assertTrue("front slug '$slug' for $regionId missing", BODY_FRONT_PATHS.containsKey(slug))
            }
        }
        BodyMapSlugs.backSlugsByRegion.forEach { (regionId, slugs) ->
            slugs.forEach { slug ->
                assertTrue("back slug '$slug' for $regionId missing", BODY_BACK_PATHS.containsKey(slug))
            }
        }
    }

    @Test fun everyBodyRegion_isPaintableOnSomeSide_exceptDocumentedAbductors() {
        BodyRegionId.entries.forEach { regionId ->
            val paintable = BodyMapSlugs.frontSlugsByRegion.containsKey(regionId) ||
                BodyMapSlugs.backSlugsByRegion.containsKey(regionId)
            if (regionId == BodyRegionId.ABDUCTORS) {
                // Documented gap: the upstream asset sheet has no abductor shape.
                assertTrue("ABDUCTORS unexpectedly gained a slug — update mapping/tests", !paintable)
            } else {
                assertTrue("$regionId has no slug on either side", paintable)
            }
        }
    }

    // ---- Palette ------------------------------------------------------------------------

    @Test fun everyColorToken_resolvesInPalette_noneIsNeutral() {
        MuscleHeatColorToken.entries.forEach { token ->
            val argb = MuscleHeatPalette.heatArgb(token)
            if (token == MuscleHeatColorToken.HEAT_NONE) {
                assertNull("HEAT_NONE must be neutral (null)", argb)
            } else {
                assertTrue("no color for $token", argb != null)
            }
        }
        // Heat colors are distinct (no two bands render identically).
        val colors = MuscleHeatColorToken.entries.mapNotNull { MuscleHeatPalette.heatArgb(it) }
        assertEquals(colors.size, colors.toSet().size)
    }

    // ---- Slug token resolution (merge + graceful degradation) ---------------------------

    @Test fun sharedUpperBackSlug_takesMaxToken_ofLatsAndMidBack() {
        val tokens = BodyMapSlugs.slugTokens(
            BodyMapSide.BACK,
            listOf(
                region(BodyRegionId.LATS, MuscleHeatColorToken.HEAT_LIGHT),
                region(BodyRegionId.MID_BACK, MuscleHeatColorToken.HEAT_MAX),
            ),
        )
        assertEquals(MuscleHeatColorToken.HEAT_MAX, tokens["upper-back"])
    }

    @Test fun frontOnlyRegion_doesNotPaintOnBack_andMissingRegionDoesNotCrash() {
        val regions = listOf(
            region(BodyRegionId.CHEST, MuscleHeatColorToken.HEAT_HIGH),
            region(BodyRegionId.ABDUCTORS, MuscleHeatColorToken.HEAT_MAX), // unmapped: must be ignored
        )
        val back = BodyMapSlugs.slugTokens(BodyMapSide.BACK, regions)
        assertTrue("chest must not paint on the back figure", "chest" !in back)
        val front = BodyMapSlugs.slugTokens(BodyMapSide.FRONT, regions)
        assertEquals(MuscleHeatColorToken.HEAT_HIGH, front["chest"])
        assertTrue(front.values.none { it == MuscleHeatColorToken.HEAT_MAX }) // abductors ignored, no crash
    }

    @Test fun emptyRegions_produceNoTokens_neutralBody() {
        assertTrue(BodyMapSlugs.slugTokens(BodyMapSide.FRONT, emptyList()).isEmpty())
        assertTrue(BodyMapSlugs.slugTokens(BodyMapSide.BACK, emptyList()).isEmpty())
    }

    // ---- Duration formatting (review header/card, mockup style) --------------------------

    @Test fun durationFormatting_hidesUnknown_showsHoursAndMinutes() {
        assertNull(formatDurationMillis(0L))
        assertNull(formatDurationMillis(59_000L))          // < 1 min = unknown/noise → hidden
        assertEquals("1 min", formatDurationMillis(60_000L))
        assertEquals("38 mins", formatDurationMillis(38 * 60_000L))
        assertEquals("1 hour, 38 mins", formatDurationMillis(98 * 60_000L))
        assertEquals("2 hours, 1 min", formatDurationMillis(121 * 60_000L))
        assertEquals("2 hours", formatDurationMillis(2 * 60 * 60_000L))
        assertEquals("1 hour", formatDurationMillis(60 * 60_000L))
    }

    // ---- MUSCLE-3B: upstream slug inventory + silhouette outlines -------------------------

    @Test fun generatedSlugInventory_matchesUpstreamExactly() {
        // Verified against react-muscle-highlighter assets/bodyFront.ts + bodyBack.ts (MIT).
        assertEquals(
            setOf(
                "abs", "adductors", "ankles", "biceps", "calves", "chest", "deltoids", "feet",
                "forearm", "hair", "hands", "head", "knees", "neck", "obliques", "quadriceps",
                "tibialis", "trapezius", "triceps",
            ),
            BODY_FRONT_PATHS.keys,
        )
        assertEquals(
            setOf(
                "adductors", "ankles", "calves", "deltoids", "feet", "forearm", "gluteal", "hair",
                "hamstring", "hands", "head", "lower-back", "neck", "trapezius", "triceps",
                "upper-back",
            ),
            BODY_BACK_PATHS.keys,
        )
    }

    @Test fun silhouetteOutlines_present_andInTheirSidesCoordinateSpace() {
        assertTrue(BODY_FRONT_OUTLINE.startsWith("M"))
        assertTrue(BODY_BACK_OUTLINE.startsWith("M"))
        // Front outline is authored in the front viewBox (x < 724); back at x in [724, 1448].
        val frontX = BODY_FRONT_OUTLINE.removePrefix("M").trim().split(" ")[0].toDouble()
        val backX = BODY_BACK_OUTLINE.removePrefix("M").trim().split(" ")[0].toDouble()
        assertTrue("front outline x=$frontX", frontX < 724.0)
        assertTrue("back outline x=$backX", backX >= 724.0)
    }
}
