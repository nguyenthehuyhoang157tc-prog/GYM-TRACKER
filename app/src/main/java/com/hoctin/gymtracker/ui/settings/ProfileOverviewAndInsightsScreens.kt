package com.hoctin.gymtracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.ui.format.MeasurementFormatter
import com.hoctin.gymtracker.ui.history.HistoryUiState
import com.hoctin.gymtracker.ui.onboarding.OnboardingProfileJson
import com.hoctin.gymtracker.ui.onboarding.OnboardingTrainingProfileDraft
import java.math.RoundingMode

@Composable
fun PersonalProfileScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
) {
    val profile = OnboardingProfileJson.decodeDraft(state.trainingProfileJson)
    ProfilePageShell(
        title = "Personal profile",
        eyebrow = "Your profile",
        onBack = onBack,
    ) {
        ProfileContentCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                ) {
                    Text(
                        text = profile?.displayName?.takeIf { it.isNotBlank() } ?: "Local AURON profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Stored only in this app's private preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ProfileStatusPill("Local")
            }
        }

        if (profile == null) {
            ProfilePlaceholderState(
                title = "Profile details are not available yet",
                explanation = "Finish the current onboarding flow to create a local training profile. Your plan, history and settings are unchanged.",
                status = "Unavailable",
            )
        } else {
            ProfileDetailsCard(profile = profile)
            ProfileTrainingContextCard(profile = profile, state = state)
        }

        ProfilePlaceholderState(
            title = "Profile editing is updating",
            explanation = "AURON can display the context already saved by onboarding. Editing these optional profile fields is not connected yet, so this screen does not expose a fake Save action or rewrite onboarding-owned data.",
            status = "Updating",
        )
    }
}

@Composable
private fun ProfileDetailsCard(profile: OnboardingTrainingProfileDraft) {
    ProfileContentCard {
        Text(
            text = "PROFILE DETAILS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        ProfileValueRow("Display name", profile.displayName.takeIf { it.isNotBlank() } ?: "Not provided")
        ProfileValueRow("Age group", profile.ageGroup?.label ?: "Not provided")
        ProfileValueRow("Gender", profile.gender?.label ?: "Not provided")
        val height = profile.heightCm?.let { "${formatNumber(it)} cm" } ?: "Not provided"
        val weight = profile.weightKg?.let { "${formatNumber(it)} kg" } ?: "Not provided"
        ProfileValueRow("Height", height)
        ProfileValueRow("Weight", weight)
    }
}

@Composable
private fun ProfileTrainingContextCard(
    profile: OnboardingTrainingProfileDraft,
    state: SettingsUiState,
) {
    ProfileContentCard {
        Text(
            text = "TRAINING CONTEXT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        ProfileValueRow("Goal declared in onboarding", profile.primaryGoal?.label ?: "Auto")
        ProfileValueRow("Experience", profile.experience?.label ?: "Not provided")
        ProfileValueRow("Session preference", profile.sessionDuration?.label ?: "Not provided")
        ProfileValueRow("Environment", profile.environment?.label ?: "Not provided")
        ProfileValueRow(
            "Equipment",
            profile.equipment.takeIf { it.isNotEmpty() }?.joinToString { it.label } ?: "Not provided",
        )
        ProfileValueRow(
            "Priority muscles",
            profile.priorityMuscles.takeIf { it.isNotEmpty() }?.joinToString { it.label } ?: "Not provided",
        )
        ProfileValueRow(
            "Current plan",
            state.planDayNames.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Not available",
        )
        Text(
            text = "These optional profile fields are context only unless another screen explicitly states a current product effect. They do not invent workout history or silently regenerate your plan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WorkoutInsightsScreen(
    historyState: HistoryUiState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val summary = profileInsightsOf(historyState)
    ProfilePageShell(
        title = "Workout insights",
        eyebrow = "Plan & progress",
        onBack = onBack,
    ) {
        if (summary.totalWorkouts == 0) {
            ProfilePlaceholderState(
                title = "Your evidence starts with a saved workout",
                explanation = "Insights use the same local sessions as History V2. Nothing is estimated and no sample workout is created for this screen.",
                status = "No history",
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileMetricTile(
                    label = "Workouts",
                    value = summary.totalWorkouts.toString(),
                    modifier = Modifier.weight(1f),
                )
                ProfileMetricTile(
                    label = "Personal records",
                    value = summary.personalRecords.toString(),
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileMetricTile(
                    label = "Total sets",
                    value = summary.totalSets.toString(),
                    modifier = Modifier.weight(1f),
                )
                ProfileMetricTile(
                    label = "Total reps",
                    value = summary.totalReps.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
            ProfileContentCard {
                Text(
                    text = "TRAINING EVIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                ProfileValueRow("Rep volume", "${formatNumber(summary.totalVolumeKg)} kg")
                ProfileValueRow(
                    "Average duration",
                    summary.averageDurationSeconds?.let { MeasurementFormatter.duration(it).inline() }
                        ?: "Not recorded",
                )
                ProfileValueRow("Most logged exercise", summary.favoriteExercise ?: "Not available")
                ProfileValueRow("First saved workout", summary.firstSavedWorkout ?: "Not available")
                Text(
                    text = "Volume includes rep-based sets only. Time and distance sessions remain valid history but do not produce fake kg or rep totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        androidx.compose.material3.Button(
            onClick = onOpenHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text("Open workout history", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ProfileValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.1f),
        )
    }
}

private fun formatNumber(value: Double): String = value
    .toBigDecimal()
    .setScale(if (value % 1.0 == 0.0) 0 else 1, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()
