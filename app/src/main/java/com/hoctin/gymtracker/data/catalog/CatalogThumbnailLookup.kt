package com.hoctin.gymtracker.data.catalog

/**
 * Builds the exact name/alias lookup used by exercise thumbnails. The first catalog entry wins when
 * aliases collide, matching [ExerciseCatalogRepository.findByName]'s catalog-order behavior.
 */
fun catalogThumbnailRefsByName(exercises: List<CatalogExercise>): Map<String, String> {
    val refs = LinkedHashMap<String, String>()
    exercises.forEach { exercise ->
        val imageRef = exercise.imageRefs.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
        addThumbnailRef(refs, exercise.name, imageRef)
        exercise.aliases.forEach { alias -> addThumbnailRef(refs, alias, imageRef) }
    }
    return refs
}

fun Map<String, String>.thumbnailRefFor(name: String): String? = this[thumbnailLookupKey(name)]

private fun addThumbnailRef(refs: MutableMap<String, String>, name: String, imageRef: String) {
    val key = thumbnailLookupKey(name)
    if (key.isNotEmpty() && key !in refs) refs[key] = imageRef
}

private fun thumbnailLookupKey(name: String): String = name.trim().lowercase()
