# DB-5D — Import MAYBE SETS_REPS_WEIGHT Exercises — COMPLETED ✅

Imported the DB-5C `SETS_REPS_WEIGHT` MAYBE exercises that survive a strict dedup pass. Data-only and
additive: **no Room schema change, no engine change, no Log Workout input change, no time/distance/
velocity fields.** Unit tests + debug + release builds are green.

## Headline numbers

| Metric | Value |
|---|---|
| 1. Before catalog count | **548** |
| 2. Final catalog count | **553** |
| 3. SETS_REPS_WEIGHT reviewed | **39** |
| 4. Imported | **5** |
| 5. Skipped — duplicate | **25** |
| 6. Skipped — review-skip | **9** |
| 9. Image folders / files — before | 548 / 1096 |
| 9. Image folders / files — after | **553 / 1106** (+5 folders, +10 images) |
| 10. CONTENT_VERSION | **11 → 12** |
| 11. Room schema / `@Database version` | **4 (unchanged)** |
| 12. Engine | **not modified; engine tests pass** |

Final count is **553, not ~587** because dedup removed **34 of 39** candidates — almost the entire SRW
pool is accommodating-resistance / olympic near-duplicates of lifts already in the catalog.

## Phase 1 — dedup result

### 7. Imported (5) — all `COMPOUND_LOAD`, log with existing reps/kg/RIR
| Name | Equipment | Force | Why distinct |
|---|---|---|---|
| Board Press | BARBELL | PUSH | Partial-ROM lockout press (boards) — not in catalog |
| Pin Presses | BARBELL | PUSH | Dead-stop press from pins — distinct stimulus, not in catalog |
| Kneeling Squat | BARBELL | PUSH | Kneeling hip-dominant glute movement — distinct, not in catalog |
| Seated Good Mornings | BARBELL | PULL | Seated posterior-chain variant — distinct setup, not in catalog |
| Svend Press | OTHER (plate) | PUSH | Plate-squeeze inner-chest press — distinct, not in catalog |

### 8. Skipped, with reasons (34)

**SKIP_DUPLICATE (25)** — same movement already in the 548 catalog; adding creates clutter. The catalog
already even contains accommodating-resistance variants (`Box Squat with Chains`, `Bench Press - With
Bands`, `Squats - With Bands`, `Hip Flexion with Band`), so more band/chain variants are pure clutter.

- *Band/chain/reverse-band/speed powerlifting (21):* Band Good Morning, Band Good Morning (Pull Through),
  Bench Press - Powerlifting, Bench Press with Chains, Box Squat with Bands, Chain Press, Deadlift with
  Bands, Deadlift with Chains, Floor Press with Chains, Hip Lift with Band, Rack Pull with Bands, Reverse
  Band Bench Press, Reverse Band Box Squat, Reverse Band Deadlift, Reverse Band Power Squat, Reverse Band
  Sumo Deadlift, Speed Box Squat, Squat with Bands, Squat with Chains, Sumo Deadlift with Bands, Sumo
  Deadlift with Chains. *(Their base lifts — Barbell Squat, Box Squat, Barbell Deadlift, Sumo Deadlift,
  Floor Press, Rack Pulls, Good Morning, Barbell Glute Bridge — are all already present.)*
- *Olympic near-duplicates (4):* Olympic Squat (= Barbell Squat), Frankenstein Squat (= Front Barbell
  Squat), Clean Shrug (= Barbell Shrug), Snatch Shrug (= Barbell Shrug).

**REVIEW_SKIP (9)** — ambiguous / niche / olympic-guarded.
- *Niche powerlifting (3):* Good Morning off Pins, Hanging Bar Good Morning, Chain Handle Extension.
- *Olympic-category, excluded per the DB-5D "no olympic" guard + near-duplicate (5):* Clean Deadlift,
  Snatch Deadlift, Clean Pull, Romanian Deadlift from Deficit, Wide Stance Stiff Legs. *(DB-5C had
  classified these SRW; DB-5D's stricter olympic guard and dedup overlap defer them — they are
  olympic-category and/or duplicate existing deadlift/RDL variants.)*
- *Strongman (1):* Axle Deadlift — thick-bar grip-specialty near-duplicate of Barbell Deadlift.

## Phase 2 — import implementation
Each of the 5 entries uses FED canonical name/slug/instructions/muscles/level; deterministic AURON
defaults: `progressionType = COMPOUND_LOAD` (FED mechanic=compound), equipment BARBELL (Svend Press →
OTHER/plate), FED `force` present (no backfill needed), COMPOUND_LOAD defaults (3 / 8–12 / +2.5 kg /
1 session), `isBodyweight=false`, `isUnilateral=false`, empty aliases/tips, `imageRefs` = the two copied
frames. 10 images copied from `free-exercise-db/exercises/<id>/{0,1}.jpg` into
`assets/exercise_images/<fed-slug>/`. External dataset untouched; no unrelated assets deleted.
`CONTENT_VERSION` bumped **11 → 12**. README got one short "FED catalog growth (DB-5B / DB-5D)" note
(none existed).

