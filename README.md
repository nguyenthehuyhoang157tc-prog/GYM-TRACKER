# AURON

Local-first Android gym trainer for progressive overload and aesthetic bodybuilding.
(Formerly "Gym Tracker". The Android package, classes, and database remain `com.hoctin.gymtracker`
— only the visible product branding changed.)

## Critical Project Safety Rules

> **Read this first.** These rules exist to protect the current AURON / Progression Engine V3 app
> state, much of which lives as **uncommitted** working-tree files in the real main project.

- **README.md is the project source of truth.** When a task prompt, branch, or worktree disagrees
  with this file, trust this file and stop to reconcile.
- **Every new task must read README.md first**, before editing or building anything.
- **Every task must confirm it is working inside `D:\HOCTIN\GYM TRACKER`** (run
  `git rev-parse --show-toplevel`). This folder is the real, latest main project.
- **Never assume a secondary worktree contains the latest app state.** Anything under
  `.claude\worktrees\` (for example a `peaceful-raman-*` branch) may be branched from an **older
  baseline** and can be missing AURON UI, the theme, the engine, and the tests.
- **If the app appears to be missing** AURON UI, the dotted/OLED theme, the bottom-navigation cutout,
  the Weekly Progress Hub, Daily Boost, or Progression Engine V3 — **STOP and audit project state
  before coding.** Do not rebuild features that already exist; they are likely present in the live
  working tree, just not in the branch you are on.
- **Never restore or copy files from older backups/worktrees** unless explicitly instructed.
- **Never run destructive git commands** — `git reset --hard`, `git clean -fd`, `git checkout .`, or
  `git stash` — **without explicit user approval.** Large parts of the current app are uncommitted, so
  these commands can permanently destroy work.
- **Never touch signing secrets:** `*.jks`, `keystore.properties`, `signing.properties`. Do not read,
  move, print, or commit them.
- **Always create a timestamped backup before feature work.**
- **High-risk tasks must create a full filesystem backup** (e.g. `robocopy` to a timestamped folder),
  not only a `git diff` — untracked files are invisible to git and would otherwise be lost.
- **Progression Engine V3.5 is frozen and production-tested.** Do not refactor, simplify, rename,
  optimize, or otherwise touch progression code (`domain/ProgressiveOverload.kt`,
  `ProgressiveOverloadEngineTest.kt`) unless the task is specifically about the progression engine.

## Current Stable App State

The current completed, working app (live in `D:\HOCTIN\GYM TRACKER`):

- **AURON** user-facing branding (app name, Home and onboarding wordmarks).
- Internal package / classes / Room database remain **`com.hoctin.gymtracker`** — unchanged.
- **Local-first Android app** — Kotlin, Jetpack Compose, Material 3, Room, SharedPreferences, MVVM;
  no backend, no login.
- **AURON Performance Intelligence canvas** (ambient purple fading into OLED black) and a clean
  Light theme.
- **Custom floating bottom navigation** (display-cutout aware) with **motion polish** (sliding pills,
  micro-animations).
- **Home hero artwork** per workout day (keyword-mapped, graceful fallback).
- **ONBOARDING-V3** first-launch setup: a resumable, local-only training profile followed by an
  explicit current-vs-future review and a truthful starting-week receipt; no fake plan generation.
- **Routine-only Home Hero** with persisted selected Routine; deliberate Skip/Modify actions now live
  in Routine Detail instead of a redundant direct-switch chip row.
- **Weekly Progress ring + seven-day Streak strip** on Home, with a pushed **Streaks** detail screen
  (milestones, workout-only weekly schedule, month calendar, and selected-day details).
- **History V2 month archive** with direct navigation to Session Review.
- **Log Workout accordion** + debounced **draft persistence**.
- **Save confirmation** dialog + root **snackbar** on completed save.
- **Previous Performance** audit (latest session, `exerciseId`-scoped) + **BW** display for
  bodyweight sets.
- **Progression Engine V3.11B-MG** with **152 engine unit tests** — *frozen; see
  the safety rules above.*
- **Daily Boost** (optional ~2-minute rest-day habit) + **Active streak** (a workout OR a boost
  counts the day active).
- **PLAN-V3B Plan experience**: compact Routine previews, Routine Detail with swipe edit/delete and
  long-press reorder, plus a separate Your Workouts builder and typed Saved One-Off logging flow.
- **Profile & Settings hub** in the fourth floating-dock destination, with focused Profile, Insights,
  Training, Units & Appearance, Data, Recovery Research, Help, Privacy and About destinations.
- **Settings AI Import Helper** (clipboard prompt only; no AI calls, no schema change).
- **Local JSON export/import** from Settings.
- **Saved One-Off Workouts** have their own Room template graph and typed session provenance, remain
  separate from Routine/Home rotation, can be created/edited/reordered/started through Plan -> Your
  Workouts, and are exported/imported without duplicating catalog data.
- **WEARABLE-2A read-only Health Connect shadow context** keeps raw ingestion frozen under
  `WEARABLE_2A_SHADOW_CONTEXT_V1` and versions the split-sleep repair as snapshot contract V1.1 /
  selection policy V2. It has zero effect on readiness/Heat/PO and remains **awaiting physical repair
  verification plus the first natural snapshot**.

### PS-1 — Profile & Settings Hub redesign (August 11, 2026)

The fourth floating-dock destination is now labelled **Profile** and opens a complete Profile &
Settings information architecture. The floating dock implementation, animation and route identity
remain unchanged; only the destination label/icon and its screen graph changed.

- The new landing screen uses the current AURON purple-to-OLED canvas, large tab-labelled section
  enclosures, restrained right/lower card depth, white hierarchy and accessible 48 dp rows.
- **Plan & progress** opens the existing Streaks/Weekly Progress experience, a new History-derived
  Workout Insights screen, and the existing Plan tab. Insights use only real History V2 sessions and
  omit invented kg/reps for time/distance work.
- **Preferences** separates the existing coaching goal/training-weekday controls from theme/distance
  units. Goal and weekday ownership are unchanged.
- **Your profile** reads the local ONBOARDING-V3 profile without rewriting it. Profile editing is
  deliberately marked `Updating` until a real save contract exists. Workout History still opens the
  canonical History V2 archive.
- **Data & research** preserves the existing plan/history JSON export/import, clipboard-only AI helper,
  Recovery Research opt-in, SAF export and destructive local delete behavior.
- **Help & AURON** provides truthful product/FAQ/privacy context. Notifications, contact/community,
  formal hosted legal documents and profile editing are explicit polished unavailable/updating states;
  the app does not show fake switches, accounts or dead external links.

PS-1 is presentation/navigation only: no Room schema, repository behavior, onboarding persistence,
import/export contract, History grouping, progression logic, Muscle Recovery scoring, catalog data or
floating-dock implementation changed. Detailed evidence is in
`docs/ps1_profile_settings_hub_redesign_result.md`.

### PLAN-V3A - Saved Workout persistence contract (August 12, 2026)

PLAN-V3A implements only the data/domain foundation for future **Your Workouts** UI. It does not add
the PLAN-V3 cards, builder, swipe gestures, Home Hero changes or Generate Workout functionality.

- **Routine and Saved One-Off are separate domains.** Routine continues to own Plan order, selected
  Routine, Home Hero, normal Log Workout auto-advance and workout-only Weekly Progress. A Saved
  One-Off template lives in `saved_workouts` + `saved_workout_exercises`; it never uses
  `WorkoutDayEntity`, never enters Routine rotation and never changes selected Routine.
- **Room v10 -> v11 is additive.** The migration creates the two Saved Workout tables and adds
  `sourceType` (`ROUTINE` / `SAVED_ONE_OFF`) plus nullable `savedWorkoutId` provenance to
  `workout_sessions`. Existing sessions default to `ROUTINE`. Session provenance deliberately has no
  FK back to a template, so deleting a template cascades only its template exercises and never History.
  Exported schema: `app/schemas/com.hoctin.gymtracker.data.AppDatabase/11.json`.
- **Snapshots stay truthful.** Saved exercises retain optional catalog slug, canonical key, display
  name and prescription order. Non-rep `0/0` target sentinels are preserved for tracking-aware
  duration/distance exercises; catalog rows themselves are never copied or mutated.
- **One-Off sessions use normal History/evidence storage.** The typed repository save path writes a
  normal session and logged-set snapshots with `dayId = null`, so History V2, canonical exercise
  evidence, Active Streak and Muscle Recovery can consume real work. It does not call the Routine
  selected-day or auto-advance path. Weekly Progress filters to `ROUTINE` sessions only.
- **Recovery Research remains frozen and Routine-shaped.** Its current production API still requires
  a real `WorkoutDayEntity`; PLAN-V3A does not fabricate one or rewrite Research heuristics. Saved
  One-Off sessions therefore do not capture a Research observation until that owner defines a typed
  source/abstention contract. Wearable context and all existing Research files remain unchanged.
- **Backup JSON is now schemaVersion 6.** New backups round-trip Saved Workout definitions, order and
  prescriptions. Old backups still import; because they cannot express Saved Workouts, importing one
  preserves any local Saved Workout templates instead of silently deleting them.

Validation: **423/423 JVM tests** and **9/9 focused emulator tests** passed (v8->v9, v9->v10 and
v10->v11 migrations; Saved Workout CRUD/reorder/delete/export; Recovery Research; Wearable Shadow).
Debug and release APKs built. A release-signed v10 WEARABLE-2A APK was launched and upgraded in place
with `adb install -r` to the final v11 release: `firstInstallTime` was preserved, v11 launched, and
the crash/migration-fatal buffers were empty. Detailed evidence and the PLAN-V3B handoff are in
`docs/plan_v3a_saved_workout_persistence_contract_result.md`.

### PLAN-V3B - Plan, Routine & Your Workouts UI (August 13, 2026)

PLAN-V3B connects the v11 persistence contract to a complete Plan product flow while preserving the
Routine / Saved One-Off boundary:

- **Plan** now leads with Your Workouts and a truthful Generate Workout `Unavailable / Coming soon`
  destination, followed by real Routine cards showing total sets and up to four thumbnail/name previews.
- **Routine Detail** keeps normal Start Workout behavior, opens existing Exercise Detail/Progression,
  supports red swipe-delete, white swipe-edit and long-press vertical reorder, and adds truthful
  `Skip this workout` / `Modify this workout` actions using persisted Routine order.
- **Your Workouts** lists the isolated v11 templates with created date and completion count. Expanded
  templates expose Delete Template, Edit Workout and Start Session without entering Routine rotation.
- **Build a Workout** reuses the real FED catalog and thumbnail system with ALL/SELECTED tabs,
  search, muscle filters, multi-select, red remove controls, long-press reorder and a final named review.
- **Saved One-Off Log** reuses the existing Log Workout presentation through a typed launch source and
  saves through `saveSavedOneOffWorkoutSession(...)`: `dayId = null`, normal History/canonical evidence,
  no selected-Routine mutation, no auto-advance and no Weekly Progress contribution.
- Home Hero remains Routine-only. Saved One-Off sessions still do not create Recovery Research
  observations because that protected subsystem remains intentionally Routine-shaped.

Room remains **v11**; no schema, catalog, import/export, Progression Engine, History grouping,
wearable or Recovery Research implementation changed. Final JVM validation passed **426/426 tests**;
debug and release APKs built. Device/runtime screenshots remain pending because no connected device or
configured AVD was available in the validation environment. Detailed evidence:
`docs/plan_v3b_full_plan_routine_saved_workouts_result.md`.

### PLAN-V3B.1 - focused Plan UI polish (August 15, 2026)

PLAN-V3B.1 keeps the v11 architecture intact and tightens only the connected Plan experience:

- the shared AURON canvas has a more saturated upper plum atmosphere while retaining the OLED-black
  lower canvas;
- Build a Workout keeps ALL/SELECTED, search and muscle filters pinned while its catalog rows scroll;
  a row still toggles selection, while its thumbnail opens the single shared Exercise Detail popup;
- that shared popup now uses a dimmed backdrop, true-dark compact surface, a 70% visible-height cap,
  internal scrolling and a restrained upward settle; the final Builder review uses the same modal
  family below half-screen;
- PLAN-V3 surfaces use darker charcoal-black cards, Your Workouts has balanced Delete/Edit actions,
  and Routine Detail adds a direct `Start single workout` route to Your Workouts;
- Log Workout received color alignment only. Save/draft, progression, set input and Saved One-Off
  semantics remain unchanged.

Room remains **v11**. No onboarding, History grouping/layout, Weekly Progress, Muscle Recovery,
catalog, import/export, Progression Engine, wearable or Recovery Research behavior changed. Targeted
unit tests and the debug APK build passed; Pixel 8 runtime evidence is documented in
`docs/plan_v3b1_ui_polish_result.md`.

### LOG-V4A - Log Workout, smart rest timer, and advanced-set UI foundation (August 16, 2026)

LOG-V4A keeps Room at **v11** and redesigns only the active logging presentation and transient
interaction state:

- AURON/workout identity and elapsed Duration remain pinned while exercise cards scroll; each
  exercise thumbnail/name opens the shared Exercise Detail popup and the right chevron only controls
  expansion.
- Progression Target now opens a full-width compact modal with the existing Engine recommendation
  wording, current note field and canonical exercise history instead of visually navigating away.
- Explicit set completion can open an idempotent smart Rest Timer. Suggestions use the catalog's
  existing `ProgressionType`, completed-set RIR, a clear previous-set rep drop and a capped extra-set
  surcharge; the helper is UI-domain logic and does not modify Progressive Overload.
- The last programmed set offers a restrained `Next exercise` state before rest. Finishing/skipping
  that rest collapses the current exercise, expands the next one and requests a comfortable scroll
  position.
- The set overflow sheet introduces Straight/Warmup/Dropset presentation, weight-distribution UI,
  W/D badges and one continuation child per base set. These advanced labels and relationships are
  intentionally transient: they do not yet change progression, volume, History, recovery or saved
  set semantics. A valid continuation's measurements are saved as an ordinary logged set, while its
  advanced identity is not persisted or restored from a draft.
- The Saved Workout review name field now follows the IME with `imePadding` and focused-field
  bring-into-view behavior. Log and Settings opt into the established charcoal surface tokens without
  recoloring unrelated screens.

Room/schema, import/export, PLAN-V3 Saved Workout provenance, History V2 grouping, onboarding,
wearable/Recovery Research and Progressive Overload remain unchanged. Focused Log/timer tests and the
debug APK build passed. New runtime screenshots remain pending because the Pixel 8 AVD contains a
differently signed installed package; the app was not uninstalled or cleared to bypass that safety
boundary. Detailed evidence: `docs/log_v4a_log_workout_redesign_result.md`.

**Follow-up (September 3, 2026) — bottom-sheet actions clipped by the system gesture bar.** The
pinned `Delete Set` / `Close` / `Cancel` action row rendered partially behind the system navigation
bar. Root cause was *not* height: `AuronCompactModalSheet` opened its `Dialog` with
`decorFitsSystemWindows = true`, and **a dialog whose decor fits the system windows consumes the
window insets, so every inset read inside it — including `navigationBarsPadding()` — resolves to
`0`.** The footer ended up 20dp above the true screen bottom, inside the gesture bar. Raising
`maxHeightFraction` or button height cannot fix this; two earlier attempts that did so had no
effect. Fixed by switching to `decorFitsSystemWindows = false`, adding a floored bottom lift
(`maxOf(navigationBarBottom, 34.dp)`) applied *before* the `clip`/`background` chain, and insetting
the scrim out of the system-bar strips so light-theme status/nav icons stay legible. Clearance went
from 20dp to ~60dp (gesture nav) / ~74dp (3-button nav), across all 7 bottom-sheet call sites.
Full root-cause write-up in
[`docs/log_v4a_log_workout_redesign_result.md`](docs/log_v4a_log_workout_redesign_result.md).

### LOG-V4B — Daily Win cleanup + Liquid Glass research + Settings plan (September 4, 2026)

Two-scope task. **Task A is implemented** (Daily Win cleanup + glass card); **Task B is
research-only** (Settings follow-up plan). Task A keeps Kimi K3's lime design language
(`DailyWinLime = 0xFFB6E24A`, two-action card, confetti) and removes three unwanted
popups: `DailyWinInfoSheet` ("How it works" — explanations rejected by DESIGN.md §1
restraint), `DailyBoostChooseSheet` (intermediate exercise picker — picking moves
behind Random), and the local `DailyBoostTimerDialog` (replaced by the shared
`RestTimerSheet` extracted from `LogWorkoutScreen.kt`). The `Icons.Outlined.Shuffle`
on Random is replaced by an inline `BoostRandomIcon`: the same crossed-S shuffle
semantic, but headed only at the outgoing ends, so four arrowheads inside 20dp become
two and the glyph stops collapsing into a smudge.

**Glass is refraction, not blur.** The `DailyBoostCard` body is wrapped in
`Modifier.auronLiquidGlass(Lens(...))`, an AGSL `RuntimeShader` that bends the card's
**own painted pixels** (medallion, type, buttons, status chip) — it never samples the Home
canvas behind it, so hit-testing and the layout tree stay unwarped. The displacement field
is a rounded-rect SDF: zero at the centre, strongest at the rim, sampled *inward* so the
rim magnifies instead of reading outside the layer. Aave's two finishing passes are in the
same shader — a faint chromatic fringe (`uChroma`) and a specular rim plus top-edge sweep
(`uSpecular`). Blur is explicitly **not** the look: `Modifier.blur` / `createBlurEffect` is
a named non-goal, and chaining one onto displacement would destroy the effect. Programmable
displacement needs **API 33** (API 31 exposes only blur/offset/color-filter/chain), so the
modifier is a no-op below 33 where the card keeps its translucent gradient. That fallback is
crash-safety only and is not called glass.

Task B (research only) catalogues the 13 `ProfileDestination` rows against the OLED /
charcoal language. Settings charts and photos may reuse `auronLiquidGlass` later; they are
not a prerequisite. Full plan
[`docs/log_v4b_daily_win_redesign_plan.md`](docs/log_v4b_daily_win_redesign_plan.md) and
result [`docs/log_v4b_daily_win_redesign_result.md`](docs/log_v4b_daily_win_redesign_result.md).

### SET-TYPE-P0 — Set Type architecture and evidence plan (September 9, 2026)

Research/design only — **no production Kotlin, test, Room schema, migration, import/export or Gradle
change was made.** AURON can tag a set Warm-up or Dropset and can create one continuation through
`Continue this set`, but none of that survives the save (`LoggedSetEntity` and `SetInput` carry no set
type), so Muscle Heat, Muscle Recovery and the Progression Engine currently read every saved row as an
ordinary working set. The plan recommends a single additive ledger extension (`setType`, `parentSetId`
on `logged_sets`, DB v11 → v12), progression filtering **at the data-supply boundary so the frozen
`ProgressiveOverload.kt` is never edited**, and explicitly labels separate physiological coefficients
for drop sets / rest-pause as a `MODEL_LIMITATION` because no retrieved source quantifies local
recovery cost per set type. Companion current-state audit:
[`docs/advset_accuracy_context_audit.md`](docs/advset_accuracy_context_audit.md). Full plan:
[`docs/set_type_architecture_and_evidence_plan.md`](docs/set_type_architecture_and_evidence_plan.md).

### Build & signing (current, verified)

- **Known-good JDK:** `D:\jdk17\jdk-17.0.19+10`. The bundled Android Studio JBR is corrupted
  (`jvm.cfg` missing), so set `JAVA_HOME` to this JDK 17 path before building.
- **Debug build:** `.\gradlew.bat :app:assembleDebug` → `app\build\outputs\apk\debug\app-debug.apk`.
- **Release build:** `.\gradlew.bat :app:assembleRelease`.
- **Release signing is conditional** — `app/build.gradle.kts` loads `keystore.properties` from the
  repo root and only applies the release `signingConfig` when that file exists:
  - **`keystore.properties` present (this machine):** release is **signed** →
    `app\build\outputs\apk\release\app-release.apk`.
  - **`keystore.properties` absent (fresh clone / CI):** release is **unsigned** →
    `app\build\outputs\apk\release\app-release-unsigned.apk`.
  - **Always inspect `build.gradle.kts` and the actual output filename before claiming a release is
    signed or unsigned. Report the real artifact path honestly.**
- **Engine tests:** `.\gradlew.bat :app:testDebugUnitTest` (152 `ProgressiveOverloadEngineTest` cases).

## Verified Status

Verified on Windows on June 3-4, 2026:

- Android Studio: installed at `C:\Program Files\Android\Android Studio`.
- JDK: Android Studio JBR 21 at `C:\Program Files\Android\Android Studio\jbr`.
- Android SDK: installed at `C:\Users\hoang\AppData\Local\Android\Sdk`.
- Emulator: Pixel 8 AVD running API 37.
- Debug build: successful.
- Debug install: successful on `emulator-5554`.
- Runtime QA: Home, Plan, History, Settings, Quick Start, add exercise, add set, save workout, auto-advance to the next workout day, previous performance, PR display, export JSON, theme mode, and persistence after force-stop/relaunch verified.
- Crash buffer: no app crash entries after QA.

Debug APK:

```text
D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk
```

## AURON Branding & Hero Artwork (patch notes)

Visual/branding patch — no business logic, Room schema, save/draft/auto-advance, or progressive-overload
logic was changed. Verified on the Pixel 8 (API 37) emulator in dark mode.

June 9, 2026 UI polish: added a shared subtle 1.dp, theme-aware border to major cards,
containers, expanded set tables, compact log inputs, and the floating bottom navigation dock so Light
and Dark mode retain clearer separation while preserving existing spacing, shadows, and workflows.
Debug QA uses `app/build/outputs/apk/debug/app-debug.apk`.

June 11, 2026 Settings AI Import Helper: added a secondary card near Import that copies a
constant-backed prompt for converting screenshots or workout notes into AURON import JSON. It uses
the local clipboard and snackbar only; there are no AI API calls, no internet requirement, and no
automatic import or JSON schema change.

June 11, 2026 Log Workout safeguard: tapping Save workout now opens a confirmation dialog before the
existing save flow runs. A root snackbar confirms the completed save after navigation to History; save
logic, auto-advance, draft cleanup, Room schema, and progression logic are unchanged.

June 11, 2026 AURON rebrand + dark texture theme: visible app branding now reads AURON, while
internal package/class/database names remain unchanged. Dark mode top-level screen roots use the
static `auron_dark_texture.png` resource behind cards and surfaces; Light mode keeps the existing
clean background, and Log Workout remains distraction-free.

What changed:

- **Name:** App label, Home wordmark, and onboarding wordmark now read **AURON** with the **TRAINER** subtitle.
  - `res/values/strings.xml` → `app_name` = `AURON`.
  - `ui/home/HomeScreen.kt` → `BrandHeader` text = `AURON`.
  - `ui/onboarding/OnboardingScreen.kt` → Welcome eyebrow text = `AURON`.
  - Internal package/classes/tables are unchanged (`com.hoctin.gymtracker`).
- **Launcher icon:** Adaptive icon uses the existing text-free app mark `res/drawable-nodpi/gymate_icon.png`
  (1254×1254, white mascot + dumbbells on black) as the adaptive `foreground`, over a pure-black
  `background` (`res/values/colors.xml` → `launcher_background = #000000`). The mark is centered well
  within the adaptive safe zone, so it survives circle/squircle masks and reads on light or dark
  launchers. Adaptive XML: `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
- **Home hero artwork:** The hero card shows workout-specific art behind the text on the right side, with
  a left-to-right scrim so the title, badges, and Quick Start stay readable. Mapping is keyword-based and
  case-insensitive (`heroResForDay` in `HomeScreen.kt`):
  - Push → `hero_push`, Pull → `hero_pull`, Legs → `hero_legs`, Full Body → `hero_full_body`.
  - Any other / unknown day name falls back to `hero_default` (a lightweight purple dumbbell **vector**,
    kept at `res/drawable/hero_default.xml`). The mapping always returns a valid drawable, so a missing
    image can never crash the card.
- **Final hero assets:** The four day heroes ship as bitmaps in `res/drawable-nodpi/`
  (`hero_push.jpg`, `hero_pull.jpg`, `hero_legs.jpg`, `hero_full_body.jpg`, ~1535×1024 / 1672×941).
  Only the unknown-day fallback remains a vector.
- **June 5, 2026 hero refresh:** Corrected day hero artwork was installed as the four
  `hero_*.jpg` files. The previous old JPEGs were moved to `_asset_backup_hero_old/`, and the
  provided PNG sources are preserved outside Android resources in `_asset_sources_hero_new/`.
  The Home hero image keeps `ContentScale.Crop` with a slight overscale inside the rounded clip so
  the supplied artwork's white outer canvas cannot appear as a corner/frame.
- **Performance:** One hero is decoded per selected day via `painterResource` (cached by resource id, no
  runtime image processing). Home scroll was re-verified smooth with the bitmaps in place.

### Replacing the hero illustrations later (drop-in)

To swap artwork, replace files by name — no code change required (`R.drawable.hero_*` resolves to whichever
single file exists, bitmap or vector):

1. For a day hero, drop a same-named bitmap into `app/src/main/res/drawable-nodpi/`
   (e.g. `hero_push.jpg` / `.png` / `.webp`). Names: `hero_push`, `hero_pull`, `hero_legs`, `hero_full_body`.
2. Keep exactly one file per resource name (don't have both `hero_push.xml` and `hero_push.jpg`).
3. Recommended: landscape ~1.5–1.7:1 (the card crops to fit), subject on the right with a darker/empty left
   side so it integrates with the purple gradient + scrim.

## Windows Setup

For a fresh Windows machine, install the required tools:

```powershell
winget install --id Google.AndroidStudio -e
winget install --id EclipseAdoptium.Temurin.17.JDK -e
```

Open Android Studio once, then install these SDK components from **Tools > SDK Manager**:

- Android SDK Platform 35
- Android SDK Build-Tools
- Android SDK Platform-Tools
- Android Emulator
- Android SDK Command-line Tools

Create an emulator from **Tools > Device Manager**, for example Pixel 8 with API 35 or newer.

Set environment variables in a new PowerShell session:

```powershell
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
  Sort-Object Name -Descending |
  Select-Object -First 1 -ExpandProperty FullName
