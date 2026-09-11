package com.hoctin.gymtracker.ui.settings

import com.hoctin.gymtracker.ui.history.HistoryUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Presentation destinations owned by the Profile & Settings information architecture. */
enum class ProfileDestination {
    CONSISTENCY,
    INSIGHTS,
    PLAN,
    TRAINING,
    PREFERENCES,
    NOTIFICATIONS,
    PERSONAL_PROFILE,
    HISTORY,
    DATA,
    RECOVERY_RESEARCH,
    RESOURCES,
    PRIVACY,
    ABOUT,
}

data class ProfileHubItem(
    val destination: ProfileDestination,
    val title: String,
    val supportingText: String,
    val status: String? = null,
)

data class ProfileHubSection(
    val label: String,
    val items: List<ProfileHubItem>,
)

/**
 * One audited source for every row on the Profile hub. Keeping this pure makes missing/dead entries
 * visible in tests while navigation itself stays in GymTrackerApp.
 */
fun profileHubSections(): List<ProfileHubSection> = listOf(
    ProfileHubSection(
        label = "Plan & progress",
        items = listOf(
            ProfileHubItem(
                ProfileDestination.CONSISTENCY,
                "Streaks & weekly progress",
                "Workout schedule and activity continuity",
            ),
            ProfileHubItem(
                ProfileDestination.INSIGHTS,
                "Workout insights",
                "Evidence from your saved training history",
            ),
            ProfileHubItem(
                ProfileDestination.PLAN,
                "View & modify plan",
                "Exercises, sets and training-day structure",
            ),
        ),
    ),
    ProfileHubSection(
        label = "Preferences",
        items = listOf(
            ProfileHubItem(
                ProfileDestination.TRAINING,
                "Training setup",
                "Coaching goal and planned weekdays",
            ),
            ProfileHubItem(
                ProfileDestination.PREFERENCES,
                "Units & appearance",
                "Theme and distance measurement",
            ),
            ProfileHubItem(
                ProfileDestination.NOTIFICATIONS,
                "Notifications & reminders",
                "A future home for intentional prompts",
                status = "Unavailable",
            ),
        ),
    ),
    ProfileHubSection(
        label = "Your profile",
        items = listOf(
            ProfileHubItem(
                ProfileDestination.PERSONAL_PROFILE,
                "Personal profile",
                "Locally stored onboarding context",
            ),
            ProfileHubItem(
                ProfileDestination.HISTORY,
                "Workout history",
                "Open the complete History V2 archive",
            ),
        ),
    ),
    ProfileHubSection(
        label = "Data & research",
        items = listOf(
            ProfileHubItem(
                ProfileDestination.DATA,
                "Import, export & AI helper",
                "Portable plan and workout-history data",
            ),
            ProfileHubItem(
                ProfileDestination.RECOVERY_RESEARCH,
                "Recovery Research",
                "Optional local validation records",
                status = "Local only",
            ),
        ),
    ),
    ProfileHubSection(
        label = "Help & AURON",
        items = listOf(
            ProfileHubItem(
                ProfileDestination.RESOURCES,
                "Help & resources",
                "Product guidance, FAQ and contact status",
            ),
            ProfileHubItem(
                ProfileDestination.PRIVACY,
                "Privacy & local data",
                "What stays on this device and what can be exported",
            ),
            ProfileHubItem(
                ProfileDestination.ABOUT,
                "About AURON",
                "Product scope and current capabilities",
            ),
        ),
    ),
)

data class ProfileInsightsSummary(
    val totalWorkouts: Int = 0,
    val totalSets: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val personalRecords: Int = 0,
    val averageDurationSeconds: Int? = null,
    val favoriteExercise: String? = null,
    val firstSavedWorkout: String? = null,
)

/** Pure presentation aggregation over the canonical History V2 state. */
fun profileInsightsOf(state: HistoryUiState): ProfileInsightsSummary {
    val sessions = state.months.flatMap { it.sessions }
    if (sessions.isEmpty()) return ProfileInsightsSummary()

    val durations = sessions.mapNotNull { session ->
        session.wallDurationSeconds?.takeIf { it > 0 }
            ?: session.durationSeconds.takeIf { it > 0 }
    }
    val exerciseFrequency = sessions
        .flatMap { session -> session.exercises.map { it.name } }
        .groupingBy { it }
        .eachCount()

    val firstEpochDay = sessions.minOf { it.dateEpochDay }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH)

    return ProfileInsightsSummary(
        totalWorkouts = sessions.size,
        totalSets = sessions.sumOf { it.totalSets },
        totalVolumeKg = sessions.sumOf { it.totalVolume },
        totalReps = sessions.sumOf { session -> session.exercises.sumOf { it.totalReps } },
        personalRecords = sessions.sumOf { it.prCount },
        averageDurationSeconds = durations.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        favoriteExercise = exerciseFrequency.maxWithOrNull(
            compareBy<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key },
        )?.key,
        firstSavedWorkout = LocalDate.ofEpochDay(firstEpochDay).format(dateFormatter),
    )
}
