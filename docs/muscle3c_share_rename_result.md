# MUSCLE-3C - Share Heat Card + Rename Session Result

## Status

Completed in `D:\HOCTIN\GYM TRACKER`.

MUSCLE-3C wires the two circular actions on Muscle Heat Review:

- Share captures only the heat card and opens Android's share sheet with a cache-backed `content://` URI.
- Edit renames a saved workout/session title and persists it through Room.

No Progressive Overload Engine logic, MuscleHeatEngine scoring, exercise imports, catalog JSON/assets,
signing secrets, female figure work, per-region tap behavior, or MUSCLE-4 recovery-map work was changed.

## Backup

Created before edits:

`D:\HOCTIN\GYM_TRACKER_MUSCLE3C_BACKUP_20260704_100843`

Backup contents:

- `app_src`
- `app_schemas`

## Pre-Edit Verification

- Repo root confirmed as `D:\HOCTIN\GYM TRACKER`.
- `README.md` and the latest MUSCLE/HISTORY reports were read before implementation:
  - `docs/muscle3b1_heat_review_duration_backmap_fix_result.md`
  - `docs/muscle_heat1_stimulus_quality_color_result.md`
  - `docs/history2_workout_archive_redesign_result.md`
  - `docs/muscle3_after_save_heat_review_ui_result.md`
  - `docs/muscle3b_heat_review_ui_polish_result.md`
- Baseline Room database version was v7.
- `SessionReviewScreen` and the MUSCLE heat card existed in `ui/review`.
- History and Home Weekly Progress Hub session rows already opened Muscle Heat Review directly.
- The circular Share/Edit actions were placeholders/no-op before this task.

## Subagent Summaries

Context agent (`gpt-5.4-mini`, reasoning MED, Descartes) reported:

- Current architecture is `GymRepository.sessionWithSets -> SessionMuscleHeatUseCase ->
  MuscleExposureCalculator -> MuscleHeatEngine -> MuscleHeatUiMapper -> SessionReviewViewModel /
  SessionReviewScreen`.
- `SessionReviewScreen` lives in `app/src/main/java/com/hoctin/gymtracker/ui/review/`.
- History-2 changed History rows and Home Weekly Progress Hub rows to open `sessionReview` directly.
- Share/rename risks were: preserve card-only capture, keep History-2 navigation intact, and persist rename
  without mutating the historical `dayNameSnapshot`.

Implementation owner (`gpt-5.5` main, reasoning EXTRA HIGH, current Codex thread) implemented the scoped
changes and ran runtime QA.

QA agent (`gpt-5.4`, reasoning LOW, Kierkegaard) found no blocking regressions:

- Share uses heat-card bounds, app cache, `FileProvider`, `ACTION_SEND`, and a read-grant URI.
- Rename persists through DAO/repository/ViewModel, not local UI state only.
- Room v8 migration is additive; exported schema v8 exists.
- `sessionDisplayTitle(...)` centralizes renamed-title fallback for History/Home/Review.
- History-2 and after-save review flow are preserved.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed.

Report agent (`gpt-5.4-mini`, reasoning MED, Galileo) drafted this report after implementation and QA pass.

## Files Changed

