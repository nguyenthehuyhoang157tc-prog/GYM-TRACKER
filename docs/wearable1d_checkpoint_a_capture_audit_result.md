# WEARABLE-1D — Checkpoint A Natural Capture Audit Result

**Checkpoint state:** `CHECKPOINT_A_CAPTURE_WARNING`  
**Pipeline decision:** Continue the same verified build and database; no capture repair is indicated.  
**WEARABLE-1D final state:** Not reached. Checkpoint B requirements are not met.  
**Biological-validation conclusion:** None.

## 1. Scope

This is an early, aggregate-only integrity audit of the first physical-phone canonical Research export. The user reported two naturally performed saved workouts. The export also contains one linked abandoned attempt.

This checkpoint evaluates:

- supported export provenance and byte preservation;
- schema, version, graph-link, timestamp/cutoff and payload-hash integrity;
- prediction/outcome capture coverage visible inside the export;
- deterministic WEARABLE-1B audit execution; and
- whether collection should continue or stop for a capture defect.

It does not evaluate MUSCLE-4B biological performance.

## 2. Input preservation and privacy

- The original file was hashed at the user-supplied path before inspection.
- It was moved byte-for-byte to a private evidence directory outside the repository and graphify inputs.
- The preserved file's size and hash matched the source before the repository copy was removed.
- Input size: `106,989` bytes.
- Input SHA-256: `D58C57A596C7E6D57770DB5580BFDB358F08FDAC33E4F504585A346512F9D7C9`.
- No raw exercise names, loads, repetitions, identifiers or exact timestamps are recorded here.

## 3. Provenance

The user explicitly attested that:

- the workouts occurred naturally on the physical pilot phone;
- the file came from **Settings → Export Research JSON → Android document picker**; and
- the supplied file was the original unedited export.

The file is therefore classified as:

`USER_ATTESTED_SUPPORTED_PHYSICAL_EXPORT_FLOW`

The JSON does not embed a hardware attestation, APK certificate or physical-device identifier. Physical-device provenance is consequently operationally confirmed by the user's first-party attestation, not cryptographically self-proven by the file.

Additional separation evidence:

- one research subject is present;
- one `GRANTED` consent record precedes all three frozen predictions;
- exported identifiers have zero intersections with known WEARABLE-1B fixture identifiers; and
- no synthetic/QA marker or unsupported version was detected.

No emulator record is used as pilot evidence. These checks do not claim the export can cryptographically prove the absence of every possible external QA record.

## 4. Frozen versions

