# DB-5B — Import SAFE_ADD REVIEW Exercises — COMPLETED ✅

Imported the DB-5A **SAFE_ADD** REVIEW exercises (only) into the FED-canonical catalog. No MAYBE_ADD,
no REJECT, no olympic/velocity/strongman-carry entries. No Room schema change, no engine change, no
History change. Unit tests + debug + release builds are green.

## Headline numbers

| Metric | Value |
|---|---|
| 1. Before catalog count | **515** |
| 2. Final catalog count | **548** |
| 3. SAFE_ADD reviewed | **40** |
| 4. Imported | **33** |
| 5. Skipped — duplicate | **4** |
| 6. Skipped — review-skip | **3** |
| 9. Image folders / files — before | 515 / 1030 |
| 9. Image folders / files — after | **548 / 1096** (+33 folders, +66 images) |
| 10. CONTENT_VERSION | **10 → 11** |
| 11. Room schema / `@Database version` | **4 (unchanged)** |
| 12. Engine V3.6B/V3.7A | **not modified by DB-5B; engine tests pass** |

Final count is **548, not ~555**, because the strict dedup pass removed **7** of the 40 SAFE_ADD
candidates (4 true duplicates + 3 ambiguous) — see the skip list and reasons below.

## Phase 1 — dedup result

### 7. Imported (33)
**Compound (13):** Trap Bar Deadlift, Sumo Deadlift, Deficit Deadlift, Rack Pulls, Barbell Hip Thrust,
Barbell Glute Bridge, Good Morning, Box Squat, Floor Press, Dumbbell Floor Press, Glute Ham Raise,
Weighted Pull Ups, Weighted Bench Dip.
**Isolation (9):** Donkey Calf Raises, Front Plate Raise, Hyperextensions (Back Extensions),
Reverse Hyperextension, Reverse Plate Curls, Seated Band Hamstring Curl, Wrist Roller, Plate Twist,
Weighted Sit-Ups - With Bands.
**Bodyweight (11):** Ab Roller, Bodyweight Walking Lunge, Decline Push-Up, Dips - Chest Version,
Inverted Row, Inverted Row with Straps, Knee/Hip Raise On Parallel Bars, Push-Up Wide, Side Bridge,
Band Assisted Pull-Up, Platform Hamstring Slides.

### 8. Skipped, with reasons (7)
**SKIP_DUPLICATE (4):**
| Candidate | Existing 515 entry it duplicates |
|---|---|
| Smith Machine Decline Press | **Decline Smith Press** (same Smith-machine decline press) |
| Floor Glute-Ham Raise | **Natural Glute Ham Raise** (same improvised bodyweight GHR) |
| Parallel Bar Dip | **Dips - Triceps Version** (same parallel-bar triceps dip) |
| Bodyweight Mid Row | **Inverted Row** (same bodyweight horizontal row; also imported) |

**REVIEW_SKIP (3 — ambiguous, "if unsure, skip"):**
| Candidate | Reason |
|---|---|
| Incline Inner Biceps Curl | Overlaps existing **Standing Inner-Biceps Curl** + **Incline Dumbbell Curl**; niche cue |
| Single Dumbbell Raise | Vague "two-handed single-dumbbell raise" overlapping the imported **Front Plate Raise**; ambiguous |
| Weighted Squat | Vague naming (equipment `other`); ambiguous vs existing **Barbell Squat** |

## Phase 2 — import implementation

For each of the 33 imports: FED canonical **name**, FED **slug** (`kebab(id)`), FED **instructions**
(normalized ≤6 steps), FED **muscles** (mapped to AURON enums, unmapped dropped), FED **level**, plus
deterministic AURON-compatible fields:

- **progressionType** by the existing derivation, with the genuine gap (bodyweight movements tagged
  `other`/null equipment) handled explicitly: a curated bodyweight set → `BODYWEIGHT`; else
  `compound→COMPOUND_LOAD`, `isolation→ISOLATION_SLOW`. `isBodyweight = (type==BODYWEIGHT)`.
- **equipment** mapped FED→AURON; `other`/null resolved deterministically (calisthenics→`BODYWEIGHT`,
  plates/trap-bar/belt→`OTHER`, banded→`BAND`, GHD/reverse-hyper→`MACHINE`).
