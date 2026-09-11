# MUSCLE-4 — Home Muscle Recovery Map — Result

**Status:** COMPLETE — deterministic recovery engine + Home card, device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE4_BACKUP_20260704_215516\app_src`.
**No Room schema change (runtime-only), no Progressive Overload Engine change, no MuscleHeatEngine
scoring change, no catalog change, no imports, signing untouched.** No hard stop hit.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE4_BACKUP_20260704_215516\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `domain/muscleheat/MuscleRecovery.kt` | **NEW** — `RecoveryStatus`, `RecoveryColorToken` + `RecoveryPalette` (separate from heat tokens), `MuscleRecoveryResult`/`RecoveryContributor`, pure `MuscleRecoveryEngine` (size windows + decay + stacking), pure `MuscleRecoveryPipeline` (sessions+catalog → results + plan overlap), `recoveryRegionPaints`, thin `MuscleRecoveryUseCase`. |
| `ui/muscleheat/BodyMap.kt` | Renderer generalized: `BodyMapFigureCustom(side, slugArgb)` core (heat path unchanged via wrapper); `BodyMapSlugs.recoverySlugArgb` (max-fatigue slug merge). |
| `ui/home/MuscleRecoverySection.kt` | **NEW** — the Home card (front/back recovery maps, status-dot highlight lines, ready line, plan-overlap warning, empty state). Presentational only. |
| `ui/home/HomeViewModel.kt` | +`catalogRepository` param; `muscleRecovery: StateFlow<MuscleRecoveryUiState>` combining session history + plan + prefs → use case → UI state (all math outside the UI). |
| `ui/home/HomeScreen.kt` | Card inserted between "Your progress" stats and Daily Boost. |
| `ui/GymTrackerApp.kt` | Passes `catalogRepository` into `HomeViewModel`. |
| `test/domain/muscleheat/MuscleRecoveryEngineTest.kt` | **NEW** — 13 pure-JVM pipeline tests. |
| `README.md`, this doc | Docs. |

## 3. Recovery model summary (V1, deterministic & explainable)
- **Window:** the last **7 days** of saved sessions (from the already-observed session history — no new
  DAO query; sessions outside the window or with future timestamps are ignored; old rows without
  duration are unaffected — only `startedAtMillis` is used).
- **Fatigue seed per session/muscle** = the session pipeline's **quality-weighted stimulus**
  (`MuscleHeatResult.finalIntensityScore` = raw exposure × RIR quality), reusing MUSCLE-1/HEAT-1
  unchanged. A session's `RECOVERY_RISK` status marks that seed as a grind.
- **Decay:** `residual = stimulus × max(0, 1 − elapsed/window)` where the window is **size-based**
  (LARGE 72 h: quads/hams/glutes/back-group/chest/full-body · MEDIUM 48 h: delts/traps/core-group/
  adductors/abductors · SMALL 36 h: biceps/triceps/forearms/calves) and stretched **×1.35** for
  grinding seeds. **Residuals from repeated sessions SUM** (accumulated fatigue).
- **Outputs per muscle:** `fatigueScore` (Σ residuals), `recoveryScore` (0–100), status
  (READY / PRIMED / WORKED / SORE_OR_FATIGUED / RECOVERY_RISK / UNKNOWN), recovery color token,
  `lastTrainedAt`, per-session contributors, and a plain-language reason ("Trained 20h ago — heavy
  fatigue, give it more time.").
- **Recovery colors are a separate token system**: red/orange = fatigue still carried; READY/UNKNOWN
  paint **nothing** (neutral figure = recovered, per the design doc's "skin = ready"); the READY green
  exists only on text dots. Session-heat semantics are never reused.

## 4. Why the model is scientifically reasonable
Verified the provided sports-science review against the code first: the heat bands, the RIR quality
table (RIR ≥4 junk-like ×0.40; RIR 1–3 productive ×0.95; RIR 0 stimulating-but-costly ×0.70 +
RECOVERY_RISK), and primary/secondary anatomical weighting all exist as described — so recovery
**reuses** that stimulus as its fatigue seed instead of re-inventing it. On top, it adds exactly the
two factors the review found missing for recovery: **muscle-size-dependent recovery windows** (large
muscles/compounds recover slower — RP/NSCA guidance and the design doc's 24–36 h vs 48–72 h bands) and
**grind-aware decay** (stimulus-to-fatigue ratio: failure work leaves disproportionate fatigue, so
risk-flagged seeds decay ×1.35 slower). Session Heat (one workout's stimulus) and Recovery (debt over
time) remain strictly separate concepts. The review's `exerciseDamageFactor` (stretch-mediated damage
metadata) was **deferred** — it needs per-exercise catalog metadata that doesn't exist; documented as
future work rather than guessing values.

## 5. How RIR / fatigue / time interact
Same 3 sets: RIR 5 seeds 3×0.40 = 1.2 debt; RIR 2 seeds 3×0.95 = 2.85; a 5-set grind seeds 5×0.70 = 3.5
**and** decays 35% slower. At 40 h, biceps (36 h window) are READY while chest (72 h) still carries
~44% of its seed. Two hits inside the window stack. Everything decays linearly to zero → old sessions
read READY with "recovered" wording.

## 6. Home UI behavior
A premium AURON card ("Muscle Recovery") between the stats row and Daily Boost: front/back body maps
painted with recovery colors, up to 3 recovery-limited muscle lines with colored status dots + reasons
("Mid back · Trained 20h ago — heavy fatigue, give it more time."), a green ready line ("4 muscle
groups are fully recovered." / "All trained muscles recovered — great day to push."), and an optional
**"Today's plan hits X — still recovering."** warning (selected plan day's exercise names → catalog
primary muscles ∩ SORE/RISK muscles; unmatched names safely skipped). Empty state: "No workouts in the
last 7 days — every muscle is fresh and ready." Weekly Progress / History untouched.

## 7. Tests / build result
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0** (13 new `MuscleRecoveryEngineTest` cases green
on the first run; all prior suites unchanged): decay over time, stacking, large-vs-small windows,
grind decays slower than productive, easy RIR-5 seeds less, old sessions → READY (score 100), time/skill
conservative, custom/unknown no-crash + no attribution, no-workouts empty snapshot, 7-day window cutoff,
recovery-vs-heat semantics (READY paints nothing; RISK paints recovery red), fresh-session status +
reason wording, plan-overlap flag + unmatched-name safety. `:app:assembleDebug` → **exit 0** (93.16 MB).

## 8. Runtime QA (Pixel_8) — PASSED (actually run)
- Home shows the recovery card with real data: **mid-back/lats red** ("Trained 20h ago — heavy fatigue"),
  **biceps/shoulders orange** ("still recovering"), **chest/abs lime (PRIMED)** from older sessions —
  visibly lower fatigue for older training ✓; "4 muscle groups are fully recovered." ✓.
- Recovery map colors clearly differ in meaning from the session-heat map (verified side by side:
  History→review still shows heat semantics; Home shows debt semantics) ✓.
- History month archive → tap → Muscle Heat Review (incl. a 3C-renamed session) still fully working ✓.
- **App crash buffer clean** throughout ✓.
- Empty state (no workouts in 7 days): implemented + unit-tested (`hasData=false` path); **not
  observable on this emulator dataset** (recent sessions exist) — stated honestly.
- Plan-overlap warning: logic unit-tested; not visible in QA because the selected plan day didn't hit a
  still-fatigued muscle — stated honestly.

## 9. Known limitations
- Linear decay (no supercompensation curve/eccentric-damage modelling); the review's
  `exerciseDamageFactor` deferred (needs catalog metadata).
- `now` is sampled when the flow (re)computes — the card refreshes on Home revisits/data changes, not
  by a ticking clock while the screen stays open.
- Recovery considers the 7-day window only (no long-term readiness/periodization).
- Muscles never trained in the window simply don't appear (implicit UNKNOWN); the "losing gains /
  neglected muscle" nudge from the design doc is future work.
- Plan overlap uses primary muscles of the *selected* day only.

## 10. Next recommendation
**MUSCLE-4B:** neglected-muscle nudges ("Legs untrained for 9 days") + tap-through from the Home card to
a small recovery detail (per-muscle contributors), and the catalog `exerciseDamageFactor` metadata pass.
Alternatively jump to the PO-metadata quality hook (MUSCLE-HEAT-2) so recovery/heat also read
CoachInsight/readiness context.
