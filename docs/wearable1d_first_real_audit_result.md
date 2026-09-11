# WEARABLE-1D — First Real Audit Result

## 1. Final state

`FIRST_REAL_AUDIT_COMPLETE_WITH_LIMITATIONS`

`WEARABLE_2A_GATE = OPEN`

Checkpoint B is complete because the preserved physical-phone export contains exactly 10 valid natural
`COMPLETED/PARTIAL` terminal workouts, the frozen evaluator accepts the export, every hard integrity
requirement passes, outcome linkage works, the unresolved `ACTIVE` proportion remains below the frozen
limit, and primary eligibility is non-zero and measurable.

The limitations are material but do not indicate a capture defect: the first two saved workouts remain
temporally uncertain for performance validation, the all-workout denominator is only partially known,
physical provenance is user-attested, Room completeness was not independently runtime-cross-checked, and
the dataset remains far below WEARABLE-1B's biological/statistical gates. No biological validation is
claimed.

## 2. Pilot protocol, version, and hash

| Artifact | Frozen identity |
|---|---|
| Natural pilot protocol | `WEARABLE_1D_PILOT_PROTOCOL_V1` |
| Pilot protocol SHA-256 | `F6A9CD283D9A1F94326EF2ADF6888872E020F39118F4F6EAB747DA925C3332DC` |
| Evaluator | `WEARABLE_1B_EVALUATOR_V1` |
| Evaluator protocol | `WEARABLE_1B_EVALUATION_PROTOCOL_V1` |
| Evaluator protocol SHA-256 | `494AC18B8A92114091464AB1731776DEF680AF40FA26E2CB27A4742748EA6998` |
| Evaluator seed | `41701` |

The pilot-protocol hash and evaluator-protocol hash identify different frozen artifacts and must not be
interchanged.

## 3. Roadmap context

WEARABLE-0 defined the validation and shadow-mode boundary. WEARABLE-1A added opt-in prospective capture,
WEARABLE-1B froze the evaluator, WEARABLE-1C activated and technically exercised the capture/export path,
WEARABLE-1C.1 hardened backup privacy, and WEARABLE-1D collected and audited natural physical-phone
evidence. This audit decides only whether read-only Health Connect shadow context may begin in
WEARABLE-2A. It does not decide whether MUSCLE-4B is biologically accurate or whether a wearable modifier
should exist.

## 4. Subagent A findings

The prior independent preflight review returned `GO_WITH_LIMITATIONS`. It found the technical capture,
canonical export, privacy and evaluator path sufficient to start a natural pilot while reserving the real
decision for evidence from the physical phone. It explicitly treated the Pixel 8 API 37 emulator as
QA/preflight only and required a physical installation, physical consent boundary, natural export and
later independent review. The current audit satisfies those evidence prerequisites without promoting any
emulator row to pilot evidence.

## 5. Pilot start boundary

The authoritative boundary remains the first `GRANTED` Recovery Research consent event created on the
physical pilot phone after installation of the frozen release. The export contains one `GRANTED` consent
record and it precedes all 14 frozen predictions. The exact timestamp remains private.

The user attests that the physical phone remained on the frozen WEARABLE-1D release installation and the
same Room database with Recovery Research enabled. No emulator consent timestamp is used. The export does
not contain cryptographic hardware or installed-APK attestation, so this boundary is operationally
confirmed by the user's first-party provenance statement plus the export's consent ordering.

## 6. App, Room, research, and model versions

| Surface | Accepted value | Export coverage |
|---|---|---:|
| App release identity | `versionName 1.0`, `versionCode 1` | Frozen pilot record |
| Room schema | `9` | Export root |
| Export schema | `1` | Export root |
| Research contract | `WEARABLE_1A_V1` | 14/14 predictions |
| Recovery model | `MUSCLE_4B_BASELINE` | 14/14 predictions |
| Muscle Heat model | `MUSCLE_HEAT_1_STIMULUS_QUALITY` | 14/14 predictions |
| Progressive Overload model | `PROGRESSIVE_OVERLOAD_V3_11D` | 14/14 predictions |
| Prediction first-set contract | `TRACKING_AWARE_FIRST_WORKING_SET_V1` | 14/14 predictions |
| Prediction source | `PRE_FIRST_VALID_WORKING_SET` | 14/14 predictions |
| Wearable modifier | `false` | Evaluator report |

