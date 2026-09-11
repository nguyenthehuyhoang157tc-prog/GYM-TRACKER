# WEARABLE-1B — Frozen Baseline Prospective Evaluation + Rolling-Origin Benchmark

## Executive result

WEARABLE-1B is complete as a standalone, offline, deterministic research evaluator. It reads only an
immutable WEARABLE-1A JSON export and does not depend on Android, Room, current app state, a backend,
Health Connect, or a network.

The evaluator itself is verified. The repository contains no safely available, consented canonical
WEARABLE-1A JSON export, so no real private outcome values were opened and no real-data validation
claim is made. The honest current real-data status is **`INSUFFICIENT_DATA` (no evaluable export was
available; real audit was not run)**. Synthetic conclusions below prove the evaluator's gates—not
MUSCLE-4B's biological validity.

No wearable modifier is active. MUSCLE-4B, Muscle Heat, Progressive Overload, WEARABLE-1A capture,
Room schema, and Android UI are unchanged.

## 1. Backup

Pre-edit scoped backup:

`D:\HOCTIN\GYM TRACKER\_backup_wearable1b_20260717_205744`

It contains the current README and all five protected production files before WEARABLE-1B work.

## 2. Read-only subagent findings

Exactly one High-effort, read-only subagent audited WEARABLE-1A and modified no files. Its distilled
decision was that there is no unconditional hard stop if V1 is kept strict:

- defensible primary evaluation exists for `REPS_WEIGHT`, `MODERATE` first-set confidence, supported
  V1 rules, terminal completed/evaluable-partial outcomes, and strict target-attempt matching;
- frozen `primaryReadinessMin`, PO target/history snapshots, canonical identity, reference timestamps,
  first-set observations, exercise outcomes, and payload hashes are present;
- `SKILL_FORM`, bodyweight difficulty, and time/distance targets are not structured enough for V1
  primary PO-target attainment;
- the export cannot independently reconstruct raw historical inputs behind `inputDigestSha256` or
  enumerate every workout that was never captured;
- duplicate canonical targets in a workout must abstain because the same saved sets could otherwise be
  attached twice;
- Kotlin payload SHA uses data-class `toString()` and JVM `Double.toString()` semantics, not canonical
  JSON hashing.

Those findings directly set the implemented V1 boundary.

## 3. External research findings

Methodological requirements were separated from AURON-specific engineering choices:

- [TRIPOD+AI](https://www.bmj.com/content/385/bmj-2023-078378) supports transparent reporting of the
  target population, outcome, missingness, validation design, performance, uncertainty, and limits. It
  is a reporting standard, not proof that a model is valid.
- Tashman's rolling-origin review supports genuine out-of-sample evaluation over time instead of
  shuffled random folds: [doi:10.1016/S0169-2070(00)00065-0](https://doi.org/10.1016/S0169-2070(00)00065-0).
- Steyerberg et al. distinguish overall performance, discrimination, and calibration; no one number is
  sufficient: [doi:10.1097/EDE.0b013e3181c30fb2](https://doi.org/10.1097/EDE.0b013e3181c30fb2).
- Added-value literature supports comparing the same baseline model with and without the new marker,
  using complementary metrics and out-of-sample validation rather than only a change in AUROC:
  [BMC Medicine review](https://pmc.ncbi.nlm.nih.gov/articles/PMC6460632/).
- The classic external-validation heuristic of about 100 outcome and 100 non-outcome events motivated
  a conservative floor, while later sample-size work emphasizes that required size depends on the
  estimand and precision: [Vergouwe et al.](https://doi.org/10.1016/j.jclinepi.2004.08.017),
  [Riley et al.](https://ora.ox.ac.uk/objects/uuid:eb9a1138-a73e-4f30-85ba-9633521bf093).
- Moving-block bootstrap preserves local time dependence better than resampling exercise rows as if
  independent: [Künsch](https://doi.org/10.1214/aos/1176347265).
- [PROBAST+AI](https://www.bmj.com/content/388/bmj-2024-082505) reinforces that small samples,
  overfitting, weak missing-data handling, and inadequate performance assessment create high risk of
  bias.

Evidence-supported requirements implemented here are temporal separation, out-of-sample comparison,
calibration plus discrimination, dependence-aware uncertainty, explicit missingness/abstention, and
transparent limitations. Exact numeric gates, load tolerance, feature list, regularization, seed, and
bootstrap length are versioned AURON engineering decisions and still require future empirical review.

## 4. Methodology decisions

- Freeze a machine-readable protocol before any real outcome analysis.
- Keep one target exercise as the primary unit but one workout as the indivisible temporal/dependence
  block.
- Require that every training outcome was captured by the next test prediction's cutoff—not merely
  that its prediction cutoff sorts earlier.
- Use the same eligible rows for all six comparators.
- Fit preprocessing and probability mapping only inside historical training folds.
- Treat readiness as ordinal/model input, never as a probability.
- Prefer abstention to cross-tracking coercion or guessed mappings.
- Produce descriptive audit output below sample gates and suppress inferential precision.

## 5. Protocol path, version, and hash

- Protocol: `tools/recovery-evaluation/protocol/WEARABLE_1B_EVALUATION_PROTOCOL_V1.json`
- Version: `WEARABLE_1B_EVALUATION_PROTOCOL_V1`
- Evaluator: `WEARABLE_1B_EVALUATOR_V1`
- SHA-256: `494ac18b8a92114091464ab1731776def680af40fa26e2cb27a4742748ea6998`
- Sidecar: `WEARABLE_1B_EVALUATION_PROTOCOL_V1.sha256`

The JSON protocol was created and hashed before searching for or opening any real WEARABLE-1A outcome
export. V1 was not retuned after synthetic results.

## 6. Evaluation unit

Primary unit: one frozen target exercise with one eligible linked exercise outcome.

Dependence/temporal unit: `targetWorkoutAttemptId`. All target exercises in one workout remain in one
test block. `subjectId` is retained for future multi-subject support, but this V1 makes no multi-user
generalization claim.

## 7. Exercise-readiness mapping

- Primary: frozen `primaryReadinessMin` across snapshotted primary muscles.
- Sensitivity: frozen `primaryReadinessMean`.
- Descriptive only: `secondaryReadinessMin` where available.
- No weighted mapping: WEARABLE-1A exports no stable frozen primary-muscle weights.

The evaluator never recomputes anatomy or current readiness.

## 8. Target-attempt rule

Strict `TARGET_ATTEMPTED` requires:

1. matching canonical exercise and target/outcome links;
2. `matchedPlannedTarget=true` and a valid linked first working set;
3. actual load within `0.01 kg` of frozen `suggestedLoadKg`;
4. positive actual reps;
5. no duplicate canonical target in that prediction.

A materially different load or no matching saved set is `TARGET_NOT_ATTEMPTED`, not automatic
underperformance. Missing/contradictory required fields or ambiguous links are
`TARGET_ATTEMPT_AMBIGUOUS`. Both remain in coverage/selection accounting but not strict primary
attainment.

## 9. Primary and secondary outcomes

Primary V1 outcome for `REPS_WEIGHT`:

- attained: strict target attempted and `achievedReps >= suggestedMinReps`;
- underperformed: strict target attempted and `achievedReps < suggestedMinReps`.

Suggested RIR is not evaluated because WEARABLE-1A deliberately stores it as unavailable. Secondary
outputs remain separate: load/rep deltas, frozen-range position, performance change versus frozen
reference, target-set completion, exercise completion, status/exercise/muscle/tracking/rule/confidence
groups, time since frozen reference, and completed-versus-partial context. No composite score was
invented.

## 10. Eligibility and exclusion rules

Every target gets exactly one ledger state:

`ELIGIBLE_PRIMARY`, `ELIGIBLE_SECONDARY_ONLY`, `EXCLUDED`, `ABSTAINED`, or `UNRESOLVED`.

Explicit reason codes cover frozen/cutoff/model/hash/link/timestamp failures, ACTIVE/abandoned outcomes,
missing PO suggestions, unsupported tracking/rules, low confidence, no muscle mapping, duplicate
canonical targets, and attempted/not-attempted/ambiguous distinctions. Rows are never silently dropped.

## 11. Comparator definitions

1. `BASE_RATE_V1`: Laplace-smoothed historical attainment rate from training rows only.
2. `LAST_OUTCOME_V1`: prior eligible same-canonical-exercise outcome plus missingness.
3. `TIME_SINCE_TRAINED_V1`: log hours since the frozen canonical-exercise reference timestamp plus
   missingness. Per-muscle last-trained time remains sensitivity context because MUSCLE-4B's seven-day
   window makes null ambiguous.
4. `PO_HISTORY_ONLY_V1`: frozen load/rep/set/confidence/reference/history features, no MUSCLE-4B value.
5. `MUSCLE_4B_ONLY_V1`: only frozen exercise readiness.
6. `PO_PLUS_MUSCLE_4B_V1`: exactly comparator 4 plus `primaryReadinessMin`.

The central incremental comparison is comparator 6 versus 4. Status is not added as an extra combined
feature because it is a deterministic bin of the same readiness score.

## 12. Rolling-origin design

Rows sort by `inputDataCutoffAtMillis`, then stable workout-attempt ID. Each fold trains on expanding
earlier workout blocks and tests the next workout block. A training block is usable only if its latest
captured outcome timestamp is no later than the test cutoff. Normalization, missing-value means,
last-outcome history, and ridge-logistic probability mapping use training data only.

Synthetic positive fixture evidence:

- fold 1: 30 training workouts/60 rows, test workout `attempt-0030`/2 rows;
- train max cutoff `1702605600000` < test cutoff `1702692000000`;
- all 100 folds reported `strictly_ordered=true`;
- all reported `train_outcomes_available_before_test=true`;
- all reported `workout_split=false`.

## 13. Sample/event gates

Full evaluation requires all of:

- 60 eligible workout blocks;
- 200 eligible exercise outcomes;
- 100 target-attainment successes;
- 100 underperformance events;
- 30 rolling folds after 30 minimum history blocks;
- at least three canonical exercises with 20 records;
- at least three readiness status buckets with 20 records;
- 70% evaluable coverage among exported target rows.

These are conservative AURON-specific release gates informed by validation literature, not universal
sample-size claims. Descriptive audit output remains available below them.

## 14. Data-integrity gates

Critical failures include invalid protocol/payload hash, unsupported export/Room/contract/model version,
duplicate prediction/workout/entity identity, broken internal links, impossible timestamp ordering,
input max after cutoff, mixed incompatible models, more than 10% ACTIVE outcomes, or more than 2%
outcome-linkage failure.

Two limitations are flags, not fabricated checks:

- `raw_history_digest_reconstructable=false`;
- `capture_denominator_known=false`.

## 15. Metrics

When full gates pass the tool reports:

- AUROC and PR-AUC for underperformance;
- fold-trained Brier score, log loss, calibration intercept/slope, and reliability bins;
- target-attainment and underperformance by readiness status;
- PO-only versus combined Brier difference, log-loss/AUROC context, fold win proportion, and time
  stability;
- false-ready and false-not-ready rates at the pre-registered readiness threshold 72;
- canonical exercise and sensitivity breakdowns.

`readiness / 100` is never passed to a probability metric.

## 16. Bootstrap and dependence handling

V1 assumes one local subject and clusters at workout. The primary interval uses a 2,000-replicate
moving workout-block bootstrap, fixed seed `41701`, block length five, 95% interval. Exercise rows are
never resampled independently. Future multi-subject work must resample subjects first and preserve
workouts within subject; internal-external validation is unavailable for one subject.

## 17. Conclusion logic

Precedence is exact:

1. `DATA_INTEGRITY_BLOCKED`
2. `INSUFFICIENT_DATA`
3. `BASELINE_SUPPORTED`
4. `PROMISING_NOT_VALIDATED`
5. `NO_INCREMENTAL_SIGNAL`

`BASELINE_SUPPORTED` additionally requires at least `0.01` Brier improvement, bootstrap lower bound
above zero, at least 60% fold improvement, readiness-risk AUROC at least `0.60` with lower bound above
`0.50`, useful status ordering, three sufficiently represented exercises, at least two with
nonnegative incremental Brier, and no exercise over 50% of evaluated rows.

`PROMISING_NOT_VALIDATED` is available only after full sample gates pass; insufficient samples remain
`INSUFFICIENT_DATA` rather than being softened into a positive label.

## 18. Evaluator architecture and CLI

Implementation is Python standard library under `tools/recovery-evaluation/`:

- frozen protocol and hash sidecar;
- isolated `recovery_evaluation` package for protocol, legacy payload hash, validation/eligibility,
  statistics, rolling-origin models, metrics, conclusions, and deterministic reporting;
- CLI `evaluate.py`;
- synthetic fixture generator and `unittest` suite;
- ignored local fixture/run output directories.

Commands:

```powershell
python tools\recovery-evaluation\evaluate.py --input <export.json> --mode audit --output <dir>
python tools\recovery-evaluation\evaluate.py --input <export.json> --mode full --output <dir>
```

Exit `0` includes valid negative/insufficient/blocked analytical conclusions. Malformed invocation,
protocol, input JSON, or I/O returns `2`.

## 19. Files changed

Created:

- `tools/recovery-evaluation/evaluate.py`
- `tools/recovery-evaluation/.gitignore`
- `tools/recovery-evaluation/README.md`
- `tools/recovery-evaluation/protocol/WEARABLE_1B_EVALUATION_PROTOCOL_V1.json`
- `tools/recovery-evaluation/protocol/WEARABLE_1B_EVALUATION_PROTOCOL_V1.sha256`
- `tools/recovery-evaluation/src/recovery_evaluation/__init__.py`
- `tools/recovery-evaluation/src/recovery_evaluation/protocol.py`
- `tools/recovery-evaluation/src/recovery_evaluation/payload_hash.py`
- `tools/recovery-evaluation/src/recovery_evaluation/statistics.py`
- `tools/recovery-evaluation/src/recovery_evaluation/evaluator.py`
- `tools/recovery-evaluation/fixtures/generate_fixtures.py`
- `tools/recovery-evaluation/fixtures/README.md`
- `tools/recovery-evaluation/tests/test_recovery_evaluation.py`
- this result document.

Updated after verification: root `README.md` with WEARABLE-1B state/next-step note.

Repository knowledge-graph maintenance: the required `graphify update .` completed successfully and
regenerated the existing `graphify-out` graph/report artifacts. These are navigation artifacts, not
Android production or evaluator behavior.

No Android production/test/schema file was modified by WEARABLE-1B.

## 20. Test and build results

- `python -m unittest discover -s tools\recovery-evaluation\tests -v`: **50 tests, 0 failures**.
- `:app:testDebugUnitTest --no-daemon`: **BUILD SUCCESSFUL** in 1m27s.
- `:app:assembleDebug --no-daemon`: **BUILD SUCCESSFUL** in 21s.
- Debug APK exists at `app/build/outputs/apk/debug/app-debug.apk`, 97,909,432 bytes.

Gradle emitted the repository's existing AGP 8.5.2/compileSdk 35 compatibility warning; it did not fail
the build and is unrelated to this standalone tool.

## 21. Synthetic fixture results

| Fixture | Expected/actual conclusion | Key evidence |
|---|---|---|
| sufficient signal | `BASELINE_SUPPORTED` | 260 rows, 130 blocks, 156 successes, 104 failures, 100 folds, readiness AUROC 1.0 |
| no incremental signal | `NO_INCREMENTAL_SIGNAL` | Brier gain `-0.0000216193`, readiness AUROC `0.5` |
| corrupt payload | `DATA_INTEGRITY_BLOCKED` | exact `PAYLOAD_HASH_INVALID` finding |
| small sample | `INSUFFICIENT_DATA` | 20 rows, 10 blocks, 12 successes, 8 failures, 0 folds |

The sufficient synthetic fixture's PO-only minus combined Brier gain was `0.225861` with 95%
workout-block interval `[0.224487, 0.227123]` and all folds improved. This is deliberately generated
signal and is not evidence about real MUSCLE-4B accuracy.

Additional generated/tested scenarios cover inverted signal, future leakage, heavy missingness,
first-set ambiguity, multiple exercises in one workout, and incompatible model versions.

## 22. Real local-data result

After V1 was frozen and hashed, a filename-only repository search found no canonical WEARABLE-1A
research export. Only the protocol JSON and historical Room schema JSON files were present. Private QA
database snapshots are not canonical evaluator inputs and were not opened or transformed.

Therefore:

- prospective MUSCLE-4B signal on real data: **not measured**;
- incremental value over PO/history: **not measured**;
- evidence sufficiency: **insufficient**;
- real capture quality: **cannot be audited without an export**;
- real-data conclusion: **`INSUFFICIENT_DATA` (no run/input)**.

## 23. Deterministic-output evidence

Two independent full runs of the sufficient-signal fixture, with the same input/protocol/seed, produced
byte-identical files:

| Artifact | SHA-256 |
|---|---|
| `evaluation_report.json` | `9d49ad80a48b9bc0ac0843265348837493a45da49516454ee1b94c9796dc5c6b` |
| `evaluation_report.md` | `7652a0425c84a9fb71240c1d395da394e3349f27938e6ae29d6ba785b65e37b1` |
| `eligibility_ledger.json` | `1f320892962932b819e63ea4433eb778e6ee88692e4d4bd617e37abea272ec86` |
| `fold_manifest.json` | `bf1c616f67ec49ca13647d3182ccb46b7e1d7dff14ca7afb1f1b4b4e311c33a1` |
| `metrics.json` | `7a0e9abd80d54e1987bd65e19d649014a90d43ff5cd09192140878416eddd665` |

The test suite also proves semantic invariance to export-array order after stable internal sorting and
records a different seed only in seed-dependent analysis.

## 24. Protected before/after hashes

All hashes are identical before and after:

| Protected file | SHA-256 before = after |
|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` |
| `RecoveryResearchContract.kt` | `83EEACC0E28F194B0C3649684C80CF3500105C0B9337913C52F1D3B30B0D6724` |
| `RecoveryResearchExport.kt` | `10F777E20619AFB0F85A08B49516D0F576D9A53F27CD85DCA857D60F2CA9A1724` |

## 25. Limitations

- No real canonical export was available, so the task validates an evaluator, not MUSCLE-4B.
- Target attainment is a practical prospective performance proxy, not a direct biological recovery
  measurement.
- Strict attempted-only analysis may select users/workouts that followed the PO load; not-attempted and
  ambiguous coverage must remain visible.
- First-set modal-load selection is frozen heuristic evidence. WEARABLE-1A exports no alternate loaded
  rule, so a “remove all modal-rule rows” sensitivity has no primary rows and is explicitly unavailable.
- Bodyweight difficulty, technique quality, and metric targets are not sufficiently structured for V1
  primary comparison.
- Raw input history and total capture denominator are unavailable.
- One local-device subject cannot establish population transportability.
- Sample thresholds and `0.01 kg` tolerance are preregistered AURON choices, not universal constants.
- The legacy Kotlin payload hash codec must remain compatible with WEARABLE-1A data-class/JVM number
  formatting; protocol/version rejection prevents silent reinterpretation.

## 26. Exact next recommendation

**Next: continue WEARABLE-1A prospective capture and create one explicit, consented canonical JSON
export; then run WEARABLE-1B `audit` before `full`.** Do not build Health Connect yet because no real
export has passed the audit gates.

After audit passes, continue capture until the preregistered event/block/fold gates are met. Then:

- `BASELINE_SUPPORTED`: Health Connect shadow research may be considered, still with no active modifier;
- `PROMISING_NOT_VALIDATED` or `INSUFFICIENT_DATA`: collect targeted prospective data without changing
  the formula;
- `NO_INCREMENTAL_SIGNAL`: preserve the negative result and investigate calibration/labels/selection
  rather than modifying MUSCLE-4B in this task;
- `DATA_INTEGRITY_BLOCKED`: repair only the smallest explicit capture/export prerequisite in a new task.

WEARABLE-1B stops here. No formula, schema, capture, UI, Health Connect, or wearable modifier expansion
was performed.
