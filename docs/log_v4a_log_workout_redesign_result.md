# LOG-V4A — Log Workout Redesign + Smart Rest Timer + Advanced Set UI Foundation — Result

Date: 2026-08-16

## Outcome

LOG-V4A implements the requested Log Workout presentation, smart rest timer and safe advanced-set UI
foundation without changing Room v11, Progressive Overload, History grouping, wearable/Recovery
Research, Saved One-Off provenance, catalog data, onboarding or import/export.

The task deliberately separates two layers:

- The sticky Log layout, progression/history modal, smart rest timer, next-exercise transition and
  IME-safe Saved Workout review are functional now.
- Warmup/Dropset/weight-distribution/continuation identity is a transient session UI contract. It does
  not claim Engine, History, recovery or persistence semantics that do not exist yet.

## Backup

Pre-edit filesystem backup:

`D:\HOCTIN\GYM_TRACKER_LOGV4A_BACKUP_20260816_160025`

It contains `app_src`, `docs` and `README.md`. No signing secret was read or copied.

## Files changed

- `app/src/main/java/com/hoctin/gymtracker/ui/components/AuronBottomSheet.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutInteractionModels.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutBuilderScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileSettingsComponents.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/theme/Theme.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/log/LogWorkoutTrackingInputTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/log/RestSuggestionHeuristicTest.kt` (new)
- `README.md`
- `docs/log_v4a_log_workout_redesign_result.md` (new)

## UI and interaction changes

### Saved Workout review and IME

The existing compact review modal keeps its original design. Its name field now uses
`BringIntoViewRequester` after focus and the shared compact modal can opt into `imePadding`. This keeps
the focused field and actionable sheet content above the software keyboard without device-specific
coordinates.

### Log canvas and sticky identity

- Log and Settings opt into shared charcoal tokens: `#0D0E11` and raised `#111218`.
- The upper plum/OLED canvas architecture is preserved.
- AURON branding, workout title and elapsed Duration live in a `LazyColumn.stickyHeader`; exercises
  scroll below that identity block.
- Exercise thumbnail + name is one 48dp-aware target that opens the existing shared Exercise Detail
  popup. The redundant information icon is removed.
- The right-side chevron controls only expansion/collapse; status remains visible.

### Progression and history modal

Progression Target opens a full usable-width AURON compact modal capped near 74% of screen height.
The background is dimmed, the surface is OLED-dark, and content scrolls internally. It reuses:

- the current exercise name and target;
- the existing Progressive Overload recommendation and wording without recalculation or rewriting;
- the existing exercise note field/persistence path;
- canonical exercise history rows, including load/reps/RIR and current PR markers.

No progression route or data query was deleted; the new modal is presentation around existing state.

## Smart Rest Timer

### Trigger contract

`commitSet(exerciseId, index)` is the centralized idempotent completion boundary. It is called only
from an explicit final-field IME action, not after every digit. A research row key prevents
recomposition from reopening the timer for the same completed set. If a committed set is made
incomplete, its key becomes eligible for a later genuine recommit.

Validation follows the existing tracking contract:

- reps and RIR are required for rep/RIR rows;
- external weight remains required for `REPS_WEIGHT`;
- blank external load is valid for `BODYWEIGHT_REPS` and `SKILL_FORM` and is converted through the
  existing zero-load representation, without forcing the user to enter fake kilograms;
- other time/distance/load types continue through their existing parsing rules.

### Timer state and controls

The timer is ViewModel-owned transient state, so recomposition or closing the sheet does not reset an
active countdown. It includes:

- smart-suggestion lightbulb toggle;
- large circular remaining-time display;
- numeric entry with exact `Sec` / whole `Min` interpretation;
- reset/restart and start/pause;
- one-second countdown updates;
- finish/skip behavior for a pending next-exercise transition.

`75 Sec` formats and counts as `1:15`.

### Suggestion heuristic

`RestSuggestionHeuristic` is a small pure UI-domain helper. It consumes only the catalog's existing
`ProgressionType` values; no new Engine type was invented.

| Existing type | Range |
|---|---:|
| `COMPOUND_LOAD` | 150–240 sec |
| `ISOLATION_SLOW` | 75–120 sec |
| `BODYWEIGHT` | 90–150 sec |
| `SKILL_FORM` | 120–180 sec |

The current enum has no true power/velocity value, so the proposed 180–300 second mapping was not
fabricated.

Effort placement within each range:

- RIR 3+ → low end;
- RIR 2 → low-middle;
- RIR 1 → upper-middle;
- RIR 0 → near the high end.

A clear rep drop of at least 20% versus the immediately previous comparable completed set moves the
suggestion upward one tier. Each working set above the programmed target adds 15 seconds, capped at
+45 seconds. Suggestions round to five-second increments. With Smart off, manual duration is retained.

This is an AURON timer heuristic only. It does not live in or alter `ProgressiveOverload.kt`.

## Last set and next exercise

- A normal non-final completed set opens the timer once.
- The last programmed base set, when another exercise exists, exposes a restrained reduced-motion-
  aware `Next exercise` highlight instead of immediately covering the UI.
