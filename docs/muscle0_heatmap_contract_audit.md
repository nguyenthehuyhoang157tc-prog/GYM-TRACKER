# MUSCLE-0 — Muscle Heat Map Contract Audit

**Status:** Audit only. No source, schema, engine, UI, or asset changes. No implementation started.
**Scope:** Design the **DB → Muscle Heat Engine → UI** contract for a **session-only** Muscle Heat Map
shown on a review/share screen after a workout is saved.
**Model:** claude-opus-4-8. **Repo:** `D:\HOCTIN\GYM TRACKER`. **Room v6 / Catalog 577 / CONTENT_VERSION 16.**

> Note on inputs: the `docs/db_*` result files referenced in the task do not exist in the repo — those were
> chat-delivered reports. This audit is grounded in `README.md` plus direct inspection of the live code
> (`data/`, `data/catalog/`, the Log save flow, and the domain engine). Findings below cite real files.

---

## 1. DB current capability summary

**Catalog (reference data, read-only, never in History/Export, never consumed by the Progression Engine):**
- `data/catalog/catalog_fed_v1.json` → `CatalogExerciseEntity` (Room table `catalog_exercises`), loaded by
  `CatalogJsonLoader`, re-seeded by `ExerciseCatalogRepository.ensureSeeded()` on `CONTENT_VERSION` bump.
- **577 entries** (`CatalogSeed.CONTENT_VERSION = 16`; DB‑6E added 9 sled/carry entries).
- `CatalogExercise` ([data/catalog/CatalogExercise.kt](app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogExercise.kt)) carries everything the heat map needs:
  `slug`, `primaryMuscles: List<MuscleGroup>`, `secondaryMuscles: List<MuscleGroup>`, `trackingType`,
  `defaultSets`, `isBodyweight`, `isUnilateral`, `equipment`, `force`.
- `MuscleGroup` ([data/catalog/CatalogTaxonomy.kt](app/src/main/java/com/hoctin/gymtracker/data/catalog/CatalogTaxonomy.kt)) is a **fixed enum of 24 values**:
  CHEST, UPPER_CHEST, BACK, LATS, MID_BACK, LOWER_BACK, TRAPS, SHOULDERS, FRONT_DELTS, SIDE_DELTS,
  REAR_DELTS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES, CALVES, ABS, OBLIQUES, CORE,
  ADDUCTORS, ABDUCTORS, FULL_BODY. Stored by `name`; additive-only; `fromName` returns `null` on unknown.
- `TrackingType` (8 values): REPS_WEIGHT, BODYWEIGHT_REPS, TIME_ONLY, TIME_LOAD, DISTANCE_LOAD,
  TIME_DISTANCE, TIME_DISTANCE_LOAD, SKILL_FORM (with `isRepBased` / `showsLoad` / `showsDuration` /
  `showsDistance` field-config flags).

**Session / logged data ([data/Entities.kt](app/src/main/java/com/hoctin/gymtracker/data/Entities.kt), Room v6):**
- `LoggedSetEntity`: `sessionId`, `exerciseId?`, `exerciseNameSnapshot`, `setNumber`, `weight`, `reps`,
  `rir`, `durationSeconds?`, `distanceMeters?`, **`canonicalExerciseKey?`** (`catalog:<slug>` | `custom:<name>`),
  `isPrWeight`, `isPrReps`.
- `canonicalExerciseKey` is set on **every** saved set (`GymRepository.saveWorkoutSession`,
  `ExerciseLogInput.canonicalExerciseKey`) and backfilled for old rows
  ([data/catalog/CanonicalExerciseKey.kt](app/src/main/java/com/hoctin/gymtracker/data/catalog/CanonicalExerciseKey.kt), `CanonicalKeyBackfill`). This is the **stable link from a logged set to its
  catalog muscles**: strip `catalog:` → `slug` → `CatalogExercise` (via `ExerciseCatalogRepository.all()`).
- `ExerciseEntity` (plan row): `targetSets`, `targetMinReps`, `targetMaxReps` — the authoritative
  `targetSets` source.
