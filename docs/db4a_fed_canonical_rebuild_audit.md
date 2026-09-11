# DB-4A — FED Canonical Rebuild Audit & Migration Plan (report only; no code changed)

## Summary

The current catalog is a hybrid of curated AURON entries and imported free-exercise-db (FED) entries.
The decided end-state is **FED-first**: FED is canonical for identity/name/equipment/muscles/instructions/
images; AURON keeps only **explicit, reviewed overrides**. This report maps every current catalog entry
to FED, audits the Plan/template/draft/History migration impact, confirms the Engine V3.6B contract, and
specifies a safe DB-4B plan. **Nothing was deleted, seeded, or modified.**

## 1. Current catalog composition

| Metric | Value |
|---|---|
| Catalog entries | **607** |
| Curated AURON (have hand-written `tips`) | **155** (94 enriched with FED images + 61 image-less) |
| Imported FED (no `tips`) | **452** |
| `CONTENT_VERSION` | **9** |
| Image folders / files / size | 546 / 1092 / **65 MB** |
| Release APK | **82.82 MB** |

## 2. FED supported set (rebuild candidate pool)

From `dist/exercises.json` (873 total), using the project's DB-3A/DB-3C rules (category=strength,
equipment maps, primary muscle maps (no `neck`), `force`/`mechanic` present):

| Class | Count |
|---|---|
| **SUPPORTED_STRENGTH** | **515** |
| REVIEW_NEEDED (powerlifting/strongman/olympic, `neck`, `other`/null equipment, missing force/mechanic) | 160 |
| SKIP_UNSUPPORTED (stretching/cardio/plyometrics) | 198 |

