# DB-5A — REVIEW Dataset Audit (audit only; nothing imported or changed)

Re-evaluation, from first principles, of the **160 REVIEW** free-exercise-db (FED) entries that DB-4B
excluded from the 515-entry FED-canonical catalog. **No source / JSON / assets / tests / plans / README
were modified. No imports, no deletions, no migrations.**

## What "REVIEW" is
FED `dist/exercises.json` = 873 entries. The DB-4B import rule
(`category=strength` ∧ equipment∈{barbell,dumbbell,machine,cable,bodyweight,kettlebell,band,EZ-bar,
medicine-ball,exercise-ball,foam-roll} ∧ primary≠neck ∧ `mechanic` present ∧ `force`∈{push,pull,static})
split the dataset into **SUPPORTED 515 / SKIP 198 (stretching·cardio·plyometrics) / REVIEW 160**.

The 160 REVIEW reconstructs exactly as: **strength 66, powerlifting 38, olympic weightlifting 35,
strongman 21.** Why each was held back:

| Reason | Count |
|---|---|
| Non-strength category (PL / OLY / SM) | 94 |
| `strength` but equipment = `other`/null | 55 |
| `strength` but `force` null | 7 |
| `strength` but primary = `neck` | 5 |
| `strength` but `mechanic` null | 4 |
*(strength entries can fail more than one criterion → reasons overlap.)*

## Classification strategy (refined from the flat SAFE/MAYBE/REJECT)
The single most useful finding is that the 160 are **not one pool** — they split into two fundamentally
different origins, and conflating them hides the signal:

- **Technicality exclusions (the 66 `strength` entries):** mainstream gym movements dropped only on a
  *metadata gap* (equipment tagged `other`/null, or a missing `force`/`mechanic` field) — not because
  they're out of scope. **This is the goldmine: 30 of 40 SAFE additions come from here.**
- **Category exclusions (94: powerlifting / olympic / strongman):** genuinely different training
  modalities; fit must be judged per item and per the engine's progression model.