- `WorkoutSessionEntity`: `dayId?`, `dayNameSnapshot`, `dateEpochDay`, `startedAtMillis`, `readiness`.
  **There is no session end-time, duration, or calories column.**
- `SessionWithSets` relation exists; `getRecentSetsForCanonicalKey(key, name)` exists. There is **no**
  `getSetsForSession(sessionId)` query yet (not required for V1 — see §11).

**What the DB can already answer, per saved session, with zero schema change:**
per exercise → its `canonicalExerciseKey` → catalog `primaryMuscles`/`secondaryMuscles`/`trackingType`;
`completedSets` (count of saved `logged_sets`); `targetSets` (plan `ExerciseEntity`); per-set `weight`,
`reps`, `rir`, `durationSeconds`, `distanceMeters`.

**Known gaps:** (a) no **session duration** persisted (only `startedAtMillis`); (b) **skipped** is not an
explicit flag — it is inferred (§6); (c) **custom** exercises have no catalog muscles (§9); (d) the catalog
carries anatomical muscles but **no body-map region / front-back mapping** (§6 taxonomy risks).

---

## 2. Engine-side agreed boundary

Three clean layers; the Muscle Heat feature is a **new, separate** engine, NOT part of Progressive Overload.

| Layer | Responsibility | Output |
|------|----------------|--------|
| **DB / data** | Raw **anatomical exposure** — pure counting from canonical key → catalog muscles + logged sets. No training-quality judgment. | `List<MuscleExposure>` (+ `ExerciseContributor`s) |
| **Muscle Heat Engine** (new, pure, runtime-only) | **Training-quality interpretation** — turn raw exposure into color band + status + quality modifier + notes. | `List<MuscleHeatResult>` |
| **UI** | Render the front/back body map, color bands, top muscles, session summary. | — |

- **Progressive Overload Engine stays untouched** and focused on load/reps/RIR progression, bad‑day /
  plateau / new‑baseline, readiness trend, workout‑quality context, and plateau strategy.
- The Muscle Heat Engine **may read** PO Engine outputs (`CoachInsight`, `PlateauStrategy`, `sessionQuality`,
  `confidence`) **per exercise contributor** as an optional quality input — it never lives inside PO and
  never calls back into it.
- **Purity mirror of the existing engines:** the Muscle Heat Engine is a pure function
  (`analyze(exposures, context): List<MuscleHeatResult>`); no DAO inside it (same rule as
  `ProgressiveOverloadEngine`). The ViewModel/data layer computes exposure and passes it in.

**Critical semantic distinction (drives the whole contract):**
- **Color band = exposure** (how much anatomical work the muscle received) — derived from
  `rawIntensityScore` / `finalIntensityScore`.
- **Status = interpretation** (STIMULATED / PRODUCTIVE / FATIGUED / RECOVERY_RISK / UNDERTRAINED) — derived
  from exposure **combined with quality** (RIR, completion, PO signals).
- Therefore a muscle can be **DEEP_RED (high exposure) but low quality**, or **moderate band but
  RECOVERY_RISK**. Band and status are computed on different axes and must be separate fields.

---

## 3. Proposed DB raw-exposure contract (MUSCLE-1)

Pure, deterministic, runtime-only. Computed from a saved session's sets + the catalog muscle map. No new
Room column/table. Produced by a data-layer mapper (e.g. `MuscleExposureMapper`), not by any engine.

