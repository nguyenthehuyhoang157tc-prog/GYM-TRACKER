package com.hoctin.gymtracker.data.catalog

/**
 * Exact name/alias lookup for catalog tracking types. The first catalog entry wins when aliases
 * collide, matching [ExerciseCatalogRepository.findByName].
 */
fun catalogTrackingTypesByName(exercises: List<CatalogExercise>): Map<String, TrackingType> {
    val refs = LinkedHashMap<String, TrackingType>()
    exercises.forEach { exercise ->
        addTrackingType(refs, exercise.name, exercise.trackingType)
        exercise.aliases.forEach { alias -> addTrackingType(refs, alias, exercise.trackingType) }
    }
    return refs
}

fun Map<String, TrackingType>.trackingTypeFor(name: String): TrackingType? = this[trackingLookupKey(name)]

private fun addTrackingType(refs: MutableMap<String, TrackingType>, name: String, trackingType: TrackingType) {
    val key = trackingLookupKey(name)
    if (key.isNotEmpty() && key !in refs) refs[key] = trackingType
}

private fun trackingLookupKey(name: String): String = name.trim().lowercase()
