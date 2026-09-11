# DB-6E Time / Distance / Load Exercise Import Result

## Scope

DB-6E imports the FED carry/sled exercises that were intentionally deferred until the DB-6B/DB-6C
tracking foundation could represent load, distance, and duration without fake reps.

This pass stayed data-focused:

- Imported only exercises whose real tracking can be represented by existing `TrackingType` values.
- Reused the existing DB-6B schema and DB-6C display path.
- Did not modify Progression Engine logic.
- Did not modify the Room schema.
- Did not touch signing secrets.

Backup created before edits:

`D:\HOCTIN\GYM TRACKER\_backup_db6e_time_distance_import_20260630_113108`

## Prerequisite State Confirmed

Before import, DB-6C.1 and DB-6C.2 were present and documented:

- `docs/db6c1_multi_tracking_consistency_fix_result.md`
- `docs/db6c2_duration_input_polish_result.md`

The app already supported tracking-aware non-rep display through `MeasurementFormatter` and
`SetDisplay`, duration input normalization, export/import v2 duration/distance fields, and
tracking-type-driven Log Workout fields.

## Candidate Selection

Source candidate file:

- `docs/db5e_missing_fed_coverage.csv`

FED source file:

- `external_catalog_sources/free-exercise-db-main/free-exercise-db-main/dist/exercises.json`

Supported DB-6E candidates found: **9**.

Imported count: **9**.

Duplicate / review-skip / reject count within the supported DB-6E slice: **0**.

Existing unrelated DB-5E exclusions remain out of scope. This pass did not import Olympic/unsupported
or already-rejected exercises.

## Imported Exercises

| Exercise | Slug | TrackingType | ProgressionType | Equipment | Force |
| --- | --- | --- | --- | --- | --- |
| Backward Drag | `backward-drag` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PULL` |
| Bear Crawl Sled Drags | `bear-crawl-sled-drags` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PUSH` |
| Farmer's Walk | `farmers-walk` | `DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `CARRY` |
| Sled Drag - Harness | `sled-drag-harness` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PUSH` |
| Sled Overhead Backward Walk | `sled-overhead-backward-walk` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PULL` |
| Sled Overhead Triceps Extension | `sled-overhead-triceps-extension` | `TIME_DISTANCE_LOAD` | `ISOLATION_SLOW` | `OTHER` | `PUSH` |
| Sled Push | `sled-push` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PUSH` |
| Sled Reverse Flye | `sled-reverse-flye` | `TIME_DISTANCE_LOAD` | `ISOLATION_SLOW` | `OTHER` | `PULL` |
| Sled Row | `sled-row` | `TIME_DISTANCE_LOAD` | `COMPOUND_LOAD` | `OTHER` | `PULL` |

All imported entries use:

- `defaultSets = 3`
- `defaultMinReps = 0`
- `defaultMaxReps = 0`
- two image refs: `exercise_images/<slug>/0.jpg` and `exercise_images/<slug>/1.jpg`

The zero min/max reps are storage/target sentinels for non-rep exercises. User-facing targets are
rendered from `trackingType`, for example `3 sets · load + distance + duration`, not from fake reps.

## Catalog And Assets

Catalog:

- Before: **568** exercises.
- After: **577** exercises.
- `CatalogSeed.CONTENT_VERSION`: **15 -> 16**.

Exercise image folders:

- Before: **568** folders.
- After: **577** folders.
- Added 18 image files total, two per imported exercise.

Validated current catalog tracking distribution:

- `REPS_WEIGHT`: 471
- `BODYWEIGHT_REPS`: 87
- `SKILL_FORM`: 8
- `TIME_ONLY`: 2
- `TIME_DISTANCE_LOAD`: 8
- `DISTANCE_LOAD`: 1

Catalog slug uniqueness and the new import metadata are covered by unit tests.

## Schema / Engine Status

Room schema:

- No Room schema change.
- Database version remains the DB-6B version.
- Export/import schema remains v2.

Progression Engine:

- No Progression Engine logic changed.
- The imported non-rep exercises use catalog metadata only; kg-volume remains `weight x reps` for
  rep-based work.

Signing:

- Signing secrets were not read or changed.

## Tests And Builds

Passed:

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`

