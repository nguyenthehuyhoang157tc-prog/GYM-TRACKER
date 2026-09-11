# DB-5F — Final Catalog Coverage Polish — COMPLETED ✅

Two data-only parts: **(A)** search aliases for discoverability and **(B)** a final 15-entry import.
**No Room schema change, no engine change, no Log Workout input change, no History/Plan change.** Unit
tests + debug + release builds green. README updated.

## Headline
| Metric | Value |
|---|---|
| 1. Before catalog count | **553** |
| 2. Final catalog count | **568** |
| 4. Candidates reviewed (IMPORT_NOW 23 + IMPORT_WITH_FALLBACK 37) | **60** |
| 5. IMPORT_FINAL | **15** |
| 6. SKIP_LOW_VALUE | **42** |
| 7. WAIT_ENGINE | **0** |
| 8. REVIEW_MANUAL | **3** |
| 9. Image folders / files | 553 / 1106 → **568 / 1136** (+15 / +30) |
| 10. CONTENT_VERSION | **13 → 14** |
| 11. Room schema / `@Database version` | **4 (unchanged)** |
| 12. Engine | **not modified; engine tests pass** |

## 3. Alias edits (Part A) — 10 entries, 17 aliases (no new exercises)
Conservative, same-movement aliases only (verified none collides with an existing entry name):

| Entry | Aliases added |
|---|---|
| **Side Lateral Raise** | Dumbbell Lateral Raise, Lateral Raise, Dumbbell Side Raise |
| Pushups | Push-Up, Push Up, Standard Push-Up |
| Pullups | Pull-Up, Pull Up |
| Chin-Up | Chin Up, Chin-Ups |
| Seated Cable Rows | Seated Cable Row |
| Machine Shoulder (Military) Press | Machine Shoulder Press |
| Arnold Dumbbell Press | Arnold Press |
| Dips - Triceps Version | Triceps Dip, Parallel Bar Dip (same movement) |
| Side Bridge | Side Plank (same movement) |
| Plank | Forearm Plank (same movement) |

This directly fixes the reported pain: searching **"Dumbbell Lateral Raise"** / "Lateral Raise" now
resolves to **Side Lateral Raise** (verified by test). No misleading aliases, no duplicate exercises.

## 5. IMPORT_FINAL (15) — Part B
All log with the existing reps/kg/bodyweight model; **no new Log Workout fields, no engine change.**

- **Advanced calisthenics → `BODYWEIGHT` (7):** Muscle Up, Ring Dips, One Arm Chin-Up, Scapular Pull-Up,
  Single-Leg High Box Squat, Suspended Row, Suspended Push-Up.
- **Skill / olympic → `SKILL_FORM` fallback (8):** Overhead Squat, Plate Pinch, Snatch, Clean and Jerk,
  Push Press, Hang Clean, Hang Snatch, Power Snatch. *(SKILL_FORM = conservative: progress reps/technique,
  never auto-load; the olympic lifts log reps/kg today and get a safe non-RIR progression — no velocity
  field required for basic logging, per the DB-5C contract.)*

## 6/8. SKIP_LOW_VALUE (42) and REVIEW_MANUAL (3)
- **REVIEW_MANUAL (3):** Weighted Squat (vague name), Incline Inner Biceps Curl & Single Dumbbell Raise
  (overlap existing entries) — left for a human dedup decision.
- **SKIP_LOW_VALUE (42):** niche olympic variants (from-blocks / balance / split / behind-the-neck:
  Clean, Clean from Blocks, Power Clean from Blocks, Power Snatch from Blocks, Snatch from Blocks, Split
  Snatch/Clean/Jerk, Snatch/Jerk Balance, Heaving Snatch Balance, Jerk Dip Squat, Kneeling Jump Squat,
  Muscle Snatch, Power Jerk, Push Press - Behind the Neck, Rack Delivery, Hang Clean/Snatch - Below…),
  olympic pulls/dups (Clean Pull, Clean/Snatch Deadlift, RDL from Deficit, Wide Stance Stiff Legs, Axle
  Deadlift, Good Morning off Pins, Hanging Bar Good Morning, Chain Handle Extension), and niche
  calisthenics/grip/KB (Kipping Muscle Up, Rope Climb, Side To Side Chins, Rocky Pull-Ups, Mixed Grip
  Chin, Gironda Sternum Chins, London Bridges, Otis-Up, Suspended Fallout/Reverse Crunch/Split Squat,
  Kettlebell Figure 8, Double Kettlebell Windmill, Standing Olympic Plate Hand Squeeze).
