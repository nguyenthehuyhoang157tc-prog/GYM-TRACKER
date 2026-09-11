package com.hoctin.gymtracker.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogThumbnailLookupTest {

    @Test
    fun usesCanonicalNamesAndAliasesForTheFirstImage() {
        val refs = catalogThumbnailRefsByName(
            listOf(exercise(name = "Barbell Bench Press", aliases = listOf("Bench"), imageRefs = listOf("bench/0.jpg", "bench/1.jpg"))),
        )

        assertEquals("bench/0.jpg", refs.thumbnailRefFor("barbell bench press"))
        assertEquals("bench/0.jpg", refs.thumbnailRefFor(" BENCH "))
    }

    @Test
    fun skipsExercisesWithoutImagesAndKeepsTheFirstAliasMatch() {
        val refs = catalogThumbnailRefsByName(
            listOf(
                exercise(name = "Custom Source", aliases = listOf("Shared"), imageRefs = emptyList()),
                exercise(name = "First", aliases = listOf("Shared"), imageRefs = listOf("first/0.jpg")),
                exercise(name = "Second", aliases = listOf("Shared"), imageRefs = listOf("second/0.jpg")),
            ),
        )

        assertNull(refs.thumbnailRefFor("Custom Source"))
        assertEquals("first/0.jpg", refs.thumbnailRefFor("Shared"))
    }

    @Test
    fun trackingLookupUsesCanonicalNamesAndAliases() {
        val refs = catalogTrackingTypesByName(
            listOf(
                exercise(
                    name = "Plank",
                    aliases = listOf("Forearm Plank"),
                    imageRefs = emptyList(),
                    trackingType = TrackingType.TIME_ONLY,
                ),
            ),
        )

        assertEquals(TrackingType.TIME_ONLY, refs.trackingTypeFor("plank"))
        assertEquals(TrackingType.TIME_ONLY, refs.trackingTypeFor(" FOREARM PLANK "))
    }

    @Test
    fun trackingLookupKeepsFirstAliasMatch() {
        val refs = catalogTrackingTypesByName(
            listOf(
                exercise("First", aliases = listOf("Shared"), imageRefs = emptyList(), trackingType = TrackingType.TIME_ONLY),
                exercise("Second", aliases = listOf("Shared"), imageRefs = emptyList(), trackingType = TrackingType.REPS_WEIGHT),
            ),
        )

        assertEquals(TrackingType.TIME_ONLY, refs.trackingTypeFor("Shared"))
    }

    private fun exercise(
        name: String,
        aliases: List<String>,
        imageRefs: List<String>,
        trackingType: TrackingType = TrackingType.REPS_WEIGHT,
    ) = CatalogExercise(
        slug = name.lowercase().replace(' ', '-'),
        name = name,
        equipment = Equipment.BARBELL,
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = emptyList(),
        level = Level.BEGINNER,
        force = Force.PUSH,
        category = Category.STRENGTH,
        progressionType = ProgressionType.COMPOUND_LOAD,
        trackingType = trackingType,
        defaultSets = 3,
        defaultMinReps = 8,
        defaultMaxReps = 12,
        defaultIncrementKg = 2.5,
        sessionsRequiredBeforeIncrease = 2,
        isBodyweight = false,
        isUnilateral = false,
        aliases = aliases,
        imageRefs = imageRefs,
    )
}