```
data class MuscleExposure(
    val muscleGroup: MuscleGroup,
    val primarySetEquivalent: Double,     // Σ over exercises where this muscle is PRIMARY
    val secondarySetEquivalent: Double,   // Σ over exercises where this muscle is SECONDARY (weighted down)
    val completedExercises: Int,
    val partialExercises: Int,
    val skippedExercises: Int,
    val rawIntensityScore: Double,        // pure exposure; NO quality judgment (see §2)
    val exerciseContributors: List<ExerciseContributor>,
)

data class ExerciseContributor(
    val canonicalExerciseKey: String,     // catalog:<slug> | custom:<name>
    val exerciseName: String,             // exerciseNameSnapshot (display)
    val trackingType: TrackingType,
    val role: MuscleRole,                 // PRIMARY | SECONDARY (for this muscle)
    val targetSets: Int,
    val completedSets: Int,
    val completionRatio: Double,          // completedSets / max(1,targetSets), clamped 0..1 (partial)
    val avgRir: Double?,                  // null for non-rep types
    val avgReps: Double?,                 // null for non-rep types
    val avgWeight: Double?,               // null / 0 for bodyweight
    val durationSeconds: Int?,            // Σ for time types
    val distanceMeters: Double?,          // Σ for distance types
    // Optional PO Engine outputs if accessible for this exercise this session (else null):
    val coachInsight: CoachInsight? = null,
    val plateauStrategy: PlateauStrategy? = null,
    val sessionQuality: SessionQuality? = null,
    val confidence: Int? = null,
)

enum class MuscleRole { PRIMARY, SECONDARY }
```

**Raw exposure model (contract shape; exact constants tuned in MUSCLE-1/2):**
```
per contributor, setEquivalent =
    baseCompletedSets(trackingType, completedSets, durationSeconds, distanceMeters)   // §8 tracking rules
    × completionRatio                                                                 // partial scaling
roleWeight = 1.0 for PRIMARY, ~0.5 for SECONDARY
muscle.rawIntensityScore = Σ (setEquivalent × roleWeight)
```
- **DB output is exposure only.** No color, no status, no "recovery risk" here.
- `exerciseContributors` is included for **explainability** (§10) so the UI can show "why this muscle is hot".

---

## 4. Proposed Muscle Heat Engine contract (MUSCLE-2)

Pure, deterministic, runtime-only. Input = `List<MuscleExposure>` (+ a small `MuscleHeatContext`: goal
profile default HYPERTROPHY, optional readiness). Output:

```
data class MuscleHeatResult(
    val muscleGroup: MuscleGroup,
    val finalIntensityScore: Double,   // exposure-driven (normalized rawIntensityScore) → colorBand
    val colorBand: HeatColorBand,      // EXPOSURE axis
    val status: MuscleStatus,          // INTERPRETATION axis (exposure × quality)
    val reason: String,                // short human-readable "why"
    val recoveryNote: String?,         // optional (V1: light; full recovery model is MUSCLE-4)
    val qualityModifier: Double,       // 0..1-ish factor from RIR/completion/PO signals (drives status, not band)
)

enum class HeatColorBand { NONE, LIGHT_YELLOW, YELLOW_ORANGE, ORANGE_RED, DEEP_RED }
enum class MuscleStatus { NONE, STIMULATED, PRODUCTIVE, FATIGUED, RECOVERY_RISK, UNDERTRAINED }
```

**Mapping rules (contract; thresholds tuned in MUSCLE-2):**
- `colorBand` is a monotonic function of `finalIntensityScore` (exposure). NONE at ~0 → DEEP_RED at high
  exposure. **Never depends on quality** — this is the "color = exposure" rule.
- `qualityModifier` blends: avg RIR proximity to failure (reuse the effective-reps idea, hypertrophy),
  `completionRatio`, and optional PO `sessionQuality`/`CoachInsight` per contributor. Limited for
  time/distance/skill types (§8).
- `status` = exposure band × quality:
  - low exposure → `UNDERTRAINED` (trained little) or `NONE` (untouched);
  - moderate exposure + adequate quality → `STIMULATED`;
  - high exposure + good quality → `PRODUCTIVE`;
  - high exposure + poor quality/near-failure grind → `FATIGUED` → `RECOVERY_RISK` at the extreme.
- **Never** changes any DB value, never persists, never shows a raw number.

---

## 5. Proposed UI contract (MUSCLE-3)

The after-save review/share screen consumes **only** the engine output + a static body-region map:
- `List<MuscleHeatResult>` → color each front/back body region by `colorBand`; show `status`/`reason` on tap.
- **Top muscles** = `MuscleHeatResult` sorted by `finalIntensityScore` desc (take N).
- **Session summary:** duration (see gap below), top muscles, and — later — estimated calories.
- A **`MuscleGroup → BodyRegion(front|back, regionId)` mapping table** is a **UI/domain-side constant** the
  UI owns (the catalog has anatomical muscles, not drawing regions). It must cover all 24 enum values with a
  safe fallback (§6).
