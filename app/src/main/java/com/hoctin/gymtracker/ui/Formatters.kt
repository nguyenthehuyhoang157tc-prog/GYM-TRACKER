package com.hoctin.gymtracker.ui

import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val longDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun Long.toLocalDateFromEpochDay(): LocalDate = LocalDate.ofEpochDay(this)

fun Long.toShortDate(): String = toLocalDateFromEpochDay().format(shortDateFormatter)

fun Long.toLongDate(): String = toLocalDateFromEpochDay().format(longDateFormatter)

fun Long.toClockTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a"))
}

fun LoggedSetEntity.displayLine(): String {
    val weight = ProgressiveOverloadEngine.formatWeight(weight)
    return "$setNumber. ${weight}kg x $reps, RIR $rir"
}

fun SessionWithSets.totalVolume(): Double = sets.sumOf { it.weight * it.reps }

fun SessionWithSets.prCount(): Int = sets.count { it.isPrWeight || it.isPrReps }

/**
 * "Day 3: Legs" style label. [dayNumber] is the 1-based plan position (sortOrder order); when it is
 * unknown (0) or the name is blank the label degrades gracefully so it never reads "Day 0:".
 */
fun workoutDayLabel(dayNumber: Int, name: String?): String {
    val safeName = name?.takeIf { it.isNotBlank() } ?: "Workout"
    return if (dayNumber > 0) "Day $dayNumber: $safeName" else safeName
}

/**
 * Short descriptive subtitle for a workout day. Matching is keyword-based on the day name so renamed
 * days still read sensibly, and any custom/unknown day falls back to a neutral line — no day name is
 * required to exist, so user-created days never break.
 */
fun workoutDaySubtitle(name: String?): String {
    val normalized = name?.lowercase()?.trim().orEmpty()
    return when {
        normalized.contains("push") -> "Upper-body pressing day focused on chest, shoulders, and triceps."
        normalized.contains("pull") -> "Back and biceps day focused on pulling strength and width."
        normalized.contains("leg") -> "Lower-body day focused on quads, hamstrings, and calves."
        normalized.contains("full") -> "Balanced full-body session to keep momentum across the week."
        else -> "Review the plan, then start logging your workout."
    }
}
