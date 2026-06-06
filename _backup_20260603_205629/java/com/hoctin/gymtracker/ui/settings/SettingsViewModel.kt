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
    val message: String? = null,
    val busy: Boolean = false,
)

class SettingsViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(themeMode = preferencesRepository.themeMode.value),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
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
