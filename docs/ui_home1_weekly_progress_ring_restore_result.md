# UI-HOME.1 — Weekly Progress Ring Restore Result

Date: 2026-07-28  
Project root: `D:\HOCTIN\GYM TRACKER`

## Result

UI-HOME.1 restores the existing animated `ProgressRing` as the single primary Weekly Progress
summary on Home while preserving the current Consistency calculator, route, calendar, milestones,
evidence receipts, and Active Streak behavior.

This is a presentation merge, not a rollback. The old Weekly Hub, Recent sessions, expandable workout
cards, and workout exercise tables were not restored.

## Backup

The verified pre-edit filesystem backup is:

`D:\HOCTIN\GYM TRACKER\_backup_uihome1_full_20260728_001149`

- 1,504 files
- 107,165,653 bytes
- Includes Room schema exports, `AndroidManifest.xml`, catalog assets, and the Lottie raw resource.

An earlier copy at `_backup_uihome1_20260728_001127` was incomplete because its exclusion rule was too
broad. It was not used as the safety baseline and was not treated as a valid backup.

## Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeConsistencyModule.kt` | Restored the Ring summary, retained the horizontal streak signal, differentiated activity evidence, and fixed the post-animation flame state. |
| `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeScreen.kt` | Updated the placement comment to describe the Ring/Boost responsibility boundary. No Home state or navigation implementation was replaced. |
| `app/src/test/java/com/hoctin/gymtracker/ui/home/ConsistencyCalculatorTest.kt` | Added exact boundary coverage for the four shared Weekly Progress color bands. |
| `README.md` | Reconciled the current Ring, Consistency, Active Streak, History V2, catalog, schema, and engine state. |
| `docs/ui_home1_weekly_progress_ring_restore_result.md` | This implementation and validation report. |

Before this report was added, a full hash comparison of `README.md`, `app/src`, Gradle configuration,
and `docs` against the backup found only the four intended existing-file modifications above.

## Why the Ring was restored

The UI-HOME replacement put a weekly rail and two aligned day lanes on Home. Those elements were useful
as detailed evidence, but they made the primary weekly answer slower to read and removed the strong,
animated focal point of the former Ring card.

Home now answers the weekly question with one glance:

`How many scheduled workout days have I completed this week?`

The richer rail, week ribbon, schedule state, activity evidence, milestones, and calendar remain on the
dedicated Consistency route, where that detail has enough space.

## Home behavior: before and after

| Before UI-HOME.1 | After UI-HOME.1 |
|---|---|
| Home showed a weekly rail and week ribbon. | Home shows one animated 96 dp Ring. |
| Weekly information competed with the Active Streak block. | Ring summary and horizontal Active Streak are separate, compact signals. |
| Weekly completion was repeated across summary, rail, and lane. | Ring center shows only `<completed>/<target>`; adjacent text shows one plain-language summary. |
| The Lottie fire disappeared after about five seconds. | Lottie plays for about five seconds, then a guaranteed-visible static two-tone flame remains. |
| Activity sources were primarily distinguished by the earlier mint/purple treatment. | Workout uses a red circular check, Boost a purple diamond/bolt, and Both a nested red/purple marker with a check. |

The Home enclosure follows the AURON Performance Intelligence language: purple-to-black ambient
canvas, a restrained semi-transparent dark-purple surface, hairline border, and typography-led
hierarchy. It does not restore the old gray slab or hard bottom-right shadow.

## Weekly Progress semantics

- Source of truth remains `ConsistencyCalculator`.
- Home directly reuses:
  - `state.weeklyCompleted`
  - `state.weeklyTarget`
  - `state.weeklyProgress`
- Weekly target follows selected training weekdays with the existing plan-size fallback.
- Completed progress counts distinct saved-workout dates in the current ISO week.
- Multiple saved workouts on one date count once.
- Daily Boost never increases Weekly Progress.
- The existing `ProgressRing` owns the smooth fill animation.
- One shared pure `weeklyProgressColor` function provides:
  - `0–24%`: red
  - `25–49%`: orange
  - `50–74%`: yellow-green
  - `75–100%`: green
- Home does not add a duplicate percentage, progress rail, or week ribbon.
- A completed goal adds the short message `Weekly goal complete.`
- The full summary and the 48 dp `View More` action both invoke the existing Consistency navigation
  callback.

The Ring exposes a progress semantic range and a spoken description of completed scheduled workout
days. The summary is exposed as a button that opens Consistency details.

## Active Streak semantics

- Active Streak remains the union of Workout and Daily Boost activity dates.
- Workout + Boost on the same date is `BOTH`, but increments the streak only once.
- Today continues to use the existing `Active today`, `Continue today`, and `Start today` states.
- `bestActiveStreak` and `lastCelebratedEpochDay` remain presentation-only persistence.
- The first qualifying activity can consume that day's celebration.
- A second activity source on the same epoch day cannot replay it.
- System animator scale `0` skips spring and Lottie playback and renders the settled flame immediately.

### Fire disappearance: confirmed root cause and fix

`auron_streak_fire.json` is a 24 fps composition from frame `0` to frame `26`; its visible layer ends at
frame `25.85`. The old implementation retained Lottie at exact progress `1f` after five loops, which is
the transparent final composition frame. That is why the image disappeared after approximately five
seconds.

The animation still runs for five loops at `1.08x` speed, approximately 5.0 seconds in total. When
playback reaches the transparent end frame, the composable now swaps to a static filled flame rather
than retaining `progress = 1f`. Inactive state uses an outlined flame.

## Consistency and navigation preserved

The existing full-screen Consistency route remains responsible for:

- current streak hero
- best recorded streak
- today's continuity state and static flame
- workout-only weekly rail and ribbon
- milestones at 7, 14, 30, and 60 days
- month navigation
- workout schedule and activity evidence layers
- Workout, Boost, and Both markers
- selected-day evidence receipt
- workout receipt navigation to `SessionReviewScreen`
- Boost-only receipt without invented exercise data or timestamps

The existing non-top-level route mechanism still hides the floating navigation. The floating
navigation implementation itself is byte-identical to the backup.

## Validation

Known-good toolchain: JDK 17 at `D:\jdk17\jdk-17.0.19+10`.

| Command | Result |
|---|---|
| `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS — 365 tests, 0 failures, 0 errors, 0 skipped |
| `.\gradlew.bat :app:assembleDebug --console=plain` | PASS |
| `.\gradlew.bat :app:assembleRelease --console=plain` | PASS |

Focused results:

- `ProgressiveOverloadEngineTest`: 152 tests, 0 failures, 0 errors.
- `ConsistencyCalculatorTest`: 12 tests, 0 failures, 0 errors.
- Exact Weekly Progress color boundaries are covered at `0`, `0.249`, `0.25`, `0.499`, `0.50`,
  `0.749`, `0.75`, and `1.0`.
- Existing Consistency tests retain coverage for distinct workout dates, Boost exclusion, ISO week
  rollover, Workout/Boost/Both union, pending-today grace, historical gaps, best/current streak,
  milestones, calendar boundaries, and once-per-day celebration.

Artifacts:

| Artifact | Size | SHA-256 | Signature |
|---|---:|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 98,819,174 bytes | `BC733411A7A43CDBE7DF75BE037EFC764F6E6F883DFEE1E737E0FD80971651E7` | APK Signature Scheme v2 verified; Android Debug signer |
| `app/build/outputs/apk/release/app-release.apk` | 91,737,702 bytes | `ED5DF22F1536B58508F09E579D53AD8CEC692929A810B22E8EBF1CCB2D5FE0FA` | APK Signature Scheme v2 verified; existing GYMATE release signer |

The existing Android Gradle Plugin `compileSdk` compatibility warning was unchanged and did not fail
tests or either APK build.

## Protected-surface verification

SHA-256 comparisons against the verified backup found:

- 28 explicitly protected implementation/test files: byte-identical
- all 8 Room schema exports: byte-identical
- catalog JSON: byte-identical
- catalog count: 577
- catalog SHA-256:
  `C09E400ECC0289FC3AA408E306750209F06327796EF019A305B6F432F3BD4D69`
- `CatalogSeed.CONTENT_VERSION`: 16
- Room database version: 9
- old `WeeklyProgressHubSheet.kt`: absent
- Home `Recent sessions`: absent

The protected comparison includes the Progression Engine and its test, Room database/entities/DAO,
schemas, catalog seed/data, `GymRepository`, user preferences and draft repositories, Log save/draft
ViewModel, History V2 screen/ViewModel/grouping mapper, Daily Boost domain/UI completion path, Muscle
Heat/Recovery domain and UI map files, `HomeViewModel`, the Consistency calculator/store/screen,
`ProgressRing`, Settings import/export surfaces, and `GymTrackerApp` floating navigation.

## Runtime QA and screenshots

Runtime screenshot recovery was stopped immediately after the emulator/ADB hard error, as requested.
`adb devices -l` started the daemon but did not return an attached device, so no new post-change
screenshots were captured and no runtime interaction is claimed.

Screenshot matrix:

| Requested capture | Result |
|---|---|
| Home with restored Ring | Not captured — emulator unavailable |
| Home with active fire | Not captured — emulator unavailable |
| Home with settled flame | Not captured — emulator unavailable |
| Consistency hero | Not captured — emulator unavailable |
| Consistency calendar | Not captured — emulator unavailable |
| Selected workout receipt | Not captured — emulator unavailable |
| Boost-only receipt | Not captured — emulator unavailable |

Dark theme, Light theme, font scaling, TalkBack, reduced-motion runtime behavior, Ring/`View More`
navigation taps, receipt navigation, and crash-buffer inspection therefore remain device QA items. The
reduced-motion branch, touch targets, semantics, and unchanged navigation were verified statically and
compiled successfully.

## Muscle Recovery color investigation

The reported symptom is visible in the supplied screenshots:

- the small body-map fragment underneath the floating dock contains green/yellow/orange recovery
  colors;
- the centered body map in the expanded Recovery view contains no green/yellow/orange/red fill even
  though the tiles show Back at 79% (`Primed`) and Shoulders at 58% (`Recovering`).

Offline pixel inspection confirmed that difference. Source inspection found no scroll-position branch
that intentionally removes recovery color. `MuscleRecoverySection.kt`, `BodyMap.kt`, the recovery
domain model, and the Home recovery mapping are byte-identical to the pre-edit backup; older QA imagery
also shows colored regions rendering with those same source files.

Therefore:

- screenshot symptom: **confirmed**
- source-level cause: **not confirmed**
- runtime root cause: **UNKNOWN**

Because ADB could not attach to the emulator, the live render tree/state could not be inspected. No
speculative Body Map or Recovery-model change was made in this UI-HOME.1 task.

## Known limitations

- No fresh emulator screenshots or interactive runtime QA were possible after the hard ADB attachment
  failure.
- The Recovery body-map color disappearance remains an independently reproducible runtime issue to
  diagnose with an attached device; this task deliberately does not guess at or patch its cause.
- UI-HOME.1 does not redesign the full Consistency calendar or change its data responsibilities. It
  restores Home hierarchy while preserving the existing detailed route.

