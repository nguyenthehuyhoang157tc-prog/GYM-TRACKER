# DB-6C.2 Duration Input Auto-Format Polish Result

## Scope

DB-6C.2 polished TIME_ONLY duration entry before DB-6E imports time/distance exercises. The change was
limited to duration parsing/normalization and Log Workout field blur behavior.

No Progression Engine logic, Room schema, catalog assets, exercise imports, or tracking type
definitions were changed. DB-6E was not started.

Backup created before edits:

`D:\HOCTIN\GYM TRACKER\_backup_db6c2_duration_input_20260630_001506`

## Root Cause

DB-6C.1 made TIME_ONLY display coherent, but duration entry still made users type separators manually.
The parser also treated some digits as plain seconds, which did not support predictable right-to-left
duration entry for longer values.

## Files Changed

- `app/src/main/java/com/hoctin/gymtracker/ui/format/Measurement.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/format/MeasurementFormatterTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/log/LogWorkoutTrackingInputTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/data/MultiTrackingFoundationTest.kt`
- `README.md`
- `docs/db6c2_duration_input_polish_result.md`

## Behavior

Duration parsing is centralized in `MeasurementFormatter.parseDurationSeconds`.

Accepted colon input:

- `1:30` -> 90 seconds
- `01:30` -> 90 seconds
- `1:00:00` -> 3600 seconds

Accepted digits-only input uses right-to-left `HHMMSS` grouping:

- `3` -> `00:03`
- `55` -> `00:55`
- `130` -> `01:30`
- `3012` -> `30:12`
- `010000` -> `01:00:00`

Invalid values are rejected safely, including non-digits, overlong digit strings, blank input, and
minutes/seconds outside `0..59`.

Display format remains `MM:SS` below one hour and uses padded `HH:MM:SS` at one hour or more. Existing
saved duration rendering still flows through `MeasurementFormatter.duration` and `SetDisplay`.

## Log Input

Duration fields keep the existing `Duration` placeholder and `mm:ss` suffix. Typing is not reformatted
on every keypress, so keyboard behavior remains stable. When focus leaves a duration field, valid input
normalizes in place. If a field is still focused at save time, the save/review/history path parses the
raw value and displays normalized duration.

## Tests And Builds

Passed:

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`

Build artifacts:

- Debug: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk` (97,327,138 bytes)
- Release: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\release\app-release.apk` (89,474,410 bytes)

Notes: Gradle emitted the existing compileSdk warning for AGP 8.5.2 and the existing kapt warning for
unrecognized Room/KAPT options. They did not fail the requested gates.

## Runtime QA

Device: Pixel 8 emulator, `emulator-5554`.

Flow:

1. Installed `app-debug.apk`.
2. Opened Home -> Push -> View Detail -> Start Workout.
3. Expanded Plank TIME_ONLY.
4. Entered duration values:
   - `3` normalized to `00:03`.
   - `130` normalized to `01:30`.
   - `3012` normalized to `30:12`.
   - `010000` was saved from focused raw input and rendered as `01:00:00`.
5. Saved workout.
6. Review showed:
   - Summary duration `01:31:45`.
   - Plank row `4/3 sets · 01:31:45 duration`.
7. History showed:
   - This Week summary `1 workout · 4 sets · 01:31:45 duration`.
   - Session summary `4 sets` and `01:31:45 duration`.
   - Expanded Plank rows:
     - Set 1 `00:03 duration`
     - Set 2 `01:30 duration`
     - Set 3 `30:12 duration`
     - Set 4 `01:00:00 duration`
8. Crash buffer check found no `com.hoctin.gymtracker` entries.

Review and History remained tracking-aware and did not show fake `0 reps` / `0 kg` for Plank.

## DB-6E Prevention Rules

- Do not import time/distance exercises until they can use the centralized `TrackingType` display path.
- Do not add per-screen duration parsing or formatter copies.
- Route duration input through `MeasurementFormatter.parseDurationSeconds` and normalized display through
  `MeasurementFormatter.duration`.
- Keep non-rep sentinel values hidden from user-facing rows and summaries.
- Keep kg-volume as reps x weight only; duration/distance must count completed sets and show their own
  tracking totals where the UI has room.

## Remaining Notes

The only runtime-reachable non-rep type remains TIME_ONLY. DB-6E imports are intentionally deferred.
