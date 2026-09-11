package com.hoctin.gymtracker.ui.home

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Presentation-only persistence for the Consistency UI. It is intentionally separate from Room,
 * GymRepository, workout history, backup/import, and Daily Boost storage.
 */
class ConsistencyPresentationStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val store = ConsistencyPresentationStateStore(
        readBest = { preferences.getInt(KEY_BEST_ACTIVE_STREAK, 0) },
        writeBest = { best ->
            preferences.edit().putInt(KEY_BEST_ACTIVE_STREAK, best).apply()
        },
        readLastCelebrated = {
            preferences.getLong(KEY_LAST_CELEBRATED_EPOCH_DAY, NO_DAY)
                .takeIf { it != NO_DAY }
        },
        writeLastCelebrated = { epochDay ->
            preferences.edit().putLong(KEY_LAST_CELEBRATED_EPOCH_DAY, epochDay).apply()
        },
    )

    val bestActiveStreak: StateFlow<Int> = store.bestActiveStreak

    fun updateBestActiveStreak(candidate: Int) {
        store.updateBestActiveStreak(candidate)
    }

    fun consumeCelebration(todayEpochDay: Long, isActiveToday: Boolean): Boolean =
        store.consumeCelebration(todayEpochDay, isActiveToday)

    private companion object {
        const val PREFERENCES_NAME = "auron_consistency_presentation"
        const val KEY_BEST_ACTIVE_STREAK = "best_active_streak"
        const val KEY_LAST_CELEBRATED_EPOCH_DAY = "last_celebrated_epoch_day"
        const val NO_DAY = Long.MIN_VALUE
    }
}

/**
 * Lambda-backed implementation keeps the once-per-day and max-only rules deterministic in JVM tests.
 */
internal class ConsistencyPresentationStateStore(
    readBest: () -> Int,
    private val writeBest: (Int) -> Unit,
    private val readLastCelebrated: () -> Long?,
    private val writeLastCelebrated: (Long) -> Unit,
) {
    private val _bestActiveStreak = MutableStateFlow(readBest().coerceAtLeast(0))
    val bestActiveStreak: StateFlow<Int> = _bestActiveStreak.asStateFlow()

    @Synchronized
    fun updateBestActiveStreak(candidate: Int) {
        val sanitized = candidate.coerceAtLeast(0)
        if (sanitized <= _bestActiveStreak.value) return
        writeBest(sanitized)
        _bestActiveStreak.value = sanitized
    }

    @Synchronized
    fun consumeCelebration(todayEpochDay: Long, isActiveToday: Boolean): Boolean {
        val shouldCelebrate = StreakCelebrationGate.shouldCelebrate(
            todayEpochDay = todayEpochDay,
            isActiveToday = isActiveToday,
            lastCelebratedEpochDay = readLastCelebrated(),
        )
        if (shouldCelebrate) writeLastCelebrated(todayEpochDay)
        return shouldCelebrate
    }
}

internal object StreakCelebrationGate {
    fun shouldCelebrate(
        todayEpochDay: Long,
        isActiveToday: Boolean,
        lastCelebratedEpochDay: Long?,
    ): Boolean = isActiveToday && todayEpochDay > 0L && lastCelebratedEpochDay != todayEpochDay
}
