package com.hoctin.gymtracker.ui.plan

import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.Category
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.Force
import com.hoctin.gymtracker.data.catalog.Level
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlanPresentationTest {
    @Test
    fun builderSearchMatchesNameAndAliasCaseInsensitively() {
        val bench = exercise("Barbell Bench Press", listOf("Flat press"), MuscleGroup.CHEST)
        val squat = exercise("Back Squat", emptyList(), MuscleGroup.QUADS)

        assertEquals(listOf(bench), filterBuilderExercises(listOf(bench, squat), "BENCH", null))
        assertEquals(listOf(bench), filterBuilderExercises(listOf(bench, squat), "flat", null))
    }

    @Test
    fun muscleFilterIncludesPrimaryAndSecondaryEvidence() {
        val press = exercise(
            name = "Incline Press",
            aliases = emptyList(),
            primary = MuscleGroup.CHEST,
            secondary = listOf(MuscleGroup.FRONT_DELTS),
        )

        assertEquals(listOf(press), filterBuilderExercises(listOf(press), "", MuscleGroup.FRONT_DELTS))
    }

    @Test
    fun genericShouldersFilterIsHiddenButSpecificDeltsRemain() {
        val filters = builderFilterMuscles()

        assertFalse(MuscleGroup.SHOULDERS in filters)
        assert(filters.containsAll(listOf(MuscleGroup.FRONT_DELTS, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS)))
    }

    private fun exercise(
        name: String,
        aliases: List<String>,
        primary: MuscleGroup,
        secondary: List<MuscleGroup> = emptyList(),
    ) = CatalogExercise(
        slug = name.lowercase().replace(' ', '-'),
        name = name,
        equipment = Equipment.BARBELL,
        primaryMuscles = listOf(primary),
        secondaryMuscles = secondary,
        level = Level.BEGINNER,
        force = Force.PUSH,
        category = Category.STRENGTH,
        progressionType = ProgressionType.COMPOUND_LOAD,
        defaultSets = 3,
        defaultMinReps = 8,
        defaultMaxReps = 12,
        defaultIncrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 2,
        isBodyweight = false,
        isUnilateral = false,
        aliases = aliases,
    )
}
