# MUSCLE-4C.2 - Recovery Bar Animation Real Visibility Fix - Result

**Status:** COMPLETE - runtime verified. The first Home Muscle Recovery readiness bar now renders at
0 width until the actual bar track is visible above the bottom dock, then visibly fills from 0 to its
target once per app process.

No Room schema change. No recovery engine/scoring/value/status/color semantic change. No
`MuscleRecovery.kt` change. No HomeViewModel change. No BodyMap rendering change. No Progressive
Overload Engine change. No History/Plan/Settings change.

---

## 1. Exact root cause

MUSCLE-4C.1 fixed part of the trigger, but not the visible animation.

Two issues remained:

1. Each tile could still render a final-width bar before the user saw it because the previous tile state
   initialized to `targetFraction` when `animateReadiness == false`. Later code tried to reset to zero,
   but that was a reset-after-final-render pattern.
2. The visibility trigger was too coarse. It was attached to the first tile row and used a fixed 220dp
   bottom guard. On Pixel 8, the first bar track was visibly above the dock while the 220dp guard still
   classified it as not visible, so the trigger either missed the true first visible moment or never
   started at the visible bar position.

## 2. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/ui/home/MuscleRecoverySection.kt` | Replaced target-before-trigger bar state with an explicit one-shot `Animatable` state machine; moved visibility detection to the first actual bar track; adjusted dock guard to 96dp; added debug-only runtime animation logging. |
| `docs/muscle4c2_recovery_animation_real_visibility_fix_result.md` | This report. |
| `README.md` | Added MUSCLE-4C.2 note correcting the MUSCLE-4C.1 runtime visibility status. |

## 3. State machine / animation mechanism

`MuscleRecoverySection` now uses:

- `WaitingForVisibility`: bars render at `0f`; no target width is rendered before trigger.
- `Running`: the process gate starts once, each tile `Animatable.snapTo(0f)` then `animateTo(targetFraction)`.
- `Settled`: bars snap/render at target immediately and never replay in this process.

The one-shot process gate flips when the first actual readiness bar track intersects the usable viewport
above the bottom dock. Returning to Home, recomposition, rotation/config recreation, scroll away/back, and
tab away/back all see the process gate already started and render settled target values without replay.

## 4. Why this prevents precompose consumption

LazyColumn precompose can compose the Recovery card, but pre-trigger bars no longer initialize to their
target. They stay at `0f` while `WaitingForVisibility`.

The gate is not consumed by card composition or body-map visibility. It starts only after
`onGloballyPositioned` on the first bar track reports that the track itself is inside the viewport above
the dock. That means the first human-visible bar frame is zero/near-zero, not already final.

## 5. Runtime evidence

Emulator: Pixel 8 / `emulator-5554`

Runtime proof folder:

`C:\Users\hoang\AppData\Local\Temp\muscle4c2_runtime_verified_20260708_012459`

Screenshots:

- `00_top_before_any_bar.png`: Home top state; Weekly Progress visible, Recovery header lower on screen,
  readiness bars not visible.
- `01_before_bar_track.png`: body map visible; first readiness bar track still not visible.
- `anim_01.png`: first bar track visible with only low/near-zero fill.
- `anim_02.png`: same row at final fill.

Debug runtime log from the actual `Animatable` for the `Legs` bar:

```text
01:25:04.906 fraction=0.0
01:25:05.152 fraction=0.023732916
01:25:05.376 fraction=0.14755344
01:25:05.529 fraction=0.5874883
01:25:05.673 fraction=0.80718994
01:25:05.816 fraction=0.9041158
01:25:05.961 fraction=0.9644095
01:25:06.076 fraction=0.99280554
01:25:06.221 fraction=0.99959123
```

This proves the visible entrance run progressed from 0 to low partial, mid partial, and final. The debug
log is gated by `ApplicationInfo.FLAG_DEBUGGABLE`; release/non-debug installs do not need it for behavior.

## 6. No-replay verification

After the verified animation run:

- Cleared logcat, scrolled away, scrolled back: `no_replay_scroll_logcat.txt` length `0`.
- Cleared logcat, switched Home -> Plan -> Home: `no_replay_tab_logcat.txt` length `0`.

No extra `AuronRecoveryBars` entries appeared, so the animation did not replay.

## 7. Build result

Using local command env only:

```powershell
$env:JAVA_HOME='D:\jdk17\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

Results:

- `.\gradlew.bat :app:testDebugUnitTest --console=plain` - BUILD SUCCESSFUL
- `.\gradlew.bat :app:assembleDebug --console=plain` - BUILD SUCCESSFUL

Existing AGP/compileSdk 35 warning remains unchanged.

## 8. Crash check

Crash buffer filtered after runtime QA:

```text
clean: no com.hoctin.gymtracker crash entries
```

## 9. Limitations

- Screenshot capture still jumps from low fill to final because ADB screencap latency is slow. The
  mid-animation proof comes from runtime `Animatable` log values, which the task explicitly allowed.
- The bottom dock guard is still a constant, now 96dp. It matches the current Pixel 8 floating dock QA
  and avoids the previous too-conservative 220dp miss. If the dock height changes substantially, this
  guard should be revisited with another runtime visibility pass.