- **Duration gap:** no session duration is stored. V1 options: (a) compute in the UI from screen-open →
  save time (runtime, no schema); (b) sum `durationSeconds` for time-tracked work only; (c) defer the
  duration chip. Recommend (a) for V1; a persisted session duration would be a separate schema task.

UI needs nothing from the DB layer directly — it depends on the engine output + the region map, keeping the
three-layer boundary clean.

---

## 6. Muscle taxonomy / body-region mapping risks

**Q1 — are catalog muscle names stable enough for body-map regions?** **Yes.** `MuscleGroup` is a fixed
Kotlin enum stored by `name`, additive-only, with a safe `fromName`. Stable identifiers to key a region map.

**Q2 — aliases/normalization needed?** **Not for muscles** (the enum is already canonical — no free-text
muscle strings). Exercise-name normalization is already solved by `canonicalExerciseKey`. What is missing is
a **new `MuscleGroup → body region` mapping** (a UI/domain constant), not muscle normalization.

**Q3 — do primary/secondary map cleanly to UI regions?** Mostly. A standard front/back silhouette maps
cleanly for CHEST/UPPER_CHEST, LATS/MID_BACK/LOWER_BACK/TRAPS, the three deltoid heads, BICEPS/TRICEPS/
FOREARMS, QUADS/HAMSTRINGS/GLUTES/CALVES, ABS/OBLIQUES. Needs a front/back **split** and a decision on
grouped vs split regions.

**Q4 — muscles with no obvious single region (must be handled explicitly):**
- `FULL_BODY` — no single region; spread as light exposure across major regions, or render as a neutral
  "whole body" indicator (do NOT drop it).
- `CORE` — overlaps ABS/OBLIQUES/LOWER_BACK; map to the abdominal region (or a core cluster).
- `BACK` — generic parent of LATS/MID_BACK/LOWER_BACK/TRAPS; decide whether the catalog uses `BACK`
  standalone or always the specific children (audit the JSON in MUSCLE-1) and map to a back cluster.
- `SHOULDERS` — generic parent of FRONT/SIDE/REAR_DELTS; same parent/child decision.
- `ADDUCTORS` / `ABDUCTORS` — inner/outer thigh; a simple silhouette may lack these regions → map to an
  upper-leg region or show in the muscle list only.
- `FOREARMS` — small region; render or fold into arms.
**Risk:** a region map that silently omits any of the 24 values would drop exposure. MUSCLE-1 must assert
**every enum value has a mapping or an explicit "list-only / no-region" decision.**

---

## 7. Skipped / partial rules

**Q6 — skipped explicit or inferred?** **Inferred.** There is no skip flag. `buildInputs()`
([ui/log/LogWorkoutViewModel.kt](app/src/main/java/com/hoctin/gymtracker/ui/log/LogWorkoutViewModel.kt)) drops any planned exercise with zero parsed sets, so a **skipped exercise = a day-plan
exercise (`currentExercises` / `WorkoutDayWithExercises`) with no `logged_sets` in the session.** Detect by
diffing the day's planned exercises against the exercises that produced saved sets. After save this is
trivial in-memory (the ViewModel holds `currentExercises` + the just-saved `inputs`).

**Q7 — partial completion representation?** `completedSets` = count of saved working sets for the exercise;
`targetSets` = `ExerciseEntity.targetSets`. `completionRatio = completedSets / max(1, targetSets)` clamped
to 0..1. **Contribution is scaled by `completionRatio`.**

**Rules (per task):**
- **skipped** (no sets) → `skippedExercises++`, **0 contribution**.
- **partial** (`0 < completedSets < targetSets`) → `partialExercises++`, contribution × `completionRatio`.
- **completed** (`completedSets ≥ targetSets`) → `completedExercises++`, **full** contribution (ratio 1.0).
- **primary weighs more than secondary** via `roleWeight` (1.0 vs ~0.5).

---

## 8. TrackingType rules (raw exposure, V1)

