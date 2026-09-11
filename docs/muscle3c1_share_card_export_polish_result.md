# MUSCLE-3C.1 - Share Heat Card Export Polish Result

## Status

Completed in `D:\HOCTIN\GYM TRACKER`.

MUSCLE-3C.1 replaces the fragile live-view crop used by the Heat Review Share action with a dedicated
standalone PNG renderer for the heat card. The FileProvider + Android `ACTION_SEND` share flow is
preserved.

No Room schema, MuscleHeatEngine scoring, Progressive Overload Engine, signing secrets, or MUSCLE-4 work
was changed.

## Backup

Created before edits:

`D:\HOCTIN\GYM_TRACKER_MUSCLE3C1_BACKUP_20260704_201444`

Backup contents:

- `app_src`
- `app_schemas`

## Files Changed

- `app/src/main/java/com/hoctin/gymtracker/ui/review/HeatCardShare.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/review/SessionReviewScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/review/HeatCardShareTest.kt`
- `README.md`
- `docs/muscle3c1_share_card_export_polish_result.md`

Runtime QA evidence:

- `qa_muscle3c1_shared_heat_card_final.png`
- `qa_muscle3c1_share_sheet_final.xml`
- `qa_muscle3c1_review_final.xml`
- `qa_muscle3c1_history_final.xml`

## Implementation

Before MUSCLE-3C.1, Share used `LocalView.drawToBitmap(...)` and cropped the currently visible heat-card
bounds. That made the output depend on live screen composition and could leak surrounding UI/background.

After MUSCLE-3C.1:

- `shareHeatCard(...)` takes `HeatCardShareContent` (`heat`, `topMuscles`, `durationText`) instead of a
  root view and crop bounds.
- `HeatCardShare.kt` renders a dedicated 1080 px wide Android `Bitmap` with transparent rounded corners.
- The renderer draws the near-OLED card fill, subtle top glow, labels, duration, top muscles, front/back
  body maps, and AURON wordmark directly into the export image.
- Body maps reuse the existing MUSCLE path data and mapping:
  - `BODY_FRONT_PATHS`
  - `BODY_BACK_PATHS`
  - `BODY_FRONT_OUTLINE`
  - `BODY_BACK_OUTLINE`
  - `BodyMapSlugs.slugTokens(...)`
  - `MuscleHeatPalette`
  - `normalizeSvgArcFlags(...)`
- The renderer does not capture the screen, header, buttons, bottom nav, exercise breakdown, or any live
  outside background.
- Existing cache behavior remains: PNG files are written to `cache/shared_heat_cards`, stale PNGs are
  cleaned up, and FileProvider exposes only that cache path.
- Existing share behavior remains: `Intent.ACTION_SEND`, `image/png`, `EXTRA_STREAM`, and
  `FLAG_GRANT_READ_URI_PERMISSION`.

`SessionReviewScreen` now only blocks sharing while the review is still loading. A computed-empty heat
state can still share a graceful fallback card instead of crashing.

## Tests

Updated focused JVM tests in `HeatCardShareTest`:

- Share filename remains stable and PNG-only.
- Cache cleanup still deletes only expired PNG files.
- Export layout height is deterministic and grows when duration/footnotes require it.

Commands run:

```powershell
$env:JAVA_HOME='D:\jdk17\jdk-17.0.19+10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Results:

- `:app:testDebugUnitTest` passed, exit 0.
- `:app:assembleDebug` passed, exit 0 (`BUILD SUCCESSFUL in 6s` on the final run).
- Only the existing AGP/compileSdk 35 warning appeared during assemble.

## Runtime QA

Device:

- `emulator-5554`
- `Pixel_8`

Steps verified:

1. Installed the debug APK over existing data.
2. Launched AURON and opened History.
3. Opened saved session `testing_M3CQA`.
4. Confirmed Muscle Heat Review loaded with Duration, Top Muscles, body maps, Share/Edit buttons, and the
   exercise breakdown below.
5. Tapped Share.
6. Confirmed Android chooser opened as `Sharing image`.
7. Confirmed a new cache PNG was generated:
   `cache/shared_heat_cards/auron_heat_session_9_1783171945171.png`.
8. Pulled the generated PNG as `qa_muscle3c1_shared_heat_card_final.png`.
9. Visually inspected the PNG: standalone rounded heat card only; no header, buttons, bottom nav, exercise
   breakdown, or leaked outside screen background.
10. Pixel-checked the pulled PNG:
    - size `1080x1240`
    - corner pixels alpha `0`
    - card interior opaque
11. Checked crash buffer; no `com.hoctin.gymtracker` crash entries were present.

## Preserved Behavior

- Share still uses FileProvider + `ACTION_SEND`.
- No internet or broad storage permission is required.
- No raw app-private file path is shared.
- Rename/session-title behavior from MUSCLE-3C is unchanged.
- History-2 navigation and after-save Review flow are unchanged.
- Exercise breakdown below the heat card is unchanged.

## Known Limitations

- The export renderer intentionally duplicates the heat-card visual layout in Android Canvas instead of
  reusing the Compose `HeatCard` composable directly. It reuses the same body-map data/mapping, but text
  metrics are Canvas-based rather than Material typography.
- Android's system chooser on the QA emulator showed the image-share sheet; the definitive visual
  verification was the pulled PNG because this resolver build did not expose a large inline preview.
- Full share-intent/crop rendering remains runtime-QA verified rather than instrumentation-tested.

## Next Recommendation

Proceed to MUSCLE-4 only after accepting this export style. If hardening is preferred first, add an
instrumentation test that taps Share and verifies the generated PNG dimensions/corner alpha on-device.
