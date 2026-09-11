# PLAN-V3B — Full Plan / Routine / Your Workouts UI — Result

Date: 2026-08-13

## Outcome

PLAN-V3B is implemented on top of the validated PLAN-V3A Room v11 foundation. The connected product
paths now exist in source and compile together:

```text
Routine
Plan -> Routine -> Routine Detail -> Start / Modify / Skip -> Log -> History

Saved One-Off
Plan -> Your Workouts -> Create/Edit -> ALL / SELECTED -> Review -> Save
     -> Start Session -> Log -> History
```

The two domains remain separate. A Saved One-Off template is never converted to a `WorkoutDayEntity`,
never enters the selected-Routine/Home lifecycle, and never uses the normal Routine save/advance path.

## Safety baseline and backup

- Real project root: `D:\HOCTIN\GYM TRACKER`.
- Pre-edit backup: `D:\HOCTIN\GYM_TRACKER_PLANV3B_BACKUP_20260813_204938`.
- Preserved: `app/src`, `docs`, `README.md`, and Room schema exports.
- The working tree was already extensively modified/untracked before PLAN-V3B. No reset, checkout,
  clean, stash, or broad formatting operation was used.
- Signing secrets were not read or copied.

## Reference-image gate

All nine task references were available and visually inspected before editing:

1. Main Plan hierarchy, two primary entries, and compact Routine previews.
2. Your Workouts list, explanatory copy, chevrons, and fixed white creation CTA.
3. Builder ALL tab: title/tabs, search, horizontal filters, open catalog rows and selectors.
4. Builder SELECTED tab: instruction, open selected rows, red minus controls and reorder intent.
5. Final save/review: name field, compact selected rows, Close and white Save actions.
6. Expanded Saved Workout: Delete/Edit/Start actions and compact exercise evidence rows.
7. Existing Exercise Detail / Progression / History behavior.
8. Routine Detail `Change today's workout` composition with Skip and Modify actions.
9. Existing AURON Home Hero, which represents the selected Routine only.

The implementation uses Images 2–6 and 8 as strong composition references, while retaining current
AURON purple-to-OLED atmosphere, open exercise rows, restrained neutral surfaces and white primary CTAs.

## Architecture findings and Room status

- Room remains **version 11**.
- PLAN-V3A already owns `saved_workouts`, `saved_workout_exercises`, typed session provenance,
  `SavedWorkoutRepository`, JSON schema v6, Weekly-vs-Streak separation and template-safe History.
- PLAN-V3B introduced no Room entity, DAO, migration, schema-export or import/export change.
- Existing FED catalog rows and thumbnail assets are reused; no catalog copy or parallel exercise source
  was introduced.

## Main Plan and Routine

The Plan destination now renders `PlanV3Screen`:

- prominent `Your Workouts` and `Generate Workout` entries;
- `Generate Workout` opens a truthful polished `Unavailable — coming soon` destination and does not
  claim or simulate AI generation;
- real Routine days remain sourced from `PlanViewModel`;
- each Routine preview shows its real name, summed target-set count, up to four thumbnail/name rows and
  `and N more` when required;
- the whole Routine card opens detail; Rename/Delete/Add remain reachable.

Routine Detail now contains:

- existing Routine hero and Start Workout confirmation;
- existing Exercise Detail navigation, preserving canonical History and Progression wording;
- compact open exercise rows with thumbnail and muscle hashtags;
- swipe right-to-left for a red delete action;
- swipe left-to-right for a white edit action;
- long-press vertical reorder with haptic feedback and no permanent handle;
- add-from-FED and custom-exercise entry points;
- `Change today's workout`, using actual persisted Plan order for Skip and the current Routine editor for
  Modify. No Routine names are hardcoded.

Home no longer renders the redundant direct Routine chip row. Its Hero still reads the existing persisted
selected Routine and opens the same Routine Detail; Saved One-Off templates cannot enter this state.

## Your Workouts and Builder

`Your Workouts` observes PLAN-V3A templates plus their real completion counts. A row can expand to show:

- `DELETE WORKOUT`: deletes only the template; History remains intact by v11 design;
- `EDIT WORKOUT`: preloads the same template id, name, selection and order;
- `START SESSION`: opens typed Saved One-Off logging and is disabled for an empty template;
- ordered thumbnail/name/hashtag exercise rows.

The builder is one coherent create/edit flow:

- `ALL`: real FED catalog, search, horizontal muscle filters, open rows, catalog metadata, hashtags and
  circular multi-select controls;
- generic `SHOULDERS` remains hidden while front/side/rear delts remain available;
- `SELECTED`: red circular remove actions and long-press reorder;
- review: editable workout name, tighter thumbnail/name rows, remove/reorder, Close and white Save;
- create/update persists only through `SavedWorkoutRepository`.

## Typed Saved One-Off Log integration

`LogWorkoutViewModel` now accepts a presentation launch contract:

- `LogWorkoutLaunch.Routine(dayId)` preserves the existing Routine initialization, draft, save,
  Recovery Research and auto-advance lifecycle;
- `LogWorkoutLaunch.SavedOneOff(savedWorkoutId)` loads the ordered template/prescription snapshots and
  reuses the existing Log UI without creating a fake Routine/day.

On Saved One-Off completion:

