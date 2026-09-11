package com.hoctin.gymtracker.ui.settings

import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import com.hoctin.gymtracker.ui.components.EXERCISE_MODALITY_DETAIL
import com.hoctin.gymtracker.ui.components.GOAL_CHANGE_SCOPE_DETAIL
import com.hoctin.gymtracker.ui.components.autoGoalOption
import com.hoctin.gymtracker.ui.components.explicitGoalOptions
import com.hoctin.gymtracker.ui.components.goalOptionContent
import com.hoctin.gymtracker.ui.components.goalSelectorOptions
import com.hoctin.gymtracker.ui.components.modeLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSelectorContentTest {

    @Test
    fun `selector covers every persisted selection exactly once`() {
        assertEquals(
            WorkoutGoalSelection.entries.toSet(),
            goalSelectorOptions.map { it.selection }.toSet(),
        )
        assertEquals(WorkoutGoalSelection.entries.size, goalSelectorOptions.size)
    }

    @Test
    fun `auto is separated from explicit goals and explains production inference`() {
        assertEquals(WorkoutGoalSelection.AUTO, autoGoalOption.selection)
        assertTrue(explicitGoalOptions.none { it.selection == WorkoutGoalSelection.AUTO })
        assertTrue(autoGoalOption.description.contains("Low reps"))
        assertTrue(autoGoalOption.description.contains("high reps"))
        assertTrue(autoGoalOption.description.contains("skill work"))
        assertEquals("AUTO", WorkoutGoalSelection.AUTO.modeLabel())
        assertTrue(
            WorkoutGoalSelection.entries
                .filterNot { it == WorkoutGoalSelection.AUTO }
                .all { it.modeLabel() == "EXPLICIT GOAL" },
        )
    }

    @Test
    fun `every explicit goal has concise user facing guidance`() {
        explicitGoalOptions.forEach { option ->
            assertTrue(option.selection.name, option.title.isNotBlank())
            assertTrue(option.selection.name, option.description.length in 30..180)
        }
    }

    @Test
    fun `unsupported outcome claims remain bounded`() {
        val power = WorkoutGoalSelection.POWER.goalOptionContent().description.lowercase()
        val fatLoss = WorkoutGoalSelection.FAT_LOSS_SUPPORT.goalOptionContent().description.lowercase()
        val skill = WorkoutGoalSelection.SKILL.goalOptionContent().description.lowercase()

        assertTrue(power.contains("advisory"))
        assertTrue(power.contains("measured output"))
        assertTrue(fatLoss.contains("without treating") && fatLoss.contains("measured fat loss"))
        assertTrue(skill.contains("form") && skill.contains("without technique data"))
    }

    @Test
    fun `scope copy protects history and explains modality precedence`() {
        val scope = GOAL_CHANGE_SCOPE_DETAIL.lowercase()
        val modality = EXERCISE_MODALITY_DETAIL.lowercase()

        assertTrue(scope.contains("recommendations"))
        assertTrue(scope.contains("planned set/rep targets"))
        assertTrue(scope.contains("logged sets"))
        assertTrue(scope.contains("workout history stay unchanged"))
        assertTrue(modality.contains("skill work"))
        assertTrue(modality.contains("timed holds"))
        assertTrue(modality.contains("carries or sleds"))
    }
}
