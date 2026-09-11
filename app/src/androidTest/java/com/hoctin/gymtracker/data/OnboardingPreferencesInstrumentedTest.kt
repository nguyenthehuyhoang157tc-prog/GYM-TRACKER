package com.hoctin.gymtracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingPreferencesInstrumentedTest {
    private lateinit var context: Context

    @Before
    fun clearPreferences() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun cleanUp() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun draftResumesWithoutPublishingProductionValues() {
        val firstProcess = UserPreferencesRepository(context)
        firstProcess.saveOnboardingDraft(
            step = "schedule",
            profileJson = PROFILE_JSON,
            editReturnAfter = "review",
        )

        assertFalse(firstProcess.onboardingCompleted.value)
        assertEquals(WorkoutGoalSelection.AUTO, firstProcess.workoutGoal.value)
        assertTrue(firstProcess.trainingWeekdays.value.isEmpty())
        assertNull(firstProcess.trainingProfileJson.value)

        val reloaded = UserPreferencesRepository(context)
        assertEquals("schedule", reloaded.onboardingDraft.value?.step)
        assertEquals(PROFILE_JSON, reloaded.onboardingDraft.value?.profileJson)
        assertEquals("review", reloaded.onboardingDraft.value?.editReturnAfter)
        assertFalse(reloaded.onboardingCompleted.value)
    }

    @Test
    fun v3CommitPublishesGoalWeekdaysProfileVersionAndReceiptTogether() = runBlocking {
        val repository = UserPreferencesRepository(context)
        repository.saveOnboardingDraft(step = "review", profileJson = PROFILE_JSON)

        val result = repository.completeOnboardingV3(
            goal = WorkoutGoalSelection.STRENGTH,
            weekdays = setOf(1, 2, 4, 6),
            profileJson = PROFILE_JSON,
        )

        assertTrue(result.isSuccess)
        assertTrue(repository.onboardingCompleted.value)
        assertEquals(LATEST_ONBOARDING_VERSION, repository.onboardingCompletionVersion.value)
        assertEquals(WorkoutGoalSelection.STRENGTH, repository.workoutGoal.value)
        assertEquals(setOf(1, 2, 4, 6), repository.trainingWeekdays.value)
        assertEquals(PROFILE_JSON, repository.trainingProfileJson.value)
        assertTrue(repository.trackingStartEpochDay.value != null)
        assertTrue(repository.onboardingReceiptPending.value)
        assertEquals("receipt", repository.onboardingDraft.value?.step)

        val reloaded = UserPreferencesRepository(context)
        assertTrue(reloaded.onboardingCompleted.value)
        assertEquals(LATEST_ONBOARDING_VERSION, reloaded.onboardingCompletionVersion.value)
        assertEquals(WorkoutGoalSelection.STRENGTH, reloaded.workoutGoal.value)
        assertEquals(PROFILE_JSON, reloaded.trainingProfileJson.value)
        assertTrue(reloaded.onboardingReceiptPending.value)
    }

    @Test
    fun rejectedV3CommitDoesNotPartiallyPublishProductionValues() = runBlocking {
        val repository = UserPreferencesRepository(context)
        repository.saveOnboardingDraft(step = "review", profileJson = PROFILE_JSON)

        val result = repository.completeOnboardingV3(
            goal = WorkoutGoalSelection.POWER,
            weekdays = emptySet(),
            profileJson = PROFILE_JSON,
        )

        assertTrue(result.isFailure)
        assertFalse(repository.onboardingCompleted.value)
        assertEquals(WorkoutGoalSelection.AUTO, repository.workoutGoal.value)
        assertTrue(repository.trainingWeekdays.value.isEmpty())
        assertNull(repository.trainingProfileJson.value)
        assertEquals("review", repository.onboardingDraft.value?.step)
    }

    @Test
    fun receiptFinishClearsOnlyDraftAndPendingState() = runBlocking {
        val repository = UserPreferencesRepository(context)
        repository.saveOnboardingDraft("review", PROFILE_JSON)
        repository.completeOnboardingV3(
            WorkoutGoalSelection.HYPERTROPHY,
            setOf(1, 3, 5, 7),
            PROFILE_JSON,
        ).getOrThrow()
        val trackingStart = repository.trackingStartEpochDay.value

        assertTrue(repository.finishOnboardingReceipt().isSuccess)
        assertFalse(repository.onboardingReceiptPending.value)
        assertNull(repository.onboardingDraft.value)
        assertTrue(repository.onboardingCompleted.value)
        assertEquals(WorkoutGoalSelection.HYPERTROPHY, repository.workoutGoal.value)
        assertEquals(setOf(1, 3, 5, 7), repository.trainingWeekdays.value)
        assertEquals(PROFILE_JSON, repository.trainingProfileJson.value)
        assertEquals(trackingStart, repository.trackingStartEpochDay.value)
    }

    @Test
    fun resetClearsDraftOnlyAndNeverClearsCompletedProductionState() = runBlocking {
        val repository = UserPreferencesRepository(context)
        repository.completeOnboardingV3(
            WorkoutGoalSelection.GENERAL_FITNESS,
            setOf(2, 4, 6),
            PROFILE_JSON,
        ).getOrThrow()

        repository.resetOnboardingDraft()

        assertNull(repository.onboardingDraft.value)
        assertTrue(repository.onboardingCompleted.value)
        assertEquals(WorkoutGoalSelection.GENERAL_FITNESS, repository.workoutGoal.value)
        assertEquals(setOf(2, 4, 6), repository.trainingWeekdays.value)
        assertEquals(PROFILE_JSON, repository.trainingProfileJson.value)
    }

    @Test
    fun legacyCompletedInstallResolvesToV1AndIsNeverMarkedIncomplete() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .putString("training_weekdays", "1,3,5,7")
            .commit()

        val repository = UserPreferencesRepository(context)
        assertTrue(repository.onboardingCompleted.value)
        assertEquals(1, repository.onboardingCompletionVersion.value)
        assertEquals(setOf(1, 3, 5, 7), repository.trainingWeekdays.value)
        assertFalse(repository.onboardingReceiptPending.value)
        assertNull(repository.trainingProfileJson.value)
    }

    @Test
    fun reconfigurationNeverMovesTrackingStartForward() = runBlocking {
        val repository = UserPreferencesRepository(context)
        repository.completeOnboarding(setOf(1, 2, 4, 6))
        val originalStart = repository.trackingStartEpochDay.value

        repository.completeOnboardingV3(
            WorkoutGoalSelection.AUTO,
            setOf(1, 3, 5, 7),
            PROFILE_JSON,
        ).getOrThrow()

        assertEquals(originalStart, repository.trackingStartEpochDay.value)
    }

    private companion object {
        const val PREFERENCES_NAME = "gym_tracker_preferences"
        const val PROFILE_JSON = "{\"schemaVersion\":1,\"profile\":{\"displayName\":\"AURON\"}}"
    }
}
