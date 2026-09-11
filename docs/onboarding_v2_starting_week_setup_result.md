# ONBOARDING-V2-BUILD — Truthful Starting-Week Setup Result

Date: 2026-08-02  
Project root: `D:\HOCTIN\GYM TRACKER`

## Outcome

The previous weekday-only first-launch gate is now a versioned, resumable five-stage onboarding route:

`Welcome → Coaching goal → Training week → Review setup → Your starting week is ready`

The implementation collects only values with confirmed production consumers:

- The existing `WorkoutGoalSelection`, with `AUTO` as the valid recommended default.
- Exact ISO training weekdays, with the required count derived from the current plan size.

It does not collect experience, duration, equipment, muscle priorities, gender, age, height, weight,
target weight, BMI, injury/pain, recovery or cardio preference. It does not claim AI plan generation,
create a workout/session/draft/history item, rewrite the starting split or request a permission.

## Safety baseline and backup

- Real checkout confirmed: `D:\HOCTIN\GYM TRACKER`.
- The worktree already contained extensive unrelated changes; none were reset or cleaned.
- Timestamped filesystem backup:
  `D:\HOCTIN\GYM TRACKER\_backup_onboardingv2_20260802_061955`
- Backup contents: 1,377 files, 83,876,416 bytes, covering source, schemas, docs, Gradle files and README.
- Protected-surface SHA-256 values were captured before edits for 28 files.
- A final filesystem comparison against that backup confirmed the app/source/test/README/docs delta is
  exactly the file list below; unrelated dirty-worktree content was not folded into this task.

## Files changed

Production:

- `app/src/main/java/com/hoctin/gymtracker/data/UserPreferencesRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/components/GoalSelectorContent.kt` (moved here as shared content)
- `app/src/main/java/com/hoctin/gymtracker/ui/components/WeekdayPicker.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingModels.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingScreen.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingComponents.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingVisuals.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/GoalSelectorContent.kt` (removed after shared move)
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt` (imports only; behavior unchanged)

Tests:

- `app/src/test/java/com/hoctin/gymtracker/ui/onboarding/OnboardingReducerTest.kt` (new)
- `app/src/test/java/com/hoctin/gymtracker/ui/onboarding/OnboardingCopyContractTest.kt` (new)
- `app/src/test/java/com/hoctin/gymtracker/ui/settings/GoalSelectorContentTest.kt` (shared-content imports only)
- `app/src/androidTest/java/com/hoctin/gymtracker/data/OnboardingPreferencesInstrumentedTest.kt` (new)

Documentation/generated graph:

- `README.md`
- `docs/onboarding_v2_starting_week_setup_result.md` (this report)
- Graphify generated output/reflection files refreshed by the required graph workflow.

No new grain asset was added. The onboarding shell reuses the existing static
`res/drawable-nodpi/auron_dark_texture.png` at very low alpha, avoiding a duplicate resource.

## Ownership and persistence

### Existing owners preserved

- `WorkoutGoalPreferenceStore` remains the single encoding/observable owner of `workout_goal`.
- `UserPreferencesRepository` remains the owner of training weekdays and tracking-start preferences.
- `GymRepository` behavior was not changed; onboarding only observes the existing plan and ensures seed data.

### Added V2 preference state

- `onboarding_completion_version`
- `onboarding_draft_version`
- `onboarding_draft_step`
- `onboarding_draft_goal`
- `onboarding_draft_weekdays`
- `onboarding_receipt_pending`

The existing keys remain intact: `onboarding_completed`, `training_weekdays`,
`tracking_start_epoch_day` and `workout_goal`.

Legacy `onboarding_completed=true` installs resolve to completion version 1 and start at Home. They are
not forced through V2. Fresh users complete version 2.

### Atomic completion

`completeOnboardingV2(...)` uses one disk-backed SharedPreferences commit to publish:

- Goal.
- Sanitized weekdays.
- Legacy completion Boolean.
- Completion version 2.
- Existing tracking start, or today only when no start exists.
- Pending-receipt state.
- A complete receipt draft containing goal and weekdays.

Observable production flows update only after the commit succeeds. A failure retains the draft and leaves
production values unchanged. Writing the receipt draft in the same commit closes the process-death gap between
“setup saved” and “receipt resumable.”

Finishing the receipt clears only pending-receipt and draft keys. Production goal, weekdays, completion and
tracking start remain intact. `Start over` creates a clean Welcome draft without clearing production state.

## User-visible flow

### 1. Welcome

- Copy: `Train with a plan. Improve from evidence.`
- States that setup creates no history and choices can be changed later.
- Seed preparation is real; progress UI appears only after 300 ms and has no fake percentage.

### 2. Coaching goal

- Reuses all existing Settings goal content and descriptions.
- `AUTO` is selected by default and labelled Recommended.
- Copy explicitly limits the effect to future Log Workout and Exercise Detail coaching.
- Selection writes only the resumable draft until confirmation.

### 3. Training week

- Reuses the canonical ISO weekday list and a narrowly parameterized `WeekdayPicker` style.
- Requires exactly the current plan-day count (1–7).
- A blocked over-limit tap has no haptic and asks the user to deselect first.
- Copy states that Daily Boost never counts toward Weekly Progress.

### 4. Review setup

- Shows the real current plan day names, selected goal, selected schedule and workout-only weekly target.
- Provides focused Edit actions for goal and schedule.
- States that goal does not regenerate the split and weekdays do not bind workout names to dates.
- `Confirm setup` performs the atomic commit; no session or draft workout is created.

### 5. Starting-week receipt

- Hero: `Your starting week is ready`.
- Shows the existing starting split, schedule, goal and explicit assumptions.
- Explains that later progression suggestions require real saved workout evidence.
- `Review Day 1` opens the existing Day 1 detail without starting a workout.
- `Go to Home` leaves onboarding directly.
- `Adjust setup` returns to Review with the committed draft.

## Navigation and dock behavior

- Added one `onboarding` route inside the existing `NavHost`.
- Fresh/incomplete users start Onboarding; legacy/completed users start Home.
- A pending receipt starts/resumes Onboarding even after process death.
- Leaving onboarding removes it from the back stack.
- `Review Day 1` first establishes Home and then opens the existing Detail route, so Back returns Home.
- Onboarding is absent from the existing top-level dock destination list, so the dock is hidden automatically.
- The `GymBottomBar` implementation block is byte-identical to the pre-edit backup: 4,257 characters before
  and after.

## Visual, motion and accessibility behavior

- Onboarding-local plum/ink/true-black atmospheric canvas; separate clean Light palette.
- One restrained violet ambient source and static low-alpha grain.
- No gray slab, hard card shadow, nested card stack, continuous pulse, parallax or global glow.
- Strong 32sp Black question hierarchy, high-contrast body copy and a fixed full-width primary action.
- Choices communicate state through fill, border, icon and semantic description—not color alone.
- Directional 260–280 ms screen transitions, 0.98 press response and short selection crossfades.
- `ValueAnimator.areAnimatorsEnabled()` removes slide, scale and spring motion and uses a short dissolve/snap.
- Platform `performHapticFeedback` only: toggle events (API 34+) or `VIRTUAL_KEY` fallback, and `CONFIRM`
  after a successful final commit (API 30+) or fallback. No direct vibrator or ignore-setting flag.
- Headings receive focus on committed screen changes; progress exposes `Step N of 5`; validation uses a polite
  live region; rows expose RadioButton/Checkbox roles and selected state.
- Interactive targets are at least 48dp and content remains scrollable for narrow/large-text layouts.

## Test and build results

| Command | Result |
|---|---|
| `:app:compileDebugKotlin --console=plain` | PASS |
| `:app:testDebugUnitTest --console=plain` | PASS — 377 tests, 0 failures, 0 errors, 0 skipped |
| `:app:assembleDebug --console=plain` | PASS |
| `:app:assembleDebugAndroidTest --console=plain` | PASS |
| `:app:assembleRelease --console=plain` | PASS, including release lint vital |
| `:app:connectedDebugAndroidTest --console=plain` | NOT RUN — ADB device discovery hard-hung; see Runtime QA |
| `graphify update .` | PASS — generated graph refreshed after implementation |

Artifacts:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — 99,075,001 bytes.
- Release APK: `app/build/outputs/apk/release/app-release.apk` — 91,794,886 bytes.
- Instrumentation APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — 515,537 bytes.

Focused JVM tests cover exact schedule gating, plan-size 0/4 behavior, committed progress, back behavior,
versioned draft restoration/sanitization and forbidden claim boundaries. The compiled instrumentation suite
covers fresh draft isolation, process reload, version 2 atomic publication, legacy-v1 compatibility,
receipt cleanup and tracking-start preservation.

## Runtime QA and screenshots

Runtime QA could not begin. The first read-only device probe,
`adb devices -l`, produced no output and remained hung until the command was terminated. Per the task rule,
screenshot/device recovery stopped immediately after that hard error.

Therefore:

- No emulator state was cleared.
- No debug APK was installed.
- No connected instrumentation test was claimed.
- No launch, TalkBack, animator-scale, Light/Dark or physical-haptic result was claimed.
- No onboarding screenshots are attached. Compile/build evidence is not presented as visual QA.

## Protected-surface verification

Post-build SHA-256 comparison: **28/28 protected files unchanged**. This includes:

- `ProgressiveOverload.kt` and `ProgressiveOverloadEngineTest.kt`.
- All Room schema exports 2–9, entities, DAO and `AppDatabase.kt`.
- Catalog JSON and `CatalogSeed.kt`.
- `GymRepository.kt` and `WorkoutDraftRepository.kt`.
- Daily Boost domain logic.
- Muscle Heat and Muscle Recovery domain/scoring files.
- History V2 screen and ViewModel.
- `app/build.gradle.kts` and `gradle/libs.versions.toml`.

No dependency, Kotlin, Compose, Material, AGP, signing, schema or catalog change was made. Import/export,
save/draft, History grouping, progression behavior, recovery scoring and floating-navigation implementation
remain outside this diff.

## Known limitations

1. Runtime visual quality, device haptics, TalkBack focus, font scale 200%, reduced motion and process-death
   navigation still require a working emulator or physical device.
2. The instrumentation persistence tests are compiled but not executed because connected device discovery failed.
3. Goal remains a local user-global coaching preference; it does not generate or replace the plan.
4. Exact weekdays remain tracking/schedule evidence, not weekday-to-workout assignments.
5. No profile-aware plan generator exists; experience, equipment, duration and muscle priorities remain deferred.
