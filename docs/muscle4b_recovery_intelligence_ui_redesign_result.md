# MUSCLE-4B — Training Readiness Upgrade + Home Recovery UI Redesign — Result

**Status:** COMPLETE — bi-exponential readiness engine + redesigned Home dashboard, device-verified.
**Backup (before any edit):** `D:\HOCTIN\GYM_TRACKER_MUSCLE4B_BACKUP_20260705_225559\app_src`.
**No Room schema change (runtime-only), no Progressive Overload Engine change, no MuscleHeat scoring
change, no catalog/asset change, no imports, signing untouched.** No hard stop hit.

---

## 1. Backup path
`D:\HOCTIN\GYM_TRACKER_MUSCLE4B_BACKUP_20260705_225559\app_src` (robocopy exit 1 = files copied OK).

## 2. Files changed
| File | Change |
|---|---|
| `domain/muscleheat/MuscleRecovery.kt` | **Model upgraded** to a bi-exponential (fitness-fatigue) readiness cost; new `RecoveryStatus` stages (READY/PRIMED/RECOVERING/SORE/OVERLOADED); `RecoveryProfile` per size; `exerciseRecoveryCost` from taxonomy; delayed-soreness flag; coarse `RecoveryBodyGroup` roll-ups; `NeglectedMuscle`; overall readiness. Pipeline gains structural-cost attribution + neglect scan; use case signature extended. |
| `ui/home/MuscleRecoverySection.kt` | **UI redesigned** into a readiness dashboard: hero score + stage, near-OLED body-map stage, coarse group tiles with colored bars + %, intelligence nudges, recovery legend. |
| `ui/home/HomeViewModel.kt` | New UI-state (`RecoveryGroupTile`, `RecoveryNudge`, overall readiness); builds tiles/nudges/neglect/overlap; passes selected-day + all-plan exercise names. |
| `test/domain/muscleheat/MuscleRecoveryEngineTest.kt` | Rewritten to the new model + new coverage (19 tests). |
| `README.md`, this doc | Docs. |
No changes to `HomeScreen.kt` (the card slot was added in MUSCLE-4) or `GymTrackerApp.kt`.

## 3. Exact model implemented
Per session × muscle, the quality-weighted **stimulus S** (raw exposure × RIR quality — unchanged
MUSCLE-1/HEAT-1 seed) becomes a **bi-exponential readiness cost** in the Banister/Busso impulse-response
tradition (two exponentials, different time constants):

```
cost(t) = A·e^(−ln2·t / fastHalfLife)  +  B·e^(−ln2·max(0, t − delay) / slowHalfLife)
A = S·(1 − structuralFraction)                         // acute neuromuscular/metabolic strain (fast)
B = S·structuralFraction·exerciseRecoveryCost          // structural muscle damage (slow, delayed)
```
- **Size-based [RecoveryProfile]** (fast/slow half-life, delay, structuralFraction): LARGE
  (chest/back/legs/full-body) `fast 12h · slow 42h · delay 11h · struct .55`; MEDIUM (delts/traps/
  core/adductors) `10/30/7/.50`; SMALL (biceps/triceps/forearms/calves) `8/20/4/.45`.
- **`delay`** holds the slow (structural) term at its plateau through the DOMS-like window, then it
  decays — a plateau-then-decay soreness curve instead of V4's straight line. When the structural
  residual exceeds the acute residual and total cost is meaningful, the muscle is flagged
  `soreWindow` ("Delayed soreness window").
