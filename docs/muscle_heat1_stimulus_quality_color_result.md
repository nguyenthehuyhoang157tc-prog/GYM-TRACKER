# MUSCLE-HEAT-1 — Stimulus Quality Color Calibration + MID_BACK Trap Overlay — Result

**Status:** COMPLETE. **No Room schema change, no Progressive Overload Engine change, no exercise
imports, no share/edit/female/per-region UI work, signing untouched.**
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLEHEAT1_BACKUP_20260704_014908\app_src`.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLEHEAT1_BACKUP_20260704_014908\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `domain/muscleheat/MuscleHeatEngine.kt` | `finalIntensityScore = rawIntensityScore × qualityModifier`; `colorBand` now reads the FINAL (stimulus) score; status stays on the raw/meaning axis; header + result docs updated; PO-metadata injection left as a TODO contract. |
| `domain/muscleheat/MuscleBodyRegion.kt` | `MuscleGroup.MID_BACK → [BodyRegionId.MID_BACK, BodyRegionId.TRAPS]` (rendering overlay only). |
| `test/domain/muscleheat/MuscleHeatEngineTest.kt` | Rewrote the old "same band regardless of quality" contract test to the new calibrated contract; updated realistic-session expectations to raw×quality; +3 new calibration tests (23 total). |
| `test/domain/muscleheat/MuscleBodyRegionMappingTest.kt` | +1 MID_BACK overlay test (mapping + UI model, same single result painted twice; 17 total). |
| `README.md`, this doc | Docs. |

## 3. Scoring issue found (audit result)
Confirmed in code: `analyzeMuscle` passed `rawIntensityScore` straight through as `finalIntensityScore`,
and `colorBand()` read that raw score — the RIR-driven `qualityModifier` affected **only** the `status`
text. So the map color represented raw anatomical volume: 3 easy sets at RIR 5 heated exactly like 3 hard
sets at RIR 1–2, and enough junk volume could reach deep red.

## 4. Exact finalIntensity / colorBand rule — before → after
- **Before:** `finalIntensityScore = rawIntensityScore`; `colorBand = f(rawIntensityScore)`.
  Quality (RIR) → status only.
- **After:** `finalIntensityScore = rawIntensityScore × qualityModifier`;
  `colorBand = f(finalIntensityScore)` (same thresholds 0/2/4/7 — now over the stimulus score).
  Quality table unchanged (REPS_WEIGHT/BODYWEIGHT_REPS by avg RIR: ≥4→0.40, 3–4→0.60, 0.5–3→0.95,
  <0.5→0.70; SKILL_FORM 0.60; time/distance 0.50). Concretely, identical raw work now renders:
  RIR 5 → ×0.40 (e.g. 8 easy sets = 3.2 → YELLOW_ORANGE, never deep red), RIR 1–2 → ×0.95
  (8 hard sets = 7.6 → DEEP_RED), all-out grinding → ×0.70 with `RECOVERY_RISK` status.
- **Kept separate:** `MuscleExposure.rawIntensityScore` (Layer A anatomical work) is untouched;
  `status`/`reason` still compute from the RAW exposure + quality + grind share, so junk volume can read
  FATIGUED while only mildly hot and grinding reads RECOVERY_RISK without a "productive-hot" color.
- The engine never calls the Progressive Overload Engine; optional PO metadata (CoachInsight,
  PlateauStrategy, SessionQuality, Readiness, WorkoutContext) is documented as a pass-in-only TODO
  contract next to `analyzeMuscle` (the MUSCLE-1 contributor already reserves the fields).

## 5. MID_BACK mapping change
`MuscleGroup.MID_BACK` now maps to **both** `BodyRegionId.MID_BACK` (renders the upstream `upper-back`
shape) **and** `BodyRegionId.TRAPS` (upstream `trapezius`), because the upstream sheet splits the traps
from the upper-back mass while the catalog's MID_BACK means the whole area. Rendering-only: Layer A
exposure math and contributors are untouched — the SAME single `MuscleHeatResult` paints both regions
(verified by test: identical band/intensity, `contributingMuscles = [MID_BACK]` in each, no duplication).
When the TRAPS muscle itself also has heat, the existing deterministic max-token merge still wins.

## 6. Tests / build result
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0**. Muscle-suite totals: engine 23 (+3 new
calibration + 1 rewritten contract), mapping 17 (+1 MID_BACK overlay), arc-parser 5, body-map contract 10;
all other suites (PO engine 99, catalog, readiness, session-duration, multi-tracking) unchanged and green.
New coverage: identical raw exposure at RIR 5 vs RIR 2 → lower final + cooler band; 8×RIR-5 easy sets ≠
DEEP_RED; hard RIR 2/1/0 heats stronger than easy; raw unchanged while final differs; status/reason
separate from band; MID_BACK dual-paint. `:app:assembleDebug` → **exit 0**.

## 7. Known limitations
- Band thresholds (0/2/4/7) were kept while the score became quality-weighted — time/distance and
  SKILL_FORM work now render one band cooler than before (3 plank sets = 1.5 → LIGHT_YELLOW). Consistent
  with "limited quality" semantics, but a future calibration pass may tune per-type factors.
- Quality still derives from self-reported RIR (avg per exercise); no per-set weighting.
- Status thresholds (UNDERTRAINED <2, FATIGUED ≥7, RECOVERY_RISK ≥4+grind) remain raw-exposure-based by
  design — a deliberate axis split, not an oversight.
- MID_BACK↔TRAPS overlay is a rendering decision; per-region tap details (future) should attribute by
  muscle, not by painted region.

## 8. Future PO metadata hook recommendation
Wire it as **caller-supplied data only** (ViewModel → `SessionMuscleHeatUseCase` → contributor fields):
populate the existing optional `ExerciseContributor.coachInsight / plateauStrategy / sessionQuality /
confidence` from the per-exercise PO results the Log/Review screens already compute, plus pass
`Readiness`/`WorkoutContext` on a small `MuscleHeatContext`. `MuscleHeatEngine` may then refine
`qualityModifier` (e.g. cap quality when PO says RECOVERY_RISK-adjacent signals like repeated-tired) and
`reason` wording — never calling the PO engine, never changing Layer A exposure, and keeping every input a
pure value type so the JVM test harness stays intact.

## Next task recommendation
MUSCLE-3C (share-as-image + session rename) or the PO-metadata quality hook above; then MUSCLE-4
(7-day recovery map).