## Phase 3 — tests & builds

| 13. Item | Result |
|---|---|
| `:app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Engine tests (within unit suite) | ✅ pass (engine files not edited) |

Catalog test updated: count 548→**553** (size, blank-query, level partition), plus
`db5d_setsRepsWeightImportsPresent_blockedBucketsAbsent` asserting the 5 imports are present and
load-based (`COMPOUND_LOAD/ISOLATION_SLOW`, not bodyweight), and that VELOCITY_POWER (Snatch, Hang Clean,
Clean and Jerk, Power Clean from Blocks, Clean Pull), TIME/DISTANCE_LOAD (Farmer's Walk, Sled Push, Sled
Drag - Harness, Backward Drag), UNSUPPORTED (Sandbag Load, Log Lift), and dedup-skips (Squat with Chains,
Reverse Band Deadlift, Bench Press - Powerlifting, Olympic Squat) are all absent. Integrity verified by
tests: **553 unique slugs/names, all imageRefs resolve, image folders 1:1 with slugs (0 orphans)**.

## 14. APK size before → after
| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 94,890,529 B | 95,408,678 B | +518,149 B (+0.49 MB) |
| Release | 87,195,572 B | 87,789,124 B | +593,552 B (+0.57 MB) |

## 15. Remaining DB-5C buckets after DB-5D
| Bucket | Count | Status |
|---|---|---|
| SETS_REPS_WEIGHT | 39 | **5 imported (DB-5D)**, 34 deferred (25 dup, 9 review-skip) |
| VELOCITY_POWER | 25 | deferred — needs engine (technical/velocity mode) |
| TIME_DISTANCE_LOAD | 8 | deferred — needs engine (time/distance fields) |
| DISTANCE_LOAD | 1 | deferred — needs engine (distance field) |
| BODYWEIGHT_SKILL | 18 | deferred — engine-compatible (BODYWEIGHT), curation hold |
| SKILL_FORM | 5 | deferred — engine-compatible (SKILL_FORM), curation hold |
| UNSUPPORTED_FOR_NOW | 2 | stays out of scope |

## 16. Message to the Engine owner

> **DB-5D done — catalog at 553. No engine work was required or performed.** The 5 imported entries log
> with the existing reps/kg/RIR model (`COMPOUND_LOAD`). The engine is still unchanged.
> The deferred MAYBE buckets still need engine support before they can be imported (unchanged from
> DB-5C): **(a)** Olympic/power lifts (25) need a non-RIR **technical/velocity mode** (at minimum route
> like `SKILL_FORM` — never suggest grinding O-lifts to RIR 0); **(b)** sleds (8) and carries (1) need
> **time/distance log fields** + a non-rep progression. The 23 BODYWEIGHT_SKILL/SKILL_FORM are
> engine-compatible today (existing `BODYWEIGHT`/`SKILL_FORM` types) and are a curation, not engine,
> decision. Any future mode must stay additive: no Room schema break, no change to existing V3.7A/V3.6B
> rules, RIR-0 still blocks increases, bodyweight/skill never auto +kg. Full contract:
> `docs/db5c_maybe_tracking_contract.csv`.

## 17. Rollback plan
- **Catalog:** restore `catalog_fed_v1.json` to its 548-entry state and revert `CONTENT_VERSION` 12→11 —
  the catalog table re-seeds on next launch (reference data, independent of user data).
- **Images:** delete the 5 new `<fed-slug>` folders (`board-press`, `pin-presses`, `kneeling-squat`,
  `seated-good-mornings`, `svend-press`); re-derivable from the FED dataset. No pre-existing folders changed.
- **Tests:** revert `CatalogSearchTest.kt` (553→548, remove the DB-5D test).
- **README:** remove the "FED catalog growth (DB-5B / DB-5D)" note.
- No History/Plan/schema/engine/signing changes to undo. Existing backup of `app/src` + `docs` at
  `D:\HOCTIN\GYM_TRACKER_DB4B2_BACKUP_20260620_123717` (and the UI-2A backup) remain available.

## Hard-stop check
None triggered: JSON valid, every import has 2 resolving images, no duplicate slug/name, all imageRefs
resolve, no Room schema change, no engine change, no Log Workout UI change, no non-SETS_REPS_WEIGHT entry
needed, build+tests green (no fixes required).
