package com.hoctin.gymtracker.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.ui.components.PrimaryGymButton
import com.hoctin.gymtracker.ui.components.WeekdayPicker

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    // Local selection seeded from any previously saved weekdays (used when re-onboarding).
    var selected by remember(state.initialSelection) { mutableStateOf(state.initialSelection) }

    val requiredDays = state.requiredDays
    val isValid = requiredDays in 1..7 && selected.size == requiredDays

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandHeader()

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Set your training week",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = helperText(state.ready, requiredDays),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = selectionCounter(selected.size, requiredDays),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                WeekdayPicker(
                    selected = selected,
                    onToggle = { day ->
                        selected = if (day in selected) selected - day else selected + day
                    },
                )
            }

            // Pinned CTA stays visible regardless of how far the weekday list is scrolled.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                PrimaryGymButton(
                    text = "Start training",
                    enabled = isValid,
                    onClick = { viewModel.complete(selected) },
                )
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "GYMATE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 8.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "TRAINER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 6.sp,
        )
    }
}

private fun helperText(ready: Boolean, requiredDays: Int): String = when {
    !ready -> "Preparing your plan…"
    requiredDays <= 0 -> "Add workout days to your plan to begin."
    else -> "Choose $requiredDays training ${if (requiredDays == 1) "day" else "days"} for your current plan."
}

private fun selectionCounter(selectedCount: Int, requiredDays: Int): String {
    if (requiredDays <= 0) return ""
    return "$selectedCount / $requiredDays selected"
}
