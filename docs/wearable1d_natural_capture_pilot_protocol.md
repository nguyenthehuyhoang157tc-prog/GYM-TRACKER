# WEARABLE-1D — Natural Prospective Capture Pilot Protocol

**Protocol ID:** `WEARABLE_1D_PILOT_PROTOCOL_V1`  
**Status:** Frozen operational protocol; pilot activation remains pending until the verified build is installed and Recovery Research consent is enabled.  
**Subject scope:** One local user, prospective natural-use pilot.  
**Natural capture device alias:** `AURON-PILOT-DEVICE-01` — reserved for the user's physical Android phone and bound only after the verified release APK is installed there. No phone model, serial, advertising ID or host identity is a research identifier.  
**QA/preflight device alias:** `QA-PREFLIGHT-DEVICE-PIXEL8-API37` — Pixel 8 API 37 emulator used only for technical preflight. It is never a natural capture device or a source of pilot evidence.  
**Repository:** `D:\HOCTIN\GYM TRACKER`

## 1. Frozen technical identity

| Surface | Frozen value |
|---|---|
| App ID | `com.hoctin.gymtracker` |
| App version | `versionName 1.0`, `versionCode 1` |
| Pilot APK | `app/build/outputs/apk/release/app-release.apk` built during WEARABLE-1D preflight |
| Room schema | `9` |
| Research contract | `WEARABLE_1A_V1` |
| Research export schema | `1` |
| Recovery model | `MUSCLE_4B_BASELINE` |
| Muscle Heat model | `MUSCLE_HEAT_1_STIMULUS_QUALITY` |
| Progressive Overload model | `PROGRESSIVE_OVERLOAD_V3_11D` |
| First-set rule | `TRACKING_AWARE_FIRST_WORKING_SET_V1` |
| Evaluator | `WEARABLE_1B_EVALUATOR_V1` |
| Evaluator protocol | `WEARABLE_1B_EVALUATION_PROTOCOL_V1` |

### Activation build record

This table is completed from the produced artifact before user handoff. A different APK is outside this pilot.

| Field | Frozen value |
|---|---|
| Release artifact | `app/build/outputs/apk/release/app-release.apk` — 90,652,425 bytes |
| APK SHA-256 | `4F8D5757EDA28ACC0E6F995C91D4AC78BF624945FE8ECE4A596E774A9CF8B42C` |
| Signing state | Signed; one signer; APK Signature Scheme v2 verified |
| Signing-certificate SHA-256 | `ED37A53A772F4B143195DDE662991E43BDECAFF83BC411E5AF32FCBA5165D8AA` |
| QA/preflight installation | Installed on `QA-PREFLIGHT-DEVICE-PIXEL8-API37`; installed base APK SHA-256 exactly matches the release artifact; package reports `1.0 (1)`, non-debuggable and `allowBackup=false` |
| Physical pilot installation | `PENDING_PHYSICAL_PHONE_INSTALL_VERIFICATION`; `AURON-PILOT-DEVICE-01` is not bound and the natural pilot is not active until this verification is complete |

## 2. Pilot start boundary

The pilot starts at the first `GRANTED` Recovery Research consent event created **on the physical phone** after all of the following:

1. the exact release APK identified in the activation build record is installed on the physical phone;
2. the physical installation's package version and signing certificate match that record, after which the non-identifying alias `AURON-PILOT-DEVICE-01` is bound to that installation;
3. the physical installation contains no QA or synthetic prediction, target, set-observation, outcome or exercise-outcome rows;
4. Recovery Research is initially disabled; and
5. the user has received the natural-use instructions in section 12.

The first qualifying physical-phone consent event timestamp is the authoritative database boundary. A redacted private pilot ledger records the same boundary, build identity, consent state and aggregate workout counts outside the repository. Attempts or sessions before the boundary are excluded. Exact personal timestamps are not committed.

`QA-PREFLIGHT-DEVICE-PIXEL8-API37` is categorically outside this boundary. Emulator consent timestamps, workouts, research rows and exports—whether created before or after physical activation—are QA-only and must never enter the natural export, pilot denominator or audit evidence.