No unsupported or mixed version appears.

## 7. Natural-use instructions and compliance boundary

The pilot instructions were to train normally, keep the same physical installation/database, leave
Recovery Research enabled, save or discard naturally, avoid manufactured failures, and export only through
Settings and the Android document picker. The user attests that these are natural physical-phone workouts
and that later workouts were logged during training.

The first two saved workouts were entered near the end of their real sessions. Their app-recorded duration
and set-entry timestamps are not interpreted as the true workout duration or the time the physical sets
were performed. No duration-based prospective claim is made for any workout.

## 8. Real export provenance

Provenance classification:

`USER_ATTESTED_SUPPORTED_PHYSICAL_EXPORT_FLOW`

The user supplied the original unedited output from **Settings → Export Research JSON → Android document
picker** and attested that it came from the preserved physical pilot phone. The export has one research
subject, one granted-consent chain, supported versions, and zero known WEARABLE-1B synthetic-fixture or QA
markers.

The complete Checkpoint A snapshot is an exact immutable subset of the current export across all eight
collections: every prior row is present and byte-equivalent after canonical object comparison, with zero
missing or changed prior rows. This proves continuity of the research dataset and precisely isolates the
two previously declared late-entry saved workouts. Newer computer development builds and emulator
databases are excluded.

## 9. Export SHA-256 and preservation

- Original size: `491,006` bytes.
- Expected SHA-256: `47DAE3D1621AF5CFFCCAB3212A1161F8F65D24C8AF3D327FEA8F9B93E0460E82`.
- Observed before preservation: exact match.
- Preserved original: exact same size and SHA-256.
- Repository copy after preservation: absent.
- Storage boundary: private evidence directory outside the repository and graphify inputs.

The original was not edited, reformatted, merged, repaired or normalized.

## 10. Canonical export format validation

`EXPORT_FORMAT_VALID = PASS`

All eight required collections are present:

| Collection | Rows |
|---|---:|
| `consent` | 1 |
| `predictions` | 14 |
| `predictionGroups` | 84 |
| `predictionMuscles` | 204 |
| `targetExercises` | 130 |
| `setObservations` | 213 |
| `outcomes` | 14 |
| `exerciseOutcomes` | 106 |

All root versions are supported, all collection identity keys are unique, and the evaluator parsed the
canonical document successfully in both runs.

## 11. Export versus Room completeness

`EXPORT_ROOM_COMPLETE = NOT_INDEPENDENTLY_RUNTIME_CROSS_CHECKED`

No physical release database was extracted. Android sandboxing/signing was not weakened, and no root,
re-signing or `run-as` workaround was used. The audit therefore validates canonical structure, all eight
collections, supported versions, internal relationships, payload hashes, timestamp/cutoff ordering,
Checkpoint A continuity and evaluator compatibility, but does not claim independent Room-relative
completeness.

## 12. Denominator classification

The observable, user-attested natural saved-terminal denominator is exactly 10 workouts:

- `COMPLETED`: 2;
- `PARTIAL`: 8; and
- valid-for-evaluation outcome flag: 10/10.

This satisfies the frozen Checkpoint B minimum of at least 10 natural `COMPLETED/PARTIAL` terminal
workouts. The broader observable research-attempt ledger contains 14 attempts: 10 saved terminal, 3
abandoned terminal and 1 unresolved `ACTIVE`.

`CAPTURE_DENOMINATOR = PARTIALLY_KNOWN_DENOMINATOR`

Among the 14 observable research attempts, prediction coverage is 14/14, outcome-row attachment is 14/14,
terminal resolution is 13/14 (92.9%), and `ACTIVE` is 1/14 (7.1%). The canonical research export cannot
enumerate any workout absent from research capture, so complete all-workout coverage is not claimed even
though the user attests that Recovery Research remained enabled.

## 13. Prediction, target, set, and outcome counts

