package com.hoctin.gymtracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

/**
 * Canonical list of weekdays as ISO-8601 day-of-week numbers (Monday = 1 … Sunday = 7).
 * Used for both onboarding and the Settings escape hatch so the representation stays consistent
 * and is never tied to a fixed number of training days.
 */
val WEEKDAYS: List<DayOfWeek> = DayOfWeek.values().toList()

fun DayOfWeek.fullLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

fun DayOfWeek.shortLabel(): String = fullLabel().take(3)

enum class WeekdayPickerStyle {
    DEFAULT,
    ONBOARDING,
}

/** Human-readable summary of selected weekdays, e.g. "Mon, Wed, Fri". */
fun formatWeekdays(weekdays: Set<Int>): String {
    if (weekdays.isEmpty()) return "None selected"
    return WEEKDAYS.filter { it.value in weekdays }.joinToString(", ") { it.shortLabel() }
}

/**
 * A vertical list of selectable weekday rows. Stateless: the caller owns [selected] and reacts to
 * [onToggle] with the ISO day number (1..7).
 */
@Composable
fun WeekdayPicker(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: WeekdayPickerStyle = WeekdayPickerStyle.DEFAULT,
    maxSelections: Int = 7,
    motionEnabled: Boolean = true,
    onSelectionBlocked: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WEEKDAYS.forEach { day ->
            WeekdayRow(
                label = day.fullLabel(),
                selected = day.value in selected,
                style = style,
                motionEnabled = motionEnabled,
                onClick = {
                    if (day.value in selected || selected.size < maxSelections) {
                        onToggle(day.value)
                    } else {
                        onSelectionBlocked()
                    }
                },
            )
        }
    }
}

@Composable
private fun WeekdayRow(
    label: String,
    selected: Boolean,
    style: WeekdayPickerStyle,
    motionEnabled: Boolean,
    onClick: () -> Unit,
) {
    // Lightweight selection motion: colours cross-fade, the row settles with a tiny spring, and the
    // check icon scale-bounces + cross-fades in. All small per-element animations (7 rows max).
    val selectedContainer = when (style) {
        WeekdayPickerStyle.DEFAULT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        WeekdayPickerStyle.ONBOARDING -> Color(0xFF2E153B)
    }
    val unselectedContainer = when (style) {
        WeekdayPickerStyle.DEFAULT -> MaterialTheme.colorScheme.surface
        WeekdayPickerStyle.ONBOARDING -> Color(0xFF211F24)
    }
    val selectedBorder = when (style) {
        WeekdayPickerStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        WeekdayPickerStyle.ONBOARDING -> Color(0xFFC47CFF)
    }
    val unselectedBorder = when (style) {
        WeekdayPickerStyle.DEFAULT -> MaterialTheme.colorScheme.outlineVariant
        WeekdayPickerStyle.ONBOARDING -> Color.White.copy(alpha = 0.22f)
    }
    val colorDuration = if (motionEnabled) 160 else 0
    val container by animateColorAsState(
        targetValue = if (selected) selectedContainer else unselectedContainer,
        animationSpec = tween(colorDuration),
        label = "weekdayContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedBorder else unselectedBorder,
        animationSpec = tween(colorDuration),
        label = "weekdayBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = when (style) {
            WeekdayPickerStyle.DEFAULT -> if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            WeekdayPickerStyle.ONBOARDING -> if (selected) Color.White else Color(0xFFC5BACB)
        },
        animationSpec = tween(colorDuration),
        label = "weekdayContent",
    )
    val iconTint by animateColorAsState(
        targetValue = when (style) {
            WeekdayPickerStyle.DEFAULT -> if (selected) selectedBorder else MaterialTheme.colorScheme.onSurfaceVariant
            WeekdayPickerStyle.ONBOARDING -> if (selected) selectedBorder else Color(0xFFC5BACB)
        },
        animationSpec = tween(colorDuration),
        label = "weekdayIconTint",
    )
    val rowScale by animateFloatAsState(
        targetValue = if (style == WeekdayPickerStyle.ONBOARDING || selected) 1f else 0.99f,
        animationSpec = if (motionEnabled) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(0)
        },
        label = "weekdayRowScale",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.85f,
        animationSpec = if (motionEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(0)
        },
        label = "weekdayIconScale",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large,
            )
            .semantics {
                stateDescription = if (selected) "$label selected" else "$label not selected"
            }
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
        )
        Crossfade(
            targetState = selected,
            animationSpec = tween(if (motionEnabled) 160 else 0),
            label = "weekdayCheck",
        ) { isSelected ->
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale),
            )
        }
    }
}
