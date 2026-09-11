# DB-4B.2 — FED Canonical Catalog Rebuild — COMPLETED ✅

**Status: COMPLETED.** The AURON exercise catalog is now rebuilt from free-exercise-db (FED,
public domain) as the single source of truth. Unit tests pass; debug + release builds are green.
No hard-stop condition was hit.

- **Outcome:** completed (not halted).
- **Backup (created before any change):** `D:\HOCTIN\GYM_TRACKER_DB4B2_BACKUP_20260620_123717`
  (snapshot of `app/src` + `docs`).
- **Verified rename source used:** `docs/db4a1_verified_rename_map.csv` (63 SAFE_RENAME rows only).
  The old unsafe `docs/db4a_plan_name_migration.csv` was **not** used.

---

## Architecture

The catalog is no longer hand-authored in Kotlin. The 515 FED `SUPPORTED_STRENGTH` entries are bundled
as a JSON asset and loaded into Room at seed time:

- **Catalog data:** `app/src/main/assets/catalog/catalog_fed_v1.json` (515 entries, ~735 KB).
- **Loader:** `CatalogJsonLoader.parse(json)` → `List<CatalogExercise>` (pure, org.json; enum fields
  decode via the taxonomy `fromName` helpers with safe fallbacks).
- **Seeding:** `ExerciseCatalogRepository.ensureSeeded()` reads the asset and re-seeds (clear+insert)
  when `CatalogSeed.CONTENT_VERSION` changes — bumped **9 → 10**. Reference data only; never touches
  user Plan/History.
- The 12,969-line Kotlin `CatalogSeed.exercises` was removed; `CatalogSeed.kt` is now a 20-line version
  marker.

---

## Files changed

**Created**
- `app/src/main/assets/catalog/catalog_fed_v1.json` — 515-entry FED catalog (752,958 B).
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogJsonLoader.kt` — JSON → domain loader.
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/PlanNameMigration.kt` — 63 SAFE pairs + runner.
- 68 image files copied into 34 fresh FED-slug folders.

**Modified**
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogSeed.kt` — gutted to `CONTENT_VERSION = 10`.
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/ExerciseCatalogRepository.kt` — load JSON asset.
- `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt` — added `renameExercisesByName(old,new)` (Plan table only).
- `app/src/main/java/com/hoctin/gymtracker/GymTrackerApplication.kt` — flag-guarded one-time Plan name migration after seeding.
- `app/src/main/java/com/hoctin/gymtracker/data/SeedData.kt` — default templates updated to FED canonical.
- `app/src/test/java/com/hoctin/gymtracker/data/catalog/CatalogSearchTest.kt` — rewritten for FED-first invariants.
- `app/build.gradle.kts` — added `testImplementation("org.json:json:20231013")`.

**Deleted**
- 65 orphan exercise-image folders (legacy AURON-slug folders not in the 515 FED set). No source files deleted.

> Note: `data/catalog/`, `src/test/`, and `assets/exercise_images/` are **untracked** in the main repo's
> git (pre-existing condition — the git baseline is old GYMATE). They are real on disk and must be
> `git add`-ed when this work is eventually committed.

---

## Catalog

| Metric | Value |
|---|---|
| Final catalog count | **515** (FED `SUPPORTED_STRENGTH`) |
| Slugs unique | ✅ yes (slug = kebab(FED id)) |
| Names unique | ✅ yes |
| Every entry has instructions + ≥1 primary muscle | ✅ yes |
| JSON catalog path | `app/src/main/assets/catalog/catalog_fed_v1.json` |

## Images

| Metric | Value |
|---|---|
| Image folders (final) | **515** |
| Image files (final) | **1030** (2 per exercise: `0.jpg`, `1.jpg`) |
| Orphan folders removed | **65** |
| Fresh images copied | **68** (34 new FED-slug folders × 2) |
| All `imageRefs` resolve to bundled assets | ✅ yes (verified by unit test) |
| Orphan folders remaining | **0** (folders are 1:1 with catalog slugs) |

## Active Plan name migration

- **SAFE_RENAME pairs applied in code:** **63** (= 25 identity no-ops + 38 real renames).
- **Unsafe mappings present:** **NONE** (all 8 known-unsafe pairs confirmed absent).
- **Collisions:** **NONE.** All 63 targets exist in the 515-entry catalog.
- **REVIEW/CUSTOM left untouched:** **92** (62 REVIEW_REQUIRED + 30 KEEP_CUSTOM from DB-4A.1). Those
  Plan rows, if a user has them, stay on their existing name and resolve via the engine's
  name-inference fallback — exactly like a custom exercise.
- **Guarding:** runs once at app startup after `ensureSeeded()`, gated by prefs flag
  `fed_canonical_plan_name_migration_v1_done` (prefs file `auron_migrations`).
