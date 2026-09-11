package com.hoctin.gymtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.ui.components.SecondaryGymButton
import com.hoctin.gymtracker.ui.components.formatWeekdays
import com.hoctin.gymtracker.ui.components.goalOptionContent
import com.hoctin.gymtracker.ui.components.modeLabel

@Composable
fun ProfilePreferencesScreen(
    state: SettingsUiState,
    onThemeChanged: (com.hoctin.gymtracker.data.ThemeMode) -> Unit,
    onDistanceChanged: (com.hoctin.gymtracker.data.DistanceUnitSystem) -> Unit,
    onBack: () -> Unit,
) {
    ProfilePageShell(
        title = "Units & appearance",
        eyebrow = "Preferences",
        onBack = onBack,
    ) {
        state.message?.let { ProfileMessageCard(it) }

        ProfileContentCard {
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Follow the system or choose a consistent AURON canvas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedThemeControl(
                selected = state.themeMode,
                onSelect = onThemeChanged,
            )
        }

        ProfileContentCard {
            Text(
                text = "MEASUREMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Distance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Used for carries, sleds and distance work. Stored metric values are never rewritten.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedDistanceControl(
                selected = state.distanceUnitSystem,
                onSelect = onDistanceChanged,
            )
        }

        ProfilePlaceholderState(
            title = "Language follows your current app copy",
            explanation = "A dedicated language selector is not implemented. AURON keeps the existing English product language instead of showing a control that cannot persist a real choice.",
            status = "Unavailable",
        )
    }
}

@Composable
fun ProfileTrainingScreen(
    state: SettingsUiState,
    onSaveWeekdays: (Set<Int>) -> Unit,
    onSaveGoal: (com.hoctin.gymtracker.domain.WorkoutGoalSelection) -> Unit,
    onBack: () -> Unit,
) {
    var editingDays by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf(false) }
    val goalContent = state.workoutGoal.goalOptionContent()

    ProfilePageShell(
        title = "Training setup",
        eyebrow = "Preferences",
        onBack = onBack,
    ) {
        state.message?.let { ProfileMessageCard(it) }

        ProfileContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TRAINING WEEK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Planned weekdays",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = formatWeekdays(state.trainingWeekdays),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "These days set the Weekly Progress target and planned/missed calendar interpretation. They do not bind a named workout to a specific weekday.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryGymButton(
                text = "Edit training days",
                onClick = { editingDays = true },
                icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            )
        }

        ProfileContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.workoutGoal.modeLabel().uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = goalContent.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = goalContent.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Changes future coaching only. Your starting split, saved sets and workout history stay unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            SecondaryGymButton(
                text = "Review coaching goal",
                onClick = { editingGoal = true },
                icon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
            )
        }

        ProfileContentCard {
            Text(
                text = "CURRENT PLAN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = state.planDayNames.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                    ?: "Plan unavailable",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Plan editing remains in the Plan tab so schedule tracking and workout structure keep separate ownership.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (editingDays) {
        EditTrainingDaysDialog(
            initialSelection = state.trainingWeekdays,
            requiredDays = state.requiredDays,
            onDismiss = { editingDays = false },
            onSave = {
                onSaveWeekdays(it)
                editingDays = false
            },
        )
    }
    if (editingGoal) {
        GoalSelectorSheet(
            initialSelection = state.workoutGoal,
            onDismiss = { editingGoal = false },
            onSave = {
                onSaveGoal(it)
                editingGoal = false
            },
        )
    }
}

@Composable
fun NotificationsPlaceholderScreen(onBack: () -> Unit) {
    ProfilePageShell(
        title = "Notifications & reminders",
        eyebrow = "Preferences",
        onBack = onBack,
    ) {
        ProfilePlaceholderState(
            title = "Reminders are unavailable",
            explanation = "AURON does not currently schedule workout, streak or weekly-progress notifications. No permission is requested and no background reminder is silently enabled.",
            status = "Unavailable",
        )
        ProfileContentCard {
            Text(
                text = "WHEN THIS BECOMES AVAILABLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Notification controls will appear here only after each reminder has a real schedule, permission flow and off switch.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
