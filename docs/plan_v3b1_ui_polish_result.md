# PLAN-V3B.1 — UI Polish Pass — Result

Date: 2026-08-15

## Outcome

PLAN-V3B.1 is a focused presentation and interaction pass over the validated PLAN-V3B flow. It does
not alter the Routine / Saved One-Off domain boundary, persistence, logging semantics or protected
training/recovery systems.

Backup created before edits:

`D:\HOCTIN\GYM_TRACKER_PLANV3B1_BACKUP_20260815_192215`

The backup contains `app_src`, `docs` and `README.md`. Signing material was not read or copied.

## Files changed

- `README.md`
- `docs/plan_v3b1_ui_polish_result.md`
- `app/src/main/java/com/hoctin/gymtracker/ui/theme/Theme.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/components/AuronBottomSheet.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/catalog/CatalogExerciseDetailSheet.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanPresentation.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanV3Screen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutsScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/SavedWorkoutBuilderScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/GenerateWorkoutUnavailableScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/detail/WorkoutDetailV3Screen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`

## Visual and interaction changes

### Shared canvas and Plan surfaces

- Increased saturation only in the upper dark-theme plum atmosphere. The lower canvas remains OLED
  black and the light theme is unchanged.
- Added PLAN-V3-local charcoal surfaces (`#0D0E11` / `#111218`) rather than changing cards globally.
- Applied the darker surface language to Plan action cards, Routine previews, Your Workouts,
  Generate Workout unavailable state and Routine Detail change/single-workout cards.
- New PLAN-V3 navigation cards use a restrained `0.982` press scale and a short fade/upward page
  settle; system-disabled animation is respected.

### Build a Workout

- ALL/SELECTED tabs, search and muscle filters are outside the catalog `LazyColumn`, so they remain
  pinned while rows scroll underneath.
- The complete exercise row remains the multi-select target. The nested thumbnail target opens the
  shared detail popup and consumes the pointer event; the circular selector remains available.
- The final named review is now a compact modal capped at 46% of screen height with internal scroll,
  dark scrim, true-dark surface and gesture-safe footer spacing.

### Shared Exercise Detail popup

- `CatalogExerciseDetailSheet` still owns the single popup used by Plan, Builder, Log Workout,
  History/Catalog entry points and other existing callers.
- The shared shell is capped at 70% of the visible screen and scrolls internally.
- The backdrop uses a 0.68 black scrim. The surface is OLED `#050609`, rounded 28dp with a restrained
  hairline border.
- Entrance motion uses a bottom-up Compose spring (`dampingRatio 0.90`, `stiffness 245`) plus a 220ms
  fade. This produces the video-reference feel: slower rise, tiny soft overshoot, one settle and rest,
  without a repeated/cartoon bounce. Exit is a 230ms downward settle/fade. Animator scale 0 removes
  the spring and shows the state directly.

### Your Workouts and Routine Detail

- Delete Workout and Edit Workout now share a fixed 56dp action height; Delete stays red and
  single-line, Edit stays white. Behavior is unchanged.
- Added `Start single workout` directly below Skip/Modify in Routine Detail. Its truthful copy is
  `Start a single workout outside of your plan.` and it navigates to the existing Your Workouts route.
- Existing Skip and Modify callbacks are unchanged.

### Log Workout

- Changed only the canvas and local card/inset colors to match current AURON plum/OLED presentation.
- Save, draft, progression, set input, info popup and Saved One-Off behavior were not changed.

## Validation

### Source/build

- Room verified at v11 before editing; no schema/version file was changed.
- Focused tests passed: `PlanPresentationTest`, `SavedWorkoutFoundationTest` and
  `LogWorkoutTrackingInputTest`.
- Debug APK: `:app:assembleDebug` — PASS.
- `graphify update .` — PASS; code graph rebuilt to 9,414 nodes / 14,580 edges.
- Existing compiler warnings remain; no new build error remains.

### Pixel 8 AVD runtime QA

- Builder header stayed pinned after scrolling.
- Normal row tap selected the exercise.
- Thumbnail tap opened Exercise Detail and left the selection circle unchanged.
- Shared detail popup opened from both Builder and Log Workout; the latter QA workout was exited
  without saving a session.
- Shared popup showed the 70% cap, dark scrim/surface and internal scrolling.
- Builder review stayed compact below half-screen and used internal scrolling.
- Routine Detail retained Skip/Modify and the new single-workout card opened Your Workouts directly.
- A temporary `PLANV3B1_QA` template confirmed the populated-card Delete/Edit actions have equal,
  single-line 56dp treatment. Its delete confirmation remained intact and the template was removed;
  no workout session was started or saved.
- No route crash was observed. History contained no sessions on this AVD, so Log Workout was used as
  the second live caller for shared-popup verification.
- Final logcat check was clean: no Gym Tracker fatal-exception or ANR marker in the current buffer.

Screenshots:

- `build/reports/plan_v3b1_home.png`
- `build/reports/plan_v3b1_plan_final.png`
- `build/reports/plan_v3b1_builder_scrolled.png`
- `build/reports/plan_v3b1_detail.png`
- `build/reports/plan_v3b1_thumbnail_consumed.png`
- `build/reports/plan_v3b1_row_selected.png`
- `build/reports/plan_v3b1_log_detail.png`
- `build/reports/plan_v3b1_detail_route_top.png`
- `build/reports/plan_v3b1_shortcut.png`
- `build/reports/plan_v3b1_review_final_inset.png`
- `build/reports/plan_v3b1_delete_edit_balanced.png`
- `build/reports/plan_v3b1_saved_cleanup.png`

## Protected-surface verification

No intended changes were made to:

- Room entities, DAO, migrations or schema exports;
- Saved Workout persistence/session-source contracts;
- Progressive Overload implementation or tests;
- onboarding source;
- History layout/grouping;
- Weekly Progress or Muscle Recovery layout/logic;
- FED catalog data, metadata or images;
- import/export;
- wearable or Recovery Research;
- save/draft and set-entry behavior.

The existing dirty worktree was preserved; no destructive Git operation or broad formatting was used.
All eight pre-recorded protected-file SHA256 values matched after implementation, including the
Progression Engine and test, Room database/entity owner, repository, Recovery Research, onboarding
and History screen. Room remained v11.
