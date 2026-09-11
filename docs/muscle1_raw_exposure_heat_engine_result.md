# MUSCLE-1 — Raw Muscle Exposure + Baseline Muscle Heat Engine — Result

**Status:** COMPLETE. Runtime foundation only — **no body-map UI, no assets, no Room schema change, no
Progressive Overload Engine change, no catalog/JSON change, no exercise imports.**
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE1_BACKUP_20260702_133247\app_src`.
**Contract source:** `docs/muscle0_heatmap_contract_audit.md` (MUSCLE-0).

---

## 1. Files changed

| File | Change |
|---|---|
| `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleExposure.kt` | **NEW** — Layer A: `MuscleRole`, `ExerciseSessionSnapshot`, `ExerciseContributor`, `MuscleExposure`, `SessionMuscleExposure`, pure `MuscleExposureCalculator`. |
| `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/MuscleHeatEngine.kt` | **NEW** — Layer B: `HeatColorBand`, `MuscleStatus`, `MuscleHeatResult`, pure `MuscleHeatEngine` V1. |
| `app/src/main/java/com/hoctin/gymtracker/domain/muscleheat/SessionMuscleHeatUseCase.kt` | **NEW** — integration: compute `SessionMuscleHeat` from a **saved session id** (orchestration only). |
| `app/src/main/java/com/hoctin/gymtracker/data/GymDao.kt` | +1 **read-only** `@Transaction @Query getSessionWithSets(sessionId)` (no schema change; same precedent as the V3.6B/V3.7B read-only queries). |
| `app/src/main/java/com/hoctin/gymtracker/data/GymRepository.kt` | +`sessionWithSets(sessionId)` passthrough. |
| `app/src/test/java/com/hoctin/gymtracker/domain/muscleheat/MuscleHeatEngineTest.kt` | **NEW** — 20 deterministic pure-JVM tests. |
| `README.md`, `docs/muscle1_raw_exposure_heat_engine_result.md` | Docs. |

Untouched: Room schema (v6, no migration), `ProgressiveOverload.kt`, save flow, all UI, catalog JSON/assets.

## 2. Architecture summary

```
DB (facts)                    Layer A (counting)                Layer B (meaning)            MUSCLE-3 UI (later)
logged_sets + plan day  ──►   MuscleExposureCalculator   ──►    MuscleHeatEngine      ──►    body map / top muscles
+ catalog muscles             SessionMuscleExposure             List<MuscleHeatResult>
        └────────── SessionMuscleHeatUseCase.forSession(sessionId) orchestrates (runtime-only) ──────────┘