Unit-test result XML:

- 7 result files
- 133 tests
- 0 failures
- 0 errors
- 0 skipped

Build artifacts:

- Debug APK: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk`
  - 97,907,077 bytes
  - Last written: June 30, 2026 11:43:36 AM
- Release APK: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\release\app-release.apk`
  - 90,294,574 bytes
  - Last written: June 30, 2026 11:46:05 AM

Notes:

- Gradle emitted the existing compileSdk warning for AGP 8.5.2 and compileSdk 35.
- Release build emitted the existing kapt warning for unrecognized Room/KAPT options.
- Both warnings were non-fatal and pre-existing for this checkout.

## Runtime QA

Device:

- Pixel 8 emulator, `emulator-5554`

Flow verified:

1. Installed the DB-6E debug APK with `adb install -r`.
2. Confirmed existing TIME_ONLY History rows still render as duration rows.
3. Added Sled Push and Farmer's Walk from the catalog picker.
4. Verified catalog/plan/detail target labels:
   - Sled Push: `3 sets · load + distance + duration`
   - Farmer's Walk: `3 sets · load + distance`
5. Started Push and confirmed a normal rep exercise still exposes unchanged `REPS`, `KG`, and `RIR`
   fields.
6. Expanded Sled Push and confirmed the Log card exposes `Load`, `Dist`, and `Duration` fields with no
   `REPS` or `RIR` controls.
7. Entered one Sled Push set:
   - load: `80 kg`
   - distance: `100 ft` display setting
   - duration: `40`, saved/displayed as `00:40`
8. Review Workout showed the imported Sled Push row as:
   - `1/3 sets · 00:40 duration · 100 ft distance`
   - no fake reps or fake kg-volume
9. Saved the workout.
10. History showed the saved Push session with:
    - `5 sets`
    - `01:32:25 duration`
    - `100 ft distance`
11. Expanded History showed Sled Push as:
    - exercise summary: `1 set · 00:40 duration · 100 ft distance`
    - set row: `80 kg • 100 ft • 00:40 duration`
12. Crash buffer check found no `com.hoctin.gymtracker` entries.

Runtime QA note: repeated manual add taps during catalog probing created duplicate Sled Push and
Farmer's Walk rows in the local emulator Push plan. This is emulator data only. Source catalog entries
remain unique and are covered by tests.

## Known Limitations

- Aggregated non-rep exercise summary lines show additive totals such as duration and distance. Load is
  not summed in aggregate summary text because summing working loads across sets is misleading; the
  saved per-set History row shows the actual load.
- DB-6E does not add non-rep progression scoring. That remains intentionally outside this import.
- Only supported carry/sled entries were imported. Unrelated DB-5E rejected/unsupported rows remain
  deferred.

## Prevention Rules For Future Non-Rep Imports

- Every imported non-rep exercise must declare its exact `trackingType`; never infer fields from
  `progressionType`.
- Target text must use `SetDisplay.targetLabel(...)`.
- Summary text must use `SetDisplay.summaryLine(...)` / `SetDisplay.nonRepInline(...)`.
- Duration parsing must stay centralized in `MeasurementFormatter.parseDurationSeconds(...)`.
- User-facing rows must never show non-rep sentinels as `0 reps`, `0 kg`, or fake kg-volume.
- Distance display must route through `MeasurementFormatter.distance(...)`; the database stores meters,
  while Settings controls metric/imperial display.
- Export/import and draft serialization must preserve `durationSeconds` and `distanceMeters` for any
  tracking type that exposes those fields.
- Future imports must QA at least Catalog, Plan/View Detail, Log, Review, History, and weekly/session
  summaries before increasing `CONTENT_VERSION`.
