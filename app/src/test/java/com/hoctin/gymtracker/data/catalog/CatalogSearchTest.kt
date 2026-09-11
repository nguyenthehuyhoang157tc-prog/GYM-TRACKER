package com.hoctin.gymtracker.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Deterministic, pure-JVM verification of the FED-canonical catalog (DB-4B.2) and the search/filter
 * foundation. The catalog is now the single source of truth from free-exercise-db (public domain),
 * bundled as `assets/catalog/catalog_fed_v1.json` and loaded by [CatalogJsonLoader] — so this test
 * loads the same asset the app ships and asserts durable, structural invariants:
 *  - the current FED-backed catalog count, unique slugs, instructions + primary muscle present,
 *  - every entry has exactly two bundled image refs that resolve to real asset files,
 *  - the `exercise_images/` folders are a 1:1 match for the catalog slugs (no missing, no orphans),
 *  - search / filters behave correctly (property-based, not magic counts),
 *  - the entity mapping round-trips every field,
 *  - the active-Plan rename migration map (the 63 verified SAFE pairs) is collision-free and idempotent.
 */
class CatalogSearchTest {

    private val assetsDir: File = run {
        listOf("src/main/assets", "app/src/main/assets", "../app/src/main/assets")
            .map { File(it) }
            .firstOrNull { it.isDirectory }
            ?: error("assets dir not found from ${File(".").absolutePath}")
    }

    private val all: List<CatalogExercise> =
        CatalogJsonLoader.parse(File(assetsDir, "catalog/catalog_fed_v1.json").readText())

    private fun names(items: List<CatalogExercise>) = items.map { it.name }.toSet()

    // ---- Seed integrity ---------------------------------------------------------------------------

    @Test
    fun seed_has577FedExercises_withUniqueSlugs() {
        // 568 (through DB-6B) + 9 DB-6E sled/carry time-distance imports = 577.
        assertEquals(577, all.size)
        assertEquals(all.size, all.map { it.slug }.toSet().size)
        assertTrue(all.all { it.name.isNotBlank() && it.slug.isNotBlank() })
    }

    @Test
    fun seed_everyExerciseHasInstructionsAndPrimaryMuscle() {
        assertTrue(all.all { it.instructions.isNotEmpty() })
        assertTrue(all.all { it.primaryMuscles.isNotEmpty() })
    }

    @Test
    fun seed_isFedCanonical_renamedNamesPresent_legacyNamesAbsent() {
        val n = names(all)
        // FED canonical targets that legacy AURON names were renamed to.
        listOf(
            "Seated Cable Rows", "Arnold Dumbbell Press", "Barbell Deadlift", "Pushups",
            "Leg Press", "Barbell Squat", "Lying Leg Curls", "Face Pull",
        ).forEach { assertTrue("missing FED canonical: $it", n.contains(it)) }
        // Legacy AURON-only names must no longer exist in the FED-first catalog.
        listOf(
            "Conventional Barbell Deadlift", "Barbell Bicep Curl", "Barbell Back Squat",
            "Cable Face Pull", "Push-Up",
        ).forEach { assertFalse("legacy name leaked: $it", n.contains(it)) }
    }

    @Test
    fun db5b_safeAddImportsPresent_advancedAndRejectAbsent() {
        val n = names(all)
        // DB-5B SAFE_ADD imports must be present.
        listOf(
            "Barbell Hip Thrust", "Trap Bar Deadlift", "Weighted Pull Ups", "Sumo Deadlift",
            "Barbell Glute Bridge", "Good Morning", "Glute Ham Raise", "Rack Pulls", "Floor Press",
            "Hyperextensions (Back Extensions)",
        ).forEach { assertTrue("missing SAFE_ADD import: $it", n.contains(it)) }
        // Non-imported olympic variants must remain absent.
        // (NB: foundational olympic lifts — Snatch, Hang Clean, Clean and Jerk — were later imported by
        //  DB-5F with a conservative SKILL_FORM fallback, so they are intentionally NOT listed here.)
        listOf(
            "Split Snatch", "Power Jerk", "Muscle Snatch",
        ).forEach { assertFalse("non-imported bucket leaked: $it", n.contains(it)) }
        // REJECT (neck / strongman event / drill) must NOT be imported.
        listOf(
            "Atlas Stones", "Tire Flip", "Yoke Walk", "Isometric Neck Exercise - Sides", "Balance Board",
        ).forEach { assertFalse("REJECT leaked: $it", n.contains(it)) }
        // Phase-1 dedup skips must remain absent.
        listOf("Smith Machine Decline Press", "Parallel Bar Dip", "Floor Glute-Ham Raise").forEach {
            assertFalse("dedup-skipped entry leaked: $it", n.contains(it))
        }
    }

