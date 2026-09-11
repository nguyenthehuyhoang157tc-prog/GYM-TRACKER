# WEARABLE-1A — Recovery Research Foundation — Final Report

**Status: COMPLETE.** AURON now freezes one local, versioned MUSCLE-4B baseline prediction for a
durable workout attempt before any valid working set can enter production history, then links later
completed, partial, or explicitly abandoned outcomes without mutating the frozen payload. Capture is
off by default and local-only. No wearable integration was added.

## 1. Backup

Created before the first edit:

`D:\HOCTIN\GYM TRACKER\_backup_wearable1a_20260714_120238`

The backup contains the pre-task source, tests, Room schemas, build configuration, and README.

## 2. Read-only subagent audit

Exactly one read-only subagent audited the requested paths and made no filesystem changes. Its distilled
findings determined the implementation boundary:

- Production `WorkoutSessionEntity` and `LoggedSetEntity` rows are created only by **Save workout**;
  the existing production `WorkoutSessionEntity.startedAtMillis` is therefore save-time, not a reliable
  workout-start timestamp.
- The safe prospective boundary is a fresh `LogWorkoutViewModel` initialization after day, plan,
  catalog, history, goal, and draft resolution, but before the first UI state is emitted and before a
  valid set can be saved. A resumed legacy draft that already contains a valid set is too late and must
  skip prediction capture rather than reconstruct hindsight.
- AURON has no warm-up flag. The defensible loaded-set rule is modal load, heaviest load on a frequency
  tie, then the earliest set at that load. The uncertainty must remain explicit.
- The Home lineage is saved history → Muscle Heat exposure/session pipeline → MUSCLE-4B recovery
  pipeline → Home readiness. Progressive Overload exposes public suggestion/progression APIs, so the
  research layer can snapshot their result without modifying the engine.
- The previous production database was Room v8, with no migration instrumentation infrastructure.
  Ordinary app import replaces production data, so research tables must have no foreign key to mutable
  production workout rows.
- Settings → Data is the narrowest safe surface for explicit local opt-in, separate export, and delete.

## 3. Research and platform findings

### Verified platform requirements

