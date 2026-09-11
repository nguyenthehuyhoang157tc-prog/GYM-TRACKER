# DB-3E — Catalog Source-of-Truth Audit (report only; no code changed)

## 1. Current composition

| Metric | Count |
|---|---|
| Catalog entries (CatalogSeed) | **607** |
| Curated AURON entries (DB-2A..2E; have hand-written `tips`) | **155** |
| Imported free-exercise-db entries (DB-3C; no `tips`) | **452** |
| Enriched curated (AURON metadata + free-exercise-db image) | **94** (76 DB-3B/3B.5 + 18 DB-3D.3) |
| Image-bearing entries | **546** |
| Placeholder (image-less curated) | **61** |
| free-exercise-db dataset total / strength-category | 873 / 581 |

**Curated 155 vs free-exercise-db:** 39 `FED_EXACT`, 42 `FED_EQUIVALENT_RENAME`, **74 `AURON_ONLY`** (no reliable FED equivalent). The 452 imports are, by construction (DB-3C), the FED strength exercises that did **not** match any curated entry — so there is **no per-exercise duplication** (no "curated X" and "import X" as two rows).

## 2. Is the hybrid catalog safe or risky?

**Safe for correctness, the engine, and user data. One moderate UX inconsistency.**

- **Engine (V3.6B) fields are consistent.** Both curated and imported entries derive `defaultIncrementKg`, `sessionsRequiredBeforeIncrease`, `defaultSets/Min/MaxReps`, and `isBodyweight` from the **same** `defaultsFor(progressionType)` helper. So for any given `progressionType` the load-bearing profile fields are identical regardless of origin. The engine reads only `progressionType`, `incrementKg`, `sessionsRequiredBeforeIncrease`, `isBodyweight`, `isUnilateral` (via `profileFromCatalog`) — none of which differ by curated/import style.
- **Deletion is provably safe** (see §4).
- **The real issue — taxonomy granularity (moderate, UX not correctness):** curated entries use **fine-grained** primary-muscle enums that the imports never use:
  - curated-only: `FRONT_DELTS`, `SIDE_DELTS`, `REAR_DELTS`, `UPPER_CHEST`, `BACK`, `CORE`, `OBLIQUES`
  - imports-only: `SHOULDERS` (FED is coarse: `shoulders`/`chest`/`abdominals`/`middle back`)
  - **38 curated entries** carry a fine-grained primary muscle. Consequence: the Catalog Picker **muscle filter is skewed** — filtering "Shoulders" returns the ~93 imported shoulder exercises but **none** of the curated shoulder work (tagged `FRONT_DELTS`/`SIDE_DELTS`); filtering "Front Delts"/"Upper Chest" returns **only** curated. This is the only material inconsistency, and it affects discoverability, not progression or data integrity.

## 3. Metadata / image conflict audit

