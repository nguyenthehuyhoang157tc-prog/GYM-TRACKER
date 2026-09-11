package com.hoctin.gymtracker.domain.muscleheat

import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.TrackingType

/**
 * MUSCLE-1 — Layer B: baseline Muscle Heat Engine V1.
 *
 * Converts Layer-A raw exposure ([SessionMuscleExposure]) into training meaning. Deliberately **separate
 * from the Progressive Overload Engine** (which stays focused on load/reps/RIR progression) and pure:
 * no DAO, no I/O, no clock, no stored state. Session-only — NO 7-day recovery/decay model (MUSCLE-4).
 *
 * The axes (MUSCLE-HEAT-1 calibration):
 *  - [MuscleExposure.rawIntensityScore] = **raw anatomical exposure** (Layer A, untouched here);
 *  - [MuscleHeatResult.finalIntensityScore] / [MuscleHeatResult.colorBand] = **training stimulus**
 *    (raw exposure × RIR-driven [MuscleHeatResult.qualityModifier]) — identical raw work at RIR 4–5
 *    heats less than at RIR 1–2, and easy volume can no longer reach deep red;
 *  - [MuscleHeatResult.status] = **interpretation** (raw exposure + quality + grind) — kept separate
 *    from the color axis, so junk volume can read FATIGUED while mildly hot, and moderate grinding can
 *    be RECOVERY_RISK without being called productive.
 *
 * V1 scoring is simple, deterministic, explainable, and conservative: fixed thresholds, no decay, no
 * adaptive tuning, hypertrophy-oriented quality only where reps+RIR genuinely exist.
 */

/** Exposure color band for the body map. Driven ONLY by [MuscleHeatResult.finalIntensityScore]. */
enum class HeatColorBand { NONE, LIGHT_YELLOW, YELLOW_ORANGE, ORANGE_RED, DEEP_RED }

/** Training meaning for the muscle this session. Driven by exposure × quality — not by color. */
enum class MuscleStatus { NONE, STIMULATED, PRODUCTIVE, FATIGUED, RECOVERY_RISK, UNDERTRAINED }

data class MuscleHeatResult(
    val muscleGroup: MuscleGroup,
    /**
     * MUSCLE-HEAT-1: the **training stimulus** score = `rawIntensityScore × qualityModifier`.
     * Raw anatomical exposure lives on [MuscleExposure.rawIntensityScore] and stays untouched; this
     * final score (and therefore [colorBand]) is what RIR quality modulates — the same raw exposure
     * at RIR 4–5 heats less than at RIR 1–2.
     */
    val finalIntensityScore: Double,
    /** Color for the body map — a monotonic function of [finalIntensityScore] (stimulus, not raw). */
    val colorBand: HeatColorBand,
    val status: MuscleStatus,
    /** Short human-readable "why" (drivers + effort quality). */
    val reason: String,
    /** Present only for FATIGUED / RECOVERY_RISK; V1 keeps it light (full recovery model is MUSCLE-4). */
    val recoveryNote: String?,
    /** 0..1 effort-quality factor (multiplies raw exposure into [finalIntensityScore]; also feeds [status]). */
    val qualityModifier: Double,
)

object MuscleHeatEngine {

    // ---- Stimulus → color band thresholds (quality-weighted set-equivalents; deterministic) ------
    const val BAND_MODERATE = 2.0
    const val BAND_HIGH = 4.0
    const val BAND_VERY_HIGH = 7.0

    // ---- Status thresholds ----------------------------------------------------------------
    /** Below this exposure a touched muscle reads as UNDERTRAINED (below a meaningful session dose). */
    const val MEANINGFUL_EXPOSURE = 2.0
    /** Quality at/above this on meaningful exposure reads as PRODUCTIVE. */
    const val QUALITY_PRODUCTIVE = 0.65
    /** Share of full-scoring volume ground to ~failure that flags recovery risk (at enough exposure). */
    const val GRIND_SHARE_FOR_RISK = 0.5
    const val RISK_MIN_EXPOSURE = 4.0

