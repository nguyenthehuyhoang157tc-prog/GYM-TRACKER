package com.hoctin.gymtracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.ThemeMode
import com.hoctin.gymtracker.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val exportText: String = "",
    val importText: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val trainingWeekdays: Set<Int> = emptySet(),
    val requiredDays: Int = 0,
    val message: String? = null,
    val busy: Boolean = false,
)

class SettingsViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = preferencesRepository.themeMode.value,
            trainingWeekdays = preferencesRepository.trainingWeekdays.value,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesRepository.trainingWeekdays.collect { days ->
                _uiState.value = _uiState.value.copy(trainingWeekdays = days)
            }
        }
        viewModelScope.launch {
            // The number of training days that should be chosen tracks the current plan size.
            repository.observePlan().collect { plan ->
                _uiState.value = _uiState.value.copy(requiredDays = plan.size.coerceIn(0, 7))
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }

    fun setTrainingWeekdays(weekdays: Set<Int>) {
        preferencesRepository.setTrainingWeekdays(weekdays)
    }

    fun updateImportText(value: String) {
        _uiState.value = _uiState.value.copy(importText = value)
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            val json = repository.exportJson()
            _uiState.value = _uiState.value.copy(
                exportText = json,
                busy = false,
                message = "Export generated.",
            )
        }
    }

    fun importData() {
        val json = _uiState.value.importText
        if (json.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Paste exported JSON first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            runCatching { repository.importJson(json) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        message = "Import complete.",
                        importText = "",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        message = "Import failed: ${error.message ?: "invalid JSON"}",
                    )
                }
        }
    }
}
