package com.hoctin.gymtracker.data

data class SeedExercise(
    val name: String,
    val targetSets: Int,
    val targetMinReps: Int,
    val targetMaxReps: Int,
)

data class SeedWorkoutDay(
    val name: String,
    val exercises: List<SeedExercise>,
)

object SeedData {
    // DB-4B.2: default plan exercise names use **FED-canonical** catalog names so Progression Engine
    // V3.6B catalog lookup (findByName) resolves. Only the 13 of 19 names that had a verified
    // SAFE_RENAME (docs/db4a1_verified_rename_map.csv) are kept/renamed; the 6 legacy curated names with
    // no safe FED equivalent (Incline/Seated Machine Chest Press, Pec Deck Machine Fly, Dumbbell Seated
    // Lateral Raise, Wide-Grip Pull-Up, Dumbbell Hammer Curl) were REMOVED rather than force-mapped to a
    // different FED variant. The two "optional" accessories are intentional name-inference customs.
    // Day structure, order, and target sets/reps are otherwise preserved.
    val defaultSplit = listOf(
        SeedWorkoutDay(
            name = "Push",
            exercises = listOf(
                SeedExercise("Face Pull", 3, 12, 20),
                SeedExercise("Reverse Grip Triceps Pushdown", 3, 10, 15),
                SeedExercise("Push-up optional", 2, 8, 20),
            ),
        ),
        SeedWorkoutDay(
            name = "Pull",
            exercises = listOf(
                SeedExercise("Wide-Grip Lat Pulldown", 3, 8, 12),
                SeedExercise("Seated Cable Rows", 3, 8, 12),
                SeedExercise("Straight-Arm Pulldown", 3, 10, 15),
                SeedExercise("Incline Dumbbell Curl", 3, 10, 15),
                SeedExercise("EZ curl optional", 2, 10, 15),
            ),
        ),
        SeedWorkoutDay(
            name = "Legs",
            exercises = listOf(
                SeedExercise("Leg Press", 3, 8, 12),
                SeedExercise("Seated Leg Curl", 3, 8, 12),
                SeedExercise("Leg Extensions", 3, 10, 15),
                SeedExercise("Lying Leg Curls", 3, 10, 15),
                SeedExercise("Standing Calf Raises", 4, 10, 20),
            ),
        ),
        SeedWorkoutDay(
            name = "Full Body",
            exercises = listOf(
                SeedExercise("Wide-Grip Lat Pulldown", 3, 8, 12),
                SeedExercise("Leg Press", 3, 8, 12),
                SeedExercise("Triceps Pushdown", 3, 10, 15),
                SeedExercise("Cable Crunch", 3, 10, 15),
            ),
        ),
    )
}