So the working axes are **(1) modality** then **(2) fit** = `SAFE_ADD / MAYBE_ADD / REJECT`, judged
against AURON's actual capabilities: progression types `COMPOUND_LOAD / ISOLATION_SLOW / BODYWEIGHT /
SKILL_FORM`, an `OTHER` equipment enum (so `other` *is* representable), and **no `NECK` muscle** in the
taxonomy. Decisions were made from the entries themselves (not the prior audit).

- **SAFE_ADD** — mainstream, trackable as weight×reps or reps, clean non-neck muscle map, useful to most lifters.
- **MAYBE_ADD** — niche / advanced / olympic / strongman / accommodating-resistance / suspension / grip / sled.
- **REJECT** — neck (no muscle mapping), rehab/prehab, balance/conditioning/partner drills, and pure strongman *events* (no standard load, awkward/expert-only).

## 1. Counts by class

| Class | Count | of which strength / PL / OLY / SM |
|---|---|---|
| **SAFE_ADD** | **40** | 30 / 10 / 0 / 0 |
| **MAYBE_ADD** | **98** | 27 / 28 / 35 / 8 |
| **REJECT** | **22** | 9 / 0 / 0 / 13 |
| Total | 160 | 66 / 38 / 35 / 21 |

**Images:** all 160 candidates ship **2 bundled images** each in the FED dataset → any future import is a
clean slug-keyed image copy (no missing-media risk).

### SAFE_ADD (40)
- **Equipment:** other 20, barbell 8, null 4, dumbbell 3, machine 3, bodyweight 2.
- **Primary muscles:** hamstrings 7, abs 5, quads 4, chest 4, triceps 4, mid-back 3, lower-back 3, lats 2, glutes 2, delts 2, biceps 2, calves 1, forearms 1.
- **Examples:** Barbell Hip Thrust, Trap Bar Deadlift, Weighted Pull Ups, Sumo Deadlift, Good Morning, Glute Ham Raise, Rack Pulls, Floor Press, Hyperextensions, Dips (Chest), Donkey Calf Raises, Reverse Hyperextension, Inverted Row.
- **Progression fit: excellent.** Loaded compounds (Trap Bar/Sumo DL, Hip Thrust, Good Morning, Box Squat, Floor Press, Rack Pulls, GHR) → `COMPOUND_LOAD`; isolations (Donkey/Front Plate Raise, Reverse Plate Curls, Wrist Roller, Seated Band Ham Curl) → `ISOLATION_SLOW`; bodyweight (Inverted Row, Dips, Walking Lunge, Side Bridge, Ab Roller, Band-Assisted Pull-Up) → `BODYWEIGHT`; weighted-bodyweight (Weighted Pull Ups / Bench Dip / Sit-Ups) → loaded. All map to existing enums; equipment `other`/null resolves to `OTHER` or the obvious type.

### MAYBE_ADD (98)
- **Equipment:** barbell 58, other 34, bands 3, kettlebell 2, null 1.
- **Primary muscles:** quads 29, hamstrings 18, triceps 9, delts 8, lats 7, lower-back 6, abs 5, mid-back 4, chest 3, traps 3, forearms 3, glutes 3.
- **Sub-groups & examples:**
  - **Olympic weightlifting (35)** — Clean, Snatch, Clean and Jerk, Push Press, Power Clean/Snatch from Blocks, Overhead Squat, Hang Clean/Snatch, Snatch/Clean Pull, Snatch/Clean Shrug.
  - **Powerlifting accommodating-resistance (28)** — Squat/Bench/Deadlift with Bands/Chains, Reverse-Band variants, Pin Presses, Board Press, Speed Box Squat, Band Good Mornings, Chain Press.
  - **Strength niche (27)** — suspension/TRX (Suspended Row/Push-Up/Fallout/Split Squat), sled (Sled Row/Reverse Flye/Overhead), advanced bodyweight (Muscle Up, Ring Dips, One Arm Chin-Up, Rocky Pull-Ups), grip (Plate Pinch, Plate Hand Squeeze), KB (Windmill, Figure 8), Svend Press, Scapular Pull-Up.
  - **Strongman carries (8)** — Farmer's Walk, Sled Push, Sled Drag, Backward Drag, Bear Crawl Sled Drags, Sandbag Load, Axle Deadlift, Log Lift.
- **Progression fit: partial / caveated.**
  - *Olympic:* explosive & technique-driven — the V3.6B RIR / grind-to-target model is a **poor semantic fit** (you don't take a snatch to RIR 0). If ever added, route to `SKILL_FORM`, not `COMPOUND_LOAD`.
  - *Bands/chains:* the engine only sees **bar load**; added band/chain tension is invisible, so suggestions slightly understate true load. Works, imperfectly.
  - *Carries/sleds:* **time/distance based**, not weight×reps — the engine has no unit for them; they'd need a different tracking field.
  - *Suspension/grip/advanced BW:* fit fine (`BODYWEIGHT`/`ISOLATION_SLOW`) but serve a narrow audience.

### REJECT (22)
- **Equipment:** other 18, bodyweight 2, bands 1, null 1.
- **Primary muscles:** quads 5, **neck 5**, delts 4, lower-back 3, hamstrings 2, calves 1, chest 1, forearms 1.
- **Examples & reasons:**
  - **Neck (5)** — Isometric Neck (Front/Back, Sides), Lying Face Up/Down Plate Neck, Seated Head Harness: **no `NECK` muscle in the taxonomy** → cannot map.
  - **Drills / rehab / non-progressible (4)** — Balance Board (balance drill), Battling Ropes (conditioning), Internal Rotation with Band (rotator-cuff prehab), Prone Manual Hamstring (partner-manual, untrackable).
  - **Strongman events (13)** — Atlas Stones/Trainer, Tire Flip, Yoke Walk, Conan's Wheel, Keg Load, Car Deadlift, Circus Bell, Rickshaw Carry/Deadlift, Power Stairs, Crucifix, Forward Drag with Press: bespoke implements, no standard weight×reps load, expert/event-only.

## 2. Top 25 highest-value additions (all SAFE; mainstream demand + fills a catalog gap)
1. Barbell Hip Thrust  2. Trap Bar Deadlift  3. Weighted Pull Ups  4. Sumo Deadlift
5. Barbell Glute Bridge  6. Good Morning  7. Glute Ham Raise  8. Rack Pulls
9. Floor Press  10. Hyperextensions (Back Extensions)  11. Dips - Chest Version  12. Parallel Bar Dip
13. Deficit Deadlift  14. Box Squat  15. Reverse Hyperextension  16. Donkey Calf Raises
17. Inverted Row  18. Bodyweight Walking Lunge  19. Dumbbell Floor Press  20. Band Assisted Pull-Up
21. Weighted Bench Dip  22. Front Plate Raise  23. Decline Push-Up  24. Plate Twist
25. Seated Band Hamstring Curl

## 3. Top 50 highest-value additions
Top 25 above, plus:
26. Push-Up Wide  27. Wrist Roller  28. Reverse Plate Curls  29. Platform Hamstring Slides
30. Single Dumbbell Raise  31. Smith Machine Decline Press  32. Side Bridge  33. Knee/Hip Raise On Parallel Bars
34. Incline Inner Biceps Curl  35. Bodyweight Mid Row  36. Ab Roller  37. Weighted Sit-Ups - With Bands
38. Weighted Squat  39. Inverted Row with Straps  40. Floor Glute-Ham Raise
*(40 SAFE exhausted — the next 10 are the strongest MAYBE picks):*
41. Push Press  42. Overhead Squat  43. Pin Presses  44. Seated Good Mornings  45. Farmer's Walk
46. Sled Push  47. Muscle Up  48. Ring Dips  49. Single-Leg High Box Squat  50. Svend Press

## 4. Estimated final catalog sizes
| Scenario | Size |
|---|---|
| Current (DB-4B) | **515** |
| + SAFE_ADD only | **555** (+40) |
| + SAFE_ADD + MAYBE_ADD | **653** (+138) |
| (REJECT 22 never added) | — |

*Image cost:* +40 SAFE ≈ +80 images (~5 MB); +138 ≈ +276 images (~16 MB). All candidates already have 2
frames in the dataset.

## 5. Risks
- **Duplicate / near-duplicate creep.** Several candidates are variants of catalog entries (e.g. *Sumo
  Deadlift* vs *Barbell Deadlift*, *Box Squat* vs *Barbell Squat*, the vague *Weighted Squat*, *Bench
  Press - Powerlifting* — held to MAYBE for this reason). A name/dedup pass is required before any import.
- **Engine semantic mismatch (MAYBE).** Olympic lifts and carries/sleds map awkwardly to the RIR/load
  model; importing them without a `SKILL_FORM` route (olympic) or a time/distance unit (carries) would
  produce odd suggestions. SAFE set has none of this risk.
- **Taxonomy gaps.** Neck (5) is unmappable today; band/chain tension and grip-hold "load" aren't
  representable. Don't force-map.
- **Coarse FED muscle mapping.** FED uses generic `shoulders`/`chest` (no front/side/rear-delt split) —
  same limitation noted in DB-3E; SAFE additions inherit it.
- **Force/mechanic data gaps** on the technicality set mean a few SAFE entries need a one-time
  `progressionType`/`force` assignment at import (deterministic from name + mechanic), not a blocker.
- **Scope creep.** Jumping straight to +138 (653) dilutes the catalog with band/chain/olympic entries
  most users won't touch; SAFE-only (555) keeps quality high.

## 6. Recommended DB-5B scope
**Import SAFE_ADD only (40 → catalog 555).** Rationale: highest value-per-entry, clean progression
mapping, fills real gaps (glutes/hip thrust, trap-bar, weighted calisthenics, posterior chain), all with
images, and minimal engine/taxonomy risk. Concretely for DB-5B:
1. Take the 40 SAFE entries; run a **dedup/name pass** against the 515 (resolve *Sumo Deadlift*, *Box
   Squat*, *Weighted Squat*, GHR vs Floor Glute-Ham Raise, etc.).
2. Generate their `CatalogExercise` rows into the JSON catalog with deterministic `progressionType`
   (compound→`COMPOUND_LOAD`, isolation→`ISOLATION_SLOW`, bodyweight→`BODYWEIGHT`); equipment `other`/null →
   `OTHER`/inferred; backfill the missing `force` from mechanic+name.
3. Copy the 80 images under FED slugs; bump `CatalogSeed.CONTENT_VERSION`; **no Room schema change**.
4. Update catalog tests to the new count; rebuild + on-device smoke (same discipline as DB-4B.2/4B.3).
5. **Defer** MAYBE_ADD to a later, opt-in pass — and only after the engine gains a `SKILL_FORM` route for
   olympic lifts and a time/distance unit for carries. **Drop REJECT (22) permanently.**

*Deliverable note:* this is an audit only — no code, JSON, assets, tests, plans, or README were touched.