- **7. WAIT_ENGINE: 0** — none of the 60 candidates need time/distance fields (those were already in the
  DB-5E `WAIT_FOR_TRACKING` bucket and excluded from this review).

## 13. Tests / builds
| Item | Result |
|---|---|
| `:app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Engine tests (within unit suite) | ✅ pass (engine files not edited) |

Catalog tests updated: count 553→**568** (size, blank-query, level-partition); new
`db5f_aliasesResolveToCanonicalEntries` (dumbbell-lateral-raise / lateral-raise → Side Lateral Raise;
push-up → Pushups; seated-cable-row → Seated Cable Rows; arnold-press → Arnold Dumbbell Press; +
alias-never-equals-a-name) and `db5f_finalImportsPresent_loadModelSafe_waitItemsAbsent` (15 imports
present, all `BODYWEIGHT`/`SKILL_FORM`, Farmer's Walk / Sled Push / sled drags absent). The DB-5B/DB-5D
"olympic absent" lists were updated (the foundational olympic lifts are now legitimately imported);
integrity (568 unique slugs/names, all imageRefs resolve, image folders 1:1 / 0 orphans) verified by tests.

## 14. APK size before → after
| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 95,561,741 B | 96,979,522 B | +1,417,781 B (+1.35 MB) |
| Release | 87,789,136 B | 89,441,290 B | +1,652,154 B (+1.58 MB) |

## 15. Remaining deferred exercises
- **WAIT_FOR_TRACKING (9, unchanged):** 8 sleds + 1 carry (Farmer's Walk) — need **seconds/distance** Log
  Workout fields + engine support. Not imported.
- **Niche/duplicate (skipped):** the 42 SKIP_LOW_VALUE above + the broader ~251 REJECT (out-of-scope
  stretching/cardio/plyometrics, neck, strongman events, accommodating-resistance duplicates).
- Per-exercise contract: `docs/db5c_advanced_metrics_contract.md`, `docs/db5e_missing_fed_coverage.csv`.

## 16. DB MVP status recommendation
**The DB MVP catalog phase is complete.** DB-5E confirmed **0 missing common strength exercises** and all
17 movement families have full coverage; DB-5F closed the discoverability gap (aliases) and added the
last batch of high-value advanced/olympic entries that fit the current logging model. The only remaining
work is genuinely **engine-blocked** (sled/carry time-distance tracking) — a future Engine/Log task, not a
catalog gap. No further catalog import is needed to ship the MVP.

## 17. README update summary
Updated the README section **"FED catalog growth & MVP coverage (DB-5B … DB-5F)"** to reflect: final
count **568** / `CONTENT_VERSION 14`; the DB-5F alias list + 15 imports; the DB-5E coverage status (0
missing common strength, all families covered); the deferred sled/carry/time-distance categories; and a
statement that the **DB MVP catalog phase is complete**.

## Confirmation
No Room schema/version, Progression Engine, Log Workout input fields, History, or signing secrets were
modified. Files changed: `catalog_fed_v1.json` (10 alias edits + 15 new entries), 30 new image files,
`CatalogSeed.kt` (13→14), `CatalogSearchTest.kt` (counts + new/updated tests), `README.md` (coverage
note), and this report.

## Rollback
Restore `catalog_fed_v1.json` + `CatalogSeed.kt` (CV 14→13) + `CatalogSearchTest.kt` + the README note;
delete the 15 new `<fed-slug>` image folders (re-derivable from the FED dataset). The catalog re-seeds on
next launch (reference data, independent of user data). No user-data/schema/engine changes to undo.
