package com.hoctin.gymtracker.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import com.hoctin.gymtracker.ui.components.StatChip
import com.hoctin.gymtracker.ui.toLongDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(viewModel: ExerciseDetailViewModel) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val historyBySession = state.history.groupBy { it.sessionId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = state.exercise?.name ?: state.history.firstOrNull()?.exerciseNameSnapshot ?: "Exercise",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                state.exercise?.let { exercise ->
                    Text(
                        text = "${exercise.targetSets} sets, ${exercise.targetMinReps}-${exercise.targetMaxReps} reps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.TrendingUp, contentDescription = null)
                        Text(
                            text = state.suggestion.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = state.suggestion.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.suggestion.suggestedWeight?.let { weight ->
                        StatChip("Suggested ${ProgressiveOverloadEngine.formatWeight(weight)}kg")
                    }
                }
            }
        }

        if (historyBySession.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No history yet.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        items(historyBySession.entries.toList(), key = { it.key }) { entry ->
            val first = entry.value.first()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${first.dateEpochDay.toLongDate()} - ${first.dayNameSnapshot}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    entry.value.forEach { set ->
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${set.setNumber}. ${ProgressiveOverloadEngine.formatWeight(set.weight)}kg x ${set.reps}, RIR ${set.rir}",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (set.isPrWeight || set.isPrReps) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEvents,
                                    contentDescription = "PR",
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
