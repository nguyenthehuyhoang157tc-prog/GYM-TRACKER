package com.hoctin.gymtracker.data.catalog

import org.json.JSONArray
import org.json.JSONObject

/**
 * DB-4B.2: loads the FED-canonical catalog from the bundled JSON asset
 * `assets/catalog/catalog_fed_v1.json` (free-exercise-db, public domain) into [CatalogExercise]s.
 *
 * Pure function over a JSON string so it is unit-testable without Android (the app passes the asset
 * text; tests read the asset file). Enum fields decode via the taxonomy `fromName` helpers, so an
 * unknown/future value degrades to a safe fallback instead of crashing the seed.
 */
object CatalogJsonLoader {
    fun parse(json: String): List<CatalogExercise> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i -> arr.getJSONObject(i).toCatalogExercise() }
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val a = optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }

    private fun JSONObject.toCatalogExercise(): CatalogExercise = CatalogExercise(
        slug = getString("slug"),
        name = getString("name"),
        equipment = Equipment.fromName(getString("equipment")),
        primaryMuscles = stringList("primaryMuscles").mapNotNull { MuscleGroup.fromName(it) },
        secondaryMuscles = stringList("secondaryMuscles").mapNotNull { MuscleGroup.fromName(it) },
        level = Level.fromName(getString("level")),
        force = Force.fromName(getString("force")),
        category = Category.fromName(getString("category")),
        progressionType = ProgressionType.fromName(getString("progressionType")),
        trackingType = TrackingType.fromName(optString("trackingType", null)),
        defaultSets = getInt("defaultSets"),
        defaultMinReps = getInt("defaultMinReps"),
        defaultMaxReps = getInt("defaultMaxReps"),
        defaultIncrementKg = getDouble("defaultIncrementKg"),
        sessionsRequiredBeforeIncrease = getInt("sessionsRequiredBeforeIncrease"),
        isBodyweight = getBoolean("isBodyweight"),
        isUnilateral = getBoolean("isUnilateral"),
        aliases = stringList("aliases"),
        instructions = stringList("instructions"),
        tips = stringList("tips"),
        imageRefs = stringList("imageRefs"),
    )
}