**A 100%-FED canonical catalog ≈ 515 entries** (the 452 currently imported + the ~63 FED canonical entries
that today's curated duplicates represent).

## 3. Current catalog → FED mapping

| Mapping class | Count | Recommended action |
|---|---|---|
| `ALREADY_FED_IMPORTED` | **452** | KEEP_AS_FED |
| `FED_EXACT` (curated; exact name + equipment) | **43** | REPLACE_WITH_FED |
| `FED_RENAME` (curated; same movement, FED name differs) | **49** | RENAME_TO_FED |
| `FED_CONFLICT` (curated; differs in equipment/variant — needs eyes) | **42** | REVIEW_MANUAL |
| `AURON_ONLY_NO_FED` (curated; no FED equivalent) | **21** | DELETE_FROM_CATALOG **or** keep as explicit override |

Per-entry detail: **`docs/db4a_catalog_mapping.csv`** (slug, name, source guess, mapped FED name/folder,
confidence, reason, mapping class, recommended action).

- **92 curated** (43 exact + 49 rename) map cleanly to FED → adopt FED name/metadata/images.
- **42 FED_CONFLICT** are the risky lookalikes (different equipment or a variant keyword such as
  *reverse / single-leg / preacher / kettlebell / push-press / kelso / sissy / pistol*) — these must be
  human-reviewed; do **not** auto-equate (per the task's risky examples).
- **21 AURON_ONLY_NO_FED** have no safe FED match (e.g., gym-machine variants AURON authored).

### Examples
- **FED_EXACT:** Wide-Grip Lat Pulldown, Leg Press, Seated Leg Curl, Triceps Pushdown, Cable Crunch, Incline Dumbbell Curl.
- **FED_RENAME:** Seated Cable Row→"Seated Cable Rows", Concentration Curl→"Concentration Curls", Arnold Press→"Arnold Dumbbell Press", Machine Shoulder Press→"Machine Shoulder (Military) Press", Lying Leg Curl→"Lying Leg Curls".
- **FED_CONFLICT:** Standing Calf Raise (no machine FED match — barbell/dumbbell variants), Machine Preacher Curl (FED preacher is barbell), Dumbbell Hammer Curl (nearest is Preacher Hammer), T-Bar Row (FED "Lying T-Bar Row" is chest-supported), Pistol Squat (FED is kettlebell), Single-Leg RDL (FED is bilateral).
- **AURON_ONLY_NO_FED:** Bird Dog, Dragon Flag, L-Sit, Cyclist Squat, Cossack Squat, Frog Pump, Toe Touch Crunch, Captain's Chair Leg Raise, etc.

## 4. Plan / template / draft / History migration audit

**Default routine (`SeedData.kt`)** ships 4 days (Push/Pull/Legs/Full Body) with **19 distinct exercise
names** (PLAN-1: normalized to current catalog names). **14 of them need a FED-canonical rename** under
the rebuild; the rest are "optional" accessories (`Push-up optional`, `EZ curl optional`) or already
FED/conflict. Name-migration map: **`docs/db4a_plan_name_migration.csv`** (92 rows; old AURON name → FED
canonical, with per-surface impact flags).

| Surface | Stores | Migration in DB-4B |
|---|---|---|
| Catalog seed | the entries themselves | rebuild to FED canonical |
| Default templates (`SeedData`) | hardcoded names | **rename ~14 to FED canonical** |
| Active Plan rows (`ExerciseEntity.name`) | name snapshot (no catalog FK) | **rename the high-confidence 92** so Engine V3.6B `findByName` resolves; leave others as custom |
| Drafts (`WorkoutDraftRepository`) | keyed by **exerciseId, not name** | **unaffected** by name changes ✅ |
| History (`LoggedSetEntity.exerciseNameSnapshot`, `WorkoutSessionEntity.dayNameSnapshot`) | historical text | **preserve unchanged** (it records what was actually done) |
| Import/Export | name snapshots in JSON | unaffected; remains a faithful export |

**Recommendation:** migrate catalog + default templates + *active* Plan rows (high-confidence only) to FED
canonical; **do not rewrite History**; drafts need no migration (id-keyed). Plan rows with no safe FED map
stay as custom exercises (engine name-inference fallback, same as any user-created exercise).

## 5. Engine V3.6B impact

- The engine is fed at **log time** via `LogWorkoutViewModel`: `catalogRepository.findByName(name)?.let { ProgressiveOverloadEngine.profileFromCatalog(...) }`. It is a **name lookup against the catalog**.
- **Profile fields consumed:** `progressionType`, `defaultIncrementKg`, `sessionsRequiredBeforeIncrease`, `isBodyweight`, `isUnilateral`. (`defaultSets/Min/MaxReps` set the plan targets when an exercise is added.)
- **No match → graceful name-based inference** (no crash; slightly less precise).
- **Therefore DB-4B must keep names aligned:** if the catalog becomes FED canonical but an active Plan row keeps an old AURON name, that exercise drops to name-inference. Renaming the high-confidence 92 active-plan names to FED canonical is what *prevents* fallback for normal FED exercises.
- **Caution:** FED imports use *derived* progression (`compound/isolation/bodyweight`), not AURON's hand-tuned values. Replacing the 92 curated entries with pure FED means their progression comes from derivation — fine for most, but the few hand-tuned ones should be preserved via the **override map** (progression defaults only, reviewed).

## 6. Proposed final counts

| Quantity | Value |
|---|---|
| Final FED-canonical catalog (pure FED) | **~515** |
| …keeping the 21 AURON-only as explicit overrides | ~536 |
| Curated AURON entries whose AURON metadata is removed/replaced | **155** (92 replaced by FED, 21 dropped-or-override, 42 reviewed) |
| Names to rename to FED | **92** |
| No-FED-equivalent (core) | **21** (+42 conflict pending review) |
| Default-template names to rename | **14 / 19** |

## 7. Asset cleanup estimate

- FED canonical entries reference images under **FED slugs**; the 92 renamed entries currently use
  **AURON-slug** folders → after rebuild those AURON-slug folders become **orphaned**, and images must be
  re-keyed/copied under the FED slugs (or the entries left pointing at the existing AURON-slug folders as
  an override).
- A pure-FED 515-entry catalog with 2 frames ≈ **~1030 images** (vs 1092 now) → **~62–65 MB** (roughly
  flat). Dropping the 21 AURON-only images saves ~2–3 MB. **The real size lever is still WebP (DB-3D.2).**
- Net: re-key images by FED slug; delete orphaned AURON-only folders; no big size change.

## 8. DB-4B implementation plan (safest path to FED-first)

1. **Catalog from FED JSON backbone.** Generate `assets/catalog/catalog_fed_vN.json` from the 515
   SUPPORTED_STRENGTH set; load + seed from it (per the DB-3D/3E architecture rec). This removes the
   12.9k-line Kotlin seed and the method-size chunking.
2. **AURON override map (small, explicit, reviewed):** `aliases`, optional app display name, and
   **reviewed progression defaults only** — never blind muscle/equipment overrides. Optionally retain the
   21 AURON-only as flagged overrides.
3. **Re-key images** under FED slugs for the 515; delete orphaned AURON-slug folders.
4. **Rename default templates** (`SeedData`) to FED canonical (the ~14).
5. **One-time active-Plan name normalization** (data update, **not** a Room schema change): on startup,
   rename `exercises.name` rows that match the high-confidence map (the 92) to FED canonical, guarded by a
   SharedPreferences "migration done" flag. Leave unmatched names as custom.
6. **Preserve History** (no rewrite of `*_Snapshot` columns).
7. **Update catalog tests** to the new count and structural invariants.
8. **Bump `CatalogSeed.CONTENT_VERSION`**; **do not** bump the Room `@Database` version (no schema change —
   the name normalization is a row UPDATE via the repository).
9. Keep the FED dataset in `external_catalog_sources/` (re-derivable).

## 9. Risks & rollback

- **Progression drift:** replacing 92 curated entries with FED-derived progression can subtly change
  suggestions; mitigate with the reviewed override map. Run the 56 engine tests + a catalog-consistency test.
- **Wrong active-Plan rename:** only rename the high-confidence 92 (exact/rename); never the 42 conflicts.
  A bad rename changes what the user sees in their plan.
- **Image re-keying** must complete or renamed entries show placeholder.
- **Loss of curated quality** (tips, fine taxonomy) for the 92 — partly mitigated by the override map; the
  fine-grained delt/chest taxonomy reverts to FED's coarse `shoulders`/`chest` (note DB-3E filter concern).
- **42 FED_CONFLICT + 21 AURON_ONLY** require human decisions before any deletion.
- **Rollback:** full `app/src` timestamped backup before DB-4B; the catalog fully re-seeds every
  `CONTENT_VERSION`; the active-Plan migration is flag-guarded and one-way, so the backup + a flag reset is
  the rollback. History is never touched, so it's inherently safe.
