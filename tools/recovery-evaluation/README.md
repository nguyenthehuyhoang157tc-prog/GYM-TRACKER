# WEARABLE-1B recovery evaluation

Offline, deterministic, research-only evaluation of immutable WEARABLE-1A JSON exports. The tool has
no Android, Room, network, backend, Health Connect, or production UI dependency. It never writes to its
input and never changes MUSCLE-4B, Muscle Heat, Progressive Overload, or WEARABLE-1A capture.

## Protocol

The frozen machine-readable protocol is
`protocol/WEARABLE_1B_EVALUATION_PROTOCOL_V1.json`. Its SHA-256 is recorded in
`protocol/WEARABLE_1B_EVALUATION_PROTOCOL_V1.sha256` and every report:

`494ac18b8a92114091464ab1731776def680af40fa26e2cb27a4742748ea6998`

V1 primary analysis is deliberately narrow:

- unit: one target exercise with one eligible linked outcome;
- tracking: `REPS_WEIGHT` only;
- first set: `MODERATE` or better using `MODAL_LOAD_TIE_HEAVIEST_V1`;
- exercise readiness: frozen `primaryReadinessMin`;
- attempted target: canonical match, valid linked first set, actual load within `0.01 kg` of the
  frozen suggested load, and positive reps;
- attainment: actual reps are at least frozen `suggestedMinReps` at the attempted load;
- readiness is an ordinal/model feature, never `readiness / 100` probability.

`SKILL_FORM`, bodyweight, and time/distance tracking remain descriptive or secondary-only because
WEARABLE-1A does not freeze a sufficiently structured comparable V1 target for them.

## Run

Python 3.11+ standard library only:

```powershell
python tools\recovery-evaluation\evaluate.py `
  --input D:\private\wearable1a-export.json `
  --mode audit `
  --output D:\private\wearable1b-audit

python tools\recovery-evaluation\evaluate.py `
  --input D:\private\wearable1a-export.json `
  --mode full `
  --output D:\private\wearable1b-full
```

Optional arguments are `--protocol <path>` and `--seed <integer>`. The default seed is `41701`.
Exit `0` means the evaluator completed and emitted a valid analytical conclusion, including
`INSUFFICIENT_DATA` or `DATA_INTEGRITY_BLOCKED`. Exit `2` means malformed input, protocol failure,
unsupported invocation, or an internal read/write error.

Each run writes:

- `evaluation_report.json` and `evaluation_report.md`;
- `eligibility_ledger.json` with exactly one final state per target;
- `fold_manifest.json` proving cutoff order, outcome availability, and no workout split;
- `metrics.json` with comparator/calibration/incremental metrics when full gates pass.

Canonical artifacts omit wall-clock generation time. The same input, protocol, and seed produce the
same bytes.

## Evaluation design

The six comparators use the same eligible rows and expanding rolling-origin folds:

1. `BASE_RATE_V1`
2. `LAST_OUTCOME_V1`
3. `TIME_SINCE_TRAINED_V1`
4. `PO_HISTORY_ONLY_V1`
5. `MUSCLE_4B_ONLY_V1`
6. `PO_PLUS_MUSCLE_4B_V1`

The central comparison isolates one addition: `PO_PLUS_MUSCLE_4B_V1` has the exact
`PO_HISTORY_ONLY_V1` features plus frozen `primaryReadinessMin`. Numeric normalization, missing-value
handling, ridge-logistic probability mapping, and calibration are fit only on historical training
rows. A training outcome may enter a fold only when it was captured by the test prediction cutoff.
Every workout is an indivisible dependence block.

Full evaluation requires at least 60 eligible workout blocks, 200 exercise outcomes, 100 attainments,
100 underperformance events, 30 rolling folds, three sufficiently represented exercises/status
buckets, and 70% captured-target coverage. These are conservative AURON engineering gates, not a
universal statistical law. Confidence intervals use a fixed-seed 2,000-replicate moving workout-block
bootstrap with block length five.

The only conclusion codes are:

- `DATA_INTEGRITY_BLOCKED`
- `INSUFFICIENT_DATA`
- `NO_INCREMENTAL_SIGNAL`
- `PROMISING_NOT_VALIDATED`
- `BASELINE_SUPPORTED`

No conclusion activates a wearable modifier.

## Tests and synthetic fixtures

```powershell
python tools\recovery-evaluation\fixtures\generate_fixtures.py
python -m unittest discover -s tools\recovery-evaluation\tests -v
```

Generated fixture JSON and run outputs are ignored. Only deterministic generators are committed;
they contain no private workout data. The fixtures cover positive/no/inverted signal, insufficient
data, payload corruption, future leakage, heavy missingness, first-set ambiguity, multiple exercises
per workout, and incompatible model versions.

## Known evidence boundary

The export can verify frozen payload hashes, schema/model versions, internal links, and recorded cutoff
metadata. It cannot independently recompute `inputDigestSha256`, because raw pre-cutoff history is not
exported, and it cannot enumerate workouts never captured while consent/capture was unavailable.
Reports therefore set `raw_history_digest_reconstructable=false` and
`capture_denominator_known=false` rather than inventing coverage.

