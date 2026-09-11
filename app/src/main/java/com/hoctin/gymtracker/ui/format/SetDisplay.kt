package com.hoctin.gymtracker.ui.format

import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.data.LoggedSetEntity
import com.hoctin.gymtracker.data.catalog.TrackingType
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine

/**
 * DB-6C: turns stored tracking values into display-safe text, independent of catalog state.
 * Non-rep sets are detected from saved duration/distance fields, so the reps=0 sentinel is never
 * rendered as the user's main set value.
 */
object SetDisplay {
    private const val INLINE_SEPARATOR = " \u2022 "
    private const val SUMMARY_SEPARATOR = " \u00B7 "

    data class Aggregate(
        val setCount: Int,
        val totalReps: Int,
        val totalVolume: Double,
        val durationSeconds: Int,
        val distanceMeters: Double,
        val hasRepSets: Boolean,
    ) {
        val hasDuration: Boolean get() = durationSeconds > 0
        val hasDistance: Boolean get() = distanceMeters > 0.0
    }

    /** True when this set was logged as time/distance, so reps/RIR must be hidden. */
    fun isNonRep(durationSeconds: Int?, distanceMeters: Double?): Boolean =
        durationSeconds != null || distanceMeters != null

    fun isNonRep(set: LoggedSetEntity): Boolean = isNonRep(set.durationSeconds, set.distanceMeters)

    /**
     * The measurements for a non-rep set, in display order. Load is included only when > 0, so a pure
     * TIME_ONLY hold shows just its duration.
     */
    fun nonRepMeasurements(
        durationSeconds: Int?,
        distanceMeters: Double?,
        load: Double,
        system: DistanceUnitSystem,
    ): List<Measurement> = buildList {
        if (load > 0.0) add(MeasurementFormatter.weight(load))
        distanceMeters?.let { add(MeasurementFormatter.distance(it, system)) }
        durationSeconds?.let { add(MeasurementFormatter.duration(it)) }
    }

    fun nonRepMeasurements(set: LoggedSetEntity, system: DistanceUnitSystem): List<Measurement> =
        nonRepMeasurements(set.durationSeconds, set.distanceMeters, set.weight, system)

    /** Single-string form for content descriptions and tests. */
    fun nonRepInline(set: LoggedSetEntity, system: DistanceUnitSystem): String =
        nonRepMeasurements(set, system).joinToString(INLINE_SEPARATOR) { it.inline() }

    fun aggregate(sets: List<LoggedSetEntity>): Aggregate {
        val repSets = sets.filterNot { isNonRep(it) }
        return Aggregate(
            setCount = sets.size,
            totalReps = repSets.sumOf { it.reps },
            totalVolume = repSets.sumOf { it.weight * it.reps },
            durationSeconds = sets.sumOf { it.durationSeconds ?: 0 },
            distanceMeters = sets.sumOf { it.distanceMeters ?: 0.0 },
            hasRepSets = repSets.isNotEmpty(),
        )
    }

    fun summaryLine(
        setCount: Int,
        totalReps: Int,
        totalVolume: Double,
        durationSeconds: Int,
        distanceMeters: Double,
        hasRepSets: Boolean,
        system: DistanceUnitSystem,
        targetSets: Int? = null,
        includeRepCount: Boolean = true,
    ): String {
        val parts = mutableListOf<String>()
        val setText = if (targetSets == null) {
            "$setCount ${setWord(setCount)}"
        } else {
            "$setCount/$targetSets ${setWord(targetSets)}"
        }
        parts += setText
        if (hasRepSets) {
            if (includeRepCount) {
                parts += "$totalReps reps"
            }
            parts += "${ProgressiveOverloadEngine.formatWeight(totalVolume)} kg"
        }
        if (durationSeconds > 0) {
            parts += "${MeasurementFormatter.duration(durationSeconds).inline()} duration"
        }
        if (distanceMeters > 0.0) {
            parts += "${MeasurementFormatter.distance(distanceMeters, system).inline()} distance"
        }
        return parts.joinToString(SUMMARY_SEPARATOR)
    }

    fun summaryLine(
        aggregate: Aggregate,
        system: DistanceUnitSystem,
        targetSets: Int? = null,
        includeRepCount: Boolean = true,
    ): String =
        summaryLine(
            setCount = aggregate.setCount,
            totalReps = aggregate.totalReps,
            totalVolume = aggregate.totalVolume,
            durationSeconds = aggregate.durationSeconds,
            distanceMeters = aggregate.distanceMeters,
            hasRepSets = aggregate.hasRepSets,
            system = system,
            targetSets = targetSets,
            includeRepCount = includeRepCount,
        )

    fun targetLabel(
        targetSets: Int,
        targetMinReps: Int,
        targetMaxReps: Int,
        trackingType: TrackingType,
    ): String {
        val sets = "$targetSets ${setWord(targetSets)}"
        return if (trackingType.isRepBased) {
            "$sets$SUMMARY_SEPARATOR$targetMinReps-$targetMaxReps reps"
        } else {
            "$sets$SUMMARY_SEPARATOR${trackingFieldsLabel(trackingType)}"
        }
    }

    private fun trackingFieldsLabel(type: TrackingType): String {
        val fields = buildList {
            if (type.showsLoad) add("load")
            if (type.showsDistance) add("distance")
            // DB-6F: "time" (not "duration") keeps catalog/plan chips concise and matches the compact
            // "Time" Log field label, so e.g. TIME_DISTANCE_LOAD reads "load + distance + time".
            if (type.showsDuration) add("time")
        }
        return fields.joinToString(" + ")
    }

    private fun setWord(count: Int): String = if (count == 1) "set" else "sets"
}
