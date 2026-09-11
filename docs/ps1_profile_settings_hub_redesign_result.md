# PS-1 — Profile & Settings Hub Redesign Result

Date: 2026-08-11  
Project root: `D:\HOCTIN\GYM TRACKER`

## Outcome

The fourth floating-navigation destination is now **Profile** and opens a complete Profile & Settings
hub. The redesign is a presentation/navigation reorganization over existing owners: Settings logic,
History V2, the ONBOARDING-V3 local profile, Plan, Streaks/Weekly Progress, import/export and Recovery
Research remain the sources of truth.

The primary reference was translated into AURON's current visual grammar rather than copied: large
title hierarchy, purple-to-OLED canvas, spacious tab-labelled black enclosures, hairline borders,
restrained right/lower depth, white navigation rows and purple used as identity/accent rather than a
universal button fill. Light theme remains supported through the existing semantic color scheme.

## Safety baseline and backup

- Confirmed Git root: `D:\HOCTIN\GYM TRACKER`.
- Read the complete current README before editing.
- Queried the existing Graphify graph before broad traversal.
- Worktree was already extensively dirty; no unrelated user change was cleaned, reset, stashed or
  overwritten.
- Timestamped filesystem backup:
  `D:\HOCTIN\GYM_TRACKER_PS1_BACKUP_20260811_211400`
- Backup contains `app/src` (1,304 files), `docs` (108 files), README and the app Gradle file.
- Signing secrets were not read, copied or printed.

## Final information architecture

### Profile hub

1. **Plan & progress**
   - Streaks & weekly progress → existing pushed Streaks/Consistency screen.
   - Workout insights → new History V2-derived statistics screen.
   - View & modify plan → existing Plan top-level destination.
2. **Preferences**
   - Training setup → existing coaching-goal and exact-weekday owners.
   - Units & appearance → existing theme and distance-unit owners.
   - Notifications & reminders → explicit unavailable destination.
3. **Your profile**
   - Personal profile → read-only ONBOARDING-V3 local profile receipt.
   - Workout history → existing History V2 archive.
4. **Data & research**
   - Import, export & AI helper → existing plan/history JSON and clipboard prompt behavior.
   - Recovery Research → existing opt-in, SAF export and local deletion behavior.
5. **Help & AURON**
   - Help & resources → product-specific FAQ plus honest contact/community status.
   - Privacy & local data → truthful local-storage and export boundaries.
   - About AURON → current product capability and claim boundary.

The hub's destination model is pure and unit-tested so every declared destination appears exactly
once and no row is silently orphaned.

## Functional screens

- **Profile hub:** grouped tab-card composition, 48 dp+ interactive rows, press feedback that respects
  disabled system animation, destination status labels and semantic icons.
- **Workout insights:** total workouts, sets, reps, rep volume, PRs, average recorded duration, most
  logged exercise and first saved-workout date. All values are calculated from `HistoryUiState`; no
  repository query or new history grouping was added. Non-rep work remains valid but cannot create
  fake kg/reps.
- **Training setup:** preserves `WorkoutGoalSelection`, AUTO/explicit goal sheet, exact weekday picker,
  plan-size validation and existing SharedPreferences owners.
- **Units & appearance:** preserves System/Light/Dark and Metric/Imperial controls.
- **Import & export:** preserves JSON generation, JSON replacement/import, canonical-key backfill and
  the clipboard-only AI helper. No network/AI call was introduced.
- **Recovery Research:** preserves consent, local capture semantics, deterministic JSON preparation,
  Android document-picker export and destructive local delete. The destructive action is red and the
  confirmation explicitly states that plan/history are unchanged.
- **Personal profile:** decodes the existing `training_profile_json` document and shows user-provided
  profile/training context without inventing account data or changing onboarding persistence.
- **Help, Privacy, About:** provide in-app, capability-grounded content with no dead external links.

## Explicit unavailable/updating states

- Profile-field editing: **Updating**. Current profile is visible, but there is no fake Save action and
  no mutation of onboarding-owned fields.
- Notifications/reminders: **Unavailable**. No permission is requested and no fake toggle is shown.
- Contact/community: **Unavailable** because the project has no verified support/community URL.
- Formal hosted Privacy Policy/Terms URLs: **Unavailable**; a truthful local-data summary is shown.
- Language selection: **Unavailable** because there is no persisted language owner.

These destinations remain polished and informative rather than becoming dead rows.

## Before / after behavior

Before PS-1, the fourth destination was labelled Settings and one long screen mixed display, units,
training, research, export, import and AI-helper controls. The ONBOARDING-V3 profile and History-derived
statistics had no Profile-system entry point.

