package com.hoctin.gymtracker.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.Equipment
import com.hoctin.gymtracker.data.catalog.Level
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.data.catalog.ProgressionType
import com.hoctin.gymtracker.ui.components.GymBadge
import com.hoctin.gymtracker.ui.components.ExerciseThumbnail
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.format.SetDisplay
import com.hoctin.gymtracker.ui.theme.auronCardElevation
import com.hoctin.gymtracker.ui.theme.auronGlassSurface
import com.hoctin.gymtracker.ui.theme.auronScreenBackground
import com.hoctin.gymtracker.ui.theme.auronScreenTopPadding

// enumLabel(...) is shared from CatalogExerciseDetailSheet.kt (same package).

@Composable
fun CatalogPickerScreen(
    viewModel: CatalogPickerViewModel,
    onBack: () -> Unit,
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    // Tapping a row opens its detail popup (the VM already holds the CatalogExercise, so no lookup).
    var detailExercise by remember { mutableStateOf<CatalogExercise?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.added.collect { name ->
            snackbarHostState.showSnackbar("Added $name")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auronScreenBackground(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = auronScreenTopPadding(12.dp), bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Add from catalog",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.dayName.isNotBlank()) {
                            Text(
                                text = "to ${state.dayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    singleLine = true,
                    placeholder = { Text("Search name or alias") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Compact dropdown-backed filter chips (scales to many enum values).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterDropdownChip(
                        label = "Equipment",
                        selected = state.equipment,
                        options = Equipment.entries,
                        optionLabel = { enumLabel(it.name) },
                        onSelect = viewModel::setEquipment,
                    )
                    FilterDropdownChip(
                        label = "Muscle",
                        selected = state.primaryMuscle,
                        // UI-1 Part G: hide the generic SHOULDERS option (it returns no results and
                        // confuses the filter). The specific delt heads stay. Enum/schema unchanged.
                        options = MuscleGroup.entries.filter { it != MuscleGroup.SHOULDERS },
                        optionLabel = { enumLabel(it.name) },
                        onSelect = viewModel::setPrimaryMuscle,
                    )
                    FilterDropdownChip(
                        label = "Level",
                        selected = state.level,
                        options = Level.entries,
                        optionLabel = { enumLabel(it.name) },
                        onSelect = viewModel::setLevel,
                    )
                    FilterDropdownChip(
                        label = "Progression",
                        selected = state.progressionType,
                        options = ProgressionType.entries,
                        optionLabel = { enumLabel(it.name) },
                        onSelect = viewModel::setProgressionType,
                    )
                }
            }

            val results = state.results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (results.isEmpty() && !state.loading) {
                    item(key = "empty") {
                        Text(
                            text = "No exercises match those filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
                items(results, key = { it.slug }) { exercise ->
                    CatalogRow(
                        exercise = exercise,
                        onAdd = { viewModel.add(exercise) },
                        onOpenDetail = { detailExercise = exercise },
                    )
                }
            }
        }

        detailExercise?.let { exercise ->
            CatalogExerciseDetailSheet(exercise = exercise, onDismiss = { detailExercise = null })
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun <T> FilterDropdownChip(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected?.let(optionLabel) ?: label) },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogRow(
    exercise: CatalogExercise,
    onAdd: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    Card(
        onClick = onOpenDetail,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = auronGlassSurface()),
        border = gymContainerBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .auronCardElevation(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseThumbnail(
                imageRef = exercise.imageRefs.firstOrNull(),
                exerciseName = exercise.name,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exerciseSubtitle(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // FlowRow so the progression chip wraps to a clean second line on narrow widths
                // instead of crushing into a vertical letter stack (DB-6F). Each GymBadge is itself
                // single-line, so the whole metadata block stays readable and balanced.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GymBadge(
                        SetDisplay.targetLabel(
                            targetSets = exercise.defaultSets,
                            targetMinReps = exercise.defaultMinReps,
                            targetMaxReps = exercise.defaultMaxReps,
                            trackingType = exercise.trackingType,
                        ),
                    )
                    GymBadge(enumLabel(exercise.progressionType.name))
                }
            }
            FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "Add ${exercise.name}")
            }
        }
    }
}

/** "Barbell · Chest" — equipment plus primary muscle(s). */
private fun exerciseSubtitle(exercise: CatalogExercise): String {
    val muscles = exercise.primaryMuscles.joinToString(", ") { enumLabel(it.name) }
    val equipment = enumLabel(exercise.equipment.name)
    return if (muscles.isBlank()) equipment else "$equipment · $muscles"
}
