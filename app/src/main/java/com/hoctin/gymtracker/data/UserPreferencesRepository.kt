package com.hoctin.gymtracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

/**
 * Why a workout day is currently selected on Home. Drives the hero/section label wording:
 * AUTO = suggested for today, MANUAL = user picked/skipped, ADVANCED = auto-advanced after a save.
 */
enum class DaySelectionOrigin(val storageValue: String) {
    AUTO("auto"),
    MANUAL("manual"),
    ADVANCED("advanced");

    companion object {
        fun fromStorageValue(value: String?): DaySelectionOrigin {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("gym_tracker_preferences", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorageValue(preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.storageValue)),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _selectedDayId = MutableStateFlow(
        preferences.getLong(KEY_SELECTED_DAY_ID, NO_DAY_SELECTED).takeIf { it != NO_DAY_SELECTED },
    )

    /** The workout day the user manually picked on Home, or null to fall back to the auto pick. */
    val selectedDayId: StateFlow<Long?> = _selectedDayId.asStateFlow()

    private val _selectionOrigin = MutableStateFlow(
        DaySelectionOrigin.fromStorageValue(preferences.getString(KEY_SELECTION_ORIGIN, null)),
    )

    /** Why [selectedDayId] is set (manual pick/skip vs. auto-advanced after completing a workout). */
    val selectionOrigin: StateFlow<DaySelectionOrigin> = _selectionOrigin.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false),
    )

    /** True once the user has finished first-launch onboarding (picking training weekdays). */
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _trainingWeekdays = MutableStateFlow(
        decodeWeekdays(preferences.getString(KEY_TRAINING_WEEKDAYS, null)),
    )

    /**
     * Weekdays the user trains on, as ISO-8601 day-of-week numbers (Monday = 1 … Sunday = 7).
     * Stored as a sorted CSV so the format stays clear and is not tied to a fixed day count.
     */
    val trainingWeekdays: StateFlow<Set<Int>> = _trainingWeekdays.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
        _themeMode.value = mode
    }

    fun setSelectedDayId(dayId: Long?, origin: DaySelectionOrigin = DaySelectionOrigin.MANUAL) {
        val resolvedOrigin = if (dayId == null) DaySelectionOrigin.AUTO else origin
        preferences.edit()
            .putLong(KEY_SELECTED_DAY_ID, dayId ?: NO_DAY_SELECTED)
            .putString(KEY_SELECTION_ORIGIN, resolvedOrigin.storageValue)
            .apply()
        _selectedDayId.value = dayId
        _selectionOrigin.value = resolvedOrigin
    }

    /** Update the training weekdays (ISO 1..7) without changing onboarding status. */
    fun setTrainingWeekdays(weekdays: Set<Int>) {
        val sanitized = weekdays.filter { it in 1..7 }.toSortedSet()
        preferences.edit().putString(KEY_TRAINING_WEEKDAYS, encodeWeekdays(sanitized)).apply()
        _trainingWeekdays.value = sanitized
    }

    /** Persist the chosen training weekdays and mark onboarding complete in one step. */
    fun completeOnboarding(weekdays: Set<Int>) {
        val sanitized = weekdays.filter { it in 1..7 }.toSortedSet()
        preferences.edit()
            .putString(KEY_TRAINING_WEEKDAYS, encodeWeekdays(sanitized))
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
        _trainingWeekdays.value = sanitized
        _onboardingCompleted.value = true
    }

    private fun encodeWeekdays(weekdays: Set<Int>): String =
        weekdays.sorted().joinToString(separator = ",")

    private fun decodeWeekdays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSortedSet()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SELECTED_DAY_ID = "selected_day_id"
        const val KEY_SELECTION_ORIGIN = "selection_origin"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_TRAINING_WEEKDAYS = "training_weekdays"
        const val NO_DAY_SELECTED = -1L
    }
}