setx JAVA_HOME "$jdk"
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx ANDROID_SDK_ROOT "$env:LOCALAPPDATA\Android\Sdk"
setx PATH "$env:PATH;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:LOCALAPPDATA\Android\Sdk\emulator;$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin"
```

Verify setup:

```powershell
java -version
adb version
emulator -list-avds
```

This project also builds with Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

## Open In Android Studio

1. Open Android Studio.
2. Choose **Open**.
3. Select this folder: `D:\HOCTIN\GYM TRACKER`.
4. Let Gradle sync finish.
5. If Android Studio asks to install missing SDK packages, accept.

## Run On A Phone With USB

1. Enable Developer Options on the phone.
2. Enable USB debugging.
3. Connect the phone by USB.
4. Accept the RSA debugging prompt on the phone.
5. Verify the device:

```powershell
adb devices
```

6. Run from Android Studio, or use:

```powershell
.\gradlew.bat :app:installDebug
adb shell monkey -p com.hoctin.gymtracker 1
```

## Build Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

Output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Verified absolute output path:

```text
D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk
```

## Install Debug APK

With SDK platform-tools on PATH:

```powershell
adb devices
adb install -r "D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk"
adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

Using the verified SDK path directly:

```powershell
$adb = "C:\Users\hoang\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r "D:\HOCTIN\GYM TRACKER\app\build\outputs\apk\debug\app-debug.apk"
& $adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

## Emulator QA Checklist

Verified emulator run:

```powershell
$emulator = "C:\Users\hoang\AppData\Local\Android\Sdk\emulator\emulator.exe"
$adb = "C:\Users\hoang\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $emulator -list-avds
& $adb devices
.\gradlew.bat :app:installDebug
& $adb shell am start -n com.hoctin.gymtracker/.MainActivity
```

Verified flows:

1. Home opens with a suggested workout and the seeded Push/Pull/Legs/Full Body split exists.
2. Quick start opens Log Workout for the currently selected day.
3. Plan allows creating, editing, and deleting workout days.
4. Plan allows creating, editing, and deleting exercises.
5. Log Workout saves sets with kg, reps, RIR, and notes.
6. Log Workout shows previous performance after saving and starting the same workout again.
7. History shows saved sessions grouped by date and exercise.
8. PR markers appear after hitting a new max weight or reps.
9. Exercise Detail shows exercise history and the overload suggestion.
10. Settings generates export JSON.
11. Settings imports previously exported JSON and restores plan/history data.
12. Close and relaunch the app, then confirm history and plan edits persist.
13. Settings Light mode remains selected after force-stop/relaunch.

### Workout day selector QA (Home)

The Home screen lets you manually choose the current workout day instead of relying only on the auto suggestion. Verify:

1. Home shows a **Workout day** section with one chip per plan day (Push, Pull, Legs, Full Body).
2. Tapping a chip (for example **Legs**) selects it; the "Today" header and the workout exercise list update to that day, and the subtitle reads "Manually selected".
3. **Skip / Next workout day** advances to the next day in the plan and cycles back to the first after the last day.
4. **Quick start** opens Log Workout for the currently selected day, and the saved session in History shows that day's name (for example a Legs session is labelled "Legs").
5. Selecting a day, then force-stopping and relaunching the app, keeps the same day selected (persisted in preferences).
6. With no manual selection yet (fresh install), Home falls back to the day-of-week suggestion and the subtitle reads "Suggested for today".

Capture screenshots:

```powershell
adb exec-out screencap -p > home.png
```

Use UI tree inspection for tap targets when debugging:

```powershell
adb exec-out uiautomator dump /dev/tty > ui.xml
```

### Auto-advance after saving QA (Home)

When a workout is saved, Home advances the selected workout day to the next day in the current plan order (`sortOrder`, then id). The saved History session keeps the original completed day name.

Verified on Pixel 8 API 37 on June 4, 2026:

1. Selected **Pull** on Home, opened **Quick Start**, saved a valid workout, and confirmed History recorded **Pull**.
2. Returned Home and confirmed the hero showed **NEXT WORKOUT** / **Legs** and the workout section showed **Next workout**.
3. Pressed **Quick Start** again and confirmed Log Workout opened **Legs**.
4. Saved a valid **Legs** workout and confirmed History recorded **Legs**.
5. Returned Home and confirmed the selected workout advanced to **Full Body**.
6. Force-stopped and relaunched the app, then confirmed **Full Body** was still selected and shown as **NEXT WORKOUT**.

### ONBOARDING-V3.1 — raised controls and simplified schedule (August 9, 2026)

Fresh installs now enter one resumable route containing 18 active steps:

`Welcome → Name → Gender → Age → Goal → Experience → Days per Week → Specific Days → Duration → Environment → Equipment → Priority Muscles → Physique → Measurements → Target Weight → Any Injuries? → Review → Receipt`

V3.1 is a focused presentation and input correction. It restores the pre-Figma raised AURON controls: an
OLED-black face, restrained top/left hairline, dark bottom/right depth layer, slightly squarer corners and a
mechanical press that moves the face down while collapsing its depth. White and black provide the hierarchy;
purple is limited to the ambient canvas, selected border/check and progress identity. Step changes now use a
restrained 430ms fade instead of combining horizontal page motion with upward row motion. Option rows keep a short
upward entrance stagger, but remain measured for their entire lifetime; reduced motion removes translation, spring
and stagger behavior.

The active Gender screen now offers only **Male** and **Female**. A compact info action explains that the value is
stored locally as profile context and is not used for sex-based programming. The older non-binary and
prefer-not-to-say storage IDs remain readable so V3 drafts stay backward compatible; they are not active choices.

One reusable inertial ruler now owns **height**, **current weight**, **target weight** and **Days per Week**. It uses
canonical metric values, centered snapping, fling/deceleration, evenly spaced ticks, stable metric/imperial
formatting, throttled tick haptics and accessibility increase/decrease actions. Weight values resolve to 0.1 kg.
Days per Week accepts 1–7; the following Specific Days screen requires exactly that number of ISO weekdays.

Only current production owners are reused:

- **Coaching goal** still maps through `WorkoutGoalSelection` / `WorkoutGoalPreferenceStore`. It changes future
  coaching, not the starting split, planned targets, History or saved sets.
- **Specific training weekdays** still use the existing `training_weekdays` preference. Their count becomes the
  workout-only Weekly Progress target and drives planned/missed/rest interpretation. They do not assign
  Push/Pull/etc. to dates, and Daily Boost remains excluded from Weekly Progress.

Compound-lift familiarity, Recovery, Cardio, Pain, Evidence and Profile Ready are no longer active questions.
Recovery/Cardio/Pain are replaced by one optional multiline **Any injuries?** field with a small medical boundary.
Old nullable JSON fields remain decodable; V3.1 does not rewrite or delete persisted onboarding keys. Duration,
environment/equipment, priorities, physique and measurements remain explicit profile context and do not silently
alter the plan, catalog, progression or recovery systems.

Target BMI uses `kg / meters²`. For the age bucket that can contain users under 18, adult thresholds are not
applied. Adult-screened profiles show a height-derived reference range calculated from BMI 18.5–24.9; for example,
166cm produces **51–68kg** after safe inward rounding. A raw target BMI below 18.5 shows a translucent red warning,
bold height/BMI evidence and blocks Continue. A target at or above BMI 25 shows a non-blocking advisory instead of
claiming a diagnosis. The rounded BMI is display-only.

Persistence remains SharedPreferences-only, versioned, resumable and atomically committed through the existing
owner. Legacy completed users still bypass onboarding; tracking start is preserved; finishing setup creates no
workout, session, draft or History row. The receipt now uses a deterministic ten-second presentation timeline:
Calculating 1–45%, Building setup 45–60%, then an irregular-cadence Personalizing phase to 100%, followed by the
truthful **Your training profile is ready** state. It does not claim AI generation or create a plan/session.

V3.1 validation now covers **400 JVM tests** with zero failures/errors/skips, and debug/release APK assembly
succeeded. Two zero-width regressions are fixed. Gender uses an explicit width-producing parent. Equipment and
later multi-choice pages no longer disappear on compact/high-font-scale phones because both grid branches and each
tile now own width, while the stagger uses alpha/offset without removing content from measurement. Common phone
widths retain the intended two-column equipment grid; genuinely narrow or large-text layouts use a full-width
single column. No connected test, APK install or emulator mutation was performed because `adb devices` returned no
device and no disposable state was available; prior screenshots remain historical evidence rather than acceptance
of this follow-up. Details, artifact hashes and remaining runtime QA are in
`docs/onboarding_v3_complete_profile_motion_result.md`.

### Weekly Progress ring + hub (Tasks 2.2a / 2.2b)

Home shows a **Weekly Progress** card with an animated ring (Task 2.2a). The weekly **target is derived from the selected training weekdays** (`trainingWeekdays.size`, falling back to the plan size) — never hardcoded to 4, so it adapts to 5/6/custom plans. **Completed = distinct calendar days (Monday–today) with at least one saved session**, so multiple saves on the same day count once. The week resets at the start of a new ISO calendar week (Monday). All of this is computed in `HomeViewModel` (`weeklyCompleted`, `weeklyTarget`, `weeklyProgress`).

Tapping the Weekly Progress card opens the **Weekly Progress Hub** bottom sheet (Task 2.2b) — it no longer navigates to the History tab. The hub is precomputed in `HomeViewModel.weeklyHub` (a `WeeklyHubUiState` with a 6-week calendar and collapsed session summaries) so the sheet opens instantly with no date/session grouping inside composables and no artificial loading delay.

Calendar cell states (purely derived from `workout_sessions` — **no new Room table**):

- **Completed** (green ✓): any session exists on that date.
- **Missed** (red ✗): a *past* selected training weekday with no session, **only on or after the tracking start date** (see Task 2.2c below).
- **Planned** (muted purple, shows day number): today or a *future* selected training weekday with no session yet.
- **Rest** (muted gray, shows day number): a non-training weekday, **or any day before the tracking start date**.
- **Today**: a purple outline drawn on top of whatever the day's state is.

Verified on Pixel 8 API 37 on June 6, 2026:

1. Tapping the Weekly Progress card opens the bottom sheet with a smooth slide-up animation (no Crossfade, no infinite animations).
2. Header shows "Weekly Progress", the calendar month range (e.g. "Apr – Jun 2026"), and "1 of 4 workouts this week".
3. The 6-week Monday-start calendar renders; selected training weekdays look different from rest days; completed dates are green checks; past selected training days without a session are red ✗; today has a purple outline.
4. Two sessions saved on the same day count the date as completed **once** (calendar shows one green day; ring stays 1/4).
5. Recent sessions are collapsed by default (header + sets/volume/exercises/PR badges + chevron).
6. Expanding a session reveals a table-like **SET | VOLUME | REPS | RIR** layout with PR trophies; expanding one session does not expand the others; collapsing works.
7. The sheet dismisses via the close (✕) button and via swipe-down.
8. Regression pass: Home still scrolls without crashing, Quick Start still opens Log Workout (accordion + sticky Save/Discard bar intact), History tab is unchanged, and Settings theme / training days / export / import all still work. Clean `adb logcat -b crash`.

### Tracking start date / missed-day fix (Task 2.2c)

The calendar must not blame the user for scheduled days that happened **before they started using the app**. A **tracking start date** is recorded when onboarding completes:

- `UserPreferencesRepository` stores `tracking_start_epoch_day` (the epoch day onboarding finished) and exposes it as `trackingStartEpochDay: StateFlow<Long?>`. **No Room schema change** — it is a SharedPreferences value, set inside `completeOnboarding(...)` and preserved if onboarding is re-run.
- `HomeViewModel.buildWeeklyHub` resolves an effective start = `trackingStartEpochDay` **else** the earliest saved session date **else** today (the fallback covers old installs that completed onboarding before this field existed).
- A day is only ever shown **Missed** when `date >= trackingStart`. Any earlier day with no session is rendered **Rest** (muted), never red. Completed sessions before the start date still show green; today still gets its outline; future training days are still Planned (never red). The Weekly Progress **ring** logic is unchanged (it only counts completed days this week, so it does not depend on the missed-day model).

Verified on Pixel 8 API 37 on June 6, 2026 (device date Saturday, June 6):

1. Cleared app data and completed onboarding the same day with training days Mon/Wed/Fri/Sat. Prefs show `tracking_start_epoch_day=20610` (June 6, 2026).
2. Opened the Weekly Progress Hub: **Monday/Wednesday/Friday of the current week are muted gray, not red** (they precede the tracking start), today (Saturday) has the purple outline, and there are no red missed cells anywhere.
3. Saved a workout today → today's cell turns **green/completed**; Mon/Wed/Fri stay muted; future/rest days stay muted (never red).
4. Multiple sessions on the same day still count the date once. The ring still reads "1 of 4 this week".
5. Fallback path (no stored start date / old installs): missed days are measured from the earliest saved session date, then today — so nothing before the user's first recorded activity is marked missed.
6. Regression + clean `adb logcat -b crash`: onboarding, auto-advance, draft persistence, History, Quick Start, Log Workout accordion, Settings, export/import all still work.

### History weekly collapse (Task 2.3)

History is grouped into a scalable, collapsible hierarchy so it stays readable for 5, 20 or more weeks:

```
Week group  ->  Workout session/day  ->  Exercise  ->  Set rows
```

- **Grouping/aggregation happens in `HistoryViewModel`** (`HistoryUiState` → `HistoryWeek` → `HistorySession` → `HistoryExercise` → `HistorySet`). Composables only render prepared, lightweight state — no date/session grouping inside Compose.
- **Weeks** are Monday-start calendar weeks. Only weeks that contain at least one saved session produce a group, **newest week first**. Labels: **This Week**, **Last Week**, otherwise **Week of MMM d**, each with a date range (e.g. `Jun 1 – Jun 7`).
- Week aggregates: `workoutCount` (sessions), `totalSets`, `totalVolume` (Σ weight×reps, shown with thousands separators), `prCount` (Σ `isPrWeight || isPrReps`).
- Session summary: day name (`dayNameSnapshot`, so old sessions stay readable), date/time, total sets, total kg volume, PR count, exercise count (distinct `exerciseNameSnapshot`).
- **Collapse defaults:** the **current week is expanded**, all other weeks collapsed; **every session is collapsed**. Expanding a week does **not** expand its sessions; expanding a session reveals its exercises and a **SET | VOLUME | REPS | RIR** table with PR trophies. Multiple sessions on the same day are listed separately but counted once per date by the calendar.
- **Performance:** a flattened `LazyColumn` with stable namespaced keys — `week-${weekStartEpochDay}` and `session-${session.id}` (exercise rows use `exercise-${session.id}-${name}` identity inside the expanded session). Session items only exist while their week is expanded; set rows only compose while the session is expanded. No Room schema change; no root-level Crossfade.
- **Home cleanup:** the old **Recent Sessions** list was removed from the bottom of Home (the Weekly Progress Hub and History now cover session review). Home keeps hero, workout selector, weekly progress/stats, today's exercises, and Quick Start.

Verified on Pixel 8 API 37 on June 6, 2026 (with sessions seeded across 3 weeks):

1. History groups sessions by week, newest first: **This Week** (expanded) → **Last Week** → **Week of May 18** (collapsed).
2. Week aggregates are correct (e.g. Last Week "2 workouts · 10 sets · 5,470 kg · 2 PR").
3. Week expand/collapse works; expanding a week shows its sessions **collapsed**.
4. Session expand reveals the aligned **SET | VOLUME | REPS | RIR** table with PR trophies on PR sets and the exercise title.
5. Home no longer shows Recent Sessions; the Weekly Progress Hub still works.
6. Saving a new workout adds it to **This Week** (2 workouts) and Home auto-advances (Legs → Full Body).
7. Restart persistence holds; scrolling is smooth; `adb logcat -b crash` is clean. Quick Start, Log Workout accordion + sticky Save bar, Settings/theme/export/import all still work.

### "Previous" performance source — audit + BW fix (Task 2.4a)

Audit of where the Log Workout **Previous** chips come from, and a display-only fix. **No data-source bug was found**; the only change is a safe formatting tweak.

**Data path (read-only audit):**

```
LogWorkoutViewModel.init
  -> GymRepository.recentSetsForExercise(exerciseId)
     -> GymDao.getRecentSetsForExercise(exerciseId)   // SQL below
  -> historyByExercise[exerciseId]
  -> latestSessionSets(history)   // narrows to ONE session
  -> ExerciseLogState.previousSets
  -> PreviousPerformance(...) chips
