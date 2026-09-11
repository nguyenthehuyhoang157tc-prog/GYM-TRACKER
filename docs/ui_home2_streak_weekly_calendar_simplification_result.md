# UI-HOME.2 — Streak / Weekly / Calendar Simplification Result

Date: 2026-07-29  
Project root: `D:\HOCTIN\GYM TRACKER`

## Result

UI-HOME.2 is implemented as a focused presentation refinement of UI-HOME.1. The animated,
workout-only Weekly Progress Ring remains the primary Home summary. The large Active Streak stat
block is replaced by a compact rolling seven-day strip, while the pushed detail route is visibly
titled `Streaks` and now reads as a quiet continuity view instead of a second dashboard/archive.

No legacy Weekly Hub, Recent sessions, reward economy, XP, chest, or parallel weekly/streak state was
restored.

The graphify ownership query kept the patch within the current flow:

- `HomeConsistencyModule` owns the Home Ring and streak presentation.
- `ConsistencyScreen` owns the pushed milestones/weekly/calendar/selected-day presentation.
- `GymTrackerApp` continues to own route visibility, floating-dock hiding, and Session Review
  navigation; it was not modified.

The redesign skill influenced the result through hierarchy reduction, solid schedule-state tiles,
fewer nested surfaces, and responsive seven-column composition. It did not introduce a new UI
framework or dependency.

## Backup and baseline

Fresh pre-edit filesystem backup:

`D:\HOCTIN\GYM TRACKER\_backup_uihome2_20260729_224058`

Backup scope:

- 10,354 files
- 812,753,139 bytes
- `.git`, `.gradle`, build output, graph output, prior backups, keystores, and signing-property files
  were excluded.

Protected baseline manifest:

`D:\HOCTIN\GYM TRACKER\_backup_uihome2_20260729_224058\UIHOME2_PROTECTED_HASHES.csv`

The manifest contains SHA-256 values for 1,256 protected files covering engine/tests, Room/data,
repositories, catalog data/assets, History, Log/Save/Draft, Session Review, Daily Boost, Muscle
Heat/Recovery, navigation, `ProgressRing`, theme/build files, and the Lottie source/runtime asset.

## Files changed

Product/source:

- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeConsistencyModule.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/ConsistencyScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/home/ConsistencyPresentationTest.kt` (new,
  presentation-only)
- `README.md`
- `docs/ui_home2_streak_weekly_calendar_simplification_result.md` (this report)

Generated graph maintenance:

- `graphify-out/graph.json`
- `graphify-out/graph.html`
- `graphify-out/GRAPH_REPORT.md`
- `graphify-out/manifest.json`
- `graphify-out/.graphify_labels.json`
- `graphify-out/.graphify_root`

No state, calculator, persistence, repository, schema, navigation, or theme source file changed.

## Home before / after

Before UI-HOME.2:

- The Ring center showed only `<completed>/<target>`.
- A large nested Active Streak block repeated the streak number, today status, evidence source, and
  explanatory copy.
- The Home streak presentation did not provide the compact seven-day scan requested by the visual
  reference.

After UI-HOME.2:

- The existing 96 dp `ProgressRing` remains byte-unchanged.
- Ring center line 1 is `<completed>/<target>`.
- Ring center line 2 is the rounded integer percentage.
- The percentage appears visually once; no progress rail or duplicate percentage badge is added to
  Home.
- `Weekly Progress`, `<completed> of <target> workout days`, and the 48 dp `View More` action remain
  beside the Ring.
- The whole Ring summary and `View More` continue opening the pushed detail route.
- The large streak content is replaced by `Streak` plus seven equal columns ending on today.

## Weekly Progress semantics

The current `ConsistencyCalculator` remains the single source of truth:

- Target comes from selected training weekdays, with the existing plan-size fallback.
- Completed work is distinct saved-workout dates in the current ISO week.
- Multiple workouts on one date count once.
- Daily Boost never increases Weekly Progress.
- Monday rollover behavior is unchanged.
- Ring/rail semantic colors remain:
  - 0–24% red
  - 25–49% orange
  - 50–74% yellow-green
  - 75–100% green
- Existing smooth Ring and rail animation remains intact.

## Home Streak semantics and motion

The Home strip deliberately hides the evidence source:

- Workout, Daily Boost, and Both map to the same active flame.
- An active date increments streak continuity once.
- Historical active flames are static.
- Only a newly active **today** cell receives the Lottie playback request.
- Past inactive dates are solid muted-red circles with a high-contrast `X`.
- Today pending is a neutral circle with a purple outline.
- Dates before tracking are neutral and never presented as failures.

The existing presentation store remains byte-unchanged:

- `lastCelebratedEpochDay` still prevents replay after returning to Home.
- A second activity source on the same date still does not replay the fire.
- `bestActiveStreak` remains presentation-only persistence.

The existing `StreakFire` lifecycle remains:

- 5 iterations at approximately 1.08× (about five seconds)
- spring pop when system motion is enabled
- swap to a guaranteed-visible static two-tone flame after playback
- immediate static rendering when system animator scale is zero

This retains the UI-HOME.1 fix for the Lottie asset's transparent final frame; the flame no longer
disappears after playback by holding progress at `1f`.

## Streaks detail screen

Visible structure:

1. Restrained app bar titled `Streaks`
2. Current-streak hero
3. Milestones
4. Weekly workout
5. Month calendar
6. Selected-day detail

Hero:

- large current-streak number on the left
- `current streak` label
- large static flame on the right
- no best/status/evidence/counted copy in the hero

Milestones:

- retained at 7/14/30/60 days
- rendered as a quiet horizontal checkpoint line
- no reward, XP, season, chest, or economy treatment

Weekly workout progress:

- one primary `<completed> of <target>` summary
- one animated threshold-colored rail
- one Monday–Sunday row
- no Boost explanation, activity lane, or duplicate percentage
- solid schedule grammar:
  - Done: mint + check
  - Missed: muted red + `X`
  - Planned: deep purple + date
  - Rest: near-black/neutral + date
  - Today: purple outer outline while preserving the underlying state

Calendar:

- one visible axis: `WORKOUT SCHEDULE`
- seven equal columns derived from available width
- no horizontal scrolling at the normal phone layout or 360 dp design width
- no top-right schedule glyph, nested activity marker, or `ACTIVE EVIDENCE` legend
- Boost-only and Both days keep their workout-schedule appearance; source disclosure occurs only in
  selected-day details
- selected/today cells use a purple outer outline without replacing their schedule state

Selected-day detail:

- no permanent `Evidence receipt` card or nested text cards
- compact full-date heading
- workout row shows real saved title/time; the whole row opens Session Review through the unchanged
  callback
- Boost-only line is exactly `Daily Boost completed`
- Both displays workout row(s) plus the Boost line
- empty/pre-tracking dates use at most one quiet line

## Presentation tests

New pure presentation tests cover:

- Workout/Boost/Both mapping to one Home active visual
- past inactive vs today pending vs before-tracking Home states
- Boost-only calendar cells retaining workout-schedule visual state
- before-tracking calendar dates never mapping to Missed

The existing calculator suite remains unchanged and continues to cover weekly distinct dates,
Boost exclusion, rollover, Workout/Boost/Both union, today grace, gaps, best/current streak,
milestones, calendar boundaries, and once-per-day celebration.

## Build validation

JDK:

`D:\jdk17\jdk-17.0.19+10` — Temurin 17.0.19

| Command | Result |
| --- | --- |
| `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS — 369/369 tests, 0 failures/errors/skips |
| `.\gradlew.bat :app:assembleDebug --console=plain` | PASS |
| `.\gradlew.bat :app:assembleRelease --console=plain` | PASS, including release lint/signing validation |
| `graphify update .` | PASS — 3,855 nodes, 7,214 edges, 277 communities |

Focused suites:

- `ProgressiveOverloadEngineTest`: 152/152 PASS
- `ConsistencyCalculatorTest`: 12/12 PASS
- `ConsistencyPresentationTest`: 4/4 PASS

APK artifacts:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
  - 99,513,831 bytes
  - SHA-256 `E0E03AD44DCF3C8B156E39BB15F20F16406802F81616797193D029372310F1F6`