- Tapping it opens the timer with an advance intent.
- Timer completion or explicit finish collapses the current exercise, expands the next exercise and
  emits a one-shot scroll request with an offset below the sticky header.
- If no next exercise exists, none is invented.

## Advanced set UI foundation

The normal set overflow now opens an OLED modal around 62% height with:

- Straight Set / Warmup Set / Dropset;
- Total / Each Side / Left Side / Right Side;
- descriptions matching the supplied interaction references;
- Continue this set;
- red outlined Delete Set.

Warmup and Dropset display compact W/D badges. Multiple Drop-tagged sets on a compound exercise show
a restrained technique/fatigue caution and never block input.

Each normal base set can own at most one continuation. A continuation:

- appears immediately after its parent without a normal set number;
- remains loggable for the existing fields;
- cannot change set type or weight distribution;
- cannot create another continuation;
- can still be deleted.

`Continue last set` applies the same one-child rule to the latest eligible base set. `Add Set` keeps
its existing behavior.

## Durability boundary

| State | Durability in LOG-V4A |
|---|---|
| Existing normal Routine draft values/notes | Existing draft persistence unchanged |
| Existing completed normal set measurements | Existing Routine/Saved-One-Off save paths unchanged |
| Timer running/paused/manual/smart state | ViewModel session state; survives recomposition/sheet close, not process death |
| W/D set type and weight distribution | ViewModel/session presentation state only |
| Continuation parent/child identity | ViewModel/session presentation state only |
| Valid continuation measurements at final save | Saved as an ordinary logged set through the existing save contract |
| Advanced identity in saved History/Engine/recovery | Not persisted and not interpreted |

Routine draft snapshots intentionally omit continuation rows and advanced metadata so process restore
cannot misrepresent a continuation as a normal base set. No Room, draft schema, session schema,
History semantic or import/export migration was introduced.

## Validation

### Focused JVM tests

Command:

`.\gradlew.bat :app:testDebugUnitTest --tests com.hoctin.gymtracker.ui.log.RestSuggestionHeuristicTest --tests com.hoctin.gymtracker.ui.log.LogWorkoutTrackingInputTest --console=plain`

Result: **PASS — BUILD SUCCESSFUL**.

Twelve focused tests cover the heuristic, 75-second formatting, bodyweight blank-load behavior and
the existing rep/time/distance parsing contracts.

### Debug APK

Command:

`.\gradlew.bat :app:assembleDebug --console=plain`

Result: **PASS — BUILD SUCCESSFUL**.

Artifact: `app/build/outputs/apk/debug/app-debug.apk`.

### Graphify

Incremental code-graph update completed through the project-pinned interpreter:

`C:\Python314\python.exe -m graphify update .`

Result: **PASS** — `graphify-out/graph.json` and `GRAPH_REPORT.md` were rebuilt with 9,503 nodes,
14,793 edges and 854 communities. `graph.html` was intentionally skipped by Graphify because the
graph exceeds its 5,000-node visualization safety limit.

### Pixel 8 runtime QA and screenshots

The Pixel 8 AVD booted, but replacing its installed app failed safely with:

`INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.hoctin.gymtracker signatures do not match newer version`

The existing app was not uninstalled and its data was not cleared. Therefore the new APK was not run
on-device and no LOG-V4A after screenshots are claimed. IME position, modal height/motion, sticky
header behavior, next-exercise scroll and the two launch sources remain runtime acceptance items for a
device/emulator carrying a compatible signature or a disposable profile.

## Protected-surface verification

Room remains **v11**. Current hashes match the pre-edit baseline:

- `ProgressiveOverload.kt`: `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B`
- `ProgressiveOverloadEngineTest.kt`: `FF4DDE81CE2A225EC09BEDA89C21CF8E9EFC0D633A36E79EBB233C7B9666B947`
- `AppDatabase.kt`: `1D85E0D69A4C0B2B707D6D9F81D203827C46611FCA5F58865E71AA18004161E8`
- `Entities.kt`: `0805DC0BF8C1BFB528C73944AD79659ED783D93E570B282EC982558ED5340016`
- `GymRepository.kt`: `D841C2E71470C6C4E68864BFE9B707995FE6941E1563D048AC7515653E15F059`
- `RecoveryResearchRepository.kt`: `9ACE377FEA89FEA35C47BCD4A51A0831EB7CC93C8FB050E5D5D4DED296185999`
- `HistoryScreen.kt`: `6FEC1F1D621F123CCAF877E05EEEEC0D8BE8A6786CD265298725F61AFEA2525B`
- `OnboardingScreen.kt`: `541E5A57474FBE8B208969777304227C82E1C949C311589949FD66EFC485B04A`

No Room schema/version, Progressive Overload algorithm/test, History V2 layout/grouping,
wearable/Recovery Research implementation, Saved Workout persistence/provenance, catalog data/assets,
onboarding source or import/export contract was changed.

## Engine/data owner handoff

