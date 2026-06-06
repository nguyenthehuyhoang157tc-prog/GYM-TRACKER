package com.hoctin.gymtracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A single in-progress set exactly as typed by the user (kept as raw strings, not parsed). */
data class DraftSet(
    val weight: String,
    val reps: String,
    val rir: String,
    val notes: String,
)

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
            clearDraft(dayId)
            return
        }
        preferences.edit().putString(key(dayId), serialize(entries)).apply()
    }

    /** Removes the saved draft for a day (after save or explicit discard). */
    fun clearDraft(dayId: Long) {
        preferences.edit().remove(key(dayId)).apply()
    }

    private fun serialize(entries: Map<Long, List<DraftSet>>): String {
        val exercisesJson = JSONObject()
        entries.forEach { (exerciseId, sets) ->
            val setsArray = JSONArray()
            sets.forEach { set ->
                setsArray.put(
                    JSONObject()
                        .put("w", set.weight)
                        .put("r", set.reps)
                        .put("rir", set.rir)
                        .put("n", set.notes),
                )
            }
            exercisesJson.put(exerciseId.toString(), setsArray)
        }
        return JSONObject()
            .put("v", SCHEMA_VERSION)
            .put("entries", exercisesJson)
            .toString()
    }

    private fun parse(raw: String): Map<Long, List<DraftSet>> {
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
                )
            }
            if (sets.isNotEmpty()) result[exerciseId] = sets
        }
        return result
    }

    private fun key(dayId: Long): String = "draft_$dayId"

    private companion object {
        const val PREFS_NAME = "gym_tracker_workout_drafts"
        const val SCHEMA_VERSION = 1
    }
}
