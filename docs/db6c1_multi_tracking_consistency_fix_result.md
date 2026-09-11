# DB-6C.1 - Multi-Tracking UI Consistency Fix

## 1. Root Cause Analysis

DB-6C correctly added `trackingType`-driven Log fields and per-set storage, but several summary and target surfaces still assumed the old reps/kg model. Those surfaces aggregated all sets as if every set had meaningful reps and kg-volume, so a valid TIME_ONLY set using `reps = 0` / `weight = 0` as storage sentinels could leak into UI as empty-looking work.

The fix was to move display decisions into shared formatter helpers and make downstream screens consume stored `durationSeconds` / `distanceMeters` instead of inferring work from reps and weight alone.

## 2. Files Changed

- `app/src/main/java/com/hoctin/gymtracker/ui/format/Measurement.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/format/SetDisplay.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogTrackingLookup.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/ExerciseCatalogRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/Entities.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/WorkoutDraftRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/history/HistoryViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/history/HistoryScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/WeeklyProgressHubSheet.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/exercise/ExerciseDetailViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/exercise/ExerciseDetailScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogPickerScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/format/MeasurementFormatterTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/data/catalog/CatalogThumbnailLookupTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/data/MultiTrackingFoundationTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/log/LogWorkoutTrackingInputTest.kt`
- `README.md`

No Progression Engine logic, catalog JSON/assets, signing secrets, or Room schema version were changed in DB-6C.1.

## 3. Duration Input Behavior

- TIME_ONLY duration fields now show placeholder `Duration` with suffix `mm:ss`.
- Accepted duration inputs:
  - `55` -> 55 seconds -> displays as `00:55`
  - `1:30` -> 90 seconds -> displays as `01:30`
  - `01:30` -> 90 seconds -> displays as `01:30`
- Invalid colon values such as `1:99` and `1:02:99` are rejected instead of silently normalizing to a confusing value.
- The same parsing helper is used by the save path and the review summary path.

## 4. Review Workout Behavior

The Review Workout sheet now summarizes each exercise through `SetDisplay.summaryLine(...)`.

- TIME_ONLY rows show sets plus total duration, for example `3/3 sets - 02:45 duration`.
- Duration-only workouts show meaningful metric cards: Sets and Duration.
- Mixed workouts retain Reps and Volume for rep exercises and add Duration / Distance totals when present.
- Non-rep sets do not show fake `0 reps` or fake `0 kg` as the primary representation.

## 5. History Behavior

History aggregation now carries duration and distance through `HistoryViewModel`.

- TIME_ONLY exercise summaries include total duration.
- Expanded TIME_ONLY set rows show `Set n` plus formatted duration and a `duration` label where helpful.
- Week/session badges omit kg-volume when there are no rep-based sets, while still counting time-based sets as completed sets.

## 6. Weekly / Home Behavior

Weekly Progress Hub aggregation now carries duration and distance through `HomeViewModel`.

- Time-based sets count toward sets and session completion.
- kg-volume remains strictly weight x reps for rep-based sets.
- Duration and distance badges appear where there is room.
- Duration-only sessions no longer appear as fake zero-volume rep workouts.

## 7. Formatter / Helper Changes

- `MeasurementFormatter.parseDurationSeconds(...)` now supports plain seconds and strict `mm:ss` / `h:mm:ss`.
- `SetDisplay` now owns:
  - non-rep detection from stored duration/distance fields
  - aggregate set totals
  - summary lines for review/history/weekly/home
  - tracking-aware target labels for Plan, View Detail, Exercise Detail, and Catalog rows
- `CatalogTrackingLookup` centralizes exact-name/alias -> `TrackingType` lookup so plan/detail surfaces can avoid misleading rep targets when catalog metadata is available.

## 8. Tests / Build Results

Final validation completed after the catalog-picker target-label patch:

- `.\gradlew.bat :app:testDebugUnitTest` - BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug` - BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleRelease` - BUILD SUCCESSFUL

Build environment:

- `JAVA_HOME=D:\jdk17\jdk-17.0.19+10`
- `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`
- `ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk`

APK outputs from the final pass:

- Debug APK: `app\build\outputs\apk\debug\app-debug.apk` - 97,325,704 bytes
- Release APK: `app\build\outputs\apk\release\app-release.apk` - 89,474,174 bytes

New/updated coverage includes duration parsing, `SetDisplay` sentinel hiding, TIME_ONLY review summary formatting, mixed duration+rep summary formatting, tracking target labels, catalog tracking lookup, export/import duration round-trip, draft duration round-trip, and shared Log Workout input parsing.

## 9. Runtime QA Result

Runtime QA completed on `Pixel_8` / `emulator-5554` with the final debug APK:

- Emulator booted successfully and installed the updated debug APK with `adb install -r`.
- Home selected Push and still rendered the existing rep workout path normally.
- Plan Push showed Plank as `3 sets` plus `duration`, not a rep target.
- Workout Detail preserved existing rep rows such as `3 sets - 8-12 reps`.
- Log Workout preserved rep exercise inputs (`REPS`, `KG`, `RIR`) unchanged.
- Plank rendered `Duration tracking`, previous chips such as `00:15 duration`, duration placeholder `Duration`, and suffix `mm:ss`.
- Entered Plank sets `00:55`, `01:00`, and `01:05`; the collapsed Plank row showed `3/3 sets - 03:00 duration`.
- Draft recovery was verified by waiting for the draft debounce, force-stopping the app, relaunching, navigating back to Push, and confirming Plank restored as `Done` with `3/3 sets - 03:00 duration`.
- Review Workout showed summary cards `3 SETS` and `03:00 DURATION`; the Plank row showed `3/3 sets - 03:00 duration` and did not show `0 reps` or `0 kg`.
- Saving landed on History. The latest session showed `3 sets`, `03:00 duration`, and `1 exercises`.
- Expanded History showed Plank `3 sets - 03:00 duration`, then Set 1 `00:55 duration`, Set 2 `01:00 duration`, and Set 3 `01:05 duration`.
- History weekly summary showed `4 workouts - 8 sets - 07:25 duration`, proving time-based sets count while kg-volume is not faked.
- Home advanced to the next workout and did not misrepresent the duration-only saved work as fake 0 kg work.
- Weekly Progress Hub Recent Sessions showed the latest Push session as `3 sets`, `03:00 duration`, and `1 exercises`.
- Crash buffer check returned no `com.hoctin.gymtracker` entries.

Export/import duration preservation and draft serialization are covered by unit tests; runtime QA exercised the draft persistence path through process restart and the saved database-backed history path.

## 10. Remaining Limitations

- TIME_LOAD, DISTANCE_LOAD, TIME_DISTANCE, and TIME_DISTANCE_LOAD remain future-reachable until DB-6E imports exercises using those tracking types.
- Non-rep PR/progression scoring is intentionally unchanged; this task only fixes display, parsing, storage round-trip, and summaries.

## 11. Rules For DB-6E Imports

- Every new imported exercise must set the correct `trackingType`; do not rely on `progressionType` to infer input fields.
- Non-rep displays must go through `SetDisplay` / `MeasurementFormatter`; do not hand-build `kg x reps` strings for time/distance/load types.
- `reps = 0` and `weight = 0` are storage sentinels for non-rep sets, never user-facing work summaries.
- kg-volume remains weight x reps only; time/distance sets count as completed sets/sessions but do not create fake volume.
- Export/import schema v2 must continue writing and reading `durationSeconds` and `distanceMeters`.
- Draft serialization must keep duration/distance fields whenever the set's tracking type exposes them.
- Plan, View Detail, Catalog, Log, Review, History, and Weekly surfaces must all use the same tracking-aware target/summary helpers before importing new time/distance exercises.

## Backup

Pre-work backup:

```text
D:\HOCTIN\GYM_TRACKER_DB6C1_BACKUP_20260627_083948
```