**Q9 — how tracking type affects raw exposure.** DB emits exposure for every type; the Heat Engine applies
**full quality** only where reps+RIR exist.

| TrackingType | Raw exposure basis | Quality scoring (Heat Engine) |
|---|---|---|
| `REPS_WEIGHT` | completed working sets (set‑equivalent) | **Full** (RIR + reps + completion) |
| `BODYWEIGHT_REPS` | completed working sets | **Full** |
| `SKILL_FORM` | completed sets | **Conservative / form‑focused** (never "recovery risk" aggressively) |
| `TIME_ONLY` | from `durationSeconds` (normalize to set‑equivalent, e.g. per ~30–45s) | **Limited** (no RIR) |
| `TIME_LOAD` | duration (+ load context) | **Limited** |
| `DISTANCE_LOAD` | from `distanceMeters` (+ load) | **Limited** |
| `TIME_DISTANCE` | duration + distance | **Limited** |
| `TIME_DISTANCE_LOAD` | duration + distance + load | **Limited** |

- The task's V1 list names `TIME_ONLY` / `DISTANCE_LOAD` / `TIME_DISTANCE_LOAD`; the live enum also has
  `TIME_LOAD` and `TIME_DISTANCE` — all non‑rep types follow the same "raw exposure allowed, limited
  quality" rule, so the extra two are covered.
- For time/distance sets, `reps`/`rir` carry the `0` sentinel — the mapper must read `durationSeconds`/
  `distanceMeters`, **not** treat `reps=0` as a failed set.

---

## 9. Custom exercise fallback

**Q8 — can custom exercises get safe exposure?** A `custom:<name>` key has **no catalog entry → no
primary/secondary muscles**. V1 safe fallback:
- **Never crash / never mis-attribute.** If the key resolves to no catalog exercise, the exercise
  contributes to **no specific muscle region** (or a single neutral `FULL_BODY`/"other" bucket at reduced
  weight) and is **still listed in `exerciseContributors`** so the user sees it was logged.
- It counts toward `completedExercises`/duration but not toward any specific body-map region unless a future
  MUSCLE‑N adds name inference.
- The same fallback covers a `catalog:<slug>` whose slug is missing from the loaded catalog (defensive).

---

## 10. Explainability

**Q10 — should DB include `exerciseContributors`?** **Yes.** The review screen shows top muscles and should
explain "these exercises drove it." Carrying contributors (with optional PO outputs) makes the map
inspectable and makes the Heat Engine's quality scoring auditable/testable without a DB round-trip. Runtime
object only — never stored, never shown as a raw number.

---

## 11. Is a schema change needed?

**Q11 — V1 runtime-only, no schema?** **Yes, no schema change for V1.** Everything is derivable from
existing columns: `logged_sets.canonicalExerciseKey` → catalog muscles/`trackingType` (via
`ExerciseCatalogRepository.all()`), `weight`/`reps`/`rir`/`durationSeconds`/`distanceMeters`, plus
`ExerciseEntity.targetSets` and the day plan (for skipped/partial).

**Q12 — what the Heat Engine needs from DB:** `List<MuscleExposure>` (with contributors), i.e. the raw
mapping described in §3 — computed by a data-layer mapper from the just-saved session (in-memory after save,
**zero new DAO query**) or, for a freshly-opened review screen, from `SessionWithSets`/a small read-only
`getSetsForSession(sessionId)` (a read-only query, still no schema change) plus the catalog.

**Q13 — what the UI needs from the Heat Engine:** `List<MuscleHeatResult>` (§4) + a `MuscleGroup → body
region` map + top-muscles ordering + a session summary (duration, top muscles). Nothing from the DB layer
directly.

**Only true gap:** **session duration** is not persisted (§5). V1 should compute it at runtime or defer the
duration chip; a stored duration is a separate, later schema task if wanted.

---

## 12. Recommended implementation phases