- **force** backfilled where FED was null (Push-Up Wide→`PUSH`, Band Assisted Pull-Up→`PULL`).
- **defaults** (sets/reps/increment/sessions) from the per-type defaults; `isUnilateral=false` (all
  bilateral); `aliases`/`tips` empty; `imageRefs` = the two copied frames.

Result mix: progression **BODYWEIGHT 11 / ISOLATION_SLOW 9 / COMPOUND_LOAD 13**; equipment
**BODYWEIGHT 10 / OTHER 9 / BARBELL 8 / BAND 3 / MACHINE 2 / DUMBBELL 1**.

Images: 66 copied from `free-exercise-db/exercises/<id>/{0,1}.jpg` into
`app/src/main/assets/exercise_images/<fed-slug>/{0,1}.jpg`. External dataset untouched; no unrelated
assets deleted. `CatalogSeed.CONTENT_VERSION` bumped **10 → 11**.

## Phase 3 — tests & builds

| 13. Item | Result |
|---|---|
| `:app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** (95 tests) |
| `:app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Engine V3.6B/V3.7A tests | ✅ pass (run within `testDebugUnitTest`; engine files not edited) |

Catalog test updated: count 515→548, level partition 548, blank-query 548, plus
`db5b_safeAddImportsPresent_advancedAndRejectAbsent` asserting the 10 known SAFE_ADD present and the
MAYBE/olympic/strongman/REJECT names + dedup-skips absent. Integrity verified by tests: **548 unique
slugs, 548 unique names, all imageRefs resolve, image folders 1:1 with slugs (0 orphans)**.

**One test self-correction (not a data bug):** the first run failed because the assertion list included
`"Power Clean"` as "must be absent". FED tags **Power Clean as `category=strength`**, so it was already
a member of the canonical **515** from DB-4B.2 — it is *out of DB-5B scope*, not a new import. DB-5B did
not add it. The assertion was corrected (Power Clean removed from the must-be-absent list, with a
comment); the other olympic names (Hang Clean, Snatch, Clean and Jerk) and strongman names
(Farmer's Walk, Sled Push) remain correctly absent.

## 14. APK size before → after

| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 93,143,595 B (88.83 MB) | 94,720,600 B (90.33 MB) | +1,577,005 B (+1.50 MB) |
| Release | 83,004,360 B (79.16 MB) | 87,190,036 B (83.15 MB) | +4,185,676 B (+3.99 MB) |

Growth is the +66 bundled images (dataset-original JPEGs) plus 33 JSON entries.

## 11/12. Schema & engine status
- **Room:** `@Database version` stays **4**. No schema change — the catalog is reference data re-seeded
  on the `CONTENT_VERSION` bump.
- **Engine:** `ProgressiveOverload.kt` / `ProgressiveOverloadEngineTest.kt` were **not edited by DB-5B**;
  the engine unit suite passes. (The engine file carries a prior, non-DB-5B mtime — the task header
  references V3.7A — which this task did not touch.)
- **History:** not touched. **Signing secrets:** not touched.

## 15. Rollback plan
- **Catalog:** `git`-revert / restore `app/src/main/assets/catalog/catalog_fed_v1.json` to its 515-entry
  state and revert `CatalogSeed.CONTENT_VERSION` 11→10 — the catalog table re-seeds on next launch
  (reference data, independent of user data).
- **Images:** delete the 33 newly-created `<fed-slug>` folders (re-derivable from the FED dataset); no
  pre-existing folders were modified.
- **Tests:** revert `CatalogSearchTest.kt` (counts 548→515, remove the DB-5B test).
- A pre-existing full backup of `app/src` + `docs` remains at
  `D:\HOCTIN\GYM_TRACKER_DB4B2_BACKUP_20260620_123717`. No History/Plan/schema/engine/signing changes to
  undo.

## Hard-stop check
None triggered: JSON valid, every import has 2 resolving images, no duplicate slug/name, all imageRefs
resolve, no Room schema change, no engine change, no MAYBE/REJECT needed, build+tests green with a single
minimal test fix.