- **`exerciseRecoveryCost` (≈0.7–1.4)** scales only the structural term, derived from EXISTING catalog
  taxonomy — `progressionType` (compound 1.15 → skill 0.8) + `force` (squat/hinge +0.20, carry +0.10,
  rotation/isometric −0.10) + `equipment` (barbell +0.10, machine/cable −0.10), clamped. Barbell RDL →
  1.4; machine leg curl → 0.7 (the review's RDL≫leg-curl damage example), custom/unknown → 1.0.
- **Grind / recovery-risk** stretches the structural half-life ×1.3 (fatigue outlasts stimulus).
- **Repeated sessions stack** (costs sum). **7-day window** for fatigue; a **60-day** scan feeds the
  neglect check only.
- Per muscle: `recoveryScore` = `round(100·(1 − min(1, totalCost/6.5)))`; **monotone status ladder**
  READY ≥88 · PRIMED ≥72 · RECOVERING ≥50 · SORE ≥22 · OVERLOADED <22 (status, color and % bar always
  agree). Coarse groups roll up to their **most-fatigued trained member**; overall = mean of the 6
  groups.

## 4. Kept vs changed vs MUSCLE-4
- **Kept:** the Session-Heat ↔ Recovery separation; the quality-weighted stimulus seed; the separate
  recovery color system (red = fatigue, neutral = recovered); the region-paint max-fatigue merge;
  runtime-only, pure, deterministic, no schema; PO engine & heat scoring untouched.
- **Changed:** linear-window decay → bi-exponential fast+slow(+delay); statuses
  WORKED/SORE_OR_FATIGUED/RECOVERY_RISK → READY/PRIMED/RECOVERING/SORE/OVERLOADED; added
  exercise-cost, delayed-soreness, coarse group roll-ups, overall readiness, and neglect nudges.

## 5. Why the model is scientifically reasonable
The two-time-constant fast/slow structure is the peer-reviewed standard for how training strain
dissipates (Banister et al. fitness-fatigue impulse-response; Busso variants) — verified against the
search literature before implementing. Modelling the *cost* side as itself bi-exponential (fast
acute + slower structural/damage) matches the physiology the design note and RP/NSCA guidance describe:
metabolic/neuromuscular fatigue clears fast while structural damage (DOMS) is delayed and lingers,
larger muscles/compounds recover slower, and grinding to failure leaves disproportionate fatigue. The
`exerciseRecoveryCost` implements the review's `exerciseDamageFactor` from existing taxonomy rather than
invented per-exercise numbers. Sources: Banister fitness-fatigue model — [ResearchGate](https://www.researchgate.net/publication/15242395_Fatigue_and_fitness_modelled_from_the_effects_of_training_on_performance),
[FFM review, PMC](https://pmc.ncbi.nlm.nih.gov/articles/PMC8894528/).

## 6. How RIR / fatigue / time decay interact
RIR sets the seed S upstream (RIR 5 ≈ ×0.40, RIR 1–3 ≈ ×0.95, failure ≈ ×0.70 + recovery-risk). S
splits into fast acute (A) and slow structural (B); B is scaled by exercise damage cost and (for grind)
decays ~30% slower. Right after a hard session acute dominates (OVERLOADED); a day later acute has
cleared and the structural plateau dominates (SORE / delayed-soreness window); then it decays through
RECOVERING → PRIMED → READY on a size-dependent timeline. Easy RIR-5 and time/skill work seed little
cost; old sessions read READY (≥88%).

## 7. Home UI changed
The MUSCLE-4 text-list card became a **readiness dashboard** (references adapted to phone): header +
lime "READINESS" pill; a hero score ("75 % Primed / Overall readiness") colored by stage; the front/
back body map on a **near-OLED inset stage** painted with recovery colors (red/orange = fatigue,
neutral = recovered); a 2-column grid of coarse **group tiles** (Legs/Back/Chest/Shoulders/Arms/Core)
each with name, % (status-colored), a thin readiness bar, and stage label; **intelligence nudges**
(limited-muscle reasons, plan-overlap warning, neglected-muscle lines); a "ready to train" line; and a
compact recovery **legend**. Near-black surfaces + thin edges (no gray slabs), consistent with the
History/Home OLED polish. Empty state preserved.

## 8. Test results
`:app:testDebugUnitTest` → **BUILD SUCCESSFUL, exit 0**. `MuscleRecoveryEngineTest` = **19 tests, 0
failures** (verified in the JUnit XML): decay over time; stacking; large-vs-small; grind slower +
`grindCarried`; easy RIR-5 lower; old→READY (≥88, never exactly 0 — bi-exp); delayed-soreness window +
structural>acute; very-fresh acute present; `exerciseRecoveryCost` compound-hinge-barbell > isolation-
machine (=1.4 vs clamp, null=1.0); higher cost → more structural residual later; time/skill
conservative; custom/unknown no-crash; no-workouts empty (6 groups, all READY, overall 100); 7-day
cutoff; groups always 6 + untrained READY; neglect with daysSince; never-trained neglect null-days +
only-when-training-exists; plan overlap + unmatched-name safety; recovery-vs-heat semantics (READY
paints nothing, monotone token ladder). All other suites unchanged and green.

## 9. Build results
`:app:assembleDebug` → **exit 0** (APK 93.31 MB). No warnings introduced beyond the existing baseline.

## 10. Runtime QA (Pixel_8) — actually run
- Home dashboard renders: hero "**75 % Primed / Overall readiness**" (lime); body-map stage with back
  **lats/mid-back red**, front **biceps lime**, recovered muscles neutral; group tiles **Legs 99%
  Ready, Back 0% Needs rest, Chest 91% Ready, Shoulders 88% Ready, Arms 83% Primed, Core 93% Ready**
  with colored bars; nudges "**Mid back · Overloaded — trained 46h ago. Give it rest.**", "**Lats ·
  Recovering — trained 46h ago.**", "**You haven't trained Forearms recently.**" (neglect); ready line
  "**Legs, Chest, Shoulders, Core ready to train.**"; legend Ready/Primed/Recovering/Sore/Rest. ✓
- **Recovery semantics distinct from session heat** — confirmed side by side: Home paints fatigue debt
  (red back), while History → Review still paints session *stimulus* ("TRAPS, MID BACK", heat colors,
  AURON, breakdown intact). ✓
- Recovery states differ with recent-session patterns (fresh back overloaded vs older chest recovered). ✓
- Neglect nudges appear on real data ✓. Plan-overlap: logic unit-tested; **not observed** on this
  dataset (the selected day didn't hit a currently-SORE/OVERLOADED muscle) — stated honestly.
- History / Review flows still work; **app crash buffer clean** throughout. ✓
- Empty state (no 7-day workouts): unit-tested; **not observable** on this seeded dataset — stated
  honestly.

## Assumptions verified vs deferred
- **Verified:** taxonomy fields (progressionType/force/equipment) exist and drive exercise cost; bi-exp
  parameters produce the intended ordering (tests); recovery≠heat on device; no schema/PO/heat change.
- **Deferred (documented):** true rising-edge DOMS curve (plateau-then-decay approximation chosen for
  stability/explainability); per-set (vs per-exercise avg RIR) weighting; connective-tissue/CNS as
  separate compartments; tap-through recovery detail; long-term periodization beyond 7 days.

## Known limitations
- **Granularity mismatch (honest):** group tiles roll up to the most-fatigued *trained* sub-muscle,
  while neglect fires on a specific *planned primary* muscle — so a group can read "Ready" while a
  sub-muscle in it is flagged neglected (e.g. Shoulders 88% + "haven't trained Shoulders recently" when
  only front delts were hit as a pressing secondary). Different signals, not a bug.
- `now` sampled at (re)compute (refreshes on Home revisit/data change, not a live ticking clock).
- Bi-exponential asymptotes near — not exactly — zero; the 7-day window and READY band absorb this.
- Overall readiness = simple mean of 6 groups (not stimulus-weighted).
- Model constants are calibrated estimates, not individualized; no per-user recovery-rate learning.

## Next recommendation
**MUSCLE-4C:** tap-through from the Home card to a per-muscle recovery detail (contributors + the
fast/slow cost breakdown + the supercompensation "best day to PR" hint), and align neglect to the coarse
group granularity to remove the tile-vs-nudge mismatch. Alternatively the PO-metadata quality hook
(MUSCLE-HEAT-2) so readiness also reads CoachInsight/readiness context.
