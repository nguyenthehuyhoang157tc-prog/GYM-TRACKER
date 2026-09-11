package com.hoctin.gymtracker.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoctin.gymtracker.data.catalog.CatalogExercise
import com.hoctin.gymtracker.data.catalog.ExerciseCatalogRepository
import com.hoctin.gymtracker.ui.components.AuronCompactModalSheet
import com.hoctin.gymtracker.ui.components.gymContainerBorder
import com.hoctin.gymtracker.ui.components.rememberExerciseAssetImageBitmap
import com.hoctin.gymtracker.ui.theme.auronGlassSurfaceVariant
import kotlinx.coroutines.launch

/** Title-cases an enum name for display, e.g. SIDE_DELTS -> "Side Delts". Shared across catalog UI. */
fun enumLabel(raw: String): String =
    raw.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

/**
 * Reusable exercise detail popup (AURON bottom sheet). Shows the catalog reference data: title, a
 * carousel-ready media slot (placeholder for now), Quick Breakdown, a clear Primary/Secondary muscle
 * breakdown, step-by-step instructions, and optional tips. Pure UI - pass a [CatalogExercise].
 */
@Composable
fun CatalogExerciseDetailSheet(
    exercise: CatalogExercise,
    onDismiss: () -> Unit,
) {
    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.70f,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        TitleRow(exercise.name)

        MediaSlot(imageRefs = exercise.imageRefs)

        DetailSection(title = "Quick Breakdown", icon = Icons.Outlined.Info) {
            BreakdownRow("Level", enumLabel(exercise.level.name))
            BreakdownRow("Equipment", enumLabel(exercise.equipment.name))
            BreakdownRow("Force", enumLabel(exercise.force.name))
            BreakdownRow("Category", enumLabel(exercise.category.name))
            BreakdownRow("Progression", enumLabel(exercise.progressionType.name))
        }

        DetailSection(title = "Muscle Breakdown", icon = FlexedBicepsIcon) {
            MuscleLine("Primary", exercise.primaryMuscles.map { enumLabel(it.name) })
            MuscleLine("Secondary", exercise.secondaryMuscles.map { enumLabel(it.name) })
        }

        if (exercise.instructions.isNotEmpty()) {
            DetailSection(title = "Instructions", icon = Icons.Outlined.EditNote) {
                exercise.instructions.forEachIndexed { index, step ->
                    NumberedStep(number = index + 1, text = step)
                }
            }
        }

        if (exercise.tips.isNotEmpty()) {
            DetailSection(title = "Tips", icon = Icons.Outlined.TipsAndUpdates) {
                exercise.tips.forEach { tip -> BulletLine(tip) }
            }
        }
    }
}