    @Test
    fun db5d_setsRepsWeightImportsPresent_blockedBucketsAbsent() {
        val n = names(all)
        // The 5 DB-5D SETS_REPS_WEIGHT imports must be present and load-based (reps/kg/rir):
        // COMPOUND_LOAD/ISOLATION_SLOW progression, never bodyweight — i.e. no new tracking fields.
        val db5d = listOf("Board Press", "Pin Presses", "Kneeling Squat", "Seated Good Mornings", "Svend Press")
        db5d.forEach { name ->
            val e = all.firstOrNull { it.name == name }
            assertTrue("missing DB-5D import: $name", e != null)
            assertTrue(
                "DB-5D import not load-based: $name",
                e!!.progressionType in setOf(ProgressionType.COMPOUND_LOAD, ProgressionType.ISOLATION_SLOW) &&
                    !e.isBodyweight,
            )
        }
        // VELOCITY_POWER variants NOT imported in DB-5F must remain absent (DB-5F imported only the
        // foundational olympic lifts; from-blocks/balance/split variants stay out).
        listOf("Power Clean from Blocks", "Clean Pull", "Split Jerk", "Jerk Balance", "Snatch from Blocks")
            .forEach { assertFalse("non-imported velocity variant leaked: $it", n.contains(it)) }
        // Time/distance/load sled and carry items are intentionally imported by DB-6E below.
        // UNSUPPORTED_FOR_NOW must NOT be imported.
        listOf("Sandbag Load", "Log Lift").forEach { assertFalse("UNSUPPORTED leaked: $it", n.contains(it)) }
        // Accommodating-resistance dedup skips must remain absent.
        listOf("Squat with Chains", "Reverse Band Deadlift", "Bench Press - Powerlifting", "Olympic Squat")
            .forEach { assertFalse("dedup-skipped DB-5D entry leaked: $it", n.contains(it)) }
    }

    @Test
    fun db5d2_highConfidenceLevelCorrectionsApplied() {
        fun level(name: String) = all.first { it.name == name }.level
        // DB-5D.2: 5 upgraded to ADVANCED, 1 downgraded to INTERMEDIATE.
        listOf(
            "Weighted Pull Ups", "Power Clean", "Single-Arm Push-Up", "Snatch Pull",
            "Smith Machine Hang Power Clean",
        ).forEach { assertEquals("expected ADVANCED: $it", Level.ADVANCED, level(it)) }
        assertEquals(Level.INTERMEDIATE, level("Barbell Ab Rollout - On Knees"))
    }

    @Test
    fun db5f_aliasesResolveToCanonicalEntries() {
        fun search(q: String) = names(CatalogSearch.filter(all, query = q))
        // DB-5F search aliases (the real user-reported discoverability fixes).
        assertTrue(search("dumbbell lateral raise").contains("Side Lateral Raise"))
        assertTrue(search("lateral raise").contains("Side Lateral Raise"))
        assertTrue(search("push up").contains("Pushups"))
        assertTrue(search("seated cable row").contains("Seated Cable Rows"))
        assertTrue(search("arnold press").contains("Arnold Dumbbell Press"))
        assertTrue(search("machine shoulder press").contains("Machine Shoulder (Military) Press"))
        // Aliases must never collide with an actual entry name.
        val nameSet = names(all)
        all.flatMap { it.aliases }.forEach { assertFalse("alias==name: $it", nameSet.contains(it)) }
    }

    @Test
    fun db5f_finalImportsPresent_loadModelSafe() {
        val db5f = listOf(
            "Muscle Up", "Ring Dips", "One Arm Chin-Up", "Scapular Pull-Up", "Single-Leg High Box Squat",
            "Suspended Row", "Suspended Push-Up", "Overhead Squat", "Plate Pinch", "Snatch",
            "Clean and Jerk", "Push Press", "Hang Clean", "Hang Snatch", "Power Snatch",
        )
        db5f.forEach { name ->
            val e = all.firstOrNull { it.name == name }
            assertTrue("missing DB-5F import: $name", e != null)
            // Must log with the existing reps/kg/bodyweight model — BODYWEIGHT or SKILL_FORM only
            // (no time/distance/velocity tracking type exists; nothing needs new Log Workout fields).
            assertTrue(
                "DB-5F import not load-model-safe: $name",
                e!!.progressionType in setOf(ProgressionType.BODYWEIGHT, ProgressionType.SKILL_FORM),
            )
        }
    }

