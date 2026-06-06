package com.hoctin.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        WEEKDAYS.forEach { day ->
            WeekdayRow(
                label = day.fullLabel(),
                selected = day.value in selected,
                onClick = { onToggle(day.value) },
            )
        }
    }
}

@Composable
private fun WeekdayRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (selected) "$label selected" else "$label not selected",
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
