package com.hoctin.gymtracker.data

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** A single in-progress set exactly as typed by the user (kept as raw strings, not parsed). */
data class DraftSet(
    val weight: String,
    val reps: String,
    val rir: String,
    val notes: String,
    // DB-6B: raw time/distance fields for future non-rep exercises. Default empty so existing call
    // sites and old drafts (schemaVersion 1, without these keys) keep working unchanged.
    val duration: String = "",
    val distance: String = "",
    /** WEARABLE-1A stable row identity. Blank means a pre-v3 draft and is upgraded by the ViewModel. */
    val researchRowKey: String = "",
)

/** Stable local identity for one Log Workout attempt; persisted independently of ViewModel/UI state. */
data class WorkoutAttemptIdentity(
    val attemptId: String,
    val createdAtMillis: Long,
    val startedAtMillis: Long,
)

internal fun serializeDraftEntries(entries: Map<Long, List<DraftSet>>): String {
    val exercisesJson = JSONObject()
    entries.forEach { (exerciseId, sets) ->
        val setsArray = JSONArray()
        sets.forEach { set ->
            setsArray.put(
                JSONObject()
                    .put("w", set.weight)
                    .put("r", set.reps)
                    .put("rir", set.rir)
                    .put("n", set.notes)
                    .put("d", set.duration)
                    .put("dist", set.distance)
                    .put("rk", set.researchRowKey),
            )
        }
        exercisesJson.put(exerciseId.toString(), setsArray)
    }
    return JSONObject()
        .put("v", WORKOUT_DRAFT_SCHEMA_VERSION)
        .put("entries", exercisesJson)
        .toString()
}

internal fun parseDraftEntries(raw: String): Map<Long, List<DraftSet>> {
    val root = JSONObject(raw)
    val entries = root.optJSONObject("entries") ?: return emptyMap()
    val result = mutableMapOf<Long, List<DraftSet>>()
    val keys = entries.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val exerciseId = key.toLongOrNull() ?: continue
        val setsArray = entries.optJSONArray(key) ?: continue
        val sets = mutableListOf<DraftSet>()
        for (i in 0 until setsArray.length()) {
            val obj = setsArray.optJSONObject(i) ?: continue
            sets += DraftSet(
                weight = obj.optString("w"),
                reps = obj.optString("r"),
                rir = obj.optString("rir"),
                notes = obj.optString("n"),
                duration = obj.optString("d"),
                distance = obj.optString("dist"),
                researchRowKey = obj.optString("rk"),
            )
        }
        if (sets.isNotEmpty()) result[exerciseId] = sets
    }
    return result
}

private const val WORKOUT_DRAFT_SCHEMA_VERSION = 3

/**
 * Persists the unsaved "workout in progress" draft for each workout day.
 *
 * Drafts are transient UI state (what the user is currently typing), not saved history, so they
 * live in SharedPreferences as JSON rather than the Room database. This avoids any schema change or
 * migration. One draft is stored per day id (key "draft_<dayId>"), so switching the selected day
 * keeps each day's draft independent. Because it is on-disk, the draft survives navigating between
 * screens, app background/foreground, and process death.
 *
 * Nothing here is written to History — only the explicit "Save workout" action does that.
 */
class WorkoutDraftRepository(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the saved draft for a day as exerciseId -> ordered sets, or empty if none. */
    fun loadDraft(dayId: Long): Map<Long, List<DraftSet>> {
        val raw = preferences.getString(key(dayId), null) ?: return emptyMap()
        return runCatching { parse(raw) }.getOrDefault(emptyMap())
    }

    /** Stores the current draft for a day. An empty draft is treated as "no draft" and cleared. */
    fun saveDraft(dayId: Long, entries: Map<Long, List<DraftSet>>) {
        if (entries.isEmpty()) {
            clearDraftPayload(dayId)
            return
        }
        preferences.edit().putString(key(dayId), serialize(entries)).apply()
    }

    /** Removes the saved draft and its terminal attempt identity after save or confirmed discard. */
    fun clearDraft(dayId: Long) {
        preferences.edit()
            .remove(key(dayId))
            .remove(startedKey(dayId))
            .remove(attemptIdKey(dayId))
            .remove(attemptCreatedKey(dayId))
            .apply()
    }

    /** Clears only typed rows; an in-flight research attempt remains durable until save/discard. */
    private fun clearDraftPayload(dayId: Long) {
        preferences.edit().remove(key(dayId)).apply()
    }

    /**
     * Returns the same attempt across recomposition, navigation, process death, and repository
     * recreation. A tiny synchronous commit ensures the identity is durable before research freeze.
     */
    fun loadOrCreateAttempt(dayId: Long, nowMillis: Long = System.currentTimeMillis()): WorkoutAttemptIdentity {
        loadAttempt(dayId)?.let { return it }
        val existingStartedAt = loadDraftStartedAt(dayId)
        val createdAt = existingStartedAt ?: nowMillis
        val startedAt = existingStartedAt ?: nowMillis
        val attempt = WorkoutAttemptIdentity(
            attemptId = UUID.randomUUID().toString(),
            createdAtMillis = createdAt,
            startedAtMillis = startedAt,
        )
        preferences.edit()
            .putString(attemptIdKey(dayId), attempt.attemptId)
            .putLong(attemptCreatedKey(dayId), attempt.createdAtMillis)
            .putLong(startedKey(dayId), attempt.startedAtMillis)
            .commit()
        return attempt
    }

    fun loadAttempt(dayId: Long): WorkoutAttemptIdentity? {
        val attemptId = preferences.getString(attemptIdKey(dayId), null)?.takeIf { it.isNotBlank() }
            ?: return null
        val startedAt = preferences.getLong(startedKey(dayId), 0L).takeIf { it > 0L } ?: return null
        val createdAt = preferences.getLong(attemptCreatedKey(dayId), 0L).takeIf { it > 0L } ?: startedAt
        return WorkoutAttemptIdentity(attemptId, createdAt, startedAt)
    }

    /**
     * Session start time (epoch millis) for an in-progress workout, used only to display the live
     * duration timer. Stored as a separate parallel key so it never touches the set-draft JSON above.
     * Returns null when no start time is recorded.
     */
    fun loadDraftStartedAt(dayId: Long): Long? {
        val value = preferences.getLong(startedKey(dayId), 0L)
        return value.takeIf { it > 0L }
    }

    /** Records the workout start time for a day (duration timer only). */
    fun saveDraftStartedAt(dayId: Long, startedAtMillis: Long) {
        preferences.edit().putLong(startedKey(dayId), startedAtMillis).apply()
    }

    private fun serialize(entries: Map<Long, List<DraftSet>>): String {
        return serializeDraftEntries(entries)
    }

    private fun parse(raw: String): Map<Long, List<DraftSet>> {
        return parseDraftEntries(raw)
    }

    private fun key(dayId: Long): String = "draft_$dayId"

    private fun startedKey(dayId: Long): String = "draft_started_$dayId"

    private fun attemptIdKey(dayId: Long): String = "research_attempt_id_$dayId"

    private fun attemptCreatedKey(dayId: Long): String = "research_attempt_created_$dayId"

    private companion object {
        const val PREFS_NAME = "gym_tracker_workout_drafts"
    }
}
