package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.catalog.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MUSCLE-2 — deterministic verification of the body-region mapping + UI contract. Pure JVM; no Android.
 */
class MuscleBodyRegionMappingTest {

    /** Muscle groups the catalog (`catalog_fed_v1.json`, 577 entries) actually uses — verified in MUSCLE-2. */
    private val catalogUsedMuscles = setOf(
        MuscleGroup.ABS, MuscleGroup.ABDUCTORS, MuscleGroup.ADDUCTORS, MuscleGroup.BICEPS,
        MuscleGroup.CALVES, MuscleGroup.CHEST, MuscleGroup.FOREARMS, MuscleGroup.GLUTES,
        MuscleGroup.HAMSTRINGS, MuscleGroup.LATS, MuscleGroup.LOWER_BACK, MuscleGroup.MID_BACK,
        MuscleGroup.QUADS, MuscleGroup.SHOULDERS, MuscleGroup.TRAPS, MuscleGroup.TRICEPS,
    )

    private fun heat(
        muscle: MuscleGroup,
        score: Double,
        band: HeatColorBand = HeatColorBand.YELLOW_ORANGE,
        status: MuscleStatus = MuscleStatus.STIMULATED,
        reason: String = "reason",
    ) = MuscleHeatResult(muscle, score, band, status, reason, null, 0.5)

    // ---- Coverage --------------------------------------------------------------------

    @Test fun everyMuscleGroup_isCovered_mappedOrSummaryOnly() {
        MuscleGroup.entries.forEach { muscle ->
            val mapping = MuscleBodyRegions.mappingFor(muscle)
            assertNotNull("no mapping for $muscle", mapping)
            when (mapping) {
                is MuscleRegionMapping.Mapped ->
                    assertTrue("empty region list for $muscle", mapping.regions.isNotEmpty())
                is MuscleRegionMapping.SummaryOnly ->
                    assertTrue("blank summary reason for $muscle", mapping.reason.isNotBlank())
            }
        }
    }

    @Test fun exactly24MuscleGroups_and8AreUnusedByCatalog() {
        assertEquals(24, MuscleGroup.entries.size)
        val unused = MuscleGroup.entries.filterNot { it in catalogUsedMuscles }.toSet()
        assertEquals(
            setOf(
                MuscleGroup.UPPER_CHEST, MuscleGroup.BACK, MuscleGroup.FRONT_DELTS,
                MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.OBLIQUES,
                MuscleGroup.CORE, MuscleGroup.FULL_BODY,
            ),
            unused,
        )
    }

    @Test fun everyCatalogUsedPrimaryAndSecondaryMuscle_mapsToARegion() {
        // Catalog primary and secondary use the SAME 16 muscles — all must be paintable regions.
        catalogUsedMuscles.forEach { muscle ->
            val mapping = MuscleBodyRegions.mappingFor(muscle)
            assertTrue("$muscle should map to a region, not summary-only",
                mapping is MuscleRegionMapping.Mapped)
        }
    }

    @Test fun everyColorBand_mapsToAToken() {
        HeatColorBand.entries.forEach { band ->
            assertNotNull("no token for $band", MuscleHeatUiMapper.colorTokenFor(band))
        }
        assertEquals(MuscleHeatColorToken.HEAT_NONE, MuscleHeatUiMapper.colorTokenFor(HeatColorBand.NONE))
        assertEquals(MuscleHeatColorToken.HEAT_LIGHT, MuscleHeatUiMapper.colorTokenFor(HeatColorBand.LIGHT_YELLOW))
        assertEquals(MuscleHeatColorToken.HEAT_MODERATE, MuscleHeatUiMapper.colorTokenFor(HeatColorBand.YELLOW_ORANGE))
        assertEquals(MuscleHeatColorToken.HEAT_HIGH, MuscleHeatUiMapper.colorTokenFor(HeatColorBand.ORANGE_RED))
        assertEquals(MuscleHeatColorToken.HEAT_MAX, MuscleHeatUiMapper.colorTokenFor(HeatColorBand.DEEP_RED))
    }

    // ---- Special muscle rules ---------------------------------------------------------