    // ---- Quality table (V1, fixed, explainable) --------------------------------------------
    private const val QUALITY_NEUTRAL = 0.5          // no usable quality signal
    private const val QUALITY_SKILL_FORM = 0.6       // conservative, form-focused
    private const val QUALITY_LIMITED = 0.5          // time/distance types: exposure yes, quality limited
    private const val GRIND_AVG_RIR = 0.5            // avg RIR at/below this = ground to ~failure

    fun analyze(exposure: SessionMuscleExposure): List<MuscleHeatResult> =
        exposure.muscles.map(::analyzeMuscle)

    fun analyzeMuscle(exposure: MuscleExposure): MuscleHeatResult {
        val raw = exposure.rawIntensityScore
        val quality = qualityModifier(exposure)
        val grindShare = grindShare(exposure)
        // MUSCLE-HEAT-1: final heat = raw anatomical exposure × training-quality modifier, so the color
        // band represents meaningful stimulus. Identical raw exposure at RIR 4–5 (quality 0.40) heats
        // roughly half as much as productive RIR 1–2 work (0.95); easy completed sets can no longer
        // reach deep red on volume alone. Raw exposure itself stays untouched on [MuscleExposure].
        val finalScore = raw * quality
        val band = colorBand(finalScore)
        // Status (training MEANING) stays on the raw-exposure axis + quality/grind signals — separate
        // from the color axis by contract: a big junk-volume day can be FATIGUED while only mildly hot,
        // and a moderate grind day can be RECOVERY_RISK without being called productive.
        val status = status(raw, quality, grindShare)
        return MuscleHeatResult(
            muscleGroup = exposure.muscleGroup,
            finalIntensityScore = finalScore,
            colorBand = band,
            status = status,
            reason = reason(exposure, raw, quality, status),
            recoveryNote = recoveryNote(status),
            qualityModifier = quality,
        )
    }

    // TODO(MUSCLE-HEAT-2+ contract): optional Progressive Overload metadata injection. The MUSCLE-1
    // contributor already reserves per-exercise fields (CoachInsight, PlateauStrategy, SessionQuality,
    // confidence) and Readiness/WorkoutContext exist as pure domain types — when wired, they must be
    // PASSED IN by the caller (ViewModel) as plain data and only refine qualityModifier/status/reason.
    // MuscleHeatEngine must never call the Progressive Overload Engine directly.

    /** Band mapping over the FINAL stimulus score (raw exposure × quality). Monotonic and pure. */
    fun colorBand(score: Double): HeatColorBand = when {
        score <= 0.0 -> HeatColorBand.NONE
        score < BAND_MODERATE -> HeatColorBand.LIGHT_YELLOW
        score < BAND_HIGH -> HeatColorBand.YELLOW_ORANGE
        score < BAND_VERY_HIGH -> HeatColorBand.ORANGE_RED
        else -> HeatColorBand.DEEP_RED
    }

    private fun status(score: Double, quality: Double, grindShare: Double): MuscleStatus = when {
        score <= 0.0 -> MuscleStatus.NONE
        score < MEANINGFUL_EXPOSURE -> MuscleStatus.UNDERTRAINED
        grindShare >= GRIND_SHARE_FOR_RISK && score >= RISK_MIN_EXPOSURE -> MuscleStatus.RECOVERY_RISK
        score >= BAND_VERY_HIGH -> MuscleStatus.FATIGUED
        quality >= QUALITY_PRODUCTIVE -> MuscleStatus.PRODUCTIVE
        else -> MuscleStatus.STIMULATED
    }