- **MUSCLE-1 — DB raw session exposure (data layer, no schema).** Add a pure `MuscleExposureMapper` +
  `MuscleExposure`/`ExerciseContributor` types; build a `slug → CatalogExercise` map from
  `ExerciseCatalogRepository.all()`; compute per-muscle exposure from the saved session (in-memory after
  save) applying the skipped/partial (§7) and tracking-type (§8) rules; custom fallback (§9). Deterministic
  unit tests (pure JVM). No engine, no UI.
- **MUSCLE-2 — Muscle Heat Engine (pure runtime scoring).** New `MuscleHeatEngine.analyze(...)` →
  `List<MuscleHeatResult>`; exposure→`colorBand`, quality→`status`/`qualityModifier`; tracking-type-aware
  quality; may read optional PO outputs per contributor. Deterministic unit tests. No UI.
- **MUSCLE-3 — After-save review / body map UI.** Review/share screen after `LogWorkoutEvent.Saved`:
  front/back body map colored by `colorBand`, top muscles, session summary; `MuscleGroup → region` map with
  full-coverage assertion. Runtime duration (§5).
- **MUSCLE-4 — 7-day recovery map.** Deferred by design — needs a decay model, weekly volume, readiness
  trend, muscle overlap, user level, and goal profile. **Out of scope for V1.**

---

## 13. Exact next prompt for MUSCLE-1

> **MUSCLE-1 — DB Raw Session Exposure (data layer, no schema)**
>
> Work only in `D:\HOCTIN\GYM TRACKER`. Create a timestamped backup of `app/src` before editing. Do not
> modify Room schema, the Progressive Overload Engine, or any UI. No new assets. No new DAO query if the
> after-save data is available in-memory.
>
> Implement, per `docs/muscle0_heatmap_contract_audit.md` §3/§7/§8/§9/§11:
> - New pure data-layer types `MuscleExposure`, `ExerciseContributor`, `MuscleRole` (runtime-only; never
>   stored, never exported).
> - A pure `MuscleExposureMapper` that, given a saved session's logged sets (grouped by
>   `canonicalExerciseKey`), the day's planned exercises (for skipped/partial), and a
>   `slug → CatalogExercise` map from `ExerciseCatalogRepository.all()`, returns `List<MuscleExposure>`:
>   - skipped exercise (in plan, 0 sets) → 0 contribution, `skippedExercises++`;
>   - partial (`0 < completed < target`) → contribution × `completionRatio`;
>   - completed (`≥ target`) → full contribution;
>   - PRIMARY roleWeight 1.0, SECONDARY ~0.5;
>   - tracking-type exposure basis per §8 (reps/bodyweight = set-equivalent; time/distance from
>     `durationSeconds`/`distanceMeters`, never treat `reps=0` as a failed set; skill conservative);
>   - custom (`custom:` key or missing slug) → safe fallback (no specific muscle; still a contributor).
> - Keep it engine-free and DAO-free (a pure function; the ViewModel/data layer passes data in).
> - Do NOT compute color/status/quality (that is MUSCLE-2). DB emits **raw exposure only**.
>
> Add deterministic pure-JVM unit tests: primary vs secondary weighting, skipped=0, partial scaling,
> completed=full, a time-only and a distance-load exposure, custom fallback, and full `MuscleGroup`
> coverage sanity. Run `.\gradlew.bat :app:testDebugUnitTest` and `.\gradlew.bat :app:assembleDebug`.
> Update the README MUSCLE section. Deliver: backup path, files changed, exposure formula, test counts,
> build result, confirmation of no schema/engine/UI change, next step (MUSCLE-2).

---

### Summary

The DB already owns everything the heat map needs (canonical identity → catalog muscles, tracking type,
target/completed sets, per-set metrics) and can produce **raw anatomical exposure with no schema change**.
The clean split is **DB = exposure (counting) / Muscle Heat Engine = interpretation (band + status +
quality) / UI = body map** — with the Progressive Overload Engine untouched and only optionally *read* for
per-exercise quality. Color = exposure, status = interpretation (they are separate axes). The only real gap
is a persisted session duration; everything else is runtime-derivable. Proceed phase-by-phase: MUSCLE‑1
(raw exposure), MUSCLE‑2 (heat engine), MUSCLE‑3 (review UI), MUSCLE‑4 (7‑day recovery, deferred).
