# DB-QUICKFIX1 Exercise Detail Image Fit Result

## Scope

DB-QUICKFIX1 fixes the reusable Exercise Detail / Catalog Detail media frame so full exercise images
remain visible. This is a UI-only detail-sheet polish.

No Room schema, catalog JSON, exercise imports, Progression Engine logic, Log Workout tracking logic,
thumbnail component, or signing secrets were changed.

Backup created before edits:

`D:\HOCTIN\GYM TRACKER\_backup_dbquickfix1_detail_image_fit_20260630_211151`

## Files Changed

- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogExerciseDetailSheet.kt`
- `README.md`
- `docs/db6f1_exercise_detail_image_fit_result.md`

## Root Cause

`CatalogExerciseDetailSheet.MediaSlot` rendered every detail image in a fixed 200 dp-high full-width
frame with `ContentScale.Crop`. That worked acceptably for some landscape assets, but it cropped portrait
assets aggressively. For example, Alternating Cable Shoulder Press source images are 500 x 750, so
center-crop inside a wide frame removed important body and cable-machine context.

The row thumbnail path is separate:

- `app/src/main/java/com/hoctin/gymtracker/ui/components/ExerciseThumbnailRow.kt`
- `ExerciseThumbnail(...)`

That thumbnail path intentionally remains a small cropped square/rounded thumbnail and was not edited.

## Image Fit Strategy

`MediaSlot` now uses:

- Stable frame height: `240.dp`
- Full-width rounded AURON frame
- `ContentScale.Fit`
- Centered image content
- Existing dark themed frame background and border

This keeps the whole bitmap visible without distortion. Portrait images letterbox horizontally inside
the frame; landscape images continue to fill most of the frame naturally. The carousel dots were kept and
now sit on a small translucent dark pill so they remain readable when a bright image reaches the bottom
of the contained frame.

Tap-to-cycle behavior is unchanged:

- `imageRefs.size > 1` still makes the frame clickable.
- Tapping advances `index = (index + 1) % imageRefs.size`.
- The dot state still tracks the active frame.

## Responsive Behavior

The detail sheet still uses the existing `AuronModalSheet` scroll container, so taller content scrolls
instead of clipping the header or overflowing horizontally. The media frame is fixed at 240 dp to give
portrait images enough height without creating a giant layout jump.

Expected behavior:

- Small phones: sheet remains scrollable; header stays above the image.
- Tall portrait images: entire image fits with side letterboxing.
- Wide landscape images: image fits without stretching or vertical crop.
- Image-less fallback: placeholder remains centered in the same rounded frame.
- Horizontal overflow: none; frame uses `fillMaxWidth()`.

## Thumbnail Confirmation

Catalog row thumbnails are untouched. `ExerciseThumbnail(...)` still owns row thumbnails and still uses
its own size, cache key, fallback, and `ContentScale.Crop`. The detail sheet only reuses
`rememberExerciseAssetImageBitmap(...)` for loading the asset bitmap; the cache key includes target size,
so the detail slot's 240 dp target does not alter the 52 dp thumbnail decode path.

## Tests And Builds

Passed:

- `.\gradlew.bat :app:testDebugUnitTest`
  - 7 XML result files
  - 133 tests
  - 0 failures
  - 0 errors
  - 0 skipped
- `.\gradlew.bat :app:assembleDebug`
- `.\gradlew.bat :app:assembleRelease`

Artifacts:

- Debug APK: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk`
  - 97,989,302 bytes
  - Last written: June 30, 2026 9:17:17 PM
- Release APK: `D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\release\app-release.apk`
  - 90,299,058 bytes
  - Last written: June 30, 2026 9:18:38 PM

Notes:

- The sandboxed Gradle invocations hit the known wrapper network restriction:
  `java.net.SocketException: Permission denied: getsockopt`.
- The same Gradle gates passed when rerun with approved out-of-sandbox access.
- Gradle emitted the existing non-fatal AGP compileSdk 35 warning.

## Runtime QA Result

Device:

- Pixel 8 emulator, `emulator-5554`

Installed:

- `app\build\outputs\apk\debug\app-debug.apk`

Verified:

1. Opened Catalog Picker detail for Alternating Cable Shoulder Press.
   - Source image shape: 500 x 750 portrait.
   - Detail sheet showed the full image in the wider 240 dp frame.
   - No important body/movement context was cropped.
   - Side letterboxing used the dark AURON frame background.
2. Tapped the image to cycle from frame 0 to frame 1.
   - Carousel advanced.
   - Dots updated.
   - Frame 1 also fit fully.
3. Opened Catalog Picker detail for Barbell Deadlift.
   - Source image shape: 850 x 567 landscape.
   - Image still looked good in the same frame.
   - No distortion was visible.
4. Opened an unmatched View Detail exercise:
   - Plate-loaded incline chest press machine.
   - Placeholder still showed `Image coming soon`.
   - Missing-detail copy still showed below the frame.
5. Crash buffer check:
   - `adb logcat -b crash -d | Select-String "com\.hoctin\.gymtracker"`
   - No matching crash entries.

Captured QA screenshots:

- `qa_dbquickfix1_alternating_dark.png`
- `qa_dbquickfix1_alternating_dark_frame1.png`
- `qa_dbquickfix1_deadlift_dark.png`
- `qa_dbquickfix1_first_row_detail.png`
- `qa_dbquickfix1_lateral_light.png`

## Known Limitations

- The quick light-theme pass did not visibly switch the app before the captured detail screenshot, so
  the conclusive visual QA evidence is dark-theme runtime QA. Static review confirms the frame background
  and border remain theme-derived.
- Valid asset decode still starts from a nullable bitmap state. A valid image can briefly show the
  placeholder while the asset decodes, which existed before this quickfix. This task did not change the
  shared bitmap loader contract.
