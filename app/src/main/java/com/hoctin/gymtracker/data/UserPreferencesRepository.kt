package com.hoctin.gymtracker.data

import android.content.Context
import com.hoctin.gymtracker.domain.WorkoutGoalSelection
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

const val LATEST_ONBOARDING_VERSION = 3

/** Versioned, presentation-only onboarding draft. Production preferences are committed separately. */
data class OnboardingDraftPreferences(
    val version: Int = LATEST_ONBOARDING_VERSION,
    val step: String,
    val profileJson: String,
    val editReturnAfter: String? = null,
)

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

/**
 * DB-6C: display unit system for distances (and future velocity/pace). The database always stores SI
 * (meters, m/s); only rendering converts. Defaults to METRIC. Designed so adding more unit families
 * later does not require new preference plumbing.
 */
enum class DistanceUnitSystem(val storageValue: String) {
    METRIC("metric"),
    IMPERIAL("imperial");

    companion object {
        fun fromStorageValue(value: String?): DistanceUnitSystem =
            entries.firstOrNull { it.storageValue == value } ?: METRIC
    }
}

/**
 * Why a workout day is currently selected on Home. Drives the hero/section label wording:
 * AUTO = suggested for today, MANUAL = user picked/skipped, ADVANCED = auto-advanced after a save.
 */
enum class DaySelectionOrigin(val storageValue: String) {
    AUTO("auto"),
    MANUAL("manual"),
    ADVANCED("advanced");

