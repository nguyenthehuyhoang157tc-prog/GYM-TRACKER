# MUSCLE-2 — Body Region Mapping + Muscle Heat UI Contract — Result

**Status:** COMPLETE. Deterministic mapping/UI-contract layer only — **no body-map screen, no image/vector
assets, no Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change,
no exercise imports, no catalog change.** All runtime-only.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE2_BACKUP_20260702_190441\app_src`.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE2_BACKUP_20260702_190441\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleBodyRegion.kt` | **NEW** — `BodyRegionSide`, `BodyRegionId` (17 regions w/ displayName+side), `MuscleRegionMapping` (Mapped / SummaryOnly), `MuscleBodyRegions.mappingFor` (exhaustive `when` over all 24 muscles) + `regionsFor`. |
| `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleHeatUi.kt` | **NEW** — `MuscleHeatColorToken`, `MuscleHeatUiRegion`, `MuscleHeatSummaryItem`, `MuscleHeatUiModel`, `MuscleHeatUiMapper` (band→token, merge, full pipeline). |
| `app/src/test/java/com/hoctin/gymtracker/domain/muscleheat/MuscleBodyRegionMappingTest.kt` | **NEW** — 16 deterministic pure-JVM tests. |
| `README.md`, `docs/muscle2_body_region_mapping_result.md` | Docs. |

Untouched: Room schema (v6), `ProgressiveOverload.kt`, `MuscleHeatEngine.kt` / `MuscleExposure.kt` scoring
(MUSCLE-1), all UI, catalog JSON/assets, signing.

## 3. Actual MuscleGroup enum count
**24** values (verified from `data/catalog/CatalogTaxonomy.kt` and asserted in test
`exactly24MuscleGroups_and8AreUnusedByCatalog`): CHEST, UPPER_CHEST, BACK, LATS, MID_BACK, LOWER_BACK,
TRAPS, SHOULDERS, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS,
GLUTES, CALVES, ABS, OBLIQUES, CORE, ADDUCTORS, ABDUCTORS, FULL_BODY.

## 4. Actual catalog primary/secondary muscle usage (from `catalog_fed_v1.json`, 577 entries)

**16 muscles are used** — the **same set for primary and secondary**: ABS, ABDUCTORS, ADDUCTORS, BICEPS,
CALVES, CHEST, FOREARMS, GLUTES, HAMSTRINGS, LATS, LOWER_BACK, MID_BACK, QUADS, SHOULDERS, TRAPS, TRICEPS.

Top counts — **primary:** SHOULDERS 103, ABS 76, CHEST 66, QUADS 66, TRICEPS 61, BICEPS 49, HAMSTRINGS 31,
MID_BACK 28, LATS 23, FOREARMS 21, CALVES/GLUTES 14, TRAPS 13, LOWER_BACK 8, ABDUCTORS/ADDUCTORS 2.
**Secondary:** SHOULDERS 146, TRICEPS 115, GLUTES 113, HAMSTRINGS 101, CALVES 83, LOWER_BACK 59, BICEPS 55,
FOREARMS 50, MID_BACK 40, LATS 39, CHEST 37, QUADS 36, ABS 29, TRAPS 42, ADDUCTORS 5, ABDUCTORS 2.

**8 enum values are NOT used by the catalog** (still fully mapped for custom exercises / future data):
UPPER_CHEST, BACK, FRONT_DELTS, SIDE_DELTS, REAR_DELTS, OBLIQUES, CORE, FULL_BODY. The catalog uses the
aggregate **SHOULDERS** (not the delt heads), **CHEST** (not UPPER_CHEST), **ABS** (not CORE/OBLIQUES),
and the specific back muscles (not generic **BACK**).

## 5. Mapping design

**17 body regions** (`BodyRegionId`, each with `displayName` + `BodyRegionSide` FRONT/BACK/BOTH):
CHEST, SHOULDERS(BOTH), BICEPS, TRICEPS, FOREARMS(BOTH), ABS, OBLIQUES, TRAPS(BOTH), LATS, MID_BACK,
LOWER_BACK, QUADS, HAMSTRINGS, GLUTES, CALVES, ADDUCTORS, ABDUCTORS(BOTH).