- Release: `app/build/outputs/apk/release/app-release.apk`
  - 91,738,814 bytes
  - SHA-256 `215F2FBEBC6A651DC94A8FDD3BFDB66E22EB38E26F5A4080EACDDE34995B006D`

The Android Gradle Plugin emitted its existing compileSdk 35 compatibility warning; it did not fail
tests, lint, or either APK build.

## Protected-surface verification

Post-build comparison against the 1,256-file baseline manifest:

- changed protected files: **0**
- missing protected files: **0**

Explicit checks:

- `ProgressiveOverload.kt`: byte-identical
- `ProgressiveOverloadEngineTest.kt`: byte-identical; 152 tests pass
- `ProgressRing.kt`: byte-identical
- `GymTrackerApp.kt` including `GymBottomBar`: byte-identical
- Room version remains **9**
- every schema export remains byte-identical
- catalog remains **577 exercises**
- `CatalogSeed.CONTENT_VERSION` remains **16**
- catalog JSON/assets remain byte-identical
- `HomeViewModel`, `ConsistencyModels`, `ConsistencyPresentationStore`, `HomeScreen`, Theme, Daily
  Boost completion, Muscle Heat/Recovery, History V2, Import/Export, Save/Draft, repositories, and
  signing configuration remain byte-identical

## Runtime QA and screenshots

Runtime QA was attempted once with:

`adb devices -l`

ADB started its daemon but reported no attached device, then the command did not terminate normally.
This was treated as the requested hard emulator/ADB error. Screenshot recovery stopped immediately:
no AVD restart, repeated attachment loop, install, navigation, or screenshot capture was attempted.

Therefore:

- runtime visual QA: **NOT AVAILABLE — no attached emulator**
- crash-buffer inspection: **NOT AVAILABLE**
- requested screenshots: **NOT CAPTURED**
- screenshot paths: **none**

No reference image or old screenshot is presented as proof of the new runtime UI.

## Known limitation

The Compose source compiled in both debug and release variants, all unit/presentation tests passed,
and responsive seven-column layout is enforced structurally, but visual behavior on a live 360 dp
device, Light theme, font scaling, reduced motion, and the exact in-flight five-second frame sequence
still require a successfully attached emulator or physical device.

No other known implementation limitation was found in the validated scope.

---

## Follow-up — selected-day session summary restoration (2026-07-30)

This follow-up stays inside UI-HOME.2. It restores the useful session-level summary composition below
the selected calendar date without restoring Weekly Hub, Recent Sessions, expandable exercise tables,
or a second archive.

### Backup

Timestamped filesystem backup:

`D:\HOCTIN\GYM TRACKER\_backup_uihome2_followup_20260730_000733`

The backup contains the six follow-up files as they existed before editing plus
`FOLLOWUP_PROTECTED_HASHES.csv`, a 1,290-file protected-source baseline.

### Authored files changed

- `app/src/main/java/com/hoctin/gymtracker/ui/home/ConsistencyModels.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/ConsistencyScreen.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/home/ConsistencyCalculatorTest.kt`
- `README.md`
- this existing result report