```

- **Query** (`GymDao.getRecentSetsForExercise`): `... WHERE logged_sets.exerciseId = :exerciseId ORDER BY workout_sessions.startedAtMillis DESC, logged_sets.setNumber ASC`.
- **Latest-session selection:** `latestSessionSets()` takes the `sessionId` of the first (newest) row and keeps **only that session's** sets — so Previous is always the **most recent session that contains this exact exercise**, never merged across sessions.
- **Exercise matching is by `exerciseId`** (each exercise row is day-scoped via `ExerciseEntity.dayId`), so Previous never mixes other exercises **or** other days. No fallback to `exerciseNameSnapshot` is used — and adding one is intentionally avoided, because a name fallback would risk pulling a *different* day's same-named exercise (e.g. two days both having "Pull-up"), violating the "don't mix other workout days" rule. `exerciseId` is always written on save and is preserved across renames (`@Update` keeps the id), so the id path is correct for all app-created history. Legacy/imported rows with a null `exerciseId` simply show "No previous sets".
- **Why Pull-up showed `0kg x 12 @0`:** that is **real bodyweight data** (0 kg added load), not a wrong/stale source. `SeedData` seeds only days+exercises (never sessions/sets), and `LoggedSetEntity` is only ever written by `saveWorkoutSession`/import, so the chips reflect an actual saved Pull-up session. The matching `Previous 0kg volume` badge (Σ weight×reps = 0) confirms the stored weight is genuinely 0.

**Fix (display only, `LogWorkoutScreen.kt`):** for bodyweight-like exercises (local name match: pull-up, chin-up, push-up, muscle-up, dip, plank) a `0kg` previous set now renders as **`BW x 12 @0`** instead of `0kg x 12 @0`. This is a pure UI formatter (`formatPreviousSet`/`isBodyweightLike`) — **no DB field, no schema change**, and it never touches stored weight, volume math, PR detection, or progressive-overload suggestions. Weighted sets (and weighted variations like a +20 kg pull-up) still show `kg`.

Verified on the Pixel 8 emulator on June 7, 2026 (two Pull-up sessions seeded with different values — older `BW×8 @3`, newer `BW×12 @1` — plus a Lat pulldown `50kg×10 @2` in the newer session):

1. Pull-up **Previous** shows the **newest** session only: three `BW x 12 @1` chips (not the older `8 @3`).
2. Lat pulldown **Previous** shows its own `50kg x 10 @2`; Pull-up never shows the 50 kg sets (no cross-exercise/cross-day mixing).
3. Bodyweight 0 kg renders as **BW**; weighted sets render as `kg`.
4. Regression + clean `adb logcat -b crash`: Quick Start, Log Workout accordion, debounced draft persistence (survives leaving/returning), Save → History, and auto-advance (Pull → Legs) all still work; Home scrolls without crashing.

### Rule-Based Progressive Overload Engine V2 (Task 2.4b)

`ProgressiveOverloadEngine` was upgraded from one-rule-fits-all to a **deterministic, exercise-specific** coach. No AI/LLM, no backend, **no Room schema change**, and `OverloadSuggestion(title, detail, suggestedWeight)` is unchanged so the Log Workout suggestion box and Exercise Detail card render exactly as before.

**Progression types** (temporary name-based inference — until a real exercise database adds metadata; no `progressionType` Room field yet):

| Type | Inferred from (name keywords) | How it progresses |
|------|------------------------------|-------------------|
| `COMPOUND_LOAD` | chest press, bench, squat, deadlift, rdl, leg press, hack squat, row, pulldown, press | Add load once **every** working set reaches `minRep`. Upper body **+2.5 kg**, heavy lower-body (squat/deadlift/leg press/hack/rdl) **+5 kg**. |
| `ISOLATION_SLOW` | lateral raise, rear delt, fly, curl, face pull, extension, pushdown, triceps, biceps, calf raise, wrist | Reps first. Increase only after a **top-range** session — and when history exists, **two consecutive top-range sessions** — then a small **+2.5 kg** "only if the stack allows and form stays strict". |
| `BODYWEIGHT` | pull-up, chin-up, push-up, dip, plank, hanging leg raise, bodyweight (added weight 0) | **Never an automatic +2.5 kg.** Build reps to `minRep`, then toward `maxRep`, then progress reps/tempo/harder variation. A bodyweight name logged **with** added load is treated as a (conservative) compound lift. |

Inference order is bodyweight → compound → isolation, defaulting to the conservative `COMPOUND_LOAD` for unknown names. The name is normalized first (`normalizeName`: lowercase; hyphens, underscores, slashes and unicode dashes → spaces; whitespace runs collapsed; trimmed), so `Pull-up`, `pull up`, `pull_up`, `PULL UP` and `lateral  raise` all match the same keywords.

**Core metrics** (latest session, first `targetSets` working sets): `totalReps`, `minTotal = minRep × targetSets`, `allSetsAtLeastMin`, `allSetsAtTopRange`, `tooHeavy = totalReps < 70% of minTotal`, `currentWeight` = most common working weight (ties → heaviest; drop-set safe), average RIR, and a drop-set flag. **RIR is a modifier, not the decision**: high RIR → more confident wording; mostly-failure (RIR 0) at the rep target → hold instead of increase.

**Conservative guards (`Action`):** fewer sets than target → `KEEP_WEIGHT` (collect data); reps collapse below 70% → `DECREASE_WEIGHT` (~−10%, rounded to 2.5); drop sets (descending weights) → `KEEP_WEIGHT` unless clearly top-range; bodyweight rep targets → `INCREASE_REPS_OR_DIFFICULTY` with no weight chip; no history → "Log first session".

**Data-flow choice (no new query, no schema change):** both call sites already hold the full multi-session history (`recentSetsForExercise` in Log Workout, `observeExerciseHistory` in Exercise Detail, both newest-session-first from Task 2.4a). V2 passes that whole list to the engine, which **groups by `sessionId` internally** — so the isolation "two strong sessions" rule works with **zero** extra DAO queries. With only one session present it degrades to the conservative single-session isolation rule.

**Limitations (deferred to a future task):** progression type is name-inferred, not stored (no metadata, no `ProgressionType` Room field); there is no warm-up/working-set flag, so the first `targetSets` logged sets are treated as working sets; increments are fixed (2.5 / 5 kg) rather than per-equipment; only the two most recent sessions inform isolation.

Verified by `:app:testDebugUnitTest` (`ProgressiveOverloadEngineTest`, 17 deterministic cases) **and** live on the Pixel 8 emulator on June 7, 2026:

1. Compound 3×10–12: `12/11/11` → **Increase (+2.5 → 42.5)**; `12/10/8` → **Hold**; `5/4/3` → **Reduce & recover**. Heavy lower-body (Leg press) → **+5**.
2. Isolation 12–20: `12/12/12/12` → **Hold / build reps** (live: Lateral raise); `20×n` with a second top session → **cautious +2.5** (live: Pec fly → 17.5); top-but-previous-weak → **Hold (one more strong session)**.
3. Bodyweight 6–10 at 0 kg: reps in range → **Build reps / difficulty, no weight chip** (live: Pull-up); added load → treated compound (**+2.5**).
4. Edge cases: fewer sets than target → Hold; drop set → Hold; all-failure at target → Hold; no/invalid history → "Log first session".
5. Regression + clean `adb logcat -b crash`: Previous chips (still `BW x 8 @2`), Log Workout accordion, draft persistence (survives leaving/returning), Save → History, auto-advance (Pull → Legs), Exercise Detail suggestion card, Plan and History all still work.

### Progression Engine V3 — Trend-Aware + Outlier-Aware (Task 2.4c)

V3 builds **on top of** the V2 type rules (`COMPOUND_LOAD` / `ISOLATION_SLOW` / `BODYWEIGHT`, name-inferred). Still deterministic and explainable — no AI/LLM, no backend, **no Room schema change**, no new tables, and the `OverloadSuggestion(title, detail, suggestedWeight)` shape is unchanged (a rule may now refine the generic title via `ProgressionSuggestion.titleOverride`, e.g. **"Hold & verify"** — wording only, no new `Action`).

**1. Main-working-weight selection (mixed weights / drop sets / ramps).** Within a session the working weight is the **most frequent load (ties → heaviest)**. Sets are then classified positionally: lighter sets *before* the first working set = **ramp-up**, lighter sets *after* the last = **back-off/drop** — neither drives the decision. Real cases from the QA screenshots:
- EZ curl `20×8, 20×7, 20×11, 15×12`: main weight **20 kg**; the 15 kg pump set is ignored → **Hold 20 kg** (8/7 under the 10-rep minimum), never a suggestion based on 15 kg.
- Incline DB curl ramp `5×12, 7.5×6, 7.5×7`: main weight **7.5 kg** (mode), the 5 kg opener counts as ramp → only 2 of 3 working sets → conservative **Hold 7.5 kg** with a "settle on one load" explanation (V2 silently mixed all three weights).

**2. Recent-session trend + outlier detection.** The latest session is compared against the **strongest of the previous two comparable sessions** (session score = working weight × average working-set reps; average reps alone for bodyweight; bodyweight vs weighted sessions are never compared). The latest session always carries the most weight — a strong latest session is never dragged down by older history. If the latest score lands **≥30 % below** that baseline:
- default → **Hold & verify**: "could be an off day, an unfamiliar machine, or a logging slip — keep the load, verify next session, reduce only if it repeats" (notes a changed load when the working weight moved >10 %);
- **≥50 % below at RIR ≈ 0** → confident **Reduce & recover**;
- **repeated collapse** (latest *and* previous both ≤70 % of the older baseline) → **Reduce & recover**;
- bodyweight outlier → Hold & verify wording only (never a "weight cut" at BW).

**3. RIR-gated increases (RIR is a modifier, never the sole decision).**
- Compound: increase needs every working set ≥ `minRep` **and average RIR ≥ 1.5** (reps met at RIR 0–1 → Hold "repeat this load until it feels stronger").
- Strong overshoot (every set ≥ `maxRep`, average a full rep past it, avg RIR ≥ 2, not weighted-bodyweight) → **doubled but still safe increment** (+5 upper / +10 lower).
- Isolation: top-range increase additionally requires avg RIR ≥ 1.5 and not mostly-failed (20/20/20 at RIR 0 → Hold). The V2 "two consecutive top sessions" rule stays.
- Severe undershoot logged **with** reps in reserve (e.g. 3/3/3 @ RIR 3) → still steps down but tells the user to **check the weight/unit/machine entry first** (possible input mistake).
- Bodyweight: unchanged — reps/tempo/variation first, never an automatic +2.5 kg.

**4. Decreases land on a real lower step.** "Drop ~10 %" now **floors to the next 2.5 kg step strictly below** the current load. This fixes the screenshot bug where Hammer curl `7.5×3/4/6 @ RIR 0` showed *Reduce & recover… Suggested 7.5 kg* (0.9 × 7.5 = 6.75 rounded back **up** to 7.5). V3 suggests **5 kg**. 40 → 35, 100 → 90, 20 → 17.5.

**Limitations (unchanged constraints, deferred):** progression type still name-inferred (no metadata/Room field); no warm-up/working-set flag (positional ramp/back-off classification is a heuristic); no gym/equipment identity, so "unfamiliar machine" can only be *suspected* from the score drop; fixed 2.5/5 kg steps (no per-equipment increments); trend window is the last 3 sessions; sessions are compared by ordering, not dates; RIR is user-estimated.

Verified by `:app:testDebugUnitTest` — **33 deterministic cases** (all 19 V2 tests kept + 14 new V3 tests: trend/outlier hold-vs-reduce, repeated collapse, strong-latest-not-dragged-down, RIR gates, strong overshoot, EZ-curl back-off, incline ramp, hammer rounding fix, input-mistake wording, BW outlier, BW-vs-weighted comparability) — **and** live with the **release APK** on the Pixel 8 emulator on June 10, 2026:

1. Logged the three QA screenshots as a real Pull session (Pull-up `BW×8×3 @2`, Hammer curl `7.5×3/4/6 @0`, EZ curl `20×8, 20×7, 20×11, 15×12`).
2. Log Workout suggestions render: Hammer curl → **Reduce & recover, Next: 5 kg** (bug fixed, was 7.5); EZ curl → **Hold weight, Next: 20 kg**; Pull-up → **Build reps / difficulty**, no weight chip.
3. Exercise Detail card matches (Hammer curl: *Suggested 5 kg* + correct session history).
4. Previous chips still use the latest session via `exerciseId` (`BW x 8 @2`, `7.5kg x 3 @0`, all four EZ-curl sets incl. the 15 kg back-off).
5. Draft persistence (leave → return keeps `3/3 sets logged`), Save → History (1 session, 10 sets, 798 kg, 7 PR, full set tables), auto-advance (Next exercise; next-day rotation Pull → Legs), Weekly Progress Hub (calendar, 1/4 week, recent session), Home/Plan/History scroll stress — all OK. `adb logcat -d -b crash` **empty**.

**Future Ideas (not implemented):** a per-suggestion **Confidence Score (0–100)** derived from data quantity/consistency; **longer trend windows** (4–8 sessions with decay weighting) once more history accrues; **database-driven progression profiles** (per-exercise metadata replacing name inference, per-equipment increments, gym/equipment identity for true machine-change detection). No code for these yet.

### Progression Engine V3.5 — Catalog-Aware Intelligence

An **intelligence upgrade, not a rewrite**: V3.5 adds an optional `ProgressionProfile` parameter to
`ProgressiveOverloadEngine.suggestion()` / `progression()`. **No Room schema change, no save/draft/
History/import/export change, no engine rewrite** — the V3 rules are intact and all 33 V3 tests still
pass unchanged.

- **Catalog-aware progression profile.** The Log Workout and Exercise Detail ViewModels look up the
  exercise in the catalog (`ExerciseCatalogRepository.findByName`, exact name/alias match) and build a
  `ProgressionProfile` via `ProgressiveOverloadEngine.profileFromCatalog(...)` carrying
  `progressionType`, `defaultIncrementKg`, `sessionsRequiredBeforeIncrease`, `isBodyweight`,
  `isUnilateral`. When a profile is present the engine uses **catalog metadata** instead of guessing
  from the name; when it is **null (custom / unmatched exercise) it falls back to the existing
  name-inference path** exactly as V3 did. The engine stays catalog-package-free (the profile is built
  from primitives in the UI layer), and a failed lookup never crashes — it just degrades to name
  inference.
- **New `SKILL_FORM` type.** Catalog skill/anti-movement exercises (e.g. **Pallof Press**, planks,
  L-sit) now route to a conservative skill rule: progress on reps / holds / harder variation, **never
  an automatic load bump** — so a "Pallof **press**" is no longer mis-read as a compound press by the
  name keyword. Name inference never produces `SKILL_FORM`; only catalog metadata does.
- **Load-jump awareness (better mixed weights / big improvements).** When the latest comparable
  session used a clearly heavier working weight (>10%) than the previous one but did **not** cleanly
  succeed, the engine **holds the new load with positive framing** (e.g. "Strong load increase
  (15kg → 55kg), but not all working sets hit the target yet. Repeat this load…") instead of punishing
  the jump or increasing off an unfinished session. A clean success at the heavier load still flows to
  the normal increase rules. Main-working-weight selection (mode / ramp / back-off) is unchanged.
- **Wording refinement.** The "reps met but low reserve" hold now reads data-specifically, e.g.
  *"You hit the top of the range, but a set reached RIR 0. Repeat this load and keep 1-2 reps in
  reserve before adding weight."*
- **Lightweight confidence score.** `ProgressionSuggestion.confidence` (0–100, deterministic) reflects
  comparable-history depth, a settled working weight, RIR presence, and whether catalog metadata was
  used; "Hold & verify" outliers are explicitly low-confidence. **Engine-only — no UI change** (the
  Log/Detail suggestion cards are visually unchanged).
- **Increment** uses `max(catalog increment, name-based smart step)`, so catalog data can raise but
  never weaken the heavy-lower-body bump.

**Safety preserved:** RIR 0 still blocks blind increases; one strong set never justifies an increase;
bodyweight never gets an early auto +kg; isolation/skill stay more conservative than compound; severe
or repeated collapse still reduces; suspected logging/machine mismatch still avoids overreacting.

**Readiness prompt (tired / normal / strong): deferred.** A save-time readiness signal would need a
new persisted field and a save-flow/UI change, which is out of scope for an engine-only upgrade — so it
is documented here and **not implemented**.

Verified by `:app:testDebugUnitTest` — **42 deterministic cases** (all 33 V3 tests kept + 9 new V3.5
tests: catalog overrides name inference, custom-exercise fallback, mixed-weight hold at the top working
weight, huge-jump positive hold, top-range RIR-0 wording, stable-success higher confidence, bad-day
lower confidence, bodyweight catalog profile gives no random load, catalog increment used when larger).
Catalog tests remain 23/23. Builds: debug `app-debug.apk` + signed release `app-release.apk` both green.

**Limitations (V3.5):** catalog match is by exact name/alias (renamed plan exercises that diverge from
the catalog fall back to name inference); `sessionsRequiredBeforeIncrease` and `isUnilateral` are read
into the profile and used by confidence but do not yet change the core action; confidence is
engine-only (not surfaced in the UI); readiness prompt deferred as above.

### Session Readiness (V3.6A — schema + save flow)

The first slice of the deferred readiness work: **capture and persist** a subjective "how did today
feel" signal per workout. **Data + save UI only — the Progression Engine is byte-for-byte unchanged**
(it does not read readiness yet; that is V3.6B). No save/draft/History/Weekly/Daily-Boost/Plan/Nav
logic changed.

- **Enum:** `data/Readiness.kt` → `TIRED` / `NORMAL` / `STRONG`, with a tolerant `fromName` (trims +
  uppercases; null / blank / unrecognized → `null` = UNKNOWN, never throws).
- **Storage:** nullable `readiness` TEXT column on `workout_sessions` (stores the enum `name`). **null
  = unknown / pre-feature**; new saves store the chosen value (default NORMAL).
- **Migration:** DB **v3 → v4**, additive and user-data-safe:
  `ALTER TABLE workout_sessions ADD COLUMN readiness TEXT` (registered as `MIGRATION_3_4`; **no**
  `fallbackToDestructiveMigration`). First migration to touch a user table — it adds the column in
  place, so every existing session and logged set is preserved (old rows get `readiness = NULL`).
  Verified on-device by upgrading a populated v3 install: no crash, History still showed the older
  sessions.
- **Save flow:** the existing review sheet now shows **"How did today feel?"** (Tired / Normal /
  Strong, **Normal preselected**) above the Save button. It is **non-blocking** — Save works whatever
  is chosen; the value threads `review sheet → LogWorkoutViewModel.saveWorkout(readiness) →
  GymRepository.saveWorkoutSession(..., readiness) → WorkoutSessionEntity.readiness`.
- **Import/Export:** each `workoutSessions` object gains an optional `"readiness"` field. Purely
  additive — old exports without it import as `null`, unrecognized values import as `null`, and older
  app versions ignore the unknown key. `schemaVersion` stays **1** (the importer is key-based and
  tolerant, so a bump isn't required).
- **Tests:** `data/ReadinessTest.kt` (5 pure-JVM cases — valid/case/whitespace, null/blank→null,
  invalid→null, enum-name round-trip). Engine (42) and catalog (23) suites unchanged. *(Room-backed
  migration/save/JSON round-trip aren't part of the JVM `testDebugUnitTest` harness — verified instead
  by the additive migration's design + build + the on-device upgrade/save QA above.)*

**Next:** V3.6B will let the engine read the latest session's readiness as a **confidence/wording
modifier only** (never overriding the RIR-0 / safety gates).

### Session Quality + Readiness Modifier (V3.6B — engine intelligence)

Engine-only upgrade — **no Room schema change, no new columns, no save/draft/History/import-export
change, no confidence UI**. The final training signal stays driven by **objective** performance;
**subjective** readiness is a small modifier on top.

- **Objective session quality (runtime-only, never stored):** a `SessionQuality` of
  `POOR / MIXED / SOLID / EXCELLENT`, derived from the metrics the engine already computes (rep
  completion, top-range, RIR, working-weight stability/ramp-backoff, collapse). EXCELLENT = clean top
  completion with reps in reserve; POOR = severe collapse (too-heavy); MIXED = incomplete / final set
  below target; SOLID = all sets to min without failure. Exposed on `ProgressionSuggestion.sessionQuality`
  for tests; it only **nudges confidence**, never the action or weight.
- **Readiness modifier:** the latest saved session's readiness (V3.6A) is now read by the engine and
  used as a **confidence + wording modifier only**. On a suspected off-day (Hold & verify), **TIRED**
  corroborates the dip ("a tired day, which fits the dip — verify when fresh", higher hold confidence)
  while **STRONG** contradicts it ("a strong day, so double-check the machine or the entry", lower
  confidence). Otherwise STRONG lifts and TIRED tempers confidence. **Safety gates always win:** STRONG
  never forces an increase, TIRED never forces a decrease, RIR 0 / final-set-below-min still block
  increases, and a severe/repeated collapse still reduces even when TIRED.
- **Baseline adaptation:** confirmed and tested. The outlier baseline is the strongest of the last two
  comparable sessions, so an old peak naturally rolls out of the window — a sustained lower level
  becomes the new baseline (no perpetual "drop" vs an old peak), while a one-session dip is Hold &
  verify and a repeated/extreme collapse still reduces. The V3.5 big-jump rule (e.g. 15kg → 55kg with a
  short final set → hold 55kg with positive wording) is retained.
- **Confidence:** now also factors objective session quality and the readiness modifier (still
  bounded 0–100, engine-only). Confidence varies across readiness while the safety-gated action and
  suggested weight stay invariant.
- **Readiness data path (smallest safe wiring):** a read-only `getSessionReadiness(sessionId)` DAO
  query → `GymRepository.readinessForSession` → the Log/Exercise-Detail ViewModels pass the latest
  session's readiness into the engine. No schema/save/repository-write change.

Verified by `:app:testDebugUnitTest` — **56 engine cases** (all 42 prior tests kept + 14 new V3.6B:
quality excellent/mixed/poor, STRONG raises confidence without forcing increase, TIRED lowers without
forcing decrease, top-range RIR-0 softer hold, 50/55/55 positive hold, one-session-drop Hold & verify,
repeated drop still reduces even when tired, sustained-lower new-baseline, bodyweight/isolation/skill
stay conservative with readiness, confidence varies while action/weight invariant). Catalog 23/23 and
Readiness 5/5 unchanged. Builds (debug + signed release) green; on-device smoke: suggestion renders in
Log Workout, no crash.

### Coach Tone Layer (V3.6C — deterministic wording)

Wording-only polish — **no schema, save-flow, UI, action, suggested-weight, or confidence-scoring
change**. Progression reasons were re-worded to read like a supportive coach, and a small
**deterministic paraphrase layer** was added so several exercises in one session no longer repeat the
*same* sentence for the same action. `toneIndex(exercise, n)` folds the **exercise name** to a stable,
non-negative variant index and `pick(...)` selects one — so the **same exercise always reads the same
way** (no refresh-to-refresh churn) while **different exercises vary**. No randomness, no clock, no I/O.
Multi-variant phrasing is applied only to high-frequency, no-protected-substring scenarios (first
session, weight increase, build-reps); the rarer safety/verify scenarios keep a single polished string.
Protected substrings (`off day`, `entered correctly`, `Strong load increase`, `RIR 0`, `reps in
reserve`, the `Hold & verify` / `Log first session` titles) are preserved. Verified by
`:app:testDebugUnitTest` — **60 engine cases** (56 prior + 4 new: deterministic wording, action/weight
invariance across names, ≥2 variants across exercises, stable first-session detail).

### Long-Term Coach Intelligence (V3.7A — per-exercise trend intelligence)

A **read-only trend overlay** on top of the V3.6C decision. It uses the **full per-exercise history
already available** (`recentSetsForExercise`) — **no Room schema change, no new tables/columns, no
save-flow change, no UI change, no new data access, no distance/time/skill/velocity work**. It widens
the comparison window beyond the single-session rules to name three long-term patterns, and it **never
changes the action or the suggested weight** — only the wording, an optional title refinement, and a new
internal classification. The existing `Action` enum is unchanged.

- **New internal classification — `CoachInsight`** (runtime-only, never stored, exposed on
  `ProgressionSuggestion.insight` like `confidence`/`sessionQuality`): `NONE`, `BAD_DAY_VERIFY`,
  `NEW_BASELINE`, `PLATEAU`. UI is unaffected (it still reads title/detail/weight).
- **Goal.** Distinguish *one bad day* from *a settled lower baseline* from *a plateau* — three cases the
  V3.6C single-session outlier rule (latest vs the strongest of the last two sessions) could not tell
  apart, and stop forever comparing the user to an old peak they have moved away from.
- **Bad-day detection.** One session dips while the prior level was *stable* → the existing
  `KEEP_WEIGHT` "Hold & verify" wording, now tagged `BAD_DAY_VERIFY`. (The recent pair is *not* settled,
  so it is not mistaken for a new baseline.)
- **Regression / new baseline.** When the two most recent comparable sessions are **settled** (scores
  within `STABLE_BAND` = 15%) at a level **≥`REGRESSION_DROP` (15%) below an older peak** *and* the lower
  load is **handled** (reps at/above min, not collapsing) *and* it is **not** a clean top-range success,
  the engine accepts the lower level: `KEEP_WEIGHT` at the current load, title **"New baseline"**, insight
  `NEW_BASELINE`, with rebuild-here wording that does **not** chase the old peak. Worked example: an old
  55kg peak followed by three sessions settling at 40kg (set 1 RIR 2, set 2 RIR 1, set 3 RIR 0) → stabilize
  and rebuild at 40kg — **no increase** (40kg is not clearly comfortable) and **no decrease** (reps are
  handled). A user who has cleanly **adapted** to the lower level (top range at RIR ≥ 1.5) still gets a
  normal increase upstream — `NEW_BASELINE` never fires on a clean success and never creates a decrease.
- **Plateau detection.** Same working weight (within `WEIGHT_EPSILON`) and **flat reps** (avg-rep spread
  ≤ `PLATEAU_REP_BAND` = 1) across the recent window, with no sharp drop → insight `PLATEAU`, refining a
  hold or a reps/difficulty cue (never an increase/decrease).
- **RIR trend (a modifier, never the sole truth).** Within a plateau, the latest avg RIR vs the older
  window average (`RIR_TREND_DELTA` = 0.5) splits the advice: **improving** RIR → "room to push, add a
  rep/set"; otherwise (grinding / near failure) → "take a lighter week (deload) or add a set". RIR can
  never flip a safety-gated action — it only changes wording.
- **Safety & graceful degradation.** Below `MIN_TREND_SESSIONS` (3) comparable sessions the overlay is
  **inert** → exact V3.6C behavior (insight `NONE`). All increase/decrease/RIR gates are decided entirely
  upstream; the overlay only refines `KEEP_WEIGHT` and `INCREASE_REPS_OR_DIFFICULTY` outcomes. Bodyweight,
  isolation, and skill-form actions/weights are untouched.

Verified by `:app:testDebugUnitTest` — **70 engine cases** (60 prior + 10 new V3.7A: one-session bad day
stays Hold & verify, repeated lower level becomes new baseline, old peak does not haunt forever, plateau
with RIR improving vs worsening, regression never decreases while an adapted level still increases,
insufficient history keeps V3.6C behavior, and bodyweight/isolation/skill-form safety unchanged with deep
history). Catalog and Readiness suites unchanged; debug build green.

**Limitations (V3.7A).** Per-exercise only — no whole-workout/systemic-fatigue awareness yet, and
readiness is still read **latest-session-only** (no readiness *trend*). Confidence scoring is not changed
by the trend overlay in this phase. Window length is fixed (`TREND_WINDOW` = 6, recent pair for
settle/peak); thresholds are deterministic constants, not adaptive.

**Next:** **V3.7B — readiness trend** (repeated TIRED, STRONG-but-down mismatch, readiness↔SessionQuality
mismatch feeding the bad-day↔regression lean, confidence, and wording — never overriding safety gates),
via a small read-only readiness-series data access. **V3.7C** then adds whole-workout vs exercise-specific
fatigue.

### Readiness Trend Intelligence (V3.7B — subjective trend as a modifier)

V3.6B read only the **latest** session's readiness. V3.7B adds the **trend** across recent sessions and
the **readiness↔objective-quality mismatch**, as a **confidence + wording modifier only** — it never
changes the `Action` or the suggested weight, and it can never bypass a safety gate (increase/decrease/RIR
are decided upstream on objective performance). **No Room schema change, no save-flow change, no UI change,
no new tables/columns.** Objective performance still dominates; readiness is subjective context, not truth.

- **Read-only data access.** A new batch query `getReadinessForSessions(ids)` (projection `SessionReadiness`)
  → `GymRepository.readinessBySession(ids): Map<sessionId, Readiness?>` → the Log/Exercise-Detail
  ViewModels build the map from the recent sessions' ids and pass it to the engine as **optional data**.
  The **engine stays pure** (no DAO inside it); the map is an optional param, so every existing caller/test
  is unchanged. No schema change (reads the existing v4 `workout_sessions.readiness` column).
- **Repeated TIRED** (≥2 of the recent comparable sessions logged tired — a multi-session signal the
  latest-only path can't see): lowers confidence in aggressive progression (−5) and adds a **recovery**
  clause ("treat this as a recovery signal — rest up before pushing again"). Never auto-decreases.
- **STRONG but the session collapsed** (STRONG + `SessionQuality.POOR`, outside the outlier "Hold & verify"
  that V3.6B already owns): keeps the safety action, lowers confidence (−6), and shifts wording to verify
  ("you felt strong but the reps came up short — double-check the machine, your form, or the logged
  numbers"). It only ever *reduces* if the existing safety gates already do.
- **NORMAL but a poor session** (NORMAL + POOR): flags a possible real drop vs a one-off ("verify next
  session; if it sticks it may be a real drop rather than an off day"), −5 confidence.
- **TIRED but excellent** (a positive mismatch): a small confidence bump (+3) and an acknowledging clause —
  and, critically, **TIRED never blocks a valid increase** (a clean session still progresses).
- **UNKNOWN/null** readiness is strictly **neutral** — never penalized, never adds wording.
- **Not too powerful:** the readiness-trend confidence effect is a single, bounded, non-stacking nudge
  (≤6), layered on the V3.6B latest-readiness handling without double-counting (STRONG-but-down excludes
  the outlier hold V3.6B covers). RIR trend (V3.7A) remains a modifier, not the final truth.

Verified by `:app:testDebugUnitTest` — **78 engine cases** (70 prior + 8 new V3.7B: repeated-tired lowers
confidence/adds recovery wording without touching action/weight, STRONG+poor keeps the safety action but
changes verify wording/confidence, NORMAL+poor flags a possible real drop, UNKNOWN is neutral, NORMAL
plateau stays a plateau, TIRED+good performance still increases, readiness trend never bypasses the RIR-0
gate, and a bad day with repeated tired still Holds & verifies with recovery wording). Catalog and
Readiness suites unchanged; debug build green.

**Limitations (V3.7B).** Still **per-exercise** — readiness is read for that exercise's own recent sessions,
not correlated across the whole workout (that's V3.7C). "Repeated tired" uses a simple count over the recent
window, not a decayed/weighted trend. The mismatch signals key off the *latest* session's objective quality
plus the multi-session tired count; deeper readiness modelling (e.g. rolling readiness averages) is future
work. Confidence remains engine-only (no UI surface).

**Next:** **V3.7C — whole-workout vs exercise-specific fatigue**: correlate the latest session's dips across
the day's exercises (systemic fatigue / bad day when many are down; a local/technique issue when only one
is), via a read-only sibling-session data access — no schema change.

### Whole-Workout + Hypertrophy Quality Context (V3.7C-H — hypertrophy-oriented, runtime-only)

A **read-only overlay** that layers whole-session context on top of the per-exercise decision. It is
**hypertrophy-oriented** but **goal-gated** so future goals can plug in without a core rewrite. **No Room
schema change, no new tables/columns, no new DAO query, no save-flow change, no UI change, no
distance/time/skill/velocity progression, no MAYBE-DB import.** It **never changes the action or the
suggested weight** — only confidence, wording, and an internal insight tag; safety gates and the
per-exercise decision are decided entirely upstream. When no context is supplied (or the goal isn't
HYPERTROPHY) the overlay is **inert** — exact V3.7B behavior, so all prior tests pass unchanged.

**The WQS research doc was used as inspiration, not copied.** Audited concept-by-concept (Effective Reps,
EVR, FAI, SFM, WQS) against the engine:
- **Rejected the raw composite Workout Quality Score** (RSS×SFM×(1−FAI/200)×(EVR/100)) — opaque, hypertrophy-
  biased, and it tempts storage/score-chasing. No numeric WQS is computed, stored, or shown.
- **Rejected cross-exercise FAI** (the "first-half vs second-half RIR" formula is fooled by the normal
  compound→isolation order) and the **subjective multiplier** (readiness is already handled by V3.6B/V3.7B —
  no double-counting).
- **Reused** Effective Reps `max(0, 5 − RIR)` and the EVR "productive volume" idea **only** as an internal,
  per-exercise **stimulus** read over already-warmup-filtered working sets — never a gate.

**New internal classifications** (`CoachInsight`, runtime-only, wording/confidence only):
- **`SYSTEMIC_FATIGUE`** — this lift is down **and** ≥2 of the day's lifts are down vs their own baselines →
  whole-session fatigue wording; **never forces a decrease** (a hold stays a hold; a safety decrease is
  preserved unchanged).
- **`LOCAL_DROP`** — this lift is down while the day's others held up → "check the machine/form/setup/logging
  here" wording; **never forces an unsafe increase** elsewhere (each lift is judged on its own).
- **`HYPERTROPHY_QUALITY`** — a per-exercise stimulus note (only when no stronger trend/fatigue label
  applies): avg effective reps `< 1.5` = **submaximal** ("you hit the reps but kept a lot in reserve — lighter
  stimulus", −3 confidence, action unchanged — **not** treated as failure); `≥ 2.5` = **productive** ("clean
  stimulus, no junk volume", +2 confidence). Order-safe by construction (per-exercise, not cross-exercise), so
  a light isolation after a heavy compound is judged on its own and never rewarded as "recovery".

**Goal extensibility.** `WorkoutGoalProfile { HYPERTROPHY, STRENGTH, POWER, ENDURANCE, SKILL }` (default
HYPERTROPHY). Only HYPERTROPHY drives behavior now; the others are reserved and **no-op**, so STRENGTH/POWER/
ENDURANCE/SKILL branches can be added later without changing the goal-neutral core rules (rep targets + RIR
gates). No user-facing goal UI yet.

**Data access (smallest possible = zero new queries).** `LogWorkoutViewModel` already holds every day-exercise's
recent history in memory (`historyByExercise`), so it computes the `WorkoutContext` (how many of the day's
lifts are currently down vs their own baselines) using a pure engine helper `latestSessionDownVsBaseline(...)`
— **no new DAO query, no repository change**. The engine stays pure: `WorkoutContext` is optional input data,
never a service locator, and is not stored on `ProgressionSuggestion`. `ExerciseDetailViewModel` (single
exercise, no siblings) passes a hypertrophy context with no sibling counts, so only the per-exercise stimulus
note can apply there — never systemic/local.

Verified by `:app:testDebugUnitTest` — **89 engine cases** (78 prior + 11 new V3.7C-H: multiple-down systemic
fatigue holds not decreases, one-down local drop, systemic-on-a-decrease keeps the safety decrease, high
productive ratio good-stimulus wording, many high-RIR sets read as lower stimulus not failure, isolation-after-
compound is neutral not rewarded, TIRED+strong workout still increases, STRONG+poor whole-workout verify wording,
null-context inert, non-HYPERTROPHY goal inert, and the pure sibling-down helper). Catalog and Readiness suites
unchanged; debug build green.

**Limitations (V3.7C-H).** Whole-workout systemic/local is computed on the **Log** screen (where the day's
exercises are in memory); Exercise Detail gets only the per-exercise stimulus note. "Siblings down" is a
same-day proxy (each exercise's latest vs its own baseline), not a strict single-session join. Stimulus
thresholds are hypertrophy defaults (deterministic constants), and the effective-reps model trusts logged RIR
(self-reported) — it is a confidence/wording nudge, never a gate. Only HYPERTROPHY is implemented; other goals
are structural placeholders.

**Next:** goal-aware branches (STRENGTH/POWER/ENDURANCE/SKILL) that plug into `WorkoutGoalProfile`, and an
optional confidence/insight UI surface — both deferred; no schema/save/UI change required to add them.

### Hypertrophy Plateau Strategy Engine (V3.8-H — runtime-only, hypertrophy-oriented)

The engine already **detects** a plateau (`CoachInsight.PLATEAU`, V3.7A); V3.8-H turns that into an
actionable **"how to get unstuck" strategy**. It is a **read-only overlay** that runs last in
`progression()` and is **gated behind a hypertrophy `WorkoutContext`** — with a null context (or a
non-HYPERTROPHY goal) it is inert, so all prior tests are byte-identical. **No Room schema change, no new
DAO query, no save-flow change, no UI change** (the strategy text flows through the existing suggestion
detail), no distance/time/skill/velocity progression, no MAYBE-DB import. It **never changes the action or
the suggested weight** — only wording, confidence, and a new runtime-only `PlateauStrategy` tag; safety
gates always win, and nothing is stored or shown as a raw score.

**`plateau_strategy.md` was used as inspiration, not copied.** Audited concept-by-concept:
- **Rejected the EVL / ΔP second detector** (couples subjective RIR into volume math and duplicates
  detection) — V3.8-H **reuses `CoachInsight.PLATEAU`** (same working weight + flat reps over ≥3 comparable
  sessions) as the single trigger.
- **Rejected the weekly muscle-volume / MRV math**, the **escalation/cooldown state machine**, and
  **dynamic rep-range-target mutation** — all need stored cross-session state the runtime, pure engine
  cannot access (no schema change).
- **Kept** double progression, deload-for-fatigue, rep-range rotation, and the effort-deficit idea — each
  realized on top of signals the engine already computes.

**New internal type** `PlateauStrategy { NONE, INCREASE_EFFORT, ADD_REP, ADD_SET, DELOAD, CHANGE_REP_RANGE,
STABILIZE_BASELINE }` on `ProgressionSuggestion` (runtime-only, never stored). Rather than the doc's heavy
PlateauType/Scope/RootCause/Advice objects, scope/type are read from the existing `CoachInsight`
(PLATEAU/SYSTEMIC_FATIGUE/LOCAL_DROP) and the root cause is internal-only — one enum is enough.

**Strategy selection (priority order), on a confirmed `PLATEAU`:**
1. **DELOAD** — repeated‑tired readiness trend (V3.7B) or systemic siblings‑down (V3.7C-H) = accumulated
   fatigue. Never fired for a single bad day (that isn't a PLATEAU) and never forces a decrease.
2. **INCREASE_EFFORT** — latest avg RIR ≥ 3 (leaving too much in reserve). The RIR gate guarantees this is
   never suggested at an already-low RIR.
3. **ADD_REP** — RIR is improving within range → double progression (chase a rep, then load).
4. **CHANGE_REP_RANGE** — a deep (≥4-session) persistent local stall → rotate the rep range (keep the
   exercise; targets are never mutated — wording only).
5. **ADD_SET** — a shallower local stall with adequate effort and no systemic fatigue → one more working
   set, **guarded** (only 3–4 existing working sets, never at ≥5 = junk-volume guard).
- **STABILIZE_BASELINE** — for a forming `NEW_BASELINE` or a one-off `BAD_DAY_VERIFY`: hold and gather data,
  never a plateau strategy (no "deload for a bad day", no new-baseline spam).

**How the existing signals drive it:** RIR separates *effort deficit* (RIR high) from a *real* stall
(effort adequate) and *improving* (double progression); the V3.7B readiness trend separates *fatigue
accumulation* (repeated tired → deload) from a local stall; the V3.7C-H `WorkoutContext` separates
*systemic* (many lifts down → deload) from *local* (rotate/add-set); and the V3.7C-H hypertrophy stimulus
read informs the effort-vs-volume call. **Goal-extensible:** only HYPERTROPHY is active; STRENGTH/POWER/
ENDURANCE/SKILL are reserved on `WorkoutGoalProfile` and remain inert.

Verified by `:app:testDebugUnitTest` — **99 engine cases** (89 prior + 10 new V3.8-H: plateau+high-RIR →
INCREASE_EFFORT, improving RIR → ADD_REP, repeated-tired/systemic → DELOAD without forcing a decrease, deep
local plateau → CHANGE_REP_RANGE, shallow local plateau → guarded ADD_SET, one bad day → STABILIZE_BASELINE
not deload, new baseline → STABILIZE_BASELINE not spam, sparse history → no aggressive strategy, RIR-0 gate
still blocks the increase, and isolation/bodyweight safety unchanged). Catalog and Readiness suites
unchanged; debug build green.

**Limitations (V3.8-H).** Strategy is chosen from the *current* window only — no escalation memory (which
strategy was tried before) since nothing is stored. "Rep-range staleness" is approximated by plateau depth
(the engine doesn't track a per-exercise assigned rep range); CHANGE_REP_RANGE is advice only and never
edits targets. ADD_SET is a guarded heuristic without true weekly per-muscle volume (no MRV data). Only
HYPERTROPHY is implemented. Strategy remains engine-only (no dedicated UI surface yet).

**Next:** an optional UI surface for the `PlateauStrategy` (e.g. a "try this" chip), and goal-aware plateau
strategies once STRENGTH/POWER/ENDURANCE/SKILL branches land — both deferred; no schema/save/UI-core change
needed to add them. *(The goal-aware branches landed as V3.9-G below; the UI surface remains deferred.)*

### Goal-Aware Progression Layer (V3.9-G — runtime-only, all overlays wording/strategy/confidence)

Makes the quality/plateau overlays **goal-aware** across HYPERTROPHY / STRENGTH / POWER / ENDURANCE /
GENERAL_FITNESS (SKILL stays overlay-inert: control work progresses on form, not effort math). **No Room
schema change, no new DAO query, no save-flow change, no UI change, no Action-enum change.** Like every
prior overlay it **never changes the action or the suggested weight** — verified by a dedicated
invariance test that runs the same input under every goal and asserts identical `action` +
`suggestedWeight`. Nothing new is stored; no raw score is computed or shown.
*(Historical V3.9 behavior: V3.11B-MG later adds conservative, fully tested suppression of unsupported
non-H increases; the H branch remains unchanged.)*

**Where the goal comes from (no goal UI exists yet).** An explicit non-default `WorkoutContext.goal`
always wins (a future goal setting plugs in with zero engine change). When callers pass the default
HYPERTROPHY — every current caller does — the engine resolves an **effective per-exercise emphasis from
the rep targets**: `targetMaxReps ≤ 6` → STRENGTH, `targetMinReps ≥ 20` → ENDURANCE (20 on purpose:
15–20-rep isolation is still normal hypertrophy work; hypertrophy is effective across ~6–30 reps).
POWER / GENERAL_FITNESS / SKILL are **never inferred** — explicit context only. FAT_LOSS is deliberately
**not** a separate mode: fat-loss training is programmed like GENERAL_FITNESS (the deficit lives in the
diet, which the app doesn't track).

**Science grounding (used as principles, not copied formulas):** proximity to failure meaningfully
drives hypertrophy but has a **negligible relationship with strength gain** (Robinson et al. 2024
meta-regression, *Sports Medicine*) → strength branches never prescribe "closer to failure" as the fix;
power quality degrades with grinding — **non-failure, low-velocity-loss sets are preferred** (VBT
literature) → POWER never gets `INCREASE_EFFORT` and a low-RIR power session draws a "keep reps crisp"
caution (`CoachInsight.GOAL_QUALITY`, new enum value, runtime-only); endurance progresses on
**reps/time/density before load**; general fitness favors consistency and sustainable effort.

**Goal-aware plateau strategies** (fatigue always deloads first, for every goal):
- **HYPERTROPHY** — V3.8-H behavior byte-identical (effort → double progression → rotate range → add set).
- **STRENGTH** — effort/double progression when clearly under-efforted or improving; deep stall → rotate
  the range for a block; otherwise **STABILIZE** ("strength moves slower") — **never ADD_SET** (junk-volume).
- **POWER** — deep stall → vary the load/speed emphasis for a block; otherwise quality/speed hold —
  **never INCREASE_EFFORT**, no failure/grinding advice anywhere.
- **ENDURANCE** — reserve left or improving → **ADD_REP** (more work/density, "not more load"); shallow
  stall → guarded ADD_SET; deep → change the challenge. High reps at high RIR are never "failure".
- **GENERAL_FITNESS** — improving → gentle rep nudge; otherwise consistency-first STABILIZE. Poor
  recovery (repeated tired / systemic) still routes to DELOAD before anything aggressive.
Systemic-vs-local whole-workout classification is now **goal-agnostic** (every goal compares each lift
to its own baseline); the hypertrophy stimulus note stays HYPERTROPHY-only.

**Wording fixes:**
- **RIR-0 plural bug:** "one set reached RIR 0" now counts — `60kg 12@2 / 12@0 / 12@0` reads "**2 sets**
  reached RIR 0" (one set still reads "one set").
- **Contradiction guard:** the "Clean stimulus" praise is suppressed whenever any working set actually
  hit RIR 0 (praise never sits next to a failure caution). Both stimulus clauses lost the jargon
  ("junk volume" / "(high RIR)" phrasing gone).
- The isolation top-range near-failure hold reads plainly: "Top reps, but you finished right at your
  limit — hold this weight one more session with a rep in reserve, then add."

**Multi-metric (time/distance) support at V3.9-G — historical minimal fallback.** `LoggedSetEntity` already persists
`durationSeconds`/`distanceMeters` (DB-6B), and non-rep sets use `reps = 0` sentinels — which previously
made the engine treat a Plank/Sled history as *no history* ("Log first session … start around **0-0
reps**", visible in Log + Exercise Detail). Now: when the history contains only non-rep sets the engine
returns a safe, load-free read — title **"Build time"** / **"Build distance"**, latest-vs-previous
session total comparison (e.g. "Up from 2:30 to 3:00 total"), never a suggested weight, never invented
metrics. **Contract for full multi-metric progression (deferred):** requires the exercise's
`trackingType` passed into the engine (e.g. via `ProgressionProfile`), per-type targets (target
duration/distance don't exist — `defaultMin/MaxReps` are 0 sentinels), and progression rules per type
(TIME_ONLY: +time; DISTANCE_LOAD: +distance then +load; TIME_DISTANCE_LOAD: density). This minimal
field-presence fallback is superseded by the explicit V3.11A contract below.

Verified by `:app:testDebugUnitTest` — **114 engine cases** (99 prior kept + 15 new V3.9-G: RIR-0
plural + singular wording, productive-clause contradiction guard, strength plateau never-add-set +
patience wording, strength inferred from 3-5-rep targets, power plateau no-grind advice, power low-RIR
crispness caution, endurance plateau reps/density preference, endurance high-RIR not shamed, general
fitness sustainable advice, action/weight invariance across all six goals, SKILL no prescription,
duration-only safe degradation with trend, duration first-session no fake rep target, isolation top-range
final-RIR-0 plain hold). Full JVM suite **273 tests, 0 failures**; debug build green.

**Limitations (V3.9-G).** Goal is rep-target-inferred only for STRENGTH/ENDURANCE emphasis (no user
goal setting yet; POWER/GENERAL_FITNESS reachable only by a future explicit context); strength "bar
speed" and power "velocity" cannot be measured from reps/RIR logs, so those branches advise rather than
detect; at V3.9 the time/distance read compared session totals only. V3.11A now adds explicit
tracking-type comparability while intentionally keeping target-aware programming and automatic
carry/sled loading deferred. Readiness/deload interplay is inherited from V3.7B/V3.8-H unchanged. `PlateauStrategy` and
`CoachInsight` remain engine-only (no UI surface).

### Goal-Aware Plateau Safety (V3.10 — runtime-only policy hardening)

V3.10 closes the default-context safety gap without changing the goal-neutral core decision. When a
caller uses the current default `WorkoutContext()`, a catalog `SKILL_FORM` profile now resolves to the
effective `SKILL` goal before rep-target inference. Skill/control work therefore keeps its form-first
core advice and receives neither hypertrophy stimulus text nor a load/volume plateau strategy. An explicit
non-default goal still wins, so this is a default-safety guard rather than a goal-selection feature.

- **Strength:** high-reserve plateau wording now asks for crisp, repeatable, technically stable reps and
  a load increase only after that quality is owned. It never tells a strength user to move closer to
  failure or chase an RIR target. The existing action and suggested-weight decision remain unchanged.
- **Power:** remains conservative: no `INCREASE_EFFORT` plateau strategy and low-RIR work gets a speed/
  quality caution. Reps/RIR cannot measure bar speed, velocity loss, or power output, so this is advice
  only — not power-progression detection or automated load prescription.
- **Endurance / GENERAL_FITNESS:** remain outside hypertrophy stimulus judgement; high reserve is not
  labelled a weak hypertrophy session. Their existing reps/density or consistency-first strategy wording
  is unchanged.

No Room schema, DAO, repository, save-flow, UI, catalog, import, Action enum, or signing change. The
engine remains pure and every goal overlay still preserves `action` and `suggestedWeight`. Verified by
`:app:testDebugUnitTest`: **116 `ProgressiveOverloadEngineTest` cases** and **275 JVM tests**, all passing.
New regression coverage proves a default-context catalog `SKILL_FORM` result is overlay-inert and that a
strength high-reserve plateau preserves action/weight while using technique-first, non-failure wording.
*(V3.11B-MG later turns unsupported non-H progression into a HOLD at a final safety stage; it never
overrides a safety decrease.)*

### Multi-Metric Progressive Overload Contract (V3.11A - runtime-only)

V3.11A makes metric meaning explicit before additional goal branches are enabled. The engine remains
pure: catalog `TrackingType.name` is converted to the domain-only `ProgressionTrackingType` inside
`ProgressionProfile`; there is no DAO/Room/catalog dependency in `ProgressiveOverload.kt`. Log Workout
and Exercise Detail now pass the matched catalog tracking type into the existing profile. Custom or
unmatched exercises retain the existing `REPS_WEIGHT` behavior.

**Verified current storage and reachability:** Room is v8; `LoggedSetEntity` stores `weight`, `reps`,
`rir`, optional `durationSeconds`, optional `distanceMeters`, and stable `canonicalExerciseKey`.
Export/import schema v5 already round-trips all of those set metrics. The 577-entry catalog contains
471 `REPS_WEIGHT`, 87 `BODYWEIGHT_REPS`, 8 `SKILL_FORM`, 8 `TIME_DISTANCE_LOAD`, 2 `TIME_ONLY`, and 1
`DISTANCE_LOAD`. `TIME_LOAD` and `TIME_DISTANCE` exist in the catalog enum/logging path but currently
have no catalog entries. `DISTANCE_ONLY` is a domain future contract only; it is not a current catalog
`TrackingType`, so no UI/logging reachability is claimed.

**Comparison contract:** metric sessions are grouped by `sessionId`, newest first. Only the latest two
sessions are compared. Every required metric must be positive on every set, the number of sets must
match, and a load-bearing session must use one stable load. A single valid session establishes a
baseline. Missing fields return `INCOMPLETE`; changed set count, mixed tracking shapes, unstable loads,
or multiple changed dimensions return `INCOMPARABLE`. Results are structured as
`BASELINE / IMPROVED / PLATEAU / REGRESSED / INCOMPLETE / INCOMPARABLE / ADVISORY_ONLY`. They are
runtime-only: no derived score or status is stored or shown as a raw score.

| Tracking contract | Required fields | Primary / secondary progression | Safe implemented comparison | Must not infer |
|---|---|---|---|---|
| `REPS_WEIGHT` | reps, load, RIR | reps, then load | Existing H branch unchanged | Nothing new; V3 action/RIR gates stay authoritative |
| `BODYWEIGHT_REPS` | reps, RIR; optional added load | reps/difficulty, then added load | Existing bodyweight branch unchanged | No automatic added-load jump from bodyweight work |
| `TIME_ONLY` | duration | duration; form/technique | Same set count: longer total = improved, equal = plateau | A timed hold is not pace or power |
| `TIME_LOAD` | duration + stable load | duration at same load; load at same duration | Exactly one changed dimension can be compared | No automatic load prescription |
| `DISTANCE_ONLY` | distance | distance; technique | Same set count: farther = improved | Future contract only; no current catalog/UI support |
| `DISTANCE_LOAD` | distance + stable load | distance at same load; load at same distance | Farmer carry can recognize either clean single-variable improvement | No automatic load increase or equipment equivalence |
| `TIME_DISTANCE` | time + distance | pace/distance | Faster at same distance, or farther in same time | No pace claim when both time and distance change |
| `TIME_DISTANCE_LOAD` | time + distance + stable load | pace/distance at same load | Sled work compares one variable only and says the sled/surface must match | No cross-machine/surface equivalence; no automatic load increase |
| `SKILL_FORM` | current reps/load/RIR logging; real quality is not stored | form/control | Advisory/form-first route only | Reps, load, or RIR cannot prove better skill |

**Power/Olympic safety:** the catalog is mixed: some skill/control entries use `SKILL_FORM`, while
Olympic-style load/reps entries such as Power Clean can still reach the reps/load branch. Load and reps
can describe work, but without velocity or a reliable quality metric they cannot prove higher power.
V3.11A therefore adds no power score, velocity estimate, or automatic power-load prescription.

**Why the guardrails are conservative:** the 2026 ACSM resistance-training position stand emphasizes
goal specificity and progressive overload without requiring complicated prescriptions
([ACSM summary](https://acsm.org/resistance-training-guidelines-update-2026/)). A systematic review and
meta-analysis found that lower velocity-loss thresholds better support sprint/jump/submaximal-velocity
outcomes than high-fatigue sets, supporting the no-grind/no-fake-power rule
([PubMed 36178597](https://pubmed.ncbi.nlm.nih.gov/36178597/)). Sled resistance depends on friction as
well as loaded mass, so load alone is not portable across sleds/surfaces
([PubMed 22964856](https://pubmed.ncbi.nlm.nih.gov/22964856/)). These sources validate the safety
boundaries; the engine does not copy research formulas or create fake precision.

**Preserved and deferred:** the existing hypertrophy H branch, `Action` enum, action/suggested-weight
safety, readiness, plateau, whole-workout context, Room schema, save flow, import/export, UI layout,
catalog assets, and signing are unchanged. Per-type target duration/distance, surface/equipment identity,
velocity/power capture, technique ratings, metric PRs, multi-session metric plateau strategies, and full
STRENGTH/POWER/ENDURANCE/GENERAL_FITNESS programming remain intentionally deferred. Future goal branches
plug in after this tracking/comparability layer; they must consume its status rather than reinterpret raw
fields or exercise names independently.

Validation: `ProgressiveOverloadEngineTest` contains **129 passing cases**, including
time-only improvement/plateau/incomplete data, distance and load single-variable progression,
time+distance pace comparability, mixed/incomplete fallback, sled/carry no-auto-load safety,
skill/power advisory behavior, and reps/load action-weight invariance. Full JVM/build evidence is recorded
by `:app:testDebugUnitTest` (**288 tests, 0 failures**) and `:app:assembleDebug` (successful; debug APK
generated). The only build notices were the existing compileSdk 35 / Android Gradle Plugin 8.5.2
compatibility warning and existing kapt option warnings.

### Multi-Goal Metric Progression Policy (V3.11B-MG - runtime-only)

V3.11B-MG keeps V3.11A as the **only metric detector**. `metricProgression(...)` still owns field
validation, session comparability, and the neutral status; the new pure `metricGoalPolicy(...)` consumes
only that `MetricProgressionResult`, the requested goal, and optional latest readiness. It never re-reads
sets or creates a second detector. Neutral evidence now also identifies the one changed dimension
(`DURATION`, `DISTANCE`, `PACE`, or `LOAD`) and whether the tracking type had to be inferred.

The runtime-only `MetricGoalPolicyResult` carries the goal, neutral evidence, user-facing suggestion,
`CoachInsight`, `PlateauStrategy`, and categorical `LOW / MODERATE / HIGH` confidence. No derived score is
stored or exposed. Every metric policy returns `suggestedWeight = null`; carry, sled, hold, distance, and
pace evidence can describe what changed but cannot silently prescribe a new load.

| Goal | `IMPROVED` | `PLATEAU` / `REGRESSED` | Forbidden inference / fallback |
|---|---|---|---|
| `HYPERTROPHY` | Preserves the V3.11A neutral suggestion exactly | Preserves the completed H behavior | No V3.11B reinterpretation of the H branch |
| `STRENGTH` | Higher load on the same task is a moderate loaded-task signal; time/distance/pace is only work-capacity evidence | Stabilize setup/load and repeat clean reps | Never chase failure; metric work alone does not prove strength or technique |
| `POWER` | Advisory only, even when a metric improved | Stabilize quality; no confident output claim | No measured power claim without velocity/jump/throw output; no aggressive load progression or RIR-0 reward |
| `ENDURANCE` | Comparable duration, distance, or pace can support progression | Change one measured dimension in a small step; regression stabilizes | Load alone is not endurance progress; multi-variable changes remain incomparable |
| `GENERAL_FITNESS` | Modest, repeatable progression; poor readiness tempers the next step | Consistency-first hold/rebuild | No aggressive load/density wording on a tired day and no recovery claim from one readiness value |
| `FAT_LOSS_SUPPORT` | Same sustainable work-capacity policy as general fitness | Consistency-first hold/rebuild | Exercise supports the goal; logged work does not measure fat loss or body-composition change |
| `SKILL` | Always form/control/repeatability first | Remains advisory with no load escalation | Reps/load/RIR do not measure technique; the rep-based trend overlay is also inert for `SKILL_FORM` |
| `UNKNOWN` | Low-confidence neutral hold | Low-confidence neutral hold | Repeat comparable work before choosing a progression policy |

`BASELINE`, `INCOMPLETE`, and `ADVISORY_ONLY` are treated as policy-level **insufficient evidence**;
`INCOMPARABLE` is always low confidence. None can produce an aggressive strategy. An inferred/unknown
tracking shape is also capped at low confidence, clears its plateau strategy, and asks the caller to
confirm the exercise profile.

**Action and RIR safety.** The reps/load H branch and its existing action/suggested-weight gates are
unchanged. A final non-H safety stage can only turn an unsupported increase into HOLD; a pre-existing
safety decrease is never suppressed. Explicit POWER holds every reps/load increase and any RIR-0 result.
STRENGTH holds an attempted progression when even one working set reached RIR 0. SKILL keeps progression
advisory when technique quality is unavailable, UNKNOWN holds without a policy, and TIRED
GENERAL_FITNESS/FAT_LOSS_SUPPORT work cannot auto-increase load in the same recommendation. These guards
are deterministic and covered for the real catalog-shaped `REPS_WEIGHT` Power Clean path, including the
case where one RIR-0 set is hidden by a safe-looking session average.

**Goal source at V3.11B-MG completion.** `GoalSource.AUTO / EXPLICIT` distinguished the default from a
future selected HYPERTROPHY goal, but production ViewModels still passed `WorkoutContext()` with AUTO.
Rep-based AUTO inference and all explicit policies were deterministic but only AUTO was runtime-reachable.
V3.11C below supersedes that production-wiring limitation without changing these policies.

**Evidence used for the touched policy boundaries:** the 2026 ACSM position-stand summary emphasizes
goal specificity, individualization, sustainable adherence, heavier loading for strength, and fast
concentric intent for power ([ACSM 2026](https://acsm.org/resistance-training-guidelines-update-2026/)).
A meta-regression found proximity to failure more relevant to hypertrophy than strength, supporting the
non-failure strength wording ([PMID 38970765](https://pubmed.ncbi.nlm.nih.gov/38970765/)). A velocity-loss
meta-analysis supports limiting fatigue when speed/power outcomes matter
([PMID 36178597](https://pubmed.ncbi.nlm.nih.gov/36178597/)). ACSM's weight-management consensus supports
physical activity as part of weight management without treating one logged exercise metric as measured
fat loss ([PMID 39277776](https://pubmed.ncbi.nlm.nih.gov/39277776/)). These sources define conservative
boundaries; no research formula or fake precision was copied into the engine.

**Intentionally advisory/deferred:** velocity and velocity loss, jump/throw output, technique ratings,
surface/sled identity, rest intervals and true density, target duration/distance, and multi-session metric
programming are not available. Latest readiness only tempers wording; it is not
treated as truth. Metric plateau is currently two comparable sessions, so policy stays conservative.
No Room/schema, catalog, import/export, save-flow, UI, ViewModel, signing, or release-config change is part
of V3.11B-MG.

Validation: `ProgressiveOverloadEngineTest` contains **152 passing cases** (129 prior + 23 deterministic
V3.11B-MG/safety cases). The full JVM suite contains **311 tests, 0 failures**, and `:app:assembleDebug` succeeds.
Coverage includes every goal policy, each neutral fallback, explicit POWER action/RIR guards, endurance
duration/distance/pace, general-fitness readiness wording, no fat-loss outcome promise, SKILL trend
inertness, unknown tracking/goal fallback, and preserved H action/suggested-weight decisions.

**Completed next boundary:** V3.11C implements the persisted production goal source described below.
Measured power/technique data remains later work and is not inferred by V3.11C.

### Goal Source, Persistence, and Engine Wiring (V3.11C)

V3.11C makes every existing V3.11B-MG goal policy production-reachable through one user-global source of
truth. AURON currently has one local plan and no `PlanEntity` or current-plan selection owner, while
`UserPreferencesRepository` already owns user-global settings. The selected goal therefore belongs in its
existing SharedPreferences store under `workout_goal`; workout/session/exercise ownership and a Room
migration would add precedence and historical semantics the current product does not have.

`WorkoutGoalSelection` is the stable persistence contract: `AUTO`, `HYPERTROPHY`, `STRENGTH`, `POWER`,
`ENDURANCE`, `GENERAL_FITNESS`, `FAT_LOSS_SUPPORT`, and `SKILL`. Missing, blank, legacy, or invalid values
resolve to `AUTO`. AUTO maps to `WorkoutContext(goal = HYPERTROPHY, goalSource = AUTO)` so the existing
low-rep strength, high-rep endurance, `SKILL_FORM`, and default hypertrophy inference remains unchanged.
Every saved explicit selection maps to the matching profile with `GoalSource.EXPLICIT`; explicit
HYPERTROPHY therefore cannot be re-inferred from rep ranges.

`LogWorkoutViewModel` and `ExerciseDetailViewModel` observe the same preference `StateFlow` and use the
same pure `toWorkoutContext()` resolver. Log retains its sibling-fatigue counts; Detail has no sibling
context, but both receive identical goal/source precedence. The existing V3.11A metric detector and
V3.11B-MG policies remain untouched. The Log session-level hint is goal-neutral; per-exercise targets still
come only from the engine. Settings originally contained a narrow goal picker needed to exercise
persistence; V3.11D now replaces that temporary surface with the production selector documented below.

Goal changes affect future runtime advice only. Historical sessions and sets are not rewritten or tagged.
Room remains version 8, no entity/DAO/migration/save-flow change was needed, and the JSON export schema
remains version 5. Like theme, units, and training weekdays, this user-global preference is not part of the
current plan/history JSON backup. Import therefore preserves the device's current goal instead of silently
overwriting it; exporting preferences can be considered separately if that backup contract is expanded.

Validation: the V3.11C source suite adds **14 deterministic tests** for legacy/invalid fallback, persistence
reload, AUTO inference, explicit override, all supported explicit policies, Log/Detail resolver parity,
metric/reps-load parity, and preserved Action/suggested-weight safety. The full JVM suite now contains
**325 tests, 0 failures**; all **152 ProgressiveOverloadEngineTest** cases remain green. Debug and release
APKs build successfully. Emulator QA verified `Power` persisted across process restart and produced
`Power remains advisory` in both Exercise Detail and Log for the same tracked Plank history; restoring
AUTO also persisted across restart, Home relaunched normally, and the app crash buffer was clean.

**Completed next boundary:** V3.11C.1 adds the program-goal x exercise-modality compatibility resolver
below. The persistence contract remains unchanged.

### Program Goal x Exercise Modality Compatibility (V3.11C.1)

V3.11C established the user's **program intent**; V3.11C.1 keeps that intent distinct from an individual
exercise's **modality**. `ExerciseGoalCompatibilityResolver` is a pure policy selector. It consumes only
the existing `WorkoutContext`, catalog-derived `ProgressionType`, `ProgressionTrackingType`, and target
rep fields, then returns one existing `WorkoutGoalProfile`. It never reads sets, compares sessions,
creates advice, stores state, or duplicates `metricProgression(...)` / `metricGoalPolicy(...)`.

Precedence is deterministic:

1. `SKILL_FORM` progression or tracking always resolves to `SKILL`, regardless of the program goal;
   technique quality is unavailable, so form-first safety cannot be overridden.
2. `AUTO` is byte-compatible with the prior contract: metric rows do not use rep sentinels for goal
   inference; reps/load rows still infer STRENGTH at `max <= 6`, ENDURANCE at `min >= 20`, otherwise
   HYPERTROPHY.
3. A BODYWEIGHT/ISOLATION metric accessory inside a POWER program uses the existing sustainable
   GENERAL_FITNESS work-capacity policy instead of pretending the accessory is a power exercise.
4. All already-compatible explicit combinations keep their program policy. This includes reps/load
   compound lifts, compound metric work (conservatively advisory for POWER), ENDURANCE metric work,
   GENERAL_FITNESS/FAT_LOSS_SUPPORT, and Strength's existing metric wording.

Verified examples: explicit HYPERTROPHY + Plank (`BODYWEIGHT` / `TIME_ONLY`) preserves the neutral
`Build time` result with no hypertrophy-stimulus overlay; POWER + Plank uses `Sustainable progress` with
no power claim; POWER + Sled Push (`COMPOUND_LOAD` / `TIME_DISTANCE_LOAD`) remains `Power remains
advisory`; POWER + Power Clean (`COMPOUND_LOAD` / `REPS_WEIGHT`) keeps the existing hold/no-output safety;
and any explicit goal + `SKILL_FORM` resolves to `Form first`.

Only policy selection changed. The V3.11A metric detector, V3.11B-MG policies, reps/load rules, readiness,
trend/plateau formulas, `Action` enum, suggested-weight calculations, Room v8, persistence, export/import,
save flow, catalog, UI, and signing configuration are unchanged. The existing safety guards remain the
only layer allowed to suppress an unsafe increase; no new formula or detector was added.

Validation: **9 new deterministic compatibility tests** cover AUTO, explicit goals, skill precedence,
metric/accessory/compound modalities, HYPERTROPHY Plank, POWER Plank, POWER Sled Push, POWER skill-form,
and reps/load action/weight safety. The full JVM suite is **334 tests, 0 failures**; all **152 existing
ProgressiveOverloadEngineTest** and **14 WorkoutGoalSourceTest** cases remain green. Debug and release
APKs build successfully.

**Limitations:** modality resolution is only as specific as today's catalog metadata. It does not infer
explosiveness from an exercise name, distinguish sled surfaces, measure technique/velocity/output, or
turn timed work into a measured strength/power result. Unknown/custom profiles retain their conservative
existing fallback rather than receiving a guessed modality.

**Completed next boundary:** V3.11D adds the production Goal Selector UX below while reusing the V3.11C
persistence and V3.11C.1 compatibility contracts without changing policy formulas.

### Goal Selector UX (V3.11D)

V3.11D completes the first user-facing training-goal experience in **Settings > Training goal**. The
Settings card now shows whether the current choice is `AUTO` or an `EXPLICIT GOAL`, the selected goal's
plain-language meaning, and the future-only scope. `Review training goal` opens the shared AURON modal-sheet
surface rather than a dense label-only dialog; no navigation route or onboarding gate was added.

The selector deliberately separates **AUTO** from **EXPLICIT GOALS**:

- **Auto** explains the existing production resolver: low-rep reps/load work leans Strength, high-rep work
  leans Endurance, normal rep ranges retain Hypertrophy, and exercise modality/skill safety still takes
  precedence.
- **Hypertrophy, Strength, Power, Endurance, General fitness, Fat-loss support, and Skill** each have a short
  description of their existing V3.11B-MG policy. Power remains advisory without measured output;
  Fat-loss support does not claim measured fat loss; Skill remains form/control-first without technique data.
- A scope notice states that the selection changes future recommendations in Log Workout and Exercise Detail
  only. Workout plans, planned set/rep targets, logged sets, and workout history are not rewritten.
- A modality notice explains that skill work, timed holds, carries, and sleds continue to follow their own
  V3.11A/C.1 progression rules inside every program goal.

The user selects locally in the sheet and commits explicitly with `Use <goal>`; closing or swiping the sheet
cancels the pending choice. Saving still calls only `SettingsViewModel.setWorkoutGoal(...)`, which writes the
existing `workout_goal` preference through `UserPreferencesRepository`. Log Workout and Exercise Detail keep
observing that same `StateFlow` and resolving it through the same pure V3.11C context path. No persistence
owner, precedence rule, metric contract, coaching policy, progression formula, historical record, or export
format changed.

This V3.11D note originally left onboarding as weekday-only. ONBOARDING-V2 first reused this exact selector,
and ONBOARDING-V3 retains the same preference owner without creating a second goal source; legacy users still
bypass the new first-launch flow, AUTO remains backward compatible, and no selector state moved into Room.

Validation: **5 new deterministic GoalSelectorContentTest cases** cover every persisted selection exactly
once, AUTO/explicit separation, production AUTO explanation, bounded Power/Fat-loss/Skill claims, and the
history/modality scope copy. The full JVM suite is **339 tests, 0 failures**; all **152
ProgressiveOverloadEngineTest**, **14 WorkoutGoalSourceTest**, and **9 C.1 compatibility tests** remain green.
Debug and release APKs both build successfully.

Emulator QA on `Pixel_8` verified the full production path: selecting Endurance persisted
`workout_goal=endurance`, Settings showed `EXPLICIT GOAL`, and force-stop/relaunch retained it. The same saved
Plank history then showed the Endurance plateau policy `Build one metric` in both Exercise Detail and Log
Workout. Restoring AUTO persisted `workout_goal=auto`; after another process restart the same Plank returned
to AUTO's modality-aware `Build time` advice. The QA workout was discarded through the app, the draft store
was empty, the existing nine-workout history remained visible, and the app crash buffer was clean.

**Limitations:** the goal remains one device-local, user-global preference; it is not per-plan, not attached
to historical sessions, and not included in the current plan/history JSON backup. The selector explains
existing policies but does not add measured velocity, technique quality, body-composition outcomes, or new
goal formulas.

**Implemented later by ONBOARDING-V2 and retained by ONBOARDING-V3:** the first-launch goal step reuses this
exact selector/persistence contract. It introduces no new goal source and no Progressive Overload behavior.

### MUSCLE-0 — Muscle Heat Map contract audit (audit only, no implementation)

Audit **completed** for a **session-only Muscle Heat Map** shown on an after-save review/share screen — full
contract in [`docs/muscle0_heatmap_contract_audit.md`](docs/muscle0_heatmap_contract_audit.md). **No source,
schema, engine, UI, or asset changes were made.**

- **Agreed layer boundary:** **DB → raw anatomical exposure** (count from `logged_sets.canonicalExerciseKey`
  → catalog `primaryMuscles`/`secondaryMuscles` + `trackingType` + target/completed sets; skipped=0,
  partial=scaled, primary>secondary; custom = safe fallback). **Muscle Heat Engine (new, separate, pure) →
  training-quality interpretation** (`colorBand` from exposure, `status`/`qualityModifier` from quality —
  color = exposure, status = meaning, kept on separate axes). **UI → front/back body map.** The
  **Progressive Overload Engine stays untouched** (only optionally *read* for per-exercise quality).
- **Findings:** catalog muscles are a stable 24-value enum; `canonicalExerciseKey` cleanly links a set to
  its catalog muscles; **V1 needs no schema change** (all runtime-derivable). Only gap: session **duration**
  isn't persisted (compute at runtime or defer). A `MuscleGroup → body-region` map is a UI constant to add
  (must cover all 24 values incl. FULL_BODY/CORE/ADDUCTORS etc.).
- **Phases:** MUSCLE-1 DB raw exposure → MUSCLE-2 Muscle Heat Engine → MUSCLE-3 after-save review/body map UI
  → MUSCLE-4 7-day recovery map (deferred; needs a decay/volume/overlap model).
- **No implementation yet. Next task: MUSCLE-1** (DB raw session exposure, no schema).

### MUSCLE-1 — Raw muscle exposure + baseline Muscle Heat Engine (runtime foundation, no UI)

Implemented per the MUSCLE-0 contract — full report in
[`docs/muscle1_raw_exposure_heat_engine_result.md`](docs/muscle1_raw_exposure_heat_engine_result.md).
**No body-map UI/assets yet, no Room schema change (one read-only `getSessionWithSets` query), no
Progressive Overload Engine change, no catalog change.** All runtime-only (nothing stored/exported).

- **Layer A — raw exposure** (`domain/muscleheat/MuscleExposure.kt`): pure `MuscleExposureCalculator` —
  canonical key → catalog primary (×1.0) / secondary (×0.5) muscles → weighted set-equivalents; skipped = 0
  (still visible), partial = scaled by `completedSets/targetSets`, completed = full; per-muscle
  `rawIntensityScore` + `exerciseContributors` for explainability; custom exercises fall back safely to an
  unattributed bucket (no crash, no fake anatomy).
- **Layer B — Muscle Heat Engine V1** (`domain/muscleheat/MuscleHeatEngine.kt`): pure, deterministic,
  conservative. **`colorBand` = exposure only** (NONE/LIGHT_YELLOW/YELLOW_ORANGE/ORANGE_RED/DEEP_RED at
  0/2/4/7 set-equivalents); **`status` = meaning** (NONE/UNDERTRAINED/STIMULATED/PRODUCTIVE/FATIGUED/
  RECOVERY_RISK from exposure × RIR-based quality + grind share) — separate axes by contract. Full quality
  for REPS_WEIGHT/BODYWEIGHT_REPS, conservative for SKILL_FORM, limited for time/distance types; the
  `reps=0` sentinel is never read as failure.
- **Integration point:** `SessionMuscleHeatUseCase.forSession(sessionId)` computes the complete
  `SessionMuscleHeat` from a **saved session id** (read-only; ready for the after-save screen). Session-only —
  no 7-day recovery model (MUSCLE-4).
- **Verified:** 20 new pure-JVM `MuscleHeatEngineTest` cases (all green; PO engine 99 + all other suites
  unchanged); debug + signed release builds green.
- **Next: MUSCLE-3** — after-save review/body-map UI (`MuscleGroup → body-region` map + rendering), with an
  optional MUSCLE-2 threshold-calibration pass first.

### MUSCLE-2 — Body-region mapping + Muscle Heat UI contract (no body-map UI yet)

**Completed** — full report in
[`docs/muscle2_body_region_mapping_result.md`](docs/muscle2_body_region_mapping_result.md). Deterministic
mapping/UI-contract layer only: **no body-map screen, no image/vector assets, no Room schema change, no
Progressive Overload Engine change, no MuscleHeatEngine scoring change.** Runtime-only.

- **MuscleGroup count verified from code: 24.** Catalog (`catalog_fed_v1.json`, 577 entries) actually uses
  **16** of them — the **same 16 for primary and secondary** (ABS, ABDUCTORS, ADDUCTORS, BICEPS, CALVES,
  CHEST, FOREARMS, GLUTES, HAMSTRINGS, LATS, LOWER_BACK, MID_BACK, QUADS, SHOULDERS, TRAPS, TRICEPS); the
  8 unused (UPPER_CHEST, BACK, FRONT/SIDE/REAR_DELTS, OBLIQUES, CORE, FULL_BODY) are still fully mapped.
- **Body-region mapping** (`domain/muscleheat/MuscleBodyRegion.kt`): `MuscleBodyRegions.mappingFor` is an
  **exhaustive `when`** over all 24 muscles → 17 `BodyRegionId`s (each with `displayName` + FRONT/BACK/BOTH
  side) or a documented `SummaryOnly`. FULL_BODY = summary-only (never a fake region); CORE → ABS (overlap
  documented); BACK → LATS+MID_BACK broad fallback; the delt heads → SHOULDERS; FOREARMS/ADDUCTORS/ABDUCTORS
  are visible regions.
- **UI contract** (`domain/muscleheat/MuscleHeatUi.kt`): `MuscleHeatUiMapper.toUiModel(...)` →
  `MuscleHeatUiModel` (merged `MuscleHeatUiRegion`s + summary-only + unattributed). Merge = **max-intensity
  muscle dominant** for color/status, all contributors preserved; deterministic region order. Centralized
  `MuscleHeatColorToken` per band (**no hardcoded colors** — composables resolve later).
- **Verified:** 16 new pure-JVM tests (all green; MUSCLE-1's 20 + all other suites unchanged); debug build
  green.
- **Next: MUSCLE-3** — after-save review/body-map UI (theme colors per token + silhouette rendering).

### MUSCLE-3 — After-save Muscle Heat Review UI + native body map (completed)

Full report in [`docs/muscle3_after_save_heat_review_ui_result.md`](docs/muscle3_after_save_heat_review_ui_result.md).
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change, no
WebView.**

- **Body map renderer:** native Compose `Canvas` (`ui/muscleheat/BodyMap.kt`) drawing SVG path data
  ported from **react-muscle-highlighter (MIT)** — attribution preserved in
  [`docs/third_party_react_muscle_highlighter.md`](docs/third_party_react_muscle_highlighter.md) and the
  verbatim license at `app/src/main/assets/licenses/react_muscle_highlighter_LICENSE.txt`. Path data is
  script-generated into `BodyMapPaths.kt` (35 slugs, male front/back). Colors resolve only via the
  centralized `MuscleHeatPalette` per `MuscleHeatColorToken`. Known gaps (documented): no abductor shape;
  one shared `upper-back` shape for LATS/MID_BACK (max color wins); male figure only.
- **Muscle Heat Review screen** (`ui/review/`): mockup-style — header (back, title, time/date—duration),
  heat card (Duration when known, Top Muscles, front/back body maps, "not on the map" footnotes for
  custom/summary-only), the **existing exercise/set breakdown below** (reused History `ExerciseBlock`),
  circular Share/Edit actions (wired in MUSCLE-3C). Heat failure degrades gracefully — the breakdown
  always renders.
- **Wired on three surfaces:** automatically **after save** (with a runtime-only logging-duration; save
  behavior otherwise preserved — History underneath, review on top), from **History** session rows, and
  from the **Home Weekly-hub** session rows (purple body icon).
- **Verified:** 8 new pure-JVM contract tests (all suites green); debug build green; **on-device QA
  passed** (Pixel_8): full log→save→review flow, correct region coloring from a real Plank+Sled session
  (ABS/QUADS top muscles), custom exercises safely unpainted, History/back intact, app crash buffer clean.
- **Next:** MUSCLE-3B polish, MUSCLE-3C share/rename, then MUSCLE-4 (7-day recovery map).

### MUSCLE-3B — Heat review UI polish + body-map accuracy fix (completed)

Full report in [`docs/muscle3b_heat_review_ui_polish_result.md`](docs/muscle3b_heat_review_ui_polish_result.md).
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change.**

- **Body-map fix:** the broken back "patch" was caused by drawing only solid fills (no outline / no
  per-shape strokes). The renderer now draws the upstream **silhouette outline first**, then each upstream
  shape with a **fill + thin dark separator stroke** — restoring muscle definition and the silhouette. The
  outline stroke paths were ported from upstream `SvgMaleWrapper.tsx` into `BodyMapPaths.kt`.
- **Slug accuracy audit:** every generated slug verified to exist in upstream and to color the exact
  upstream anatomical shape (the app only chooses *which* slug gets color; the asset defines *where*).
  16 catalog-used regions map to exact shapes; LATS/MID_BACK share upstream `upper-back` (max color wins);
  **ABDUCTORS** has no upstream shape (list-only, documented). Full table in the report.
- **UI polish (to the mockup):** purple top gradient, near-OLED black card, prominent uppercase Duration,
  uppercase Top Muscles, front/back maps, **AURON** wordmark at the card bottom (no SLOPAX, no Calories),
  white circular Share/Edit under the card; exercise/set breakdown preserved below.
- **History / Weekly behavior:** removed the extra body/person icon — **tapping a saved session row opens
  the Muscle Heat Review directly**; the chevron still expands/collapses the inline breakdown.
- **Verified:** unit tests (incl. new upstream-slug-inventory + outline coordinate-space tests) green;
  debug build green; **on-device QA passed** (Pixel_8) — direct-open from History/Weekly, chevron expands
  inline, no icon, outlined maps with the back patch gone, breakdown intact, app crash buffer clean.

### MUSCLE-3B.1 — Back-map artifact root cause + persisted session duration (completed)

Full report in [`docs/muscle3b1_heat_review_duration_backmap_fix_result.md`](docs/muscle3b1_heat_review_duration_backmap_fix_result.md).

- **Back-map artifact — root cause PROVEN:** Compose's `PathParser` mis-tokenizes packed SVG arc flags
  (`a910.025 910.025 0 0136.94-.25` — flags must read `0`,`1`,`36.94`); the 910-radius arcs in the back
  `head` path drew the displaced blob. Verified by failing `toNodes()` equality tests, fixed with
  `normalizeSvgArcFlags` (arc-aware tokenizer applied before parsing; identical upstream geometry, no
  anatomy reshaping), re-verified on device — blob gone. +5 parser tests incl. a full-inventory parse net.
- **Persisted session duration (Room v6 → v7, additive only):** `workout_sessions.durationSeconds`
  (wall-clock Log-open → save) persisted at save; the review prefers the stored value, so the after-save
  and History/Home-opened reviews show the SAME duration — proven on device across `am force-stop`
  (process death). Old sessions show no duration (NULL). Export adds the optional field (JSON
  schemaVersion 4); import stays tolerant. **Migration v6→v7 verified on-device over real data** —
  plan/history fully preserved. Schema `7.json` exported.
- **OLED polish:** the review background now fades purple → **true #000000**; card unchanged near-OLED.
- **No Progressive Overload Engine change; no destructive migration; tests + debug build green.**

### MUSCLE-3C — Share Heat Card + Rename Session (completed)

Full report in [`docs/muscle3c_share_rename_result.md`](docs/muscle3c_share_rename_result.md).

- **Share Heat Card:** the circular Share button now renders a standalone 1080 px PNG of the heat card
  content using the same body-map path data and heat-region mapping as the review UI. The export has
  transparent rounded corners, near-OLED card fill, the current heat maps, and the AURON wordmark, with
  no live-screen crop or leaked header/buttons/bottom UI. The PNG is written under app cache
  (`shared_heat_cards`), exposed through `FileProvider`, and shared via Android `ACTION_SEND` with a
  read-grant URI; no internet or raw app-private file paths.
- **Rename Session:** the circular Edit button opens a rename dialog and persists the saved-session title
  through DAO/repository/ViewModel (`workout_sessions.customTitle`), not UI-only state. Review, History,
  and the Home Weekly Progress Hub use the same `sessionDisplayTitle(...)` fallback, so renamed sessions
  show the custom title while old sessions keep `dayNameSnapshot`.
- **Schema / export:** Room `v7 → v8` adds nullable `customTitle TEXT` to `workout_sessions` via additive
  `ALTER TABLE`; schema `8.json` is exported. Export/import is now JSON `schemaVersion 5`, includes
  `customTitle`, and remains tolerant of older exports where the field is absent.
- **No Progressive Overload Engine change, no MuscleHeatEngine scoring change, no catalog/assets/import
  change, no signing-secret change, and no female figure / per-region tap / MUSCLE-4 work.** Unit tests,
  debug build, and emulator QA passed.

### MUSCLE-HEAT-1 — Stimulus-quality color calibration + MID_BACK trap overlay (completed)

Full report in [`docs/muscle_heat1_stimulus_quality_color_result.md`](docs/muscle_heat1_stimulus_quality_color_result.md).

- **RIR now affects the final heat color:** `finalIntensityScore = rawIntensityScore × qualityModifier`
  and the color band reads that stimulus score — identical raw work at RIR 4–5 renders visibly cooler
  than productive RIR 1–2 work, and easy completed sets can no longer reach deep red on volume alone.
- **Raw exposure and final stimulus stay separate:** Layer A `MuscleExposure.rawIntensityScore`
  (anatomical work) is untouched; `status`/`reason` keep their own raw+quality axis (junk volume can be
  FATIGUED while mildly hot; grinding reads RECOVERY_RISK without a productive-hot color).
- **MID_BACK now overlays TRAPS** on the back map (the upstream sheet splits traps from the upper-back
  mass): one MID_BACK heat result paints both regions — rendering-only, no exposure/contributor change.
- **No Progressive Overload Engine mutation** — future PO metadata (CoachInsight, PlateauStrategy,
  SessionQuality, Readiness, WorkoutContext) documented as a pass-in-only TODO contract.
- Tests updated/added (muscle suites: 23 engine + 17 mapping + 5 parser + 10 contract) — all green;
  debug build green.

### HISTORY-2 — Workout History archive redesign + direct review navigation (completed)

Full report in [`docs/history2_workout_archive_redesign_result.md`](docs/history2_workout_archive_redesign_result.md).
**No Room schema change, no Progressive Overload Engine change, no MuscleHeatEngine scoring change.**

- **History is now a month-based archive** (supersedes the old week-collapse UI): big "Workout History"
  title, a **swipeable year/month selector** (adjacent months peek at the edges and track the drag;
  tap a peek to jump), months rendered newest → oldest including empty months; empty months show the
  imported thinking illustration (`res/drawable/empty_history.webp`) with the mockup caption.
- **Each workout is a clean list item** — bold day title, uppercase time/full-date/duration subtitle
  (persisted wall duration), and a compact table (rounded WORKOUT/VOLUME/REPS header row, tight rows,
  PR trophies, no per-exercise cards, no expand/collapse). **Time/distance exercises show real
  duration/distance values, never fake reps/kg.**
- **Tapping any workout (History or the Home Weekly-hub) opens the Muscle Heat Review directly** —
  no chevrons/expansion anywhere; the review keeps the heat card first with the same table style below.
- **Global OLED polish:** the shared screen background now fades from a hollow-purple top into true
  `#000000` across Home/Plan/History/Settings (+ Detail/Catalog/Onboarding) in dark theme.
