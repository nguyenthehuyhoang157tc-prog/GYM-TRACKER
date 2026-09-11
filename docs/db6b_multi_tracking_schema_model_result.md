# DB-6B — Multi-Tracking Schema + Data Model Foundation — COMPLETED ✅

Implemented the additive data/model foundation for non-rep exercises. **No UI wiring, no engine change,
no behavior change for the current 568 catalog exercises.** Unit tests + debug + release builds green.

## Backup
`D:\HOCTIN\GYM_TRACKER_DB6B_BACKUP_20260626_122627` (pre-change `app/src/main/java`, `app/src/test`,
`app/src/main/assets/catalog`).

## Files changed
- `data/catalog/CatalogTaxonomy.kt` — new **`TrackingType`** enum (8 values + safe `fromName`).
- `data/catalog/CatalogExercise.kt` — `trackingType: TrackingType = REPS_WEIGHT` (default-safe).
- `data/catalog/CatalogExerciseEntity.kt` — `trackingType` column + `toEntity`/`toDomain` mapping.
- `data/catalog/CatalogJsonLoader.kt` — parse `trackingType` (tolerant default).
- `data/catalog/CatalogSeed.kt` — `CONTENT_VERSION` **14 → 15**.
- `assets/catalog/catalog_fed_v1.json` — added `trackingType` to all **568** entries (count unchanged).
- `data/Entities.kt` — `LoggedSetEntity` gains nullable `durationSeconds: Int?`, `distanceMeters: Double?`.
- `data/AppDatabase.kt` — `@Database version` **4 → 5** + additive **`MIGRATION_4_5`**.
- `data/GymRepository.kt` — export `schemaVersion` **1 → 2** + duration/distance set keys; tolerant import + `optNullableInt`/`optNullableDouble`.
- `data/WorkoutDraftRepository.kt` — `DraftSet` gains `duration`/`distance` (default ""); serialize/parse; draft `SCHEMA_VERSION` **1 → 2**.
- `ui/Formatters.kt` — `formatDuration(seconds)` / `formatDistance(meters)` helpers (not wired to UI).
- `test/.../CatalogSearchTest.kt` — trackingType assertions + entity round-trip.
- `test/.../MultiTrackingFoundationTest.kt` — new (volume invariant, entity/draft defaults, formatters).
- generated `app/schemas/.../5.json`.

## Room version change & migration SQL
`@Database version` **4 → 5**. `MIGRATION_4_5` (additive, no destructive fallback):
```sql
ALTER TABLE logged_sets ADD COLUMN durationSeconds INTEGER;   -- nullable
ALTER TABLE logged_sets ADD COLUMN distanceMeters  REAL;      -- nullable
DROP TABLE IF EXISTS catalog_exercises;                       -- reference data only
CREATE TABLE catalog_exercises (... existing 20 cols ..., trackingType TEXT NOT NULL, PRIMARY KEY(slug));
```
- `logged_sets` ALTERs are in-place — **every existing session/set is preserved** (old rows get NULL),
  the same proven shape as the `readiness` v3→v4 migration.
- `catalog_exercises` is reference data always re-seeded by `ensureSeeded`, so DROP+CREATE is safe (same
  pattern as `MIGRATION_2_3`); the `CONTENT_VERSION 15` bump repopulates it with real `trackingType`
  values on next launch.
- **Cross-checked against the Room-generated `5.json`:** the migration's `catalog_exercises` CREATE and
  the `logged_sets` nullable columns match Room's expected v5 schema exactly → the runtime migration
  validation will pass.

## Catalog trackingType summary
All 568 entries tagged (derived from `progressionType`, with the two isometric holds re-typed):

| trackingType | Count | Source |
|---|---|---|
| REPS_WEIGHT | 471 | COMPOUND_LOAD / ISOLATION_SLOW |
| BODYWEIGHT_REPS | 87 | BODYWEIGHT (Pull-Up, Pushups, Dips, …) |
| SKILL_FORM | 8 | DB-5F olympic/skill fallback (Snatch, Clean and Jerk, Plate Pinch, …) |
| TIME_ONLY | 2 | Plank, Side Bridge (isometric holds) |