After PS-1, the fourth destination is labelled Profile, the hub exposes all major capabilities in a
stable hierarchy, and pushed subpages hide the floating dock through the existing top-level-route
mechanism. Existing Plan, History and Streaks destinations are reused instead of duplicated.

## Files changed

- `README.md`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileSettingsModels.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileSettingsComponents.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileHubScreen.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileOverviewAndInsightsScreens.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfilePreferencesAndTrainingScreens.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileDataResearchResourcesScreens.kt` (new)
- `app/src/test/java/com/hoctin/gymtracker/ui/settings/ProfileSettingsModelsTest.kt` (new)
- `docs/ps1_profile_settings_hub_redesign_result.md` (new)
- Graphify-generated graph/reflection artifacts after `graphify update .`.

`SettingsScreen.kt` only exposes existing focused controls for reuse; Settings behavior remains owned
by the existing `SettingsViewModel` and repositories.

## Validation

### Source and tests

- Full non-cached Kotlin compile:
  `:app:compileDebugKotlin --rerun-tasks --no-build-cache` — **PASS**.
- Focused `ProfileSettingsModelsTest` — **PASS**.
- `:app:testDebugUnitTest` — **PASS: 404 tests, 0 failures, 0 errors, 0 skipped**.
- `:app:assembleDebug` — **PASS**.
  - `app/build/outputs/apk/debug/app-debug.apk`
  - 99,406,138 bytes.
- `:app:assembleRelease` — **PASS** with a final explicit exit code 0 rerun.
  - `app/build/outputs/apk/release/app-release.apk`
  - 92,220,930 bytes.
- Existing AGP 8.5.2 / compileSdk 35 compatibility warning remains; no dependency upgrade was made.

### Protected-surface verification

Post-change SHA-256 values match the pre-edit baseline for:

- `ProgressiveOverload.kt` — `8D2355D5…957B`
- `ProgressiveOverloadEngineTest.kt` — `FF4DDE81…B947`
- `GymRepository.kt` — `6F5EA12B…F952`
- `AppDatabase.kt` — `42EA8504…760B`
- `CatalogSeed.kt` — `92CFBDB1…9B6E`
- `catalog_fed_v1.json` — `C09E400E…D69`
- Room schema exports `2.json` through `9.json` — all hashes unchanged.

The complete `GymBottomBar` implementation block and `navigateTopLevel` code are byte-identical to the
timestamped pre-edit backup (`4,709` characters each). Only the fourth destination's label/icon and
the Profile route graph changed.

No changes were introduced to:

- Progressive Overload behavior/tests.
- Room entities, DAO, migrations or schema exports.
- Catalog data/content version.
- Import/export repository contract.
- Save/draft behavior.
- History V2 grouping/presentation or Session Review.
- GymRepository behavior.
- Muscle Heat/Muscle Recovery scoring.
- Daily Boost completion.
- Onboarding persistence/commit behavior.
- Floating-dock implementation.

## Runtime QA and screenshots

Runtime visual QA was attempted once using the existing `Pixel_8` AVD. The emulator process started,
but ADB reported:

```text
emulator-5554 unauthorized
```

Per the task's hard-error rule, screenshot recovery stopped immediately. The emulator was closed; no
data clear, wipe, connected instrumentation test, authorization workaround or destructive device action
was attempted.

Therefore the requested screenshots (hub, four subpages and unavailable state) are **not available in
this report**, and runtime visual acceptance, tap-through verification, dark/light screenshots,
font-scale QA, TalkBack QA and crash-buffer inspection remain **NOT COMPLETED**. Build and source proof
must not be interpreted as runtime visual proof.

## Known limitations

1. Runtime screenshots and on-device visual comparison are pending because the available emulator was
   unauthorized.
2. Profile editing, reminders, contact/community and hosted legal links remain explicit unavailable or
   updating states until each has a real persistence/permission/destination contract.
3. The existing long `SettingsScreen` presentation remains compiled as a compatibility implementation,
   while the new route graph uses the focused Profile pages. Its business owners are shared, not forked.
4. Motion uses platform Compose animation and `ValueAnimator.areAnimatorsEnabled()`; tactile quality and
   TalkBack focus order still require physical/runtime QA.

## Graph status

`graphify update .` completed successfully after implementation: AST extraction processed 1,007
uncached code files and rebuilt `graphify-out/graph.json` / `GRAPH_REPORT.md` with 6,320 nodes, 10,636
edges and 449 communities. Graphify skipped `graph.html` because the graph exceeds its default
5,000-node visualization limit; this does not affect the query graph.