```

- **Layer A is pure counting** (anatomical exposure; zero quality judgment). **Layer B is pure
  interpretation.** Both are `object`s with no DAO/I/O/clock — the use case joins DB facts and passes them in.
- **Separate from the Progressive Overload Engine** by design; PO keeps progression/plateau/readiness.
- Everything is **runtime-only**: nothing stored, nothing exported, no raw score shown.

## 3. Raw exposure contract (Layer A)

`SessionMuscleHeatUseCase` (or MUSCLE-3 directly after save) builds one `ExerciseSessionSnapshot` per
planned/logged exercise (canonical key, name, catalog primary/secondary muscles, trackingType, targetSets,
this session's sets). `MuscleExposureCalculator.compute(snapshots)` returns `SessionMuscleExposure`:

- `muscles: List<MuscleExposure>` (hottest-first) — per muscle: `primarySetEquivalent`,
  `secondarySetEquivalent`, `completedExercises` / `partialExercises` / `skippedExercises`,
  `rawIntensityScore` (= primary + secondary weighted set-equivalents, **exposure only**),
  `exerciseContributors`.
- `exerciseContributors` (explainability): `canonicalExerciseKey`, `exerciseName`, `trackingType`,
  `role` (PRIMARY/SECONDARY), `targetSets`, `completedSets`, `completionRatio`, `setEquivalent`,
  `avgRir?`, `avgReps?`, `avgWeight?`, `durationSeconds?` (Σ), `distanceMeters?` (Σ).
- `unattributedContributors` — the custom-exercise safe bucket (§7).

**Exposure formula:** one valid logged working set = one set-equivalent (every tracking type; duration/
distance ride along for explainability, not inflated into volume). Contribution to a muscle =
`completedSets × roleWeight` with `PRIMARY = 1.0`, `SECONDARY = 0.5`.
- **skipped** (planned, zero valid sets) → 0 contribution, counted in `skippedExercises`, still listed as a
  contributor ("planned but skipped" explainability);
- **partial** → contributes exactly the sets done (`completionRatio = completed/target`, clamped 0..1);
- **completed** (≥ target, or work logged with no known target) → full contribution, ratio 1.0.

## 4. Heat engine contract (Layer B)

`MuscleHeatEngine.analyze(exposure)` → `List<MuscleHeatResult>`:
`muscleGroup`, `finalIntensityScore` (V1: raw score passed through), `colorBand`, `status`, `reason`
(short "why": drivers + effort phrase), `recoveryNote?` (only FATIGUED/RECOVERY_RISK), `qualityModifier`
(0..1; 0.5 = neutral/unknown).

**Two axes, never collapsed:** `colorBand` = **exposure only** (monotonic in `finalIntensityScore`, never
reads quality); `status` = **meaning** (exposure × quality). Verified by test: identical 5-set exposure at
RIR 2 / RIR 4 / RIR 0 → same ORANGE_RED band, but PRODUCTIVE / STIMULATED / RECOVERY_RISK.

## 5. Scoring rules (V1 — simple, deterministic, conservative)

**Color bands** (weighted set-equivalents): `0 → NONE`, `<2 → LIGHT_YELLOW`, `2–<4 → YELLOW_ORANGE`,
`4–<7 → ORANGE_RED`, `≥7 → DEEP_RED`.

**Quality per contributor** (contribution-weighted mean per muscle): full-scoring types map avg RIR →
`≥4 → 0.40` (a lot in reserve), `3–4 → 0.60`, `0.5–3 → 0.95` (productive window), `<0.5 → 0.70` (all-out
grind: stimulating but costly). SKILL_FORM fixed `0.60`; time/distance fixed `0.50`; no signal → `0.50`.

**Status:** `score 0 → NONE`; `<2 → UNDERTRAINED`; `grindShare ≥ 0.5 of full-scoring volume AND score ≥ 4 →
RECOVERY_RISK`; `score ≥ 7 → FATIGUED`; else `quality ≥ 0.65 → PRODUCTIVE` else `STIMULATED`.
`grindShare` counts only REPS_WEIGHT/BODYWEIGHT_REPS volume with avg RIR ≤ 0.5 — so "moderate exposure but
high recovery risk" is representable, and non-rep sentinels can never fake failure. No decay, no 7-day
model, no adaptive tuning (MUSCLE-4 territory).

## 6. TrackingType behavior

| Type | Exposure | Quality |
|---|---|---|
| REPS_WEIGHT, BODYWEIGHT_REPS | valid set = `reps > 0` | **Full** (avg-RIR table above) |
| SKILL_FORM | valid set = `reps > 0` | **Conservative 0.60**, form-focused, never PRODUCTIVE alone |
| TIME_ONLY, TIME_LOAD, TIME_DISTANCE | valid set = `duration > 0` (or distance) | **Limited 0.50** |
| DISTANCE_LOAD, TIME_DISTANCE_LOAD | valid set = `distance > 0` (or duration) | **Limited 0.50** |

The `reps = 0` / `rir = 0` sentinels on time/distance sets are **never** surfaced (`avgRir`/`avgReps` null)
and **never** read as failed hypertrophy reps (tested).

## 7. Custom exercise behavior

`custom:<name>` keys (and defensively, a `catalog:` key whose slug is missing) have no catalog anatomy →
the exercise goes to `unattributedContributors`: fully visible with all its metrics, attributed to **no**
body region, engine returns no result for it, nothing crashes (tested).

## 8. Tests / builds

- **New:** `MuscleHeatEngineTest` — **20/20 green**, covering every required case: primary>secondary,
  skipped=0 (+visible), partial scaling, full contribution, multi-exercise stacking, custom fallback,
  REPS_WEIGHT/BODYWEIGHT_REPS full scoring, SKILL_FORM conservative, TIME_ONLY / DISTANCE_LOAD /
  TIME_DISTANCE_LOAD limited scoring, deterministic band thresholds, **status ≠ band**, reps=0 sentinel,
  canonical-key stability across computations, RECOVERY_RISK / FATIGUED / UNDERTRAINED, and a realistic
  session (Bench → chest/triceps/delts, Plank → abs/core via TIME_ONLY, Farmer's Walk → forearms/traps/core
  via time+distance+load, CORE stacking 1.5 + 1.0 = 2.5).
- **Full suite:** `:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0** (PO engine 99, catalog,
  readiness, multi-tracking suites unchanged).
- `:app:assembleDebug` → **exit 0**, `app-debug.apk` 93.73 MB.
- `:app:assembleRelease` → **exit 0**, **signed** `app-release.apk` 86.15 MB (keystore present; untouched).

## 9. Known limitations (V1, by design)

- **Session-only.** No 7-day recovery/decay, weekly volume, muscle overlap, readiness or goal weighting
  (MUSCLE-4). `finalIntensityScore` = raw score (no normalization yet).
- Set-equivalents treat every valid set equally (a 30 s plank set = one set-equivalent; long holds aren't
  volume-inflated — conservative choice documented in the contract).
- Band/status thresholds are fixed, hypertrophy-oriented constants; per-goal tuning comes with the goal
  branches. Quality trusts logged RIR (self-reported).
- Skipped detection follows the plan day's **current** exercises (plan edits after saving shift it);
  exercises logged outside the plan get `targetSets = 0` → counted as completed, never mislabelled partial.
- `MuscleGroup → body-region` mapping (incl. FULL_BODY/CORE/BACK/SHOULDERS parents) is deliberately left to
  MUSCLE-3 — it's a UI constant per the MUSCLE-0 contract.
- No UI calls `SessionMuscleHeatUseCase` yet — it is the ready integration point for
  `LogWorkoutEvent.Saved(sessionId)`.

## 10. Next task recommendation

**MUSCLE-3 — after-save review / body-map UI** (MUSCLE-2's baseline scoring shipped here as Layer B V1):
wire `SessionMuscleHeatUseCase.forSession(sessionId)` into the post-save flow, add the
`MuscleGroup → BodyRegion(front|back)` map with full 24-value coverage (explicit decisions for FULL_BODY /
CORE / generic BACK & SHOULDERS / ADDUCTORS / ABDUCTORS / FOREARMS), render bands + top muscles + duration
(runtime-computed). If deeper scoring is wanted first, run a small **MUSCLE-2 calibration pass** on the V1
thresholds/quality table instead — the contracts here won't need to change either way.