- Verified on device: month swipe, compact tables, direct-open review (heat card + breakdown intact,
  persisted duration shown from History/Home), hub behavior, crash buffer clean. Tests + debug build green.

### WEARABLE-0 - Recovery validation + shadow-mode contract (completed)

Contract: [`docs/wearable0_recovery_validation_shadow_mode_contract.md`](docs/wearable0_recovery_validation_shadow_mode_contract.md).
MUSCLE-4B remains frozen. The validation/shadow-mode contract is complete; Health Connect is not
implemented and wearable data has no effect on readiness or advice. **Next:** WEARABLE-1A - Recovery
Research Foundation + Frozen Baseline/Outcome Capture.

### WEARABLE-1A - Recovery Research Foundation (completed)

Full evidence: [`docs/wearable1a_recovery_research_foundation_result.md`](docs/wearable1a_recovery_research_foundation_result.md).
Room is now **v9** through the additive, migration-tested `8 -> 9` path. Eight independent local
research tables freeze one versioned MUSCLE-4B prediction per durable workout-attempt identity before
the first valid working-set observation, preserve its cutoff/input digest/plan and exercise snapshots,
and attach later completed, partial or explicitly abandoned outcomes without rewriting the prediction.

Research capture is **off by default** and requires an explicit local opt-in in Settings. Its canonical
JSON export is a separate, user-invoked SAF action; AURON's Settings plan/history backup/import neither
contains nor imports research records. There is still **no Health Connect, wearable permission/data/entity, candidate
readiness, AI/ML, backend or automatic upload**. Instrumentation proved exact prediction identity/hash
survive database and repository recreation; Pixel 8 runtime QA additionally proved the same identity/hash
across a real force-stop/relaunch before any set. Post-set completion/partial/abandon persistence was
verified at instrumentation level, not claimed as real process-kill QA.

