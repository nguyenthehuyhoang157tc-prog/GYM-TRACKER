package com.hoctin.gymtracker.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoctin.gymtracker.data.GymRepository
import com.hoctin.gymtracker.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    /** Number of training weekdays the user must choose, derived from the current plan size. */
    val requiredDays: Int = 0,
    /** Weekdays (ISO 1..7) already chosen previously, so re-onboarding can pre-select them. */
    val initialSelection: Set<Int> = emptySet(),
    /** True once seed data is ensured and the plan count is known. */
    val ready: Boolean = false,
)

class OnboardingViewModel(
    repository: GymRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val ready = MutableStateFlow(false)

    val uiState: StateFlow<OnboardingUiState> = combine(
        ready,
        repository.observePlan(),
        preferencesRepository.trainingWeekdays,
    ) { isReady, plan, savedWeekdays ->
        OnboardingUiState(
            // A week has 7 days, so the achievable target is capped at 7 even if the plan grows larger.
            requiredDays = plan.size.coerceIn(0, 7),
            initialSelection = savedWeekdays,
            ready = isReady,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState(),
    )

    init {
        viewModelScope.launch {
            // Onboarding shows before Home, so make sure the default plan exists here too.
            repository.ensureSeedData()
            ready.value = true
        }
    }

    fun complete(selectedWeekdays: Set<Int>) {
        preferencesRepository.completeOnboarding(selectedWeekdays)
    }
}
