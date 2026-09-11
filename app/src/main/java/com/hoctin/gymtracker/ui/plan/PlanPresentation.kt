package com.hoctin.gymtracker.ui.plan

import android.animation.ValueAnimator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.CanonicalExerciseKey
import com.hoctin.gymtracker.data.catalog.MuscleGroup
import com.hoctin.gymtracker.ui.catalog.enumLabel
import com.hoctin.gymtracker.ui.theme.isAuronDarkTheme

@Composable
internal fun planV3Surface(): Color =
    if (isAuronDarkTheme()) Color(0xFF0D0E11) else MaterialTheme.colorScheme.surface

@Composable
internal fun planV3MutedSurface(): Color =
    if (isAuronDarkTheme()) Color(0xFF111218) else MaterialTheme.colorScheme.surfaceVariant

@Composable
internal fun Modifier.planV3Pressable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && motionEnabled) 0.982f else 1f,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 520f),
        label = "planV3PressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}

@Composable
internal fun Modifier.planV3PageEntrance(): Modifier {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    var entered by remember { mutableStateOf(!motionEnabled) }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (motionEnabled) 300 else 0),
        label = "planV3PageAlpha",
    )
    val offset by animateFloatAsState(
        targetValue = if (entered) 0f else 14f,
        animationSpec = tween(if (motionEnabled) 300 else 0),
        label = "planV3PageOffset",
    )
    LaunchedEffect(Unit) { entered = true }
    return graphicsLayer {
        this.alpha = alpha
        translationY = offset
    }
}

internal fun catalogLookup(exercises: List<CatalogExercise>): Map<String, CatalogExercise> {
    val lookup = LinkedHashMap<String, CatalogExercise>()
    exercises.forEach { exercise ->
        (listOf(exercise.name) + exercise.aliases).forEach { name ->
            lookup.putIfAbsent(CanonicalExerciseKey.normalizeName(name), exercise)
        }
    }
    return lookup
}

internal fun CatalogExercise.muscleTags(limit: Int = 3): String =
    (primaryMuscles + secondaryMuscles)
        .distinct()
        .take(limit)
        .joinToString("  ") { muscle -> "#${enumLabel(muscle.name).lowercase().replace(' ', '_')}" }

internal fun CatalogExercise.metadataLabel(): String =
    "${enumLabel(level.name)}, ${enumLabel(equipment.name)}"

internal fun CatalogExercise.firstThumbnailRef(): String? = imageRefs.firstOrNull()

internal fun builderFilterMuscles(): List<MuscleGroup> =
    MuscleGroup.entries.filterNot { it == MuscleGroup.SHOULDERS }

internal fun filterBuilderExercises(
    exercises: List<CatalogExercise>,
    query: String,
    muscle: MuscleGroup?,
): List<CatalogExercise> = exercises.filter { exercise ->
    val matchesQuery = query.isBlank() ||
        exercise.name.contains(query, ignoreCase = true) ||
        exercise.aliases.any { it.contains(query, ignoreCase = true) }
    val matchesMuscle = muscle == null ||
        muscle in exercise.primaryMuscles || muscle in exercise.secondaryMuscles
    matchesQuery && matchesMuscle
}
