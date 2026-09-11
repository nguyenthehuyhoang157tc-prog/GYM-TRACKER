package com.hoctin.gymtracker.ui.format

import com.hoctin.gymtracker.data.DistanceUnitSystem
import com.hoctin.gymtracker.domain.ProgressiveOverloadEngine

/**
 * DB-6C unified measurement model.
 *
 * A formatted, **structured** value: a numeric [value] string plus its [unit] string, kept separate so
 * the UI can style them independently (value normal, unit bold/emphasized) and render them *inline*
 * (`N unit`) consistently across every screen. Formatting never happens by raw string concatenation in a
 * composable — every screen asks [MeasurementFormatter] and renders with `MeasurementValue`.
 *
 * `unit` may be empty (e.g. a duration `01:30` is self-contained, and a plain rep count carries the
 * label elsewhere). This shape is deliberately future-proof: new metrics (velocity m/s, pace, %1RM …)
 * are added as new factory functions returning the same [Measurement], with **no UI redesign**.
 */
data class Measurement(val value: String, val unit: String) {
    /** Single-string form for places that genuinely need one (logs, content descriptions, tests). */
    fun inline(): String = if (unit.isEmpty()) value else "$value $unit"
}

/**
 * The single source of truth for turning stored values (kg, meters, seconds, m/s) into display
 * [Measurement]s. Distance respects the user's [DistanceUnitSystem] (DB stays in meters; only display
 * converts). Pure, no Android/UI dependency, so it is fully unit-testable.
 */
object MeasurementFormatter {

    private const val FEET_PER_METER = 3.280839895
    private const val FEET_PER_MILE = 5280.0
    private const val METERS_PER_KM = 1000.0
    private const val SECONDS_PER_KMH = 3.6 // m/s -> km/h

    /** Weight / load, e.g. `Measurement("80", "kg")`. Trailing `.0` is dropped (reuses the engine rule). */
    fun weight(kg: Double): Measurement = Measurement(ProgressiveOverloadEngine.formatWeight(kg), "kg")

    /** Duration as `mm:ss` (or `hh:mm:ss` past an hour); unit is empty because the format is self-evident. */
    fun duration(seconds: Int): Measurement {
        val s = seconds.coerceAtLeast(0)
        val hh = s / 3600
        val mm = (s % 3600) / 60
        val ss = s % 60
        val text = if (hh > 0) {
            "${hh.pad()}:${mm.pad()}:${ss.pad()}"
        } else {
            "${mm.pad()}:${ss.pad()}"
        }
        return Measurement(text, "")
    }

    /**
     * Distance from stored **meters**, formatted in the user's [system]. Metric: m below 1 km, else km.
     * Imperial: ft below a mile, else mi. Value uses the shared 1-decimal weight formatter.
     */
    fun distance(meters: Double, system: DistanceUnitSystem): Measurement {
        val m = meters.coerceAtLeast(0.0)
        return when (system) {
            DistanceUnitSystem.METRIC ->
                if (m >= METERS_PER_KM) num(m / METERS_PER_KM, "km") else num(m, "m")
            DistanceUnitSystem.IMPERIAL -> {
                val feet = m * FEET_PER_METER
                if (feet >= FEET_PER_MILE) num(feet / FEET_PER_MILE, "mi") else num(feet, "ft")
            }
        }
    }

    /**
     * Future-ready velocity from stored **m/s**, in the user's [system] (m/s metric, km/h imperial-ish).
     * Defined now so DB-6E+ velocity work needs no new formatter/UI scaffolding (compatibility check).
     */
    fun velocity(metersPerSecond: Double, system: DistanceUnitSystem): Measurement {
        val v = metersPerSecond.coerceAtLeast(0.0)
        return when (system) {
            DistanceUnitSystem.METRIC -> num(v, "m/s")
            DistanceUnitSystem.IMPERIAL -> num(v * SECONDS_PER_KMH, "km/h")
        }
    }

    private fun num(value: Double, unit: String) =
        Measurement(ProgressiveOverloadEngine.formatWeight(value), unit)

    private fun Int.pad(): String = toString().padStart(2, '0')

    /**
     * Parse duration input into whole seconds.
     *
     * Colon input is explicit (`mm:ss` or `hh:mm:ss`). Digits-only input is grouped from the right so
     * users can type a keypad-friendly value without `:`: `3` -> 3s, `130` -> 1m30s,
     * `010000` -> 1h. Inputs above the displayed `HH:MM:SS` shape or with invalid minute/second
     * groups are rejected.
     */
    fun parseDurationSeconds(raw: String): Int? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (':' in t) {
            val parts = t.split(':')
            if (parts.size !in 2..3 || parts.any { it.isBlank() }) return null
            val nums = parts.map { it.trim().toIntOrNull() ?: return null }
            return when (nums.size) {
                2 -> {
                    val (minutes, seconds) = nums
                    if (minutes !in 0..59 || seconds !in 0..59) return null
                    minutes * 60 + seconds
                }
                else -> {
                    val (hours, minutes, seconds) = nums
                    if (hours !in 0..99 || minutes !in 0..59 || seconds !in 0..59) return null
                    hours * 3600 + minutes * 60 + seconds
                }
            }.takeIf { it >= 0 }
        }
        if (!t.all { it.isDigit() } || t.length > 6) return null
        val padded = t.padStart(6, '0')
        val hours = padded.substring(0, 2).toInt()
        val minutes = padded.substring(2, 4).toInt()
        val seconds = padded.substring(4, 6).toInt()
        if (minutes !in 0..59 || seconds !in 0..59) return null
        return hours * 3600 + minutes * 60 + seconds
    }

    /** Normalized display form for a valid duration draft, or null when the input should be left alone. */
    fun normalizeDurationInput(raw: String): String? =
        parseDurationSeconds(raw)?.let { duration(it).value }

    /** Parse a user-typed distance in the [system]'s display unit back to stored **meters**. */
    fun parseDistanceMeters(raw: String, system: DistanceUnitSystem): Double? {
        val v = raw.trim().toDoubleOrNull()?.takeIf { it >= 0.0 } ?: return null
        // Inputs use the "short" unit (m / ft) the row shows; km/mi entry is out of scope for typing.
        return when (system) {
            DistanceUnitSystem.METRIC -> v
            DistanceUnitSystem.IMPERIAL -> v / FEET_PER_METER
        }
    }

    /** The short distance unit the input row labels itself with, for the current [system]. */
    fun shortDistanceUnit(system: DistanceUnitSystem): String =
        if (system == DistanceUnitSystem.IMPERIAL) "ft" else "m"
}
