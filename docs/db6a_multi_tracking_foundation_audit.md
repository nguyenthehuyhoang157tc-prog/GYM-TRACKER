# DB-6A — Multi-Tracking Foundation Audit (audit / design only)

Design for the **minimal** data + logging foundation that lets non-rep exercises (sled, carry,
time/distance, holds) live in the app alongside the existing reps/kg/RIR model. **Nothing was changed**
— no source, Room schema, catalog JSON, or engine. Companion: `docs/db6a_tracking_type_contract.csv`.

## 1. Current state (inspected)

| Component | Current shape | Relevance |
|---|---|---|
| `LoggedSetEntity` (table `logged_sets`) | `weight: Double`, `reps: Int`, `rir: Int` (**all NOT NULL**), notes?, isPrWeight/Reps | the only place a set's numbers live |
| `ExerciseEntity` (table `exercises`, the Plan) | name, targetSets/Min/MaxReps, sortOrder — **no tracking type, no catalog link** | log-time profile is resolved by name lookup |
| `CatalogExercise` model | progressionType (COMPOUND_LOAD/ISOLATION_SLOW/BODYWEIGHT/SKILL_FORM), isBodyweight — **no `trackingType`** | drives engine, not "what to log" |
| Export/Import (`GymRepository`) | `schemaVersion 1`; set keys `weight/reps/rir/...`; **`readiness` added as an optional key without bumping the version** (tolerant `optNullableString`) | the additive precedent to follow |
| Draft (`WorkoutDraftRepository`) | SharedPreferences JSON, own `SCHEMA_VERSION 1`; `DraftSet(weight, reps, rir, notes)` raw strings | no Room migration involved |
| Log Workout UI | per-set **REPS / KG / RIR** columns; volume `Σ w×r` (`LogWorkoutScreen`) | needs per-type field switching |
| History | SET | VOLUME | REPS | RIR table; volume `Σ w×r` (`Formatters.totalVolume`) | needs per-type rows |
| Weekly Progress | ring counts **days with ≥1 session** (not volume); Weekly Hub shows kg-volume `Σ w×r` | see key insight below |
| Engine profile input | `findByName(name)` → `profileFromCatalog(progressionType, …)`; reads each set's `weight/reps/rir` | rep-based; no time/distance concept |

**Key insight (volume safety):** every volume figure in the app is `Σ weight × reps`. If a time/distance
set stores **`reps = 0`**, its `weight × reps = 0`, so it **contributes nothing to kg-volume and cannot
corrupt the existing math** — Weekly Hub, History totals, and the engine's volume delta all stay correct
with zero special-casing. This makes `reps = 0` the safe sentinel and avoids making `reps` nullable.

## 2. Minimal schema additions

| Add | Where | How | Why |
|---|---|---|---|
| `trackingType` (TEXT, NOT NULL, default `REPS_WEIGHT`) | `catalog_exercises` table + `CatalogExercise` + JSON | additive `ALTER TABLE ADD COLUMN` (then re-seeded from JSON on `CONTENT_VERSION` bump) | tells the Log UI which fields to show |
| `durationSeconds` (INTEGER, **nullable**) | `logged_sets` | additive `ALTER TABLE ADD COLUMN` | time-based sets |
| `distanceMeters` (REAL, **nullable**) | `logged_sets` | additive `ALTER TABLE ADD COLUMN` | distance-based sets |

Decisions:
- **Load reuses `weight`.** Sled/carry/weighted-plank load is stored in the existing `weight` column — no
  new "load" column.
- **`reps` stays NOT NULL; use `0`** for non-rep sets (sentinel, keeps volume math safe — see above). Do
  **not** make `reps` nullable (would force a destructive column rebuild).
- **`rir` stays NOT NULL; use `0`** for non-rep sets and **hide the field in the UI** for those types.
- **`trackingType` is NOT added to `ExerciseEntity`** for the MVP — it is resolved at log time via the
  catalog (`findByName`), exactly like the engine profile; custom/unmatched exercises default to
  `REPS_WEIGHT`. (A plan-owned column can be added later if catalog drift becomes an issue.)

Net: **3 additive columns, no destructive change, no `fallbackToDestructiveMigration`** — the same
pattern as the proven `readiness` migration (DB v3→v4).

## 3/4. Tracking types — per-type contract
Full machine-readable table in `docs/db6a_tracking_type_contract.csv`. Summary:

| Type | Log fields | Stored (besides reps=0/rir=0) | History row | Weekly volume | Export keys added | Draft fields | Progression placeholder |
|---|---|---|---|---|---|---|---|
| REPS_WEIGHT | Reps, Kg, RIR | weight, reps, rir | SET·VOL·REPS·RIR | w×r (current) | — | w,r,rir | full V3.x |
| BODYWEIGHT_REPS | Reps, +Kg, RIR | weight(added), reps, rir | SET·REPS·(+KG)·RIR | added w×r | — | w,r,rir | BODYWEIGHT rule |
| TIME_ONLY | Duration | durationSeconds | SET·TIME | 0 kg | durationSeconds | d | "beat your hold time" |
| TIME_LOAD | Duration, Kg | durationSeconds, weight | SET·TIME·KG | 0 kg | durationSeconds | d,w | placeholder |
| DISTANCE_LOAD | Distance, Kg | distanceMeters, weight | SET·DIST·KG | 0 kg | distanceMeters | dist,w | placeholder |
| TIME_DISTANCE | Time, Distance | durationSeconds, distanceMeters | SET·TIME·DIST | 0 kg | both | d,dist | placeholder |
| TIME_DISTANCE_LOAD | Time, Distance, Kg | durationSeconds, distanceMeters, weight | SET·TIME·DIST·KG | 0 kg | d,dist,w | both | placeholder |
| SKILL_FORM | Reps, Kg, RIR(opt) | weight, reps, rir | SET·REPS·KG·RIR | w×r | — | w,r,rir | conservative (no auto-load) |

