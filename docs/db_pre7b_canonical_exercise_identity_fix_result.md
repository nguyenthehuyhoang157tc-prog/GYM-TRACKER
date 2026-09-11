# DB-PRE7B — Canonical Exercise History Identity Fix — COMPLETED ✅

Fixes the DB-PRE7 `BROKEN` finding: the same exercise in different workout days now shares
previous-performance / progression / PR history. History/PR/previous lookups are keyed on a new
catalog-aware **canonical identity** (`canonicalExerciseKey`) instead of the day-specific plan
`ExerciseEntity.id`. Additive Room v5→v6 migration (no destructive change), one-time idempotent backfill,
**no Progression Engine logic change** (only the history list passed into it changed), History snapshots
left immutable. Unit tests + debug + release green; on-device upgrade + cross-day QA verified.

## 1. Backup
`D:\HOCTIN\GYM TRACKER\_backup_db_pre7b_canonical_identity_20260701_004538` (pre-change
`app/src/main/java`, `app/src/test/java`, `app/schemas`, `README.md`).

## 2. Files changed
**New**
- `data/catalog/CanonicalExerciseKey.kt` — the centralized resolver (`resolve` → `catalog:<slug>` /
  `custom:<normalized-name>`), `catalogSlugsByName(...)` (name+alias → slug map), and
  `loggedSetMatchesCanonical(...)` (pure mirror of the key-scoped SQL `WHERE`, for tests).
- `data/CanonicalKeyBackfill.kt` — flag (`canonical_exercise_key_backfill_v1_done`) + `run(...)`.
- `test/.../data/catalog/CanonicalExerciseKeyTest.kt` — resolver + cross-day identity + fallback tests.

**Modified**
- `data/Entities.kt` — `LoggedSetEntity.canonicalExerciseKey: String? = null` + `Index("canonicalExerciseKey")`.
- `data/AppDatabase.kt` — `version = 6` + additive `MIGRATION_5_6` (registered).
- `data/GymDao.kt` — key-scoped reads (`observeExerciseHistoryByCanonicalKey`,
  `getRecentSetsForCanonicalKey`, `maxWeightForCanonicalKey`, `maxRepsForCanonicalKey`) + backfill queries
  (`distinctNamesMissingCanonicalKey`, `backfillCanonicalKeyForName`). Old id-scoped queries kept.
- `data/GymRepository.kt` — `ExerciseLogInput.canonicalExerciseKey`; save persists the key + key-based PR
  baseline; `recentSetsForExerciseKey` / `observeExerciseHistoryByKey`; `backfillCanonicalKeys`;
  export `schemaVersion 2→3` + `canonicalExerciseKey` key; tolerant import.
- `data/catalog/ExerciseCatalogRepository.kt` — `canonicalSlugsByName()` / `canonicalKeyFor(name)`.
- `ui/log/LogWorkoutViewModel.kt` — resolve a key per plan exercise (catalog slug map built once); history
  via `recentSetsForExerciseKey`; persist the key on save.
- `ui/exercise/ExerciseDetailViewModel.kt` — history via `observeExerciseHistoryByKey` (`flatMapLatest`);
  removed-from-plan still falls back to the plan-row id history.
- `ui/settings/SettingsViewModel.kt` (+ `ui/GymTrackerApp.kt` factory) — re-run the backfill after Import.
- `GymTrackerApplication.kt` — `runCanonicalKeyBackfillOnce()` at startup (after catalog seed).
- `test/.../data/MultiTrackingFoundationTest.kt` — export/import key round-trip + v1/v2-missing → null.
- generated `app/schemas/.../6.json`.

## 3. Schema / migration summary
Room `@Database` **v5 → v6**. `MIGRATION_5_6` is **purely additive** (no destructive fallback, no
`fallbackToDestructiveMigration`):
```sql
ALTER TABLE logged_sets ADD COLUMN canonicalExerciseKey TEXT;            -- nullable; old rows stay NULL
CREATE INDEX IF NOT EXISTS index_logged_sets_canonicalExerciseKey
    ON logged_sets (canonicalExerciseKey);
```
Cross-checked against the Room-generated `6.json`: the column (`canonicalExerciseKey TEXT`, nullable) and
the index name/definition match exactly, so the runtime migration validation passes. Every existing
session/set is preserved in place (same proven shape as the readiness v3→v4 and duration/distance v4→v5
additive columns).

## 4. Canonical key format
Resolved once from the exercise name via the catalog name+alias → slug map:
- `catalog:<slug>` when the name (or an alias) matches a FED catalog entry — e.g.
  `Incline Machine Chest Press` → `catalog:incline-machine-chest-press`; the alias
  `Dumbbell Lateral Raise` → `catalog:side-lateral-raise` (same key as `Side Lateral Raise`).
- `custom:<normalized-name>` otherwise — e.g. a user exercise `My Secret Lift` → `custom:my-secret-lift`
  (trim + lowercase, internal whitespace → hyphens).

`exerciseId` (plan-row relation / rollback context) and `exerciseNameSnapshot` (historical display name)
are **unchanged**. One resolver (`CanonicalExerciseKey`) is the single source of truth, used by the save
path, previous-performance, progression, PR, Exercise Detail, the backfill, and import.

## 5. Backfill behavior
On first launch after upgrade (and after Import), `CanonicalKeyBackfill` resolves a key for every
`logged_sets` row whose `canonicalExerciseKey` is null/blank, grouped by distinct `exerciseNameSnapshot`
(slug where the catalog matches, else custom). Guarded by `canonical_exercise_key_backfill_v1_done`;
**idempotent** (only null/blank rows are touched, resolution is stable), History display names are never
rewritten, and no row is deleted. Verified on real data: 20 pre-v6 rows → `Plank`=`catalog:plank` (19),
`Sled Push`=`catalog:sled-push` (1), 0 rows left null.

