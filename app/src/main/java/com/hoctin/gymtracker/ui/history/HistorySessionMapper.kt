package com.hoctin.gymtracker.ui.history

import com.hoctin.gymtracker.data.SessionWithSets
import com.hoctin.gymtracker.data.WorkoutSessionEntity
import com.hoctin.gymtracker.ui.format.SetDisplay

internal fun sessionDisplayTitle(session: WorkoutSessionEntity): String =
    session.customTitle?.trim()?.takeIf { it.isNotBlank() } ?: session.dayNameSnapshot

/**
 * MUSCLE-3: one saved session → the [HistorySession] display model. Extracted from HistoryViewModel
 * (unchanged logic) so the after-save / session review screen can reuse the exact same exercise
 * breakdown mapping History renders — one source of truth for grouping and aggregates.
 */
internal fun historySessionOf(sws: SessionWithSets): HistorySession {
    val byExercise = sws.sets.groupBy { it.exerciseNameSnapshot }
    val exercises = byExercise.map { (name, exSets) ->
        val sorted = exSets.sortedBy { it.setNumber }
        val aggregate = SetDisplay.aggregate(sorted)
        HistoryExercise(
            name = name,
            setCount = aggregate.setCount,
            totalReps = aggregate.totalReps,
            totalVolume = aggregate.totalVolume,
            durationSeconds = aggregate.durationSeconds,
            distanceMeters = aggregate.distanceMeters,
            hasRepSets = aggregate.hasRepSets,
            hasPr = sorted.any { it.isPrWeight || it.isPrReps },
            sets = sorted.map {
                HistorySet(
                    setNumber = it.setNumber,
                    weight = it.weight,
                    reps = it.reps,
                    rir = it.rir,
                    isPr = it.isPrWeight || it.isPrReps,
                    durationSeconds = it.durationSeconds,
                    distanceMeters = it.distanceMeters,
                )
            },
            isNonRep = sorted.isNotEmpty() &&
                sorted.all { it.durationSeconds != null || it.distanceMeters != null },
        )
    }
    val sessionAggregate = SetDisplay.aggregate(sws.sets)
    return HistorySession(
        id = sws.session.id,
        dayName = sessionDisplayTitle(sws.session),
        dateEpochDay = sws.session.dateEpochDay,
        startedAtMillis = sws.session.startedAtMillis,
        totalSets = sessionAggregate.setCount,
        totalVolume = sessionAggregate.totalVolume,
        durationSeconds = sessionAggregate.durationSeconds,
        distanceMeters = sessionAggregate.distanceMeters,
        hasRepSets = sessionAggregate.hasRepSets,
        prCount = sws.sets.count { it.isPrWeight || it.isPrReps },
        exerciseCount = byExercise.size,
        exercises = exercises,
        // MUSCLE-3B.1/HISTORY-2: persisted wall-clock workout duration (null = pre-feature session).
        wallDurationSeconds = sws.session.durationSeconds?.takeIf { it > 0 },
    )
}