Spot-checks: Side Lateral Raise → REPS_WEIGHT; Pushups → BODYWEIGHT_REPS; Plank / Side Bridge →
TIME_ONLY; Snatch / Clean and Jerk / Hang Clean → SKILL_FORM. (No TIME_LOAD / DISTANCE_* / TIME_DISTANCE
entries yet — those arrive with the DB-6E sled/carry imports.)

## CONTENT_VERSION change
`CatalogSeed.CONTENT_VERSION` **14 → 15** (catalog re-seeds on next launch — reference data only).

## Export/Import schema change
`schemaVersion` **1 → 2**. Each `loggedSets` object now also exports optional `durationSeconds` /
`distanceMeters` (NULL for normal reps/kg sets). The importer is **backward compatible**: missing keys
(any v1 export) decode to `null` via `optNullableInt`/`optNullableDouble` (`has && !isNull`), and the
workoutDays/exercises/workoutSessions shapes are unchanged. A v1 export still imports correctly.

## Draft compatibility
`DraftSet` gains `duration`/`distance` (raw strings, default ""), serialized as `d`/`dist`; draft
`SCHEMA_VERSION` 1 → 2. Old drafts without those keys load fine (`optString` → ""); existing reps/kg/RIR
drafts behave identically; both `DraftSet` call sites compile unchanged (default args).

## Tests / builds
| Item | Result |
|---|---|
| `:app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `:app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Engine suite | ✅ pass (engine + its tests not modified) |

New/updated JVM tests: catalog `trackingType` present + known values + entity round-trip;
`TrackingType.fromName` fallback; **volume invariant** (a `reps=0` set adds 0 to `Σ weight×reps`; mixed
session counts only rep-based sets); `LoggedSetEntity` duration/distance default null; `DraftSet`
duration/distance default ""; formatter output.

### APK size before → after
| APK | Before | After | Δ |
|---|---|---|---|
| Debug | 96,979,522 B | 97,139,309 B | +159,787 B (+0.15 MB) |
| Release | 89,441,290 B | 89,445,094 B | +3,804 B (negligible) |

## Confirmation: existing 568 behavior unchanged
- **No UI reads `trackingType` and no UI reads the new set columns in DB-6B** — Log Workout, History,
  Plan, Weekly Progress render exactly as in DB-5F. The two TIME_ONLY holds still log reps/kg until
  DB-6C wires the time field.
- **Engine unchanged** — it reads `weight/reps/rir`; the new `LoggedSetEntity` columns are nullable and
  ignored; engine source + tests untouched; all engine tests pass.
- **Volume math unchanged** — `Σ weight×reps`; the `reps=0` sentinel for future time/distance sets adds
  0 (test-verified). No current entry uses `reps=0`.
- **Migration is purely additive** — existing user plans/sessions/sets preserved; no destructive fallback.

## Known limitations
- Room migration + export/import are Room/DAO-backed, so they're verified by **build + the v5-schema
  cross-check + (deferred) on-device upgrade QA**, not the pure-JVM test harness — same approach as the
  readiness v3→v4 migration. The on-device v4→v5 upgrade smoke test belongs to DB-6C/6D QA.
- `trackingType` is **not** stored on `ExerciseEntity` (the Plan); it is resolved at log time via the
  catalog (`findByName`) in DB-6C, and custom/unmatched exercises default to `REPS_WEIGHT`.
- Plank / Side Bridge are now typed TIME_ONLY but still log as reps until DB-6C; their pre-existing
  rep-based history stays rep-based.
- `reps`/`rir` remain NOT NULL (0 sentinel) by design; no nullable-column rebuild.

## Next step — DB-6C (Log UI)
Wire `trackingType` into the Log Workout card: render REPS/KG/RIR vs DURATION/DISTANCE/LOAD per type,
hide reps/RIR for non-rep types, surface the duration/distance draft fields and the progression
placeholder hint — without touching the engine.