## 3. Purpose and decision

WEARABLE-1D asks whether naturally occurring AURON workouts create a complete enough and auditable prospective chain:

`frozen prediction → frozen targets → first valid set observation → terminal workout outcome → exercise outcomes → canonical export → deterministic WEARABLE-1B audit`

The pilot will:

- verify prediction/outcome lineage under normal use;
- quantify capture coverage, missingness, unresolved `ACTIVE` records and eligibility;
- produce and byte-preserve the first natural canonical research export;
- verify the export against Room aggregates when the same database is securely accessible, without weakening Android release security;
- run the frozen WEARABLE-1B evaluator twice; and
- decide whether the capture pipeline is ready to receive read-only Health Connect shadow context in a later task.

## 4. Explicit non-purpose

This pilot does **not**:

- prove biological validity or clinical accuracy of MUSCLE-4B;
- tune any formula, constant, threshold, taxonomy or status band;
- add wearable data, Health Connect, a modifier or a readiness UI change;
- treat HRV, sleep, soreness, DOMS or abandonment as recovery ground truth;
- manipulate exercise selection, target attainment, failure or abandonment;
- satisfy the full WEARABLE-1B sample, event or fold gates from 3–10 workouts; or
- generalize from one local user to a population.

## 5. Methodological basis

### Evidence-supported methodology