## 6. Lookup paths changed (now canonical, was `exerciseId`)
| Surface | Before | After |
|---|---|---|
| Log "Previous" chips + default weights | `recentSetsForExercise(id)` | `recentSetsForExerciseKey(key, name)` |
| Log progression-engine input | per-id history | canonical history list (engine code unchanged) |
| Log "Potential PR" | per-id history max | canonical history max |
| Save-time PR baseline | `maxWeight/RepsForExercise(id)` | `maxWeight/RepsForCanonicalKey(key, name)` |
| Exercise Detail history + suggestion | `observeExerciseHistory(id)` | `observeExerciseHistoryByKey(key, name)` |

Each key-scoped query also tolerates not-yet-backfilled rows via a `canonicalExerciseKey IS NULL AND
TRIM(LOWER(exerciseNameSnapshot)) = :normalizedName` fallback, so cross-day history is correct even before
the backfill completes. History/Weekly/Home remain snapshot displays (group by `exerciseNameSnapshot`) —
unchanged.

## 7. Cross-day scenario result
**Same exercise in two days now shares history.** Runtime-verified: `Plank` had 19 sets logged only under
**Push**. After adding `Plank` to **Full Body** (a different plan day, different `ExerciseEntity.id`) and
opening its Log card, the **Previous** section showed the Push sets (`00:03`, `01:30`, `30:12`,
`01:00:00 duration`) and the trend read "Duration tracking" (not "First session"). Both plan rows resolve
to `catalog:plank`. Unit test `crossDay_sameExerciseInTwoDays_sharesHistory` asserts the same at the logic
level (two `ExerciseEntity` ids → one key → Push sets visible to Full Body).

## 8. Export/import compatibility
- Export is now `schemaVersion 3` and includes `canonicalExerciseKey` per logged set.
- Import is backward compatible: v1/v2 exports (no key) decode to `null`; **after import the backfill
  re-runs** (SettingsViewModel) so imported old rows get keys immediately. v3 exports preserve the key.
- `replaceAllData` and all other shapes unchanged. Round-trip + v1/v2-missing→null are unit-tested.

## 9. Test / build results
| Gate | Result |
|---|---|
| `.\gradlew.bat :app:testDebugUnitTest` | ✅ **BUILD SUCCESSFUL** — 144 tests, 0 failures |
| `.\gradlew.bat :app:assembleDebug` | ✅ **BUILD SUCCESSFUL** |
| `.\gradlew.bat :app:assembleRelease` | ✅ **BUILD SUCCESSFUL** (signed; keystore untouched) |
| Generated `6.json` vs `MIGRATION_5_6` | ✅ column + index match exactly |

New tests: resolver (catalog name→slug, alias→same slug, custom, normalization), cross-day identity, alias
cross-day, different-names-don't-merge, null-key name fallback; export/import canonical-key round-trip +
v1/v2-missing→null. Engine + its tests untouched and passing. APK: debug 98,184,621 B; release 90,308,294 B.

## 10. Runtime QA result (Pixel 8, real v5→v6 upgrade)
| Check | Result |
|---|---|
| Install v6 over the existing v5 install; launch | ✅ no crash (migration ran) |
| `PRAGMA user_version` | ✅ 6 |
| `logged_sets.canonicalExerciseKey` column + `index_logged_sets_canonicalExerciseKey` | ✅ present |
| Existing 20 sets preserved (no data loss) | ✅ |
| Backfill resolved all rows (0 null) — `Plank`→`catalog:plank`, `Sled Push`→`catalog:sled-push` | ✅ |
| Snapshot names untouched (`Plank`, `Sled Push`) | ✅ |
| **Cross-day**: Plank added to Full Body shows Push's Previous durations | ✅ |
| History display still renders by snapshot name; weekly totals intact | ✅ |
| Crash buffer (`logcat -b crash`) for the package | ✅ EMPTY (0) |

QA note: adding Plank to Full Body leaves a permanent Plank plan row in Full Body (intended test action;
emulator data only). The end-to-end Save-button round-trip was not needed for this proof — Plank already
had saved Push history, so opening Full Body's card alone demonstrates the cross-day lookup.

## 11. Known limitations
- **Pre-rename history:** the DB-4B.2 plan-name migration renamed plan rows but not history snapshots. A
  set logged under a legacy name backfills to that legacy name's key; if the current plan name resolves to
  a different catalog slug, that old session won't link. No regression (it never linked before); an alias
  map could close this later.
- **Name collisions:** two genuinely different exercises that share a normalized name (or alias) merge
  history — acceptable/expected (catalog names are canonical; identical custom names are user intent).
- **PR semantics:** PR baseline is now the cross-day max (the correct behavior); newly saved PR flags can
  differ from the old per-day flags. Existing stored flags are not rewritten.
- **TIME_ONLY progression target** still reads "Log first session" because the engine only progresses
  rep>0 history — unchanged engine behavior, independent of this fix (the Previous sharing is the signal).
- Room migration/backfill are DAO-backed, so they are verified by build + the `6.json` cross-check + the
  on-device upgrade QA above (the JVM harness has no Room/Robolectric), mirroring DB-6B.

## Hard-stop checklist (none triggered)
Additive migration only (no destructive change / no `fallbackToDestructiveMigration`); backfill is safe +
idempotent; History snapshots not rewritten; **no Progression Engine logic change**; the cross-day test
passes; tests + debug + release all green.

## Confirmation
No Progression Engine logic, no catalog JSON/`CONTENT_VERSION` (stays 16), no signing secrets were
changed. The Room change is additive (v6). Existing 577 catalog exercises and all prior history behave as
before; the only behavioral change is the intended cross-day identity sharing. DB-7 not started.