The enriched-duplicate images are correct (verified in DB-3B/3D.3). The "conflicts" are **granularity differences in muscle metadata**, not wrong images or wrong progression. Representative examples (AURON fine tag vs the matched FED-source's coarse tag — same exercise, same image):

| AURON exercise | AURON primary | FED source primary | Note |
|---|---|---|---|
| Conventional Barbell Deadlift | BACK, LOWER_BACK | lower back | `BACK` is a curated-only enum; overlaps on lower back |
| Seated Dumbbell Shoulder Press | FRONT_DELTS, SIDE_DELTS | shoulders | filter "Shoulders" misses it |
| Arnold Press | FRONT_DELTS, SIDE_DELTS | shoulders | same |
| Barbell Incline Bench Press | UPPER_CHEST | chest | filter "Chest" misses it |
| Cable Face Pull | TRAPS, REAR_DELTS | shoulders | finer (and arguably more correct) than FED |

Load-bearing fields (`equipment`, `progressionType`, increments, `isBodyweight`, `isUnilateral`) showed **no engine-affecting conflicts** — AURON's fine taxonomy is generally *better* than FED's coarse one, so the recommendation is to **keep AURON metadata and reconcile only the filter**, not overwrite AURON with FED.

## 4. Does deleting catalog entries affect user Plan / History? — No (verified in code)

- **Plan exercises are snapshots.** `CatalogPickerViewModel.add()` → `GymRepository.addExercise(name, sets, reps)` copies values; there is **no stored FK from a plan exercise back to the catalog** (DB-1 rule).
- **Catalog table is independent.** `CatalogExerciseEntity` doc: *"never read or written by the workout/session/log tables… Progression V3, or Import/Export."*
- **Re-seed already clears the whole catalog.** `ExerciseCatalogRepository.ensureSeeded()` does `dao.clearAll()` then `dao.insertAll(...)` on **only** `catalog_exercises`, on every `CONTENT_VERSION` change — this has happened 9× with zero user-data impact.
- **Engine degrades gracefully.** `LogWorkoutViewModel` builds the progression profile via `catalogRepository.findByName(name)?.let { profileFromCatalog(...) }`. If a catalog entry is deleted, `findByName` returns null → the engine falls back to **name-based inference** (exactly as for any custom exercise). No crash, no history loss, just a slightly less precise progression profile.

**Conclusion:** deleting catalog entries is technically safe, but **not recommended** — the curated 155 are the quality layer (hand-written tips, aliases, fine taxonomy, tuned progression).

## 5. Recommended source-of-truth policy

**Adopt the override model (Option 3 / D), implemented conservatively (Option 1 near-term). Do NOT rebuild from FED (Option 2 rejected).**

- **free-exercise-db = canonical backbone** for exercise *identity and images* (it already provides 452 entries + all images).
- **AURON curated entries = explicit, intentional overrides**, kept because they are genuinely better: hand-written `tips`, `aliases`, finer muscle taxonomy, and tuned `progressionType`. The **74 AURON_ONLY** entries (no FED equivalent) are intentional keeps.
- **Reconcile, don't overwrite.** The only fix needed now is the muscle-filter granularity mismatch — solved by making filtering *granularity-aware* (a fine enum implies its coarse parent), **not** by flattening AURON's superior taxonomy or deleting entries.
- Overrides must be **explicit** (documented), never accidental.

Rejected: **Option 2 (FED canonical rebuild)** would discard curated tips/aliases/fine taxonomy/tuned progression for ~81 exercises and force plan exercises onto name-inference — high effort, real quality loss, low upside.

## 6. Exact DB-3E.1 implementation plan (conservative, non-destructive)

1. **Fix muscle-filter granularity** in the **search layer only** (`CatalogSearch`/picker) — when filtering by a coarse muscle, include its fine children, and vice-versa:
   - `SHOULDERS` ⊇ {FRONT_DELTS, SIDE_DELTS, REAR_DELTS}; `CHEST` ⊇ {UPPER_CHEST}; `ABS` ⊇ {OBLIQUES, CORE}; `BACK`/`MID_BACK`/`LOWER_BACK`/`LATS` treated as a back family for filtering.
   - **Do not change any entry's stored `primaryMuscles`** (and therefore nothing the engine reads). Pure filter/search behavior.
2. **Add a catalog self-consistency unit test:** every entry has ≥1 primary muscle + instructions; image-bearing entries have exactly 2 asset frames; `defaultIncrementKg`/`sessionsRequiredBeforeIncrease`/`defaultSets/Min/Max` equal `defaultsFor(progressionType)`; slugs unique; total = 607.
3. **Document the policy** (`docs/catalog_source_of_truth.md` or a `CatalogSeed` header): FED backbone + explicit AURON overrides; list the 74 AURON_ONLY keeps.
4. **No deletions, no metadata overwrites, no image changes, no schema change.** `CONTENT_VERSION` bump only if `primaryMuscles` are *displayed* differently (the filter fix alone may not need a re-seed if it's pure UI filtering on the in-memory list — confirm at implementation).

## 7. Risks & rollback

- **Risk:** the filter-granularity change must stay in the search layer; touching stored `primaryMuscles` is unnecessary and could (cosmetically) change the popup's Muscle Breakdown — keep it filter-only.
- **Risk (Option 2 only, not chosen):** rebuilding from FED loses curated quality and pushes plan exercises to name-inference.
- **Rollback:** timestamped `app/src` backup before DB-3E.1; the change is a small, isolated search-layer edit + a test; revert the file(s) and (if bumped) the `CONTENT_VERSION` to restore. The catalog fully re-seeds on every version, so rollback is deterministic.
