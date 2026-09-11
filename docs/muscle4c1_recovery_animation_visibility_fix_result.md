# MUSCLE-4C.1 - Recovery Animation Visibility Trigger Fix - Result

**Status:** COMPLETE - Recovery bar animation no longer consumes on initial Home composition/precompose;
it waits until the first readiness-bar row is actually visible above the bottom dock.

**Backup:** `D:\HOCTIN\GYM_TRACKER_MUSCLE4C1_BACKUP_20260706_222334`

No Room schema change. No recovery engine/value/status/color semantic change. No `MuscleRecovery.kt`
change. No Progressive Overload Engine change. No BodyMap rendering change. No History/Plan/Settings
change.

---

## 1. Context read

Read before editing:
- `README.md`
- `docs/muscle4c_home_recovery_ui_polish_result.md`

Root confirmed: `D:/HOCTIN/GYM TRACKER`.

## 2. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeScreen.kt` | Added a keyed LazyColumn item and viewport visibility state for the Recovery item. |
| `app/src/main/java/com/hoctin/gymtracker/ui/home/MuscleRecoverySection.kt` | Delayed process-gate consumption until the first readiness-bar row is visible above the bottom dock. |
| `docs/muscle4c1_recovery_animation_visibility_fix_result.md` | This report. |

Verified unchanged by SHA-256 against the pre-edit backup:
- `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleRecovery.kt`
- `app/src/main/java/com/hoctin/gymtracker/domain/ProgressiveOverload.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/history/HistoryScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/plan/PlanScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt`

## 3. Implementation details

The old MUSCLE-4C gate was process-scoped, but it lived inside `MuscleRecoverySection`; LazyColumn could
compose/precompose the section before the user reached the bars, consuming the one-shot animation early.

New flow:
- `HomeScreen` gives the Recovery LazyColumn item a stable key: `home_muscle_recovery`.
- `HomeScreen` derives `recoveryCardVisible` from `listState.layoutInfo.visibleItemsInfo`.
- `MuscleRecoverySection` receives `animationVisibleTrigger`.
- Inside `MuscleRecoverySection`, the first group-tile row owns an `onGloballyPositioned` visibility
  check using `boundsInWindow()`.
- The bottom dock area is excluded with a 220dp bottom guard, so a row hidden behind the dock does not
  consume the animation.
- `HomeRecoveryReadinessAnimationGate.consume()` now runs only when all are true:
  `state.hasData && recovery item visible && first readiness-bar row visible above dock`.
- Once consumed, the process-scoped gate prevents replay on scroll, recomposition, and tab return.

No plain `LaunchedEffect(Unit)` was used for the trigger.

## 4. Build result

Using `JAVA_HOME=D:\jdk17\jdk-17.0.19+10`:
- `.\gradlew.bat :app:testDebugUnitTest` - BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug` - BUILD SUCCESSFUL

Existing AGP/compileSdk 35 warning remains; no new build failure.

## 5. Runtime QA

Emulator: `Pixel_8` / `emulator-5554`

Screenshots:
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_final_top_from_plan.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_final_before_bar_trigger.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_final_tiles_animating.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_final_tiles_settled.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_no_replay_tab_return_tiles.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c1_crash_filter.txt`

Observed:
- Opened app, switched Plan -> Home: Home showed Weekly Progress first; Recovery title/score could be
  partially visible lower on the screen, but readiness bars were still not visible.
- Scrolled until the body map and then the first bar row entered the visible area above the dock.
- Bars rendered at the correct final values (`Legs 100`, `Back 36`, `Chest 94`, `Shoulders 93`,
  `Arms 95`, `Core 90` on the live QA dataset). These values come from unchanged ViewModel/engine data.
- Scroll away/back and tab away/back did not replay the fill; the bar rows returned at final width.
- Crash buffer filtered for `com.hoctin.gymtracker`: clean.

Animation capture note:
- ADB screenshot latency did not capture a clear mid-tween frame; the "animating" and "settled" PNGs
  looked effectively the same. The code path now guarantees the animation starts from `0f` only after
  the bar row visibility trigger flips, and runtime QA verified the old early-consume condition no longer
  applies at Home top / body-map-only visibility.

## 6. Known limitations

- The visibility guard subtracts a fixed 220dp dock-safe area. This matches the current floating Home
  dock and prevents hidden-under-dock rows from consuming the animation. If the dock height changes
  substantially, this guard should be revisited.
- Mid-animation proof is code-based plus runtime timing observation, not a captured frame-perfect
  screenshot.

## 7. Next recommendation

No follow-up needed for recovery science. If more polish is desired, isolate it as a separate Home
bottom-dock/content-inset task rather than modifying recovery logic.
