# DB-6F — Multi-Tracking UI Polish & Responsive Layout Fix — COMPLETED ✅

Fixes the responsive-layout regressions that the DB-6E carry/sled exercises exposed once the non-rep
`TrackingType`s became runtime-reachable. **UI only** — no Room schema change, no Progression Engine
change, no catalog imports, no catalog JSON change, no `CONTENT_VERSION` bump (stays 16), signing
secrets untouched. Unit tests + debug + release builds green; emulator QA passed in light and dark.

Backup created before edits: `D:\HOCTIN\GYM TRACKER\_backup_db6f_ui_polish_20260630_181537`
(source `java` + `test-java` + `README.md`).

## Root cause

Two surfaces laid their children out in a single fixed-width `Row` that **shrank** its children to fit
instead of **reflowing** them, and the inner text composables had no single-line guard:

1. **Catalog Picker row** placed the target chip and the `progressionType` chip in a plain
   `Row(spacedBy(8.dp))` inside a `weight(1f)` column. When the two pills exceeded the column width, the
   trailing pill (`Compound Load`) was squeezed to a few px and its `GymBadge` `Text` (no `maxLines` /
   `softWrap=false`) wrapped **letter-by-letter vertically** → the `C/o/m/p/o/u/n/d…` stack in the
   screenshot.
2. **Log Workout tracking row** placed every active field (load + distance + duration) in one weighted
   `Row` plus the index circle and a delete control. For `TIME_DISTANCE_LOAD` (3 fields) on a normal
   phone each field got ~80 dp, too narrow for the `Duration` placeholder + `mm:ss` unit, and
   `CompactField`'s placeholder `Text` (no `maxLines`) wrapped to `Dura / tio / n`.

Both are the same class of bug: **text allowed to wrap vertically inside a container that crushes rather
than reflows.**

## Files changed

- `app/src/main/java/com/hoctin/gymtracker/ui/components/AppComponents.kt` — `GymBadge` text is now
  hard single-line (`maxLines = 1`, `softWrap = false`, `overflow = Ellipsis`). A pill can never become
  a vertical letter stack; in a width-bounded parent it ellipsizes. (Shared by every chip in the app —
  Catalog, Plan, History, Home, Weekly, Log status badge.)
- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogPickerScreen.kt` — the two metadata chips
  now live in a `FlowRow` (`@OptIn(ExperimentalLayoutApi::class)`), so the progression chip wraps to a
  clean second line instead of crushing. Thumbnail / title / subtitle / Add button unchanged.
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`:
  - New `TrackingFieldSpec` + `TrackingFieldsRow` (a `BoxWithConstraints` responsive grid) +
    `TrackingFieldCell`. `TrackingFieldsRow` renders **one row when the fields fit, otherwise a
    two-per-line grid** (only the 3-field `TIME_DISTANCE_LOAD` reflows on a normal phone). This is the
    single place the load/distance/time fields are arranged.
  - `TrackingSetRow` rewritten to build the field list from the `TrackingType` config and delegate
    layout to `TrackingFieldsRow`; the duration field uses the compact label **`Time`** (units inline:
    `kg`, `m`/`ft`, `mm:ss`).
  - Extracted `SetIndexBadge` (the set-number circle) and `SetRowMenu` (the overflow **⋮ → Delete set**
    menu); **both the rep `SetRow` and the non-rep `TrackingSetRow` now use them**, so the delete
    affordance is identical. The always-visible `✕` on tracking rows is gone.
  - `CompactField` placeholder is now single-line (`maxLines = 1`, `softWrap = false`) — the real
    root-cause fix, since `singleLine` on `BasicTextField` only governs typed text, not the placeholder.
- `app/src/main/java/com/hoctin/gymtracker/ui/format/SetDisplay.kt` — `trackingFieldsLabel` renders the
  duration token as the concise **`time`** (matches the Log `Time` field), so `TIME_DISTANCE_LOAD` →
  `load + distance + time`, `TIME_ONLY` → `time`.
- `app/src/test/java/com/hoctin/gymtracker/ui/format/MeasurementFormatterTest.kt` — updated the two
  `targetLabel` assertions to the new `time` token.
- `README.md` — DB-6E label tokens updated to current behavior + new **DB-6F** subsection with the
  responsive layout rules and the no-vertical-wrap prevention rule.

## Catalog row fix (PART A)

`FlowRow` + single-line `GymBadge`. The row now reads as: target chip (`3 sets · load + distance + time`)
on line 1, and the `progressionType` chip (`Compound Load`) wraps to its own line when it doesn't fit —
both horizontal and readable. For concise tracking labels the `time` token keeps `DISTANCE_LOAD` =
`load + distance` and `TIME_DISTANCE_LOAD` = `load + distance + time`. Matches AURON pill style; the
thumbnail, title, ellipsized subtitle, and `+` Add button stay balanced.

> Note on "copy pic 2": the formal PART A spec ("Match AURON style", "keep … Add button visually
> balanced") was treated as authoritative over a literal copy of pic 2's reference styling (radio-circle
> multi-select + `#hashtag` muscles), which would be a behavior change outside DB-6F's responsive-fix
> scope. Pic 2's intent — a clean, single-line, ellipsized, never-crushing row — is fully honored within
> AURON's existing chip + Add-button design.

## Log row fix (PART B)

