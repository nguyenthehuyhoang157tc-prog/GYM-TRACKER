# WEARABLE-1C — Real Prospective Capture Activation + Canonical Research Export + First Audit

## Executive result

**Final status: `READY_FOR_REAL_CAPTURE`.**

The consent → durable attempt → prospective frozen prediction → first-valid-set observation → saved or
confirmed-discarded outcome → Settings SAF export → WEARABLE-1B audit path is operational on the
disposable Pixel_8 QA environment. No canonical real export or naturally occurring real research record
was available, so **no real-data audit was completed and no MUSCLE-4B validation claim is made**.

The only implementation repair is a bounded evaluator-reporting fix: audit output now exposes the
predeclared input versions and capture aggregates required by WEARABLE-1C. It does not change eligibility,
metrics, protocol, capture semantics, Room v9, or any production engine.

## 1. Final status

`READY_FOR_REAL_CAPTURE`

This status is used because the isolated technical path passed, while the real-input condition was
`NO_REAL_INPUT_AVAILABLE`. Synthetic/controlled QA was not promoted to `COMPLETE_REAL_AUDIT`.

## 2. Real canonical export availability

**No real canonical export existed or was created.** Before controlled QA, the connected Pixel_8 had no
installed `com.hoctin.gymtracker` package, hence no on-device AURON database to inspect. A filename-only
search of the repository and the user's standard Downloads/Documents locations found zero canonical-name
candidates. This was a bounded search, not a claim about every byte on the machine.

The only export created in this task came from an explicitly disposable QA database. It was deleted from
emulator shared storage after verification; its private evidence copy remains outside the repository.

## 3. Consent and privacy boundary

- Capture is off by default and begins only after the Settings toggle writes a local consent row.
- Consent-off QA created zero rows in all eight research tables.
- The research export is explicitly user initiated and uses Android's document picker.
- No Health Connect, wearable, sleep, heart-rate, AI, backend, or app-network source participates.
- Raw DB snapshots, JSON exports, screenshots, evaluator artifacts, and exact timestamps stayed under a
  private `%TEMP%\auron-wearable1c-<timestamp>` evidence directory and were not added to source control.
- This report contains aggregates and hashes only; it omits raw workout rows, exercise-level values,
  subject IDs, and exact private training timestamps.

Privacy qualification: the app's own plan/history JSON backup excludes every research table, but
`AndroidManifest.xml` currently declares `android:allowBackup="true"` without table-aware backup rules.
Because research and production rows share one Room database file, strict exclusion from Android Auto
Backup is **not established**. The QA emulator's Backup Manager was disabled, so no QA upload was observed.

## 4. Subagent A — capture integrity findings

Verdict: **`PASS_WITH_LIMITATIONS`**; read-only, no emulator use, no edits.

Confirmed statically:

- explicit consent is checked both before building and again inside the freeze transaction;
- an attempt UUID is synchronously persisted before a prediction can freeze;
- the valid-set boundary is prospective and restored already-valid drafts are not backfilled;
- the unique attempt key plus idempotent insert permits at most one prediction per attempt;
- the first invalid → valid draft transition records the observation, and Save joins pending observation jobs;
- production session persistence commits before optional research outcome attachment;
- only the confirmed discard action marks `ABANDONED`.

Limitations identified:

- a rare post-production-save research failure can leave an `ACTIVE` outcome because no reconciliation job
  exists;
- consent is a mutable current-state row, not an append-only grant/revoke history;
- terminal attempt cleanup uses asynchronous `SharedPreferences.apply()`;
- the export cannot enumerate the total denominator of uncaptured workouts;
- an observation can be lost if the process dies before its asynchronous write and before Save joins it.

No static hard blocker was found.

## 5. Subagent B — export/evaluator findings

Verdict: **compatible, with explicit limitations**; read-only, no private export opened before the boundary,
no edits.

- The current serializer and V1 validator/legacy Kotlin-JVM payload codec are compatible.
- DAO ordering plus canonical sorted-object serialization make identical database state byte-deterministic.
- `CreateDocument` keeps destination choice with the user; the export operation itself is read-only.
- Null/unavailable/abstained values and all graph links remain distinct.
- The app's ordinary Settings plan/history backup omits research arrays.
- Android platform Auto Backup eligibility remains unresolved because `ALLOW_BACKUP` is present.
- `pendingResearchExport` is Compose `remember` state; process death while the system picker is open can
  lose the in-memory payload. Picker cancellation itself is safe and created no file in QA.