/** Minimal fallback sheet for a surface that knows only a name with no catalog match (graceful state). */
@Composable
fun CatalogExerciseMissingSheet(
    name: String,
    onDismiss: () -> Unit,
) {
    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.70f,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TitleRow(name)
        MediaSlot(imageRefs = emptyList())
        Text(
            text = "Detailed breakdown isn't available for this exercise yet.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TitleRow(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                .border(gymContainerBorder(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 29.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MediaSlot(imageRefs: List<String>) {
    // DB-3B / DB-3B.5: render bundled local asset images (no network). When multiple frames exist,
    // tapping the image cycles to the next and page dots track the current frame; otherwise a clean
    // placeholder is shown. The detail frame uses contain/fit behavior so portrait and landscape
    // exercise photos keep their full movement context instead of being center-cropped.
    var index by remember(imageRefs) { mutableStateOf(0) }
    val multi = imageRefs.size > 1
    val bitmap = rememberExerciseAssetImageBitmap(
        imageRef = imageRefs.getOrNull(index),
        targetSize = 240.dp,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.large)
            .background(auronGlassSurfaceVariant())
            .border(gymContainerBorder(alpha = 0.08f), MaterialTheme.shapes.large)
            .then(if (multi) Modifier.clickable { index = (index + 1) % imageRefs.size } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (multi) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.34f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    imageRefs.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (i == index) 0.95f else 0.45f)),
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "Image coming soon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        SectionHeader(title = title, icon = icon)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(letterSpacing = 1.2.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MuscleLine(label: String, muscles: List<String>) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp),
        )
        Text(
            text = if (muscles.isEmpty()) "-" else muscles.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(30.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(30.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

private val FlexedBicepsIcon: ImageVector = ImageVector.Builder(
    name = "FlexedBiceps",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = SolidColor(Color.Black),
    ) {
        moveTo(4.7f, 20.0f)
        curveTo(3.4f, 19.1f, 3.4f, 17.3f, 4.8f, 16.4f)
        lineTo(7.2f, 14.9f)
        curveTo(7.9f, 14.4f, 8.4f, 13.7f, 8.6f, 12.8f)
        lineTo(9.2f, 10.4f)
        curveTo(9.6f, 8.9f, 11.1f, 8.1f, 12.5f, 8.6f)
        curveTo(13.9f, 9.1f, 14.6f, 10.7f, 14.0f, 12.1f)
        lineTo(13.4f, 13.6f)
        curveTo(14.5f, 12.9f, 15.7f, 12.5f, 17.0f, 12.5f)
        horizontalLineTo(17.4f)
        lineTo(17.4f, 9.8f)
        horizontalLineTo(16.4f)
        curveTo(15.7f, 9.8f, 15.1f, 9.2f, 15.1f, 8.5f)
        curveTo(15.1f, 7.8f, 15.7f, 7.2f, 16.4f, 7.2f)
        horizontalLineTo(19.0f)
        curveTo(20.3f, 7.2f, 21.3f, 8.2f, 21.3f, 9.5f)
        verticalLineTo(12.1f)
        curveTo(21.3f, 15.7f, 18.5f, 18.5f, 14.7f, 18.5f)
        curveTo(12.9f, 18.5f, 11.2f, 18.1f, 9.8f, 17.3f)
        lineTo(7.9f, 19.3f)
        curveTo(7.1f, 20.3f, 5.7f, 20.6f, 4.7f, 20.0f)
        close()
    }
}.build()

// ---- Reusable controller so any surface can open the popup from just a name --------------------------

/** What the detail host is currently showing. */
sealed interface CatalogDetailState {
    object Hidden : CatalogDetailState
    data class Shown(val exercise: CatalogExercise) : CatalogDetailState
    data class Missing(val name: String) : CatalogDetailState
}

/**
 * Drives the reusable detail popup. [open] shows a known catalog exercise directly; [openByName] looks
 * it up via the repository (for surfaces that only know a name, e.g. plan/log/history) and falls back
 * to a graceful "no details" sheet when there is no match.
 */
class CatalogDetailController internal constructor(
    private val repository: ExerciseCatalogRepository,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    var state by mutableStateOf<CatalogDetailState>(CatalogDetailState.Hidden)
        private set

    fun open(exercise: CatalogExercise) {
        state = CatalogDetailState.Shown(exercise)
    }

    fun openByName(name: String) {
        scope.launch {
            val match = repository.findByName(name)
            state = if (match != null) CatalogDetailState.Shown(match) else CatalogDetailState.Missing(name)
        }
    }

    fun dismiss() {
        state = CatalogDetailState.Hidden
    }
}

@Composable
fun rememberCatalogDetailController(repository: ExerciseCatalogRepository): CatalogDetailController {
    val scope = rememberCoroutineScope()
    return remember(repository) { CatalogDetailController(repository, scope) }
}

/** Renders the popup for the current controller state. Place once near the root of a screen. */
@Composable
fun CatalogDetailHost(controller: CatalogDetailController) {
    when (val current = controller.state) {
        is CatalogDetailState.Shown -> CatalogExerciseDetailSheet(current.exercise, controller::dismiss)
        is CatalogDetailState.Missing -> CatalogExerciseMissingSheet(current.name, controller::dismiss)
        CatalogDetailState.Hidden -> Unit
    }
}