`TrackingFieldsRow` (`BoxWithConstraints`): when every field can keep ≥ ~104 dp it lays out one row;
otherwise a two-per-line grid. On a Pixel 8, `TIME_DISTANCE_LOAD` reflows to **`Load` + `Dist` on line 1,
`Time` on line 2**; 1–2 field types (`TIME_ONLY`, `TIME_LOAD`, `DISTANCE_LOAD`, `TIME_DISTANCE`) stay on
one line. Compact labels `Load` / `Dist` / `Time`; units inline `kg` / `m` or `ft` / `mm:ss`. The
single-line placeholder guarantees no field ever shows a broken `Dura / tio / n` word. Duration entry
keeps the `KeyboardType.Text` colon support + blur-normalization from DB-6C.2.

## Delete / action consistency fix (PART C)

The non-rep rows now use the shared `SetRowMenu` — the same **⋮** overflow with a single **Delete set**
item (trash icon) used by the rep rows — instead of an always-visible `✕`. Delete functionality is
preserved (verified the menu opens and the item is present), `Add set` is unchanged, and the smaller,
menu-gated target makes accidental deletes far less likely.

## Shared responsive component (PART D)

Centralized, not one-off: `GymBadge` (single-line) fixes every chip surface; `FlowRow` handles catalog
chips; `TrackingFieldsRow` + `TrackingFieldCell` + `TrackingFieldSpec` own the load/distance/time
arrangement for **all** non-rep types (`TIME_ONLY`, `TIME_LOAD`, `DISTANCE_LOAD`, `TIME_DISTANCE`,
`TIME_DISTANCE_LOAD`); `SetIndexBadge` + `SetRowMenu` are shared by rep and non-rep rows. A new
`TrackingType` inherits the responsive behavior automatically via its `showsLoad/showsDistance/
showsDuration` config — no per-type layout code.

## Test / build result (PART E)

Environment: `JAVA_HOME=D:\jdk17\jdk-17.0.19+10`, `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`.

| Gate | Result |
|---|---|
| `.\gradlew.bat :app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** (133 tests; DB-6F changed 2 assertions, added none) |
| `.\gradlew.bat :app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `.\gradlew.bat :app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |

Pre-existing non-fatal warnings only (AGP 8.5.2 `compileSdk 35`; kapt `room.schemaLocation` option).

APK artifacts:
- Debug: `app/build/outputs/apk/debug/app-debug.apk` — 97,989,302 B
- Release: `app/build/outputs/apk/release/app-release.apk` — 90,298,894 B

## Runtime QA result (Pixel 8 emulator, `emulator-5554`)

Installed the DB-6F debug APK (`adb install -r`). Verified, **dark and light**:

| Check | Result |
|---|---|
| Catalog `far` → Farmer's Walk: `3 sets · load + distance` line 1, `Compound Load` wrapped to line 2 | ✅ no vertical letters |
| Catalog `sled` → Sled Overhead Triceps Exte… (title ellipsized) / Sled Push: `3 sets · load + distance + time` + progression chip wrapped | ✅ clean |
| Catalog rows in general (`Bodyweight` / `Isolation Slow` chips) wrap cleanly | ✅ |
| Log Farmer's Walk (`DISTANCE_LOAD`): `Load` `kg` + `Dist` `ft` on one row, ⋮ menu, no reps/RIR | ✅ |
| Log Sled Push (`TIME_DISTANCE_LOAD`): `Load`+`Dist` on line 1, `Time` `mm:ss` on line 2 — no broken `Duration` word, no reps/RIR | ✅ (dark + light) |
| ⋮ overflow menu opens **Delete set** (same as rep rows) | ✅ |
| Imperial unit display (`ft`) honored from Settings → Units | ✅ |
| Rep exercises (`REPS`/`KG`/`RIR`) and the rep ⋮ menu unchanged | ✅ |
| Collapsed Plank label `3 sets · time` | ✅ |
| Light + dark visual pass | ✅ |
| Crash buffer (`logcat -b crash`) for `com.hoctin.gymtracker` | ✅ EMPTY |

QA screenshots captured to the session scratchpad (`qa8_far`, `qa9_sled`, `qa14`/`qa17` dark Sled/Farmer,
`qa18_menu` delete menu, `qa30_light_sled` light Sled). The app theme was returned to **Dark** (the state
it was found in) after the light-mode pass.

## Known limitations

- The two-line reflow threshold is width-based (~104 dp/field); the 3-field `TIME_DISTANCE_LOAD` reflows
  on phones and stays on one line only on tablet-width field regions — intended.
- Runtime-reachable non-rep types today are `TIME_ONLY`, `DISTANCE_LOAD`, and `TIME_DISTANCE_LOAD` (the
  catalog members from DB-6E); `TIME_LOAD` / `TIME_DISTANCE` are wired + responsive but have no catalog
  members yet, so they were not exercised on-device (covered by the shared layout + unit tests).
- The end-to-end Save-button round-trip via blind `adb` taps remains flaky (documented since DB-5B.1);
  the save path is unchanged and is covered by `LogWorkoutTrackingInputTest` + the DB-6E save QA. No new
  save logic in DB-6F.
- Non-rep PR/progression scoring is still intentionally not modeled (no engine change).

## Confirmation

No Progression Engine logic, no Room schema/version, no catalog JSON / `CONTENT_VERSION` (stays 16), no
exercise imports, and no signing secrets were changed. DB-6F is a UI-layer change only; the 577 existing
exercises behave exactly as before, with the multi-tracking surfaces now responsive and free of vertical
text crushing. DB-7 was not started.