Protected SHA-256 values remained byte-identical: MUSCLE-4B
`0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748`, Muscle Heat
`12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341`, and Progressive Overload
`8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B`.
**Next task: WEARABLE-1B only.**

### WEARABLE-1B - Frozen baseline prospective evaluator (completed)

Full evidence: [`docs/wearable1b_frozen_baseline_prospective_evaluation_result.md`](docs/wearable1b_frozen_baseline_prospective_evaluation_result.md).
The standalone `tools/recovery-evaluation/` CLI now audits immutable WEARABLE-1A JSON exports and, only
after conservative sample/event gates pass, benchmarks six same-row comparators with expanding
rolling-origin workout blocks. `PO_PLUS_MUSCLE_4B` differs from `PO_HISTORY_ONLY` only by frozen
exercise readiness; preprocessing/probability mapping use historical folds only, every training
outcome must exist before the test cutoff, and one workout is never split. Readiness remains an
ordinal/model feature, never `readiness / 100` probability.

Protocol `WEARABLE_1B_EVALUATION_PROTOCOL_V1` was frozen before real-outcome search at SHA-256
`494ac18b8a92114091464ab1731776def680af40fa26e2cb27a4742748ea6998`. Fifty evaluator tests, four
required synthetic conclusion runs, byte-identical rerun proof, `:app:testDebugUnitTest`, and
`:app:assembleDebug` are green. No safely available consented WEARABLE-1A JSON export exists in this
checkout, so the honest real-data status is **`INSUFFICIENT_DATA` (no evaluator input/run)**; synthetic
`BASELINE_SUPPORTED` is test evidence only and is not a MUSCLE-4B validation claim.