    companion object {
        fun fromStorageValue(value: String?): DaySelectionOrigin {
            return entries.firstOrNull { it.storageValue == value } ?: AUTO
        }
    }
}

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("gym_tracker_preferences", Context.MODE_PRIVATE)
    private val workoutGoalStore = WorkoutGoalPreferenceStore(
        read = { preferences.getString(KEY_WORKOUT_GOAL, null) },
        write = { value -> preferences.edit().putString(KEY_WORKOUT_GOAL, value).apply() },
    )

    /** V3.11C user-global coaching intent. Missing/legacy/invalid storage resolves to AUTO. */
    val workoutGoal: StateFlow<WorkoutGoalSelection> = workoutGoalStore.selection

    private val _themeMode = MutableStateFlow(
        ThemeMode.fromStorageValue(preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.storageValue)),
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _distanceUnitSystem = MutableStateFlow(
        DistanceUnitSystem.fromStorageValue(preferences.getString(KEY_DISTANCE_UNIT, null)),
    )

    /** DB-6C: how distances (and future velocity) are displayed. DB stays in meters; display converts. */
    val distanceUnitSystem: StateFlow<DistanceUnitSystem> = _distanceUnitSystem.asStateFlow()

    private val _selectedDayId = MutableStateFlow(
        preferences.getLong(KEY_SELECTED_DAY_ID, NO_DAY_SELECTED).takeIf { it != NO_DAY_SELECTED },
    )

    /** The workout day the user manually picked on Home, or null to fall back to the auto pick. */
    val selectedDayId: StateFlow<Long?> = _selectedDayId.asStateFlow()

    private val _selectionOrigin = MutableStateFlow(
        DaySelectionOrigin.fromStorageValue(preferences.getString(KEY_SELECTION_ORIGIN, null)),
    )

    /** Why [selectedDayId] is set (manual pick/skip vs. auto-advanced after completing a workout). */
    val selectionOrigin: StateFlow<DaySelectionOrigin> = _selectionOrigin.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(
        preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false),
    )

    /** True once the user has finished first-launch onboarding (picking training weekdays). */
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _onboardingCompletionVersion = MutableStateFlow(
        when {
            preferences.contains(KEY_ONBOARDING_COMPLETION_VERSION) ->
                preferences.getInt(KEY_ONBOARDING_COMPLETION_VERSION, 0)
            preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false) -> 1
            else -> 0
        },
    )

    /** Completed content version. Legacy completed installs resolve to v1 and are never forced into V3. */
    val onboardingCompletionVersion: StateFlow<Int> = _onboardingCompletionVersion.asStateFlow()

    private val _onboardingDraft = MutableStateFlow(loadOnboardingDraft())

    /** Resumable V3 profile state. It is intentionally separate from production goal/weekdays. */
    val onboardingDraft: StateFlow<OnboardingDraftPreferences?> = _onboardingDraft.asStateFlow()

    private val _onboardingReceiptPending = MutableStateFlow(
        preferences.getBoolean(KEY_ONBOARDING_RECEIPT_PENDING, false),
    )

    /** True after an atomic onboarding commit until the user leaves the final setup receipt. */
    val onboardingReceiptPending: StateFlow<Boolean> = _onboardingReceiptPending.asStateFlow()

    private val _trainingProfileJson = MutableStateFlow(
        preferences.getString(KEY_TRAINING_PROFILE_JSON, null),
    )

    /** Stable local V3 profile document. It is never sent to a network by this repository. */
    val trainingProfileJson: StateFlow<String?> = _trainingProfileJson.asStateFlow()

    private val _trainingWeekdays = MutableStateFlow(
        decodeWeekdays(preferences.getString(KEY_TRAINING_WEEKDAYS, null)),
    )

    /**
     * Weekdays the user trains on, as ISO-8601 day-of-week numbers (Monday = 1 … Sunday = 7).
     * Stored as a sorted CSV so the format stays clear and is not tied to a fixed day count.
     */
    val trainingWeekdays: StateFlow<Set<Int>> = _trainingWeekdays.asStateFlow()

    private val _trackingStartEpochDay = MutableStateFlow(
        preferences.getLong(KEY_TRACKING_START_EPOCH_DAY, NOT_SET).takeIf { it != NOT_SET },
    )

    /**
     * The epoch day on which the user started tracking (set when onboarding completes). Used so the
     * Weekly Progress calendar never marks days *before* the user began as "missed". Null on old
     * installs that completed onboarding before this was introduced — callers fall back to the
     * earliest saved session date, then today.
     */
    val trackingStartEpochDay: StateFlow<Long?> = _trackingStartEpochDay.asStateFlow()

    // ---------------------------------------------------------------------------
    // Daily Boost (optional ~2-minute rest-day habit). Stored here, in plain
    // SharedPreferences, deliberately separate from workout sessions/history, the
    // Weekly Progress ring, and the workout Day streak — completing a boost must
    // never look like (or interfere with) completing a training day.
    // ---------------------------------------------------------------------------

    private val _dailyBoostExerciseKey = MutableStateFlow(
        preferences.getString(KEY_DAILY_BOOST_EXERCISE, null),
    )

    /** Stable key of the chosen Daily Boost exercise, or null to fall back to the catalog default. */
    val dailyBoostExerciseKey: StateFlow<String?> = _dailyBoostExerciseKey.asStateFlow()

    private val _dailyBoostCompletionDays = MutableStateFlow(loadDailyBoostCompletionDays())

    /**
     * The set of epoch days on which a Daily Boost was completed, kept to a recent window. This is the
     * single source of truth for "completed today" **and** for the boost's contribution to the Home
     * activity streak (a day counts as active if it has a workout OR a boost). It deliberately never
     * flows into Weekly Progress or History — those stay workout-only.
     */
    val dailyBoostCompletionDays: StateFlow<Set<Long>> = _dailyBoostCompletionDays.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.storageValue).apply()
        _themeMode.value = mode
    }

    fun setDistanceUnitSystem(system: DistanceUnitSystem) {
        preferences.edit().putString(KEY_DISTANCE_UNIT, system.storageValue).apply()
        _distanceUnitSystem.value = system
    }

    fun setWorkoutGoal(selection: WorkoutGoalSelection) {
        workoutGoalStore.set(selection)
    }

    /** Save a resumable V3 draft without changing production goal, schedule, plan or history. */
    fun saveOnboardingDraft(
        step: String,
        profileJson: String,
        editReturnAfter: String? = null,
    ) {
        preferences.edit()
            .putInt(KEY_ONBOARDING_DRAFT_VERSION, LATEST_ONBOARDING_VERSION)
            .putString(KEY_ONBOARDING_DRAFT_STEP, step)
            .putString(KEY_ONBOARDING_DRAFT_PROFILE_JSON, profileJson)
            .putString(KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER, editReturnAfter)
            .apply()
        _onboardingDraft.value = OnboardingDraftPreferences(
            step = step,
            profileJson = profileJson,
            editReturnAfter = editReturnAfter,
        )
    }

    /** Clear only an unfinished draft. Production choices, tracking start and completion stay intact. */
    fun resetOnboardingDraft() {
        preferences.edit()
            .remove(KEY_ONBOARDING_DRAFT_VERSION)
            .remove(KEY_ONBOARDING_DRAFT_STEP)
            .remove(KEY_ONBOARDING_DRAFT_PROFILE_JSON)
            .remove(KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER)
            .apply()
        _onboardingDraft.value = null
    }

    /**
     * Atomically publish the only two profile values consumed today plus the complete local profile.
     * A failed disk commit leaves production flows unchanged and retains the resumable draft.
     */
    suspend fun completeOnboardingV3(
        goal: WorkoutGoalSelection,
        weekdays: Set<Int>,
        profileJson: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitized = weekdays.filter { it in 1..7 }.toSortedSet()
            require(sanitized.isNotEmpty()) { "At least one training weekday is required." }
            require(profileJson.isNotBlank()) { "A local training profile is required." }

            // Never move an existing tracking boundary forward during onboarding or later setup edits.
            val trackingStart = _trackingStartEpochDay.value ?: LocalDate.now().toEpochDay()
            val committed = preferences.edit()
                .putString(KEY_WORKOUT_GOAL, goal.storageValue)
                .putString(KEY_TRAINING_WEEKDAYS, encodeWeekdays(sanitized))
                .putBoolean(KEY_ONBOARDING_COMPLETED, true)
                .putInt(KEY_ONBOARDING_COMPLETION_VERSION, LATEST_ONBOARDING_VERSION)
                .putLong(KEY_TRACKING_START_EPOCH_DAY, trackingStart)
                .putBoolean(KEY_ONBOARDING_RECEIPT_PENDING, true)
                .putString(KEY_TRAINING_PROFILE_JSON, profileJson)
                .putInt(KEY_ONBOARDING_DRAFT_VERSION, LATEST_ONBOARDING_VERSION)
                .putString(KEY_ONBOARDING_DRAFT_STEP, ONBOARDING_RECEIPT_STEP)
                .putString(KEY_ONBOARDING_DRAFT_PROFILE_JSON, profileJson)
                .remove(KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER)
                .commit()
            check(committed) { "SharedPreferences rejected the onboarding commit." }

            workoutGoalStore.publishCommitted(goal)
            _trainingWeekdays.value = sanitized
            _onboardingCompleted.value = true
            _onboardingCompletionVersion.value = LATEST_ONBOARDING_VERSION
            _trackingStartEpochDay.value = trackingStart
            _onboardingReceiptPending.value = true
            _trainingProfileJson.value = profileJson
            _onboardingDraft.value = OnboardingDraftPreferences(
                step = ONBOARDING_RECEIPT_STEP,
                profileJson = profileJson,
            )
        }
    }

    /** Clear the pending receipt and its draft only after a destination has been chosen successfully. */
    suspend fun finishOnboardingReceipt(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val committed = preferences.edit()
                .putBoolean(KEY_ONBOARDING_RECEIPT_PENDING, false)
                .remove(KEY_ONBOARDING_DRAFT_VERSION)
                .remove(KEY_ONBOARDING_DRAFT_STEP)
                .remove(KEY_ONBOARDING_DRAFT_PROFILE_JSON)
                .remove(KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER)
                .commit()
            check(committed) { "SharedPreferences rejected the receipt completion." }
            _onboardingReceiptPending.value = false
            _onboardingDraft.value = null
        }
    }

    fun setSelectedDayId(dayId: Long?, origin: DaySelectionOrigin = DaySelectionOrigin.MANUAL) {
        val resolvedOrigin = if (dayId == null) DaySelectionOrigin.AUTO else origin
        preferences.edit()
            .putLong(KEY_SELECTED_DAY_ID, dayId ?: NO_DAY_SELECTED)
            .putString(KEY_SELECTION_ORIGIN, resolvedOrigin.storageValue)
            .apply()
        _selectedDayId.value = dayId
        _selectionOrigin.value = resolvedOrigin
    }

    /** Update the training weekdays (ISO 1..7) without changing onboarding status. */
    fun setTrainingWeekdays(weekdays: Set<Int>) {
        val sanitized = weekdays.filter { it in 1..7 }.toSortedSet()
        preferences.edit().putString(KEY_TRAINING_WEEKDAYS, encodeWeekdays(sanitized)).apply()
        _trainingWeekdays.value = sanitized
    }

    /** Persist the chosen training weekdays and mark onboarding complete in one step. */
    fun completeOnboarding(weekdays: Set<Int>) {
        val sanitized = weekdays.filter { it in 1..7 }.toSortedSet()
        // Record when tracking began so the calendar never shows "missed" days before this date.
        // Preserve any existing start date so a re-run never moves it forward.
        val trackingStart = _trackingStartEpochDay.value ?: LocalDate.now().toEpochDay()
        preferences.edit()
            .putString(KEY_TRAINING_WEEKDAYS, encodeWeekdays(sanitized))
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putLong(KEY_TRACKING_START_EPOCH_DAY, trackingStart)
            .apply()
        _trainingWeekdays.value = sanitized
        _onboardingCompleted.value = true
        _trackingStartEpochDay.value = trackingStart
    }

    /** Persist a user-picked Daily Boost exercise by its stable [key]. Independent of workout selection. */
    fun setDailyBoostExercise(key: String) {
        preferences.edit().putString(KEY_DAILY_BOOST_EXERCISE, key).apply()
        _dailyBoostExerciseKey.value = key
    }

    /**
     * Record that a Daily Boost was completed on [today] (idempotent within a day). Only adds the day to
     * the completion set, pruned to a recent window so the stored CSV stays small. The streak itself is
     * derived at read time in the ViewModel by unioning these days with workout days — there is no
     * separate stored boost streak. This touches Daily Boost state only — never workout history, the
     * workout sessions, or the Weekly Progress ring.
     */
    fun markDailyBoostCompleted(today: Long = LocalDate.now().toEpochDay()) {
        if (today in _dailyBoostCompletionDays.value) return // already counted today
        val updated = (_dailyBoostCompletionDays.value + today)
            .filter { it >= today - DAILY_BOOST_RETENTION_DAYS }
            .toSortedSet()
        preferences.edit().putString(KEY_DAILY_BOOST_COMPLETION_DAYS, encodeEpochDays(updated)).apply()
        _dailyBoostCompletionDays.value = updated
    }

    /**
     * Load the boost completion-day set, migrating the legacy single-`lastCompletion` + `streak` model
     * (which was always a contiguous run) into the set on first read so existing users keep their streak.
     */
    private fun loadDailyBoostCompletionDays(): Set<Long> {
        preferences.getString(KEY_DAILY_BOOST_COMPLETION_DAYS, null)?.let { return decodeEpochDays(it) }
        val last = preferences.getLong(KEY_DAILY_BOOST_LAST_COMPLETION, NOT_SET).takeIf { it != NOT_SET }
            ?: return emptySet()
        val streak = preferences.getInt(KEY_DAILY_BOOST_STREAK, 1).coerceAtLeast(1)
        val migrated = (0 until streak).map { last - it }.toSortedSet()
        preferences.edit().putString(KEY_DAILY_BOOST_COMPLETION_DAYS, encodeEpochDays(migrated)).apply()
        return migrated
    }

    private fun encodeEpochDays(days: Set<Long>): String = days.sorted().joinToString(separator = ",")

    private fun decodeEpochDays(raw: String?): Set<Long> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSortedSet()
    }

    private fun encodeWeekdays(weekdays: Set<Int>): String =
        weekdays.sorted().joinToString(separator = ",")

    private fun decodeWeekdays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSortedSet()
    }

    private fun loadOnboardingDraft(): OnboardingDraftPreferences? {
        val version = preferences.getInt(KEY_ONBOARDING_DRAFT_VERSION, 0)
        if (version != LATEST_ONBOARDING_VERSION) return null
        val step = preferences.getString(KEY_ONBOARDING_DRAFT_STEP, null)?.takeIf { it.isNotBlank() }
            ?: return null
        return OnboardingDraftPreferences(
            version = version,
            step = step,
            profileJson = preferences.getString(KEY_ONBOARDING_DRAFT_PROFILE_JSON, null)
                ?.takeIf { it.isNotBlank() }
                ?: return null,
            editReturnAfter = preferences.getString(KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER, null),
        )
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DISTANCE_UNIT = "distance_unit_system"
        const val KEY_WORKOUT_GOAL = "workout_goal"
        const val KEY_SELECTED_DAY_ID = "selected_day_id"
        const val KEY_SELECTION_ORIGIN = "selection_origin"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_ONBOARDING_COMPLETION_VERSION = "onboarding_completion_version"
        const val KEY_ONBOARDING_DRAFT_VERSION = "onboarding_draft_version"
        const val KEY_ONBOARDING_DRAFT_STEP = "onboarding_draft_step"
        const val KEY_ONBOARDING_DRAFT_PROFILE_JSON = "onboarding_draft_profile_json"
        const val KEY_ONBOARDING_DRAFT_EDIT_RETURN_AFTER = "onboarding_draft_edit_return_after"
        const val KEY_ONBOARDING_RECEIPT_PENDING = "onboarding_receipt_pending"
        const val KEY_TRAINING_PROFILE_JSON = "training_profile_json"
        const val ONBOARDING_RECEIPT_STEP = "receipt"
        const val KEY_TRAINING_WEEKDAYS = "training_weekdays"
        const val KEY_TRACKING_START_EPOCH_DAY = "tracking_start_epoch_day"
        const val KEY_DAILY_BOOST_EXERCISE = "daily_boost_exercise"
        const val KEY_DAILY_BOOST_COMPLETION_DAYS = "daily_boost_completion_days"
        // Legacy keys (pre-activity-streak): still read once to migrate into the completion-day set.
        const val KEY_DAILY_BOOST_LAST_COMPLETION = "daily_boost_last_completion_epoch_day"
        const val KEY_DAILY_BOOST_STREAK = "daily_boost_streak"
        const val NO_DAY_SELECTED = -1L
        const val NOT_SET = Long.MIN_VALUE
        // Keep ~14 months of boost days so the stored CSV stays tiny but any realistic streak survives.
        const val DAILY_BOOST_RETENTION_DAYS = 430L
    }
}

/** Small testable persistence boundary; production supplies SharedPreferences read/write lambdas. */
internal class WorkoutGoalPreferenceStore(
    private val read: () -> String?,
    private val write: (String) -> Unit,
) {
    private val _selection = MutableStateFlow(WorkoutGoalSelection.fromStorageValue(read()))
    val selection: StateFlow<WorkoutGoalSelection> = _selection.asStateFlow()

    fun set(selection: WorkoutGoalSelection) {
        write(selection.storageValue)
        _selection.value = selection
    }

    /** Publish a value already persisted by an enclosing atomic editor without writing it twice. */
    fun publishCommitted(selection: WorkoutGoalSelection) {
        _selection.value = selection
    }
}
