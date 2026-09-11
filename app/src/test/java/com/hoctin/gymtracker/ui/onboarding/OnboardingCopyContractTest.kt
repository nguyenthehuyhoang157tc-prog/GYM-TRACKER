package com.hoctin.gymtracker.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingCopyContractTest {
    private val productionCopy = buildString {
        append(WELCOME_SUPPORT_COPY)
        append(SCHEDULE_SCOPE_COPY)
        append(REVIEW_TRUTH_COPY)
        append(INJURY_DISCLAIMER_COPY)
        append(TARGET_BMI_WARNING_COPY)
        append(RECEIPT_BOUNDARY_COPY)
    }.lowercase()

    @Test
    fun `copy never claims fake AI generation medical clearance or predicted results`() {
        listOf(
            "ai-generated",
            "ai generated",
            "we generated your plan",
            "your personalized plan",
            "your personalised plan",
            "safe to train",
            "injury-free",
            "body-fat estimate",
            "recommended weight",
            "guaranteed results",
            "predicted results for you",
        ).forEach { forbidden ->
            assertFalse(forbidden, productionCopy.contains(forbidden))
        }
    }

    @Test
    fun `copy states every current product boundary`() {
        assertTrue(productionCopy.contains("plan already in auron"))
        assertTrue(productionCopy.contains("only your coaching goal and current training weekdays affect auron today"))
        assertTrue(productionCopy.contains("future, separately validated plan generator"))
        assertTrue(productionCopy.contains("daily boost never counts"))
        assertTrue(productionCopy.contains("distinct routine workout dates"))
        assertTrue(productionCopy.contains("saved one-off workouts"))
        assertTrue(productionCopy.contains("current split is unchanged"))
        assertTrue(productionCopy.contains("not assigned to specific weekdays"))
        assertFalse(productionCopy.contains("active today"))
    }

    @Test
    fun `sensitive and evidence copy stays within an honest scope`() {
        assertTrue(productionCopy.contains("not a diagnosis, treatment plan, or clearance to train"))
        assertTrue(productionCopy.contains("screening value, not a diagnosis"))
        assertTrue(productionCopy.contains("below 18.5"))
        assertFalse(productionCopy.contains("ideal weight"))
    }
}