    @Test
    fun db6b_everyEntryHasTrackingType_knownValues() {
        // Every entry carries a trackingType, including DB-6E's newly live non-rep sled/carry types.
        assertEquals(577, all.size)
        val used = all.map { it.trackingType }.toSet()
        assertEquals(
            setOf(
                TrackingType.REPS_WEIGHT, TrackingType.BODYWEIGHT_REPS,
                TrackingType.SKILL_FORM, TrackingType.TIME_ONLY,
                TrackingType.DISTANCE_LOAD, TrackingType.TIME_DISTANCE_LOAD,
            ),
            used,
        )
        fun tt(name: String) = all.first { it.name == name }.trackingType
        assertEquals(TrackingType.REPS_WEIGHT, tt("Side Lateral Raise"))
        assertEquals(TrackingType.BODYWEIGHT_REPS, tt("Pushups"))
        assertEquals(TrackingType.TIME_ONLY, tt("Plank"))
        assertEquals(TrackingType.TIME_ONLY, tt("Side Bridge"))
        listOf("Snatch", "Clean and Jerk", "Hang Clean")
            .forEach { assertEquals("expected SKILL_FORM: $it", TrackingType.SKILL_FORM, tt(it)) }
        // trackingType survives the entity round-trip (Room mapping).
        all.forEach { assertEquals(it.trackingType, it.toEntity().toDomain().trackingType) }
    }

    @Test
    fun db6e_sledCarryImportsPresent_withCorrectTrackingTypes() {
        val expected = mapOf(
            "Backward Drag" to TrackingType.TIME_DISTANCE_LOAD,
            "Bear Crawl Sled Drags" to TrackingType.TIME_DISTANCE_LOAD,
            "Farmer's Walk" to TrackingType.DISTANCE_LOAD,
            "Sled Drag - Harness" to TrackingType.TIME_DISTANCE_LOAD,
            "Sled Overhead Backward Walk" to TrackingType.TIME_DISTANCE_LOAD,
            "Sled Overhead Triceps Extension" to TrackingType.TIME_DISTANCE_LOAD,
            "Sled Push" to TrackingType.TIME_DISTANCE_LOAD,
            "Sled Reverse Flye" to TrackingType.TIME_DISTANCE_LOAD,
            "Sled Row" to TrackingType.TIME_DISTANCE_LOAD,
        )

        expected.forEach { (name, trackingType) ->
            val exercise = all.firstOrNull { it.name == name }
            assertTrue("missing DB-6E import: $name", exercise != null)
            assertEquals("wrong DB-6E trackingType: $name", trackingType, exercise!!.trackingType)
            assertTrue("DB-6E import must not be REPS_WEIGHT: $name", exercise.trackingType != TrackingType.REPS_WEIGHT)
            assertEquals("non-rep imports must use rep sentinel target min: $name", 0, exercise.defaultMinReps)
            assertEquals("non-rep imports must use rep sentinel target max: $name", 0, exercise.defaultMaxReps)
            assertEquals(
                listOf("exercise_images/${exercise.slug}/0.jpg", "exercise_images/${exercise.slug}/1.jpg"),
                exercise.imageRefs,
            )
        }
    }

    @Test
    fun db6b_trackingType_fromName_fallsBackSafely() {
        assertEquals(TrackingType.REPS_WEIGHT, TrackingType.fromName(null))
        assertEquals(TrackingType.REPS_WEIGHT, TrackingType.fromName("NOT_A_TYPE"))
        assertEquals(TrackingType.TIME_DISTANCE_LOAD, TrackingType.fromName("TIME_DISTANCE_LOAD"))
    }

    // ---- Bundled images: exactly two refs each, all resolve, folders are 1:1 with slugs -----------

    @Test
    fun imageRefs_areBundledAssetPairs_andResolve() {
        assertTrue(all.all { it.imageRefs.size == 2 })
        all.forEach { ex ->
            assertEquals(
                listOf("exercise_images/${ex.slug}/0.jpg", "exercise_images/${ex.slug}/1.jpg"),
                ex.imageRefs,
            )
            ex.imageRefs.forEach { ref ->
                assertTrue("missing asset: $ref", File(assetsDir, ref).exists())
            }
        }
    }

    @Test
    fun imageFolders_are1to1WithCatalogSlugs_noOrphans() {
        val folders = File(assetsDir, "exercise_images")
            .listFiles { f -> f.isDirectory }!!
            .map { it.name }
            .toSet()
        assertEquals(all.map { it.slug }.toSet(), folders)
    }

    // ---- Text query (name) -----------------------------------------------------------------------

    @Test
    fun query_matchesName_caseInsensitive() {
        val squat = names(CatalogSearch.filter(all, query = "squat"))
        assertTrue(squat.contains("Barbell Squat"))
        assertEquals(squat, names(CatalogSearch.filter(all, query = "SQUAT")))
    }

    @Test
    fun query_blankReturnsAll_noMatchReturnsEmpty() {
        assertEquals(577, CatalogSearch.filter(all, query = "  ").size)
        assertTrue(CatalogSearch.filter(all, query = "zzzznope").isEmpty())
    }

