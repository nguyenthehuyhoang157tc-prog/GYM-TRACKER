# PLAN-V3A — Saved Workout Persistence Contract — Result

Date: **2026-08-12**  
Project: `D:\HOCTIN\GYM TRACKER`  
Scope: persistence/domain foundation only; **no PLAN-V3 visual redesign**

## 1. Outcome

PLAN-V3A is complete. AURON now has a real, isolated persistence contract for Saved One-Off Workouts
without representing them as Routine days, drafts, negative ids or preferences.

The important product split is enforced at the data boundary:

| Behavior | Routine | Saved One-Off |
|---|---:|---:|
| Stored in Plan / `workout_days` | Yes | No |
| Stored as reusable Saved Workout template | No | Yes |
| Eligible for Home Hero / selected Routine | Yes | No |
| Eligible for Routine Skip / auto-advance | Yes | No |
| Weekly Progress | Yes | No |
| Active Streak after a real completed session | Yes | Yes |
| History V2 and canonical exercise evidence | Yes | Yes |
| Muscle Recovery from real logged work | Yes | Yes |
| Recovery Research outcome capture today | Existing Routine contract | Pending typed Research owner contract |
| Template deletion removes completed History | N/A | **No** |

## 2. Backup and post-wearable baseline

Fresh pre-edit backup:

`D:\HOCTIN\GYM_TRACKER_PLANV3A_BACKUP_20260812_181501`

Contents: `app_src`, `docs`, `README.md`, and the pre-task `room_schemas` export. The backup contains
**1,448 files / 117,205,416 bytes**. Signing files were not read or copied.

The final post-WEARABLE-2A baseline was re-read rather than inferred from the previous audit:

- Room was still **v10** with exported `10.json`; the stale audit assumption happened to match the
  final code, but was independently re-verified.
- v10 already contained the complete additive wearable graph from `MIGRATION_9_10`.
- no Saved Workout entity/table/repository existed;
- `WorkoutSessionEntity` had no typed source;
- `LogWorkoutViewModel` and `RecoveryResearchRepository.saveWorkoutAndCaptureOutcome(...)` were still
  deliberately Routine-shaped;
- Home Consistency used all sessions for both weekly completion and activity continuity;
- local JSON backup was schemaVersion 5 and contained Plan + History, not Saved Workout templates.

The previous Phase-A proposal was therefore retained conceptually but adjusted against the actual
v10 schema. No completed wearable table, field, trigger, source or heuristic was replaced.

## 3. Room version and schema additions

Room changed **v10 -> v11** through `MIGRATION_10_11`.

### `saved_workouts`

- `id` INTEGER primary key, auto-generated
- `name` TEXT
- `createdAtMillis` INTEGER
- `updatedAtMillis` INTEGER
- `sortOrder` INTEGER

### `saved_workout_exercises`

- `id` INTEGER primary key, auto-generated
- `savedWorkoutId` INTEGER
- nullable `catalogSlug`
- `canonicalExerciseKey`
- `exerciseNameSnapshot`
- `targetSets`
- `targetMinReps`
- `targetMaxReps`
- `sortOrder`
- FK to `saved_workouts(id)` with `ON DELETE CASCADE`
- indices on `savedWorkoutId` and `canonicalExerciseKey`

The prescription stores only a reference/snapshot contract. It does not copy catalog rows. `0/0`
rep sentinels remain intact for time/distance tracking types.

### `workout_sessions` additions

- non-null `sourceType` TEXT, default `ROUTINE`
- nullable `savedWorkoutId` INTEGER
- indices on both fields

`savedWorkoutId` intentionally has **no foreign key**. It is historical provenance and may outlive a
deleted template. Existing v10 rows receive `ROUTINE` automatically.

Exported Room schema:

`app/schemas/com.hoctin.gymtracker.data.AppDatabase/11.json`

- version: 11
- identity hash: `881e2f1165035925aefc8f7663f7d58d`
- entities: 22

