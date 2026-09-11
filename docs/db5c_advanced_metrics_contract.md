# DB-5C — Advanced Metrics Contract for MAYBE Exercises (planning/report only)

A data contract for the **98 MAYBE_ADD** exercises from DB-5A: how each would be tracked and what the
engine would need before any future import. **Nothing was imported or changed** — no source, catalog
JSON, assets, Room schema, engine, History, or Plan. Machine-readable companion:
`docs/db5c_maybe_tracking_contract.csv`.

## State verified
catalog FED-canonical **548**, images **548 / 1096**, Room `@Database version` **4**, CONTENT_VERSION
**11**, DB-5B.1 runtime QA = GO. Engine intentionally untouched by this task.

## MAYBE set
The 98 MAYBE = the 160 REVIEW set minus the 40 SAFE_ADD (imported in DB-5B) minus the 22 REJECT.
By FED category: **olympic weightlifting 35, powerlifting 28, strength 27, strongman 8**.

## Tracking-type classification (7 types)

| Tracking type | Count | Required log fields | Engine support today | Import now? |
|---|---|---|---|---|
| **SETS_REPS_WEIGHT** | **39** | sets; reps; weight; RIR | ✅ Yes — current `COMPOUND_LOAD`/`ISOLATION_SLOW` | **Import-ready** (after dedup + metadata cleanup) |
| **VELOCITY_POWER** | **25** | sets; reps; weight; intent/%1RM (bar speed) | ❌ No — grind-to-RIR model is wrong for O-lifts | Defer (engine) |
| **BODYWEIGHT_SKILL** | **18** | sets; reps; optional added-weight; difficulty/variation | ⚠️ Partial — existing `BODYWEIGHT` covers it conservatively | Defer (curation/scope) |
| **TIME_DISTANCE_LOAD** | **8** | load; distance; time | ❌ No — no time/distance unit | Defer (engine) |
| **SKILL_FORM** | **5** | sets; reps OR hold-time; quality/RIR | ⚠️ Partial — existing `SKILL_FORM` covers it (+optional hold-time field) | Defer (curation/scope) |
| **UNSUPPORTED_FOR_NOW** | **2** | n/a | — | **Do not import** |
| **DISTANCE_LOAD** | **1** | load; distance | ❌ No — no distance unit | Defer (engine) |
| **Total** | **98** | | | |

### SETS_REPS_WEIGHT (39) — import-ready, no engine change
- **What:** all 28 powerlifting MAYBE (band/chain/board/pin/reverse-band squat·bench·deadlift·good-morning
  variants, Bench Press - Powerlifting, Kneeling Squat) + 9 olympic *slow-strength accessories*
  (Clean/Snatch Deadlift, Clean/Snatch Pull, Clean/Snatch Shrug, RDL from Deficit, Olympic Squat,
  Frankenstein Squat, Wide Stance Stiff Legs) + Axle Deadlift + Svend Press.
- **Examples:** Pin Presses, Board Press, Squat with Chains, Reverse Band Deadlift, Seated Good Mornings,
  Clean Pull, Snatch Deadlift, Olympic Squat.
- **Progression idea:** standard load progression (RIR-gated, exactly like the SAFE imports).
- **Engine:** none. Band/chain tension and dynamic-effort bar speed are coaching cues, **not** required
  log fields — the bar load is what's logged and progressed.
- **Caveat:** several are near-duplicates of existing 548 entries (e.g. *Bench Press - Powerlifting* vs
  Barbell Bench Press; the chain/band variants vs their straight-weight forms) → a strict dedup pass is
  mandatory before importing, and accommodating-resistance variants add clutter for a hypertrophy app.

### VELOCITY_POWER (25) — engine-blocked
- **What:** the explosive Olympic lifts and derivatives.
- **Examples:** Clean, Snatch, Clean and Jerk, Power Clean from Blocks, Hang Clean, Push Press, Power
  Jerk, Split Jerk, Muscle Snatch, Snatch Balance, Kneeling Jump Squat.
- **Progression idea:** load at sub-maximal % with **intent/bar-speed**, never reps-to-failure; technique
  and power, not hypertrophy fatigue.
- **Engine:** **needs a non-RIR technical/velocity route.** Logging them as SETS_REPS_WEIGHT would make
  the engine suggest grinding to RIR 0 — wrong and unsafe for O-lifts. Defer until the engine has at
  least a "technical lift" mode (treat like SKILL_FORM) or a true velocity/intent model.

### BODYWEIGHT_SKILL (18) — engine-compatible, deferred for scope
- **What:** advanced calisthenics + suspension (TRX).
- **Examples:** Muscle Up, One Arm Chin-Up, Ring Dips, Rocky Pull-Ups, Scapular Pull-Up, Suspended
  Row/Push-Up/Split Squat, Single-Leg High Box Squat, Rope Climb.
- **Progression idea:** build reps, then progress to a harder variation/lever (the existing `BODYWEIGHT`
  rule already says "reps/tempo/harder variation, no auto +kg").
