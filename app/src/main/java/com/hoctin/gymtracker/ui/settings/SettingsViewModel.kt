package com.hoctin.gymtracker.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.ThemeMode
import com.hoctin.gymtracker.data.UserPreferencesRepository
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.data.research.RecoveryResearchRepository
import com.hoctin.gymtracker.data.research.ResearchConsentState
import com.hoctin.gymtracker.data.wearable.WearableHomeContext
import com.hoctin.gymtracker.data.wearable.WearableShadowRepository
import com.hoctin.gymtracker.data.wearable.WearableSnapshotDiagnostic
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val exportText: String = "",
    val importText: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnitSystem: DistanceUnitSystem = DistanceUnitSystem.METRIC,
    val workoutGoal: WorkoutGoalSelection = WorkoutGoalSelection.AUTO,
    val trainingWeekdays: Set<Int> = emptySet(),
    val requiredDays: Int = 0,
    val message: String? = null,
    val busy: Boolean = false,
    val recoveryResearchEnabled: Boolean = false,
    val trainingProfileJson: String? = null,
    val planDayNames: List<String> = emptyList(),
    val wearableContext: WearableHomeContext = WearableHomeContext(),
    val latestWearableSnapshot: WearableSnapshotDiagnostic? = null,
)

class SettingsViewModel(
    private val repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val catalogRepository: ExerciseCatalogRepository,
    private val recoveryResearchRepository: RecoveryResearchRepository,
    private val wearableShadowRepository: WearableShadowRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = preferencesRepository.themeMode.value,
            distanceUnitSystem = preferencesRepository.distanceUnitSystem.value,
            workoutGoal = preferencesRepository.workoutGoal.value,
            trainingWeekdays = preferencesRepository.trainingWeekdays.value,
            trainingProfileJson = preferencesRepository.trainingProfileJson.value,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            wearableShadowRepository.latestSnapshotDiagnostic.collect { diagnostic ->
                _uiState.value = _uiState.value.copy(latestWearableSnapshot = diagnostic)
            }
        }
        viewModelScope.launch {
            wearableShadowRepository.homeContext.collect { context ->
                _uiState.value = _uiState.value.copy(wearableContext = context)
            }
        }
        viewModelScope.launch {
            recoveryResearchRepository.observeConsent().collect { consent ->
                _uiState.value = _uiState.value.copy(
                    recoveryResearchEnabled = consent?.stateCode == ResearchConsentState.GRANTED.name,
                )
            }
        }
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesRepository.distanceUnitSystem.collect { system ->
                _uiState.value = _uiState.value.copy(distanceUnitSystem = system)
            }
        }
        viewModelScope.launch {
            preferencesRepository.trainingWeekdays.collect { days ->
                _uiState.value = _uiState.value.copy(trainingWeekdays = days)
            }
        }
        viewModelScope.launch {
            preferencesRepository.workoutGoal.collect { goal ->
                _uiState.value = _uiState.value.copy(workoutGoal = goal)
            }
        }
        viewModelScope.launch {
            preferencesRepository.trainingProfileJson.collect { profileJson ->
                _uiState.value = _uiState.value.copy(trainingProfileJson = profileJson)
            }
        }
        viewModelScope.launch {
            // The number of training days that should be chosen tracks the current plan size.
            repository.observePlan().collect { plan ->
                _uiState.value = _uiState.value.copy(
                    requiredDays = plan.size.coerceIn(0, 7),
                    planDayNames = plan.map { it.day.name },
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }

    fun setDistanceUnitSystem(system: DistanceUnitSystem) {
        preferencesRepository.setDistanceUnitSystem(system)
    }

    fun setTrainingWeekdays(weekdays: Set<Int>) {
        preferencesRepository.setTrainingWeekdays(weekdays)
    }

    fun setWorkoutGoal(selection: WorkoutGoalSelection) {
        preferencesRepository.setWorkoutGoal(selection)
    }

    fun setRecoveryResearchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            recoveryResearchRepository.setConsent(enabled)
            if (enabled) {
                wearableShadowRepository.refreshConnectionState("RESEARCH_CONSENT_GRANTED")
            } else {
                wearableShadowRepository.pauseForResearchRevocation()
            }
            _uiState.value = _uiState.value.copy(
                message = if (enabled) {
                    "Recovery research enabled for future workout attempts."
                } else {
                    "Recovery research capture disabled. Existing local records were kept."
                },
            )
        }
    }

    fun prepareRecoveryResearchExport(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, message = null)
            runCatching { recoveryResearchRepository.exportDeterministicJson() }
                .onSuccess { json ->
                    _uiState.value = _uiState.value.copy(busy = false)
                    onReady(json)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        message = "Research export failed: ${error.message ?: "unknown error"}",
                    )
                }
        }
    }

    fun deleteRecoveryResearchData() {
        viewModelScope.launch {
            wearableShadowRepository.deleteImportedHealthData()
            recoveryResearchRepository.deleteAllResearchData()
            _uiState.value = _uiState.value.copy(
                recoveryResearchEnabled = false,
                message = "Local recovery research data deleted.",
            )
        }
    }

    fun onHealthPermissionResult(grantedPermissions: Set<String>) {
        viewModelScope.launch {
            runCatching { wearableShadowRepository.enableAfterPermissionResult(grantedPermissions) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = if (grantedPermissions.isEmpty()) {
                            "Health Connect access was not granted. AURON readiness is unchanged."
                        } else {
                            "Health Connect shadow sync updated. It is not used in readiness."
                        },
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        message = "Health Connect setup failed: ${error.message ?: "unknown error"}",
                    )
                }
        }
    }

    fun refreshHealthContext() {
        viewModelScope.launch {
            runCatching { wearableShadowRepository.refreshExplicit() }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        message = "Health Connect shadow refresh finished. Missing provider data remains missing.",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        message = "Health Connect refresh failed: ${error.message ?: "unknown error"}",
                    )
                }
        }
    }

    fun disconnectHealthConnect() {
        viewModelScope.launch {
            wearableShadowRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                message = "Health Connect disconnected. Imported local context was retained until deletion.",
            )
        }
    }

    fun deleteImportedHealthData() {
        viewModelScope.launch {
            wearableShadowRepository.deleteImportedHealthData()
            _uiState.value = _uiState.value.copy(
                message = "Imported Health Connect context and wearable snapshots were deleted locally.",
            )
        }
    }

    fun healthConnectManageAccessIntent(): Intent? = wearableShadowRepository.manageAccessIntent()

    fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
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
            runCatching {
                repository.importJson(json)
                // DB-PRE7B: imported v1/v2 rows have no canonical key (and v3 rows already do); resolve
                // any missing keys now so cross-day history works immediately after import. Idempotent.
                catalogRepository.ensureSeeded()
                repository.backfillCanonicalKeys(catalogRepository.canonicalSlugsByName())
            }
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
