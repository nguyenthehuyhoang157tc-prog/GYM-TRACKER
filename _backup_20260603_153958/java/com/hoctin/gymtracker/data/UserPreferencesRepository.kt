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

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
        _themeMode.value = mode
    }

    fun setSelectedDayId(dayId: Long?) {
        preferences.edit().putLong(KEY_SELECTED_DAY_ID, dayId ?: NO_DAY_SELECTED).apply()
        _selectedDayId.value = dayId
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SELECTED_DAY_ID = "selected_day_id"
        const val NO_DAY_SELECTED = -1L
    }
}