    @Test
    fun query_matchesManyCurls() {
        assertTrue(CatalogSearch.filter(all, query = "curl").size > 15)
    }

    // ---- Filters (property-based: results must match the filter and be non-empty) -----------------

    @Test
    fun filter_byEquipment_allMatch() {
        for (eq in listOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.CABLE, Equipment.MACHINE, Equipment.BODYWEIGHT)) {
            val r = CatalogSearch.filter(all, equipment = eq)
            assertTrue("empty for $eq", r.isNotEmpty())
            assertTrue("mismatch for $eq", r.all { it.equipment == eq })
        }
    }

    @Test
    fun filter_byPrimaryMuscle_allMatch() {
        for (mg in listOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.QUADS)) {
            val r = CatalogSearch.filter(all, primaryMuscle = mg)
            assertTrue("empty for $mg", r.isNotEmpty())
            assertTrue("mismatch for $mg", r.all { it.primaryMuscles.contains(mg) })
        }
    }

    @Test
    fun filter_byLevel_allMatch_andPartition() {
        for (lv in listOf(Level.BEGINNER, Level.INTERMEDIATE, Level.ADVANCED)) {
            val r = CatalogSearch.filter(all, level = lv)
            assertTrue("empty for $lv", r.isNotEmpty())
            assertTrue("mismatch for $lv", r.all { it.level == lv })
        }
        assertEquals(577, Level.entries.sumOf { CatalogSearch.filter(all, level = it).size })
    }

    @Test
    fun filter_byProgressionType_allMatch() {
        for (pt in listOf(ProgressionType.COMPOUND_LOAD, ProgressionType.ISOLATION_SLOW, ProgressionType.BODYWEIGHT)) {
            val r = CatalogSearch.filter(all, progressionType = pt)
            assertTrue("empty for $pt", r.isNotEmpty())
            assertTrue("mismatch for $pt", r.all { it.progressionType == pt })
        }
    }

    @Test
    fun filters_areAndCombined() {
        val r = CatalogSearch.filter(all, equipment = Equipment.BARBELL, primaryMuscle = MuscleGroup.QUADS)
        assertTrue(r.isNotEmpty())
        assertTrue(r.all { it.equipment == Equipment.BARBELL && it.primaryMuscles.contains(MuscleGroup.QUADS) })
        assertTrue(names(r).contains("Barbell Squat"))
    }

    // ---- Entity round-trip + safe enum decoding --------------------------------------------------

    @Test
    fun entityRoundTrip_preservesAllFields() {
        all.forEach { original -> assertEquals(original, original.toEntity().toDomain()) }
    }

    @Test
    fun fromName_unknownValue_fallsBackSafely() {
        assertEquals(Equipment.OTHER, Equipment.fromName("NOT_A_REAL_VALUE"))
        assertEquals(ProgressionType.COMPOUND_LOAD, ProgressionType.fromName(null))
        assertEquals(null, MuscleGroup.fromName("NOPE"))
    }

    // ---- Active-Plan rename migration (DB-4B.2) --------------------------------------------------

    @Test
    fun planMigration_mapHas63SafePairs_targetsExistInCatalog() {
        assertEquals(63, PlanNameMigration.MAP.size)
        val catalogNames = names(all)
        PlanNameMigration.MAP.values.forEach {
            assertTrue("rename target not in catalog: $it", catalogNames.contains(it))
        }
    }

    @Test
    fun planMigration_isCollisionFree() {
        val targets = PlanNameMigration.MAP.values.toList()
        assertEquals("two legacy names map to the same FED name", targets.size, targets.toSet().size)
    }

    @Test
    fun planMigration_containsKnownSafe_excludesKnownUnsafe() {
        assertEquals("Seated Cable Rows", PlanNameMigration.canonicalName("Seated Cable Row"))
        assertEquals("Arnold Dumbbell Press", PlanNameMigration.canonicalName("Arnold Press"))
        assertEquals("Barbell Deadlift", PlanNameMigration.canonicalName("Conventional Barbell Deadlift"))
        // Known-unsafe pairs from DB-4A.1 must NEVER be present.
        assertFalse(PlanNameMigration.MAP["Diamond Push-Up"] == "Pushups")
        assertFalse(PlanNameMigration.MAP["Side Plank"] == "Plank")
        assertFalse(PlanNameMigration.MAP["Bicycle Crunch"] == "Crunches")
        assertFalse(PlanNameMigration.MAP.containsKey("Barbell Flat Bench Press"))
    }

    @Test
    fun planMigration_isIdempotent() {
        (PlanNameMigration.MAP.keys + PlanNameMigration.MAP.values).forEach { name ->
            val once = PlanNameMigration.canonicalName(name)
            assertEquals(once, PlanNameMigration.canonicalName(once))
        }
    }
}
