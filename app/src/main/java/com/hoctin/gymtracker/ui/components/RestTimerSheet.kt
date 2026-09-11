package com.hoctin.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hoctin.gymtracker.ui.log.CompactField
import com.hoctin.gymtracker.ui.log.RestTimerUiState
import com.hoctin.gymtracker.ui.log.RestTimerUnit
import com.hoctin.gymtracker.ui.log.SegmentedChoices
import com.hoctin.gymtracker.ui.log.formatRestDuration

/**
 * LOG-V4B — the single rest/timer sheet shared by Log Workout and Daily Win.
 *
 * Extracted verbatim from `LogWorkoutScreen.RestTimerSheet` so the two callers cannot drift.
 * Everything the two flows disagree on is a parameter:
 *
 * | | Log Workout | Daily Win |
 * |---|---|---|
 * | [accent] | `0xFF45D483` (rest green) | `dailyWinAccent()` (lime) |
 * | [maxHeightFraction] | `0.56f` | `0.60f` |
 * | [showSmartToggle] | true (rest-suggestion lightbulb) | false |
 * | [showDurationInput] | true (Sec/Min entry) | false (fixed 2:00) |
 * | [showSkipContinue] | `state.advanceAfterRest` | false |
 * | [overlay] | none | the one-shot confetti on DONE |
 *
 * [overlay] is drawn on top of the sheet body with `matchParentSize`. It exists because a Dialog
 * renders into its own window: anything drawn in the caller's composition (Home) would sit *behind*
 * the sheet, so the confetti has to be handed in rather than overlaid outside.
 *
 * The sheet owns no countdown logic — the caller drives [RestTimerUiState] and owns completion.
 */
@Composable
fun RestTimerSheet(
    state: RestTimerUiState,
    accent: Color,
    title: String = "Rest Timer",
    showSmartToggle: Boolean = true,
    showDurationInput: Boolean = true,
    showSkipContinue: Boolean = false,
    maxHeightFraction: Float = 0.56f,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    onSmartChanged: (Boolean) -> Unit,
    onInputChanged: (String) -> Unit,
    onUnitChanged: (RestTimerUnit) -> Unit,
    onReset: () -> Unit,
    onToggleRunning: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit,
) {
    AuronCompactModalSheet(
        onDismiss = onDismiss,
        maxHeightFraction = maxHeightFraction,
        applyImePadding = true,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (showSmartToggle) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable { onSmartChanged(!state.smartEnabled) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = if (state.smartEnabled) "Smart rest suggestion on" else "Smart rest suggestion off",
                                tint = if (state.smartEnabled) Color(0xFF45D483) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                    Text(
                        title,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close ${title.lowercase()}")
                    }
                }
                val progress = if (state.totalSeconds > 0) state.remainingSeconds.toFloat() / state.totalSeconds else 0f
                Box(Modifier.size(184.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFF262A31),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progress.coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    Text(
                        formatRestDuration(state.remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                }
                if (showDurationInput) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompactField(
                            value = state.inputValue,
                            onValueChange = onInputChanged,
                            placeholder = "90",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            modifier = Modifier.weight(0.75f),
                            centered = true,
                        )
                        SegmentedChoices(
                            choices = listOf("Sec", "Min"),
                            selectedIndex = if (state.unit == RestTimerUnit.SEC) 0 else 1,
                            onSelected = { onUnitChanged(if (it == 0) RestTimerUnit.SEC else RestTimerUnit.MIN) },
                            modifier = Modifier.weight(1.25f),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TimerCircleButton(Icons.Outlined.Replay, "Reset timer", onReset)
                    Spacer(Modifier.width(24.dp))
                    TimerCircleButton(
                        if (state.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        if (state.running) "Pause timer" else "Start timer",
                        onToggleRunning,
                        primary = true,
                    )
                }
                if (showSkipContinue) {
                    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip rest & continue", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            if (overlay != null) {
                Box(Modifier.matchParentSize()) { overlay() }
            }
        }
    }
}

@Composable
private fun TimerCircleButton(icon: ImageVector, description: String, onClick: () -> Unit, primary: Boolean = false) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (primary) Color.White else Color(0xFF24272D))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (primary) Color(0xFF111218) else Color.White)
    }
}