- **Engine:** none required — `BODYWEIGHT` handles them safely. **Deferred for curation reasons** (advanced,
  niche audience), not engine reasons. A future "difficulty/variation ladder" field would make them
  first-class but is optional.

### TIME_DISTANCE_LOAD (8) — engine-blocked
- **What:** sleds and drags.
- **Examples:** Sled Push, Sled Drag - Harness, Backward Drag, Bear Crawl Sled Drags, Sled Row, Sled
  Reverse Flye, Sled Overhead Triceps Extension, Sled Overhead Backward Walk.
- **Progression idea:** load × distance (and/or time); progress distance or load, no rep target.
- **Engine:** **needs new time/distance log fields** and a non-rep progression. Defer.

### SKILL_FORM (5) — engine-compatible, deferred for scope
- **What:** grip isometrics + kettlebell skill.
- **Examples:** Plate Pinch, Standing Olympic Plate Hand Squeeze, Double Kettlebell Windmill, Kettlebell
  Figure 8, Overhead Squat.
- **Progression idea:** reps/holds and quality, never auto-load (the existing `SKILL_FORM` rule, added in
  V3.5 for Pallof/planks, already does this).
- **Engine:** none required — `SKILL_FORM` covers them. An optional **hold-time** field would make grip
  isometrics (Plate Pinch) first-class; without it they log as reps. Deferred for curation.

### DISTANCE_LOAD (1) — engine-blocked
- **What:** Farmer's Walk (loaded carry).
- **Progression idea:** load × distance; progress distance then load.
- **Engine:** **needs a distance log field.** Defer.

### UNSUPPORTED_FOR_NOW (2) — do not import
- **What:** Sandbag Load, Log Lift — specialty strongman implements/events, out of current hypertrophy
  scope. Keep out (alongside the 22 DB-5A REJECT) until scope explicitly expands.

## Recommended DB next steps

1. **Importable now, no engine change (39 → catalog ~587 max):** the SETS_REPS_WEIGHT group. **But**
   gate it behind a strict dedup pass — many are accommodating-resistance/specialty near-duplicates that
   would clutter a hypertrophy catalog. *Recommendation:* import only the genuinely distinct,
   broadly-useful ones (e.g. Pin Presses, Board Press, Clean/Snatch Pull, Olympic Squat) in a future
   small batch; do **not** bulk-import all 39.
2. **Engine-compatible but deferred (23):** BODYWEIGHT_SKILL (18) + SKILL_FORM (5) map to existing engine
   types and could be added with no engine change, but they're advanced/niche — hold until there's demand
   (and ideally a difficulty-ladder / hold-time field).
3. **Needs Engine support first (34):** VELOCITY_POWER (25), TIME_DISTANCE_LOAD (8), DISTANCE_LOAD (1).
   Do not import until the engine gains the corresponding tracking units/modes.
4. **Stay rejected (24):** the 2 UNSUPPORTED_FOR_NOW here + the 22 DB-5A REJECT (neck, drills, strongman
   events).

## Message to the Engine owner

> **DB-5C summary — what the engine would need to unlock the deferred MAYBE exercises (34 of 98):**
>
> 1. **Olympic/power lifts (25, VELOCITY_POWER):** add a **"technical lift" mode** that does *not* apply
>    the RIR/grind-to-target rules — at minimum route these like `SKILL_FORM` (progress load/technique,
>    never suggest reps-to-failure). A true velocity/intent (%1RM, bar-speed) model is the ideal but a
>    non-RIR technical route is the blocking minimum.
> 2. **Sleds & drags (8, TIME_DISTANCE_LOAD) and carries (1, DISTANCE_LOAD):** add **distance and time
>    log fields** and a progression that advances distance/time/load instead of reps. This is a new
>    tracking unit the app doesn't have yet (and is exactly the unit DB-5C was told *not* to build).
> 3. **No action needed for 39 SETS_REPS_WEIGHT** (current `COMPOUND_LOAD`/`ISOLATION_SLOW` handle them)
>    or for the 23 BODYWEIGHT_SKILL/SKILL_FORM (existing `BODYWEIGHT`/`SKILL_FORM` types) — those are
>    catalog/curation decisions, not engine work. Optional nice-to-haves: a **hold-time** field (grip
>    isometrics) and a **difficulty/variation ladder** (advanced calisthenics).
>
> Safety constraints to preserve: RIR-0 still blocks increases; bodyweight/skill never get an auto +kg;
> any new mode must be additive (no Room schema break, no change to the V3.7A/V3.6B rules for existing
> types). Full per-exercise contract: `docs/db5c_maybe_tracking_contract.csv`.

## Confirmation
No source code, catalog JSON, assets, Room schema/version, engine, History, or Plan were modified. Only
two docs were created (`db5c_advanced_metrics_contract.md`, `db5c_maybe_tracking_contract.csv`).