| Surface | Observed |
|---|---|
| Research contract | `WEARABLE_1A_V1` |
| Export schema | `1` |
| Room schema declared by export | `9` |
| Recovery model | `MUSCLE_4B_BASELINE` — 3/3 predictions |
| Muscle Heat model | `MUSCLE_HEAT_1_STIMULUS_QUALITY` — 3/3 |
| Progressive Overload model | `PROGRESSIVE_OVERLOAD_V3_11D` — 3/3 |
| First-set rule | `TRACKING_AWARE_FIRST_WORKING_SET_V1` — 3/3 |
| Evaluator | `WEARABLE_1B_EVALUATOR_V1` |
| Evaluator protocol | `WEARABLE_1B_EVALUATION_PROTOCOL_V1` |
| Evaluator protocol SHA-256 | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` |
| Wearable modifier active | `false` |

## 5. Canonical export structure

`EXPORT_FORMAT_VALID = PASS`

All eight required collections are present:

| Collection | Count |
|---|---:|
| `consent` | 1 |
| `predictions` | 3 |
| `predictionGroups` | 18 |
| `predictionMuscles` | 44 |
| `targetExercises` | 26 |
| `setObservations` | 64 |
| `outcomes` | 3 |
| `exerciseOutcomes` | 21 |

Outcome states:

- `COMPLETED`: 1
- `PARTIAL`: 1
- `ABANDONED`: 1
- `ACTIVE`: 0

The three prediction/outcome records therefore reconcile to two saved workouts plus one abandoned attempt; they are not duplicate predictions.

## 6. Integrity results

All frozen WEARABLE-1B integrity checks passed:

| Check | Result |
|---|---:|
| Duplicate prediction IDs | 0 |
| Duplicate workout-attempt IDs | 0 |
| Broken group links | 0 |
| Broken muscle links | 0 |
| Broken target links | 0 |
| Broken set-observation links | 0 |
| Broken outcome links | 0 |
| Broken exercise-outcome links | 0 |
| Invalid frozen payload hashes | 0 |
| Unsupported schema/contract/model versions | 0 |
| Prediction cutoff/order violations | 0 |
| Outcome timestamp violations | 0 |
| Exercise-observation timestamp violations | 0 |
| Critical evaluator findings | 0 |
| Duplicate canonical target groups | 0 |
| Missing primary-muscle mappings | 0 |

Three of three predictions were frozen after granted consent and before their app-recorded first-valid-set timestamps. All 21 exercise outcomes have a linked first-set observation and supported first-set rule.

## 7. Temporal prospectiveness limitation

The user reported that the two workouts were performed naturally, but sets were entered near the end of the gym session. This produced an app-recorded workout duration of approximately 6–7 minutes.

That recorded duration and the set-entry timestamps are **not** treated as the true workout duration or true exercise-performance timestamps.

The export proves only:

- frozen prediction before app-recorded first-valid-set entry: 3/3 outcomes;
- frozen prediction before app-recorded exercise observation: 21/21 exercise outcomes; and
- frozen prediction before terminal completion timestamp: 2/2 saved workouts.

The export contains no direct timestamp for when the real-world sets were physically performed. It therefore cannot prove that the frozen prediction preceded the actual lifting performance for either saved workout.

Classification:

`PERFORMANCE_PROSPECTIVENESS_TEMPORALLY_UNCERTAIN`

The two saved workouts remain valid for export, schema, consent, graph, linkage, payload and pipeline-integrity checks. They are not accepted as confirmed prospective performance-validation evidence. This is a logging-timing limitation, not a MUSCLE-4B failure.

The evaluator's 12 `ELIGIBLE_PRIMARY` target rows describe eligibility under the frozen machine-readable contract and recorded timestamps. They are not interpreted as biologically prospective rows in this checkpoint because of the user-declared late entry.

## 8. Aggregate capture results

### Capture and linkage

- observable research attempts with frozen predictions: 3;
- linked terminal outcomes: 3/3;
- saved `COMPLETED` or `PARTIAL` workouts: 2;
- confirmed abandoned attempts: 1;
- unresolved `ACTIVE` outcomes: 0/3;
- exercise outcomes with first-set observations: 21/21;
- outcome-linkage failures: 0.

### Target-attempt classification

| Classification | Count | Rate |
|---|---:|---:|
| `TARGET_ATTEMPTED` | 12 | 46.2% |
| `TARGET_ATTEMPT_AMBIGUOUS` | 11 | 42.3% |
| `TARGET_NOT_ATTEMPTED` | 3 | 11.5% |

### Evaluator eligibility

| State | Count | Rate |
|---|---:|---:|
| `ELIGIBLE_PRIMARY` | 12 | 46.2% |
| `ABSTAINED` | 9 | 34.6% |
| `EXCLUDED` | 5 | 19.2% |

Reason counts:

- `NO_BASELINE_HISTORY`: 6
- `ABANDONED_WITHOUT_EVALUABLE_EXERCISE`: 5
- `TARGET_NOT_ATTEMPTED`: 3
- `ELIGIBLE_PRIMARY`: 12

### Supporting coverage

- first-set confidence: `MODERATE` for 21/21 exercise outcomes;
- target tracking types: `REPS_WEIGHT` 24, `BODYWEIGHT_REPS` 2;
- target completeness: `COMPLETE` 20, `NONE` 6;
- target abstentions: 6/26;
- missing suggestion action: 0;
- missing primary-muscle mapping: 0; and
- duplicate canonical target groups: 0.

## 9. Denominator and Room completeness

`CAPTURE_DENOMINATOR = PARTIALLY_KNOWN_DENOMINATOR`

The user attests to two naturally saved workouts, and both corresponding saved outcome states are present. The export also records one abandoned attempt. Among these three observable research attempts, prediction and terminal linkage are complete.

The canonical export cannot enumerate workouts performed while consent/capture was disabled or otherwise absent from the research tables. Full all-workout denominator completeness is therefore not claimed.

`EXPORT_ROOM_COMPLETE = NOT_INDEPENDENTLY_RUNTIME_CROSS_CHECKED`

No physical release database was accessed. Android release sandboxing was not weakened and no `run-as`, root, re-signing or private database extraction was required. Canonical format, all eight collections, internal graph integrity, versions, hashes, consent ordering, denominator evidence and evaluator compatibility were validated instead.

## 10. WEARABLE-1B deterministic audit

Command shape:

```powershell
python .\tools\recovery-evaluation\evaluate.py `
  --input "<private-original-export.json>" `
  --mode audit `
  --output "<private-audit-run>"
```

Both runs exited successfully.

