package com.hoctin.gymtracker.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingRulerMathTest {
    @Test
    fun `days ruler exposes every integer from one through seven`() {
        val range = 1.0..7.0

        assertEquals(7, OnboardingRulerMath.itemCount(range, 1.0))
        assertEquals(1.0, OnboardingRulerMath.valueFor(0, range, 1.0), 0.0)
        assertEquals(7.0, OnboardingRulerMath.valueFor(6, range, 1.0), 0.0)
        assertEquals(3, OnboardingRulerMath.indexFor(4.0, range, 1.0))
    }

    @Test
    fun `weight ruler snaps to one tenth kilogram and clamps its bounds`() {
        val range = 35.0..250.0

        assertEquals(57.0, OnboardingRulerMath.snap(57.04, range, 0.1), 0.0001)
        assertEquals(57.1, OnboardingRulerMath.snap(57.06, range, 0.1), 0.0001)
        assertEquals(35.0, OnboardingRulerMath.snap(10.0, range, 0.1), 0.0001)
        assertEquals(250.0, OnboardingRulerMath.snap(400.0, range, 0.1), 0.0001)
    }

    @Test
    fun `index and canonical value remain stable across repeated conversion`() {
        val range = 35.0..250.0
        val canonicalKg = OnboardingRulerMath.snap(81.6466, range, 0.1)
        val pounds = MeasurementConversion.kilogramsToPounds(canonicalKg)
        val roundTripKg = MeasurementConversion.poundsToKilograms(pounds)
        val index = OnboardingRulerMath.indexFor(roundTripKg, range, 0.1)

        assertEquals(canonicalKg, OnboardingRulerMath.valueFor(index, range, 0.1), 0.0001)
    }
}