MUSCLE-4B, Muscle Heat, Progressive Overload, WEARABLE-1A capture, Room, Android UI, Health Connect,
and wearable behavior are unchanged; no modifier is active. **Next:** continue consented WEARABLE-1A
prospective capture, export canonical JSON, run WEARABLE-1B `audit`, and only then consider the full
evaluation/data-collection gate. Do not start Health Connect from synthetic results.

### WEARABLE-1C - Real prospective capture activation audit (`READY_FOR_REAL_CAPTURE`)

Full evidence: [`docs/wearable1c_real_capture_activation_audit_result.md`](docs/wearable1c_real_capture_activation_audit_result.md).
No naturally occurring real research record or canonical real export was available, so no real-data audit
and no MUSCLE-4B validation claim was made. A disposable Pixel_8 QA database verified consent-off capture,
prospective/idempotent freeze, force-stop restoration, first-valid-set plus production Save linkage,
confirmed Discard, user-created SAF export, DB/export equality, and byte-identical evaluator reruns.

The only implementation repair adds predeclared versions and capture aggregates to WEARABLE-1B audit
reports, with one focused regression test; protocol V1, Room v9, Android production code, MUSCLE-4B,
Muscle Heat, and Progressive Overload remain byte-for-byte unchanged. Python tests are 51/51, Android unit
tests are 348/348, connected tests are 2/2, debug build is green, and independent review returned
`PASS_WITH_LIMITATIONS`.

Historical privacy qualification: at WEARABLE-1C completion, “ordinary backup” meant AURON's explicit
Settings JSON backup and Android framework Auto Backup/D2D had not yet been excluded. That gap is now
superseded by WEARABLE-1C.1 below. No Health Connect or wearable modifier is active.

### WEARABLE-1C.1 - Research backup privacy boundary hardening (`PARTIAL_VERIFICATION_ENVIRONMENT_BLOCKED`)

Full evidence: [`docs/wearable1c1_research_backup_privacy_hardening_result.md`](docs/wearable1c1_research_backup_privacy_hardening_result.md).
Android framework automatic cloud backup is disabled by `allowBackup=false`; legacy and modern rules
also exclude all nine supported credential/device-protected domains, with separate modern cloud and
device-transfer sections. API 37 runtime QA independently triggered LocalTransport and official GMS D2D,
then uninstalled/reinstalled the same APK: no plan/history, research/consent, settings, draft or internal
user state returned. API 30 runtime could not be exercised because no legacy image/device is installed,
so this is deliberately not labeled complete legacy verification. Proprietary out-of-framework OEM
migration remains outside a universal guarantee.

Automatic restoration of plans/history/settings/research is intentionally unavailable. Explicit Settings
Plan/History JSON export/import and separate user-invoked Research SAF export remain operational; the chosen
document provider controls the Research file destination. Room remains v9, and MUSCLE-4B, Muscle Heat,
Progressive Overload, the Research contract/export, Protocol V1 and AppDatabase remain byte-identical.
**Next:** natural consented prospective capture only; rerun the API 30 privacy test if an approved image/device
becomes available. Do not start Health Connect or a wearable modifier from this task.

### WEARABLE-1D - First Real Audit (`FIRST_REAL_AUDIT_COMPLETE_WITH_LIMITATIONS`)

Full evidence: [`docs/wearable1d_first_real_audit_result.md`](docs/wearable1d_first_real_audit_result.md).
The preserved, user-attested physical-phone canonical export reached Checkpoint B with 10 natural saved
terminal workouts (`COMPLETED` 2, `PARTIAL` 8), plus three abandoned attempts and one `ACTIVE` attempt.
All eight collections and frozen versions are supported; payload hashes, timestamps/cutoffs and lineage
pass with zero critical findings. Two WEARABLE-1B audit runs produced byte-identical hashes for all five
required artifacts, and independent review returned `PASS_WITH_LIMITATIONS`.

Capture quality is `CAPTURE_QUALITY_ACCEPTABLE_WITH_LIMITATIONS`: the two early late-entry workouts remain
temporally uncertain for performance validation, while eight later workouts have operationally prospective
recorded chains supported by contemporaneous-logging attestation. The evaluator still returns the expected
`INSUFFICIENT_DATA`; this is not biological validation, and no wearable benefit or modifier has been
demonstrated. Raw evidence remains outside the repository, and physical Room completeness was not
independently runtime-cross-checked.

`WEARABLE_2A_GATE = OPEN` for a separate **read-only Health Connect shadow-context** task only. MUSCLE-4B,
Muscle Heat, Progressive Overload, Room/export semantics and WEARABLE-1B Protocol V1 remain frozen; no
production readiness change or wearable modifier is authorized.

### WEARABLE-2A - Read-only Health Connect shadow context (`REPAIR_IMPLEMENTED_AWAITING_PHYSICAL_VERIFICATION`)

`WEARABLE_2A_SHADOW_CONTEXT_V1` reads only Health Connect Sleep sessions/stages, HRV RMSSD and Resting
HR after explicit Recovery Research consent and an explicit Connect action. It preserves source,
metadata, measurement/session time, source last-modified time and AURON first-seen time; normalizes
missing/overlapping sleep conservatively; syncs only in foreground or on explicit refresh; and stores
immutable snapshot lineage at a frozen prediction cutoff. Room moves additively from v9 to v10.

The physical split-sleep audit found that frozen V1's longest-first rule could retain a prior day's
longer session as primary. The versioned repair uses `WEARABLE_2A_SHADOW_CONTEXT_V1_1` /
`WEARABLE_2A_SLEEP_SELECTION_V2`: the latest ending eligible session wins; duration, origin package,
Health Connect ID and revision ID are tie-breakers only. The 36-hour window is unchanged, raw sessions
are never merged, all eligible sessions stay linked in a snapshot, and Home explicitly labels the
displayed value as selected-session data while disclosing other eligible sessions.

Home displays a clearly labelled `Shadow context · not used in readiness` card. Settings provides
Connect, Refresh, Manage, Disconnect/revoke and local deletion. Automatic backup remains disabled,
health values are never logged/networked, and frozen Research JSON V1 is not extended.

Repair evidence: 431 JVM tests pass with zero failures/errors/skips; androidTest compilation,
`lintVitalRelease`, debug and signed release assembly pass. Full `lintRelease` found no issue in the
repair files but remains non-green on one unrelated existing `SuspiciousIndentation` error in
`ExerciseDetailViewModel.kt:92`. Current emulator execution is pending USB-debug authorization. Real
Samsung Sleep sync/provenance is confirmed, but the `1.1.1 (3)` repair and first natural snapshot still
require physical verification. Therefore `WEARABLE_2B_GATE = CLOSED`. Full contract and handoff:
[`docs/WEARABLE_2A_SHADOW_CONTEXT_V1.md`](docs/WEARABLE_2A_SHADOW_CONTEXT_V1.md); result:
[`docs/wearable2a_read_only_health_connect_shadow_context_result.md`](docs/wearable2a_read_only_health_connect_shadow_context_result.md).

### MUSCLE-4C.2 - Recovery bar animation real visibility fix (completed)

Full report in [`docs/muscle4c2_recovery_animation_real_visibility_fix_result.md`](docs/muscle4c2_recovery_animation_real_visibility_fix_result.md).
**UI animation only - no recovery engine/scoring/value/status/color semantic change, no Room schema
change, no HomeViewModel change, no BodyMap rendering change, no Progressive Overload Engine change.**

- Corrects the MUSCLE-4C.1 runtime gap: the readiness bars no longer render at target width before the
  user sees them. Bars stay at `0f` until the first actual bar track is visible above the bottom dock,
  then a one-shot `Animatable` entrance fills 0 -> target.
- Runtime proof on `emulator-5554`: debug `Animatable` log for the Legs bar showed `0.0 -> 0.0237 ->
  0.1475 -> 0.5874 -> 0.9995`; screenshot evidence captured pre-bar, low-fill, and final states.
  Scroll away/back and Home -> Plan -> Home did not replay (empty animation logs); crash buffer clean.

### MUSCLE-4B - Training Readiness upgrade + Home recovery UI redesign (completed)

Full report in [`docs/muscle4b_recovery_intelligence_ui_redesign_result.md`](docs/muscle4b_recovery_intelligence_ui_redesign_result.md).
**Runtime-only — no schema/migration, no Progressive Overload Engine mutation, no MuscleHeat scoring change.**