`MuscleBodyRegions.mappingFor(muscle)` is an **exhaustive `when`** over `MuscleGroup` (no `else`), so a new
enum value fails to compile until mapped — the strongest possible coverage guarantee. Every value returns
`Mapped(non-empty regions)` or `SummaryOnly(reason)`:
- CHEST/UPPER_CHEST → CHEST · BACK → LATS+MID_BACK (broad fallback) · LATS/MID_BACK/LOWER_BACK/TRAPS → self
- SHOULDERS/FRONT_DELTS/SIDE_DELTS/REAR_DELTS → SHOULDERS · BICEPS/TRICEPS/FOREARMS → self
- QUADS/HAMSTRINGS/GLUTES/CALVES/ADDUCTORS/ABDUCTORS → self · ABS/OBLIQUES → self · CORE → ABS
- **FULL_BODY → SummaryOnly** (never blindly paints the whole silhouette).

## 6. Unsupported / summary-only groups
Only **FULL_BODY** is summary-only (documented reason). It (and any future summary-only value) is surfaced
in `MuscleHeatUiModel.summaryOnly` — **visible** with its band/token/status/exercises, but **never painted
as a fake body region**. Custom exercises with no catalog anatomy flow to
`MuscleHeatUiModel.unattributedExercises` (never dropped, never crash). Generic/unknown handling can't crash:
the `when` is total and `regionsFor` returns empty for summary-only.

## 7. Merge rule
Multiple muscles → one region (e.g. SHOULDERS + the 3 delt heads → the shoulder region): the **max-intensity
muscle is dominant**. `colorBand` / `colorToken` / `intensity` and `status` / `reason` all follow that
dominant muscle (keeping the exposure and meaning axes mutually consistent); **all** contributing muscles
and exercises are preserved for explanation. Ties break deterministically by `MuscleGroup` declaration
order; region output is sorted by `BodyRegionId` declaration order (stable regardless of input order —
tested).

## 8. Color token contract
Centralized `MuscleHeatColorToken` (composables resolve to theme colors later — **no hardcoded colors**):
`NONE → HEAT_NONE`, `LIGHT_YELLOW → HEAT_LIGHT`, `YELLOW_ORANGE → HEAT_MODERATE`, `ORANGE_RED → HEAT_HIGH`,
`DEEP_RED → HEAT_MAX`. `MuscleHeatUiMapper.colorTokenFor` is an exhaustive `when` over `HeatColorBand`.

## 9. Tests added / counts
**16 new** deterministic pure-JVM cases in `MuscleBodyRegionMappingTest` — all green:
every-muscle-covered, exact-24-count + 8-unused, catalog-used-primary/secondary all map to regions, every
band→token, FULL_BODY summary-only, CORE→ABS overlap, BACK broad fallback, SHOULDERS + all delt heads,
FOREARMS visible, ADDUCTORS/ABDUCTORS distinct, UPPER_CHEST→CHEST, multi-muscle merge (max intensity +
preserve contributors), deterministic region order, full `SessionMuscleHeat`→UI with contributingExercises,
custom unattributed visibility, and empty-input no-crash. (MUSCLE-1's 20 tests remain green.)

## 10. Debug build result
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0** (all suites). `:app:assembleDebug` → **exit 0**,
`app-debug.apk` **93.75 MB**. (Release not run — not needed.)

## 11. Confirmation of scope
✅ No Room schema change. ✅ No Progressive Overload Engine change. ✅ No MuscleHeatEngine scoring change
(no bug found). ✅ No body-map screen, no image/vector assets. ✅ No exercise imports, no catalog change,
signing untouched. ✅ PO Engine data NOT guessed — none is referenced here (it stays optional future
metadata on the MUSCLE-1 contributor, not consumed by this layer).

## 12. Known limitations
- Body regions are a **logical** set (17 IDs) — the actual silhouette drawable/hit-regions are MUSCLE-3's job;
  this layer just supplies stable region IDs + sides + tokens.
- `BACK` fallback maps to LATS+MID_BACK only (not TRAPS/LOWER_BACK) — a deliberate "main back mass" choice.
- Merge uses the dominant (max-intensity) muscle for status/reason; it does not synthesize a blended
  sentence across muscles (contributors are preserved so the UI can if desired).
- `contributingExercises` requires the full `SessionMuscleHeat` (exposure); the heat-only overload leaves it
  empty by design.
- Color tokens are semantic names; the actual theme colors are defined in MUSCLE-3.

## 13. Next step — MUSCLE-3
Build the after-save review/body-map UI: wire `SessionMuscleHeatUseCase.forSession(sessionId)` →
`MuscleHeatUiMapper.toUiModel(...)` into the post-save flow (`LogWorkoutEvent.Saved`), add the theme colors
for each `MuscleHeatColorToken`, render the front/back silhouette painting each `BodyRegionId` by its
`colorToken`, list top muscles + summary-only + unattributed, and show a runtime-computed session duration.