The migration performs only `CREATE TABLE`, `CREATE INDEX`, and `ALTER TABLE ADD COLUMN`. There is no
`DROP`, destructive fallback or rewrite of existing Plan, History, canonical-key, Research or wearable
data.

## 4. Repository and domain API

`SavedWorkoutRepository` owns template persistence and exposes:

- `observeAll()` and `observeList()`;
- observe/get one full workout with ordered exercises;
- create;
- rename;
- replace exercises;
- add/remove an exercise;
- reorder exercises with exact-membership validation;
- delete template;
- completion count derived from immutable `SAVED_ONE_OFF` session rows.

`SavedWorkoutExerciseDraft` accepts catalog/custom snapshot identity and the target prescription.
All writes are transactional. Reorder must provide every persisted exercise exactly once and cannot
move an exercise between templates.

`GymTrackerApplication` exposes the repository for PLAN-V3B. No screen or navigation route consumes it
yet.

## 5. Typed session-source design

`WorkoutSessionSource` is the persisted enum:

- `ROUTINE`
- `SAVED_ONE_OFF`

`WorkoutLaunchSource` is the typed domain boundary:

- `Routine(day: WorkoutDayEntity)`
- `SavedOneOff(workout: SavedWorkoutEntity)`

The existing Routine save delegates through the typed core and retains its current behavior. The new
`GymRepository.saveSavedOneOffWorkoutSession(...)` path:

- stores `dayId = null`;
- stores Saved Workout name/source/template provenance;
- writes ordinary `WorkoutSessionEntity` + `LoggedSetEntity` history;
- keeps `exerciseId = null` because a template-exercise id is not a Routine exercise id;
- preserves `canonicalExerciseKey` and name snapshots for cross-day evidence and PR/history queries;
- does not touch selected Routine preferences or call Routine auto-advance.

PLAN-V3B must use this path for `Start Session`; it must not route a one-off template through the
existing Routine-only `LogWorkoutViewModel` save call.

## 6. Weekly Progress, Active Streak, History and recovery

`WorkoutSessionEntity.countsTowardWeeklyProgress()` accepts only `ROUTINE`.

Home maps this flag into `ConsistencyWorkoutEvidence`. The calculator now uses:

- all valid workout dates for Active Streak / Workout+Boost union;
- only Routine workout dates for weekly completion and completed schedule cells.

Therefore a Saved One-Off session on a planned day is workout activity evidence but does not mark that
Routine schedule obligation completed. Focused unit coverage proves Weekly remains 0 while current
Active Streak becomes 1.

History and Muscle Recovery already consume the complete Room session stream. One-Off sessions use the
same immutable session/set snapshots, so they appear in History and contribute real muscle evidence
without a special-case scoring change. Canonical evidence is verified after template deletion.

## 7. Recovery Research and wearable result

No file under either of these folders changed relative to the pre-edit backup:

- `data/research`
- `data/wearable`

The final Research API still requires a real `WorkoutDayEntity` and a Routine prediction attempt.
PLAN-V3A did not pass a fake day and did not alter its heuristics.

Exact owner contract still needed:

> PLAN-V3A now has typed Saved One-Off sessions and requires Recovery Research to define whether
> `SavedOneOff` captures an observation or explicitly abstains with a source-specific reason.

Until that decision, One-Off production saves are authoritative normal sessions but do not create a
Recovery Research outcome. This is an explicit abstention-by-boundary, not false Routine identity.
It does not prevent the session from affecting Muscle Recovery, History or Active Streak.

## 8. Import/export contract

Local backup advances **schemaVersion 5 -> 6**.

New optional top-level arrays:

- `savedWorkouts`
- `savedWorkoutExercises`

New session fields:

- `sourceType`
- `savedWorkoutId`

Compatibility behavior:

- old sessions with no `sourceType` import as `ROUTINE`;
- old backups with no Saved Workout arrays still import and preserve local Saved Workout templates;
- new backups require both Saved Workout arrays together and validate every exercise parent id;
- new backups replace Saved Workout definitions exactly, including order and prescriptions;
- History snapshots are not rewritten;
- catalog rows are not exported as Saved Workout data;
- the AI Import prompt omits the arrays unless the source explicitly describes reusable one-off
  templates. Empty arrays are documented as an intentional delete-all replacement.

## 9. Files changed

Foundation/source:

- `app/src/main/java/com/hoctin/gymtracker/data/AppDatabase.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/Entities.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/SavedWorkoutRepository.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/data/WorkoutLaunchSource.kt` (new)
- `app/src/main/java/com/hoctin/gymtracker/GymTrackerApplication.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/ConsistencyModels.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/AiImportPrompt.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/ProfileDataResearchResourcesScreens.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/onboarding/OnboardingModels.kt`

Schema/tests:

- `app/schemas/com.hoctin.gymtracker.data.AppDatabase/11.json` (new)
- `app/src/androidTest/java/com/hoctin/gymtracker/data/AppDatabaseMigrationTest.kt`
- `app/src/androidTest/java/com/hoctin/gymtracker/data/SavedWorkoutRepositoryTest.kt` (new)
- `app/src/test/java/com/hoctin/gymtracker/data/SavedWorkoutFoundationTest.kt` (new)
- `app/src/test/java/com/hoctin/gymtracker/ui/home/ConsistencyCalculatorTest.kt`
- `app/src/test/java/com/hoctin/gymtracker/ui/onboarding/OnboardingCopyContractTest.kt`

Documentation:

- `README.md`
- this report

No PLAN-V3 screen, Plan card, builder, swipe gesture, Home Hero composition or Generate Workout UI was
implemented.

## 10. Tests and builds

Final verification after the production import-provenance correction:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.hoctin.gymtracker.data.AppDatabaseMigrationTest,com.hoctin.gymtracker.data.SavedWorkoutRepositoryTest,com.hoctin.gymtracker.data.research.RecoveryResearchRepositoryTest,com.hoctin.gymtracker.data.wearable.WearableShadowRepositoryTest" `
  --console=plain
```

Result:

- JVM: **423 tests, 0 failures, 0 errors, 0 skipped**;
- Progression Engine subset: **152 tests, 0 failures**; implementation and test files are byte-identical
  to the pre-edit backup;
- instrumentation: **9 tests, 0 failures** on the connected Pixel 8 AVD (API 37 / Android 17);
- includes v8->v9, v9->v10 and actual v10->v11 migration validation;
- Saved Workout CRUD, reorder, non-rep prescription, completion count, canonical evidence, deletion
  retention, new export round-trip (including typed session provenance and canonical evidence) and
  old-import preservation passed;
- Recovery Research repository and Wearable Shadow repository suites passed unchanged.

Final builds:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease --console=plain
```

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
  - 101,972,269 bytes
  - SHA-256 `0ACCBCF01375E5CAE6999AECD12ADBEAE9DB82DF018029B0013A417ECA80F9E2`
- Release: `app/build/outputs/apk/release/app-release.apk`
  - 93,828,390 bytes
  - SHA-256 `02647023875506B6719CCB71EACC5B3651EA88166E3C4855E0FADF609E003F21`

Both builds passed. Release used the existing conditional signing configuration; no signing secret was
opened or printed. `apksigner verify --verbose` passed with APK Signature Scheme v2 and one signer.

### QA corrections found during implementation

- The first raw migration cascade assertion exposed that `MigrationTestHelper` had foreign keys off;
  the test now explicitly enables the same FK behavior Room uses before testing `ON DELETE CASCADE`.
- A release-v10/debug-test signature conflict blocked a wearable test run before any tests executed;
  release upgrade QA was completed first, then the disposable QA package was removed and the debug
  instrumentation suites passed.
- The final copy contract caught ambiguous Weekly wording; it was rewritten to state separately that
  Saved One-Off and Daily Boost never count.