- **Readiness model upgraded** from MUSCLE-4's linear recovery timer to a **bi-exponential
  fitness-fatigue cost** (Banister/Busso impulse-response tradition): each session's quality-weighted
  stimulus splits into a fast-clearing **acute strain** term and a slower, **delayed structural**
  (DOMS-like) term, both size-profiled (large compounds recover slowest). Adds **exercise recovery
  cost** from existing catalog taxonomy (compound+hinge+barbell ≫ machine isolation — the review's
  `exerciseDamageFactor`, no new schema/values), a **delayed-soreness window** flag, grind slower-decay,
  stacking, coarse group roll-ups, overall readiness, and **neglected-muscle nudges** ("You haven't
  trained Forearms recently."). New stages: Ready / Primed / Recovering / Sore / Needs rest.
- **Home recovery UI redesigned** into a premium OLED **readiness dashboard** (inspired by the provided
  body-strain references, phone-native): hero readiness score + stage, front/back body map on a
  near-OLED stage (recovery colors — separate from session heat), coarse **group tiles**
  (Legs/Back/Chest/Shoulders/Arms/Core) with thin colored bars + %, intelligence nudges, and a recovery
  legend. Near-black surfaces with thin edges, no gray slabs.
- Recovery and session-heat semantics stay strictly distinct (verified on device: Home = fatigue debt,
  History→Review = session stimulus). 19 pure-JVM recovery tests + all suites green; debug build green;
  on-device QA passed (crash buffer clean). **Next:** MUSCLE-4C — per-muscle recovery detail tap-through.

### MUSCLE-4 — Home Muscle Recovery Map (completed)

Full report in [`docs/muscle4_home_recovery_map_result.md`](docs/muscle4_home_recovery_map_result.md).
**Runtime-only — no Room schema change or migration, no Progressive Overload Engine change, no
MuscleHeatEngine scoring change.**

- **Home now shows a "Muscle Recovery" card** (between Your Progress and Daily Boost): front/back body
  maps painted with **recovery colors** (red/orange = fatigue still carried; neutral figure = recovered
  — a separate token system from session heat, never mixed), the top recovery-limited muscles with
  plain-language reasons ("Mid back · Trained 20h ago — heavy fatigue, give it more time."), a green
  ready line, an optional "Today's plan hits X — still recovering." warning, and a friendly empty state.
- **Recovery model (deterministic, pure, JVM-tested):** last-7-days sessions → the existing
  quality-weighted session stimulus (raw exposure × RIR quality) seeds per-muscle fatigue → linear decay
  over **size-based windows** (large 72 h / medium 48 h / small 36 h), **×1.35 slower for
  grind/RECOVERY_RISK work**, residuals **stack** across repeated hits; per-muscle
  READY/PRIMED/WORKED/SORE/RISK status + 0–100 recovery score + contributors + reason. Easy RIR-5 work
  seeds little debt; time/distance/skill stay conservative; custom exercises attribute nothing safely.
- Engine/pipeline are pure (`domain/muscleheat/MuscleRecovery.kt`); the body-map renderer gained a
  color-agnostic core (`BodyMapFigureCustom`) so heat and recovery drive the same figure without mixing
  semantics. 13 new pure-JVM tests; tests + debug build green; **on-device QA passed** (recent back
  session red, older chest/abs visibly decayed to "almost ready", History/review flows intact, crash
  buffer clean).

### PLAN-1 — Default plan normalized to catalog names

The default seeded split (`data/SeedData.kt → SeedData.defaultSplit`, used only by
`GymRepository.ensureSeedData()` when the plan table is empty) was written before the catalog existed,
so several names were vague ("Leg press or squat") and didn't match any catalog entry. PLAN-1 renames
them to **exact catalog names** so V3.5 catalog lookup succeeds. **Day structure, exercise order, and
target sets/reps are unchanged**; only `SeedData.kt` changed (no engine, Room schema, save/draft,
History, or Import/Export change; `CatalogSeed` untouched). Existing user data is not migrated — this
only affects the starter plan a fresh install seeds.

Rename mapping applied (old → final catalog name):

- **Push:** Plate-loaded incline chest press machine → **Incline Machine Chest Press**; Machine chest
  press → **Seated Machine Chest Press**; Pec fly → **Pec Deck Machine Fly**; Lateral raise →
  **Dumbbell Seated Lateral Raise**; Face pull → **Cable Face Pull**; Reverse-grip straight-bar cable
  pushdown → **Reverse-Grip Triceps Pushdown**. *("Push-up optional" left as-is.)*
- **Pull:** Pull-up → **Wide-Grip Pull-Up**; Lat pulldown → **Wide-Grip Lat Pulldown**; Seated row →
  **Seated Cable Row**; Straight-arm pulldown → **Straight-Arm Cable Pulldown**; Hammer curl →
  **Dumbbell Hammer Curl**. *("Incline dumbbell curl" → "Incline Dumbbell Curl" (already a catalog
  name; cased to match); "EZ curl optional" left as-is.)*
- **Legs:** Leg press or squat → **Leg Press**; Romanian deadlift or leg curl → **Seated Leg Curl**;
  Leg extension → **Leg Extension**; Hamstring curl → **Lying Leg Curl**; Calf raise →
  **Standing Calf Raise**.
- **Full Body:** Machine chest press → **Seated Machine Chest Press**; Lat pulldown or light row →
  **Wide-Grip Lat Pulldown**; Leg press or hack squat → **Leg Press**; Curl machine or rope pushdown →
  **Triceps Pushdown**; Cable crunch or sit-up → **Cable Crunch**.

**Catalog mismatches & chosen fallbacks** (the task's suggested name did not exist in the catalog, so
the closest exact catalog name was used and is reported here):

- "Incline Chest Press Machine" → not in catalog → used **Incline Machine Chest Press**.
- "Pec Dec Fly" → not in catalog → used **Pec Deck Machine Fly**.
- "Cable Reverse-Grip Pushdown" → not in catalog → used **Reverse-Grip Triceps Pushdown**.
- "Cable Rope Pushdown" (Full Body) → not in catalog → fell back to **Triceps Pushdown** (per task).

**Known limitations:** "Push-up optional" and "EZ curl optional" are intentionally left un-normalized
(not in the provided mapping; no clean catalog match) — they still progress correctly via the engine's
name-inference fallback. All 19 normalized names resolve to an exact catalog name (verified).

### Motion Polish pass (UI/motion only)

Lightweight premium motion added with small per-element animations (`animate*AsState`, `Crossfade`, `AnimatedVisibility`) — no Room/repository/overload/draft changes, no root-level Crossfade, no full-screen transitions, no infinite or per-row list animations.

- **Home workout selector** (`HomeScreen.DayChipRow`): a single primary pill **slides** behind the selected day chip. Each chip's position/size is measured (`onGloballyPositioned`) and the pill's x/width animate (`animateDpAsState`, 260 ms). The selected chip's own background fades to transparent so the pill shows through; text colour cross-fades. Works for any number of DB workout days, and **Skip / Next** drives the same slide.
- **Bottom navigation** (`GymTrackerApp.GymBottomBar`): replaced `NavigationBar` with a custom bar where one rounded pill **slides** between Home/Plan/History/Settings (equal-width items → `itemWidth × animatedIndex`, 280 ms, no per-item measurement). Keeps `navigationBarsPadding()` safe-area, full-slot hitboxes, icon+label, and lives in the Scaffold `bottomBar` slot so content insets are unchanged.
- **Training day selection** (`WeekdayPicker`, used by onboarding **and** Settings → Edit training days): colour cross-fade + a small spring scale on the row and a scale-bounce/`Crossfade` on the check icon.
- **Weekly Progress Hub** (`WeeklyProgressHubSheet`): explicit chevron rotation + `expandVertically`/`shrinkVertically` + fade on session expand/collapse (calendar and recent-sessions list unchanged; details still collapsed by default).
- **Hero text** (`HomeScreen.HeroWorkoutCard`): subtle `Crossfade` on just the hero label + title when the selected workout changes.

QA on the Pixel 8 emulator (light **and** dark) on June 8, 2026, built with the known-good JDK 17 at `D:\jdk17\jdk-17.0.19+10` (the bundled Android Studio JBR was corrupted — `jvm.cfg` missing):

1. Workout selector pill slides Push → Legs (manual select); Skip slides too; hero title cross-fades.
2. Bottom-nav pill slides Home → Plan → History → Settings; hitboxes and safe-area intact.
3. Edit-training-days rows animate on toggle (count validation still gates Save).
4. Weekly Hub opens, session expands/collapses with chevron rotation, dismiss works; calendar + recent sessions intact.
5. History weekly collapsed groups, Plan, theme switch (light↔dark), Log Workout accordion + sticky Save bar all still render. Clean `adb logcat -b crash`, no FATAL.

Outputs: debug `app/build/outputs/apk/debug/app-debug.apk`; signed release `app/build/outputs/apk/release/app-release.apk` (`:app:assembleRelease`, keystore untouched).

#### Motion patch 2 (follow-up)

- **Bottom nav → floating dock** (`GymTrackerApp.GymBottomBar`): the dock is no longer glued to the bottom — it's a rounded `Surface` (30 dp) with `shadowElevation` and side/top/bottom margins (`navigationBarsPadding()` preserved), so the page background shows around it and it visually floats. Hitboxes stay inside the dock; the existing sliding tab indicator is unchanged.
- **Settings theme control** (`SettingsScreen.SegmentedThemeControl`): now uses the same sliding-pill motion as the other selectors (equal-width segments, pill x animates to the selected index; content colour cross-fades).
- **Plan day cards** (`PlanScreen.DayCard`): expanded content wrapped in `AnimatedVisibility` (`expandVertically/shrinkVertically + fade`); chevron rotation already present; outer `spacedBy` removed so a collapsed card has no leftover gap.

Verified on Pixel 8 (light + dark) June 9, 2026: floating dock slides Home→Plan→Settings with safe-area + margins; theme pill slides Dark→System and the theme switches; Plan cards expand/collapse with chevron rotation and no collapsed gap; clean `adb logcat -b crash`, no FATAL. Built with `D:\jdk17\jdk-17.0.19+10` (Android Studio JBR still corrupted). Outputs unchanged (debug + signed release APK).

#### Motion patch 3 (follow-up)

- **Floating dock is now a true overlay** (`GymTrackerApp`): removed the `Scaffold` `bottomBar` slot (it reserved a full-width rectangle that blocked content like Settings → Import). The dock is rendered in a root `Box` overlay aligned `BottomCenter`, only on the four top-level routes (hidden on Log Workout / Exercise Detail so their own bottom bars are never overlapped). `Scaffold` is kept solely for system-bar insets. Scrollable top-level screens add `BottomNavSpace` (96dp) bottom content-padding so the last item scrolls clear of the dock, while content still renders/scrolls *under* the floating dock. The dock's transparent side margins are not clickable (taps fall through to content).
- **Day-streak flame** (`HomeScreen.StreakFlame` + `StatCard.iconContent` slot): the flame fills when a workout was just saved and stays lit; it resets when the user skips / picks another day. State is derived purely from the existing `DaySelectionOrigin` (`ADVANCED` after the post-save auto-advance → active; `MANUAL`/`AUTO` → inactive) — no schema, no logic change, no new persisted state. Active = filled two-tone (outer gold `#FFC107`, inner orange `#FF6D00`) with a one-shot spring "burst" (no looping); inactive = the muted outlined flame (old look).

Verified on Pixel 8: Settings → Import (field + button) scrolls fully clear of the dock; Home/Plan/History last items clear it; nav + hitboxes work. Save a Pull-up workout → streak flame fills two-tone (origin `advanced`, streak `1`) and stays lit; Skip → flame resets to the outlined icon (origin `manual`). Clean `adb logcat -b crash`, no FATAL. Outputs: debug `app/build/outputs/apk/debug/app-debug.apk`, signed release `app/build/outputs/apk/release/app-release.apk` (keystore untouched).

### Daily Boost MVP (Task B)

A lightweight, **optional ~2-minute habit for rest / low-energy days** — not a workout. Daily Boost is
deliberately isolated from the training pipeline: it **never** writes to Room, the Weekly Progress ring,
training-day completion, the workout Day streak, History, or the post-save auto-advance. **No Room schema
change** — it lives entirely in `SharedPreferences`.

- **Home card** (`ui/home/DailyBoostSection.kt → DailyBoostCard`): sits **below "Your progress" and above
  the selected workout's exercise list**. Shows the selected boost exercise, a short muted how-to, a green
  **"Done today"** badge when completed, an optional **"Boost streak: N day(s)"** line, and **Start /
  Random / Choose** actions. Reuses the Task A subtle border and matches Light/Dark.
- **Catalog** (`domain/DailyBoost.kt → DailyBoostExercise`): a small fixed list — Push-up, **Dead hang**
  (the gentle default pulling option instead of pull-up), Plank, Bodyweight squat, Hollow hold, Mobility
  stretch. Default selection = Push-up. Each entry has a stable `key` (the only thing persisted), display
  name, and description.
- **Random** picks a *different* catalog exercise and persists it; **Choose** opens a `ModalBottomSheet`
  list (selected row highlighted) and persists the pick.
- **Timer modal** (`DailyBoostTimerDialog`, full-screen `Dialog`): exercise name + muted description on top,
  a large centred **2:00** countdown inside a circular progress arc with a small dot tracking the leading
  edge, and **Pause/Resume · Cancel · Done**. The countdown is driven by a single per-frame `elapsedMs`
  read **only inside the Canvas draw lambda and a `derivedStateOf`**, so the arc redraws at 60fps in the
  draw phase while the text recomposes ~once/second — no Home scroll impact, no per-frame recomposition of
  the screen. Subtle entrance fade/scale via `graphicsLayer`. **Cancel / ✕ / back never mark completion**;
  reaching **0:00** marks it complete, and **Done** asks a quick "Finish boost early?" confirm before
  marking. (The ring + controls are centred as one group so the buttons are never clipped by the dialog
  window's insets.)
- **Persistence** (`UserPreferencesRepository`): `daily_boost_exercise`,
  `daily_boost_last_completion_epoch_day`, `daily_boost_streak`. Completion is idempotent per day; the
  separate **boost streak** increments on consecutive days and shows a one-day grace (mirrors the workout
  streak rule) — completely independent of the workout Day streak. Exposed to Home via a **separate**
  `HomeViewModel.dailyBoost` StateFlow that shares nothing with `uiState`/`weeklyHub`.

Verified on Pixel 8 API 37 on June 10, 2026 (Light **and** Dark):

1. Card appears below Your Progress / above the workout list, premium in both themes (Dark not muddy).
2. **Random** changes the selected exercise and persists (`push_up → bodyweight_squat → dead_hang`).
3. **Choose** opens the sheet and persists the pick (`plank`, `dead_hang`).
4. **Start** opens the timer; **2:00** counts down; **Pause** freezes the value; **Resume** continues.
5. **Cancel** returns to Home and writes **no** completion key (clean prefs).
6. Completing (0:00 **or** Done → Finish) sets `daily_boost_last_completion_epoch_day` = today and
   `daily_boost_streak` = 1; Home shows **Done today** + **Boost streak: 1 day**.
7. Force-stop/relaunch: selected exercise (`dead_hang`) **and** completed-today state persist.
8. **Weekly Progress ring (2/4) and workout Day streak (2) are unchanged** by Daily Boost; History still
   shows 3 workouts this week (no boost entry). Quick Start opens Log Workout, History opens. Clean
   `adb logcat -b crash`, no FATAL. Debug QA build: `app/build/outputs/apk/debug/app-debug.apk`.

### Daily Boost activity streak + UI polish (Task C-1)

Two refinements to Daily Boost: the streak model and the visuals.

- **Activity streak (purpose fix).** The old Daily Boost had its **own** separate boost streak, which
  drifted from the intent (keep consistency alive on rest days). It is gone. The Home streak card —
  relabelled **"Active streak"** — now counts a day as active if it has **a workout OR a Daily Boost**.
  `HomeViewModel` computes it as `computeStreak(workoutDays ∪ boostDays)`; the flame lights when today
  is active by either path. **Weekly Progress, History, PRs, volume and session counts stay workout-only**
  — a boost never creates a session or touches those. No Room change: boost completion days live in
  SharedPreferences as `daily_boost_completion_days` (a pruned CSV), migrated once from the legacy
  `last_completion` + `streak` keys. Verified live: adding a boost on a gap day **bridged** a workout
  streak (2 → 4) while Weekly Progress stayed 2/4 and the workout count stayed 6.
- **Card polish** (`DailyBoostCard`): brought into the Weekly Progress design family — a brand-gradient
  identity medallion, a status chip ("Done today" / "2 min"), a grouped **"Selected boost"** tile, and a
  clear Start (primary) + Random / Choose (tonal) action row. The separate "Boost streak" line was removed.
- **Timer polish** (`DailyBoostTimerDialog`): a pill **DAILY BOOST** eyebrow, a larger title, a soft
  brand radial glow behind the ring, a refined progress ring/dot, and a clearer control hierarchy —
  primary **Pause/Resume**, outlined **Cancel**, tonal **✓ Done**. The 2:00 countdown, draw-phase ring,
  pause/resume, Cancel-doesn't-complete, and Done→"Finish boost early?" confirm are all unchanged.

Verified on Pixel 8 (Light + Dark) June 10, 2026: card premium in both themes; **Active streak** counts
the boost (2 → 4 bridge test); Random/Choose/Start/Pause/Resume/Cancel work; completion persists across
restart (migration preserved it); Weekly ring + History unaffected. Clean `adb logcat -b crash`.

### Plan reorder + action polish (Task C-2)

- **Drag-to-reorder exercises within a day** (`PlanScreen.ReorderableExerciseColumn`): a leading drag
  handle (`Icons.Outlined.DragIndicator`) starts a **long-press** drag; the dragged row lifts
  (shadow + scale + raised z, haptics on pick-up and each swap) and neighbours reflow as it crosses their
  mid-point. Order is persisted locally via `GymRepository.reorderExercises` (renumber `sortOrder` in one
  REPLACE batch — **no schema change**; `sortOrder` already drives `observePlan`). Only the per-frame drag
  offset is read in the draw/layout phase, so dragging doesn't recompose the list each frame. Verified:
  moving an exercise 1 → 3 persists across force-stop/relaunch.
- **Red-accented delete** (Task C-2B): both the **day** Delete button and each **exercise** delete icon
  use `colorScheme.error` (text/icon + a subtle red border on the day button) — tasteful, not loud.
- **Centered day actions** (Task C-2C): the day-level **Edit** / **Delete** buttons are now equal-width
  and balanced across the card (not the per-exercise icons).

Verified on Pixel 8 (Light + Dark): long-press drag reorders within a day and persists across relaunch;
day Edit/Delete are centered/balanced with red Delete; exercise delete icons are red; expanding,
reordering and navigating away/back never crash (clean `adb logcat -b crash`). Outputs: debug
`app/build/outputs/apk/debug/app-debug.apk`, signed release `app/build/outputs/apk/release/app-release.apk`
(existing keystore, untouched).

## Build Release APK

```powershell
.\gradlew.bat :app:assembleRelease
```

Output depends on local signing (see **Build & signing (current, verified)** near the top). The
release `signingConfig` is applied only when `keystore.properties` exists at the repo root:

- **`keystore.properties` present (this machine):** signed →
  `app\build\outputs\apk\release\app-release.apk`.
- **`keystore.properties` absent (fresh clone / CI):** unsigned →
  `app\build\outputs\apk\release\app-release-unsigned.apk`; sign it with `apksigner` from the Android
  SDK Build Tools.

`app/build.gradle.kts` never hardcodes secrets and never fails for missing local credentials — it
simply leaves the APK unsigned when the keystore is absent. **Always confirm the actual output
filename before calling a release signed or unsigned. Never read, move, or commit the `.jks` /
`keystore.properties`.**

## Build Signed AAB For Google Play

Create a keystore:

```powershell
keytool -genkeypair -v `
  -keystore gym-tracker-release.jks `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias gymtracker
```

In Android Studio:

1. Select **Build > Generate Signed Bundle / APK**.
2. Choose **Android App Bundle**.
3. Select `gym-tracker-release.jks`.
4. Enter the alias and passwords.
5. Choose the `release` variant.
6. Finish the wizard.

CLI alternative:

```powershell
.\gradlew.bat :app:bundleRelease
```

If signing is configured in Android Studio, output appears at:

```text
app\build\outputs\bundle\release\app-release.aab
```

## Publish To Google Play

1. Create a Google Play Developer account.
2. Create a new app in Play Console.
3. Complete store listing, privacy policy, app category, content rating, and data safety.
4. Upload the signed `app-release.aab`.
5. Create an internal testing release first.
6. Test install from Play on a real device.
7. Promote to closed/open testing or production when ready.

This app stores workout data locally only. If you add cloud sync later, update the Play Console Data Safety section before publishing.

## MVP Features

- Default 4-day split: Push, Pull, Legs, Full Body.
- Versioned/resumable ONBOARDING-V3 for a complete local training profile; only the existing coaching goal and
  exact current weekdays affect the app today. The final receipt presents the existing starting plan without
  claiming plan generation or creating history, while all other accepted fields remain explicit future context.
- Weekly Progress ring on Home (target derived from training weekdays, workout-only) that opens the
  full Consistency screen with weekly detail, Active Streak, milestones, month calendar, and evidence
  receipt.
- Manually choose the current workout day on Home, or skip to the next day; the choice is persisted.
- Create, edit, and delete workout days.
- Create, edit, delete, and drag-to-reorder exercises within a workout day (order persists).
- Log sets with weight, reps, RIR, and optional notes.
- Save workout history in the History V2 month archive; tapping a workout opens Session Review.
- Show previous exercise performance while logging.
- Suggest progressive overload based on target reps and RIR.
- Show current vs previous volume trend while logging.
- Highlight PRs for weight or reps.
- Exercise detail history and overload suggestion.
- Daily Boost: an optional ~2-minute rest-day habit on Home (pick/random from a small exercise list, a 2:00 timer, "completed today"). A completed boost counts toward the Home **Active streak** (workout OR boost = an active day) but never toward Weekly Progress, History, PRs, volume, or session counts.
- Local JSON export/import from Settings.
- Light mode, Dark mode, and System default theme setting.

## Architecture

- Kotlin
- Jetpack Compose
- Room Database
- MVVM ViewModels
- Local-only repository, no backend and no login

Main package:

```text
com.hoctin.gymtracker
```

## Exercise Catalog (DB-0 foundation)

DB-0 adds the **foundation** for a future 150+ exercise library — **not** the full database. It is a
**reference catalog** kept deliberately separate from the user's Plan.

**Two separate concepts (do not merge):**

- **Plan Exercise** = `ExerciseEntity` (table `exercises`). User-editable snapshots owned by a workout
  day and used by Log Workout / save / Progression V3. **Unchanged by DB-0.**
- **Exercise Catalog** = `CatalogExerciseEntity` (table `catalog_exercises`). Read-only reference
  library for future search/filter/add-to-plan. It does **not** replace or link to plan exercises in DB-0.

**Storage & migration:**

- Room, `version = 2` (`exportSchema = true`; schemas in `app/schemas/`).
- **Additive `MIGRATION_1_2`** in `AppDatabase.kt` runs **only** `CREATE TABLE IF NOT EXISTS
  catalog_exercises (...)`. It never touches `workout_days` / `exercises` / `workout_sessions` /
  `logged_sets`, and **no `fallbackToDestructiveMigration` is used anywhere** — so existing users keep
  all plans and history on upgrade (verified: real v1→v2 upgrade preserved the plan + saved sessions,
  no crash; migration SQL matches the generated v2 schema).
- The catalog table uses all-primitive columns (enums stored as `name`, lists as `|`-CSV), so it needs
  **no Room TypeConverters**.

**Seeding:** `ExerciseCatalogRepository.ensureSeeded()` inserts a small sample (12 exercises) on launch
**only when the table is empty**. This is **seeded reference data, not user data**.

**Import/Export:** **unchanged.** The catalog is reference data and is intentionally **excluded** from
the JSON export/import (which remains scoped to the user's plan/sessions/sets).

**Taxonomy** (`data/catalog/CatalogTaxonomy.kt`): `Equipment` (12), `MuscleGroup` (24), `Level` (4),
`Force` (8), `Category` (STRENGTH), `ProgressionType` (4). Each `CatalogExercise` carries: slug, name,
equipment, primary/secondary muscles, level, force, category, progressionType, default sets/min/max
reps, default increment kg, sessions-required-before-increase, isBodyweight, isUnilateral, aliases.

**Search/filter** (`data/catalog/CatalogSearch.kt`): pure, deterministic `filter(items, query, equipment,
primaryMuscle, level, progressionType)` — text matches name + aliases (case-insensitive), filters
AND-combined. Covered by `CatalogSearchTest` (13 pure-JVM tests).

**Progression V3 is frozen.** The catalog's `ProgressionType` is **metadata only** for future engine/AI
work; DB-0 does **not** wire it into `ProgressiveOverload.kt`, and that file + `ProgressiveOverloadEngineTest.kt`
(33 tests) are untouched.

### Catalog Picker (DB-1)

Users can add catalog exercises into their Plan. **Plan → expand a day → Add exercise** now opens a
chooser bottom sheet (`AuronModalSheet`): **Browse catalog** or **Create custom exercise** (the original
custom dialog, unchanged).

- **Catalog Picker** (`ui/catalog/CatalogPickerScreen` + `CatalogPickerViewModel`, route `catalog/{dayId}`):
  search field (name/alias), dropdown-backed filter chips (equipment / primary muscle / level /
  progression), and result rows (name, equipment · primary muscle, default sets×reps, progression badge,
  Add button). Filtering uses `ExerciseCatalogRepository` + the pure `CatalogSearch` (deterministic).
- **Add = snapshot, not a link.** Tapping Add calls `GymRepository.addExercise(dayId, name, defaultSets,
  defaultMinReps, defaultMaxReps)` — it creates a brand-new, fully-editable `ExerciseEntity` in that day
  using the catalog defaults. There is **no stored dependency** from the plan exercise back to the catalog,
  so editing/deleting/reordering it later behaves exactly like any custom exercise. A snackbar confirms each
  add; back returns to Plan.
- **No schema change from DB-0** (catalog table/version untouched). V3, Log Workout, History, Weekly
  Progress, Daily Boost, and Import/Export are unchanged; the picker only reads the catalog and writes a
  normal plan exercise via the existing repository method.

### Exercise Detail popup + first content batch (DB-1.5 / DB-2A)

The first real content batch — **25 chest exercises** — plus a **reusable exercise detail popup**.

- **Detail model (additive):** `CatalogExercise` gains `instructions`, `tips`, and `imageRefs`
  (carousel-ready media; empty for now → the popup shows a placeholder). Stored on the catalog table
  only — **DB bumped v2 → v3** via `MIGRATION_2_3`, which DROP+CREATEs `catalog_exercises` with the new
  schema (safe: catalog is reference data, always re-seeded; **no user table touched**, no destructive
  fallback). Verified against the exported v3 schema and a real v2→v3 upgrade (no crash, plans/history
  intact).
- **Content re-seed:** `CatalogSeed.CONTENT_VERSION` + `ExerciseCatalogRepository.ensureSeeded()` clear
  and re-insert the bundled catalog whenever the version changes — so future batches are a one-line
  version bump with **no schema change**. The 25 chest batch replaces the DB-0 sample set.
- **Reusable popup** (`ui/catalog/CatalogExerciseDetailSheet.kt`): an `AuronModalSheet` showing Title →
  media slot (placeholder) → Quick Breakdown (Level/Equipment/Force/Category/Progression) → **Muscle
  Breakdown with explicit Primary / Secondary** (not flattened hashtags) → numbered Instructions →
  bulleted Tips. A `CatalogDetailController` (+ `rememberCatalogDetailController` / `CatalogDetailHost`)
  lets any surface open it by `CatalogExercise` or by name (with a graceful "no details yet" fallback).
- **Wired into:** the **Catalog Picker** (tap a row → detail; the `+` still adds) and the **View Detail**
  screen (exercise info icon → detail by name). The controller is reusable on Log/History too; those
  weren't wired this batch to avoid touching the frozen Log/History code.
- **Content source:** the 25 chest exercises and their taxonomy come from the provided chest PDF
  (normalized to the catalog enums); instructions/tips are written in concise English. Per-progression
  default sets/reps/increment are conservative defaults (the source gave no targets).
- **Unchanged:** Progression V3 (+33 tests), Log Workout, History, Weekly Progress, Daily Boost,
  Import/Export, custom exercise creation, and all existing workout/session/log Room tables.

### Detail popup wired everywhere (DB-1.6)

The reusable detail popup is now available on every reasonable surface that shows an exercise name.

- **One shared popup, four surfaces:** the same `CatalogExerciseDetailSheet` + `CatalogDetailController`
  (`rememberCatalogDetailController` / `CatalogDetailHost`) is opened from:
  - **Catalog Picker** — tap a row (has the `CatalogExercise` directly).
  - **View Detail** — exercise info icon (lookup by name).
  - **Log Workout** — a small info "i" beside each exercise name in the card header (lookup by name).
  - **History** — a small info "i" beside each exercise name in the expanded session detail (lookup by name).
- **Open by name + graceful fallback:** Log/History/View Detail only know an exercise *name*, so they
  call `controller.openByName(name)` → `ExerciseCatalogRepository.findByName` (exact name/alias match).
  A match shows the full detail; no match shows the existing "Detailed breakdown isn't available yet"
  sheet. No wrong matches, never a crash.
- **Architecture / safety:** DB-1.6 is **UI-only** — it adds info icons and popup wiring to three
  Compose files (`LogWorkoutScreen`, `HistoryScreen`, `GymTrackerApp`) and threads `catalogRepository`
  into those screens. The info icons have their own `clickable`, so they never trigger the surrounding
  expand/collapse. **Untouched:** Progression Engine V3 (+33 tests), the Log/History ViewModels, save
  logic, draft logic, History grouping, Room schema, Import/Export, and the catalog data/seed — all
  verified byte-identical to backup.

### Exercise Detail Popup Polish (DB-1.6.5)

DB-1.6.5 — Exercise Detail Popup Polish is a visual-only pass on the shared
`CatalogExerciseDetailSheet`.

- Larger white title treatment with a white fitness icon, preserving the same `AuronModalSheet` shell,
  drag handle, sheet position, controller, and open/close behavior.
- Carousel-ready image placeholder remains the original no-image slot; no image loading, networking,
  storage, or catalog media changes were added.
- Quick Breakdown, Muscle Breakdown, Instructions, and Tips keep an unboxed section layout with white
  section icons, including a flexed-biceps icon for Muscle Breakdown, stronger all-caps headers, more
  vertical spacing, and clearer label/value hierarchy.
- Instructions and tips have improved line height, spacing, numbering, and bullet readability so long
  future content can scroll cleanly in the existing sheet.
- No functional changes, no database changes, no catalog content changes, no Room schema changes, and no
  Progression Engine V3 changes.

**Next step — DB-2:** the next muscle-group batch (e.g. back/legs), added to `CatalogSeed` with a
`CONTENT_VERSION` bump — no schema change. Optional later: real local images for the media slot.

### Global Visual Polish Pass (UI-1)

UI-1 is a **UI-only** pass that promotes the DB-2D/DB-2E Log Workout card language across the whole
app. **No** changes to `ProgressiveOverload.kt` / `ProgressiveOverloadEngineTest.kt` (33 tests still
pass), the Room schema (still `version = 3`), catalog data (`CONTENT_VERSION = 5`, byte-identical
`CatalogSeed.kt`), import/export, save/draft, History grouping, or repository logic — all verified
byte-identical to a pre-edit backup. Thirteen files changed, all under `ui/`.

- **Part A — shared surface system (`ui/theme/Theme.kt`).** Dark mode now uses a **flat OLED dark-gray
  page** (`background = #0F0F12`) instead of the dotted `auron_dark_texture` parallax (the texture
  asset is retained but no longer drawn by `auronScreenBackground`). Cards are an opaque charcoal
  (`surface = #16181D`, slightly lighter than the page) with a subtle all-around border
  (`gymContainerBorder`) and the DB-2E **hard shadow** — a new shared `Modifier.auronCardElevation(cornerRadius)`
  paints a blur-free OLED-black panel offset to the bottom-right (Dark only; Light keeps flat bordered
  cards). `auronInsetColor()` provides darker recessed wells. Depth comes from contrast, not glow/blur.
  Applied to Home, Plan, View Detail, Weekly Hub, Daily Win, History, Settings, Exercise Detail,
  Catalog, dialogs and sheets. **The floating bottom-nav dock is excluded and pinned to its prior
  color so it stays visually unchanged.**
- **Part B — typography.** Big screen/hero/dialog titles use `FontWeight.Black`; the shared
  `SectionTitle` is `ExtraBold` with slightly wider tracking. Body and label styles are unchanged; no
  font-size changes, so no layout shifts.
- **Part C — Start Workout confirmation.** View Detail's Start Workout now opens a confirmation dialog
  ("Let's get it" / `Are you ready to start "<name>"?` / Cancel + Start Workout) built on the new card
  surface with a clean **white** primary button (no purple).
- **Part D — Weekly Progress color ramp.** The Home weekly ring + "% complete" badge animate through
  red (0–24%) → orange (25–49%) → yellow-green (50–74%) → green (75–100%). The existing fill animation
  is unchanged.
- **Part E — Daily Win redesign.** Exactly two actions — **Start** (primary) and **Random** (proper
  shuffle icon); choosing a different win moved to tapping the "Today's win" tile. A slim completion
  bar animates to full (brand → green) when the win is done. No XP, no streaks, no new gamification.
- **Part F — dialog cleanup.** AlertDialogs (Discard workout, Edit training days, Add/Edit day &
  exercise, Finish boost early, Settings status) use a neutral card container with `tonalElevation =
  0.dp` (kills the M3 dark purple tint). The Discard action is red/destructive.
- **Part G — Shoulders filter.** The generic `SHOULDERS` option is hidden from the catalog Muscle
  filter UI (the specific `FRONT_DELTS` / `SIDE_DELTS` / `REAR_DELTS` remain). The enum and schema are
  untouched — a pure UI filter.

Validated with the known-good JDK 17 (`D:\jdk17\jdk-17.0.19+10`): `:app:testDebugUnitTest` (33 V3
tests pass), `:app:assembleDebug`, and a **signed** `:app:assembleRelease`
(`app/build/outputs/apk/release/app-release.apk`; keystore untouched). Live QA on the Pixel 8 (API 37)
emulator in Dark **and** Light: Home, Weekly Hub, Daily Win, View Detail, the new Start Workout dialog,
Log Workout, History and Settings all render the consistent surface language; the floating dock and the
weekly calendar colors are intact.

### UI-HOME.2 — Weekly Progress Ring + focused Streaks detail

The current Home module keeps the animated workout-only Ring, then reduces streak continuity to one
source-agnostic seven-day glance. Its pushed detail route is visibly titled **Streaks** and separates
milestones, weekly workout schedule, calendar, and selected-day details without restoring the former
Weekly Hub.

- **Home Ring restored:** the existing `ProgressRing` renders near 96 dp with
  `<completed>/<target>` plus one integer percentage in the center. The target comes from selected training weekdays with the
  existing plan-size fallback; completed work is the count of distinct saved-workout dates in the
  current ISO week. Multiple workouts on one date count once.
- **Workout-only contract:** Daily Boost never changes the Ring or its target/completed values. The
  Ring keeps the red → orange → yellow-green → green thresholds and smooth fill animation. Home has no
  duplicate percentage badge, progress rail, or week ribbon.
- **Navigation:** tapping the Ring summary or its 48 dp `View More` action opens the pushed
  **Streaks** detail route. The floating dock remains hidden through the existing non-top-level route
  behavior.
- **Compact Streak strip:** the seven equal columns end on today. Workout, Boost, and Both use the
  same flame visual and still increment the union only once; Home does not disclose the activity
  source. Past inactive dates use a solid muted-red `X`, today-pending uses a neutral purple outline,
  and pre-tracking dates remain neutral.
- **Fire lifecycle fixed:** the imported Lottie fire still plays five loops at about 1.08×
  (approximately five seconds) only in today's newly active cell, once per epoch day. It then swaps
  to a static two-tone flame instead of holding the asset's transparent final frame. Historical cells
  are static; reduced-motion mode renders the static flame immediately; a second activity source on
  the same day does not replay it.
- **Focused Streaks detail:** the hero contains only current-streak number, label, and static flame.
  Quiet 7/14/30/60 checkpoints come next, followed by one workout-only rail and a Monday–Sunday
  schedule row. The month calendar uses the same solid Done/Missed/Planned/Rest grammar with one
  visible workout-schedule axis and no horizontal scrolling.
- **Selected-day details:** Workout and Daily Boost sources remain available below the calendar only.
  Every saved workout on the selected date renders as one full-width session card with a purple workout
  tile, real title/time, and a responsive chip row ordered as completed sets, real rep-volume when
  positive, meaningful duration when available, exercise count, and PR count when positive. The whole
  card opens Session Review; there is no chevron or expandable duplicate archive. Time/distance sessions
  never expose stored `0 reps` / `0 kg` sentinels, and distance is omitted here because this route does
  not carry the user's unit preference. Boost-only days still show `Daily Boost completed` without
  invented exercise data or timestamps. Best streak, continuity state, source union, and once-per-day
  persistence remain stored/calculated even though the hero no longer repeats them.
- **Responsibility split:** the old `WeeklyProgressHubSheet`, Recent sessions, expandable session
  cards, and exercise tables inside Weekly Progress remain removed. **History V2 is the canonical
  workout archive**; Streaks explains continuity evidence without becoming another archive.
- **Safety:** no Room/schema, Progression Engine, catalog, import/export, save/draft, History grouping,
  `GymRepository`, Muscle Heat/Recovery scoring, Daily Boost completion, or floating-navigation
  implementation change.

### Exercise Thumbnail Rows (UI-2A)

UI-2A is a frontend-only exercise-row media pass. It replaces the repeated purple dumbbell placeholder
where an exercise row has a catalog match with the first bundled catalog image, while keeping the
existing purple fallback for custom, unmatched, or unreadable exercises.

- **Shared component:** `ui/components/ExerciseThumbnailRow.kt` provides `ExerciseThumbnail`, backed by
  the detail popup's shared asset thumbnail loader. Decoding runs off the main thread and uses a bounded
  in-memory bitmap cache; the detail sheet reuses the same loader for its larger media slot.
- **Lookup:** `ExerciseCatalogRepository.observeThumbnailRefsByName()` builds an exact canonical-name and
  alias lookup to the first `imageRef`. It does not use fuzzy matching, so a custom name never receives an
  incorrect catalog image.
- **Updated screens:** Catalog Picker, View Detail, Plan, Log Workout, and expanded History exercise
  rows. Weekly Progress is intentionally unchanged because its session card is not an exercise row.
- **Preserved behavior:** Plan reorder/edit/delete, Catalog add/detail, Log accordion/info/save/draft,
  and History grouping/info behavior are unchanged. No engine, Room schema, catalog JSON/assets,
  save/draft repository, or History data-model logic changed.
- **Validation:** the user manually built and ran the app; screenshots confirmed thumbnails and layouts in
  Catalog Picker, Plan, Log Workout, and View Detail. Release APK size was approximately 79.2 MB before
  and 83.2 MB after. Codex could not run Gradle or emulator QA in its environment because the wrapper
  download was blocked and no emulator was attached.

### FED catalog growth & MVP coverage (DB-5B … DB-5F)

The FED-canonical catalog (DB-4B.2, JSON at `assets/catalog/catalog_fed_v1.json`) grew via data-only,
additive batches — **no Room schema change, no engine change, no Log Workout input change** throughout
the DB-5 series. DB-5F left the catalog at **568 exercises, `CONTENT_VERSION 14`**; DB-6E later grows
the current catalog to **577 exercises, `CONTENT_VERSION 16`** using the DB-6 tracking foundation.

- **DB-5B** imported 33 SAFE_ADD exercises from the free-exercise-db REVIEW set (515 → **548**, `CV 11`).
- **DB-5D** imported 5 MAYBE `SETS_REPS_WEIGHT` exercises (Board Press, Pin Presses, Kneeling Squat,
  Seated Good Mornings, Svend Press) after a strict dedup pass (548 → **553**, `CV 12`).
- **DB-5D.2** applied 6 high/medium-confidence difficulty-level corrections (e.g. Weighted Pull Ups,
  Power Clean → ADVANCED); count unchanged (`CV 13`).
- **DB-5F (final coverage polish)** added **search aliases** to 10 existing entries — fixing the real
  discoverability gap where FED names differ from common names — and imported **15** high-value entries
  (553 → **568**, `CV 14`):
  - *Aliases (no new exercises):* e.g. **Side Lateral Raise** ← "Dumbbell Lateral Raise"/"Lateral Raise";
    Pushups ← "Push-Up"/"Push Up"; Seated Cable Rows ← "Seated Cable Row"; Arnold Dumbbell Press ←
    "Arnold Press"; Machine Shoulder (Military) Press ← "Machine Shoulder Press"; Dips - Triceps Version ←
    "Triceps Dip"/"Parallel Bar Dip"; Side Bridge ← "Side Plank"; Plank ← "Forearm Plank".
  - *Imports (existing reps/kg model only):* 7 advanced calisthenics (Muscle Up, Ring Dips, One Arm
    Chin-Up, Scapular Pull-Up, Single-Leg High Box Squat, Suspended Row/Push-Up → `BODYWEIGHT`) and 8
    skill/olympic (Overhead Squat, Plate Pinch, Snatch, Clean and Jerk, Push Press, Hang Clean, Hang
    Snatch, Power Snatch → `SKILL_FORM` fallback — conservative, no auto-load, no new log fields).

**Coverage status:** the DB-5E full re-audit (all 873 FED entries) found **0 missing common strength
exercises** — every common movement family (lateral raise, bench, row, pulldown, pull-up, shoulder press,
squat, deadlift/hinge, hip thrust, curl, triceps, calf, abs, plank, lunge, leg curl/extension, fly) has
complete coverage.

**Deferred at DB-5F:** sled pushes, farmer/loaded carries, and other time/distance/velocity exercises
were intentionally held until tracking-specific Log fields existed. DB-6E now imports the supported
carry/sled slice (8 `TIME_DISTANCE_LOAD`, 1 `DISTANCE_LOAD`) using that tracking foundation; unsupported
velocity/strongman/cardio/stretching rows remain rejected or deferred. The per-exercise tracking
contract is in `docs/db5c_advanced_metrics_contract.md` / `docs/db5e_missing_fed_coverage.csv`.

**DB MVP catalog phase: complete.** All common strength movements are covered and searchable; remaining
candidates are advanced/olympic (conservative SKILL_FORM) already partly imported, unsupported, or
held for future tracking types beyond DB-6E's carry/sled slice. See
`docs/db5f_final_catalog_coverage_polish_result.md`.

### Multi-Tracking foundation (DB-6A … DB-6C)

The catalog and logging now support **non-rep exercises** (holds, carries, sleds, distance) alongside the
classic reps/kg/RIR model — a clean, future-proof foundation that does **not** change the engine or the
behavior of existing exercises.

- **Data model (DB-6B):** a `TrackingType` per catalog exercise
  (`REPS_WEIGHT` / `BODYWEIGHT_REPS` / `TIME_ONLY` / `TIME_LOAD` / `DISTANCE_LOAD` / `TIME_DISTANCE` /
  `TIME_DISTANCE_LOAD` / `SKILL_FORM`) — orthogonal to `progressionType`; nullable `durationSeconds` /
  `distanceMeters` on `logged_sets` (load reuses `weight`; `reps`/`rir` use a `0` sentinel hidden from
  the UI, so the `Σ weight×reps` volume math is untouched). **Room `@Database version 5`**, additive
  `MIGRATION_4_5` (no destructive fallback); catalog `CONTENT_VERSION 15`; **Export/Import `schemaVersion 2`**
  (backward-compatible with v1). DB-6B classified the original 568 entries as REPS_WEIGHT /
  BODYWEIGHT_REPS / SKILL_FORM, plus TIME_ONLY for Plank & Side Bridge; DB-6E now adds 8
  TIME_DISTANCE_LOAD entries and 1 DISTANCE_LOAD entry.
- **UI wiring (DB-6C):** the Log Workout card now renders fields **from the TrackingType** (reps/weight/RIR
  for rep types — unchanged; duration / distance / load for the new types) via reusable inline-unit
  `TrackingField`s; History renders each set per type (e.g. `01:30`, `40 kg • 20 m`) and never shows a
  fake `reps = 0`; Weekly kg-volume is unchanged (time/distance contribute 0). A new **Settings → Units →
  Distance** preference (Metric / Imperial) controls display only (the DB always stores meters).
- **Unified formatter:** a single `MeasurementFormatter` returns a structured `Measurement(value, unit)`;
  the UI renders it inline (value + **bold** unit) everywhere via `MeasurementValue`. Velocity (m/s, km/h)
  is already defined for future metrics — no UI redesign needed. Design: `docs/db6a_multi_tracking_foundation_audit.md`.
  Results: `docs/db6b_multi_tracking_schema_model_result.md`, `docs/db6c_multi_tracking_ui_result.md`.

#### DB-6C.1 - Multi-tracking UI consistency pass

DB-6C.1 closes the TIME_ONLY display gaps left after the initial UI wiring. The root cause was that
Review Workout, History, Weekly/Home summaries, and some target rows still summarized work through the
old reps/kg assumptions, so valid non-rep storage sentinels (`reps = 0`, `weight = 0`) could make a
duration exercise look like "0 reps / 0 kg" work. The prevention rule is now: **all target and summary
text for tracking-aware surfaces must go through `MeasurementFormatter` / `SetDisplay`; never hand-build
rep/weight strings for non-rep tracking types.**

Current supported tracking behavior:

- `REPS_WEIGHT`, `BODYWEIGHT_REPS`, `SKILL_FORM`: unchanged reps / kg / RIR input and display; kg-volume
  remains `weight x reps`.
- `TIME_ONLY`: Log input shows `Duration` with `mm:ss` suffix; accepts colon values (`1:30`,
  `01:30`, `1:00:00`) and digits-only right-to-left grouping (`3` -> `00:03`, `130` -> `01:30`,
  `3012` -> `30:12`, `010000` -> `01:00:00`), rejects invalid values, and displays normalized
  duration after focus leaves the field or the value is saved.
- `TIME_LOAD`, `DISTANCE_LOAD`, `TIME_DISTANCE`, `TIME_DISTANCE_LOAD`: helper support is wired; DB-6E
  now makes DISTANCE_LOAD and TIME_DISTANCE_LOAD reachable, and these types must show only their
  exposed load/distance/duration fields while hiding rep/RIR sentinels.

UI behavior after DB-6C.1:

- Review Workout shows TIME_ONLY rows as completed sets plus total duration, and duration-only workouts
  get meaningful summary cards instead of empty reps/volume metrics.
- History and Weekly/Home carry duration/distance totals through their ViewModels, count non-rep sets as
  completed work, and omit fake kg-volume when no rep-based sets exist.
- Plan, View Detail, Exercise Detail, and Catalog rows use tracking-aware target labels where catalog
  `trackingType` is available, so Plank-style exercises show `duration` targets instead of rep targets.
- Export/Import schema v2 and draft recovery preserve `durationSeconds` / `distanceMeters`.

Validation note: final post-patch `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:assembleRelease` passed with JDK 17. Pixel 8 emulator QA entered Plank sets `00:55`, `01:00`, and
`01:05`; verified draft recovery after force-stop/relaunch; saved the workout; confirmed Review,
History, Home, and Weekly Progress show `03:00 duration` without fake `0 reps` / `0 kg`; and found no
`com.hoctin.gymtracker` crash-buffer entries. The detailed evidence is in
`docs/db6c1_multi_tracking_consistency_fix_result.md`.

After DB-6E, runtime-reachable non-rep types include **TIME_ONLY**, **DISTANCE_LOAD**, and
**TIME_DISTANCE_LOAD**. Remaining non-rep variants are still wired for future imports, but should not
be made reachable until an exercise's real tracking fields are known and QA'd end to end.

#### DB-6C.2 - Duration input normalization polish

DB-6C.2 removes the remaining manual-colon friction before DB-6E imports time/distance exercises.
`MeasurementFormatter.parseDurationSeconds` is now the single parser for duration entry: colon input
is strict up to `HH:MM:SS`, digits-only input is grouped from the right as `HHMMSS`, and invalid
minutes/seconds or overlong values are rejected safely. Log Workout normalizes duration fields on
focus loss and the save path normalizes persisted display, while existing saved duration rendering
continues through `MeasurementFormatter.duration`.

Prevention rule for DB-6E imports: do not introduce per-screen duration parsing or string formatting.
Any imported TIME/DISTANCE exercise must use the existing tracking-aware formatter/display helpers and
must preserve the rule that non-rep sentinels are never shown as `0 reps` / `0 kg` work.

Validation note: DB-6C.2 passed `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:assembleRelease`. Pixel 8 emulator QA used Plank TIME_ONLY, entered `3`, `130`, `3012`, and
`010000`; Review showed `4/3 sets · 01:31:45 duration`; History showed set rows `00:03`, `01:30`,
`30:12`, and `01:00:00 duration`; crash buffer was clean. Details:
`docs/db6c2_duration_input_polish_result.md`.

#### DB-6E - Time / distance / load exercise import

DB-6E imports the first carry/sled exercises that depend on the DB-6 tracking model. The catalog now
has **577 exercises** and `CatalogSeed.CONTENT_VERSION = 16`. Room remains on the DB-6B schema and the
Progression Engine remains unchanged.

Imported:

- `TIME_DISTANCE_LOAD`: Backward Drag, Bear Crawl Sled Drags, Sled Drag - Harness, Sled Overhead
  Backward Walk, Sled Overhead Triceps Extension, Sled Push, Sled Reverse Flye, Sled Row.
- `DISTANCE_LOAD`: Farmer's Walk.

The imported rows use `defaultMinReps = 0` / `defaultMaxReps = 0` only as non-rep target sentinels.
User-facing labels come from `trackingType`, for example `3 sets · load + distance + time` and
`3 sets · load + distance` (the duration token was shortened from `duration` to `time` in DB-6F);
these rows must never be shown as fake `0 reps` work.

Validation note: DB-6E passed `:app:testDebugUnitTest`, `:app:assembleDebug`, and
`:app:assembleRelease`. Pixel 8 emulator QA added Sled Push and Farmer's Walk from Catalog, verified
Sled Push Log fields `Load` / `Dist` / `Time` with no `REPS` or `RIR`, saved a Sled Push set, and
confirmed History rendered `80 kg · 100 ft · 00:40 duration`; crash buffer was clean. Details:
`docs/db6e_time_distance_exercise_import_result.md`.

#### DB-6F - Multi-tracking UI polish & responsive layout

DB-6F fixes the responsive-layout regressions the carry/sled exercises exposed once DB-6E made the
non-rep tracking types reachable. It changes **UI only** — no Room schema, no Progression Engine, no
catalog imports, and no `CONTENT_VERSION` bump.

Root cause: two pill/field surfaces were laid out in a single fixed `Row` that shrank its children
instead of reflowing, so on a normal-width phone a long `progressionType` chip (e.g. `Compound Load`)
and the `Duration` field placeholder collapsed into a vertical, one-letter-per-line stack.

Fixes:

- **Catalog Picker rows** put the target + progression chips in a `FlowRow`, and `GymBadge` text is now
  hard single-line (`maxLines = 1`, `softWrap = false`, ellipsis). A chip that does not fit wraps to a
  clean second line instead of crushing.
- **Log Workout tracking rows** route all of load / distance / time through one responsive
  `TrackingFieldsRow` (a `BoxWithConstraints` grid): one line when the fields fit, otherwise a
  two-per-line layout. The duration field uses the compact label **`Time`** (units stay inline:
  `kg`, `m`/`ft`, `mm:ss`). The `CompactField` placeholder is also forced single-line, so no field can
  ever break a word vertically.
- **Delete consistency:** the non-rep set rows now use the same overflow **⋮ → Delete set** menu
  (`SetRowMenu`) as the rep rows, replacing the always-visible `✕` (harder to mis-tap). `SetIndexBadge`
  and `SetRowMenu` are shared by both row types.
- **Concise label:** `SetDisplay.targetLabel` renders the duration token as `time`, so
  `TIME_DISTANCE_LOAD` reads `load + distance + time` and `TIME_ONLY` reads `time`.

**Responsive layout rules for future tracking types:** any new `TrackingType` should expose its fields
through the `TrackingType.showsLoad/showsDistance/showsDuration` config and let `TrackingFieldsRow`
arrange them — do not add a per-type `Row` of fields. Catalog metadata chips must use `GymBadge`
inside a `FlowRow`. **Prevention rule: no chip or input field may wrap vertically on narrow screens** —
keep pill/placeholder text single-line (`maxLines = 1`, `softWrap = false`) and let the container
reflow (FlowRow / the two-line `TrackingFieldsRow` grid).

Validation note: DB-6F passed `:app:testDebugUnitTest` (133 tests), `:app:assembleDebug`, and
`:app:assembleRelease`. Pixel 8 emulator QA (light + dark) confirmed Catalog `far`/`sled` render with
no vertical chip text, Farmer's Walk logs `Load`+`Dist` on one row, Sled Push reflows to
`Load`+`Dist` then `Time` with no broken `Duration` word, the ⋮ menu deletes safely, and the crash
buffer was clean. Details: `docs/db6f_multi_tracking_ui_polish_result.md`.

#### DB-QUICKFIX1 - Exercise Detail image fit

The reusable Exercise Detail / Catalog Detail media frame now uses fit/contain behavior instead of
center-crop, so portrait and landscape exercise images stay fully visible in a stable 240 dp rounded
AURON frame. Carousel tap/cycle behavior, dots, sheet scrolling, and the image-less placeholder are
preserved.

Catalog row thumbnails are separate and untouched; `ExerciseThumbnail(...)` still owns the small row
thumbnail crop path. Details: `docs/db6f1_exercise_detail_image_fit_result.md`.

#### DB-PRE7 - Exercise history identity audit (audit only, no implementation)

Audit completed of how exercise identity flows through logging, previous-performance, history, and the
Progression Engine. **Classification: `BROKEN`** — previous-performance, PR detection, the per-exercise
history screen, and the engine input are all keyed on the day-specific plan `ExerciseEntity.id` (queries
filter `WHERE logged_sets.exerciseId = :id`), while the cross-day-stable `exerciseNameSnapshot` is never
used as a lookup key. So the same exercise (e.g. *Incline Machine Chest Press*) added to two days has two
different ids, and its history does not carry across days. History/Weekly/Home are snapshot displays only
and group by name within a session — they do not drive progression. **Next task: DB-PRE7B — repoint the
four lookups to a normalized-name key (no Room schema change).** No code/schema/engine/catalog changes in
DB-PRE7. Full audit: `docs/db_pre7_exercise_history_identity_audit.md`.

#### DB-PRE7B - Canonical exercise history identity fix

Fixes the DB-PRE7 `BROKEN` finding: the same exercise in different workout days now **shares**
previous-performance / progression / PR history.

**Canonical identity rule:** every logged set carries a catalog-aware `LoggedSetEntity.canonicalExerciseKey`
— `catalog:<slug>` when the exercise name (or an alias) resolves to the FED catalog, else
`custom:<normalized-name>`. All history/previous/PR/Exercise-Detail lookups key on this canonical identity
(resolved by the single `CanonicalExerciseKey` resolver), not on the day-specific plan `ExerciseEntity.id`.
`exerciseId` (plan-row relation) and `exerciseNameSnapshot` (historical display name) are kept unchanged,
and History/Weekly/Home still display by snapshot name.

**Migration v5 → v6 (additive):** `ALTER TABLE logged_sets ADD COLUMN canonicalExerciseKey TEXT` + index
`index_logged_sets_canonicalExerciseKey`. No destructive migration, no `fallbackToDestructiveMigration`. A
one-time, idempotent, flag-guarded backfill resolves the key for all pre-v6 rows on first launch (and again
after Import); export/import is now `schemaVersion 3` (tolerant of older v1/v2 exports). The Progression
Engine logic is unchanged — only the history list passed into it changed.

**Prevention rule:** future analytics / AI / progression / history features must key on
`canonicalExerciseKey` (via `CanonicalExerciseKey`), **never** on `ExerciseEntity.id` (a day-specific plan
row). Resolve identity through the one resolver; do not re-derive keys ad hoc.

Validation: DB-PRE7B passed `:app:testDebugUnitTest` (144 tests), `:app:assembleDebug`, and
`:app:assembleRelease`. Pixel 8 real v5→v6 upgrade QA confirmed no-crash migration, all existing sets
preserved + backfilled (`Plank`→`catalog:plank`, `Sled Push`→`catalog:sled-push`, 0 null keys), and the
cross-day scenario (Plank logged under Push, then shown with that history after being added to Full Body);
crash buffer clean. Details: `docs/db_pre7b_canonical_exercise_identity_fix_result.md`.