- **Idempotent:** keys are legacy names, values are FED names; no value is a different key, and the flag
  prevents re-runs. Verified by unit test.
- **History-safe:** the SQL updates only the `exercises` (active Plan) table:
  `UPDATE exercises SET name=:new WHERE name=:old`. History (`logged_sets.exerciseNameSnapshot`) is
  never touched.
- ⚠️ **Plan rows have NOT been migrated yet.** The app was not launched, so the one-time migration has
  not executed. It will run on the next real app start. No user Plan data has changed.

## Default templates (`SeedData`)

- **Renamed to FED canonical (13):** Cable Face Pull→Face Pull, Reverse-Grip Triceps Pushdown→Reverse
  Grip Triceps Pushdown, Seated Cable Row→Seated Cable Rows, Straight-Arm Cable Pulldown→Straight-Arm
  Pulldown, Leg Extension→Leg Extensions, Lying Leg Curl→Lying Leg Curls, Standing Calf Raise→Standing
  Calf Raises, plus identity-safe Wide-Grip Lat Pulldown, Incline Dumbbell Curl, Leg Press, Seated Leg
  Curl, Triceps Pushdown, Cable Crunch.
- **Removed (6 — no SAFE FED equivalent, not force-mapped):** Incline Machine Chest Press, Seated
  Machine Chest Press (Push + Full Body), Pec Deck Machine Fly, Dumbbell Seated Lateral Raise,
  Wide-Grip Pull-Up, Dumbbell Hammer Curl.
- **Kept (intentional name-inference customs):** "Push-up optional", "EZ curl optional".
- Day structure / order / target sets·reps otherwise preserved. All non-optional template names now
  exist in the FED catalog (verified).

---

## Validation results

| Check | Result |
|---|---|
| Unit tests (`:app:testDebugUnitTest`) | ✅ **BUILD SUCCESSFUL** |
| Debug build (`:app:assembleDebug`) | ✅ **BUILD SUCCESSFUL** |
| Release build (`:app:assembleRelease`) | ✅ **BUILD SUCCESSFUL** (signed via existing keystore; secrets untouched) |
| Engine V3.6B tests (`ProgressiveOverloadEngineTest`) | ✅ pass (ran within unit-test task) |
| `ProgressiveOverload.kt` / engine test files | ✅ unchanged (mtime 06-17; not edited) |
| Import/Export | ✅ compiles (full app assembled) |
| History code | ✅ unchanged (no edits to `logged_sets`/History UI/snapshots) |
| Room `@Database version` | ✅ **4** (unchanged — no schema change) |

### APK size before → after

| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 94,009,007 B (89.65 MB) | 93,143,595 B (88.83 MB) | −865,412 B (−0.85 MB) |
| Release | 86,841,246 B (82.82 MB) | 83,001,128 B (79.16 MB) | −3,840,118 B (−3.66 MB) |

Release shrank ~3.66 MB: fewer image folders (607→515 catalog entries; 1092→1030 image files) and the
catalog moved from compiled Kotlin to a 735 KB JSON asset.

---

## Known limitations

1. **Thinned default days.** Because 6 curated machine-press/variant exercises had no SAFE FED match
   and were removed (not force-mapped to a different FED variant), the default **Push** day is now thin
   (Face Pull, Reverse Grip Triceps Pushdown, Push-up optional) and **Full Body** has 4 entries.
   New users should populate these from the now 515-exercise picker. (Strict consequence of FED-first +
   conservative matching; intentional, not a bug.)
2. **Mixed image optimization.** The 34 freshly-copied folders use dataset-original JPEGs; the other
   481 retain DB-3D.1 optimization. Minor per-image size variance only.
3. **No FED aliases/tips.** FED entries carry instructions but no tips and no aliases. So `findByName`
   resolves only exact FED names; legacy Plan names rely on the one-time migration (63 SAFE) or on
   engine name-inference (everything else).
4. **Untracked in git.** The catalog package, test tree, and image assets are untracked in the main
   repo (pre-existing); a future commit must `git add` them.

## Rollback plan

- **Restore code/data:** copy `app/src` and `docs` back from
  `D:\HOCTIN\GYM_TRACKER_DB4B2_BACKUP_20260620_123717`, then rebuild.
- **Images:** also restorable from the backup, and fully re-derivable from the FED dataset.
- **Catalog re-seed:** restoring the old `CatalogSeed.kt` + reverting `CONTENT_VERSION` re-seeds the old
  catalog on next launch (catalog table is reference data, independent of user data).
- **Plan migration:** not yet executed (no app launch), so there is nothing to undo. The migration is
  idempotent and flag-guarded; if it had run, reverting it would require restoring Plan names from a DB
  backup, not just code — but that situation does not apply here.
- **Signing secrets and History were never touched.**