- `GymRepository.saveSavedOneOffWorkoutSession(...)` is used;
- the saved session has `dayId = null` and normal set/name/canonical snapshots;
- selected Routine is not updated and no Routine auto-advance runs;
- it appears in History and canonical evidence and counts Active Streak/Muscle Recovery through the
  PLAN-V3A source contract;
- it remains excluded from Weekly Progress;
- no Routine draft is persisted for the one-off launch.

The accepted Recovery Research limitation is preserved: a Saved One-Off session does **not** create a
Research observation because the protected Research API still requires a real Routine day. No fake
`WorkoutDayEntity`, heuristic rewrite or wearable change was introduced.

## Files changed

### Modified

- `app/src/main/java/com/hoctin/gymtracker/MainActivity.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt`
- `README.md`

### Added

- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanPresentation.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanV3Screen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/RoutineExerciseList.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailV3Screen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutsViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutsScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutBuilderViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutBuilderScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/GenerateWorkoutUnavailableScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/plan/PlanPresentationTest.kt`
- `docs/plan_v3b_full_plan_routine_saved_workouts_result.md`

The previous public Plan/WorkoutDetail functions were renamed to private legacy implementations in their
original files while the V3 screens became the public entry points. This minimized destructive rewrites in
the dirty tree and provides a filesystem-backup rollback path; only the V3 entry points are navigated.

## Validation

Final source after the explicit polish pass:

| Validation | Result |
|---|---|
| `:app:testDebugUnitTest` | PASS — 426 tests, 0 failures/errors/skips |
| `:app:assembleDebug` | PASS — `app/build/outputs/apk/debug/app-debug.apk` |
| `:app:assembleRelease` | PASS — `app/build/outputs/apk/release/app-release.apk` |
| Room version | 11 |
| `git diff --check` | PASS; only repository-wide line-ending warnings |

One attempted combined validation timed out and was not counted as evidence. A subsequent offline retry
failed during configuration because the isolated workspace cache did not contain all AGP/Kotlin artifacts;
this was not a source failure. The three requested tasks were then run independently against the machine's
standard Gradle cache and passed as shown above.

## Runtime and visual QA

`adb devices -l` returned no connected devices, and the installed emulator tooling listed no AVDs.
Therefore no install, destructive data reset, connected instrumentation run, crash-buffer read or runtime
screenshot capture was possible in this environment. Screenshots are intentionally **not fabricated**.

Source-level comparison was completed against all nine references. The explicit post-comparison polish
corrected Image 8's action separator: an accidental outline around the entire right action was replaced by
the intended horizontal hairline plus one central vertical divider.

Visual status by reference:

1. Implemented in source: Plan hierarchy and compact Routine previews; runtime screenshot pending.
2. Implemented in source: Your Workouts list and fixed white CTA; runtime screenshot pending.
3. Implemented in source: ALL tab/search/filter/open rows; runtime screenshot pending.
4. Implemented in source: SELECTED rows/remove/reorder/white CTA; runtime screenshot pending.
5. Implemented in source: final name/review/Close/Save composition; runtime screenshot pending.
6. Implemented in source: expanded template actions/evidence rows; runtime screenshot pending.
7. Existing Exercise Detail/Progression destination reused without semantic change.
8. Implemented and source-polished: truthful Skip/Modify card and separators; runtime screenshot pending.
9. Existing AURON Hero preserved as Routine-only; direct switch chips removed; runtime screenshot pending.

Runtime acceptance still required on a disposable or user-approved Pixel 8/device: gesture arbitration,
builder create/edit persistence, one-off completion/History navigation, selected-Routine invariance, font
scaling, light theme, crash/ANR buffers and screenshots for Images 1–9.

## Protected-surface verification

Compared with the fresh PLAN-V3B backup, PLAN-V3B did not alter:

- `ProgressiveOverload.kt` or `ProgressiveOverloadEngineTest.kt`;
- Room version/entities/DAO/migrations/schema exports;
- `GymRepository` or PLAN-V3A import/export contract;
- FED catalog source/metadata/assets;
- History V2 implementation/grouping;
- onboarding implementation/ownership;
- wearable implementation;
- Recovery Research implementation/heuristics;
- floating-bottom-navigation implementation.

The `GymBottomBar` implementation segment is byte-for-byte text-equivalent to the fresh backup; only
route wiring above it changed so pushed Plan/Builder/Log screens continue to use the existing dock-visibility
mechanism.

The only Recovery-related behavior is consumption of the already-approved PLAN-V3A Saved One-Off session
contract; the known Research-observation abstention remains unchanged.

## Known limitations and rollback

- Runtime/device acceptance and the requested after-state screenshots are pending because no device/AVD
  was available.
- Horizontal swipe versus long-press vertical reorder compiles with deliberate gesture separation, but
  tactile arbitration must still be exercised on a real pointer/touch target.
- Generate Workout is intentionally unavailable; no AI or deterministic generator is claimed.
- Recovery Research remains Routine-only for observation capture.

Rollback does not require a database downgrade because Room stayed v11. Restore the listed modified/added
source files, README and docs from `D:\HOCTIN\GYM_TRACKER_PLANV3B_BACKUP_20260813_204938`; retain the v11
user database and PLAN-V3A template/history data. Never downgrade Room or delete Saved Workout tables.
