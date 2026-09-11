package com.hoctin.gymtracker.data.catalog

import com.hoctin.gymtracker.data.ExerciseEntity
import com.hoctin.gymtracker.data.LoggedSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DB-PRE7B — the canonical exercise identity resolver + the key-scoped match predicate that powers
 * cross-day history. Pure JVM (no Room): the resolver and [loggedSetMatchesCanonical] mirror the SQL
 * `WHERE` clause used by the key-scoped DAO queries, so the identity semantics are verified here while the
 * SQL itself is exercised by on-device QA.
 */
class CanonicalExerciseKeyTest {

    private fun catalog(slug: String, name: String, aliases: List<String> = emptyList()) =
        CatalogExercise(
            slug = slug,
            name = name,
            equipment = Equipment.entries.first(),
            primaryMuscles = emptyList(),
            secondaryMuscles = emptyList(),
            level = Level.entries.first(),
            force = Force.entries.first(),
            category = Category.entries.first(),
            progressionType = ProgressionType.entries.first(),
            defaultSets = 3,
            defaultMinReps = 8,
            defaultMaxReps = 12,
            defaultIncrementKg = 2.5,
            sessionsRequiredBeforeIncrease = 2,
            isBodyweight = false,
            isUnilateral = false,
            aliases = aliases,
        )

    private val library = listOf(
        catalog(
            slug = "incline-machine-chest-press",
            name = "Incline Machine Chest Press",
        ),
        catalog(
            slug = "side-lateral-raise",
            name = "Side Lateral Raise",
            aliases = listOf("Dumbbell Lateral Raise", "Lateral Raise"),
        ),
    )
    private val slugs = catalogSlugsByName(library)

    private fun planExercise(id: Long, dayId: Long, name: String) =
        ExerciseEntity(id = id, dayId = dayId, name = name, targetSets = 3, targetMinReps = 8, targetMaxReps = 12, sortOrder = 0)

    private fun loggedSet(exerciseId: Long?, name: String, key: String?, weight: Double = 60.0) =
        LoggedSetEntity(
            sessionId = 1, exerciseId = exerciseId, exerciseNameSnapshot = name, setNumber = 1,
            weight = weight, reps = 10, rir = 2, isPrWeight = false, isPrReps = false,
            canonicalExerciseKey = key,
        )

    // ---- resolver ------------------------------------------------------------------------------

    @Test
    fun resolve_catalogName_yieldsCatalogSlugKey() {
        assertEquals("catalog:incline-machine-chest-press", CanonicalExerciseKey.resolve("Incline Machine Chest Press", slugs))
        // Case-insensitive / trimmed.
        assertEquals("catalog:incline-machine-chest-press", CanonicalExerciseKey.resolve("  incline machine chest press  ", slugs))
    }

    @Test
    fun resolve_catalogAlias_yieldsSameSlugKey() {
        val canonical = CanonicalExerciseKey.resolve("Side Lateral Raise", slugs)
        assertEquals("catalog:side-lateral-raise", canonical)
        // Alias resolves to the SAME catalog key.
        assertEquals(canonical, CanonicalExerciseKey.resolve("Dumbbell Lateral Raise", slugs))
        assertEquals(canonical, CanonicalExerciseKey.resolve("lateral raise", slugs))
    }

    @Test
    fun resolve_unmatched_yieldsCustomNormalizedKey() {
        assertEquals("custom:my-secret-lift", CanonicalExerciseKey.resolve("My Secret Lift", slugs))
        // Normalization is stable: case + surrounding/inner whitespace collapse to the same custom key.
        assertEquals(
            CanonicalExerciseKey.resolve("My Secret Lift", slugs),
            CanonicalExerciseKey.resolve("  MY   secret   lift ", slugs),
        )
    }

    // ---- cross-day identity (the DB-PRE7 bug) --------------------------------------------------

    @Test
    fun crossDay_sameExerciseInTwoDays_sharesHistory() {
        // Same exercise name added to two different days -> two ExerciseEntity rows, different ids.
        val push = planExercise(id = 10, dayId = 1, name = "Incline Machine Chest Press")
        val fullBody = planExercise(id = 42, dayId = 4, name = "Incline Machine Chest Press")
        val pushKey = CanonicalExerciseKey.resolve(push.name, slugs)
        val fullBodyKey = CanonicalExerciseKey.resolve(fullBody.name, slugs)
        assertEquals(pushKey, fullBodyKey) // identity is shared despite different plan-row ids

        // Sets were logged under Push (exerciseId = 10), tagged with the canonical key.
        val pushSets = listOf(
            loggedSet(exerciseId = 10, name = "Incline Machine Chest Press", key = pushKey, weight = 60.0),
            loggedSet(exerciseId = 10, name = "Incline Machine Chest Press", key = pushKey, weight = 62.5),
        )

        // Full Body's lookup (its own id 42) finds the Push sets via the shared canonical key.
        val visibleInFullBody = pushSets.filter { loggedSetMatchesCanonical(it, fullBodyKey, fullBody.name) }
        assertEquals(pushSets, visibleInFullBody)
    }

    @Test
    fun crossDay_aliasLoggedName_stillSharesHistory() {
        // A set logged under the alias name still shares the canonical (slug) identity.
        val planKey = CanonicalExerciseKey.resolve("Side Lateral Raise", slugs)
        val aliasSet = loggedSet(exerciseId = 7, name = "Dumbbell Lateral Raise", key = "catalog:side-lateral-raise")
        assertTrue(loggedSetMatchesCanonical(aliasSet, planKey, "Side Lateral Raise"))
    }

    @Test
    fun differentExercises_doNotMerge() {
        val inclineKey = CanonicalExerciseKey.resolve("Incline Machine Chest Press", slugs)
        val lateralSet = loggedSet(exerciseId = 5, name = "Side Lateral Raise", key = "catalog:side-lateral-raise")
        assertFalse(loggedSetMatchesCanonical(lateralSet, inclineKey, "Incline Machine Chest Press"))
        // Two custom names that are genuinely different must not collide.
        assertFalse(
            loggedSetMatchesCanonical(
                loggedSet(exerciseId = 9, name = "My Curl A", key = "custom:my-curl-a"),
                CanonicalExerciseKey.resolve("My Curl B", slugs),
                "My Curl B",
            ),
        )
    }

    @Test
    fun nullKeyRow_matchesByNormalizedNameFallback() {
        // Not-yet-backfilled row (null key) still matches the canonical lookup by normalized name.
        val legacy = loggedSet(exerciseId = 10, name = "Incline Machine Chest Press", key = null)
        val key = CanonicalExerciseKey.resolve("Incline Machine Chest Press", slugs)
        assertTrue(loggedSetMatchesCanonical(legacy, key, "Incline Machine Chest Press"))
        // But a null-key row with a different name does not match.
        val other = loggedSet(exerciseId = 11, name = "Barbell Bench Press", key = null)
        assertFalse(loggedSetMatchesCanonical(other, key, "Incline Machine Chest Press"))
    }
}