- Pilot work should focus on feasibility, prespecified progression criteria and uncertainty rather than efficacy claims. This follows the [CONSORT extension for pilot and feasibility trials](https://www.bmj.com/content/355/bmj.i5239) and methodological guidance that a pilot is not a hypothesis test of effectiveness ([Leon, Davis & Kraemer, 2011](https://pmc.ncbi.nlm.nih.gov/articles/PMC3081994/)).
- Counts at every capture stage, missing data and reasons for exclusion must be reported. This follows [STROBE](https://www.equator-network.org/reporting-guidelines/strobe/) and the [RECORD statement](https://journals.plos.org/plosmedicine/article?id=10.1371/journal.pmed.1001885), which specifically emphasizes transparent linkage rates, linkage error and missingness in routinely collected data.
- Provenance is represented as immutable entities and activities with explicit relationships, consistent with the [W3C PROV data model](https://www.w3.org/TR/prov-dm/).
- A future predictive-validation claim requires fit-for-purpose data, adequate outcomes, missing-data analysis, calibration/discrimination and uncertainty. Those requirements belong to later validation, not this capture pilot ([BMJ external-validation guidance](https://www.bmj.com/content/384/bmj-2023-074820), [PROBAST](https://www.probast.org/)).

### AURON operational decisions

- Checkpoint A uses 3–5 natural terminal workouts to catch engineering defects early.
- Checkpoint B uses at least 10 natural terminal workouts as a pipeline milestone.
- Checkpoint C uses approximately 20–30 workouts to look for recurring capture patterns.
- The operational percentages in section 10 are engineering targets, not scientific thresholds.
- WEARABLE-1B V1 remains frozen; no small-sample result may change its tolerance or classification rules.

### One-user limitations

One user cannot establish population calibration, external validity, fairness, device generalization, exercise diversity or robust event-rate estimates. Natural observational data may also be selectively captured: consent state, drafts, skipped sessions, exercise choice and app use can affect which outcomes appear. Counts, missingness and denominator confidence therefore accompany every rate.

### Questions reserved for full validation

Biological validity, effect size, calibration, discrimination, subgroup performance, cross-device portability, wearable incremental value and production readiness remain outside this protocol.

## 6. Natural-use rules

The user:

- trains normally;
- follows or ignores Progressive Overload suggestions naturally;
- does not intentionally fail targets;
- does not intentionally abandon workouts;
- does not create partial sessions only for research;
- does not change exercise selection to increase evaluator eligibility; and
- leaves illness, injury and unusual interruptions as naturally observed events rather than editing them away.

A terminal workout has a linked `COMPLETED` or `PARTIAL` outcome. A confirmed `ABANDONED` outcome may occur naturally but is neither required nor manufactured. `ACTIVE` is not terminal.

## 7. Checkpoints

| Checkpoint | Minimum natural evidence | Purpose |
|---|---:|---|
| A | 3–5 saved terminal workouts | Detect early lineage, export, linkage or missingness defects; no biological evaluation |
| B | At least 10 terminal workouts | First formal real WEARABLE-1B audit and capture-quality decision |
| C | Approximately 20–30 terminal workouts | Assess stability and recurring capture/exclusion patterns before broader testing |

The checkpoints need not occur in one Codex run. The same app installation and Room database remain intact until each checkpoint export succeeds.

## 8. Denominator contract

### Natural workout denominator

Count, where observable:

- production workouts saved inside the pilot interval;
- naturally confirmed discarded attempts;
- unresolved active attempts;
- known workouts performed while research capture was disabled; and
- known late-existing drafts that were correctly skipped because no prospective freeze was possible.

### Research capture numerator

Count:

- frozen predictions;
- target exercises;
- set observations;
- terminal outcomes;
- `ACTIVE` outcomes; and
- exercise outcomes.

### Required metrics

- prediction coverage among known pilot workout attempts;
- terminal linkage rate;
- `ACTIVE` rate;
- target-attempt classification rate;
- eligible-primary rate;
- outcome-linkage failure rate;
- duplicate-attempt count; and
- critical timestamp, payload-hash or internal-link failure count.

Every checkpoint classifies the denominator as:

- `KNOWN_DENOMINATOR`;
- `PARTIALLY_KNOWN_DENOMINATOR`; or
- `UNKNOWN_DENOMINATOR`.

The canonical export proves completeness relative to the research tables, not automatically relative to every workout performed. If production-workout and disabled-capture counts cannot be established, no complete-denominator claim is allowed.

## 9. Canonical export and provenance contract

A natural export is accepted only when it:

1. comes from **Settings → Export Research JSON → Android document picker**;
2. is preserved byte-for-byte and hashed immediately;
3. is neither edited, merged, reformatted nor manually reconstructed;
4. contains supported contract, export, Room and model versions;
5. contains all eight collections:
   - `consent`
   - `predictions`
   - `predictionGroups`
   - `predictionMuscles`
   - `targetExercises`
   - `setObservations`
   - `outcomes`
   - `exerciseOutcomes`
6. passes ID, relationship, payload-hash and timestamp/cutoff validation;
7. is accepted by WEARABLE-1B audit mode; and
8. contains no QA, synthetic or evaluator-fixture records.

Three separate conclusions must be reported:

- `EXPORT_FORMAT_VALID`;
- `EXPORT_ROOM_COMPLETE`; and
- `CAPTURE_DENOMINATOR_COMPLETE_OR_PARTIAL`.

When the original database remains accessible, read-only aggregate comparison covers row counts, distinct prediction and attempt IDs, outcome states, target links, exercise-outcome links, set-observation links and consent state.

Room inspection is not required when Android application sandboxing or release signing prevents secure `run-as` access on the physical phone. In that case:

- do not weaken the build, replace its signing identity, root the phone or copy a private Room database merely to satisfy the cross-check;
- report `EXPORT_ROOM_COMPLETE = NOT_INDEPENDENTLY_RUNTIME_CROSS_CHECKED`;
- do not interpret that label as proof of Room-relative completeness; and
- validate the supported canonical export flow, all eight collections, graph relationships, versions, payload hashes, timestamps/cutoffs, denominator ledger and WEARABLE-1B evaluator compatibility.

## 10. Capture-quality gates

### Hard integrity requirements

All must equal zero:

- duplicate frozen prediction per attempt;
- invalid payload hash;
- future-data or cutoff violation;
- broken internal link;
- incompatible model/protocol version;
- prediction mutation; and
- synthetic/real mixing.

### Operational targets

- prediction coverage among known eligible attempts: preferably at least 95%;
- terminal outcome linkage: preferably at least 90%;
- unresolved `ACTIVE` rate: preferably at most 10%;
- outcome-linkage failure: preferably at most 2%;
- primary eligibility: non-zero and not structurally near zero;
- target-attempt ambiguity: explicitly quantified; and
- skipped captures: reason recorded where observable.

Counts, rates, denominator confidence and reason-level diagnosis are interpreted together. A one-record miss in a ten-workout sample does not by itself fail the pipeline.

### Verdicts

- `CAPTURE_QUALITY_ACCEPTABLE`: no hard error; lineage works; eligibility is non-trivial; missingness is explainable.
- `CAPTURE_QUALITY_ACCEPTABLE_WITH_LIMITATIONS`: no hard error; usable pipeline; one or more rates remain weak or uncertain but measurable.
- `CAPTURE_QUALITY_NEEDS_REPAIR`: recurrent missing predictions, excessive `ACTIVE`, attachment failure, routine first-set loss, structurally near-zero eligibility, unusable denominator, Room/export mismatch or evaluator integrity findings.

Any repair requires an observed reproduction, the smallest bounded change, a focused regression test, a new supported export and independent review. No speculative repair is part of this protocol.

## 11. Private evidence handling

Real exports, Room DB/WAL/SHM files, audit outputs and the pilot ledger:

- stay outside the repository and graphify inputs;
- remain local and are not uploaded;
- are never committed as fixtures;
- are not pasted into README or reports;
- do not expose raw exercise names, loads, repetitions or exact personal timestamps in committed documents; and
- are summarized only as aggregates with redacted identifiers.

For each checkpoint, record privately: pilot boundary, build identity, consent state, known natural workout count, export path and SHA-256. The original export is immutable.

## 12. User handoff — exactly 14 actions

1. Confirm the intended AURON build is installed.
2. Open Settings.
3. Enable Recovery Research.
4. Confirm the disclosure and research state.
5. Use AURON normally.
6. Do not modify training to satisfy the study.
7. Save workouts normally.
8. Do not deliberately create abandoned or failed sessions.
9. Return after 3–5 naturally saved workouts for the first checkpoint.
10. Use Settings → Export Research JSON.
11. Save the file through the Android document picker to a private location.
12. Do not edit, merge, reformat or open-and-resave the JSON.
13. Provide Codex the local path only when ready for the private audit.
14. Keep the same app installation and database until the checkpoint export succeeds.

Codex will not request screenshots of private workout values, raw exercise names in chat, pasted JSON, public upload or deliberate workout failure.

## 13. Audit execution

At a real checkpoint, preserve the input privately and run:

```powershell
python .\tools\recovery-evaluation\evaluate.py `
  --input "<private-real-export.json>" `
  --mode audit `
  --output "<private-real-audit-output>"
```

Run twice. The following outputs must be byte-identical:

- `evaluation_report.json`
- `evaluation_report.md`
- `eligibility_ledger.json`
- `fold_manifest.json`
- `metrics.json`

Record the input, protocol and output artifact SHA-256 values. Report only aggregate distributions for outcomes, target attempts, eligibility/exclusions/abstentions, first-set confidence/rules, tracking types, PO availability, muscle mapping and integrity findings.

## 14. Decision for WEARABLE-2A

Read-only Health Connect shadow capture may be recommended only after Checkpoint B when:

1. a natural canonical export exists;
2. WEARABLE-1B accepts it;
3. critical integrity findings equal zero;
4. prediction/outcome linkage functions;
5. `ACTIVE` is not structurally excessive;
6. eligibility is non-zero and measurable;
7. capture limitations are understood;
8. private-data handling remains compliant; and
9. MUSCLE-4B, Muscle Heat, Progressive Overload and V1 protocols remain frozen.

Full WEARABLE-1B statistical support is not required to begin read-only shadow inputs. If capture quality needs repair, the smallest observed capture defect is addressed first. WEARABLE-2A remains context-only: no production modifier, vendor sleep score, structural-damage inference or readiness UI change.

## 15. Initial task-state rule

If technical preflight passes but no natural export exists, the correct state is:

`AWAITING_USER_CAPTURE`

This state also applies while the verified release has not yet been installed and activated on the physical phone. Emulator preflight never advances it. This is a valid prospective boundary, not a completed real audit and not a failure. No README completion claim or first-real-audit report is permitted until natural evidence exists.