| Artifact | Run 1 vs run 2 | SHA-256 |
|---|---|---|
| `evaluation_report.json` | Byte-identical | `8B6AE61428226D05CD8DECA2E94130FE5555F2922A5CB0C6CD81C9480FAFE200` |
| `evaluation_report.md` | Byte-identical | `FE1BA47884C386206B8B91197D10C6853BBBA9BA8ACC7D6CDE8E54C43FFC75EF` |
| `eligibility_ledger.json` | Byte-identical | `3CDCDCA371F74CB40B612AB0F08BE822DDF3FCF4BE6D2FF51B75BA2F0E0C68D1` |
| `fold_manifest.json` | Byte-identical | `37517E5F3DC66819F61F5A7BB8ACE1921282415F10551D2DEFA5C3EB0985B570` |
| `metrics.json` | Byte-identical | `C81301EF5BC1004D583F02184D68B9554E26B5C8FB6FF9C6DE92EACF5A40A612` |

Determinism mismatches: `0/5`.

Evaluator conclusion:

`INSUFFICIENT_DATA`

Evaluator data-quality verdict:

`ACCEPTABLE_WITH_DECLARED_LIMITATIONS`

No rolling fold was produced and no full statistical gate passed. This is expected and is not a capture failure.

## 11. Checkpoint classification

`CHECKPOINT_A_CAPTURE_WARNING`

Reasons for warning rather than pass:

1. only two naturally saved workouts exist, below the planned 3–5-workout Checkpoint A range;
2. performance prospectiveness is temporally uncertain because sets were entered late;
3. the all-workout denominator is only partially known;
4. physical-device provenance is user-attested rather than cryptographically embedded;
5. Room-relative completeness was not independently runtime-cross-checked; and
6. target-attempt ambiguity is measurable at 11/26 targets.

Reasons this is not blocked:

- critical integrity findings are zero;
- prediction/outcome linkage is complete for all observable attempts;
- unresolved `ACTIVE` count is zero;
- first-set capture exists for all 21 exercise outcomes;
- primary eligibility is non-zero;
- no supported-version, payload-hash, future-data, duplicate or broken-link error exists; and
- both evaluator runs are byte-deterministic.

## 12. Decision

Choose:

`1. CONTINUE_USING_THE_SAME_BUILD_AND_DATABASE_UNTIL_10_TERMINAL_WORKOUTS`

No capture defect currently requires repair.

For future workouts:

- keep the same physical installation and Room database;
- keep Recovery Research enabled;
- log sets during the workout, close to when they are performed;
- do not change the training itself to satisfy the study;
- do not deliberately fail targets or manufacture abandoned sessions; and
- export again through Settings after at least 10 naturally terminal workouts.

Contemporaneous set logging is needed to make future prediction-before-performance timing substantially more credible. The first two saved workouts remain in the immutable dataset with their declared temporal limitation; they must not be edited or silently removed.

## 13. Protected surfaces

All protected hashes remain equal to the frozen WEARABLE-1D preflight values:

| Protected surface | SHA-256 |
|---|---|
| `MuscleRecovery.kt` | `0520CE83248BE1A5F9431E97981E12F75ECBA77F9262355644B0D8E2F8DBF748` |
| `MuscleHeatEngine.kt` | `12804F8654BA8124B2200BFB1066360FA70D5A82058C948FAA3F8DE1EB065341` |
| `ProgressiveOverload.kt` | `8D2355D55980B647CE327C7FF790718007FC97360A092886CE6EF5C88CBB957B` |
| `RecoveryResearchContract.kt` | `83EEACC0E28F194B0C3649684C80CF3500105C0B9337913C52F1D3B30B0D6724` |
| `RecoveryResearchExport.kt` | `10F777E20619AFB0F85A08B49516D0F576D9A53F27CD85DCA857D60F2CA9A172` |
| `WEARABLE_1B_EVALUATION_PROTOCOL_V1.json` | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` |
| `AppDatabase.kt` | `42EA850483CF84577F5438940518A9E1F5ABE5EA17B6680F5D9DE4A7A149760B` |
| `AndroidManifest.xml` | `0EC31FF112D9D2FE77ED6107442D69280A220F7BA43369CA8D11AE9542282964` |
| `backup_rules.xml` | `0D5E73555C17D67990C444F7AA688CA279B893AC5D0053094E70D6CDF97AE8EC` |
| `data_extraction_rules.xml` | `023CAFFA7E9CCC229D775C41CCFD90B3D4EC80BEC7E2B5EB77AE4EE663BFA355` |

No production source, Room schema, export semantics or WEARABLE-1B V1 protocol changed.

## 14. Not completed

- `FIRST_REAL_AUDIT_COMPLETE` is not claimed.
- Checkpoint B has not started.
- Subagent B has not been invoked.
- WEARABLE-2A is not authorized by this two-workout checkpoint.
- README is not updated as though WEARABLE-1D were complete.