- The final repository audit caught non-rep prescriptions being coerced from `0/0` to `1/1`; this was
  fixed and covered before final validation.
- The final production-path audit found that the reusable session decoder handled `sourceType` and
  `savedWorkoutId`, while `GymRepository.importJson()` still constructed sessions through its older
  inline path. Production import now restores both fields, and the instrumentation round-trip proves
  `SAVED_ONE_OFF`, nullable `dayId`, template association and canonical evidence survive export/import.

## 11. Real release upgrade QA

The preserved WEARABLE-2A release baseline was verified by its published hash:

- v10 APK SHA-256: `7ECD87E86D9D7849125E7A09E166C69E332FF34F086D86786D0A2E0ABB094EEC`

On the disposable Pixel 8 AVD:

1. installed and launched v10;
2. recorded `firstInstallTime=2026-08-12 20:16:34` and live PID `10781`;
3. installed the final v11 release using `adb install -r`;
4. confirmed `firstInstallTime` remained `20:16:34` while `lastUpdateTime` became `20:16:54`;
5. confirmed final v11 PID `10919`;
6. crash buffer was empty;
7. scans for `FATAL EXCEPTION`, Room verification failure and missing v10->v11 migration were empty.

Representative row preservation is proven by `AppDatabaseMigrationTest`; the release upgrade proves
the actual signed application opens through the migration path. The two evidence types are not
substituted for each other.

`graphify update .` completed after final source changes: **6,840 nodes, 11,653 edges and 475
communities**. HTML visualization was intentionally skipped by Graphify because the graph exceeds its
5,000-node visualization limit; `graph.json` and `GRAPH_REPORT.md` were updated successfully.

## 12. Protected-surface verification

SHA-256 comparison against the new pre-edit backup shows these files byte-identical:

- `ProgressiveOverload.kt`
- `ProgressiveOverloadEngineTest.kt`
- `RecoveryResearchRepository.kt`
- `RecoveryResearchContract.kt`
- `RecoveryResearchExport.kt`
- `WearableShadowRepository.kt`
- `WearableContextSelector.kt`
- `GymTrackerApp.kt`
- `HomeScreen.kt`
- `LogWorkoutViewModel.kt`
- `WorkoutDraftRepository.kt`

Folder-level comparison found **0 changed files** under `data/research` and **0 changed files** under
`data/wearable`. Catalog source/assets, History grouping, floating navigation and progression algorithms
were not modified by PLAN-V3A.

## 13. Rollback strategy

The v11 migration is additive and cannot be safely reversed in place without losing Saved Workout
templates/source metadata. The safe rollback plan is:

1. keep/install the v11 app when preserving v11 user data;
2. export schemaVersion 6 backup before any product rollback;
3. restore source from the timestamped pre-edit backup only for development rollback;
4. do not install an older Room-v10 binary over a database already opened by v11;
5. if a release rollback is unavoidable, ship an explicitly designed forward migration in a higher
   database version rather than deleting v11 tables or using destructive fallback.

Deleting a Saved Workout template through the v11 repository is safe and leaves History intact; that
is product deletion, not a schema rollback.

## 14. PLAN-V3B gate

**PLAN-V3B = GO WITH ONE EXPLICIT RESEARCH BOUNDARY.**

It may now implement:

- Plan -> Your Workouts list/detail;
- builder/edit/reorder/delete;
- Start Session through `saveSavedOneOffWorkoutSession(...)`;
- truthful History/Active Streak/Muscle Recovery behavior;
- strict exclusion from Home Hero, Routine rotation and Weekly Progress.

It must not:

- reuse `WorkoutDayEntity` or the Routine-only Log save route for one-offs;
- invent a Recovery Research observation;
- alter Research heuristics while building UI.

If one-off sessions must participate in Recovery Research, obtain the typed source/abstention contract
from that owner first. PLAN-V3B is not started automatically by this task.
