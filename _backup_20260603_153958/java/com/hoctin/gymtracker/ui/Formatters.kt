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