    @Test fun fullBody_isSummaryOnly_neverAFakeRegion() {
        val mapping = MuscleBodyRegions.mappingFor(MuscleGroup.FULL_BODY)
        assertTrue(mapping is MuscleRegionMapping.SummaryOnly)
        val model = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.FULL_BODY, 5.0, HeatColorBand.ORANGE_RED, MuscleStatus.FATIGUED)),
        )
        assertTrue("FULL_BODY must not paint a region", model.regions.isEmpty())
        assertEquals(1, model.summaryOnly.size)                       // but stays visible
        assertEquals(MuscleGroup.FULL_BODY, model.summaryOnly.single().muscleGroup)
        assertEquals(MuscleHeatColorToken.HEAT_HIGH, model.summaryOnly.single().colorToken)
    }

    @Test fun core_mapsToAbsRegion_overlapWithAbs() {
        assertEquals(listOf(BodyRegionId.ABS), MuscleBodyRegions.regionsFor(MuscleGroup.CORE))
        assertEquals(listOf(BodyRegionId.ABS), MuscleBodyRegions.regionsFor(MuscleGroup.ABS))
        // CORE + ABS both present -> merge into the single ABS region.
        val model = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.ABS, 3.0), heat(MuscleGroup.CORE, 2.0)),
        )
        val abs = model.regions.single { it.regionId == BodyRegionId.ABS }
        assertTrue(abs.contributingMuscles.containsAll(listOf(MuscleGroup.ABS, MuscleGroup.CORE)))
    }

    @Test fun back_isBroadFallback_toLatsAndMidBack() {
        assertEquals(
            listOf(BodyRegionId.LATS, BodyRegionId.MID_BACK),
            MuscleBodyRegions.regionsFor(MuscleGroup.BACK),
        )
    }

    @Test fun midBack_paintsBothUpperBackAndTraps() {
        // MUSCLE-HEAT-1: one MID_BACK heat result overlays the traps region too (back-map readability).
        assertEquals(
            listOf(BodyRegionId.MID_BACK, BodyRegionId.TRAPS),
            MuscleBodyRegions.regionsFor(MuscleGroup.MID_BACK),
        )
        val model = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.MID_BACK, 4.0, HeatColorBand.ORANGE_RED, MuscleStatus.PRODUCTIVE)),
        )
        val midBack = model.regions.single { it.regionId == BodyRegionId.MID_BACK }
        val traps = model.regions.single { it.regionId == BodyRegionId.TRAPS }
        // The SAME single result paints both regions — identical band/intensity, no duplicated data.
        assertEquals(midBack.colorBand, traps.colorBand)
        assertEquals(midBack.intensity, traps.intensity, 1e-9)
        assertEquals(listOf(MuscleGroup.MID_BACK), midBack.contributingMuscles)
        assertEquals(listOf(MuscleGroup.MID_BACK), traps.contributingMuscles)
    }

    @Test fun shoulders_aggregate_andAllDeltHeads_mapToShoulderRegion() {
        listOf(
            MuscleGroup.SHOULDERS, MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS,
        ).forEach { delt ->
            assertEquals("$delt should map to SHOULDERS", listOf(BodyRegionId.SHOULDERS),
                MuscleBodyRegions.regionsFor(delt))
        }
    }

    @Test fun forearms_isVisibleRegion() {
        assertEquals(listOf(BodyRegionId.FOREARMS), MuscleBodyRegions.regionsFor(MuscleGroup.FOREARMS))
        val model = MuscleHeatUiMapper.toUiModel(listOf(heat(MuscleGroup.FOREARMS, 2.0)))
        assertEquals(BodyRegionId.FOREARMS, model.regions.single().regionId)
    }

    @Test fun adductorsAndAbductors_areHandled_asDistinctRegions() {
        assertEquals(listOf(BodyRegionId.ADDUCTORS), MuscleBodyRegions.regionsFor(MuscleGroup.ADDUCTORS))
        assertEquals(listOf(BodyRegionId.ABDUCTORS), MuscleBodyRegions.regionsFor(MuscleGroup.ABDUCTORS))
        val model = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.ADDUCTORS, 1.0), heat(MuscleGroup.ABDUCTORS, 1.0)),
        )
        assertEquals(2, model.regions.size)
    }

    @Test fun upperChest_foldsIntoChestRegion() {
        assertEquals(listOf(BodyRegionId.CHEST), MuscleBodyRegions.regionsFor(MuscleGroup.UPPER_CHEST))
    }

    // ---- Merge rules ------------------------------------------------------------------

    @Test fun multipleMuscles_toOneRegion_mergeByMaxIntensity_preserveContributors() {
        // SHOULDERS (score 5, DEEP_RED/RECOVERY_RISK) + REAR_DELTS (score 2) -> one shoulder region.
        val model = MuscleHeatUiMapper.toUiModel(
            listOf(
                heat(MuscleGroup.REAR_DELTS, 2.0, HeatColorBand.YELLOW_ORANGE, MuscleStatus.STIMULATED, "rear"),
                heat(MuscleGroup.SHOULDERS, 5.0, HeatColorBand.DEEP_RED, MuscleStatus.RECOVERY_RISK, "shoulders"),
            ),
        )
        val shoulder = model.regions.single { it.regionId == BodyRegionId.SHOULDERS }
        assertEquals(5.0, shoulder.intensity, 1e-9)                    // max intensity wins the color
        assertEquals(HeatColorBand.DEEP_RED, shoulder.colorBand)
        assertEquals(MuscleHeatColorToken.HEAT_MAX, shoulder.colorToken)
        assertEquals(MuscleStatus.RECOVERY_RISK, shoulder.status)      // status follows the dominant muscle
        assertEquals("shoulders", shoulder.reason)
        // All contributing muscles preserved, deterministically ordered by enum declaration order.
        assertEquals(
            listOf(MuscleGroup.SHOULDERS, MuscleGroup.REAR_DELTS).sortedBy { it.ordinal },
            shoulder.contributingMuscles,
        )
    }

    @Test fun regionOrder_isDeterministic_regardlessOfInputOrder() {
        val a = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.CALVES, 1.0), heat(MuscleGroup.CHEST, 1.0), heat(MuscleGroup.ABS, 1.0)),
        ).regions.map { it.regionId }
        val b = MuscleHeatUiMapper.toUiModel(
            listOf(heat(MuscleGroup.ABS, 1.0), heat(MuscleGroup.CALVES, 1.0), heat(MuscleGroup.CHEST, 1.0)),
        ).regions.map { it.regionId }
        assertEquals(a, b)
        assertEquals(a, a.sortedBy { it.ordinal }) // sorted by BodyRegionId declaration order
    }

    // ---- Full pipeline + explainability ----------------------------------------------

    @Test fun sessionMuscleHeat_convertsToUiRegions_withContributingExercises() {
        // Build a realistic exposure so contributingExercises flows through end-to-end.
        val bench = ExerciseSessionSnapshot(
            canonicalExerciseKey = "catalog:bench-press",
            exerciseName = "Bench Press",
            trackingType = com.hoctin.gymtracker.data.catalog.TrackingType.REPS_WEIGHT,
            primaryMuscles = listOf(MuscleGroup.CHEST),
            secondaryMuscles = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            targetSets = 3,
            sets = (1..3).map {
                com.hoctin.gymtracker.data.LoggedSetEntity(
                    id = it.toLong(), sessionId = 1L, exerciseId = 1L, exerciseNameSnapshot = "Bench Press",
                    setNumber = it, weight = 60.0, reps = 8, rir = 2, notes = null,
                    isPrWeight = false, isPrReps = false, canonicalExerciseKey = "catalog:bench-press",
                )
            },
        )
        val exposure = MuscleExposureCalculator.compute(listOf(bench))
        val session = SessionMuscleHeat(exposure = exposure, heat = MuscleHeatEngine.analyze(exposure))
        val model = MuscleHeatUiMapper.toUiModel(session)

        val chest = model.regions.single { it.regionId == BodyRegionId.CHEST }
        assertEquals(listOf("Bench Press"), chest.contributingExercises)
        assertEquals(listOf(MuscleGroup.CHEST), chest.contributingMuscles)
        // Triceps + shoulders (secondary) also become regions, each explained by the same exercise.
        assertTrue(model.regions.any { it.regionId == BodyRegionId.TRICEPS })
        assertTrue(model.regions.any { it.regionId == BodyRegionId.SHOULDERS })
        assertEquals(listOf("Bench Press"),
            model.regions.single { it.regionId == BodyRegionId.TRICEPS }.contributingExercises)
    }

    @Test fun customUnattributedExercises_areVisible_notRegions() {
        val custom = ExerciseSessionSnapshot(
            canonicalExerciseKey = "custom:weird-thing",
            exerciseName = "Weird Thing",
            trackingType = com.hoctin.gymtracker.data.catalog.TrackingType.REPS_WEIGHT,
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            targetSets = 3,
            sets = (1..3).map {
                com.hoctin.gymtracker.data.LoggedSetEntity(
                    id = it.toLong(), sessionId = 1L, exerciseId = 1L, exerciseNameSnapshot = "Weird Thing",
                    setNumber = it, weight = 40.0, reps = 10, rir = 2, notes = null,
                    isPrWeight = false, isPrReps = false, canonicalExerciseKey = "custom:weird-thing",
                )
            },
        )
        val exposure = MuscleExposureCalculator.compute(listOf(custom))
        val model = MuscleHeatUiMapper.toUiModel(SessionMuscleHeat(exposure, MuscleHeatEngine.analyze(exposure)))
        assertTrue(model.regions.isEmpty())
        assertEquals(listOf("Weird Thing"), model.unattributedExercises)
        assertTrue(model.summaryOnly.isEmpty())
    }

    @Test fun emptyResults_produceEmptyModel_noCrash() {
        val model = MuscleHeatUiMapper.toUiModel(emptyList())
        assertTrue(model.regions.isEmpty())
        assertTrue(model.summaryOnly.isEmpty())
        assertTrue(model.unattributedExercises.isEmpty())
    }
}
