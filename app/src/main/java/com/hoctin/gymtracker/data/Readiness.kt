package com.hoctin.gymtracker.data

/**
 * V3.6A subjective "how did today feel" signal, captured once per workout in the Save review sheet.
 *
 * Stored on [WorkoutSessionEntity] as the enum `name` (TEXT), nullable: **null / absent = UNKNOWN**
 * (pre-V3.6 sessions never had the prompt). New saves default to [NORMAL] (the prompt is preselected
 * and non-blocking), so a stored null cleanly means "recorded before this feature existed".
 *
 * This is captured and persisted in V3.6A only — the Progression Engine does not read it yet (that is
 * V3.6B). It never affects save/draft/history logic.
 */
enum class Readiness {
    TIRED,
    NORMAL,
    STRONG;

    companion object {
        /**
         * Tolerant parse used by import and any string decode: trims + uppercases, and maps anything
         * unrecognized (null, blank, or an unknown value) to null (UNKNOWN) rather than throwing.
         */
        fun fromName(value: String?): Readiness? {
            val normalized = value?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return null
            return entries.firstOrNull { it.name == normalized }
        }
    }
}