- The original evaluator validated the data but did not report all WEARABLE-1C-required input/capture
  aggregates. This bounded reporting defect was reproduced and repaired.
- Some consent/link/timestamp checks remain cross-check SQL responsibilities rather than evaluator checks.

## 6. External operational research

The operational conclusions use current official Android behavior:

- [`ActivityResultContracts.CreateDocument`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument)
  prompts the user for a destination and returns the created document's `content:` URI.
- [`SharedPreferences.Editor`](https://developer.android.com/reference/android/content/SharedPreferences.Editor)
  documents synchronous disk persistence for `commit()` and asynchronous persistence for `apply()`;
  force-stop evidence therefore supports the durable-attempt claim but not every possible crash/power-loss case.
- Room's [`@Transaction`](https://developer.android.com/reference/androidx/room/Transaction) and
  [`withTransaction`](https://developer.android.com/reference/kotlin/androidx/room/package-summary#withTransaction(androidx.room.RoomDatabase,kotlin.coroutines.SuspendFunction0))
  support atomic graph insertion and a consistent multi-table export snapshot.
- Android's [Auto Backup guide](https://developer.android.com/identity/data/autobackup) states that eligible
  app databases/shared preferences can be backed up unless disabled or scoped with platform rules. This is
  the basis for the privacy qualification above, not evidence that a backup occurred here.

Repository facts, platform facts, and AURON decisions are kept separate: the Android APIs provide the
mechanisms; AURON chooses the prospective boundary, frozen payload, status semantics, deterministic format,
and V1 evaluation rules.

## 7. Static capture-path audit

| Stage | Code evidence | Result |
|---|---|---|
| Settings consent | `SettingsViewModel.setRecoveryResearchEnabled` (99–109) | explicit local grant/revoke |
| Durable attempt | `WorkoutDraftRepository.loadOrCreateAttempt` (131–146) | UUID + timestamps synchronously `commit()` |
| Prospective init | `LogWorkoutViewModel` (183–262) | seed/catalog/history loaded, then freeze before a valid row |
| No hindsight backfill | `LogWorkoutViewModel` (233–247) | restored already-valid draft skips freeze |
| Frozen graph | `RecoveryResearchRepository.freezePredictionIfEligible` (63–83) | cutoff check + Room transaction |
| Idempotence | `RecoveryResearchDao` (47–72) | unique attempt + insert-ignore + transactional child rows |
| First valid set | `LogWorkoutViewModel.updateSet` (268–307) | only invalid → valid transition records observation |
| Production Save | `LogWorkoutViewModel.saveWorkout` (367–408) | waits observation jobs; production save is authoritative |
| Outcome attachment | `RecoveryResearchRepository.saveWorkoutAndCaptureOutcome` (115–136) | optional research transaction after production commit |
| Confirmed discard | `LogWorkoutViewModel.discardDraft` (350–363) | explicit call to `abandonAttempt` only |
| Abandoned semantics | `RecoveryResearchRepository.abandonAttempt` (139–160) | `USER_CONFIRMED_DISCARD`, non-evaluable |

Room v9 immutable-update triggers and unique indexes protect frozen predictions, one outcome per prediction,
attempt uniqueness, target ordinals, first-set observations, and exercise-outcome links. No current Home
readiness path updates historical prediction rows.

## 8. Static export/evaluator audit and bounded repair

Path verified:

`SettingsScreen` → `CreateDocument(application/json)` →
`RecoveryResearchRepository.exportDeterministicJson()` → transactional DAO reads →
`RecoveryResearchExport.encode()` → canonical JSON → WEARABLE-1B validator/audit artifacts.

The export uses deterministic DAO `ORDER BY` queries and recursively sorted object keys, preserves all eight
research arrays and versions, rejects non-finite numbers, and ends with one newline. Export is read-only.

Failing reproduction before repair: a valid audit report did not contain `input_versions`; querying the
predeclared field raised `KeyError: input_versions`. It also omitted consent count/state, set-observation
count, outcome-state distribution, and first-set-confidence distribution. Baseline tests were 50/50 green;
the new focused regression failed first, then passed after the reporting-only patch.

Repair:

- add those aggregates to `evaluation_report.json` and its Markdown mirror;
- add `AuditReportContractTests.test_report_includes_wearable_1c_required_aggregates`;
- leave validation, eligibility, metrics, protocol, seed, and conclusions unchanged.

A timestamped pre-edit source backup exists at
`D:\HOCTIN\GYM_TRACKER_WEARABLE1C_BACKUP_20260721_002312`.

## 9. Isolated QA scenarios

All scenarios used a fresh app install/disposable Room database and controlled values. They are pipeline QA,
not natural or biological observations.

| Scenario | Runtime result | Aggregate evidence |
|---|---|---|
| QA-0 consent off | Started a workout with research disabled | all 8 research-table counts = 0 |
| QA-1 prospective freeze | Granted consent, opened a fresh planned attempt before entering a valid set | 1 attempt, 1 prediction, 1 `ACTIVE`, 3 targets, 0 observations; hash captured |
| QA-4 process restart | Operator used ADB force-stop, relaunched, and reopened the same attempt before entering a valid set | before/after DB + WAL bytes, attempt/prediction/hash, and counts matched; prediction count still 1; target count still 3 |
| QA-2 first valid + Save | Entered one valid first set, saved through production UI | pre-save observation count 1; post-save outcome `PARTIAL`; 3 linked exercise outcomes; frozen hash unchanged |
| QA-3 confirmed discard | Operator backgrounded and force-stopped a second attempt, then reopened it and used the real confirmation dialog | before/after snapshots show navigation/force-stop left `ACTIVE`; confirmation produced exactly 1 `ABANDONED` with `USER_CONFIRMED_DISCARD` and `validForEvaluation=false` |
| QA-5 canonical export | Operator used Settings → Android SAF; first canceled, then exported twice | observed cancel created no file; retained exports were byte-identical and both accepted by audit mode |

Narrow DB snapshots, Settings/SAF screenshots, exports, and evaluator artifacts remain in the private
evidence directory. Exact operator commands remain in the Codex task execution transcript, not in the
repository. Cleanup was operator-observed and the final emulator checks showed the package absent and
Downloads empty.

## 10. QA versus real-data separation

- No production/real AURON database existed on the connected device before QA.
- QA began only after installing the current debug APK into a fresh app data directory.
- The QA export was labeled and stored as QA; it was never presented as a real canonical export.
- No real outcomes were intentionally completed, partially completed, abandoned, or manipulated.
- The emulator app/data and shared-storage exports were deleted after evidence capture.
- No QA row, DB, export, screenshot, or evaluator output is committed.

## 11. Real record availability

Condition: **`NO_REAL_INPUT_AVAILABLE`**.

Real captured attempts: unknown/none available for inspection. Real frozen predictions, linked outcomes,
ACTIVE outcomes, target rows, and evaluator eligibility: not measured. Therefore the first real WEARABLE-1B
audit was not run.

## 12. Canonical export path and SHA-256

Real export path/hash: **not applicable; no real export existed**.

Private QA path (redacted):
`%TEMP%\auron-wearable1c-<timestamp>\qa5\export1.json`

QA export SHA-256:
`926153c23d688d4659a6a2d8723aadd0fc404210cf282e7db22f2c1f0bf84730`

Second Settings/SAF export: same 22,707 bytes and same SHA-256.

## 13. QA DB/export count cross-check

The canonical QA JSON was compared with a force-stopped, read-only Room snapshot without editing the
persisted DB/WAL or export. Independent review additionally compared every exported row field-for-field;
all eight arrays matched, not only their counts. Read-only SQLite access can refresh transient SHM metadata,
so no SHM timestamp/hash is used as immutable evidence.

| Array/table | Room | Export |
|---|---:|---:|
| consent | 1 | 1 |
| predictions | 2 | 2 |
| prediction groups | 12 | 12 |
| prediction muscles | 9 | 9 |
| target exercises | 8 | 8 |
| set observations | 1 | 1 |
| outcomes | 2 | 2 |
| exercise outcomes | 3 | 3 |

Additional aggregates: 2 distinct attempts/predictions; outcomes = 1 `PARTIAL` + 1 `ABANDONED`; `ACTIVE=0`.
Every pair matched.

## 14. QA relationship and timestamp checks

SQLite integrity = `ok`; foreign-key violations = 0. All predeclared anomaly queries returned zero:

- duplicate attempts/predictions;
- predictions without exactly one outcome;
- attempt, observation, outcome, target, or exercise-outcome link mismatches;
- cross-prediction exercise links;
- prediction-generation/cutoff inversion;
- observation or terminal timestamp before freeze/cutoff;
- inconsistent terminal states;
- missing production session for a saved terminal outcome;
- invalid first-working-set link;
- duplicate canonical target within one prediction;
- ambiguous consent provenance in this one-grant QA dataset.

The first frozen payload hash remained unchanged across process restart, first-set observation, and outcome
attachment.

## 15. WEARABLE-1B audit command

No real audit command was run. The operational QA command was:

```powershell
python .\tools\recovery-evaluation\evaluate.py `
  --input "<private-qa-export.json>" `
  --mode audit `
  --output "<private-qa-audit-output>"
```

The same input/protocol/seed was run twice.

## 16. QA audit aggregates

- Canonical evaluator conclusion: `INSUFFICIENT_DATA`.
- Human interpretation: **`INSUFFICIENT_DATA_WITH_VALID_INPUT` for QA only**, not a real-data result.
- Protocol: `WEARABLE_1B_EVALUATION_PROTOCOL_V1`.
- Evaluator: `WEARABLE_1B_EVALUATOR_V1`.
- Input versions: contract `WEARABLE_1A_V1`, export schema 1, Room schema 9.
- Predictions 2; targets 8; linked outcomes 2; `ACTIVE=0`; set observations 1; consent `GRANTED=1`.
- Attempt classification: `TARGET_ATTEMPT_AMBIGUOUS=8`.
- Eligibility: `ABSTAINED=8`; primary eligible 0; secondary-only 0.
- Reasons: `NO_BASELINE_HISTORY=7`, `NO_PRIMARY_MUSCLE_MAPPING=1`.
- First-set confidence over linked exercise outcomes: `MODERATE=1`, `NONE=2`.
- Tracking type: `REPS_WEIGHT=8`; critical integrity findings: none.
- Evaluator data-quality verdict: `ACCEPTABLE_WITH_DECLARED_LIMITATIONS`.
- Capture denominator known: false; raw-history digest reconstructable from export alone: false.

These values diagnose a deliberately tiny fresh QA database and cannot estimate MUSCLE-4B performance.

## 17. Deterministic rerun hashes

Both QA audit runs produced byte-identical artifacts:

| Artifact | SHA-256 in run 1 = run 2 |
|---|---|
| `eligibility_ledger.json` | `f6e91cacaccdf3b85f5581cc7a494ab4dfc185e4bc666bf84434c578578efaf4` |
| `evaluation_report.json` | `5f35be1a009d2ccb0bf73a9ab2ab17fc347ae08c4460fffa2ebc15e04be15948` |
| `evaluation_report.md` | `cce3a2ad26ffb5fe6ca4f18c8c1957dca545594bd5562601093ca854950c4208` |
| `fold_manifest.json` | `37517e5f3dc66819f61f5a7bb8ace1921282415f10551d2defa5c3eb0985b570` |
| `metrics.json` | `c81301ef5bc1004d583f02184d68b9554e26b5c8fb6ff9c6de92eacf5a40a612` |

Input hash was
`926153c23d688d4659a6a2d8723aadd0fc404210cf282e7db22f2c1f0bf84730`; protocol hash was
`494ac18b8a92114091464ab1731776def680af40fa26e2cb27a4742748ea6998`.

## 18. Capture-quality diagnosis

The technical capture/export path is useful enough to begin natural prospective collection: consent-off
is inert, freeze is prospective/idempotent, attempt identity survives force-stop, Save/Discard links are
coherent, DB/export counts agree, and audit accepts the file.

No real capture-quality rate can be estimated. The QA exclusions are expected from a new empty-history
dataset and must not be used to tune Protocol V1. The future real audit must quantify unknown denominator,
ACTIVE rate, target-attempt coverage, first-set ambiguity, PO availability, mapping/tracking support, and
duplicate targets before any biological model assessment.

## 19. Tests, build, and connected test

- Python evaluator: **51 tests, 0 failures** (`Ran 51 tests ... OK`).
- Android unit tests: **348 tests across 21 suites, 0 failures/errors/skips** from JUnit XML.
- `:app:assembleDebug`: exit 0; APK = 97,724,248 bytes.
- `:app:connectedDebugAndroidTest`: exit 0 on runner `Pixel_8(AVD) - 17`; **2 tests, 0 failures**:
  - Room 8→9 migration preserves production rows and creates an empty research graph;
  - frozen prediction survives DB/repository recreation and links terminal outcome variants.
- Combined assemble/connected Gradle run: `BUILD SUCCESSFUL`.

Existing Android Gradle Plugin compile-SDK warning remained non-fatal.

## 20. Crash-log result

Final `adb logcat -b crash -d` output was empty, so no crash entry was retained for the exercised runtime
sequence. This does not prove absence of ANRs or that every device/failure mode is crash-free.

## 21. Files changed

Task delta:

1. `tools/recovery-evaluation/src/recovery_evaluation/evaluator.py` — report-only input/capture aggregates.
2. `tools/recovery-evaluation/tests/test_recovery_evaluation.py` — one focused regression test.
3. `docs/wearable1c_real_capture_activation_audit_result.md` — this redacted audit report.
4. `README.md` — added only after independent review.
5. `graphify-out/` generated graph/cache/manifest artifacts — refreshed by the repository-required
   code-only `graphify update .`; post-update search found zero raw QA DB/export references.

No Android production source, UI, schema, serializer, research contract, protocol, engine, or advice logic
was changed. The repository was already heavily dirty; the scoped source backup/no-index comparison was used
to isolate this task's delta. Twelve pre-existing untracked WEARABLE-1A QA DB/WAL/SHM files remain untouched;
no new WEARABLE-1C DB/export was placed in the repository.

## 22. Protected before/after hashes

Every protected file is byte-for-byte unchanged:

| Protected file | SHA-256 before = after |
|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` |
| `RecoveryResearchContract.kt` | `83EEACC0E28F194B0C3649684C80CF3500105C0B9337913C52F1D3B30B0D6724` |
| `RecoveryResearchExport.kt` | `10F777E20619AFB0F85A08B49516D0F576D9A53F27CD85DCA857D60F2CA9A172` |
| `WEARABLE_1B_EVALUATION_PROTOCOL_V1.json` | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` |
| protocol SHA sidecar | `D555EFC7E49A503FF0C773FC3FD5D8F553405B0F0490EBF68369698CFBE76266` |

## 23. Independent subagent C verdict

Verdict: **`PASS_WITH_LIMITATIONS`**; read-only, no edits, no emulator use.

Reviewer C independently opened the private QA evidence and challenged all ten required questions. It found:

- no synthetic export was presented as real and `NO_REAL_INPUT_AVAILABLE` was not presented as a real audit;
- QA-1/QA-2 establish freeze before the first valid-set observation and production set;
- every row in all eight export arrays matched the Room snapshot field-for-field;
- the force-stop evidence proves persisted equality for the exercised attempt, not every process-failure mode;
- no real outcome was fabricated and no raw WEARABLE-1C artifact entered the repository;
- evaluator compatibility was not confused with MUSCLE-4B validation;
- all seven protected hashes matched; and
- `READY_FOR_REAL_CAPTURE` matches the evidence.

Its limitations/corrections are incorporated here: action sequences are labeled operator-observed, the
crash claim excludes ANRs, the exact connected-test runner label is used, SHM metadata is not treated as
immutable evidence, and Android Auto Backup remains an explicit privacy qualification.

## 24. Known limitations

- No real input existed; no real audit or biological validation occurred.
- `android:allowBackup="true"` means strict exclusion of the shared Room DB from Android Auto Backup is not
  established. The app has no backend/upload path and QA backup was disabled, but “local-only” should not be
  read as a proven OS-backup exclusion.
- A production save followed by a research attachment failure may strand an `ACTIVE` outcome; no such
  failure occurred in QA and no reconciliation mechanism exists.
- Consent stores current state rather than an append-only history, so long-term grant-period provenance is
  limited.
- Total capture denominator is unknown by export design.
- First-set observation is asynchronous until Save joins it; a narrow abrupt-death window remains.
- Attempt cleanup and pending SAF payload use non-durable asynchronous/in-memory state respectively.
- ADB force-stop proved persistence for the exercised attempt but does not simulate every kernel, storage,
  low-memory, or power-loss condition.
- QA had only fresh empty history; its eligibility/exclusion distribution is intentionally non-generalizable.
- Pre-existing unrelated dirty/untracked repository content was preserved rather than cleaned destructively.

## 25. Exact next action

This is **not another implementation step**:

1. Before real capture, explicitly accept the current Android Auto Backup eligibility **or** complete a
   separate privacy-hardening task if “local-only” must mean never cloud/device-transfer backed up.
2. Install/open the intended AURON build and enable **Settings → Recovery research**.
3. Use AURON normally; do not change training behavior to satisfy the study.
4. Allow naturally occurring saved/partial linked outcomes to accumulate.
5. In Settings, choose **Export research JSON** and save the user-created file to an explicitly local provider.
6. Rerun WEARABLE-1C/WEARABLE-1B audit mode against that private canonical export.

Do not activate Health Connect or a wearable modifier yet. A separate privacy-hardening task is appropriate
if the requirement is strict exclusion from Android cloud/device-transfer backup rather than app-level
local-only capture.
