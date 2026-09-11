package com.hoctin.gymtracker.domain

/**
 * The fixed, intentionally small Daily Boost catalog (MVP).
 *
 * Daily Boost is a short, optional ~2-minute habit for rest / low-energy days — it is **not** a
 * workout. It never touches workout history, the Weekly Progress ring, training-day completion, or
 * the post-save auto-advance; it lives entirely in SharedPreferences (see
 * `UserPreferencesRepository`).
 *
 * Each entry carries a stable [key] (the only thing persisted, so display copy can change freely
 * without breaking saved state), a user-facing [displayName], and a short muted [description] shown
 * on the Home card and the timer modal.
 */
enum class DailyBoostExercise(
    val key: String,
    val displayName: String,
    val description: String,
) {
    PUSH_UP(
        key = "push_up",
        displayName = "Push-up",
        description = "Steady reps with a full range. Keep your core braced.",
    ),

    // "Dead hang" is the default pulling option on purpose — gentler than a pull-up for an everyday
    // habit, while still scratching the same itch.
    DEAD_HANG(
        key = "dead_hang",
        displayName = "Dead hang",
        description = "Hang from a bar and let your shoulders decompress.",
    ),

    PLANK(
        key = "plank",
        displayName = "Plank",
        description = "Hold a straight line from head to heels — don't let the hips sag.",
    ),

    BODYWEIGHT_SQUAT(
        key = "bodyweight_squat",
        displayName = "Bodyweight squat",
        description = "Slow tempo, sit back, and drive up through your heels.",
    ),

    HOLLOW_HOLD(
        key = "hollow_hold",
        displayName = "Hollow hold",
        description = "Low back pressed down, ribs in, legs long and steady.",
    ),

    MOBILITY_STRETCH(
        key = "mobility_stretch",
        displayName = "Mobility stretch",
        description = "Gentle full-body stretch to loosen up and reset.",
    ),
    ;

    companion object {
        /** Safe, universally doable default used before the user has chosen anything. */
        val DEFAULT = PUSH_UP

        /** Resolve a stored [key] back to an entry, falling back to [DEFAULT] for unknown/null. */
        fun fromKey(key: String?): DailyBoostExercise =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
