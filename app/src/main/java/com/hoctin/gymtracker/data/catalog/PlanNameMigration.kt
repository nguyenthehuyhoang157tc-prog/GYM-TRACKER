package com.hoctin.gymtracker.data.catalog

import com.hoctin.gymtracker.data.GymDao

/**
 * DB-4B.2 — one-time, idempotent migration that renames active **Plan** exercise rows from legacy
 * AURON curated names to their FED-canonical names, so Progression Engine V3.6B catalog lookup
 * (`findByName`) resolves after the catalog became FED-first.
 *
 * Source of truth: [MAP] is exactly the 63 `SAFE_RENAME` rows from
 * `docs/db4a1_verified_rename_map.csv` (the verified map — strict tokenset match, equipment-checked,
 * protected-variant-guarded, collision-free, every target present in the 515-entry FED catalog).
 * REVIEW_REQUIRED / KEEP_CUSTOM names are **not** migrated — those Plan rows stay as-is and fall back
 * to the engine's name-inference, exactly like a user's custom exercise.
 *
 * Safety properties:
 * - **History is never touched** — only the `exercises` table is updated (see [GymDao.renameExercisesByName]).
 * - **Idempotent** — keys are legacy names, values are FED names; no value is also a (different) key, so
 *   re-running is a no-op. It is additionally flag-guarded by the caller.
 * - **No collisions** — verified upstream; two legacy names never map to the same FED name.
 */
object PlanNameMigration {

    /** prefs flag key set by the caller once this migration has run. */
    const val FLAG_KEY = "fed_canonical_plan_name_migration_v1_done"

    /** The 63 verified SAFE_RENAME pairs: legacy AURON name -> FED canonical name. */
    val MAP: Map<String, String> = mapOf(
        "Barbell Incline Bench Press" to "Barbell Incline Bench Press - Medium Grip",
        "Barbell Decline Bench Press" to "Decline Barbell Bench Press",
        "Dumbbell Decline Bench Press" to "Decline Dumbbell Bench Press",
        "Push-Up" to "Pushups",
        "Incline Dumbbell Fly" to "Incline Dumbbell Flyes",
        "Decline Dumbbell Fly" to "Decline Dumbbell Flyes",
        "Cable Crossover" to "Cable Crossover",
        "Smith Machine Bench Press" to "Smith Machine Bench Press",
        "Incline Push-Up" to "Incline Push-Up",
        "Conventional Barbell Deadlift" to "Barbell Deadlift",
        "Barbell Bent-Over Row" to "Bent Over Barbell Row",
        "Chin-Up" to "Chin-Up",
        "Wide-Grip Lat Pulldown" to "Wide-Grip Lat Pulldown",
        "Seated Cable Row" to "Seated Cable Rows",
        "One-Arm Dumbbell Row" to "One-Arm Dumbbell Row",
        "Barbell Shrug" to "Barbell Shrug",
        "Dumbbell Shrug" to "Dumbbell Shrug",
        "Straight-Arm Cable Pulldown" to "Straight-Arm Pulldown",
        "Cable Face Pull" to "Face Pull",
        "Barbell Bicep Curl" to "Barbell Curl",
        "Dumbbell Alternate Bicep Curl" to "Dumbbell Alternate Bicep Curl",
        "Incline Dumbbell Curl" to "Incline Dumbbell Curl",
        "Concentration Curl" to "Concentration Curls",
        "Reverse Barbell Curl" to "Reverse Barbell Curl",
        "Zottman Curl" to "Zottman Curl",
        "Machine Preacher Curl" to "Machine Preacher Curls",
        "Cross-Body Hammer Curl" to "Cross Body Hammer Curl",
        "Drag Curl" to "Drag Curl",
        "Triceps Pushdown" to "Triceps Pushdown",
        "Close-Grip Bench Press" to "Close-Grip Barbell Bench Press",
        "Bench Dip" to "Bench Dips",
        "Reverse-Grip Triceps Pushdown" to "Reverse Grip Triceps Pushdown",
        "JM Press" to "JM Press",
        "Tate Press" to "Tate Press",
        "Machine Triceps Dip" to "Dip Machine",
        "Barbell Back Squat" to "Barbell Squat",
        "Barbell Front Squat" to "Front Barbell Squat",
        "Hack Squat Machine" to "Hack Squat",
        "Leg Press" to "Leg Press",
        "Leg Extension" to "Leg Extensions",
        "Smith Machine Squat" to "Smith Machine Squat",
        "Dumbbell Step-Up" to "Dumbbell Step Ups",
        "Zercher Squat" to "Zercher Squats",
        "Romanian Deadlift" to "Romanian Deadlift",
        "Seated Leg Curl" to "Seated Leg Curl",
        "Lying Leg Curl" to "Lying Leg Curls",
        "Cable Pull-Through" to "Pull Through",
        "Machine Thigh Abductor" to "Thigh Abductor",
        "Machine Thigh Adductor" to "Thigh Adductor",
        "Standing Calf Raise" to "Standing Calf Raises",
        "Seated Calf Raise" to "Seated Calf Raise",
        "Leg Press Calf Raise" to "Calf Press",
        "Smith Machine Reverse Calf Raise" to "Smith Machine Reverse Calf Raises",
        "Arnold Press" to "Arnold Dumbbell Press",
        "Dumbbell Front Raise" to "Front Dumbbell Raise",
        "Machine Shoulder Press" to "Machine Shoulder (Military) Press",
        "Barbell Upright Row" to "Upright Barbell Row",
        "Hanging Leg Raise" to "Hanging Leg Raise",
        "Decline Bench Crunch" to "Decline Crunch",
        "Cable Crunch" to "Cable Crunch",
        "Forearm Plank" to "Plank",
        "Pallof Press" to "Pallof Press",
        "Flutter Kicks" to "Flutter Kicks",
    )

    /**
     * Pure helper: the FED-canonical name for a Plan exercise [name], or the name unchanged when it is
     * not a SAFE_RENAME source. Idempotent: `canonicalName(canonicalName(x)) == canonicalName(x)`.
     */
    fun canonicalName(name: String): String = MAP[name] ?: name

    /** Applies the rename to the active Plan only (identity pairs are skipped). Returns rows updated. */
    suspend fun run(dao: GymDao): Int {
        var updated = 0
        MAP.forEach { (old, fed) -> if (old != fed) updated += dao.renameExercisesByName(old, fed) }
        return updated
    }
}
