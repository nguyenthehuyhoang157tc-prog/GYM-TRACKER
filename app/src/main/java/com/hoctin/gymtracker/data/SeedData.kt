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
    val defaultSplit = listOf(
        SeedWorkoutDay(
            name = "Push",
            exercises = listOf(
                SeedExercise("Plate-loaded incline chest press machine", 3, 8, 12),
                SeedExercise("Machine chest press", 3, 8, 12),
                SeedExercise("Pec fly", 3, 12, 20),
                SeedExercise("Lateral raise", 4, 12, 20),
                SeedExercise("Face pull", 3, 12, 20),
                SeedExercise("Reverse-grip straight-bar cable pushdown", 3, 10, 15),
                SeedExercise("Push-up optional", 2, 8, 20),
            ),
        ),
        SeedWorkoutDay(
            name = "Pull",
            exercises = listOf(
                SeedExercise("Pull-up", 3, 6, 10),
                SeedExercise("Lat pulldown", 3, 8, 12),
                SeedExercise("Seated row", 3, 8, 12),
                SeedExercise("Straight-arm pulldown", 3, 10, 15),
                SeedExercise("Incline dumbbell curl", 3, 10, 15),
                SeedExercise("Hammer curl", 3, 10, 15),
                SeedExercise("EZ curl optional", 2, 10, 15),
            ),
        ),
        SeedWorkoutDay(
            name = "Legs",
            exercises = listOf(
                SeedExercise("Leg press or squat", 3, 8, 12),
                SeedExercise("Romanian deadlift or leg curl", 3, 8, 12),
                SeedExercise("Leg extension", 3, 10, 15),
                SeedExercise("Hamstring curl", 3, 10, 15),
                SeedExercise("Calf raise", 4, 10, 20),
            ),
        ),
        SeedWorkoutDay(
            name = "Full Body",
            exercises = listOf(
                SeedExercise("Machine chest press", 3, 8, 12),
                SeedExercise("Lat pulldown or light row", 3, 8, 12),
                SeedExercise("Leg press or hack squat", 3, 8, 12),
                SeedExercise("Curl machine or rope pushdown", 3, 10, 15),
                SeedExercise("Cable crunch or sit-up", 3, 10, 15),
            ),
        ),
    )
}
