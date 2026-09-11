package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.catalog.MuscleGroup

/**
 * MUSCLE-2 — UI contract for the (future) after-save body map.
 *
 * Converts Layer-B [MuscleHeatResult]s into merged, deterministic, UI-ready [MuscleHeatUiRegion]s plus
 * the summary-only / unattributed items that must stay visible but are not paintable regions. Pure domain
 * code: **no Compose, no hardcoded colors** — regions carry a [MuscleHeatColorToken] the composable will
 * resolve to a theme color later. The exposure↔meaning split from Layer B is preserved: [colorBand]/
 * [colorToken]/[intensity] are the exposure axis, [status] is the meaning axis.
 */

/** Centralized color token per heat band. Composables resolve these to theme colors — never hardcode. */
enum class MuscleHeatColorToken { HEAT_NONE, HEAT_LIGHT, HEAT_MODERATE, HEAT_HIGH, HEAT_MAX }

/** One paintable body-map region after merging every muscle that maps to it. */
data class MuscleHeatUiRegion(
    val regionId: BodyRegionId,
    val displayName: String,
    val side: BodyRegionSide,
    val colorBand: HeatColorBand,
    val colorToken: MuscleHeatColorToken,
    /** Exposure driving the color — the MAX contributing-muscle intensity (see merge rule). */
    val intensity: Double,
    val status: MuscleStatus,
    val reason: String,
    val contributingMuscles: List<MuscleGroup>,
    val contributingExercises: List<String>,
)

/** A muscle that is deliberately not a body region (e.g. FULL_BODY) — visible as a session summary. */
data class MuscleHeatSummaryItem(
    val muscleGroup: MuscleGroup,
    val displayName: String,
    val reason: String,
    val colorBand: HeatColorBand,
    val colorToken: MuscleHeatColorToken,
    val intensity: Double,
    val status: MuscleStatus,
    val contributingExercises: List<String>,
)

/** The complete UI model MUSCLE-3 will render. Runtime-only, never stored. */
data class MuscleHeatUiModel(
    /** Paintable regions, deterministic order (by [BodyRegionId] declaration order). */
    val regions: List<MuscleHeatUiRegion>,
    /** Summary-only muscles (e.g. FULL_BODY) — shown, but not painted as a fake region. */
    val summaryOnly: List<MuscleHeatSummaryItem>,
    /** Custom / uncategorized exercises with no catalog anatomy — listed so they are never silently lost. */
    val unattributedExercises: List<String>,
)

object MuscleHeatUiMapper {

    /** Total-coverage band → token mapping (exhaustive `when`, no `else`). */
    fun colorTokenFor(band: HeatColorBand): MuscleHeatColorToken = when (band) {
        HeatColorBand.NONE -> MuscleHeatColorToken.HEAT_NONE
        HeatColorBand.LIGHT_YELLOW -> MuscleHeatColorToken.HEAT_LIGHT
        HeatColorBand.YELLOW_ORANGE -> MuscleHeatColorToken.HEAT_MODERATE
        HeatColorBand.ORANGE_RED -> MuscleHeatColorToken.HEAT_HIGH
        HeatColorBand.DEEP_RED -> MuscleHeatColorToken.HEAT_MAX
    }

    /**
     * Full mapping from a computed session (heat + exposure). Uses the exposure contributors to attach
     * `contributingExercises` per region. This is the primary entry point for MUSCLE-3.
     */
    fun toUiModel(sessionMuscleHeat: SessionMuscleHeat): MuscleHeatUiModel {
        val exercisesByMuscle: Map<MuscleGroup, List<String>> = sessionMuscleHeat.exposure.muscles
            .associate { exposure ->
                exposure.muscleGroup to exposure.exerciseContributors.map { it.exerciseName }
            }
        val unattributed = sessionMuscleHeat.exposure.unattributedContributors
            .map { it.exerciseName }
            .distinct()
            .sorted()
        return build(sessionMuscleHeat.heat, exercisesByMuscle, unattributed)
    }

    /**
     * Heat-only mapping (no exposure contributors available) — `contributingExercises` is empty. Kept for
     * callers/tests that only hold the [MuscleHeatResult] list.
     */
    fun toUiModel(results: List<MuscleHeatResult>): MuscleHeatUiModel =
        build(results, emptyMap(), emptyList())

    private fun build(
        results: List<MuscleHeatResult>,
        exercisesByMuscle: Map<MuscleGroup, List<String>>,
        unattributed: List<String>,
    ): MuscleHeatUiModel {
        val perRegion = LinkedHashMap<BodyRegionId, MutableList<MuscleHeatResult>>()
        val summary = mutableListOf<MuscleHeatSummaryItem>()

        results.forEach { result ->
            when (val mapping = MuscleBodyRegions.mappingFor(result.muscleGroup)) {
                is MuscleRegionMapping.Mapped ->
                    mapping.regions.forEach { region ->
                        perRegion.getOrPut(region) { mutableListOf() }.add(result)
                    }
                is MuscleRegionMapping.SummaryOnly ->
                    summary += summaryItem(result, exercisesByMuscle, mapping.reason)
            }
        }

        val regions = perRegion.entries
            .map { (region, contributors) -> mergeRegion(region, contributors, exercisesByMuscle) }
            .sortedBy { it.regionId.ordinal } // deterministic order, independent of input order
        return MuscleHeatUiModel(
            regions = regions,
            summaryOnly = summary.sortedBy { it.muscleGroup.ordinal },
            unattributedExercises = unattributed,
        )
    }

    /**
     * Merge rule for several muscles → one region: the **max-intensity** muscle is dominant. Color/band/
     * intensity and status/reason all follow that dominant muscle (so the two axes stay mutually
     * consistent); every contributing muscle and exercise is preserved for explainability. Ties break
     * deterministically by [MuscleGroup] declaration order.
     */
    private fun mergeRegion(
        region: BodyRegionId,
        contributors: List<MuscleHeatResult>,
        exercisesByMuscle: Map<MuscleGroup, List<String>>,
    ): MuscleHeatUiRegion {
        val dominant = contributors
            .sortedWith(compareByDescending<MuscleHeatResult> { it.finalIntensityScore }
                .thenBy { it.muscleGroup.ordinal })
            .first()
        val muscles = contributors.map { it.muscleGroup }.distinct().sortedBy { it.ordinal }
        val exercises = contributors
            .flatMap { exercisesByMuscle[it.muscleGroup].orEmpty() }
            .distinct()
            .sorted()
        return MuscleHeatUiRegion(
            regionId = region,
            displayName = region.displayName,
            side = region.side,
            colorBand = dominant.colorBand,
            colorToken = colorTokenFor(dominant.colorBand),
            intensity = dominant.finalIntensityScore,
            status = dominant.status,
            reason = dominant.reason,
            contributingMuscles = muscles,
            contributingExercises = exercises,
        )
    }

    private fun summaryItem(
        result: MuscleHeatResult,
        exercisesByMuscle: Map<MuscleGroup, List<String>>,
        reason: String,
    ) = MuscleHeatSummaryItem(
        muscleGroup = result.muscleGroup,
        displayName = result.muscleGroup.name.lowercase().replace('_', ' ')
            .replaceFirstChar { it.uppercase() },
        reason = reason,
        colorBand = result.colorBand,
        colorToken = colorTokenFor(result.colorBand),
        intensity = result.finalIntensityScore,
        status = result.status,
        contributingExercises = exercisesByMuscle[result.muscleGroup].orEmpty().distinct().sorted(),
    )
}