- Room requires an explicit migration path when an entity schema changes; exported schemas and
  `MigrationTestHelper` are the supported way to exercise a real prior schema and validate the final
  Room schema. Sources: [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions),
  [MigrationTestHelper](https://developer.android.com/reference/androidx/room/testing/MigrationTestHelper),
  and [database testing](https://developer.android.com/training/data-storage/room/testing-db).
- `ViewModel` memory does not survive system process death. Durable research identity and frozen data
  therefore require on-disk persistence; UI state restoration alone is insufficient. Source:
  [Save UI states](https://developer.android.com/topic/libraries/architecture/saving-states).
- The Storage Access Framework `ACTION_CREATE_DOCUMENT` flow lets the user explicitly choose the
  destination for a local export without exposing an internal database path. Source:
  [Create a new file](https://developer.android.com/training/data-storage/shared/documents-files#create-file).
- Temporal evaluation must train/compute on earlier observations and evaluate later observations.
  Randomly mixing future records into prediction input creates leakage. Rolling-origin evaluation is
  appropriate for WEARABLE-1B. Sources: [TimeSeriesSplit](https://scikit-learn.org/stable/modules/generated/sklearn.model_selection.TimeSeriesSplit.html)
  and Tashman, [Out-of-sample tests of forecasting accuracy](https://www.researchgate.net/publication/223319987_Out-of-sample_tests_of_forecasting_accuracy_An_analysis_and_review).

### Sound engineering decisions

- Use a durable local attempt UUID before freezing, a unique database index on that UUID, `INSERT
  IGNORE`, and a transactionally inserted prediction graph. Duplicate/recomposed/concurrent callers
  return the already-frozen record.
- Keep immutable predictions and mutable lifecycle outcomes in different tables. Database `BEFORE
  UPDATE` triggers enforce immutability below Kotlin/Room.
- Commit the production workout first, then capture optional research in a separate transaction.
  Research failure cannot roll back or change normal workout persistence.
- Keep the research export separate from ordinary backup/import, canonicalize key and row order, and
  omit a generated export timestamp so identical stored data produces byte-identical JSON.
- Use only internal research foreign keys. This preserves audit records when ordinary production
  backup/import replaces workout rows and avoids coupling research integrity to production deletion.

### AURON-specific assumptions

- A fresh explicit **Start workout** entry establishes `workoutCreatedAt` and `workoutStartedAt`; they
  are equal in v1 but stored separately because their semantics may diverge later.
- `firstWorkingSetLoggedAt` means the first time a draft row became syntactically valid in AURON, not
  the physical start of a set or a sensor-derived timestamp.
- No warm-up label exists. Loaded-set selection is therefore a moderate-confidence heuristic; metric
  tracking is low confidence. The system does not claim certainty it cannot observe.
- Suggested RIR is not exposed by the current PO contract, so it remains `null` and completeness is
  kept separate from abstention.

## 4. Exact schema, entities, keys, and indexes

Room is **v9**, research contract/schema is **WEARABLE_1A_V1 / 1**. Eight additive tables were created:

| Table | Primary/foreign keys | Unique indexes | Secondary indexes |
|---|---|---|---|
| `recovery_research_consent` | PK `subjectId` | — | `stateCode` |
| `recovery_research_predictions` | PK `predictionId`; `subjectId` → consent, cascade delete | `targetWorkoutAttemptId` | `subjectId`; `predictionGeneratedAtMillis`; `inputDataCutoffAtMillis`; (`recoveryModelVersion`, `predictionGeneratedAtMillis`) |
| `recovery_research_prediction_groups` | PK (`predictionId`, `groupCode`); prediction FK, cascade | PK | `groupCode` |
| `recovery_research_prediction_muscles` | PK (`predictionId`, `muscleCode`); prediction FK, cascade | PK | `muscleCode`; `lastTrainedAtMillis` |
| `recovery_research_target_exercises` | PK `targetExerciseId`; prediction FK, cascade | (`predictionId`, `targetOrdinal`) | `canonicalExerciseKey`; `planExerciseIdSnapshot`; `referenceSessionId` |
| `recovery_research_set_observations` | PK `setObservationId`; prediction FK, cascade | (`predictionId`, `planExerciseIdSnapshot`, `draftSetKey`) | `targetWorkoutAttemptId`; `canonicalExerciseKey`; `firstValidAtMillis` |
| `recovery_research_outcomes` | PK `outcomeId`; prediction FK, cascade | `predictionId`; `targetWorkoutAttemptId`; nullable `sessionId` | `statusCode`; `outcomeCapturedAtMillis` |
| `recovery_research_exercise_outcomes` | PK `exerciseOutcomeId`; outcome and target FKs, cascade | — | `outcomeId`; `targetExerciseId`; `canonicalExerciseKey`; `firstWorkingSetId` |

The frozen graph stores overall/group/muscle readiness, status/color codes, fast/slow residuals,
last-trained timestamps, plan/day/exercise snapshots, canonical identity, target muscles, typed and
reduced PO outputs, historical reference set/session, exact input session IDs/digest, cutoff, catalog
version, completeness, abstention, model versions, and a frozen payload SHA-256.

`BEFORE UPDATE ... RAISE(ABORT, 'immutable research payload')` triggers protect predictions, groups,
muscles, target snapshots, set observations, and exercise outcomes. Consent and the one lifecycle
outcome row remain intentionally mutable. Deleting the consent root through the explicit delete action
cascades through the entire isolated research graph. No destructive fallback exists.

## 5. Migration path and evidence

`AppDatabase.MIGRATION_8_9` executes only additive `CREATE TABLE`, `CREATE INDEX`, and immutable-trigger
statements. It is registered in the database builder and the exported schema is:

`app/schemas/com.hoctin.gymtracker.data.AppDatabase/9.json`

The device migration test opened the exported v8 schema, inserted representative production rows,
ran 8 → 9, and verified:

- session ID/title/custom title/date/readiness/duration survived;
- logged-set weight/reps/name/canonical key survived;
- all research tables existed and were empty;
- foreign-key check was clean;
- duplicate-attempt uniqueness and an immutable trigger were effective;
- Room's final schema validation succeeded.

## 6. Exact prediction lifecycle and timestamps

1. The explicit Log Workout route loads or creates a durable `WorkoutAttemptIdentity` in
   `WorkoutDraftRepository` using synchronous `SharedPreferences.commit()`.
2. `workoutCreatedAtMillis` records creation of that attempt; `workoutStartedAtMillis` records entry
   into that attempt. Both are persisted independently from draft JSON and survive navigation/restart.
3. Existing draft, day, exercises, goal, catalog identities, histories, tracking types, and profiles
   resolve. If the restored draft already contains a valid set and no prior prediction exists, capture
   is prospectively ineligible and skipped rather than backfilled. `LATE_EXISTING_VALID_DRAFT` remains
   a registry reason for a future audit/reconciliation record; v1 does not create a late prediction
   merely to persist that reason.
4. `inputDataCutoffAtMillis` is captured **before** the research history query. The DAO query includes
   only sessions with `startedAtMillis <= cutoff`, and the builder repeats that invariant with checks.
5. MUSCLE-4B and PO are computed through their existing public production APIs with `nowMillis = cutoff`.
   `predictionGeneratedAtMillis`/`frozenAtMillis` are captured after input retrieval and are constrained
   to be `>= cutoff`.
6. The complete prediction graph and initial `ACTIVE` outcome are inserted transactionally. Only then
   can normal UI interaction continue.
7. `firstValidSetObservedAtMillis` is the first invalid → valid transition for a stable draft-row key.
   On Save, `firstWorkingSetLoggedAtMillis` is derived from the selected persisted set and its matching
   observation. `workoutCompletedAtMillis` is Save time. `workoutAbandonedAtMillis` is explicit confirmed
   Discard time.
8. The existing production `WorkoutSessionEntity.startedAtMillis` semantics were not changed; it remains
   production save-time. Research timestamps are additive and do not silently reinterpret it.

No prediction is created by Home recomposition, passive readiness refresh, background work, or after a
legacy valid draft has already crossed the cutoff.

## 7. Freeze and idempotency mechanism

- Durable key: `targetWorkoutAttemptId` persisted before freeze.
- Fast path: DAO lookup returns an existing prediction unchanged.
- Database guard: unique attempt index plus `INSERT IGNORE`.
- Atomic graph: `@Transaction getOrInsertFrozen` inserts prediction, group, muscle, target, and initial
  outcome rows, then reloads the winner if another caller won the race.
- Immutable storage: SQL triggers reject every later update to frozen rows.
- Stable draft row identity: draft schema v3 persists `researchRowKey`; removal/reordering cannot make
  an ordinal point to a different set.
- Terminal identity cleanup occurs only after production Save or confirmed Discard, not on empty draft
  payload, recomposition, navigation, or process recreation.

## 8. Exact first-working-set rule

Registry code: `TRACKING_AWARE_FIRST_WORKING_SET_V1`.

- `REPS_WEIGHT` and `SKILL_FORM`: eligible sets require `reps > 0` and `weight >= 0`; choose the modal
  load, choose the heavier load when frequencies tie, then the earliest `(setNumber, id)` at that load.
  Rule `MODAL_LOAD_TIE_HEAVIEST_V1`, confidence `MODERATE`.
- `BODYWEIGHT_REPS`: first ordered set with `reps > 0`.
  Rule `FIRST_POSITIVE_REP_SET_V1`, confidence `MODERATE`.
- Time/distance/load metrics: first ordered set with every required metric valid (`duration > 0`,
  `distance > 0`, and/or load present according to tracking type).
  Rule `FIRST_TRACKING_VALID_SET_V1`, confidence `LOW`.
- Invalid/incomplete rows are not observed. Skipped/removed rows retain no outcome. Custom exercises use
  their stable canonical custom key. A selected persisted set must map back to its stable draft row;
  otherwise the outcome is retained but excluded as `OUTCOME_AMBIGUOUS`.

Because there is no warm-up flag, this is a versioned heuristic rather than an assertion that the first
visible row is a working set.

## 9. Completed, partial, and abandoned lifecycle

- Every frozen prediction begins with exactly one `ACTIVE` outcome.
- Save persists the production session first. Research then selects exercise outcomes, copies achieved
  load/reps/RIR/duration/distance, records set/session quality where available, computes suggested-minus-
  actual deltas, and links the production `sessionId`.
- Status is `COMPLETED` only when at least one exercise is evaluable and every non-abstained planned
  target reaches its snapshotted target-set count. Otherwise a saved session is `PARTIAL`.
- Confirmed Discard transitions `ACTIVE → ABANDONED`, reason `USER_CONFIRMED_DISCARD`, and is not called
  a recovery failure.
- Navigation, backgrounding, or process death do not infer abandonment. The row remains `ACTIVE`; the
  contract also defines `UNKNOWN_INTERRUPTED` for future explicit reconciliation.
- Transition SQL permits terminal attachment only while status is `ACTIVE`. Exercise outcomes are
  immutable `INSERT IGNORE` rows.

## 10. Leakage protections

- History query and builder assert every input session timestamp is `<= inputDataCutoffAtMillis`.
- `attempt.startedAt <= cutoff <= predictionGeneratedAt` is enforced.
- The prediction graph is committed before a first valid row can be persisted to production history.
- No completed/current attempt set is present in the cutoff history query.
- Input session IDs, maximum timestamp, canonical input digest, and frozen payload hash make the exact
  evidence auditable.
- Targets are copied as snapshots. Later plan edits cannot rewrite historical targets.
- Current Home readiness recomputation cannot reach research update APIs.
- Model versions are values inside each frozen row; a registry change affects future records only.
- Outcomes have separate tables and no update path to prediction payload columns.

## 11. Data quality and abstention

Completeness (`COMPLETE`, `PARTIAL`, `NONE`), confidence (`HIGH`, `MODERATE`, `LOW`, `NONE`), and
abstention are separate fields. Supported machine-readable reasons are:

`NO_VALID_TARGET_EXERCISE`, `NO_BASELINE_HISTORY`, `INSUFFICIENT_READINESS_INPUT`,
`NO_PO_SUGGESTION`, `NO_VALID_FIRST_WORKING_SET`, `SESSION_INTERRUPTED`,
`USER_CONFIRMED_DISCARD`, `OUTCOME_AMBIGUOUS`, `TRACKING_TYPE_UNSUPPORTED_FOR_METRIC`,
`TIMESTAMP_INCONSISTENT`, `DUPLICATE_ATTEMPT_PREVENTED`, `LATE_EXISTING_VALID_DRAFT`,
`NO_PRIMARY_MUSCLE_MAPPING`, and `SUGGESTED_RIR_UNAVAILABLE`.

Null means unavailable/not emitted by the source contract. `abstained=true` plus a reason means the row
must not be evaluated. Missing suggested RIR therefore remains null and does not masquerade as RIR 0.

## 12. Explicit model/version registry

| Component | Stored identifier |
|---|---|
| Prediction source | `PRE_FIRST_VALID_WORKING_SET` |
| Recovery | `MUSCLE_4B_BASELINE` |
| Muscle Heat | `MUSCLE_HEAT_1_STIMULUS_QUALITY` |
| Progressive Overload | `PROGRESSIVE_OVERLOAD_V3_11D` |
| Research contract | `WEARABLE_1A_V1` |
| Research schema | `1` |
| Room schema | `9` |
| First-set contract | `TRACKING_AWARE_FIRST_WORKING_SET_V1` |
| Export schema | `1` |

Catalog content version is also stored per prediction. These identifiers are independent of app
`versionName`.

## 13. Process-death persistence

The attempt UUID/timestamps and stable draft-row keys are stored in SharedPreferences; the frozen graph
and outcomes are stored in Room. ViewModel/SavedStateHandle is not the source of truth.

Evidence levels are deliberately separated:

- **Instrumentation/device database recreation:** close DB, rebuild database/repository objects, reload
  the same SharedPreferences attempt, and verify the exact prediction ID and payload hash with count 1.
  The same test then links completed, partial, and abandoned outcomes to their original predictions.
- **Real Pixel 8 force-stop/relaunch before any set:** prediction
  `9fc6fd3a-3137-40e7-80b1-aaa4de05b6f7`, attempt
  `495124ba-5def-42b6-bdcb-e2f3d10ee16b`, payload hash
  `e60124190bd9a570131d376a5472bdf205f0a8a74c7f88053f3b25b9d3d7655f` remained exact and the
  prediction count remained 1 after force-stop, relaunch, and reopening the workout.
- A real device kill **after** a valid set followed by finish/abandon was not completed; that part is
  supported by instrumentation persistence/terminal-lifecycle evidence only.

## 14. Research export

Settings exposes a separate `CreateDocument("application/json")` action. The format is one canonical
JSON document because the dataset is a small linked relational graph and arrays retain FK relationships
cleanly. Object keys are lexically sorted; DAO rows have explicit stable ordering; no `exportedAt` value
is injected. The same database state therefore produces byte-identical UTF-8 plus a final newline.

Timestamps are Unix epoch milliseconds (absolute UTC instants; no device-local timezone conversion).
Nulls remain JSON `null`; completeness and abstention remain explicit fields. There is no database path,
secret, health/wearable data, or automatic upload.

```json
{
  "contractVersion": "WEARABLE_1A_V1",
  "exportSchemaVersion": 1,
  "roomSchemaVersion": 9,
  "predictions": [{"predictionId": "...", "inputDataCutoffAtMillis": 1784008613519, "payloadSha256": "..."}],
  "predictionGroups": [],
  "predictionMuscles": [],
  "targetExercises": [],
  "setObservations": [],
  "outcomes": [{"predictionId": "...", "statusCode": "ACTIVE"}],
  "exerciseOutcomes": []
}
```

Ordinary backup/export is unchanged and does not silently include research. Research import is
intentionally unsupported; exported predictions can never be merged into live production advice.

## 15. Files changed

New production research package:

- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchEntities.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchDao.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchSchema.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchContract.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/research/RecoveryResearchExport.kt`

Additive integration:

- `app/src/main/java/com/hoctin/gymtracker/data/AppDatabase.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/data/WorkoutDraftRepository.kt`
- `app/src/main/java/com/hoctin/gymtracker/GymTrackerApplication.kt`
- `app/src/main/java/com/hoctin/gymtracker/MainActivity.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/GymTrackerApp.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/hoctin/gymtracker/ui/settings/SettingsScreen.kt`
- `app/build.gradle.kts`
- `app/schemas/com.hoctin.gymtracker.data.AppDatabase/9.json`

Tests/docs:

- `app/src/test/java/com/hoctin/gymtracker/data/research/RecoveryResearchContractTest.kt`
- `app/src/androidTest/java/com/hoctin/gymtracker/data/AppDatabaseMigrationTest.kt`
- `app/src/androidTest/java/com/hoctin/gymtracker/data/research/RecoveryResearchRepositoryTest.kt`
- `README.md`
- `docs/wearable1a_recovery_research_foundation_result.md`

Knowledge-graph maintenance required by `AGENTS.md`:

- `.graphifyignore` (excludes timestamped full-code backups and WEARABLE-1A QA artifacts from duplicate
  graph nodes)
- `graphify-out/` regenerated by the AST-only `graphify update .` path

## 16. Tests and build

Final combined command:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug --console=plain
```

Result: **BUILD SUCCESSFUL**.

- JVM: **348 tests, 0 failures, 0 errors, 0 skipped** across 21 suites.
- Connected Pixel_8(AVD) runner: **2 instrumentation tests, 0 failures/errors/skips**:
  - `migrate8To9_preservesProductionRows_andCreatesEmptyValidatedResearchGraph`
  - `frozenPrediction_survivesDatabaseAndRepositoryRecreation_andLinksCompletedPartialAbandoned`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, **97,909,432 bytes** (about 93.37 MiB) in
  the final post-documentation build.
- Only the existing AGP warning remained: AGP 8.5.2 is tested through compileSdk 34 while this project
  uses compileSdk 35. It did not fail compilation or tests.
- `graphify update .` completed after excluding duplicate backup trees: **3,244 nodes, 6,127 edges,
  221 communities**. A focused graph query returned the new DAO/repository/contract/test/report lineage.

Unit coverage includes snapshot/hash/version persistence, cutoff rejection, first-set rules, delta
semantics, null vs unavailable vs abstained, terminal status semantics, and deterministic export.
Repository instrumentation additionally covers repeated/concurrent freeze, immutable SQL rejection,
plan mutation isolation, DB/repository recreation, and all three terminal outcomes.

## 17. Device/runtime evidence

Runtime target: `emulator-5554`, Pixel 8 AVD.

- Settings rendered the minimal local research card, off-by-default toggle, disclosure, separate export,
  and explicit delete action: `qa-wearable1a-settings-research-card.png`.
- A real planned workout froze a v9 prediction before any set: `qa-wearable1a-log-real.png` and
  `qa-wearable1a-realfrozen.db`.
- After actual force-stop/relaunch/reopen, the DB snapshot `qa-wearable1a-resumed.db` contained the exact
  same prediction/attempt/hash and one row, proving real pre-set process persistence.
- The SAF export entry point rendered. Manual file-picker completion was not claimed; deterministic file
  content and links were inspected by instrumentation.
- Completed/partial/abandoned post-set linkage and plan/no-leakage assertions were verified by
  instrumentation, not overstated as manually completed runtime scenarios.

## 18. Frozen-engine hash proof

| Protected file | Before SHA-256 | After SHA-256 | Result |
|---|---|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` | same | unchanged |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` | same | unchanged |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` | same | unchanged |

MUSCLE-4B equations/constants/status thresholds, Muscle Heat scoring, and Progressive Overload behavior
remain byte-for-byte unchanged.

## 19. Known limitations

- No warm-up flag exists; loaded/bodyweight selection is moderate confidence and time/distance selection
  is low confidence.
- `firstWorkingSetLoggedAt` is an app observation/log timestamp, not physical set-start time.
- A process interruption stays `ACTIVE`; v1 does not infer crash, fatigue, or abandonment reasons.
- Suggested RIR is unavailable from the current PO API and is stored as null.
- Optional research outcome capture intentionally cannot roll back production Save. If that separate
  transaction fails after production commits, v1 has no automatic reconciliation worker.
- Real runtime process-kill evidence covers the pre-set freeze/resume boundary. Post-set finish/abandon
  process recreation was verified in instrumentation, not manually on the emulator.
- Research import is intentionally absent.
- There is no wearable, sleep, HR, HRV, Health Connect, candidate readiness, AI/ML, backend, or upload.
- This establishes prospective measurement integrity; it does not yet validate physiological predictive
  accuracy and does not treat soreness or abandonment as ground truth.

## 20. Exact next recommendation

**WEARABLE-1B only** — use the frozen prospective records to implement the pre-declared evaluation and
rolling-origin analysis layer. Do not let wearable/context data influence readiness until that task has
demonstrated an out-of-sample improvement over `MUSCLE_4B_BASELINE`.

Stopping here per the task boundary; no Health Connect or scope expansion was performed.