Before advanced set types become durable semantics, the owner must define a versioned model for:

1. persisted set type (`STRAIGHT`, `WARMUP`, `DROP`);
2. persisted weight distribution and how entered load is normalized;
3. continuation parent identity and ordering;
4. progression eligibility for warmups, drops and continuation work;
5. volume, PR, History, Muscle Recovery and Recovery Research interpretation;
6. draft/process-death behavior and import/export compatibility;
7. migration and backward-compatibility rules.

Until that contract exists, LOG-V4A intentionally treats every valid saved measurement as ordinary
logged work and never changes Engine behavior based on its temporary UI label.

## Known limitations / next acceptance step

- Run the debug APK on a disposable Pixel 8 profile or a device signed compatibly with the existing
  package.
- Verify the 20 requested runtime interactions, especially IME avoidance, sticky identity, modal
  heights/motion, idempotent timer opening, Next Exercise scroll, W/D badges and continuation locking.
- Capture the requested six after screenshots only after that compatible runtime pass.

## Follow-up fix (September 3, 2026) — bottom-sheet actions clipped by the system gesture bar

Addresses the post-QA report that the `Delete Set` (Advanced Set sheet) and `Close` (Progression
Target sheet) buttons were still submerged at the bottom of the screen.

### Symptom

The pinned `fixedFooter` action row rendered at the very bottom of the screen, partially hidden
behind the system navigation bar / gesture bar.

### Root cause — NOT a height problem

`AuronCompactModalSheet` (`ui/components/AuronBottomSheet.kt`) opened its `Dialog` with
`DialogProperties(decorFitsSystemWindows = true)`. **A Compose dialog whose decor fits the system
windows consumes the window insets, so every inset read inside the dialog resolves to `0`** —
including `navigationBarsPadding()` and `WindowInsets.navigationBars`.

The footer therefore sat only `8.dp` (sheet offset) + `12.dp` (footer padding) = **20dp** above the
true screen bottom, i.e. inside the gesture bar.

**Consequence for future work: raising `maxHeightFraction` or the button height cannot fix this.**
Two earlier attempts did exactly that (buttons 56dp → 64dp, sheet fraction 0.62 → 0.68 and
0.74 → 0.78) and did not help, because the sheet's own container was sitting in the gesture zone.
Do not iterate on heights for this class of bug — fix the inset plumbing.

The same defect is documented in-repo at `ui/home/DailyBoostSection.kt:688-694`, which already used
`decorFitsSystemWindows = false` for precisely this reason.

### Fix

1. `decorFitsSystemWindows` → `false`, so real insets reach the content.
2. Deterministic bottom lift with a floor, because gesture nav reports only a thin bar:
   `bottomClearance = maxOf(navigationBarBottom, 34.dp)`.
3. The lift is applied **before** the `heightIn / clip / background / border` chain, so the visible
   card itself clears the bar. Padding placed after `clip` only insets the content and leaves the
   card's own bottom edge in the gesture zone.
4. Footer container padding `vertical = 12.dp` → `top = 12.dp, bottom = 18.dp`.
5. Non-`fixedFooter` branch: dropped the now-redundant `navigationBarsPadding()`; trailing content
   padding `56dp` → `40dp` because 56dp had been tuned while the inset silently resolved to 0.

Button bottom clearance: **20dp → ~60dp** (gesture nav) / **~74dp** (3-button nav).

**Side effect that must be checked every time:** with `decorFitsSystemWindows = false` the scrim
(`Color.Black @ 0.68`) now paints over the status/nav bars. `GymTrackerApp.kt:807` sets
`isAppearanceLightStatusBars = !darkTheme`, so in **light theme** that leaves dark icons on a dark
scrim. The scrim is therefore inset out of the `systemBars` strips (padded *before*
`.background()`), keeping the content edge-to-edge.

### Blast radius

All 7 `AuronCompactModalSheet` call sites are bottom-anchored sheets, so all benefit:
`LogWorkoutScreen` ×4, `CatalogExerciseDetailSheet` ×2, `SavedWorkoutBuilderScreen` ×1.

### The rule

> A Compose `Dialog` with bottom-anchored content or pinned action buttons **must** use
> `decorFitsSystemWindows = false`. Any inset read inside a `decorFitsSystemWindows = true` dialog
> returns 0. The bug only bites full-height / bottom-anchored content — short centered confirm
> cards are unaffected, so the default is fine for them.

Swept all 5 `Dialog(` call sites in the app: `WorkoutDetailV3Screen.kt:468`,
`WorkoutDetailScreen.kt:213` and `OnboardingScreen.kt:346` use the default `true` but are short
centered cards with no inset reads — correctly left alone.

### Known limitation left in place

Fixed-footer sheets use a hard `.height(maxHeight)`. Combined with `applyImePadding = true`
(Progression Target has a note field), an open keyboard (~350dp) can push the sheet's top
off-screen. This pre-dates the fix and was deliberately not changed to avoid altering the layout
under QA. Candidate fix if reported: swap `.height(maxHeight)` for `.heightIn(max = maxHeight)`.