    /**
     * Contribution-weighted effort quality across the muscle's contributors.
     *
     * Per tracking type (V1 rules):
     *  - REPS_WEIGHT / BODYWEIGHT_REPS → **full scoring** from avg RIR (hypertrophy effective-reps idea):
     *    RIR ≥ 4 → 0.40 (a lot left in reserve), 3–4 → 0.60, 0.5–3 → 0.95 (productive window),
     *    < 0.5 → 0.70 (all-out grinding: stimulating but costly);
     *  - SKILL_FORM → fixed 0.60 (**conservative / form-focused**, never a failure-chasing signal);
     *  - TIME_ONLY / TIME_LOAD / DISTANCE_LOAD / TIME_DISTANCE / TIME_DISTANCE_LOAD → fixed 0.50
     *    (**raw exposure allowed, limited quality** — no RIR to read, sentinels never interpreted).
     */
    private fun qualityModifier(exposure: MuscleExposure): Double {
        val weighted = exposure.exerciseContributors.filter { it.setEquivalent > 0.0 }
        if (weighted.isEmpty()) return 0.0
        val total = weighted.sumOf { it.setEquivalent }
        val sum = weighted.sumOf { it.setEquivalent * contributorQuality(it) }
        return sum / total
    }

    private fun contributorQuality(c: ExerciseContributor): Double {
        return when (c.trackingType) {
            TrackingType.SKILL_FORM -> QUALITY_SKILL_FORM
            TrackingType.REPS_WEIGHT, TrackingType.BODYWEIGHT_REPS -> {
                val rir = c.avgRir ?: return QUALITY_NEUTRAL
                when {
                    rir >= 4.0 -> 0.40
                    rir >= 3.0 -> 0.60
                    rir >= GRIND_AVG_RIR -> 0.95
                    else -> 0.70
                }
            }
            else -> QUALITY_LIMITED
        }
    }

    /** Share of full-scoring volume that was ground to ~failure (avg RIR ≤ [GRIND_AVG_RIR]). */
    private fun grindShare(exposure: MuscleExposure): Double {
        val fullScoring = exposure.exerciseContributors.filter {
            it.setEquivalent > 0.0 && it.avgRir != null &&
                (it.trackingType == TrackingType.REPS_WEIGHT || it.trackingType == TrackingType.BODYWEIGHT_REPS)
        }
        val total = fullScoring.sumOf { it.setEquivalent }
        if (total <= 0.0) return 0.0
        val grinding = fullScoring.filter { (it.avgRir ?: Double.MAX_VALUE) <= GRIND_AVG_RIR }
            .sumOf { it.setEquivalent }
        return grinding / total
    }

    private fun reason(exposure: MuscleExposure, score: Double, quality: Double, status: MuscleStatus): String {
        if (score <= 0.0) {
            return if (exposure.skippedExercises > 0) {
                "Planned but skipped this session — no exposure logged."
            } else {
                "No exposure this session."
            }
        }
        val drivers = exposure.exerciseContributors
            .filter { it.setEquivalent > 0.0 }
            .sortedByDescending { it.setEquivalent }
            .take(3)
            .joinToString(", ") { it.exerciseName }
        val volume = "${formatScore(score)} weighted hard-set equivalents from $drivers"
        val effort = when {
            status == MuscleStatus.RECOVERY_RISK -> "most working sets went to failure"
            status == MuscleStatus.FATIGUED -> "a lot of volume for one session"
            status == MuscleStatus.UNDERTRAINED -> "a light touch this session"
            quality >= QUALITY_PRODUCTIVE -> "solid effort quality"
            else -> "moderate effort signal"
        }
        return "$volume — $effort."
    }

    private fun recoveryNote(status: MuscleStatus): String? = when (status) {
        MuscleStatus.RECOVERY_RISK ->
            "Most sets here were taken to failure — give this muscle extra recovery before hitting it hard again."
        MuscleStatus.FATIGUED ->
            "High volume for one session — a normal rest gap is fine, but expect some soreness."
        else -> null
    }

    private fun formatScore(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else "%.1f".format(value)
}