No separate redesign, route, repository query, or result document was created.
The required Graphify update also refreshed generated files under `graphify-out/` (`graph.json`,
`graph.html`, and `GRAPH_REPORT.md` plus Graphify's own cache/backup metadata); these are derived
analysis artifacts, not app/runtime source.

### Before / after

Before:

- each selected-day workout was a compact mint row with only title, saved time, and a chevron
- saved aggregate evidence already existed in the observed `SessionWithSets`, but the row did not
  present it

After:

- the full selected-date heading remains unchanged
- every workout on that date renders as its own 22 dp-radius AURON session card
- the card uses a semi-transparent dark-purple enclosure, subtle purple hairline, and a 46 dp purple
  workout icon tile; Light theme falls back to readable theme surfaces
- title and saved time remain the first hierarchy level
- chips use a responsive `FlowRow`, so later chips wrap cleanly without compressing text
- the chevron is removed
- the complete card remains one accessible touch target and opens the existing Session Review callback
- the existing `day.workouts.forEach` path continues to support multiple workouts on the same date

### Aggregate and chip behavior

`HomeViewModel` now maps each already-observed `SessionWithSets` through the existing
`historySessionOf(sessionWithSets)` mapper. That mapper remains the one aggregation path and continues
to delegate set math to `SetDisplay`; no repository or DAO query was added.

Presentation-only fields added to `ConsistencyWorkoutEvidence`, all with safe defaults:

- total completed sets
- nullable rep-volume in kg
- nullable meaningful duration
- exercise count
- PR count

Visible chip order is fixed and covered by a pure test:

1. `<N> sets`, only when the count is positive
2. `<volume> kg`, green, only when positive rep-based volume exists
3. `<duration> duration`, only when a positive saved wall duration or tracked-set duration exists
4. `<N> exercises`, only when the count is positive
5. gold trophy + `<N> PR`, only when PR count is positive

The existing shared `MeasurementFormatter` formats volume and duration. The UI does not display
`0 kg`, `0 reps`, zero duration, zero-count chips, or any storage sentinel. Pure bodyweight sessions
therefore omit volume when their computed kg-volume is zero. Time/distance sessions can still show
completed sets, meaningful duration, exercise count, and PRs; distance is intentionally omitted because
the Consistency route does not receive the user's distance-unit preference and this follow-up does not
change navigation.

### Tests and APKs

JDK:

`D:\jdk17\jdk-17.0.19+10` — Temurin 17.0.19

| Command | Result |
| --- | --- |
| `.\gradlew.bat :app:testDebugUnitTest --console=plain` | PASS — 370/370 tests, 0 failures/errors/skips |
| `.\gradlew.bat :app:assembleDebug --console=plain` | PASS |
| `.\gradlew.bat :app:assembleRelease --console=plain` | PASS, including release lint/signing validation |
| `graphify update .` | Literal command unavailable in this PowerShell PATH; saved-interpreter equivalent passed — 3,878 nodes, 7,250 edges, 266 communities |

Focused verification:

- `ProgressiveOverloadEngineTest`: 152/152 PASS
- `ConsistencyCalculatorTest`: 13/13 PASS
- new selected-day presentation case verifies full chip order, non-rep omission, and all-zero omission

APK artifacts:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
  - 98,826,490 bytes
  - SHA-256 `F318F3A289F19BFCFAD1E7814607BB2633C443764880E56EB42DD4756AC3D703`
- Release: `app/build/outputs/apk/release/app-release.apk`
  - 91,743,050 bytes
  - SHA-256 `477AF99C83F7810A1ABA836FD69D900A394CB1CE64631F0D72030A7D337B281E`

The Android Gradle Plugin emitted the existing compileSdk 35 compatibility warning; it did not fail
tests, lint, or either APK build.

Graphify launcher note: the project-local interpreter recorded in
`graphify-out/.graphify_python` was used as
`C:\Python314\python.exe -m graphify update .`; this is the graphify-windows fallback for the same
incremental AST update when the shell shim is not installed in PATH.

### Runtime screenshot result

Runtime QA was attempted once after both APK builds:

`adb devices -l`

ADB returned an empty device list. Per the task's hard-error boundary, screenshot recovery stopped
immediately: no AVD restart, repeated attachment loop, install, navigation, or screenshot capture was
attempted.

- runtime selected-day visual QA: **NOT AVAILABLE — no attached emulator**
- Session Review tap runtime QA: **NOT AVAILABLE**
- requested follow-up screenshot: **NOT CAPTURED**
- screenshot path: **none**

The two supplied references were used only for composition guidance and are not presented as runtime
proof.

### Protected-surface verification

Post-build comparison against the new 1,290-file follow-up baseline:

- changed protected files: **0**
- missing protected files: **0**

Explicitly unchanged:

- Room remains version 9 and all schema exports are unchanged
- catalog remains 577 exercises with `CatalogSeed.CONTENT_VERSION = 16`
- `ProgressiveOverload.kt` and `ProgressiveOverloadEngineTest.kt`
- repositories/DAO, History V2, Session Review, save/draft, import/export, catalog data/assets,
  Daily Boost completion, navigation, and floating dock implementation

Known limitation: source compilation, JVM tests, debug APK, release APK, release lint, and protected
hash verification passed, but the final responsive card composition still needs live-device visual QA
when an emulator or physical device is attached.
