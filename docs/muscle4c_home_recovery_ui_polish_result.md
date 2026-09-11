# MUSCLE-4C - Home Recovery UI Polish - Result

**Status:** COMPLETE - Home Recovery presentation polished to the newer OLED visual language; recovery
engine untouched; build and emulator QA passed.

**Backup:** `D:\HOCTIN\GYM_TRACKER_MUSCLE4C_BACKUP_20260706_012007`

No Room schema change. No `MuscleRecovery.kt` change. No recovery algorithm/readiness/status/score/color
semantic change. No Progressive Overload Engine change. No signing/catalog/import/MUSCLE-4 expansion.

---

## 1. Context read

Read before editing:
- `README.md`
- `docs/history2_workout_archive_redesign_result.md`
- `docs/muscle4b_recovery_intelligence_ui_redesign_result.md`
- `docs/muscle3b1_heat_review_duration_backmap_fix_result.md`

Subagent UI summary:
- Newest screens use compact spacing, restrained hierarchy, near-black/OLED surfaces, thin 1dp borders,
  controlled purple/black background visibility, and depth through tonal layers/edges rather than heavy
  Material slabs.
- Premium feel comes from restraint: one visual hero, low-noise surfaces, compact typography, and no
  decorative motion.

## 2. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/ui/home/MuscleRecoverySection.kt` | UI-only polish and one-shot readiness bar animation. |
| `docs/muscle4c_home_recovery_ui_polish_result.md` | This report. |

Verified unchanged by SHA-256 against the pre-edit backup:
- `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleRecovery.kt`
- `app/src/main/java/com/hoctin/gymtracker/domain/ProgressiveOverload.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`

## 3. UI changes

Before:
- Recovery section used a normal `Card` surface and inset/tile fills that read closer to gray Material
  cards.
- Group tiles looked like independent inset cards.
- Body-map stage was correct functionally but visually heavier than the newer History V2 / Heat Review
  OLED language.

After:
- Main recovery section is now a custom rounded column with a subtle transparent OLED brush and thin
  border, instead of an opaque Material `Card`.
- Body map remains the visual hero, slightly taller, on a near-black rounded stage with a thin edge.
- Group tiles use transparent dark gradients plus border/edge definition, not solid gray slabs.
- Readiness pill and legend were tightened to match the compact dashboard language.
- Existing labels, percentages, body-map paints, nudges, and legend semantics are unchanged.

## 4. Animation implementation

Readiness bars now animate once per app process:
- A UI-only process gate (`HomeRecoveryReadinessAnimationGate`) is consumed only when data exists.
- Each bar starts from `0f`, waits one frame with `withFrameNanos`, then animates to the ViewModel
  percentage using an 850ms `FastOutSlowInEasing` tween.
- Fill color is derived from the existing target color and track color during the fill; final color is
  still exactly the ViewModel/recovery status color.
- After the gate is consumed, recomposition, LazyColumn disposal/re-entry, Home return, and configuration
  changes render bars immediately at their final values.
- Killing the app process resets the object, so a full app restart may replay as requested.

No body map animation was added.

## 5. Runtime QA evidence

Emulator: `Pixel_8` / `emulator-5554`

Screenshots:
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c_final_after_patch.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c_final_tiles.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c_no_replay_scroll_return.png`
- `D:\HOCTIN\GYM TRACKER\qa_muscle4c_no_replay_rotation_stable.png`

Observed on emulator:
- Home opens and Recovery dashboard renders with `77% Primed`.
- Body map remains the hero and uses the same recovery paints as before.
- Group values render unchanged on the QA dataset: Legs 99, Back 4, Chest 92, Shoulders 89, Arms 85,
  Core 93.
- Scroll away and immediately return: visible bars stayed at final width, no replay.
- Rotation/configuration return: bars stayed at final width, no replay.
- Bottom dock still overlays the lowest part of long Home content as before; not changed in this task.
- Crash buffer filtered for `com.hoctin.gymtracker` after QA: clean.

Animation caveat:
- The initial 0-to-target fill is implemented in code, but was difficult to capture frame-perfect on this
  emulator because the app splash and below-fold Home layout delay access to the Recovery section. The
  no-replay cases were verified visually with screenshots, and the start-from-0 path is enforced by the
  `withFrameNanos` animation setup.

## 6. Build result

Using `JAVA_HOME=D:\jdk17\jdk-17.0.19+10`:
- `.\gradlew.bat :app:testDebugUnitTest` - BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug` - BUILD SUCCESSFUL

Existing AGP/compileSdk 35 compatibility warning still appears; no new build failure.

## 7. Known limitations

- No UI screenshot was captured before editing in this MUSCLE-4C pass; the before/after comparison above
  is based on the pre-edit source and MUSCLE-4B report.
- Full initial animation capture was not frame-perfect for the reason stated above.
- The Home bottom navigation can still cover the very bottom of the long Recovery dashboard at some
  scroll positions. This existed outside the task scope and was not redesigned.

## 8. Next recommendation

If continuing MUSCLE work, the next safest polish is bottom-dock/content inset tuning for long Home
sections, but only as a separate UI-only task. Recovery science should remain frozen.