Core app:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/main/java/com/hoctin/gymtracker/data/Entities.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/AppDatabase.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/history/HistorySessionMapper.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/review/SessionReviewScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/review/SessionReviewViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/review/HeatCardShare.kt`
- `app/schemas/com.hoctin.gymtracker.data.AppDatabase/8.json`

Tests:

- `app/src/test/java/com/hoctin/gymtracker/data/SessionRenameFoundationTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/history/SessionDisplayTitleTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/review/HeatCardShareTest.kt`

Docs:

- `README.md`
- `docs/muscle3c_share_rename_result.md`

## Share Implementation

`HeatCardShare.kt` isolates the share flow:

- Uses `LocalView.drawToBitmap(...)` for the current review root.
- Tracks the `HeatCard` via `Modifier.onGloballyPositioned { boundsInRoot() }`.
- Crops the root bitmap to only those heat-card bounds.
- Writes a PNG into `context.cacheDir/shared_heat_cards`.
- Uses `FileProvider.getUriForFile(...)` with authority `${applicationId}.fileprovider`.
- Starts an `Intent.ACTION_SEND` chooser with `type = image/png`, `EXTRA_STREAM`, and
  `FLAG_GRANT_READ_URI_PERMISSION`.
- Deletes old cached heat-card PNGs older than 24 hours when sharing.

The FileProvider is declared in `AndroidManifest.xml`, and `res/xml/file_paths.xml` exposes only the
`shared_heat_cards/` cache path.

Runtime QA pulled the generated image from app cache as `qa_muscle3c_shared_heat_card.png`; visual
inspection confirmed it contains only the heat card, with no header, circular buttons, bottom nav, or
exercise breakdown.

## Rename Implementation

Rename is persisted, not UI-only:

- `WorkoutSessionEntity` now has nullable `customTitle: String? = null`.
- `GymDao.updateSessionCustomTitle(sessionId, customTitle)` writes the title.
- `GymRepository.renameWorkoutSession(sessionId, title)` trims input and writes null for blank values.
- `SessionReviewViewModel.renameSession(...)` calls the repository and reloads the session.
- `SessionReviewScreen` opens a rename dialog from the circular Edit action and updates the title.

Display fallback is centralized:

- `sessionDisplayTitle(session)` returns nonblank `customTitle`, else `dayNameSnapshot`.
- Review uses `historySessionOf(...)`, so the header reflects the saved title after reload.
- History and Home Weekly Progress Hub both reuse the same mapping.
- Old sessions with `customTitle = null` keep the existing title behavior.

## Migration / Export / Import

Room:

- Database version is now v8.
- `MIGRATION_7_8` uses additive `ALTER TABLE workout_sessions ADD COLUMN customTitle TEXT`.
- Existing rows keep `NULL`, so old sessions continue to show `dayNameSnapshot`.
- Schema `8.json` was exported and includes nullable `customTitle TEXT`.

Export/import:

- Export schema is now JSON `schemaVersion = 5`.
- `workoutSessions` includes optional `customTitle`.
- Import remains tolerant of older exports where `customTitle` is absent.
- Blank imported custom titles normalize to null.

No destructive migration or destructive fallback was added.

## Tests

Added/updated focused unit coverage:

- Rename foundation: default null title, old-session fallback shape, export/import roundtrip, missing field
  import, blank-title normalization.
- Display title mapping: custom title wins, old sessions fall back to `dayNameSnapshot`.
- Share helper: stable share filename and cleanup of expired cached PNGs.

Final command:

```powershell
$env:JAVA_HOME='D:\jdk17\jdk-17.0.19+10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --no-daemon --console=plain :app:testDebugUnitTest :app:assembleDebug
```

Result:

- `BUILD SUCCESSFUL in 2m 44s`
- `46 actionable tasks: 14 executed, 32 up-to-date`

Only known build warning was the existing Android Gradle Plugin warning for `compileSdk = 35` with AGP
8.5.2 tested up to compileSdk 34.

## Runtime QA

Device:

- `emulator-5554`
- `Pixel_8`

QA performed:

1. Installed debug APK and launched the app.
2. Opened History and selected a saved session review.
3. Confirmed Review loaded title, stored duration, heat card, Share/Edit actions, and exercise breakdown.
4. Tapped Share.
5. Confirmed Android share sheet opened with image sharing.
6. Pulled generated PNG from app cache and verified it is only the heat card.
7. Cancelled share and returned safely.
8. Tapped Edit.
9. Renamed the session to `testing_M3CQA`.
10. Confirmed the Review title changed.
11. Returned to History and confirmed the renamed session displayed `testing_M3CQA`.
12. Confirmed an older session still displayed `testing`.
13. Force-stopped and relaunched the app.
14. Confirmed History still displayed `testing_M3CQA`.
15. Opened Home Weekly Progress Hub and confirmed Recent Sessions displayed `testing_M3CQA`.
16. Checked crash buffer; no `com.hoctin.gymtracker` crash entries were present.

Runtime evidence files kept in the workspace include:

- `qa_muscle3c_share_sheet_retry.png`
- `qa_muscle3c_shared_heat_card.png`
- `qa_muscle3c_review_renamed2.png`
- `qa_muscle3c_history_renamed.png`
- `qa_muscle3c_history_after_restart2.png`
- `qa_muscle3c_weekly_hub_renamed.png`

## Preserved Behavior

- History-2 direct review navigation remains intact.
- After-save flow still navigates to History first and opens Review on top, so Back returns to History.
- Exercise breakdown below the heat card remains available.
- Empty/failed heat state still degrades to the normal breakdown.
- Progressive Overload Engine, MuscleHeatEngine scoring, catalog imports/assets, signing secrets, and
  MUSCLE-4 scope were not touched.

## Known Limitations

- Share captures the currently laid-out heat-card bounds. The current layout makes the card visible before
  the Share button is reachable; the helper does not render an offscreen duplicate.
- Automated tests cover share filename/cache cleanup, but not a full Android share-intent/crop
  instrumentation path. Runtime QA covered the actual share sheet and crop output.
- No dedicated Room migration unit test boots a legacy v7 database and upgrades to v8. Safety is supported
  by the additive `ALTER TABLE`, exported schema v8, and on-device upgrade/runtime verification.

## Next Recommendation

Proceed to MUSCLE-4 recovery map work. If one small hardening task is preferred first, add instrumentation
coverage for heat-card share crop/intent and a v7-to-v8 Room migration test.
