# LOG-V4B — Daily Win cleanup + Liquid Glass — Result

Date: 2026-09-04
Project: `D:\HOCTIN\GYM TRACKER`
Package: `com.hoctin.gymtracker` (internal; brand is AURON)
minSdk = 26 / targetSdk = 35 / compileSdk = 36
Compose BOM `2024.06.00`

## Outcome

Task A is implemented and green. Task B is a research deliverable (see §7) — no Settings UI
was touched.

- **Three popups removed.** `DailyWinInfoSheet`, `DailyBoostChooseSheet` and the local
  `DailyBoostTimerDialog` (plus its three children) are gone.
- **One shared timer.** `RestTimerSheet` moved out of `LogWorkoutScreen` into
  `ui/components/`, parameterised, and is now used by both Log Workout and Daily Win.
- **Shuffle icon replaced** with an inline `BoostRandomIcon`.
- **Liquid glass shipped on the card** — AGSL displacement refraction of the card's own
  pixels on API 33+. Not blur.

## Backup

Pre-edit filesystem backup of the two files that were edited (not created):

`D:\HOCTIN\GYM TRACKER\_backup_logv4b_20260904_141601\`

- `DailyBoostSection.kt` (45,326 bytes)
- `LogWorkoutScreen.kt` (99,769 bytes)

Signing material was not read, copied or printed.

## Files changed

### Modified

| File | Change |
|---|---|
| `ui/home/DailyBoostSection.kt` | Removed 10 top-level declarations (3 popups + timer dialog + 3 timer children + `BoostTimerPhase` + `formatBoostTime` + `BoostChoiceRow`). Card signature lost `onChoose` / `onOpenDetail`. Added `BoostRandomIcon` and `DailyWinTimerSheet`. Card body wrapped in `auronLiquidGlass`. 1,105 → 640 lines. |
| `ui/log/LogWorkoutScreen.kt` | Removed the inlined `RestTimerSheet` and `TimerCircleButton`. `CompactField` and `SegmentedChoices` changed `private` → `internal` so the extracted sheet can reuse them. Call site now passes the full parameter set. |
| `ui/home/HomeScreen.kt` | Removed `showBoostChooser` / `showWinSheet` state and both sheet invocations. `DailyBoostCard` call lost `onChoose` / `onOpenDetail`; `onRandom` now randomizes **and** opens the timer. `DailyBoostTimerDialog` → `DailyWinTimerSheet`. |
| `README.md` | LOG-V4B section rewritten to describe refraction, not blur. |

### Added

| File | Lines | Purpose |
|---|--:|---|
| `ui/components/AuronLiquidGlass.kt` | 145 | `Lens` data class + the AGSL shader + `Modifier.auronLiquidGlass(lens)`. |
| `ui/components/RestTimerSheet.kt` | 199 | The shared timer sheet, parameterised, with an optional `overlay` slot. |

## 1. Popup removal

| Removed | Was opened by | Where the function went |
|---|---|---|
| `DailyWinInfoSheet` | `Icons.Outlined.Tune` on the card header | Nowhere — the title, lime accent and streak bar already say what Daily Win is. DESIGN.md §1 rejects this kind of explanation. |
| `DailyBoostChooseSheet` + `BoostChoiceRow` | Chevron on the "Today's win" row | Picking a different win moved behind **Random**. The row is now a read-only label. |
| `DailyBoostTimerDialog` + `DailyBoostTimerContent` | Start | Replaced by the shared `RestTimerSheet`. |
| `BoostTimerRing` / `BoostTimerControls` / `BoostCompleteBadge` | (only the local dialog) | Superseded by the ring and controls inside `RestTimerSheet`. |
| `BoostTimerPhase` / `formatBoostTime` | (only the local dialog) | `RestTimerSheet` uses `formatRestDuration`. |

New flow:

```
Tap "Start"   → open shared RestTimerSheet (0.60f)
Tap "Random"  → HomeViewModel.randomizeDailyBoostExercise() → open shared RestTimerSheet
Done (0:00)   → confetti overlay → markDailyBoostCompleted() → close
```

## 2. Shared `RestTimerSheet`

The Log sheet was extracted verbatim and everything the two flows disagree on became a
parameter, so they cannot drift:

| | Log Workout | Daily Win |
|---|---|---|
| `accent` | `0xFF45D483` (rest green) | `dailyWinAccent()` (lime) |
| `maxHeightFraction` | `0.56f` | `0.60f` |
| `showSmartToggle` | `true` | `false` |
| `showDurationInput` | `true` (Sec/Min entry) | `false` (fixed 2:00) |
| `showSkipContinue` | `state.advanceAfterRest` | `false` |
| `overlay` | none | one-shot confetti on DONE |

Two design notes worth keeping:

- **`overlay` exists because a Dialog renders into its own window.** Drawing confetti in the
  caller's composition (Home) would put it *behind* the sheet. Handing it in as a
  `BoxScope` slot drawn with `matchParentSize()` is the only correct placement.
- **`CompactField` / `SegmentedChoices` became `internal`** rather than being moved. They
  have 7 and 3 other call sites in `LogWorkoutScreen.kt`; moving them would have churned
  the set-row and advanced-set UI for no benefit. `TimerCircleButton` moved because the
  timer was its only caller.

Daily Win's countdown driver (`DailyWinTimerSheet`) stays in `DailyBoostSection.kt`. It
accumulates real elapsed time per frame with `withFrameNanos`, so pause/resume continues
from the frozen value and a dropped frame cannot drift the countdown — the same pattern the
removed `DailyBoostTimerContent` used.

## 3. `BoostRandomIcon`

Material's `Icons.Outlined.Shuffle` puts an arrowhead at **both** ends of **both** shafts:
four arrowheads inside 20dp, which collapses into a smudge at action-button size.

`BoostRandomIcon` keeps the same crossed-S shuffle semantic but heads only the *outgoing*
ends — four heads become two. Built as an inline `ImageVector` (24dp viewport, 1.8dp round
stroke, `SolidColor(Color.Black)` so the button's `tint` applies), matching the hand-built
`BoostMedallion` glyph rather than the Material icon set.

## 4. Liquid glass — refraction, not blur

### What it does

`Modifier.auronLiquidGlass(Lens(radiusPx, scale, chromaAmount, specular))` attaches an AGSL
`RuntimeShader` to the card layer. For each fragment:

1. Signed distance to a rounded rect gives a `0 → 1` field from centre to rim, squared so
   the middle of the card stays readable.
2. Displacement is applied along the **inward** normal. Sampling inward magnifies, so edges
   bulge like a convex lens and the rim never samples outside the layer (which would read as
   a transparent halo).
3. `uChroma` samples R and B at slightly different scales for Aave's chromatic fringe.
4. `uSpecular` adds a rim highlight plus a soft top-left sweep.

### What it deliberately is not

- **Not `Modifier.blur` / `createBlurEffect`.** That is Aave's explicitly-rejected frosted
  shortcut. It is a named non-goal here.
- **Not sampling the Home canvas.** Aave's line that matters: *"Nothing is sampled from
  underneath the glass. The content's own pixels are the ones moving."* The lens input is the
  card's own painted pixels — medallion, type, buttons, status chip.
- **Not chained with blur.** Chaining `createBlurEffect` onto the displacement would destroy
  the refraction.

### API floor

Programmable displacement needs **API 33 (Tiramisu)**. API 31's `RenderEffect` exposes only
blur / offset / color-filter / chain / bitmap-shader — there is no per-pixel map. Below 33
the modifier is a no-op and the card keeps its translucent gradient. That fallback is
crash-safety only and is **not** glass.

This project is pinned to Compose BOM `2024.06.00`, which predates
`androidx.compose.ui.graphics.RuntimeShader`, so the file uses the framework
`android.graphics.RuntimeShader`.

### Regeneration rule

The displacement field is a pure GPU-side function of layer size and lens shape. There is no
CPU-side map to rebuild, so scrolling, pressing, or opening the timer regenerate nothing —
the strictest possible version of Aave's "map is rebuilt only when the glass changes shape."

### Tuning

`scale = 12f`, `chromaAmount = 2.4f`, `specular = 0.14f`, `radiusPx = 22.dp`. Chosen at
Aave's "slider" magnitude (gentler bend) rather than the "switch" magnitude, because the
card carries text that must stay legible.

## 5. Validation

```
:app:compileDebugKotlin   BUILD SUCCESSFUL
:app:testDebugUnitTest    BUILD SUCCESSFUL  —  438 tests / 0 failures / 0 errors / 0 skipped
:app:assembleDebug        BUILD SUCCESSFUL  —  app-debug.apk     104,200,211 bytes
:app:assembleRelease      BUILD SUCCESSFUL  —  app-release.apk    95,080,633 bytes
```

Test count is unchanged from the 438 baseline.

### Protected-surface verification

| File | SHA-256 (first 16) | Status |
|---|---|---|
| `domain/ProgressiveOverload.kt` | `8d2355d55980b647` | unchanged |
| `domain/muscleheat/MuscleRecovery.kt` | `0520ce83248be1a5` | unchanged |
| `domain/muscleheat/MuscleHeatEngine.kt` | `12804f8654ba8124` | unchanged |
| `data/research/RecoveryResearchContract.kt` | `83eeacc0e28f194b` | unchanged |
| `data/AppDatabase.kt` | `1d85e0d69a4c0b2b` | unchanged |
| `app/src/test/.../ProgressiveOverloadEngineTest.kt` | `ff4dde81ce2a225e` | unchanged |
| `data/GymRepository.kt` | `83a5d6d007ce742b` | **differs from the LOG-V4A baseline** — see note |

`GymRepository.kt` does not match the `D841C2E7…` value recorded in the LOG-V4A doc
(2026-08-16). Its mtime is **2026-09-01 20:32**, i.e. it was changed by the earlier Max
Weight task (DAO → repository → 2 ViewModels), not by this pass. `GymRepository` was not
opened or edited here. This is a pre-existing difference, recorded rather than hidden.

Room remains **version 11**. No migration, no entity, no DAO, no schema export was added or
changed. No import/export contract change. No catalog change. No onboarding change.

## 6. Lint / API notes hit during implementation

Two import errors, both from assuming a symbol was top-level when it is a member:

1. `androidx.compose.ui.graphics.RuntimeShader` does not exist in BOM `2024.06.00` — use the
   framework `android.graphics.RuntimeShader`.
2. `Modifier.matchParentSize()` is a **member extension of `BoxScope`**, not a top-level
   function. Importing `androidx.compose.foundation.layout.matchParentSize` fails; the call
   resolves through the `BoxScope` receiver with no import. Same class of mistake as
   `calculateTopPadding()` / `calculateBottomPadding()` (members of `PaddingValues`).
3. `ImageVector.Builder.addPath(...)` is a member of `Builder`, not an extension — no import.

39 unused imports were removed from `DailyBoostSection.kt` and 7 from `LogWorkoutScreen.kt`.
`getValue` / `setValue` were deliberately kept: they look unused to a text search because the
compiler plugin generates the calls for `by` delegates.

## 7. Task B — Settings research

**Not performed in this pass.** The plan (§5 of `log_v4b_daily_win_redesign_plan.md`) scopes
it as a separate read-only deliverable: one entry per `ProfileDestination` (13 rows) covering
source file, whether the screen uses the OLED/charcoal language or legacy Material, whether
its `Unavailable` / `Updating` status is honest, and whether reusing `auronLiquidGlass` there
is justified.

Current code tree, for the follow-up:

| File | LOC | Role |
|---|--:|---|
| `ui/settings/ProfileHubScreen.kt` | 108 | The hub. |
| `ui/settings/ProfileSettingsComponents.kt` | 339 | Shared row/section widgets. |
| `ui/settings/ProfileSettingsModels.kt` | 176 | `ProfileDestination` enum + `profileHubSections()`. |
| `ui/settings/ProfileOverviewAndInsightsScreens.kt` | 252 | Streaks, Insights, Personal profile, History. |
| `ui/settings/ProfilePreferencesAndTrainingScreens.kt` | 267 | Training setup, Units & appearance, Notifications. |
| `ui/settings/ProfileDataResearchResourcesScreens.kt` | 616 | Data, Research, Resources, Privacy, About. |
| `ui/settings/SettingsScreen.kt` | 901 | **Legacy, dead code.** |
| `ui/settings/SettingsViewModel.kt` | 279 | Legacy, dead code. |

## 8. Known limitations / not done

1. **No on-device QA.** The glass effect has never been rendered. It is verified to compile,
   to keep 438 tests green, and to gate correctly on API level — but the *look* (displacement
   magnitude, chroma fringe, specular strength) is unvalidated on a real GPU. This is the
   single largest open item.
2. **`scale = 12f` is a guess.** It needs a screenshot of the map-vs-result pair (Aave shows
   them side by side) before the magnitude can be called right.
3. **API 26–32 has no glass.** That is ~15% of active devices in 2026. The card degrades to
   its translucent gradient, which is correct but is a visible inconsistency between devices.
4. **`GymRepository.kt` hash drift** is carried forward from the 2026-09-01 Max Weight task
   and is unexplained by any doc. Worth reconciling.
5. **Task B not started.**
6. **`graphify update .` not run** — the CLI is not on PATH in this environment, so
   `graphify-out/graph.json` remains stale from 2026-08-16.

## 9. Rollback

`_backup_logv4b_20260904_141601/` holds the two edited files. To roll back:

1. `cp` both files back over the current versions.
2. Delete `ui/components/AuronLiquidGlass.kt` and `ui/components/RestTimerSheet.kt`.
3. Revert the four edits in `HomeScreen.kt` (state block, `DailyBoostCard` call, sheet block).
4. In `LogWorkoutScreen.kt`, restore `private` on `CompactField` and `SegmentedChoices`.

No data migration. Room stays v11. No settings reset, no sign-out.
