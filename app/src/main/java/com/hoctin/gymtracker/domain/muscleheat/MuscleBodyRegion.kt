package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.catalog.MuscleGroup

/**
 * MUSCLE-2 — deterministic body-region mapping layer.
 *
 * Converts anatomical [MuscleGroup]s into UI-ready **body-map regions** so a later screen (MUSCLE-3) can
 * paint a front/back silhouette without any anatomy logic of its own. Pure data + a pure `when` mapping;
 * no Room, no Compose, no colors, no I/O.
 *
 * Catalog usage verified from `assets/catalog/catalog_fed_v1.json` (577 entries): only 16 of the 24
 * [MuscleGroup] values are actually used (both as primary and secondary) — the catalog uses the aggregate
 * `SHOULDERS` (not FRONT/SIDE/REAR_DELTS), `CHEST` (not UPPER_CHEST), `ABS` (not CORE/OBLIQUES), and the
 * specific back muscles (not the generic `BACK`). All 24 are still mapped here so custom exercises and any
 * future catalog values are covered; the `when` is exhaustive (no `else`) so a new enum value fails to
 * compile until it is mapped or explicitly marked summary-only.
 */

/** Which side(s) of a front/back body map a region appears on. */
enum class BodyRegionSide { FRONT, BACK, BOTH }

/**
 * A paintable body-map region. Broader than [MuscleGroup]: several anatomical muscles collapse to one
 * drawable region (e.g. the three deltoid heads → [SHOULDERS]). [displayName]/[side] are UI-facing.
 */
enum class BodyRegionId(val displayName: String, val side: BodyRegionSide) {
    CHEST("Chest", BodyRegionSide.FRONT),
    SHOULDERS("Shoulders", BodyRegionSide.BOTH),
    BICEPS("Biceps", BodyRegionSide.FRONT),
    TRICEPS("Triceps", BodyRegionSide.BACK),
    FOREARMS("Forearms", BodyRegionSide.BOTH),
    ABS("Abs", BodyRegionSide.FRONT),
    OBLIQUES("Obliques", BodyRegionSide.FRONT),
    TRAPS("Traps", BodyRegionSide.BOTH),
    LATS("Lats", BodyRegionSide.BACK),
    MID_BACK("Mid Back", BodyRegionSide.BACK),
    LOWER_BACK("Lower Back", BodyRegionSide.BACK),
    QUADS("Quads", BodyRegionSide.FRONT),
    HAMSTRINGS("Hamstrings", BodyRegionSide.BACK),
    GLUTES("Glutes", BodyRegionSide.BACK),
    CALVES("Calves", BodyRegionSide.BACK),
    ADDUCTORS("Adductors", BodyRegionSide.FRONT),
    ABDUCTORS("Abductors", BodyRegionSide.BOTH),
}

/** Result of mapping one [MuscleGroup]: paintable region(s), or a documented summary-only exclusion. */
sealed interface MuscleRegionMapping {
    /** Maps to one or more paintable [BodyRegionId]s. [regions] is always non-empty. */
    data class Mapped(val regions: List<BodyRegionId>) : MuscleRegionMapping

    /** Deliberately not a single body region (e.g. FULL_BODY) — surfaced as a session summary instead. */
    data class SummaryOnly(val reason: String) : MuscleRegionMapping
}

object MuscleBodyRegions {

    /**
     * Total-coverage mapping. The `when` is exhaustive over [MuscleGroup] (no `else`) so every value is a
     * conscious decision and a new enum value will not compile until mapped here. Never crashes / never
     * returns an empty [MuscleRegionMapping.Mapped].
     */
    fun mappingFor(muscle: MuscleGroup): MuscleRegionMapping = when (muscle) {
        MuscleGroup.CHEST -> mapped(BodyRegionId.CHEST)
        // No dedicated upper-chest region on the silhouette; folds into the chest region.
        MuscleGroup.UPPER_CHEST -> mapped(BodyRegionId.CHEST)
        // Generic "back": broad-back fallback → the main visible back mass (lats + mid-back).
        MuscleGroup.BACK -> mapped(BodyRegionId.LATS, BodyRegionId.MID_BACK)
        MuscleGroup.LATS -> mapped(BodyRegionId.LATS)
        // MUSCLE-HEAT-1: the upstream sheet splits the trapezius from the upper-back mass, but the
        // catalog's MID_BACK means the whole mid-back area — so one MID_BACK heat result paints BOTH
        // regions for back-map readability. Rendering-only: Layer A exposure math and contributors are
        // untouched (the same single result is simply attached to two paintable regions).
        MuscleGroup.MID_BACK -> mapped(BodyRegionId.MID_BACK, BodyRegionId.TRAPS)
        MuscleGroup.LOWER_BACK -> mapped(BodyRegionId.LOWER_BACK)
        MuscleGroup.TRAPS -> mapped(BodyRegionId.TRAPS)
        // Catalog uses the SHOULDERS aggregate; the three delt heads all collapse to the shoulder region.
        MuscleGroup.SHOULDERS -> mapped(BodyRegionId.SHOULDERS)
        MuscleGroup.FRONT_DELTS -> mapped(BodyRegionId.SHOULDERS)
        MuscleGroup.SIDE_DELTS -> mapped(BodyRegionId.SHOULDERS)
        MuscleGroup.REAR_DELTS -> mapped(BodyRegionId.SHOULDERS)
        MuscleGroup.BICEPS -> mapped(BodyRegionId.BICEPS)
        MuscleGroup.TRICEPS -> mapped(BodyRegionId.TRICEPS)
        MuscleGroup.FOREARMS -> mapped(BodyRegionId.FOREARMS)
        MuscleGroup.QUADS -> mapped(BodyRegionId.QUADS)
        MuscleGroup.HAMSTRINGS -> mapped(BodyRegionId.HAMSTRINGS)
        MuscleGroup.GLUTES -> mapped(BodyRegionId.GLUTES)
        MuscleGroup.CALVES -> mapped(BodyRegionId.CALVES)
        MuscleGroup.ABS -> mapped(BodyRegionId.ABS)
        MuscleGroup.OBLIQUES -> mapped(BodyRegionId.OBLIQUES)
        // Trunk/core → the abs region (overlaps ABS; OBLIQUES kept as its own region when logged directly).
        MuscleGroup.CORE -> mapped(BodyRegionId.ABS)
        MuscleGroup.ADDUCTORS -> mapped(BodyRegionId.ADDUCTORS)
        MuscleGroup.ABDUCTORS -> mapped(BodyRegionId.ABDUCTORS)
        // Whole-body work: never blindly paint the entire silhouette — shown as a session summary.
        MuscleGroup.FULL_BODY -> MuscleRegionMapping.SummaryOnly(
            "Whole-body exposure — shown as a session summary, not a single body region.",
        )
    }

    /** Convenience: paintable regions for a muscle, or empty if it is summary-only. */
    fun regionsFor(muscle: MuscleGroup): List<BodyRegionId> =
        (mappingFor(muscle) as? MuscleRegionMapping.Mapped)?.regions.orEmpty()

    private fun mapped(vararg regions: BodyRegionId) = MuscleRegionMapping.Mapped(regions.toList())
}
