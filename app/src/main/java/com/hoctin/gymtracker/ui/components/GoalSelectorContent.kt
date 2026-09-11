package com.hoctin.gymtracker.ui.components

import com.hoctin.gymtracker.domain.WorkoutGoalSelection

internal data class GoalOptionContent(
    val selection: WorkoutGoalSelection,
    val title: String,
    val description: String,
)

internal val goalSelectorOptions = listOf(
    GoalOptionContent(
        selection = WorkoutGoalSelection.AUTO,
        title = "Auto",
        description = "Adapts coaching from rep targets and exercise modality. Low reps lean strength, high reps lean endurance, and skill work stays form-first.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.HYPERTROPHY,
        title = "Hypertrophy",
        description = "Build muscle through productive sets, rep progression, and conservative load increases.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.STRENGTH,
        title = "Strength",
        description = "Build force with clean, repeatable reps, stable technique, and conservative load progress.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.POWER,
        title = "Power",
        description = "Keep reps fast and crisp. AURON avoids grinding and stays advisory without measured output.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.ENDURANCE,
        title = "Endurance",
        description = "Build repeatable work through reps, time, distance, or pace when sessions are comparable.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.GENERAL_FITNESS,
        title = "General fitness",
        description = "Prioritize consistency, recovery, and sustainable work capacity.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.FAT_LOSS_SUPPORT,
        title = "Fat-loss support",
        description = "Support consistent training and work capacity without treating a workout as measured fat loss.",
    ),
    GoalOptionContent(
        selection = WorkoutGoalSelection.SKILL,
        title = "Skill",
        description = "Prioritize form, control, and repeatability. Load changes stay advisory without technique data.",
    ),
)

internal val autoGoalOption: GoalOptionContent = goalSelectorOptions.first()
internal val explicitGoalOptions: List<GoalOptionContent> = goalSelectorOptions.drop(1)

internal fun WorkoutGoalSelection.goalOptionContent(): GoalOptionContent =
    goalSelectorOptions.first { it.selection == this }

internal fun WorkoutGoalSelection.modeLabel(): String =
    if (this == WorkoutGoalSelection.AUTO) "AUTO" else "EXPLICIT GOAL"

internal const val GOAL_CHANGE_SCOPE_TITLE = "Future coaching only"
internal const val GOAL_CHANGE_SCOPE_DETAIL =
    "This changes recommendations in Log Workout and Exercise Detail. Your workout plan, planned set/rep targets, logged sets, and workout history stay unchanged."

internal const val EXERCISE_MODALITY_TITLE = "Exercise rules still matter"
internal const val EXERCISE_MODALITY_DETAIL =
    "Skill work stays form-first. Timed holds build time, and carries or sleds follow their logged distance, load, and pace rules in every program goal."
