package com.hoctin.gymtracker.ui.onboarding

import kotlin.math.roundToInt

/** Pure canonical-value math shared by the three measurement rulers and the weekly-frequency ruler. */
internal object OnboardingRulerMath {
    fun itemCount(
        range: ClosedFloatingPointRange<Double>,
        step: Double,
    ): Int {
        require(step > 0.0 && step.isFinite())
        require(range.start.isFinite() && range.endInclusive.isFinite())
        require(range.endInclusive >= range.start)
        return (((range.endInclusive - range.start) / step).roundToInt() + 1).coerceAtLeast(1)
    }

    fun indexFor(
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
    ): Int = (((value.coerceIn(range.start, range.endInclusive) - range.start) / step).roundToInt())
        .coerceIn(0, itemCount(range, step) - 1)

    fun valueFor(
        index: Int,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
    ): Double {
        val boundedIndex = index.coerceIn(0, itemCount(range, step) - 1)
        return MeasurementConversion.round(
            (range.start + boundedIndex * step).coerceIn(range.start, range.endInclusive),
            decimalsFor(step),
        )
    }

    fun snap(
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        step: Double,
    ): Double = valueFor(indexFor(value, range, step), range, step)

    private fun decimalsFor(step: Double): Int = when {
        step >= 1.0 -> 2
        step >= 0.1 -> 3
        else -> 4
    }
}