| Entity/state | Count |
|---|---:|
| Frozen predictions | 14 |
| Unique workout-attempt identities | 14 |
| Target exercises | 130 |
| Set observations | 213 |
| Outcome rows | 14 |
| Exercise outcomes | 106 |
| `COMPLETED` | 2 |
| `PARTIAL` | 8 |
| `ABANDONED` | 3 |
| `ACTIVE` | 1 |

All 14 predictions have one linked outcome row. The 1 `ACTIVE` outcome contributes six unresolved target
rows and is retained rather than retroactively closed.

## 14. Target-attempt distribution

| Frozen classification | Count | Rate |
|---|---:|---:|
| `TARGET_ATTEMPTED` | 43 | 33.1% |
| `TARGET_ATTEMPT_AMBIGUOUS` | 68 | 52.3% |
| `TARGET_NOT_ATTEMPTED` | 19 | 14.6% |

Ambiguity is material and remains a limitation. It is measurable rather than silently coerced into an
attempt or failure, and it does not create a hard integrity error under Protocol V1.

## 15. Eligibility, exclusion, and abstention distribution

| Evaluator state | Count | Rate |
|---|---:|---:|
| `ELIGIBLE_PRIMARY` | 43 | 33.1% |
| `ELIGIBLE_SECONDARY_ONLY` | 37 | 28.5% |
| `ABSTAINED` | 27 | 20.8% |
| `EXCLUDED` | 17 | 13.1% |
| `UNRESOLVED` | 6 | 4.6% |

Reason-level totals are: 43 primary-eligible, 29 low-confidence first-set secondary rows, 8
unsupported-tracking secondary rows, 8 no-baseline abstentions, 19 target-not-attempted abstentions, 17
abandoned exclusions and 6 active-outcome unresolved rows.

The frozen machine contract identifies 43 primary rows across 10 saved workout blocks. A separate
prospectiveness layer is required before future biological-performance use:

- 12 primary rows across the two Checkpoint A workouts remain
  `PERFORMANCE_PROSPECTIVENESS_TEMPORALLY_UNCERTAIN` and are not accepted as confirmed prospective
  performance evidence;
- 31 primary rows across eight later workouts are operationally prospective: all eight predictions were
  frozen before the app-recorded first-valid-set and terminal timestamps, and all 52 later exercise
  outcomes that contain an observed first-set timestamp place that timestamp after freeze; and
- this later classification also depends on the user's attestation that the sets were logged naturally
  during training. The export cannot cryptographically timestamp the physical lifting action.

No attainment or readiness result is interpreted as biological validation in this audit.

## 16. First-set confidence and rule distribution

First-set confidence across 106 exercise outcomes:

| Confidence | Count |
|---|---:|
| `MODERATE` | 73 |
| `NONE` | 33 |

First-set rule distribution:

| Rule | Count |
|---|---:|
| `MODAL_LOAD_TIE_HEAVIEST_V1` | 67 |
| `FIRST_POSITIVE_REP_SET_V1` | 6 |
| `TRACKING_AWARE_FIRST_WORKING_SET_V1` | 33 |

All 73 planned-target matches have moderate confidence; 66 are valid for evaluation and seven are
excluded only because baseline history was unavailable. The 33 `NONE` rows are retained explicitly and
contribute to ambiguity/secondary-only handling rather than being invented into valid first sets.

## 17. Progressive Overload, muscle, and tracking coverage

- Target tracking types: `REPS_WEIGHT` 119, `BODYWEIGHT_REPS` 11.
- Exercise-outcome tracking types: `REPS_WEIGHT` 96, `BODYWEIGHT_REPS` 10.
- Structured load/min/max PO target available: 114/130.
- Target completeness: `COMPLETE` 120, `PARTIAL` 2, `NONE` 8.
- Target abstention: 8/130, all due to no baseline history.
- Primary-muscle mapping present: 130/130.
- Secondary-muscle mapping present: 59/130.
- Duplicate prediction/canonical-target groups: 0.
- Exercise outcomes valid for evaluation: 66/106.
- Planned-target matches: 73/106.

These are capture-coverage descriptors only. They do not measure recovery accuracy.

## 18. Integrity findings

