# UI-2A - Exercise Thumbnail Rows

**Status: PASS WITH VALIDATION CAVEAT**

UI-2A adds catalog-image thumbnails to exercise rows without changing workout data, catalog content,
or progression behavior.

## Backup

- Pre-implementation backup: `D:\HOCTIN\GYM_TRACKER_UI2A_BACKUP_20260623_212225`
- The backup contains the existing `app_src` and `docs` snapshot used for this UI-only task.

## Implementation Files

- `app/src/main/java/com/hoctin/gymtracker/ui/components/ExerciseThumbnailRow.kt`
  - Shared `ExerciseThumbnail` composable and shared asset-image loader.
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogThumbnailLookup.kt`
  - Exact canonical-name and alias lookup to the first catalog image reference.
- `app/src/main/java/com/hoctin/gymtracker/data/catalog/ExerciseCatalogRepository.kt`
  - Exposes the thumbnail-reference lookup as a catalog flow.
- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogExerciseDetailSheet.kt`
  - Reuses the shared loader for detail-sheet media.
- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogPickerScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/history/HistoryScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
  - Supplies the existing catalog repository to Plan.
- `app/src/test/java/com/hoctin/gymtracker/data/catalog/CatalogThumbnailLookupTest.kt`
  - Covers canonical-name, alias, no-image, and alias-collision behavior.

## Shared Component, Loader, and Lookup

- `ExerciseThumbnail` displays the first catalog image with `ContentScale.Crop` and falls back to the
  existing purple dumbbell treatment for custom, unmatched, blank, or unreadable image references.
- Asset bitmap decoding is dispatched off the main thread, sampled for the requested target size, and
  retained in a bounded 12 MiB LRU cache. The detail popup uses the same loader at its larger target size.
- Thumbnail resolution is exact, case-insensitive, and supports both canonical names and aliases. The
  first catalog entry wins an alias collision, matching `findByName` catalog-order behavior.

## Updated and Unchanged Screens

- Updated: Catalog Picker, View Detail, Plan exercise rows, Log Workout accordion headers, and expanded
  History exercise rows.
- Intentionally unchanged: Weekly Progress and its recent-session card. Its existing thumbnail represents
  a workout session rather than an individual exercise; its expanded exercise table did not contain the
  purple exercise-placeholder row targeted by UI-2A.

## Preserved Behavior and Guardrails

- Plan reorder, edit, and delete controls remain on their existing paths; the drag handle remains the
  exclusive long-press reorder target.
- Catalog add and detail behavior remain unchanged.
- Log accordion, info action, save flow, and draft persistence remain unchanged.
- History grouping, set aggregation, and info action remain unchanged.
- Against the dedicated UI-2A backup, SHA-256 comparisons confirm no changes to:
  - `ProgressiveOverload.kt` or `ProgressiveOverloadEngineTest.kt`
  - `AppDatabase.kt` or `Entities.kt`
  - `GymRepository.kt` or `WorkoutDraftRepository.kt`
  - `HistoryViewModel.kt`
  - `catalog_fed_v1.json`
  - all 1,096 bundled exercise-image files
- No signing secrets were read or modified.

## Validation

- Manual user validation: build and app run succeeded; screenshots confirmed thumbnail UI in Catalog
  Picker, Plan, Log Workout, and View Detail.
- Manual user validation: release APK size is approximately 83.2 MB, compared with approximately 79.2 MB
  before UI-2A.
- Codex validation attempted: `.\gradlew.bat :app:testDebugUnitTest` with the documented JDK 17.
  The Gradle wrapper attempted to download Gradle 8.7 and failed with sandbox network permission denied
  (`java.net.SocketException: Permission denied: getsockopt`). Unit tests did not start.
- Not performed by Codex: unit tests, debug build, release build, or emulator runtime QA. Debug and release
  tasks were not retried because they would fail at the same blocked wrapper-download step; no emulator was
  attached in this environment.

## Known Limitations

- Thumbnail images are limited to the existing bundled catalog assets; no network image loading is added.
- Custom or unmatched exercises deliberately retain the purple dumbbell fallback.
- Codex cannot independently certify the final binary or runtime behavior until the Gradle distribution and
  Android toolchain are locally available to this environment.

## Rollback

With explicit approval, restore the listed UI-2A implementation files from
`D:\HOCTIN\GYM_TRACKER_UI2A_BACKUP_20260623_212225\app_src` and remove the new thumbnail helper/test
files only after confirming that backup. Do not use destructive Git commands. The catalog JSON, exercise
assets, Room schema, engine, save/draft repositories, and History data logic require no rollback because
they were not changed by UI-2A.