**`trackingType` vs `progressionType` (orthogonal):** `trackingType` decides *what fields to log*;
`progressionType` decides *how the engine progresses*. The current 568 catalog entries map to
`REPS_WEIGHT` (COMPOUND_LOAD/ISOLATION_SLOW), `BODYWEIGHT_REPS` (BODYWEIGHT), or `SKILL_FORM` — **no
time/distance type is used until DB-6E imports sleds/carries**, so DB-6B/6C/6D ship with zero behavior
change for the existing catalog.

**Progression placeholder:** for the time/distance types the engine has no rule. The Log card shows a
static hint ("Track your time/distance — auto-progression coming later") instead of a suggestion; the
ViewModel simply does **not** call the rep-based engine for those types. No engine code changes in the
6B–6E phases.

**PR detection:** `isPrWeight`/`isPrReps` stay rep/weight-based; time/distance PRs (best time/distance)
are deferred (no PR shown for those types initially).

## 5. Migration risk

| Risk | Assessment |
|---|---|
| Room version bump | **Yes — v4 → v5**, one additive migration adding the 3 columns. |
| Existing data compatibility | **Full.** Old `logged_sets` get `durationSeconds`/`distanceMeters` = NULL; old catalog rows get `trackingType` default `REPS_WEIGHT` (then re-seeded with real values on the `CONTENT_VERSION` bump). reps/weight/rir untouched. **No destructive fallback.** |
| Export/Import schema | Add optional `durationSeconds`/`distanceMeters` set keys; importer reads them with `optInt`/`optDouble` defaulting to null/0. Old exports (no keys) import fine. **Recommend bumping `schemaVersion` 1 → 2** for clarity (importer stays tolerant of v1 and v2). |
| Draft | Add optional `d`/`dist` to `DraftSet` JSON; bump draft `SCHEMA_VERSION` 1 → 2; parse with `optString` so old drafts load. |
| Catalog re-seed | `trackingType` is added to the JSON for all 568 entries; bump `CatalogSeed.CONTENT_VERSION` so the table re-seeds. |
| Tests needed | (a) Room v4→v5 migration preserves a populated DB (mirror the readiness on-device upgrade check); (b) `CatalogExercise.trackingType` JSON round-trip + every entry has a valid type; (c) `LoggedSetEntity` duration/distance round-trip incl. nulls; (d) export→import round-trip with the new keys (and a v1 export still imports); (e) volume math: a time/distance set (reps=0) adds 0 kg-volume; (f) Log UI shows the correct fields per `trackingType`; (g) History renders each type; (h) draft round-trip with new fields; engine suite unchanged. |

## 6. Outputs
- `docs/db6a_multi_tracking_foundation_audit.md` (this file)
- `docs/db6a_tracking_type_contract.csv` (8 tracking types × log/stored/history/weekly/export/draft/progression)

## 7. Recommended implementation phases

| Phase | Scope | Risk |
|---|---|---|
| **DB-6B — schema + model** | Room **v4→v5** additive migration (+`trackingType` on catalog, +`durationSeconds`/+`distanceMeters` on `logged_sets`); add `TrackingType` enum; `CatalogExercise` + JSON loader/export field; backfill `trackingType` for all 568 (derive from progressionType); migration + round-trip tests. **No UI, no new exercises.** | low (additive; readiness precedent) |
| **DB-6C — Log Workout UI** | Per-`trackingType` field rendering (REPS/KG/RIR ↔ DURATION/DISTANCE/LOAD), hide reps/RIR for non-rep types, draft fields, the progression-placeholder hint. Existing types render identically. | medium (UI only) |
| **DB-6D — History / Export / Import / Weekly** | Per-type History rows; export/import the new optional keys (`schemaVersion` 2, tolerant of 1); Weekly Hub shows time/distance stats and keeps kg-volume correct (reps=0 ⇒ 0). | low-medium |
| **DB-6E — import carry/sled/time exercises** | Import the 9 DB-5E `WAIT_FOR_TRACKING` entries (8 sleds + Farmer's Walk) with their `trackingType` (TIME_DISTANCE_LOAD / DISTANCE_LOAD); bump `CONTENT_VERSION`; coverage tests. | low (data-only, now unblocked) |

**Sequencing note:** 6B is purely additive and ships invisibly (no exercise uses a new type yet), so it
can land safely without 6C–6E. 6E must come last (the new exercises only render correctly once 6C/6D
support their fields).

## Confirmation
No source code, Room schema/version, catalog JSON, assets, or engine were modified. Only two docs were
created (`db6a_multi_tracking_foundation_audit.md`, `db6a_tracking_type_contract.csv`).