| Hard check | Findings |
|---|---:|
| Duplicate prediction IDs | 0 |
| Duplicate workout-attempt IDs | 0 |
| Duplicate entity identities across all eight collections | 0 |
| Broken prediction/group/muscle/target/set/outcome links | 0 |
| Prediction/outcome attempt mismatch | 0 |
| More than one outcome per prediction | 0 |
| More than one exercise outcome per target | 0 |
| Invalid frozen payload hashes | 0 |
| Unsupported root/contract/model/first-set versions | 0 |
| Prediction cutoff/order violations | 0 |
| Input-history maximum after cutoff | 0 |
| Future target reference | 0 |
| Outcome/exercise timestamp violation | 0 |
| Checkpoint A row mutation or deletion | 0 |
| Known synthetic/QA fixture marker | 0 |
| Critical evaluator findings | 0 |

The frozen payload hashes validate the exported prediction payloads. The separate pre-cutoff raw-history
digest cannot be independently reconstructed because raw history is intentionally absent from the export.

## 19. Frozen evaluator command

The same command shape was executed twice against the preserved private original:

```powershell
python .\tools\recovery-evaluation\evaluate.py `
  --input "<private-preserved-canonical-export>" `
  --mode audit `
  --output "<private-audit-run>"
```

Both runs used the default frozen protocol and seed and exited `0`. Inputs and outputs remained outside the
repository.

## 20. Evaluator conclusion

- Conclusion: `INSUFFICIENT_DATA`.
- Data-quality verdict: `ACCEPTABLE_WITH_DECLARED_LIMITATIONS`.
- Eligible primary exercise outcomes: 43.
- Eligible workout blocks: 10.
- Evaluable target coverage: 33.1%.
- Target attainments: 40.
- Underperformance events: 3.
- Rolling-origin folds: 0.
- Full sample/event gates passed: 0/8.

`INSUFFICIENT_DATA` is the expected scientific conclusion at this size, not a pipeline failure. No full
benchmark metric, calibration result or biological-performance conclusion was produced.

## 21. Deterministic rerun hashes

All five required artifacts are byte-identical across both runs:

| Artifact | SHA-256 |
|---|---|
| `evaluation_report.json` | `4AEF8AE214E2988A4624A16B3FD32F50F002FAC35C611EEEA70D766CC2CB37A9` |
| `evaluation_report.md` | `A8D65B34BBE2446CAE7C3B282DF2A56CAF0122A1C6CB1402D3D51A90EFB21D83` |
| `eligibility_ledger.json` | `ABDB4522A0794C26B5247AF7798E5EC5F4DE68C23B1DEC69DB5F27A6971ABB4D` |
| `fold_manifest.json` | `37517E5F3DC66819F61F5A7BB8ACE1921282415F10551D2DEFA5C3EB0985B570` |
| `metrics.json` | `C81301EF5BC1004D583F02184D68B9554E26B5C8FB6FF9C6DE92EACF5A40A612` |

Determinism mismatches: `0/5`.

## 22. Capture-quality verdict

| Dimension | Classification |
|---|---|
| Pipeline/export integrity | `PASS` |
| Prospective capture quality | `ACCEPTABLE_WITH_LIMITATIONS` |
| Biological-performance eligibility | 31 later primary rows across 8 operationally prospective workout blocks; 12 early rows across 2 blocks temporally uncertain |
| Biological validation | `NOT_PERFORMED` |
| Overall capture verdict | `CAPTURE_QUALITY_ACCEPTABLE_WITH_LIMITATIONS` |

There is no observed capture defect requiring repair. Hard errors are zero, lineage is complete for
observable attempts, terminal linkage is 92.9%, `ACTIVE` is 7.1%, and eligibility is non-zero. The early
temporal limitation, target ambiguity and incomplete first-set confidence remain explicit constraints.

## 23. Protected hashes

All protected source/protocol hashes remain equal to the frozen WEARABLE-1D values:

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

The pilot APK remains identified by its frozen preflight SHA-256
`4F8D5757EDA28ACC0E6F995C91D4AC78BF624945FE8ECE4A596E774A9CF8B42C`. The current computer's release
output is a newer development artifact with a different hash and is explicitly excluded from pilot
evidence. It was not installed, audited or treated as the physical pilot build in this task.

## 24. Files changed

Material task documentation only:

- `docs/wearable1d_first_real_audit_result.md` — this aggregate-only First Real Audit record; and
- `README.md` — WEARABLE-1D completion state and WEARABLE-2A handoff.

Project-required graph maintenance also refreshed generated files under `graphify-out/` and saved one
aggregate-only query result. The raw export and private evaluator artifacts were absent before that refresh
and were not graph inputs.

No production source, Room schema, export semantics, evaluator implementation/protocol, Health Connect
surface or wearable behavior changed.

## 25. Tests and build results

- Frozen evaluator unit suite: 51 tests, 0 failures.
- Real audit run 1: exit `0`.
- Real audit run 2: exit `0`.
- Required deterministic artifacts: 5/5 byte-identical.
- Android source/build changes in this audit: none.
- New Android build or install in this audit: deliberately not performed.

The frozen pilot release build/signing evidence remains the preflight record. Rebuilding the current, later
development checkout would create a different artifact and would not validate the already-installed pilot
release, so it is not substituted for physical-pilot evidence.

## 26. Independent Subagent B verdict

Independent read-only Reviewer B returned `PASS_WITH_LIMITATIONS`.

The reviewer independently confirmed the input hash, all eight collection counts, supported versions,
zero critical integrity findings, exact Checkpoint A continuity, 10-workout Checkpoint B denominator,
acceptable `ACTIVE` rate, non-zero eligibility and byte-deterministic reruns. It agreed that the two early
workouts remain temporally uncertain, later contemporaneous logging is user-attested rather than hardware
attested, Room completeness and the all-workout denominator remain unproven, and no biological claim is
supported. It found `FIRST_REAL_AUDIT_COMPLETE_WITH_LIMITATIONS` and opening WEARABLE-2A for read-only
shadow context defensible under the frozen protocol.

## 27. Privacy

- Raw export and both audit-output directories remain local and outside the repository.
- No raw export, Room DB/WAL/SHM file, eligibility ledger or personal workout value is committed.
- No exact timestamp, record identifier, exercise name, load or repetition value appears in this report.
- No raw evidence entered graphify.
- Only aggregate counts, rates, version identities and cryptographic hashes are documented.
- The original export remains immutable.

## 28. Limitations

1. Physical-phone provenance, frozen-install continuity and later contemporaneous logging are user-attested,
   not hardware-attested by the JSON.
2. The physical release Room database was not independently runtime-cross-checked.
3. The all-workout denominator remains partially known because research tables cannot expose missed or
   capture-disabled workouts.
4. Two early saved workouts and their 12 machine-eligible primary rows remain temporally uncertain for
   physical-performance validation.
5. Later app timestamps prove freeze before recorded observations, not the exact moment a physical set was
   lifted.
6. One attempt remains `ACTIVE`; it is below the frozen rate limit but unresolved.
7. Target ambiguity is 68/130, and 33/106 exercise outcomes have no usable first-set confidence.
8. The pre-cutoff raw-history digest is not reconstructable from the export.
9. All WEARABLE-1B statistical gates fail; the evaluator correctly returns `INSUFFICIENT_DATA`.
10. One local user cannot support population validity, device generalization or fairness claims.
11. Newer computer builds/emulator databases are separate development evidence and were not compared as
    pilot records.

None of these limitations is classified as a MUSCLE-4B failure.

## 29. Exact next action

`WEARABLE_2A_HANDOFF_STATE = READY_FOR_READ_ONLY_HEALTH_CONNECT_SHADOW_CONTEXT`

Begin WEARABLE-2A only as a separate read-only shadow-input task. Preserve the frozen canonical export,
both audit runs and this handoff. Health Connect data may be captured as versioned, provenance-aware
context, but it must not modify MUSCLE-4B, Muscle Heat, Progressive Overload, Room/export semantics,
WEARABLE-1B Protocol V1 or production readiness. No wearable modifier, vendor sleep score, structural
damage inference or biological-validation claim is authorized by this audit.

WEARABLE-1D stops here.
